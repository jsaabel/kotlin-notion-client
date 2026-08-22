@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.comments

import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.files.FileUpload
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.richtext.RichTextBuilder
import it.saabel.kotlinnotionclient.utils.FileSource
import it.saabel.kotlinnotionclient.utils.asFileSource
import java.io.File
import java.nio.file.Path

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
 *     parent.page("12345678-1234-1234-1234-123456789abc")
 *     content {
 *         text("This is a simple comment.")
 *     }
 * }
 * ```
 *
 * ## Comment with Formatting:
 * ```kotlin
 * val request = commentRequest {
 *     parent.block("87654321-4321-4321-4321-210987654321")
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
 *     parent.page("12345678-1234-1234-1234-123456789abc")
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
    private var markdownValue: String? = null
    private var discussionIdValue: String? = null
    private var attachmentsValue: List<CommentAttachmentRequest>? = null
    private var displayNameValue: CommentDisplayNameRequest? = null

    /**
     * Builder for configuring the parent of the comment.
     * Comments can be attached to pages or blocks.
     */
    val parent = ParentBuilder()

    /**
     * Configures the parent in a lambda, as an alternative to the `parent.xxx()` receiver form.
     *
     * Both forms drive the same builder and are last-call-wins; see
     * [docs/dsl-conventions.md](https://github.com/jsaabel/kotlin-notion-client/blob/main/docs/dsl-conventions.md).
     *
     * @param block Configuration block applied to the parent builder
     */
    fun parent(block: ParentBuilder.() -> Unit) {
        parent.block()
    }

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
        fun page(pageId: String) {
            this@CreateCommentRequestBuilder.parentValue = Parent.PageParent(pageId = pageId)
        }

        /**
         * Sets the parent to a block.
         *
         * @param blockId The ID of the block to comment on
         */
        fun block(blockId: String) {
            this@CreateCommentRequestBuilder.parentValue = Parent.BlockParent(blockId = blockId)
        }

        /**
         * Sets the parent to a page.
         *
         * @param pageId The ID of the page to comment on
         */
        @Deprecated(
            message = "Parent accessors are named after the object, not its id, in every DSL; use page(id).",
            replaceWith = ReplaceWith("page(pageId)"),
        )
        fun pageId(pageId: String) {
            page(pageId)
        }

        /**
         * Sets the parent to a block.
         *
         * @param blockId The ID of the block to comment on
         */
        @Deprecated(
            message = "Parent accessors are named after the object, not its id, in every DSL; use block(id).",
            replaceWith = ReplaceWith("block(blockId)"),
        )
        fun blockId(blockId: String) {
            block(blockId)
        }
    }

    /**
     * Sets the content of the comment using the rich text DSL.
     *
     * @param block DSL block for building rich text content
     */
    fun content(block: RichTextBuilder.() -> Unit) {
        richTextValue =
            it.saabel.kotlinnotionclient.models.richtext
                .richText(block)
    }

    /**
     * Sets the rich text content of the comment using the rich text DSL.
     * This is an alias for content() for better semantic clarity.
     *
     * @param block DSL block for building rich text content
     */
    fun richText(block: RichTextBuilder.() -> Unit) {
        content(block)
    }

    /**
     * Sets the comment content as a Markdown string.
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
        addAttachment(CommentAttachmentRequest(fileUploadId = fileUploadId))
    }

    /**
     * Adds a single file attachment to the comment.
     *
     * @param fileUpload The upload returned by the File Upload API
     */
    fun attachment(fileUpload: FileUpload) {
        attachment(fileUpload.id)
    }

    /**
     * Attaches a local file, uploading it when the comment is created.
     *
     * This is the recommended way to attach a local file: nothing is uploaded while the builder
     * runs — the file is recorded as a [CommentAttachmentRequest.PendingUpload] sentinel and
     * `comments.create` resolves it before the request goes out, keeping the attachment inside
     * the one call. See `docs/adr/0001-deferred-file-upload-resolution.md`.
     *
     * ```kotlin
     * notion.comments.create {
     *     parent.page(pageId)
     *     content { text("Trace attached") }
     *     attachment(File("trace.txt"))
     * }
     * ```
     *
     * @param source the file to upload
     * @param options upload options — content type override, progress callback, validation
     * @throws IllegalArgumentException if the comment would carry more than 3 attachments
     */
    fun attachment(
        source: FileSource,
        options: FileUploadOptions = FileUploadOptions(),
    ) {
        addAttachment(CommentAttachmentRequest.PendingUpload(source = source, options = options))
    }

    /** Attaches a local file, uploading it when the comment is created. See [attachment]. */
    fun attachment(
        file: File,
        options: FileUploadOptions = FileUploadOptions(),
    ) {
        attachment(file.asFileSource(), options)
    }

    /** Attaches a local file, uploading it when the comment is created. See [attachment]. */
    fun attachment(
        path: Path,
        options: FileUploadOptions = FileUploadOptions(),
    ) {
        attachment(path.asFileSource(), options)
    }

    /**
     * Appends [attachment] to the accumulated list, enforcing Notion's per-comment cap.
     *
     * Pending uploads are counted here as the attachments they will become, so the cap is
     * reported at the call site that overshoots it rather than after three files have been
     * uploaded for nothing.
     */
    private fun addAttachment(attachment: CommentAttachmentRequest) {
        val currentAttachments = attachmentsValue?.toMutableList() ?: mutableListOf()
        if (currentAttachments.size >= 3) {
            throw IllegalArgumentException("Comments can have a maximum of 3 attachments")
        }
        currentAttachments.add(attachment)
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

        val hasRichText = richTextValue.isNotEmpty()
        val hasMarkdown = markdownValue != null
        if (hasRichText && hasMarkdown) {
            throw IllegalStateException("Comment content must use either rich_text or markdown, not both")
        }
        if (!hasRichText && !hasMarkdown) {
            throw IllegalStateException("Comment content cannot be empty — provide either content { ... } or markdown(\"...\")")
        }

        return CreateCommentRequest(
            parent = parent,
            richText = if (hasRichText) richTextValue else null,
            markdown = markdownValue,
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
fun createCommentRequest(block: CreateCommentRequestBuilder.() -> Unit): CreateCommentRequest =
    CreateCommentRequestBuilder().apply(block).build()
