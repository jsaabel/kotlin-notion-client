package no.saabelit.kotlinnotionclient.models.blocks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import no.saabelit.kotlinnotionclient.models.base.NotionObject
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.users.User

/**
 * Represents a block in Notion.
 *
 * Blocks are the building blocks of content in Notion pages.
 * They can contain text, media, and other structured content.
 */
@Serializable
sealed class Block : NotionObject {
    abstract val parent: Parent
    abstract val hasChildren: Boolean
    abstract val type: String

    @Serializable
    @SerialName("heading_2")
    data class Heading2(
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
        override val archived: Boolean,
        @SerialName("parent")
        override val parent: Parent,
        @SerialName("has_children")
        override val hasChildren: Boolean,
        @SerialName("heading_2")
        val heading2: Heading2Content,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "heading_2"
    }

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
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
        override val archived: Boolean,
        @SerialName("parent")
        override val parent: Parent,
        @SerialName("has_children")
        override val hasChildren: Boolean,
        @SerialName("paragraph")
        val paragraph: ParagraphContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "paragraph"
    }
}

/**
 * Represents the content of a heading_2 block.
 */
@Serializable
data class Heading2Content(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: String = "default",
    @SerialName("is_toggleable")
    val isToggleable: Boolean = false,
)

/**
 * Represents the content of a paragraph block.
 */
@Serializable
data class ParagraphContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: String = "default",
)

/**
 * Represents a list of blocks (used for block children responses).
 */
@Serializable
data class BlockList(
    @SerialName("object")
    val objectType: String = "list",
    @SerialName("results")
    val results: List<Block>,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    @SerialName("has_more")
    val hasMore: Boolean = false,
    @SerialName("type")
    val type: String = "block",
    @SerialName("block")
    val block: JsonObject,
)
