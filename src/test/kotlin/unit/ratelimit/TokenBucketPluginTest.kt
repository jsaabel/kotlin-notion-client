@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package unit.ratelimit

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.saabel.kotlinnotionclient.ratelimit.NotionRateLimit
import it.saabel.kotlinnotionclient.ratelimit.RateLimitStrategy
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verifies the [NotionRateLimit] plugin acquires a token from its per-client [TokenBucket] before
 * each outbound request, so requests beyond `burstCapacity` are paced at the sustained rate.
 *
 * The bucket's clock is wired to the test scheduler (`timeSourceMillis = { currentTime }`) so the
 * pacing `delay()`s run in virtual time.
 */
@Tags("Unit")
class TokenBucketPluginTest :
    FunSpec({

        fun okClient(
            counter: AtomicInteger,
            timeSource: () -> Long,
            burst: Int,
            rate: Double,
        ): HttpClient =
            HttpClient(
                MockEngine {
                    counter.incrementAndGet()
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            ) {
                install(NotionRateLimit) {
                    strategy = RateLimitStrategy.CUSTOM
                    maxRetries = 0
                    sustainedRate = rate
                    burstCapacity = burst
                    timeSourceMillis = timeSource
                }
            }

        test("requests beyond burst capacity are paced through the plugin's token bucket") {
            runTest {
                val counter = AtomicInteger(0)
                val client = okClient(counter, { currentTime }, burst = 5, rate = 3.0)

                val total = 10
                val jobs = (1..total).map { launch { client.get("https://api.notion.com/v1/ping") } }
                advanceUntilIdle()
                jobs.forEach { it.join() }

                // Every request reached the engine (bucket throttles, it does not drop).
                counter.get() shouldBe total

                // 5 burst at t=0, then 5 paced at ~3/s → last lands near (total - burst) / rate s.
                val expectedMillis = ((total - 5) / 3.0 * 1000).toLong() // ≈ 1666 ms
                (currentTime in (expectedMillis - 400)..(expectedMillis + 400)) shouldBe true

                client.close()
            }
        }
    })
