package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.saabelit.kotlinnotionclient.config.NotionApiLimits
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.comments.Comment
import no.saabelit.kotlinnotionclient.models.comments.CommentList
import no.saabelit.kotlinnotionclient.models.comments.CreateCommentRequest
import no.saabelit.kotlinnotionclient.models.comments.CreateCommentRequestBuilder
import no.saabelit.kotlinnotionclient.models.comments.commentRequest
import no.saabelit.kotlinnotionclient.ratelimit.executeWithRateLimit

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
     * Retrieves all comments for a specified page or block.
     *
     * Automatically fetches all comments by handling pagination transparently.
     * Returns all comments in a single list.
     *
     * @param blockId The ID of the block to retrieve comments for
     * @return List of all comments across all result pages
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieve(blockId: String): List<Comment> {
        val allComments = mutableListOf<Comment>()
        var currentCursor: String? = null
        var pageCount = 0

        do {
            val response = retrievePage(blockId, currentCursor, NotionApiLimits.Response.MAX_PAGE_SIZE)
            allComments.addAll(response.results)

            currentCursor = response.nextCursor
            pageCount++

            // Safety check to prevent infinite loops
            val maxPages = 50 // Should be plenty for comments
            if (pageCount >= maxPages) {
                throw NotionException.ApiError(
                    code = "PAGINATION_LIMIT_EXCEEDED",
                    status = 500,
                    details =
                        "Comments retrieval exceeded $maxPages pages. " +
                            "This may indicate an infinite loop or an extremely large comment thread.",
                )
            }
        } while (response.hasMore)

        return allComments
    }

    /**
     * Retrieves a single page of comments.
     *
     * This is the low-level method that handles a single API request. Most users should
     * use the `retrieve` method instead, which automatically handles pagination.
     *
     * @param blockId The ID of the block to retrieve comments for
     * @param startCursor Pagination cursor for retrieving next page of results
     * @param pageSize Number of comments to return (max 100)
     * @return CommentList containing a single page of comments
     */
    private suspend fun retrievePage(
        blockId: String,
        startCursor: String? = null,
        pageSize: Int? = null,
    ): CommentList =
        httpClient.executeWithRateLimit {
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

    /**
     * Creates a new comment on a page or block.
     *
     * @param request The comment creation request containing parent, rich text, and optional properties
     * @return Comment The created comment
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws IllegalArgumentException if attachments exceed limit of 3
     */
    suspend fun create(request: CreateCommentRequest): Comment =
        try {
            // Validate attachment limit
            request.attachments?.let { attachments ->
                if (attachments.size > 3) {
                    throw IllegalArgumentException("Comments can have a maximum of 3 attachments, but ${attachments.size} were provided")
                }
            }

            // Validate rich text is not empty
            if (request.richText.isEmpty()) {
                throw IllegalArgumentException("Comment rich text cannot be empty")
            }

            val url = "${config.baseUrl}/comments"
            val response: HttpResponse =
                httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (response.status.isSuccess()) {
                response.body<Comment>()
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
        } catch (e: IllegalArgumentException) {
            throw e // Re-throw validation errors as-is
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Creates a new comment on a page or block using the DSL builder.
     *
     * This is a convenience method that provides a fluent DSL for creating comments
     * without manually constructing CreateCommentRequest objects.
     *
     * ## Basic Usage:
     * ```kotlin
     * val comment = client.comments.create {
     *     parent {
     *         pageId("12345678-1234-1234-1234-123456789abc")
     *     }
     *     content {
     *         text("This is a comment with ")
     *         bold("formatted text")
     *         text("!")
     *     }
     * }
     * ```
     *
     * ## Advanced Usage:
     * ```kotlin
     * val comment = client.comments.create {
     *     parent {
     *         blockId("87654321-4321-4321-4321-210987654321")
     *     }
     *     content {
     *         text("Replying to discussion with ")
     *         italic("styled text")
     *     }
     *     discussionId("existing-discussion-id")
     *     displayName("Custom Bot Name")
     * }
     * ```
     *
     * @param builder DSL block for building the comment request
     * @return Comment The created comment
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws IllegalArgumentException if validation fails (empty content, too many attachments)
     * @throws IllegalStateException if required fields are not set in the DSL
     */
    suspend fun create(builder: CreateCommentRequestBuilder.() -> Unit): Comment {
        val request = commentRequest(builder)
        return create(request)
    }
}
