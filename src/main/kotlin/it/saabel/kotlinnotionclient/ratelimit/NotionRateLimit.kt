package it.saabel.kotlinnotionclient.ratelimit

import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
 * - Classifies failures by *type*, never by string-matching `error.message`: the plugin has the
 *   response object in scope, so it inspects [io.ktor.http.HttpStatusCode] and exception classes
 *   directly.
 * - On a `429`, honours the `Retry-After` header (seconds) when present and [respectRetryAfter] is
 *   enabled: waits exactly that long plus a 1s rounding-safety margin, then retries. This is
 *   Notion's published 429 contract. The header-driven delay deliberately does **not** stack with
 *   the exponential schedule. Falls back to exponential backoff with jitter via [BackoffCalculator]
 *   when a `429` arrives without `Retry-After` (defence-in-depth — per the docs this shouldn't
 *   happen).
 * - Retries `502 / 503 / 504` (transient gateway/availability failures) on the exponential backoff
 *   schedule. `500` is deliberately **not** retried — it typically signals a non-transient
 *   server-side fault rather than a blip.
 * - Retries network failures ([IOException] and its subtypes — `SocketTimeoutException`,
 *   `ConnectException`, `UnknownHostException`) on the same exponential schedule. These surface as
 *   thrown exceptions from `proceed`, so they are caught and folded into the same retry loop.
 * - Everything else (other `4xx`, `500`, success) is returned/propagated immediately — no retry,
 *   no delay.
 * - Does **not** read `x-ratelimit-*` headers — Notion does not emit them on 2xx responses.
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
            var cumulativeDelay = Duration.ZERO

            // Set once a terminal response is reached (success or a non-retryable failure); the loop
            // exits only then, so after it `result` is a guaranteed non-null call.
            var result: HttpClientCall? = null
            while (result == null) {
                // Every send — initial or retry — is paced through the bucket.
                tokenBucket.acquire()

                // Network failures (IOException family) surface as thrown exceptions from `proceed`.
                // Catch them so they enter the same retry loop as HTTP failures; non-retryable
                // throwables (including coroutine cancellation) are re-thrown untouched.
                val call =
                    try {
                        proceed(request)
                    } catch (cause: CancellationException) {
                        throw cause
                    } catch (cause: IOException) {
                        if (attemptNumber >= config.maxRetries) throw cause
                        val retryAttempt = RetryAttempt(attemptNumber, cause, cumulativeDelay)
                        val networkDelay = backoffCalculator.calculateDelay(retryAttempt, null)
                        delay(networkDelay)
                        cumulativeDelay += networkDelay
                        attemptNumber++
                        continue
                    }

                val status = call.response.status.value
                val retriesRemaining = attemptNumber < config.maxRetries

                // Typed classifier: decide the retry delay from the status code alone. 429 takes the
                // Retry-After path; 502/503/504 take exponential backoff; everything else (other 4xx,
                // 500, success) is terminal.
                val delayDuration =
                    when {
                        status == 429 && retriesRemaining -> {
                            retryAfterDelay(call, config, backoffCalculator, attemptNumber, cumulativeDelay)
                        }

                        status in RETRYABLE_SERVER_STATUSES && retriesRemaining -> {
                            backoffCalculator.calculateDelay(RetryAttempt(attemptNumber, null, cumulativeDelay), null)
                        }

                        else -> {
                            null
                        }
                    }

                if (delayDuration == null) {
                    result = call
                } else {
                    delay(delayDuration)
                    cumulativeDelay += delayDuration
                    attemptNumber++
                }
            }

            result
        }
    }

/**
 * Server-side statuses worth retrying on the exponential schedule. `500` is deliberately excluded:
 * it usually signals a non-transient server fault rather than a transient blip, whereas `502`
 * (bad gateway), `503` (service unavailable) and `504` (gateway timeout) are typically momentary.
 */
private val RETRYABLE_SERVER_STATUSES = setOf(502, 503, 504)

/**
 * Computes the delay for a `429` retry. Notion publishes `Retry-After` (seconds) as its 429
 * contract: when present (and honouring is enabled) it is load-bearing — wait exactly that long
 * plus a 1s rounding-safety margin. This deliberately does **not** stack with the exponential
 * schedule; it replaces it on the primary 429 path. Falls back to exponential backoff only when the
 * header is absent (defence-in-depth — per the docs this shouldn't happen).
 */
private fun retryAfterDelay(
    call: HttpClientCall,
    config: RateLimitConfig,
    backoffCalculator: BackoffCalculator,
    attemptNumber: Int,
    cumulativeDelay: Duration,
): Duration {
    val retryAfterSeconds =
        if (config.respectRetryAfter) {
            call.response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()
        } else {
            null
        }

    return if (retryAfterSeconds != null) {
        retryAfterSeconds.seconds + 1.seconds
    } else {
        backoffCalculator.calculateDelay(RetryAttempt(attemptNumber, null, cumulativeDelay), null)
    }
}
