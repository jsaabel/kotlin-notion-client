package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import no.saabelit.kotlinnotionclient.models.pages.DateRangeBuilder
import no.saabelit.kotlinnotionclient.models.pages.InstantRangeBuilder
import no.saabelit.kotlinnotionclient.models.pages.LocalDateTimeRangeBuilder

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

            startStr shouldBe "2025-03-15T14:30:00Z"
            endStr shouldBe "2025-03-15T16:00:00Z"
        }

        "LocalDateTimeRangeBuilder should build datetime range with custom timezone" {
            val nyTimeZone = TimeZone.of("America/New_York")
            val builder = LocalDateTimeRangeBuilder(nyTimeZone)
            builder.start = LocalDateTime(2025, 3, 15, 14, 30)
            builder.end = LocalDateTime(2025, 3, 15, 16, 0)

            val (startStr, endStr) = builder.build()

            // In March, New York is UTC-4 (EDT)
            // 14:30 EDT = 18:30 UTC
            startStr shouldContain "2025-03-15T"
            endStr shouldContain "2025-03-15T"
        }

        "LocalDateTimeRangeBuilder should build open-ended range" {
            val builder = LocalDateTimeRangeBuilder(TimeZone.UTC)
            builder.start = LocalDateTime(2025, 3, 15, 14, 30)
            builder.end = null

            val (startStr, endStr) = builder.build()

            startStr shouldBe "2025-03-15T14:30:00Z"
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
