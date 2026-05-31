package it.saabel.kotlinnotionclient.ratelimit

import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the [NotionRateLimit] plugin.
 */
class NotionRateLimitConfig {
    /**
     * Throttle + retry settings. Defaults to [RateLimitConfig]'s own defaults.
     */
    var rateLimitConfig: RateLimitConfig = RateLimitConfig()

    /**
     * Test seam: overrides the token bucket's millisecond time source so tests can drive pacing
     * with `kotlinx.coroutines.test` virtual time. `null` uses a real monotonic clock.
     */
    internal var timeSourceMillis: (() -> Long)? = null
}

/**
 * Ktor client plugin that handles Notion API throttling and retries automatically.
 *
 * The plugin hooks the [Send] pipeline phase, so **every** outbound HTTP request flows through it
 * without any call-site wrapping. API methods simply issue their requests.
 *
 * Behaviour:
 * - Proactively throttles outbound requests through a per-client [TokenBucket] (continuous refill at
 *   `sustainedRate` req/s, bursting up to `burstCapacity`). A token is acquired before each send,
 *   including retries.
 * - Classifies failures by *type*, never by string-matching `error.message`: the plugin has the
 *   response object in scope, so it inspects [io.ktor.http.HttpStatusCode] and exception classes
 *   directly.
 * - On a `429`, honours the `Retry-After` header (seconds) when present: waits exactly that long
 *   plus a 1s rounding-safety margin, then retries. This is Notion's published 429 contract. The
 *   header-driven delay deliberately does **not** stack with the exponential schedule. Falls back to
 *   exponential backoff with jitter when a `429` arrives without `Retry-After` (defence-in-depth —
 *   per the docs this shouldn't happen).
 * - Retries `502 / 503 / 504` (transient gateway/availability failures) on the exponential backoff
 *   schedule. `500` is deliberately **not** retried — it typically signals a non-transient
 *   server-side fault rather than a blip.
 * - Retries network failures ([IOException] and its subtypes — `SocketTimeoutException`,
 *   `ConnectException`, `UnknownHostException`) on the same exponential schedule. These surface as
 *   thrown exceptions from `proceed`, so they are caught and folded into the same retry loop.
 * - Everything else (other `4xx`, `500`, success) is returned/propagated immediately — no retry, no
 *   delay. In particular, after the final permitted attempt fails, the result is returned/thrown
 *   immediately with no wasted post-mortem sleep.
 *
 * Usage:
 * ```kotlin
 * val client = HttpClient {
 *     install(NotionRateLimit) {
 *         rateLimitConfig = RateLimitConfig(maxRetries = 5, burstCapacity = 40)
 *     }
 * }
 * ```
 */
val NotionRateLimit =
    createClientPlugin("NotionRateLimit", ::NotionRateLimitConfig) {
        val config = pluginConfig.rateLimitConfig
        val random = Random.Default

        // One bucket per plugin install == one per HttpClient / NotionClient (never global).
        val tokenBucket =
            TokenBucket(
                sustainedRate = config.sustainedRate,
                burstCapacity = config.burstCapacity,
                currentTimeMillis = pluginConfig.timeSourceMillis ?: { System.nanoTime() / 1_000_000 },
            )

        // Exponential backoff with jitter, capped at retryMaxDelay. The whole schedule is these few
        // lines, so it lives inline rather than in a separate calculator type.
        fun exponentialBackoff(attemptNumber: Int): Duration {
            val base = config.retryBaseDelay * 2.0.pow(attemptNumber)
            val jittered =
                if (config.jitterFactor > 0.0) {
                    val jitterRange = (base.inWholeMilliseconds * config.jitterFactor).toLong()
                    val jitterMs = random.nextLong(-jitterRange, jitterRange + 1)
                    (base.inWholeMilliseconds + jitterMs).coerceAtLeast(0L).milliseconds
                } else {
                    base
                }
            return minOf(jittered, config.retryMaxDelay)
        }

        // 429 delay: Retry-After is load-bearing (Notion's published contract) — wait exactly that
        // long plus a 1s rounding-safety margin, replacing (not stacking with) the exponential
        // schedule. Falls back to exponential backoff only when the header is absent.
        fun retryAfterDelay(
            call: HttpClientCall,
            attemptNumber: Int,
        ): Duration {
            val retryAfterSeconds = call.response.headers[HttpHeaders.RetryAfter]?.toLongOrNull()
            return if (retryAfterSeconds != null) {
                retryAfterSeconds.seconds + 1.seconds
            } else {
                exponentialBackoff(attemptNumber)
            }
        }

        on(Send) { request ->
            var attemptNumber = 0

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
                        // Only sleep when a retry will actually follow. After the final permitted
                        // attempt, rethrow immediately — no wasted post-mortem delay.
                        if (attemptNumber >= config.maxRetries) throw cause
                        delay(exponentialBackoff(attemptNumber))
                        attemptNumber++
                        continue
                    }

                val status = call.response.status.value
                val retriesRemaining = attemptNumber < config.maxRetries

                // Typed classifier: decide the retry delay from the status code alone. 429 takes the
                // Retry-After path; 502/503/504 take exponential backoff; everything else (other 4xx,
                // 500, success) is terminal. `retriesRemaining` guards the wasted-delay case: once
                // retries are exhausted the failed call is returned immediately with no extra sleep.
                val delayDuration =
                    when {
                        status == 429 && retriesRemaining -> retryAfterDelay(call, attemptNumber)
                        status in RETRYABLE_SERVER_STATUSES && retriesRemaining -> exponentialBackoff(attemptNumber)
                        else -> null
                    }

                if (delayDuration == null) {
                    result = call
                } else {
                    delay(delayDuration)
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
