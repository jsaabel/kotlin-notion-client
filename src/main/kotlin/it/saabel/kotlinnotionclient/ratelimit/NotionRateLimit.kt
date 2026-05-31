package it.saabel.kotlinnotionclient.ratelimit

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.delay

/**
 * Configuration for the [NotionRateLimit] plugin.
 */
class NotionRateLimitConfig {
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
     * Sustained request rate (tokens/second) for the proactive token bucket. Defaults to Notion's
     * documented sustained ceiling of 3 requests/second.
     */
    var sustainedRate: Double = 3.0

    /**
     * Burst capacity (token-bucket size). Up to this many requests proceed immediately before
     * pacing kicks in at [sustainedRate].
     */
    var burstCapacity: Int = 20

    /**
     * Test seam: overrides the token bucket's millisecond time source so tests can drive pacing
     * with `kotlinx.coroutines.test` virtual time. `null` uses a real monotonic clock.
     */
    internal var timeSourceMillis: (() -> Long)? = null

    /**
     * Converts this config to a [RateLimitConfig].
     */
    internal fun toRateLimitConfig(): RateLimitConfig =
        RateLimitConfig(
            maxRetries = maxRetries,
            baseDelayMs = baseDelayMs,
            maxDelayMs = maxDelayMs,
            jitterFactor = jitterFactor,
            strategy = strategy,
            respectRetryAfter = respectRetryAfter,
            sustainedRate = sustainedRate,
            burstCapacity = burstCapacity,
        )
}

/**
 * Ktor client plugin that handles Notion API rate limits automatically.
 *
 * Unlike a plain wrapper, this plugin hooks the [Send] pipeline phase, so **every** outbound
 * HTTP request flows through the retry interceptor without any call-site wrapping. API methods
 * simply issue their requests; the plugin transparently retries on `429 Too Many Requests`.
 *
 * Every request is proceeded once; while the response status is `429` and retries remain, the
 * interceptor waits for the backoff delay and re-sends the same request.
 *
 * Current behaviour (structural foundation — see the rate-limiting overhaul task):
 * - Proactively throttles outbound requests through a per-client [TokenBucket] (continuous refill
 *   at `sustainedRate` req/s, bursting up to `burstCapacity`). A token is acquired before each
 *   send, including retries.
 * - Retries only on `429` responses.
 * - Uses exponential backoff with jitter via [BackoffCalculator].
 * - Does **not** read `Retry-After` / `x-ratelimit-*` headers yet.
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
val NotionRateLimit =
    createClientPlugin("NotionRateLimit", ::NotionRateLimitConfig) {
        val config = pluginConfig.toRateLimitConfig()
        val backoffCalculator = BackoffCalculator(config)

        // One bucket per plugin install == one per HttpClient / NotionClient (never global).
        val tokenBucket =
            TokenBucket(
                sustainedRate = config.sustainedRate,
                burstCapacity = config.burstCapacity,
                currentTimeMillis = pluginConfig.timeSourceMillis ?: { System.nanoTime() / 1_000_000 },
            )

        on(Send) { request ->
            var attemptNumber = 0
            var cumulativeDelay = kotlin.time.Duration.ZERO
            tokenBucket.acquire()
            var call = proceed(request)

            while (call.response.status.value == 429 && attemptNumber < config.maxRetries) {
                val retryAttempt =
                    RetryAttempt(
                        attemptNumber = attemptNumber,
                        lastError = null,
                        cumulativeDelay = cumulativeDelay,
                    )

                // Preserve current behaviour: 429-only retry with exponential backoff,
                // no header reading yet.
                val delayDuration = backoffCalculator.calculateDelay(retryAttempt, null)
                delay(delayDuration)
                cumulativeDelay += delayDuration
                attemptNumber++

                // A retry is another outbound request — pace it through the bucket too.
                tokenBucket.acquire()
                call = proceed(request)
            }

            call
        }
    }
