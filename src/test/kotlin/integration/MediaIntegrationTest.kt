@file:Suppress("UnusedVariable")

package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.blocks.Block
import no.saabelit.kotlinnotionclient.models.blocks.pageContent
import no.saabelit.kotlinnotionclient.models.files.CreateFileUploadRequest
import no.saabelit.kotlinnotionclient.models.files.FileUploadStatus
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Integration tests for media block functionality and file uploads.
 *
 * This test validates:
 * 1. File upload API (create, send, complete workflow)
 * 2. Media block creation using external URLs
 * 3. Media block creation using uploaded files
 * 4. End-to-end workflow from file upload to block creation
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update pages and blocks
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 *
 * Run with: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi")
class MediaIntegrationTest :
    StringSpec({

        // Helper function to check if cleanup should be performed after tests
        fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"

        // Helper function to create sample file content for testing
        fun createSampleFileContent(filename: String): ByteArray =
            when {
                filename.endsWith(".txt") -> "Sample text file content for testing\nLine 2\nLine 3".toByteArray()
                filename.endsWith(".json") -> """{"test": "data", "number": 42}""".toByteArray()
                filename.endsWith(".csv") -> "name,age,city\nJohn,30,New York\nJane,25,London".toByteArray()
                else -> "Binary file content placeholder".toByteArray()
            }

        "Should create media blocks using external URLs" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Step 1: Create test page
                    println("📄 Creating test page for external media blocks...")
                    val pageRequest =
                        CreatePageRequest(
                            parent =
                                Parent(
                                    type = "page_id",
                                    pageId = parentPageId,
                                ),
                            icon = RequestBuilders.createEmojiIcon("🖼️"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title = listOf(RequestBuilders.createSimpleRichText("Media Blocks - External URLs Test")),
                                        ),
                                ),
                        )

                    val createdPage = client.pages.create(pageRequest)
                    println("✅ Test page created: ${createdPage.id}")

                    delay(500) // Give Notion time to process

                    // Step 2: Create content with external media blocks
                    println("🖼️ Creating media blocks with external URLs...")

                    val pageContent =
                        pageContent {
                            heading1("📱 External Media Blocks Test")

                            paragraph("Testing various media block types with external URLs:")

                            divider()

                            heading2("🖼️ Image Block")
                            image(
                                url = "https://placehold.co/600x400.png",
                                caption = "Test image from placeholder service",
                            )

                            heading2("🎥 Video Block")
                            video(
                                url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                                caption = "Sample YouTube video",
                            )

                            heading2("🎵 Audio Block")
                            audio(
                                url = "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav",
                                caption = "Sample audio file",
                            )

                            heading2("📄 File Block")
                            file(
                                url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                                name = "sample-document.pdf",
                                caption = "Sample PDF file",
                            )

                            heading2("📋 PDF Block")
                            pdf(
                                url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                                caption = "Sample PDF as PDF block",
                            )

                            divider()
                            paragraph("All external media blocks created successfully!")
                        }

                    // Step 3: Append content and verify
                    println("📤 Appending media content to Notion...")
                    val appendResponse = client.blocks.appendChildren(createdPage.id, pageContent)
                    appendResponse.objectType shouldBe "list"

                    println("✅ Media content appended: ${appendResponse.results.size} blocks")

                    // Step 4: Retrieve and verify specific media blocks
                    delay(1000)
                    println("🔍 Verifying media blocks in Notion...")

                    val blocks = client.blocks.retrieveChildren(createdPage.id)

                    // Find media blocks by type
                    val imageBlocks = blocks.filterIsInstance<Block.Image>()
                    val videoBlocks = blocks.filterIsInstance<Block.Video>()
                    val audioBlocks = blocks.filterIsInstance<Block.Audio>()
                    val fileBlocks = blocks.filterIsInstance<Block.File>()
                    val pdfBlocks = blocks.filterIsInstance<Block.PDF>()

                    // Verify we have the expected media blocks
                    imageBlocks shouldHaveSize 1
                    videoBlocks shouldHaveSize 1
                    audioBlocks shouldHaveSize 1
                    fileBlocks shouldHaveSize 1
                    pdfBlocks shouldHaveSize 1

                    // Verify image block content
                    val imageBlock = imageBlocks[0]
                    imageBlock.image.type shouldBe "external"
                    imageBlock.image.external?.url shouldBe "https://placehold.co/600x400.png"
                    imageBlock.image.caption.isNotEmpty() shouldBe true

                    println("✅ All media block types created and verified successfully!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        delay(500)
                        println("🧹 Cleaning up test page...")
                        client.pages.archive(createdPage.id)
                        println("✅ Test page archived")
                    } else {
                        println("🔧 Cleanup skipped - page preserved: ${createdPage.id}")
                    }
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping external media test - missing environment variables")
            }
        }

        "Should upload file and create media blocks from uploads" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Step 1: Create test page
                    println("📄 Creating test page for file upload media blocks...")
                    val pageRequest =
                        CreatePageRequest(
                            parent =
                                Parent(
                                    type = "page_id",
                                    pageId = parentPageId,
                                ),
                            icon = RequestBuilders.createEmojiIcon("📁"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title = listOf(RequestBuilders.createSimpleRichText("Media Blocks - File Uploads Test")),
                                        ),
                                ),
                        )

                    val createdPage = client.pages.create(pageRequest)
                    println("✅ Test page created: ${createdPage.id}")

                    delay(500)

                    // Step 2: Upload a test file
                    println("📤 Testing file upload workflow...")

                    val testFilename = "test-document.txt"
                    val testContent = createSampleFileContent(testFilename)

                    // Create file upload
                    val createRequest =
                        CreateFileUploadRequest(
                            filename = testFilename,
                            contentType = "text/plain",
                        )

                    val fileUpload = client.fileUploads.createFileUpload(createRequest)
                    fileUpload.status shouldBe FileUploadStatus.PENDING
                    fileUpload.uploadUrl shouldNotBe null

                    println("✅ File upload created: ${fileUpload.id}")

                    // Send file content
                    val sentUpload = client.fileUploads.sendFileUpload(fileUpload.id, testContent)
                    println("✅ File content sent")

                    // For single-part uploads, the file should now be ready
                    // Let's retrieve it to check status
                    delay(2000) // Give Notion time to process the upload
                    val finalUpload = client.fileUploads.retrieveFileUpload(fileUpload.id)

                    println("📊 Upload status: ${finalUpload.status}")

                    // Step 3: Create media blocks using the uploaded file
                    println("🖼️ Creating media blocks from uploaded file...")

                    val pageContent =
                        pageContent {
                            heading1("📁 File Upload Media Test")

                            paragraph("Testing media blocks created from uploaded files:")

                            divider()

                            heading2("📄 File Block from Upload")
                            fileFromUpload(
                                fileUploadId = finalUpload.id,
                                name = testFilename,
                                caption = "File created from upload: ${finalUpload.filename}",
                            )

                            // Note: For actual image/video/audio files, we would use:
                            // imageFromUpload(finalUpload.id, "Uploaded image")
                            // videoFromUpload(finalUpload.id, "Uploaded video")
                            // audioFromUpload(finalUpload.id, "Uploaded audio")
                            // pdfFromUpload(finalUpload.id, "Uploaded PDF")

                            divider()
                            paragraph("File upload and media block creation completed!")
                        }

                    // Step 4: Append content and verify
                    println("📤 Appending upload-based media content...")
                    val appendResponse = client.blocks.appendChildren(createdPage.id, pageContent)
                    appendResponse.objectType shouldBe "list"

                    println("✅ Upload-based content appended: ${appendResponse.results.size} blocks")

                    // Step 5: Verify the file block was created correctly
                    delay(1000)
                    val blocks = client.blocks.retrieveChildren(createdPage.id)
                    val fileBlocks = blocks.filterIsInstance<Block.File>()

                    fileBlocks shouldHaveSize 1
                    val fileBlock = fileBlocks[0]

                    // The file block should reference our uploaded file
                    println("🔍 File block type: ${fileBlock.file.type}")
                    println("🔍 File block external: ${fileBlock.file.external}")
                    println("🔍 File block fileUpload: ${fileBlock.file.fileUpload}")
                    println("🔍 Expected upload ID: ${finalUpload.id}")

                    fileBlock.file.type shouldBe "file"
                    // Note: Notion converts file_upload blocks to regular file blocks after processing
                    // So we can't verify the upload ID in the response, but the file content is there

                    println("✅ File block created from upload verified!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        delay(500)
                        println("🧹 Cleaning up test page...")
                        client.pages.archive(createdPage.id)
                        println("✅ Test page archived")
                    } else {
                        println("🔧 Cleanup skipped - page preserved: ${createdPage.id}")
                    }
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping file upload media test - missing environment variables")
            }
        }

        "Should demonstrate complete media workflow with mixed sources" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Create a comprehensive test page showing all media capabilities
                    println("📄 Creating comprehensive media demonstration page...")

                    val pageRequest =
                        CreatePageRequest(
                            parent =
                                Parent(
                                    type = "page_id",
                                    pageId = parentPageId,
                                ),
                            icon = RequestBuilders.createEmojiIcon("🎬"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title = listOf(RequestBuilders.createSimpleRichText("Complete Media Workflow Demonstration")),
                                        ),
                                ),
                        )

                    val createdPage = client.pages.create(pageRequest)
                    println("✅ Demonstration page created: ${createdPage.id}")

                    delay(500)

                    // Upload a sample file for the demonstration
                    println("📤 Uploading sample file for demonstration...")
                    val testFilename = "demo-document.txt"
                    val testContent = createSampleFileContent(testFilename)

                    val createRequest =
                        CreateFileUploadRequest(
                            filename = testFilename,
                            contentType = "text/plain",
                        )

                    val fileUpload = client.fileUploads.createFileUpload(createRequest)
                    val sentUpload = client.fileUploads.sendFileUpload(fileUpload.id, testContent)
                    delay(2000) // Give Notion time to process the upload
                    val finalUpload = client.fileUploads.retrieveFileUpload(fileUpload.id)
                    println("✅ Sample file uploaded: ${finalUpload.id}")

                    // Create comprehensive content showcasing all media features
                    val pageContent =
                        pageContent {
                            heading1("🎬 Media & File Integration Showcase")

                            paragraph("This page demonstrates the complete media handling capabilities of our Kotlin Notion Client.")

                            callout("📋", "Supports both external URLs and file uploads for all media types!")

                            divider()

                            heading2("🌐 External Media Sources")

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

                            heading2("📁 File Upload Integration")

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

                            heading2("🛠️ Developer-Friendly DSL")

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

                            heading2("✅ Integration Test Results")

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

                    // Append the comprehensive showcase
                    println("📤 Creating comprehensive media showcase...")
                    val appendResponse = client.blocks.appendChildren(createdPage.id, pageContent)

                    println("✅ Media showcase created with ${appendResponse.results.size} blocks")

                    // Verify the showcase was created correctly
                    delay(1000)
                    val blocks = client.blocks.retrieveChildren(createdPage.id)

                    // Count different block types to verify diversity
                    val headingCount = blocks.count { it is Block.Heading1 || it is Block.Heading2 || it is Block.Heading3 }
                    val mediaCount =
                        blocks.count {
                            it is Block.Image ||
                                it is Block.Video ||
                                it is Block.Audio ||
                                it is Block.File ||
                                it is Block.PDF
                        }
                    val interactiveCount = blocks.count { it is Block.Callout || it is Block.ToDo || it is Block.Code || it is Block.Quote }

                    println("📊 Showcase statistics:")
                    println("   - Total blocks: ${blocks.size}")
                    println("   - Headings: $headingCount")
                    println("   - Media blocks: $mediaCount")
                    println("   - Interactive blocks: $interactiveCount")

                    // Basic validation
                    blocks.size shouldBe appendResponse.results.size
                    headingCount shouldBe 7 // 1 h1 + 4 h2 + 2 h3 = 7 headings
                    mediaCount shouldBe 2 // The sample image + uploaded file

                    println("✅ Media workflow demonstration completed successfully!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        delay(500)
                        println("🧹 Cleaning up demonstration page...")
                        client.pages.archive(createdPage.id)
                        println("✅ Demonstration page archived")
                    } else {
                        println("🔧 Cleanup skipped - demonstration preserved: ${createdPage.id}")
                        println("   Title: \"Complete Media Workflow Demonstration\"")
                        println("   Contains comprehensive media showcase with ${blocks.size} blocks")
                    }
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping comprehensive media test - missing environment variables")
                println("   Required: NOTION_API_TOKEN and NOTION_TEST_PAGE_ID")
            }
        }
    })
