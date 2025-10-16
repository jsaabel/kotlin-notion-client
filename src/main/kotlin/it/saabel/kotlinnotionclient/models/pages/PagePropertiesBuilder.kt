@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.richtext.RichTextBuilder
import it.saabel.kotlinnotionclient.models.richtext.richText
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Builder class for creating page properties with a fluent DSL.
 *
 * This builder provides a convenient way to construct property maps for page creation
 * and updates, dramatically reducing boilerplate compared to manual property construction.
 *
 * ## Rich Text Property Patterns
 *
 * The `richText` property type (for database properties) supports three input patterns:
 *
 * 1. **Simple String** (most common for plain text):
 * ```kotlin
 * pageProperties {
 *     richText("Description", "Plain text description")
 * }
 * ```
 *
 * 2. **Pre-built RichText List** (when building programmatically):
 * ```kotlin
 * val formattedText = richText {
 *     text("Complex ")
 *     bold("formatted")
 *     text(" content")
 * }
 * pageProperties {
 *     richText("Description", formattedText)
 * }
 * ```
 *
 * 3. **Inline DSL Lambda** (for formatted database properties):
 * ```kotlin
 * pageProperties {
 *     richText("Description") {
 *         text("Created by ")
 *         userMention(userId)
 *         text(" on ")
 *         dateMention(LocalDate.now())
 *     }
 * }
 * ```
 *
 * **Note**: Title properties only support plain text. Use `title("Name", "text")` for titles.
 *
 * ## General Usage Example
 * ```kotlin
 * val properties = pageProperties {
 *     title("Name", "My Task")
 *     richText("Description", "Task description")
 *     number("Score", 85.5)
 *     checkbox("Completed", false)
 *     email("Contact", "user@example.com")
 *     select("Priority", "High")
 *     date("Due", "2024-12-31")
 * }
 * ```
 */
@PagePropertiesDslMarker
class PagePropertiesBuilder {
    private val properties = mutableMapOf<String, PagePropertyValue>()

    /**
     * Adds a title property value.
     *
     * @param name The property name
     * @param text The title text content
     */
    fun title(
        name: String,
        text: String,
    ) {
        properties[name] = PagePropertyValue.TitleValue.fromPlainText(text)
    }

    /**
     * Adds a title property value with rich text content.
     *
     * @param name The property name
     * @param richText The rich text content
     */
    fun title(
        name: String,
        richText: List<RichText>,
    ) {
        properties[name] = PagePropertyValue.TitleValue(title = richText)
    }

    /**
     * Adds a rich text property value.
     *
     * @param name The property name
     * @param text The text content
     */
    fun richText(
        name: String,
        text: String,
    ) {
        properties[name] = PagePropertyValue.RichTextValue.fromPlainText(text)
    }

    /**
     * Adds a rich text property value with rich text content.
     *
     * @param name The property name
     * @param richText The rich text content
     */
    fun richText(
        name: String,
        richText: List<RichText>,
    ) {
        properties[name] = PagePropertyValue.RichTextValue(richText = richText)
    }

    /**
     * Adds a rich text property value using the rich text DSL.
     *
     * This provides a consistent API with block content creation, allowing
     * inline rich text formatting for rich text properties.
     *
     * Example:
     * ```kotlin
     * properties {
     *     richText("Description") {
     *         text("Created by ")
     *         userMention(userId)
     *         text(" on ")
     *         dateMention(LocalDate.now())
     *     }
     * }
     * ```
     *
     * Note: For simple text, prefer `richText(name, "text")`.
     * Use this lambda form for moderately complex formatting with multiple
     * styles, links, or mentions. For highly complex content, consider using
     * block content instead of properties.
     *
     * @param name The property name
     * @param block The rich text DSL builder block
     */
    fun richText(
        name: String,
        block: RichTextBuilder.() -> Unit,
    ) {
        properties[name] = PagePropertyValue.RichTextValue(richText = richText(block))
    }

    /**
     * Adds a number property value.
     *
     * @param name The property name
     * @param number The numeric value (null for empty)
     */
    fun number(
        name: String,
        number: Double?,
    ) {
        properties[name] = PagePropertyValue.NumberValue(number = number)
    }

    /**
     * Adds a number property value with non-null number.
     *
     * @param name The property name
     * @param number The numeric value
     */
    fun number(
        name: String,
        number: Double,
    ) {
        properties[name] = PagePropertyValue.NumberValue(number = number)
    }

    /**
     * Adds a number property value with integer conversion.
     *
     * @param name The property name
     * @param number The integer value
     */
    fun number(
        name: String,
        number: Int,
    ) {
        properties[name] = PagePropertyValue.NumberValue(number = number.toDouble())
    }

    /**
     * Adds a checkbox property value.
     *
     * @param name The property name
     * @param checked The checkbox state
     */
    fun checkbox(
        name: String,
        checked: Boolean,
    ) {
        properties[name] = PagePropertyValue.CheckboxValue(checkbox = checked)
    }

    /**
     * Adds a URL property value.
     *
     * @param name The property name
     * @param url The URL (null for empty)
     */
    fun url(
        name: String,
        url: String?,
    ) {
        properties[name] = PagePropertyValue.UrlValue(url = url)
    }

    /**
     * Adds an email property value.
     *
     * @param name The property name
     * @param email The email address (null for empty)
     */
    fun email(
        name: String,
        email: String?,
    ) {
        properties[name] = PagePropertyValue.EmailValue(email = email)
    }

    /**
     * Adds a phone number property value.
     *
     * @param name The property name
     * @param phoneNumber The phone number (null for empty)
     */
    fun phoneNumber(
        name: String,
        phoneNumber: String?,
    ) {
        properties[name] = PagePropertyValue.PhoneNumberValue(phoneNumber = phoneNumber)
    }

    /**
     * Adds a select property value by option name.
     *
     * @param name The property name
     * @param optionName The name of the option to select (null for empty)
     */
    fun select(
        name: String,
        optionName: String?,
    ) {
        properties[name] =
            if (optionName != null) {
                PagePropertyValue.SelectValue.byName(optionName)
            } else {
                PagePropertyValue.SelectValue(select = null)
            }
    }

    /**
     * Adds a select property value with full SelectOption.
     *
     * @param name The property name
     * @param selectOption The select option
     */
    fun select(
        name: String,
        selectOption: SelectOption,
    ) {
        properties[name] = PagePropertyValue.SelectValue(select = selectOption)
    }

    /**
     * Adds a multi-select property value by option names.
     *
     * @param name The property name
     * @param optionNames The names of the options to select
     */
    fun multiSelect(
        name: String,
        vararg optionNames: String,
    ) {
        properties[name] = PagePropertyValue.MultiSelectValue.byNames(*optionNames)
    }

    /**
     * Adds a multi-select property value by option names list.
     *
     * @param name The property name
     * @param optionNames The names of the options to select
     */
    fun multiSelectFromList(
        name: String,
        optionNames: List<String>,
    ) {
        properties[name] = PagePropertyValue.MultiSelectValue.byNames(optionNames)
    }

    /**
     * Adds a multi-select property value with full SelectOption list.
     *
     * @param name The property name
     * @param selectOptions The select options
     */
    fun multiSelectFromOptions(
        name: String,
        selectOptions: List<SelectOption>,
    ) {
        properties[name] = PagePropertyValue.MultiSelectValue(multiSelect = selectOptions)
    }

    /**
     * Adds a date property value from a date string.
     *
     * @param name The property name
     * @param date The date string in ISO format (YYYY-MM-DD)
     */
    fun date(
        name: String,
        date: String?,
    ) {
        properties[name] =
            if (date != null) {
                PagePropertyValue.DateValue.fromDateString(date)
            } else {
                PagePropertyValue.DateValue(date = null)
            }
    }

    /**
     * Adds a datetime property value from a datetime string.
     *
     * @param name The property name
     * @param datetime The datetime string in ISO format (YYYY-MM-DDTHH:MM:SS or with timezone)
     */
    fun dateTime(
        name: String,
        datetime: String?,
    ) {
        properties[name] =
            if (datetime != null) {
                PagePropertyValue.DateValue.fromDateTimeString(datetime)
            } else {
                PagePropertyValue.DateValue(date = null)
            }
    }

    /**
     * Adds a date range property value.
     *
     * @param name The property name
     * @param startDate The start date string in ISO format (YYYY-MM-DD)
     * @param endDate The end date string in ISO format (YYYY-MM-DD)
     */
    fun dateRange(
        name: String,
        startDate: String,
        endDate: String,
    ) {
        properties[name] = PagePropertyValue.DateValue.fromDateRange(startDate, endDate)
    }

    /**
     * Adds a datetime range property value.
     *
     * @param name The property name
     * @param startDateTime The start datetime string in ISO format
     * @param endDateTime The end datetime string in ISO format
     */
    fun dateTimeRange(
        name: String,
        startDateTime: String,
        endDateTime: String,
    ) {
        properties[name] = PagePropertyValue.DateValue.fromDateTimeRange(startDateTime, endDateTime)
    }

    /**
     * Adds a date property value with timezone.
     *
     * @param name The property name
     * @param date The date string in ISO format (YYYY-MM-DD)
     * @param timeZone The timezone (e.g., "America/Los_Angeles", "UTC")
     */
    fun dateWithTimeZone(
        name: String,
        date: String,
        timeZone: String,
    ) {
        properties[name] = PagePropertyValue.DateValue.fromDateWithTimeZone(date, timeZone)
    }

    /**
     * Adds a datetime property value with timezone.
     *
     * @param name The property name
     * @param datetime The datetime string in ISO format
     * @param timeZone The timezone (e.g., "America/Los_Angeles", "UTC")
     */
    fun dateTimeWithTimeZone(
        name: String,
        datetime: String,
        timeZone: String,
    ) {
        properties[name] = PagePropertyValue.DateValue.fromDateTimeWithTimeZone(datetime, timeZone)
    }

    /**
     * Adds a date property value with full DateData.
     *
     * @param name The property name
     * @param dateData The date data
     */
    fun date(
        name: String,
        dateData: DateData,
    ) {
        properties[name] = PagePropertyValue.DateValue(date = dateData)
    }

    // ========================================
    // Typed date/datetime overloads using kotlinx-datetime
    // ========================================

    /**
     * Adds a date property value using LocalDate.
     *
     * @param name The property name
     * @param value The LocalDate value
     */
    fun date(
        name: String,
        value: LocalDate,
    ) {
        properties[name] = PagePropertyValue.DateValue.fromDateString(value.toString())
    }

    /**
     * Adds a datetime property value using LocalDateTime with timezone.
     *
     * @param name The property name
     * @param value The LocalDateTime value
     * @param timeZone The timezone (defaults to UTC)
     */
    fun dateTime(
        name: String,
        value: LocalDateTime,
        timeZone: TimeZone = TimeZone.UTC,
    ) {
        val instant = value.toInstant(timeZone)
        properties[name] = PagePropertyValue.DateValue.fromDateTimeString(instant.toString())
    }

    /**
     * Adds a datetime property value using Instant (timezone-unambiguous).
     *
     * @param name The property name
     * @param value The Instant value
     */
    fun dateTime(
        name: String,
        value: Instant,
    ) {
        properties[name] = PagePropertyValue.DateValue.fromDateTimeString(value.toString())
    }

    /**
     * Adds a date range property value using LocalDate with DSL.
     *
     * Example:
     * ```kotlin
     * dateRange("Project Duration") {
     *     start = LocalDate(2025, 3, 15)
     *     end = LocalDate(2025, 3, 22)
     * }
     * ```
     *
     * @param name The property name
     * @param block The date range builder configuration
     */
    fun dateRange(
        name: String,
        block: DateRangeBuilder.() -> Unit,
    ) {
        val (startDate, endDate) = DateRangeBuilder().apply(block).build()
        properties[name] =
            if (endDate != null) {
                PagePropertyValue.DateValue.fromDateRange(startDate, endDate)
            } else {
                PagePropertyValue.DateValue.fromDateString(startDate)
            }
    }

    /**
     * Adds a date range property value using LocalDate directly.
     *
     * @param name The property name
     * @param start The start date
     * @param end The end date (null for open-ended)
     */
    fun dateRange(
        name: String,
        start: LocalDate,
        end: LocalDate?,
    ) {
        properties[name] =
            if (end != null) {
                PagePropertyValue.DateValue.fromDateRange(start.toString(), end.toString())
            } else {
                PagePropertyValue.DateValue.fromDateString(start.toString())
            }
    }

    /**
     * Adds a datetime range property value using LocalDateTime with timezone and DSL.
     *
     * Example:
     * ```kotlin
     * dateTimeRange("Meeting", timeZone = TimeZone.of("America/New_York")) {
     *     start = LocalDateTime(2025, 3, 15, 14, 0)
     *     end = LocalDateTime(2025, 3, 15, 15, 30)
     * }
     * ```
     *
     * @param name The property name
     * @param timeZone The timezone for the datetime values
     * @param block The datetime range builder configuration
     */
    fun dateTimeRange(
        name: String,
        timeZone: TimeZone = TimeZone.UTC,
        block: LocalDateTimeRangeBuilder.() -> Unit,
    ) {
        val (startDateTime, endDateTime) = LocalDateTimeRangeBuilder(timeZone).apply(block).build()
        properties[name] =
            if (endDateTime != null) {
                PagePropertyValue.DateValue.fromDateTimeRange(startDateTime, endDateTime)
            } else {
                PagePropertyValue.DateValue.fromDateTimeString(startDateTime)
            }
    }

    /**
     * Adds a datetime range property value using LocalDateTime directly.
     *
     * @param name The property name
     * @param start The start datetime
     * @param end The end datetime (null for open-ended)
     * @param timeZone The timezone for the datetime values (defaults to UTC)
     */
    fun dateTimeRange(
        name: String,
        start: LocalDateTime,
        end: LocalDateTime?,
        timeZone: TimeZone = TimeZone.UTC,
    ) {
        val startInstant = start.toInstant(timeZone)
        val endInstant = end?.toInstant(timeZone)
        properties[name] =
            if (endInstant != null) {
                PagePropertyValue.DateValue.fromDateTimeRange(startInstant.toString(), endInstant.toString())
            } else {
                PagePropertyValue.DateValue.fromDateTimeString(startInstant.toString())
            }
    }

    /**
     * Adds a datetime range property value using Instant with DSL.
     *
     * Example:
     * ```kotlin
     * dateTimeRange("Deployment Window") {
     *     start = Instant.parse("2025-03-15T00:00:00Z")
     *     end = Instant.parse("2025-03-15T04:00:00Z")
     * }
     * ```
     *
     * @param name The property name
     * @param block The instant range builder configuration
     */
    fun dateTimeRange(
        name: String,
        block: InstantRangeBuilder.() -> Unit,
    ) {
        val (startInstant, endInstant) = InstantRangeBuilder().apply(block).build()
        properties[name] =
            if (endInstant != null) {
                PagePropertyValue.DateValue.fromDateTimeRange(startInstant, endInstant)
            } else {
                PagePropertyValue.DateValue.fromDateTimeString(startInstant)
            }
    }

    /**
     * Adds a datetime range property value using Instant directly.
     *
     * @param name The property name
     * @param start The start instant
     * @param end The end instant (null for open-ended)
     */
    fun dateTimeRange(
        name: String,
        start: Instant,
        end: Instant?,
    ) {
        properties[name] =
            if (end != null) {
                PagePropertyValue.DateValue.fromDateTimeRange(start.toString(), end.toString())
            } else {
                PagePropertyValue.DateValue.fromDateTimeString(start.toString())
            }
    }

    /**
     * Adds a people property value.
     *
     * @param name The property name
     * @param userIds The user IDs to reference
     */
    fun people(
        name: String,
        vararg userIds: String,
    ) {
        properties[name] =
            PagePropertyValue.PeopleValue(
                people = userIds.map { UserReference(id = it) },
            )
    }

    /**
     * Adds a people property value with UserReference list.
     *
     * @param name The property name
     * @param userReferences The user references
     */
    fun people(
        name: String,
        userReferences: List<UserReference>,
    ) {
        properties[name] = PagePropertyValue.PeopleValue(people = userReferences)
    }

    /**
     * Adds a relation property value.
     *
     * @param name The property name
     * @param pageIds The page IDs to reference
     */
    fun relation(
        name: String,
        vararg pageIds: String,
    ) {
        properties[name] =
            PagePropertyValue.RelationValue(
                relation = pageIds.map { PageReference(id = it) },
            )
    }

    /**
     * Adds a relation property value with PageReference list.
     *
     * @param name The property name
     * @param pageReferences The page references
     */
    fun relation(
        name: String,
        pageReferences: List<PageReference>,
    ) {
        properties[name] = PagePropertyValue.RelationValue(relation = pageReferences)
    }

    /**
     * Builds and returns the property map.
     *
     * @return The constructed property map
     */
    fun build(): Map<String, PagePropertyValue> = properties.toMap()
}

/**
 * DSL marker to prevent nested builder usage.
 */
@DslMarker
annotation class PagePropertiesDslMarker

/**
 * Creates a page properties map using the builder DSL.
 *
 * @param block The builder configuration block
 * @return The constructed property map
 */
fun pageProperties(block: PagePropertiesBuilder.() -> Unit): Map<String, PagePropertyValue> = PagePropertiesBuilder().apply(block).build()
