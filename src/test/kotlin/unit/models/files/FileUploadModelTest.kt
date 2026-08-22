package unit.models.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.files.FileImportResult
import it.saabel.kotlinnotionclient.models.files.FileUpload
import it.saabel.kotlinnotionclient.models.files.FileUploadError
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus
import kotlinx.serialization.json.Json
import unit.util.TestFixtures

/**
 * Model-correctness tests for [FileUpload] (issue #68).
 *
 * Each test pins one shape the API documents but the model previously got wrong, so the
 * regression shows up as a failing decode rather than a runtime surprise in a caller's workspace.
 */
@Tags("Unit")
class FileUploadModelTest :
    FunSpec({
        // Mirrors the client's own Json configuration (NotionClient.kt).
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }

        context("status") {
            test("decodes the documented expired status instead of throwing") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.expiredFileUploadAsString(),
                    )

                upload.status shouldBe FileUploadStatus.EXPIRED
                upload.status.isTerminal shouldBe true
                upload.status.isUnusable shouldBe true
            }

            test("decodes all four documented statuses") {
                listOf(
                    "pending" to FileUploadStatus.PENDING,
                    "uploaded" to FileUploadStatus.UPLOADED,
                    "expired" to FileUploadStatus.EXPIRED,
                    "failed" to FileUploadStatus.FAILED,
                ).forEach { (wire, expected) ->
                    json.decodeFromString<FileUploadStatus>("\"$wire\"") shouldBe expected
                }
            }

            test("falls back to UNKNOWN for a status this library does not know yet") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.unknownStatusFileUploadAsString(),
                    )

                upload.status shouldBe FileUploadStatus.UNKNOWN
                upload.status.isTerminal shouldBe false
                upload.status.isUnusable shouldBe false
            }

            test("serializes back to the documented wire values") {
                json.encodeToString(FileUploadStatus.EXPIRED) shouldBe "\"expired\""
                json.encodeToString(FileUploadStatus.UPLOADED) shouldBe "\"uploaded\""
            }

            test("only uploaded, expired and failed are terminal") {
                FileUploadStatus.PENDING.isTerminal shouldBe false
                FileUploadStatus.UPLOADED.isTerminal shouldBe true
                FileUploadStatus.EXPIRED.isTerminal shouldBe true
                FileUploadStatus.FAILED.isTerminal shouldBe true

                // uploaded is terminal but perfectly usable
                FileUploadStatus.UPLOADED.isUnusable shouldBe false
            }
        }

        context("nullable filename and content_type") {
            test("decodes an upload created without a filename or content type") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.multiPartFileUploadAsString(),
                    )

                upload.filename.shouldBeNull()
                upload.contentType.shouldBeNull()
                upload.status shouldBe FileUploadStatus.PENDING
            }

            test("decodes a response that omits the keys entirely") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        """
                        {
                          "object": "file_upload",
                          "id": "b52b8ed6-e029-4707-a671-832549c09de3",
                          "created_time": "2025-03-15T20:53:00.000Z",
                          "last_edited_time": "2025-03-15T20:53:00.000Z",
                          "status": "pending"
                        }
                        """.trimIndent(),
                    )

                upload.filename.shouldBeNull()
                upload.contentType.shouldBeNull()
            }
        }

        context("file_import_result") {
            test("decodes the error variant with the reason the import failed") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.failedImportFileUploadAsString(),
                    )

                upload.status shouldBe FileUploadStatus.FAILED

                val result = upload.fileImportResult.shouldBeInstanceOf<FileImportResult.Error>()
                result.type shouldBe "error"
                result.importedTime shouldBe "2025-03-15T20:54:00.000Z"
                result.error.type shouldBe "validation_error"
                result.error.code shouldBe "file_upload_invalid_size"
                result.error.message shouldContain "5 MiB"
                result.error.parameter.shouldBeNull()
                result.error.statusCode.shouldBeNull()

                // The convenience accessor is the whole point: a failed import can explain itself.
                upload.importError shouldBe result.error
            }

            test("decodes the success variant") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.importedFileUploadAsString(),
                    )

                upload.status shouldBe FileUploadStatus.UPLOADED

                val result = upload.fileImportResult.shouldBeInstanceOf<FileImportResult.Success>()
                result.type shouldBe "success"
                result.importedTime shouldBe "2025-03-15T20:54:00.000Z"

                upload.importError.shouldBeNull()
            }

            test("falls back to Unknown for an unrecognized result type, keeping the raw JSON") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.unknownStatusFileUploadAsString(),
                    )

                val result = upload.fileImportResult.shouldBeInstanceOf<FileImportResult.Unknown>()
                result.type shouldBe "partial"
                result.importedTime shouldBe "2025-03-15T20:54:00.000Z"
                result.rawContent.toString() shouldContain "still processing"
            }

            test("is absent for uploads that are not external-URL imports") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.retrieveFileUploadAsString(),
                    )

                upload.fileImportResult.shouldBeNull()
                upload.importError.shouldBeNull()
            }
        }

        context("complete_url, number_of_parts and created_by") {
            test("decodes the fields a pending multi-part upload carries") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.multiPartFileUploadAsString(),
                    )

                upload.completeUrl shouldBe
                    "https://api.notion.com/v1/file_uploads/9c6e2b47-8f31-4d05-b7a2-c04e91f8d3b6/complete"
                upload.numberOfParts?.total shouldBe 5
                upload.numberOfParts?.sent shouldBe 0
                upload.createdBy?.id shouldBe "c1e5b3a7-9d24-4f68-b0aa-2c8e4f7d1b93"
                upload.createdBy?.type shouldBe "bot"
            }

            test("leaves them null for a single-part upload that has none") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.retrieveFileUploadAsString(),
                    )

                upload.completeUrl.shouldBeNull()
                upload.numberOfParts.shouldBeNull()
            }
        }

        context("in_trash") {
            test("reads the documented in_trash key") {
                // Verified against the fileUploadObjectResponse OpenAPI schema on
                // developers.notion.com: the object carries `in_trash`, not `archived`.
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.retrieveFileUploadAsString(),
                    )

                upload.inTrash shouldBe false

                json
                    .decodeFromString<FileUpload>(
                        """
                        {
                          "object": "file_upload",
                          "id": "b52b8ed6-e029-4707-a671-832549c09de3",
                          "created_time": "2025-03-15T20:53:00.000Z",
                          "last_edited_time": "2025-03-15T20:53:00.000Z",
                          "in_trash": true,
                          "status": "uploaded"
                        }
                        """.trimIndent(),
                    ).inTrash shouldBe true
            }
        }

        context("UploadUnusableError") {
            test("names the status and carries the import reason") {
                val upload =
                    json.decodeFromString<FileUpload>(
                        TestFixtures.FileUploads.failedImportFileUploadAsString(),
                    )

                val error =
                    shouldThrow<FileUploadError.UploadUnusableError> {
                        throw FileUploadError.UploadUnusableError(
                            uploadId = upload.id,
                            status = upload.status,
                            importError = upload.importError,
                        )
                    }

                error.status shouldBe FileUploadStatus.FAILED
                error.importError?.code shouldBe "file_upload_invalid_size"
                error.message shouldContain "failed"
                error.message shouldContain "file_upload_invalid_size"
            }

            test("reads cleanly for an expired upload with no import result") {
                val error =
                    FileUploadError.UploadUnusableError(
                        uploadId = "a3f9c1d2-4b6e-4a10-9c33-7de2f0a1b845",
                        status = FileUploadStatus.EXPIRED,
                    )

                error.message shouldContain "expired"
                error.importError.shouldBeNull()
            }
        }
    })
