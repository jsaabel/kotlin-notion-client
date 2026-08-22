@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.FileUploadReference
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.NativeIconColor
import it.saabel.kotlinnotionclient.models.base.NativeIconObject
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.PageContentBuilder
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import it.saabel.kotlinnotionclient.models.files.FileUpload
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus

/**
 * Builder class for creating page requests with a fluent DSL.
 *
 * This builder provides a convenient way to construct CreatePageRequest objects
 * with significantly less boilerplate than manual construction.
 *
 * ## Data Source Page Example (with properties) - API version 2025-09-03+:
 * ```kotlin
 * val request = pageRequest {
 *     parent.dataSource(dataSourceId)  // Use data source ID, not database ID
 *     properties {
 *         title("Name", "My New Database Entry")
 *         richText("Description", "A detailed description")
 *         number("Score", 85)
 *         checkbox("Completed", false)
 *     }
 *     icon.emoji("📄")
 *     content {
 *         heading1("Welcome")
 *         paragraph("This is the page content")
 *     }
 * }
 * ```
 *
 * ## Child Page Example (content only):
 * ```kotlin
 * val request = pageRequest {
 *     parent.page(parentPageId)
 *     title("My New Page")  // This sets the page title, not a property
 *     icon.emoji("📄")
 *     content {
 *         heading1("Welcome")
 *         paragraph("This is the page content")
 *     }
 * }
 * ```
 *
 * **Important**: Pages can only have custom properties when created in a database.
 * The properties must already exist in the database schema before they can be set.
 * Child pages (parent.page()) can only have a title and content.
 */
@PageRequestDslMarker
class CreatePageRequestBuilder {
    private var parentValue: Parent? = null
    private var properties = mutableMapOf<String, PagePropertyValue>()
    private var iconValue: Icon? = null
    private var coverValue: PageCover? = null
    private var children: List<BlockRequest>? = null
    private var markdownValue: String? = null
    private var templateValue: PageTemplate? = null
    private var positionValue: PagePosition? = null

    /**
     * Builder for parent configuration.
     */
    val parent = ParentBuilder()

    /**
     * Builder for icon configuration.
     */
    val icon = IconBuilder()

    /**
     * Builder for cover configuration.
     */
    val cover = CoverBuilder()

    /**
     * Builder for template configuration.
     */
    val template = TemplateBuilder()

    /**
     * Builder for position configuration.
     */
    val position = PositionBuilder()

    /**
     * Sets the page title.
     *
     * For database pages: sets the "title" property (database must have a title property)
     * For child pages: sets the page title directly in properties
     *
     * Note: Page titles only support plain text. Notion strips any formatting from titles.
     *
     * @param titleText The title text
     */
    fun title(titleText: String) {
        properties["title"] = PagePropertyValue.TitleValue.fromPlainText(titleText)
    }

    /**
     * Configures page properties using the PagePropertiesBuilder DSL.
     *
     * **Important**: This should only be used when creating pages in a database.
     * The properties must already exist in the database schema.
     *
     * For child pages (parent.page()), use only the title() method.
     *
     * @param block Configuration block for properties
     */
    fun properties(block: PagePropertiesBuilder.() -> Unit) {
        val builder = PagePropertiesBuilder()
        builder.block()
        properties.putAll(builder.build())
    }

    /**
     * Configures page content using the PageContentBuilder DSL.
     *
     * Mutually exclusive with [markdown] — the last call wins, but [build] enforces the constraint.
     *
     * @param block Configuration block for content
     */
    fun content(block: PageContentBuilder.() -> Unit) {
        children = pageContent(block)
    }

    /**
     * Sets page content as enhanced Markdown.
     *
     * Mutually exclusive with [content] and [template]. When provided, the API converts the markdown
     * string into Notion blocks server-side. The first `# h1` heading in the markdown becomes the
     * page title if no `properties.title` is set.
     *
     * Requires the integration to have `insert_content` capability on the target parent.
     *
     * @param content The enhanced Markdown string (use `\n` for line breaks)
     */
    fun markdown(content: String) {
        markdownValue = content
    }

    /**
     * Builds the CreatePageRequest.
     *
     * @return The configured CreatePageRequest
     * @throws IllegalStateException if parent is not set or if template and children are both specified
     */
    fun build(): CreatePageRequest {
        require(parentValue != null) { "Parent must be specified" }

        // Validate that properties are only used with data source parents (API version 2025-09-03+)
        val hasCustomProperties = properties.keys.any { it != "title" }
        val isDataSourceParent = parentValue?.type == "data_source_id"

        if (hasCustomProperties && !isDataSourceParent) {
            throw IllegalStateException(
                "Custom properties can only be set when creating pages in a data source. " +
                    "For child pages, use only the title() method.",
            )
        }

        // Validate that template and children are mutually exclusive
        if (templateValue != null && children != null) {
            throw IllegalStateException(
                "Template and children are mutually exclusive. " +
                    "When using a template, do not specify children - the template content will be applied.",
            )
        }

        // Validate that markdown is mutually exclusive with children and template
        if (markdownValue != null && children != null) {
            throw IllegalStateException(
                "markdown and children are mutually exclusive. " +
                    "Use either markdown() or content(), not both.",
            )
        }
        if (markdownValue != null && templateValue != null) {
            throw IllegalStateException(
                "markdown and template are mutually exclusive. " +
                    "Use either markdown() or template(), not both.",
            )
        }

        return CreatePageRequest(
            parent = parentValue!!,
            properties = properties,
            icon = iconValue,
            cover = coverValue,
            children = children,
            markdown = markdownValue,
            template = templateValue,
            position = positionValue,
        )
    }

    /**
     * Builder for parent configuration.
     */
    @PageRequestDslMarker
    inner class ParentBuilder {
        /**
         * Sets the parent to a page (creates a child page).
         *
         * Child pages can only have a title and content - no custom properties.
         *
         * @param pageId The parent page ID
         */
        fun page(pageId: String) {
            this@CreatePageRequestBuilder.parentValue = Parent.PageParent(pageId = pageId)
        }

        /**
         * Sets the parent to a data source (creates a database entry) - API version 2025-09-03+.
         *
         * Data source pages can have custom properties that match the data source schema.
         * Use the data source ID, not the database ID. To get a data source ID:
         * 1. Retrieve the database to get its data_sources array
         * 2. Use the ID from the desired data source
         *
         * @param dataSourceId The parent data source ID
         */
        fun dataSource(dataSourceId: String) {
            this@CreatePageRequestBuilder.parentValue = Parent.DataSourceParent(dataSourceId = dataSourceId)
        }

        /**
         * Sets the parent to a block.
         *
         * @param blockId The parent block ID
         */
        fun block(blockId: String) {
            this@CreatePageRequestBuilder.parentValue = Parent.BlockParent(blockId = blockId)
        }

        /**
         * Sets the parent to workspace.
         */
        fun workspace() {
            this@CreatePageRequestBuilder.parentValue = Parent.WorkspaceParent
        }
    }

    /**
     * Builder for icon configuration.
     */
    @PageRequestDslMarker
    inner class IconBuilder {
        /**
         * Sets an emoji icon.
         *
         * @param emoji The emoji character(s)
         */
        fun emoji(emoji: String) {
            this@CreatePageRequestBuilder.iconValue = Icon.Emoji(emoji = emoji)
        }

        /**
         * Sets an external image icon.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@CreatePageRequestBuilder.iconValue = Icon.External(external = ExternalFile(url = url))
        }

        /**
         * Sets a Notion-hosted file icon from its expiring URL.
         *
         * This emits the *read* shape (`type: "file"`), which Notion rejects on write. Verified
         * live: the request fails with HTTP 400 `validation_error`, naming `emoji`, `external`,
         * `custom_emoji`, `file_upload` and `icon` as the shapes it will accept. Kept for source
         * compatibility only — there is no input for which this call succeeds.
         *
         * @param url The uploaded file URL
         * @param expiryTime Optional expiry time
         */
        @Deprecated(
            message =
                "Verified live: a `type: \"file\"` icon is rejected with HTTP 400 validation_error — it is " +
                    "the read shape (a Notion-hosted expiring URL) and can never be written back. Notion " +
                    "accepts emoji, external, custom_emoji, file_upload and icon. Use external(url) for a " +
                    "publicly hosted file, or upload(id) for a file sent through the File Upload API.",
            replaceWith = ReplaceWith("external(url)"),
        )
        fun file(
            url: String,
            expiryTime: String? = null,
        ) {
            this@CreatePageRequestBuilder.iconValue =
                Icon.File(file = NotionFile(url = url, expiryTime = expiryTime))
        }

        /**
         * Sets an icon from a file uploaded via the File Upload API.
         *
         * The upload must already have reached [FileUploadStatus.UPLOADED]; attach it within its
         * one-hour expiry window or the upload is archived.
         *
         * Note the write/read asymmetry, verified live: the icon is *written* as `file_upload`
         * and *reads back* as `Icon.File` — a time-limited signed S3 URL. Round-tripping an icon
         * therefore means re-uploading or switching to `external`, not echoing back what was read.
         *
         * @param fileUploadId The ID of the uploaded file
         */
        fun upload(fileUploadId: String) {
            this@CreatePageRequestBuilder.iconValue =
                Icon.FileUpload(fileUpload = FileUploadReference(id = fileUploadId))
        }

        /**
         * Sets an icon from a file uploaded via the File Upload API.
         *
         * @param fileUpload The upload returned by the File Upload API
         */
        fun upload(fileUpload: FileUpload) {
            upload(fileUpload.id)
        }

        /**
         * Sets a native Notion icon.
         *
         * @param name The icon name (e.g. "pizza")
         * @param color Optional color. Defaults to [NativeIconColor.GRAY] when omitted.
         */
        fun native(
            name: String,
            color: NativeIconColor? = null,
        ) {
            this@CreatePageRequestBuilder.iconValue = Icon.NativeIcon(NativeIconObject(name = name, color = color))
        }
    }

    /**
     * Builder for cover configuration.
     */
    @PageRequestDslMarker
    inner class CoverBuilder {
        /**
         * Sets an external image cover.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@CreatePageRequestBuilder.coverValue = PageCover.External(external = ExternalFile(url = url))
        }

        /**
         * Sets a Notion-hosted file cover from its expiring URL.
         *
         * This emits the *read* shape (`type: "file"`), which Notion rejects on write. Verified
         * live: the request fails with HTTP 400 `validation_error`, naming `emoji`, `external`,
         * `custom_emoji`, `file_upload` and `icon` as the shapes it will accept. Kept for source
         * compatibility only — there is no input for which this call succeeds.
         *
         * @param url The uploaded file URL
         * @param expiryTime Optional expiry time
         */
        @Deprecated(
            message =
                "Verified live: a `type: \"file\"` cover is rejected with HTTP 400 validation_error — it is " +
                    "the read shape (a Notion-hosted expiring URL) and can never be written back. Notion " +
                    "accepts emoji, external, custom_emoji, file_upload and icon. Use external(url) for a " +
                    "publicly hosted file, or upload(id) for a file sent through the File Upload API.",
            replaceWith = ReplaceWith("external(url)"),
        )
        fun file(
            url: String,
            expiryTime: String? = null,
        ) {
            this@CreatePageRequestBuilder.coverValue =
                PageCover.File(file = NotionFile(url = url, expiryTime = expiryTime))
        }

        /**
         * Sets a cover from a file uploaded via the File Upload API.
         *
         * The upload must already have reached [FileUploadStatus.UPLOADED]; attach it within its
         * one-hour expiry window or the upload is archived.
         *
         * Note the write/read asymmetry, verified live: the cover is *written* as `file_upload`
         * and *reads back* as `PageCover.File` — a time-limited signed S3 URL. Round-tripping a
         * cover therefore means re-uploading or switching to `external`, not echoing back what
         * was read.
         *
         * @param fileUploadId The ID of the uploaded file
         */
        fun upload(fileUploadId: String) {
            this@CreatePageRequestBuilder.coverValue =
                PageCover.FileUpload(fileUpload = FileUploadReference(id = fileUploadId))
        }

        /**
         * Sets a cover from a file uploaded via the File Upload API.
         *
         * @param fileUpload The upload returned by the File Upload API
         */
        fun upload(fileUpload: FileUpload) {
            upload(fileUpload.id)
        }
    }

    /**
     * Builder for template configuration.
     *
     * Templates allow creating pages from predefined blueprints in the data source.
     * Note: When using a template, the children (content) parameter is prohibited.
     */
    @PageRequestDslMarker
    inner class TemplateBuilder {
        /**
         * Creates the page with no template content.
         */
        fun none() {
            this@CreatePageRequestBuilder.templateValue = PageTemplate.None
        }

        /**
         * Uses the data source's default template.
         */
        fun default() {
            this@CreatePageRequestBuilder.templateValue = PageTemplate.Default
        }

        /**
         * Uses a specific template by ID.
         *
         * @param templateId The ID of the template page to use
         */
        fun byId(templateId: String) {
            this@CreatePageRequestBuilder.templateValue = PageTemplate.TemplateId(templateId = templateId)
        }
    }

    /**
     * Builder for position configuration.
     *
     * Controls where the new page is placed within its parent.
     */
    @PageRequestDslMarker
    inner class PositionBuilder {
        /**
         * Places the page after a specific block.
         *
         * @param blockId The ID of the block to place the page after
         */
        fun afterBlock(blockId: String) {
            this@CreatePageRequestBuilder.positionValue = PagePosition.AfterBlock(afterBlock = blockId)
        }

        /**
         * Places the page at the start of the parent.
         */
        fun pageStart() {
            this@CreatePageRequestBuilder.positionValue = PagePosition.PageStart
        }

        /**
         * Places the page at the end of the parent.
         */
        fun pageEnd() {
            this@CreatePageRequestBuilder.positionValue = PagePosition.PageEnd
        }
    }
}

/**
 * DSL marker to prevent nested scopes.
 */
@DslMarker
annotation class PageRequestDslMarker

/**
 * Entry point function for the page request DSL.
 *
 * @param block Configuration block for the page request
 * @return The configured CreatePageRequest
 */
fun createPageRequest(block: CreatePageRequestBuilder.() -> Unit): CreatePageRequest {
    val builder = CreatePageRequestBuilder()
    builder.block()
    return builder.build()
}
