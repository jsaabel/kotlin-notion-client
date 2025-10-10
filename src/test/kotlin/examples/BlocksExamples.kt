package examples

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.blocks.Block
import kotlinx.coroutines.delay

/**
 * Validation tests for all code examples in docs/blocks.md
 *
 * These tests ensure that every code example in the documentation:
 * 1. Compiles successfully
 * 2. Runs against the real Notion API
 * 3. Produces expected results
 *
 * Prerequisites:
 * - export NOTION_RUN_INTEGRATION_TESTS=true
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="page-id"
 *
 * Run with: ./gradlew test --tests "*BlocksExamples"
 */
@Tags("Integration", "RequiresApi", "Examples")
class BlocksExamples :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) Blocks examples" {
                println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true and required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            // Test data setup
            var testPageId: String? = null

            beforeSpec {
                println("🔧 Setting up test data for blocks examples...")

                // Create a test page with some initial content
                val page =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Blocks Examples Test")

                        content {
                            heading1("Initial Content")
                            paragraph("This is the initial paragraph.")
                        }
                    }

                testPageId = page.id
                delay(2000)
                println("✅ Test setup complete")
            }

            afterSpec {
                if (shouldCleanupAfterTest() && testPageId != null) {
                    println("🧹 Cleaning up test page...")
                    try {
                        notion.pages.archive(testPageId!!)
                        println("✅ Cleanup complete")
                    } catch (e: Exception) {
                        println("⚠️ Cleanup failed: ${e.message}")
                    }
                }
            }

            // ========================================
            // Example 1: Retrieve Block Children
            // ========================================
            "Example 1: Retrieve block children" {
                println("\n📖 Running Example 1: Retrieve block children")

                val blocks = notion.blocks.retrieveChildren(testPageId!!)
                blocks.forEach { block ->
                    println("${block.type}: ${block.id}")
                }

                // Validation
                blocks.shouldNotBeEmpty()
                blocks.any { it is Block.Heading1 } shouldBe true

                println("✅ Example 1 passed")
            }

            // ========================================
            // Example 2: Append Simple Blocks
            // ========================================
            "Example 2: Append simple blocks to a page" {
                println("\n📖 Running Example 2: Append simple blocks")

                notion.blocks.appendChildren(testPageId!!) {
                    heading1("Project Overview")

                    paragraph("This project aims to...")

                    bullet("First key point")
                }

                // Verify by retrieving children
                delay(1000)
                val blocks = notion.blocks.retrieveChildren(testPageId!!)

                // Validation
                blocks.shouldNotBeEmpty()
                blocks.any { block ->
                    block is Block.Heading1 &&
                        block.heading1.richText
                            .firstOrNull()
                            ?.plainText == "Project Overview"
                } shouldBe true

                println("✅ Example 2 passed")
            }

            // ========================================
            // Example 3: Append Rich Text Blocks
            // ========================================
            "Example 3: Append blocks with rich text formatting" {
                println("\n📖 Running Example 3: Append rich text blocks")

                notion.blocks.appendChildren(testPageId!!) {
                    paragraph {
                        text("This text is ")
                        bold("bold")
                        text(", this is ")
                        italic("italic")
                        text(", and this is ")
                        code("code")
                        text(".")
                    }
                }

                // Verify
                delay(1000)
                val blocks = notion.blocks.retrieveChildren(testPageId!!)

                // Validation - should have at least one paragraph with multiple rich text segments
                blocks.shouldNotBeEmpty()
                blocks.any { it is Block.Paragraph } shouldBe true

                println("✅ Example 3 passed")
            }

            // ========================================
            // Example 4: Create Different List Types
            // ========================================
            "Example 4: Create different list types" {
                println("\n📖 Running Example 4: Create lists")

                notion.blocks.appendChildren(testPageId!!) {
                    heading2("Task List")

                    toDo("Review pull requests", checked = false)
                    toDo("Update documentation", checked = true)
                    toDo("Deploy to production", checked = false)

                    heading2("Features")

                    bullet("Type-safe API")
                    bullet("Coroutine support")
                    bullet("Comprehensive error handling")

                    heading2("Steps")

                    number("Initialize the client")
                    number("Configure authentication")
                    number("Make API calls")
                }

                // Verify
                delay(1000)
                val blocks = notion.blocks.retrieveChildren(testPageId!!)

                // Validation
                blocks.shouldNotBeEmpty()
                blocks.any { it is Block.ToDo } shouldBe true
                blocks.any { it is Block.BulletedListItem } shouldBe true
                blocks.any { it is Block.NumberedListItem } shouldBe true

                println("✅ Example 4 passed")
            }

            // ========================================
            // Example 5: Create Code Block
            // ========================================
            "Example 5: Create a code block" {
                println("\n📖 Running Example 5: Create code block")

                notion.blocks.appendChildren(testPageId!!) {
                    code(
                        language = "kotlin",
                        code =
                            """
                            fun main() {
                                println("Hello, Notion!")
                            }
                            """.trimIndent(),
                    )
                }

                // Verify
                delay(1000)
                val blocks = notion.blocks.retrieveChildren(testPageId!!)

                // Validation
                blocks.shouldNotBeEmpty()
                val codeBlock = blocks.firstOrNull { it is Block.Code } as? Block.Code
                codeBlock.shouldNotBeNull()
                codeBlock.code.language shouldBe "kotlin"

                println("✅ Example 5 passed")
            }

            // ========================================
            // Example 6: Create Callout
            // ========================================
            "Example 6: Create a callout block" {
                println("\n📖 Running Example 6: Create callout")

                notion.blocks.appendChildren(testPageId!!) {
                    callout("⚠️") {
                        text("Important: Make sure to read the documentation before proceeding.")
                    }
                }

                // Verify
                delay(1000)
                val blocks = notion.blocks.retrieveChildren(testPageId!!)

                // Validation
                blocks.shouldNotBeEmpty()
                blocks.any { it is Block.Callout } shouldBe true

                println("✅ Example 6 passed")
            }

            // ========================================
            // Example 7: Create Quote and Divider
            // ========================================
            "Example 7: Create quote and divider blocks" {
                println("\n📖 Running Example 7: Create quote and divider")

                notion.blocks.appendChildren(testPageId!!) {
                    quote("The best way to predict the future is to invent it.")

                    divider()

                    paragraph("Content after the divider.")
                }

                // Verify
                delay(1000)
                val blocks = notion.blocks.retrieveChildren(testPageId!!)

                // Validation
                blocks.shouldNotBeEmpty()
                blocks.any { it is Block.Quote } shouldBe true
                blocks.any { it is Block.Divider } shouldBe true

                println("✅ Example 7 passed")
            }

            // ========================================
            // Example 8: Retrieve a Single Block
            // ========================================
            "Example 8: Retrieve a single block" {
                println("\n📖 Running Example 8: Retrieve single block")

                // First, get the children to find a block ID
                val blocks = notion.blocks.retrieveChildren(testPageId!!)
                val firstBlockId = blocks.first().id

                // Retrieve that specific block
                val block = notion.blocks.retrieve(firstBlockId)

                // Validation
                block.shouldNotBeNull()
                block.id shouldBe firstBlockId

                println("✅ Example 8 passed")
            }

            // ========================================
            // Example 9: Update a Block
            // ========================================
            "Example 9: Update a block" {
                println("\n📖 Running Example 9: Update a block")

                // First, create a block to update
                val result =
                    notion.blocks.appendChildren(testPageId!!) {
                        paragraph("Original text")
                    }
                val blockToUpdate = result.results.first()

                delay(1000)

                // Update the block
                val updated =
                    notion.blocks.update(blockToUpdate.id) {
                        paragraph("Updated text with new content")
                    }

                // Validation
                updated.shouldNotBeNull()
                (updated as? Block.Paragraph)
                    ?.paragraph
                    ?.richText
                    ?.firstOrNull()
                    ?.plainText shouldBe
                    "Updated text with new content"

                println("✅ Example 9 passed")
            }

            // ========================================
            // Example 10: Delete a Block
            // ========================================
            "Example 10: Delete a block" {
                println("\n📖 Running Example 10: Delete a block")

                // First, create a block to delete
                val result =
                    notion.blocks.appendChildren(testPageId!!) {
                        paragraph("This block will be deleted")
                    }
                val blockToDelete = result.results.first()

                delay(1000)

                // Delete the block
                val deleted = notion.blocks.delete(blockToDelete.id)

                // Validation
                deleted.shouldNotBeNull()
                deleted.archived shouldBe true

                println("✅ Example 10 passed")
            }

            // ========================================
            // Example 11: Create Nested Blocks
            // ========================================
            "Example 11: Create nested blocks" {
                println("\n📖 Running Example 11: Create nested blocks")

                // Create a bullet list item
                val result =
                    notion.blocks.appendChildren(testPageId!!) {
                        bullet("Parent item")
                    }
                val parentBlock = result.results.first()

                delay(1000)

                // Add nested items
                notion.blocks.appendChildren(parentBlock.id) {
                    bullet("Nested item 1")
                    bullet("Nested item 2")
                }

                // Verify
                delay(1000)
                val nestedBlocks = notion.blocks.retrieveChildren(parentBlock.id)

                // Validation
                nestedBlocks.shouldNotBeEmpty()
                nestedBlocks.size shouldBe 2

                println("✅ Example 11 passed")
            }

            // ========================================
            // Example 12: Create Toggle Block
            // ========================================
            "Example 12: Create a toggle block" {
                println("\n📖 Running Example 12: Create toggle block")

                // Create a toggle block
                val result =
                    notion.blocks.appendChildren(testPageId!!) {
                        toggle("Click to expand")
                    }
                val toggleBlock = result.results.first()

                delay(1000)

                // Add content inside the toggle
                notion.blocks.appendChildren(toggleBlock.id) {
                    paragraph("This content is hidden inside the toggle.")
                    bullet("Hidden bullet point 1")
                    bullet("Hidden bullet point 2")
                }

                // Verify
                delay(1000)
                val toggleContent = notion.blocks.retrieveChildren(toggleBlock.id)

                // Validation
                toggleContent.shouldNotBeEmpty()
                toggleContent.any { it is Block.Paragraph } shouldBe true
                toggleContent.any { it is Block.BulletedListItem } shouldBe true

                println("✅ Example 12 passed")
            }

            // ========================================
            // Example 13: Create Multiple Block Types at Once
            // ========================================
            "Example 13: Create a complete document structure" {
                println("\n📖 Running Example 13: Create complete document")

                // Create a new page with comprehensive content
                val documentPage =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Complete Document Example")

                        content {
                            heading1("Introduction")
                            paragraph("This document demonstrates all block types.")

                            divider()

                            heading2("Code Example")
                            code(
                                language = "kotlin",
                                code =
                                    """
                                    val client = NotionClient.create(config)
                                    val page = client.pages.retrieve(pageId)
                                    """.trimIndent(),
                            )

                            heading2("Important Notes")
                            callout("💡") {
                                text("Remember to handle errors appropriately.")
                            }

                            heading2("Tasks")
                            toDo("Complete implementation", checked = false)
                            toDo("Write tests", checked = false)
                            toDo("Update documentation", checked = true)

                            heading2("Resources")
                            bullet("Official Notion API documentation")
                            bullet("Kotlin coroutines guide")
                            bullet("Best practices for API clients")

                            divider()

                            quote("Well-documented code is as important as the code itself.")
                        }
                    }

                // Verify
                delay(2000)
                val blocks = notion.blocks.retrieveChildren(documentPage.id)

                // Validation
                blocks.shouldNotBeEmpty()
                val blockTypes = blocks.map { it.type }
                blockTypes shouldContain "heading_1"
                blockTypes shouldContain "heading_2"
                blockTypes shouldContain "paragraph"
                blockTypes shouldContain "code"
                blockTypes shouldContain "callout"
                blockTypes shouldContain "to_do"
                blockTypes shouldContain "bulleted_list_item"
                blockTypes shouldContain "quote"
                blockTypes shouldContain "divider"

                // Cleanup
                if (shouldCleanupAfterTest()) {
                    notion.pages.archive(documentPage.id)
                }

                println("✅ Example 13 passed")
            }
        }
    })
