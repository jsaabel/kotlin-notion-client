package integration.pagination

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.comments.CreateCommentRequest
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Integration tests for comment pagination functionality.
 *
 * These tests verify that the client correctly handles paginated responses
 * from the Notion API when retrieving comments for discussions with many comments.
 *
 * Tests cover:
 * - Automatic pagination for comment retrieval
 * - Multiple comment threads
 * - Performance characteristics
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 */
@Tags("Integration", "RequiresApi", "Slow")
class CommentPaginationIntegrationTest :
    StringSpec({

        fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"

        "Should automatically paginate comments for discussions with many comments" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Create a page for comment testing
                    println("📄 Creating test page for comment pagination...")
                    val pageRequest =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            icon = RequestBuilders.createEmojiIcon("💬"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title =
                                                listOf(
                                                    RequestBuilders.createSimpleRichText(
                                                        "Comment Pagination Test - ${System.currentTimeMillis()}",
                                                    ),
                                                ),
                                        ),
                                ),
                        )

                    val page = client.pages.create(pageRequest)
                    println("✅ Test page created: ${page.id}")
                    delay(500)

                    // Create multiple individual comments (each creates its own discussion thread)
                    println("\n💬 Creating multiple comments for pagination testing...")
                    val totalComments = 25 // More than typical page size for comments

                    val createdCommentIds = mutableListOf<String>()
                    println("   Creating $totalComments individual comments...")

                    for (i in 1..totalComments) {
                        val comment =
                            client.comments.create(
                                CreateCommentRequest(
                                    parent = Parent(type = "page_id", pageId = page.id),
                                    richText =
                                        listOf(
                                            RequestBuilders.createSimpleRichText(
                                                "Test comment $i - This is a comment for testing pagination",
                                            ),
                                        ),
                                ),
                            )
                        createdCommentIds.add(comment.id)

                        if (i % 10 == 0) {
                            println("   Created $i/$totalComments comments")
                            delay(200) // Small delay to avoid rate limits
                        }
                    }

                    println("✅ Created ${createdCommentIds.size} comments")
                    delay(1000)

                    // Retrieve all comments (should trigger automatic pagination)
                    println("\n🔍 Testing automatic pagination for comments...")
                    val startTime = System.currentTimeMillis()
                    val allComments = client.comments.retrieve(page.id)
                    val retrievalTime = System.currentTimeMillis() - startTime

                    // We should have all comments
                    allComments.size shouldBe createdCommentIds.size
                    println("✅ Comment pagination successful!")
                    println("   - Retrieved ${allComments.size} comments")
                    println("   - Retrieval time: ${retrievalTime}ms")
                    println("   - Automatically handled pagination")

                    // Verify the comments are properly structured
                    val pageComments = allComments.filter { it.parent.type == "page_id" }

                    println("   - Comment structure:")
                    println("     • Page comments: ${pageComments.size}")

                    // Verify we got the expected comments
                    val commentTexts = allComments.map { it.richText.firstOrNull()?.plainText ?: "" }
                    val expectedComments = commentTexts.count { it.contains("Test comment") }
                    expectedComments shouldBe totalComments

                    println("   - Found $expectedComments test comments as expected")

                    // Cleanup - just archive the page, comments are automatically cleaned up
                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test page...")
                        client.pages.archive(page.id)
                        println("✅ Test page archived (comments are automatically cleaned up)")
                    } else {
                        println("\n🔧 Test page preserved: ${page.id}")
                        println("   Contains ${allComments.size} comments")
                    }

                    println("\n🎉 Comment pagination test completed successfully!")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping comment pagination test - missing environment variables")
                println("   Required:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_TEST_PAGE_ID: Parent page for test database")
            }
        }
    })
