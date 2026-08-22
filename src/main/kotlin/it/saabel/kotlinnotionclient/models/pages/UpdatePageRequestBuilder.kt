@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.FileUploadReference
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.NativeIconColor
import it.saabel.kotlinnotionclient.models.base.NativeIconObject
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.files.FileUpload
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus

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
 *     trash()
 * }
 * ```
 *
 * **Important Notes**:
 * - Only properties specified in the DSL will be updated; other properties remain unchanged
 * - Trash status can be set with `trash()` or `trash(true/false)`
 */
@UpdatePageRequestDslMarker
class UpdatePageRequestBuilder {
    private var properties = mutableMapOf<String, PagePropertyValue>()
    private var iconValue: Icon? = null
    private var coverValue: PageCover? = null
    private var inTrashValue: Boolean? = null
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
     * Moves the page to trash (or restores it).
     *
     * @param inTrash Whether to move to trash (true) or restore from trash (false). Defaults to true.
     */
    fun trash(inTrash: Boolean = true) {
        inTrashValue = inTrash
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
            inTrash = inTrashValue,
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
            this@UpdatePageRequestBuilder.iconValue = Icon.Emoji(emoji = emoji)
        }

        /**
         * Sets an external image icon.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@UpdatePageRequestBuilder.iconValue = Icon.External(external = ExternalFile(url = url))
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
            this@UpdatePageRequestBuilder.iconValue =
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
            this@UpdatePageRequestBuilder.iconValue =
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
            this@UpdatePageRequestBuilder.iconValue = Icon.NativeIcon(NativeIconObject(name = name, color = color))
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
            this@UpdatePageRequestBuilder.coverValue =
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
            this@UpdatePageRequestBuilder.coverValue =
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
