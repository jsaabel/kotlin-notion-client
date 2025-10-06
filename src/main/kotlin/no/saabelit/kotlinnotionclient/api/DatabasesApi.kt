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
import no.saabelit.kotlinnotionclient.models.databases.DatabaseRequestBuilder
import no.saabelit.kotlinnotionclient.models.databases.databaseRequest
import no.saabelit.kotlinnotionclient.ratelimit.executeWithRateLimit
import no.saabelit.kotlinnotionclient.validation.RequestValidator
import no.saabelit.kotlinnotionclient.validation.ValidationConfig
import no.saabelit.kotlinnotionclient.validation.ValidationException

/**
 * API client for Notion Databases endpoints (API version 2025-09-03+).
 *
 * As of the 2025-09-03 API version, databases are containers that can hold
 * multiple data sources. This API handles container-level operations:
 * - Retrieving database metadata and list of data sources
 * - Creating databases with initial data source
 * - Updating database container properties (title, icon, cover, parent)
 * - Archiving databases
 *
 * For operations on individual data sources (querying, updating schema):
 * @see DataSourcesApi
 */
class DatabasesApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
    private val validationConfig: ValidationConfig = ValidationConfig.default(),
) {
    private val validator = RequestValidator(validationConfig)

    /**
     * Retrieves a database object using the ID specified (API version 2025-09-03+).
     *
     * Returns database container metadata including list of data sources.
     * To get the schema/properties of a specific data source, use DataSourcesApi.retrieve()
     *
     * @param databaseId The ID of the database to retrieve
     * @return Database object with data_sources array (no longer includes properties)
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @see DataSourcesApi.retrieve
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
     * Creates a new database using a fluent DSL builder.
     *
     * This is a convenience method that accepts a DSL builder lambda for more natural
     * Kotlin-style API usage. The builder provides type-safe construction of database requests.
     *
     * @param builder DSL builder lambda for constructing the database request
     * @return Database object representing the created database
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails for non-fixable violations
     */
    suspend fun create(builder: DatabaseRequestBuilder.() -> Unit): Database {
        val request = databaseRequest(builder)
        return create(request)
    }

    /**
     * Creates a new database in the specified parent page.
     *
     * @param request The database creation request with title, properties, and parent
     * @return Database object representing the created database
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails for non-fixable violations
     */
    suspend fun create(request: CreateDatabaseRequest): Database {
        val finalRequest = validator.validateOrFix(request)

        return httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.post("${config.baseUrl}/databases") {
                        contentType(ContentType.Application.Json)
                        setBody(finalRequest)
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
                val request = ArchiveDatabaseRequest(inTrash = true)
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
}
