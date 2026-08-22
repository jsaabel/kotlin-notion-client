@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.FileUploadReference
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.NativeIconColor
import it.saabel.kotlinnotionclient.models.base.NativeIconObject
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.files.FileUpload
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.utils.FileSource
import it.saabel.kotlinnotionclient.utils.asFileSource
import java.io.File
import java.nio.file.Path

/**
 * Builder for updating a database container with a fluent DSL.
 *
 * The container owns `parent`, `title`, `icon`, `cover`, `is_inline` and `in_trash`; a data
 * source owns the schema. Reach the other half through
 * [updateDataSourceRequest][it.saabel.kotlinnotionclient.models.datasources.updateDataSourceRequest].
 *
 * ## Example
 * ```kotlin
 * val request = updateDatabaseRequest {
 *     title("Q3 Planning")
 *     icon.emoji("📊")
 *     parent.page(newParentId)
 *     inline(true)
 * }
 * ```
 *
 * **Important notes**:
 * - Only what the block names is sent; everything else on the database is left alone.
 * - Setting a cover on an inline database is rejected by [build] rather than by the API.
 * - **There is no `icon.remove()` / `cover.remove()` here.** Unlike the page endpoint, which
 *   documents `"icon": null` as the removal instruction, `PATCH /v1/databases` rejects it —
 *   verified live on 2026-08-22:
 *
 *   ```
 *   HTTP 400 validation_error: body failed validation:
 *   body.icon should be an object or `undefined`, instead was `null`.
 *   ```
 *
 *   "an object or `undefined`" leaves no room for a removal: the container icon and cover can be
 *   replaced but not cleared. The sentinels from
 *   `docs/adr/0002-explicit-null-payloads.md` still exist and still encode correctly — the
 *   endpoint simply does not accept what they encode, so the affordance is not offered rather
 *   than offered and broken. `DatabaseAttributeProbeIntegrationTest` re-checks this, and whether
 *   the data source endpoint differs.
 */
@UpdateDatabaseRequestDslMarker
class UpdateDatabaseRequestBuilder {
    private var parentValue: Parent? = null
    private var titleValue: List<RichText>? = null
    private var iconValue: Icon? = null
    private var coverValue: PageCover? = null
    private var isInlineValue: Boolean? = null
    private var inTrashValue: Boolean? = null

    /**
     * Builder for parent configuration — moving the database to a different parent.
     */
    val parent = ParentBuilder()

    /**
     * Configures the parent in a lambda, as an alternative to the `parent.xxx()` receiver form.
     *
     * Both forms drive the same builder and are last-call-wins; see
     * [docs/dsl-conventions.md](https://github.com/jsaabel/kotlin-notion-client/blob/main/docs/dsl-conventions.md).
     *
     * @param block Configuration block applied to the parent builder
     */
    fun parent(block: ParentBuilder.() -> Unit) {
        parent.block()
    }

    /**
     * Builder for icon configuration.
     */
    val icon = IconBuilder()

    /**
     * Configures the icon in a lambda, as an alternative to the `icon.xxx()` receiver form.
     *
     * Both forms drive the same builder and are last-call-wins; see
     * [docs/dsl-conventions.md](https://github.com/jsaabel/kotlin-notion-client/blob/main/docs/dsl-conventions.md).
     *
     * @param block Configuration block applied to the icon builder
     */
    fun icon(block: IconBuilder.() -> Unit) {
        icon.block()
    }

    /**
     * Builder for cover configuration.
     */
    val cover = CoverBuilder()

    /**
     * Configures the cover in a lambda, as an alternative to the `cover.xxx()` receiver form.
     *
     * Both forms drive the same builder and are last-call-wins; see
     * [docs/dsl-conventions.md](https://github.com/jsaabel/kotlin-notion-client/blob/main/docs/dsl-conventions.md).
     *
     * @param block Configuration block applied to the cover builder
     */
    fun cover(block: CoverBuilder.() -> Unit) {
        cover.block()
    }

    /**
     * Sets the database title.
     *
     * This is the *container's* title. A data source carries its own, set through
     * [updateDataSourceRequest][it.saabel.kotlinnotionclient.models.datasources.updateDataSourceRequest].
     *
     * @param titleText The title text
     */
    fun title(titleText: String) {
        titleValue = listOf(RichText.fromPlainText(titleText))
    }

    /**
     * Sets the database title from rich text.
     *
     * @param richText The title as rich text objects
     */
    fun title(richText: List<RichText>) {
        titleValue = richText
    }

    /**
     * Renders the database inline in its parent page (or as a full page).
     *
     * Notion does not support a cover on an inline database, so combining `inline(true)` with a
     * cover in the same request fails in [build].
     *
     * @param inline Whether to render inline. Defaults to true.
     */
    fun inline(inline: Boolean = true) {
        isInlineValue = inline
    }

    /**
     * Moves the database to trash (or restores it).
     *
     * @param inTrash Whether to move to trash (true) or restore from trash (false). Defaults to true.
     */
    fun trash(inTrash: Boolean = true) {
        inTrashValue = inTrash
    }

    /**
     * Restores the database from trash.
     */
    fun restore() {
        inTrashValue = false
    }

    /**
     * Builds the [UpdateDatabaseRequest].
     *
     * @return The configured request
     * @throws IllegalArgumentException if the request sets a cover on an inline database
     */
    fun build(): UpdateDatabaseRequest {
        // "cover is not supported when is_inline is true" —
        // reference/notion-api/upgrading_to_2025_09_03/upgrade_guide.md. Only a request that
        // says both things is decidable here: a cover with no `is_inline` may be landing on a
        // full-page database, and that is the API's call, not ours.
        require(!(isInlineValue == true && coverValue != null)) {
            "Notion does not support a cover on an inline database: this request sets both " +
                "cover and is_inline = true. Drop the cover, or set inline(false)."
        }

        return UpdateDatabaseRequest(
            parent = parentValue,
            title = titleValue,
            icon = iconValue,
            cover = coverValue,
            isInline = isInlineValue,
            inTrash = inTrashValue,
        )
    }

    /**
     * Builder for parent configuration.
     *
     * Setting a parent **moves** the database — new in the 2025-09-03 API. The upgrade guide
     * documents moving to a different page, and (for public integrations) to the workspace
     * level as a private page.
     */
    @UpdateDatabaseRequestDslMarker
    inner class ParentBuilder {
        /**
         * Moves the database under a page.
         *
         * @param pageId The new parent page ID
         */
        fun page(pageId: String) {
            this@UpdateDatabaseRequestBuilder.parentValue = Parent.PageParent(pageId = pageId)
        }

        /**
         * Moves the database under a block.
         *
         * @param blockId The new parent block ID
         */
        fun block(blockId: String) {
            this@UpdateDatabaseRequestBuilder.parentValue = Parent.BlockParent(blockId = blockId)
        }

        /**
         * Moves the database to the workspace level, as a private page.
         *
         * Documented as available to public integrations.
         */
        fun workspace() {
            this@UpdateDatabaseRequestBuilder.parentValue = Parent.WorkspaceParent
        }
    }

    /**
     * Builder for icon configuration.
     */
    @UpdateDatabaseRequestDslMarker
    inner class IconBuilder {
        /**
         * Sets an emoji icon.
         *
         * @param emoji The emoji character(s)
         */
        fun emoji(emoji: String) {
            this@UpdateDatabaseRequestBuilder.iconValue = Icon.Emoji(emoji = emoji)
        }

        /**
         * Sets an external image icon.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@UpdateDatabaseRequestBuilder.iconValue = Icon.External(external = ExternalFile(url = url))
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
            this@UpdateDatabaseRequestBuilder.iconValue =
                Icon.File(file = NotionFile(url = url, expiryTime = expiryTime))
        }

        /**
         * Sets an icon from a file uploaded via the File Upload API.
         *
         * The upload must already have reached [FileUploadStatus.UPLOADED]; attach it within its
         * one-hour expiry window or the upload is archived.
         *
         * @param fileUploadId The ID of the uploaded file
         */
        fun upload(fileUploadId: String) {
            this@UpdateDatabaseRequestBuilder.iconValue =
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
         * Sets an icon from a local file, uploading it when the request is sent.
         *
         * Nothing is uploaded while this builder runs: the file is recorded as
         * [Icon.PendingUpload] and resolved by the client before the request goes out. See
         * `docs/adr/0001-deferred-file-upload-resolution.md`.
         *
         * ```kotlin
         * icon.upload(File("logo.png"))
         * ```
         *
         * @param source the file to upload
         * @param options upload options — content type override, progress callback, validation
         */
        fun upload(
            source: FileSource,
            options: FileUploadOptions = FileUploadOptions(),
        ) {
            this@UpdateDatabaseRequestBuilder.iconValue = Icon.PendingUpload(source = source, options = options)
        }

        /** Sets an icon from a local file, uploading it when the request is sent. See [upload]. */
        fun upload(
            file: File,
            options: FileUploadOptions = FileUploadOptions(),
        ) {
            upload(file.asFileSource(), options)
        }

        /** Sets an icon from a local file, uploading it when the request is sent. See [upload]. */
        fun upload(
            path: Path,
            options: FileUploadOptions = FileUploadOptions(),
        ) {
            upload(path.asFileSource(), options)
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
            this@UpdateDatabaseRequestBuilder.iconValue = Icon.NativeIcon(NativeIconObject(name = name, color = color))
        }
    }

    /**
     * Builder for cover configuration.
     */
    @UpdateDatabaseRequestDslMarker
    inner class CoverBuilder {
        /**
         * Sets an external image cover.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@UpdateDatabaseRequestBuilder.coverValue = PageCover.External(external = ExternalFile(url = url))
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
            this@UpdateDatabaseRequestBuilder.coverValue =
                PageCover.File(file = NotionFile(url = url, expiryTime = expiryTime))
        }

        /**
         * Sets a cover from a file uploaded via the File Upload API.
         *
         * The upload must already have reached [FileUploadStatus.UPLOADED]; attach it within its
         * one-hour expiry window or the upload is archived.
         *
         * @param fileUploadId The ID of the uploaded file
         */
        fun upload(fileUploadId: String) {
            this@UpdateDatabaseRequestBuilder.coverValue =
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
         * Sets a cover from a local file, uploading it when the request is sent.
         *
         * Nothing is uploaded while this builder runs: the file is recorded as
         * [PageCover.PendingUpload] and resolved by the client before the request goes out. See
         * `docs/adr/0001-deferred-file-upload-resolution.md`.
         *
         * ```kotlin
         * cover.upload(File("banner.png"))
         * ```
         *
         * @param source the file to upload
         * @param options upload options — content type override, progress callback, validation
         */
        fun upload(
            source: FileSource,
            options: FileUploadOptions = FileUploadOptions(),
        ) {
            this@UpdateDatabaseRequestBuilder.coverValue = PageCover.PendingUpload(source = source, options = options)
        }

        /** Sets a cover from a local file, uploading it when the request is sent. See [upload]. */
        fun upload(
            file: File,
            options: FileUploadOptions = FileUploadOptions(),
        ) {
            upload(file.asFileSource(), options)
        }

        /** Sets a cover from a local file, uploading it when the request is sent. See [upload]. */
        fun upload(
            path: Path,
            options: FileUploadOptions = FileUploadOptions(),
        ) {
            upload(path.asFileSource(), options)
        }
    }
}

/**
 * DSL marker to prevent nested scopes.
 */
@DslMarker
annotation class UpdateDatabaseRequestDslMarker

/**
 * Entry point function for the update database request DSL.
 *
 * @param block Configuration block for the update database request
 * @return The configured [UpdateDatabaseRequest]
 */
fun updateDatabaseRequest(block: UpdateDatabaseRequestBuilder.() -> Unit): UpdateDatabaseRequest {
    val builder = UpdateDatabaseRequestBuilder()
    builder.block()
    return builder.build()
}
