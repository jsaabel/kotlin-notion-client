@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package unit.ratelimit

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.ratelimit.TokenBucket
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest

/**
 * Virtual-time tests for [TokenBucket] (rate-limiting overhaul §2).
 *
 * The bucket's `delay()`-based pacing is driven by `kotlinx.coroutines.test` virtual time, with
 * the bucket's clock wired to the test scheduler via `timeSourceMillis`. This keeps the tests
 * deterministic and free of real wall-clock cost (see `VirtualTimeProbeTest` for the precedent).
 */
@Tags("Unit")
class TokenBucketTest :
    FunSpec({

        test("first burstCapacity acquisitions are instant; the remainder pace at the sustained rate") {
            runTest {
                val sustainedRate = 3.0
                val burstCapacity = 20
                val extra = 10
                val bucket =
                    TokenBucket(
                        sustainedRate = sustainedRate,
                        burstCapacity = burstCapacity,
                        currentTimeMillis = { currentTime },
                    )

                val completionTimes = mutableListOf<Long>()
                repeat(burstCapacity + extra) {
                    launch {
                        bucket.acquire()
                        completionTimes += currentTime
                    }
                }
                advanceUntilIdle()

                completionTimes.size shouldBe burstCapacity + extra

                // The first `burstCapacity` requests drain the full bucket immediately.
                completionTimes.take(burstCapacity).all { it == 0L } shouldBe true

                // The remaining `extra` requests pace at ~sustainedRate req/s, so total wall time
                // is approximately extra / sustainedRate seconds.
                val expectedMillis = (extra / sustainedRate * 1000).toLong() // ≈ 3333 ms
                val tolerance = 400L
                val total = completionTimes.last()
                (total in (expectedMillis - tolerance)..(expectedMillis + tolerance)) shouldBe true
            }
        }

        test("acquisitions are FIFO-fair under contention") {
            runTest {
                val bucket =
                    TokenBucket(
                        sustainedRate = 3.0,
                        burstCapacity = 5,
                        currentTimeMillis = { currentTime },
                    )

                val completionOrder = mutableListOf<Int>()
                val total = 25
                repeat(total) { index ->
                    launch {
                        bucket.acquire()
                        completionOrder += index
                    }
                }
                advanceUntilIdle()

                // No starvation: every acquirer completes, strictly in arrival order.
                completionOrder shouldBe (0 until total).toList()
            }
        }

        test("the bucket refills continuously while idle") {
            runTest {
                val bucket =
                    TokenBucket(
                        sustainedRate = 3.0,
                        burstCapacity = 5,
                        currentTimeMillis = { currentTime },
                    )

                // Drain the full burst instantly.
                repeat(5) { bucket.acquire() }
                currentTime shouldBe 0L

                // Idle for 1s → 3 tokens refill (3/s), so 3 acquisitions are instant again.
                kotlinx.coroutines.delay(1000)
                val before = currentTime
                repeat(3) { bucket.acquire() }
                currentTime shouldBe before // all three were free
            }
        }

        test("each bucket instance is independent (not a global singleton)") {
            runTest {
                val makeBucket = {
                    TokenBucket(
                        sustainedRate = 3.0,
                        burstCapacity = 5,
                        currentTimeMillis = { currentTime },
                    )
                }
                val bucketA = makeBucket()
                val bucketB = makeBucket()

                // Drain A entirely and force it to pace once.
                repeat(6) { bucketA.acquire() }

                // B is untouched, so its first acquisition is still immediate at the current instant.
                val before = currentTime
                bucketB.acquire()
                currentTime shouldBe before
            }
        }
    })
