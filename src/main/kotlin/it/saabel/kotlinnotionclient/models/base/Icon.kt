package it.saabel.kotlinnotionclient.models.base

import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.files.PendingUploadRefusingSerializer
import it.saabel.kotlinnotionclient.serialization.RemovalSentinelSerializer
import it.saabel.kotlinnotionclient.utils.FileSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an icon in Notion.
 *
 * Icons appear on pages, databases, callout blocks, and tab paragraph panes.
 * All icon fields across the Notion API use this same structure.
 *
 * ## Example usage:
 * ```kotlin
 * when (page.icon) {
 *     is Icon.Emoji -> println("Emoji: ${page.icon.emoji}")
 *     is Icon.CustomEmoji -> println("Custom emoji: ${page.icon.customEmoji.name}")
 *     is Icon.External -> println("External URL: ${page.icon.external.url}")
 *     is Icon.File -> println("File URL: ${page.icon.file.url}")
 *     is Icon.FileUpload -> println("Upload ID: ${page.icon.fileUpload.id}")
 *     is Icon.NativeIcon -> println("Native icon: ${page.icon.icon.name}")
 *     Icon.Removed -> println("Icon removal (requests only)")
 *     null -> println("No icon")
 * }
 * ```
 */
@Serializable(with = IconSerializer::class)
sealed class Icon {
    abstract val type: String

    /**
     * Standard emoji icon (e.g., 🥑).
     */
    @Serializable
    data class Emoji(
        @SerialName("type")
        override val type: String,
        @SerialName("emoji")
        val emoji: String,
    ) : Icon() {
        constructor(emoji: String) : this(type = "emoji", emoji = emoji)
    }

    /**
     * Custom emoji uploaded and managed in the workspace.
     */
    @Serializable
    data class CustomEmoji(
        @SerialName("type")
        override val type: String,
        @SerialName("custom_emoji")
        val customEmoji: CustomEmojiObject,
    ) : Icon() {
        constructor(customEmoji: CustomEmojiObject) : this(type = "custom_emoji", customEmoji = customEmoji)
    }

    /**
     * Icon hosted externally via a public URL.
     */
    @Serializable
    data class External(
        @SerialName("type")
        override val type: String,
        @SerialName("external")
        val external: ExternalFile,
    ) : Icon() {
        constructor(external: ExternalFile) : this(type = "external", external = external)
    }

    /**
     * Icon uploaded manually via Notion UI (has expiring URL).
     */
    @Serializable
    data class File(
        @SerialName("type")
        override val type: String,
        @SerialName("file")
        val file: NotionFile,
    ) : Icon() {
        constructor(file: NotionFile) : this(type = "file", file = file)
    }

    /**
     * Icon uploaded via the Notion File Upload API.
     */
    @Serializable
    data class FileUpload(
        @SerialName("type")
        override val type: String,
        @SerialName("file_upload")
        val fileUpload: FileUploadReference,
    ) : Icon() {
        constructor(fileUpload: FileUploadReference) : this(type = "file_upload", fileUpload = fileUpload)
    }

    /**
     * A local file recorded by `icon.upload(File(…))`, still waiting to be uploaded.
     *
     * The client uploads it and swaps in the equivalent [FileUpload] before the request that
     * carries it is serialized — see `docs/adr/0001-deferred-file-upload-resolution.md`.
     * Serializing one yourself throws; see [IconPendingUploadSerializer].
     *
     * @property source The bytes to upload
     * @property options Upload options — content type override, progress callback, validation
     */
    @Serializable(with = IconPendingUploadSerializer::class)
    @SerialName("pending_upload")
    data class PendingUpload(
        val source: FileSource,
        val options: FileUploadOptions = FileUploadOptions(),
    ) : Icon() {
        override val type: String = "pending_upload"
    }

    /**
     * Write-only sentinel that removes the icon.
     *
     * Notion removes an icon when the request carries `"icon": null` — see
     * `reference/notion-api/documentation/endpoints/Update_Page_2025.md`. A Kotlin `null` cannot
     * express that: the client encodes with `explicitNulls = false`, so a null field is dropped
     * and the PATCH says nothing about the icon at all. This sentinel is a non-null value whose
     * serializer writes the `null` explicitly. See `docs/adr/0002-explicit-null-payloads.md`.
     *
     * Set it with `icon.remove()`; it is never produced by decoding, since a removed icon reads
     * back as `null`.
     */
    @Serializable(with = IconRemovedSerializer::class)
    data object Removed : Icon() {
        override val type: String = "removed"
    }

    /**
     * A native Notion icon (built-in icon library with optional color).
     *
     * Valid colors: "gray" (default), "lightgray", "brown", "yellow", "orange",
     * "green", "blue", "purple", "pink", "red".
     */
    @Serializable
    data class NativeIcon(
        @SerialName("type")
        override val type: String,
        @SerialName("icon")
        val icon: NativeIconObject,
    ) : Icon() {
        constructor(icon: NativeIconObject) : this(type = "icon", icon = icon)
    }
}

/**
 * Serializer for [Icon.PendingUpload] that refuses to serialize. See
 * [PendingUploadRefusingSerializer].
 */
internal object IconPendingUploadSerializer : PendingUploadRefusingSerializer<Icon.PendingUpload>(
    serialName = "pending_upload",
    filename = { it.source.filename },
    remedy =
        "pass the request through a NotionClient method (pages.create, pages.update, " +
            "databases.create, dataSources.update), or upload first and use icon.upload(id)",
)

/**
 * Serializer for [Icon.Removed] that emits JSON `null`. See [RemovalSentinelSerializer].
 */
internal object IconRemovedSerializer : RemovalSentinelSerializer<Icon.Removed>(serialName = "icon_removed")
