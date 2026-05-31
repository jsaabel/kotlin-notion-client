package it.saabel.kotlinnotionclient.ratelimit

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the client-wide throttle + retry pipeline ([NotionRateLimit]).
 *
 * Two concerns are tuned here:
 * - **Proactive throttling** — a continuous-refill token bucket paces outbound requests at
 *   [sustainedRate] req/s, allowing short bursts up to [burstCapacity]. Heavy-concurrency callers
 *   (e.g. fan-out batch jobs) should raise [burstCapacity] rather than [sustainedRate], which is
 *   pinned to Notion's documented sustained ceiling.
 * - **Reactive retry** — on `429` and transient `5xx`/network failures, the pipeline retries with
 *   exponential backoff (honouring `Retry-After` on `429`).
 *
 * @property sustainedRate Tokens (requests) refilled per second. Notion's documented sustained
 *   ceiling is 3 req/s; lower it for politeness, but raising it risks `429`s.
 * @property burstCapacity Token-bucket size. Up to this many requests proceed immediately before
 *   pacing kicks in at [sustainedRate]. Tune higher for heavy-concurrency callers.
 * @property maxRetries Maximum number of *retries* after the initial attempt — **not** the total
 *   call count. Mind the off-by-one: `maxRetries = 3` permits up to **4** HTTP calls (1 initial + 3
 *   retries). `0` disables retrying (a single attempt).
 * @property retryBaseDelay Base delay for exponential backoff; the first retry waits ~this long.
 * @property retryMaxDelay Upper bound on any single backoff delay.
 * @property jitterFactor Randomness added to each backoff delay (0.0–1.0) to avoid thundering-herd
 *   synchronisation across concurrent callers.
 */
data class RateLimitConfig(
    val sustainedRate: Double = 3.0,
    val burstCapacity: Int = 20,
    val maxRetries: Int = 3,
    val retryBaseDelay: Duration = 1.seconds,
    val retryMaxDelay: Duration = 30.seconds,
    val jitterFactor: Double = 0.1,
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(retryBaseDelay > Duration.ZERO) { "retryBaseDelay must be positive" }
        require(retryMaxDelay >= retryBaseDelay) { "retryMaxDelay must be >= retryBaseDelay" }
        require(jitterFactor in 0.0..1.0) { "jitterFactor must be between 0.0 and 1.0" }
        require(sustainedRate > 0.0) { "sustainedRate must be positive" }
        require(burstCapacity >= 1) { "burstCapacity must be at least 1" }
    }
}
