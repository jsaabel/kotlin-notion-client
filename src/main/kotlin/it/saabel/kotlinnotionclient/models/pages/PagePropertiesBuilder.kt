@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.dates.NotionDateStrings
import it.saabel.kotlinnotionclient.models.richtext.RichTextBuilder
import it.saabel.kotlinnotionclient.models.richtext.richText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

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
     * Adds a status property value by option name.
     *
     * @param name The property name
     * @param optionName The name of the status option to set (null to clear)
     */
    fun status(
        name: String,
        optionName: String?,
    ) {
        properties[name] =
            if (optionName != null) {
                PagePropertyValue.StatusValue(status = StatusOption(name = optionName))
            } else {
                PagePropertyValue.StatusValue(status = null)
            }
    }

    /**
     * Adds a date property value from a date string.
     *
     * @param name The property name
     * @param date The date string in ISO format (YYYY-MM-DD)
     * @throws IllegalArgumentException for a datetime string with no UTC offset —
     *         Notion would silently read it as UTC
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
     * The string must carry a UTC offset (e.g. `2026-06-15T14:30:00+02:00` or `...Z`);
     * for a wall-clock time in a named zone, use [dateTime] with a
     * [LocalDateTime] and [TimeZone], or [dateTimeWithTimeZone].
     *
     * @param name The property name
     * @param datetime The datetime string in ISO format, including a UTC offset
     * @throws IllegalArgumentException for a datetime string with no UTC offset —
     *         Notion would silently read it as UTC
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
     * Both strings must carry a UTC offset; each end keeps the offset it was sent with,
     * so a range may legitimately span a DST changeover with different offsets.
     *
     * @param name The property name
     * @param startDateTime The start datetime string in ISO format, including a UTC offset
     * @param endDateTime The end datetime string in ISO format, including a UTC offset
     * @throws IllegalArgumentException for a datetime string with no UTC offset —
     *         Notion would silently read it as UTC
     */
    fun dateTimeRange(
        name: String,
        startDateTime: String,
        endDateTime: String,
    ) {
        properties[name] = PagePropertyValue.DateValue.fromDateTimeRange(startDateTime, endDateTime)
    }

    /**
     * Adds a datetime property value from a **naive** local datetime string plus a named
     * time zone. Notion resolves the zone's UTC offset at that local date (DST included)
     * and stores an offset-bearing value; the named zone itself is not preserved.
     *
     * Prefer the typed [dateTime] overload with a [LocalDateTime] and [TimeZone], which
     * resolves the offset locally to the same result.
     *
     * @param name The property name
     * @param datetime The naive local datetime string (e.g. `2026-06-15T14:30:00` — no offset,
     *        since Notion misinterprets an offset combined with a time_zone)
     * @param timeZone The IANA zone id (e.g. "America/New_York", "Europe/Oslo")
     * @throws IllegalArgumentException for a date-only or offset-bearing [datetime],
     *         or an unknown [timeZone] id
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
     * @throws IllegalArgumentException for a start or end datetime with neither a UTC offset
     *         nor a time_zone, a time_zone on a date-only value, or an unknown time_zone id
     */
    fun date(
        name: String,
        dateData: DateData,
    ) {
        NotionDateStrings.validateDateString(dateData.start, timeZone = dateData.timeZone, field = "start")
        dateData.end?.let { NotionDateStrings.validateDateString(it, timeZone = dateData.timeZone, field = "end") }
        dateData.timeZone?.let { NotionDateStrings.validateTimeZoneId(it) }
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
     * Adds a datetime property value meaning "this wall-clock time, in this zone".
     *
     * The value is written as an offset-bearing string (e.g. `2026-10-25T13:00:00+01:00`),
     * preserving the wall clock exactly as given — it is **not** converted to a UTC
     * instant. The offset is resolved at the value's own local date, so DST is handled
     * per value: Europe/Oslo 13:00 is `+02:00` on 2026-10-24 and `+01:00` on 2026-10-25.
     * An ambiguous local time (clocks fell back) resolves to the earlier instant; a
     * nonexistent one (clocks sprang forward) is shifted forward by the gap — the same
     * policy Notion itself applies when resolving a named time_zone.
     *
     * For an absolute point in time, use the [Instant] overload instead.
     *
     * @param name The property name
     * @param value The wall-clock datetime
     * @param timeZone The zone the wall clock belongs to — required, because a
     *        [LocalDateTime] alone does not identify a point in time
     */
    fun dateTime(
        name: String,
        value: LocalDateTime,
        timeZone: TimeZone,
    ) {
        properties[name] =
            PagePropertyValue.DateValue.fromDateTimeString(
                NotionDateStrings.zonedDateTimeString(value, timeZone),
            )
    }

    /**
     * Adds a datetime property value from an absolute instant, written as UTC (`...Z`).
     *
     * Use this when the value is a point in time — a deployment, a measurement — rather
     * than a wall-clock time somewhere. For "13:00 in Oslo", use the [LocalDateTime]
     * overload, which preserves the local digits and offset.
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
     * Both ends are written as offset-bearing local times (see the [LocalDateTime]
     * overload of [dateTime] for the semantics); each end's offset is resolved at its
     * own local date, so a range may span a DST changeover.
     *
     * @param name The property name
     * @param timeZone The zone the wall-clock values belong to — required
     * @param block The datetime range builder configuration
     */
    fun dateTimeRange(
        name: String,
        timeZone: TimeZone,
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
     * Both ends are written as offset-bearing local times (see the [LocalDateTime]
     * overload of [dateTime] for the semantics); each end's offset is resolved at its
     * own local date, so a range may span a DST changeover.
     *
     * @param name The property name
     * @param start The start wall-clock datetime
     * @param end The end wall-clock datetime (null for open-ended)
     * @param timeZone The zone the wall-clock values belong to — required
     */
    fun dateTimeRange(
        name: String,
        start: LocalDateTime,
        end: LocalDateTime?,
        timeZone: TimeZone,
    ) {
        val startString = NotionDateStrings.zonedDateTimeString(start, timeZone)
        val endString = end?.let { NotionDateStrings.zonedDateTimeString(it, timeZone) }
        properties[name] =
            if (endString != null) {
                PagePropertyValue.DateValue.fromDateTimeRange(startString, endString)
            } else {
                PagePropertyValue.DateValue.fromDateTimeString(startString)
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
     * Adds a "Files & media" property value using the files DSL.
     *
     * Example:
     * ```kotlin
     * files("Attachments") {
     *     upload("upload-abc-123", name = "report.pdf")
     *     external("Spec doc", "https://example.com/spec.pdf")
     * }
     * ```
     *
     * @param name The property name
     * @param block The files DSL builder block
     */
    fun files(
        name: String,
        block: FilesBuilder.() -> Unit,
    ) {
        properties[name] = PagePropertyValue.FilesValue(files = FilesBuilder().apply(block).build())
    }

    /**
     * Adds a "Files & media" property value from a terse list of file objects.
     *
     * @param name The property name
     * @param files The file objects to attach
     */
    fun files(
        name: String,
        vararg files: FileObject,
    ) {
        properties[name] = PagePropertyValue.FilesValue(files = files.toList())
    }

    /**
     * Adds a "Files & media" property value from a programmatically-built list.
     *
     * @param name The property name
     * @param files The file objects to attach
     */
    fun files(
        name: String,
        files: List<FileObject>,
    ) {
        properties[name] = PagePropertyValue.FilesValue(files = files)
    }

    /**
     * Marks a page as verified in a wiki database.
     *
     * Optionally provide an ISO 8601 [start] date/datetime for when the verification begins,
     * and an [end] date/datetime after which it expires.
     *
     * @param name The property name (typically "Verification")
     * @param start Optional ISO 8601 start date/datetime string for the verification period
     * @param end Optional ISO 8601 end date/datetime string after which the verification expires
     * @throws IllegalArgumentException for a datetime string with no UTC offset —
     *         Notion would silently read it as UTC
     */
    fun verify(
        name: String,
        start: String? = null,
        end: String? = null,
    ) {
        start?.let { NotionDateStrings.validateDateString(it, field = "start") }
        end?.let { NotionDateStrings.validateDateString(it, field = "end") }
        val date = if (start != null) DateData(start = start, end = end) else null
        properties[name] = PagePropertyValue.VerificationValue(VerificationRequest(state = "verified", date = date))
    }

    /**
     * Marks a page as unverified in a wiki database.
     *
     * @param name The property name (typically "Verification")
     */
    fun unverify(name: String) {
        properties[name] = PagePropertyValue.VerificationValue(VerificationRequest(state = "unverified"))
    }

    /**
     * Sets a property to a pre-built [PagePropertyValue], bypassing the builder's
     * validation.
     *
     * This is the deliberate escape hatch for values the typed methods reject —
     * e.g. reproducing Notion's raw behaviour for an offset-less datetime in a test.
     * Prefer the typed methods everywhere else.
     *
     * @param name The property name
     * @param value The raw property value, sent exactly as given
     */
    fun property(
        name: String,
        value: PagePropertyValue,
    ) {
        properties[name] = value
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
