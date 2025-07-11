package no.saabelit.kotlinnotionclient.models.comments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.models.base.NotionObject
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.users.User

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
    val results: List<Comment>,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    @SerialName("has_more")
    val hasMore: Boolean = false,
)