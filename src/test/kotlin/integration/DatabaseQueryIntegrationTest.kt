package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryBuilder
import it.saabel.kotlinnotionclient.models.datasources.SortDirection
import it.saabel.kotlinnotionclient.models.pages.getNumberProperty
import it.saabel.kotlinnotionclient.models.pages.getTitleAsPlainText
import kotlinx.coroutines.delay

/**
 * Integration tests for database query functionality.
 *
 * These tests create real databases and pages, then query them to validate
 * that filtering, sorting, and pagination work correctly with live API data.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 */
@Tags("Integration", "RequiresApi", "Slow")
class DatabaseQueryIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping DatabaseQueryIntegrationTest due to missing environment variables") }
        } else {

            "Should query database and return created pages with real API" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    // Create test database
                    println("🗄️ Creating test database for query testing...")
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Query Test Database - Kotlin Client")
                            properties {
                                title("Name")
                                select("Priority")
                                checkbox("Completed")
                                number("Score")
                                select("Category")
                            }
                        }

                    println("✅ Database created: ${database.id}")
                    delay(1000)

                    // Get data source (2025-09-03 API)
                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id
                    println("✅ Retrieved data source: $dataSourceId")

                    // Create multiple test pages with different properties
                    println("📄 Creating test pages...")
                    val createdPages =
                        listOf(
                            client.pages.create {
                                parent.dataSource(dataSourceId)
                                properties {
                                    title("Name", "High Priority Task")
                                    checkbox("Completed", false)
                                    number("Score", 95.0)
                                }
                            },
                            client.pages.create {
                                parent.dataSource(dataSourceId)
                                properties {
                                    title("Name", "Completed Task")
                                    checkbox("Completed", true)
                                    number("Score", 75.0)
                                }
                            },
                            client.pages.create {
                                parent.dataSource(dataSourceId)
                                properties {
                                    title("Name", "Low Score Task")
                                    checkbox("Completed", false)
                                    number("Score", 45.0)
                                }
                            },
                        )

                    println("✅ Created ${createdPages.size} test pages")
                    delay(1000) // Wait for pages to be indexed

                    // Test 1: Query all pages (no filter)
                    println("🔍 Testing query all pages...")
                    val allPages = client.dataSources.query(dataSourceId)
                    allPages.size shouldBe 3
                    println("✅ Query all pages: ${allPages.size} results")

                    // Test 2: Query with checkbox filter
                    println("🔍 Testing checkbox filter...")
                    val completedQuery =
                        DataSourceQueryBuilder()
                            .filter {
                                checkbox("Completed").equals(true)
                            }.build()

                    val completedPages = client.dataSources.query(dataSourceId, completedQuery)
                    completedPages.size shouldBe 1
                    println("✅ Checkbox filter: ${completedPages.size} completed tasks")

                    // Test 3: Query with number filter
                    println("🔍 Testing number filter...")
                    val highScoreQuery =
                        DataSourceQueryBuilder()
                            .filter {
                                number("Score").greaterThan(80)
                            }.build()

                    val highScorePages = client.dataSources.query(dataSourceId, highScoreQuery)
                    highScorePages.size shouldBe 1
                    println("✅ Number filter: ${highScorePages.size} high score tasks")

                    // Test 4: Query with title filter
                    println("🔍 Testing title filter...")
                    val titleQuery =
                        DataSourceQueryBuilder()
                            .filter {
                                title("Name").contains("Priority")
                            }.build()

                    val priorityPages = client.dataSources.query(dataSourceId, titleQuery)
                    priorityPages.size shouldBe 1
                    println("✅ Title filter: ${priorityPages.size} priority tasks")

                    // Test 5: Query with AND filter
                    println("🔍 Testing AND filter...")
                    val andQuery =
                        DataSourceQueryBuilder()
                            .filter {
                                and(
                                    checkbox("Completed").equals(false),
                                    number("Score").greaterThan(50),
                                )
                            }.build()

                    val andResults = client.dataSources.query(dataSourceId, andQuery)
                    andResults.size shouldBe 1 // Only "High Priority Task" matches (Score=95, Completed=false)

                    // Verify the correct page was returned
                    val resultPage = andResults.first()
                    val title = resultPage.getTitleAsPlainText("Name")
                    title shouldBe "High Priority Task"
                    println("✅ AND filter: ${andResults.size} results - correct page: '$title'")

                    // Test 6: Query with sorting
                    println("🔍 Testing sorting...")
                    val sortQuery =
                        DataSourceQueryBuilder()
                            .sortBy("Score", SortDirection.DESCENDING)
                            .build()

                    val sortedResults = client.dataSources.query(dataSourceId, sortQuery)
                    sortedResults.shouldNotBeEmpty()
                    sortedResults.size shouldBe 3

                    // Verify results are actually sorted by Score in descending order
                    val scores =
                        sortedResults.map { page ->
                            page.getNumberProperty("Score") ?: 0.0
                        }
                    scores shouldBe listOf(95.0, 75.0, 45.0) // Expected descending order
                    println("✅ Sorting: ${sortedResults.size} results correctly sorted by Score (${scores.joinToString(" → ")})")

                    // Test 7: Query with pagination
                    println("🔍 Testing pagination...")
                    val pageQuery =
                        DataSourceQueryBuilder()
                            .pageSize(2)
                            .build()

                    val pagedResults = client.dataSources.query(dataSourceId, pageQuery)
                    // The API now automatically fetches all pages
                    pagedResults.size shouldBe 3 // All pages fetched automatically
                    println("✅ Pagination: API automatically fetched all ${pagedResults.size} results")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up test data...")
                        createdPages.forEach { page ->
                            client.pages.trash(page.id)
                            delay(100)
                        }
                        client.databases.trash(database.id)
                        println("✅ Cleanup completed")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created database: ${database.id}")
                    }

                    println("🎉 Database query integration test completed successfully!")
                } finally {
                    client.close()
                }
            }

            "Should handle empty query results gracefully" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    // Create empty database (no pages)
                    println("🗄️ Creating empty database for query testing...")
                    val emptyDb =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Empty Query Test DB")
                            properties {
                                title("Name")
                            }
                        }

                    delay(1000)

                    // Get data source (2025-09-03 API)
                    val retrievedDb = client.databases.retrieve(emptyDb.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    // Query empty database
                    val results = client.dataSources.query(dataSourceId)
                    results.size shouldBe 0

                    // Query with filter that matches nothing
                    val noMatchQuery =
                        DataSourceQueryBuilder()
                            .filter {
                                title("Name").contains("NonexistentText12345")
                            }.build()

                    val noResults = client.dataSources.query(dataSourceId, noMatchQuery)
                    noResults.size shouldBe 0

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        client.databases.trash(emptyDb.id)
                        println("✅ Empty query test completed")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
