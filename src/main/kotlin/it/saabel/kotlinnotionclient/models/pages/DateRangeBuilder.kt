@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
     * Builds the datetime range as ISO-8601 formatted strings with timezone.
     *
     * @return Pair of (startDateTime, endDateTime) as ISO-8601 strings with timezone
     * @throws IllegalStateException if start datetime is not set
     */
    fun build(): Pair<String, String?> {
        val startDateTime = start ?: throw IllegalStateException("Start datetime must be set")
        val startInstant = startDateTime.toInstant(timeZone)
        val endInstant = end?.toInstant(timeZone)

        return startInstant.toString() to endInstant?.toString()
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
