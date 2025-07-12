@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.pages

import no.saabelit.kotlinnotionclient.models.base.RichText

/**
 * Builder class for creating page properties with a fluent DSL.
 *
 * This builder provides a convenient way to construct property maps for page creation
 * and updates, dramatically reducing boilerplate compared to manual property construction.
 *
 * Example usage:
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
