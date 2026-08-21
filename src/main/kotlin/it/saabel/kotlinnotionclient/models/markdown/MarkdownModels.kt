@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.markdown

import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for the Page Markdown API.
 *
 * Returned by both GET (retrieve) and PATCH (update) markdown endpoints.
 *
 * @property truncated `true` when the page was large enough that some subtrees could not be loaded
 * @property unknownBlockIds Up to 50 root IDs of the omitted subtrees
 * @property unknownBlockCount Total number of omitted subtree roots, which may exceed the number of
 *   IDs in [unknownBlockIds]. Defaults to `0` when the API omits the field.
 */
@Serializable
data class PageMarkdownResponse(
    @SerialName("object")
    val objectType: String = "page_markdown",
    @SerialName("id")
    val id: String,
    @SerialName("markdown")
    val markdown: String,
    @SerialName("truncated")
    val truncated: Boolean,
    @SerialName("unknown_block_ids")
    val unknownBlockIds: List<String> = emptyList(),
    @SerialName("unknown_block_count")
    val unknownBlockCount: Int = 0,
)

/**
 * A single search-and-replace operation for the update_content command.
 */
@Serializable
data class ContentUpdate(
    @SerialName("old_str")
    val oldStr: String,
    @SerialName("new_str")
    val newStr: String,
    @SerialName("replace_all_matches")
    val replaceAllMatches: Boolean? = null,
)

/**
 * Body of the update_content command — performs targeted search-and-replace operations.
 *
 * Supports up to 100 [contentUpdates] per request.
 */
@Serializable
data class UpdateContentBody(
    @SerialName("content_updates")
    val contentUpdates: List<ContentUpdate>,
    @SerialName("allow_deleting_content")
    val allowDeletingContent: Boolean? = null,
)

/**
 * Body of the replace_content command — replaces the entire page content.
 */
@Serializable
data class ReplaceContentBody(
    @SerialName("new_str")
    val newStr: String,
    @SerialName("allow_deleting_content")
    val allowDeletingContent: Boolean? = null,
)

/**
 * Request body for the update_content PATCH command.
 *
 * Performs targeted search-and-replace operations on the page's markdown content.
 * Prefer this over [ReplaceContentRequest] for partial edits.
 *
 * @property allowAsync Opt into asynchronous execution. When `true`, the API may respond
 *   with HTTP 202 and an async task handle instead of the updated markdown. Use the
 *   `MarkdownApi.updateContentAsync` methods rather than setting this directly.
 */
@Serializable
data class UpdateContentRequest(
    @SerialName("type")
    val type: String = "update_content",
    @SerialName("update_content")
    val updateContent: UpdateContentBody,
    @SerialName("allow_async")
    val allowAsync: Boolean? = null,
)

/**
 * Request body for the replace_content PATCH command.
 *
 * Replaces the entire page content with new markdown.
 * Use [UpdateContentRequest] for more targeted edits.
 *
 * @property allowAsync Opt into asynchronous execution. When `true`, the API may respond
 *   with HTTP 202 and an async task handle instead of the updated markdown. Use the
 *   `MarkdownApi.replaceContentAsync` methods rather than setting this directly.
 */
@Serializable
data class ReplaceContentRequest(
    @SerialName("type")
    val type: String = "replace_content",
    @SerialName("replace_content")
    val replaceContent: ReplaceContentBody,
    @SerialName("allow_async")
    val allowAsync: Boolean? = null,
)

/**
 * Result of an opt-in async markdown page write.
 *
 * Even with `allow_async: true`, the API only moves work to the background when it
 * decides to (HTTP 202); small writes may still complete synchronously (HTTP 200).
 * This sealed type captures both outcomes.
 */
sealed class AsyncMarkdownResult {
    /**
     * The write completed synchronously; [response] is the updated page markdown.
     */
    data class Completed(
        val response: PageMarkdownResponse,
    ) : AsyncMarkdownResult()

    /**
     * The write was accepted for background execution. Poll [task] via
     * `client.asyncTasks` (e.g. `waitForCompletion(task.id)`) to obtain the result.
     */
    data class Accepted(
        val task: AsyncTask,
    ) : AsyncMarkdownResult()
}
