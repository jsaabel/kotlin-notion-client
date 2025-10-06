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
import no.saabelit.kotlinnotionclient.models.databases.SortDirection
import no.saabelit.kotlinnotionclient.models.pages.PageProperty

/**
 * Validation tests for all code examples in docs/data-sources.md
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
 * Run with: ./gradlew test --tests "*DataSourcesExamples"
 */
@Tags("Integration", "RequiresApi", "Examples")
class DataSourcesExamples :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) Data sources examples" {
                println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true and required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            // Test data setup - create once, use for all examples
            var testDatabaseId: String? = null
            var testDataSourceId: String? = null

            beforeSpec {
                println("🔧 Setting up test database and data source...")

                // Create a database with initial data source
                val database =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("DataSources Examples Test")
                        description("Test database for documentation examples")

                        properties {
                            title("Task Name")
                            select("Status") {
                                option("To Do")
                                option("In Progress")
                                option("Done")
                            }
                            number("Priority")
                            date("Due Date")
                            checkbox("Completed")
                        }
                    }

                testDatabaseId = database.id
                testDataSourceId = database.dataSources.firstOrNull()?.id

                // Create some test pages
                if (testDataSourceId != null) {
                    notion.pages.create {
                        parent.dataSource(testDataSourceId!!)
                        properties {
                            title("Task Name", "High priority task")
                            select("Status", "In Progress")
                            number("Priority", 8.0)
                        }
                    }

                    notion.pages.create {
                        parent.dataSource(testDataSourceId!!)
                        properties {
                            title("Task Name", "Low priority task")
                            select("Status", "To Do")
                            number("Priority", 3.0)
                        }
                    }

                    notion.pages.create {
                        parent.dataSource(testDataSourceId!!)
                        properties {
                            title("Task Name", "Overdue task")
                            select("Status", "To Do")
                            date("Due Date", "2025-01-01")
                        }
                    }
                }

                delay(2000) // Allow API to process
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
            // Example 1: Retrieve a Data Source
            // ========================================
            "Example 1: Retrieve a data source" {
                println("\n📖 Running Example 1: Retrieve a data source")

                val dataSource = notion.dataSources.retrieve(testDataSourceId!!)

                val name = dataSource.title.firstOrNull()?.plainText ?: "Untitled"
                println("Name: $name")
                println("Properties:")
                dataSource.properties.forEach { (propName, config) ->
                    println("  - $propName: ${config.type}")
                }

                // Validation
                dataSource.shouldNotBeNull()
                dataSource.properties.values.shouldNotBeEmpty()

                println("✅ Example 1 passed")
            }

            // ========================================
            // Example 2: Query Pages - Simple
            // ========================================
            "Example 2: Query pages from a data source (simple)" {
                println("\n📖 Running Example 2: Simple query")
                val allPages = notion.dataSources.query(testDataSourceId!!) {}

                allPages.forEach { page ->
                    println(page.properties["Task Name"])
                }

                // Validation
                allPages.shouldNotBeEmpty()
                println("✅ Example 2 passed - Found ${allPages.size} pages")
            }

            // ========================================
            // Example 3: Query with Filters and Sorting
            // ========================================
            "Example 3: Query with filters and sorting" {
                println("\n📖 Running Example 3: Filtered and sorted query")
                val filteredPages =
                    notion.dataSources.query(testDataSourceId!!) {
                        filter {
                            and(
                                select("Status").equals("In Progress"),
                                number("Priority").greaterThan(5.0),
                            )
                        }

                        sortBy("Due Date", SortDirection.ASCENDING)
                        sortBy("Priority", SortDirection.DESCENDING)
                    }

                // Validation
                println("Found ${filteredPages.size} filtered pages")
                filteredPages.forEach { page ->
                    val status = (page.properties["Status"] as? PageProperty.Select)?.select?.name
                    println("  - Status: $status")
                }
                println("✅ Example 3 passed")
            }

            // ========================================
            // Example 4: Create a Data Source
            // ========================================
            "Example 4: Create a data source" {
                println("\n📖 Running Example 4: Create a data source")
                val dataSource =
                    notion.dataSources.create {
                        databaseId(testDatabaseId!!)
                        title("Projects") // Name of the new table

                        properties {
                            title("Project Name")

                            select("Status") {
                                option("Planning")
                                option("Active")
                                option("Completed")
                            }

                            date("Start Date")
                            date("End Date")

                            people("Team Members")

                            number("Budget")
                        }
                    }

                println("Created data source: ${dataSource.id}")

                // Validation
                dataSource.shouldNotBeNull()
                dataSource.title.firstOrNull()?.plainText shouldBe "Projects"
                dataSource.properties.values.shouldNotBeEmpty()

                println("✅ Example 4 passed")
            }

            // ========================================
            // Example 5: Update Data Source Schema
            // ========================================
            "Example 5: Update data source schema" {
                println("\n📖 Running Example 5: Update data source schema")
                val updated =
                    notion.dataSources.update(testDataSourceId!!) {
                        // Update the title
                        title("Updated Projects")

                        // Modify properties
                        properties {
                            // Re-define existing properties to keep them
                            title("Task Name")
                            select("Status") {
                                option("To Do")
                                option("In Progress") // Added
                                option("On Hold") // Added
                                option("Done")
                            }
                            number("Priority")
                            date("Due Date")
                            checkbox("Completed")

                            // Add a new property
                            checkbox("Is Critical")
                        }
                    }

                // Validation
                updated.shouldNotBeNull()
                updated.title.firstOrNull()?.plainText shouldBe "Updated Projects"

                println("✅ Example 5 passed")
            }

            // ========================================
            // Example 6: Getting Data Source ID from Database
            // ========================================
            "Example 6: Getting a data source ID from a database" {
                println("\n📖 Running Example 6: Get data source ID from database")
                // After creating a database
                val database =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("My Database")
                        properties {
                            title("Name")
                        }
                    }

                // Get the first (and usually only) data source
                val dataSourceId =
                    database.dataSources.firstOrNull()?.id
                        ?: error("Database has no data sources")

                // Or from an existing database
                val existingDb = notion.databases.retrieve(database.id)
                val dataSourceIdFromRetrieve = existingDb.dataSources.firstOrNull()?.id

                // Validation
                dataSourceId.shouldNotBeNull()
                dataSourceIdFromRetrieve shouldBe dataSourceId

                // Cleanup
                if (shouldCleanupAfterTest()) {
                    notion.databases.archive(database.id)
                }

                println("✅ Example 6 passed")
            }

            // ========================================
            // Example 7: Automatic Pagination
            // ========================================
            "Example 7: Automatic pagination" {
                println("\n📖 Running Example 7: Automatic pagination")
                // This will fetch ALL pages, even if there are thousands
                val allPages =
                    notion.dataSources.query(testDataSourceId!!) {
                        filter {
                            select("Status").equals("To Do")
                        }
                    }

                println("Total matching pages: ${allPages.size}")

                // Validation - we know we have at least 2 "To Do" pages from setup
                allPages.size shouldBe 2

                println("✅ Example 7 passed")
            }

            // ========================================
            // Example 8: Complex Filters
            // ========================================
            "Example 8: Complex filters" {
                println("\n📖 Running Example 8: Complex filters")
                val results =
                    notion.dataSources.query(testDataSourceId!!) {
                        filter {
                            or(
                                // High priority tasks
                                and(
                                    number("Priority").greaterThanOrEqualTo(8.0),
                                    select("Status").equals("To Do"),
                                ),
                                // Overdue tasks
                                date("Due Date").before("2025-10-05"),
                            )
                        }
                    }

                // Validation - should find at least the overdue task
                results.shouldNotBeEmpty()
                println("Found ${results.size} matching pages")

                println("✅ Example 8 passed")
            }

            // ========================================
            // Example 9: Working with Properties
            // ========================================
            "Example 9: Working with properties" {
                println("\n📖 Running Example 9: Working with properties")
                val pages = notion.dataSources.query(testDataSourceId!!) {}

                pages.forEach { page ->
                    // Access different property types
                    val titleProp = page.properties["Task Name"] as? PageProperty.Title
                    val title = titleProp?.plainText

                    val selectProp = page.properties["Status"] as? PageProperty.Select
                    val status = selectProp?.select?.name

                    println("$title - $status")
                }

                // Validation
                pages.shouldNotBeEmpty()

                println("✅ Example 9 passed")
            }
        }
    })
