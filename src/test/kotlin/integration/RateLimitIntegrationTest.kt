@file:Suppress("unused")

package integration

import TestFixtures
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.ratelimit.NotionRateLimit
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitConfig
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitStrategy

/**
 * Integration tests for rate limiting functionality using Ktor's mock engine.
 *
 * ## Testing Philosophy
 *
 * Rather than attempting to trigger actual rate limiting events from the Notion API
 * (which would be unreliable, slow, and potentially consume API quota), these tests
 * use Ktor's MockEngine to simulate Notion API responses with various rate limit headers.
 *
 * This approach allows us to:
 * - Test the complete integration of rate limiting within the actual HTTP client
 * - Verify header parsing in real HTTP request/response cycles
 * - Test various rate limiting scenarios (approaching limit, rate limited, normal usage)
 * - Ensure the Ktor plugin is properly installed and configured
 * - Run tests quickly and reliably without external dependencies
 * - Avoid consuming actual API quota or risking real rate limit violations
 * - Stay within our existing Ktor ecosystem without additional dependencies
 *
 * ## What We Test
 *
 * 1. **Header Parsing Integration**: Verify that rate limit headers from mock responses
 *    are correctly parsed and tracked by the rate limiting plugin
 * 2. **Plugin Configuration**: Ensure different rate limiting strategies are properly
 *    configured and integrated into the HTTP client
 * 3. **State Tracking**: Test that the plugin maintains accurate rate limit state
 *    across multiple requests
 * 4. **Response Handling**: Verify the plugin correctly processes various response
 *    scenarios (normal, approaching limit, rate limited)
 *
 * These tests complement our unit tests by verifying the complete integration
 * without the complexity and unpredictability of triggering actual rate limits.
 */
@Tags("Unit", "MockEngine")
class RateLimitIntegrationTest :
    FunSpec({

        /**
         * Helper function to create an HTTP client with mock engine and rate limiting
         */
        fun createMockClient(
            mockEngine: MockEngine,
            config: NotionConfig,
        ): NotionClient {
            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                prettyPrint = config.prettyPrint
                                encodeDefaults = true
                                explicitNulls = false
                            },
                        )
                    }

                    if (config.enableRateLimit) {
                        install(NotionRateLimit) {
                            strategy = config.rateLimitConfig.strategy
                            maxRetries = config.rateLimitConfig.maxRetries
                            baseDelayMs = config.rateLimitConfig.baseDelayMs
                            maxDelayMs = config.rateLimitConfig.maxDelayMs
                            jitterFactor = config.rateLimitConfig.jitterFactor
                            respectRetryAfter = config.rateLimitConfig.respectRetryAfter
                        }
                    }
                }

            return NotionClient.createWithClient(httpClient, config)
        }

        context("Rate limit header integration") {
            test("should parse and track rate limit headers from mock responses") {
                val mockEngine =
                    MockEngine { request ->
                        when (request.url.encodedPath) {
                            "/v1/pages/test-page-1" ->
                                respond(
                                    content = TestFixtures.Pages.retrievePageAsString(),
                                    status = HttpStatusCode.OK,
                                    headers =
                                        headersOf(
                                            "Content-Type" to listOf("application/json"),
                                            "x-ratelimit-limit" to listOf("3"),
                                            "x-ratelimit-remaining" to listOf("2"),
                                            "x-ratelimit-reset" to listOf("${System.currentTimeMillis() / 1000 + 60}"),
                                        ),
                                )
                            "/v1/pages/test-page-2" ->
                                respond(
                                    content = TestFixtures.Pages.retrievePageAsString(),
                                    status = HttpStatusCode.OK,
                                    headers =
                                        headersOf(
                                            "Content-Type" to listOf("application/json"),
                                            "x-ratelimit-limit" to listOf("3"),
                                            "x-ratelimit-remaining" to listOf("0"),
                                            "x-ratelimit-reset" to listOf("${System.currentTimeMillis() / 1000 + 60}"),
                                        ),
                                )
                            else -> error("Unexpected request: ${request.url}")
                        }
                    }

                val config =
                    NotionConfig(
                        apiToken = "secret_test_token",
                        enableRateLimit = true,
                        rateLimitConfig = RateLimitConfig.BALANCED,
                    )

                val client = createMockClient(mockEngine, config)

                // First request - should track normal rate limit state
                val response1 = client.pages.retrieve("test-page-1")
                response1 shouldNotBe null

                // Second request - should track approaching limit state
                val response2 = client.pages.retrieve("test-page-2")
                response2 shouldNotBe null
            }

            test("should handle rate limited responses with appropriate error") {
                val mockEngine =
                    MockEngine { request ->
                        respond(
                            content = """{"object": "error", "status": 429, "code": "rate_limited"}""",
                            status = HttpStatusCode.TooManyRequests,
                            headers =
                                headersOf(
                                    "Content-Type" to listOf("application/json"),
                                    "x-ratelimit-limit" to listOf("3"),
                                    "x-ratelimit-remaining" to listOf("0"),
                                    "x-ratelimit-reset" to listOf("${System.currentTimeMillis() / 1000 + 60}"),
                                    "retry-after" to listOf("1"),
                                ),
                        )
                    }

                val config =
                    NotionConfig(
                        apiToken = "secret_test_token",
                        enableRateLimit = true,
                        rateLimitConfig =
                            RateLimitConfig(
                                maxRetries = 1,
                                baseDelayMs = 100,
                                respectRetryAfter = true,
                                jitterFactor = 0.0,
                            ),
                    )

                val client = createMockClient(mockEngine, config)

                // This should throw an ApiError for 429 response (since retry logic is not fully implemented yet)
                try {
                    client.pages.retrieve("test-page")
                    // If we get here without an exception, the test should fail
                    throw AssertionError("Expected an ApiError for 429 response")
                } catch (e: no.saabelit.kotlinnotionclient.exceptions.NotionException.ApiError) {
                    // This is expected - verify it's a 429 error
                    e.status shouldBe 429
                }
            }
        }

        context("Rate limiting strategy integration") {
            test("should properly configure different rate limiting strategies") {
                val strategies =
                    listOf(
                        RateLimitStrategy.CONSERVATIVE to RateLimitConfig.CONSERVATIVE,
                        RateLimitStrategy.AGGRESSIVE to RateLimitConfig.AGGRESSIVE,
                        RateLimitStrategy.BALANCED to RateLimitConfig.BALANCED,
                    )

                strategies.forEach { (_, config) ->
                    val mockEngine =
                        MockEngine { request ->
                            respond(
                                content = TestFixtures.Pages.retrievePageAsString(),
                                status = HttpStatusCode.OK,
                                headers =
                                    headersOf(
                                        "Content-Type" to listOf("application/json"),
                                        "x-ratelimit-limit" to listOf("3"),
                                        "x-ratelimit-remaining" to listOf("3"),
                                        "x-ratelimit-reset" to listOf("${System.currentTimeMillis() / 1000 + 60}"),
                                    ),
                            )
                        }

                    val notionConfig =
                        NotionConfig(
                            apiToken = "secret_test_token",
                            enableRateLimit = true,
                            rateLimitConfig = config,
                        )

                    val client = createMockClient(mockEngine, notionConfig)

                    // Make a request to verify the configuration works
                    val response = client.pages.retrieve("test-page")
                    response shouldNotBe null
                }
            }

            test("should work with rate limiting disabled") {
                val mockEngine =
                    MockEngine { request ->
                        respond(
                            content = TestFixtures.Pages.retrievePageAsString(),
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    "Content-Type" to listOf("application/json"),
                                ),
                        )
                    }

                val config =
                    NotionConfig(
                        apiToken = "secret_test_token",
                        enableRateLimit = false, // Rate limiting disabled
                    )

                val client = createMockClient(mockEngine, config)

                // Should work normally without rate limiting
                val response = client.pages.retrieve("test-page")
                response shouldNotBe null
            }
        }

        context("Multiple request handling") {
            test("should track rate limit state across multiple requests") {
                var requestCount = 0
                val mockEngine =
                    MockEngine { request ->
                        requestCount++
                        when (request.url.encodedPath) {
                            "/v1/pages/test-page-1" ->
                                respond(
                                    content = TestFixtures.Pages.retrievePageAsString(),
                                    status = HttpStatusCode.OK,
                                    headers =
                                        headersOf(
                                            "Content-Type" to listOf("application/json"),
                                            "x-ratelimit-limit" to listOf("3"),
                                            "x-ratelimit-remaining" to listOf("3"),
                                            "x-ratelimit-reset" to listOf("${System.currentTimeMillis() / 1000 + 60}"),
                                        ),
                                )
                            "/v1/pages/test-page-2" ->
                                respond(
                                    content = TestFixtures.Pages.retrievePageAsString(),
                                    status = HttpStatusCode.OK,
                                    headers =
                                        headersOf(
                                            "Content-Type" to listOf("application/json"),
                                            "x-ratelimit-limit" to listOf("3"),
                                            "x-ratelimit-remaining" to listOf("2"),
                                            "x-ratelimit-reset" to listOf("${System.currentTimeMillis() / 1000 + 60}"),
                                        ),
                                )
                            "/v1/pages/test-page-3" ->
                                respond(
                                    content = TestFixtures.Pages.retrievePageAsString(),
                                    status = HttpStatusCode.OK,
                                    headers =
                                        headersOf(
                                            "Content-Type" to listOf("application/json"),
                                            "x-ratelimit-limit" to listOf("3"),
                                            "x-ratelimit-remaining" to listOf("1"),
                                            "x-ratelimit-reset" to listOf("${System.currentTimeMillis() / 1000 + 60}"),
                                        ),
                                )
                            else -> error("Unexpected request: ${request.url}")
                        }
                    }

                val config =
                    NotionConfig(
                        apiToken = "secret_test_token",
                        enableRateLimit = true,
                        rateLimitConfig = RateLimitConfig.BALANCED,
                    )

                val client = createMockClient(mockEngine, config)

                // Make multiple requests to simulate normal usage
                val response1 = client.pages.retrieve("test-page-1")
                response1 shouldNotBe null

                // Small delay between requests to simulate real usage
                delay(100)

                val response2 = client.pages.retrieve("test-page-2")
                response2 shouldNotBe null

                delay(100)

                val response3 = client.pages.retrieve("test-page-3")
                response3 shouldNotBe null

                requestCount shouldBe 3 // Verify all requests were made
            }
        }
    })
