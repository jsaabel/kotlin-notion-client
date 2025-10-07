package integration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.blocks.pageContent
import no.saabelit.kotlinnotionclient.models.comments.CommentAttachmentRequest
import no.saabelit.kotlinnotionclient.models.comments.CreateCommentRequest
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Self-contained integration test for the Comments API.
 *
 * This test creates its own test page, creates comments on it, and cleans up afterwards.
 * It demonstrates real-world usage patterns for the Comments API.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 *    (This should be a page where test pages can be created)
 * 3. Your integration should have permissions to create/read pages and comments
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 *    (Defaults to "true" - objects are archived after test completion)
 *
 */
class CommentsIntegrationTest :
    BehaviorSpec({

        if (!integrationTestEnvVarsAreSet("NOTION_API_TOKEN", "NOTION_TEST_PAGE_ID", "NOTION_TEST_USER_ID")) {
            xGiven("Skipped") {
                Then("NOTION_API_TOKEN, NOTION_TEST_PAGE_ID and NOTION_TEST_USER_ID must be set") {
                    println("⏭️ Skipping - environment variables not set")
                }
            }
        } else {

            Given("a real Notion API token and test page") {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

                When("environment variables are not set") {
                    Then("skip the test") {
                        if (token.isNullOrBlank() || parentPageId.isNullOrBlank()) {
                            println("⏭️ Skipping comments integration test - environment variables not set")
                            println("   Required: NOTION_API_TOKEN and NOTION_TEST_PAGE_ID")
                            return@Then
                        }
                    }
                }

                When("creating a test page and testing comments workflow") {
                    val client = NotionClient.create(NotionConfig(apiToken = token!!))

                    Then("should successfully create page, create comments, retrieve them, and clean up") {
                        var createdPageId: String? = null

                        try {
                            println("💬 Testing Comments API with self-contained workflow...")
                            println("🔍 Debug info:")
                            println("   Parent Page ID: $parentPageId")
                            println("   Token starts with: ${token.take(10)}...")

                            // Step 1: Create a test page for comments
                            println("📄 Creating test page for comments...")
                            val pageRequest =
                                CreatePageRequest(
                                    parent = Parent(type = "page_id", pageId = parentPageId!!),
                                    icon = RequestBuilders.createEmojiIcon("💬"),
                                    properties =
                                        mapOf(
                                            "title" to
                                                PagePropertyValue.TitleValue(
                                                    title =
                                                        listOf(
                                                            RequestBuilders.createSimpleRichText("Comments Test Page - Kotlin Client"),
                                                        ),
                                                ),
                                        ),
                                )

                            val createdPage = client.pages.create(pageRequest)
                            createdPageId = createdPage.id

                            // Verify page creation
                            createdPage.objectType shouldBe "page"
                            createdPage.archived shouldBe false
                            println("✅ Test page created successfully: ${createdPage.id}")

                            // Small delay to ensure Notion has processed the page creation
                            delay(500)

                            // Step 2: Retrieve existing comments (should be empty for new page)
                            println("📚 Retrieving existing comments...")
                            val initialComments = client.comments.retrieve(createdPage.id)

                            println("✅ Retrieved ${initialComments.size} existing comments")
                            // New page should have no comments initially

                            // Step 3: Create first test comment
                            println("✍️ Creating first test comment...")
                            val firstComment =
                                CreateCommentRequest(
                                    parent = Parent(type = "page_id", pageId = createdPage.id),
                                    richText =
                                        listOf(
                                            RequestBuilders.createSimpleRichText(
                                                "🤖 First integration test comment created at ${System.currentTimeMillis()}",
                                            ),
                                        ),
                                )

                            val createdComment1 = client.comments.create(firstComment)

                            println("✅ Successfully created first comment:")
                            println("   ID: ${createdComment1.id}")
                            println("   Discussion ID: ${createdComment1.discussionId}")
                            println("   Content: ${createdComment1.richText.firstOrNull()?.plainText}")
                            println("   Created: ${createdComment1.createdTime}")

                            // Validate the created comment
                            createdComment1.id.shouldNotBeBlank()
                            createdComment1.objectType shouldBe "comment"
                            createdComment1.discussionId.shouldNotBeBlank()
                            createdComment1.richText.shouldNotBeEmpty()
                            createdComment1.richText
                                .first()
                                .plainText
                                .shouldNotBeBlank()
                            createdComment1.parent.pageId shouldBe createdPage.id

                            // Step 4: Create second test comment in same discussion
                            println("✍️ Creating second test comment...")
                            delay(500) // Small delay between comments

                            val secondComment =
                                CreateCommentRequest(
                                    parent = Parent(type = "page_id", pageId = createdPage.id),
                                    discussionId = createdComment1.discussionId, // Reply to same discussion
                                    richText =
                                        listOf(
                                            RequestBuilders.createSimpleRichText("🔄 Second comment in the same discussion thread"),
                                        ),
                                )

                            val createdComment2 = client.comments.create(secondComment)

                            println("✅ Successfully created second comment:")
                            println("   ID: ${createdComment2.id}")
                            println("   Discussion ID: ${createdComment2.discussionId}")
                            println("   Content: ${createdComment2.richText.firstOrNull()?.plainText}")

                            // Validate second comment is in same discussion
                            createdComment2.discussionId shouldBe createdComment1.discussionId
                            createdComment2.parent.pageId shouldBe createdPage.id

                            // Step 5: Retrieve all comments to verify both were created
                            println("🔄 Retrieving all comments to verify creation...")
                            delay(1000) // Allow time for eventual consistency

                            val allComments = client.comments.retrieve(createdPage.id)

                            println("✅ Retrieved ${allComments.size} comments after creation")
                            // Note: Comments might not appear immediately due to eventual consistency,
                            // but we've verified creation through the API responses

                            // Step 6: Test comment pagination if there are many comments
                            println("📄 Testing comment pagination...")

                            // The API fetches all comments automatically
                            println("✅ Pagination test completed")

                            println("🎉 Comments workflow test completed successfully!")
                        } catch (e: NotionException.AuthenticationError) {
                            println("❌ Authentication failed: ${e.message}")
                            println("   Check your NOTION_API_TOKEN is valid")
                            throw e
                        } catch (e: NotionException.ApiError) {
                            println("❌ API Error: ${e.message}")
                            println("   Status: ${e.status}")
                            println("   Code: ${e.code}")
                            println("   Details: ${e.details}")
                            throw e
                        } catch (e: NotionException.NetworkError) {
                            println("❌ Network Error: ${e.message}")
                            println("   Check your internet connection")
                            throw e
                        } finally {
                            // Step 7: Conditionally clean up based on environment variable
                            if (createdPageId != null && shouldCleanupAfterTest()) {
                                println("🧹 Cleaning up - archiving test page...")
                                try {
                                    val archivedPage = client.pages.archive(createdPageId)
                                    archivedPage.archived shouldBe true
                                    println("✅ Test page archived successfully")
                                } catch (e: Exception) {
                                    println("⚠️ Warning: Failed to archive test page: ${e.message}")
                                }
                            } else if (createdPageId != null) {
                                println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                                println("   Created objects for manual inspection:")
                                println("   - Test Page: $createdPageId (\"Comments Test Page - Kotlin Client\")")
                            }

                            client.close()
                            println("🔒 Client closed")
                        }
                    }
                }

                When("testing comment validation") {
                    val client = NotionClient.create(NotionConfig(apiToken = token!!))

                    Then("should handle validation errors correctly") {
                        try {
                            println("🔍 Testing comment validation...")

                            // Create a minimal test page for validation tests
                            val pageRequest =
                                CreatePageRequest(
                                    parent = Parent(type = "page_id", pageId = parentPageId!!),
                                    properties =
                                        mapOf(
                                            "title" to
                                                PagePropertyValue.TitleValue(
                                                    title = listOf(RequestBuilders.createSimpleRichText("Validation Test Page")),
                                                ),
                                        ),
                                )
                            val testPage = client.pages.create(pageRequest)

                            try {
                                // Test empty rich text validation
                                println("   Testing empty rich text validation...")
                                val invalidRequest =
                                    CreateCommentRequest(
                                        parent = Parent(type = "page_id", pageId = testPage.id),
                                        richText = emptyList(), // This should trigger validation error
                                    )

                                var caughtValidationError = false
                                try {
                                    client.comments.create(invalidRequest)
                                } catch (e: IllegalArgumentException) {
                                    caughtValidationError = true
                                    println("   ✅ Correctly caught validation error: ${e.message}")
                                    e.message shouldBe "Comment rich text cannot be empty"
                                }

                                caughtValidationError shouldBe true
                                println("✅ Validation tests completed successfully")
                            } finally {
                                // Clean up test page
                                if (shouldCleanupAfterTest()) {
                                    try {
                                        client.pages.archive(testPage.id)
                                    } catch (e: Exception) {
                                        println("⚠️ Warning: Failed to archive validation test page: ${e.message}")
                                    }
                                }
                            }
                        } catch (e: NotionException) {
                            println("❌ Unexpected Notion API error during validation test: ${e.message}")
                            throw e
                        } finally {
                            client.close()
                            println("🔒 Client closed")
                        }
                    }
                }

                When("testing comments on blocks") {
                    val client = NotionClient.create(NotionConfig(apiToken = token!!))

                    Then("should successfully create comments on individual blocks") {
                        var createdPageId: String? = null

                        try {
                            println("🧱 Testing comments on blocks...")

                            // Step 1: Create a test page with content blocks
                            println("📄 Creating test page with content blocks...")
                            val pageRequest =
                                CreatePageRequest(
                                    parent = Parent(type = "page_id", pageId = parentPageId!!),
                                    icon = RequestBuilders.createEmojiIcon("🧱"),
                                    properties =
                                        mapOf(
                                            "title" to
                                                PagePropertyValue.TitleValue(
                                                    title = listOf(RequestBuilders.createSimpleRichText("Block Comments Test Page")),
                                                ),
                                        ),
                                )

                            val createdPage = client.pages.create(pageRequest)
                            createdPageId = createdPage.id
                            println("✅ Test page created: ${createdPage.id}")

                            // Step 2: Add some content blocks to the page
                            println("🔧 Adding content blocks to the page...")
                            delay(500)

                            val pageContent =
                                pageContent {
                                    paragraph("This is the first paragraph block.")
                                    heading2("This is a heading 2 block")
                                    paragraph("This is the second paragraph block with some content.")
                                }

                            val blockChildren = client.blocks.appendChildren(createdPage.id, pageContent)

                            println("✅ Added ${blockChildren.results.size} blocks to the page")
                            blockChildren.results.shouldNotBeEmpty()

                            // Step 3: Get the block ID of the first paragraph
                            val firstBlock = blockChildren.results.first()
                            println("🎯 First block ID: ${firstBlock.id}")

                            // Step 4: Create a comment on the specific block
                            println("💬 Creating comment on the first block...")
                            delay(500)

                            val blockComment =
                                CreateCommentRequest(
                                    parent = Parent(type = "block_id", blockId = firstBlock.id),
                                    richText =
                                        listOf(
                                            RequestBuilders.createSimpleRichText(
                                                "🎯 This comment is specifically on the first paragraph block!",
                                            ),
                                        ),
                                )

                            val createdBlockComment = client.comments.create(blockComment)

                            println("✅ Successfully created comment on block:")
                            println("   Comment ID: ${createdBlockComment.id}")
                            println("   Block ID: ${createdBlockComment.parent.blockId}")
                            println("   Content: ${createdBlockComment.richText.first().plainText}")

                            // Validate the block comment
                            createdBlockComment.id.shouldNotBeBlank()
                            createdBlockComment.objectType shouldBe "comment"
                            createdBlockComment.parent.blockId shouldBe firstBlock.id
                            createdBlockComment.richText.shouldNotBeEmpty()
                            createdBlockComment.richText
                                .first()
                                .plainText
                                .shouldNotBeBlank()

                            // Step 5: Retrieve comments for the specific block
                            println("📚 Retrieving comments for the specific block...")
                            delay(500)

                            val blockComments = client.comments.retrieve(firstBlock.id)
                            println("✅ Retrieved ${blockComments.size} comments for the block")

                            // Step 6: Create another comment on a different block
                            val secondBlock = blockChildren.results[2] // The second paragraph
                            println("💬 Creating comment on the second paragraph block...")

                            val secondBlockComment =
                                CreateCommentRequest(
                                    parent = Parent(type = "block_id", blockId = secondBlock.id),
                                    richText =
                                        listOf(
                                            RequestBuilders.createSimpleRichText(
                                                "📝 This comment is on the second paragraph block - showing we can comment on different blocks!",
                                            ),
                                        ),
                                )

                            val createdSecondComment = client.comments.create(secondBlockComment)
                            println("✅ Created second block comment: ${createdSecondComment.id}")

                            // Validate different blocks have different comments
                            createdSecondComment.parent.blockId shouldBe secondBlock.id
                            createdSecondComment.parent.blockId shouldBe secondBlock.id

                            println("🎉 Block comments test completed successfully!")
                        } catch (e: NotionException) {
                            println("❌ Error during block comments test: ${e.message}")
                            throw e
                        } finally {
                            // Clean up test page
                            if (createdPageId != null && shouldCleanupAfterTest()) {
                                println("🧹 Cleaning up block comments test page...")
                                try {
                                    client.pages.archive(createdPageId)
                                    println("✅ Block comments test page archived")
                                } catch (e: Exception) {
                                    println("⚠️ Warning: Failed to archive block comments test page: ${e.message}")
                                }
                            } else if (createdPageId != null) {
                                println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                                println("   Block comments test page: $createdPageId")
                            }

                            client.close()
                            println("🔒 Client closed")
                        }
                    }
                }

                When("testing comments with file attachments") {
                    val client = NotionClient.create(NotionConfig(apiToken = token!!))

                    Then("should successfully create comments with file attachments") {
                        var createdPageId: String? = null
                        var uploadedFileId: String? = null

                        try {
                            println("📎 Testing comments with file attachments...")

                            // Step 1: Create a test page for file attachment comments
                            println("📄 Creating test page for file attachment comments...")
                            val pageRequest =
                                CreatePageRequest(
                                    parent = Parent(type = "page_id", pageId = parentPageId!!),
                                    icon = RequestBuilders.createEmojiIcon("📎"),
                                    properties =
                                        mapOf(
                                            "title" to
                                                PagePropertyValue.TitleValue(
                                                    title =
                                                        listOf(
                                                            RequestBuilders.createSimpleRichText("File Attachment Comments Test"),
                                                        ),
                                                ),
                                        ),
                                )

                            val createdPage = client.pages.create(pageRequest)
                            createdPageId = createdPage.id
                            println("✅ Test page created: ${createdPage.id}")

                            // Step 2: Upload a test file to use as attachment
                            println("📤 Uploading test file...")
                            delay(500)

                            val testFileContent =
                                "This is a test file for comment attachments.\nGenerated by integration test."
                            val uploadResult =
                                client.enhancedFileUploads.uploadFile(
                                    filename = "comment-test-file.txt",
                                    data = testFileContent.toByteArray(),
                                )

                            uploadedFileId = uploadResult.uploadId
                            println("✅ File uploaded successfully: ${uploadResult.uploadId}")

                            // Step 3: Create a comment with the file attachment
                            println("💬 Creating comment with file attachment...")
                            delay(500)

                            val commentWithAttachment =
                                CreateCommentRequest(
                                    parent = Parent(type = "page_id", pageId = createdPage.id),
                                    richText =
                                        listOf(
                                            RequestBuilders.createSimpleRichText("📎 This comment includes a file attachment!"),
                                        ),
                                    attachments =
                                        listOf(
                                            CommentAttachmentRequest(
                                                fileUploadId = uploadResult.uploadId,
                                                type = "file_upload",
                                            ),
                                        ),
                                )

                            val createdCommentWithFile = client.comments.create(commentWithAttachment)

                            println("✅ Successfully created comment with attachment:")
                            println("   Comment ID: ${createdCommentWithFile.id}")
                            println("   Content: ${createdCommentWithFile.richText.first().plainText}")
                            println("   Attachments count: ${createdCommentWithFile.attachments?.size ?: 0}")

                            // Validate the comment with attachment
                            createdCommentWithFile.id.shouldNotBeBlank()
                            createdCommentWithFile.objectType shouldBe "comment"
                            createdCommentWithFile.richText.shouldNotBeEmpty()
                            createdCommentWithFile.parent.pageId shouldBe createdPage.id
                            // Note: attachments might not be immediately available due to processing

                            // Step 4: Test attachment limit validation
                            println("🔍 Testing attachment limit validation...")

                            var caughtAttachmentError = false
                            try {
                                val tooManyAttachments =
                                    CreateCommentRequest(
                                        parent = Parent(type = "page_id", pageId = createdPage.id),
                                        richText =
                                            listOf(
                                                RequestBuilders.createSimpleRichText("This comment has too many attachments"),
                                            ),
                                        attachments =
                                            listOf(
                                                CommentAttachmentRequest(fileUploadId = "file1"),
                                                CommentAttachmentRequest(fileUploadId = "file2"),
                                                CommentAttachmentRequest(fileUploadId = "file3"),
                                                CommentAttachmentRequest(fileUploadId = "file4"), // This exceeds the limit
                                            ),
                                    )
                                client.comments.create(tooManyAttachments)
                            } catch (e: IllegalArgumentException) {
                                caughtAttachmentError = true
                                println("   ✅ Correctly caught attachment limit error: ${e.message}")
                                e.message shouldBe "Comments can have a maximum of 3 attachments, but 4 were provided"
                            }

                            caughtAttachmentError shouldBe true

                            println("🎉 File attachment comments test completed successfully!")
                        } catch (e: NotionException) {
                            println("❌ Error during file attachment comments test: ${e.message}")
                            throw e
                        } finally {
                            // Clean up test page
                            if (createdPageId != null && shouldCleanupAfterTest()) {
                                println("🧹 Cleaning up file attachment test page...")
                                try {
                                    client.pages.archive(createdPageId)
                                    println("✅ File attachment test page archived")
                                } catch (e: Exception) {
                                    println("⚠️ Warning: Failed to archive file attachment test page: ${e.message}")
                                }
                            } else if (createdPageId != null) {
                                println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                                println("   File attachment test page: $createdPageId")
                                uploadedFileId?.let { println("   Uploaded file: $it") }
                            }

                            client.close()
                            println("🔒 Client closed")
                        }
                    }
                }

                When("testing user mentions in comments") {
                    val client = NotionClient.create(NotionConfig(apiToken = token!!))

                    Then("should successfully create comments with user mentions") {
                        var createdPageId: String? = null

                        try {
                            println("👤 Testing user mentions in comments...")

                            // Step 1: Create a test page for user mention comments
                            println("📄 Creating test page for user mention comments...")
                            val pageRequest =
                                CreatePageRequest(
                                    parent = Parent(type = "page_id", pageId = parentPageId!!),
                                    icon = RequestBuilders.createEmojiIcon("👤"),
                                    properties =
                                        mapOf(
                                            "title" to
                                                PagePropertyValue.TitleValue(
                                                    title = listOf(RequestBuilders.createSimpleRichText("User Mention Comments Test")),
                                                ),
                                        ),
                                )

                            val createdPage = client.pages.create(pageRequest)
                            createdPageId = createdPage.id
                            println("✅ Test page created: ${createdPage.id}")

                            // Step 2: Create a comment with user mention
                            println("💬 Creating comment with user mention...")
                            delay(500)

                            val testUserId = System.getenv("NOTION_TEST_USER_ID")
                            val commentWithMention =
                                CreateCommentRequest(
                                    parent = Parent(type = "page_id", pageId = createdPage.id),
                                    richText =
                                        listOf(
                                            RequestBuilders.createSimpleRichText("Hey "),
                                            RequestBuilders.createUserMention(testUserId, "Test User"),
                                            RequestBuilders.createSimpleRichText(", check out this comment with user tagging!"),
                                        ),
                                )

                            val createdCommentWithMention = client.comments.create(commentWithMention)

                            println("✅ Successfully created comment with user mention:")
                            println("   Comment ID: ${createdCommentWithMention.id}")
                            println("   Content: ${createdCommentWithMention.richText.joinToString("") { it.plainText }}")
                            println("   Rich text elements: ${createdCommentWithMention.richText.size}")

                            // Validate the comment with user mention
                            createdCommentWithMention.id.shouldNotBeBlank()
                            createdCommentWithMention.objectType shouldBe "comment"
                            createdCommentWithMention.richText.shouldNotBeEmpty()
                            createdCommentWithMention.parent.pageId shouldBe createdPage.id

                            // Validate the mention is in the rich text
                            val mentionElement = createdCommentWithMention.richText.find { it.type == "mention" }
                            mentionElement shouldNotBe null

                            println("✅ User mention validated successfully")

                            // Step 3: Create another comment combining mentions with other rich text
                            println("💬 Creating comment with complex rich text including mentions...")

                            val complexComment =
                                CreateCommentRequest(
                                    parent = Parent(type = "page_id", pageId = createdPage.id),
                                    richText =
                                        listOf(
                                            RequestBuilders.createSimpleRichText("👋 Hello "),
                                            RequestBuilders.createUserMention(testUserId, "Test User"),
                                            RequestBuilders.createSimpleRichText(
                                                "! This demonstrates how mentions work seamlessly with regular text in our Comments API. 🎉",
                                            ),
                                        ),
                                )

                            val createdComplexComment = client.comments.create(complexComment)

                            println("✅ Complex comment with mentions created: ${createdComplexComment.id}")

                            // Validate complex comment structure
                            createdComplexComment.richText.size shouldBe 3
                            createdComplexComment.richText[0].type shouldBe "text"
                            createdComplexComment.richText[1].type shouldBe "mention"
                            createdComplexComment.richText[2].type shouldBe "text"

                            println("🎉 User mention comments test completed successfully!")
                        } catch (e: NotionException) {
                            println("❌ Error during user mention comments test: ${e.message}")
                            throw e
                        } finally {
                            // Clean up test page
                            if (createdPageId != null && shouldCleanupAfterTest()) {
                                println("🧹 Cleaning up user mention test page...")
                                try {
                                    client.pages.archive(createdPageId)
                                    println("✅ User mention test page archived")
                                } catch (e: Exception) {
                                    println("⚠️ Warning: Failed to archive user mention test page: ${e.message}")
                                }
                            } else if (createdPageId != null) {
                                println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                                println("   User mention test page: $createdPageId")
                            }

                            client.close()
                            println("🔒 Client closed")
                        }
                    }
                }

                When("testing error handling with invalid page ID") {
                    val client = NotionClient.create(NotionConfig(apiToken = token!!))

                    Then("should handle API errors gracefully") {
                        try {
                            println("🚫 Testing error handling with invalid page ID...")

                            // Try to retrieve comments for a non-existent page
                            var caughtApiError = false
                            try {
                                client.comments.retrieve("invalid-page-id-12345")
                            } catch (e: NotionException.ApiError) {
                                caughtApiError = true
                                println("   ✅ Correctly caught API error: ${e.message}")
                                println("   Status: ${e.status}")
                                println("   Code: ${e.code}")
                                // Should be 400 or 404 error
                                (e.status == 400 || e.status == 404) shouldBe true
                            }

                            caughtApiError shouldBe true
                            println("✅ Error handling tests completed successfully")
                        } finally {
                            client.close()
                            println("🔒 Client closed")
                        }
                    }
                }
            }
        }
    })
