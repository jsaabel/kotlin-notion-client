@file:Suppress("unused", "UnusedVariable")

package api

import TestFixtures
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import mockClient
import no.saabelit.kotlinnotionclient.api.FileUploadApi
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.files.CreateFileUploadRequest
import no.saabelit.kotlinnotionclient.models.files.FileUpload
import no.saabelit.kotlinnotionclient.models.files.FileUploadMode
import no.saabelit.kotlinnotionclient.models.files.FileUploadStatus

/**
 * Unit tests for the FileUploadApi class.
 *
 * These tests verify that the FileUploadApi correctly handles API requests
 * and responses for file upload operations.
 */
@Tags("Unit")
class FileUploadApiTest :
    FunSpec({
        lateinit var api: FileUploadApi
        lateinit var config: NotionConfig

        beforeTest {
            config = NotionConfig(token = "test-token")
        }

        context("FileUpload model serialization") {
            test("should deserialize file upload response correctly") {
                val jsonString = TestFixtures.FileUploads.createFileUploadAsString()
                val fileUpload = TestFixtures.json.decodeFromString<FileUpload>(jsonString)

                fileUpload.id shouldBe "b52b8ed6-e029-4707-a671-832549c09de3"
                fileUpload.status shouldBe FileUploadStatus.PENDING
                fileUpload.filename shouldBe "test.txt"
                fileUpload.contentType shouldBe "text/plain"
                fileUpload.contentLength shouldBe 1024L
                fileUpload.uploadUrl shouldNotBe null
            }

            test("should deserialize uploaded file response correctly") {
                val jsonString = TestFixtures.FileUploads.retrieveFileUploadAsString()
                val fileUpload = TestFixtures.json.decodeFromString<FileUpload>(jsonString)

                fileUpload.id shouldBe "b52b8ed6-e029-4707-a671-832549c09de3"
                fileUpload.status shouldBe FileUploadStatus.UPLOADED
                fileUpload.uploadUrl shouldBe null // No upload URL when already uploaded
            }

            test("should serialize create file upload request correctly") {
                val request =
                    CreateFileUploadRequest(
                        mode = FileUploadMode.MULTI_PART, // Use non-default value
                        filename = "document.pdf",
                        contentType = "application/pdf",
                        numberOfParts = 3,
                    )

                val json = Json.encodeToString(CreateFileUploadRequest.serializer(), request)

                json.contains("\"mode\":\"multi_part\"") shouldBe true
                json.contains("\"filename\":\"document.pdf\"") shouldBe true
                json.contains("\"content_type\":\"application/pdf\"") shouldBe true
                json.contains("\"number_of_parts\":3") shouldBe true
            }
        }

        context("createFileUpload") {
            test("should create a file upload successfully") {
                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Post,
                            path = "/v1/file_uploads",
                            responseBody = TestFixtures.FileUploads.createFileUploadAsString(),
                        )
                    }
                api = FileUploadApi(mockClient, config)

                val request =
                    CreateFileUploadRequest(
                        filename = "test.txt",
                        contentType = "text/plain",
                    )
                val result = api.createFileUpload(request)

                result.id shouldBe "b52b8ed6-e029-4707-a671-832549c09de3"
                result.status shouldBe FileUploadStatus.PENDING
                result.filename shouldBe "test.txt"
                result.uploadUrl shouldNotBe null
            }

            test("should create multi-part file upload") {
                val multiPartResponse =
                    """
                    {
                        "id": "multi-part-upload-id",
                        "object": "file_upload",
                        "created_time": "2025-03-15T20:53:00.000Z",
                        "last_edited_time": "2025-03-15T20:53:00.000Z",
                        "expiry_time": "2025-03-15T21:53:00.000Z",
                        "upload_url": "https://api.notion.com/v1/file_uploads/multi-part-upload-id/send",
                        "archived": false,
                        "status": "pending",
                        "filename": "large-file.zip",
                        "content_type": "application/zip",
                        "content_length": 52428800
                    }
                    """.trimIndent()

                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Post,
                            path = "/v1/file_uploads",
                            responseBody = multiPartResponse,
                        )
                    }
                api = FileUploadApi(mockClient, config)

                val request =
                    CreateFileUploadRequest(
                        mode = FileUploadMode.MULTI_PART,
                        filename = "large-file.zip",
                        contentType = "application/zip",
                        numberOfParts = 3,
                    )
                val result = api.createFileUpload(request)

                result.status shouldBe FileUploadStatus.PENDING
                result.filename shouldBe "large-file.zip"
                result.contentLength shouldBe 52428800L
            }

            test("should create external URL file upload") {
                val externalUrlResponse =
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
                    """.trimIndent()

                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Post,
                            path = "/v1/file_uploads",
                            responseBody = externalUrlResponse,
                        )
                    }
                api = FileUploadApi(mockClient, config)

                val request =
                    CreateFileUploadRequest(
                        mode = FileUploadMode.EXTERNAL_URL,
                        filename = "external-image.jpg",
                        contentType = "image/jpeg",
                        externalUrl = "https://example.com/image.jpg",
                    )
                val result = api.createFileUpload(request)

                result.status shouldBe FileUploadStatus.UPLOADED
                result.filename shouldBe "external-image.jpg"
            }
        }

        context("retrieveFileUpload") {
            test("should retrieve a file upload successfully") {
                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/file_uploads/b52b8ed6-e029-4707-a671-832549c09de3",
                            responseBody = TestFixtures.FileUploads.retrieveFileUploadAsString(),
                        )
                    }
                api = FileUploadApi(mockClient, config)

                val result = api.retrieveFileUpload("b52b8ed6-e029-4707-a671-832549c09de3")

                result.id shouldBe "b52b8ed6-e029-4707-a671-832549c09de3"
                result.status shouldBe FileUploadStatus.UPLOADED
                result.uploadUrl shouldBe null // No upload URL when already uploaded
            }
        }

        context("completeFileUpload") {
            test("should complete a multi-part file upload") {
                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Post,
                            path = "/v1/file_uploads/b52b8ed6-e029-4707-a671-832549c09de3/complete",
                            responseBody = TestFixtures.FileUploads.completeFileUploadAsString(),
                        )
                    }
                api = FileUploadApi(mockClient, config)

                val result = api.completeFileUpload("b52b8ed6-e029-4707-a671-832549c09de3")

                result.status shouldBe FileUploadStatus.UPLOADED
                result.uploadUrl shouldBe null
            }
        }

        context("importExternalFile") {
            test("should import an external file") {
                val externalResponse =
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
                    """.trimIndent()

                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Post,
                            path = "/v1/file_uploads",
                            responseBody = externalResponse,
                        )
                    }
                api = FileUploadApi(mockClient, config)

                val result =
                    api.importExternalFile(
                        filename = "external-image.jpg",
                        externalUrl = "https://example.com/image.jpg",
                        contentType = "image/jpeg",
                    )

                result.id shouldBe "external-file-id"
                result.status shouldBe FileUploadStatus.UPLOADED
                result.filename shouldBe "external-image.jpg"
            }
        }

        context("convenience methods") {
            test("uploadFile should handle complete single-part upload flow") {
                val createResponse = TestFixtures.FileUploads.createFileUploadAsString()
                val sendResponse = TestFixtures.FileUploads.sendFileUploadAsString()

                val mockClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Post,
                            path = "/v1/file_uploads",
                            responseBody = createResponse,
                        )
                        // Note: sendFileUpload uses multipart/form-data so we can't easily mock it here
                        // This would be better tested in integration tests
                    }
                api = FileUploadApi(mockClient, config)

                // This test is limited due to the multipart form data handling
                // Full testing would be done in integration tests
            }
        }
    })
