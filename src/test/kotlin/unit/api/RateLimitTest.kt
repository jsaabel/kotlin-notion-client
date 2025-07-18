@file:Suppress("unused")

package unit.api

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.saabelit.kotlinnotionclient.ratelimit.BackoffCalculator
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitConfig
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitDecision
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitState
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitStrategy
import no.saabelit.kotlinnotionclient.ratelimit.RetryAttempt
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for rate limiting functionality.
 *
 * These tests verify the core rate limiting logic including:
 * - Rate limit state parsing from headers
 * - Backoff calculation algorithms
 * - Retry decision logic
 * - Configuration validation
 */
@Tags("Unit")
class RateLimitTest :
    FunSpec({

        context("RateLimitState") {
            test("should parse rate limit headers correctly") {
                val headers =
                    mapOf(
                        "x-ratelimit-limit" to "3",
                        "x-ratelimit-remaining" to "2",
                        "x-ratelimit-reset" to "${System.currentTimeMillis() / 1000 + 60}",
                        "retry-after" to "5",
                    )

                val state = RateLimitState.fromHeaders(headers)

                state shouldNotBe null
                state!!.limit shouldBe 3
                state.remaining shouldBe 2
                state.retryAfterSeconds shouldBe 5
                state.isRateLimited shouldBe false
                state.isApproachingLimit shouldBe false // 2/3 = 67% remaining, which is NOT approaching limit (< 20%)
            }

            test("should handle missing headers gracefully") {
                val headers = mapOf<String, String>()
                val state = RateLimitState.fromHeaders(headers)
                state shouldBe null
            }

            test("should detect rate limited state") {
                val headers =
                    mapOf(
                        "x-ratelimit-limit" to "3",
                        "x-ratelimit-remaining" to "0",
                        "x-ratelimit-reset" to "${System.currentTimeMillis() / 1000 + 60}",
                    )

                val state = RateLimitState.fromHeaders(headers)!!
                state.isRateLimited shouldBe true
                state.isApproachingLimit shouldBe true
            }

            test("should detect approaching limit state") {
                val headers =
                    mapOf(
                        "x-ratelimit-limit" to "10",
                        "x-ratelimit-remaining" to "1", // 1/10 = 10% remaining, which IS approaching limit (< 20%)
                        "x-ratelimit-reset" to "${System.currentTimeMillis() / 1000 + 60}",
                    )

                val state = RateLimitState.fromHeaders(headers)!!
                state.isRateLimited shouldBe false // Still has 1 request remaining
                state.isApproachingLimit shouldBe true // But approaching limit (< 20% remaining)
            }

            test("should calculate suggested delay correctly") {
                // Test with retry-after header
                val headersWithRetryAfter =
                    mapOf(
                        "x-ratelimit-limit" to "3",
                        "x-ratelimit-remaining" to "0",
                        "x-ratelimit-reset" to "${System.currentTimeMillis() / 1000 + 60}",
                        "retry-after" to "10",
                    )

                val stateWithRetryAfter = RateLimitState.fromHeaders(headersWithRetryAfter)!!
                stateWithRetryAfter.suggestedDelay shouldBe 10.seconds
            }
        }

        context("BackoffCalculator") {
            test("should calculate exponential backoff correctly") {
                val config =
                    RateLimitConfig(
                        maxRetries = 3,
                        baseDelayMs = 1000,
                        maxDelayMs = 30000,
                        jitterFactor = 0.0, // No jitter for predictable testing
                        strategy = RateLimitStrategy.BALANCED,
                    )

                val calculator = BackoffCalculator(config)

                // First retry (attempt 1)
                val attempt1 = RetryAttempt.initial().nextAttempt(RuntimeException("test"), 0.seconds)
                val delay1 = calculator.calculateDelay(attempt1)
                delay1.inWholeMilliseconds shouldBe 2000 // baseDelay * 2^1

                // Second retry (attempt 2)
                val attempt2 = attempt1.nextAttempt(RuntimeException("test"), delay1)
                val delay2 = calculator.calculateDelay(attempt2)
                delay2.inWholeMilliseconds shouldBe 4000 // baseDelay * 2^2
            }

            test("should respect retry-after headers") {
                val config = RateLimitConfig(respectRetryAfter = true, jitterFactor = 0.0) // No jitter for predictable testing
                val calculator = BackoffCalculator(config)

                val rateLimitState =
                    RateLimitState(
                        limit = 3,
                        remaining = 0,
                        resetTimeUnix = System.currentTimeMillis() / 1000 + 60,
                        retryAfterSeconds = 15,
                    )

                val attempt = RetryAttempt.initial()
                val delay = calculator.calculateDelay(attempt, rateLimitState)

                // Should use retry-after value exactly (no jitter in this test)
                delay.inWholeSeconds shouldBe 15L
            }

            test("should apply strategy modifiers") {
                val conservativeConfig =
                    RateLimitConfig(
                        baseDelayMs = 1000,
                        jitterFactor = 0.0,
                        strategy = RateLimitStrategy.CONSERVATIVE,
                    )

                val aggressiveConfig =
                    RateLimitConfig(
                        baseDelayMs = 1000,
                        jitterFactor = 0.0,
                        strategy = RateLimitStrategy.AGGRESSIVE,
                    )

                val conservativeCalculator = BackoffCalculator(conservativeConfig)
                val aggressiveCalculator = BackoffCalculator(aggressiveConfig)

                val attempt = RetryAttempt.initial().nextAttempt(RuntimeException("test"), 0.seconds)

                val conservativeDelay = conservativeCalculator.calculateDelay(attempt)
                val aggressiveDelay = aggressiveCalculator.calculateDelay(attempt)

                // Conservative should be longer than aggressive
                conservativeDelay.inWholeMilliseconds shouldBe (aggressiveDelay.inWholeMilliseconds * 1.5 / 0.7).toLong()
            }
        }

        context("Retry logic") {
            test("should allow retries for rate limit errors") {
                val config = RateLimitConfig(maxRetries = 3)
                val calculator = BackoffCalculator(config)

                val rateLimitError = RuntimeException("429: Too Many Requests")
                val attempt = RetryAttempt.initial()

                val decision = calculator.shouldRetry(attempt, rateLimitError, null)
                decision.shouldBeInstanceOf<RateLimitDecision.Wait>()
            }

            test("should reject after max retries") {
                val config = RateLimitConfig(maxRetries = 2)
                val calculator = BackoffCalculator(config)

                val rateLimitError = RuntimeException("429: Too Many Requests")
                var attempt = RetryAttempt.initial()

                // First retry should be allowed
                attempt = attempt.nextAttempt(rateLimitError, 1.seconds)
                val decision1 = calculator.shouldRetry(attempt, rateLimitError, null)
                decision1.shouldBeInstanceOf<RateLimitDecision.Wait>()

                // Second retry should be allowed
                attempt = attempt.nextAttempt(rateLimitError, 1.seconds)
                val decision2 = calculator.shouldRetry(attempt, rateLimitError, null)
                decision2.shouldBeInstanceOf<RateLimitDecision.Wait>()

                // Third retry should be rejected (exceeded maxRetries)
                attempt = attempt.nextAttempt(rateLimitError, 1.seconds)
                val decision3 = calculator.shouldRetry(attempt, rateLimitError, null)
                decision3.shouldBeInstanceOf<RateLimitDecision.Reject>()
            }

            test("should not retry non-retryable errors") {
                val config = RateLimitConfig(maxRetries = 3)
                val calculator = BackoffCalculator(config)

                val clientError = RuntimeException("400: Bad Request")
                val attempt = RetryAttempt.initial()

                val decision = calculator.shouldRetry(attempt, clientError, null)
                decision.shouldBeInstanceOf<RateLimitDecision.Reject>()
            }
        }

        context("Configuration validation") {
            test("should validate positive values") {
                val invalidConfigs =
                    listOf(
                        { RateLimitConfig(maxRetries = -1) },
                        { RateLimitConfig(baseDelayMs = 0) },
                        { RateLimitConfig(maxDelayMs = 500, baseDelayMs = 1000) },
                        { RateLimitConfig(jitterFactor = -0.1) },
                        { RateLimitConfig(jitterFactor = 1.1) },
                    )

                invalidConfigs.forEach { configFactory ->
                    var threwException = false
                    try {
                        configFactory()
                    } catch (e: IllegalArgumentException) {
                        threwException = true
                    }
                    threwException shouldBe true
                }
            }

            test("should accept valid configurations") {
                // These should not throw
                RateLimitConfig()
                RateLimitConfig.CONSERVATIVE
                RateLimitConfig.AGGRESSIVE
                RateLimitConfig.BALANCED
            }
        }
    })
