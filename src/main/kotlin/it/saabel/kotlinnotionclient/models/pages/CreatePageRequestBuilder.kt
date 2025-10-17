@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.PageContentBuilder
import it.saabel.kotlinnotionclient.models.blocks.pageContent

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
    private var iconValue: PageIcon? = null
    private var coverValue: PageCover? = null
    private var children: List<BlockRequest>? = null

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
     * @param block Configuration block for content
     */
    fun content(block: PageContentBuilder.() -> Unit) {
        children = pageContent(block)
    }

    /**
     * Builds the CreatePageRequest.
     *
     * @return The configured CreatePageRequest
     * @throws IllegalStateException if parent is not set
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

        return CreatePageRequest(
            parent = parentValue!!,
            properties = properties,
            icon = iconValue,
            cover = coverValue,
            children = children,
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
            this@CreatePageRequestBuilder.iconValue = PageIcon.Emoji(emoji = emoji)
        }

        /**
         * Sets an external image icon.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@CreatePageRequestBuilder.iconValue = PageIcon.External(external = ExternalFile(url = url))
        }

        /**
         * Sets an uploaded file icon.
         *
         * @param url The uploaded file URL
         * @param expiryTime Optional expiry time
         */
        fun file(
            url: String,
            expiryTime: String? = null,
        ) {
            this@CreatePageRequestBuilder.iconValue =
                PageIcon.File(file = NotionFile(url = url, expiryTime = expiryTime))
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
         * Sets an uploaded file cover.
         *
         * @param url The uploaded file URL
         * @param expiryTime Optional expiry time
         */
        fun file(
            url: String,
            expiryTime: String? = null,
        ) {
            this@CreatePageRequestBuilder.coverValue =
                PageCover.File(file = NotionFile(url = url, expiryTime = expiryTime))
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
