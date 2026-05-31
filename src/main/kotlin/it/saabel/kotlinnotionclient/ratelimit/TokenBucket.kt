package it.saabel.kotlinnotionclient.ratelimit

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil

/**
 * Continuous-refill token bucket that proactively throttles outbound requests to Notion's
 * documented sustained ceiling while still allowing short bursts.
 *
 * Tokens refill **continuously** at [sustainedRate] tokens per second (not in periodic batches),
 * clamped at [burstCapacity]. The bucket starts full, so the first [burstCapacity] acquisitions
 * proceed immediately; thereafter callers are paced at roughly [sustainedRate] requests per second.
 *
 * [acquire] consumes a single token, suspending the caller (via [delay]) until one is available.
 * A single [Mutex] serialises acquisitions so they are FIFO-fair across concurrent coroutines —
 * no caller can starve another under contention.
 *
 * One bucket is created per [NotionRateLimit] plugin install — i.e. per `HttpClient` /
 * `NotionClient` — never as a process-global singleton.
 *
 * @param currentTimeMillis monotonic millisecond time source. Injectable so tests can drive the
 *   bucket with `kotlinx.coroutines.test` virtual time.
 */
internal class TokenBucket(
    private val sustainedRate: Double,
    private val burstCapacity: Int,
    private val currentTimeMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    init {
        require(sustainedRate > 0.0) { "sustainedRate must be positive" }
        require(burstCapacity >= 1) { "burstCapacity must be at least 1" }
    }

    private val tokensPerMillis = sustainedRate / 1000.0
    private val mutex = Mutex()

    private var tokens: Double = burstCapacity.toDouble()
    private var lastRefill: Long = currentTimeMillis()

    /**
     * Consumes a single token, suspending until one becomes available.
     *
     * Serialised and FIFO-fair: the [Mutex] is held across the wait so queued coroutines are
     * served strictly in arrival order.
     */
    suspend fun acquire() {
        mutex.withLock {
            while (true) {
                refill()
                if (tokens >= 1.0) {
                    tokens -= 1.0
                    return
                }
                val deficit = 1.0 - tokens
                val waitMillis = ceil(deficit / tokensPerMillis).toLong().coerceAtLeast(1L)
                delay(waitMillis)
            }
        }
    }

    private fun refill() {
        val now = currentTimeMillis()
        val elapsed = now - lastRefill
        if (elapsed > 0) {
            tokens = (tokens + elapsed * tokensPerMillis).coerceAtMost(burstCapacity.toDouble())
            lastRefill = now
        }
    }
}
