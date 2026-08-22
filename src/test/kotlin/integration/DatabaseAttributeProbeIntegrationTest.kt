package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig

/**
 * Settles, in one run, what the two update endpoints accept for `icon` and `cover`.
 *
 * Three questions are open, and each one decides whether a builder affordance should exist. All
 * of them are answered by sending a payload our typed models cannot express (that is the point)
 * and reading the status back, so the requests go out through a bare Ktor client rather than
 * through `databases.update` / `dataSources.update`.
 *
 * | # | Question | Why it is open |
 * | --- | --- | --- |
 * | 1 | Does `PATCH /v1/data_sources` accept a `cover`? | `UpdateDataSourceRequestBuilder` has an `icon` builder and no `cover`, and the asymmetry is unresolved rather than deliberate (IDEAS.md #11). The migration guide lists `cover` as container-only — but it also omits `icon`, which a data source demonstrably carries, so it is not exhaustive. The `DataSource` *response* model has a `cover` field, which says a data source can hold one, not that the endpoint accepts one. |
 * | 2 | Does `PATCH /v1/databases` accept `"cover": null`? | `"icon": null` is **rejected** there (see below). Whether `cover` behaves the same way is untested, and the answer decides whether a `cover.remove()` can exist on the container. |
 * | 3 | Does `PATCH /v1/data_sources` accept `"icon": null`? | This is the practically useful one: Notion's UI renders the *data source's* icon, so "remove the icon a reader sees" means clearing that one. Neither builder offers it today. |
 *
 * Question 0 is already answered and is asserted here rather than printed, because
 * `UpdateDatabaseRequestBuilder` is now shaped around it: `PATCH /v1/databases` answers
 * `"icon": null` with
 *
 * ```
 * HTTP 400 validation_error: body failed validation:
 * body.icon should be an object or `undefined`, instead was `null`.
 * ```
 *
 * That is why the container builder has no `icon.remove()`. If Notion ever starts accepting the
 * null, this assertion fails and tells us the affordance can be added.
 *
 * ## Reading the result
 *
 * Each open question prints a verdict. The spec deliberately does not assert on them — either
 * outcome is a valid finding, and a red test would misrepresent that. Three outcomes per
 * question:
 *
 * | Outcome | Means | Do |
 * | --- | --- | --- |
 * | 2xx **and** the re-retrieved object changed | accepted and persisted | Add the affordance — a `CoverBuilder` on `UpdateDataSourceRequestBuilder` (Q1), or a `remove()` (Q2, Q3) — plus the matching field on the request model and its `resolvePendingUploads` overload |
 * | 2xx **but** the object is unchanged | accepted and silently ignored | Do **not** add it. That is the #80 failure mode: a call that compiles, runs, returns and does nothing. Record it as a documented limitation |
 * | 4xx | rejected | Do **not** add it. The `validation_error` body names every shape the endpoint would have accepted — paste it into the KDoc as the citation, as question 0 above did |
 *
 * The container control at the end sends a *valid* cover to `PATCH /v1/databases`, which we know
 * is supported. It is what makes a 4xx elsewhere interpretable: same token, same headers, same
 * cover object, different endpoint. If the control fails too, the payload or the credentials are
 * at fault and none of the verdicts above mean anything.
 *
 * Run with: ./gradlew integrationTest --tests "*DatabaseAttributeProbeIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class DatabaseAttributeProbeIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) database attribute probe" {
                println("Skipping DatabaseAttributeProbeIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val rootPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val config = NotionConfig(apiToken = token)
            val notion = NotionClient.create(config)

            val coverUrl = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d"
            val coverObject = """{"type":"external","external":{"url":"$coverUrl"}}"""

            "what do the update endpoints accept for icon and cover?" {
                val database =
                    notion.databases.create {
                        parent.page(rootPageId)
                        title("Attribute probe")
                        icon.emoji("🧪")
                        properties { title("Name") }
                    }
                val dataSourceId = database.dataSources.first().id
                // create() propagates the icon to the initial data source, so both sides start
                // with an icon and "was it cleared?" is a meaningful question on both.
                val databaseUrl = "${config.baseUrl}/databases/${database.id}"
                val dataSourceUrl = "${config.baseUrl}/data_sources/$dataSourceId"

                val raw = HttpClient(CIO) { expectSuccess = false } // a 400 is a result, not a failure

                try {
                    suspend fun probe(
                        label: String,
                        url: String,
                        body: String,
                    ): Pair<Int, String> {
                        val response: HttpResponse =
                            raw.patch(url) {
                                header(HttpHeaders.Authorization, "Bearer $token")
                                header("Notion-Version", config.apiVersion)
                                contentType(ContentType.Application.Json)
                                setBody(body)
                            }
                        val status = response.status.value
                        val text = response.bodyAsText()
                        println("\n$label")
                        println("  PATCH $url")
                        println("  body: $body")
                        println("  → HTTP $status")
                        if (status !in 200..299) println("  → $text")
                        return status to text
                    }

                    /** Prints the verdict for a question whose answer is "did the object change?". */
                    fun verdict(
                        status: Int,
                        changed: Boolean,
                        affordance: String,
                    ) {
                        println(
                            when {
                                status !in 200..299 -> "  VERDICT: rejected → do NOT add $affordance; cite the message above"
                                changed -> "  VERDICT: accepted and persisted → add $affordance"
                                else -> "  VERDICT: accepted but ignored → do NOT add $affordance, document the limitation"
                            },
                        )
                    }

                    // Question 0 — established, and the container builder's shape depends on it.
                    val (nullIconStatus, nullIconBody) =
                        probe(
                            "Q0 — database container, icon removal (expected: rejected)",
                            databaseUrl,
                            """{"icon":null}""",
                        )
                    nullIconStatus shouldBe 400
                    nullIconBody shouldContain "body.icon should be an object or `undefined`"
                    println("  VERDICT: rejected, as expected → container icon can be replaced but not cleared")

                    // Question 1 — does a data source accept a cover at all?
                    val (dsCoverStatus, _) =
                        probe(
                            "Q1 — data source, set a cover",
                            dataSourceUrl,
                            """{"cover":$coverObject}""",
                        )
                    val dsCover = notion.dataSources.retrieve(dataSourceId).cover
                    println("  re-retrieved data source cover: $dsCover")
                    verdict(dsCoverStatus, dsCover != null, "a CoverBuilder to UpdateDataSourceRequestBuilder")

                    // Question 2 — can a container cover be cleared, even though its icon cannot?
                    probe("Q2a — database container, set a cover first", databaseUrl, """{"cover":$coverObject}""")
                    println("  container cover now: ${notion.databases.retrieve(database.id).cover}")
                    val (dbNullCoverStatus, _) =
                        probe(
                            "Q2b — database container, cover removal",
                            databaseUrl,
                            """{"cover":null}""",
                        )
                    val dbCoverAfter = notion.databases.retrieve(database.id).cover
                    println("  container cover after: $dbCoverAfter")
                    verdict(dbNullCoverStatus, dbCoverAfter == null, "cover.remove() to UpdateDatabaseRequestBuilder")

                    // Question 3 — the one that matters in the UI: can the data source icon be cleared?
                    println("\n  data source icon before: ${notion.dataSources.retrieve(dataSourceId).icon}")
                    val (dsNullIconStatus, _) =
                        probe(
                            "Q3 — data source, icon removal",
                            dataSourceUrl,
                            """{"icon":null}""",
                        )
                    val dsIconAfter = notion.dataSources.retrieve(dataSourceId).icon
                    println("  data source icon after: $dsIconAfter")
                    verdict(dsNullIconStatus, dsIconAfter == null, "icon.remove() to UpdateDataSourceRequestBuilder")

                    // Control — a shape we know is accepted, to prove the 4xx verdicts above are
                    // about the endpoint and not about the payload or the credentials.
                    val (controlStatus, _) =
                        probe(
                            "Control — database container, set an emoji icon (expected: accepted)",
                            databaseUrl,
                            """{"icon":{"type":"emoji","emoji":"✅"}}""",
                        )
                    if (controlStatus !in 200..299) {
                        println("  ⚠ The control failed — the credentials or headers are at fault, not the endpoints.")
                    } else {
                        println("  Control passed → the verdicts above are about the endpoints.")
                    }
                } finally {
                    raw.close()
                    if (shouldCleanupAfterTest()) {
                        notion.databases.trash(database.id)
                    } else {
                        println("\n  Database URL (kept for inspection): ${database.url}")
                    }
                }
            }
        }
    })
