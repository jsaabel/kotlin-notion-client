package no.saabelit.kotlinnotionclient.models.blocks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.models.base.Color
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.files.FileUploadReference

/**
 * Represents a block creation request for the Notion API.
 *
 * These models represent the structure needed for creating blocks via API calls,
 * which differs from the response models that include metadata like IDs and timestamps.
 */
@Serializable
sealed class BlockRequest {
    /**
     * Paragraph block request.
     * Contains rich text content and optional nested children.
     */
    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        @SerialName("paragraph")
        val paragraph: ParagraphRequestContent,
    ) : BlockRequest()

    /**
     * Heading 1 block request.
     * Large heading with optional toggle functionality.
     */
    @Serializable
    @SerialName("heading_1")
    data class Heading1(
        @SerialName("heading_1")
        val heading1: Heading1RequestContent,
    ) : BlockRequest()

    /**
     * Heading 2 block request.
     * Medium heading with optional toggle functionality.
     */
    @Serializable
    @SerialName("heading_2")
    data class Heading2(
        @SerialName("heading_2")
        val heading2: Heading2RequestContent,
    ) : BlockRequest()

    /**
     * Heading 3 block request.
     * Small heading with optional toggle functionality.
     */
    @Serializable
    @SerialName("heading_3")
    data class Heading3(
        @SerialName("heading_3")
        val heading3: Heading3RequestContent,
    ) : BlockRequest()

    /**
     * Bulleted list item block request.
     * List item with bullet point and optional nested children.
     */
    @Serializable
    @SerialName("bulleted_list_item")
    data class BulletedListItem(
        @SerialName("bulleted_list_item")
        val bulletedListItem: BulletedListItemRequestContent,
    ) : BlockRequest()

    /**
     * Numbered list item block request.
     * List item with number and optional nested children.
     */
    @Serializable
    @SerialName("numbered_list_item")
    data class NumberedListItem(
        @SerialName("numbered_list_item")
        val numberedListItem: NumberedListItemRequestContent,
    ) : BlockRequest()

    /**
     * To-do block request.
     * Checkbox item with optional nested children.
     */
    @Serializable
    @SerialName("to_do")
    data class ToDo(
        @SerialName("to_do")
        val toDo: ToDoRequestContent,
    ) : BlockRequest()

    /**
     * Toggle block request.
     * Collapsible content block with nested children.
     */
    @Serializable
    @SerialName("toggle")
    data class Toggle(
        @SerialName("toggle")
        val toggle: ToggleRequestContent,
    ) : BlockRequest()

    /**
     * Code block request.
     * Code snippet with syntax highlighting.
     */
    @Serializable
    @SerialName("code")
    data class Code(
        @SerialName("code")
        val code: CodeRequestContent,
    ) : BlockRequest()

    /**
     * Quote block request.
     * Emphasized quote text.
     */
    @Serializable
    @SerialName("quote")
    data class Quote(
        @SerialName("quote")
        val quote: QuoteRequestContent,
    ) : BlockRequest()

    /**
     * Callout block request.
     * Highlighted content with icon.
     */
    @Serializable
    @SerialName("callout")
    data class Callout(
        @SerialName("callout")
        val callout: CalloutRequestContent,
    ) : BlockRequest()

    /**
     * Image block request.
     * Image with optional caption.
     */
    @Serializable
    @SerialName("image")
    data class Image(
        @SerialName("image")
        val image: ImageRequestContent,
    ) : BlockRequest()

    /**
     * Video block request.
     * Video with optional caption.
     */
    @Serializable
    @SerialName("video")
    data class Video(
        @SerialName("video")
        val video: VideoRequestContent,
    ) : BlockRequest()

    /**
     * Audio block request.
     * Audio file with optional caption.
     */
    @Serializable
    @SerialName("audio")
    data class Audio(
        @SerialName("audio")
        val audio: AudioRequestContent,
    ) : BlockRequest()

    /**
     * File block request.
     * Generic file with optional caption and name.
     */
    @Serializable
    @SerialName("file")
    data class File(
        @SerialName("file")
        val file: FileRequestContent,
    ) : BlockRequest()

    /**
     * PDF block request.
     * PDF file with optional caption.
     */
    @Serializable
    @SerialName("pdf")
    data class PDF(
        @SerialName("pdf")
        val pdf: PDFRequestContent,
    ) : BlockRequest()

    /**
     * Divider block request.
     * Horizontal line separator.
     */
    @Serializable
    @SerialName("divider")
    data class Divider(
        @SerialName("divider")
        val divider: DividerRequestContent = DividerRequestContent(),
    ) : BlockRequest()

    /**
     * Table block request.
     * Table container with configurable width and headers.
     */
    @Serializable
    @SerialName("table")
    data class Table(
        @SerialName("table")
        val table: TableRequestContent,
    ) : BlockRequest()

    /**
     * Table row block request.
     * Row of cells within a table.
     */
    @Serializable
    @SerialName("table_row")
    data class TableRow(
        @SerialName("table_row")
        val tableRow: TableRowRequestContent,
    ) : BlockRequest()

    /**
     * Bookmark block request.
     * Bookmarks web links with metadata.
     */
    @Serializable
    @SerialName("bookmark")
    data class Bookmark(
        @SerialName("bookmark")
        val bookmark: BookmarkRequestContent,
    ) : BlockRequest()

    /**
     * Embed block request.
     * Embeds external content.
     */
    @Serializable
    @SerialName("embed")
    data class Embed(
        @SerialName("embed")
        val embed: EmbedRequestContent,
    ) : BlockRequest()

    /**
     * Child page block request.
     * References to child pages.
     */
    @Serializable
    @SerialName("child_page")
    data class ChildPage(
        @SerialName("child_page")
        val childPage: ChildPageRequestContent,
    ) : BlockRequest()

    /**
     * Child database block request.
     * References to child databases.
     */
    @Serializable
    @SerialName("child_database")
    data class ChildDatabase(
        @SerialName("child_database")
        val childDatabase: ChildDatabaseRequestContent,
    ) : BlockRequest()

    /**
     * Column list block request.
     * Container for column layout.
     */
    @Serializable
    @SerialName("column_list")
    data class ColumnList(
        @SerialName("column_list")
        val columnList: ColumnListRequestContent,
    ) : BlockRequest()

    /**
     * Column block request.
     * Individual column within a column list.
     */
    @Serializable
    @SerialName("column")
    data class Column(
        @SerialName("column")
        val column: ColumnRequestContent,
    ) : BlockRequest()

    /**
     * Breadcrumb block request.
     * Navigation breadcrumb display.
     */
    @Serializable
    @SerialName("breadcrumb")
    data class Breadcrumb(
        @SerialName("breadcrumb")
        val breadcrumb: BreadcrumbRequestContent = BreadcrumbRequestContent(),
    ) : BlockRequest()

    /**
     * Table of contents block request.
     * Auto-generated table of contents.
     */
    @Serializable
    @SerialName("table_of_contents")
    data class TableOfContents(
        @SerialName("table_of_contents")
        val tableOfContents: TableOfContentsRequestContent,
    ) : BlockRequest()

    /**
     * Equation block request.
     * Mathematical equations in LaTeX format.
     */
    @Serializable
    @SerialName("equation")
    data class Equation(
        @SerialName("equation")
        val equation: EquationRequestContent,
    ) : BlockRequest()

    /**
     * Synced block request.
     * Synchronized content blocks.
     */
    @Serializable
    @SerialName("synced_block")
    data class SyncedBlock(
        @SerialName("synced_block")
        val syncedBlock: SyncedBlockRequestContent,
    ) : BlockRequest()

    /**
     * Template block request.
     * Template button for repeated content.
     */
    @Serializable
    @SerialName("template")
    data class Template(
        @SerialName("template")
        val template: TemplateRequestContent,
    ) : BlockRequest()
}

// REQUEST CONTENT CLASSES

/**
 * Content for paragraph block requests.
 */
@Serializable
data class ParagraphRequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for heading_1 block requests.
 */
@Serializable
data class Heading1RequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("is_toggleable")
    val isToggleable: Boolean = false,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for heading_2 block requests.
 */
@Serializable
data class Heading2RequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("is_toggleable")
    val isToggleable: Boolean = false,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for heading_3 block requests.
 */
@Serializable
data class Heading3RequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("is_toggleable")
    val isToggleable: Boolean = false,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for bulleted_list_item block requests.
 */
@Serializable
data class BulletedListItemRequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for numbered_list_item block requests.
 */
@Serializable
data class NumberedListItemRequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for to_do block requests.
 */
@Serializable
data class ToDoRequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("checked")
    val checked: Boolean = false,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for toggle block requests.
 */
@Serializable
data class ToggleRequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for code block requests.
 */
@Serializable
data class CodeRequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("language")
    val language: String = "plain text",
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
)

/**
 * Content for quote block requests.
 */
@Serializable
data class QuoteRequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for callout block requests.
 */
@Serializable
data class CalloutRequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("icon")
    val icon: CalloutIcon? = null,
    @SerialName("color")
    val color: Color = Color.DEFAULT,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for image block requests.
 */
@Serializable
data class ImageRequestContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("type")
    val type: String, // "external", "file", or "file_upload"
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
    @SerialName("file_upload")
    val fileUpload: FileUploadReference? = null,
)

/**
 * Content for video block requests.
 */
@Serializable
data class VideoRequestContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("type")
    val type: String, // "external", "file", or "file_upload"
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
    @SerialName("file_upload")
    val fileUpload: FileUploadReference? = null,
)

/**
 * Content for audio block requests.
 */
@Serializable
data class AudioRequestContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("type")
    val type: String, // "external", "file", or "file_upload"
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
    @SerialName("file_upload")
    val fileUpload: FileUploadReference? = null,
)

/**
 * Content for file block requests.
 */
@Serializable
data class FileRequestContent(
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
    val fileUpload: FileUploadReference? = null,
)

/**
 * Content for PDF block requests.
 */
@Serializable
data class PDFRequestContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("type")
    val type: String, // "external", "file", or "file_upload"
    @SerialName("external")
    val external: ExternalFile? = null,
    @SerialName("file")
    val file: FileReference? = null,
    @SerialName("file_upload")
    val fileUpload: FileUploadReference? = null,
)

/**
 * Content for divider block requests.
 */
@Serializable
class DividerRequestContent

/**
 * Content for table block requests.
 */
@Serializable
data class TableRequestContent(
    @SerialName("table_width")
    val tableWidth: Int,
    @SerialName("has_column_header")
    val hasColumnHeader: Boolean = false,
    @SerialName("has_row_header")
    val hasRowHeader: Boolean = false,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for table_row block requests.
 */
@Serializable
data class TableRowRequestContent(
    @SerialName("cells")
    val cells: List<List<RichText>>,
)

/**
 * Content for bookmark block requests.
 */
@Serializable
data class BookmarkRequestContent(
    @SerialName("caption")
    val caption: List<RichText> = emptyList(),
    @SerialName("url")
    val url: String,
)

/**
 * Content for embed block requests.
 */
@Serializable
data class EmbedRequestContent(
    @SerialName("url")
    val url: String,
)

/**
 * Content for child_page block requests.
 */
@Serializable
data class ChildPageRequestContent(
    @SerialName("title")
    val title: String,
)

/**
 * Content for child_database block requests.
 */
@Serializable
data class ChildDatabaseRequestContent(
    @SerialName("title")
    val title: String,
)

/**
 * Content for column_list block requests.
 */
@Serializable
data class ColumnListRequestContent(
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for column block requests.
 */
@Serializable
data class ColumnRequestContent(
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for breadcrumb block requests.
 */
@Serializable
class BreadcrumbRequestContent

/**
 * Content for table_of_contents block requests.
 */
@Serializable
data class TableOfContentsRequestContent(
    @SerialName("color")
    val color: Color = Color.DEFAULT,
)

/**
 * Content for equation block requests.
 */
@Serializable
data class EquationRequestContent(
    @SerialName("expression")
    val expression: String,
)

/**
 * Content for synced_block block requests.
 */
@Serializable
data class SyncedBlockRequestContent(
    @SerialName("synced_from")
    val syncedFrom: SyncedBlockReference? = null,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for template block requests.
 */
@Serializable
data class TemplateRequestContent(
    @SerialName("rich_text")
    val richText: List<RichText>,
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

// Note: CalloutIcon and SyncedBlockReference are defined in Block.kt to avoid duplication
