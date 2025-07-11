package no.saabelit.kotlinnotionclient.models.blocks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.models.base.RichText

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
     * Divider block request.
     * Horizontal line separator.
     */
    @Serializable
    @SerialName("divider")
    data class Divider(
        @SerialName("divider")
        val divider: DividerRequestContent = DividerRequestContent(),
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
    val color: String = "default",
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
    val color: String = "default",
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
    val color: String = "default",
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
    val color: String = "default",
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
    val color: String = "default",
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
    val color: String = "default",
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
    val color: String = "default",
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
    val color: String = "default",
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
    val color: String = "default",
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
    val color: String = "default",
    @SerialName("children")
    val children: List<BlockRequest>? = null,
)

/**
 * Content for divider block requests.
 */
@Serializable
class DividerRequestContent

// Note: CalloutIcon is defined in Block.kt to avoid duplication
