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
import it.saabel.kotlinnotionclient.models.datasources.CreateDataSourceRequest
import it.saabel.kotlinnotionclient.models.datasources.CreateDataSourceRequestBuilder
import it.saabel.kotlinnotionclient.models.datasources.DataSource
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryBuilder
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryRequest
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryResponse
import it.saabel.kotlinnotionclient.models.datasources.Template
import it.saabel.kotlinnotionclient.models.datasources.TemplatesResponse
import it.saabel.kotlinnotionclient.models.datasources.UpdateDataSourceRequest
import it.saabel.kotlinnotionclient.models.datasources.UpdateDataSourceRequestBuilder
import it.saabel.kotlinnotionclient.models.datasources.createDataSourceRequest
import it.saabel.kotlinnotionclient.models.datasources.dataSourceQuery
import it.saabel.kotlinnotionclient.models.datasources.updateDataSourceRequest
import it.saabel.kotlinnotionclient.ratelimit.executeWithRateLimit
import it.saabel.kotlinnotionclient.utils.Pagination
import it.saabel.kotlinnotionclient.validation.RequestValidator
import it.saabel.kotlinnotionclient.validation.ValidationConfig
import kotlinx.coroutines.flow.Flow

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
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse = httpClient.get("${config.baseUrl}/data_sources/$dataSourceId")

                if (response.status.isSuccess()) {
                    response.body<DataSource>()
                } else {
                    val errorBody =
                        try {
                            response.body<String>()
                        } catch (e: Exception) {
                            "Could not read error response body"
                        }

                    throw NotionException.ApiError(
                        code = response.status.value.toString(),
                        status = response.status.value,
                        details = "HTTP ${response.status.value}: ${response.status.description}. Response: $errorBody",
                    )
                }
            } catch (e: NotionException) {
                throw e // Re-throw our own exceptions
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
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
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.post("${config.baseUrl}/data_sources/$dataSourceId/query") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<DataSourceQueryResponse>()
                } else {
                    val errorBody =
                        try {
                            response.body<String>()
                        } catch (e: Exception) {
                            "Could not read error response body"
                        }

                    throw NotionException.ApiError(
                        code = response.status.value.toString(),
                        status = response.status.value,
                        details = "HTTP ${response.status.value}: ${response.status.description}. Response: $errorBody",
                    )
                }
            } catch (e: NotionException) {
                throw e // Re-throw our own exceptions
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
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
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.post("${config.baseUrl}/data_sources") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<DataSource>()
                } else {
                    val errorBody =
                        try {
                            response.body<String>()
                        } catch (e: Exception) {
                            "Could not read error response body"
                        }

                    throw NotionException.ApiError(
                        code = response.status.value.toString(),
                        status = response.status.value,
                        details = "HTTP ${response.status.value}: ${response.status.description}. Response: $errorBody",
                    )
                }
            } catch (e: NotionException) {
                throw e // Re-throw our own exceptions
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
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
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.patch("${config.baseUrl}/data_sources/$dataSourceId") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<DataSource>()
                } else {
                    val errorBody =
                        try {
                            response.body<String>()
                        } catch (e: Exception) {
                            "Could not read error response body"
                        }

                    throw NotionException.ApiError(
                        code = response.status.value.toString(),
                        status = response.status.value,
                        details = "HTTP ${response.status.value}: ${response.status.description}. Response: $errorBody",
                    )
                }
            } catch (e: NotionException) {
                throw e // Re-throw our own exceptions
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
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
        httpClient.executeWithRateLimit {
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
                    val errorBody =
                        try {
                            response.body<String>()
                        } catch (e: Exception) {
                            "Could not read error response body"
                        }

                    throw NotionException.ApiError(
                        code = response.status.value.toString(),
                        status = response.status.value,
                        details = "HTTP ${response.status.value}: ${response.status.description}. Response: $errorBody",
                    )
                }
            } catch (e: NotionException) {
                throw e // Re-throw our own exceptions
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
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
     * @param dataSourceId The ID of the data source to query
     * @param request The query request with filters and sorts
     * @return Flow<Page> that emits individual pages from all result pages
     */
    fun queryAsFlow(
        dataSourceId: String,
        request: DataSourceQueryRequest = DataSourceQueryRequest(),
    ): Flow<it.saabel.kotlinnotionclient.models.pages.Page> =
        Pagination.asFlow { cursor ->
            querySinglePage(
                dataSourceId,
                request.copy(
                    startCursor = cursor,
                    pageSize = NotionApiLimits.Response.MAX_PAGE_SIZE,
                ),
            )
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
}
