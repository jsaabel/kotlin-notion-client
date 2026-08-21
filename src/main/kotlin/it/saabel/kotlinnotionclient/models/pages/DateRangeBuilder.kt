@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.dates.NotionDateStrings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * DSL marker for date range builders to prevent nested usage.
 */
@DslMarker
annotation class DateRangeDslMarker

/**
 * Builder for constructing date ranges using LocalDate.
 *
 * Example usage:
 * ```kotlin
 * dateRange("Project Duration") {
 *     start = LocalDate(2025, 3, 15)
 *     end = LocalDate(2025, 3, 22)
 * }
 * ```
 */
@DateRangeDslMarker
class DateRangeBuilder {
    /**
     * The start date of the range.
     */
    var start: LocalDate? = null

    /**
     * The end date of the range (null for open-ended).
     */
    var end: LocalDate? = null

    /**
     * Builds the date range as ISO-8601 formatted strings.
     *
     * @return Pair of (startDate, endDate) as ISO-8601 strings
     * @throws IllegalStateException if start date is not set
     */
    fun build(): Pair<String, String?> {
        val startDate = start ?: throw IllegalStateException("Start date must be set")
        return startDate.toString() to end?.toString()
    }
}

/**
 * Builder for constructing datetime ranges using LocalDateTime with timezone.
 *
 * Example usage:
 * ```kotlin
 * dateTimeRange("Meeting", timeZone = TimeZone.of("America/New_York")) {
 *     start = LocalDateTime(2025, 3, 15, 14, 0)
 *     end = LocalDateTime(2025, 3, 15, 15, 30)
 * }
 * ```
 */
@DateRangeDslMarker
class LocalDateTimeRangeBuilder(
    private val timeZone: TimeZone,
) {
    /**
     * The start datetime of the range.
     */
    var start: LocalDateTime? = null

    /**
     * The end datetime of the range (null for open-ended).
     */
    var end: LocalDateTime? = null

    /**
     * Builds the datetime range as offset-bearing ISO-8601 strings.
     *
     * Each end keeps its wall-clock digits and gets the zone's UTC offset at its own
     * local date — the values are not converted to UTC instants, and a range may span
     * a DST changeover with different offsets on each end.
     *
     * @return Pair of (startDateTime, endDateTime) as offset-bearing ISO-8601 strings
     * @throws IllegalStateException if start datetime is not set
     */
    fun build(): Pair<String, String?> {
        val startDateTime = start ?: throw IllegalStateException("Start datetime must be set")
        return NotionDateStrings.zonedDateTimeString(startDateTime, timeZone) to
            end?.let { NotionDateStrings.zonedDateTimeString(it, timeZone) }
    }
}

/**
 * Builder for constructing datetime ranges using Instant (timezone-unambiguous).
 *
 * Example usage:
 * ```kotlin
 * dateTimeRange("Deployment Window") {
 *     start = Instant.parse("2025-03-15T00:00:00Z")
 *     end = Instant.parse("2025-03-15T04:00:00Z")
 * }
 * ```
 */
@DateRangeDslMarker
class InstantRangeBuilder {
    /**
     * The start instant of the range.
     */
    var start: Instant? = null

    /**
     * The end instant of the range (null for open-ended).
     */
    var end: Instant? = null

    /**
     * Builds the instant range as ISO-8601 formatted strings.
     *
     * @return Pair of (startInstant, endInstant) as ISO-8601 strings
     * @throws IllegalStateException if start instant is not set
     */
    fun build(): Pair<String, String?> {
        val startInstant = start ?: throw IllegalStateException("Start instant must be set")
        return startInstant.toString() to end?.toString()
    }
}
