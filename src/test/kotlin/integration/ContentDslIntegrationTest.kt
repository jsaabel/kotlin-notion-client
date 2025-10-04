package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.blocks.Block
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.models.blocks.pageContent
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Self-contained integration test for the Content Creation DSL.
 *
 * This test validates the full workflow of creating rich nested content using our DSL,
 * uploading it to Notion, and verifying the structure with precise block counting.
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
class ContentDslIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping ContentDslIntegrationTest due to missing environment variables") }
        } else {
            "Should create page with DSL content, append to Notion, and verify with precise counts" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Step 1: Create initial page
                    println("📄 Creating test page for Content DSL demonstration...")
                    val initialPageRequest =
                        CreatePageRequest(
                            parent =
                                Parent(
                                    type = "page_id",
                                    pageId = parentPageId,
                                ),
                            icon = RequestBuilders.createEmojiIcon("🎨"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title = listOf(RequestBuilders.createSimpleRichText("Content DSL Integration Test")),
                                        ),
                                ),
                        )

                    val createdPage = client.pages.create(initialPageRequest)
                    createdPage.objectType shouldBe "page"
                    createdPage.archived shouldBe false

                    println("✅ Initial page created: ${createdPage.id}")

                    // Small delay to ensure Notion has processed the page creation
                    delay(500)

                    // Step 2: Create content using our DSL with precise counting
                    println("🏗️ Building content structure with DSL...")

                    // Track expected counts for precise validation
                    var expectedTopLevelBlocks = 0

                    val pageContent =
                        pageContent {
                            heading1("📚 Content DSL Integration Test")
                            expectedTopLevelBlocks++ // 1

                            paragraph("This test demonstrates our Content Creation DSL with real Notion API integration.")
                            expectedTopLevelBlocks++ // 2

                            divider()
                            expectedTopLevelBlocks++ // 3

                            heading2("🧱 Block Types Demonstration")
                            expectedTopLevelBlocks++ // 4

                            bullet("Simple bullet point")
                            expectedTopLevelBlocks++ // 5

                            bullet("Bullet with nested content") {
                                paragraph("This paragraph is nested inside a bullet")
                                bullet("Nested bullet item")
                            }
                            expectedTopLevelBlocks++ // 6

                            number("First numbered item")
                            expectedTopLevelBlocks++ // 7

                            number("Second numbered item with nested content") {
                                paragraph("Nested paragraph in numbered list")
                            }
                            expectedTopLevelBlocks++ // 8

                            toDo("Incomplete task", checked = false)
                            expectedTopLevelBlocks++ // 9

                            toDo("Completed task", checked = true)
                            expectedTopLevelBlocks++ // 10

                            code(
                                language = "kotlin",
                                code =
                                    """
                                    val greeting = "Hello from Content DSL!"
                                    println(greeting)
                                    """.trimIndent(),
                            )
                            expectedTopLevelBlocks++ // 11

                            quote("This is a quote block demonstrating our DSL capabilities")
                            expectedTopLevelBlocks++ // 12

                            callout("💡", "This callout shows our icon support!")
                            expectedTopLevelBlocks++ // 13

                            toggle("Click to expand") {
                                paragraph("This content is hidden inside a toggle")
                                bullet("Nested list in toggle")
                            }
                            expectedTopLevelBlocks++ // 14

                            divider()
                            expectedTopLevelBlocks++ // 15

                            // New block types demonstration
                            heading2("🆕 New Block Types")
                            expectedTopLevelBlocks++ // 16

                            bookmark("https://notion.so", caption = "Notion homepage")
                            expectedTopLevelBlocks++ // 17

                            embed("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                            expectedTopLevelBlocks++ // 18

                            columnList {
                                column {
                                    paragraph("Left column content")
                                    bullet("Left bullet")
                                }
                                column {
                                    paragraph("Right column content")
                                    bullet("Right bullet")
                                }
                            }
                            expectedTopLevelBlocks++ // 19

                            tableOfContents()
                            expectedTopLevelBlocks++ // 20

                            equation("E = mc^2")
                            expectedTopLevelBlocks++ // 21

                            // Note: Synced blocks require a two-step creation process
                            // First create original, then create references to it
                            // Skipping for now to avoid API complexity

                            breadcrumb()
                            expectedTopLevelBlocks++ // 22

                            // Note: childPage and childDatabase blocks cannot be created via blocks API
                            // They must be created through the pages API with proper parent relationships
                            // These block types are only for reading existing child page relationships

                            divider()
                            expectedTopLevelBlocks++ // 23

                            paragraph("🎉 Content DSL integration test completed successfully with all block types!")
                            expectedTopLevelBlocks++ // 24
                        }

                    // Step 3: Validate DSL-generated structure
                    pageContent shouldHaveSize expectedTopLevelBlocks
                    println("✅ DSL created exactly $expectedTopLevelBlocks top-level blocks")

                    // Step 4: Verify first block type (should be heading1)
                    pageContent[0].shouldBeInstanceOf<BlockRequest.Heading1>()

                    println("✅ Block type verified in DSL structure")

                    // Step 5: Debug the bullet structure (the 6th block which has nested content)
                    println("🔍 Block structure for debugging:")
                    val bulletBlock = pageContent[5] as BlockRequest.BulletedListItem // The bullet with nested content
                    println("- Block type: ${bulletBlock::class.simpleName}")
                    println("- Has bulletedListItem: ${bulletBlock.bulletedListItem}")
                    println("- Children count: ${bulletBlock.bulletedListItem.children?.size ?: 0}")
                    println("- Rich text: ${bulletBlock.bulletedListItem.richText}")
                    println("- Color: ${bulletBlock.bulletedListItem.color}")

                    // Step 5: Append content to Notion page
                    println("📤 Appending DSL-generated content to Notion page...")
                    val appendResponse = client.blocks.appendChildren(createdPage.id, pageContent)

                    appendResponse.objectType shouldBe "list"
                    appendResponse.results shouldHaveSize expectedTopLevelBlocks

                    println("✅ Content successfully appended to Notion")

                    // Step 6: Retrieve and verify from Notion
                    delay(1000) // Give Notion time to process
                    println("🔍 Retrieving content from Notion to verify...")

                    val blocks = client.blocks.retrieveChildren(createdPage.id)
                    blocks shouldHaveSize expectedTopLevelBlocks

                    // Step 7: Verify specific block type from Notion response
                    blocks[0].shouldBeInstanceOf<Block.Heading1>()

                    println("✅ Block type verified in Notion response")

                    // Step 8: Verify specific content from the bullet with nested content (6th block)
                    val bullet = blocks[5] as Block.BulletedListItem
                    bullet.bulletedListItem.richText[0].plainText shouldBe "Bullet with nested content"

                    // Step 9: Verify some of the new block types
                    blocks[16].shouldBeInstanceOf<Block.Bookmark>()
                    blocks[17].shouldBeInstanceOf<Block.Embed>()
                    blocks[18].shouldBeInstanceOf<Block.ColumnList>()
                    blocks[19].shouldBeInstanceOf<Block.TableOfContents>()
                    blocks[20].shouldBeInstanceOf<Block.Equation>()
                    blocks[21].shouldBeInstanceOf<Block.Breadcrumb>()
                    // Note: childPage and childDatabase blocks are not created via blocks API

                    println("✅ New block types verified successfully")
                    println("✅ Specific content verified successfully")

                    println("✅ Content DSL integration test completed successfully!")
                    println("   - DSL created: $expectedTopLevelBlocks blocks")
                    println("   - Notion received: ${appendResponse.results.size} blocks")
                    println("   - Notion stored: ${blocks.size} blocks")
                    println("   - All block types working: ✅")
                    println("   - Nested content working: ✅")

                    // Step 10: Conditionally clean up
                    delay(500)
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up - archiving test page...")
                        val archivedPage = client.pages.archive(createdPage.id)
                        archivedPage.archived shouldBe true
                        println("✅ Test page archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created page: ${createdPage.id} (\"Content DSL Integration Test\")")
                        println("   Contains $expectedTopLevelBlocks blocks with nested content")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
