package unit.ratelimit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.ratelimit.RateLimitConfig
import kotlin.time.Duration.Companion.seconds

/**
 * Validates the final [RateLimitConfig] surface (issue #19): the six tunable fields and their
 * `require` guards. The former header-parsing / strategy-preset machinery is gone, so this is the
 * whole config contract.
 */
@Tags("Unit")
class RateLimitConfigTest :
    FunSpec({

        test("defaults match the documented surface") {
            val config = RateLimitConfig()
            config.sustainedRate shouldBe 3.0
            config.burstCapacity shouldBe 20
            config.maxRetries shouldBe 3
            config.retryBaseDelay shouldBe 1.seconds
            config.retryMaxDelay shouldBe 30.seconds
            config.jitterFactor shouldBe 0.1
        }

        test("rejects invalid configurations") {
            val invalidConfigs =
                listOf(
                    { RateLimitConfig(maxRetries = -1) },
                    { RateLimitConfig(retryBaseDelay = 0.seconds) },
                    { RateLimitConfig(retryBaseDelay = 10.seconds, retryMaxDelay = 5.seconds) },
                    { RateLimitConfig(jitterFactor = -0.1) },
                    { RateLimitConfig(jitterFactor = 1.1) },
                    { RateLimitConfig(sustainedRate = 0.0) },
                    { RateLimitConfig(burstCapacity = 0) },
                )

            invalidConfigs.forEach { factory ->
                shouldThrow<IllegalArgumentException> { factory() }
            }
        }

        test("accepts valid edge configurations") {
            // maxRetries = 0 disables retrying; equal base/max delay is allowed.
            RateLimitConfig(maxRetries = 0, retryBaseDelay = 5.seconds, retryMaxDelay = 5.seconds, jitterFactor = 0.0)
            RateLimitConfig(burstCapacity = 1, sustainedRate = 0.5)
        }
    })
