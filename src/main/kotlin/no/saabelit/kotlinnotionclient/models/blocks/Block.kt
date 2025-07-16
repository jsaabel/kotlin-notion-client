@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.blocks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import no.saabelit.kotlinnotionclient.models.base.Color
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

    // HEADINGS (ordered hierarchically)

    @Serializable
    @SerialName("heading_1")
    data class Heading1(
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
        @SerialName("heading_1")
        val heading1: Heading1Content,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "heading_1"
    }

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
    @SerialName("heading_3")
    data class Heading3(
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
        @SerialName("heading_3")
        val heading3: Heading3Content,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "heading_3"
    }

    // BASIC TEXT BLOCKS

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

    @Serializable
    @SerialName("quote")
    data class Quote(
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
        @SerialName("quote")
        val quote: QuoteContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "quote"
    }

    // LIST BLOCKS

    @Serializable
    @SerialName("bulleted_list_item")
    data class BulletedListItem(
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
        @SerialName("bulleted_list_item")
        val bulletedListItem: BulletedListItemContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "bulleted_list_item"
    }

    @Serializable
    @SerialName("numbered_list_item")
    data class NumberedListItem(
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
        @SerialName("numbered_list_item")
        val numberedListItem: NumberedListItemContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "numbered_list_item"
    }

    @Serializable
    @SerialName("to_do")
    data class ToDo(
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
        @SerialName("to_do")
        val toDo: ToDoContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "to_do"
    }

    // SPECIAL CONTENT BLOCKS

    @Serializable
    @SerialName("callout")
    data class Callout(
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
        @SerialName("callout")
        val callout: CalloutContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "callout"
    }

    @Serializable
    @SerialName("code")
    data class Code(
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
        @SerialName("code")
        val code: CodeContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "code"
    }

    @Serializable
    @SerialName("toggle")
    data class Toggle(
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
        @SerialName("toggle")
        val toggle: ToggleContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "toggle"
    }

    // MEDIA BLOCKS

    @Serializable
    @SerialName("image")
    data class Image(
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
        @SerialName("image")
        val image: ImageContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "image"
    }

    @Serializable
    @SerialName("video")
    data class Video(
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
        @SerialName("video")
        val video: VideoContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "video"
    }

    @Serializable
    @SerialName("audio")
    data class Audio(
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
        @SerialName("audio")
        val audio: AudioContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "audio"
    }

    @Serializable
    @SerialName("file")
    data class File(
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
        @SerialName("file")
        val file: FileContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "file"
    }

    @Serializable
    @SerialName("pdf")
    data class PDF(
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
        @SerialName("pdf")
        val pdf: PDFContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "pdf"
    }

    // LAYOUT/FORMATTING BLOCKS

    @Serializable
    @SerialName("divider")
    data class Divider(
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
        @SerialName("divider")
        val divider: DividerContent,
    ) : Block() {
        @SerialName("object")
        override val objectType: String = "block"

        @SerialName("type")
        override val type: String = "divider"
    }
}

// CONTENT CLASSES (ordered to match block order above)

/**
 * Represents the content of a heading_1 block.
 */
@Serializable
data class Heading1Content(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("is_toggleable")
    val isToggleable: Boolean = false,
)

/**
 * Represents the content of a heading_2 block.
 */
@Serializable
data class Heading2Content(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("is_toggleable")
    val isToggleable: Boolean = false,
)

/**
 * Represents the content of a heading_3 block.
 */
@Serializable
data class Heading3Content(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
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
    val color: Color = Color.DEFAULT,
)

/**
 * Represents the content of a quote block.
 */
@Serializable
data class QuoteContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
)

/**
 * Represents the content of a bulleted_list_item block.
 */
@Serializable
data class BulletedListItemContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
)

/**
 * Represents the content of a numbered_list_item block.
 */
@Serializable
data class NumberedListItemContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
)

/**
 * Represents the content of a to_do block.
 */
@Serializable
data class ToDoContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("checked")
    val checked: Boolean = false,
)

/**
 * Represents the content of a callout block.
 */
@Serializable
data class CalloutContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("icon")
    val icon: CalloutIcon? = null,
)

/**
 * Represents the content of a code block.
 */
@Serializable
data class CodeContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("language")
    val language: String = "plain text",
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
)

/**
 * Represents the content of a toggle block.
 */
@Serializable
data class ToggleContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
)

/**
 * Represents the content of an image block.
 */
@Serializable
data class ImageContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("type")
    val type: String, // "external", "file", or "file_upload"
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
    @SerialName("file_upload")
    val fileUpload: FileUpload? = null,
)

/**
 * Represents the content of a video block.
 */
@Serializable
data class VideoContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("type")
    val type: String, // "external", "file", or "file_upload"
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
    @SerialName("file_upload")
    val fileUpload: FileUpload? = null,
)

/**
 * Represents the content of an audio block.
 */
@Serializable
data class AudioContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("type")
    val type: String, // "external", "file", or "file_upload"
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
    @SerialName("file_upload")
    val fileUpload: FileUpload? = null,
)

/**
 * Represents the content of a file block.
 */
@Serializable
data class FileContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("name")
    val name: String? = null,
    @SerialName("type")
    val type: String, // "external", "file", or "file_upload"
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
    @SerialName("file_upload")
    val fileUpload: FileUpload? = null,
)

/**
 * Represents the content of a PDF block.
 */
@Serializable
data class PDFContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("type")
    val type: String, // "external", "file", or "file_upload"
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
    @SerialName("file_upload")
    val fileUpload: FileUpload? = null,
)

/**
 * Represents the content of a divider block.
 */
@Serializable
class DividerContent

// SUPPORT CLASSES (alphabetically ordered)

/**
 * Represents an icon in a callout block.
 */
@Serializable
data class CalloutIcon(
    @SerialName("type")
    val type: String, // "emoji" or "external" or "file"
    @SerialName("emoji")
    val emoji: String? = null,
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
)

/**
 * Represents an external file reference.
 */
@Serializable
data class ExternalFile(
    @SerialName("url")
    val url: String,
)

/**
 * Represents a file reference in Notion.
 */
@Serializable
data class FileReference(
    @SerialName("url")
    val url: String,
    @SerialName("expiry_time")
    val expiryTime: String,
)

/**
 * Represents a file upload reference for API-uploaded files.
 */
@Serializable
data class FileUpload(
    @SerialName("id")
    val id: String,
)

// API RESPONSE CLASSES

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
