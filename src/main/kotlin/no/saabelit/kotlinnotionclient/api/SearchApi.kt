package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.search.SearchRequest
import no.saabelit.kotlinnotionclient.models.search.SearchResponse
import no.saabelit.kotlinnotionclient.utils.Pagination

/**
 * API client for Notion Search operations.
 *
 * The Search API allows you to search all pages and data sources that have been shared
 * with your integration. In the 2025-09-03 API version, databases are now called data sources.
 *
 * **Search Limitations:**
 * - Search indexing is not immediate - results may be delayed after sharing
 * - Not optimized for exhaustive enumeration of all documents
 * - Not for searching within a specific database (use Query API instead)
 * - Best when filtering by object type and providing a text query
 *
 * Example usage:
 * ```kotlin
 * // Search all pages
 * val response = client.search.search(searchRequest {
 *     query("meeting notes")
 *     filterPages()
 *     sortDescending()
 * })
 *
 * // Search data sources only
 * val dataSources = client.search.search(searchRequest {
 *     filterDataSources()
 *     pageSize(50)
 * })
 *
 * // Paginate through results
 * var cursor: String? = null
 * do {
 *     val page = client.search.search(searchRequest {
 *         query("project")
 *         startCursor(cursor)
 *     })
 *     // Process page.results
 *     cursor = page.nextCursor
 * } while (page.hasMore)
 * ```
 *
 * @property httpClient The HTTP client for making requests
 * @property config The Notion API configuration
 */
class SearchApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Searches all pages and data sources shared with the integration.
     *
     * Returns all pages or data sources that have titles matching the query parameter.
     * If no query is provided, returns all pages or data sources shared with the integration.
     *
     * @param request The search request parameters
     * @return SearchResponse containing matching results
     */
    suspend fun search(request: SearchRequest = SearchRequest()): SearchResponse =
        httpClient
            .post("/v1/search") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

    /**
     * Searches with a simple text query.
     *
     * Convenience method for searching by text only.
     *
     * @param query The text to search for
     * @return SearchResponse containing matching results
     */
    suspend fun search(query: String): SearchResponse = search(SearchRequest(query = query))

    // ========== Pagination Helper Methods ==========

    /**
     * Searches and returns results as a Flow for reactive processing.
     *
     * This method emits individual search results (JsonElement) as they become available,
     * enabling efficient memory usage for large search result sets.
     *
     * **Note**: Results are polymorphic JsonElements that can be either Page or DataSource objects.
     *
     * Example usage:
     * ```kotlin
     * client.search.searchAsFlow(SearchRequest(query = "project")).collect { result ->
     *     // result is a JsonElement - can be Page or DataSource
     *     println("Processing search result...")
     * }
     * ```
     *
     * @param request The search request parameters
     * @return Flow<JsonElement> that emits individual search results from all result pages
     */
    fun searchAsFlow(request: SearchRequest = SearchRequest()): Flow<JsonElement> =
        Pagination.asFlow { cursor ->
            search(request.copy(startCursor = cursor))
        }

    /**
     * Searches and returns response pages as a Flow.
     *
     * Unlike [searchAsFlow], this emits complete [SearchResponse] objects,
     * allowing access to pagination metadata alongside results.
     *
     * Example usage:
     * ```kotlin
     * client.search.searchPagedFlow(SearchRequest(query = "notes")).collect { response ->
     *     println("Got ${response.results.size} results (has more: ${response.hasMore})")
     *     response.results.forEach { result -> /* process result */ }
     * }
     * ```
     *
     * @param request The search request parameters
     * @return Flow<SearchResponse> that emits complete response pages
     */
    fun searchPagedFlow(request: SearchRequest = SearchRequest()): Flow<SearchResponse> =
        Pagination.asPagesFlow { cursor ->
            search(request.copy(startCursor = cursor))
        }
}
