@file:Suppress("DuplicatedCode")

package it.saabel.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.exceptions.toNotionApiError
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTask
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.pages.AsyncPageCreateResult
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequestBuilder
import it.saabel.kotlinnotionclient.models.pages.MovePageParent
import it.saabel.kotlinnotionclient.models.pages.MovePageRequest
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PagePropertyItemResponse
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.PropertyItem
import it.saabel.kotlinnotionclient.models.pages.TrashPageRequest
import it.saabel.kotlinnotionclient.models.pages.UpdatePageRequest
import it.saabel.kotlinnotionclient.models.pages.UpdatePageRequestBuilder
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import it.saabel.kotlinnotionclient.models.pages.updatePageRequest
import it.saabel.kotlinnotionclient.utils.Pagination
import it.saabel.kotlinnotionclient.validation.RequestValidator
import it.saabel.kotlinnotionclient.validation.ValidationConfig
import it.saabel.kotlinnotionclient.validation.ValidationException
import kotlinx.coroutines.flow.Flow

/**
 * API client for Notion Pages endpoints.
 *
 * Handles operations related to pages in Notion workspaces,
 * including retrieving page information and content.
 *
 * Features proactive validation to prevent API errors and provide helpful feedback
 * about content that exceeds Notion's API limits before making HTTP requests.
 */
class PagesApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
    private val validationConfig: ValidationConfig = ValidationConfig.default(),
) {
    private val validator = RequestValidator(validationConfig)

    /**
     * Retrieves a page object using the ID specified.
     *
     * @param pageId The ID of the page to retrieve
     * @param filterProperties Optional list of property IDs to restrict the properties returned in
     *   the response. Accepts both the percent-encoded form returned by the data source schema
     *   (e.g. `%7DVpb`) and the decoded form returned elsewhere (e.g. `}Vpb`).
     * @return Page object with all properties and metadata
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieve(
        pageId: String,
        filterProperties: List<String>? = null,
    ): Page {
        validateFilterPropertiesLimit(filterProperties)
        return try {
            val response: HttpResponse =
                httpClient.get("${config.baseUrl}/pages/$pageId") {
                    filterProperties(filterProperties)
                }

            if (response.status.isSuccess()) {
                response.body<Page>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: ClientRequestException) {
            // Handle HTTP client errors (4xx)
            throw e.response.toNotionApiError()
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }
    }

    /**
     * Creates a new page using a fluent DSL builder.
     *
     * This is a convenience method that accepts a DSL builder lambda for more natural
     * Kotlin-style API usage. The builder provides type-safe construction of page requests.
     *
     * @param filterProperties Optional list of property IDs to restrict the properties returned in
     *   the response.
     * @param builder DSL builder lambda for constructing the page request
     * @return Page object representing the created page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails for non-fixable violations
     */
    suspend fun create(
        filterProperties: List<String>? = null,
        builder: CreatePageRequestBuilder.() -> Unit,
    ): Page {
        val request = createPageRequest(builder)
        return create(request, filterProperties)
    }

    /**
     * Creates a new page in the specified parent.
     *
     * Pages can be created as children of other pages or as entries in databases.
     * The properties must conform to the parent database schema if the parent is a database.
     *
     * This method performs proactive validation to check for content that exceeds
     * Notion's API limits before making the HTTP request. Depending on the validation
     * configuration, violations will either cause an exception or be automatically fixed.
     *
     * @param request The page creation request with parent, properties, and optional content
     * @param filterProperties Optional list of property IDs to restrict the properties returned in
     *   the response. Accepts both the percent-encoded form returned by the data source schema
     *   (e.g. `%7DVpb`) and the decoded form returned elsewhere (e.g. `}Vpb`).
     * @return Page object representing the created page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails for non-fixable violations
     */
    suspend fun create(
        request: CreatePageRequest,
        filterProperties: List<String>? = null,
    ): Page {
        requireSynchronous(request.allowAsync)
        validateFilterPropertiesLimit(filterProperties)
        val finalRequest = validator.validateOrFix(request)

        return try {
            val response: HttpResponse =
                httpClient.post("${config.baseUrl}/pages") {
                    contentType(ContentType.Application.Json)
                    filterProperties(filterProperties)
                    setBody(finalRequest)
                }

            if (response.status.isSuccess()) {
                response.body<Page>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: ClientRequestException) {
            // Handle HTTP client errors (4xx)
            throw e.response.toNotionApiError()
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }
    }

    /**
     * Creates a new page whose content is supplied as enhanced Markdown.
     *
     * Convenience wrapper over [create] for the common markdown case — the API converts
     * the markdown string into blocks server-side. The first `# h1` heading becomes the
     * page title when no [title] is given.
     *
     * Requires the integration to have **insert content** capability on [parent].
     * `markdown` is mutually exclusive with block children and with templates.
     *
     * For large markdown bodies that may exceed an HTTP client's timeout budget, prefer
     * [createFromMarkdownAsync].
     *
     * @param parent The parent the page is created under (page or data source)
     * @param markdown The enhanced Markdown content (use `\n` for line breaks)
     * @param title Optional page title; when omitted Notion derives it from the markdown
     * @param filterProperties Optional list of property IDs to restrict the properties returned
     * @return Page object representing the created page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun createFromMarkdown(
        parent: Parent,
        markdown: String,
        title: String? = null,
        filterProperties: List<String>? = null,
    ): Page = create(markdownRequest(parent, markdown, title), filterProperties)

    /**
     * Creates a page from Markdown, opting into asynchronous execution.
     *
     * Sets `allow_async: true` on the request. Per the Jun 29 2026 Notion changelog this is
     * accepted on `POST /v1/pages` **only when a `markdown` body is supplied**. As on the
     * markdown write endpoint, opting in does not force background execution — the API
     * decides, and a 202 cannot be provoked:
     * - HTTP 202: [AsyncPageCreateResult.Accepted] with an [AsyncTask] to poll via
     *   `client.asyncTasks` (e.g. `waitForCompletion(task.id)`)
     * - HTTP 200: the page was created synchronously and [AsyncPageCreateResult.Completed]
     *   carries the [Page]
     *
     * @param parent The parent the page is created under (page or data source)
     * @param markdown The enhanced Markdown content (use `\n` for line breaks)
     * @param title Optional page title; when omitted Notion derives it from the markdown
     * @return The result of the create, either completed or accepted for background execution
     */
    suspend fun createFromMarkdownAsync(
        parent: Parent,
        markdown: String,
        title: String? = null,
    ): AsyncPageCreateResult = createAsync(markdownRequest(parent, markdown, title))

    /**
     * Creates a page asynchronously from an explicit request.
     *
     * `allow_async` is forced to `true` on [request]. See [createFromMarkdownAsync] for the
     * async semantics.
     *
     * @param request The page creation request; must carry a `markdown` body
     * @param filterProperties Optional list of property IDs to restrict the properties returned
     *   when the API answers synchronously
     * @return The result of the create, either completed or accepted for background execution
     * @throws NotionException.ValidationError if [request] carries no `markdown` body —
     *   `allow_async` is only supported for markdown page creates
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun createAsync(
        request: CreatePageRequest,
        filterProperties: List<String>? = null,
    ): AsyncPageCreateResult {
        if (request.markdown == null) {
            throw NotionException.ValidationError(
                field = "allow_async",
                details =
                    "Asynchronous page creation is only supported when the request supplies a " +
                        "markdown body. Set markdown(...) on the request, or use PagesApi.create " +
                        "for a synchronous create.",
            )
        }
        validateFilterPropertiesLimit(filterProperties)
        val finalRequest = validator.validateOrFix(request).copy(allowAsync = true)

        return try {
            val response: HttpResponse =
                httpClient.post("${config.baseUrl}/pages") {
                    contentType(ContentType.Application.Json)
                    filterProperties(filterProperties)
                    setBody(finalRequest)
                }

            when {
                response.status == HttpStatusCode.Accepted -> {
                    AsyncPageCreateResult.Accepted(response.body<AsyncTask>())
                }

                response.status.isSuccess() -> {
                    AsyncPageCreateResult.Completed(response.body<Page>())
                }

                else -> {
                    throw response.toNotionApiError()
                }
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: ClientRequestException) {
            throw e.response.toNotionApiError()
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }
    }

    /**
     * Creates a page asynchronously using the fluent DSL builder.
     *
     * See [createFromMarkdownAsync] for the async semantics. The built request must call
     * `markdown(...)`.
     *
     * @param filterProperties Optional list of property IDs to restrict the properties returned
     *   when the API answers synchronously
     * @param builder DSL builder lambda for constructing the page request
     * @return The result of the create, either completed or accepted for background execution
     */
    suspend fun createAsync(
        filterProperties: List<String>? = null,
        builder: CreatePageRequestBuilder.() -> Unit,
    ): AsyncPageCreateResult = createAsync(createPageRequest(builder), filterProperties)

    private fun markdownRequest(
        parent: Parent,
        markdown: String,
        title: String?,
    ): CreatePageRequest =
        CreatePageRequest(
            parent = parent,
            properties =
                title?.let { mapOf("title" to PagePropertyValue.TitleValue.fromPlainText(it)) }
                    ?: emptyMap(),
            markdown = markdown,
        )

    private fun requireSynchronous(allowAsync: Boolean?) {
        if (allowAsync == true) {
            throw NotionException.ValidationError(
                field = "allow_async",
                details =
                    "This method returns the synchronous response shape and cannot handle an async task. " +
                        "Use PagesApi.createAsync for requests with allow_async = true.",
            )
        }
    }

    /**
     * Updates an existing page's properties, icon, cover, or archived status.
     *
     * This method performs proactive validation to check for content that exceeds
     * Notion's API limits before making the HTTP request. Depending on the validation
     * configuration, violations will either cause an exception or be automatically fixed.
     *
     * @param pageId The ID of the page to update
     * @param request The update request with modified properties
     * @param filterProperties Optional list of property IDs to restrict the properties returned in
     *   the response. Accepts both the percent-encoded form returned by the data source schema
     *   (e.g. `%7DVpb`) and the decoded form returned elsewhere (e.g. `}Vpb`).
     * @return Page object representing the updated page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails for non-fixable violations
     */
    suspend fun update(
        pageId: String,
        request: UpdatePageRequest,
        filterProperties: List<String>? = null,
    ): Page {
        validateFilterPropertiesLimit(filterProperties)
        val finalRequest = validator.validateOrFix(request)

        return try {
            val response: HttpResponse =
                httpClient.patch("${config.baseUrl}/pages/$pageId") {
                    contentType(ContentType.Application.Json)
                    filterProperties(filterProperties)
                    setBody(finalRequest)
                }

            if (response.status.isSuccess()) {
                response.body<Page>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: ClientRequestException) {
            // Handle HTTP client errors (4xx)
            throw e.response.toNotionApiError()
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }
    }

    /**
     * Updates an existing page using a fluent DSL builder.
     *
     * This is a convenience method that accepts a DSL builder lambda for more natural
     * Kotlin-style API usage. The builder provides type-safe construction of update requests.
     *
     * @param pageId The ID of the page to update
     * @param filterProperties Optional list of property IDs to restrict the properties returned in
     *   the response.
     * @param builder DSL builder lambda for constructing the update request
     * @return Page object representing the updated page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails for non-fixable violations
     */
    suspend fun update(
        pageId: String,
        filterProperties: List<String>? = null,
        builder: UpdatePageRequestBuilder.() -> Unit,
    ): Page {
        val request = updatePageRequest(builder)
        return update(pageId, request, filterProperties)
    }

    /**
     * Moves a page to trash by setting its in_trash property to true.
     *
     * Notion doesn't support permanent deletion - pages are moved to trash instead.
     * Pages in trash are no longer accessible through the UI but can still
     * be retrieved via the API.
     *
     * @param pageId The ID of the page to trash
     * @return Page object representing the trashed page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun trash(pageId: String): Page =
        try {
            val request = TrashPageRequest()

            val response: HttpResponse =
                httpClient.patch("${config.baseUrl}/pages/$pageId") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (response.status.isSuccess()) {
                response.body<Page>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: ClientRequestException) {
            // Handle HTTP client errors (4xx)
            throw e.response.toNotionApiError()
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Moves a page to a new parent location.
     *
     * The page being moved must be a regular Notion page, not a database.
     * The integration must have appropriate permissions on both source and destination.
     *
     * @param pageId The ID of the page to move
     * @param parent The new parent (page or data source)
     * @return Page object representing the moved page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun move(
        pageId: String,
        parent: MovePageParent,
    ): Page =
        try {
            val request = MovePageRequest(parent = parent)

            val response: HttpResponse =
                httpClient.post("${config.baseUrl}/pages/$pageId/move") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (response.status.isSuccess()) {
                response.body<Page>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: ClientRequestException) {
            // Handle HTTP client errors (4xx)
            throw e.response.toNotionApiError()
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Moves a page to be a child of another page.
     *
     * Convenience method for moving to a page parent.
     *
     * @param pageId The ID of the page to move
     * @param parentPageId The ID of the new parent page
     * @return Page object representing the moved page
     */
    suspend fun moveToPage(
        pageId: String,
        parentPageId: String,
    ): Page = move(pageId, MovePageParent.PageParent(pageId = parentPageId))

    /**
     * Moves a page into a data source (database).
     *
     * Convenience method for moving to a data source parent.
     *
     * @param pageId The ID of the page to move
     * @param dataSourceId The ID of the target data source
     * @return Page object representing the moved page
     */
    suspend fun moveToDataSource(
        pageId: String,
        dataSourceId: String,
    ): Page = move(pageId, MovePageParent.DataSourceParent(dataSourceId = dataSourceId))

    /**
     * Retrieves all items for a specific page property that may be paginated.
     *
     * This method automatically handles pagination for properties like relations
     * that may have more items than the API returns by default (e.g., >20 relations).
     * Returns all property items in a single list.
     *
     * @param pageId The ID of the page containing the property
     * @param propertyId The ID of the property to retrieve items for
     * @return List of all property items across all pages
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrievePropertyItems(
        pageId: String,
        propertyId: String,
    ): List<PropertyItem> {
        val allItems = mutableListOf<PropertyItem>()
        var currentCursor: String? = null
        var pageCount = 0

        do {
            val url =
                buildString {
                    append("${config.baseUrl}/pages/$pageId/properties/$propertyId")
                    if (currentCursor != null) {
                        append("?start_cursor=$currentCursor")
                    }
                }

            val response = retrievePropertyItemsPage(url)
            allItems.addAll(response.results)

            currentCursor = response.nextCursor
            pageCount++

            // Safety check to prevent infinite loops
            val maxPages = 100 // Should be plenty for relation properties
            if (pageCount >= maxPages) {
                throw NotionException.ApiError(
                    code = "PAGINATION_LIMIT_EXCEEDED",
                    status = 500,
                    details =
                        "Property retrieval exceeded $maxPages pages. " +
                            "This may indicate an infinite loop or an extremely large property.",
                )
            }
        } while (response.hasMore)

        return allItems
    }

    /**
     * Retrieves a single page of property items.
     */
    private suspend fun retrievePropertyItemsPage(url: String): PagePropertyItemResponse =
        try {
            val response: HttpResponse = httpClient.get(url)

            if (response.status.isSuccess()) {
                response.body<PagePropertyItemResponse>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: ClientRequestException) {
            // Handle HTTP client errors (4xx)
            throw e.response.toNotionApiError()
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    // ========== Pagination Helper Methods ==========

    /**
     * Retrieves property items as a Flow for reactive processing.
     *
     * This method emits individual property items as they become available, enabling
     * efficient memory usage for properties with many items (e.g., large relation lists).
     *
     * Example usage:
     * ```kotlin
     * client.pages.retrievePropertyItemsAsFlow("page-id", "property-id").collect { item ->
     *     println("Processing property item...")
     *     // Process each item individually
     * }
     * ```
     *
     * @param pageId The ID of the page containing the property
     * @param propertyId The ID of the property to retrieve items for
     * @return Flow<PropertyItem> that emits individual property items from all result pages
     */
    fun retrievePropertyItemsAsFlow(
        pageId: String,
        propertyId: String,
    ): Flow<PropertyItem> =
        Pagination.asFlow { cursor ->
            val url =
                buildString {
                    append("${config.baseUrl}/pages/$pageId/properties/$propertyId")
                    if (cursor != null) {
                        append("?start_cursor=$cursor")
                    }
                }
            retrievePropertyItemsPage(url)
        }

    /**
     * Retrieves property items and returns response pages as a Flow.
     *
     * Unlike [retrievePropertyItemsAsFlow], this emits complete [PagePropertyItemResponse] objects,
     * allowing access to pagination metadata alongside results.
     *
     * Example usage:
     * ```kotlin
     * client.pages.retrievePropertyItemsPagedFlow("page-id", "property-id").collect { response ->
     *     println("Got ${response.results.size} items (has more: ${response.hasMore})")
     *     response.results.forEach { item -> /* process item */ }
     * }
     * ```
     *
     * @param pageId The ID of the page containing the property
     * @param propertyId The ID of the property to retrieve items for
     * @return Flow<PagePropertyItemResponse> that emits complete response pages
     */
    fun retrievePropertyItemsPagedFlow(
        pageId: String,
        propertyId: String,
    ): Flow<PagePropertyItemResponse> =
        Pagination.asPagesFlow { cursor ->
            val url =
                buildString {
                    append("${config.baseUrl}/pages/$pageId/properties/$propertyId")
                    if (cursor != null) {
                        append("?start_cursor=$cursor")
                    }
                }
            retrievePropertyItemsPage(url)
        }
}
