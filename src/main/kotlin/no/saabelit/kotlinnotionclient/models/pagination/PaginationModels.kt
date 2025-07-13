package no.saabelit.kotlinnotionclient.models.pagination

import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.config.NotionApiLimits

/**
 * Represents a paginated response from the Notion API.
 *
 * Most Notion API endpoints that return lists use cursor-based pagination
 * with this structure.
 */
@Serializable
data class PaginatedResponse<T>(
    val results: List<T>,
    val next_cursor: String? = null,
    val has_more: Boolean = false,
    val type: String? = null,
    val `object`: String? = null, // TODO: see whether this works / should be replaced
) {
    /**
     * Whether there are more results available to fetch.
     */
    val hasMore: Boolean get() = has_more

    /**
     * The cursor to use for the next page request.
     */
    val nextCursor: String? get() = next_cursor

    /**
     * Whether this is the last page of results.
     */
    val isLastPage: Boolean get() = !has_more

    /**
     * Number of results in this page.
     */
    val resultCount: Int get() = results.size
}

/**
 * Parameters for making paginated requests to the Notion API.
 */
@Serializable
data class PaginationRequest(
    val start_cursor: String? = null,
    val page_size: Int = NotionApiLimits.Response.DEFAULT_PAGE_SIZE,
) {
    init {
        require(page_size > 0) { "Page size must be positive" }
        require(page_size <= NotionApiLimits.Response.MAX_PAGE_SIZE) {
            "Page size cannot exceed ${NotionApiLimits.Response.MAX_PAGE_SIZE}"
        }
    }

    /**
     * Creates a request for the next page using the provided cursor.
     */
    fun nextPage(cursor: String): PaginationRequest = copy(start_cursor = cursor)

    /**
     * Creates a request with a different page size.
     */
    fun withPageSize(size: Int): PaginationRequest = copy(page_size = size)
}

/**
 * Configuration for automatic pagination behavior.
 */
data class PaginationConfig(
    val fetchAllPages: Boolean = false,
    val maxPagesToFetch: Int = 50,
    val pageSize: Int = NotionApiLimits.Response.DEFAULT_PAGE_SIZE,
) {
    init {
        require(maxPagesToFetch > 0) { "Max pages to fetch must be positive" }
        require(pageSize > 0) { "Page size must be positive" }
        require(pageSize <= NotionApiLimits.Response.MAX_PAGE_SIZE) {
            "Page size cannot exceed ${NotionApiLimits.Response.MAX_PAGE_SIZE}"
        }
    }

    companion object {
        /**
         * Default configuration that fetches only the first page.
         */
        val DEFAULT = PaginationConfig()

        /**
         * Configuration that automatically fetches all pages.
         * Use with caution for large datasets.
         */
        val FETCH_ALL = PaginationConfig(fetchAllPages = true)

        /**
         * Configuration optimized for small datasets.
         */
        val SMALL_PAGES = PaginationConfig(pageSize = 25)

        /**
         * Configuration optimized for large datasets.
         */
        val LARGE_PAGES = PaginationConfig(pageSize = NotionApiLimits.Response.MAX_PAGE_SIZE)
    }
}

/**
 * Represents the complete result of fetching multiple pages.
 */
data class PaginatedResult<T>(
    val items: List<T>,
    val totalPages: Int,
    val finalCursor: String?,
    val hasMore: Boolean,
) {
    /**
     * Total number of items across all fetched pages.
     */
    val totalItems: Int get() = items.size

    /**
     * Whether all available results were fetched.
     */
    val isComplete: Boolean get() = !hasMore
}
