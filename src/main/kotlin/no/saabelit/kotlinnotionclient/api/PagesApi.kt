package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.pages.Page

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
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }
}
