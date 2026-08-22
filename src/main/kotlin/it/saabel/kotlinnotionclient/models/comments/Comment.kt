package it.saabel.kotlinnotionclient.models.comments

import it.saabel.kotlinnotionclient.models.base.NotionObject
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.files.PendingUploadRefusingSerializer
import it.saabel.kotlinnotionclient.models.users.User
import it.saabel.kotlinnotionclient.utils.FileSource
import it.saabel.kotlinnotionclient.utils.PaginatedResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a comment in Notion.
 *
 * Comments are discussions attached to pages and blocks in Notion.
 */
@Serializable
data class Comment(
    @SerialName("id")
    override val id: String,
    @SerialName("created_time")
    override val createdTime: String,
    @SerialName("last_edited_time")
    override val lastEditedTime: String,
    @SerialName("created_by")
    override val createdBy: User? = null,
    @SerialName("last_edited_by")
    override val lastEditedBy: User? = null,
    @SerialName("in_trash")
    override val inTrash: Boolean = false,
    @SerialName("parent")
    val parent: Parent,
    @SerialName("discussion_id")
    val discussionId: String,
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("attachments")
    val attachments: List<CommentAttachment>? = null,
    @SerialName("display_name")
    val displayName: CommentDisplayName? = null,
) : NotionObject {
    @SerialName("object")
    override val objectType: String = "comment"
}

/**
 * Represents a list of comments (used for comment retrieval responses).
 */
@Serializable
data class CommentList(
    @SerialName("object")
    val objectType: String = "list",
    @SerialName("results")
    override val results: List<Comment>,
    @SerialName("next_cursor")
    override val nextCursor: String? = null,
    @SerialName("has_more")
    override val hasMore: Boolean = false,
) : PaginatedResponse<Comment>

/**
 * Represents an attachment in a comment.
 */
@Serializable
data class CommentAttachment(
    @SerialName("category")
    val category: CommentAttachmentCategory,
    @SerialName("file")
    val file: CommentAttachmentFile,
)

/**
 * Categories of comment attachments.
 */
@Serializable
enum class CommentAttachmentCategory {
    @SerialName("audio")
    AUDIO,

    @SerialName("image")
    IMAGE,

    @SerialName("pdf")
    PDF,

    @SerialName("productivity")
    PRODUCTIVITY,

    @SerialName("video")
    VIDEO,
}

/**
 * Represents a file attachment in a comment.
 */
@Serializable
data class CommentAttachmentFile(
    @SerialName("url")
    val url: String,
    @SerialName("expiry_time")
    val expiryTime: String,
)

/**
 * Represents the display name configuration for a comment.
 * This is used when the comment author wants to display a custom name.
 */
@Serializable
data class CommentDisplayName(
    @SerialName("type")
    val type: CommentDisplayNameType,
    @SerialName("resolved_name")
    val resolvedName: String,
)

/**
 * Types of comment display names.
 */
@Serializable
enum class CommentDisplayNameType {
    @SerialName("integration")
    INTEGRATION,

    @SerialName("user")
    USER,

    @SerialName("custom")
    CUSTOM,
}

// Request models for creating comments

/**
 * Request model for creating a comment.
 *
 * Exactly one of [richText] or [markdown] must be provided.
 */
@Serializable
data class CreateCommentRequest(
    @SerialName("parent")
    val parent: Parent,
    @SerialName("rich_text")
    val richText: List<RichText>? = null,
    @SerialName("markdown")
    val markdown: String? = null,
    @SerialName("discussion_id")
    val discussionId: String? = null,
    @SerialName("attachments")
    val attachments: List<CommentAttachmentRequest>? = null,
    @SerialName("display_name")
    val displayName: CommentDisplayNameRequest? = null,
)

/**
 * Request model for updating a comment.
 *
 * Notion's `PATCH /v1/comments/{comment_id}` endpoint accepts only content changes —
 * no parent, discussion, attachments, or display name. Exactly one of [richText] or
 * [markdown] must be provided. Null fields are omitted on the wire.
 */
@Serializable
data class UpdateCommentRequest(
    @SerialName("rich_text")
    val richText: List<RichText>? = null,
    @SerialName("markdown")
    val markdown: String? = null,
)

/**
 * Request model for comment attachments.
 *
 * A comment attachment is either an already-uploaded file referenced by id
 * ([CommentAttachmentRequest.FileUpload]) or a local file the client still has to upload
 * ([CommentAttachmentRequest.PendingUpload], recorded by `attachment(File(…))` in the comment
 * DSL). `CommentsApi.create` resolves every pending one before the request is serialized.
 *
 * `CommentAttachmentRequest("upload-id")` keeps working and builds the [FileUpload] variant.
 */
@Serializable
sealed class CommentAttachmentRequest {
    /**
     * The file-upload id this attachment references, or `null` while it is still
     * [PendingUpload].
     */
    abstract val fileUploadId: String?

    /** The `type` discriminator Notion sees for this attachment. */
    abstract val type: String

    /**
     * References a file already created through the File Upload API.
     *
     * @property fileUploadId The ID of the uploaded file
     */
    @Serializable
    @SerialName("file_upload")
    data class FileUpload(
        @SerialName("file_upload_id")
        override val fileUploadId: String,
    ) : CommentAttachmentRequest() {
        override val type: String get() = "file_upload"
    }

    /**
     * A local file recorded by `attachment(File(…))`, still waiting to be uploaded.
     *
     * `CommentsApi.create` uploads it and swaps in the equivalent [FileUpload] before the
     * request is serialized — see `docs/adr/0001-deferred-file-upload-resolution.md`.
     * Serializing one yourself throws; see [CommentAttachmentPendingUploadSerializer].
     *
     * @property source The bytes to upload
     * @property options Upload options — content type override, progress callback, validation
     */
    @Serializable(with = CommentAttachmentPendingUploadSerializer::class)
    @SerialName("pending_upload")
    data class PendingUpload(
        val source: FileSource,
        val options: FileUploadOptions = FileUploadOptions(),
    ) : CommentAttachmentRequest() {
        override val fileUploadId: String? get() = null
        override val type: String get() = "pending_upload"
    }

    companion object {
        /**
         * Builds a [FileUpload] attachment referencing an uploaded file by its id.
         *
         * @param fileUploadId The ID of the uploaded file
         * @param type Kept for source compatibility; only `"file_upload"` is accepted
         */
        operator fun invoke(
            fileUploadId: String,
            type: String = "file_upload",
        ): FileUpload {
            require(type == "file_upload") {
                "Comment attachments are always type \"file_upload\", but \"$type\" was provided"
            }
            return FileUpload(fileUploadId = fileUploadId)
        }
    }
}

/**
 * Serializer for [CommentAttachmentRequest.PendingUpload] that refuses to serialize. See
 * [PendingUploadRefusingSerializer].
 */
internal object CommentAttachmentPendingUploadSerializer :
    PendingUploadRefusingSerializer<CommentAttachmentRequest.PendingUpload>(
        serialName = "pending_upload",
        filename = { it.source.filename },
        remedy =
            "pass the request through comments.create, or upload first and use " +
                "attachment(id)",
    )

/**
 * Request model for comment display name.
 */
@Serializable
data class CommentDisplayNameRequest(
    @SerialName("type")
    val type: CommentDisplayNameType,
    @SerialName("custom")
    val custom: CommentCustomDisplayName? = null,
)

/**
 * Custom display name for comments.
 */
@Serializable
data class CommentCustomDisplayName(
    @SerialName("name")
    val name: String,
)
