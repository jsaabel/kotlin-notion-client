@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.blocks

import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.files.FileUploadReference
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
}

/**
 * DSL function for creating page content.
 *
 * @param init The builder configuration
 * @return List of block requests
 */
fun pageContent(init: PageContentBuilder.() -> Unit): List<BlockRequest> = PageContentBuilder().apply(init).build()
