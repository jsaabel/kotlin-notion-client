@file:Suppress("unused")

package it.saabel.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.views.CreateViewQueryRequest
import it.saabel.kotlinnotionclient.models.views.CreateViewRequest
import it.saabel.kotlinnotionclient.models.views.CreateViewRequestBuilder
import it.saabel.kotlinnotionclient.models.views.DeletedViewQuery
import it.saabel.kotlinnotionclient.models.views.PartialView
import it.saabel.kotlinnotionclient.models.views.UpdateViewRequest
import it.saabel.kotlinnotionclient.models.views.UpdateViewRequestBuilder
import it.saabel.kotlinnotionclient.models.views.View
import it.saabel.kotlinnotionclient.models.views.ViewList
import it.saabel.kotlinnotionclient.models.views.ViewQuery
import it.saabel.kotlinnotionclient.models.views.ViewQueryResults
import it.saabel.kotlinnotionclient.models.views.ViewReference
import it.saabel.kotlinnotionclient.models.views.createViewRequest
import it.saabel.kotlinnotionclient.models.views.updateViewRequest
import it.saabel.kotlinnotionclient.ratelimit.executeWithRateLimit
import it.saabel.kotlinnotionclient.utils.Pagination
import kotlinx.coroutines.flow.Flow

/**
 * API client for Notion Views endpoints.
 *
 * Views are saved configurations that control how data from a data source is displayed.
 * Each view belongs to a database and defines layout type (table, board, calendar, etc.),
 * optional filters, sorts, and display settings.
 *
 * Supports 8 endpoints:
 * - POST   /v1/views                              — create a view
 * - GET    /v1/views/{view_id}                    — retrieve a view
 * - PATCH  /v1/views/{view_id}                    — update a view
 * - DELETE /v1/views/{view_id}                    — delete a view
 * - GET    /v1/views                              — list views (paginated)
 * - POST   /v1/views/{view_id}/queries            — create a view query (execute + cache)
 * - GET    /v1/views/{view_id}/queries/{query_id} — get cached query results
 * - DELETE /v1/views/{view_id}/queries/{query_id} — delete a cached query
 */
class ViewsApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    // ========== View CRUD ==========

    /**
     * Creates a new view using a DSL builder.
     *
     * Example:
     * ```kotlin
     * val view = client.views.create {
     *     dataSourceId("ds-id")
     *     name("My Board")
     *     type(ViewType.BOARD)
     *     database("db-id")
     * }
     * ```
     *
     * @param builder DSL builder lambda
     * @return The created [View]
     */
    suspend fun create(builder: CreateViewRequestBuilder.() -> Unit): View = create(createViewRequest(builder))

    /**
     * Creates a new view.
     *
     * Exactly one of [CreateViewRequest.databaseId], [CreateViewRequest.viewId], or
     * [CreateViewRequest.createDatabase] must be non-null.
     *
     * @param request The view creation request
     * @return The created [View]
     * @throws IllegalArgumentException if the parent field constraint is violated
     * @throws NotionException.ApiError for API-level errors
     * @throws NotionException.NetworkError for network failures
     */
    suspend fun create(request: CreateViewRequest): View {
        val parentCount =
            listOfNotNull(request.databaseId, request.viewId, request.createDatabase).size
        require(parentCount == 1) {
            "Exactly one of databaseId, viewId, or createDatabase must be provided (got $parentCount)"
        }
        return httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.post("${config.baseUrl}/views") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }
                if (response.status.isSuccess()) {
                    response.body<View>()
                } else {
                    throw response.toApiError()
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }
    }

    /**
     * Retrieves a view by its ID.
     *
     * @param viewId The UUID of the view
     * @return The [View] object
     * @throws NotionException.ApiError for API-level errors (e.g. 404 if not found)
     * @throws NotionException.NetworkError for network failures
     */
    suspend fun retrieve(viewId: String): View =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse = httpClient.get("${config.baseUrl}/views/$viewId")
                if (response.status.isSuccess()) {
                    response.body<View>()
                } else {
                    throw response.toApiError()
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }

    /**
     * Updates a view using a DSL builder.
     *
     * Example:
     * ```kotlin
     * val view = client.views.update("view-id") {
     *     name("Renamed View")
     * }
     * ```
     *
     * @param viewId The UUID of the view
     * @param builder DSL builder lambda
     * @return The updated [View]
     */
    suspend fun update(
        viewId: String,
        builder: UpdateViewRequestBuilder.() -> Unit,
    ): View = update(viewId, updateViewRequest(builder))

    /**
     * Updates a view by its ID.
     *
     * All fields in [UpdateViewRequest] are optional. Omitted fields are unchanged.
     * Pass an explicit `null` to clear a nullable field.
     *
     * @param viewId The UUID of the view
     * @param request The update request
     * @return The updated [View]
     * @throws NotionException.ApiError for API-level errors
     * @throws NotionException.NetworkError for network failures
     */
    suspend fun update(
        viewId: String,
        request: UpdateViewRequest,
    ): View =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.patch("${config.baseUrl}/views/$viewId") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }
                if (response.status.isSuccess()) {
                    response.body<View>()
                } else {
                    throw response.toApiError()
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }

    /**
     * Deletes a view by its ID.
     *
     * Returns a [PartialView] (minimal object) confirming the deletion.
     *
     * @param viewId The UUID of the view
     * @return [PartialView] with `object`, `id`, `parent`, and `type`
     * @throws NotionException.ApiError for API-level errors
     * @throws NotionException.NetworkError for network failures
     */
    suspend fun delete(viewId: String): PartialView =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse = httpClient.delete("${config.baseUrl}/views/$viewId")
                if (response.status.isSuccess()) {
                    response.body<PartialView>()
                } else {
                    throw response.toApiError()
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }

    // ========== Listing ==========

    /**
     * Lists views filtered by database or data source.
     *
     * At least one of [databaseId] or [dataSourceId] must be provided.
     * Returns a single page of [ViewReference] objects (id + object only).
     *
     * @param databaseId Filter views by database UUID
     * @param dataSourceId Filter views by data source UUID
     * @param startCursor Pagination cursor from a previous response
     * @param pageSize Number of results per page (1–100)
     * @return [ViewList] containing references and pagination metadata
     * @throws IllegalArgumentException if neither [databaseId] nor [dataSourceId] is provided
     * @throws NotionException.ApiError for API-level errors
     * @throws NotionException.NetworkError for network failures
     */
    suspend fun list(
        databaseId: String? = null,
        dataSourceId: String? = null,
        startCursor: String? = null,
        pageSize: Int? = null,
    ): ViewList {
        require(databaseId != null || dataSourceId != null) {
            "At least one of databaseId or dataSourceId must be provided"
        }
        pageSize?.let {
            require(it in 1..100) { "pageSize must be between 1 and 100 (got $it)" }
        }
        return httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.get("${config.baseUrl}/views") {
                        databaseId?.let { url.parameters.append("database_id", it) }
                        dataSourceId?.let { url.parameters.append("data_source_id", it) }
                        startCursor?.let { url.parameters.append("start_cursor", it) }
                        pageSize?.let { url.parameters.append("page_size", it.toString()) }
                    }
                if (response.status.isSuccess()) {
                    response.body<ViewList>()
                } else {
                    throw response.toApiError()
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }
    }

    /**
     * Returns a [Flow] that emits individual [ViewReference] objects across all pages.
     *
     * @param databaseId Filter views by database UUID
     * @param dataSourceId Filter views by data source UUID
     * @return Flow of [ViewReference] items
     */
    fun listAsFlow(
        databaseId: String? = null,
        dataSourceId: String? = null,
    ): Flow<ViewReference> =
        Pagination.asFlow { cursor ->
            list(
                databaseId = databaseId,
                dataSourceId = dataSourceId,
                startCursor = cursor,
            )
        }

    /**
     * Returns a [Flow] that emits complete [ViewList] pages.
     *
     * @param databaseId Filter views by database UUID
     * @param dataSourceId Filter views by data source UUID
     * @return Flow of [ViewList] pages
     */
    fun listPagedFlow(
        databaseId: String? = null,
        dataSourceId: String? = null,
    ): Flow<ViewList> =
        Pagination.asPagesFlow { cursor ->
            list(
                databaseId = databaseId,
                dataSourceId = dataSourceId,
                startCursor = cursor,
            )
        }

    // ========== View Queries ==========

    /**
     * Creates a view query — executes the view's filter/sort logic and caches the results.
     *
     * The cache expires after 15 minutes. Use [getQueryResults] to paginate through
     * subsequent pages and [deleteQuery] to release the cache early.
     *
     * @param viewId The UUID of the view
     * @param pageSize Number of results in the first page (1–100, optional)
     * @return [ViewQuery] containing the query ID, expiry time, and first page of results
     * @throws NotionException.ApiError for API-level errors
     * @throws NotionException.NetworkError for network failures
     */
    suspend fun createQuery(
        viewId: String,
        pageSize: Int? = null,
    ): ViewQuery {
        pageSize?.let {
            require(it in 1..100) { "pageSize must be between 1 and 100 (got $it)" }
        }
        return httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.post("${config.baseUrl}/views/$viewId/queries") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateViewQueryRequest(pageSize = pageSize))
                    }
                if (response.status.isSuccess()) {
                    response.body<ViewQuery>()
                } else {
                    throw response.toApiError()
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }
    }

    /**
     * Retrieves a page of cached query results.
     *
     * @param viewId The UUID of the view
     * @param queryId The query ID returned by [createQuery]
     * @param startCursor Pagination cursor for subsequent pages
     * @param pageSize Number of results per page (1–100, optional)
     * @return [ViewQueryResults] containing page references and pagination metadata
     * @throws NotionException.ApiError for API-level errors (e.g. 404 if query expired)
     * @throws NotionException.NetworkError for network failures
     */
    suspend fun getQueryResults(
        viewId: String,
        queryId: String,
        startCursor: String? = null,
        pageSize: Int? = null,
    ): ViewQueryResults {
        pageSize?.let {
            require(it in 1..100) { "pageSize must be between 1 and 100 (got $it)" }
        }
        return httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.get("${config.baseUrl}/views/$viewId/queries/$queryId") {
                        startCursor?.let { url.parameters.append("start_cursor", it) }
                        pageSize?.let { url.parameters.append("page_size", it.toString()) }
                    }
                if (response.status.isSuccess()) {
                    response.body<ViewQueryResults>()
                } else {
                    throw response.toApiError()
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }
    }

    /**
     * Deletes a cached view query. This operation is idempotent.
     *
     * @param viewId The UUID of the view
     * @param queryId The query ID to delete
     * @return [DeletedViewQuery] confirming deletion
     * @throws NotionException.ApiError for API-level errors
     * @throws NotionException.NetworkError for network failures
     */
    suspend fun deleteQuery(
        viewId: String,
        queryId: String,
    ): DeletedViewQuery =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.delete("${config.baseUrl}/views/$viewId/queries/$queryId")
                if (response.status.isSuccess()) {
                    response.body<DeletedViewQuery>()
                } else {
                    throw response.toApiError()
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }

    // ========== Internal helpers ==========

    private suspend fun HttpResponse.toApiError(): NotionException.ApiError {
        val errorBody =
            try {
                body<String>()
            } catch (e: Exception) {
                "Could not read error response body"
            }
        return NotionException.ApiError(
            code = status.value.toString(),
            status = status.value,
            details = "HTTP ${status.value}: ${status.description}. Response: $errorBody",
        )
    }
}
