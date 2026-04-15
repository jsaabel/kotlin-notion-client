package it.saabel.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.markdown.ContentUpdate
import it.saabel.kotlinnotionclient.models.markdown.PageMarkdownResponse
import it.saabel.kotlinnotionclient.models.markdown.ReplaceContentBody
import it.saabel.kotlinnotionclient.models.markdown.ReplaceContentRequest
import it.saabel.kotlinnotionclient.models.markdown.UpdateContentBody
import it.saabel.kotlinnotionclient.models.markdown.UpdateContentRequest
import it.saabel.kotlinnotionclient.ratelimit.executeWithRateLimit

/**
 * API client for the Notion Page Markdown endpoints.
 *
 * Provides access to page content as enhanced Markdown. Useful for reading page
 * content in a portable format and making programmatic edits via search-and-replace
 * or full-content replacement.
 *
 * Your integration must have **read content** capabilities to call [retrieve], and
 * **update content** capabilities to call [updateContent] or [replaceContent].
 *
 * Example usage:
 * ```kotlin
 * // Retrieve a page as markdown
 * val response = client.markdown.retrieve("page-id")
 * println(response.markdown)
 * println("Truncated: ${response.truncated}")
 *
 * // Replace entire page content
 * val updated = client.markdown.replaceContent("page-id", "# Hello\n\nNew content.")
 *
 * // Targeted search-and-replace
 * val patched = client.markdown.updateContent("page-id") {
 *     replace("old text", "new text")
 *     replaceAll("foo", "bar")
 * }
 * ```
 *
 * @property httpClient The HTTP client for making requests
 * @property config The Notion API configuration
 */
class MarkdownApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Retrieves a page's content rendered as enhanced Markdown.
     *
     * If the page has more than ~20,000 blocks, the response will be [PageMarkdownResponse.truncated].
     * The truncated block IDs are available in [PageMarkdownResponse.unknownBlockIds] and can be
     * submitted as a new page_id to retrieve the remaining content.
     *
     * @param pageId The ID of the page (or block) to retrieve
     * @param includeTranscript Include meeting note transcripts. Defaults to false.
     * @return Page content as markdown
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun retrieve(
        pageId: String,
        includeTranscript: Boolean = false,
    ): PageMarkdownResponse =
        httpClient.executeWithRateLimit {
            try {
                val url =
                    buildString {
                        append("${config.baseUrl}/pages/$pageId/markdown")
                        if (includeTranscript) append("?include_transcript=true")
                    }
                val response: HttpResponse = httpClient.get(url)

                if (response.status.isSuccess()) {
                    response.body<PageMarkdownResponse>()
                } else {
                    val errorBody = readErrorBody(response)
                    throw NotionException.ApiError(
                        code = response.status.value.toString(),
                        status = response.status.value,
                        details = "HTTP ${response.status.value}: ${response.status.description}. Response: $errorBody",
                    )
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: ClientRequestException) {
                throw clientError(e)
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }

    /**
     * Updates a page's content using targeted search-and-replace operations.
     *
     * This is the recommended update approach for partial edits. Up to 100 operations
     * can be provided in a single request.
     *
     * @param pageId The ID of the page to update
     * @param request The update_content request with one or more search-and-replace operations
     * @return Updated page content as markdown
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun updateContent(
        pageId: String,
        request: UpdateContentRequest,
    ): PageMarkdownResponse = patch(pageId, request)

    /**
     * Updates a page's content using targeted search-and-replace operations.
     *
     * Convenience overload that builds the request from a list of [ContentUpdate] operations.
     *
     * @param pageId The ID of the page to update
     * @param contentUpdates The list of search-and-replace operations (max 100)
     * @param allowDeletingContent Whether child pages/databases may be deleted. Defaults to false.
     * @return Updated page content as markdown
     */
    suspend fun updateContent(
        pageId: String,
        contentUpdates: List<ContentUpdate>,
        allowDeletingContent: Boolean? = null,
    ): PageMarkdownResponse =
        updateContent(
            pageId,
            UpdateContentRequest(
                updateContent =
                    UpdateContentBody(
                        contentUpdates = contentUpdates,
                        allowDeletingContent = allowDeletingContent,
                    ),
            ),
        )

    /**
     * Updates a page's content using targeted search-and-replace operations via a DSL builder.
     *
     * @param pageId The ID of the page to update
     * @param allowDeletingContent Whether child pages/databases may be deleted. Defaults to false.
     * @param builder DSL block for adding [ContentUpdate] operations
     * @return Updated page content as markdown
     */
    suspend fun updateContent(
        pageId: String,
        allowDeletingContent: Boolean? = null,
        builder: ContentUpdateBuilder.() -> Unit,
    ): PageMarkdownResponse =
        updateContent(
            pageId,
            ContentUpdateBuilder().apply(builder).build(allowDeletingContent),
        )

    /**
     * Replaces the entire content of a page with new markdown.
     *
     * This is the recommended approach when rewriting most or all of a page's content.
     * For targeted edits, prefer [updateContent].
     *
     * @param pageId The ID of the page to update
     * @param request The replace_content request
     * @return Updated page content as markdown
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun replaceContent(
        pageId: String,
        request: ReplaceContentRequest,
    ): PageMarkdownResponse = patch(pageId, request)

    /**
     * Replaces the entire content of a page with new markdown.
     *
     * Convenience overload that builds the request from a plain string.
     *
     * @param pageId The ID of the page to update
     * @param newContent The new markdown content for the page
     * @param allowDeletingContent Whether child pages/databases may be deleted. Defaults to false.
     * @return Updated page content as markdown
     */
    suspend fun replaceContent(
        pageId: String,
        newContent: String,
        allowDeletingContent: Boolean? = null,
    ): PageMarkdownResponse =
        replaceContent(
            pageId,
            ReplaceContentRequest(
                replaceContent =
                    ReplaceContentBody(
                        newStr = newContent,
                        allowDeletingContent = allowDeletingContent,
                    ),
            ),
        )

    private suspend inline fun <reified T : Any> patch(
        pageId: String,
        request: T,
    ): PageMarkdownResponse =
        httpClient.executeWithRateLimit {
            try {
                val response: HttpResponse =
                    httpClient.patch("${config.baseUrl}/pages/$pageId/markdown") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }

                if (response.status.isSuccess()) {
                    response.body<PageMarkdownResponse>()
                } else {
                    val errorBody = readErrorBody(response)
                    throw NotionException.ApiError(
                        code = response.status.value.toString(),
                        status = response.status.value,
                        details = "HTTP ${response.status.value}: ${response.status.description}. Response: $errorBody",
                    )
                }
            } catch (e: NotionException) {
                throw e
            } catch (e: ClientRequestException) {
                throw clientError(e)
            } catch (e: Exception) {
                throw NotionException.NetworkError(e)
            }
        }

    private suspend fun readErrorBody(response: HttpResponse): String =
        try {
            response.body<String>()
        } catch (e: Exception) {
            "Could not read error response body"
        }

    private suspend fun clientError(e: ClientRequestException): NotionException.ApiError {
        val errorBody =
            try {
                e.response.body<String>()
            } catch (ex: Exception) {
                "Could not read error response body"
            }
        return NotionException.ApiError(
            code =
                e.response.status.value
                    .toString(),
            status = e.response.status.value,
            details = "HTTP ${e.response.status.value}: ${e.response.status.description}. Response: $errorBody",
        )
    }
}

/**
 * DSL builder for assembling a list of [ContentUpdate] search-and-replace operations.
 *
 * Example:
 * ```kotlin
 * client.markdown.updateContent("page-id") {
 *     replace("old heading", "new heading")
 *     replaceAll("TODO", "DONE")
 * }
 * ```
 */
class ContentUpdateBuilder {
    private val updates = mutableListOf<ContentUpdate>()

    /** Replace the first occurrence of [oldStr] with [newStr]. */
    fun replace(
        oldStr: String,
        newStr: String,
    ) {
        updates.add(ContentUpdate(oldStr = oldStr, newStr = newStr))
    }

    /** Replace all occurrences of [oldStr] with [newStr]. */
    fun replaceAll(
        oldStr: String,
        newStr: String,
    ) {
        updates.add(ContentUpdate(oldStr = oldStr, newStr = newStr, replaceAllMatches = true))
    }

    internal fun build(allowDeletingContent: Boolean?): UpdateContentRequest =
        UpdateContentRequest(
            updateContent =
                UpdateContentBody(
                    contentUpdates = updates.toList(),
                    allowDeletingContent = allowDeletingContent,
                ),
        )
}
