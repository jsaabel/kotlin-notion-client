@file:Suppress("UastIncorrectHttpHeaderInspection", "unused")

package no.saabelit.kotlinnotionclient.api

import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.files.CreateFileUploadRequest
import no.saabelit.kotlinnotionclient.models.files.FileUpload
import no.saabelit.kotlinnotionclient.models.files.FileUploadError
import no.saabelit.kotlinnotionclient.models.files.FileUploadList
import no.saabelit.kotlinnotionclient.models.files.FileUploadMode
import no.saabelit.kotlinnotionclient.models.files.FileUploadOptions
import no.saabelit.kotlinnotionclient.models.files.FileUploadProgress
import no.saabelit.kotlinnotionclient.models.files.FileUploadResult
import no.saabelit.kotlinnotionclient.models.files.FileUploadStatus
import no.saabelit.kotlinnotionclient.models.files.UploadProgressStatus
import no.saabelit.kotlinnotionclient.utils.ChunkingStrategy
import no.saabelit.kotlinnotionclient.utils.FileSource
import no.saabelit.kotlinnotionclient.utils.FileUploadUtils
import no.saabelit.kotlinnotionclient.utils.ValidationResult
import java.io.File
import java.io.InputStream
import java.nio.file.Path

/**
 * Enhanced file upload API with advanced features like progress tracking,
 * automatic chunking, retry logic, and comprehensive error handling.
 *
 * This class extends the basic FileUploadApi with production-ready features
 * for robust file upload operations.
 */
class EnhancedFileUploadApi(
    private val client: HttpClient,
    private val config: NotionConfig,
    private val basicApi: FileUploadApi = FileUploadApi(client, config),
) {
    /**
     * Uploads a file from various sources with enhanced features.
     *
     * Automatically determines whether to use single-part or multi-part upload
     * based on file size, provides progress tracking, and handles retries.
     */
    suspend fun uploadFile(
        source: FileSource,
        options: FileUploadOptions = FileUploadOptions(),
    ): FileUploadResult {
        val startTime = System.currentTimeMillis()

        try {
            // Validate inputs
            if (options.validateBeforeUpload) {
                validateFileSource(source)?.let { error ->
                    return FileUploadResult.Failure(
                        uploadId = "validation-failed",
                        filename = source.filename,
                        error = error,
                    )
                }
            }

            // Determine content type
            val contentType =
                options.contentType
                    ?: FileUploadUtils.detectContentType(source.filename)

            // Determine upload strategy
            val useMultiPart = FileUploadUtils.shouldUseMultiPart(source.sizeBytes)

            return if (useMultiPart) {
                uploadFileMultiPart(source, contentType, options, startTime)
            } else {
                uploadFileSinglePart(source, contentType, options, startTime)
            }
        } catch (e: Exception) {
            val error =
                when (e) {
                    is FileUploadError -> e
                    else -> FileUploadError.UnknownError(e)
                }

            return FileUploadResult.Failure(
                uploadId = "unknown",
                filename = source.filename,
                error = error,
            )
        }
    }

    /**
     * Uploads a file from a File object.
     */
    suspend fun uploadFile(
        file: File,
        options: FileUploadOptions = FileUploadOptions(),
    ): FileUploadResult = uploadFile(FileSource.FromFile(file), options)

    /**
     * Uploads a file from a Path object.
     */
    suspend fun uploadFile(
        path: Path,
        options: FileUploadOptions = FileUploadOptions(),
    ): FileUploadResult = uploadFile(FileSource.FromPath(path), options)

    /**
     * Uploads a file from a byte array.
     */
    suspend fun uploadFile(
        filename: String,
        data: ByteArray,
        options: FileUploadOptions = FileUploadOptions(),
    ): FileUploadResult = uploadFile(FileSource.FromByteArray(filename, data), options)

    /**
     * Uploads a file from an InputStream.
     */
    suspend fun uploadFile(
        filename: String,
        sizeBytes: Long,
        streamProvider: () -> InputStream,
        options: FileUploadOptions = FileUploadOptions(),
    ): FileUploadResult =
        uploadFile(
            FileSource.FromInputStream(filename, sizeBytes, streamProvider),
            options,
        )

    /**
     * Imports a file from an external URL with validation.
     */
    suspend fun importExternalFile(
        filename: String,
        externalUrl: String,
        contentType: String? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): FileUploadResult {
        val startTime = System.currentTimeMillis()

        try {
            // Validate inputs
            if (options.validateBeforeUpload) {
                FileUploadUtils.validateFilename(filename).let { result ->
                    if (result is ValidationResult.Invalid) {
                        return FileUploadResult.Failure(
                            uploadId = "validation-failed",
                            filename = filename,
                            error = FileUploadError.ValidationError(result.reason),
                        )
                    }
                }

                FileUploadUtils.validateExternalUrl(externalUrl).let { result ->
                    if (result is ValidationResult.Invalid) {
                        return FileUploadResult.Failure(
                            uploadId = "validation-failed",
                            filename = filename,
                            error = FileUploadError.ValidationError(result.reason),
                        )
                    }
                }
            }

            // Ensure filename has extension
            val finalFilename =
                contentType?.let {
                    FileUploadUtils.ensureFileExtension(filename, it)
                } ?: filename

            // Detect content type if not provided
            val finalContentType =
                contentType
                    ?: FileUploadUtils.detectContentType(finalFilename)

            // Report progress
            reportProgress(
                options,
                "external-import",
                finalFilename,
                0,
                0,
                status = UploadProgressStatus.STARTING,
            )

            // Create external URL upload
            val request =
                CreateFileUploadRequest(
                    mode = FileUploadMode.EXTERNAL_URL,
                    filename = finalFilename,
                    contentType = finalContentType,
                    externalUrl = externalUrl,
                )

            val fileUpload =
                withRetry(options.retryConfig) {
                    basicApi.createFileUpload(request)
                }

            reportProgress(
                options,
                fileUpload.id,
                finalFilename,
                0,
                0,
                status = UploadProgressStatus.COMPLETED,
            )

            return FileUploadResult.Success(
                uploadId = fileUpload.id,
                filename = finalFilename,
                fileUpload = fileUpload,
                uploadTimeMs = System.currentTimeMillis() - startTime,
            )
        } catch (e: Exception) {
            val error =
                when (e) {
                    is FileUploadError -> e
                    else -> FileUploadError.UnknownError(e)
                }

            return FileUploadResult.Failure(
                uploadId = "external-import-failed",
                filename = filename,
                error = error,
            )
        }
    }

    /**
     * Waits for a file upload to reach the "uploaded" status.
     * This is particularly useful for external file imports which may need time to process.
     *
     * @param fileUploadId The ID of the file upload to wait for
     * @param maxWaitTimeMs Maximum time to wait in milliseconds (default: 10 seconds)
     * @param checkIntervalMs Interval between status checks in milliseconds (default: 500ms)
     * @return The file upload once it reaches "uploaded" status
     * @throws FileUploadError.TimeoutError if the file doesn't become ready within the timeout
     */
    suspend fun waitForFileReady(
        fileUploadId: String,
        maxWaitTimeMs: Long = 10000,
        checkIntervalMs: Long = 500,
    ): FileUpload {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            val fileUpload = basicApi.retrieveFileUpload(fileUploadId)

            if (fileUpload.status == FileUploadStatus.UPLOADED) {
                return fileUpload
            }

            if (fileUpload.status == FileUploadStatus.FAILED) {
                throw FileUploadError.UnknownError(Exception("File upload failed: ${fileUpload.id}"))
            }

            delay(checkIntervalMs)
        }

        throw FileUploadError.TimeoutError("waitForFileReady")
    }

    // Delegate to basic API for standard operations
    suspend fun retrieveFileUpload(fileUploadId: String): FileUpload = basicApi.retrieveFileUpload(fileUploadId)

    suspend fun listFileUploads(
        startCursor: String? = null,
        pageSize: Int? = null,
    ): FileUploadList = basicApi.listFileUploads(startCursor, pageSize)

    // Private implementation methods

    private suspend fun uploadFileSinglePart(
        source: FileSource,
        contentType: String,
        options: FileUploadOptions,
        startTime: Long,
    ): FileUploadResult =
        try {
            // Report starting
            reportProgress(
                options,
                "single-part",
                source.filename,
                source.sizeBytes,
                0,
                status = UploadProgressStatus.STARTING,
            )

            // Create upload
            val request =
                CreateFileUploadRequest(
                    mode = FileUploadMode.SINGLE_PART,
                    filename = source.filename,
                    contentType = contentType,
                )

            val fileUpload =
                withRetry(options.retryConfig) {
                    basicApi.createFileUpload(request)
                }

            // Report upload progress
            reportProgress(
                options,
                fileUpload.id,
                source.filename,
                source.sizeBytes,
                0,
                status = UploadProgressStatus.UPLOADING,
            )

            // Upload content
            val data = source.openStream().use { it.readBytes() }
            val finalUpload =
                withRetry(options.retryConfig) {
                    basicApi.sendFileUpload(fileUpload.id, data)
                }

            // Report completion
            reportProgress(
                options,
                fileUpload.id,
                source.filename,
                source.sizeBytes,
                source.sizeBytes,
                status = UploadProgressStatus.COMPLETED,
            )

            FileUploadResult.Success(
                uploadId = fileUpload.id,
                filename = source.filename,
                fileUpload = finalUpload,
                uploadTimeMs = System.currentTimeMillis() - startTime,
            )
        } catch (e: Exception) {
            val error =
                when (e) {
                    is FileUploadError -> e
                    else -> FileUploadError.UnknownError(e)
                }

            FileUploadResult.Failure(
                uploadId = "single-part-failed",
                filename = source.filename,
                error = error,
            )
        }

    private suspend fun uploadFileMultiPart(
        source: FileSource,
        contentType: String,
        options: FileUploadOptions,
        startTime: Long,
    ): FileUploadResult =
        try {
            // Calculate chunking strategy
            val chunking = FileUploadUtils.calculateChunking(source.sizeBytes)

            // Report starting
            reportProgress(
                options,
                "multi-part",
                source.filename,
                source.sizeBytes,
                0,
                status = UploadProgressStatus.STARTING,
                currentPart = 0,
                totalParts = chunking.numberOfParts,
            )

            // Create multi-part upload
            val request =
                CreateFileUploadRequest(
                    mode = FileUploadMode.MULTI_PART,
                    filename = source.filename,
                    contentType = contentType,
                    numberOfParts = chunking.numberOfParts,
                )

            val fileUpload =
                withRetry(options.retryConfig) {
                    basicApi.createFileUpload(request)
                }

            // Upload parts
            val uploadedBytes =
                uploadParts(
                    source,
                    fileUpload,
                    chunking,
                    options,
                )

            // Report completing
            reportProgress(
                options,
                fileUpload.id,
                source.filename,
                source.sizeBytes,
                uploadedBytes,
                status = UploadProgressStatus.COMPLETING,
                currentPart = chunking.numberOfParts,
                totalParts = chunking.numberOfParts,
            )

            // Complete upload
            val finalUpload =
                withRetry(options.retryConfig) {
                    basicApi.completeFileUpload(fileUpload.id)
                }

            // Report completion
            reportProgress(
                options,
                fileUpload.id,
                source.filename,
                source.sizeBytes,
                source.sizeBytes,
                status = UploadProgressStatus.COMPLETED,
            )

            FileUploadResult.Success(
                uploadId = fileUpload.id,
                filename = source.filename,
                fileUpload = finalUpload,
                uploadTimeMs = System.currentTimeMillis() - startTime,
            )
        } catch (e: Exception) {
            val error =
                when (e) {
                    is FileUploadError -> e
                    else -> FileUploadError.UnknownError(e)
                }

            FileUploadResult.Failure(
                uploadId = "multi-part-failed",
                filename = source.filename,
                error = error,
            )
        }

    private suspend fun uploadParts(
        source: FileSource,
        fileUpload: FileUpload,
        chunking: ChunkingStrategy,
        options: FileUploadOptions,
    ): Long =
        coroutineScope {
            var uploadedBytes = 0L

            source.openStream().use { inputStream ->
                if (options.enableConcurrentParts && chunking.numberOfParts > 1) {
                    // Concurrent upload
                    val buffer = ByteArray(chunking.chunkSize.toInt())
                    val chunks = mutableListOf<Pair<Int, ByteArray>>()

                    // Read all chunks into memory (for concurrent upload)
                    for (partNumber in 1..chunking.numberOfParts) {
                        val chunkSize =
                            if (partNumber == chunking.numberOfParts) {
                                chunking.lastChunkSize.toInt()
                            } else {
                                chunking.chunkSize.toInt()
                            }

                        val chunkData = ByteArray(chunkSize)
                        inputStream.readNBytes(chunkData, 0, chunkSize)
                        chunks.add(partNumber to chunkData)
                    }

                    // Upload chunks concurrently
                    chunks.chunked(options.maxConcurrentParts).forEach { batch ->
                        batch
                            .map { (partNumber, chunkData) ->
                                async {
                                    withRetry(options.retryConfig) {
                                        basicApi.sendFileUpload(fileUpload.id, chunkData, partNumber)

                                        uploadedBytes += chunkData.size
                                        reportProgress(
                                            options,
                                            fileUpload.id,
                                            source.filename,
                                            source.sizeBytes,
                                            uploadedBytes,
                                            status = UploadProgressStatus.UPLOADING,
                                            currentPart = partNumber,
                                            totalParts = chunking.numberOfParts,
                                        )

                                        chunkData.size.toLong()
                                    }
                                }
                            }.awaitAll()
                    }
                } else {
                    // Sequential upload
                    for (partNumber in 1..chunking.numberOfParts) {
                        val chunkSize =
                            if (partNumber == chunking.numberOfParts) {
                                chunking.lastChunkSize.toInt()
                            } else {
                                chunking.chunkSize.toInt()
                            }

                        val chunkData = ByteArray(chunkSize)
                        inputStream.readNBytes(chunkData, 0, chunkSize)

                        withRetry(options.retryConfig) {
                            basicApi.sendFileUpload(fileUpload.id, chunkData, partNumber)
                        }

                        uploadedBytes += chunkSize
                        reportProgress(
                            options,
                            fileUpload.id,
                            source.filename,
                            source.sizeBytes,
                            uploadedBytes,
                            status = UploadProgressStatus.UPLOADING,
                            currentPart = partNumber,
                            totalParts = chunking.numberOfParts,
                        )
                    }
                }
            }

            uploadedBytes
        }

    private fun validateFileSource(source: FileSource): FileUploadError? {
        // Validate filename
        FileUploadUtils.validateFilename(source.filename).let { result ->
            if (result is ValidationResult.Invalid) {
                return FileUploadError.ValidationError(result.reason)
            }
        }

        // Validate file size
        FileUploadUtils.validateFileSize(source.sizeBytes).let { result ->
            if (result is ValidationResult.Invalid) {
                return FileUploadError.ValidationError(result.reason)
            }
        }

        return null
    }

    private fun reportProgress(
        options: FileUploadOptions,
        uploadId: String,
        filename: String,
        totalBytes: Long,
        uploadedBytes: Long,
        status: UploadProgressStatus,
        currentPart: Int? = null,
        totalParts: Int? = null,
        error: Throwable? = null,
    ) {
        options.progressCallback?.onProgress(
            FileUploadProgress(
                uploadId = uploadId,
                filename = filename,
                totalBytes = totalBytes,
                uploadedBytes = uploadedBytes,
                currentPart = currentPart,
                totalParts = totalParts,
                status = status,
                error = error,
            ),
        )
    }

    private suspend fun <T> withRetry(
        retryConfig: no.saabelit.kotlinnotionclient.models.files.RetryConfig,
        operation: suspend () -> T,
    ): T {
        var lastError: Exception? = null

        repeat(retryConfig.maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastError = e

                val uploadError =
                    when (e) {
                        is FileUploadError -> e
                        else -> FileUploadError.UnknownError(e)
                    }

                if (attempt < retryConfig.maxRetries && retryConfig.shouldRetry(uploadError, attempt)) {
                    val delayMs = retryConfig.calculateDelay(attempt)
                    delay(delayMs)
                } else {
                    throw uploadError
                }
            }
        }

        throw lastError!!
    }
}
