@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package unit.ratelimit

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
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
import it.saabel.kotlinnotionclient.ratelimit.RateLimitStrategy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verifies the [NotionRateLimit] plugin makes `Retry-After` load-bearing on `429` responses
 * (issue #16). Notion publishes `Retry-After` (seconds) as its 429 handling contract, so when the
 * header is present the plugin must wait exactly that long (plus a 1s rounding-safety margin)
 * rather than fall back to its exponential schedule.
 *
 * Timing is asserted under `kotlinx.coroutines.test` virtual time: the token bucket's clock is
 * wired to the test scheduler and `delay()` advances virtual time, so a "3 second" wait costs no
 * wall-clock time.
 */
@Tags("Unit")
class RetryAfterPluginTest :
    FunSpec({

        val errorBody = """{"object":"error","status":429,"code":"rate_limited","message":"slow down"}"""

        test("429 with Retry-After: 3 then 200 — exactly one retry, delay honours the header (~4s)") {
            runTest {
                val counter = AtomicInteger(0)
                val client =
                    HttpClient(
                        MockEngine { _ ->
                            val attempt = counter.incrementAndGet()
                            if (attempt == 1) {
                                respond(
                                    content = errorBody,
                                    status = HttpStatusCode.TooManyRequests,
                                    headers =
                                        headersOf(
                                            HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                                            HttpHeaders.RetryAfter to listOf("3"),
                                        ),
                                )
                            } else {
                                respond(
                                    content = "{}",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                                )
                            }
                        },
                    ) {
                        install(NotionRateLimit) {
                            strategy = RateLimitStrategy.CUSTOM
                            maxRetries = 3
                            // Exponential schedule kept tiny: were Retry-After ignored, the observed
                            // delay would be ~0ms, so the >= 3s assertion proves the header drove it.
                            baseDelayMs = 1
                            maxDelayMs = 5
                            jitterFactor = 0.0
                            timeSourceMillis = { currentTime }
                        }
                    }

                val start = currentTime
                val response: HttpResponse = client.get("https://api.notion.com/v1/ping")
                val elapsed = currentTime - start

                counter.get() shouldBe 2 // one 429 + one 200 → exactly one retry
                response.status shouldBe HttpStatusCode.OK
                // Retry-After: 3 + 1s rounding-safety margin = 4s. Bounds per the acceptance criteria.
                elapsed shouldBeGreaterThanOrEqual 3_000L
                elapsed shouldBeLessThanOrEqual 5_000L

                client.close()
            }
        }

        test("429 without Retry-After then 200 — falls back to the exponential schedule and retries") {
            runTest {
                val counter = AtomicInteger(0)
                val client =
                    HttpClient(
                        MockEngine { _ ->
                            val attempt = counter.incrementAndGet()
                            if (attempt == 1) {
                                respond(
                                    content = errorBody,
                                    status = HttpStatusCode.TooManyRequests,
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                                )
                            } else {
                                respond(
                                    content = "{}",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                                )
                            }
                        },
                    ) {
                        install(NotionRateLimit) {
                            strategy = RateLimitStrategy.CUSTOM
                            maxRetries = 3
                            baseDelayMs = 1
                            maxDelayMs = 5
                            jitterFactor = 0.0
                            timeSourceMillis = { currentTime }
                        }
                    }

                val response: HttpResponse = client.get("https://api.notion.com/v1/ping")

                counter.get() shouldBe 2 // exponential fallback still retries successfully
                response.status shouldBe HttpStatusCode.OK

                client.close()
            }
        }
    })
