@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.NotionFile

/**
 * Builder class for updating page requests with a fluent DSL.
 *
 * This builder provides a convenient way to construct UpdatePageRequest objects
 * with the same fluent API as page creation, ensuring consistency across the codebase.
 *
 * ## Basic Update Example:
 * ```kotlin
 * val request = updatePageRequest {
 *     properties {
 *         checkbox("Completed", true)
 *         number("Score", 95.0)
 *         select("Status", "Done")
 *     }
 *     icon.emoji("✅")
 * }
 * ```
 *
 * ## Complex Update Example:
 * ```kotlin
 * val request = updatePageRequest {
 *     properties {
 *         title("Task Name", "Updated Task Title")
 *         richText("Description", "Updated description with more details")
 *         multiSelect("Tags", "urgent", "completed")
 *         date("Due", "2024-12-31")
 *         people("Assignee", "user-id-1", "user-id-2")
 *     }
 *     icon.external("https://example.com/new-icon.png")
 *     cover.external("https://example.com/new-cover.jpg")
 *     archive()
 * }
 * ```
 *
 * **Important Notes**:
 * - Only properties specified in the DSL will be updated; other properties remain unchanged
 * - Archive status can be set with `archive()` or `archive(true/false)`
 */
@UpdatePageRequestDslMarker
class UpdatePageRequestBuilder {
    private var properties = mutableMapOf<String, PagePropertyValue>()
    private var iconValue: PageIcon? = null
    private var coverValue: PageCover? = null
    private var archivedValue: Boolean? = null
    private var isLockedValue: Boolean? = null
    private var templateValue: PageTemplate? = null
    private var eraseContentValue: Boolean? = null

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
     * Configures page properties to update using the PagePropertiesBuilder DSL.
     *
     * Only the properties specified in the block will be updated.
     * Other properties on the page will remain unchanged.
     *
     * @param block Configuration block for properties to update
     */
    fun properties(block: PagePropertiesBuilder.() -> Unit) {
        val builder = PagePropertiesBuilder()
        builder.block()
        properties.putAll(builder.build())
    }

    /**
     * Archives the page.
     *
     * @param archived Whether to archive (true) or unarchive (false) the page. Defaults to true.
     */
    fun archive(archived: Boolean = true) {
        archivedValue = archived
    }

    /**
     * Locks the page from editing in the Notion app UI.
     *
     * @param locked Whether to lock (true) or unlock (false) the page. Defaults to true.
     */
    fun lock(locked: Boolean = true) {
        isLockedValue = locked
    }

    /**
     * Unlocks the page for editing in the Notion app UI.
     */
    fun unlock() {
        isLockedValue = false
    }

    /**
     * Erases all existing content from the page.
     *
     * **Warning**: This is destructive and irreversible.
     * When used with a template, the template content replaces existing content.
     *
     * @param erase Whether to erase content. Defaults to true.
     */
    fun eraseContent(erase: Boolean = true) {
        eraseContentValue = erase
    }

    /**
     * Builds the UpdatePageRequest.
     *
     * @return The configured UpdatePageRequest
     */
    fun build(): UpdatePageRequest =
        UpdatePageRequest(
            properties = properties.takeIf { it.isNotEmpty() },
            icon = iconValue,
            cover = coverValue,
            archived = archivedValue,
            isLocked = isLockedValue,
            template = templateValue,
            eraseContent = eraseContentValue,
        )

    /**
     * Builder for icon configuration.
     */
    @UpdatePageRequestDslMarker
    inner class IconBuilder {
        /**
         * Sets an emoji icon.
         *
         * @param emoji The emoji character(s)
         */
        fun emoji(emoji: String) {
            this@UpdatePageRequestBuilder.iconValue = PageIcon.Emoji(emoji = emoji)
        }

        /**
         * Sets an external image icon.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@UpdatePageRequestBuilder.iconValue = PageIcon.External(external = ExternalFile(url = url))
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
            this@UpdatePageRequestBuilder.iconValue =
                PageIcon.File(file = NotionFile(url = url, expiryTime = expiryTime))
        }

        /**
         * Removes the page icon by setting it to null.
         *
         * Note: According to the Notion API, to remove an icon, you should
         * pass null for the icon field in the request.
         */
        fun remove() {
            this@UpdatePageRequestBuilder.iconValue = null
        }
    }

    /**
     * Builder for cover configuration.
     */
    @UpdatePageRequestDslMarker
    inner class CoverBuilder {
        /**
         * Sets an external image cover.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@UpdatePageRequestBuilder.coverValue = PageCover.External(external = ExternalFile(url = url))
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
            this@UpdatePageRequestBuilder.coverValue =
                PageCover.File(file = NotionFile(url = url, expiryTime = expiryTime))
        }

        /**
         * Removes the page cover by setting it to null.
         *
         * Note: According to the Notion API, to remove a cover, you should
         * pass null for the cover field in the request.
         */
        fun remove() {
            this@UpdatePageRequestBuilder.coverValue = null
        }
    }

    /**
     * Builder for template configuration.
     */
    @UpdatePageRequestDslMarker
    inner class TemplateBuilder {
        /**
         * Uses the data source's default template.
         */
        fun default() {
            this@UpdatePageRequestBuilder.templateValue = PageTemplate.Default
        }

        /**
         * Uses a specific template by ID.
         *
         * @param templateId The ID of the template page to use
         */
        fun byId(templateId: String) {
            this@UpdatePageRequestBuilder.templateValue = PageTemplate.TemplateId(templateId = templateId)
        }
    }
}

/**
 * DSL marker to prevent nested scopes.
 */
@DslMarker
annotation class UpdatePageRequestDslMarker

/**
 * Entry point function for the update page request DSL.
 *
 * @param block Configuration block for the update page request
 * @return The configured UpdatePageRequest
 */
fun updatePageRequest(block: UpdatePageRequestBuilder.() -> Unit): UpdatePageRequest {
    val builder = UpdatePageRequestBuilder()
    builder.block()
    return builder.build()
}
