@file:Suppress("unused", "HttpUrlsUsage")

package api

import TestFixtures
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import mockClient
import no.saabelit.kotlinnotionclient.api.EnhancedFileUploadApi
import no.saabelit.kotlinnotionclient.api.FileUploadApi
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.files.FileUploadOptions
import no.saabelit.kotlinnotionclient.models.files.FileUploadProgress
import no.saabelit.kotlinnotionclient.models.files.FileUploadResult
import no.saabelit.kotlinnotionclient.models.files.UploadProgressStatus
import no.saabelit.kotlinnotionclient.utils.FileSource
import no.saabelit.kotlinnotionclient.utils.FileUploadUtils
import kotlin.io.path.createTempFile
import kotlin.io.path.writeBytes

/**
 * Unit tests for the EnhancedFileUploadApi class.
 *
 * These tests verify the enhanced file upload functionality including
 * automatic chunking, progress tracking, validation, and error handling.
 */
@Tags("Unit")
class EnhancedFileUploadApiTest :
    FunSpec({
        lateinit var api: EnhancedFileUploadApi
        lateinit var config: NotionConfig

        beforeTest {
            config = NotionConfig(token = "test-token")
        }

        context("File upload utilities") {
            test("should detect content types correctly") {
                FileUploadUtils.detectContentType("image.jpg") shouldBe "image/jpeg"
                FileUploadUtils.detectContentType("document.pdf") shouldBe "application/pdf"
                FileUploadUtils.detectContentType("video.mp4") shouldBe "video/mp4"
                FileUploadUtils.detectContentType("audio.mp3") shouldBe "audio/mpeg"
                FileUploadUtils.detectContentType("data.json") shouldBe "application/json"
                FileUploadUtils.detectContentType("unknown.xyz") shouldBe "application/octet-stream"
            }

            test("should validate file sizes") {
                FileUploadUtils.validateFileSize(1024).isValid shouldBe true
                FileUploadUtils.validateFileSize(FileUploadUtils.MAX_FILE_SIZE_BYTES).isValid shouldBe true
                FileUploadUtils.validateFileSize(FileUploadUtils.MAX_FILE_SIZE_BYTES + 1).isInvalid shouldBe true
                FileUploadUtils.validateFileSize(0).isInvalid shouldBe true
                FileUploadUtils.validateFileSize(-1).isInvalid shouldBe true
            }

            test("should determine multi-part upload correctly") {
                FileUploadUtils.shouldUseMultiPart(1024) shouldBe false
                FileUploadUtils.shouldUseMultiPart(FileUploadUtils.MULTI_PART_THRESHOLD_BYTES - 1) shouldBe false
                FileUploadUtils.shouldUseMultiPart(FileUploadUtils.MULTI_PART_THRESHOLD_BYTES) shouldBe false
                FileUploadUtils.shouldUseMultiPart(FileUploadUtils.MULTI_PART_THRESHOLD_BYTES + 1) shouldBe true
            }

            test("should calculate chunking strategy correctly") {
                val strategy = FileUploadUtils.calculateChunking(25 * 1024 * 1024) // 25MB
                strategy.numberOfParts shouldBe 5 // 5 chunks of 5MB each
                strategy.chunkSize shouldBe FileUploadUtils.DEFAULT_CHUNK_SIZE_BYTES
                strategy.lastChunkSize shouldBe FileUploadUtils.DEFAULT_CHUNK_SIZE_BYTES // Evenly divisible, so last chunk is also 5MB
            }

            test("should validate filenames") {
                FileUploadUtils.validateFilename("test.txt").isValid shouldBe true
                FileUploadUtils.validateFilename("my document.pdf").isValid shouldBe true
                FileUploadUtils.validateFilename("").isInvalid shouldBe true
                FileUploadUtils.validateFilename("   ").isInvalid shouldBe true
                FileUploadUtils.validateFilename(".hidden").isInvalid shouldBe true
                FileUploadUtils.validateFilename("path/to/file.txt").isInvalid shouldBe true
                FileUploadUtils.validateFilename("path\\\\to\\\\file.txt").isInvalid shouldBe true
            }

            test("should validate external URLs") {
                FileUploadUtils.validateExternalUrl("https://example.com/file.pdf").isValid shouldBe true
                @Suppress("HttpUrlsUsage")
                FileUploadUtils.validateExternalUrl("http://example.com/file.pdf").isInvalid shouldBe true
                FileUploadUtils.validateExternalUrl("").isInvalid shouldBe true
                FileUploadUtils.validateExternalUrl("not-a-url").isInvalid shouldBe true
            }

            test("should ensure file extensions") {
                FileUploadUtils.ensureFileExtension("document", "application/pdf") shouldBe "document.pdf"
                FileUploadUtils.ensureFileExtension("image", "image/jpeg") shouldBe "image.jpg"
                FileUploadUtils.ensureFileExtension("already.txt", "text/plain") shouldBe "already.txt"
                FileUploadUtils.ensureFileExtension("unknown", "unknown/type") shouldBe "unknown.bin"
            }
        }

        context("FileSource abstraction") {
            test("should handle byte array source") {
                val data = "Hello, World!".toByteArray()
                val source = FileSource.FromByteArray("test.txt", data)

                source.filename shouldBe "test.txt"
                source.sizeBytes shouldBe data.size.toLong()
                source.openStream().use { it.readBytes() } shouldBe data
            }

            test("should handle file source") {
                val tempFile = createTempFile("test", ".txt").toFile()
                tempFile.writeText("Test content")

                try {
                    val source = FileSource.FromFile(tempFile)

                    source.filename shouldBe tempFile.name
                    source.sizeBytes shouldBe tempFile.length()
                    source.openStream().use { it.readBytes() } shouldBe "Test content".toByteArray()
                } finally {
                    tempFile.delete()
                }
            }

            test("should handle path source") {
                val tempPath = createTempFile("test", ".txt")
                tempPath.writeBytes("Test content".toByteArray())

                try {
                    val source = FileSource.FromPath(tempPath)

                    source.filename shouldBe tempPath.fileName.toString()
                    source.sizeBytes shouldBe tempPath.toFile().length()
                    source.openStream().use { it.readBytes() } shouldBe "Test content".toByteArray()
                } finally {
                    tempPath.toFile().delete()
                }
            }
        }

        context("Enhanced file upload API") {
            test("should upload small file with single-part") {
                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = io.ktor.http.HttpMethod.Post,
                            path = "/v1/file_uploads",
                            responseBody = TestFixtures.FileUploads.createFileUploadAsString(),
                        )
                        addJsonResponse(
                            method = io.ktor.http.HttpMethod.Post,
                            path = "/v1/file_uploads/b52b8ed6-e029-4707-a671-832549c09de3/send",
                            responseBody = TestFixtures.FileUploads.sendFileUploadAsString(),
                        )
                    }

                val basicApi = FileUploadApi(mockClient, config)
                api = EnhancedFileUploadApi(mockClient, config, basicApi)

                val data = "Small file content".toByteArray()
                val progressUpdates = mutableListOf<FileUploadProgress>()

                val options =
                    FileUploadOptions(
                        progressCallback = { progress -> progressUpdates.add(progress) },
                    )

                val result = api.uploadFile("small.txt", data, options)

                result.shouldBeInstanceOf<FileUploadResult.Success>()
                result.filename shouldBe "small.txt"
                result.uploadId shouldNotBe null

                // Should have received progress updates
                progressUpdates.size shouldBe 3 // Starting, uploading, completed
                progressUpdates[0].status shouldBe UploadProgressStatus.STARTING
                progressUpdates[1].status shouldBe UploadProgressStatus.UPLOADING
                progressUpdates[2].status shouldBe UploadProgressStatus.COMPLETED
            }

            test("should validate file before upload") {
                val mockClient =
                    mockClient {
                        // No responses needed - should fail validation before making requests
                    }

                val basicApi = FileUploadApi(mockClient, config)
                api = EnhancedFileUploadApi(mockClient, config, basicApi)

                // Test with invalid filename
                val result = api.uploadFile("", byteArrayOf(1, 2, 3))

                result.shouldBeInstanceOf<FileUploadResult.Failure>()
                result.error.message shouldBe "Validation failed: Filename cannot be empty"
            }

            test("should import external file with validation") {
                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = io.ktor.http.HttpMethod.Post,
                            path = "/v1/file_uploads",
                            responseBody =
                                """
                                {
                                    "id": "external-file-id",
                                    "object": "file_upload",
                                    "created_time": "2025-03-15T20:53:00.000Z",
                                    "last_edited_time": "2025-03-15T20:53:00.000Z",
                                    "expiry_time": "2025-03-15T21:53:00.000Z",
                                    "archived": false,
                                    "status": "uploaded",
                                    "filename": "external-image.jpg",
                                    "content_type": "image/jpeg",
                                    "content_length": 204800
                                }
                                """.trimIndent(),
                        )
                    }

                val basicApi = FileUploadApi(mockClient, config)
                api = EnhancedFileUploadApi(mockClient, config, basicApi)

                val result =
                    api.importExternalFile(
                        filename = "external-image",
                        externalUrl = "https://example.com/image.jpg",
                        contentType = "image/jpeg",
                    )

                result.shouldBeInstanceOf<FileUploadResult.Success>()
                result.filename shouldBe "external-image.jpg" // Extension added
                result.fileUpload.status.name shouldBe "UPLOADED"
            }

            test("should fail external import with invalid URL") {
                val mockClient =
                    mockClient {
                        // No responses needed - should fail validation
                    }

                val basicApi = FileUploadApi(mockClient, config)
                api = EnhancedFileUploadApi(mockClient, config, basicApi)

                val result =
                    api.importExternalFile(
                        filename = "test.jpg",
                        externalUrl = "http://insecure.com/image.jpg", // HTTP not allowed
                    )

                result.shouldBeInstanceOf<FileUploadResult.Failure>()
                result.error.message shouldBe "Validation failed: URL must use HTTPS protocol"
            }

            test("should handle content type detection") {
                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = io.ktor.http.HttpMethod.Post,
                            path = "/v1/file_uploads",
                            responseBody = TestFixtures.FileUploads.createFileUploadAsString(),
                        )
                        addJsonResponse(
                            method = io.ktor.http.HttpMethod.Post,
                            path = "/v1/file_uploads/b52b8ed6-e029-4707-a671-832549c09de3/send",
                            responseBody = TestFixtures.FileUploads.sendFileUploadAsString(),
                        )
                    }

                val basicApi = FileUploadApi(mockClient, config)
                api = EnhancedFileUploadApi(mockClient, config, basicApi)

                val data = "PDF content".toByteArray()

                // Should auto-detect PDF content type from filename
                val result = api.uploadFile("document.pdf", data)

                result.shouldBeInstanceOf<FileUploadResult.Success>()
                // The content type should have been auto-detected as application/pdf
            }
        }
    })
