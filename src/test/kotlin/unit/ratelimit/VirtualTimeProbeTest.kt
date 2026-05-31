@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package unit.ratelimit

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Investigation #4 from `journal/_task_07_rate_limiting_overhaul.md`:
 * confirm `kotlinx.coroutines.test` virtual time can drive `delay()` calls inside the
 * shape we plan to use in the new rate-limit plugin — a `do/while (retryNeeded)`
 * loop guarded by a `Mutex` (representing the token bucket math).
 *
 * If virtual time works here, all timing-sensitive ratelimit tests can use `runTest`
 * + `advanceTimeBy` and be deterministic. If it doesn't, the task doc should be
 * updated to fall back to wall-clock tolerances.
 */
@Tags("Unit")
class VirtualTimeProbeTest :
    StringSpec({

        "runTest + delay inside a retry/do-while loop advances virtual time" {
            runTest {
                var attempts = 0
                val delays = listOf(1.seconds, 2.seconds, 4.seconds)

                val wallStart = TimeSource.Monotonic.markNow()
                val virtualStart = currentTime

                do {
                    attempts++
                    if (attempts > delays.size) break
                    delay(delays[attempts - 1])
                } while (attempts <= delays.size)

                val wallElapsed = wallStart.elapsedNow()
                val virtualElapsed = currentTime - virtualStart

                println("  Wall-clock elapsed: ${wallElapsed.inWholeMilliseconds} ms")
                println("  Virtual elapsed:    $virtualElapsed ms")

                // Virtual time advanced by sum(delays) = 7000 ms
                virtualElapsed shouldBe 7_000L
                // Wall-clock barely moved (the whole loop was simulated)
                wallElapsed.inWholeMilliseconds shouldBeLessThan 500L
            }
        }

        "runTest + Mutex around delay (token-bucket sketch) keeps virtual time deterministic" {
            runTest {
                // Sketch of the planned token-bucket primitive: Mutex guards a shared
                // `tokens` double; when below 1, suspend `delay((1 - tokens) / rate)`.
                // We're not testing correctness of the bucket math here — only that
                // `delay()` inside `mutex.withLock { ... }` is virtualised.
                val mutex = Mutex()
                var tokens = 0.0
                val sustainedRate = 3.0 // tokens/sec
                var lastRefill = currentTime

                suspend fun acquire() {
                    mutex.withLock {
                        val now = currentTime
                        tokens = (tokens + (now - lastRefill).toDouble() / 1000.0 * sustainedRate).coerceAtMost(9.0)
                        lastRefill = now
                        if (tokens < 1.0) {
                            val needMs = ((1.0 - tokens) / sustainedRate * 1000.0).toLong()
                            delay(needMs.milliseconds)
                            tokens = 1.0
                            lastRefill = currentTime
                        }
                        tokens -= 1.0
                    }
                }

                val virtualStart = currentTime
                val wallStart = TimeSource.Monotonic.markNow()

                // Fire 5 sequential acquires. With burst=0 starting tokens and rate=3/s,
                // each needs ~333 ms to mint one token.
                repeat(5) { acquire() }

                val virtualElapsed = currentTime - virtualStart
                val wallElapsed = wallStart.elapsedNow().inWholeMilliseconds

                println("  Virtual elapsed for 5 acquires: $virtualElapsed ms (expected ~1666 ms)")
                println("  Wall-clock elapsed:             $wallElapsed ms")

                // 5 acquires at 333ms each ≈ 1665 ms in virtual time
                (virtualElapsed in 1_600L..1_700L) shouldBe true
                wallElapsed shouldBeLessThan 500L
            }
        }

        "advanceTimeBy skips a planned long sleep without wall-clock cost" {
            runTest {
                val virtualStart = currentTime
                val wallStart = TimeSource.Monotonic.markNow()

                // Simulate a Retry-After: 30 wait. Run the suspend in the background,
                // then advance virtual time past it.
                val job = launch { delay(30.seconds) }
                runCurrent()
                advanceTimeBy(30.seconds)
                runCurrent()
                job.join()

                val virtualElapsed = currentTime - virtualStart
                val wallElapsed = wallStart.elapsedNow().inWholeMilliseconds

                println("  Virtual elapsed: $virtualElapsed ms (expected 30000)")
                println("  Wall elapsed:    $wallElapsed ms")

                virtualElapsed shouldBe 30_000L
                wallElapsed shouldBeLessThan 500L
            }
        }
    })
