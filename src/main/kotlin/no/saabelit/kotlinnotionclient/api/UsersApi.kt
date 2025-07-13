package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.users.User
import no.saabelit.kotlinnotionclient.ratelimit.executeWithRateLimit

/**
 * API client for Notion Users endpoints.
 *
 * Handles operations related to users in the Notion workspace,
 * including retrieving information about the bot user associated
 * with the current API token.
 */
class UsersApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Retrieves the bot User associated with the API token provided in the authorization header.
     *
     * The bot will have an owner field with information about the person who authorized the integration.
     *
     * @return User object representing the bot associated with the API token
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun getCurrentUser(): User =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse = httpClient.get("${config.baseUrl}/users/me")

                if (response.status.isSuccess()) {
                    response.body<User>()
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
