package integration.dsl

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.databases.SortDirection
import kotlinx.coroutines.delay

/**
 * Integration tests for the Database Query DSL functionality.
 *
 * These tests validate the Query DSL against the live Notion API, ensuring
 * real-world compatibility and end-to-end functionality.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update databases and pages
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 */
@Tags("Integration", "RequiresApi")
class DatabaseQueryDslIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping DatabaseQueryDslIntegrationTest due to missing environment variables") }
        } else {

            lateinit var client: NotionClient
            lateinit var testDatabaseId: String
            lateinit var dataSourceId: String

            beforeSpec {
                val apiToken = System.getenv("NOTION_API_TOKEN")
                val testPageId = System.getenv("NOTION_TEST_PAGE_ID")

                client = NotionClient.create(NotionConfig(apiToken))

                // Create a single test database with various property types for comprehensive testing
                println("🗃️ Creating test database for Query DSL tests...")
                val testDatabase =
                    client.databases.create {
                        parent.page(testPageId)
                        title("Query DSL Integration Test Database")
                        description("Single database for Query DSL integration tests")

                        properties {
                            title("Task Name")
                            richText("Description")
                            number("Priority", format = "number")
                            checkbox("Completed")
                            select("Status") {
                                option("QueryDSL_ToDo")
                                option("QueryDSL_InProgress")
                                option("QueryDSL_Done")
                            }
                            multiSelect("Tags") {
                                option("QueryDSL_Urgent")
                                option("QueryDSL_Important")
                                option("QueryDSL_NiceToHave")
                            }
                            date("Due Date")
                            email("Assignee Email")
                            url("Reference URL")
                            phoneNumber("Contact Phone")
                        }
                    }

                testDatabaseId = testDatabase.id
                println("✅ Created test database: ${testDatabase.id}")

                // Get data source from database (2025-09-03 API)
                delay(500)
                val retrievedDb = client.databases.retrieve(testDatabaseId)
                dataSourceId = retrievedDb.dataSources.first().id
                println("✅ Retrieved data source: $dataSourceId")

                // Create some test pages with varied data for all tests to use
                println("📝 Creating test pages...")
                val testPages =
                    listOf(
                        Triple("High Priority Task", 5, "QueryDSL_ToDo"),
                        Triple("Medium Priority Task", 3, "QueryDSL_InProgress"),
                        Triple("Low Priority Task", 1, "QueryDSL_Done"),
                        Triple("Urgent High Task", 5, "QueryDSL_ToDo"),
                        Triple("Completed Medium Task", 3, "QueryDSL_Done"),
                    )

                testPages.forEach { (name, priority, status) ->
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task Name", name)
                            richText("Description", "Description for $name")
                            number("Priority", priority)
                            checkbox("Completed", status == "QueryDSL_Done")
                            select("Status", status)
                            if (name.contains("Urgent")) {
                                multiSelect("Tags", "QueryDSL_Urgent", "QueryDSL_Important")
                            } else {
                                multiSelect("Tags", "QueryDSL_NiceToHave")
                            }
                            date("Due Date", "2024-12-31")
                            email("Assignee Email", "test@example.com")
                            url("Reference URL", "https://example.com")
                            phoneNumber("Contact Phone", "555-0123")
                        }
                    }
                }

                // Wait a moment for data to be available
                delay(2000)
                println("✅ Test database setup complete with ${testPages.size} test pages")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    try {
                        client.databases.archive(testDatabaseId)
                        println("✅ Archived test database: $testDatabaseId")
                    } catch (e: Exception) {
                        println("Failed to cleanup test database: ${e.message}")
                    }
                }
            }

            "Query DSL - simple filter should return matching results" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            checkbox("Completed").equals(false)
                        }
                    }

                pages.shouldNotBeEmpty()
                pages.size shouldBeGreaterThan 0
                println("Found ${pages.size} incomplete tasks")
            }

            "Query DSL - complex AND filter should work correctly" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            and(
                                number("Priority").greaterThan(3),
                                select("Status").doesNotEqual("QueryDSL_Done"),
                            )
                        }
                    }

                pages.shouldNotBeEmpty()
                println("Found ${pages.size} high priority incomplete tasks")
            }

            "Query DSL - complex OR filter should work correctly" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            or(
                                select("Status").equals("QueryDSL_Done"),
                                number("Priority").equals(5),
                            )
                        }
                    }

                pages.shouldNotBeEmpty()
                println("Found ${pages.size} completed or high priority tasks")
            }

            "Query DSL - nested logical filters should work correctly" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            and(
                                title("Task Name").contains("Task"),
                                or(
                                    number("Priority").greaterThan(3),
                                    select("Status").equals("QueryDSL_Done"),
                                ),
                            )
                        }
                    }

                pages.shouldNotBeEmpty()
                println("Found ${pages.size} tasks matching nested criteria")
            }

            "Query DSL - text filters should work correctly" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            and(
                                title("Task Name").contains("Priority"),
                                richText("Description").startsWith("Description"),
                                email("Assignee Email").endsWith("example.com"),
                            )
                        }
                    }

                pages.shouldNotBeEmpty()
                println("Found ${pages.size} tasks matching text filters")
            }

            "Query DSL - sorting should work correctly" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        sortBy("Priority", SortDirection.DESCENDING)
                        sortBy("Task Name", SortDirection.ASCENDING)
                    }

                pages.shouldNotBeEmpty()
                println("Found ${pages.size} tasks sorted by priority desc, name asc")
            }

            "Query DSL - timestamp sorting should work correctly" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        sortByTimestamp("created_time", SortDirection.DESCENDING)
                    }

                pages.shouldNotBeEmpty()
                println("Found ${pages.size} tasks sorted by creation time")
            }

            "Query DSL - pagination should work correctly" {
                val allPages =
                    client.dataSources.query(dataSourceId) {
                        // No filters - get all pages
                    }

                val limitedPages =
                    client.dataSources.query(dataSourceId) {
                        pageSize(2)
                    }

                allPages.shouldNotBeEmpty()
                limitedPages.shouldNotBeEmpty()

                // Note: The regular query method handles pagination automatically,
                // so limitedPages might still contain all results
                println("All pages: ${allPages.size}, Limited query: ${limitedPages.size}")
            }

            "Query DSL - empty filter should return all results" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        // No filters - should return all pages
                    }

                pages.shouldNotBeEmpty()
                pages.size shouldBe 5 // We created 5 test pages
                println("Found ${pages.size} total tasks")
            }

            "Query DSL - filter for non-existent data should return empty" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            title("Task Name").contains("NonExistentTask")
                        }
                    }

                pages.shouldBeEmpty()
                println("Found ${pages.size} tasks matching non-existent filter (expected 0)")
            }

            "Query DSL - multiSelect filter should work correctly" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            multiSelect("Tags").isNotEmpty()
                        }
                    }

                pages.shouldNotBeEmpty()
                println("Found ${pages.size} tasks with tags")
            }

            "Query DSL - number comparison filters should work correctly" {
                val highPriorityPages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            number("Priority").greaterThanOrEqualTo(5)
                        }
                    }

                val lowPriorityPages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            number("Priority").lessThan(3)
                        }
                    }

                highPriorityPages.shouldNotBeEmpty()
                lowPriorityPages.shouldNotBeEmpty()
                println("High priority: ${highPriorityPages.size}, Low priority: ${lowPriorityPages.size}")
            }

            "Query DSL - combined filter, sort, and pagination should work" {
                val pages =
                    client.dataSources.query(dataSourceId) {
                        filter {
                            number("Priority").greaterThan(1)
                        }
                        sortBy("Priority", SortDirection.DESCENDING)
                        sortBy("Task Name", SortDirection.ASCENDING)
                        pageSize(3)
                    }

                pages.shouldNotBeEmpty()
                println("Found ${pages.size} filtered, sorted tasks")
            }
        }
    })
