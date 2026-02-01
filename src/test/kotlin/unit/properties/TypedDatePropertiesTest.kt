package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.pageProperties
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * Tests for typed date/datetime property creation using kotlinx-datetime types.
 *
 * Validates all the new overloads that accept LocalDate, LocalDateTime, and Instant.
 */
@Tags("Unit")
class TypedDatePropertiesTest :
    StringSpec({

        "Should create date property from LocalDate" {
            val properties =
                pageProperties {
                    date("Due Date", LocalDate(2025, 3, 15))
                }

            val dateValue = properties["Due Date"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15"
            dateValue.date?.end shouldBe null
        }

        "Should create datetime property from LocalDateTime with UTC" {
            val properties =
                pageProperties {
                    dateTime("Meeting", LocalDateTime(2025, 3, 15, 14, 30))
                }

            val dateValue = properties["Meeting"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15T14:30:00Z"
        }

        "Should create datetime property from LocalDateTime with custom timezone" {
            val nyTimeZone = TimeZone.of("America/New_York")
            val properties =
                pageProperties {
                    dateTime("Meeting", LocalDateTime(2025, 3, 15, 14, 30), timeZone = nyTimeZone)
                }

            val dateValue = properties["Meeting"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            // Should be converted to ISO-8601 (as UTC instant)
            // 14:30 in NY time zone gets converted to Instant, which is in UTC
            dateValue.date?.start shouldContain "2025-03-15T"
        }

        "Should create datetime property from Instant" {
            val properties =
                pageProperties {
                    dateTime("Event", Instant.parse("2025-03-15T14:30:00Z"))
                }

            val dateValue = properties["Event"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15T14:30:00Z"
        }

        "Should create date range from LocalDate with DSL" {
            val properties =
                pageProperties {
                    dateRange("Project Duration") {
                        start = LocalDate(2025, 3, 15)
                        end = LocalDate(2025, 3, 22)
                    }
                }

            val dateValue = properties["Project Duration"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15"
            dateValue.date?.end shouldBe "2025-03-22"
        }

        "Should create date range from LocalDate directly" {
            val properties =
                pageProperties {
                    dateRange("Sprint", LocalDate(2025, 3, 15), LocalDate(2025, 3, 29))
                }

            val dateValue = properties["Sprint"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15"
            dateValue.date?.end shouldBe "2025-03-29"
        }

        "Should create open-ended date range" {
            val properties =
                pageProperties {
                    dateRange("Started", LocalDate(2025, 3, 15), null)
                }

            val dateValue = properties["Started"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15"
            dateValue.date?.end shouldBe null
        }

        "Should create datetime range from LocalDateTime with DSL" {
            val properties =
                pageProperties {
                    dateTimeRange("Conference", timeZone = TimeZone.UTC) {
                        start = LocalDateTime(2025, 3, 15, 9, 0)
                        end = LocalDateTime(2025, 3, 15, 17, 0)
                    }
                }

            val dateValue = properties["Conference"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15T09:00:00Z"
            dateValue.date?.end shouldBe "2025-03-15T17:00:00Z"
        }

        "Should create datetime range from LocalDateTime directly" {
            val properties =
                pageProperties {
                    dateTimeRange(
                        "Meeting",
                        LocalDateTime(2025, 3, 15, 14, 0),
                        LocalDateTime(2025, 3, 15, 15, 30),
                        timeZone = TimeZone.UTC,
                    )
                }

            val dateValue = properties["Meeting"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15T14:00:00Z"
            dateValue.date?.end shouldBe "2025-03-15T15:30:00Z"
        }

        "Should create datetime range from Instant" {
            val properties =
                pageProperties {
                    dateTimeRange(
                        "Deployment",
                        Instant.parse("2025-03-15T00:00:00Z"),
                        Instant.parse("2025-03-15T04:00:00Z"),
                    )
                }

            val dateValue = properties["Deployment"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15T00:00:00Z"
            dateValue.date?.end shouldBe "2025-03-15T04:00:00Z"
        }

        "Should maintain backward compatibility with string-based API" {
            val properties =
                pageProperties {
                    date("String Date", "2025-03-15")
                    dateTime("String DateTime", "2025-03-15T14:30:00Z")
                    dateRange("String Range", "2025-03-15", "2025-03-22")
                }

            (properties["String Date"] as PagePropertyValue.DateValue).date?.start shouldBe "2025-03-15"
            (properties["String DateTime"] as PagePropertyValue.DateValue).date?.start shouldBe "2025-03-15T14:30:00Z"
            val rangeValue = properties["String Range"] as PagePropertyValue.DateValue
            rangeValue.date?.start shouldBe "2025-03-15"
            rangeValue.date?.end shouldBe "2025-03-22"
        }
    })
