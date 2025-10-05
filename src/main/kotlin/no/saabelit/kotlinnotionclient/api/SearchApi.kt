package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.search.SearchRequest
import no.saabelit.kotlinnotionclient.models.search.SearchResponse

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
}
