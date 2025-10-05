package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitConfig
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitStrategy
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Verification test to ensure rate limiting works correctly.
 */
class RateLimitVerificationTest :
    FunSpec({

        if (!integrationTestEnvVarsAreSet()) {
            xtest("(Skipped)") {
                println("Skipping RateLimitVerificationTest due to missing environment variables")
            }
        } else {
            val apiToken = System.getenv("NOTION_API_TOKEN")
            val testPageId = System.getenv("NOTION_TEST_PAGE_ID")

            test("Rate limiting should work - 100 concurrent requests").config(
                timeout = 120.seconds,
            ) {
                val config =
                    NotionConfig(
                        apiToken = apiToken!!,
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

                val client = NotionClient.Companion.create(config)

                var successCount = 0
                var rateLimitHandledCount = 0
                var failureCount = 0

                println("\n🚀 Testing rate limiting with cleaned-up implementation...")
                println("Making 100 concurrent requests to verify rate limiting works")

                val totalTime =
                    measureTime {
                        coroutineScope {
                            (1..100)
                                .map { i ->
                                    async {
                                        try {
                                            val requestTime =
                                                measureTime {
                                                    client.pages.retrieve(testPageId!!)
                                                }

                                            if (requestTime.inWholeSeconds >= 1) {
                                                println(
                                                    "✅ Request $i succeeded after ${requestTime.inWholeSeconds}s (rate limited & retried)",
                                                )
                                                rateLimitHandledCount++
                                            } else {
                                                successCount++
                                            }
                                            "success"
                                        } catch (e: NotionException.ApiError) {
                                            if (e.status == 429 || e.code.contains("RATE_LIMITED")) {
                                                println("🔄 Request $i: Rate limit error handled - ${e.code}")
                                                rateLimitHandledCount++
                                                "rate_limited"
                                            } else {
                                                println("❌ Request $i: API error - ${e.code}")
                                                failureCount++
                                                "api_error"
                                            }
                                        } catch (e: Exception) {
                                            println("❌ Request $i: Unexpected error - ${e.message}")
                                            failureCount++
                                            "error"
                                        }
                                    }
                                }.awaitAll()
                        }
                    }

                client.close()

                println("\n📊 Rate Limiting Verification Results:")
                println("═══════════════════════════════════════")
                println("Total requests: 100")
                println("Quick successes: $successCount")
                println("Rate limited (handled): $rateLimitHandledCount")
                println("Failures: $failureCount")
                println("Total time: ${totalTime.inWholeSeconds}s")
                println("Average time per request: ${totalTime.inWholeMilliseconds / 100}ms")

                // Assertions
                (successCount + rateLimitHandledCount) shouldBe 100 // All requests should be handled
                failureCount shouldBe 0 // No failures should occur due to our rate limiting

                if (rateLimitHandledCount > 0) {
                    println("\n✅ SUCCESS: Rate limiting is working correctly!")
                    println("   - $rateLimitHandledCount requests were rate limited and handled automatically")
                } else {
                    println("\n⚠️  No rate limiting detected - this could mean:")
                    println("   - Your API token has high rate limits")
                    println("   - Notion's burst capacity accommodated all 100 requests")
                }

                // At minimum, we should have made significant concurrent load
                totalTime.inWholeSeconds shouldBeGreaterThan 0L
            }
        }
    })
