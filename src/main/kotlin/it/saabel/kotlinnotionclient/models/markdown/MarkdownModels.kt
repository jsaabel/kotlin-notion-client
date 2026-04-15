@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.markdown

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for the Page Markdown API.
 *
 * Returned by both GET (retrieve) and PATCH (update) markdown endpoints.
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
 */
@Serializable
data class UpdateContentRequest(
    @SerialName("type")
    val type: String = "update_content",
    @SerialName("update_content")
    val updateContent: UpdateContentBody,
)

/**
 * Request body for the replace_content PATCH command.
 *
 * Replaces the entire page content with new markdown.
 * Use [UpdateContentRequest] for more targeted edits.
 */
@Serializable
data class ReplaceContentRequest(
    @SerialName("type")
    val type: String = "replace_content",
    @SerialName("replace_content")
    val replaceContent: ReplaceContentBody,
)
