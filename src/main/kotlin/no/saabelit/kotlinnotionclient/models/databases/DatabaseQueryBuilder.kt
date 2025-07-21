@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.databases

import no.saabelit.kotlinnotionclient.models.base.EmptyObject

/**
 * Top-level DSL function for creating database queries with a fluent API.
 *
 * This function provides a convenient entry point for building database queries
 * using Kotlin's DSL syntax, making query construction more natural and readable.
 *
 * Example usage:
 * ```kotlin
 * val request = databaseQuery {
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
fun databaseQuery(builder: DatabaseQueryBuilder.() -> Unit): DatabaseQueryRequest = DatabaseQueryBuilder().apply(builder).build()

/**
 * Builder for constructing database queries with a fluent API.
 *
 * This builder provides a type-safe, readable way to construct complex database queries
 * with filters, sorts, and pagination. It follows a DSL pattern for ease of use.
 *
 * Example usage:
 * ```kotlin
 * val query = DatabaseQueryBuilder()
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
class DatabaseQueryBuilder {
    private var filter: DatabaseFilter? = null
    private var sorts: MutableList<DatabaseSort> = mutableListOf()
    private var startCursor: String? = null
    private var pageSize: Int? = null

    /**
     * Sets the filter for the query using a DSL builder.
     */
    fun filter(block: FilterBuilder.() -> DatabaseFilter): DatabaseQueryBuilder {
        this.filter = FilterBuilder().block()
        return this
    }

    /**
     * Adds a sort by property name and direction.
     */
    fun sortBy(
        propertyName: String,
        direction: SortDirection = SortDirection.ASCENDING,
    ): DatabaseQueryBuilder {
        sorts.add(DatabaseSort(property = propertyName, direction = direction))
        return this
    }

    /**
     * Adds a sort by timestamp (created_time or last_edited_time).
     */
    fun sortByTimestamp(
        timestamp: String,
        direction: SortDirection = SortDirection.ASCENDING,
    ): DatabaseQueryBuilder {
        sorts.add(DatabaseSort(timestamp = timestamp, direction = direction))
        return this
    }

    /**
     * Sets the starting cursor for pagination.
     */
    fun startCursor(cursor: String?): DatabaseQueryBuilder {
        this.startCursor = cursor
        return this
    }

    /**
     * Sets the page size (max 100).
     */
    fun pageSize(size: Int): DatabaseQueryBuilder {
        this.pageSize = size.coerceIn(1, 100)
        return this
    }

    /**
     * Builds the final query request.
     */
    fun build(): DatabaseQueryRequest =
        DatabaseQueryRequest(
            filter = filter,
            sorts = if (sorts.isEmpty()) null else sorts.toList(),
            startCursor = startCursor,
            pageSize = pageSize,
        )
}

/**
 * Builder for constructing database filters with type-safe property accessors.
 */
class FilterBuilder {
    /**
     * Creates an AND filter with multiple conditions.
     */
    fun and(vararg conditions: DatabaseFilter): DatabaseFilter = DatabaseFilter(and = conditions.toList())

    /**
     * Creates an OR filter with multiple conditions.
     */
    fun or(vararg conditions: DatabaseFilter): DatabaseFilter = DatabaseFilter(or = conditions.toList())

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
    fun equals(value: String): DatabaseFilter = createFilter(PropertyCondition(equals = value))

    fun doesNotEqual(value: String): DatabaseFilter = createFilter(PropertyCondition(doesNotEqual = value))

    fun contains(value: String): DatabaseFilter = createFilter(PropertyCondition(contains = value))

    fun doesNotContain(value: String): DatabaseFilter = createFilter(PropertyCondition(doesNotContain = value))

    fun startsWith(value: String): DatabaseFilter = createFilter(PropertyCondition(startsWith = value))

    fun endsWith(value: String): DatabaseFilter = createFilter(PropertyCondition(endsWith = value))

    fun isEmpty(): DatabaseFilter = createFilter(PropertyCondition(isEmpty = true))

    fun isNotEmpty(): DatabaseFilter = createFilter(PropertyCondition(isNotEmpty = true))

    private fun createFilter(condition: PropertyCondition): DatabaseFilter =
        DatabaseFilter(
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
    fun equals(value: String): DatabaseFilter = createFilter(PropertyCondition(equals = value))

    fun doesNotEqual(value: String): DatabaseFilter = createFilter(PropertyCondition(doesNotEqual = value))

    fun contains(value: String): DatabaseFilter = createFilter(PropertyCondition(contains = value))

    fun doesNotContain(value: String): DatabaseFilter = createFilter(PropertyCondition(doesNotContain = value))

    fun startsWith(value: String): DatabaseFilter = createFilter(PropertyCondition(startsWith = value))

    fun endsWith(value: String): DatabaseFilter = createFilter(PropertyCondition(endsWith = value))

    fun isEmpty(): DatabaseFilter = createFilter(PropertyCondition(isEmpty = true))

    fun isNotEmpty(): DatabaseFilter = createFilter(PropertyCondition(isNotEmpty = true))

    private fun createFilter(condition: PropertyCondition): DatabaseFilter =
        DatabaseFilter(
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
    fun equals(value: Number): DatabaseFilter = createFilter(NumberCondition(equals = value.toDouble()))

    fun doesNotEqual(value: Number): DatabaseFilter = createFilter(NumberCondition(doesNotEqual = value.toDouble()))

    fun greaterThan(value: Number): DatabaseFilter = createFilter(NumberCondition(greaterThan = value.toDouble()))

    fun lessThan(value: Number): DatabaseFilter = createFilter(NumberCondition(lessThan = value.toDouble()))

    fun greaterThanOrEqualTo(value: Number): DatabaseFilter = createFilter(NumberCondition(greaterThanOrEqualTo = value.toDouble()))

    fun lessThanOrEqualTo(value: Number): DatabaseFilter = createFilter(NumberCondition(lessThanOrEqualTo = value.toDouble()))

    fun isEmpty(): DatabaseFilter = createFilter(NumberCondition(isEmpty = true))

    fun isNotEmpty(): DatabaseFilter = createFilter(NumberCondition(isNotEmpty = true))

    private fun createFilter(condition: NumberCondition): DatabaseFilter =
        DatabaseFilter(
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
    fun equals(value: String): DatabaseFilter = createFilter(SelectCondition(equals = value))

    fun doesNotEqual(value: String): DatabaseFilter = createFilter(SelectCondition(doesNotEqual = value))

    fun isEmpty(): DatabaseFilter = createFilter(SelectCondition(isEmpty = true))

    fun isNotEmpty(): DatabaseFilter = createFilter(SelectCondition(isNotEmpty = true))

    private fun createFilter(condition: SelectCondition): DatabaseFilter =
        DatabaseFilter(
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
    fun contains(value: String): DatabaseFilter = createFilter(MultiSelectCondition(contains = value))

    fun doesNotContain(value: String): DatabaseFilter = createFilter(MultiSelectCondition(doesNotContain = value))

    fun isEmpty(): DatabaseFilter = createFilter(MultiSelectCondition(isEmpty = true))

    fun isNotEmpty(): DatabaseFilter = createFilter(MultiSelectCondition(isNotEmpty = true))

    private fun createFilter(condition: MultiSelectCondition): DatabaseFilter =
        DatabaseFilter(
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
    fun equals(date: String): DatabaseFilter = createFilter(DateCondition(equals = date))

    fun before(date: String): DatabaseFilter = createFilter(DateCondition(before = date))

    fun after(date: String): DatabaseFilter = createFilter(DateCondition(after = date))

    fun onOrBefore(date: String): DatabaseFilter = createFilter(DateCondition(onOrBefore = date))

    fun onOrAfter(date: String): DatabaseFilter = createFilter(DateCondition(onOrAfter = date))

    fun isEmpty(): DatabaseFilter = createFilter(DateCondition(isEmpty = true))

    fun isNotEmpty(): DatabaseFilter = createFilter(DateCondition(isNotEmpty = true))

    fun pastWeek(): DatabaseFilter = createFilter(DateCondition(pastWeek = EmptyObject()))

    fun pastMonth(): DatabaseFilter = createFilter(DateCondition(pastMonth = EmptyObject()))

    fun pastYear(): DatabaseFilter = createFilter(DateCondition(pastYear = EmptyObject()))

    fun nextWeek(): DatabaseFilter = createFilter(DateCondition(nextWeek = EmptyObject()))

    fun nextMonth(): DatabaseFilter = createFilter(DateCondition(nextMonth = EmptyObject()))

    fun nextYear(): DatabaseFilter = createFilter(DateCondition(nextYear = EmptyObject()))

    private fun createFilter(condition: DateCondition): DatabaseFilter =
        DatabaseFilter(
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
    fun equals(value: Boolean): DatabaseFilter = createFilter(CheckboxCondition(equals = value))

    fun doesNotEqual(value: Boolean): DatabaseFilter = createFilter(CheckboxCondition(doesNotEqual = value))

    private fun createFilter(condition: CheckboxCondition): DatabaseFilter =
        DatabaseFilter(
            property = propertyName,
            checkbox = condition,
        )
}
