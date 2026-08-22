package it.saabel.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import it.saabel.kotlinnotionclient.config.NotionApiLimits
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.exceptions.toNotionApiError
import it.saabel.kotlinnotionclient.models.datasources.CreateDataSourceRequest
import it.saabel.kotlinnotionclient.models.datasources.CreateDataSourceRequestBuilder
import it.saabel.kotlinnotionclient.models.datasources.DataSource
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryBuilder
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryRequest
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryResponse
import it.saabel.kotlinnotionclient.models.datasources.RowIterationKey
import it.saabel.kotlinnotionclient.models.datasources.Template
import it.saabel.kotlinnotionclient.models.datasources.TemplatesResponse
import it.saabel.kotlinnotionclient.models.datasources.UpdateDataSourceRequest
import it.saabel.kotlinnotionclient.models.datasources.UpdateDataSourceRequestBuilder
import it.saabel.kotlinnotionclient.models.datasources.createDataSourceRequest
import it.saabel.kotlinnotionclient.models.datasources.dataSourceQuery
import it.saabel.kotlinnotionclient.models.datasources.updateDataSourceRequest
import it.saabel.kotlinnotionclient.utils.Pagination
import it.saabel.kotlinnotionclient.validation.RequestValidator
import it.saabel.kotlinnotionclient.validation.ValidationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

/**
 * API client for Notion Data Sources endpoints (API version 2025-09-03+).
 *
 * Data sources are individual tables within a database container.
 * Each data source has its own schema (properties) and contains pages as rows.
 * This API handles operations related to individual data sources.
 */
class DataSourcesApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
    private val validationConfig: ValidationConfig = ValidationConfig.default(),
) {
    private val validator = RequestValidator(validationConfig)

    /**
     * Retrieves a data source object using the ID specified.
     *
     * This returns the full data source including its properties (schema),
     * which in the old API would have been returned by retrieving a database.
     *
     * @param dataSourceId The ID of the data source to retrieve
     * @return DataSource object with all properties and schema
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieve(dataSourceId: String): DataSource =
        try {
            val response: HttpResponse = httpClient.get("${config.baseUrl}/data_sources/$dataSourceId")

            if (response.status.isSuccess()) {
                response.body<DataSource>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Queries a data source using a fluent DSL builder.
     *
     * This is the 2025-09-03 equivalent of querying a database.
     * The data source ID identifies the specific table within a database container.
     *
     * Example usage:
     * ```kotlin
     * val pages = client.dataSources.query("data-source-id") {
     *     filter {
     *         and(
     *             title("Task").contains("Important"),
     *             checkbox("Completed").equals(false)
     *         )
     *     }
     *     sortBy("Priority", SortDirection.DESCENDING)
     *     pageSize(50)
     * }
     * ```
     *
     * @param dataSourceId The ID of the data source to query
     * @param builder DSL builder lambda for constructing the query
     * @return List of all matching pages across all result pages
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun query(
        dataSourceId: String,
        builder: DataSourceQueryBuilder.() -> Unit,
    ): List<it.saabel.kotlinnotionclient.models.pages.Page> {
        val request = dataSourceQuery(builder)
        return query(dataSourceId, request)
    }

    /**
     * Queries a data source with optional filtering and sorting.
     *
     * Automatically fetches all pages that match the query criteria by handling
     * pagination transparently. Returns all matching pages in a single list.
     *
     * @param dataSourceId The ID of the data source to query
     * @param request The query request with filters and sorts
     * @return List of all matching pages across all result pages
     * @throws NotionException.QueryResultLimitReached when Notion truncates the result
     *     set at its 10,000-row cap. The exception carries the partial results, the
     *     `nextCursor`, and the raw `requestStatus`.
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun query(
        dataSourceId: String,
        request: DataSourceQueryRequest = DataSourceQueryRequest(),
    ): List<it.saabel.kotlinnotionclient.models.pages.Page> {
        val allPages = mutableListOf<it.saabel.kotlinnotionclient.models.pages.Page>()
        var currentCursor: String? = null
        var pageCount = 0

        do {
            val paginatedRequest =
                request.copy(
                    startCursor = currentCursor,
                    pageSize = NotionApiLimits.Response.MAX_PAGE_SIZE,
                )

            val response = querySinglePage(dataSourceId, paginatedRequest)
            allPages.addAll(response.results)

            response.requestStatus?.takeIf { it.isIncomplete }?.let { status ->
                throw NotionException.QueryResultLimitReached(
                    partialResults = allPages.toList(),
                    nextCursor = response.nextCursor,
                    requestStatus = status,
                )
            }

            currentCursor = response.nextCursor
            pageCount++

            // Safety check to prevent infinite loops
            val maxPages = 1000 // 100,000 records max (100 per page * 1000 pages)
            if (pageCount >= maxPages) {
                throw NotionException.ApiError(
                    code = "PAGINATION_LIMIT_EXCEEDED",
                    status = 500,
                    details =
                        "Data source query exceeded $maxPages pages " +
                            "(${maxPages * NotionApiLimits.Response.MAX_PAGE_SIZE} records). " +
                            "This may indicate an infinite loop or an extremely large data source.",
                )
            }
        } while (response.hasMore)

        return allPages
    }

    /**
     * Queries a single page of data source results.
     *
     * This is the low-level method that handles a single API request. Most users should
     * use the `query` method instead, which automatically handles pagination.
     *
     * @param dataSourceId The ID of the data source to query
     * @param request The query request with filters, sorts, and pagination parameters
     * @return DatabaseQueryResponse containing a single page of results
     */
    private suspend fun querySinglePage(
        dataSourceId: String,
        request: DataSourceQueryRequest,
    ): DataSourceQueryResponse =
        try {
            val response: HttpResponse =
                httpClient.post("${config.baseUrl}/data_sources/$dataSourceId/query") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (response.status.isSuccess()) {
                response.body<DataSourceQueryResponse>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Creates a new data source in an existing database using a fluent DSL builder.
     *
     * Example usage:
     * ```kotlin
     * val dataSource = client.dataSources.create {
     *     databaseId("existing-database-id")
     *     title("Projects")
     *     properties {
     *         title("Project Name")
     *         select("Status", "To Do", "In Progress", "Done")
     *         date("Due Date")
     *     }
     * }
     * ```
     *
     * @param builder DSL builder lambda for constructing the request
     * @return DataSource object representing the created data source
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun create(builder: CreateDataSourceRequestBuilder.() -> Unit): DataSource {
        val request = createDataSourceRequest(builder)
        return create(request)
    }

    /**
     * Creates a new data source in an existing database.
     *
     * @param request The data source creation request
     * @return DataSource object representing the created data source
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun create(request: CreateDataSourceRequest): DataSource =
        try {
            val response: HttpResponse =
                httpClient.post("${config.baseUrl}/data_sources") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (response.status.isSuccess()) {
                response.body<DataSource>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Updates a data source using a fluent DSL builder.
     *
     * Example usage:
     * ```kotlin
     * val dataSource = client.dataSources.update("data-source-id") {
     *     title("Updated Projects")
     *     properties {
     *         // Add new property
     *         number("Priority")
     *         // Existing properties unchanged unless redefined
     *     }
     * }
     * ```
     *
     * @param dataSourceId The ID of the data source to update
     * @param builder DSL builder lambda for constructing the update request
     * @return DataSource object representing the updated data source
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun update(
        dataSourceId: String,
        builder: UpdateDataSourceRequestBuilder.() -> Unit,
    ): DataSource {
        val request = updateDataSourceRequest(builder)
        return update(dataSourceId, request)
    }

    /**
     * Updates a data source.
     *
     * @param dataSourceId The ID of the data source to update
     * @param request The update request
     * @return DataSource object representing the updated data source
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun update(
        dataSourceId: String,
        request: UpdateDataSourceRequest,
    ): DataSource =
        try {
            val response: HttpResponse =
                httpClient.patch("${config.baseUrl}/data_sources/$dataSourceId") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (response.status.isSuccess()) {
                response.body<DataSource>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Lists available templates for a data source.
     *
     * Templates allow creating pages with pre-populated content and structure.
     * This method automatically handles pagination and returns all templates.
     *
     * Example usage:
     * ```kotlin
     * val templates = client.dataSources.listTemplates("data-source-id")
     * val defaultTemplate = templates.find { it.isDefault }
     * ```
     *
     * @param dataSourceId The ID of the data source
     * @param nameFilter Optional substring to filter templates by name (case-insensitive)
     * @return List of all templates for the data source
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun listTemplates(
        dataSourceId: String,
        nameFilter: String? = null,
    ): List<Template> {
        val allTemplates = mutableListOf<Template>()
        var currentCursor: String? = null
        var pageCount = 0

        do {
            val response = listTemplatesSinglePage(dataSourceId, nameFilter, currentCursor)
            allTemplates.addAll(response.templates)

            currentCursor = response.nextCursor
            pageCount++

            // Safety check to prevent infinite loops
            val maxPages = 100 // Reasonable limit for templates
            if (pageCount >= maxPages) {
                throw NotionException.ApiError(
                    code = "PAGINATION_LIMIT_EXCEEDED",
                    status = 500,
                    details =
                        "Template listing exceeded $maxPages pages. " +
                            "This may indicate an infinite loop or an issue with the API.",
                )
            }
        } while (response.hasMore)

        return allTemplates
    }

    /**
     * Lists a single page of templates for a data source.
     *
     * This is the low-level method that handles a single API request. Most users should
     * use the `listTemplates` method instead, which automatically handles pagination.
     *
     * @param dataSourceId The ID of the data source
     * @param nameFilter Optional substring to filter templates by name
     * @param startCursor Optional pagination cursor
     * @return TemplatesResponse containing a single page of results
     */
    private suspend fun listTemplatesSinglePage(
        dataSourceId: String,
        nameFilter: String? = null,
        startCursor: String? = null,
    ): TemplatesResponse =
        try {
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/data_sources/$dataSourceId/templates") {
                    if (nameFilter != null) {
                        url.parameters.append("name", nameFilter)
                    }
                    if (startCursor != null) {
                        url.parameters.append("start_cursor", startCursor)
                    }
                    url.parameters.append("page_size", NotionApiLimits.Response.MAX_PAGE_SIZE.toString())
                }

            if (response.status.isSuccess()) {
                response.body<TemplatesResponse>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    // ========== Pagination Helper Methods ==========

    /**
     * Queries a data source and returns results as a Flow for reactive processing.
     *
     * This method emits individual pages as they become available, enabling
     * efficient memory usage for large result sets and reactive processing patterns.
     *
     * Example usage:
     * ```kotlin
     * client.dataSources.queryAsFlow("data-source-id") {
     *     filter { property("Status") { select { equals("Active") } } }
     * }.collect { page ->
     *     println("Processing page: ${page.id}")
     *     // Process each page individually
     * }
     * ```
     *
     * @param dataSourceId The ID of the data source to query
     * @param builder DSL builder lambda for constructing the query
     * @return Flow<Page> that emits individual pages from all result pages
     */
    fun queryAsFlow(
        dataSourceId: String,
        builder: DataSourceQueryBuilder.() -> Unit,
    ): Flow<it.saabel.kotlinnotionclient.models.pages.Page> {
        val request = dataSourceQuery(builder)
        return queryAsFlow(dataSourceId, request)
    }

    /**
     * Queries a data source and returns results as a Flow for reactive processing.
     *
     * Throws [NotionException.QueryResultLimitReached] (terminating the flow) when
     * Notion truncates the result set at its 10,000-row cap. To inspect truncation
     * without losing the per-page metadata, use [queryPagedFlow] instead — it emits
     * the raw response (including `requestStatus`) without throwing.
     *
     * @param dataSourceId The ID of the data source to query
     * @param request The query request with filters and sorts
     * @return Flow<Page> that emits individual pages from all result pages
     */
    fun queryAsFlow(
        dataSourceId: String,
        request: DataSourceQueryRequest = DataSourceQueryRequest(),
    ): Flow<it.saabel.kotlinnotionclient.models.pages.Page> =
        flow {
            val emitted = mutableListOf<it.saabel.kotlinnotionclient.models.pages.Page>()
            var cursor: String? = null
            do {
                val response =
                    querySinglePage(
                        dataSourceId,
                        request.copy(
                            startCursor = cursor,
                            pageSize = NotionApiLimits.Response.MAX_PAGE_SIZE,
                        ),
                    )
                response.results.forEach {
                    emitted.add(it)
                    emit(it)
                }
                response.requestStatus?.takeIf { it.isIncomplete }?.let { status ->
                    throw NotionException.QueryResultLimitReached(
                        partialResults = emitted.toList(),
                        nextCursor = response.nextCursor,
                        requestStatus = status,
                    )
                }
                cursor = response.nextCursor
            } while (response.hasMore)
        }

    /**
     * Queries a data source and returns response pages as a Flow.
     *
     * Unlike [queryAsFlow], this emits complete [DataSourceQueryResponse] objects,
     * allowing access to pagination metadata alongside results.
     *
     * Example usage:
     * ```kotlin
     * client.dataSources.queryPagedFlow("data-source-id") {
     *     filter { /* ... */ }
     * }.collect { response ->
     *     println("Got ${response.results.size} pages (has more: ${response.hasMore})")
     *     response.results.forEach { page -> /* process page */ }
     * }
     * ```
     *
     * @param dataSourceId The ID of the data source to query
     * @param builder DSL builder lambda for constructing the query
     * @return Flow<DatabaseQueryResponse> that emits complete response pages
     */
    fun queryPagedFlow(
        dataSourceId: String,
        builder: DataSourceQueryBuilder.() -> Unit,
    ): Flow<DataSourceQueryResponse> {
        val request = dataSourceQuery(builder)
        return queryPagedFlow(dataSourceId, request)
    }

    /**
     * Queries a data source and returns response pages as a Flow.
     *
     * @param dataSourceId The ID of the data source to query
     * @param request The query request with filters and sorts
     * @return Flow<DatabaseQueryResponse> that emits complete response pages
     */
    fun queryPagedFlow(
        dataSourceId: String,
        request: DataSourceQueryRequest = DataSourceQueryRequest(),
    ): Flow<DataSourceQueryResponse> =
        Pagination.asPagesFlow { cursor ->
            querySinglePage(
                dataSourceId,
                request.copy(
                    startCursor = cursor,
                    pageSize = NotionApiLimits.Response.MAX_PAGE_SIZE,
                ),
            )
        }

    /**
     * Queries a single page of results without auto-paginating.
     *
     * Unlike [query], which transparently fetches all matching pages, this method makes
     * exactly one API call and returns the raw response — including the cursor and [hasMore]
     * flag so the caller can decide whether and how to continue.
     *
     * Use this when you only want the first N results (e.g. a "top 5" dashboard query)
     * and do not need all matching records. Specify the desired count via [pageSize] in the
     * DSL builder; if omitted the Notion API default of 100 applies.
     *
     * Example:
     * ```kotlin
     * val response = notion.dataSources.queryFirstPage("data-source-id") {
     *     sort { property("Created").descending() }
     *     pageSize(5)
     * }
     * val top5 = response.results          // at most 5 pages
     * val hasMore = response.hasMore       // true if more results exist
     * val cursor = response.nextCursor     // use for manual follow-up calls if needed
     * ```
     *
     * @param dataSourceId The ID of the data source to query
     * @param builder DSL builder lambda for constructing the query (including optional [pageSize])
     * @return [DataSourceQueryResponse] for the first page of matching results
     */
    suspend fun queryFirstPage(
        dataSourceId: String,
        builder: DataSourceQueryBuilder.() -> Unit = {},
    ): DataSourceQueryResponse {
        val request = dataSourceQuery(builder)
        return querySinglePage(dataSourceId, request)
    }

    // ========== Large Data Source Iteration ==========

    /**
     * Iterates over all rows of a data source, windowing past Notion's 10,000-row
     * query result cap.
     *
     * Plain [query] and [queryAsFlow] throw [NotionException.QueryResultLimitReached]
     * when the API truncates the result set. This method is the explicit opt-in
     * alternative: it sorts rows ascending by a monotonic [key], follows the cursor
     * chain, and whenever the result set is truncated it re-queries from the last seen
     * key value until the data source is drained. Draining costs one extra query per
     * 10,000-row window (plus boundary re-reads), which is why it is not the default.
     *
     * Rows are emitted ordered by the [key], not by any caller-defined sort — a query
     * carrying its own `sorts` (or a `startCursor`) is rejected with
     * [IllegalArgumentException]. A caller-supplied filter is combined with the window
     * filter via an `and` compound; because Notion limits filter nesting to two levels,
     * an already two-level-deep caller filter cannot be combined and the API will
     * reject it.
     *
     * Consistency: the iteration is not a snapshot. Rows created, deleted, or edited
     * while draining may or may not be included. With the default
     * [RowIterationKey.CreatedTime] key, every row that exists (and keeps matching the
     * filter) for the whole drain is emitted exactly once; see [RowIterationKey] for
     * per-key guarantees and limitations.
     *
     * Example usage:
     * ```kotlin
     * client.dataSources.iterateAllRows("data-source-id") {
     *     filter { checkbox("Archived").equals(false) }
     * }.collect { page -> process(page) }
     *
     * // Or window on a unique_id property for guaranteed progress:
     * client.dataSources.iterateAllRows(
     *     "data-source-id",
     *     key = RowIterationKey.UniqueId("ID"),
     * ).collect { page -> process(page) }
     * ```
     *
     * @param dataSourceId The ID of the data source to query
     * @param request The query request (filters only — no sorts or cursor)
     * @param key The monotonic key used for windowing (defaults to `created_time`)
     * @return Flow<Page> that emits every matching row, ordered ascending by [key]
     * @throws IllegalArgumentException if the request carries sorts or a start cursor
     * @throws NotionException.IterationStalled if a truncated window yields no new rows
     *     (more than 10,000 rows sharing one key value); use [RowIterationKey.UniqueId]
     * @throws NotionException.ValidationError if [RowIterationKey.UniqueId] names a
     *     property that is missing or empty on an encountered row
     */
    fun iterateAllRows(
        dataSourceId: String,
        request: DataSourceQueryRequest = DataSourceQueryRequest(),
        key: RowIterationKey = RowIterationKey.CreatedTime,
    ): Flow<it.saabel.kotlinnotionclient.models.pages.Page> =
        DataSourceRowIteration.iterateAllRows(request, key) { pageRequest ->
            querySinglePage(dataSourceId, pageRequest)
        }

    /**
     * Iterates over all rows of a data source using a fluent DSL builder,
     * windowing past Notion's 10,000-row query result cap.
     *
     * See [iterateAllRows] for the windowing semantics and consistency guarantees.
     *
     * @param dataSourceId The ID of the data source to query
     * @param key The monotonic key used for windowing (defaults to `created_time`)
     * @param builder DSL builder lambda for constructing the query (filters only)
     * @return Flow<Page> that emits every matching row, ordered ascending by [key]
     */
    fun iterateAllRows(
        dataSourceId: String,
        key: RowIterationKey = RowIterationKey.CreatedTime,
        builder: DataSourceQueryBuilder.() -> Unit,
    ): Flow<it.saabel.kotlinnotionclient.models.pages.Page> = iterateAllRows(dataSourceId, dataSourceQuery(builder), key)

    /**
     * Collects all rows of a data source into a list, windowing past Notion's
     * 10,000-row query result cap.
     *
     * Convenience wrapper around [iterateAllRows] that loads every row into memory —
     * for very large data sources prefer the Flow variant and process rows as they
     * arrive.
     *
     * @param dataSourceId The ID of the data source to query
     * @param request The query request (filters only — no sorts or cursor)
     * @param key The monotonic key used for windowing (defaults to `created_time`)
     * @return All matching rows, ordered ascending by [key]
     */
    suspend fun collectAllRows(
        dataSourceId: String,
        request: DataSourceQueryRequest = DataSourceQueryRequest(),
        key: RowIterationKey = RowIterationKey.CreatedTime,
    ): List<it.saabel.kotlinnotionclient.models.pages.Page> = iterateAllRows(dataSourceId, request, key).toList()

    /**
     * Collects all rows of a data source into a list using a fluent DSL builder,
     * windowing past Notion's 10,000-row query result cap.
     *
     * See [iterateAllRows] for the windowing semantics and consistency guarantees.
     *
     * @param dataSourceId The ID of the data source to query
     * @param key The monotonic key used for windowing (defaults to `created_time`)
     * @param builder DSL builder lambda for constructing the query (filters only)
     * @return All matching rows, ordered ascending by [key]
     */
    suspend fun collectAllRows(
        dataSourceId: String,
        key: RowIterationKey = RowIterationKey.CreatedTime,
        builder: DataSourceQueryBuilder.() -> Unit,
    ): List<it.saabel.kotlinnotionclient.models.pages.Page> = collectAllRows(dataSourceId, dataSourceQuery(builder), key)
}
