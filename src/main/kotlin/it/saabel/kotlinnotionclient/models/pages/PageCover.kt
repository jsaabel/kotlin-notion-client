package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.FileUploadReference
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.files.PendingUploadRefusingSerializer
import it.saabel.kotlinnotionclient.serialization.RemovalSentinelSerializer
import it.saabel.kotlinnotionclient.utils.FileSource
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
 *     PageCover.Removed -> println("Cover removal (requests only)")
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

    /**
     * Write-only sentinel that removes the cover.
     *
     * Notion removes a cover when the request carries `"cover": null` — see
     * `reference/notion-api/documentation/endpoints/Update_Page_2025.md`. A Kotlin `null` cannot
     * express that: the client encodes with `explicitNulls = false`, so a null field is dropped
     * and the PATCH says nothing about the cover at all. This sentinel is a non-null value whose
     * serializer writes the `null` explicitly. See `docs/adr/0002-explicit-null-payloads.md`.
     *
     * Set it with `cover.remove()`; it is never produced by decoding, since a removed cover reads
     * back as `null`.
     */
    @Serializable(with = PageCoverRemovedSerializer::class)
    data object Removed : PageCover() {
        override val type: String = "removed"
    }

    /**
     * A local file recorded by `cover { upload(File(…)) }`, still waiting to be uploaded.
     *
     * The client uploads it and swaps in the equivalent [FileUpload] before the request that
     * carries it is serialized — see `docs/adr/0001-deferred-file-upload-resolution.md`.
     * Serializing one yourself throws; see [PageCoverPendingUploadSerializer].
     *
     * @property source The bytes to upload
     * @property options Upload options — content type override, progress callback, validation
     */
    @Serializable(with = PageCoverPendingUploadSerializer::class)
    data class PendingUpload(
        val source: FileSource,
        val options: FileUploadOptions = FileUploadOptions(),
    ) : PageCover() {
        override val type: String = "pending_upload"
    }
}

/**
 * Serializer for [PageCover.PendingUpload] that refuses to serialize. See
 * [PendingUploadRefusingSerializer].
 */
internal object PageCoverPendingUploadSerializer : PendingUploadRefusingSerializer<PageCover.PendingUpload>(
    serialName = "pending_upload",
    filename = { it.source.filename },
    remedy =
        "pass the request through a NotionClient method (pages.create, pages.update, " +
            "databases.create), or upload first and use cover { upload(id) }",
)

/**
 * Serializer for [PageCover.Removed] that emits JSON `null`. See [RemovalSentinelSerializer].
 */
internal object PageCoverRemovedSerializer : RemovalSentinelSerializer<PageCover.Removed>(serialName = "cover_removed")
