package integration.dsl

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Color
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.BlockList
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.ParagraphRequestContent
import it.saabel.kotlinnotionclient.models.databases.Database
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import kotlinx.coroutines.delay

/**
 * Integration tests for API integration overloads that accept DSL builders.
 *
 * These tests validate that the new fluent API methods work correctly with
 * the live Notion API, testing the complete request/response cycle.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update pages, databases, and blocks
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 */
@Tags("Integration", "RequiresApi")
class ApiOverloadsIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping ApiOverloadsIntegrationTest due to missing environment variables") }
        } else {

            "PagesApi create overload should work with live API" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📄 Testing PagesApi.create() overload with live API...")

                    // Test the fluent API overload method
                    val createdPage =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("API Overload Test Page")
                            icon.emoji("🧪")
                            cover.external("https://placehold.co/800x400.png")
                            content {
                                heading1("API Integration Test")
                                paragraph("This page was created using the fluent API overload method.")
                                bullet("Demonstrates direct DSL usage with API client")
                                bullet("Tests end-to-end functionality")
                                divider()
                                quote("The overload method simplifies page creation significantly.")
                            }
                        }

                    // Verify the page was created correctly
                    createdPage.shouldBeInstanceOf<Page>()
                    createdPage.objectType shouldBe "page"
                    createdPage.archived shouldBe false
                    createdPage.parent.pageId?.replace("-", "") shouldBe parentPageId.replace("-", "")
                    createdPage.icon?.emoji shouldBe "🧪"
                    createdPage.cover?.external?.url shouldContain "placehold"

                    // Verify title property
                    val titleProperty = createdPage.properties["title"]
                    titleProperty.shouldNotBeNull()
                    titleProperty.shouldBeInstanceOf<PageProperty.Title>()
                    titleProperty.plainText shouldBe "API Overload Test Page"

                    println("✅ Page created successfully: ${createdPage.id}")

                    // Small delay to ensure content is processed
                    delay(1000)

                    // Verify content was added
                    val blocks = client.blocks.retrieveChildren(createdPage.id)
                    blocks.shouldNotBeNull()
                    blocks shouldHaveSize 6 // heading1, paragraph, 2 bullets, divider, quote

                    // Verify specific content
                    val firstBlock = blocks[0] as Block.Heading1
                    firstBlock.type shouldBe "heading_1"
                    firstBlock.heading1.richText[0].plainText shouldBe "API Integration Test"

                    println("✅ Page content verified successfully")
                    println("✅ PagesApi.create() overload test completed!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        delay(500)
                        println("🧹 Cleaning up test page...")
                        val archivedPage = client.pages.archive(createdPage.id)
                        archivedPage.archived shouldBe true
                        println("✅ Test page archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created page: ${createdPage.id} (\"API Overload Test Page\")")
                    }
                } finally {
                    client.close()
                }
            }

            "DatabasesApi create overload should work with live API" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🗄️ Testing DatabasesApi.create() overload with live API...")

                    // Test the fluent API overload method
                    val createdDatabase =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("API Overload Test Database")
                            description("Database created using fluent API overload")
                            icon.emoji("📊")
                            cover.external("https://placehold.co/1200x400.png")
                            properties {
                                title("Task Name")
                                richText("Description")
                                number("Priority", format = "number")
                                checkbox("Completed")
                                select("Status") {
                                    option("To Do", SelectOptionColor.RED)
                                    option("In Progress", SelectOptionColor.YELLOW)
                                    option("Done", SelectOptionColor.GREEN)
                                }
                                multiSelect("Tags") {
                                    option("Important", SelectOptionColor.RED)
                                    option("Urgent", SelectOptionColor.ORANGE)
                                    option("Review", SelectOptionColor.BLUE)
                                }
                                date("Due Date")
                                url("Reference URL")
                                email("Assignee Email")
                            }
                        }

                    // Verify the database was created correctly
                    createdDatabase.shouldBeInstanceOf<Database>()
                    createdDatabase.objectType shouldBe "database"
                    createdDatabase.archived shouldBe false
                    createdDatabase.parent.pageId?.replace("-", "") shouldBe parentPageId.replace("-", "")

                    // Icon and cover are returned in creation response (but may not persist - see known issue below)
                    createdDatabase.icon?.emoji shouldBe "📊"
                    createdDatabase.cover?.external?.url shouldContain "placehold"

                    // KNOWN ISSUE: Icon/cover may not persist in Notion UI (2025-09-03 API behavior)
                    // See DatabaseRequestBuilderIntegrationTest for detailed explanation

                    // Verify title
                    createdDatabase.title shouldHaveSize 1
                    createdDatabase.title[0].plainText shouldBe "API Overload Test Database"

                    // Verify description
                    createdDatabase.description shouldHaveSize 1
                    createdDatabase.description[0].plainText shouldBe "Database created using fluent API overload"

                    println("✅ Database created successfully: ${createdDatabase.id}")

                    // Retrieve database to get data source (2025-09-03 API - properties are in data source)
                    delay(500)
                    val retrievedDatabase = client.databases.retrieve(createdDatabase.id)
                    retrievedDatabase.dataSources.shouldNotBeNull()
                    val dataSource = retrievedDatabase.dataSources.first()

                    // Retrieve data source to verify properties
                    val dataSourceDetails = client.dataSources.retrieve(dataSource.id)
                    val properties = dataSourceDetails.properties

                    // Verify properties were created in the data source
                    properties.shouldContainKey("Task Name")
                    properties.shouldContainKey("Description")
                    properties.shouldContainKey("Priority")
                    properties.shouldContainKey("Completed")
                    properties.shouldContainKey("Status")
                    properties.shouldContainKey("Tags")
                    properties.shouldContainKey("Due Date")
                    properties.shouldContainKey("Reference URL")
                    properties.shouldContainKey("Assignee Email")

                    // Verify specific property types
                    properties["Task Name"]?.type shouldBe "title"
                    properties["Description"]?.type shouldBe "rich_text"
                    properties["Priority"]?.type shouldBe "number"
                    properties["Completed"]?.type shouldBe "checkbox"
                    properties["Status"]?.type shouldBe "select"
                    properties["Tags"]?.type shouldBe "multi_select"
                    properties["Due Date"]?.type shouldBe "date"
                    properties["Reference URL"]?.type shouldBe "url"
                    properties["Assignee Email"]?.type shouldBe "email"

                    println("✅ Data source properties verified successfully")
                    println("✅ DatabasesApi.create() overload test completed!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        delay(500)
                        println("🧹 Cleaning up test database...")
                        val archivedDatabase = client.databases.archive(createdDatabase.id)
                        archivedDatabase.archived shouldBe true
                        println("✅ Test database archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created database: ${createdDatabase.id} (\"API Overload Test Database\")")
                    }
                } finally {
                    client.close()
                }
            }

            "BlocksApi appendChildren overload should work with live API" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🧱 Testing BlocksApi.appendChildren() overload with live API...")

                    // First create a test page to add blocks to
                    val testPage =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("Block Append Test Page")
                            content {
                                paragraph("Initial content before using overload method.")
                            }
                        }

                    delay(1000) // Wait for page creation to complete

                    // Test the fluent API overload method for appending blocks
                    val appendedBlocks =
                        client.blocks.appendChildren(testPage.id) {
                            heading1("Added via Overload Method")
                            paragraph("This content was added using the BlocksApi.appendChildren() overload.")
                            bullet("First bullet point")
                            bullet("Second bullet point")
                            number("First numbered item")
                            number("Second numbered item")
                            toggle("Expandable section") {
                                paragraph("Content inside the toggle")
                                bullet("Nested bullet")
                            }
                            divider()
                            quote("Quote added via overload method")
                            code(
                                language = "kotlin",
                                code =
                                    """
                                    // Example of fluent API usage
                                    client.blocks.appendChildren(pageId) {
                                        heading1("Title")
                                        paragraph("Content")
                                    }
                                    """.trimIndent(),
                            )
                        }

                    // Verify the blocks were appended correctly
                    appendedBlocks.shouldBeInstanceOf<BlockList>()
                    appendedBlocks.results shouldHaveSize 10 // All the blocks we added

                    // Verify specific content
                    val firstBlock = appendedBlocks.results[0] as Block.Heading1
                    firstBlock.type shouldBe "heading_1"
                    firstBlock.heading1.richText[0].plainText shouldBe "Added via Overload Method"

                    val secondBlock = appendedBlocks.results[1] as Block.Paragraph
                    secondBlock.type shouldBe "paragraph"
                    secondBlock.paragraph.richText[0].plainText shouldContain "BlocksApi.appendChildren() overload"

                    println("✅ Blocks appended successfully")

                    // Verify the full page content
                    delay(1000)
                    val allBlocks = client.blocks.retrieveChildren(testPage.id)
                    allBlocks shouldHaveSize 11 // 1 initial + 10 appended

                    println("✅ Full page content verified")
                    println("✅ BlocksApi.appendChildren() overload test completed!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        delay(500)
                        println("🧹 Cleaning up test page...")
                        val archivedPage = client.pages.archive(testPage.id)
                        archivedPage.archived shouldBe true
                        println("✅ Test page archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created page: ${testPage.id} (\"Block Append Test Page\")")
                    }
                } finally {
                    client.close()
                }
            }

            "BlocksApi update overload should work with live API" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔄 Testing BlocksApi.update() overload with live API...")

                    // First create a test page with initial content
                    val testPage =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("Block Update Test Page")
                            content {
                                heading2("Original Heading")
                                paragraph("Original paragraph content")
                                bullet("Original bullet point")
                            }
                        }

                    delay(1000) // Wait for page creation to complete

                    // Get the blocks to update
                    val blocks = client.blocks.retrieveChildren(testPage.id)
                    val headingBlock = blocks[0] // First block is the heading
                    val paragraphBlock = blocks[1] // Second block is the paragraph

                    println("✅ Created test page with initial content")

                    // Test updating the heading using the DSL overload
                    val updatedHeading =
                        client.blocks.update(headingBlock.id) {
                            heading2("Updated Heading via DSL", color = Color.BLUE)
                        }

                    // Verify the heading was updated
                    updatedHeading.shouldBeInstanceOf<Block.Heading2>()
                    updatedHeading.type shouldBe "heading_2"
                    updatedHeading.heading2.richText[0].plainText shouldBe "Updated Heading via DSL"
                    updatedHeading.heading2.color shouldBe Color.BLUE

                    println("✅ Heading updated successfully using DSL overload")

                    // Test updating the paragraph using direct request
                    val updatedParagraph =
                        client.blocks.update(
                            paragraphBlock.id,
                            BlockRequest.Paragraph(
                                paragraph =
                                    ParagraphRequestContent(
                                        richText =
                                            listOf(
                                                RequestBuilders.createSimpleRichText(
                                                    "Updated paragraph content via direct request",
                                                ),
                                            ),
                                        color = Color.GREEN,
                                    ),
                            ),
                        )

                    // Verify the paragraph was updated
                    updatedParagraph.shouldBeInstanceOf<Block.Paragraph>()
                    updatedParagraph.type shouldBe "paragraph"
                    updatedParagraph.paragraph.richText[0].plainText shouldBe "Updated paragraph content via direct request"
                    updatedParagraph.paragraph.color shouldBe Color.GREEN

                    println("✅ Paragraph updated successfully using direct request")

                    // Verify the full page content shows the updates
                    delay(1000)
                    val allBlocks = client.blocks.retrieveChildren(testPage.id)
                    allBlocks shouldHaveSize 3 // heading, paragraph, bullet

                    val firstBlock = allBlocks[0] as Block.Heading2
                    firstBlock.heading2.richText[0].plainText shouldBe "Updated Heading via DSL"
                    firstBlock.heading2.color shouldBe Color.BLUE

                    val secondBlock = allBlocks[1] as Block.Paragraph
                    secondBlock.paragraph.richText[0].plainText shouldBe "Updated paragraph content via direct request"
                    secondBlock.paragraph.color shouldBe Color.GREEN

                    println("✅ Full page content verified with updates")
                    println("✅ BlocksApi.update() overload test completed!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        delay(500)
                        println("🧹 Cleaning up test page...")
                        val archivedPage = client.pages.archive(testPage.id)
                        archivedPage.archived shouldBe true
                        println("✅ Test page archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created page: ${testPage.id} (\"Block Update Test Page\")")
                    }
                } finally {
                    client.close()
                }
            }

            "BlocksApi delete should work with live API" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🗑️ Testing BlocksApi.delete() with live API...")

                    // First create a test page with content to delete
                    val testPage =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("Block Delete Test Page")
                            content {
                                heading2("This heading will be deleted")
                                paragraph("This paragraph will remain")
                                bullet("This bullet will be deleted")
                                divider()
                                quote("This quote will remain")
                            }
                        }

                    delay(1000) // Wait for page creation to complete

                    // Get the blocks to delete
                    val blocks = client.blocks.retrieveChildren(testPage.id)
                    blocks shouldHaveSize 5 // heading, paragraph, bullet, divider, quote

                    val headingBlock = blocks[0] // First block is the heading
                    val bulletBlock = blocks[2] // Third block is the bullet

                    println("✅ Created test page with initial content (${blocks.size} blocks)")

                    // Test deleting the heading block
                    val deletedHeading = client.blocks.delete(headingBlock.id)

                    // Verify the heading was archived (not permanently deleted)
                    deletedHeading.shouldBeInstanceOf<Block.Heading2>()
                    deletedHeading.id shouldBe headingBlock.id
                    deletedHeading.archived shouldBe true
                    deletedHeading.type shouldBe "heading_2"

                    println("✅ Heading block deleted (archived) successfully")

                    // Test deleting the bullet block
                    val deletedBullet = client.blocks.delete(bulletBlock.id)

                    // Verify the bullet was archived
                    deletedBullet.shouldBeInstanceOf<Block.BulletedListItem>()
                    deletedBullet.id shouldBe bulletBlock.id
                    deletedBullet.archived shouldBe true
                    deletedBullet.type shouldBe "bulleted_list_item"

                    println("✅ Bullet block deleted (archived) successfully")

                    // Verify the remaining blocks are still present
                    delay(1000)
                    val remainingBlocks = client.blocks.retrieveChildren(testPage.id)
                    remainingBlocks shouldHaveSize 3 // paragraph, divider, quote (2 blocks were archived)

                    // Verify the remaining blocks are the correct ones
                    val firstRemaining = remainingBlocks[0] as Block.Paragraph
                    firstRemaining.paragraph.richText[0].plainText shouldBe "This paragraph will remain"

                    val secondRemaining = remainingBlocks[1] as Block.Divider
                    secondRemaining.type shouldBe "divider"

                    val thirdRemaining = remainingBlocks[2] as Block.Quote
                    thirdRemaining.quote.richText[0].plainText shouldBe "This quote will remain"

                    println("✅ Remaining blocks verified (${remainingBlocks.size} blocks left)")
                    println("✅ BlocksApi.delete() test completed!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        delay(500)
                        println("🧹 Cleaning up test page...")
                        val archivedPage = client.pages.archive(testPage.id)
                        archivedPage.archived shouldBe true
                        println("✅ Test page archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created page: ${testPage.id} (\"Block Delete Test Page\")")
                    }
                } finally {
                    client.close()
                }
            }

            "Combined API overloads workflow should work end-to-end" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔄 Testing combined API overloads workflow...")

                    // Step 1: Create a database using overload
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Combined Workflow Database")
                            properties {
                                title("Project Name")
                                richText("Description")
                                checkbox("Completed")
                            }
                        }

                    delay(1000)

                    // Get the data source from the created database (2025-09-03 API)
                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    // Step 2: Create a page in the data source using overload
                    val page =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Project Name", "API Overload Project")
                                richText("Description", "Testing combined workflow")
                                checkbox("Completed", false)
                            }
                            content {
                                heading1("Project Overview")
                                paragraph("This demonstrates the combined usage of all API overloads.")
                            }
                        }

                    delay(1000)

                    // Step 3: Add more content to the page using overload
                    val blocks =
                        client.blocks.appendChildren(page.id) {
                            heading2("Progress Updates")
                            bullet("Database created successfully")
                            bullet("Page created in data source")
                            bullet("Additional content added")
                            divider()
                            quote("All API overloads working together seamlessly!")
                        }

                    // Verify the complete workflow
                    database.shouldBeInstanceOf<Database>()
                    page.shouldBeInstanceOf<Page>()
                    blocks.shouldBeInstanceOf<BlockList>()

                    page.parent.dataSourceId shouldBe dataSourceId
                    blocks.results shouldHaveSize 6

                    println("✅ Combined workflow completed successfully!")
                    println("   - Database: ${database.id}")
                    println("   - Data Source: $dataSourceId")
                    println("   - Page: ${page.id}")
                    println("   - Blocks added: ${blocks.results.size}")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        delay(500)
                        println("🧹 Cleaning up workflow objects...")

                        // Archive page first, then database
                        client.pages.archive(page.id)
                        delay(500)
                        client.databases.archive(database.id)

                        println("✅ Workflow objects archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created database: ${database.id}")
                        println("   Created page: ${page.id}")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
