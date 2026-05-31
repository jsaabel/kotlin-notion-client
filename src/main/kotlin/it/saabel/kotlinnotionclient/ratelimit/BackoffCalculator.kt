package it.saabel.kotlinnotionclient.ratelimit

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
            RateLimitStrategy.CONSERVATIVE -> delay * 1.5

            // 50% longer delays
            RateLimitStrategy.AGGRESSIVE -> delay * 0.7

            // 30% shorter delays
            RateLimitStrategy.BALANCED -> delay

            // No modification
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
