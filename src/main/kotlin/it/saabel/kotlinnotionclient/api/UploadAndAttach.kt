package it.saabel.kotlinnotionclient.api

import it.saabel.kotlinnotionclient.models.files.FileUpload
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus
import it.saabel.kotlinnotionclient.models.files.getOrThrow
import it.saabel.kotlinnotionclient.utils.FileSource

/**
 * Uploads [source] and returns it only once Notion reports it as
 * [uploaded][FileUploadStatus.UPLOADED].
 *
 * This is the shared first half of every one-call upload-and-attach helper on
 * [BlocksApi], [PagesApi] and [CommentsApi]: it collapses the create → send → wait
 * dance into one step and converts [FileUploadResult.Failure][
 * it.saabel.kotlinnotionclient.models.files.FileUploadResult.Failure] into a thrown
 * [FileUploadError][it.saabel.kotlinnotionclient.models.files.FileUploadError], so the attach
 * half can assume a usable upload and callers see the same throwing contract as the rest of
 * the client.
 *
 * A single-part upload is already `uploaded` when `sendFileUpload` returns, so the wait is
 * skipped in the common case and only costs a poll for uploads that are still processing.
 */
internal suspend fun EnhancedFileUploadApi.uploadAndAwait(
    source: FileSource,
    options: FileUploadOptions = FileUploadOptions(),
    maxWaitTimeMs: Long = 30_000,
): FileUpload {
    val upload = uploadFile(source, options).getOrThrow()
    return if (upload.status == FileUploadStatus.UPLOADED) {
        upload
    } else {
        waitForFileReady(upload.id, maxWaitTimeMs = maxWaitTimeMs)
    }
}
