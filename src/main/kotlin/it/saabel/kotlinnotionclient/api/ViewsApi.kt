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
import it.saabel.kotlinnotionclient.exceptions.toNotionApiError
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryRequest
import it.saabel.kotlinnotionclient.models.datasources.RowIterationKey
import it.saabel.kotlinnotionclient.models.pages.Page
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
import it.saabel.kotlinnotionclient.utils.Pagination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

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
 *
 * A view query is capped at 10,000 rows and cannot be windowed (the endpoint takes only
 * `page_size`). To read every row behind a view, use [iterateAllRows], which drains the
 * view's underlying data source with the view's filter.
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
        return try {
            val response: HttpResponse =
                httpClient.post("${config.baseUrl}/views") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            if (response.status.isSuccess()) {
                response.body<View>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
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
        try {
            val response: HttpResponse = httpClient.get("${config.baseUrl}/views/$viewId")
            if (response.status.isSuccess()) {
                response.body<View>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
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
        try {
            val response: HttpResponse =
                httpClient.patch("${config.baseUrl}/views/$viewId") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            if (response.status.isSuccess()) {
                response.body<View>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
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
        try {
            val response: HttpResponse = httpClient.delete("${config.baseUrl}/views/$viewId")
            if (response.status.isSuccess()) {
                response.body<PartialView>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
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
        return try {
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
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
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
        return try {
            val response: HttpResponse =
                httpClient.post("${config.baseUrl}/views/$viewId/queries") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateViewQueryRequest(pageSize = pageSize))
                }
            if (response.status.isSuccess()) {
                response.body<ViewQuery>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
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
        return try {
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/views/$viewId/queries/$queryId") {
                    startCursor?.let { url.parameters.append("start_cursor", it) }
                    pageSize?.let { url.parameters.append("page_size", it.toString()) }
                }
            if (response.status.isSuccess()) {
                response.body<ViewQueryResults>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
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
        try {
            val response: HttpResponse =
                httpClient.delete("${config.baseUrl}/views/$viewId/queries/$queryId")
            if (response.status.isSuccess()) {
                response.body<DeletedViewQuery>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    // ========== Large View Iteration ==========

    /**
     * Iterates over every row behind a view, windowing past Notion's 10,000-row cap.
     *
     * ## Why this does not use the view query endpoint
     *
     * A view query caches an *already capped* result set: `POST /v1/views/{id}/queries`
     * accepts nothing but `page_size`, and paginating a cached query accepts no filter,
     * so there is no way to re-open a view query past its own boundary. Notion's own
     * guidance is explicit — "the same 10,000-result limit applies to view queries, but
     * you can't window them […] to read every row behind a view, query its underlying
     * data source […] pass the view's filter into the windowed data source query so you
     * keep the same row set."
     *
     * That is exactly what this method does: it retrieves the view once, then drains
     * `View.dataSourceId` with `View.filter` as the base filter using the same windowed
     * engine as [DataSourcesApi.iterateAllRows].
     *
     * ## What carries over from the view, and what does not
     *
     * - **`filter` — applied.** Read once when iteration starts; later edits to the view
     *   are not picked up.
     * - **`sorts` — not applied.** The drain imposes its own ascending sort on [key];
     *   that is what makes windowing possible. Rows arrive ordered by the key, not in
     *   the view's order. Sort locally if the view's order matters.
     * - **`quick_filters` — not applied.** They are stored as untyped JSON with no
     *   documented query-filter equivalent, so they cannot be translated faithfully.
     *   A view relying on them yields **more** rows here than the view shows.
     * - **Grouping and sub-item scoping** (`configuration.group_by`, `subtasks`) — not
     *   applied. These shape presentation and sub-item inclusion in the UI, not the
     *   data-source row set.
     * - **Full [Page] objects** are emitted, not the `{object, id}` references a view
     *   query returns.
     *
     * ## Limitations inherited from the data source drain
     *
     * The iteration is **not a snapshot**: rows created, deleted, or edited mid-drain may
     * be missed or included, and rows crossing a window boundary can be re-read (they are
     * de-duplicated by id within a boundary bucket). Because Notion limits filter nesting
     * to two levels and the view's filter is combined with the window filter via an `and`
     * compound, a view whose filter is already two levels deep cannot be windowed — the
     * API rejects the combined filter. See [DataSourcesApi.iterateAllRows] and
     * [RowIterationKey] for the per-key guarantees.
     *
     * Example:
     * ```kotlin
     * client.views.iterateAllRows("view-id").collect { page -> process(page) }
     * ```
     *
     * @param viewId The UUID of the view to drain
     * @param key The monotonic key used for windowing (defaults to `created_time`)
     * @return Flow<Page> emitting every row matching the view's filter, ordered ascending by [key]
     * @throws NotionException.ValidationError if the view has no `data_source_id`
     *   (e.g. a dashboard view), so there is no row set to drain
     * @throws NotionException.IterationStalled if a truncated window yields no new rows
     * @throws NotionException.ApiError for API-level errors
     * @throws NotionException.NetworkError for network failures
     */
    fun iterateAllRows(
        viewId: String,
        key: RowIterationKey = RowIterationKey.CreatedTime,
    ): Flow<Page> =
        flow {
            val view = retrieve(viewId)
            val dataSourceId =
                view.dataSourceId
                    ?: throw NotionException.ValidationError(
                        field = "data_source_id",
                        details =
                            "View $viewId has no data_source_id, so it has no row set to drain. " +
                                "Dashboard views aggregate other views and cannot be iterated.",
                    )
            emitAll(
                dataSources.iterateAllRows(
                    dataSourceId = dataSourceId,
                    request = DataSourceQueryRequest(filter = view.filter),
                    key = key,
                ),
            )
        }

    /**
     * Collects every row behind a view into a list, windowing past Notion's 10,000-row cap.
     *
     * Convenience wrapper around [iterateAllRows] that loads every row into memory — for
     * very large views prefer the Flow variant and process rows as they arrive. See
     * [iterateAllRows] for which parts of the view configuration carry over and for the
     * consistency guarantees.
     *
     * @param viewId The UUID of the view to drain
     * @param key The monotonic key used for windowing (defaults to `created_time`)
     * @return All rows matching the view's filter, ordered ascending by [key]
     */
    suspend fun collectAllRows(
        viewId: String,
        key: RowIterationKey = RowIterationKey.CreatedTime,
    ): List<Page> = iterateAllRows(viewId, key).toList()

    /** Data source access used by [iterateAllRows]; views cannot be windowed directly. */
    private val dataSources by lazy { DataSourcesApi(httpClient, config) }
}
