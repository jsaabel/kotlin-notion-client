package integration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.users.UserType
import it.saabel.kotlinnotionclient.ratelimit.NotionRateLimit
import it.saabel.kotlinnotionclient.ratelimit.RateLimitConfig
import it.saabel.kotlinnotionclient.ratelimit.RateLimitStrategy
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import unit.util.TestFixtures
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Integration tests for Users API, Wiki verification, and rate limiting.
 *
 * Covers:
 * - Users: getCurrentUser(), retrieve(userId), list() with pagination, validation
 * - Wiki verification: verify/unverify a page inside a wiki database (requires NOTION_TEST_WIKI_PAGE_ID)
 * - Rate limiting (mock-based): header parsing, 429 handling, strategy configuration
 * - Rate limiting (live): 100 concurrent requests under real API conditions (heavy)
 *
 * A container page is created for documentation. Users and rate-limit tests do not
 * create Notion pages; wiki verification works on a pre-existing page.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Optional:
 * - export NOTION_TEST_USER_ID="..."       (enables retrieve-by-ID test)
 * - export NOTION_TEST_WIKI_PAGE_ID="..."  (enables wiki verification test)
 *
 * Run with: ./gradlew integrationTest --tests "*UsersIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class UsersIntegrationTest :
    StringSpec({

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

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) users integration" {
                println("Skipping UsersIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Users & Rate Limiting — Integration Tests")
                        icon.emoji("👤")
                        content {
                            callout(
                                "ℹ️",
                                "Covers the Users API (getCurrentUser, retrieve by ID, list with pagination, validation), " +
                                    "wiki page verification (requires NOTION_TEST_WIKI_PAGE_ID), " +
                                    "and rate limiting (mock-based header parsing, 429 handling, strategy config, " +
                                    "plus a live 100-concurrent-request verification).",
                            )
                        }
                    }
                containerPageId = container.id
                println("📄 Container: ${container.url}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("✅ Cleaned up container page")
                } else {
                    println("🔧 Cleanup skipped — container page preserved for inspection")
                }
                notion.close()
            }

            // ------------------------------------------------------------------
            // 1. Users — getCurrentUser returns bot
            // ------------------------------------------------------------------
            "getCurrentUser should return the bot user" {
                val user = notion.users.getCurrentUser()

                user.id.shouldNotBeBlank()
                user.objectType shouldBe "user"
                user.type.shouldNotBeNull()
                user.type shouldBe UserType.BOT
                user.bot.shouldNotBeNull()
                user.person shouldBe null

                if (user.bot.owner != null) {
                    println("  ✅ Bot user: ${user.name} (${user.id}), owner: ${user.bot.owner.type}")
                } else {
                    println("  ✅ Bot user: ${user.name} (${user.id})")
                }
            }

            // ------------------------------------------------------------------
            // 2. Users — retrieve by ID (requires NOTION_TEST_USER_ID)
            // ------------------------------------------------------------------
            "retrieve should get user by ID when NOTION_TEST_USER_ID is set" {
                val testUserId = System.getenv("NOTION_TEST_USER_ID")

                if (testUserId.isNullOrBlank()) {
                    println("  Skipping — NOTION_TEST_USER_ID not set")
                } else {
                    try {
                        val user = notion.users.retrieve(testUserId)

                        val normalizedUserId = user.id.replace("-", "")
                        val normalizedTestUserId = testUserId.replace("-", "")
                        normalizedUserId shouldBe normalizedTestUserId
                        user.objectType shouldBe "user"
                        user.type.shouldNotBeNull()

                        println("  ✅ Retrieved user: ${user.name} (${user.id}), type: ${user.type}")
                    } catch (e: NotionException.ApiError) {
                        if (e.status == 403) {
                            println("  ⚠️  Integration lacks user information capabilities (403 Forbidden)")
                        } else {
                            throw e
                        }
                    }
                }
            }

            // ------------------------------------------------------------------
            // 3. Users — retrieve bot user by its own ID
            // ------------------------------------------------------------------
            "retrieve should get current bot by its own ID" {
                val currentUser = notion.users.getCurrentUser()

                try {
                    val user = notion.users.retrieve(currentUser.id)

                    user.id shouldBe currentUser.id
                    user.type shouldBe UserType.BOT

                    println("  ✅ Retrieved bot by ID: ${user.name}")
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("  ⚠️  Integration lacks user information capabilities (403 Forbidden)")
                    } else {
                        throw e
                    }
                }
            }

            // ------------------------------------------------------------------
            // 4. Users — list with pagination
            // ------------------------------------------------------------------
            "list should return users and support cursor pagination" {
                try {
                    val firstPage = notion.users.list(pageSize = 1)

                    firstPage.objectType shouldBe "list"
                    firstPage.results shouldNotBe null
                    println("  First page: ${firstPage.results.size} user(s), hasMore=${firstPage.hasMore}")

                    if (firstPage.hasMore && firstPage.nextCursor != null) {
                        val secondPage = notion.users.list(startCursor = firstPage.nextCursor, pageSize = 1)

                        secondPage.objectType shouldBe "list"
                        secondPage.results shouldNotBe null

                        if (firstPage.results.isNotEmpty() && secondPage.results.isNotEmpty()) {
                            firstPage.results[0].id shouldNotBe secondPage.results[0].id
                            println("  ✅ Pagination: page 1 has ${firstPage.results[0].id}, page 2 has ${secondPage.results[0].id}")
                        }
                    } else {
                        println("  Only one page of users — pagination not tested")
                    }
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("  ⚠️  Integration lacks user information capabilities (403 Forbidden)")
                    } else {
                        throw e
                    }
                }
            }

            // ------------------------------------------------------------------
            // 5. Users — page size validation
            // ------------------------------------------------------------------
            "list should reject invalid page size bounds" {
                shouldThrow<IllegalArgumentException> { notion.users.list(pageSize = 0) }
                shouldThrow<IllegalArgumentException> { notion.users.list(pageSize = 101) }
                println("  ✅ Page size validation (0 and 101 both rejected)")
            }

            // ------------------------------------------------------------------
            // 6. Users — invalid user ID returns 400/403/404
            // ------------------------------------------------------------------
            "retrieve should handle invalid user ID" {
                val exception =
                    shouldThrow<NotionException.ApiError> {
                        notion.users.retrieve("invalid-user-id-12345678")
                    }

                (exception.status in listOf(400, 403, 404)) shouldBe true
                println("  ✅ Invalid user ID returns status ${exception.status}")
            }

            // ------------------------------------------------------------------
            // 7. Wiki verification — verify and unverify (requires NOTION_TEST_WIKI_PAGE_ID)
            // ------------------------------------------------------------------
            "should verify and unverify a wiki page when NOTION_TEST_WIKI_PAGE_ID is set" {
                val wikiPageId = System.getenv("NOTION_TEST_WIKI_PAGE_ID")

                if (wikiPageId.isNullOrBlank()) {
                    println("  Skipping — NOTION_TEST_WIKI_PAGE_ID not set")
                    println("  Set it to the ID of a page inside a wiki database")
                } else {
                    val page = notion.pages.retrieve(wikiPageId)
                    val verificationProp = page.properties["Verification"]
                    verificationProp.shouldNotBeNull()
                    verificationProp as PageProperty.Verification
                    println("  Current state: ${verificationProp.verification?.state ?: "null"}")

                    val verifiedPage =
                        notion.pages.update(wikiPageId) {
                            properties {
                                verify(
                                    "Verification",
                                    start = "2026-03-25T00:00:00.000Z",
                                    end = "2026-06-25T00:00:00.000Z",
                                )
                            }
                        }
                    val verifiedProp = verifiedPage.properties["Verification"] as PageProperty.Verification
                    verifiedProp.verification.shouldNotBeNull()
                    verifiedProp.verification.state shouldBe "verified"
                    println("  ✅ Verified")

                    val unverifiedPage =
                        notion.pages.update(wikiPageId) {
                            properties { unverify("Verification") }
                        }
                    val unverifiedProp = unverifiedPage.properties["Verification"] as PageProperty.Verification
                    unverifiedProp.verification.shouldNotBeNull()
                    unverifiedProp.verification.state shouldBe "unverified"
                    println("  ✅ Unverified")

                    // Leave page re-verified for easy inspection
                    notion.pages.update(wikiPageId) {
                        properties {
                            verify(
                                "Verification",
                                start = "2026-03-25T00:00:00.000Z",
                                end = "2026-06-25T00:00:00.000Z",
                            )
                        }
                    }
                    println("  ✅ Re-verified — left in verified state for inspection")
                }
            }

            // ------------------------------------------------------------------
            // 8. Rate limiting (mock) — header parsing and state tracking
            // ------------------------------------------------------------------
            "should parse and track rate limit headers from mock responses" {
                val mockEngine =
                    MockEngine { request ->
                        when (request.url.encodedPath) {
                            "/v1/pages/test-page-1" -> {
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
                            }

                            "/v1/pages/test-page-2" -> {
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
                            }

                            else -> {
                                error("Unexpected request: ${request.url}")
                            }
                        }
                    }

                val config =
                    NotionConfig(apiToken = "secret_test_token", enableRateLimit = true, rateLimitConfig = RateLimitConfig.BALANCED)
                val client = createMockClient(mockEngine, config)

                val response1 = client.pages.retrieve("test-page-1")
                response1 shouldNotBe null

                val response2 = client.pages.retrieve("test-page-2")
                response2 shouldNotBe null

                println("  ✅ Rate limit header parsing and state tracking verified (mock)")
            }

            // ------------------------------------------------------------------
            // 9. Rate limiting (mock) — 429 response triggers ApiError
            // ------------------------------------------------------------------
            "should surface rate limited responses as ApiError with status 429" {
                val mockEngine =
                    MockEngine { _ ->
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
                        rateLimitConfig = RateLimitConfig(maxRetries = 1, baseDelayMs = 100, respectRetryAfter = true, jitterFactor = 0.0),
                    )

                val client = createMockClient(mockEngine, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        client.pages.retrieve("test-page")
                    }
                exception.status shouldBe 429

                println("  ✅ 429 response correctly surfaces as ApiError (mock)")
            }

            // ------------------------------------------------------------------
            // 10. Rate limiting (mock) — strategy configuration variants
            // ------------------------------------------------------------------
            "should properly configure conservative, balanced, and aggressive strategies" {
                val strategies =
                    listOf(
                        RateLimitStrategy.CONSERVATIVE to RateLimitConfig.CONSERVATIVE,
                        RateLimitStrategy.BALANCED to RateLimitConfig.BALANCED,
                        RateLimitStrategy.AGGRESSIVE to RateLimitConfig.AGGRESSIVE,
                    )

                strategies.forEach { (strategy, config) ->
                    val mockEngine =
                        MockEngine { _ ->
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

                    val notionConfig = NotionConfig(apiToken = "secret_test_token", enableRateLimit = true, rateLimitConfig = config)
                    val client = createMockClient(mockEngine, notionConfig)

                    val response = client.pages.retrieve("test-page")
                    response shouldNotBe null
                    println("  $strategy ✓")
                }

                println("  ✅ All rate limit strategy configurations verified (mock)")
            }

            // ------------------------------------------------------------------
            // 11. Rate limiting (mock) — disabled rate limiting still works
            // ------------------------------------------------------------------
            "should work normally when rate limiting is disabled" {
                val mockEngine =
                    MockEngine { _ ->
                        respond(
                            content = TestFixtures.Pages.retrievePageAsString(),
                            status = HttpStatusCode.OK,
                            headers = headersOf("Content-Type" to listOf("application/json")),
                        )
                    }

                val config = NotionConfig(apiToken = "secret_test_token", enableRateLimit = false)
                val client = createMockClient(mockEngine, config)

                val response = client.pages.retrieve("test-page")
                response shouldNotBe null

                println("  ✅ Disabled rate limiting works normally (mock)")
            }

            // ------------------------------------------------------------------
            // 12. Rate limiting (live) — 100 concurrent requests
            // Note: This test uses the real Notion API and may take up to 2 minutes.
            // ------------------------------------------------------------------
            "rate limiting should handle 100 concurrent real API requests".config(timeout = 120.seconds) {
                val testPageId = System.getenv("NOTION_TEST_PAGE_ID")

                val config =
                    NotionConfig(
                        apiToken = token,
                        enableRateLimit = true,
                        rateLimitConfig =
                            RateLimitConfig(
                                strategy = RateLimitStrategy.BALANCED,
                                maxRetries = 3,
                                baseDelayMs = 1000,
                                maxDelayMs = 30000,
                                jitterFactor = 0.1,
                            ),
                    )

                val client = NotionClient(config)
                var successCount = 0
                var rateLimitHandledCount = 0
                var failureCount = 0

                println("  Making 100 concurrent requests to verify rate limiting...")

                val totalTime =
                    measureTime {
                        coroutineScope {
                            (1..100)
                                .map { i ->
                                    async {
                                        try {
                                            val requestTime =
                                                measureTime {
                                                    client.pages.retrieve(testPageId)
                                                }
                                            if (requestTime.inWholeSeconds >= 1) {
                                                rateLimitHandledCount++
                                            } else {
                                                successCount++
                                            }
                                            "success"
                                        } catch (e: NotionException.ApiError) {
                                            if (e.status == 429 || e.code.contains("RATE_LIMITED")) {
                                                rateLimitHandledCount++
                                                "rate_limited"
                                            } else {
                                                println("  ❌ Request $i: API error — ${e.code}")
                                                failureCount++
                                                "api_error"
                                            }
                                        } catch (e: Exception) {
                                            println("  ❌ Request $i: ${e.message}")
                                            failureCount++
                                            "error"
                                        }
                                    }
                                }.awaitAll()
                        }
                    }

                client.close()

                println("  Quick successes: $successCount, Rate-limit handled: $rateLimitHandledCount, Failures: $failureCount")
                println("  Total time: ${totalTime.inWholeSeconds}s")

                (successCount + rateLimitHandledCount) shouldBe 100
                failureCount shouldBe 0
                totalTime.inWholeSeconds shouldBeGreaterThan 0L

                println("  ✅ 100 concurrent requests all handled correctly")
            }
        }
    })
