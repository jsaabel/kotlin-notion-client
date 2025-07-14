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
import no.saabelit.kotlinnotionclient.config.NotionApiLimits
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.blocks.Block
import no.saabelit.kotlinnotionclient.models.blocks.BlockList
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.models.blocks.PageContentBuilder
import no.saabelit.kotlinnotionclient.models.blocks.pageContent
import no.saabelit.kotlinnotionclient.ratelimit.executeWithRateLimit
import no.saabelit.kotlinnotionclient.validation.RequestValidator
import no.saabelit.kotlinnotionclient.validation.ValidationConfig
import no.saabelit.kotlinnotionclient.validation.ValidationException

/**
 * API client for Notion Blocks endpoints.
 *
 * Handles operations related to blocks in Notion pages,
 * including retrieving block information and managing block content.
 *
 * Features proactive validation to prevent API errors and provide helpful feedback
 * about content that exceeds Notion's API limits before making HTTP requests.
 */
class BlocksApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
    private val validationConfig: ValidationConfig = ValidationConfig.default(),
) {
    private val validator = RequestValidator(validationConfig)

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
     * Retrieves all child blocks for the specified block.
     *
     * Automatically fetches all child blocks by handling pagination transparently.
     * Returns all child blocks in a single list.
     *
     * @param blockId The ID of the parent block
     * @return List of all child blocks across all result pages
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieveChildren(blockId: String): List<Block> {
        val allBlocks = mutableListOf<Block>()
        var currentCursor: String? = null
        var pageCount = 0

        do {
            val response = retrieveChildrenPage(blockId, currentCursor, NotionApiLimits.Response.MAX_PAGE_SIZE)
            allBlocks.addAll(response.results)

            currentCursor = response.nextCursor
            pageCount++

            // Safety check to prevent infinite loops
            val maxPages = 100 // Should be plenty for block children
            if (pageCount >= maxPages) {
                throw NotionException.ApiError(
                    code = "PAGINATION_LIMIT_EXCEEDED",
                    status = 500,
                    details =
                        "Block children retrieval exceeded $maxPages pages. " +
                            "This may indicate an infinite loop or an extremely large block structure.",
                )
            }
        } while (response.hasMore)

        return allBlocks
    }

    /**
     * Retrieves a single page of child blocks.
     *
     * This is the low-level method that handles a single API request. Most users should
     * use the `retrieveChildren` method instead, which automatically handles pagination.
     *
     * @param blockId The ID of the parent block
     * @param startCursor Pagination cursor for retrieving next page of results
     * @param pageSize Number of blocks to return (max 100)
     * @return BlockList containing a single page of child blocks
     */
    private suspend fun retrieveChildrenPage(
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
     * Appends child blocks to a parent block or page using a fluent DSL builder.
     *
     * This is a convenience method that accepts a DSL builder lambda for more natural
     * Kotlin-style API usage. The builder provides a fluent API for constructing blocks.
     *
     * @param blockId The ID of the parent block or page
     * @param builder DSL builder lambda for constructing the block content
     * @return BlockList containing the created child blocks
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails in strict mode
     */
    suspend fun appendChildren(
        blockId: String,
        builder: PageContentBuilder.() -> Unit,
    ): BlockList {
        val children = pageContent(builder)
        return appendChildren(blockId, children)
    }

    /**
     * Appends child blocks to a parent block or page.
     *
     * This method performs proactive validation to check for content that exceeds
     * Notion's API limits before making the HTTP request. Depending on the validation
     * configuration, violations will either cause an exception or be automatically fixed.
     *
     * @param blockId The ID of the parent block or page
     * @param children List of BlockRequest objects to append as children
     * @return BlockList containing the created child blocks
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails in strict mode
     */
    suspend fun appendChildren(
        blockId: String,
        children: List<BlockRequest>,
    ): BlockList {
        validator.validateOrThrow("children", children)

        return httpClient.executeWithRateLimit {
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
}

/**
 * Request body for appending children to a block.
 */
@Serializable
private data class AppendBlockChildrenRequest(
    val children: List<BlockRequest>,
)
