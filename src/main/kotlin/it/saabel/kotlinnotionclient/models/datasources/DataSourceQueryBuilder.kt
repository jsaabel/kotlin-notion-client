@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.datasources

import it.saabel.kotlinnotionclient.models.base.EmptyObject
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Top-level DSL function for creating data source queries with a fluent API.
 *
 * This function provides a convenient entry point for building data source queries
 * using Kotlin's DSL syntax, making query construction more natural and readable.
 *
 * Example usage:
 * ```kotlin
 * val request = dataSourceQuery {
 *     filter {
 *         and(
 *             title("Task Name").contains("Important"),
 *             checkbox("Completed").equals(false),
 *             date("Due Date").after("2024-01-01")
 *         )
 *     }
 *     sortBy("Priority", SortDirection.DESCENDING)
 *     pageSize(50)
 * }
 * ```
 *
 * @param builder DSL builder lambda for constructing the query
 * @return DatabaseQueryRequest ready for API execution
 */
fun dataSourceQuery(builder: DataSourceQueryBuilder.() -> Unit): DataSourceQueryRequest = DataSourceQueryBuilder().apply(builder).build()

/**
 * Builder for constructing data source queries with a fluent API.
 *
 * This builder provides a type-safe, readable way to construct complex data source queries
 * with filters, sorts, and pagination. It follows a DSL pattern for ease of use.
 *
 * Example usage:
 * ```kotlin
 * val query = DataSourceQueryBuilder()
 *     .filter {
 *         and(
 *             title("Task Name").contains("Important"),
 *             checkbox("Completed").equals(false),
 *             date("Due Date").after("2024-01-01")
 *         )
 *     }
 *     .sortBy("Priority", SortDirection.DESCENDING)
 *     .pageSize(50)
 *     .build()
 * ```
 */
class DataSourceQueryBuilder {
    private var filter: DataSourceFilter? = null
    private var sorts: MutableList<DataSourceSort> = mutableListOf()
    private var startCursor: String? = null
    private var pageSize: Int? = null

    /**
     * Sets the filter for the query using a DSL builder.
     */
    fun filter(block: FilterBuilder.() -> DataSourceFilter): DataSourceQueryBuilder {
        this.filter = FilterBuilder().block()
        return this
    }

    /**
     * Adds a sort by property name and direction.
     */
    fun sortBy(
        propertyName: String,
        direction: SortDirection = SortDirection.ASCENDING,
    ): DataSourceQueryBuilder {
        sorts.add(DataSourceSort(property = propertyName, direction = direction))
        return this
    }

    /**
     * Adds a sort by timestamp (created_time or last_edited_time).
     */
    fun sortByTimestamp(
        timestamp: String,
        direction: SortDirection = SortDirection.ASCENDING,
    ): DataSourceQueryBuilder {
        sorts.add(DataSourceSort(timestamp = timestamp, direction = direction))
        return this
    }

    /**
     * Sets the starting cursor for pagination.
     */
    fun startCursor(cursor: String?): DataSourceQueryBuilder {
        this.startCursor = cursor
        return this
    }

    /**
     * Sets the page size (max 100).
     */
    fun pageSize(size: Int): DataSourceQueryBuilder {
        this.pageSize = size.coerceIn(1, 100)
        return this
    }

    /**
     * Builds the final query request.
     */
    fun build(): DataSourceQueryRequest =
        DataSourceQueryRequest(
            filter = filter,
            sorts = if (sorts.isEmpty()) null else sorts.toList(),
            startCursor = startCursor,
            pageSize = pageSize,
        )
}

/**
 * Builder for constructing data source filters with type-safe property accessors.
 */
class FilterBuilder {
    /**
     * Creates an AND filter with multiple conditions.
     */
    fun and(vararg conditions: DataSourceFilter): DataSourceFilter = DataSourceFilter(and = conditions.toList())

    /**
     * Creates an OR filter with multiple conditions.
     */
    fun or(vararg conditions: DataSourceFilter): DataSourceFilter = DataSourceFilter(or = conditions.toList())

    /**
     * Creates a title property filter builder.
     */
    fun title(propertyName: String): TitleFilterBuilder = TitleFilterBuilder(propertyName)

    /**
     * Creates a rich text property filter builder.
     */
    fun richText(propertyName: String): TextFilterBuilder = TextFilterBuilder(propertyName, "rich_text")

    /**
     * Creates a number property filter builder.
     */
    fun number(propertyName: String): NumberFilterBuilder = NumberFilterBuilder(propertyName)

    /**
     * Creates a select property filter builder.
     */
    fun select(propertyName: String): SelectFilterBuilder = SelectFilterBuilder(propertyName)

    /**
     * Creates a multi-select property filter builder.
     */
    fun multiSelect(propertyName: String): MultiSelectFilterBuilder = MultiSelectFilterBuilder(propertyName)

    /**
     * Creates a date property filter builder.
     */
    fun date(propertyName: String): DateFilterBuilder = DateFilterBuilder(propertyName)

    /**
     * Creates a checkbox property filter builder.
     */
    fun checkbox(propertyName: String): CheckboxFilterBuilder = CheckboxFilterBuilder(propertyName)

    /**
     * Creates a URL property filter builder.
     */
    fun url(propertyName: String): TextFilterBuilder = TextFilterBuilder(propertyName, "url")

    /**
     * Creates an email property filter builder.
     */
    fun email(propertyName: String): TextFilterBuilder = TextFilterBuilder(propertyName, "email")

    /**
     * Creates a phone number property filter builder.
     */
    fun phoneNumber(propertyName: String): TextFilterBuilder = TextFilterBuilder(propertyName, "phone_number")
}

/**
 * Builder for title property filters.
 */
class TitleFilterBuilder(
    private val propertyName: String,
) {
    fun equals(value: String): DataSourceFilter = createFilter(PropertyCondition(equals = value))

    fun doesNotEqual(value: String): DataSourceFilter = createFilter(PropertyCondition(doesNotEqual = value))

    fun contains(value: String): DataSourceFilter = createFilter(PropertyCondition(contains = value))

    fun doesNotContain(value: String): DataSourceFilter = createFilter(PropertyCondition(doesNotContain = value))

    fun startsWith(value: String): DataSourceFilter = createFilter(PropertyCondition(startsWith = value))

    fun endsWith(value: String): DataSourceFilter = createFilter(PropertyCondition(endsWith = value))

    fun isEmpty(): DataSourceFilter = createFilter(PropertyCondition(isEmpty = true))

    fun isNotEmpty(): DataSourceFilter = createFilter(PropertyCondition(isNotEmpty = true))

    private fun createFilter(condition: PropertyCondition): DataSourceFilter =
        DataSourceFilter(
            property = propertyName,
            title = condition,
        )
}

/**
 * Builder for text-based property filters (rich_text, url, email, phone_number).
 */
class TextFilterBuilder(
    private val propertyName: String,
    private val type: String,
) {
    fun equals(value: String): DataSourceFilter = createFilter(PropertyCondition(equals = value))

    fun doesNotEqual(value: String): DataSourceFilter = createFilter(PropertyCondition(doesNotEqual = value))

    fun contains(value: String): DataSourceFilter = createFilter(PropertyCondition(contains = value))

    fun doesNotContain(value: String): DataSourceFilter = createFilter(PropertyCondition(doesNotContain = value))

    fun startsWith(value: String): DataSourceFilter = createFilter(PropertyCondition(startsWith = value))

    fun endsWith(value: String): DataSourceFilter = createFilter(PropertyCondition(endsWith = value))

    fun isEmpty(): DataSourceFilter = createFilter(PropertyCondition(isEmpty = true))

    fun isNotEmpty(): DataSourceFilter = createFilter(PropertyCondition(isNotEmpty = true))

    private fun createFilter(condition: PropertyCondition): DataSourceFilter =
        DataSourceFilter(
            property = propertyName,
            title = if (type == "title") condition else null,
            richText = if (type == "rich_text") condition else null,
            url = if (type == "url") condition else null,
            email = if (type == "email") condition else null,
            phoneNumber = if (type == "phone_number") condition else null,
        )
}

/**
 * Builder for number property filters.
 */
class NumberFilterBuilder(
    private val propertyName: String,
) {
    fun equals(value: Number): DataSourceFilter = createFilter(NumberCondition(equals = value.toDouble()))

    fun doesNotEqual(value: Number): DataSourceFilter = createFilter(NumberCondition(doesNotEqual = value.toDouble()))

    fun greaterThan(value: Number): DataSourceFilter = createFilter(NumberCondition(greaterThan = value.toDouble()))

    fun lessThan(value: Number): DataSourceFilter = createFilter(NumberCondition(lessThan = value.toDouble()))

    fun greaterThanOrEqualTo(value: Number): DataSourceFilter = createFilter(NumberCondition(greaterThanOrEqualTo = value.toDouble()))

    fun lessThanOrEqualTo(value: Number): DataSourceFilter = createFilter(NumberCondition(lessThanOrEqualTo = value.toDouble()))

    fun isEmpty(): DataSourceFilter = createFilter(NumberCondition(isEmpty = true))

    fun isNotEmpty(): DataSourceFilter = createFilter(NumberCondition(isNotEmpty = true))

    private fun createFilter(condition: NumberCondition): DataSourceFilter =
        DataSourceFilter(
            property = propertyName,
            number = condition,
        )
}

/**
 * Builder for select property filters.
 */
class SelectFilterBuilder(
    private val propertyName: String,
) {
    fun equals(value: String): DataSourceFilter = createFilter(SelectCondition(equals = value))

    fun doesNotEqual(value: String): DataSourceFilter = createFilter(SelectCondition(doesNotEqual = value))

    fun isEmpty(): DataSourceFilter = createFilter(SelectCondition(isEmpty = true))

    fun isNotEmpty(): DataSourceFilter = createFilter(SelectCondition(isNotEmpty = true))

    private fun createFilter(condition: SelectCondition): DataSourceFilter =
        DataSourceFilter(
            property = propertyName,
            select = condition,
        )
}

/**
 * Builder for multi-select property filters.
 */
class MultiSelectFilterBuilder(
    private val propertyName: String,
) {
    fun contains(value: String): DataSourceFilter = createFilter(MultiSelectCondition(contains = value))

    fun doesNotContain(value: String): DataSourceFilter = createFilter(MultiSelectCondition(doesNotContain = value))

    fun isEmpty(): DataSourceFilter = createFilter(MultiSelectCondition(isEmpty = true))

    fun isNotEmpty(): DataSourceFilter = createFilter(MultiSelectCondition(isNotEmpty = true))

    private fun createFilter(condition: MultiSelectCondition): DataSourceFilter =
        DataSourceFilter(
            property = propertyName,
            multiSelect = condition,
        )
}

/**
 * Builder for date property filters.
 */
class DateFilterBuilder(
    private val propertyName: String,
) {
    // String-based methods (existing, for backward compatibility)
    fun equals(date: String): DataSourceFilter = createFilter(DateCondition(equals = date))

    fun before(date: String): DataSourceFilter = createFilter(DateCondition(before = date))

    fun after(date: String): DataSourceFilter = createFilter(DateCondition(after = date))

    fun onOrBefore(date: String): DataSourceFilter = createFilter(DateCondition(onOrBefore = date))

    fun onOrAfter(date: String): DataSourceFilter = createFilter(DateCondition(onOrAfter = date))

    // Typed overloads using kotlinx-datetime

    /** Filter for dates equal to the given LocalDate. */
    fun equals(date: LocalDate): DataSourceFilter = equals(date.toString())

    /** Filter for dates before the given LocalDate. */
    fun before(date: LocalDate): DataSourceFilter = before(date.toString())

    /** Filter for dates after the given LocalDate. */
    fun after(date: LocalDate): DataSourceFilter = after(date.toString())

    /** Filter for dates on or before the given LocalDate. */
    fun onOrBefore(date: LocalDate): DataSourceFilter = onOrBefore(date.toString())

    /** Filter for dates on or after the given LocalDate. */
    fun onOrAfter(date: LocalDate): DataSourceFilter = onOrAfter(date.toString())

    /** Filter for datetimes equal to the given LocalDateTime in the specified timezone. */
    fun equals(
        dateTime: LocalDateTime,
        timeZone: TimeZone = TimeZone.UTC,
    ): DataSourceFilter = equals(dateTime.toInstant(timeZone).toString())

    /** Filter for datetimes before the given LocalDateTime in the specified timezone. */
    fun before(
        dateTime: LocalDateTime,
        timeZone: TimeZone = TimeZone.UTC,
    ): DataSourceFilter = before(dateTime.toInstant(timeZone).toString())

    /** Filter for datetimes after the given LocalDateTime in the specified timezone. */
    fun after(
        dateTime: LocalDateTime,
        timeZone: TimeZone = TimeZone.UTC,
    ): DataSourceFilter = after(dateTime.toInstant(timeZone).toString())

    /** Filter for datetimes on or before the given LocalDateTime in the specified timezone. */
    fun onOrBefore(
        dateTime: LocalDateTime,
        timeZone: TimeZone = TimeZone.UTC,
    ): DataSourceFilter = onOrBefore(dateTime.toInstant(timeZone).toString())

    /** Filter for datetimes on or after the given LocalDateTime in the specified timezone. */
    fun onOrAfter(
        dateTime: LocalDateTime,
        timeZone: TimeZone = TimeZone.UTC,
    ): DataSourceFilter = onOrAfter(dateTime.toInstant(timeZone).toString())

    /** Filter for instants equal to the given Instant. */
    fun equals(instant: Instant): DataSourceFilter = equals(instant.toString())

    /** Filter for instants before the given Instant. */
    fun before(instant: Instant): DataSourceFilter = before(instant.toString())

    /** Filter for instants after the given Instant. */
    fun after(instant: Instant): DataSourceFilter = after(instant.toString())

    /** Filter for instants on or before the given Instant. */
    fun onOrBefore(instant: Instant): DataSourceFilter = onOrBefore(instant.toString())

    /** Filter for instants on or after the given Instant. */
    fun onOrAfter(instant: Instant): DataSourceFilter = onOrAfter(instant.toString())

    // Condition-based filters (no date parameter needed)

    fun isEmpty(): DataSourceFilter = createFilter(DateCondition(isEmpty = true))

    fun isNotEmpty(): DataSourceFilter = createFilter(DateCondition(isNotEmpty = true))

    fun pastWeek(): DataSourceFilter = createFilter(DateCondition(pastWeek = EmptyObject()))

    fun pastMonth(): DataSourceFilter = createFilter(DateCondition(pastMonth = EmptyObject()))

    fun pastYear(): DataSourceFilter = createFilter(DateCondition(pastYear = EmptyObject()))

    fun nextWeek(): DataSourceFilter = createFilter(DateCondition(nextWeek = EmptyObject()))

    fun nextMonth(): DataSourceFilter = createFilter(DateCondition(nextMonth = EmptyObject()))

    fun nextYear(): DataSourceFilter = createFilter(DateCondition(nextYear = EmptyObject()))

    private fun createFilter(condition: DateCondition): DataSourceFilter =
        DataSourceFilter(
            property = propertyName,
            date = condition,
        )
}

/**
 * Builder for checkbox property filters.
 */
class CheckboxFilterBuilder(
    private val propertyName: String,
) {
    fun equals(value: Boolean): DataSourceFilter = createFilter(CheckboxCondition(equals = value))

    fun doesNotEqual(value: Boolean): DataSourceFilter = createFilter(CheckboxCondition(doesNotEqual = value))

    private fun createFilter(condition: CheckboxCondition): DataSourceFilter =
        DataSourceFilter(
            property = propertyName,
            checkbox = condition,
        )
}
