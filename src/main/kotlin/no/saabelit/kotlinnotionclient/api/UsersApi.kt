package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.users.User
import no.saabelit.kotlinnotionclient.models.users.UserList
import no.saabelit.kotlinnotionclient.ratelimit.executeWithRateLimit

/**
 * API client for Notion Users endpoints.
 *
 * Handles operations related to users in the Notion workspace, including:
 * - Retrieving information about specific users
 * - Listing all users in the workspace
 * - Getting information about the bot user associated with the current API token
 *
 * **Important**: The `retrieve()` and `list()` methods require the integration
 * to have **user information capabilities**. Without these capabilities, the API
 * will return a 403 Forbidden error.
 */
class UsersApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Retrieves a User object using the ID specified.
     *
     * **Required capability**: User information capabilities
     *
     * @param userId Identifier for a Notion user
     * @return User object for the specified user ID
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieve(userId: String): User =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse = httpClient.get("${config.baseUrl}/users/$userId")

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

    /**
     * Returns a paginated list of Users for the workspace.
     *
     * **Important notes**:
     * - Requires user information capabilities (returns 403 Forbidden without them)
     * - Guests are not included in the response
     * - The API does not currently support filtering users by email and/or name
     * - Maximum page size is 100 users
     *
     * @param startCursor Pagination cursor for retrieving next page of results
     * @param pageSize Number of users to return per page (max 100, default 100)
     * @return UserList containing the list of users with pagination info
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun list(
        startCursor: String? = null,
        pageSize: Int? = null,
    ): UserList {
        // Validate parameters before making the request
        pageSize?.let {
            require(it in 1..100) { "Page size must be between 1 and 100, but was $it" }
        }

        return httpClient.executeWithRateLimit {
            try {
                val url =
                    buildString {
                        append("${config.baseUrl}/users")
                        val params = mutableListOf<String>()
                        startCursor?.let { params.add("start_cursor=$it") }
                        pageSize?.let { params.add("page_size=$it") }
                        if (params.isNotEmpty()) {
                            append("?${params.joinToString("&")}")
                        }
                    }

                val response: HttpResponse = httpClient.get(url)

                if (response.status.isSuccess()) {
                    response.body<UserList>()
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
