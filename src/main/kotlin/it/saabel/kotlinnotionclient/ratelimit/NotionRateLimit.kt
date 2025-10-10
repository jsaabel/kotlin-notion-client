package it.saabel.kotlinnotionclient.ratelimit

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.util.AttributeKey
import it.saabel.kotlinnotionclient.exceptions.NotionException
import kotlinx.coroutines.delay

/**
 * Ktor plugin for handling Notion API rate limits automatically.
 *
 * This plugin provides automatic rate limit handling with:
 * - Exponential backoff retry logic
 * - Rate limit state tracking from response headers
 * - Configurable retry strategies
 *
 * Usage:
 * ```kotlin
 * val client = HttpClient {
 *     install(NotionRateLimit) {
 *         strategy = RateLimitStrategy.BALANCED
 *         maxRetries = 3
 *     }
 * }
 * ```
 */
class NotionRateLimit private constructor(
    private val config: RateLimitConfig,
) {
    /**
     * Backoff calculator for retry logic.
     */
    private val backoffCalculator = BackoffCalculator(config)

    class Config {
        /**
         * Rate limiting strategy to use.
         */
        var strategy: RateLimitStrategy = RateLimitStrategy.BALANCED

        /**
         * Maximum number of retry attempts.
         */
        var maxRetries: Int = 3

        /**
         * Base delay for exponential backoff in milliseconds.
         */
        var baseDelayMs: Long = 1000

        /**
         * Maximum delay between retries in milliseconds.
         */
        var maxDelayMs: Long = 30000

        /**
         * Jitter factor (0.0 to 1.0) to add randomness to delays.
         */
        var jitterFactor: Double = 0.1

        /**
         * Whether to respect Retry-After headers from 429 responses.
         */
        var respectRetryAfter: Boolean = true

        /**
         * Converts this config to a RateLimitConfig.
         */
        internal fun toRateLimitConfig(): RateLimitConfig =
            RateLimitConfig(
                maxRetries = maxRetries,
                baseDelayMs = baseDelayMs,
                maxDelayMs = maxDelayMs,
                jitterFactor = jitterFactor,
                strategy = strategy,
                respectRetryAfter = respectRetryAfter,
            )
    }

    companion object : HttpClientPlugin<Config, NotionRateLimit> {
        override val key = AttributeKey<NotionRateLimit>("NotionRateLimit")

        override fun prepare(block: Config.() -> Unit): NotionRateLimit {
            val config = Config().apply(block)
            return NotionRateLimit(config.toRateLimitConfig())
        }

        override fun install(
            plugin: NotionRateLimit,
            scope: HttpClient,
        ) {
            // Store the plugin instance on the client for access by API wrapper functions
            scope.attributes.put(key, plugin)
        }
    }

    /**
     * Executes a request with automatic retry logic for rate limiting.
     * This is the main entry point for rate-limited API calls.
     */
    suspend fun <T> executeWithRetry(request: suspend () -> T): T {
        var attemptNumber = 0
        var lastError: Throwable? = null
        var cumulativeDelay = kotlin.time.Duration.ZERO

        while (attemptNumber <= config.maxRetries) {
            try {
                return request()
            } catch (e: NotionException.ApiError) {
                // Check if this is a rate limit error that should be retried
                if (e.status == 429) {
                    val retryAttempt =
                        RetryAttempt(
                            attemptNumber = attemptNumber,
                            lastError = lastError,
                            cumulativeDelay = cumulativeDelay,
                        )

                    val decision = backoffCalculator.shouldRetry(retryAttempt, e, null)

                    when (decision) {
                        is RateLimitDecision.Proceed -> {
                            // This shouldn't happen for 429 errors, but just in case
                            throw e
                        }
                        is RateLimitDecision.Wait -> {
                            // Wait and retry
                            delay(decision.delay)
                            cumulativeDelay += decision.delay
                            attemptNumber++
                            lastError = e
                            // Continue to next iteration
                        }
                        is RateLimitDecision.Reject -> {
                            // Max retries exceeded
                            throw NotionException.ApiError(
                                code = "RATE_LIMITED_MAX_RETRIES",
                                status = 429,
                                details = decision.reason,
                            )
                        }
                    }
                } else {
                    // Non-rate-limit API error, don't retry
                    throw e
                }
            } catch (e: Exception) {
                // Non-API exceptions, don't retry
                throw e
            }
        }

        // Should not reach here, but just in case
        throw NotionException.ApiError(
            code = "MAX_RETRIES_EXCEEDED",
            status = 429,
            details = "Maximum retries (${config.maxRetries}) exceeded",
        )
    }
}

/**
 * Extension function to access the NotionRateLimit plugin from an HttpClient.
 */
fun HttpClient.getRateLimiter(): NotionRateLimit? = attributes.getOrNull(NotionRateLimit.key)

/**
 * Extension function to execute a request with rate limiting if the plugin is installed.
 */
suspend fun <T> HttpClient.executeWithRateLimit(request: suspend () -> T): T {
    val rateLimiter = getRateLimiter()
    return if (rateLimiter != null) {
        rateLimiter.executeWithRetry(request)
    } else {
        // No rate limiting configured, execute directly
        request()
    }
}
