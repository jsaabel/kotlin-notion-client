package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import kotlinx.coroutines.delay

/**
 * Self-contained integration test for Table Block functionality.
 *
 * This test validates the full workflow of creating table blocks with different configurations,
 * uploading them to Notion, and verifying the structure with precise validation.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update pages and blocks
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 *
 */
@Tags("Integration", "RequiresApi")
class TableBlockIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping TableBlockIntegrationTest due to missing environment variables") }
        } else {
            "Should create page with table blocks and verify structure" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    // Step 1: Create initial page using pageRequest DSL
                    println("📄 Creating test page for Table Block demonstration...")
                    val initialPageRequest =
                        createPageRequest {
                            parent.page(parentPageId)
                            icon.emoji("📊")
                            properties {
                                title("title", "Table Block Integration Test")
                            }
                        }

                    val createdPage = client.pages.create(initialPageRequest)
                    createdPage.objectType shouldBe "page"
                    createdPage.archived shouldBe false

                    println("✅ Initial page created: ${createdPage.id}")

                    // Small delay to ensure Notion has processed the page creation
                    delay(500)

                    // Step 2: Create content with multiple table configurations using pageContent DSL
                    println("🏗️ Building table structure with DSL...")

                    // Track expected counts for precise validation
                    var expectedTopLevelBlocks = 0

                    val tableContent =
                        pageContent {
                            heading1("📊 Table Block Integration Test")
                            expectedTopLevelBlocks++

                            paragraph("This test demonstrates table block functionality with real Notion API integration.")
                            expectedTopLevelBlocks++

                            divider()
                            expectedTopLevelBlocks++

                            heading2("🧱 Simple Table (No Headers)")
                            expectedTopLevelBlocks++

                            table(
                                tableWidth = 2,
                                hasColumnHeader = false,
                                hasRowHeader = false,
                            ) {
                                row("Cell 1", "Cell 2")
                                row("Cell 3", "Cell 4")
                            }
                            expectedTopLevelBlocks++

                            heading2("📋 Table with Column Headers")
                            expectedTopLevelBlocks++

                            table(
                                tableWidth = 3,
                                hasColumnHeader = true,
                                hasRowHeader = false,
                            ) {
                                row("Name", "Age", "City")
                                row("John", "25", "New York")
                                row("Jane", "30", "London")
                                row("Bob", "35", "Paris")
                            }
                            expectedTopLevelBlocks++

                            heading2("🏷️ Table with Row Headers")
                            expectedTopLevelBlocks++

                            table(
                                tableWidth = 3,
                                hasColumnHeader = false,
                                hasRowHeader = true,
                            ) {
                                row("Q1", "100", "200")
                                row("Q2", "150", "250")
                                row("Q3", "120", "220")
                            }
                            expectedTopLevelBlocks++

                            heading2("🎯 Table with Both Headers")
                            expectedTopLevelBlocks++

                            table(
                                tableWidth = 4,
                                hasColumnHeader = true,
                                hasRowHeader = true,
                            ) {
                                row("", "Product A", "Product B", "Product C")
                                row("Sales", "1000", "1500", "1200")
                                row("Returns", "50", "75", "60")
                                row("Profit", "950", "1425", "1140")
                            }
                            expectedTopLevelBlocks++

                            heading2("📈 Data Table Example")
                            expectedTopLevelBlocks++

                            table(
                                tableWidth = 5,
                                hasColumnHeader = true,
                                hasRowHeader = false,
                            ) {
                                row("Month", "Revenue", "Costs", "Profit", "Growth")
                                row("January", "$10,000", "$6,000", "$4,000", "5%")
                                row("February", "$12,000", "$6,500", "$5,500", "10%")
                                row("March", "$15,000", "$7,000", "$8,000", "15%")
                            }
                            expectedTopLevelBlocks++

                            divider()
                            expectedTopLevelBlocks++

                            paragraph("🎉 Table block integration test completed successfully!")
                            expectedTopLevelBlocks++
                        }

                    // Step 3: Validate DSL-generated structure
                    tableContent shouldHaveSize expectedTopLevelBlocks
                    println("✅ DSL created exactly $expectedTopLevelBlocks top-level blocks")

                    // Step 4: Verify table blocks in DSL structure
                    val tableBlocks = tableContent.filterIsInstance<BlockRequest.Table>()
                    tableBlocks shouldHaveSize 5
                    println("✅ DSL created exactly 5 table blocks")

                    // Step 5: Verify specific table structures
                    val simpleTable = tableBlocks[0]
                    simpleTable.table.tableWidth shouldBe 2
                    simpleTable.table.hasColumnHeader shouldBe false
                    simpleTable.table.hasRowHeader shouldBe false
                    simpleTable.table.children?.shouldHaveSize(2)

                    val columnHeaderTable = tableBlocks[1]
                    columnHeaderTable.table.tableWidth shouldBe 3
                    columnHeaderTable.table.hasColumnHeader shouldBe true
                    columnHeaderTable.table.hasRowHeader shouldBe false
                    columnHeaderTable.table.children?.shouldHaveSize(4)

                    val bothHeadersTable = tableBlocks[3]
                    bothHeadersTable.table.tableWidth shouldBe 4
                    bothHeadersTable.table.hasColumnHeader shouldBe true
                    bothHeadersTable.table.hasRowHeader shouldBe true
                    bothHeadersTable.table.children?.shouldHaveSize(4)

                    println("✅ All table structures verified in DSL")

                    // Step 6: Append content to Notion page
                    println("📤 Appending DSL-generated content to Notion page...")
                    val appendResponse = client.blocks.appendChildren(createdPage.id, tableContent)

                    appendResponse.objectType shouldBe "list"
                    appendResponse.results shouldHaveSize expectedTopLevelBlocks

                    println("✅ Content successfully appended to Notion")

                    // Step 7: Retrieve and verify from Notion
                    delay(1000) // Give Notion time to process
                    println("🔍 Retrieving content from Notion to verify...")

                    val blocks = client.blocks.retrieveChildren(createdPage.id)
                    blocks shouldHaveSize expectedTopLevelBlocks

                    // Step 8: Verify table blocks in Notion response
                    val notionTableBlocks = blocks.filterIsInstance<Block.Table>()
                    notionTableBlocks shouldHaveSize 5
                    println("✅ Notion contains exactly 5 table blocks")

                    // Step 9: Verify specific table properties in Notion
                    val simpleNotionTable = notionTableBlocks[0]
                    simpleNotionTable.table.tableWidth shouldBe 2
                    simpleNotionTable.table.hasColumnHeader shouldBe false
                    simpleNotionTable.table.hasRowHeader shouldBe false

                    val columnHeaderNotionTable = notionTableBlocks[1]
                    columnHeaderNotionTable.table.tableWidth shouldBe 3
                    columnHeaderNotionTable.table.hasColumnHeader shouldBe true
                    columnHeaderNotionTable.table.hasRowHeader shouldBe false

                    val bothHeadersNotionTable = notionTableBlocks[3]
                    bothHeadersNotionTable.table.tableWidth shouldBe 4
                    bothHeadersNotionTable.table.hasColumnHeader shouldBe true
                    bothHeadersNotionTable.table.hasRowHeader shouldBe true

                    println("✅ All table properties verified in Notion")

                    // Step 10: Retrieve and verify table rows
                    val simpleTableChildren = client.blocks.retrieveChildren(simpleNotionTable.id)
                    simpleTableChildren shouldHaveSize 2
                    simpleTableChildren.forEach { it.shouldBeInstanceOf<Block.TableRow>() }

                    val columnHeaderTableChildren = client.blocks.retrieveChildren(columnHeaderNotionTable.id)
                    columnHeaderTableChildren shouldHaveSize 4
                    columnHeaderTableChildren.forEach { it.shouldBeInstanceOf<Block.TableRow>() }

                    println("✅ All table rows verified in Notion")

                    // Step 11: Verify specific row content
                    val firstRow = simpleTableChildren[0] as Block.TableRow
                    firstRow.tableRow.cells shouldHaveSize 2
                    firstRow.tableRow.cells[0][0].plainText shouldBe "Cell 1"
                    firstRow.tableRow.cells[1][0].plainText shouldBe "Cell 2"

                    val headerRow = columnHeaderTableChildren[0] as Block.TableRow
                    headerRow.tableRow.cells shouldHaveSize 3
                    headerRow.tableRow.cells[0][0].plainText shouldBe "Name"
                    headerRow.tableRow.cells[1][0].plainText shouldBe "Age"
                    headerRow.tableRow.cells[2][0].plainText shouldBe "City"

                    println("✅ Table row content verified successfully")

                    println("✅ Table block integration test completed successfully!")
                    println("   - DSL created: $expectedTopLevelBlocks blocks (5 tables)")
                    println("   - Notion received: ${appendResponse.results.size} blocks")
                    println("   - Notion stored: ${blocks.size} blocks")
                    println("   - All table configurations working: ✅")
                    println("   - Table row content verified: ✅")
                    println("   - Headers functionality verified: ✅")

                    // Step 12: Conditionally clean up
                    delay(500)
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up - archiving test page...")
                        val archivedPage = client.pages.archive(createdPage.id)
                        archivedPage.archived shouldBe true
                        println("✅ Test page archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created page: ${createdPage.id} (\"Table Block Integration Test\")")
                        println("   Contains $expectedTopLevelBlocks blocks with 5 different table configurations")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
