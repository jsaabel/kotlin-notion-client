package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.comments.CommentList

/**
 * API client for Notion Comments endpoints.
 *
 * Handles operations related to comments in Notion pages and blocks,
 * including retrieving comment discussions and managing comments.
 */
class CommentsApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Retrieves a list of comments for a specified page or block.
     *
     * @param blockId The ID of the block to retrieve comments for
     * @param startCursor Pagination cursor for retrieving next page of results
     * @param pageSize Number of comments to return (max 100)
     * @return CommentList containing comments
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieve(
        blockId: String,
        startCursor: String? = null,
        pageSize: Int? = null,
    ): CommentList =
        try {
            val url =
                buildString {
                    append("${config.baseUrl}/comments")
                    val params = mutableListOf<String>()
                    params.add("block_id=$blockId")
                    startCursor?.let { params.add("start_cursor=$it") }
                    pageSize?.let { params.add("page_size=$it") }
                    append("?${params.joinToString("&")}")
                }

            val response: HttpResponse = httpClient.get(url)

            if (response.status.isSuccess()) {
                response.body<CommentList>()
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
