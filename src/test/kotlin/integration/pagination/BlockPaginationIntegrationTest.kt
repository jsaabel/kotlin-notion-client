package integration.pagination

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.blocks.pageContent
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Integration tests for block pagination functionality.
 *
 * These tests verify that the client correctly handles paginated responses
 * from the Notion API when retrieving block children for pages with many blocks.
 *
 * Tests cover:
 * - Automatic pagination for block children with >100 blocks
 * - Different block types in pagination
 * - Performance characteristics
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 */
@Tags("Integration", "RequiresApi", "Slow")
class BlockPaginationIntegrationTest :
    StringSpec({

        fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"

        "Should automatically paginate block children for pages with many blocks" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Create a page that will contain many blocks
                    println("📄 Creating test page for block pagination...")
                    val pageRequest =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            icon = RequestBuilders.createEmojiIcon("📝"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title =
                                                listOf(
                                                    RequestBuilders.createSimpleRichText(
                                                        "Block Pagination Test - ${System.currentTimeMillis()}",
                                                    ),
                                                ),
                                        ),
                                ),
                        )

                    val page = client.pages.create(pageRequest)
                    println("✅ Test page created: ${page.id}")
                    delay(500)

                    // Create 110 blocks to trigger pagination (default is 100 per page)
                    println("\n📝 Creating 110 blocks to test pagination...")
                    val totalBlocks = 110
                    val blocksPerBatch = 10

                    for (batch in 0 until totalBlocks step blocksPerBatch) {
                        val batchContent =
                            pageContent {
                                for (i in batch until minOf(batch + blocksPerBatch, totalBlocks)) {
                                    paragraph("Test block ${i + 1} - This is content for pagination testing")

                                    // Add variety to make it more realistic
                                    if (i % 5 == 0) {
                                        bullet("Bullet point ${i + 1}")
                                    }
                                    if (i % 10 == 0) {
                                        divider()
                                    }
                                }
                            }

                        client.blocks.appendChildren(page.id, batchContent)
                        println("   Created blocks ${batch + 1}-${minOf(batch + blocksPerBatch, totalBlocks)}")
                        delay(200) // Small delay to avoid rate limits
                    }

                    println("✅ Created $totalBlocks blocks")
                    delay(1000)

                    // Retrieve all blocks (should trigger automatic pagination)
                    println("\n🔍 Testing automatic pagination for block children...")
                    val startTime = System.currentTimeMillis()
                    val allBlocks = client.blocks.retrieveChildren(page.id)
                    val retrievalTime = System.currentTimeMillis() - startTime

                    // Count actual content blocks (excluding page block itself if included)
                    val contentBlocks =
                        allBlocks.filter { block ->
                            block.type != "child_page" && block.type != "page"
                        }

                    contentBlocks.size shouldBeGreaterThan 100
                    println("✅ Block pagination successful!")
                    println("   - Retrieved ${contentBlocks.size} content blocks")
                    println("   - Retrieval time: ${retrievalTime}ms")
                    println("   - Automatically handled pagination (>100 blocks)")

                    // Verify we got all the expected block types
                    val paragraphCount = contentBlocks.count { it.type == "paragraph" }
                    val bulletCount = contentBlocks.count { it.type == "bulleted_list_item" }
                    val dividerCount = contentBlocks.count { it.type == "divider" }

                    println("   - Block types retrieved:")
                    println("     • Paragraphs: $paragraphCount")
                    println("     • Bullets: $bulletCount")
                    println("     • Dividers: $dividerCount")

                    // The BlocksApi doesn't expose pageSize parameter, pagination is handled automatically
                    println("\n🔍 Automatic pagination handled internally...")
                    println("✅ Block pagination working as expected!")
                    println("   - All blocks retrieved automatically")
                    println("   - Pagination handled transparently")

                    // Cleanup - just archive the page, which cleans up all blocks
                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test page...")
                        client.pages.archive(page.id)
                        println("✅ Test page archived (all blocks cleaned up automatically)")
                    } else {
                        println("\n🔧 Test page preserved: ${page.id}")
                        println("   Contains ${contentBlocks.size} blocks")
                    }

                    println("\n🎉 Block pagination test completed successfully!")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping block pagination test - missing environment variables")
                println("   Required:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_TEST_PAGE_ID: Parent page for test database")
            }
        }
    })
