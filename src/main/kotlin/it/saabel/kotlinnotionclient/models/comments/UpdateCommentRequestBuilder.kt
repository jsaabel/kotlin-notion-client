@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.comments

import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.richtext.RichTextBuilder

/**
 * Builder class for updating a comment with a fluent DSL.
 *
 * Notion's `PATCH /v1/comments/{comment_id}` endpoint is content-only: it accepts a
 * change to the comment's rich text (or markdown), but not its parent, discussion,
 * attachments, or display name.
 *
 * Exactly one of [content] / [richText] or [markdown] must be provided.
 *
 * ## Rich Text Example:
 * ```kotlin
 * val request = updateCommentRequest {
 *     content {
 *         text("Updated comment text.")
 *     }
 * }
 * ```
 *
 * ## Markdown Example:
 * ```kotlin
 * val request = updateCommentRequest {
 *     markdown("**Updated** comment via markdown.")
 * }
 * ```
 */
@CommentDslMarker
class UpdateCommentRequestBuilder {
    private var richTextValue: List<RichText> = emptyList()
    private var markdownValue: String? = null

    /**
     * Sets the new content of the comment using the rich text DSL.
     *
     * @param block DSL block for building rich text content
     */
    fun content(block: RichTextBuilder.() -> Unit) {
        richTextValue =
            it.saabel.kotlinnotionclient.models.richtext
                .richText(block)
    }

    /**
     * Sets the new rich text content of the comment using the rich text DSL.
     * This is an alias for [content] for better semantic clarity.
     *
     * @param block DSL block for building rich text content
     */
    fun richText(block: RichTextBuilder.() -> Unit) {
        content(block)
    }

    /**
     * Sets the new comment content as a Markdown string.
     *
     * Supports inline formatting (bold, italic, strikethrough, inline code, links),
     * inline equations, and mentions.
     *
     * Mutually exclusive with [content] / [richText].
     *
     * @param content The Markdown string
     */
    fun markdown(content: String) {
        markdownValue = content
    }

    /**
     * Builds the UpdateCommentRequest from the configured values.
     *
     * @return The constructed UpdateCommentRequest
     * @throws IllegalStateException if both or neither of rich_text and markdown are set
     */
    internal fun build(): UpdateCommentRequest {
        val hasRichText = richTextValue.isNotEmpty()
        val hasMarkdown = markdownValue != null
        if (hasRichText && hasMarkdown) {
            throw IllegalStateException("Comment content must use either rich_text or markdown, not both")
        }
        if (!hasRichText && !hasMarkdown) {
            throw IllegalStateException("Comment content cannot be empty — provide either content { ... } or markdown(\"...\")")
        }

        return UpdateCommentRequest(
            richText = if (hasRichText) richTextValue else null,
            markdown = markdownValue,
        )
    }
}

/**
 * Entry point function for the update comment request DSL.
 *
 * Creates an UpdateCommentRequest using a fluent builder pattern.
 *
 * @param block The DSL block for building the update comment request
 * @return The constructed UpdateCommentRequest
 */
fun updateCommentRequest(block: UpdateCommentRequestBuilder.() -> Unit): UpdateCommentRequest =
    UpdateCommentRequestBuilder().apply(block).build()
