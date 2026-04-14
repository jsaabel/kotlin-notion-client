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
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.databases.ArchiveDatabaseRequest
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseRequest
import it.saabel.kotlinnotionclient.models.databases.Database
import it.saabel.kotlinnotionclient.models.databases.DatabaseRequestBuilder
import it.saabel.kotlinnotionclient.models.databases.databaseRequest
import it.saabel.kotlinnotionclient.models.datasources.UpdateDataSourceRequest
import it.saabel.kotlinnotionclient.ratelimit.executeWithRateLimit
import it.saabel.kotlinnotionclient.validation.RequestValidator
import it.saabel.kotlinnotionclient.validation.ValidationConfig
import it.saabel.kotlinnotionclient.validation.ValidationException

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
                    val database = response.body<Database>()
                    // Propagate the icon to the initial data source.
                    // In the 2025-09-03 API, the UI renders the data source view, not the
                    // database container, so an icon set on the container won't appear unless
                    // it is also set on the data source.
                    if (finalRequest.icon != null) {
                        val dataSourceId = database.dataSources.firstOrNull()?.id
                        if (dataSourceId != null) {
                            val iconPatchResponse: HttpResponse =
                                httpClient.patch("${config.baseUrl}/data_sources/$dataSourceId") {
                                    contentType(ContentType.Application.Json)
                                    setBody(UpdateDataSourceRequest(icon = finalRequest.icon))
                                }
                            if (!iconPatchResponse.status.isSuccess()) {
                                val errorBody =
                                    try {
                                        iconPatchResponse.body<String>()
                                    } catch (e: Exception) {
                                        "Could not read error response body"
                                    }
                                throw NotionException.ApiError(
                                    code = iconPatchResponse.status.value.toString(),
                                    status = iconPatchResponse.status.value,
                                    details =
                                        "HTTP ${iconPatchResponse.status.value}: " +
                                            "${iconPatchResponse.status.description}. Response: $errorBody",
                                )
                            }
                        }
                    }
                    database
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
     * Moves a database to trash by setting its in_trash property to true.
     *
     * Notion doesn't support permanent deletion - databases are moved to trash instead.
     * Databases in trash are no longer accessible through the UI but can still
     * be retrieved via the API.
     *
     * @param databaseId The ID of the database to trash
     * @return Database object representing the trashed database
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun trash(databaseId: String): Database =
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
