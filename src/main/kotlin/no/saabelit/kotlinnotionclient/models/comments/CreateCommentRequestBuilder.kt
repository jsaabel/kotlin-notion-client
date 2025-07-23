@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.comments

import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.richtext.RichTextBuilder
import no.saabelit.kotlinnotionclient.models.richtext.richText

/**
 * DSL marker to prevent nested scopes in comment builders.
 */
@DslMarker
annotation class CommentDslMarker

/**
 * Builder class for creating comment requests with a fluent DSL.
 *
 * This builder provides a convenient way to construct CreateCommentRequest objects
 * with significantly less boilerplate than manual construction.
 *
 * ## Basic Comment Example:
 * ```kotlin
 * val request = commentRequest {
 *     parent.pageId("12345678-1234-1234-1234-123456789abc")
 *     content {
 *         text("This is a simple comment.")
 *     }
 * }
 * ```
 *
 * ## Comment with Formatting:
 * ```kotlin
 * val request = commentRequest {
 *     parent.blockId("87654321-4321-4321-4321-210987654321")
 *     content {
 *         text("This comment has ")
 *         bold("bold text")
 *         text(" and ")
 *         italic("italic text")
 *         text("!")
 *     }
 *     discussionId("existing-discussion-id")
 * }
 * ```
 *
 * ## Comment with Custom Display Name:
 * ```kotlin
 * val request = commentRequest {
 *     parent.pageId("12345678-1234-1234-1234-123456789abc")
 *     content {
 *         text("This comment has a custom display name.")
 *     }
 *     displayName("Custom Bot Name")
 * }
 * ```
 *
 * **Important**: Comments can only be parented by pages or blocks.
 * The discussionId is optional and used to reply to existing comment threads.
 */
@CommentDslMarker
class CreateCommentRequestBuilder {
    private var parentValue: Parent? = null
    private var richTextValue: List<RichText> = emptyList()
    private var discussionIdValue: String? = null
    private var attachmentsValue: List<CommentAttachmentRequest>? = null
    private var displayNameValue: CommentDisplayNameRequest? = null

    /**
     * Builder for configuring the parent of the comment.
     * Comments can be attached to pages or blocks.
     */
    val parent = ParentBuilder()

    /**
     * Inner builder class for specifying the parent of the comment.
     * Comments can only be parented by pages or blocks.
     */
    @CommentDslMarker
    inner class ParentBuilder {
        /**
         * Sets the parent to a page.
         *
         * @param pageId The ID of the page to comment on
         */
        fun pageId(pageId: String) {
            this@CreateCommentRequestBuilder.parentValue = Parent(type = "page_id", pageId = pageId)
        }

        /**
         * Sets the parent to a block.
         *
         * @param blockId The ID of the block to comment on
         */
        fun blockId(blockId: String) {
            this@CreateCommentRequestBuilder.parentValue = Parent(type = "block_id", blockId = blockId)
        }
    }

    /**
     * Sets the content of the comment using the rich text DSL.
     *
     * @param block DSL block for building rich text content
     */
    fun content(block: RichTextBuilder.() -> Unit) {
        richTextValue = richText(block)
    }

    /**
     * Sets the discussion ID to reply to an existing comment thread.
     *
     * @param discussionId The ID of the discussion to reply to
     */
    fun discussionId(discussionId: String) {
        discussionIdValue = discussionId
    }

    /**
     * Sets a custom display name for the comment.
     * This is useful for integration bots that want to display a custom name.
     *
     * @param name The custom display name
     */
    fun displayName(name: String) {
        displayNameValue =
            CommentDisplayNameRequest(
                type = CommentDisplayNameType.CUSTOM,
                custom = CommentCustomDisplayName(name = name),
            )
    }

    /**
     * Adds file attachments to the comment.
     * Maximum of 3 attachments per comment.
     *
     * @param attachments List of attachment requests
     */
    fun attachments(attachments: List<CommentAttachmentRequest>) {
        if (attachments.size > 3) {
            throw IllegalArgumentException("Comments can have a maximum of 3 attachments, but ${attachments.size} were provided")
        }
        attachmentsValue = attachments
    }

    /**
     * Adds a single file attachment to the comment.
     *
     * @param fileUploadId The ID of the uploaded file
     */
    fun attachment(fileUploadId: String) {
        val currentAttachments = attachmentsValue?.toMutableList() ?: mutableListOf()
        if (currentAttachments.size >= 3) {
            throw IllegalArgumentException("Comments can have a maximum of 3 attachments")
        }
        currentAttachments.add(CommentAttachmentRequest(fileUploadId = fileUploadId))
        attachmentsValue = currentAttachments
    }

    /**
     * Builds the CreateCommentRequest from the configured values.
     *
     * @return The constructed CreateCommentRequest
     * @throws IllegalStateException if required fields are not set
     */
    internal fun build(): CreateCommentRequest {
        val parent = parentValue ?: throw IllegalStateException("Parent must be specified")

        if (richTextValue.isEmpty()) {
            throw IllegalStateException("Comment content cannot be empty")
        }

        return CreateCommentRequest(
            parent = parent,
            richText = richTextValue,
            discussionId = discussionIdValue,
            attachments = attachmentsValue,
            displayName = displayNameValue,
        )
    }
}

/**
 * Entry point function for the comment request DSL.
 *
 * Creates a CreateCommentRequest using a fluent builder pattern.
 *
 * @param block The DSL block for building the comment request
 * @return The constructed CreateCommentRequest
 */
fun commentRequest(block: CreateCommentRequestBuilder.() -> Unit): CreateCommentRequest = CreateCommentRequestBuilder().apply(block).build()
// TODO: Should this be called createCommentRequest? Check pattern in other DSLs.
