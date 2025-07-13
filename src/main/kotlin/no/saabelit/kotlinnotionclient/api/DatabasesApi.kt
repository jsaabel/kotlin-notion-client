package no.saabelit.kotlinnotionclient.api

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
import no.saabelit.kotlinnotionclient.config.NotionApiLimits
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.databases.ArchiveDatabaseRequest
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseRequest
import no.saabelit.kotlinnotionclient.models.databases.Database
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryRequest
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryResponse
import no.saabelit.kotlinnotionclient.ratelimit.executeWithRateLimit

/**
 * API client for Notion Databases endpoints.
 *
 * Handles operations related to databases in Notion workspaces,
 * including retrieving database information and schemas.
 */
class DatabasesApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Retrieves a database object using the ID specified.
     *
     * @param databaseId The ID of the database to retrieve
     * @return Database object with all properties and schema
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieve(databaseId: String): Database =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse = httpClient.get("${config.baseUrl}/databases/$databaseId")

                if (response.status.isSuccess()) {
                    response.body<Database>()
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
     * Creates a new database in the specified parent page.
     *
     * @param request The database creation request with title, properties, and parent
     * @return Database object representing the created database
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun create(request: CreateDatabaseRequest): Database =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.post("${config.baseUrl}/databases") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<Database>()
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
     * Archives a database by setting its archived property to true.
     *
     * Notion doesn't support true deletion - objects are archived instead.
     * Archived databases are no longer accessible through the UI but can still
     * be retrieved via the API.
     *
     * @param databaseId The ID of the database to archive
     * @return Database object representing the archived database
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun archive(databaseId: String): Database =
        httpClient.executeWithRateLimit {
            try {
                val request = ArchiveDatabaseRequest(archived = true)
                val response: HttpResponse =
                    httpClient.patch("${config.baseUrl}/databases/$databaseId") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<Database>()
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
     * Queries a database with optional filtering and sorting.
     *
     * Automatically fetches all pages that match the query criteria by handling
     * pagination transparently. Returns all matching pages in a single list.
     *
     * @param databaseId The ID of the database to query
     * @param request The query request with filters and sorts
     * @return List of all matching pages across all result pages
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun query(
        databaseId: String,
        request: DatabaseQueryRequest = DatabaseQueryRequest(),
    ): List<no.saabelit.kotlinnotionclient.models.pages.Page> {
        val allPages = mutableListOf<no.saabelit.kotlinnotionclient.models.pages.Page>()
        var currentCursor: String? = null
        var pageCount = 0

        do {
            val paginatedRequest =
                request.copy(
                    startCursor = currentCursor,
                    pageSize = NotionApiLimits.Response.MAX_PAGE_SIZE,
                )

            val response = querySinglePage(databaseId, paginatedRequest)
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
                        "Database query exceeded $maxPages pages " +
                            "(${maxPages * NotionApiLimits.Response.MAX_PAGE_SIZE} records). " +
                            "This may indicate an infinite loop or an extremely large database.",
                )
            }
        } while (response.hasMore)

        return allPages
    }

    /**
     * Queries a single page of database results.
     *
     * This is the low-level method that handles a single API request. Most users should
     * use the `query` method instead, which automatically handles pagination.
     *
     * @param databaseId The ID of the database to query
     * @param request The query request with filters, sorts, and pagination parameters
     * @return DatabaseQueryResponse containing a single page of results
     */
    private suspend fun querySinglePage(
        databaseId: String,
        request: DatabaseQueryRequest,
    ): DatabaseQueryResponse =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.post("${config.baseUrl}/databases/$databaseId/query") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<DatabaseQueryResponse>()
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
}
