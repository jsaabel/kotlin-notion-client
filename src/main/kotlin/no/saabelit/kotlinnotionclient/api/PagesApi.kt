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
import no.saabelit.kotlinnotionclient.models.pages.UpdatePageRequest
import no.saabelit.kotlinnotionclient.ratelimit.executeWithRateLimit

/**
 * API client for Notion Pages endpoints.
 *
 * Handles operations related to pages in Notion workspaces,
 * including retrieving page information and content.
 */
class PagesApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
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
     * Creates a new page in the specified parent.
     *
     * Pages can be created as children of other pages or as entries in databases.
     * The properties must conform to the parent database schema if the parent is a database.
     *
     * @param request The page creation request with parent, properties, and optional content
     * @return Page object representing the created page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun create(request: CreatePageRequest): Page =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.post("${config.baseUrl}/pages") {
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
     * Updates an existing page's properties, icon, cover, or archived status.
     *
     * @param pageId The ID of the page to update
     * @param request The update request with modified properties
     * @return Page object representing the updated page
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun update(
        pageId: String,
        request: UpdatePageRequest,
    ): Page =
        httpClient.executeWithRateLimit {
            try {
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
}
