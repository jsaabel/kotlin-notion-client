@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package unit.ratelimit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
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
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verifies the [NotionRateLimit] plugin's typed retry classifier (issue #17). Classification is
 * driven by [io.ktor.http.HttpStatusCode] and exception *types* — never by string-matching against
 * `error.message`. The plugin has the response object in scope, so it inspects the status directly.
 *
 * Retry policy under test:
 * - `5xx` — only `502 / 503 / 504` retry (exponential backoff + jitter); `500` does **not**.
 * - Network exceptions (`IOException` family) retry with the same exponential schedule.
 * - Everything else (`4xx`, `500`, …) is returned/propagated immediately with no delay.
 *
 * Timing runs under `kotlinx.coroutines.test` virtual time: the bucket clock is wired to the test
 * scheduler and `delay()` advances virtual time, so backoff waits cost no wall-clock time.
 */
@Tags("Unit")
class TypedRetryClassifierPluginTest :
    FunSpec({

        val errorBody = """{"object":"error","status":500,"code":"internal_server_error","message":"boom"}"""

        fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        test("503 -> 503 -> 200: two retries via exponential backoff, eventual success") {
            runTest {
                val counter = AtomicInteger(0)
                val client =
                    HttpClient(
                        MockEngine { _ ->
                            val attempt = counter.incrementAndGet()
                            if (attempt <= 2) {
                                respond(errorBody, HttpStatusCode.ServiceUnavailable, jsonHeaders())
                            } else {
                                respond("{}", HttpStatusCode.OK, jsonHeaders())
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

                counter.get() shouldBe 3 // two 503s + one 200 => exactly two retries
                response.status shouldBe HttpStatusCode.OK

                client.close()
            }
        }

        test("SocketTimeoutException once then 200: one retry, eventual success") {
            runTest {
                val counter = AtomicInteger(0)
                val client =
                    HttpClient(
                        MockEngine { _ ->
                            val attempt = counter.incrementAndGet()
                            if (attempt == 1) {
                                // SocketTimeoutException is an IOException — a retryable network failure.
                                throw SocketTimeoutException("simulated read timeout")
                            } else {
                                respond("{}", HttpStatusCode.OK, jsonHeaders())
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

                counter.get() shouldBe 2 // one timeout + one 200 => exactly one retry
                response.status shouldBe HttpStatusCode.OK

                client.close()
            }
        }

        test("500 response: no retry, exception propagates immediately (500 is not in the retry set)") {
            runTest {
                val counter = AtomicInteger(0)
                val client =
                    HttpClient(
                        MockEngine { _ ->
                            counter.incrementAndGet()
                            respond(errorBody, HttpStatusCode.InternalServerError, jsonHeaders())
                        },
                    ) {
                        expectSuccess = true
                        install(NotionRateLimit) {
                            strategy = RateLimitStrategy.CUSTOM
                            maxRetries = 3
                            baseDelayMs = 1
                            maxDelayMs = 5
                            jitterFactor = 0.0
                            timeSourceMillis = { currentTime }
                        }
                    }

                shouldThrow<ResponseException> {
                    client.get("https://api.notion.com/v1/ping")
                }
                counter.get() shouldBe 1 // single attempt — 500 is deliberately excluded from retries

                client.close()
            }
        }

        listOf(
            HttpStatusCode.NotFound,
            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized,
        ).forEach { status ->
            test("${status.value} response: one attempt, no delay, exception propagates") {
                runTest {
                    val counter = AtomicInteger(0)
                    val client =
                        HttpClient(
                            MockEngine { _ ->
                                counter.incrementAndGet()
                                respond(errorBody, status, jsonHeaders())
                            },
                        ) {
                            expectSuccess = true
                            install(NotionRateLimit) {
                                strategy = RateLimitStrategy.CUSTOM
                                maxRetries = 3
                                baseDelayMs = 1
                                maxDelayMs = 5
                                jitterFactor = 0.0
                                timeSourceMillis = { currentTime }
                            }
                        }

                    val start = currentTime
                    shouldThrow<ResponseException> {
                        client.get("https://api.notion.com/v1/ping")
                    }
                    val elapsed = currentTime - start

                    counter.get() shouldBe 1 // 4xx is not retryable
                    elapsed shouldBe 0L // no backoff delay was applied

                    client.close()
                }
            }
        }
    })
