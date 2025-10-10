@file:OptIn(ExperimentalSerializationApi::class)

package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.EmptyObject
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.utils.PaginatedResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for querying a database.
 *
 * Database queries allow filtering, sorting, and paginating through database entries.
 * The query endpoint returns pages that match the specified criteria.
 */
@Serializable
data class DatabaseQueryRequest(
    @SerialName("filter")
    val filter: DatabaseFilter? = null,
    @SerialName("sorts")
    val sorts: List<DatabaseSort>? = null,
    @SerialName("start_cursor")
    val startCursor: String? = null,
    @SerialName("page_size")
    val pageSize: Int? = null,
)

/**
 * Response model for database queries.
 *
 * Contains the matching pages and pagination information for handling large result sets.
 */
@Serializable
data class DatabaseQueryResponse(
    @SerialName("object")
    val objectType: String, // Always "list"
    @SerialName("results")
    override val results: List<Page>,
    @SerialName("next_cursor")
    override val nextCursor: String? = null,
    @SerialName("has_more")
    override val hasMore: Boolean,
    @SerialName("type")
    val type: String, // Always "page_or_database"
    @SerialName("page_or_database")
    val pageOrDatabase: EmptyObject = EmptyObject(),
) : PaginatedResponse<Page>

/**
 * Database filter specification.
 *
 * Filters can be simple property filters or compound filters using logical operators.
 * All pages that match the filter criteria will be returned.
 */
@Serializable
data class DatabaseFilter(
    // Property information (for property-based filters)
    @SerialName("property")
    val property: String? = null,
    // Property conditions (only one should be set for property filters)
    @SerialName("title")
    val title: PropertyCondition? = null,
    @SerialName("rich_text")
    val richText: PropertyCondition? = null,
    @SerialName("number")
    val number: NumberCondition? = null,
    @SerialName("select")
    val select: SelectCondition? = null,
    @SerialName("multi_select")
    val multiSelect: MultiSelectCondition? = null,
    @SerialName("date")
    val date: DateCondition? = null,
    @SerialName("checkbox")
    val checkbox: CheckboxCondition? = null,
    @SerialName("url")
    val url: PropertyCondition? = null,
    @SerialName("email")
    val email: PropertyCondition? = null,
    @SerialName("phone_number")
    val phoneNumber: PropertyCondition? = null,
    // Compound conditions
    @SerialName("and")
    val and: List<DatabaseFilter>? = null,
    @SerialName("or")
    val or: List<DatabaseFilter>? = null,
)

/**
 * Sort specification for database queries.
 *
 * Results can be sorted by property values in ascending or descending order.
 * Multiple sorts can be applied - earlier sorts take precedence.
 */
@Serializable
data class DatabaseSort(
    @SerialName("property")
    val property: String? = null,
    @SerialName("timestamp")
    val timestamp: String? = null, // "created_time" or "last_edited_time"
    @SerialName("direction")
    val direction: SortDirection,
)

/**
 * Sort direction enumeration.
 */
@Serializable
enum class SortDirection {
    @SerialName("ascending")
    ASCENDING,

    @SerialName("descending")
    DESCENDING,
}

/**
 * Property condition for text-based properties (title, rich_text, url, email, phone_number).
 */
@Serializable
data class PropertyCondition(
    @SerialName("equals")
    val equals: String? = null,
    @SerialName("does_not_equal")
    val doesNotEqual: String? = null,
    @SerialName("contains")
    val contains: String? = null,
    @SerialName("does_not_contain")
    val doesNotContain: String? = null,
    @SerialName("starts_with")
    val startsWith: String? = null,
    @SerialName("ends_with")
    val endsWith: String? = null,
    @SerialName("is_empty")
    val isEmpty: Boolean? = null,
    @SerialName("is_not_empty")
    val isNotEmpty: Boolean? = null,
)

/**
 * Number property condition for numeric comparisons.
 */
@Serializable
data class NumberCondition(
    @SerialName("equals")
    val equals: Double? = null,
    @SerialName("does_not_equal")
    val doesNotEqual: Double? = null,
    @SerialName("greater_than")
    val greaterThan: Double? = null,
    @SerialName("less_than")
    val lessThan: Double? = null,
    @SerialName("greater_than_or_equal_to")
    val greaterThanOrEqualTo: Double? = null,
    @SerialName("less_than_or_equal_to")
    val lessThanOrEqualTo: Double? = null,
    @SerialName("is_empty")
    val isEmpty: Boolean? = null,
    @SerialName("is_not_empty")
    val isNotEmpty: Boolean? = null,
)

/**
 * Select property condition.
 */
@Serializable
data class SelectCondition(
    @SerialName("equals")
    val equals: String? = null,
    @SerialName("does_not_equal")
    val doesNotEqual: String? = null,
    @SerialName("is_empty")
    val isEmpty: Boolean? = null,
    @SerialName("is_not_empty")
    val isNotEmpty: Boolean? = null,
)

/**
 * Multi-select property condition.
 */
@Serializable
data class MultiSelectCondition(
    @SerialName("contains")
    val contains: String? = null,
    @SerialName("does_not_contain")
    val doesNotContain: String? = null,
    @SerialName("is_empty")
    val isEmpty: Boolean? = null,
    @SerialName("is_not_empty")
    val isNotEmpty: Boolean? = null,
)

/**
 * Date property condition with various date-based filters.
 */
@Serializable
data class DateCondition(
    @SerialName("equals")
    val equals: String? = null, // ISO 8601 date string
    @SerialName("before")
    val before: String? = null,
    @SerialName("after")
    val after: String? = null,
    @SerialName("on_or_before")
    val onOrBefore: String? = null,
    @SerialName("on_or_after")
    val onOrAfter: String? = null,
    @SerialName("is_empty")
    val isEmpty: Boolean? = null,
    @SerialName("is_not_empty")
    val isNotEmpty: Boolean? = null,
    @SerialName("past_week")
    val pastWeek: EmptyObject? = null,
    @SerialName("past_month")
    val pastMonth: EmptyObject? = null,
    @SerialName("past_year")
    val pastYear: EmptyObject? = null,
    @SerialName("next_week")
    val nextWeek: EmptyObject? = null,
    @SerialName("next_month")
    val nextMonth: EmptyObject? = null,
    @SerialName("next_year")
    val nextYear: EmptyObject? = null,
)

/**
 * Checkbox property condition.
 */
@Serializable
data class CheckboxCondition(
    @SerialName("equals")
    val equals: Boolean? = null,
    @SerialName("does_not_equal")
    val doesNotEqual: Boolean? = null,
)
