package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseProperty
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseRequest
import no.saabelit.kotlinnotionclient.models.databases.InitialDataSource
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.pages.UpdatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.getCheckboxProperty
import no.saabelit.kotlinnotionclient.models.pages.getEmailProperty
import no.saabelit.kotlinnotionclient.models.pages.getNumberProperty
import no.saabelit.kotlinnotionclient.models.pages.getRichTextAsPlainText
import no.saabelit.kotlinnotionclient.models.pages.getTitleAsPlainText
import no.saabelit.kotlinnotionclient.models.pages.pageProperties
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Self-contained integration tests that create their own test data and clean up afterwards (2025-09-03 API).
 *
 * These tests validate the full create/retrieve/archive workflow without requiring
 * pre-existing test data in Notion. They demonstrate real-world usage patterns.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 *    (This should be a page where test databases can be created)
 * 3. Your integration should have permissions to create/read/update pages and databases
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 *    (Defaults to "true" - objects are archived after test completion)
 *
 * Run with: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi")
class SelfContainedIntegrationTest :
    StringSpec({

        fun String.withOrWithoutHyphens(): List<String> = listOf(this, this.replace("-", ""))

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" {
                println("⏭️ Skipping SelfContainedIntegrationTest - missing environment variables")
                println("   Required: NOTION_API_TOKEN and NOTION_TEST_PAGE_ID")
            }
        } else {
            "Should create database, create page, retrieve both, then clean up" {
                val token = System.getenv("NOTION_API_TOKEN")
                val testPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Step 1: Create a test database with multiple property types (2025-09-03 API)
                    println("🗄️ Creating test database with initial data source...")
                    val databaseRequest =
                        CreateDatabaseRequest(
                            parent =
                                Parent(
                                    type = "page_id",
                                    pageId = testPageId,
                                ),
                            title = listOf(RequestBuilders.createSimpleRichText("Test Database - Kotlin Client")),
                            icon = RequestBuilders.createEmojiIcon("🗄️"),
                            initialDataSource =
                                InitialDataSource(
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
                                ),
                        )

                    val createdDatabase = client.databases.create(databaseRequest)

                    // Step 1b: Get the data source ID from the created database (2025-09-03 API)
                    val dataSourceId =
                        createdDatabase.dataSources.firstOrNull()?.id
                            ?: error("Database should have at least one data source")

                    // Verify database creation (2025-09-03: databases are containers with data sources)
                    createdDatabase.objectType shouldBe "database"
                    createdDatabase.title.first().plainText shouldBe "Test Database - Kotlin Client"
                    createdDatabase.dataSources.size shouldBe 1
                    createdDatabase.archived shouldBe false

                    println("✅ Database created successfully: ${createdDatabase.id}")
                    println("✅ Data source ID: $dataSourceId")

                    // Small delay to ensure Notion has processed the database creation
                    delay(500)

                    // Step 2: Create a test page in the data source (2025-09-03 API uses data_source_id parent)
                    println("📄 Creating test page in data source using clean builder API...")
                    val pageRequest =
                        CreatePageRequest(
                            parent =
                                Parent(
                                    type = "data_source_id",
                                    dataSourceId = dataSourceId,
                                ),
                            icon = RequestBuilders.createEmojiIcon("📋"),
                            properties =
                                pageProperties {
                                    title("Name", "Test Task - Integration Test")
                                    richText(
                                        "Description",
                                        "This is a test task created by our Kotlin integration test",
                                    )
                                    checkbox("Completed", false)
                                    number("Score", 85.5)
                                    email("Contact", "test@example.com")
                                },
                        )

                    val createdPage = client.pages.create(pageRequest)

                    // Verify page creation (2025-09-03: pages have data_source_id parent)
                    createdPage.objectType shouldBe "page"
                    createdPage.archived shouldBe false
                    createdPage.parent.dataSourceId!!.withOrWithoutHyphens() shouldContainAnyOf dataSourceId.withOrWithoutHyphens()

                    println("✅ Page created successfully: ${createdPage.id}")

                    // Small delay to ensure Notion has processed the page creation
                    delay(500)

                    // Step 3: Retrieve database and verify it contains our data source
                    println("🔍 Retrieving database to verify structure...")
                    val retrievedDatabase = client.databases.retrieve(createdDatabase.id)

                    retrievedDatabase.id shouldBe createdDatabase.id
                    retrievedDatabase.title.first().plainText shouldBe "Test Database - Kotlin Client"
                    retrievedDatabase.dataSources.size shouldBe 1
                    retrievedDatabase.archived shouldBe false

                    // Step 4: Retrieve page and verify properties
                    println("🔍 Retrieving page to verify properties...")
                    val retrievedPage = client.pages.retrieve(createdPage.id)

                    retrievedPage.id shouldBe createdPage.id
                    retrievedPage.archived shouldBe false
                    retrievedPage.parent.dataSourceId!!.withOrWithoutHyphens() shouldContainAnyOf dataSourceId.withOrWithoutHyphens()

                    // Verify properties using type-safe access (demonstrating our new API!)
                    println("🔍 Verifying properties with type-safe access...")

                    // Test title property access
                    val title = retrievedPage.getTitleAsPlainText(name = "Name")
                    title shouldBe "Test Task - Integration Test"

                    // Test rich text property access
                    val description = retrievedPage.getRichTextAsPlainText("Description")
                    description shouldBe "This is a test task created by our Kotlin integration test"

                    // Test number property access
                    val score = retrievedPage.getNumberProperty("Score")
                    score shouldBe 85.5

                    // Test checkbox property access
                    val completed = retrievedPage.getCheckboxProperty("Completed")
                    completed shouldBe false

                    // Test email property access
                    val contact = retrievedPage.getEmailProperty("Contact")
                    contact shouldBe "test@example.com"

                    println("✅ Retrieved database and page successfully")
                    println("✅ Type-safe property access validated: title='$title', score=$score, completed=$completed")

                    // Step 5: Update the page to mark it as completed (demonstrating builder API for updates!)
                    println("✏️ Updating page properties using builder DSL...")
                    val updatedPage =
                        client.pages.update(
                            createdPage.id,
                            UpdatePageRequest(
                                properties =
                                    pageProperties {
                                        checkbox("Completed", true)
                                        number("Score", 95.0)
                                    },
                            ),
                        )

                    updatedPage.id.withOrWithoutHyphens() shouldContainAnyOf createdPage.id.withOrWithoutHyphens()

                    // Verify the updated properties using type-safe access
                    val updatedCompleted = updatedPage.getCheckboxProperty("Completed")
                    val updatedScore = updatedPage.getNumberProperty("Score")
                    updatedCompleted shouldBe true
                    updatedScore shouldBe 95.0

                    println("✅ Page updated successfully")
                    println("✅ Updated properties validated: completed=$updatedCompleted, score=$updatedScore")

                    // Small delay before cleanup
                    delay(500)

                    // Step 6: Conditionally clean up based on environment variable
                    if (shouldCleanupAfterTest()) {
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
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created objects for manual inspection:")
                        println("   - Database: ${createdDatabase.id} (\"${createdDatabase.title.first().plainText}\")")
                        println("   - Page: ${createdPage.id} (\"${retrievedPage.getTitleAsPlainText("Name")}\")")
                        println("🎉 Integration test completed successfully (objects preserved)!")
                    }
                } finally {
                    client.close()
                }
            }

            "Should create standalone page, retrieve it, then clean up" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Create a standalone page (not in a database)
                    println("📄 Creating standalone test page...")
                    val pageRequest =
                        CreatePageRequest(
                            parent =
                                Parent(
                                    type = "page_id",
                                    pageId = parentPageId,
                                ),
                            icon = RequestBuilders.createEmojiIcon("📄"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title = listOf(RequestBuilders.createSimpleRichText("Test Standalone Page - Kotlin Client")),
                                        ),
                                ),
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

                    // Conditionally clean up based on environment variable
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up standalone page...")
                        val archivedPage = client.pages.archive(createdPage.id)
                        archivedPage.archived shouldBe true

                        println("✅ Standalone page archived successfully")
                        println("🎉 Standalone page test completed!")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created objects for manual inspection:")
                        println("   - Standalone Page: ${createdPage.id} (\"Test Standalone Page - Kotlin Client\")")
                        println("🎉 Standalone page test completed (page preserved)!")
                    }
                } finally {
                    client.close()
                }
            }

            "Should demonstrate comprehensive property types in database" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Create database with many property types (2025-09-03 API)
                    println("🗄️ Creating comprehensive test database with initial data source...")
                    val comprehensiveRequest =
                        CreateDatabaseRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            title = listOf(RequestBuilders.createSimpleRichText("Comprehensive Properties Test")),
                            icon = RequestBuilders.createEmojiIcon("🧪"),
                            initialDataSource =
                                InitialDataSource(
                                    properties =
                                        mapOf(
                                            "Title" to CreateDatabaseProperty.Title(),
                                            "Text" to CreateDatabaseProperty.RichText(),
                                            "Number" to CreateDatabaseProperty.Number(),
                                            "Checkbox" to CreateDatabaseProperty.Checkbox(),
                                            "URL" to CreateDatabaseProperty.Url(),
                                            "Email" to CreateDatabaseProperty.Email(),
                                            "Phone" to CreateDatabaseProperty.PhoneNumber(),
                                            "Start Date" to CreateDatabaseProperty.Date(),
                                            "Meeting Time" to CreateDatabaseProperty.Date(),
                                            "Duration" to CreateDatabaseProperty.Date(),
                                            "Single Select" to CreateDatabaseProperty.Select(),
                                            "Multi Select" to CreateDatabaseProperty.MultiSelect(),
                                        ),
                                ),
                        )

                    val database = client.databases.create(comprehensiveRequest)

                    // Get the data source ID (2025-09-03 API)
                    val dataSourceId =
                        database.dataSources.firstOrNull()?.id
                            ?: error("Database should have at least one data source")

                    // Verify database was created (2025-09-03: databases are containers with data sources)
                    database.dataSources.size shouldBe 1

                    println("✅ Comprehensive database created with data source: $dataSourceId")

                    // Step 2: Create a page with comprehensive properties (demonstrating complex builder API!)
                    println("📄 Creating page with comprehensive properties using builder API...")
                    delay(500)

                    val pageRequest =
                        CreatePageRequest(
                            parent = Parent(type = "data_source_id", dataSourceId = dataSourceId),
                            icon = RequestBuilders.createEmojiIcon("⭐"),
                            properties =
                                pageProperties {
                                    title("Title", "Comprehensive Test Page")
                                    richText("Text", "This page demonstrates all our property types with the builder API")
                                    number("Number", 42.5)
                                    checkbox("Checkbox", true)
                                    url("URL", "https://notion.so")
                                    email("Email", "test@comprehensive.com")
                                    phoneNumber("Phone", "+1-555-0199")

                                    // Demonstrate various date/datetime formats
                                    date("Start Date", "2024-03-15")
                                    dateTime("Meeting Time", "2024-03-15T14:30:00")
                                    dateRange("Duration", "2024-03-15", "2024-03-22")

                                    // Demonstrate select options
                                    select("Single Select", "High Priority")
                                    multiSelect("Multi Select", "testing", "comprehensive", "api", "kotlin")
                                },
                        )

                    val createdPage = client.pages.create(pageRequest)

                    println("✅ Comprehensive page created: ${createdPage.id}")

                    // Step 3: Retrieve and verify the complex properties
                    println("🔍 Retrieving and verifying comprehensive properties...")
                    delay(500)

                    val retrievedPage = client.pages.retrieve(createdPage.id)

                    // Verify basic properties
                    retrievedPage.getTitleAsPlainText("Title") shouldBe "Comprehensive Test Page"
                    retrievedPage.getRichTextAsPlainText("Text") shouldBe
                        "This page demonstrates all our property types with the builder API"
                    retrievedPage.getNumberProperty("Number") shouldBe 42.5
                    retrievedPage.getCheckboxProperty("Checkbox") shouldBe true
                    retrievedPage.getEmailProperty("Email") shouldBe "test@comprehensive.com"

                    println("✅ Basic properties verified successfully")

                    // Step 4: Update page with more complex date/time examples
                    println("✏️ Updating page with advanced date/time properties...")

                    val updatedPage =
                        client.pages.update(
                            createdPage.id,
                            UpdatePageRequest(
                                properties =
                                    pageProperties {
                                        // Demonstrate timezone handling - need datetime when using timezone
                                        dateTimeWithTimeZone("Start Date", "2024-04-01T08:00:00", "America/Los_Angeles")
                                        dateTimeWithTimeZone("Meeting Time", "2024-04-01T09:00:00", "UTC")
                                        dateTimeRange("Duration", "2024-04-01T09:00:00", "2024-04-01T17:00:00")

                                        // Update other properties too
                                        number("Number", 100.0)
                                        multiSelect("Multi Select", "updated", "timezone", "demo")
                                    },
                            ),
                        )

                    println("✅ Page updated with advanced date/time properties")

                    // Step 5: Conditionally clean up based on environment variable
                    delay(500)
                    if (shouldCleanupAfterTest()) {
                        client.pages.archive(createdPage.id)
                        client.databases.archive(database.id)
                        println("✅ Comprehensive test completed and cleaned up")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created objects for manual inspection:")
                        println("   - Database: ${database.id} (\"Comprehensive Properties Test\")")
                        println("   - Page: ${createdPage.id} (\"${retrievedPage.getTitleAsPlainText("Title")}\")")
                        println("✅ Comprehensive test completed (objects preserved for inspection)")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
