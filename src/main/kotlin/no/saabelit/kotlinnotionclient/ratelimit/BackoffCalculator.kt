package no.saabelit.kotlinnotionclient.ratelimit

import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Calculates backoff delays for retry logic with exponential backoff and jitter.
 *
 * This implements industry-standard retry patterns to avoid overwhelming
 * the Notion API while providing reasonable user experience.
 */
class BackoffCalculator(
    private val config: RateLimitConfig,
    private val random: Random = Random.Default,
) {
    /**
     * Calculates the delay for a retry attempt.
     *
     * Uses exponential backoff with jitter to avoid thundering herd problems.
     * The formula is: baseDelay * (2^attempt) + jitter
     *
     * @param attempt The retry attempt (0-based, so first retry is attempt 1)
     * @param rateLimitState Current rate limit state from API headers (optional)
     * @return Duration to wait before the next attempt
     */
    fun calculateDelay(
        attempt: RetryAttempt,
        rateLimitState: RateLimitState? = null,
    ): Duration {
        // If we have a retry-after header, respect it
        if (config.respectRetryAfter && rateLimitState?.retryAfterSeconds != null) {
            val retryAfterMs = rateLimitState.retryAfterSeconds * 1000L
            return addJitter(retryAfterMs.milliseconds)
        }

        // Use rate limit state suggested delay if available and appropriate
        rateLimitState?.let { state ->
            if (state.isRateLimited && state.suggestedDelay > Duration.ZERO) {
                return addJitter(state.suggestedDelay)
            }
        }

        // Calculate exponential backoff delay
        val exponentialDelay = calculateExponentialBackoff(attempt.attemptNumber)

        // Apply strategy-specific modifications
        val strategyAdjustedDelay = applyStrategyModifier(exponentialDelay)

        // Add jitter and cap at maximum
        val jitteredDelay = addJitter(strategyAdjustedDelay)

        return minOf(jitteredDelay, config.maxDelayMs.milliseconds)
    }

    /**
     * Determines if we should retry based on the attempt and error type.
     *
     * @param attempt Current retry attempt
     * @param error The error that occurred
     * @param rateLimitState Current rate limit state (optional)
     * @return RateLimitDecision indicating what action to take
     */
    fun shouldRetry(
        attempt: RetryAttempt,
        error: Throwable,
        rateLimitState: RateLimitState? = null,
    ): RateLimitDecision {
        // Check if we've exceeded maximum retries
        if (attempt.attemptNumber > config.maxRetries) {
            return RateLimitDecision.Reject("Maximum retries (${config.maxRetries}) exceeded")
        }

        // Check if this is a retryable error
        if (!isRetryableError(error)) {
            return RateLimitDecision.Reject("Error is not retryable: ${error::class.simpleName}")
        }

        // Calculate delay for this retry
        val delay = calculateDelay(attempt, rateLimitState)

        // Check if cumulative delay would be excessive
        val totalDelay = attempt.cumulativeDelay + delay
        val maxCumulativeDelay = config.maxDelayMs.milliseconds * config.maxRetries
        if (totalDelay > maxCumulativeDelay) {
            return RateLimitDecision.Reject("Cumulative delay would exceed maximum ($maxCumulativeDelay)")
        }

        return RateLimitDecision.Wait(
            delay = delay,
            reason = "Retrying after ${error::class.simpleName} (attempt ${attempt.attemptNumber + 1}/${config.maxRetries})",
        )
    }

    /**
     * Calculates exponential backoff delay.
     */
    private fun calculateExponentialBackoff(attemptNumber: Int): Duration {
        val exponentialFactor = 2.0.pow(attemptNumber.toDouble())
        val delayMs = (config.baseDelayMs * exponentialFactor).toLong()
        return delayMs.milliseconds
    }

    /**
     * Applies strategy-specific modifications to the delay.
     */
    private fun applyStrategyModifier(delay: Duration): Duration =
        when (config.strategy) {
            RateLimitStrategy.CONSERVATIVE -> delay * 1.5 // 50% longer delays
            RateLimitStrategy.AGGRESSIVE -> delay * 0.7 // 30% shorter delays
            RateLimitStrategy.BALANCED -> delay // No modification
            RateLimitStrategy.CUSTOM -> delay // User-configured, no modification
        }

    /**
     * Adds jitter to prevent thundering herd problems.
     */
    private fun addJitter(delay: Duration): Duration {
        if (config.jitterFactor <= 0.0) return delay

        val jitterRange = (delay.inWholeMilliseconds * config.jitterFactor).toLong()
        val jitterMs = random.nextLong(-jitterRange, jitterRange + 1)
        val jitteredMs = delay.inWholeMilliseconds + jitterMs

        return maxOf(jitteredMs, 0).milliseconds
    }

    /**
     * Determines if an error is retryable.
     */
    private fun isRetryableError(error: Throwable): Boolean =
        when {
            // Rate limiting errors are always retryable
            isRateLimitError(error) -> true

            // Server errors (5xx) are generally retryable
            isServerError(error) -> true

            // Network errors are retryable
            isNetworkError(error) -> true

            // Client errors (4xx except 429) are generally not retryable
            isClientError(error) -> false

            // Other errors default to not retryable
            else -> false
        }

    /**
     * Checks if error is a rate limiting error (429).
     */
    private fun isRateLimitError(error: Throwable): Boolean {
        // This will need to be updated based on how we structure our exceptions
        val message = error.message?.lowercase() ?: ""
        return message.contains("429") || message.contains("rate limit") || message.contains("too many requests")
    }

    /**
     * Checks if error is a server error (5xx).
     */
    private fun isServerError(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("500") ||
            message.contains("502") ||
            message.contains("503") ||
            message.contains("504") ||
            message.contains("internal server error") ||
            message.contains("bad gateway") ||
            message.contains("service unavailable") ||
            message.contains("gateway timeout")
    }

    /**
     * Checks if error is a network error.
     */
    private fun isNetworkError(error: Throwable): Boolean =
        when (error) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException,
            is java.io.IOException,
            -> true

            else -> {
                val message = error.message?.lowercase() ?: ""
                message.contains("timeout") ||
                    message.contains("connection") ||
                    message.contains("network") ||
                    message.contains("unreachable")
            }
        }

    /**
     * Checks if error is a client error (4xx except 429).
     */
    private fun isClientError(error: Throwable): Boolean {
        val message = error.message?.lowercase() ?: ""
        return (
            message.contains("400") ||
                message.contains("401") ||
                message.contains("403") ||
                message.contains("404") ||
                message.contains("bad request") ||
                message.contains("unauthorized") ||
                message.contains("forbidden") ||
                message.contains("not found")
        ) &&
            !isRateLimitError(error)
    }
}

/**
 * Factory for creating BackoffCalculator instances with predefined strategies.
 */
object BackoffCalculatorFactory {
    /**
     * Creates a calculator with conservative settings.
     */
    fun conservative(): BackoffCalculator = BackoffCalculator(RateLimitConfig.CONSERVATIVE)

    /**
     * Creates a calculator with aggressive settings.
     */
    fun aggressive(): BackoffCalculator = BackoffCalculator(RateLimitConfig.AGGRESSIVE)

    /**
     * Creates a calculator with balanced settings.
     */
    fun balanced(): BackoffCalculator = BackoffCalculator(RateLimitConfig.BALANCED)

    /**
     * Creates a calculator with custom configuration.
     */
    fun custom(config: RateLimitConfig): BackoffCalculator = BackoffCalculator(config)
}
