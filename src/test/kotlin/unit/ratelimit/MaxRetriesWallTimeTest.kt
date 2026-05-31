@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package unit.ratelimit

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.saabel.kotlinnotionclient.ratelimit.NotionRateLimit
import it.saabel.kotlinnotionclient.ratelimit.RateLimitConfig
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * Regression guard for the wasted-final-delay defect (issue #19, defect #5). When every attempt
 * returns `429` and retries are exhausted, the plugin must throw/return immediately after the final
 * attempt — it must **not** sleep one extra backoff interval before giving up.
 *
 * With `maxRetries = 3`, `retryBaseDelay = 1s`, no jitter and a `429` that carries no `Retry-After`,
 * the exponential schedule is 1s + 2s + 4s = 7s across the three retries. The bug would have added
 * the unused fourth interval (2^3 = 8s) for a total of 15s. Asserting exactly 7s of virtual time
 * pins the fix.
 */
@Tags("Unit")
class MaxRetriesWallTimeTest :
    FunSpec({

        test("persistent 429: wall time equals the sum of retry delays, with no extra post-mortem sleep") {
            runTest {
                val counter = AtomicInteger(0)
                val client =
                    HttpClient(
                        MockEngine { _ ->
                            counter.incrementAndGet()
                            respond(
                                content = """{"object":"error","status":429,"code":"rate_limited","message":"slow down"}""",
                                status = HttpStatusCode.TooManyRequests,
                                // No Retry-After header → exponential schedule drives every delay.
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                            )
                        },
                    ) {
                        install(NotionRateLimit) {
                            rateLimitConfig =
                                RateLimitConfig(
                                    maxRetries = 3,
                                    retryBaseDelay = 1.seconds,
                                    retryMaxDelay = 30.seconds,
                                    jitterFactor = 0.0,
                                )
                            timeSourceMillis = { currentTime }
                        }
                    }

                val start = currentTime
                val response: HttpResponse = client.get("https://api.notion.com/v1/ping")
                val elapsed = currentTime - start

                // 1 initial + 3 retries = 4 attempts; maxRetries counts retries, not total calls.
                counter.get() shouldBe 4
                response.status shouldBe HttpStatusCode.TooManyRequests
                // 1s + 2s + 4s = 7s of backoff. NOT 7s + the unused 8s fourth interval.
                elapsed shouldBe 7_000L

                client.close()
            }
        }
    })
