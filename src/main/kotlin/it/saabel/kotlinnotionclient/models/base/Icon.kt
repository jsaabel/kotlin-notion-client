package it.saabel.kotlinnotionclient.models.base

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
}
