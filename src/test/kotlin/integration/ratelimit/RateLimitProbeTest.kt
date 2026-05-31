package integration.ratelimit

import integration.integrationTestEnvVarsAreSet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Pre-Phase-1 investigation probes for the rate-limiting overhaul (task §7).
 *
 * Bundles investigations #1, #2, #3 from `journal/_task_07_rate_limiting_overhaul.md`:
 *   1. Burst capacity — how many requests can we fire before the first 429?
 *   2. Retry-After presence — is the header always present on 429s?
 *   3. x-ratelimit-* presence — do those headers appear on 200s, 429s, both, neither?
 *
 * Probe shape: fire BURST_SIZE concurrent GETs against /v1/users/me (cheap, idempotent),
 * capture status + response headers per call, then repeat after each idle gap in IDLE_GAPS
 * so we can observe bucket refill behaviour. No production code is exercised by this probe —
 * it uses a raw HttpClient with no rate-limit plugin so we see Notion's unmediated behaviour.
 *
 * Excluded from the standard run by the `Investigation` tag. Run on demand with:
 *
 *   export NOTION_API_TOKEN=secret_...
 *   export NOTION_TEST_PAGE_ID=...               # not used by these probes but standard guard
 *   export NOTION_RUN_INTEGRATION_TESTS=true
 *   export NOTION_RUN_INVESTIGATIONS=true
 *   ./gradlew test -Dkotest.tags.include="Investigation"
 *
 * Paste the captured markdown tables into the task doc as Investigation findings.
 */
@Tags("Integration", "RequiresApi", "Slow", "Investigation")
class RateLimitProbeTest :
    StringSpec({

        /**
         * Bound concurrent fire count per burst. Started at 25; bumped to 100 after
         * an initial run against `/v1/users/me` returned zero 429s across 100 requests.
         * Now paired with a `pages.retrieve` endpoint (see `pageId` below) which is
         * more likely to hit the workspace-wide read bucket than the auth endpoint.
         */
        val burstSize = 100

        /**
         * Idle gaps (seconds) before each successive burst. 0 = back-to-back.
         * Picked to cover "no recovery" / "partial refill" / "full refill" / "long idle".
         */
        val idleGaps = listOf(0L, 5L, 30L, 60L)

        val probeEnabled =
            integrationTestEnvVarsAreSet() &&
                System.getenv("NOTION_RUN_INVESTIGATIONS")?.lowercase() == "true"

        if (!probeEnabled) {
            "!(skipped) probes require env vars + opt-in" {
                println(
                    "Skipping RateLimitProbeTest. To run: " +
                        "export NOTION_API_TOKEN=... NOTION_TEST_PAGE_ID=... " +
                        "NOTION_RUN_INTEGRATION_TESTS=true NOTION_RUN_INVESTIGATIONS=true " +
                        "and ./gradlew test -Dkotest.tags.include=\"Investigation\"",
                )
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val pageId = System.getenv("NOTION_TEST_PAGE_ID")
            val apiVersion = "2022-06-28"

            // Bare client — no auth/content plugins from our codebase, no rate limiter.
            // We want to observe Notion's raw response behaviour.
            val rawClient =
                HttpClient(CIO) {
                    install(HttpTimeout) {
                        requestTimeoutMillis = 60_000
                        connectTimeoutMillis = 10_000
                        socketTimeoutMillis = 30_000
                    }
                    defaultRequest {
                        url {
                            protocol = URLProtocol.HTTPS
                            host = "api.notion.com"
                        }
                        headers.append("Authorization", "Bearer $token")
                        headers.append("Notion-Version", apiVersion)
                        headers.append("User-Agent", "kotlin-notion-client/probe")
                    }
                }

            val mark = TimeSource.Monotonic.markNow()
            val allRecords = mutableListOf<ProbeRecord>()

            beforeSpec {
                println("=== Burst probe begins (burstSize=$burstSize, gaps=$idleGaps) ===")
                for ((roundIdx, gap) in idleGaps.withIndex()) {
                    if (gap > 0) {
                        println("  → idle ${gap}s before round ${roundIdx + 1} …")
                        delay(gap.seconds)
                    }
                    val roundStart = mark.elapsedNow()
                    val records = fireBurst(rawClient, burstSize, gap, mark, pageId)
                    allRecords += records
                    val firstNon200 = records.firstOrNull { it.status != 200 }
                    println(
                        "  round ${roundIdx + 1} (after ${gap}s idle): " +
                            "fired $burstSize at t=${roundStart.inWholeMilliseconds}ms — " +
                            (
                                firstNon200?.let { "first non-200 at idx=${it.idx} status=${it.status}" }
                                    ?: "all 200"
                            ),
                    )
                }
                println("=== Burst probe complete: ${allRecords.size} responses captured ===")
            }

            afterSpec {
                rawClient.close()
            }

            "#1 burst capacity — when does the first 429 appear?" {
                println("\n### Investigation #1 — burst capacity\n")
                println("| Round | Idle gap (s) | First non-200 idx | First non-200 status | 429 count | 200 count | Total |")
                println("| --- | --- | --- | --- | --- | --- | --- |")
                allRecords
                    .groupBy { it.roundGapSeconds }
                    .toSortedMap()
                    .entries
                    .forEachIndexed { roundIdx, (gap, records) ->
                        val firstNon200 = records.firstOrNull { it.status != 200 }
                        val countByStatus = records.groupingBy { it.status }.eachCount()
                        val n429 = countByStatus[429] ?: 0
                        val n200 = countByStatus[200] ?: 0
                        println(
                            "| ${roundIdx + 1} | $gap | ${firstNon200?.idx ?: "—"} | " +
                                "${firstNon200?.status ?: "—"} | $n429 | $n200 | ${records.size} |",
                        )
                    }
                println(
                    "\n_Decision impact: pick `burstCapacity` default ≤ smallest observed first-429 index, " +
                        "with a 1–2 token safety margin._",
                )
            }

            "#2 retry-after presence on 429s" {
                println("\n### Investigation #2 — Retry-After header on 429 responses\n")
                val all429s = allRecords.filter { it.status == 429 }
                if (all429s.isEmpty()) {
                    println(
                        "**No 429s observed across ${allRecords.size} requests.** " +
                            "Consider bumping burstSize and re-running. " +
                            "Until at least one 429 is captured, retry-after presence cannot be confirmed.",
                    )
                } else {
                    val withHeader = all429s.count { !it.retryAfter.isNullOrBlank() }
                    val withoutHeader = all429s.size - withHeader
                    println("- Total 429s observed: ${all429s.size}")
                    println("- 429s WITH `Retry-After`: $withHeader")
                    println("- 429s WITHOUT `Retry-After`: $withoutHeader")
                    if (withoutHeader > 0) {
                        println(
                            "\n**ATTENTION: at least one 429 lacked Retry-After. " +
                                "Keep exponential fallback for 429s in D4.**",
                        )
                    } else {
                        println(
                            "\n**All 429s carried Retry-After. " +
                                "D4 may safely drop the exponential fallback for 429s and use header value directly.**",
                        )
                    }
                    println(
                        "\nSample Retry-After values: " +
                            all429s.mapNotNull { it.retryAfter }.distinct().joinToString(", "),
                    )
                }
            }

            "#3 x-ratelimit-* header presence on 200s and 429s" {
                println("\n### Investigation #3 — x-ratelimit-* headers\n")
                val on200 = allRecords.filter { it.status == 200 }
                val on429 = allRecords.filter { it.status == 429 }

                fun present(
                    records: List<ProbeRecord>,
                    get: (ProbeRecord) -> String?,
                ): String {
                    if (records.isEmpty()) return "n/a (no responses)"
                    val with = records.count { !get(it).isNullOrBlank() }
                    return "$with / ${records.size}"
                }

                println("| Header | Present on 200s | Present on 429s |")
                println("| --- | --- | --- |")
                println("| x-ratelimit-limit | ${present(on200) { it.rlLimit }} | ${present(on429) { it.rlLimit }} |")
                println("| x-ratelimit-remaining | ${present(on200) { it.rlRemaining }} | ${present(on429) { it.rlRemaining }} |")
                println("| x-ratelimit-reset | ${present(on200) { it.rlReset }} | ${present(on429) { it.rlReset }} |")

                val sample200 = on200.firstOrNull { it.rlLimit != null || it.rlRemaining != null || it.rlReset != null }
                val sample429 = on429.firstOrNull { it.rlLimit != null || it.rlRemaining != null || it.rlReset != null }
                println("\nSample values:")
                println(
                    "- 200 sample: limit=${sample200?.rlLimit} remaining=${sample200?.rlRemaining} " +
                        "reset=${sample200?.rlReset}",
                )
                println(
                    "- 429 sample: limit=${sample429?.rlLimit} remaining=${sample429?.rlRemaining} " +
                        "reset=${sample429?.rlReset}",
                )
                println(
                    "\n_Decision impact: if headers absent on 200s, drop pre-emptive throttling (D3 / Q2) " +
                        "and rely on bucket math + 429 reactive path._",
                )
            }
        }
    })

private data class ProbeRecord(
    val roundGapSeconds: Long,
    val idx: Int,
    val offsetMs: Long,
    val status: Int,
    val retryAfter: String?,
    val rlLimit: String?,
    val rlRemaining: String?,
    val rlReset: String?,
    val bodyPreview: String,
)

private suspend fun fireBurst(
    client: HttpClient,
    n: Int,
    gapSeconds: Long,
    mark: TimeSource.Monotonic.ValueTimeMark,
    pageId: String,
): List<ProbeRecord> =
    coroutineScope {
        (1..n)
            .map { idx ->
                async {
                    val t0 = mark.elapsedNow()
                    val response: HttpResponse =
                        try {
                            client.get { url { path("v1", "pages", pageId) } }
                        } catch (t: Throwable) {
                            // Surface failures as a synthetic record (status=-1) rather than aborting.
                            return@async ProbeRecord(
                                roundGapSeconds = gapSeconds,
                                idx = idx,
                                offsetMs = t0.inWholeMilliseconds,
                                status = -1,
                                retryAfter = null,
                                rlLimit = null,
                                rlRemaining = null,
                                rlReset = null,
                                bodyPreview = "EXC: ${t::class.simpleName}: ${t.message}",
                            )
                        }
                    val body =
                        runCatching { response.bodyAsText() }
                            .getOrElse { "<body read failed: ${it.message}>" }
                            .take(160)
                            .replace("\n", " ")
                    ProbeRecord(
                        roundGapSeconds = gapSeconds,
                        idx = idx,
                        offsetMs = t0.inWholeMilliseconds,
                        status = response.status.value,
                        retryAfter = response.headers["Retry-After"],
                        rlLimit = response.headers["x-ratelimit-limit"],
                        rlRemaining = response.headers["x-ratelimit-remaining"],
                        rlReset = response.headers["x-ratelimit-reset"],
                        bodyPreview = body,
                    )
                }
            }.awaitAll()
            .sortedBy { it.offsetMs }
    }
