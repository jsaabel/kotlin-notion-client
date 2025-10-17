@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.blocks

import it.saabel.kotlinnotionclient.models.base.Color
import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.files.FileUploadReference
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import it.saabel.kotlinnotionclient.models.richtext.RichTextBuilder
import it.saabel.kotlinnotionclient.models.richtext.richText

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
                is BlockRequest.Image -> {
                    // Image blocks need valid file reference - basic validation
                    if (block.image.external == null && block.image.file == null && block.image.fileUpload == null) {
                        errors.add("Image blocks must have a file reference (external, file, or file_upload)")
                    }
                }
                is BlockRequest.Video -> {
                    // Video blocks need valid file reference - basic validation
                    if (block.video.external == null && block.video.file == null && block.video.fileUpload == null) {
                        errors.add("Video blocks must have a file reference (external, file, or file_upload)")
                    }
                }
                is BlockRequest.Audio -> {
                    // Audio blocks need valid file reference - basic validation
                    if (block.audio.external == null && block.audio.file == null && block.audio.fileUpload == null) {
                        errors.add("Audio blocks must have a file reference (external, file, or file_upload)")
                    }
                }
                is BlockRequest.File -> {
                    // File blocks need valid file reference - basic validation
                    if (block.file.external == null && block.file.file == null && block.file.fileUpload == null) {
                        errors.add("File blocks must have a file reference (external, file, or file_upload)")
                    }
                }
                is BlockRequest.PDF -> {
                    // PDF blocks need valid file reference - basic validation
                    if (block.pdf.external == null && block.pdf.file == null && block.pdf.fileUpload == null) {
                        errors.add("PDF blocks must have a file reference (external, file, or file_upload)")
                    }
                }
                is BlockRequest.Divider -> {
                    // Dividers don't need validation
                }
                is BlockRequest.Table -> {
                    // Tables need valid width and at least one row
                    if (block.table.tableWidth <= 0) {
                        errors.add("Table width must be positive")
                    }
                    if (block.table.children.isNullOrEmpty()) {
                        errors.add("Tables must have at least one row")
                    }
                }
                is BlockRequest.TableRow -> {
                    // Table rows need cells
                    if (block.tableRow.cells.isEmpty()) {
                        errors.add("Table rows must have at least one cell")
                    }
                }
                is BlockRequest.Bookmark -> {
                    // Bookmarks need a URL
                    if (block.bookmark.url.isBlank()) {
                        errors.add("Bookmark blocks must have a URL")
                    }
                }
                is BlockRequest.Embed -> {
                    // Embeds need a URL
                    if (block.embed.url.isBlank()) {
                        errors.add("Embed blocks must have a URL")
                    }
                }
                is BlockRequest.ChildPage -> {
                    // Child pages need a title
                    if (block.childPage.title.isBlank()) {
                        errors.add("Child page blocks must have a title")
                    }
                }
                is BlockRequest.ChildDatabase -> {
                    // Child databases need a title
                    if (block.childDatabase.title.isBlank()) {
                        errors.add("Child database blocks must have a title")
                    }
                }
                is BlockRequest.ColumnList -> {
                    // Column lists need at least one column
                    if (block.columnList.children.isNullOrEmpty()) {
                        errors.add("Column lists must have at least one column")
                    }
                }
                is BlockRequest.Column -> {
                    // Columns can have empty content, so no validation needed
                }
                is BlockRequest.Breadcrumb -> {
                    // Breadcrumbs don't need validation
                }
                is BlockRequest.TableOfContents -> {
                    // Table of contents don't need validation
                }
                is BlockRequest.Equation -> {
                    // Equations need an expression
                    if (block.equation.expression.isBlank()) {
                        errors.add("Equation blocks must have an expression")
                    }
                }
                is BlockRequest.SyncedBlock -> {
                    // Synced blocks either need children (original) or syncedFrom (reference)
                    if (block.syncedBlock.syncedFrom == null && block.syncedBlock.children.isNullOrEmpty()) {
                        errors.add("Synced blocks must have either content or reference to original block")
                    }
                }
                is BlockRequest.Template -> {
                    // Templates need rich text content
                    if (block.template.richText.isEmpty()) {
                        errors.add("Template blocks must have rich text content")
                    }
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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Heading1(
                heading1 =
                    Heading1RequestContent(
                        richText = richText,
                        color = color,
                        isToggleable = isToggleable,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a heading 1 block with rich text DSL.
     *
     * @param color The text color (default: "default")
     * @param isToggleable Whether the heading can be collapsed
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun heading1(
        color: Color = Color.DEFAULT,
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        heading1(
            richText = richText(block),
            color = color,
            isToggleable = isToggleable,
            children = children,
        )

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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Heading2(
                heading2 =
                    Heading2RequestContent(
                        richText = richText,
                        color = color,
                        isToggleable = isToggleable,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a heading 2 block with rich text DSL.
     *
     * @param color The text color (default: "default")
     * @param isToggleable Whether the heading can be collapsed
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun heading2(
        color: Color = Color.DEFAULT,
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        heading2(
            richText = richText(block),
            color = color,
            isToggleable = isToggleable,
            children = children,
        )

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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Heading3(
                heading3 =
                    Heading3RequestContent(
                        richText = richText,
                        color = color,
                        isToggleable = isToggleable,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a heading 3 block with rich text DSL.
     *
     * @param color The text color (default: "default")
     * @param isToggleable Whether the heading can be collapsed
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun heading3(
        color: Color = Color.DEFAULT,
        isToggleable: Boolean = false,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        heading3(
            richText = richText(block),
            color = color,
            isToggleable = isToggleable,
            children = children,
        )

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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Paragraph(
                paragraph =
                    ParagraphRequestContent(
                        richText = richText,
                        color = color,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a paragraph block with rich text DSL.
     *
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun paragraph(
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        paragraph(
            richText = richText(block),
            color = color,
            children = children,
        )

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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.BulletedListItem(
                bulletedListItem =
                    BulletedListItemRequestContent(
                        richText = richText,
                        color = color,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a bulleted list item with rich text DSL.
     *
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun bullet(
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        bullet(
            richText = richText(block),
            color = color,
            children = children,
        )

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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.NumberedListItem(
                numberedListItem =
                    NumberedListItemRequestContent(
                        richText = richText,
                        color = color,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a numbered list item with rich text DSL.
     *
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun number(
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        number(
            richText = richText(block),
            color = color,
            children = children,
        )

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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.ToDo(
                toDo =
                    ToDoRequestContent(
                        richText = richText,
                        checked = checked,
                        color = color,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a to-do block with rich text DSL.
     *
     * @param checked Whether the to-do is completed
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun toDo(
        checked: Boolean = false,
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        toDo(
            richText = richText(block),
            checked = checked,
            color = color,
            children = children,
        )

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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Toggle(
                toggle =
                    ToggleRequestContent(
                        richText = richText,
                        color = color,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a toggle block with rich text DSL.
     *
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun toggle(
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        toggle(
            richText = richText(block),
            color = color,
            children = children,
        )

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
                code =
                    CodeRequestContent(
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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Quote(
                quote =
                    QuoteRequestContent(
                        richText = richText,
                        color = color,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a quote block with rich text DSL.
     *
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun quote(
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        quote(
            richText = richText(block),
            color = color,
            children = children,
        )

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
        color: Color = Color.DEFAULT,
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
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
    ): PageContentBuilder {
        val childBlocks = children?.let { pageContent(it) }?.takeIf { it.isNotEmpty() }
        return addBlock(
            BlockRequest.Callout(
                callout =
                    CalloutRequestContent(
                        richText = richText,
                        icon = icon,
                        color = color,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a callout block with rich text DSL.
     *
     * @param emoji The emoji icon for the callout
     * @param color The text color (default: "default")
     * @param children Optional nested content
     * @param block The rich text DSL block
     * @return This builder for chaining
     */
    fun callout(
        emoji: String = "💡",
        color: Color = Color.DEFAULT,
        children: (PageContentBuilder.() -> Unit)? = null,
        block: RichTextBuilder.() -> Unit,
    ): PageContentBuilder =
        callout(
            icon = CalloutIcon(type = "emoji", emoji = emoji),
            richText = richText(block),
            color = color,
            children = children,
        )

    /**
     * Adds an image block from an external URL.
     *
     * @param url The external image URL
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun image(
        url: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.Image(
                image =
                    ImageRequestContent(
                        type = "external",
                        external = ExternalFile(url = url),
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds an image block from a file upload.
     *
     * @param fileUploadId The ID of the uploaded file
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun imageFromUpload(
        fileUploadId: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.Image(
                image =
                    ImageRequestContent(
                        type = "file_upload",
                        fileUpload = FileUploadReference(id = fileUploadId),
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds a video block from an external URL.
     *
     * @param url The external video URL
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun video(
        url: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.Video(
                video =
                    VideoRequestContent(
                        type = "external",
                        external = ExternalFile(url = url),
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds a video block from a file upload.
     *
     * @param fileUploadId The ID of the uploaded file
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun videoFromUpload(
        fileUploadId: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.Video(
                video =
                    VideoRequestContent(
                        type = "file_upload",
                        fileUpload = FileUploadReference(id = fileUploadId),
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds an audio block from an external URL.
     *
     * @param url The external audio URL
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun audio(
        url: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.Audio(
                audio =
                    AudioRequestContent(
                        type = "external",
                        external = ExternalFile(url = url),
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds an audio block from a file upload.
     *
     * @param fileUploadId The ID of the uploaded file
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun audioFromUpload(
        fileUploadId: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.Audio(
                audio =
                    AudioRequestContent(
                        type = "file_upload",
                        fileUpload = FileUploadReference(id = fileUploadId),
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds a file block from an external URL.
     *
     * @param url The external file URL
     * @param name Optional file name
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun file(
        url: String,
        name: String? = null,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.File(
                file =
                    FileRequestContent(
                        type = "external",
                        external = ExternalFile(url = url),
                        name = name,
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds a file block from a file upload.
     *
     * @param fileUploadId The ID of the uploaded file
     * @param name Optional file name
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun fileFromUpload(
        fileUploadId: String,
        name: String? = null,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.File(
                file =
                    FileRequestContent(
                        type = "file_upload",
                        fileUpload = FileUploadReference(id = fileUploadId),
                        name = name,
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds a PDF block from an external URL.
     *
     * @param url The external PDF URL
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun pdf(
        url: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.PDF(
                pdf =
                    PDFRequestContent(
                        type = "external",
                        external = ExternalFile(url = url),
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds a PDF block from a file upload.
     *
     * @param fileUploadId The ID of the uploaded file
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun pdfFromUpload(
        fileUploadId: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.PDF(
                pdf =
                    PDFRequestContent(
                        type = "file_upload",
                        fileUpload = FileUploadReference(id = fileUploadId),
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds a divider block.
     *
     * @return This builder for chaining
     */
    fun divider(): PageContentBuilder = addBlock(BlockRequest.Divider())

    /**
     * Adds a table block with specified structure.
     *
     * @param tableWidth The number of columns in the table
     * @param hasColumnHeader Whether the first row is a header
     * @param hasRowHeader Whether the first column is a header
     * @param rows Builder for table rows
     * @return This builder for chaining
     */
    fun table(
        tableWidth: Int,
        hasColumnHeader: Boolean = false,
        hasRowHeader: Boolean = false,
        rows: TableRowBuilder.() -> Unit,
    ): PageContentBuilder {
        val tableRows = TableRowBuilder().apply(rows).build()
        return addBlock(
            BlockRequest.Table(
                table =
                    TableRequestContent(
                        tableWidth = tableWidth,
                        hasColumnHeader = hasColumnHeader,
                        hasRowHeader = hasRowHeader,
                        children = tableRows,
                    ),
            ),
        )
    }

    /**
     * Adds a single table row.
     *
     * @param cells The cell contents (list of list of rich text)
     * @return This builder for chaining
     */
    fun tableRow(cells: List<List<RichText>>): PageContentBuilder =
        addBlock(
            BlockRequest.TableRow(
                tableRow = TableRowRequestContent(cells = cells),
            ),
        )

    /**
     * Adds a simple table row with string cell contents.
     *
     * @param cellTexts The cell contents as simple strings
     * @return This builder for chaining
     */
    fun tableRow(vararg cellTexts: String): PageContentBuilder =
        tableRow(
            cells = cellTexts.map { listOf(RequestBuilders.createSimpleRichText(it)) },
        )

    /**
     * Adds a bookmark block.
     *
     * @param url The URL to bookmark
     * @param caption Optional caption text
     * @return This builder for chaining
     */
    fun bookmark(
        url: String,
        caption: String? = null,
    ): PageContentBuilder =
        addBlock(
            BlockRequest.Bookmark(
                bookmark =
                    BookmarkRequestContent(
                        url = url,
                        caption = caption?.let { listOf(RequestBuilders.createSimpleRichText(it)) } ?: emptyList(),
                    ),
            ),
        )

    /**
     * Adds an embed block.
     *
     * @param url The URL to embed
     * @return This builder for chaining
     */
    fun embed(url: String): PageContentBuilder =
        addBlock(
            BlockRequest.Embed(
                embed = EmbedRequestContent(url = url),
            ),
        )

    /**
     * Adds a child page block.
     *
     * @param title The title of the child page
     * @return This builder for chaining
     */
    fun childPage(title: String): PageContentBuilder =
        addBlock(
            BlockRequest.ChildPage(
                childPage = ChildPageRequestContent(title = title),
            ),
        )

    /**
     * Adds a child database block.
     *
     * @param title The title of the child database
     * @return This builder for chaining
     */
    fun childDatabase(title: String): PageContentBuilder =
        addBlock(
            BlockRequest.ChildDatabase(
                childDatabase = ChildDatabaseRequestContent(title = title),
            ),
        )

    /**
     * Adds a column layout with multiple columns.
     *
     * @param columns Builder for column content
     * @return This builder for chaining
     */
    fun columnList(columns: ColumnListBuilder.() -> Unit): PageContentBuilder {
        val columnBlocks = ColumnListBuilder().apply(columns).build()
        return addBlock(
            BlockRequest.ColumnList(
                columnList = ColumnListRequestContent(children = columnBlocks),
            ),
        )
    }

    /**
     * Adds a breadcrumb block.
     *
     * @return This builder for chaining
     */
    fun breadcrumb(): PageContentBuilder = addBlock(BlockRequest.Breadcrumb())

    /**
     * Adds a table of contents block.
     *
     * @param color The color of the table of contents
     * @return This builder for chaining
     */
    fun tableOfContents(color: Color = Color.DEFAULT): PageContentBuilder =
        addBlock(
            BlockRequest.TableOfContents(
                tableOfContents = TableOfContentsRequestContent(color = color),
            ),
        )

    /**
     * Adds an equation block.
     *
     * @param expression The LaTeX expression
     * @return This builder for chaining
     */
    fun equation(expression: String): PageContentBuilder =
        addBlock(
            BlockRequest.Equation(
                equation = EquationRequestContent(expression = expression),
            ),
        )

    /**
     * Adds a synced block (original).
     *
     * @param children The content of the synced block
     * @return This builder for chaining
     */
    fun syncedBlock(children: PageContentBuilder.() -> Unit): PageContentBuilder {
        val childBlocks = pageContent(children)
        return addBlock(
            BlockRequest.SyncedBlock(
                syncedBlock =
                    SyncedBlockRequestContent(
                        syncedFrom = null,
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a synced block reference (duplicate).
     *
     * @param originalBlockId The ID of the original synced block
     * @return This builder for chaining
     */
    fun syncedBlockReference(originalBlockId: String): PageContentBuilder =
        addBlock(
            BlockRequest.SyncedBlock(
                syncedBlock =
                    SyncedBlockRequestContent(
                        syncedFrom = SyncedBlockReference(blockId = originalBlockId),
                        children = null,
                    ),
            ),
        )

    /**
     * Adds a template block.
     *
     * @param text The template button text
     * @param children The template content
     * @return This builder for chaining
     */
    fun template(
        text: String,
        children: PageContentBuilder.() -> Unit,
    ): PageContentBuilder {
        val childBlocks = pageContent(children)
        return addBlock(
            BlockRequest.Template(
                template =
                    TemplateRequestContent(
                        richText = listOf(RequestBuilders.createSimpleRichText(text)),
                        children = childBlocks,
                    ),
            ),
        )
    }

    /**
     * Adds a template block with rich text.
     *
     * @param richText The template button rich text
     * @param children The template content
     * @return This builder for chaining
     */
    fun template(
        richText: List<RichText>,
        children: PageContentBuilder.() -> Unit,
    ): PageContentBuilder {
        val childBlocks = pageContent(children)
        return addBlock(
            BlockRequest.Template(
                template =
                    TemplateRequestContent(
                        richText = richText,
                        children = childBlocks,
                    ),
            ),
        )
    }
}

/**
 * DSL builder for column lists.
 */
class ColumnListBuilder {
    private val columns = mutableListOf<BlockRequest.Column>()

    /**
     * Adds a column to the column list.
     *
     * @param content The content of the column
     * @return This builder for chaining
     */
    fun column(content: PageContentBuilder.() -> Unit): ColumnListBuilder {
        val columnContent = pageContent(content)
        columns.add(
            BlockRequest.Column(
                column =
                    ColumnRequestContent(
                        children = columnContent,
                    ),
            ),
        )
        return this
    }

    /**
     * Builds the immutable list of column blocks.
     *
     * @return List of column block requests
     */
    internal fun build(): List<BlockRequest> = columns.toList()
}

/**
 * DSL builder for table rows within a table block.
 */
class TableRowBuilder {
    private val rows = mutableListOf<BlockRequest.TableRow>()

    /**
     * Adds a table row with specified cell contents.
     *
     * @param cells The cell contents (list of list of rich text)
     * @return This builder for chaining
     */
    fun row(cells: List<List<RichText>>): TableRowBuilder {
        rows.add(
            BlockRequest.TableRow(
                tableRow = TableRowRequestContent(cells = cells),
            ),
        )
        return this
    }

    /**
     * Adds a simple table row with string cell contents.
     *
     * @param cellTexts The cell contents as simple strings
     * @return This builder for chaining
     */
    fun row(vararg cellTexts: String): TableRowBuilder = row(cells = cellTexts.map { listOf(RequestBuilders.createSimpleRichText(it)) })

    /**
     * Builds the immutable list of table row blocks.
     *
     * @return List of table row block requests
     */
    internal fun build(): List<BlockRequest> = rows.toList()
}

/**
 * DSL function for creating page content.
 *
 * @param init The builder configuration
 * @return List of block requests
 */
fun pageContent(init: PageContentBuilder.() -> Unit): List<BlockRequest> = PageContentBuilder().apply(init).build()
