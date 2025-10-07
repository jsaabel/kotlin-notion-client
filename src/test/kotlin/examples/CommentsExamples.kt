package examples

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Mention
import no.saabelit.kotlinnotionclient.models.blocks.pageContent
import no.saabelit.kotlinnotionclient.models.comments.CommentAttachmentRequest

/**
 * Comments API Examples
 *
 * This file contains validated examples for the Comments API, suitable for documentation.
 * Each example has been tested against the live Notion API.
 *
 * Prerequisites:
 * - Set environment variable: export NOTION_RUN_INTEGRATION_TESTS="true"
 * - Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * - Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * - Set environment variable: export NOTION_TEST_USER_ID="your_user_id" (for mention examples)
 * - Set environment variable: export NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 */
@Tags("Integration", "RequiresApi", "Examples")
class CommentsExamples :
    StringSpec({

        if (!integrationTestEnvVarsAreSet("NOTION_API_TOKEN", "NOTION_TEST_PAGE_ID", "NOTION_TEST_USER_ID")) {
            "!(Skipped) Comments examples" {
                println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true and required env vars")
            }
        } else {

            val token = System.getenv("NOTION_API_TOKEN")!!
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")!!
            val testUserId = System.getenv("NOTION_TEST_USER_ID")

            val notion = NotionClient.create(NotionConfig(apiToken = token))
            val createdPages = mutableListOf<String>()
            var testPageId: String? = null

            beforeSpec {
                // Create a shared test page for most examples
                val testPage =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Comments API Examples Test Page")
                    }
                testPageId = testPage.id
                createdPages.add(testPage.id)
                delay(500)
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    createdPages.forEach { pageId ->
                        runCatching { notion.pages.archive(pageId) }
                    }
                }
                notion.close()
            }

            "Example 1: Create a simple comment on a page" {
                // Create a comment using the DSL
                val comment =
                    notion.comments.create {
                        parent.page(testPageId!!)
                        richText {
                            text("This is a simple comment on the page")
                        }
                    }

                println("Created comment: ${comment.id}")
                println("Comment content: ${comment.richText.first().plainText}")
            }

            "Example 2: Create a comment thread using discussion_id" {
                // Create first comment using DSL
                val firstComment =
                    notion.comments.create {
                        parent.page(testPageId!!)
                        richText {
                            text("Starting a discussion thread")
                        }
                    }

                delay(500)

                // Reply to the same discussion using DSL
                val replyComment =
                    notion.comments.create {
                        parent.page(testPageId!!)
                        discussionId(firstComment.discussionId)
                        richText {
                            text("This is a reply in the same thread")
                        }
                    }

                println("Discussion ID: ${firstComment.discussionId}")
                println("Both comments share the same discussion ID: ${firstComment.discussionId == replyComment.discussionId}")
            }

            "Example 3: Rich text formatting in comments" {
                // Create a comment with rich text formatting using DSL
                val comment =
                    notion.comments.create {
                        parent.page(testPageId!!)
                        richText {
                            text("This is ")
                            bold("bold text")
                            text(" and this is ")
                            italic("italic text")
                            text(".")
                        }
                    }

                println("Comment with formatting created: ${comment.id}")
                println("Full text: ${comment.richText.joinToString("") { it.plainText }}")
            }

            "Example 4: Comment on a specific block" {
                // Create a page with blocks for this example
                val blockPage =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Block Comments Example")
                    }
                createdPages.add(blockPage.id)
                delay(500)

                // Add a block to the page
                val blocks =
                    pageContent {
                        paragraph("This is a paragraph that we'll comment on")
                    }

                val blockChildren = notion.blocks.appendChildren(blockPage.id, blocks)
                val blockId = blockChildren.results.first().id

                delay(500)

                // Create a comment on the specific block using DSL
                val comment =
                    notion.comments.create {
                        parent.block(blockId)
                        richText {
                            text("This comment is on a specific block")
                        }
                    }

                println("Created comment on block: ${comment.parent.blockId}")
                println("Comment content: ${comment.richText.first().plainText}")
            }

            "Example 5: Comments with links" {
                // Create a comment with a link using DSL
                val comment =
                    notion.comments.create {
                        parent.page(testPageId!!)
                        richText {
                            text("Check out ")
                            link("https://developers.notion.com", "Notion API docs")
                            text(" for more information.")
                        }
                    }

                println("Comment with link created: ${comment.id}")
                val linkElement = comment.richText.find { it.href != null }
                println("Link: ${linkElement?.plainText} -> ${linkElement?.href}")
            }

            "Example 6: User mentions in comments".config(enabled = !testUserId.isNullOrBlank()) {
                // Create a comment with a user mention using DSL
                val comment =
                    notion.comments.create {
                        parent.page(testPageId!!)
                        richText {
                            text("Hey ")
                            userMention(testUserId!!)
                            text(", please review this!")
                        }
                    }

                println("Comment with mention created: ${comment.id}")
                val mentionElement = comment.richText.find { it.type == "mention" }
                val mentionType =
                    when (mentionElement?.mention) {
                        is Mention.User -> "user"
                        is Mention.Date -> "date"
                        is Mention.Page -> "page"
                        else -> "unknown"
                    }
                println("Mention type: $mentionType")
            }

            "Example 7: Comments with file attachments" {
                // Upload a test file
                val fileContent = "This is a test file for comment attachments."
                val uploadResult =
                    notion.enhancedFileUploads.uploadFile(
                        filename = "test-attachment.txt",
                        data = fileContent.toByteArray(),
                    )

                delay(500)

                // Create a comment with the file attachment using DSL
                val comment =
                    notion.comments.create {
                        parent.page(testPageId!!)
                        richText {
                            text("This comment has an attached file")
                        }
                        attachment(uploadResult.uploadId)
                    }

                println("Comment with attachment created: ${comment.id}")
                println("Attachments count: ${comment.attachments?.size ?: 0}")
            }

            "Example 8: Retrieve all comments from a page" {
                delay(1000) // Allow time for comments to be indexed

                // Retrieve all comments using DSL
                val comments =
                    notion.comments.retrieve {
                        blockId(testPageId!!)
                    }

                println("Retrieved ${comments.size} comments from test page")
                comments.forEach { comment ->
                    println("- ${comment.richText.firstOrNull()?.plainText}")
                }
            }

            "Example 8b: Retrieve comments with pagination control" {
                delay(1000) // Allow time for comments to be indexed

                // Retrieve comments with page size limit using DSL
                val limitedComments =
                    notion.comments.retrieve {
                        blockId(testPageId!!)
                        pageSize(3) // Only get first 3 comments
                    }

                println("Retrieved ${limitedComments.size} comments (max 3) from test page")
                limitedComments.forEach { comment ->
                    println("- ${comment.richText.firstOrNull()?.plainText}")
                }
            }

            "Example 8c: Advanced DSL example with custom display name" {
                // Create a comment with all DSL features
                val advancedComment =
                    notion.comments.create {
                        parent.page(testPageId!!)
                        richText {
                            text("🤖 Advanced comment with ")
                            bold("multiple formatting")
                            text(", ")
                            italic("styles")
                            text(", and ")
                            code("inline code")
                            text("!")
                        }
                        displayName("Documentation Bot")
                    }

                println("Advanced comment created: ${advancedComment.id}")
                println("Display name: ${advancedComment.displayName?.resolvedName}")
            }

            "Example 9: Handle validation errors" {
                // Attempt to create a comment with empty rich text using DSL
                try {
                    notion.comments.create {
                        parent.page(testPageId!!)
                        // Empty richText block will fail validation
                        richText {
                            // intentionally empty
                        }
                    }
                } catch (e: IllegalStateException) {
                    println("Validation error caught: ${e.message}")
                }

                // Attempt to create a comment with too many attachments using DSL
                try {
                    notion.comments.create {
                        parent.page(testPageId!!)
                        richText {
                            text("Too many attachments")
                        }
                        attachments(
                            listOf(
                                CommentAttachmentRequest(fileUploadId = "file1"),
                                CommentAttachmentRequest(fileUploadId = "file2"),
                                CommentAttachmentRequest(fileUploadId = "file3"),
                                CommentAttachmentRequest(fileUploadId = "file4"), // Exceeds limit of 3
                            ),
                        )
                    }
                } catch (e: IllegalArgumentException) {
                    println("Attachment limit error caught: ${e.message}")
                }
            }
        }
    })
