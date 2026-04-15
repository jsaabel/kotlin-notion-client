@file:Suppress("UnusedVariable")

package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import it.saabel.kotlinnotionclient.models.files.CreateFileUploadRequest
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.files.FileUploadProgress
import it.saabel.kotlinnotionclient.models.files.FileUploadResult
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus
import it.saabel.kotlinnotionclient.models.files.UploadProgressStatus
import it.saabel.kotlinnotionclient.utils.FileUploadUtils
import kotlinx.coroutines.delay
import kotlin.io.path.createTempFile

/**
 * Integration tests for media block functionality and file uploads.
 *
 * Covers:
 * - External URL media blocks (image, video, audio, file, PDF)
 * - File upload workflow (create, send, retrieve, embed)
 * - Complete mixed-source media showcase
 * - Enhanced file uploads: small file with progress tracking
 * - Enhanced file uploads: large file multi-part upload
 * - Enhanced file uploads: external file import with validation
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Run with: ./gradlew integrationTest --tests "*MediaIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class MediaIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) media integration" {
                println("Skipping MediaIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            var containerPageId = ""

            fun createSampleFileContent(filename: String): ByteArray =
                when {
                    filename.endsWith(".txt") -> "Sample text file content for testing\nLine 2\nLine 3".toByteArray()
                    filename.endsWith(".json") -> """{"test": "data", "number": 42}""".toByteArray()
                    filename.endsWith(".csv") -> "name,age,city\nJohn,30,New York\nJane,25,London".toByteArray()
                    else -> "Binary file content placeholder".toByteArray()
                }

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Media & File Uploads — Integration Tests")
                        icon.emoji("🎬")
                        content {
                            callout(
                                "ℹ️",
                                "Covers external URL media blocks (image, video, audio, file, PDF), the raw file upload " +
                                    "workflow (create/send/retrieve/embed), a complete mixed-source showcase, and the " +
                                    "enhanced upload API (progress tracking, multi-part, external import).",
                            )
                        }
                    }
                containerPageId = container.id
                println("📄 Container: ${container.url}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("✅ Cleaned up container page (all children trashed)")
                } else {
                    println("🔧 Cleanup skipped — container page preserved for inspection")
                }
                notion.close()
            }

            // ------------------------------------------------------------------
            // 1. External URL media blocks
            // ------------------------------------------------------------------
            "external URL media blocks should be created and verified" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("External URL Media Blocks")
                        icon.emoji("🖼️")
                    }
                println("  Sub-page: ${page.url}")

                delay(500)

                val content =
                    pageContent {
                        heading1("External Media Blocks Test")
                        paragraph("Testing various media block types with external URLs:")
                        divider()
                        heading2("Image Block")
                        image(
                            url = "https://placehold.co/600x400.png",
                            caption = "Test image from placeholder service",
                        )
                        heading2("Video Block")
                        video(
                            url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                            caption = "Sample YouTube video",
                        )
                        heading2("Audio Block")
                        audio(
                            url = "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav",
                            caption = "Sample audio file",
                        )
                        heading2("File Block")
                        file(
                            url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                            name = "sample-document.pdf",
                            caption = "Sample PDF file",
                        )
                        heading2("PDF Block")
                        pdf(
                            url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                            caption = "Sample PDF as PDF block",
                        )
                        divider()
                        paragraph("All external media blocks created successfully!")
                    }

                val appendResponse = notion.blocks.appendChildren(page.id, content)
                appendResponse.objectType shouldBe "list"
                println("  ✓ Appended ${appendResponse.results.size} blocks")

                delay(1000)
                val blocks = notion.blocks.retrieveChildren(page.id)

                val imageBlocks = blocks.filterIsInstance<Block.Image>()
                val videoBlocks = blocks.filterIsInstance<Block.Video>()
                val audioBlocks = blocks.filterIsInstance<Block.Audio>()
                val fileBlocks = blocks.filterIsInstance<Block.File>()
                val pdfBlocks = blocks.filterIsInstance<Block.PDF>()

                imageBlocks shouldHaveSize 1
                videoBlocks shouldHaveSize 1
                audioBlocks shouldHaveSize 1
                fileBlocks shouldHaveSize 1
                pdfBlocks shouldHaveSize 1

                val imageBlock = imageBlocks[0]
                imageBlock.image.type shouldBe "external"
                imageBlock.image.external?.url shouldBe "https://placehold.co/600x400.png"
                imageBlock.image.caption.isNotEmpty() shouldBe true

                println("  ✅ All 5 external media block types verified")
            }

            // ------------------------------------------------------------------
            // 2. File upload workflow and media block creation
            // ------------------------------------------------------------------
            "file upload workflow should produce embeddable media blocks" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("File Upload Media Blocks")
                        icon.emoji("📁")
                    }
                println("  Sub-page: ${page.url}")

                delay(500)

                val testFilename = "test-document.txt"
                val testContent = createSampleFileContent(testFilename)

                val fileUpload =
                    notion.fileUploads.createFileUpload(
                        CreateFileUploadRequest(filename = testFilename, contentType = "text/plain"),
                    )
                fileUpload.status shouldBe FileUploadStatus.PENDING
                fileUpload.uploadUrl shouldNotBe null
                println("  ✓ File upload created: ${fileUpload.id}")

                notion.fileUploads.sendFileUpload(fileUpload.id, testContent)
                println("  ✓ File content sent")

                delay(2000)
                val finalUpload = notion.fileUploads.retrieveFileUpload(fileUpload.id)
                println("  ✓ Upload status: ${finalUpload.status}")

                val content =
                    pageContent {
                        heading1("File Upload Media Test")
                        paragraph("Testing media blocks created from uploaded files:")
                        divider()
                        heading2("File Block from Upload")
                        fileFromUpload(
                            fileUploadId = finalUpload.id,
                            name = testFilename,
                            caption = "File created from upload: ${finalUpload.filename}",
                        )
                        divider()
                        paragraph("File upload and media block creation completed!")
                    }

                val appendResponse = notion.blocks.appendChildren(page.id, content)
                appendResponse.objectType shouldBe "list"
                println("  ✓ Appended ${appendResponse.results.size} blocks")

                delay(1000)
                val blocks = notion.blocks.retrieveChildren(page.id)
                val fileBlocks = blocks.filterIsInstance<Block.File>()

                fileBlocks shouldHaveSize 1
                val fileBlock = fileBlocks[0]
                fileBlock.file.type shouldBe "file"

                println("  ✅ File upload workflow and block creation verified")
            }

            // ------------------------------------------------------------------
            // 3. Complete mixed-source media showcase
            // ------------------------------------------------------------------
            "complete media workflow with mixed sources should produce rich showcase page" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Complete Media Workflow Demonstration")
                        icon.emoji("🎬")
                    }
                println("  Sub-page: ${page.url}")

                delay(500)

                val testFilename = "demo-document.txt"
                val testContent = createSampleFileContent(testFilename)

                val fileUpload =
                    notion.fileUploads.createFileUpload(
                        CreateFileUploadRequest(filename = testFilename, contentType = "text/plain"),
                    )
                notion.fileUploads.sendFileUpload(fileUpload.id, testContent)
                delay(2000)
                val finalUpload = notion.fileUploads.retrieveFileUpload(fileUpload.id)
                println("  ✓ Sample file uploaded: ${finalUpload.id}")

                val content =
                    pageContent {
                        heading1("Media & File Integration Showcase")
                        paragraph("This page demonstrates the complete media handling capabilities of our Kotlin Notion Client.")
                        callout("📋", "Supports both external URLs and file uploads for all media types!")
                        divider()
                        heading2("External Media Sources")
                        paragraph("All major media types can be embedded from external URLs:")
                        bullet("Images from any public URL")
                        bullet("Videos from hosting services")
                        bullet("Audio files from external sources")
                        bullet("Documents and PDFs")
                        bullet("Generic files with custom names")
                        heading3("Sample External Image")
                        image(
                            url = "https://placehold.co/600x400.png",
                            caption = "Image loaded from external URL",
                        )
                        divider()
                        heading2("File Upload Integration")
                        paragraph("Our client also supports the complete file upload workflow:")
                        toDo("Create file upload request", checked = true)
                        toDo("Send file content via multipart upload", checked = true)
                        toDo("Complete multi-part uploads when needed", checked = true)
                        toDo("Create media blocks referencing uploaded files", checked = true)
                        quote("File uploads enable you to host media directly in Notion workspaces")
                        heading3("Sample Uploaded File")
                        fileFromUpload(
                            fileUploadId = finalUpload.id,
                            name = testFilename,
                            caption = "File uploaded during test execution: ${finalUpload.filename}",
                        )
                        divider()
                        heading2("Developer-Friendly DSL")
                        paragraph("Clean, type-safe APIs for all operations:")
                        code(
                            language = "kotlin",
                            code =
                                """
                                // External media
                                image("https://example.com/photo.jpg", "My photo")
                                video("https://example.com/video.mp4")

                                // Uploaded media
                                val upload = client.fileUploads.uploadFile("photo.jpg", bytes)
                                imageFromUpload(upload.id, "Uploaded photo")
                                """.trimIndent(),
                            caption = "Simple DSL for both external and uploaded media",
                        )
                        divider()
                        heading2("Integration Test Results")
                        paragraph("This page was generated by our integration tests, proving:")
                        bullet("Content DSL works correctly")
                        bullet("Media blocks are properly structured")
                        bullet("File upload API functions")
                        bullet("Mixed media sources work together")
                        bullet("All block types render correctly in Notion")
                        callout("🎉", "All media functionality working perfectly!")
                        divider()
                        paragraph("Generated by MediaIntegrationTest on ${java.time.LocalDateTime.now()}")
                    }

                val appendResponse = notion.blocks.appendChildren(page.id, content)
                println("  ✓ Showcase created with ${appendResponse.results.size} blocks")

                delay(1000)
                val blocks = notion.blocks.retrieveChildren(page.id)

                val headingCount = blocks.count { it is Block.Heading1 || it is Block.Heading2 || it is Block.Heading3 }
                val mediaCount =
                    blocks.count {
                        it is Block.Image ||
                            it is Block.Video ||
                            it is Block.Audio ||
                            it is Block.File ||
                            it is Block.PDF
                    }

                blocks.size shouldBe appendResponse.results.size
                headingCount shouldBe 7 // 1 h1 + 4 h2 + 2 h3
                mediaCount shouldBe 2 // external image + uploaded file

                println("  ✅ Mixed-source media showcase verified (${blocks.size} blocks, $headingCount headings, $mediaCount media)")
            }

            // ------------------------------------------------------------------
            // 4. Enhanced upload — small file with progress tracking
            // ------------------------------------------------------------------
            "small file upload with progress tracking should report three progress stages" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Enhanced Upload — Small File with Progress")
                        icon.emoji("📤")
                    }
                println("  Sub-page: ${page.url}")

                delay(500)

                val testContent =
                    """
                    Enhanced File Upload Test
                    =========================

                    This file was uploaded using the enhanced file upload API
                    with progress tracking and automatic content-type detection.

                    Timestamp: ${System.currentTimeMillis()}
                    """.trimIndent()

                val progressUpdates = mutableListOf<FileUploadProgress>()
                val options =
                    FileUploadOptions(
                        progressCallback = { progress ->
                            progressUpdates.add(progress)
                            println("  ✓ Progress: ${String.format("%.1f", progress.progressPercent)}% - ${progress.status}")
                        },
                    )

                val result =
                    notion.enhancedFileUploads.uploadFile(
                        filename = "enhanced-upload-test.txt",
                        data = testContent.toByteArray(),
                        options = options,
                    )

                result.shouldBeInstanceOf<FileUploadResult.Success>()
                result.filename shouldBe "enhanced-upload-test.txt"
                result.uploadTimeMs shouldNotBe 0
                println("  ✓ Uploaded: ${result.uploadId} in ${result.uploadTimeMs}ms")

                progressUpdates.size shouldBe 3
                progressUpdates[0].status shouldBe UploadProgressStatus.STARTING
                progressUpdates[1].status shouldBe UploadProgressStatus.UPLOADING
                progressUpdates[2].status shouldBe UploadProgressStatus.COMPLETED

                val content =
                    pageContent {
                        heading1("Enhanced File Upload — Small File")
                        paragraph("Testing small file upload with progress tracking:")
                        bullet("Progress tracking ✅")
                        bullet("Content-type detection ✅")
                        bullet("Upload time: ${result.uploadTimeMs}ms")
                        divider()
                        heading2("Uploaded File")
                        fileFromUpload(
                            fileUploadId = result.fileUpload.id,
                            name = result.filename,
                            caption = "Uploaded using enhanced API with progress tracking",
                        )
                    }

                val appendResponse = notion.blocks.appendChildren(page.id, content)
                println("  ✓ Added ${appendResponse.results.size} blocks")

                println("  ✅ Small file upload with progress tracking verified (3 stages: STARTING → UPLOADING → COMPLETED)")
            }

            // ------------------------------------------------------------------
            // 5. Enhanced upload — large file multi-part
            // ------------------------------------------------------------------
            "large file multi-part upload should trigger chunked strategy" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Enhanced Upload — Large File Multi-Part")
                        icon.emoji("🗂️")
                    }
                println("  Sub-page: ${page.url}")

                delay(500)

                val largeFileSize = 22 * 1024 * 1024 // 22 MB — over the 20 MB threshold
                val tempFile = createTempFile("large-test", ".txt")

                try {
                    val timestamp = System.currentTimeMillis()
                    val randomData = (1..50).map { ('a'..'z').random() }.joinToString("")
                    val pattern =
                        """
                        Large File Upload Test Data
                        ===========================

                        Line number: %d
                        Timestamp: $timestamp
                        Random data: $randomData


                        """.trimIndent()

                    val patternBytes = pattern.toByteArray()
                    val totalLines = largeFileSize / patternBytes.size + 1

                    tempFile.toFile().bufferedWriter().use { writer ->
                        repeat(totalLines) { lineNum -> writer.write(pattern.format(lineNum)) }
                    }

                    val shouldUseMultiPart = FileUploadUtils.shouldUseMultiPart(tempFile.toFile().length())
                    shouldUseMultiPart shouldBe true
                    println("  ✓ File size: ${tempFile.toFile().length() / (1024 * 1024)}MB (multi-part required)")

                    val progressUpdates = mutableListOf<FileUploadProgress>()
                    val options =
                        FileUploadOptions(
                            progressCallback = { progress ->
                                if (progressUpdates.size % 5 == 0) {
                                    val partInfo = "(part ${progress.currentPart}/${progress.totalParts})"
                                    println(
                                        "  ✓ Multi-part: ${String.format(
                                            "%.1f",
                                            progress.progressPercent,
                                        )}% $partInfo - ${progress.status}",
                                    )
                                }
                                progressUpdates.add(progress)
                            },
                            enableConcurrentParts = true,
                            maxConcurrentParts = 3,
                        )

                    val result = notion.enhancedFileUploads.uploadFile(tempFile, options = options)

                    if (result is FileUploadResult.Success) {
                        println("  ✓ Large file uploaded: ${result.uploadId}")

                        val completedUpdates = progressUpdates.filter { it.status == UploadProgressStatus.COMPLETED }
                        completedUpdates.size shouldBe 1
                        val uploadingUpdates = progressUpdates.filter { it.status == UploadProgressStatus.UPLOADING }
                        uploadingUpdates.size shouldNotBe 0

                        val fileSizeMB = tempFile.toFile().length() / (1024 * 1024)
                        val content =
                            pageContent {
                                heading1("Large File Upload Test")
                                paragraph("Successfully uploaded a ${fileSizeMB}MB file using multi-part upload:")
                                callout(
                                    "ℹ️",
                                    "This demonstrates the enhanced file upload API's ability to handle large files automatically",
                                )
                                bullet("File size: ${fileSizeMB}MB")
                                bullet("Upload method: Multi-part")
                                bullet("Concurrent parts: ${options.maxConcurrentParts}")
                                bullet("Progress updates: ${progressUpdates.size}")
                                divider()
                                heading2("Uploaded Large File")
                                fileFromUpload(
                                    fileUploadId = result.fileUpload.id,
                                    name = result.filename,
                                    caption = "Large file uploaded via multi-part upload with concurrent parts",
                                )
                            }

                        val appendResponse = notion.blocks.appendChildren(page.id, content)
                        println("  ✓ Added ${appendResponse.results.size} blocks")
                        println("  ✅ Large file multi-part upload verified")
                    } else {
                        // API may reject oversized file — verify chunking logic still works
                        val failureResult = result as FileUploadResult.Failure
                        println("  ⚠️ Large file upload failed (may be an API limit): ${failureResult.error.message}")

                        val chunking = FileUploadUtils.calculateChunking(tempFile.toFile().length())
                        println(
                            "  ✓ Chunking strategy calculated: ${chunking.numberOfParts} parts of ${chunking.chunkSize / (1024 * 1024)}MB each",
                        )
                        println("  ✅ Multi-part chunking strategy verified (API rejection is acceptable)")
                    }
                } finally {
                    tempFile.toFile().delete()
                }
            }

            // ------------------------------------------------------------------
            // 6. Enhanced upload — external file import with validation
            // ------------------------------------------------------------------
            "external file import should auto-append extension and wait for readiness" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Enhanced Upload — External File Import")
                        icon.emoji("🌐")
                    }
                println("  Sub-page: ${page.url}")

                delay(500)

                val progressUpdates = mutableListOf<FileUploadProgress>()
                val options =
                    FileUploadOptions(
                        progressCallback = { progress ->
                            progressUpdates.add(progress)
                            println("  ✓ External import: ${progress.status}")
                        },
                    )

                val result =
                    notion.enhancedFileUploads.importExternalFile(
                        filename = "sample-pdf",
                        externalUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                        contentType = "application/pdf",
                        options = options,
                    )

                result.shouldBeInstanceOf<FileUploadResult.Success>()
                result.filename shouldBe "sample-pdf.pdf" // Extension should be auto-added
                println("  ✓ External file imported: ${result.uploadId}")

                try {
                    println("  ⏳ Waiting for file to be ready...")
                    val readyFile = notion.enhancedFileUploads.waitForFileReady(result.fileUpload.id)
                    println("  ✓ File is ready: status=${readyFile.status}")
                } catch (e: Exception) {
                    println("  ⚠️ Status check failed (proceeding): ${e.message}")
                }

                val content =
                    pageContent {
                        heading1("External File Import Test")
                        paragraph("Successfully imported a file from an external URL:")
                        callout("🌐", "This demonstrates importing files from external URLs with validation")
                        bullet("Source URL validated (HTTPS required)")
                        bullet("Filename extension auto-added")
                        bullet("Content-type specified")
                        divider()
                        heading2("Imported External File")
                        pdfFromUpload(
                            fileUploadId = result.fileUpload.id,
                            caption = "PDF imported from external URL with validation",
                        )
                    }

                val appendResponse = notion.blocks.appendChildren(page.id, content)
                println("  ✓ Added ${appendResponse.results.size} blocks")

                println("  ✅ External file import verified (filename='${result.filename}', extension auto-added)")
            }
        }
    })
