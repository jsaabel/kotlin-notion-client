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
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.databases.ArchiveDatabaseRequest
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseRequest
import no.saabelit.kotlinnotionclient.models.databases.Database
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryRequest
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryResponse

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

    /**
     * Queries a database with optional filtering, sorting, and pagination.
     *
     * Returns pages that are children of the database, filtered and sorted according
     * to the query parameters. This is the primary method for retrieving database content.
     *
     * @param databaseId The ID of the database to query
     * @param request The query request with filters, sorts, and pagination parameters
     * @return DatabaseQueryResponse containing matching pages and pagination info
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun query(
        databaseId: String,
        request: DatabaseQueryRequest = DatabaseQueryRequest(),
    ): DatabaseQueryResponse =
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
