package unit.query

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.datasources.DateFilterBuilder
import it.saabel.kotlinnotionclient.models.datasources.RelativeDateValue
import it.saabel.kotlinnotionclient.models.datasources.TimestampFilterBuilder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * Timezone semantics of the date and timestamp filter builders.
 *
 * The LocalDateTime + TimeZone overloads mean "this wall-clock time, in this zone" and
 * send an offset-bearing string, matching the write-side dateTime semantics. Filter
 * conditions have no time_zone field, so offset-less datetime strings are rejected —
 * Notion would read them as UTC.
 */
@Tags("Unit")
class DateFilterTimezoneTest :
    StringSpec({

        val oslo = TimeZone.of("Europe/Oslo")

        "LocalDateTime filter sends the wall clock with the zone's offset" {
            val filter = DateFilterBuilder("Due").after(LocalDateTime(2026, 6, 15, 14, 30), oslo)

            filter.date?.after shouldBe "2026-06-15T14:30:00+02:00"
        }

        "LocalDateTime filter resolves DST at the value's own local date" {
            val before = DateFilterBuilder("Due").equals(LocalDateTime(2026, 10, 24, 13, 0), oslo)
            val after = DateFilterBuilder("Due").equals(LocalDateTime(2026, 10, 25, 13, 0), oslo)

            before.date?.equals shouldBe "2026-10-24T13:00:00+02:00"
            after.date?.equals shouldBe "2026-10-25T13:00:00+01:00"
        }

        "Instant filter sends the instant as-is" {
            val filter = DateFilterBuilder("Due").before(Instant.parse("2026-06-15T12:00:00Z"))

            filter.date?.before shouldBe "2026-06-15T12:00:00Z"
        }

        "Date-only and relative string values pass through" {
            DateFilterBuilder("Due").onOrAfter(LocalDate(2026, 6, 15)).date?.onOrAfter shouldBe "2026-06-15"
            DateFilterBuilder("Due").equals("2026-06-15").date?.equals shouldBe "2026-06-15"
            DateFilterBuilder("Due").before(RelativeDateValue.TODAY).date?.before shouldBe "today"
        }

        "Offset-bearing datetime strings pass through" {
            DateFilterBuilder("Due").after("2026-06-15T14:30:00+02:00").date?.after shouldBe
                "2026-06-15T14:30:00+02:00"
        }

        "An offset-less datetime string is rejected" {
            shouldThrow<IllegalArgumentException> {
                DateFilterBuilder("Due").after("2026-06-15T14:30:00")
            }
        }

        "Timestamp filters share the same semantics" {
            val typed = TimestampFilterBuilder("created_time").onOrAfter(LocalDateTime(2026, 10, 25, 13, 0), oslo)
            typed.createdTime?.onOrAfter shouldBe "2026-10-25T13:00:00+01:00"

            shouldThrow<IllegalArgumentException> {
                TimestampFilterBuilder("last_edited_time").before("2026-06-15T14:30:00")
            }
        }
    })
