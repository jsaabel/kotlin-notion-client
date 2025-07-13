package no.saabelit.kotlinnotionclient.ratelimit

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Represents the current rate limit state from Notion API headers.
 *
 * Notion provides rate limit information in response headers:
 * - x-ratelimit-limit: The maximum number of requests per window
 * - x-ratelimit-remaining: Number of requests remaining in current window
 * - x-ratelimit-reset: Unix timestamp when the rate limit window resets
 * - retry-after: Seconds to wait before retrying (only on 429 responses)
 */
@Serializable
data class RateLimitState(
    val limit: Int,
    val remaining: Int,
    val resetTimeUnix: Long,
    val retryAfterSeconds: Int? = null,
) {
    /**
     * Time until the rate limit window resets.
     */
    val timeUntilReset: Duration
        get() {
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            val secondsUntilReset = (resetTimeUnix - currentTimeSeconds).coerceAtLeast(0)
            return secondsUntilReset.seconds
        }

    /**
     * Whether we're currently rate limited (no remaining requests).
     */
    val isRateLimited: Boolean
        get() = remaining <= 0

    /**
     * Whether we're approaching the rate limit (less than 20% remaining).
     */
    val isApproachingLimit: Boolean
        get() = remaining.toDouble() / limit < 0.2

    /**
     * Suggested delay before next request based on current state.
     */
    val suggestedDelay: Duration
        get() =
            when {
                retryAfterSeconds != null -> retryAfterSeconds.seconds
                isRateLimited -> timeUntilReset
                isApproachingLimit -> (timeUntilReset.inWholeMilliseconds / remaining).milliseconds
                else -> Duration.ZERO
            }

    companion object {
        /**
         * Creates a RateLimitState from HTTP response headers.
         *
         * @param headers Map of header names to values
         * @return RateLimitState if headers are present, null otherwise
         */
        fun fromHeaders(headers: Map<String, String>): RateLimitState? {
            val limit = headers["x-ratelimit-limit"]?.toIntOrNull() ?: return null
            val remaining = headers["x-ratelimit-remaining"]?.toIntOrNull() ?: return null
            val reset = headers["x-ratelimit-reset"]?.toLongOrNull() ?: return null
            val retryAfter = headers["retry-after"]?.toIntOrNull()

            return RateLimitState(
                limit = limit,
                remaining = remaining,
                resetTimeUnix = reset,
                retryAfterSeconds = retryAfter,
            )
        }

        /**
         * Creates a default state for when no rate limit headers are present.
         * Assumes we have capacity and no immediate restrictions.
         */
        fun default(): RateLimitState =
            RateLimitState(
                limit = 3, // Notion's default: 3 requests per second
                remaining = 3,
                resetTimeUnix = (System.currentTimeMillis() / 1000) + 1,
                retryAfterSeconds = null,
            )
    }
}

/**
 * Strategy for handling rate limits.
 */
enum class RateLimitStrategy {
    /**
     * Conservative approach: Wait longer between requests, fewer retries.
     * Best for background tasks where latency is not critical.
     */
    CONSERVATIVE,

    /**
     * Aggressive approach: Retry quickly with shorter delays.
     * Best for interactive applications where responsiveness matters.
     */
    AGGRESSIVE,

    /**
     * Balanced approach: Middle ground between conservative and aggressive.
     * Good default for most applications.
     */
    BALANCED,

    /**
     * Custom strategy: User provides their own retry parameters.
     */
    CUSTOM,
}

/**
 * Configuration for rate limiting behavior.
 */
@Serializable
data class RateLimitConfig(
    /**
     * Maximum number of retry attempts for rate-limited requests.
     */
    val maxRetries: Int = 3,
    /**
     * Base delay in milliseconds for exponential backoff.
     */
    val baseDelayMs: Long = 1000,
    /**
     * Maximum delay in milliseconds between retries.
     */
    val maxDelayMs: Long = 30000,
    /**
     * Jitter factor (0.0 to 1.0) to add randomness to delays.
     * Helps avoid thundering herd problems.
     */
    val jitterFactor: Double = 0.1,
    /**
     * Rate limiting strategy to use.
     */
    val strategy: RateLimitStrategy = RateLimitStrategy.BALANCED,
    /**
     * Whether to respect retry-after headers from 429 responses.
     */
    val respectRetryAfter: Boolean = true,
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(baseDelayMs > 0) { "baseDelayMs must be positive" }
        require(maxDelayMs >= baseDelayMs) { "maxDelayMs must be >= baseDelayMs" }
        require(jitterFactor in 0.0..1.0) { "jitterFactor must be between 0.0 and 1.0" }
    }

    companion object {
        /**
         * Conservative configuration: Longer delays, fewer retries.
         */
        val CONSERVATIVE =
            RateLimitConfig(
                maxRetries = 2,
                baseDelayMs = 2000,
                maxDelayMs = 60000,
                jitterFactor = 0.2,
                strategy = RateLimitStrategy.CONSERVATIVE,
            )

        /**
         * Aggressive configuration: Shorter delays, more retries.
         */
        val AGGRESSIVE =
            RateLimitConfig(
                maxRetries = 5,
                baseDelayMs = 500,
                maxDelayMs = 15000,
                jitterFactor = 0.05,
                strategy = RateLimitStrategy.AGGRESSIVE,
            )

        /**
         * Balanced configuration: Good default for most use cases.
         */
        val BALANCED =
            RateLimitConfig(
                maxRetries = 3,
                baseDelayMs = 1000,
                maxDelayMs = 30000,
                jitterFactor = 0.1,
                strategy = RateLimitStrategy.BALANCED,
            )
    }
}

/**
 * Result of a rate limit check.
 */
sealed class RateLimitDecision {
    /**
     * Request should proceed immediately.
     */
    data object Proceed : RateLimitDecision()

    /**
     * Request should wait for the specified duration before proceeding.
     *
     * @param delay Duration to wait before retrying
     * @param reason Human-readable reason for the delay
     */
    data class Wait(
        val delay: Duration,
        val reason: String,
    ) : RateLimitDecision()

    /**
     * Request should be rejected (e.g., too many retries, queue full).
     *
     * @param reason Human-readable reason for rejection
     */
    data class Reject(
        val reason: String,
    ) : RateLimitDecision()
}

/**
 * Tracks retry attempts for a specific request.
 */
data class RetryAttempt(
    val attemptNumber: Int,
    val lastError: Throwable?,
    val cumulativeDelay: Duration,
) {
    /**
     * Whether this is the first attempt (no previous failures).
     */
    val isFirstAttempt: Boolean = attemptNumber == 0

    /**
     * Creates the next retry attempt.
     */
    fun nextAttempt(
        error: Throwable,
        additionalDelay: Duration,
    ): RetryAttempt =
        RetryAttempt(
            attemptNumber = attemptNumber + 1,
            lastError = error,
            cumulativeDelay = cumulativeDelay + additionalDelay,
        )

    companion object {
        /**
         * Creates the initial attempt (before any retries).
         */
        fun initial(): RetryAttempt =
            RetryAttempt(
                attemptNumber = 0,
                lastError = null,
                cumulativeDelay = Duration.ZERO,
            )
    }
}
