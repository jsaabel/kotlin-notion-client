package no.saabelit.kotlinnotionclient.models.comments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.models.base.NotionObject
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.users.User
import no.saabelit.kotlinnotionclient.utils.PaginatedResponse

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
    @SerialName("archived")
    override val archived: Boolean = false,
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
 */
@Serializable
data class CreateCommentRequest(
    @SerialName("parent")
    val parent: Parent,
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("discussion_id")
    val discussionId: String? = null,
    @SerialName("attachments")
    val attachments: List<CommentAttachmentRequest>? = null,
    @SerialName("display_name")
    val displayName: CommentDisplayNameRequest? = null,
)

/**
 * Request model for comment attachments.
 */
@Serializable
data class CommentAttachmentRequest(
    @SerialName("file_upload_id")
    val fileUploadId: String,
    @SerialName("type")
    val type: String = "file_upload",
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
