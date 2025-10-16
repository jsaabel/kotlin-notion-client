package integration.pagination

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryBuilder
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import kotlinx.coroutines.delay

/**
 * Integration tests for database pagination functionality.
 *
 * These tests verify that the client correctly handles paginated responses
 * from the Notion API when querying databases with large numbers of pages.
 *
 * Tests cover:
 * - Automatic pagination for database queries with >100 pages
 * - Custom page sizes with pagination
 * - Complex queries with filters and sorting
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 */
@Tags("Integration", "RequiresApi", "Slow")
class DatabasePaginationIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping DatabasePaginationIntegrationTest due to missing environment variables") }
        } else {

            "Should automatically paginate database queries with >100 pages" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    // Check parent page status first
                    println("🔍 Checking parent page status...")
                    val parentPage = client.pages.retrieve(parentPageId)
                    println("   Parent page archived: ${parentPage.archived}")

                    if (parentPage.archived) {
                        println("⚠️  Parent page is archived - tests may fail")
                        println("   You may need to unarchive the parent page in Notion")
                    }

                    // Create test database
                    println("🗄️ Creating test database for pagination testing...")
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("DB Pagination Test - ${System.currentTimeMillis()}")
                            icon.emoji("📊")
                            properties {
                                title("Name")
                                number("Index")
                                select("Category")
                            }
                        }

                    println("✅ Database created: ${database.id}")
                    delay(1000)

                    // Get data source from database (2025-09-03 API)
                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id
                    println("✅ Retrieved data source: $dataSourceId")

                    // Create 105 pages to trigger pagination (default page size is 100)
                    val totalPages = 105
                    println("📄 Creating $totalPages test pages...")
                    println("   This will trigger automatic pagination as the default page size is 100")

                    val batchSize = 10
                    for (batch in 0 until totalPages step batchSize) {
                        val pagesInBatch = minOf(batchSize, totalPages - batch)

                        // Create pages in batch
                        for (index in batch until batch + pagesInBatch) {
                            client.pages.create {
                                parent.dataSource(dataSourceId)
                                properties {
                                    title("Name", "Test Page ${index + 1}")
                                    number("Index", (index + 1).toDouble())
                                }
                            }
                        }

                        println(
                            "   Created batch ${batch / batchSize + 1}/${(totalPages + batchSize - 1) / batchSize} ($pagesInBatch pages)",
                        )
                        delay(200) // Small delay between batches to avoid rate limits
                    }

                    println("✅ Created $totalPages test pages")
                    delay(2000) // Wait for all pages to be indexed

                    // Test 1: Query all pages (automatic pagination)
                    println("\n🔍 Testing automatic pagination for data source query...")
                    val startTime = System.currentTimeMillis()
                    val allPages = client.dataSources.query(dataSourceId)
                    val queryTime = System.currentTimeMillis() - startTime

                    allPages.size shouldBe totalPages
                    println("✅ Automatic pagination successful!")
                    println("   - Retrieved ${allPages.size} pages automatically")
                    println("   - Query time: ${queryTime}ms")
                    println("   - The API fetched multiple pages transparently")

                    // Test 2: Query with explicit small page size to verify multiple fetches
                    println("\n🔍 Testing pagination with small page size...")
                    val smallPageSizeQuery =
                        DataSourceQueryBuilder()
                            .pageSize(10) // Small page size to trigger more API calls
                            .build()

                    val smallPageResults = client.dataSources.query(dataSourceId, smallPageSizeQuery)
                    smallPageResults.size shouldBe totalPages
                    println("✅ Small page size pagination successful!")
                    println("   - Requested page size: 10")
                    println("   - Still retrieved all ${smallPageResults.size} pages")
                    println("   - The API made ~${(totalPages + 9) / 10} requests automatically")

                    // Test 3: Query with filter and sorting to ensure pagination works with complex queries
                    println("\n🔍 Testing pagination with filters and sorting...")
                    val complexQuery =
                        DataSourceQueryBuilder()
                            .filter {
                                number("Index").greaterThan(50)
                            }.sortBy("Index")
                            .pageSize(20)
                            .build()

                    val filteredResults = client.dataSources.query(dataSourceId, complexQuery)
                    filteredResults.size shouldBe 55 // Pages 51-105
                    println("✅ Complex query pagination successful!")
                    println("   - Filter: Index > 50")
                    println("   - Retrieved ${filteredResults.size} matching pages")
                    println("   - Results properly sorted by Index")

                    // Verify the results are correctly sorted
                    val indices =
                        filteredResults.map { page ->
                            page.properties["Index"]?.let { prop ->
                                when (prop) {
                                    is PageProperty.Number -> prop.number?.toInt() ?: 0
                                    else -> 0
                                }
                            } ?: 0
                        }
                    indices shouldBe (51..105).toList()

                    // Cleanup - just delete the database, which will clean up all pages
                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test database...")
                        client.databases.archive(database.id)
                        println("✅ Database archived (all pages cleaned up automatically)")
                    } else {
                        println("\n🔧 Test database preserved: ${database.id}")
                        println("   Contains $totalPages test pages")
                    }

                    println("\n🎉 Database pagination test completed successfully!")
                } finally {
                    client.close()
                }
            }
        }
    })
