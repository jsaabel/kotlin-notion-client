package examples

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.files.FileUploadOptions
import no.saabelit.kotlinnotionclient.models.files.FileUploadResult
import no.saabelit.kotlinnotionclient.models.files.FileUploadStatus
import no.saabelit.kotlinnotionclient.models.files.UploadProgressStatus
import java.io.File
import java.nio.file.Files

/**
 * File Upload API Examples
 *
 * This file contains validated examples for both the basic and enhanced File Upload APIs,
 * suitable for documentation. Each example has been tested against the live Notion API.
 *
 * Prerequisites:
 * - Set environment variable: export NOTION_RUN_INTEGRATION_TESTS="true"
 * - Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * - Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * - Set environment variable: export NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 */
@Tags("Integration", "RequiresApi", "Examples")
class FileUploadsExamples :
    StringSpec({

        if (!integrationTestEnvVarsAreSet("NOTION_API_TOKEN", "NOTION_TEST_PAGE_ID")) {
            "!(Skipped) File upload examples" {
                println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true and required env vars")
            }
        } else {

            val token = System.getenv("NOTION_API_TOKEN")!!
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")!!

            val notion = NotionClient.create(NotionConfig(apiToken = token))
            val uploadedFiles = mutableListOf<String>()

            fun createTestFile(
                filename: String,
                content: String,
            ): File {
                val tempFile = Files.createTempFile("notion-test", filename).toFile()
                tempFile.writeText(content)
                tempFile.deleteOnExit()
                return tempFile
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    println("🧹 Cleaning up ${uploadedFiles.size} uploaded files...")
                    uploadedFiles.forEach { fileId ->
                        try {
                            val upload = notion.fileUploads.retrieveFileUpload(fileId)
                            println("   📄 File: ${upload.filename} (${upload.status})")
                        } catch (_: Exception) {
                            println("   ⚠️ Could not retrieve file info for $fileId")
                        }
                    }
                }
                notion.close()
            }

            // ===== ENHANCED FILE UPLOAD API EXAMPLES =====

            "Enhanced API - Upload from File object" {
                val testFile = createTestFile("-test-document.txt", "This is a test document content.")

                val result =
                    notion.enhancedFileUploads.uploadFile(
                        file = testFile,
                    )

                // Verify upload was successful
                when (result) {
                    is FileUploadResult.Success -> {
                        result.fileUpload.filename shouldBe testFile.name
                        result.fileUpload.status shouldBe FileUploadStatus.UPLOADED
                        uploadedFiles.add(result.uploadId)
                        println("✅ Enhanced upload successful: ${result.uploadId}")
                    }
                    is FileUploadResult.Failure -> {
                        error("Upload failed: ${result.error}")
                    }
                }
            }

            "Enhanced API - Upload from Path" {
                val testContent = "# Markdown Test\n\nThis is a **test** markdown file."
                val testPath = Files.createTempFile("notion-test", "-markdown.md")
                Files.write(testPath, testContent.toByteArray())

                val result =
                    notion.enhancedFileUploads.uploadFile(
                        path = testPath,
                    )

                // Verify upload was successful
                when (result) {
                    is FileUploadResult.Success -> {
                        result.fileUpload.filename shouldBe testPath.fileName.toString()
                        result.fileUpload.contentType shouldBe "text/markdown"
                        uploadedFiles.add(result.uploadId)
                        println("✅ Path upload successful: ${result.uploadId}")
                    }
                    is FileUploadResult.Failure -> {
                        error("Upload failed: ${result.error}")
                    }
                }
            }

            "Enhanced API - Upload from byte array" {
                val jsonContent = """{"message": "Hello from Kotlin Notion Client", "timestamp": "${System.currentTimeMillis()}"}"""

                val result =
                    notion.enhancedFileUploads.uploadFile(
                        filename = "test-data.json",
                        data = jsonContent.toByteArray(),
                    )

                // Verify upload was successful
                when (result) {
                    is FileUploadResult.Success -> {
                        result.fileUpload.filename shouldBe "test-data.json"
                        result.fileUpload.contentType shouldBe "application/json"
                        uploadedFiles.add(result.uploadId)
                        println("✅ Byte array upload successful: ${result.uploadId}")
                    }
                    is FileUploadResult.Failure -> {
                        error("Upload failed: ${result.error}")
                    }
                }
            }

            "Enhanced API - Upload with progress tracking" {
                val testFile = createTestFile("-progress-test.txt", "Content for progress tracking test.")
                var progressUpdates = 0
                var completedReceived = false

                val options =
                    FileUploadOptions(
                        contentType = "text/plain",
                        validateBeforeUpload = true,
                        progressCallback = { progress ->
                            progressUpdates++
                            println("📊 Progress: ${progress.filename} - ${progress.status} (${progress.progressPercent.toInt()}%)")

                            if (progress.status == UploadProgressStatus.COMPLETED) {
                                completedReceived = true
                            }
                        },
                    )

                val result =
                    notion.enhancedFileUploads.uploadFile(
                        file = testFile,
                        options = options,
                    )

                // Verify upload was successful
                when (result) {
                    is FileUploadResult.Success -> {
                        progressUpdates shouldNotBe 0
                        completedReceived shouldBe true
                        uploadedFiles.add(result.uploadId)
                        println("✅ Progress tracking upload successful: ${result.uploadId}")
                    }
                    is FileUploadResult.Failure -> {
                        error("Upload failed: ${result.error}")
                    }
                }
            }

            "Enhanced API - Import from external URL" {
                // Using a reliable public image for testing
                val externalUrl = "https://via.placeholder.com/150x150.png?text=Test"

                val result =
                    notion.enhancedFileUploads.importExternalFile(
                        filename = "placeholder-image.png",
                        externalUrl = externalUrl,
                        contentType = "image/png",
                    )

                // Verify upload was successful
                when (result) {
                    is FileUploadResult.Success -> {
                        result.fileUpload.filename shouldBe "placeholder-image.png"
                        // Note: FileUpload response doesn't expose the mode directly
                        uploadedFiles.add(result.uploadId)

                        // Wait for external file to be processed
                        val readyFile =
                            notion.enhancedFileUploads.waitForFileReady(
                                fileUploadId = result.uploadId,
                                maxWaitTimeMs = 15000, // 15 seconds
                            )
                        readyFile.status shouldBe FileUploadStatus.UPLOADED

                        println("✅ External URL import successful: ${result.uploadId}")
                    }
                    is FileUploadResult.Failure -> {
                        error("Upload failed: ${result.error}")
                    }
                }
            }

            // ===== BASIC FILE UPLOAD API EXAMPLES =====

            "Basic API - Single-part upload using DSL" {
                val testContent = "Basic API test content using DSL syntax."

                val fileUpload =
                    notion.fileUploads.createFileUpload {
                        filename("basic-dsl-test.txt")
                        contentType("text/plain")
                    }

                // Note: FileUpload response doesn't expose the mode directly
                fileUpload.filename shouldBe "basic-dsl-test.txt"

                val uploadedFile =
                    notion.fileUploads.sendFileUpload(
                        fileUploadId = fileUpload.id,
                        fileContent = testContent.toByteArray(),
                    )

                uploadedFile.status shouldBe FileUploadStatus.UPLOADED
                uploadedFiles.add(fileUpload.id)
                println("✅ Basic DSL upload successful: ${fileUpload.id}")
            }

            "Basic API - External URL import using DSL" {
                val externalUrl = "https://via.placeholder.com/200x200.jpg?text=BasicAPI"

                val fileUpload =
                    notion.fileUploads.createFileUpload {
                        filename("basic-external-image.jpg")
                        contentType("image/jpeg")
                        externalUrl(externalUrl) // Sets both mode and URL
                    }

                // Note: FileUpload response doesn't expose the mode directly
                fileUpload.filename shouldBe "basic-external-image.jpg"
                // Note: FileUpload response doesn't expose externalUrl directly
                uploadedFiles.add(fileUpload.id)

                println("✅ Basic external URL import successful: ${fileUpload.id}")
            }

            "Basic API - Multi-part upload using DSL" {
                // Create a larger test content (split into 3 parts for testing)
                val fullContent =
                    "Part 1 content. ".repeat(100) +
                        "Part 2 content. ".repeat(100) +
                        "Part 3 content. ".repeat(100)

                val fileUpload =
                    notion.fileUploads.createFileUpload {
                        multiPart()
                        filename("multipart-test.txt")
                        contentType("text/plain")
                        numberOfParts(3)
                    }

                // Note: FileUpload response doesn't expose the mode directly
                // Note: FileUpload response doesn't expose numberOfParts directly

                // Split content and upload parts
                val contentBytes = fullContent.toByteArray()
                val partSize = contentBytes.size / 3

                for (partNumber in 1..3) {
                    val startIdx = (partNumber - 1) * partSize
                    val endIdx = if (partNumber == 3) contentBytes.size else partNumber * partSize
                    val partContent = contentBytes.sliceArray(startIdx until endIdx)

                    notion.fileUploads.sendFileUpload(
                        fileUploadId = fileUpload.id,
                        fileContent = partContent,
                        partNumber = partNumber,
                    )
                    println("📤 Uploaded part $partNumber/3")
                }

                // Complete the multi-part upload
                val completedUpload = notion.fileUploads.completeFileUpload(fileUpload.id)
                completedUpload.status shouldBe FileUploadStatus.UPLOADED
                uploadedFiles.add(fileUpload.id)

                println("✅ Basic multi-part upload successful: ${fileUpload.id}")
            }

            "Basic API - List and retrieve operations" {
                // Upload a file to test retrieval
                val testFile = createTestFile("-retrieve-test.txt", "Content for retrieval testing.")

                val result = notion.enhancedFileUploads.uploadFile(file = testFile)
                if (result is FileUploadResult.Success) {
                    uploadedFiles.add(result.uploadId)

                    // Retrieve specific file upload
                    val retrievedUpload = notion.fileUploads.retrieveFileUpload(result.uploadId)
                    retrievedUpload.id shouldBe result.uploadId
                    retrievedUpload.filename shouldBe testFile.name

                    // List file uploads (should include our uploaded file)
                    val uploads = notion.fileUploads.listFileUploads(pageSize = 10)
                    uploads.results shouldNotBe emptyList<Any>()

                    val ourUpload = uploads.results.find { it.id == result.uploadId }
                    ourUpload shouldNotBe null
                    ourUpload?.filename shouldBe testFile.name

                    println("✅ List and retrieve operations successful")
                    println("   📋 Found ${uploads.results.size} uploads, hasMore: ${uploads.hasMore}")
                }
            }

            // ===== USING UPLOADED FILES IN PAGES =====

            "Using uploaded files in pages" {
                val testFile = createTestFile("-page-integration.md", "# Integration Test\n\nThis file is used in a page.")

                // Upload the file
                val uploadResult = notion.enhancedFileUploads.uploadFile(file = testFile)
                if (uploadResult is FileUploadResult.Success) {
                    uploadedFiles.add(uploadResult.uploadId)

                    // Create a page that references the uploaded file
                    val page =
                        notion.pages.create {
                            parent.page(parentPageId)
                            title("File Upload Integration Test")
                            content {
                                paragraph {
                                    text("This page demonstrates file upload integration with pages.")
                                }

                                paragraph {
                                    text("Uploaded file details:")
                                }

                                bullet("ID: ${uploadResult.fileUpload.id}")
                                bullet("Name: ${testFile.name}")
                                bullet("Size: ${testFile.length()} bytes")
                                bullet("Type: ${uploadResult.fileUpload.contentType}")

                                paragraph {
                                    text("The file has been successfully uploaded and can be referenced by its ID.")
                                }
                            }
                        }

                    page.id shouldNotBe null
                    println("✅ File integration with page successful")
                    println("   📄 Page ID: ${page.id}")
                    println("   📎 File ID: ${uploadResult.uploadId}")
                }
            }

            // ===== ERROR HANDLING EXAMPLES =====

            "Error handling - File validation" {
                val options =
                    FileUploadOptions(
                        validateBeforeUpload = true,
                    )

                // Test with empty filename (should fail validation)
                try {
                    val result =
                        notion.enhancedFileUploads.uploadFile(
                            filename = "", // Invalid empty filename
                            data = "test content".toByteArray(),
                            options = options,
                        )

                    if (result is FileUploadResult.Failure) {
                        println("✅ Validation correctly caught empty filename: ${result.error.message}")
                    }
                } catch (e: Exception) {
                    println("✅ Validation exception caught: ${e.message}")
                }
            }

            "Error handling - Invalid external URL" {
                try {
                    notion.fileUploads.createFileUpload {
                        filename("test.txt")
                        externalUrl("http://example.com/file.txt") // HTTP instead of HTTPS
                    }
                } catch (e: IllegalArgumentException) {
                    e.message shouldNotBe null
                    println("✅ HTTPS validation working: ${e.message}")
                }
            }

            "Basic API - Convenience upload methods" {
                val testContent = "Testing convenience method for basic uploads."

                // Use the convenience uploadFile method
                val fileUpload =
                    notion.fileUploads.uploadFile(
                        filename = "convenience-test.txt",
                        contentType = "text/plain",
                        fileContent = testContent.toByteArray(),
                    )

                fileUpload.status shouldBe FileUploadStatus.UPLOADED
                fileUpload.filename shouldBe "convenience-test.txt"
                uploadedFiles.add(fileUpload.id)

                println("✅ Convenience upload method successful: ${fileUpload.id}")
            }
        }
    })
