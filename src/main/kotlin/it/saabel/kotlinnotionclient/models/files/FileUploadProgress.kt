@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.files

/**
 * Progress tracking for file uploads.
 */
data class FileUploadProgress(
    val uploadId: String,
    val filename: String,
    val totalBytes: Long,
    val uploadedBytes: Long,
    val currentPart: Int? = null,
    val totalParts: Int? = null,
    val status: UploadProgressStatus,
    val error: Throwable? = null,
) {
    /**
     * Progress as a percentage (0.0 to 100.0).
     */
    val progressPercent: Double =
        if (totalBytes > 0) {
            (uploadedBytes.toDouble() / totalBytes.toDouble()) * 100.0
        } else {
            0.0
        }

    /**
     * Whether the upload is completed successfully.
     */
    val isCompleted: Boolean = status == UploadProgressStatus.COMPLETED

    /**
     * Whether the upload has failed.
     */
    val isFailed: Boolean = status == UploadProgressStatus.FAILED

    /**
     * Whether the upload is currently in progress.
     */
    val isInProgress: Boolean =
        status in
            setOf(
                UploadProgressStatus.STARTING,
                UploadProgressStatus.UPLOADING,
                UploadProgressStatus.COMPLETING,
            )
}

/**
 * Status of an upload operation.
 */
enum class UploadProgressStatus {
    STARTING,
    UPLOADING,
    COMPLETING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

/**
 * Callback interface for upload progress updates.
 */
fun interface UploadProgressCallback {
    fun onProgress(progress: FileUploadProgress)
}

/**
 * Result of a file upload operation.
 */
sealed class FileUploadResult {
    abstract val uploadId: String
    abstract val filename: String

    data class Success(
        override val uploadId: String,
        override val filename: String,
        val fileUpload: FileUpload,
        val uploadTimeMs: Long,
    ) : FileUploadResult()

    data class Failure(
        override val uploadId: String,
        override val filename: String,
        val error: FileUploadError,
        val partialProgress: FileUploadProgress? = null,
    ) : FileUploadResult()
}

/**
 * Specific errors that can occur during file upload.
 */
sealed class FileUploadError : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    data class ValidationError(
        val reason: String,
    ) : FileUploadError("Validation failed: $reason")

    data class NetworkError(
        val httpStatus: Int?,
        val originalError: Throwable,
    ) : FileUploadError("Network error${httpStatus?.let { " (HTTP $it)" } ?: ""}", originalError)

    data class RateLimitError(
        val retryAfterSeconds: Int?,
    ) : FileUploadError("Rate limited${retryAfterSeconds?.let { " (retry after ${it}s)" } ?: ""}")

    data class ServerError(
        val httpStatus: Int,
        val errorMessage: String?,
    ) : FileUploadError("Server error (HTTP $httpStatus)${errorMessage?.let { ": $it" } ?: ""}")

    data class PartUploadError(
        val partNumber: Int,
        val originalError: Throwable,
    ) : FileUploadError("Failed to upload part $partNumber", originalError)

    data class TimeoutError(
        val operationType: String,
    ) : FileUploadError("Timeout during $operationType")

    data class FileSizeError(
        val fileSize: Long,
        val maxSize: Long,
    ) : FileUploadError("File size $fileSize exceeds maximum $maxSize bytes")

    data class CancellationError(
        val reason: String,
    ) : FileUploadError("Upload cancelled: $reason")

    data class UnknownError(
        val originalError: Throwable,
    ) : FileUploadError("Unknown upload error", originalError)
}

/**
 * Options for file upload operations.
 *
 * Retry of failed HTTP/network requests is handled centrally by the rate-limit
 * pipeline plugin and configured via `NotionConfig.rateLimitConfig`; there is no
 * per-upload retry configuration.
 */
data class FileUploadOptions(
    val contentType: String? = null,
    val progressCallback: UploadProgressCallback? = null,
    val timeoutMs: Long = 300_000, // 5 minutes default
    val enableConcurrentParts: Boolean = true,
    val maxConcurrentParts: Int = 4,
    val validateBeforeUpload: Boolean = true,
)
