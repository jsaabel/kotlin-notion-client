package unit.properties

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.pages.DateData
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.endInstantValue
import it.saabel.kotlinnotionclient.models.pages.endLocalDateTimeIn
import it.saabel.kotlinnotionclient.models.pages.endLocalDateTimeNaive
import it.saabel.kotlinnotionclient.models.pages.endLocalDateValue
import it.saabel.kotlinnotionclient.models.pages.endOffsetIn
import it.saabel.kotlinnotionclient.models.pages.endStoredOffset
import it.saabel.kotlinnotionclient.models.pages.endStringValue
import it.saabel.kotlinnotionclient.models.pages.endToLocalDateTime
import it.saabel.kotlinnotionclient.models.pages.endUtcInstant
import it.saabel.kotlinnotionclient.models.pages.endWallClockDateTime
import it.saabel.kotlinnotionclient.models.pages.instantValue
import it.saabel.kotlinnotionclient.models.pages.localDateTimeIn
import it.saabel.kotlinnotionclient.models.pages.localDateTimeNaive
import it.saabel.kotlinnotionclient.models.pages.localDateValue
import it.saabel.kotlinnotionclient.models.pages.offsetIn
import it.saabel.kotlinnotionclient.models.pages.requireEndUtcInstant
import it.saabel.kotlinnotionclient.models.pages.requireUtcInstant
import it.saabel.kotlinnotionclient.models.pages.storedOffset
import it.saabel.kotlinnotionclient.models.pages.stringValue
import it.saabel.kotlinnotionclient.models.pages.toLocalDateTime
import it.saabel.kotlinnotionclient.models.pages.utcInstant
import it.saabel.kotlinnotionclient.models.pages.wallClockDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlin.time.Instant

private fun dateProp(
    start: String?,
    end: String? = null,
    timeZone: String? = null,
): PageProperty.Date =
    PageProperty.Date(
        id = "id",
        type = "date",
        date = start?.let { DateData(start = it, end = end, timeZone = timeZone) },
    )

private val oslo = TimeZone.of("Europe/Oslo")

/**
 * Tests for the PageProperty.Date read accessors.
 *
 * The accessors answer three different questions and the tests are grouped the same way:
 * wall-clock digits ([wallClockDateTime]), absolute instant ([utcInstant]) and the value's
 * own stored offset ([storedOffset]).
 */
@Tags("Unit")
@Suppress("DEPRECATION")
class PagePropertyDateAccessorsTest :
    StringSpec({

        // ----------------------------------------
        // Offset-bearing datetime — the normal Notion value
        // ----------------------------------------

        "offset-bearing datetime exposes wall-clock digits, stored offset and UTC instant" {
            val property = dateProp("2026-06-15T14:30:00.000+02:00")

            property.wallClockDateTime shouldBe LocalDateTime(2026, 6, 15, 14, 30)
            property.storedOffset shouldBe UtcOffset(hours = 2)
            property.utcInstant shouldBe Instant.parse("2026-06-15T12:30:00Z")
            property.requireUtcInstant() shouldBe Instant.parse("2026-06-15T12:30:00Z")
            property.localDateTimeIn(TimeZone.UTC) shouldBe LocalDateTime(2026, 6, 15, 12, 30)
        }

        "negative offsets are read as stored" {
            val property = dateProp("2026-06-15T14:30:00.000-04:00")

            property.wallClockDateTime shouldBe LocalDateTime(2026, 6, 15, 14, 30)
            property.storedOffset shouldBe UtcOffset(hours = -4)
            property.utcInstant shouldBe Instant.parse("2026-06-15T18:30:00Z")
        }

        "half-hour offsets are read as stored" {
            val property = dateProp("2026-06-15T14:30:00+05:30")

            property.storedOffset shouldBe UtcOffset(hours = 5, minutes = 30)
            property.utcInstant shouldBe Instant.parse("2026-06-15T09:00:00Z")
        }

        "basic-format and hour-only offsets are accepted" {
            dateProp("2026-06-15T14:30:00+0200").storedOffset shouldBe UtcOffset(hours = 2)
            dateProp("2026-06-15T14:30:00+02").storedOffset shouldBe UtcOffset(hours = 2)
        }

        "sub-second precision is preserved" {
            val property = dateProp("2026-06-15T14:30:00.123+02:00")

            property.wallClockDateTime shouldBe LocalDateTime(2026, 6, 15, 14, 30, 0, 123_000_000)
            property.utcInstant shouldBe Instant.parse("2026-06-15T12:30:00.123Z")
        }

        // ----------------------------------------
        // Z datetime
        // ----------------------------------------

        "Z datetime has a zero stored offset and identical wall-clock and UTC values" {
            val property = dateProp("2026-06-15T14:30:00.000Z")

            property.wallClockDateTime shouldBe LocalDateTime(2026, 6, 15, 14, 30)
            property.storedOffset shouldBe UtcOffset.ZERO
            property.utcInstant shouldBe Instant.parse("2026-06-15T14:30:00Z")
        }

        // ----------------------------------------
        // Date-only value
        // ----------------------------------------

        "date-only value has a date but no time, offset or instant" {
            val property = dateProp("2026-06-15")

            property.localDateValue shouldBe LocalDate(2026, 6, 15)
            property.wallClockDateTime shouldBe null
            property.storedOffset shouldBe null
            property.utcInstant shouldBe null
            property.stringValue shouldBe "2026-06-15"
        }

        "requireUtcInstant on a date-only value names the value and points at localDateValue" {
            val exception = shouldThrow<IllegalArgumentException> { dateProp("2026-06-15").requireUtcInstant() }

            exception.message!! shouldContain "2026-06-15"
            exception.message!! shouldContain "date-only"
            exception.message!! shouldContain "localDateValue"
        }

        // ----------------------------------------
        // Offset-less datetime — the value that caused the downstream bug
        // ----------------------------------------

        "offset-less datetime yields wall-clock digits but no offset and no instant" {
            val property = dateProp("2026-06-15T14:30:00")

            property.wallClockDateTime shouldBe LocalDateTime(2026, 6, 15, 14, 30)
            property.storedOffset shouldBe null
            property.utcInstant shouldBe null
        }

        "requireUtcInstant on an offset-less datetime says the offset is missing" {
            val exception =
                shouldThrow<IllegalArgumentException> { dateProp("2026-06-15T14:30:00").requireUtcInstant() }

            exception.message!! shouldContain "2026-06-15T14:30:00"
            exception.message!! shouldContain "no UTC offset"
        }

        // ----------------------------------------
        // Malformed values
        // ----------------------------------------

        "malformed value yields null from every accessor" {
            val property = dateProp("not-a-date")

            property.localDateValue shouldBe null
            property.wallClockDateTime shouldBe null
            property.storedOffset shouldBe null
            property.utcInstant shouldBe null
            property.stringValue shouldBe "not-a-date"
        }

        "malformed datetime with a well-formed offset still yields null" {
            val property = dateProp("2026-13-45T99:99:99+02:00")

            property.wallClockDateTime shouldBe null
            property.storedOffset shouldBe null
            property.utcInstant shouldBe null
        }

        "requireUtcInstant on a malformed value says it does not parse" {
            val exception = shouldThrow<IllegalArgumentException> { dateProp("not-a-date").requireUtcInstant() }

            exception.message!! shouldContain "not-a-date"
            exception.message!! shouldContain "not a valid ISO-8601"
        }

        // ----------------------------------------
        // Range ends
        // ----------------------------------------

        "end accessors answer the same three questions for the range end" {
            val property = dateProp("2026-06-15T14:30:00+02:00", end = "2026-06-15T16:00:00+02:00")

            property.endWallClockDateTime shouldBe LocalDateTime(2026, 6, 15, 16, 0)
            property.endStoredOffset shouldBe UtcOffset(hours = 2)
            property.endUtcInstant shouldBe Instant.parse("2026-06-15T14:00:00Z")
            property.requireEndUtcInstant() shouldBe Instant.parse("2026-06-15T14:00:00Z")
            property.endLocalDateTimeIn(TimeZone.UTC) shouldBe LocalDateTime(2026, 6, 15, 14, 0)
            property.endStringValue shouldBe "2026-06-15T16:00:00+02:00"
        }

        "end accessors are null when the value is not a range" {
            val property = dateProp("2026-06-15T14:30:00+02:00")

            property.endLocalDateValue shouldBe null
            property.endWallClockDateTime shouldBe null
            property.endStoredOffset shouldBe null
            property.endUtcInstant shouldBe null
            property.endStringValue shouldBe null
        }

        "end date-only range reads as dates" {
            val property = dateProp("2026-06-15", end = "2026-06-22")

            property.localDateValue shouldBe LocalDate(2026, 6, 15)
            property.endLocalDateValue shouldBe LocalDate(2026, 6, 22)
        }

        "requireEndUtcInstant names the end value" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    dateProp("2026-06-15T14:30:00+02:00", end = "2026-06-15T16:00:00").requireEndUtcInstant()
                }

            exception.message!! shouldContain "end"
            exception.message!! shouldContain "2026-06-15T16:00:00"
        }

        // ----------------------------------------
        // Auditing a stored offset against a named zone
        // ----------------------------------------

        "offsetIn reports the offset a zone would have had at the value's own local time" {
            dateProp("2026-10-24T13:00:00+02:00").offsetIn(oslo) shouldBe UtcOffset(hours = 2)
            dateProp("2026-10-25T13:00:00+01:00").offsetIn(oslo) shouldBe UtcOffset(hours = 1)
        }

        "comparing storedOffset with offsetIn detects a value that drifted to UTC" {
            val correct = dateProp("2026-06-15T13:00:00+02:00")
            val drifted = dateProp("2026-06-15T13:00:00Z")

            (correct.storedOffset == correct.offsetIn(oslo)) shouldBe true
            (drifted.storedOffset == drifted.offsetIn(oslo)) shouldBe false
        }

        "an ambiguous local time resolves to the earlier offset" {
            dateProp("2026-10-25T02:30:00+02:00").offsetIn(oslo) shouldBe UtcOffset(hours = 2)
        }

        "a nonexistent local time resolves past the spring gap" {
            dateProp("2026-03-29T02:30:00+01:00").offsetIn(oslo) shouldBe UtcOffset(hours = 2)
        }

        "endOffsetIn answers the same question for the range end" {
            val property = dateProp("2026-10-24T13:00:00+02:00", end = "2026-10-25T13:00:00+01:00")

            property.endOffsetIn(oslo) shouldBe UtcOffset(hours = 1)
        }

        "offsetIn is null without a parseable wall-clock value" {
            dateProp("2026-06-15").offsetIn(oslo) shouldBe null
            dateProp(null).offsetIn(oslo) shouldBe null
        }

        // ----------------------------------------
        // Absent value
        // ----------------------------------------

        "all accessors handle a null date gracefully" {
            val property = dateProp(null)

            property.localDateValue shouldBe null
            property.wallClockDateTime shouldBe null
            property.storedOffset shouldBe null
            property.utcInstant shouldBe null
            property.endLocalDateValue shouldBe null
            property.endWallClockDateTime shouldBe null
            property.endStoredOffset shouldBe null
            property.endUtcInstant shouldBe null
            property.stringValue shouldBe null
            property.endStringValue shouldBe null
            property.localDateTimeIn(TimeZone.UTC) shouldBe null
            property.endLocalDateTimeIn(TimeZone.UTC) shouldBe null
        }

        "requireUtcInstant on an absent value says so" {
            val exception = shouldThrow<IllegalArgumentException> { dateProp(null).requireUtcInstant() }

            exception.message!! shouldContain "no start value"
        }

        // ----------------------------------------
        // Deprecated aliases keep their old behaviour
        // ----------------------------------------

        "deprecated accessors delegate to their replacements" {
            val property = dateProp("2026-06-15T14:30:00+02:00", end = "2026-06-15T16:00:00+02:00")

            property.localDateTimeNaive shouldBe property.wallClockDateTime
            property.endLocalDateTimeNaive shouldBe property.endWallClockDateTime
            property.instantValue shouldBe property.utcInstant
            property.endInstantValue shouldBe property.endUtcInstant
            property.toLocalDateTime(TimeZone.UTC) shouldBe property.localDateTimeIn(TimeZone.UTC)
            property.endToLocalDateTime(TimeZone.UTC) shouldBe property.endLocalDateTimeIn(TimeZone.UTC)
        }
    })
