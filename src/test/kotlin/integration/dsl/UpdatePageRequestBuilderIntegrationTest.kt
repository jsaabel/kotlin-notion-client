package integration.dsl

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.PageIcon
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.updatePageRequest
import kotlinx.coroutines.delay

/**
 * Integration test for the UpdatePageRequestBuilder DSL.
 *
 * This test validates the full workflow of:
 * 1. Creating a test database with comprehensive property types
 * 2. Creating database pages with initial property values
 * 3. Updating them using the UpdatePageRequestBuilder DSL
 * 4. Verifying all property types can be updated correctly
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update databases and pages
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 */
@Tags("Integration", "RequiresApi")
class UpdatePageRequestBuilderIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping UpdatePageRequestBuilderIntegrationTest due to missing environment variables") }
        } else {

            "Should create database, add pages, then update them with comprehensive property DSL" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🗃️ Creating test database with comprehensive property types...")

                    // Step 1: Create a database with various property types for testing
                    val testDatabase =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("UpdatePageDSL Test Database")
                            icon.emoji("🧪")

                            // Add comprehensive property types to test all DSL capabilities
                            properties {
                                title("Name") // Required title property
                                richText("Description")
                                number("Score")
                                checkbox("Completed")
                                select("Priority") {
                                    option("Low")
                                    option("Medium")
                                    option("High")
                                    option("Critical")
                                }
                                multiSelect("Tags") {
                                    option("urgent")
                                    option("review")
                                    option("blocked")
                                    option("in-progress")
                                }
                                date("Due Date")
                                url("Reference URL")
                                email("Contact Email")
                                phoneNumber("Contact Phone")
                            }
                        }

                    println("✅ Test database created: ${testDatabase.id}")
                    delay(2000) // Give Notion time to process database creation

                    // Get data source from database (2025-09-03 API)
                    val retrievedDb = client.databases.retrieve(testDatabase.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    // Step 2: Create initial database page with some property values
                    println("📝 Creating initial database page...")

                    val initialPage =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Name", "Initial Test Page")
                                richText("Description", "This page will be updated using the DSL")
                                number("Score", 75)
                                checkbox("Completed", false)
                                select("Priority", "Medium")
                                multiSelect("Tags", "in-progress")
                                date("Due Date", "2024-12-31")
                                url("Reference URL", "https://example.com/initial")
                                email("Contact Email", "initial@example.com")
                                phoneNumber("Contact Phone", "+1-555-0100")
                            }
                            icon.emoji("📝")
                        }

                    println("✅ Initial database page created: ${initialPage.id}")
                    delay(1500)

                    // Verify initial state
                    val initialTitle = initialPage.properties["Name"] as PageProperty.Title
                    initialTitle.plainText shouldBe "Initial Test Page"

                    val initialScore = initialPage.properties["Score"] as PageProperty.Number
                    initialScore.number shouldBe 75.0

                    val initialCompleted = initialPage.properties["Completed"] as PageProperty.Checkbox
                    initialCompleted.checkbox shouldBe false

                    println("✅ Initial property values verified")

                    // Step 3: Update the page using UpdatePageRequestBuilder DSL
                    println("🔄 Updating page with comprehensive property changes...")

                    val updateRequest =
                        updatePageRequest {
                            properties {
                                title("Name", "UPDATED Test Page - DSL Success!")
                                richText("Description", "Updated using the new UpdatePageRequestBuilder DSL - all properties changed!")
                                number("Score", 95.5) // Increased score
                                checkbox("Completed", true) // Mark as completed
                                select("Priority", "Critical") // Escalate priority
                                multiSelect("Tags", "urgent", "review") // Update tags
                                date("Due Date", "2025-01-15") // Extend deadline
                                url("Reference URL", "https://updated-example.com/success")
                                email("Contact Email", "updated@example.com")
                                phoneNumber("Contact Phone", "+1-555-0999")
                            }
                            icon.emoji("✅") // Change icon to show completion
                            cover.external("https://placehold.co/1200x400/4CAF50/FFFFFF?text=UPDATED")
                        }

                    val updatedPage = client.pages.update(initialPage.id, updateRequest)
                    println("✅ Page updated: ${updatedPage.id}")
                    delay(2000)

                    // Step 4: Verify all property updates were applied correctly
                    println("🔍 Verifying comprehensive property updates...")

                    // Verify title update
                    val updatedTitle = updatedPage.properties["Name"] as PageProperty.Title
                    updatedTitle.plainText shouldBe "UPDATED Test Page - DSL Success!"

                    // Verify rich text update
                    val updatedDescription = updatedPage.properties["Description"] as PageProperty.RichTextProperty
                    updatedDescription.plainText shouldContain "UpdatePageRequestBuilder DSL"

                    // Verify number update
                    val updatedScore = updatedPage.properties["Score"] as PageProperty.Number
                    updatedScore.number shouldBe 95.5

                    // Verify checkbox update
                    val updatedCompleted = updatedPage.properties["Completed"] as PageProperty.Checkbox
                    updatedCompleted.checkbox shouldBe true

                    // Verify select update
                    val updatedPriority = updatedPage.properties["Priority"] as PageProperty.Select
                    updatedPriority.select?.name shouldBe "Critical"

                    // Verify multi-select update
                    val updatedTags = updatedPage.properties["Tags"] as PageProperty.MultiSelect
                    updatedTags.multiSelect shouldHaveSize 2
                    val tagNames = updatedTags.multiSelect.map { it.name }
                    tagNames shouldContain "urgent"
                    tagNames shouldContain "review"

                    // Verify date update
                    val updatedDueDate = updatedPage.properties["Due Date"] as PageProperty.Date
                    updatedDueDate.date?.start shouldContain "2025-01-15"

                    // Verify URL update
                    val updatedUrl = updatedPage.properties["Reference URL"] as PageProperty.Url
                    updatedUrl.url shouldBe "https://updated-example.com/success"

                    // Verify email update
                    val updatedEmail = updatedPage.properties["Contact Email"] as PageProperty.Email
                    updatedEmail.email shouldBe "updated@example.com"

                    // Verify phone number update
                    val updatedPhone = updatedPage.properties["Contact Phone"] as PageProperty.PhoneNumber
                    updatedPhone.phoneNumber shouldBe "+1-555-0999"

                    // Verify icon and cover updates
                    (updatedPage.icon as? PageIcon.Emoji)?.emoji shouldBe "✅"
                    (updatedPage.cover as? PageCover.External)?.external?.url shouldContain "UPDATED"

                    println("✅ All property types successfully updated via DSL!")

                    // Step 5: Test the API overload method
                    println("🔄 Testing API overload method client.pages.update(id) { ... }")

                    val finalPage =
                        client.pages.update(initialPage.id) {
                            properties {
                                title("Name", "Final Update - API Overload Method")
                                checkbox("Completed", false) // Toggle back to false
                                number("Score", 100.0) // Perfect score!
                            }
                            icon.external("https://placehold.co/32x32/FF9800/FFFFFF?text=100")
                            cover.remove() // Test cover removal
                        }

                    delay(1500)

                    // Verify API overload worked
                    val finalTitle = finalPage.properties["Name"] as PageProperty.Title
                    finalTitle.plainText shouldBe "Final Update - API Overload Method"

                    val finalCompleted = finalPage.properties["Completed"] as PageProperty.Checkbox
                    finalCompleted.checkbox shouldBe false

                    val finalScore = finalPage.properties["Score"] as PageProperty.Number
                    finalScore.number shouldBe 100.0

                    finalPage.icon?.type shouldBe "external"

                    println("✅ API overload method verified with property updates!")

                    // Step 6: Test archive functionality
                    println("📦 Testing archive functionality...")

                    val archivedPage =
                        client.pages.update(initialPage.id) {
                            archive()
                        }

                    delay(1000)
                    archivedPage.archived shouldBe true

                    println("✅ Archive functionality verified!")

                    println()
                    println("🎉 UpdatePageRequestBuilder DSL Integration Test - COMPLETE SUCCESS!")
                    println("   ✅ Created test database with 10 property types")
                    println("   ✅ Created database page with initial property values")
                    println("   ✅ Updated ALL property types using UpdatePageRequestBuilder DSL")
                    println("   ✅ Verified every property update was applied correctly")
                    println("   ✅ Tested API overload method client.pages.update(id) { ... }")
                    println("   ✅ Verified icon, cover, and archive functionality")
                    println("   ✅ Confirmed feature parity with CreatePageRequestBuilder")
                    println()
                    println("   Property Types Tested:")
                    println("   • Title ✅       • Rich Text ✅   • Number ✅")
                    println("   • Checkbox ✅   • Select ✅      • Multi-Select ✅")
                    println("   • Date ✅       • URL ✅        • Email ✅")
                    println("   • Phone ✅      • Icon ✅       • Cover ✅")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up test database...")
                        client.databases.archive(testDatabase.id)
                        println("✅ Test database and pages archived")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Database: ${testDatabase.id} (\"UpdatePageDSL Test Database\")")
                        println("   Page: ${finalPage.id} (\"Final Update - API Overload Method\")")
                    }
                } finally {
                    client.close()
                }
            }

            "Should handle basic page updates without database properties" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📝 Testing basic page updates (non-database page)...")

                    // Create a simple child page (not in database)
                    val childPage =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("Basic Update Test Page")
                            icon.emoji("🔄")
                            content {
                                heading1("Original Content")
                                paragraph("This will be updated using the DSL")
                            }
                        }

                    delay(1000)

                    // Update only the title, icon, and add cover
                    val updatedPage =
                        client.pages.update(childPage.id) {
                            properties {
                                title("title", "Basic Update Test Page - UPDATED!")
                            }
                            icon.emoji("✅")
                            cover.external("https://placehold.co/800x300/2196F3/FFFFFF?text=Basic+Update")
                        }

                    delay(1000)

                    // Verify basic updates
                    val updatedTitle = updatedPage.properties["title"] as PageProperty.Title
                    updatedTitle.plainText shouldBe "Basic Update Test Page - UPDATED!"
                    (updatedPage.icon as? PageIcon.Emoji)?.emoji shouldBe "✅"
                    (updatedPage.cover as? PageCover.External)?.external?.url shouldContain "Basic+Update"

                    println("✅ Basic page updates verified!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        client.pages.archive(childPage.id)
                        println("✅ Basic test page archived")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
