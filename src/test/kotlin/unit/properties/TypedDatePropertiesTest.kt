package unit.properties

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.pages.DateData
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.pageProperties
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * Tests for typed date/datetime property creation using kotlinx-datetime types.
 *
 * The LocalDateTime + TimeZone overloads mean "this wall-clock time, in this zone" and
 * write an offset-bearing string — never a `Z` instant. The offset is resolved at each
 * value's own local date, so DST boundaries are covered explicitly below.
 */
@Tags("Unit")
class TypedDatePropertiesTest :
    StringSpec({

        val oslo = TimeZone.of("Europe/Oslo")

        "Should create date property from LocalDate" {
            val properties =
                pageProperties {
                    date("Due Date", LocalDate(2025, 3, 15))
                }

            val dateValue = properties["Due Date"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15"
            dateValue.date?.end shouldBe null
        }

        "LocalDateTime in UTC writes an explicit +00:00 offset, not Z" {
            val properties =
                pageProperties {
                    dateTime("Meeting", LocalDateTime(2025, 3, 15, 14, 30), TimeZone.UTC)
                }

            val dateValue = properties["Meeting"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2025-03-15T14:30:00+00:00"
        }

        "LocalDateTime keeps its wall clock and gets the zone's offset" {
            val properties =
                pageProperties {
                    dateTime("Meeting", LocalDateTime(2025, 3, 15, 14, 30), TimeZone.of("America/New_York"))
                }

            val dateValue = properties["Meeting"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            // 14:30 stays 14:30 — the value is NOT converted to a UTC instant.
            dateValue.date?.start shouldBe "2025-03-15T14:30:00-04:00"
        }

        "DST is resolved at the value's own local date" {
            val properties =
                pageProperties {
                    dateTime("Before changeover", LocalDateTime(2026, 10, 24, 13, 0), oslo)
                    dateTime("After changeover", LocalDateTime(2026, 10, 25, 13, 0), oslo)
                }

            (properties["Before changeover"] as PagePropertyValue.DateValue).date?.start shouldBe
                "2026-10-24T13:00:00+02:00"
            (properties["After changeover"] as PagePropertyValue.DateValue).date?.start shouldBe
                "2026-10-25T13:00:00+01:00"
        }

        "An ambiguous local time resolves to the earlier instant" {
            // Europe/Oslo 2026-10-25T02:30 occurs at both +02:00 and +01:00; the earlier
            // instant wins — the same policy Notion applies when resolving a time_zone.
            val properties =
                pageProperties {
                    dateTime("Ambiguous", LocalDateTime(2026, 10, 25, 2, 30), oslo)
                }

            (properties["Ambiguous"] as PagePropertyValue.DateValue).date?.start shouldBe
                "2026-10-25T02:30:00+02:00"
        }

        "A nonexistent local time is shifted forward by the gap" {
            // Europe/Oslo 2026-03-29T02:30 does not exist (clocks jump 02:00 → 03:00);
            // the wall clock moves forward — the same policy Notion applies.
            val properties =
                pageProperties {
                    dateTime("Nonexistent", LocalDateTime(2026, 3, 29, 2, 30), oslo)
                }

            (properties["Nonexistent"] as PagePropertyValue.DateValue).date?.start shouldBe
                "2026-03-29T03:30:00+02:00"
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
            dateValue.date?.start shouldBe "2025-03-15T09:00:00+00:00"
            dateValue.date?.end shouldBe "2025-03-15T17:00:00+00:00"
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
            dateValue.date?.start shouldBe "2025-03-15T14:00:00+00:00"
            dateValue.date?.end shouldBe "2025-03-15T15:30:00+00:00"
        }

        "A datetime range spanning a DST changeover gets a different offset per end" {
            val properties =
                pageProperties {
                    dateTimeRange(
                        "Festival night",
                        LocalDateTime(2026, 10, 24, 22, 0),
                        LocalDateTime(2026, 10, 25, 4, 0),
                        timeZone = oslo,
                    )
                }

            val dateValue = properties["Festival night"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2026-10-24T22:00:00+02:00"
            dateValue.date?.end shouldBe "2026-10-25T04:00:00+01:00"
        }

        "The datetime range DSL resolves each end at its own local date" {
            val properties =
                pageProperties {
                    dateTimeRange("Changeover", timeZone = oslo) {
                        start = LocalDateTime(2026, 10, 24, 13, 0)
                        end = LocalDateTime(2026, 10, 25, 13, 0)
                    }
                }

            val dateValue = properties["Changeover"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            dateValue.date?.start shouldBe "2026-10-24T13:00:00+02:00"
            dateValue.date?.end shouldBe "2026-10-25T13:00:00+01:00"
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

        "String-based API accepts dates and offset-bearing datetimes" {
            val properties =
                pageProperties {
                    date("String Date", "2025-03-15")
                    dateTime("String DateTime", "2025-03-15T14:30:00Z")
                    dateTime("Offset DateTime", "2025-03-15T14:30:00+02:00")
                    dateRange("String Range", "2025-03-15", "2025-03-22")
                }

            (properties["String Date"] as PagePropertyValue.DateValue).date?.start shouldBe "2025-03-15"
            (properties["String DateTime"] as PagePropertyValue.DateValue).date?.start shouldBe "2025-03-15T14:30:00Z"
            (properties["Offset DateTime"] as PagePropertyValue.DateValue).date?.start shouldBe
                "2025-03-15T14:30:00+02:00"
            val rangeValue = properties["String Range"] as PagePropertyValue.DateValue
            rangeValue.date?.start shouldBe "2025-03-15"
            rangeValue.date?.end shouldBe "2025-03-22"
        }

        // ------------------------------------------------------------------
        // Validation — the guards that would have caught the original bug
        // ------------------------------------------------------------------

        "A datetime string with no offset and no time_zone is rejected" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    pageProperties {
                        dateTime("Broken", "2026-06-15T14:30:00")
                    }
                }
            exception.message shouldContain "2026-06-15T14:30:00"
            exception.message shouldContain "UTC"
            exception.message shouldContain "offset"
        }

        "An offset-less datetime is rejected in ranges too" {
            shouldThrow<IllegalArgumentException> {
                pageProperties {
                    dateTimeRange("Broken", "2026-06-15T14:30:00+02:00", "2026-06-15T15:30:00")
                }
            }
        }

        "An offset-less datetime is rejected by date(name, String) as well" {
            shouldThrow<IllegalArgumentException> {
                pageProperties {
                    date("Broken", "2026-06-15T14:30:00")
                }
            }
        }

        "A naive datetime with a time_zone is accepted" {
            val properties =
                pageProperties {
                    dateTimeWithTimeZone("Webinar", "2026-06-15T14:30:00", "America/New_York")
                }

            val dateValue = properties["Webinar"] as PagePropertyValue.DateValue
            dateValue.date?.start shouldBe "2026-06-15T14:30:00"
            dateValue.date?.timeZone shouldBe "America/New_York"
        }

        "An offset-bearing datetime combined with a time_zone is rejected" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    pageProperties {
                        dateTimeWithTimeZone("Broken", "2026-06-15T14:30:00Z", "America/New_York")
                    }
                }
            exception.message shouldContain "misinterprets"
        }

        "A time_zone on a date-only value is rejected" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    pageProperties {
                        dateTimeWithTimeZone("Broken", "2026-06-15", "America/New_York")
                    }
                }
            exception.message shouldContain "Date-only"
        }

        "An unknown time_zone id is rejected" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    pageProperties {
                        dateTimeWithTimeZone("Broken", "2026-06-15T14:30:00", "Europe/Atlantis")
                    }
                }
            exception.message shouldContain "Europe/Atlantis"
        }

        "The raw DateData path is validated too" {
            shouldThrow<IllegalArgumentException> {
                pageProperties {
                    date("Broken", DateData(start = "2026-06-15T14:30:00"))
                }
            }
            shouldThrow<IllegalArgumentException> {
                pageProperties {
                    date("Broken", DateData(start = "2026-06-15", timeZone = "Europe/Oslo"))
                }
            }
        }

        "verify() rejects offset-less datetimes" {
            shouldThrow<IllegalArgumentException> {
                pageProperties {
                    verify("Verification", start = "2026-06-15T14:30:00")
                }
            }
        }
    })
