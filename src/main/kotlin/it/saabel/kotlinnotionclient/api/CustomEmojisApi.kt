package it.saabel.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.base.CustomEmojiList
import it.saabel.kotlinnotionclient.models.base.CustomEmojiObject
import it.saabel.kotlinnotionclient.ratelimit.executeWithRateLimit
import it.saabel.kotlinnotionclient.utils.Pagination
import kotlinx.coroutines.flow.Flow

/**
 * API client for the Notion Custom Emojis endpoint.
 *
 * Provides access to workspace-specific custom emojis via GET /v1/custom_emojis.
 * Custom emojis can be used as icons on pages, databases, and blocks.
 */
class CustomEmojisApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Returns a paginated list of custom emojis in the workspace.
     *
     * @param startCursor Pagination cursor for retrieving the next page of results
     * @param pageSize Number of results to return per page (1–100)
     * @param name Optional exact-match filter on the emoji name
     * @return [CustomEmojiList] containing results with pagination info
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun list(
        startCursor: String? = null,
        pageSize: Int? = null,
        name: String? = null,
    ): CustomEmojiList {
        pageSize?.let {
            require(it in 1..100) { "Page size must be between 1 and 100, but was $it" }
        }

        return httpClient.executeWithRateLimit {
            try {
                val url =
                    buildString {
                        append("${config.baseUrl}/custom_emojis")
                        val params = mutableListOf<String>()
                        startCursor?.let { params.add("start_cursor=$it") }
                        pageSize?.let { params.add("page_size=$it") }
                        name?.let { params.add("name=$it") }
                        if (params.isNotEmpty()) {
                            append("?${params.joinToString("&")}")
                        }
                    }

                val response: HttpResponse = httpClient.get(url)

                if (response.status.isSuccess()) {
                    response.body<CustomEmojiList>()
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
                throw e
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }
    }

    /**
     * Lists all custom emojis as a Flow, optionally filtered by name.
     *
     * @param name Optional exact-match filter on the emoji name
     * @return Flow<CustomEmojiObject> that emits individual emojis from all result pages
     */
    fun listAsFlow(name: String? = null): Flow<CustomEmojiObject> =
        Pagination.asFlow { cursor ->
            list(startCursor = cursor, pageSize = 100, name = name)
        }

    /**
     * Lists custom emojis and returns response pages as a Flow.
     *
     * @param name Optional exact-match filter on the emoji name
     * @return Flow<CustomEmojiList> that emits complete response pages
     */
    fun listPagedFlow(name: String? = null): Flow<CustomEmojiList> =
        Pagination.asPagesFlow { cursor ->
            list(startCursor = cursor, pageSize = 100, name = name)
        }
}
