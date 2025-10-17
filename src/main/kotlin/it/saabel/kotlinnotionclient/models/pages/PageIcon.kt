package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.CustomEmojiObject
import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.FileUploadReference
import it.saabel.kotlinnotionclient.models.base.NotionFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an icon for a page (API version 2025-09-03+).
 *
 * Page icons can be one of several types: emoji, custom emoji, external files,
 * Notion-hosted files, or files uploaded via the API. This sealed class provides
 * type-safe access to icon information with compile-time guarantees.
 *
 * ## Example usage:
 * ```kotlin
 * when (page.icon) {
 *     is PageIcon.Emoji -> println("Emoji: ${page.icon.emoji}")
 *     is PageIcon.CustomEmoji -> println("Custom emoji: ${page.icon.customEmoji.name}")
 *     is PageIcon.External -> println("External URL: ${page.icon.external.url}")
 *     is PageIcon.File -> println("File URL: ${page.icon.file.url}")
 *     is PageIcon.FileUpload -> println("Upload ID: ${page.icon.fileUpload.id}")
 *     null -> println("No icon")
 * }
 * ```
 */
@Serializable(with = PageIconSerializer::class)
sealed class PageIcon {
    abstract val type: String

    /**
     * Standard emoji icon (e.g., 🥑).
     */
    @Serializable
    data class Emoji(
        @SerialName("emoji")
        val emoji: String,
    ) : PageIcon() {
        override val type: String = "emoji"
    }

    /**
     * Custom emoji uploaded and managed in the workspace.
     */
    @Serializable
    data class CustomEmoji(
        @SerialName("custom_emoji")
        val customEmoji: CustomEmojiObject,
    ) : PageIcon() {
        override val type: String = "custom_emoji"
    }

    /**
     * Icon hosted externally via a public URL.
     */
    @Serializable
    data class External(
        @SerialName("external")
        val external: ExternalFile,
    ) : PageIcon() {
        override val type: String = "external"
    }

    /**
     * Icon uploaded manually via Notion UI (has expiring URL).
     */
    @Serializable
    data class File(
        @SerialName("file")
        val file: NotionFile,
    ) : PageIcon() {
        override val type: String = "file"
    }

    /**
     * Icon uploaded via the Notion File Upload API.
     */
    @Serializable
    data class FileUpload(
        @SerialName("file_upload")
        val fileUpload: FileUploadReference,
    ) : PageIcon() {
        override val type: String = "file_upload"
    }
}
