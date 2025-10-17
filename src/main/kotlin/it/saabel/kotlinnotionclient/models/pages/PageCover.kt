package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.FileUploadReference
import it.saabel.kotlinnotionclient.models.base.NotionFile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a cover image for a page (API version 2025-09-03+).
 *
 * Page covers can be external files, Notion-hosted files, or files uploaded via the API.
 * This sealed class provides type-safe access to cover information with compile-time guarantees.
 *
 * ## Example usage:
 * ```kotlin
 * when (page.cover) {
 *     is PageCover.External -> println("External URL: ${page.cover.external.url}")
 *     is PageCover.File -> println("File URL: ${page.cover.file.url}")
 *     is PageCover.FileUpload -> println("Upload ID: ${page.cover.fileUpload.id}")
 *     null -> println("No cover")
 * }
 * ```
 */
@Serializable(with = PageCoverSerializer::class)
sealed class PageCover {
    abstract val type: String

    /**
     * Cover hosted externally via a public URL.
     */
    @Serializable
    data class External(
        @SerialName("external")
        val external: ExternalFile,
    ) : PageCover() {
        override val type: String = "external"
    }

    /**
     * Cover uploaded manually via Notion UI (has expiring URL).
     */
    @Serializable
    data class File(
        @SerialName("file")
        val file: NotionFile,
    ) : PageCover() {
        override val type: String = "file"
    }

    /**
     * Cover uploaded via the Notion File Upload API.
     */
    @Serializable
    data class FileUpload(
        @SerialName("file_upload")
        val fileUpload: FileUploadReference,
    ) : PageCover() {
        override val type: String = "file_upload"
    }
}
