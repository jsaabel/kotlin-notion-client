package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.blocks.BlockAppendPosition
import it.saabel.kotlinnotionclient.models.blocks.BlockReference
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import kotlinx.coroutines.delay

/**
 * Integration tests for BlocksApi position parameter in appendChildren.
 *
 * Tests all three BlockAppendPosition variants:
 * - BlockAppendPosition.Start — insert at beginning
 * - BlockAppendPosition.End — insert at end (default)
 * - BlockAppendPosition.AfterBlock — insert after a specific block
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update pages and blocks
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 *
 * Run with: ./gradlew integrationTest --tests "*.BlockAppendPositionIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class BlockAppendPositionIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping BlockAppendPositionIntegrationTest due to missing environment variables") }
        } else {
            "Should append blocks at Start position before existing content" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📍 Testing appendChildren with position=Start...")

                    // Step 1: Create a test page with two initial blocks
                    val page =
                        client.pages.create(
                            CreatePageRequest(
                                parent = Parent.PageParent(pageId = parentPageId),
                                properties =
                                    mapOf(
                                        "title" to
                                            PagePropertyValue.TitleValue(
                                                title = listOf(RequestBuilders.createSimpleRichText("Block Position Test - Start")),
                                            ),
                                    ),
                            ),
                        )
                    println("✅ Test page created: ${page.id}")
                    println("   URL: ${page.url}")
                    delay(1000)

                    // Step 2: Append initial blocks (A and B)
                    client.blocks.appendChildren(page.id) {
                        paragraph("Block A - initial first")
                        paragraph("Block B - initial second")
                    }
                    delay(1000)

                    // Step 3: Append new block at Start position
                    client.blocks.appendChildren(page.id, position = BlockAppendPosition.Start) {
                        paragraph("Block PREPENDED - should be first")
                    }
                    delay(1000)

                    // Step 4: Verify ordering — prepended block should be first
                    val children = client.blocks.retrieveChildren(page.id)
                    println("   Block order after prepend:")
                    children.forEachIndexed { i, block -> println("     [$i] ${block.id}") }

                    children.size shouldBe 3

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("✅ Test page archived")
                    } else {
                        println("🔧 Cleanup skipped — page: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }

            "Should append blocks at AfterBlock position after a specific block" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📍 Testing appendChildren with position=AfterBlock...")

                    // Step 1: Create a test page with three initial blocks (A, B, C)
                    val page =
                        client.pages.create(
                            CreatePageRequest(
                                parent = Parent.PageParent(pageId = parentPageId),
                                properties =
                                    mapOf(
                                        "title" to
                                            PagePropertyValue.TitleValue(
                                                title = listOf(RequestBuilders.createSimpleRichText("Block Position Test - AfterBlock")),
                                            ),
                                    ),
                            ),
                        )
                    println("✅ Test page created: ${page.id}")
                    println("   URL: ${page.url}")
                    delay(1000)

                    client.blocks.appendChildren(page.id) {
                        paragraph("Block A")
                        paragraph("Block B")
                        paragraph("Block C")
                    }
                    delay(1000)

                    // Get initial children so we know block A's ID
                    val initialChildren = client.blocks.retrieveChildren(page.id)
                    initialChildren.size shouldBe 3
                    val blockAId = initialChildren[0].id
                    println("   Block A ID: $blockAId")

                    // Step 2: Insert block after Block A → should appear between A and B
                    client.blocks.appendChildren(
                        page.id,
                        position = BlockAppendPosition.AfterBlock(BlockReference(id = blockAId)),
                    ) {
                        paragraph("Block AFTER_A - inserted after Block A")
                    }
                    delay(1000)

                    // Step 3: Verify ordering — new block should be at index 1 (after A, before B)
                    val children = client.blocks.retrieveChildren(page.id)
                    println("   Block order after AfterBlock insert:")
                    children.forEachIndexed { i, block -> println("     [$i] ${block.id}") }

                    children.size shouldBe 4
                    // Block A remains first
                    children[0].id shouldBe blockAId

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("✅ Test page archived")
                    } else {
                        println("🔧 Cleanup skipped — page: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }

            "Should append blocks at End position (default behavior)" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📍 Testing appendChildren with position=End...")

                    val page =
                        client.pages.create(
                            CreatePageRequest(
                                parent = Parent.PageParent(pageId = parentPageId),
                                properties =
                                    mapOf(
                                        "title" to
                                            PagePropertyValue.TitleValue(
                                                title = listOf(RequestBuilders.createSimpleRichText("Block Position Test - End")),
                                            ),
                                    ),
                            ),
                        )
                    println("✅ Test page created: ${page.id}")
                    println("   URL: ${page.url}")
                    delay(1000)

                    client.blocks.appendChildren(page.id) {
                        paragraph("Block A")
                        paragraph("Block B")
                    }
                    delay(1000)

                    val initialChildren = client.blocks.retrieveChildren(page.id)
                    val lastBlockId = initialChildren.last().id

                    // Append at End explicitly
                    client.blocks.appendChildren(page.id, position = BlockAppendPosition.End) {
                        paragraph("Block APPENDED - should be last")
                    }
                    delay(1000)

                    val children = client.blocks.retrieveChildren(page.id)
                    println("   Block order after End append:")
                    children.forEachIndexed { i, block -> println("     [$i] ${block.id}") }

                    children.size shouldBe 3
                    // Previous last block is now second-to-last
                    children[children.size - 2].id shouldBe lastBlockId

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("✅ Test page archived")
                    } else {
                        println("🔧 Cleanup skipped — page: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
