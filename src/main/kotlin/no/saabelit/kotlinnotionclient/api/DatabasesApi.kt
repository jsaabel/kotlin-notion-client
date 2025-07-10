package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.databases.Database

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
}
