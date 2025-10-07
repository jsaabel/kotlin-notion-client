package examples

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.pages.PageProperty

/**
 * Validation tests for all code examples in docs/pages.md
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
 * Run with: ./gradlew test --tests "*PagesExamples"
 */
@Tags("Integration", "RequiresApi", "Examples")
class PagesExamples :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) Pages examples" {
                println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true and required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            // Test data setup
            var testDatabaseId: String? = null
            var testDataSourceId: String? = null
            var testPageId: String? = null

            beforeSpec {
                println("🔧 Setting up test data for pages examples...")

                // Create a database with a data source
                val database =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Pages Examples Test")

                        properties {
                            title("Task Name")
                            select("Status") {
                                option("To Do")
                                option("In Progress")
                                option("Completed")
                            }
                            number("Priority")
                            date("Due Date")
                            checkbox("Is Complete")
                        }
                    }

                testDatabaseId = database.id
                testDataSourceId = database.dataSources.firstOrNull()?.id

                // Create a test page
                if (testDataSourceId != null) {
                    val page =
                        notion.pages.create {
                            parent.dataSource(testDataSourceId!!)
                            properties {
                                title("Task Name", "Test Task")
                                select("Status", "To Do")
                                number("Priority", 5.0)
                            }
                        }
                    testPageId = page.id
                }

                delay(2000)
                println("✅ Test setup complete")
            }

            afterSpec {
                if (shouldCleanupAfterTest() && testDatabaseId != null) {
                    println("🧹 Cleaning up test database...")
                    try {
                        notion.databases.archive(testDatabaseId)
                        println("✅ Cleanup complete")
                    } catch (e: Exception) {
                        println("⚠️ Cleanup failed: ${e.message}")
                    }
                }
            }

            // ========================================
            // Example 1: Retrieve a Page
            // ========================================
            "Example 1: Retrieve a page" {
                println("\n📖 Running Example 1: Retrieve a page")

                val page = notion.pages.retrieve(testPageId!!)

                // Access page metadata
                println("Created: ${page.createdTime}")
                println("Last edited: ${page.lastEditedTime}")
                println("Archived: ${page.archived}")

                // Access properties
                val title = page.properties["Task Name"] as? PageProperty.Title
                println("Title: ${title?.plainText}")

                // Validation
                page.shouldNotBeNull()
                page.archived shouldBe false
                title?.plainText shouldBe "Test Task"

                println("✅ Example 1 passed")
            }

            // ========================================
            // Example 2: Create a Page in a Data Source
            // ========================================
            "Example 2: Create a page in a data source (database row)" {
                println("\n📖 Running Example 2: Create page in data source")

                val page =
                    notion.pages.create {
                        // Specify the data source as parent
                        parent.dataSource(testDataSourceId!!)

                        // Set property values
                        properties {
                            title("Task Name", "Complete documentation")
                            select("Status", "In Progress")
                            number("Priority", 8.0)
                            date("Due Date", "2025-10-15")
                            checkbox("Is Complete", false)
                        }

                        // Optional: Set icon and cover
                        icon.emoji("📝")
                        cover.external("https://images.unsplash.com/photo-1557683316-973673baf926")
                    }

                println("Created page: ${page.id}")

                // Validation
                page.shouldNotBeNull()
                val titleProp = page.properties["Task Name"] as? PageProperty.Title
                titleProp?.plainText shouldBe "Complete documentation"

                val statusProp = page.properties["Status"] as? PageProperty.Select
                statusProp?.select?.name shouldBe "In Progress"

                val priorityProp = page.properties["Priority"] as? PageProperty.Number
                priorityProp?.number shouldBe 8.0

                page.icon?.emoji shouldBe "📝"
                page.cover?.external?.url shouldBe "https://images.unsplash.com/photo-1557683316-973673baf926"

                // Cleanup
                if (shouldCleanupAfterTest()) {
                    notion.pages.archive(page.id)
                }

                println("✅ Example 2 passed")
            }

            // ========================================
            // Example 3: Create a Child Page
            // ========================================
            "Example 3: Create a child page" {
                println("\n📖 Running Example 3: Create child page")

                val childPage =
                    notion.pages.create {
                        // Specify another page as parent
                        parent.page(parentPageId)

                        // For child pages, use title() directly (not in properties block)
                        title("Meeting Notes - Oct 2025")

                        // Optional: Add content blocks immediately
                        content {
                            heading1("Key Takeaways")

                            bullet("Decision made on architecture")
                            bullet("Timeline set for Q1 2026")

                            paragraph("Next meeting scheduled for next month.")
                        }
                    }

                // Validation
                childPage.shouldNotBeNull()
                val title = childPage.properties["title"] as? PageProperty.Title
                title?.plainText shouldBe "Meeting Notes - Oct 2025"

                // Cleanup
                if (shouldCleanupAfterTest()) {
                    notion.pages.archive(childPage.id)
                }

                println("✅ Example 3 passed")
            }

            // ========================================
            // Example 4: Create a Page with Rich Content
            // ========================================
            "Example 4: Create a page with rich content" {
                println("\n📖 Running Example 4: Create page with rich content")

                val page =
                    notion.pages.create {
                        parent.page(parentPageId)

                        // For child pages, use title() directly
                        title("Project Plan")

                        // Add structured content
                        content {
                            heading1("Project Overview")

                            paragraph {
                                text("This project aims to ")
                                bold("revolutionize")
                                text(" how we handle ")
                                italic("data processing")
                                text(".")
                            }

                            heading2("Timeline")

                            number("Phase 1: Research")
                            number("Phase 2: Implementation")
                            number("Phase 3: Testing")

                            divider()

                            callout("⚠️") {
                                text("Note: Timeline subject to change based on resource availability.")
                            }
                        }
                    }

                // Validation
                page.shouldNotBeNull()
                val title = page.properties["title"] as? PageProperty.Title
                title?.plainText shouldBe "Project Plan"

                // Cleanup
                if (shouldCleanupAfterTest()) {
                    notion.pages.archive(page.id)
                }

                println("✅ Example 4 passed")
            }

            // ========================================
            // Example 5: Update Page Properties
            // ========================================
            "Example 5: Update page properties" {
                println("\n📖 Running Example 5: Update page properties")

                val updated =
                    notion.pages.update(testPageId!!) {
                        properties {
                            // Update existing properties
                            select("Status", "Completed")
                            checkbox("Is Complete", true)
                            number("Priority", 10.0)
                        }
                    }

                // Validation
                updated.shouldNotBeNull()
                val statusProp = updated.properties["Status"] as? PageProperty.Select
                statusProp?.select?.name shouldBe "Completed"

                val checkboxProp = updated.properties["Is Complete"] as? PageProperty.Checkbox
                checkboxProp?.checkbox shouldBe true

                val priorityProp = updated.properties["Priority"] as? PageProperty.Number
                priorityProp?.number shouldBe 10.0

                println("✅ Example 5 passed")
            }

            // ========================================
            // Example 6: Update Page Icon and Cover
            // ========================================
            "Example 6: Update page icon and cover" {
                println("\n📖 Running Example 6: Update page icon and cover")

                val updated =
                    notion.pages.update(testPageId!!) {
                        // Update icon
                        icon.emoji("✅")

                        // Update cover
                        cover.external("https://images.unsplash.com/photo-1557683316-973673baf926")
                    }

                // Validation
                updated.shouldNotBeNull()
                updated.icon?.emoji shouldBe "✅"
                updated.cover?.external?.url shouldBe "https://images.unsplash.com/photo-1557683316-973673baf926"

                println("✅ Example 6 passed")
            }

            // ========================================
            // Example 7: Archive a Page
            // ========================================
            "Example 7: Archive a page" {
                println("\n📖 Running Example 8: Archive a page")

                // Create a page to archive
                val pageToArchive =
                    notion.pages.create {
                        parent.dataSource(testDataSourceId!!)
                        properties {
                            title("Task Name", "Page to archive")
                        }
                    }

                // Archive using the dedicated method
                val archived = notion.pages.archive(pageToArchive.id)

                println("Page archived: ${archived.archived}")

                // Validation
                archived.shouldNotBeNull()
                archived.archived shouldBe true

                println("✅ Example 7 passed")
            }

            // ========================================
            // Example 8: Restore an Archived Page
            // ========================================
            "Example 8: Restore an archived page" {
                println("\n📖 Running Example 9: Restore archived page")

                // Create and archive a page
                val pageToRestore =
                    notion.pages.create {
                        parent.dataSource(testDataSourceId!!)
                        properties {
                            title("Task Name", "Page to restore")
                        }
                    }
                notion.pages.archive(pageToRestore.id)

                // Restore it
                val restored =
                    notion.pages.update(pageToRestore.id) {
                        archive(false) // Set archived to false
                    }

                // Validation
                restored.shouldNotBeNull()
                restored.archived shouldBe false

                // Cleanup
                if (shouldCleanupAfterTest()) {
                    notion.pages.archive(pageToRestore.id)
                }

                println("✅ Example 8 passed")
            }

            // ========================================
            // Example 9: Create Task in Project Management DB
            // ========================================
            "Example 9: Create a task in a project management database" {
                println("\n📖 Running Example 10: Create task")

                val task =
                    notion.pages.create {
                        parent.dataSource(testDataSourceId!!)

                        properties {
                            title("Task Name", "Implement feature X")
                            select("Status", "To Do")
                            number("Priority", 8.0)
                            date("Due Date", "2025-10-20")
                        }

                        icon.emoji("🚀")
                    }

                // Validation
                task.shouldNotBeNull()
                task.icon?.emoji shouldBe "🚀"
                val titleProp = task.properties["Task Name"] as? PageProperty.Title
                titleProp?.plainText shouldBe "Implement feature X"

                // Cleanup
                if (shouldCleanupAfterTest()) {
                    notion.pages.archive(task.id)
                }

                println("✅ Example 9 passed")
            }

            // ========================================
            // Example 10: Batch Create Pages
            // ========================================
            "Example 10: Batch create pages" {
                println("\n📖 Running Example 11: Batch create pages")

                val taskNames = listOf("Task 1", "Task 2", "Task 3")

                val createdPages =
                    taskNames.map { taskName ->
                        notion.pages.create {
                            parent.dataSource(testDataSourceId!!)
                            properties {
                                title("Task Name", taskName)
                                select("Status", "To Do")
                            }
                        }
                    }

                println("Created ${createdPages.size} pages")

                // Validation
                createdPages.shouldNotBeEmpty()
                createdPages.size shouldBe 3

                // Cleanup
                if (shouldCleanupAfterTest()) {
                    createdPages.forEach { notion.pages.archive(it.id) }
                }

                println("✅ Example 10 passed")
            }

            // ========================================
            // Example 11: Update Multiple Properties at Once
            // ========================================
            "Example 11: Update multiple properties at once" {
                println("\n📖 Running Example 12: Update multiple properties")

                notion.pages.update(testPageId!!) {
                    properties {
                        select("Status", "Completed")
                        checkbox("Is Complete", true)
                        date("Due Date", "2025-10-06")
                        number("Priority", 95.0)
                    }

                    // Also update the icon to reflect completion
                    icon.emoji("✅")
                }

                // Retrieve and validate
                val updated = notion.pages.retrieve(testPageId)

                val statusProp = updated.properties["Status"] as? PageProperty.Select
                statusProp?.select?.name shouldBe "Completed"

                val checkboxProp = updated.properties["Is Complete"] as? PageProperty.Checkbox
                checkboxProp?.checkbox shouldBe true

                val priorityProp = updated.properties["Priority"] as? PageProperty.Number
                priorityProp?.number shouldBe 95.0

                updated.icon?.emoji shouldBe "✅"

                println("✅ Example 11 passed")
            }

            // ========================================
            // Example 12: Working with Page Properties
            // ========================================
            "Example 12: Accessing properties from retrieved pages" {
                println("\n📖 Running Example 13: Property access patterns")

                val page = notion.pages.retrieve(testPageId!!)

                // Type-safe property access
                val titleProp = page.properties["Task Name"] as? PageProperty.Title
                val title = titleProp?.plainText ?: "Untitled"

                val selectProp = page.properties["Status"] as? PageProperty.Select
                val status = selectProp?.select?.name ?: "No status"

                val numberProp = page.properties["Priority"] as? PageProperty.Number
                val priority = numberProp?.number ?: 0.0

                val dateProp = page.properties["Due Date"] as? PageProperty.Date
                val dueDate = dateProp?.date?.start // ISO date string

                // Validation
                title shouldBe "Test Task"
                status shouldBe "Completed"
                priority shouldBe 95.0
                dueDate shouldBe "2025-10-06"

                println("✅ Example 12 passed")
            }

            // ========================================
            // Example 13: Getting Data Source ID from Database
            // ========================================
            "Example 13: Getting data source ID from database" {
                println("\n📖 Running Example 14: Get data source ID")

                // Retrieve the database
                val database = notion.databases.retrieve(testDatabaseId!!)

                // Get the first data source (usually there's only one)
                val dataSourceId =
                    database.dataSources.firstOrNull()?.id
                        ?: error("No data sources found")

                // Now create a page
                val page =
                    notion.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task Name", "Created using retrieved data source ID")
                        }
                    }

                // Validation
                page.shouldNotBeNull()

                // Cleanup
                if (shouldCleanupAfterTest()) {
                    notion.pages.archive(page.id)
                }

                println("✅ Example 13 passed")
            }
        }
    })
