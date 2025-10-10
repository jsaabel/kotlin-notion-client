@file:Suppress("unused")

package it.saabel.kotlinnotionclient.utils

import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

/**
 * Utilities for file upload operations.
 */
object FileUploadUtils {
    /**
     * Maximum file size supported by Notion API (500 MB).
     */
    const val MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024L

    /**
     * Threshold for switching to multi-part upload (20 MB).
     */
    const val MULTI_PART_THRESHOLD_BYTES = 20 * 1024 * 1024L

    /**
     * Default chunk size for multi-part uploads (5 MB).
     */
    const val DEFAULT_CHUNK_SIZE_BYTES = 5 * 1024 * 1024L

    /**
     * Maximum number of parts allowed by Notion API.
     */
    const val MAX_PARTS = 1000

    /**
     * Detects content type from file extension.
     */
    fun detectContentType(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return when (extension) {
            // Images
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"

            // Videos
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"

            // Audio
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"

            // Documents
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "odt" -> "application/vnd.oasis.opendocument.text"
            "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
            "odp" -> "application/vnd.oasis.opendocument.presentation"

            // Text
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "html", "htm" -> "text/html"
            "xml" -> "text/xml"
            "css" -> "text/css"
            "js" -> "text/javascript"
            "json" -> "application/json"
            "md", "markdown" -> "text/markdown"
            "rtf" -> "application/rtf"

            // Archives
            "zip" -> "application/zip"
            "rar" -> "application/vnd.rar"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"

            // Code
            "java" -> "text/x-java-source"
            "kt" -> "text/x-kotlin"
            "py" -> "text/x-python"
            "cpp", "cc", "cxx" -> "text/x-c++"
            "c" -> "text/x-c"
            "h" -> "text/x-c-header"
            "php" -> "text/x-php"
            "rb" -> "text/x-ruby"
            "go" -> "text/x-go"
            "rs" -> "text/x-rust"
            "swift" -> "text/x-swift"

            // Default
            else -> "application/octet-stream"
        }
    }

    /**
     * Validates file size against Notion limits.
     */
    fun validateFileSize(sizeBytes: Long): ValidationResult =
        when {
            sizeBytes <= 0 -> ValidationResult.Invalid("File size must be greater than 0")
            sizeBytes > MAX_FILE_SIZE_BYTES -> {
                val maxSizeMB = MAX_FILE_SIZE_BYTES / (1024 * 1024)
                ValidationResult.Invalid("File size exceeds maximum limit of ${maxSizeMB}MB")
            }
            else -> ValidationResult.Valid
        }

    /**
     * Determines if multi-part upload should be used based on file size.
     */
    fun shouldUseMultiPart(sizeBytes: Long): Boolean = sizeBytes > MULTI_PART_THRESHOLD_BYTES

    /**
     * Calculates optimal chunk size and number of parts for multi-part upload.
     */
    fun calculateChunking(
        fileSizeBytes: Long,
        maxChunkSize: Long = DEFAULT_CHUNK_SIZE_BYTES,
    ): ChunkingStrategy {
        require(fileSizeBytes > 0) { "File size must be positive" }
        require(maxChunkSize > 0) { "Chunk size must be positive" }

        // Calculate number of parts needed with the max chunk size
        val partsNeeded = ((fileSizeBytes + maxChunkSize - 1) / maxChunkSize).toInt()

        return if (partsNeeded <= MAX_PARTS) {
            // We can use the preferred chunk size
            ChunkingStrategy(
                chunkSize = maxChunkSize,
                numberOfParts = partsNeeded,
                lastChunkSize = if (fileSizeBytes % maxChunkSize == 0L) maxChunkSize else fileSizeBytes % maxChunkSize,
            )
        } else {
            // We need to use larger chunks to stay within part limit
            val requiredChunkSize = (fileSizeBytes + MAX_PARTS - 1) / MAX_PARTS
            ChunkingStrategy(
                chunkSize = requiredChunkSize,
                numberOfParts = MAX_PARTS,
                lastChunkSize = if (fileSizeBytes % requiredChunkSize == 0L) requiredChunkSize else fileSizeBytes % requiredChunkSize,
            )
        }
    }

    /**
     * Validates filename according to Notion requirements.
     */
    fun validateFilename(filename: String): ValidationResult =
        when {
            filename.isBlank() -> ValidationResult.Invalid("Filename cannot be empty")
            filename.length > 255 -> ValidationResult.Invalid("Filename too long (max 255 characters)")
            filename.contains('\\') || filename.contains('/') -> ValidationResult.Invalid("Filename cannot contain path separators")
            filename.startsWith('.') -> ValidationResult.Invalid("Filename cannot start with a dot")
            else -> ValidationResult.Valid
        }

    /**
     * Validates external URL for file import.
     */
    fun validateExternalUrl(url: String): ValidationResult =
        when {
            url.isBlank() -> ValidationResult.Invalid("URL cannot be empty")
            !url.startsWith("https://") -> ValidationResult.Invalid("URL must use HTTPS protocol")
            url.length > 2048 -> ValidationResult.Invalid("URL too long (max 2048 characters)")
            else -> ValidationResult.Valid
        }

    /**
     * Ensures filename has an extension, inferring from content type if needed.
     */
    fun ensureFileExtension(
        filename: String,
        contentType: String,
    ): String {
        if (filename.contains('.')) {
            return filename
        }

        val extension =
            when (contentType.lowercase()) {
                "image/jpeg" -> ".jpg"
                "image/png" -> ".png"
                "image/gif" -> ".gif"
                "image/webp" -> ".webp"
                "image/bmp" -> ".bmp"
                "image/svg+xml" -> ".svg"
                "video/mp4" -> ".mp4"
                "video/quicktime" -> ".mov"
                "video/webm" -> ".webm"
                "audio/mpeg" -> ".mp3"
                "audio/wav" -> ".wav"
                "audio/ogg" -> ".ogg"
                "application/pdf" -> ".pdf"
                "application/json" -> ".json"
                "text/plain" -> ".txt"
                "text/csv" -> ".csv"
                "text/html" -> ".html"
                "text/markdown" -> ".md"
                "application/zip" -> ".zip"
                else -> ".bin"
            }

        return "$filename$extension"
    }
}

/**
 * Result of a validation operation.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()

    data class Invalid(
        val reason: String,
    ) : ValidationResult()

    val isValid: Boolean get() = this is Valid
    val isInvalid: Boolean get() = this is Invalid
}

/**
 * Strategy for chunking a file for multi-part upload.
 */
data class ChunkingStrategy(
    val chunkSize: Long,
    val numberOfParts: Int,
    val lastChunkSize: Long,
) {
    init {
        require(chunkSize > 0) { "Chunk size must be positive" }
        require(numberOfParts > 0) { "Number of parts must be positive" }
        require(numberOfParts <= FileUploadUtils.MAX_PARTS) { "Too many parts (max ${FileUploadUtils.MAX_PARTS})" }
        require(lastChunkSize > 0) { "Last chunk size must be positive" }
        require(lastChunkSize <= chunkSize) { "Last chunk size cannot exceed regular chunk size" }
    }
}

/**
 * File source abstraction for different input types.
 */
sealed class FileSource {
    abstract val filename: String
    abstract val sizeBytes: Long

    abstract fun openStream(): InputStream

    data class FromFile(
        private val file: File,
    ) : FileSource() {
        override val filename: String = file.name
        override val sizeBytes: Long = file.length()

        override fun openStream(): InputStream = file.inputStream()
    }

    data class FromPath(
        private val path: Path,
    ) : FileSource() {
        override val filename: String = path.name
        override val sizeBytes: Long = path.toFile().length()

        override fun openStream(): InputStream = path.toFile().inputStream()
    }

    data class FromByteArray(
        override val filename: String,
        private val data: ByteArray,
    ) : FileSource() {
        override val sizeBytes: Long = data.size.toLong()

        override fun openStream(): InputStream = data.inputStream()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FromByteArray

            if (filename != other.filename) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = filename.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    data class FromInputStream(
        override val filename: String,
        override val sizeBytes: Long,
        private val streamProvider: () -> InputStream,
    ) : FileSource() {
        override fun openStream(): InputStream = streamProvider()
    }
}
