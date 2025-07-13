package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.blocks.Block
import no.saabelit.kotlinnotionclient.models.blocks.BlockList
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.ratelimit.executeWithRateLimit

/**
 * API client for Notion Blocks endpoints.
 *
 * Handles operations related to blocks in Notion pages,
 * including retrieving block information and managing block content.
 */
class BlocksApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Retrieves a block object using the ID specified.
     *
     * @param blockId The ID of the block to retrieve
     * @return Block object with all properties and content
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieve(blockId: String): Block =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse = httpClient.get("${config.baseUrl}/blocks/$blockId")

                if (response.status.isSuccess()) {
                    response.body<Block>()
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
     * Retrieves a list of all child blocks for the specified block.
     *
     * @param blockId The ID of the parent block
     * @param startCursor Pagination cursor for retrieving next page of results
     * @param pageSize Number of blocks to return (max 100)
     * @return BlockList containing child blocks
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieveChildren(
        blockId: String,
        startCursor: String? = null,
        pageSize: Int? = null,
    ): BlockList =
        httpClient.executeWithRateLimit {
            try {
                val url =
                    buildString {
                        append("${config.baseUrl}/blocks/$blockId/children")
                        val params = mutableListOf<String>()
                        startCursor?.let { params.add("start_cursor=$it") }
                        pageSize?.let { params.add("page_size=$it") }
                        if (params.isNotEmpty()) {
                            append("?${params.joinToString("&")}")
                        }
                    }

                val response: HttpResponse = httpClient.get(url)

                if (response.status.isSuccess()) {
                    response.body<BlockList>()
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
     * Appends child blocks to a parent block or page.
     *
     * @param blockId The ID of the parent block or page
     * @param children List of BlockRequest objects to append as children
     * @return BlockList containing the created child blocks
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun appendChildren(
        blockId: String,
        children: List<BlockRequest>,
    ): BlockList =
        httpClient.executeWithRateLimit {
            try {
                val request = AppendBlockChildrenRequest(children = children)
                val response: HttpResponse =
                    httpClient.patch("${config.baseUrl}/blocks/$blockId/children") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<BlockList>()
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
 * Request body for appending children to a block.
 */
@Serializable
private data class AppendBlockChildrenRequest(
    val children: List<BlockRequest>,
)
