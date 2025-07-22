@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.pages

import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.models.blocks.PageContentBuilder
import no.saabelit.kotlinnotionclient.models.blocks.pageContent

/**
 * Builder class for creating page requests with a fluent DSL.
 *
 * This builder provides a convenient way to construct CreatePageRequest objects
 * with significantly less boilerplate than manual construction.
 *
 * ## Database Page Example (with properties):
 * ```kotlin
 * val request = pageRequest {
 *     parent.database(databaseId)
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

        // Validate that properties are only used with database parents
        val hasCustomProperties = properties.keys.any { it != "title" }
        val isDatabaseParent = parentValue?.type == "database_id"

        if (hasCustomProperties && !isDatabaseParent) {
            throw IllegalStateException(
                "Custom properties can only be set when creating pages in a database. " +
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
            this@CreatePageRequestBuilder.parentValue =
                Parent(
                    type = "page_id",
                    pageId = pageId,
                )
        }

        /**
         * Sets the parent to a database (creates a database entry).
         *
         * Database pages can have custom properties that match the database schema.
         *
         * @param databaseId The parent database ID
         */
        fun database(databaseId: String) {
            this@CreatePageRequestBuilder.parentValue =
                Parent(
                    type = "database_id",
                    databaseId = databaseId,
                )
        }

        /**
         * Sets the parent to a block.
         *
         * @param blockId The parent block ID
         */
        fun block(blockId: String) {
            this@CreatePageRequestBuilder.parentValue =
                Parent(
                    type = "block_id",
                    blockId = blockId,
                )
        }

        /**
         * Sets the parent to workspace.
         */
        fun workspace() {
            this@CreatePageRequestBuilder.parentValue =
                Parent(
                    type = "workspace",
                    workspace = true,
                )
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
            this@CreatePageRequestBuilder.iconValue =
                PageIcon(
                    type = "emoji",
                    emoji = emoji,
                )
        }

        /**
         * Sets an external image icon.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@CreatePageRequestBuilder.iconValue =
                PageIcon(
                    type = "external",
                    external = ExternalFile(url = url),
                )
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
                PageIcon(
                    type = "file",
                    file = NotionFile(url = url, expiryTime = expiryTime),
                )
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
            this@CreatePageRequestBuilder.coverValue =
                PageCover(
                    type = "external",
                    external = ExternalFile(url = url),
                )
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
                PageCover(
                    type = "file",
                    file = NotionFile(url = url, expiryTime = expiryTime),
                )
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
