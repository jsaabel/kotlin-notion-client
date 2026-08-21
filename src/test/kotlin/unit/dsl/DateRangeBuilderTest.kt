package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.pages.DateRangeBuilder
import it.saabel.kotlinnotionclient.models.pages.InstantRangeBuilder
import it.saabel.kotlinnotionclient.models.pages.LocalDateTimeRangeBuilder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * Tests for date range DSL builders.
 *
 * Validates the builder pattern for creating date and datetime ranges using kotlinx-datetime types.
 */
@Tags("Unit")
class DateRangeBuilderTest :
    StringSpec({

        "DateRangeBuilder should build date range with start and end" {
            val builder = DateRangeBuilder()
            builder.start = LocalDate(2025, 3, 15)
            builder.end = LocalDate(2025, 3, 22)

            val (startStr, endStr) = builder.build()

            startStr shouldBe "2025-03-15"
            endStr shouldBe "2025-03-22"
        }

        "DateRangeBuilder should build open-ended range with null end" {
            val builder = DateRangeBuilder()
            builder.start = LocalDate(2025, 3, 15)
            builder.end = null

            val (startStr, endStr) = builder.build()

            startStr shouldBe "2025-03-15"
            endStr shouldBe null
        }

        "DateRangeBuilder should throw when start is not set" {
            val builder = DateRangeBuilder()
            builder.end = LocalDate(2025, 3, 22)

            val exception =
                shouldThrow<IllegalStateException> {
                    builder.build()
                }

            exception.message shouldContain "Start date must be set"
        }

        "LocalDateTimeRangeBuilder should build datetime range with UTC timezone" {
            val builder = LocalDateTimeRangeBuilder(TimeZone.UTC)
            builder.start = LocalDateTime(2025, 3, 15, 14, 30)
            builder.end = LocalDateTime(2025, 3, 15, 16, 0)

            val (startStr, endStr) = builder.build()

            startStr shouldBe "2025-03-15T14:30:00+00:00"
            endStr shouldBe "2025-03-15T16:00:00+00:00"
        }

        "LocalDateTimeRangeBuilder should preserve the wall clock and attach the zone's offset" {
            val nyTimeZone = TimeZone.of("America/New_York")
            val builder = LocalDateTimeRangeBuilder(nyTimeZone)
            builder.start = LocalDateTime(2025, 3, 15, 14, 30)
            builder.end = LocalDateTime(2025, 3, 15, 16, 0)

            val (startStr, endStr) = builder.build()

            // In March, New York is UTC-4 (EDT); the local digits are preserved.
            startStr shouldBe "2025-03-15T14:30:00-04:00"
            endStr shouldBe "2025-03-15T16:00:00-04:00"
        }

        "LocalDateTimeRangeBuilder resolves DST at each end's own local date" {
            val builder = LocalDateTimeRangeBuilder(TimeZone.of("Europe/Oslo"))
            builder.start = LocalDateTime(2026, 10, 24, 22, 0)
            builder.end = LocalDateTime(2026, 10, 25, 4, 0)

            val (startStr, endStr) = builder.build()

            startStr shouldBe "2026-10-24T22:00:00+02:00"
            endStr shouldBe "2026-10-25T04:00:00+01:00"
        }

        "LocalDateTimeRangeBuilder should build open-ended range" {
            val builder = LocalDateTimeRangeBuilder(TimeZone.UTC)
            builder.start = LocalDateTime(2025, 3, 15, 14, 30)
            builder.end = null

            val (startStr, endStr) = builder.build()

            startStr shouldBe "2025-03-15T14:30:00+00:00"
            endStr shouldBe null
        }

        "LocalDateTimeRangeBuilder should throw when start is not set" {
            val builder = LocalDateTimeRangeBuilder(TimeZone.UTC)
            builder.end = LocalDateTime(2025, 3, 15, 16, 0)

            val exception =
                shouldThrow<IllegalStateException> {
                    builder.build()
                }

            exception.message shouldContain "Start datetime must be set"
        }

        "InstantRangeBuilder should build instant range" {
            val builder = InstantRangeBuilder()
            builder.start = Instant.parse("2025-03-15T00:00:00Z")
            builder.end = Instant.parse("2025-03-15T04:00:00Z")

            val (startStr, endStr) = builder.build()

            startStr shouldBe "2025-03-15T00:00:00Z"
            endStr shouldBe "2025-03-15T04:00:00Z"
        }

        "InstantRangeBuilder should build open-ended range" {
            val builder = InstantRangeBuilder()
            builder.start = Instant.parse("2025-03-15T00:00:00Z")
            builder.end = null

            val (startStr, endStr) = builder.build()

            startStr shouldBe "2025-03-15T00:00:00Z"
            endStr shouldBe null
        }

        "InstantRangeBuilder should throw when start is not set" {
            val builder = InstantRangeBuilder()
            builder.end = Instant.parse("2025-03-15T04:00:00Z")

            val exception =
                shouldThrow<IllegalStateException> {
                    builder.build()
                }

            exception.message shouldContain "Start instant must be set"
        }
    })
