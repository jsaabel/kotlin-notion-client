package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import it.saabel.kotlinnotionclient.api.EnhancedFileUploadApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.files.FileUploadError
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus
import unit.util.TestFixtures
import unit.util.mockClient

/**
 * [EnhancedFileUploadApi.waitForFileReady] must bail out on the terminal statuses an upload can
 * never leave, rather than polling to the timeout (issue #68).
 */
@Tags("Unit")
class EnhancedFileUploadApiWaitTest :
    FunSpec({
        val config = NotionConfig(apiToken = "test-token")

        fun apiReturning(responseBody: String) =
            EnhancedFileUploadApi(
                mockClient {
                    addJsonResponse(
                        method = HttpMethod.Get,
                        path = "/v1/file_uploads/",
                        responseBody = responseBody,
                    )
                },
                config,
            )

        test("fails fast on an expired upload instead of spinning to the timeout") {
            val api = apiReturning(TestFixtures.FileUploads.expiredFileUploadAsString())

            val error =
                shouldThrow<FileUploadError.UploadUnusableError> {
                    // A generous timeout: if the status were not treated as terminal, this test
                    // would take 30s instead of failing immediately.
                    api.waitForFileReady("a3f9c1d2-4b6e-4a10-9c33-7de2f0a1b845", maxWaitTimeMs = 30_000)
                }

            error.status shouldBe FileUploadStatus.EXPIRED
            error.importError.shouldBeNull()
            error.message shouldContain "expired"
        }

        test("fails fast on a failed import and reports why it failed") {
            val api = apiReturning(TestFixtures.FileUploads.failedImportFileUploadAsString())

            val error =
                shouldThrow<FileUploadError.UploadUnusableError> {
                    api.waitForFileReady("d7b2e4f1-6c08-4a3d-8e91-5f0c2a6b74de", maxWaitTimeMs = 30_000)
                }

            error.status shouldBe FileUploadStatus.FAILED
            error.importError?.code shouldBe "file_upload_invalid_size"
            error.message shouldContain "file_upload_invalid_size"
        }

        test("returns the upload once it is uploaded") {
            val api = apiReturning(TestFixtures.FileUploads.importedFileUploadAsString())

            val upload = api.waitForFileReady("f04c8a19-3b5d-4e72-a6c8-91ed7b0f2a4c")

            upload.status shouldBe FileUploadStatus.UPLOADED
            upload.filename shouldBe "remote-image.jpg"
        }

        test("times out rather than bailing out while an upload is still pending") {
            val api = apiReturning(TestFixtures.FileUploads.multiPartFileUploadAsString())

            shouldThrow<FileUploadError.TimeoutError> {
                api.waitForFileReady(
                    "9c6e2b47-8f31-4d05-b7a2-c04e91f8d3b6",
                    maxWaitTimeMs = 150,
                    checkIntervalMs = 25,
                )
            }
        }
    })
