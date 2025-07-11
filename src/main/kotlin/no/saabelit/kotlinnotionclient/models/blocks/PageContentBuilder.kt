package no.saabelit.kotlinnotionclient.models.blocks

import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * DSL builder for creating page content with blocks.
 *
 * This builder provides a fluent API for constructing lists of blocks
 * that can be used for page creation or block appending operations.
 *
 * Example usage:
 * ```kotlin
 * val content = pageContent {
 *     heading1("Welcome")
 *     paragraph("This is a paragraph")
 *     bulletList {
 *         item("First item")
 *         item("Second item")
 *     }
 * }
 * ```
 */
class PageContentBuilder {
    private val blocks = mutableListOf<BlockRequest>()

    /**
     * Adds a block to the content.
     *
     * @param block The block to add
     * @return This builder for chaining
     */
    fun addBlock(block: BlockRequest): PageContentBuilder {
        blocks.add(block)
        return this
    }

    /**
     * Builds the immutable list of blocks.
     *
     * @return List of block requests
     */
    internal fun build(): List<BlockRequest> = blocks.toList()

    /**
     * Validates the current block structure.
     *
     * @return List of validation errors, empty if valid
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        blocks.forEach { block ->
            when (block) {
                is BlockRequest.Paragraph -> {
                    if (block.paragraph.richText.isEmpty()) {
                        errors.add("Paragraph blocks must have content")
                    }
                }
                is BlockRequest.Heading1 -> {
                    if (block.heading1.richText.isEmpty()) {
                        errors.add("Heading blocks must have content")
                    }
                }
                is BlockRequest.Heading2 -> {
                    if (block.heading2.richText.isEmpty()) {
                        errors.add("Heading blocks must have content")
                    }
                }
                is BlockRequest.Heading3 -> {
                    if (block.heading3.richText.isEmpty()) {
                        errors.add("Heading blocks must have content")
                    }
                }
                is BlockRequest.BulletedListItem -> {
                    if (block.bulletedListItem.richText.isEmpty()) {
                        errors.add("List item blocks must have content")
                    }
                }
                is BlockRequest.NumberedListItem -> {
                    if (block.numberedListItem.richText.isEmpty()) {
                        errors.add("List item blocks must have content")
                    }
                }
                is BlockRequest.ToDo -> {
                    if (block.toDo.richText.isEmpty()) {
                        errors.add("To-do blocks must have content")
                    }
                }
                is BlockRequest.Toggle -> {
                    if (block.toggle.richText.isEmpty()) {
                        errors.add("Toggle blocks must have content")
                    }
                }
                is BlockRequest.Code -> {
                    if (block.code.richText.isEmpty()) {
                        errors.add("Code blocks must have content")
                    }
                }
                is BlockRequest.Quote -> {
                    if (block.quote.richText.isEmpty()) {
                        errors.add("Quote blocks must have content")
                    }
                }
                is BlockRequest.Callout -> {
                    if (block.callout.richText.isEmpty()) {
                        errors.add("Callout blocks must have content")
                    }
                }
                is BlockRequest.Divider -> {
                    // Dividers don't need validation
                }
            }
        }

        return errors
    }

    // Block creation methods

    /**
     * Adds a heading 1 block.
     *
     * @param text The heading text
     * @param color The text color (default: "default")
     * @param isToggleable Whether the heading can be collapsed
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun heading1(
        text: String,
        color: String = "default",
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        heading1(
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            color = color,
            isToggleable = isToggleable,
            children = children,
        )

    /**
     * Adds a heading 1 block with rich text.
     *
     * @param richText The heading rich text content
     * @param color The text color (default: "default")
     * @param isToggleable Whether the heading can be collapsed
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun heading1(
        richText: List<RichText>,
        color: String = "default",
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Heading1(
                heading1 = Heading1RequestContent(
                    richText = richText,
                    color = color,
                    isToggleable = isToggleable,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a heading 2 block.
     *
     * @param text The heading text
     * @param color The text color (default: "default")
     * @param isToggleable Whether the heading can be collapsed
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun heading2(
        text: String,
        color: String = "default",
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        heading2(
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            color = color,
            isToggleable = isToggleable,
            children = children,
        )

    /**
     * Adds a heading 2 block with rich text.
     *
     * @param richText The heading rich text content
     * @param color The text color (default: "default")
     * @param isToggleable Whether the heading can be collapsed
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun heading2(
        richText: List<RichText>,
        color: String = "default",
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Heading2(
                heading2 = Heading2RequestContent(
                    richText = richText,
                    color = color,
                    isToggleable = isToggleable,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a heading 3 block.
     *
     * @param text The heading text
     * @param color The text color (default: "default")
     * @param isToggleable Whether the heading can be collapsed
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun heading3(
        text: String,
        color: String = "default",
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        heading3(
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            color = color,
            isToggleable = isToggleable,
            children = children,
        )

    /**
     * Adds a heading 3 block with rich text.
     *
     * @param richText The heading rich text content
     * @param color The text color (default: "default")
     * @param isToggleable Whether the heading can be collapsed
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun heading3(
        richText: List<RichText>,
        color: String = "default",
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Heading3(
                heading3 = Heading3RequestContent(
                    richText = richText,
                    color = color,
                    isToggleable = isToggleable,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a paragraph block.
     *
     * @param text The paragraph text
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun paragraph(
        text: String,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        paragraph(
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            color = color,
            children = children,
        )

    /**
     * Adds a paragraph block with rich text.
     *
     * @param richText The paragraph rich text content
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun paragraph(
        richText: List<RichText>,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Paragraph(
                paragraph = ParagraphRequestContent(
                    richText = richText,
                    color = color,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a bulleted list item.
     *
     * @param text The list item text
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun bullet(
        text: String,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        bullet(
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            color = color,
            children = children,
        )

    /**
     * Adds a bulleted list item with rich text.
     *
     * @param richText The list item rich text content
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun bullet(
        richText: List<RichText>,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.BulletedListItem(
                bulletedListItem = BulletedListItemRequestContent(
                    richText = richText,
                    color = color,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a numbered list item.
     *
     * @param text The list item text
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun number(
        text: String,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        number(
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            color = color,
            children = children,
        )

    /**
     * Adds a numbered list item with rich text.
     *
     * @param richText The list item rich text content
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun number(
        richText: List<RichText>,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.NumberedListItem(
                numberedListItem = NumberedListItemRequestContent(
                    richText = richText,
                    color = color,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a to-do block.
     *
     * @param text The to-do text
     * @param checked Whether the to-do is completed
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun toDo(
        text: String,
        checked: Boolean = false,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        toDo(
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            checked = checked,
            color = color,
            children = children,
        )

    /**
     * Adds a to-do block with rich text.
     *
     * @param richText The to-do rich text content
     * @param checked Whether the to-do is completed
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun toDo(
        richText: List<RichText>,
        checked: Boolean = false,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.ToDo(
                toDo = ToDoRequestContent(
                    richText = richText,
                    checked = checked,
                    color = color,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a toggle block.
     *
     * @param text The toggle header text
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun toggle(
        text: String,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        toggle(
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            color = color,
            children = children,
        )

    /**
     * Adds a toggle block with rich text.
     *
     * @param richText The toggle header rich text content
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun toggle(
        richText: List<RichText>,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Toggle(
                toggle = ToggleRequestContent(
                    richText = richText,
                    color = color,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a code block.
     *
     * @param language The programming language for syntax highlighting
     * @param code The code content
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun code(
        language: String = "plain text",
        code: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.Code(
                code = CodeRequestContent(
                    richText = listOf(RequestBuilders.createSimpleRichText(code)),
                    language = language,
                    caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                ),
            ),
        )

    /**
     * Adds a quote block.
     *
     * @param text The quote text
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun quote(
        text: String,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        quote(
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            color = color,
            children = children,
        )

    /**
     * Adds a quote block with rich text.
     *
     * @param richText The quote rich text content
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun quote(
        richText: List<RichText>,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Quote(
                quote = QuoteRequestContent(
                    richText = richText,
                    color = color,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a callout block.
     *
     * @param emoji The emoji icon for the callout
     * @param text The callout text
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun callout(
        emoji: String,
        text: String,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder =
        callout(
            icon = CalloutIcon(type = "emoji", emoji = emoji),
            richText = listOf(RequestBuilders.createSimpleRichText(text)),
            color = color,
            children = children,
        )

    /**
     * Adds a callout block with rich text.
     *
     * @param icon The callout icon
     * @param richText The callout rich text content
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @return This builder for chaining
     */
    fun callout(
        icon: CalloutIcon? = null,
        richText: List<RichText>,
        color: String = "default",
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Callout(
                callout = CalloutRequestContent(
                    richText = richText,
                    icon = icon,
                    color = color,
                    children = childBlocks,
                ),
            ),
        )
    }

    /**
     * Adds a divider block.
     *
     * @return This builder for chaining
     */
    fun divider(): PageContentBuilder = addBlock(BlockRequest.Divider())
}

/**
 * DSL function for creating page content.
 *
 * @param init The builder configuration
 * @return List of block requests
 */
fun pageContent(init: PageContentBuilder.() -> Unit): List<BlockRequest> = PageContentBuilder().apply(init).build()
