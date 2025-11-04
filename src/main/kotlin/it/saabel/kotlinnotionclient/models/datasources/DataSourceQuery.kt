@file:OptIn(ExperimentalSerializationApi::class)

package it.saabel.kotlinnotionclient.models.datasources

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
data class DataSourceQueryRequest(
    @SerialName("filter")
    val filter: DataSourceFilter? = null,
    @SerialName("sorts")
    val sorts: List<DataSourceSort>? = null,
    @SerialName("start_cursor")
    val startCursor: String? = null,
    @SerialName("page_size")
    val pageSize: Int? = null,
)

/**
 * Response model for data source queries.
 *
 * Contains the matching pages and pagination information for handling large result sets.
 */
@Serializable
data class DataSourceQueryResponse(
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
 * Data source filter specification.
 *
 * Filters can be simple property filters or compound filters using logical operators.
 * All pages that match the filter criteria will be returned.
 */
@Serializable
data class DataSourceFilter(
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
    @SerialName("relation")
    val relation: RelationCondition? = null,
    @SerialName("people")
    val people: PeopleCondition? = null,
    @SerialName("status")
    val status: StatusCondition? = null,
    @SerialName("unique_id")
    val uniqueId: UniqueIdCondition? = null,
    @SerialName("files")
    val files: FilesCondition? = null,
    // Compound conditions
    @SerialName("and")
    val and: List<DataSourceFilter>? = null,
    @SerialName("or")
    val or: List<DataSourceFilter>? = null,
)

/**
 * Sort specification for data source queries.
 *
 * Results can be sorted by property values in ascending or descending order.
 * Multiple sorts can be applied - earlier sorts take precedence.
 */
@Serializable
data class DataSourceSort(
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

/**
 * Relation property condition for filtering by related pages.
 */
@Serializable
data class RelationCondition(
    @SerialName("contains")
    val contains: String? = null, // UUID
    @SerialName("does_not_contain")
    val doesNotContain: String? = null, // UUID
    @SerialName("is_empty")
    val isEmpty: Boolean? = null,
    @SerialName("is_not_empty")
    val isNotEmpty: Boolean? = null,
)

/**
 * People property condition for filtering by users.
 * Also applies to created_by and last_edited_by property types.
 */
@Serializable
data class PeopleCondition(
    @SerialName("contains")
    val contains: String? = null, // UUID
    @SerialName("does_not_contain")
    val doesNotContain: String? = null, // UUID
    @SerialName("is_empty")
    val isEmpty: Boolean? = null,
    @SerialName("is_not_empty")
    val isNotEmpty: Boolean? = null,
)

/**
 * Status property condition.
 */
@Serializable
data class StatusCondition(
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
 * Unique ID property condition for filtering by auto-incrementing IDs.
 */
@Serializable
data class UniqueIdCondition(
    @SerialName("equals")
    val equals: Int? = null,
    @SerialName("does_not_equal")
    val doesNotEqual: Int? = null,
    @SerialName("greater_than")
    val greaterThan: Int? = null,
    @SerialName("less_than")
    val lessThan: Int? = null,
    @SerialName("greater_than_or_equal_to")
    val greaterThanOrEqualTo: Int? = null,
    @SerialName("less_than_or_equal_to")
    val lessThanOrEqualTo: Int? = null,
)

/**
 * Files property condition for checking file attachment presence.
 */
@Serializable
data class FilesCondition(
    @SerialName("is_empty")
    val isEmpty: Boolean? = null,
    @SerialName("is_not_empty")
    val isNotEmpty: Boolean? = null,
)
