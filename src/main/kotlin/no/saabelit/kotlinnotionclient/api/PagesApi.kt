@file:Suppress("DuplicatedCode")

package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.pages.ArchivePageRequest
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.Page
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyItemResponse
import no.saabelit.kotlinnotionclient.models.pages.PageRequestBuilder
import no.saabelit.kotlinnotionclient.models.pages.PropertyItem
import no.saabelit.kotlinnotionclient.models.pages.UpdatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.pageRequest
import no.saabelit.kotlinnotionclient.ratelimit.executeWithRateLimit
import no.saabelit.kotlinnotionclient.validation.RequestValidator
import no.saabelit.kotlinnotionclient.validation.ValidationConfig
import no.saabelit.kotlinnotionclient.validation.ValidationException

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
     * @return Page object with all properties and metadata
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieve(pageId: String): Page =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse = httpClient.get("${config.baseUrl}/pages/$pageId")

                if (response.status.isSuccess()) {
                    response.body<Page>()
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
            } catch (e: ClientRequestException) {
                // Handle HTTP client errors (4xx)
                val errorBody =
                    try {
                        e.response.body<String>()
                    } catch (ex: Exception) {
                        "Could not read error response body"
                    }

                throw NotionException.ApiError(
                    code =
                        e.response.status.value
                            .toString(),
                    status = e.response.status.value,
                    details = "HTTP ${e.response.status.value}: ${e.response.status.description}. Response: $errorBody",
                )
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
     * @param builder DSL builder lambda for constructing the page request
     * @return Page object representing the created page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails for non-fixable violations
     */
    suspend fun create(builder: PageRequestBuilder.() -> Unit): Page {
        val request = pageRequest(builder)
        return create(request)
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
     * @return Page object representing the created page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails for non-fixable violations
     */
    suspend fun create(request: CreatePageRequest): Page {
        val finalRequest = validator.validateOrFix(request)

        return httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.post("${config.baseUrl}/pages") {
                        contentType(ContentType.Application.Json)
                        setBody(finalRequest)
                    }

                if (response.status.isSuccess()) {
                    response.body<Page>()
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
            } catch (e: ClientRequestException) {
                // Handle HTTP client errors (4xx)
                val errorBody =
                    try {
                        e.response.body<String>()
                    } catch (ex: Exception) {
                        "Could not read error response body"
                    }

                throw NotionException.ApiError(
                    code =
                        e.response.status.value
                            .toString(),
                    status = e.response.status.value,
                    details = "HTTP ${e.response.status.value}: ${e.response.status.description}. Response: $errorBody",
                )
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
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
     * @return Page object representing the updated page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails for non-fixable violations
     */
    suspend fun update(
        pageId: String,
        request: UpdatePageRequest,
    ): Page {
        val finalRequest = validator.validateOrFix(request)

        return httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.patch("${config.baseUrl}/pages/$pageId") {
                        contentType(ContentType.Application.Json)
                        setBody(finalRequest)
                    }

                if (response.status.isSuccess()) {
                    response.body<Page>()
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
            } catch (e: ClientRequestException) {
                // Handle HTTP client errors (4xx)
                val errorBody =
                    try {
                        e.response.body<String>()
                    } catch (ex: Exception) {
                        "Could not read error response body"
                    }

                throw NotionException.ApiError(
                    code =
                        e.response.status.value
                            .toString(),
                    status = e.response.status.value,
                    details = "HTTP ${e.response.status.value}: ${e.response.status.description}. Response: $errorBody",
                )
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }
    }

    /**
     * Archives a page by setting its archived property to true.
     *
     * Notion doesn't support true deletion - objects are archived instead.
     * Archived pages are no longer accessible through the UI but can still
     * be retrieved via the API.
     *
     * @param pageId The ID of the page to archive
     * @return Page object representing the archived page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun archive(pageId: String): Page =
        httpClient.executeWithRateLimit {
            try {
                val request = ArchivePageRequest()

                val response: HttpResponse =
                    httpClient.patch("${config.baseUrl}/pages/$pageId") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<Page>()
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
            } catch (e: ClientRequestException) {
                // Handle HTTP client errors (4xx)
                val errorBody =
                    try {
                        e.response.body<String>()
                    } catch (ex: Exception) {
                        "Could not read error response body"
                    }

                throw NotionException.ApiError(
                    code =
                        e.response.status.value
                            .toString(),
                    status = e.response.status.value,
                    details = "HTTP ${e.response.status.value}: ${e.response.status.description}. Response: $errorBody",
                )
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }

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
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse = httpClient.get(url)

                if (response.status.isSuccess()) {
                    response.body<PagePropertyItemResponse>()
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
            } catch (e: ClientRequestException) {
                // Handle HTTP client errors (4xx)
                val errorBody =
                    try {
                        e.response.body<String>()
                    } catch (ex: Exception) {
                        "Could not read error response body"
                    }

                throw NotionException.ApiError(
                    code =
                        e.response.status.value
                            .toString(),
                    status = e.response.status.value,
                    details = "HTTP ${e.response.status.value}: ${e.response.status.description}. Response: $errorBody",
                )
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }
}
