package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseProperty
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseRequest
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.pages.UpdatePageRequest
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Self-contained integration tests that create their own test data and clean up afterwards.
 *
 * These tests validate the full create/retrieve/archive workflow without requiring
 * pre-existing test data in Notion. They demonstrate real-world usage patterns.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_PARENT_PAGE_ID="your_parent_page_id"
 *    (This should be a page where test databases can be created)
 * 3. Your integration should have permissions to create/read/update pages and databases
 *
 * Run with: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi")
class SelfContainedIntegrationTest :
    StringSpec({

        fun String.withOrWithoutHyphens(): List<String> = listOf(this, this.replace("-", ""))

        "Should create database, create page, retrieve both, then clean up" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_PARENT_PAGE_ID") // TODO: This should have a better name.

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(token = token))

                try {
                    // Step 1: Create a test database with multiple property types
                    println("🗄️ Creating test database...")
                    val databaseRequest =
                        CreateDatabaseRequest(
                            parent =
                                Parent(
                                    type = "page_id",
                                    pageId = parentPageId,
                                ),
                            title = listOf(RequestBuilders.createSimpleRichText("Test Database - Kotlin Client")),
                            properties =
                                mapOf(
                                    "Name" to CreateDatabaseProperty.Title(),
                                    "Description" to CreateDatabaseProperty.RichText(),
                                    "Priority" to CreateDatabaseProperty.Select(),
                                    "Completed" to CreateDatabaseProperty.Checkbox(),
                                    "Score" to CreateDatabaseProperty.Number(),
                                    "Due Date" to CreateDatabaseProperty.Date(),
                                    "Contact" to CreateDatabaseProperty.Email(),
                                ),
                        )

                    val createdDatabase = client.databases.create(databaseRequest)

                    // Verify database creation
                    createdDatabase.objectType shouldBe "database"
                    createdDatabase.title.first().plainText shouldBe "Test Database - Kotlin Client"
                    createdDatabase.properties.size shouldBe 7
                    createdDatabase.properties.containsKey("Name") shouldBe true
                    createdDatabase.properties.containsKey("Priority") shouldBe true
                    createdDatabase.archived shouldBe false

                    println("✅ Database created successfully: ${createdDatabase.id}")

                    // Small delay to ensure Notion has processed the database creation
                    delay(500)

                    // Step 2: Create a test page in the database
                    println("📄 Creating test page in database...")
                    val pageRequest =
                        CreatePageRequest(
                            parent =
                                Parent(
                                    type = "database_id",
                                    databaseId = createdDatabase.id,
                                ),
                            properties =
                                mapOf(
                                    "Name" to
                                        PagePropertyValue.TitleValue(
                                            title = listOf(RequestBuilders.createSimpleRichText("Test Task - Integration Test")),
                                        ),
                                    "Description" to
                                        PagePropertyValue.RichTextValue(
                                            richText =
                                                listOf(
                                                    RequestBuilders.createSimpleRichText(
                                                        "This is a test task created by our Kotlin integration test",
                                                    ),
                                                ),
                                        ),
                                    "Completed" to
                                        PagePropertyValue.CheckboxValue(
                                            checkbox = false,
                                        ),
                                    "Score" to
                                        PagePropertyValue.NumberValue(
                                            number = 85.5,
                                        ),
                                    "Contact" to
                                        PagePropertyValue.EmailValue(
                                            email = "test@example.com",
                                        ),
                                ),
                        )

                    val createdPage = client.pages.create(pageRequest)

                    // Verify page creation
                    createdPage.objectType shouldBe "page"
                    createdPage.archived shouldBe false
                    createdPage.parent.databaseId!!.withOrWithoutHyphens() shouldContainAnyOf createdDatabase.id.withOrWithoutHyphens()

                    println("✅ Page created successfully: ${createdPage.id}")

                    // Small delay to ensure Notion has processed the page creation
                    delay(500)

                    // Step 3: Retrieve database and verify it contains our page structure
                    println("🔍 Retrieving database to verify structure...")
                    val retrievedDatabase = client.databases.retrieve(createdDatabase.id)

                    retrievedDatabase.id shouldBe createdDatabase.id
                    retrievedDatabase.title.first().plainText shouldBe "Test Database - Kotlin Client"
                    retrievedDatabase.properties.size shouldBe 7
                    retrievedDatabase.archived shouldBe false

                    // Step 4: Retrieve page and verify properties
                    println("🔍 Retrieving page to verify properties...")
                    val retrievedPage = client.pages.retrieve(createdPage.id)

                    retrievedPage.id shouldBe createdPage.id
                    retrievedPage.archived shouldBe false
                    retrievedPage.parent.databaseId!!.withOrWithoutHyphens() shouldContainAnyOf createdDatabase.id.withOrWithoutHyphens()

                    // Note: The actual properties structure in responses differs from requests
                    // This demonstrates the difference between PagePropertyValue (requests)
                    // and the JsonObject properties in responses
                    retrievedPage.properties shouldNotBe null

                    println("✅ Retrieved database and page successfully")

                    // Step 5: Update the page to mark it as completed
                    println("✏️ Updating page properties...")
                    val updatedPage =
                        client.pages.update(
                            createdPage.id,
                            UpdatePageRequest(
                                properties =
                                    mapOf(
                                        "Completed" to PagePropertyValue.CheckboxValue(checkbox = true),
                                        "Score" to PagePropertyValue.NumberValue(number = 95.0),
                                    ),
                            ),
                        )

                    updatedPage.id.withOrWithoutHyphens() shouldContainAnyOf createdPage.id.withOrWithoutHyphens()
                    println("✅ Page updated successfully")

                    // Small delay before cleanup
                    delay(500)

                    // Step 6: Clean up - Archive the page
                    println("🧹 Cleaning up - archiving page...")
                    val archivedPage = client.pages.archive(createdPage.id)
                    archivedPage.archived shouldBe true
                    archivedPage.id shouldBe createdPage.id

                    println("✅ Page archived successfully")

                    // Step 7: Clean up - Archive the database
                    println("🧹 Cleaning up - archiving database...")
                    val archivedDatabase = client.databases.archive(createdDatabase.id)
                    archivedDatabase.archived shouldBe true
                    archivedDatabase.id.withOrWithoutHyphens() shouldContainAnyOf createdDatabase.id.withOrWithoutHyphens()

                    println("✅ Database archived successfully")

                    // Step 8: Verify cleanup by trying to retrieve archived objects
                    println("🔍 Verifying cleanup...")
                    val finalPage = client.pages.retrieve(createdPage.id)
                    val finalDatabase = client.databases.retrieve(createdDatabase.id)

                    finalPage.archived shouldBe true
                    finalDatabase.archived shouldBe true

                    println("✅ Cleanup verified - both objects are archived")
                    println("🎉 Integration test completed successfully!")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping self-contained integration test")
                println("   Required environment variables:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_PARENT_PAGE_ID: Page where test database will be created")
                println(
                    "   Example: export NOTION_API_TOKEN='secret_...' && export NOTION_PARENT_PAGE_ID='12345678-1234-1234-1234-123456789abc'",
                )
            }
        }

        "Should create standalone page, retrieve it, then clean up" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_PARENT_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(token = token))

                try {
                    // Create a standalone page (not in a database)
                    println("📄 Creating standalone test page...")
                    val pageRequest =
                        RequestBuilders.createChildPage(
                            parentPageId = parentPageId,
                            title = "Test Standalone Page - Kotlin Client",
                        )

                    val createdPage = client.pages.create(pageRequest)

                    // Verify creation
                    createdPage.objectType shouldBe "page"
                    createdPage.archived shouldBe false
                    createdPage.parent.pageId!!.withOrWithoutHyphens() shouldContainAnyOf parentPageId.withOrWithoutHyphens()

                    println("✅ Standalone page created: ${createdPage.id}")

                    // Small delay to ensure Notion has processed the page creation
                    delay(500)

                    // Retrieve and verify
                    val retrievedPage = client.pages.retrieve(createdPage.id)
                    retrievedPage.id.withOrWithoutHyphens() shouldContainAnyOf createdPage.id.withOrWithoutHyphens()
                    retrievedPage.archived shouldBe false

                    println("✅ Standalone page retrieved successfully")

                    // Clean up
                    println("🧹 Cleaning up standalone page...")
                    val archivedPage = client.pages.archive(createdPage.id)
                    archivedPage.archived shouldBe true

                    println("✅ Standalone page archived successfully")
                    println("🎉 Standalone page test completed!")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping standalone page test - missing environment variables")
            }
        }

        "Should demonstrate comprehensive property types in database" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_PARENT_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(token = token))

                try {
                    // Create database with many property types
                    println("🗄️ Creating comprehensive test database...")
                    val comprehensiveRequest =
                        CreateDatabaseRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            title = listOf(RequestBuilders.createSimpleRichText("Comprehensive Properties Test")),
                            properties =
                                mapOf(
                                    "Title" to CreateDatabaseProperty.Title(),
                                    "Text" to CreateDatabaseProperty.RichText(),
                                    "Number" to CreateDatabaseProperty.Number(),
                                    "Checkbox" to CreateDatabaseProperty.Checkbox(),
                                    "URL" to CreateDatabaseProperty.Url(),
                                    "Email" to CreateDatabaseProperty.Email(),
                                    "Phone" to CreateDatabaseProperty.PhoneNumber(),
                                    "Date" to CreateDatabaseProperty.Date(),
                                    "Single Select" to CreateDatabaseProperty.Select(),
                                    "Multi Select" to CreateDatabaseProperty.MultiSelect(),
                                ),
                        )

                    val database = client.databases.create(comprehensiveRequest)

                    // Verify all property types were created
                    database.properties.size shouldBe 10
                    database.properties.keys shouldBe
                        setOf(
                            "Title",
                            "Text",
                            "Number",
                            "Checkbox",
                            "URL",
                            "Email",
                            "Phone",
                            "Date",
                            "Single Select",
                            "Multi Select",
                        )

                    println("✅ Comprehensive database created with ${database.properties.size} property types")

                    // Clean up
                    delay(500)
                    client.databases.archive(database.id)
                    println("✅ Comprehensive database archived")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping comprehensive properties test - missing environment variables")
            }
        }
    })
