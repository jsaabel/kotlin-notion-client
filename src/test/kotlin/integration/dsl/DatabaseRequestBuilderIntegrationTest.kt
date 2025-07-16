@file:Suppress("unused")

package integration.dsl

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.SelectOptionColor
import no.saabelit.kotlinnotionclient.models.databases.DatabaseProperty
import no.saabelit.kotlinnotionclient.models.databases.databaseRequest

/**
 * Self-contained integration test for the DatabaseRequestBuilder DSL.
 *
 * This test validates the full workflow of creating databases using the DSL,
 * uploading them to Notion, and verifying the structure.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update databases
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 *
 * Run with: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi")
class DatabaseRequestBuilderIntegrationTest :
    StringSpec({

        // Helper function to check if cleanup should be performed after tests
        fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"

        "Should create database with DSL and verify structure" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    println("🗄️ Creating test database with DatabaseRequestBuilder DSL...")

                    // Create database using DSL
                    val databaseRequest =
                        databaseRequest {
                            parent.page(parentPageId)
                            title("DSL Integration Test Database")
                            description("This database was created using the DatabaseRequestBuilder DSL!")
                            icon.emoji("🚀")
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
                                }
                                date("Due Date")
                                url("Reference URL")
                                email("Assignee Email")
                                phoneNumber("Phone")
                                people("Assignee")
                            }
                        }

                    val createdDatabase = client.databases.create(databaseRequest)
                    createdDatabase.objectType shouldBe "database"
                    createdDatabase.archived shouldBe false

                    println("✅ Database created: ${createdDatabase.id}")

                    // Small delay to ensure Notion has processed the database creation
                    delay(500)

                    // Verify database properties
                    createdDatabase.parent.pageId shouldBe parentPageId
                    createdDatabase.icon?.emoji shouldBe "🚀"
                    createdDatabase.cover?.external?.url shouldContain "placehold"

                    // Verify the title was set correctly
                    createdDatabase.title.shouldNotBeNull()
                    createdDatabase.title.shouldHaveSize(1)
                    createdDatabase.title[0].plainText shouldBe "DSL Integration Test Database"

                    // Verify description
                    createdDatabase.description.shouldNotBeNull()
                    createdDatabase.description shouldHaveSize (1)
                    createdDatabase.description[0].plainText shouldBe "This database was created using the DatabaseRequestBuilder DSL!"

                    // Verify all properties were created
                    createdDatabase.properties.size shouldBe 11
                    createdDatabase.properties.keys shouldBe
                        setOf(
                            "Task Name",
                            "Description",
                            "Priority",
                            "Completed",
                            "Status",
                            "Tags",
                            "Due Date",
                            "Reference URL",
                            "Assignee Email",
                            "Phone",
                            "Assignee",
                        )

                    // Verify specific property types
                    createdDatabase.properties["Task Name"].shouldBeInstanceOf<DatabaseProperty.Title>()
                    createdDatabase.properties["Description"].shouldBeInstanceOf<DatabaseProperty.RichText>()
                    createdDatabase.properties["Priority"].shouldBeInstanceOf<DatabaseProperty.Number>()
                    createdDatabase.properties["Completed"].shouldBeInstanceOf<DatabaseProperty.Checkbox>()
                    createdDatabase.properties["Status"].shouldBeInstanceOf<DatabaseProperty.Select>()
                    createdDatabase.properties["Tags"].shouldBeInstanceOf<DatabaseProperty.MultiSelect>()
                    createdDatabase.properties["Due Date"].shouldBeInstanceOf<DatabaseProperty.Date>()
                    createdDatabase.properties["Reference URL"].shouldBeInstanceOf<DatabaseProperty.Url>()
                    createdDatabase.properties["Assignee Email"].shouldBeInstanceOf<DatabaseProperty.Email>()
                    createdDatabase.properties["Phone"].shouldBeInstanceOf<DatabaseProperty.PhoneNumber>()
                    createdDatabase.properties["Assignee"].shouldBeInstanceOf<DatabaseProperty.People>()

                    println("✅ Database properties verified")

                    // Verify select options
                    val statusProperty = createdDatabase.properties["Status"] as DatabaseProperty.Select
                    statusProperty.select.options.shouldHaveSize(3)
                    statusProperty.select.options.map { it.name } shouldBe listOf("To Do", "In Progress", "Done")

                    val tagsProperty = createdDatabase.properties["Tags"] as DatabaseProperty.MultiSelect
                    tagsProperty.multiSelect.options.shouldHaveSize(2)
                    tagsProperty.multiSelect.options.map { it.name } shouldBe listOf("Important", "Urgent")

                    println("✅ DatabaseRequestBuilder DSL integration test completed successfully!")
                    println("   - DSL created database with all components: ✅")
                    println("   - Properties configuration working: ✅")
                    println("   - Icon and cover working: ✅")
                    println("   - Type safety enforced: ✅")

                    // Conditionally clean up
                    delay(500)
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up - archiving test database...")
                        val archivedDatabase = client.databases.archive(createdDatabase.id)
                        archivedDatabase.archived shouldBe true
                        println("✅ Test database archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created database: ${createdDatabase.id} (\"DSL Integration Test Database\")")
                        println("   Contains 11 properties with comprehensive type coverage")
                    }
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping DatabaseRequestBuilder DSL integration test")
                println("   Required environment variables:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_TEST_PAGE_ID: Page where test content will be created")
                println(
                    "   Example: export NOTION_API_TOKEN='secret_...' && export NOTION_TEST_PAGE_ID='12345678-1234-1234-1234-123456789abc'",
                )
            }
        }

        "Should create minimal database with DSL" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    println("🗄️ Creating minimal test database...")

                    val databaseRequest =
                        databaseRequest {
                            parent.page(parentPageId)
                            title("Minimal DSL Database")
                            properties {
                                title("Name")
                            }
                        }

                    val createdDatabase = client.databases.create(databaseRequest)
                    createdDatabase.objectType shouldBe "database"
                    createdDatabase.title[0].plainText shouldBe "Minimal DSL Database"
                    createdDatabase.properties.size shouldBe 1
                    createdDatabase.properties.keys shouldBe setOf("Name")

                    println("✅ Minimal database created and verified")

                    // Clean up
                    if (shouldCleanupAfterTest()) {
                        client.databases.archive(createdDatabase.id)
                        println("✅ Minimal database archived")
                    }
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping minimal database test - missing environment variables")
            }
        }

        "Should validate DSL constraints properly" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    println("🔍 Testing DSL validation constraints...")

                    // Test that required fields are validated
                    try {
                        databaseRequest {
                            // Missing parent - should fail
                            title("Invalid Database")
                            properties {
                                title("Name")
                            }
                        }
                        throw AssertionError("Expected validation to fail but it didn't")
                    } catch (e: IllegalArgumentException) {
                        e.message shouldContain "Parent must be specified"
                        println("✅ Parent validation working correctly")
                    }

                    // Test that title validation works
                    try {
                        databaseRequest {
                            parent.page(parentPageId)
                            // Missing title - should fail
                            properties {
                                title("Name")
                            }
                        }
                        throw AssertionError("Expected validation to fail but it didn't")
                    } catch (e: IllegalArgumentException) {
                        e.message shouldContain "Title must be specified"
                        println("✅ Title validation working correctly")
                    }

                    // Test that properties validation works
                    try {
                        databaseRequest {
                            parent.page(parentPageId)
                            title("Invalid Database")
                            // Missing properties - should fail
                        }
                        throw AssertionError("Expected validation to fail but it didn't")
                    } catch (e: IllegalArgumentException) {
                        e.message shouldContain "Database must have at least one property"
                        println("✅ Properties validation working correctly")
                    }

                    // Test that valid request works
                    val validRequest =
                        databaseRequest {
                            parent.page(parentPageId)
                            title("Valid Database")
                            properties {
                                title("Name")
                            }
                        }

                    validRequest.title.shouldNotBeNull()
                    validRequest.properties.shouldNotBeNull()
                    println("✅ Valid request construction working correctly")

                    println("✅ DSL validation tests completed successfully!")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping validation test - missing environment variables")
            }
        }
    })
