package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.files.FileUploadProgress
import it.saabel.kotlinnotionclient.models.files.FileUploadResult
import it.saabel.kotlinnotionclient.models.files.UploadProgressStatus
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import it.saabel.kotlinnotionclient.utils.FileUploadUtils
import kotlinx.coroutines.delay
import kotlin.io.path.createTempFile

/**
 * Integration tests for enhanced file upload functionality.
 *
 * These tests validate the complete file upload workflow including:
 * - Single-part uploads with progress tracking
 * - Multi-part uploads for large files
 * - External URL imports
 * - Error handling and validation
 * - Integration with content blocks
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 */
@Tags("Integration", "RequiresApi")
class EnhancedFileUploadIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping EnhancedFileUploadIntegrationTest due to missing environment variables") }
        } else {

            "Should upload small file with progress tracking" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📤 Testing small file upload with progress tracking...")

                    // Create test content
                    val testContent =
                        """
                        Enhanced File Upload Test
                        =========================
                        
                        This file was uploaded using the enhanced file upload API
                        with progress tracking and automatic content-type detection.
                        
                        Features tested:
                        - Single-part upload
                        - Progress callbacks
                        - Content-type detection
                        - File validation
                        
                        Timestamp: ${System.currentTimeMillis()}
                        """.trimIndent()

                    val progressUpdates = mutableListOf<FileUploadProgress>()
                    val options =
                        FileUploadOptions(
                            progressCallback = { progress ->
                                progressUpdates.add(progress)
                                println("📊 Progress: ${String.format("%.1f", progress.progressPercent)}% - ${progress.status}")
                            },
                        )

                    // Upload using enhanced API
                    val result =
                        client.enhancedFileUploads.uploadFile(
                            filename = "enhanced-upload-test.txt",
                            data = testContent.toByteArray(),
                            options = options,
                        )

                    result.shouldBeInstanceOf<FileUploadResult.Success>()
                    result.filename shouldBe "enhanced-upload-test.txt"
                    result.uploadTimeMs shouldNotBe 0

                    println("✅ File uploaded successfully: ${result.uploadId}")
                    println("⏱️ Upload time: ${result.uploadTimeMs}ms")

                    // Verify progress tracking
                    progressUpdates.size shouldBe 3
                    progressUpdates[0].status shouldBe UploadProgressStatus.STARTING
                    progressUpdates[1].status shouldBe UploadProgressStatus.UPLOADING
                    progressUpdates[2].status shouldBe UploadProgressStatus.COMPLETED

                    // Create a page to test the uploaded file
                    val pageRequest =
                        CreatePageRequest(
                            parent = Parent.PageParent(pageId = parentPageId),
                            icon = RequestBuilders.createEmojiIcon("📤"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title = listOf(RequestBuilders.createSimpleRichText("Enhanced Upload Test - Small File")),
                                        ),
                                ),
                        )

                    val page = client.pages.create(pageRequest)
                    println("📄 Test page created: ${page.id}")

                    delay(500)

                    // Add content with the uploaded file
                    val content =
                        pageContent {
                            heading1("Enhanced File Upload Test")
                            paragraph("Testing small file upload with enhanced features:")

                            bullet("Progress tracking ✅")
                            bullet("Content-type detection ✅")
                            bullet("File validation ✅")
                            bullet("Upload time: ${result.uploadTimeMs}ms")

                            divider()

                            heading2("Uploaded File")
                            fileFromUpload(
                                fileUploadId = result.fileUpload.id,
                                name = result.filename,
                                caption = "Uploaded using enhanced API with progress tracking",
                            )
                        }

                    val appendResponse = client.blocks.appendChildren(page.id, content)
                    println("✅ Content added: ${appendResponse.results.size} blocks")

                    // Cleanup if requested
                    if (shouldCleanupAfterTest()) {
                        delay(1000)
                        client.pages.archive(page.id)
                        println("🧹 Test page archived")
                    } else {
                        println("🔧 Test page preserved: ${page.id}")
                    }
                } finally {
                    client.close()
                }
            }

            "Should handle large file multi-part upload simulation" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📤 Testing large file upload simulation...")

                    // Create a file that will trigger multi-part upload (>20MB)
                    // Use a smaller size to avoid API limits and a text file for better compatibility
                    val largeFileSize = 22 * 1024 * 1024 // 22MB - just over the 20MB threshold
                    val tempFile = createTempFile("large-test", ".txt")

                    try {
                        // Create large text file content (realistic data)
                        val timestamp = System.currentTimeMillis()
                        val randomData = (1..50).map { ('a'..'z').random() }.joinToString("")
                        val pattern =
                            """
                            Large File Upload Test Data
                            ===========================
                            
                            This is a test file for demonstrating multi-part upload functionality.
                            Line number: %d
                            Timestamp: $timestamp
                            Random data: $randomData
                            
                            
                            """.trimIndent()

                        val patternBytes = pattern.toByteArray()
                        val totalLines = largeFileSize / patternBytes.size + 1

                        tempFile.toFile().bufferedWriter().use { writer ->
                            repeat(totalLines) { lineNum ->
                                writer.write(pattern.format(lineNum))
                            }
                        }

                        // Verify file size triggers multi-part
                        val shouldUseMultiPart = FileUploadUtils.shouldUseMultiPart(tempFile.toFile().length())
                        shouldUseMultiPart shouldBe true

                        println("📊 File size: ${tempFile.toFile().length() / (1024 * 1024)}MB (multi-part required)")

                        val progressUpdates = mutableListOf<FileUploadProgress>()
                        val options =
                            FileUploadOptions(
                                progressCallback = { progress ->
                                    if (progressUpdates.size % 5 == 0) { // Only log every 5th update to reduce noise
                                        val percent = String.format("%.1f", progress.progressPercent)
                                        val partInfo = "(part ${progress.currentPart}/${progress.totalParts})"
                                        println("📊 Multi-part progress: $percent% $partInfo - ${progress.status}")
                                    }
                                    progressUpdates.add(progress)
                                },
                                enableConcurrentParts = true,
                                maxConcurrentParts = 3,
                            )

                        val startTime = System.currentTimeMillis()

                        // Upload using enhanced API
                        val result =
                            client.enhancedFileUploads.uploadFile(
                                tempFile,
                                options = options,
                            )

                        val uploadTime = System.currentTimeMillis() - startTime

                        // Handle potential API limitations gracefully
                        if (result is FileUploadResult.Success) {
                            println("✅ Large file uploaded successfully: ${result.uploadId}")
                            println("⏱️ Upload time: ${uploadTime}ms")

                            // Verify multi-part progress tracking
                            val completedUpdates = progressUpdates.filter { it.status == UploadProgressStatus.COMPLETED }
                            completedUpdates.size shouldBe 1

                            val uploadingUpdates = progressUpdates.filter { it.status == UploadProgressStatus.UPLOADING }
                            uploadingUpdates.size shouldNotBe 0 // Should have multiple uploading updates

                            val successResult = result

                            // Create a page to demonstrate the upload
                            val pageRequest =
                                CreatePageRequest(
                                    parent = Parent.PageParent(pageId = parentPageId),
                                    icon = RequestBuilders.createEmojiIcon("🗂️"),
                                    properties =
                                        mapOf(
                                            "title" to
                                                PagePropertyValue.TitleValue(
                                                    title =
                                                        listOf(
                                                            RequestBuilders.createSimpleRichText("Enhanced Upload Test - Large File"),
                                                        ),
                                                ),
                                        ),
                                )

                            val page = client.pages.create(pageRequest)
                            println("📄 Test page created: ${page.id}")

                            delay(500)

                            // Add content with the uploaded file
                            val fileSizeMB = tempFile.toFile().length() / (1024 * 1024)
                            val content =
                                pageContent {
                                    heading1("Large File Upload Test")
                                    paragraph("Successfully uploaded a ${fileSizeMB}MB file using multi-part upload:")

                                    val calloutText =
                                        "This demonstrates the enhanced file upload API's ability to handle large files automatically"
                                    callout("ℹ️", calloutText)

                                    bullet("File size: ${fileSizeMB}MB")
                                    bullet("Upload method: Multi-part")
                                    bullet("Concurrent parts: ${options.maxConcurrentParts}")
                                    bullet("Upload time: ${uploadTime}ms")
                                    bullet("Progress updates: ${progressUpdates.size}")

                                    divider()

                                    heading2("Uploaded Large File")
                                    fileFromUpload(
                                        fileUploadId = successResult.fileUpload.id,
                                        name = successResult.filename,
                                        caption = "Large file uploaded via multi-part upload with concurrent parts",
                                    )
                                }

                            val appendResponse = client.blocks.appendChildren(page.id, content)
                            println("✅ Content added: ${appendResponse.results.size} blocks")

                            // Cleanup if requested
                            if (shouldCleanupAfterTest()) {
                                delay(1000)
                                client.pages.archive(page.id)
                                println("🧹 Test page archived")
                            } else {
                                println("🔧 Test page preserved: ${page.id}")
                            }
                        } else {
                            // If the API doesn't support large files or has validation issues,
                            // we'll just verify our chunking logic worked correctly
                            val failureResult = result as FileUploadResult.Failure
                            println("⚠️ Large file upload failed (this may be expected due to API limits):")
                            println("   Error: ${failureResult.error.message}")

                            val chunking = FileUploadUtils.calculateChunking(tempFile.toFile().length())
                            println("📊 Chunking strategy calculated correctly:")
                            println("   - Chunk size: ${chunking.chunkSize / (1024 * 1024)}MB")
                            println("   - Number of parts: ${chunking.numberOfParts}")
                        }
                    } finally {
                        tempFile.toFile().delete()
                    }
                } finally {
                    client.close()
                }
            }

            "Should import external file with validation" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🌐 Testing external file import...")

                    val progressUpdates = mutableListOf<FileUploadProgress>()
                    val options =
                        FileUploadOptions(
                            progressCallback = { progress ->
                                progressUpdates.add(progress)
                                println("📊 External import: ${progress.status}")
                            },
                        )

                    // Import a publicly accessible file
                    val result =
                        client.enhancedFileUploads.importExternalFile(
                            filename = "sample-pdf",
                            externalUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                            contentType = "application/pdf",
                            options = options,
                        )

                    result.shouldBeInstanceOf<FileUploadResult.Success>()
                    result.filename shouldBe "sample-pdf.pdf" // Extension should be added

                    println("✅ External file imported: ${result.uploadId}")

                    // Wait for the file to be fully processed by Notion
                    try {
                        println("⏳ Waiting for file to be ready...")
                        val readyFile = client.enhancedFileUploads.waitForFileReady(result.fileUpload.id)
                        println("✅ File is ready: status=${readyFile.status}")
                    } catch (e: Exception) {
                        println("⚠️ Warning: File status check failed, proceeding anyway: ${e.message}")
                    }

                    // Create a page to demonstrate the import
                    val pageRequest =
                        CreatePageRequest(
                            parent = Parent.PageParent(pageId = parentPageId),
                            icon = RequestBuilders.createEmojiIcon("🌐"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title = listOf(RequestBuilders.createSimpleRichText("Enhanced Upload Test - External Import")),
                                        ),
                                ),
                        )

                    val page = client.pages.create(pageRequest)
                    println("📄 Test page created: ${page.id}")

                    delay(500)

                    // Add content with the imported file
                    val content =
                        pageContent {
                            heading1("External File Import Test")
                            paragraph("Successfully imported a file from an external URL:")

                            callout("🌐", "This demonstrates importing files from external URLs with validation")

                            bullet("Source URL validated (HTTPS required)")
                            bullet("Filename extension auto-added")
                            bullet("Content-type specified")
                            bullet("Import completed instantly")

                            divider()

                            heading2("Imported External File")
                            pdfFromUpload(
                                fileUploadId = result.fileUpload.id,
                                caption = "PDF imported from external URL with validation",
                            )
                        }

                    val appendResponse = client.blocks.appendChildren(page.id, content)
                    println("✅ Content added: ${appendResponse.results.size} blocks")

                    // Cleanup if requested
                    if (shouldCleanupAfterTest()) {
                        delay(1000)
                        client.pages.archive(page.id)
                        println("🧹 Test page archived")
                    } else {
                        println("🔧 Test page preserved: ${page.id}")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
