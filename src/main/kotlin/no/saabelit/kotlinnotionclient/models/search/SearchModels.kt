package no.saabelit.kotlinnotionclient.models.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.saabelit.kotlinnotionclient.utils.PaginatedResponse

/**
 * Request parameters for searching pages and data sources.
 *
 * @property query Optional text to search for in titles
 * @property filter Optional filter to limit results to pages or data sources
 * @property sort Optional sort criteria
 * @property startCursor Pagination cursor from previous response
 * @property pageSize Number of results per page (max 100, default 100)
 */
@Serializable
data class SearchRequest(
    @SerialName("query")
    val query: String? = null,
    @SerialName("filter")
    val filter: SearchFilter? = null,
    @SerialName("sort")
    val sort: SearchSort? = null,
    @SerialName("start_cursor")
    val startCursor: String? = null,
    @SerialName("page_size")
    val pageSize: Int? = null,
)

/**
 * Filter criteria for search.
 *
 * @property value The type to filter by: "page" or "data_source"
 * @property property The property to filter on (currently only "object" is supported)
 */
@Serializable
data class SearchFilter(
    @SerialName("value")
    val value: String, // "page" or "data_source"
    @SerialName("property")
    val property: String = "object",
)

/**
 * Sort criteria for search results.
 *
 * @property direction Sort direction: "ascending" or "descending"
 * @property timestamp Timestamp to sort by (currently only "last_edited_time" is supported)
 */
@Serializable
data class SearchSort(
    @SerialName("direction")
    val direction: String, // "ascending" or "descending"
    @SerialName("timestamp")
    val timestamp: String = "last_edited_time",
)

/**
 * Search response containing pages and/or data sources.
 *
 * Note: The results are polymorphic - they can be either Page or DataSource objects.
 * In 2025-09-03 API, data sources (formerly databases) are returned instead of database objects.
 * Results are returned as JsonElement to handle the polymorphic nature - consumers can
 * deserialize to specific types (Page or DataSource) as needed.
 *
 * @property objectType Always "list"
 * @property results List of search results as JsonElement (can be Page or DataSource objects)
 * @property nextCursor Cursor for next page of results (null if no more results)
 * @property hasMore Whether there are more results available
 * @property type Type indicator for the list (legacy field)
 */
@Serializable
data class SearchResponse(
    @SerialName("object")
    val objectType: String,
    @SerialName("results")
    override val results: List<JsonElement>,
    @SerialName("next_cursor")
    override val nextCursor: String? = null,
    @SerialName("has_more")
    override val hasMore: Boolean,
    @SerialName("type")
    val type: String? = null,
    @SerialName("page_or_database")
    val pageOrDatabase: Map<String, String>? = null,
) : PaginatedResponse<JsonElement>

/**
 * DSL builder for SearchRequest.
 */
class SearchRequestBuilder {
    private var query: String? = null
    private var filter: SearchFilter? = null
    private var sort: SearchSort? = null
    private var startCursor: String? = null
    private var pageSize: Int? = null

    fun query(text: String) {
        query = text
    }

    fun filterPages() {
        filter = SearchFilter(value = "page")
    }

    fun filterDataSources() {
        filter = SearchFilter(value = "data_source")
    }

    fun sortAscending() {
        sort = SearchSort(direction = "ascending")
    }

    fun sortDescending() {
        sort = SearchSort(direction = "descending")
    }

    fun startCursor(cursor: String) {
        startCursor = cursor
    }

    fun pageSize(size: Int) {
        require(size in 1..100) { "Page size must be between 1 and 100" }
        pageSize = size
    }

    fun build(): SearchRequest =
        SearchRequest(
            query = query,
            filter = filter,
            sort = sort,
            startCursor = startCursor,
            pageSize = pageSize,
        )
}

/**
 * DSL entry point for building search requests.
 */
fun searchRequest(block: SearchRequestBuilder.() -> Unit): SearchRequest {
    val builder = SearchRequestBuilder()
    builder.block()
    return builder.build()
}
