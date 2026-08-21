package it.saabel.kotlinnotionclient.models.dates

import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Formatting and validation for date strings on the **write side** of the API.
 *
 * Notion's behaviour, measured by `TimezoneIntegrationTest` against the live API:
 * - A datetime string with no UTC offset and no `time_zone` field is read as **UTC**,
 *   silently reassigning the instant while preserving the wall clock. This is the
 *   failure mode that broke a downstream consumer, so [validateDateString] rejects it.
 * - A datetime string that carries an offset is preserved exactly as sent.
 * - A naive local string plus a named `time_zone` is resolved by Notion per-instant,
 *   DST included — but combining an offset (or `Z`) **with** a `time_zone` is
 *   misinterpreted (Notion strips the offset and re-applies the zone), so that
 *   combination is rejected too.
 * - A `time_zone` on a date-only value has no time to interpret and is rejected.
 */
internal object NotionDateStrings {
    /** Matches the UTC-offset suffix of an ISO datetime: `Z`, `+01`, `+0100` or `+01:00`. */
    private val OFFSET_SUFFIX_REGEX = Regex("""(?:[Zz]|[+-]\d{2}(?::?\d{2})?)$""")

    /**
     * Renders "this local time, in this zone" as an offset-bearing ISO string,
     * e.g. `2026-10-25T13:00:00+01:00` — never a `Z` instant, and never a naive string.
     *
     * The offset is the one [timeZone] has at the value's **own** local date, so values
     * on either side of a DST changeover get different offsets. Resolution of edge
     * cases follows the IANA tz database via kotlinx-datetime — which is also what
     * Notion itself does when resolving a named `time_zone`:
     * - an **ambiguous** local time (clocks fell back, the time occurs twice) resolves
     *   to the earlier of the two instants;
     * - a **nonexistent** local time (clocks sprang forward over it) is shifted
     *   forward by the length of the gap, moving the wall clock rather than the offset.
     */
    fun zonedDateTimeString(
        value: LocalDateTime,
        timeZone: TimeZone,
    ): String {
        val instant = value.toInstant(timeZone)
        // Re-derive the wall clock so a nonexistent local time comes out gap-shifted
        // (e.g. Europe/Oslo 2026-03-29T02:30 → 03:30+02:00), matching Notion's own policy.
        val resolved = instant.toLocalDateTime(timeZone)
        return localDateTimeString(resolved) + offsetString(timeZone.offsetAt(instant))
    }

    /**
     * Validates a date or datetime string (with its optional `time_zone` companion)
     * before it is sent to Notion. Throws [IllegalArgumentException] naming the
     * problem and the fix; accepts anything Notion stores faithfully.
     */
    fun validateDateString(
        value: String,
        timeZone: String? = null,
        field: String = "date",
    ) {
        if (value.contains('T')) {
            val offset = OFFSET_SUFFIX_REGEX.find(value)?.value
            require(!(offset == null && timeZone == null)) {
                "Datetime string '$value' for '$field' has a time component but neither a UTC offset nor a " +
                    "time_zone. Notion reads offset-less datetimes as UTC, silently moving the instant while " +
                    "keeping the wall clock. Either append an offset (e.g. '$value+02:00'), pass a " +
                    "kotlinx-datetime LocalDateTime with a TimeZone, or send the naive string together with a " +
                    "time_zone id."
            }
            require(!(offset != null && timeZone != null)) {
                "Datetime string '$value' for '$field' carries the UTC offset '$offset' and a time_zone " +
                    "('$timeZone') was also given. Notion misinterprets that combination — it strips the offset " +
                    "and re-applies the zone to the remaining digits. Send either the offset-bearing string " +
                    "alone, or the naive local string plus the time_zone."
            }
        } else {
            require(timeZone == null) {
                "Date-only value '$value' for '$field' was given the time_zone '$timeZone'. A time_zone only " +
                    "applies to values with a time component; drop the time_zone or send a datetime."
            }
        }
    }

    /**
     * Validates that [id] is a real IANA time-zone id, throwing [IllegalArgumentException]
     * with the offending value if not.
     */
    fun validateTimeZoneId(id: String) {
        try {
            TimeZone.of(id)
        } catch (e: IllegalTimeZoneException) {
            throw IllegalArgumentException(
                "Unknown time_zone id '$id'. Use an IANA zone id such as \"Europe/Oslo\" or \"America/New_York\".",
                e,
            )
        }
    }

    private fun localDateTimeString(value: LocalDateTime): String {
        val iso = value.toString()
        // LocalDateTime.toString() drops the seconds when they are zero; Notion always
        // carries them, so normalise to at least second precision.
        return if (iso.substringAfter('T').count { it == ':' } == 1) "$iso:00" else iso
    }

    private fun offsetString(offset: UtcOffset): String {
        val totalMinutes = offset.totalSeconds / 60
        val sign = if (totalMinutes < 0) "-" else "+"
        val abs = if (totalMinutes < 0) -totalMinutes else totalMinutes
        return "$sign${(abs / 60).toString().padStart(2, '0')}:${(abs % 60).toString().padStart(2, '0')}"
    }
}
