package integration.pagination

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseProperty
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseRequest
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryBuilder
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PageProperty
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

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

        fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"

        "Should automatically paginate database queries with >100 pages" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

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
                    val databaseRequest =
                        CreateDatabaseRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            title =
                                listOf(
                                    RequestBuilders.createSimpleRichText("DB Pagination Test - ${System.currentTimeMillis()}"),
                                ),
                            icon = RequestBuilders.createEmojiIcon("📊"),
                            properties =
                                mapOf(
                                    "Name" to CreateDatabaseProperty.Title(),
                                    "Index" to CreateDatabaseProperty.Number(),
                                    "Category" to CreateDatabaseProperty.Select(),
                                ),
                        )

                    val database = client.databases.create(databaseRequest)
                    println("✅ Database created: ${database.id}")
                    delay(500)

                    // Create 105 pages to trigger pagination (default page size is 100)
                    val totalPages = 105
                    println("📄 Creating $totalPages test pages...")
                    println("   This will trigger automatic pagination as the default page size is 100")

                    val batchSize = 10
                    for (batch in 0 until totalPages step batchSize) {
                        val pagesInBatch = minOf(batchSize, totalPages - batch)
                        val batchPages =
                            (batch until batch + pagesInBatch).map { index ->
                                CreatePageRequest(
                                    parent = Parent(type = "database_id", databaseId = database.id),
                                    properties =
                                        mapOf(
                                            "Name" to
                                                PagePropertyValue.TitleValue(
                                                    title = listOf(RequestBuilders.createSimpleRichText("Test Page ${index + 1}")),
                                                ),
                                            "Index" to PagePropertyValue.NumberValue(number = (index + 1).toDouble()),
                                        ),
                                )
                            }

                        // Create pages in batch
                        batchPages.forEach { request ->
                            client.pages.create(request)
                        }

                        println(
                            "   Created batch ${batch / batchSize + 1}/${(totalPages + batchSize - 1) / batchSize} ($pagesInBatch pages)",
                        )
                        delay(200) // Small delay between batches to avoid rate limits
                    }

                    println("✅ Created $totalPages test pages")
                    delay(2000) // Wait for all pages to be indexed

                    // Test 1: Query all pages (automatic pagination)
                    println("\n🔍 Testing automatic pagination for database query...")
                    val startTime = System.currentTimeMillis()
                    val allPages = client.databases.query(database.id)
                    val queryTime = System.currentTimeMillis() - startTime

                    allPages.size shouldBe totalPages
                    println("✅ Automatic pagination successful!")
                    println("   - Retrieved ${allPages.size} pages automatically")
                    println("   - Query time: ${queryTime}ms")
                    println("   - The API fetched multiple pages transparently")

                    // Test 2: Query with explicit small page size to verify multiple fetches
                    println("\n🔍 Testing pagination with small page size...")
                    val smallPageSizeQuery =
                        DatabaseQueryBuilder()
                            .pageSize(10) // Small page size to trigger more API calls
                            .build()

                    val smallPageResults = client.databases.query(database.id, smallPageSizeQuery)
                    smallPageResults.size shouldBe totalPages
                    println("✅ Small page size pagination successful!")
                    println("   - Requested page size: 10")
                    println("   - Still retrieved all ${smallPageResults.size} pages")
                    println("   - The API made ~${(totalPages + 9) / 10} requests automatically")

                    // Test 3: Query with filter and sorting to ensure pagination works with complex queries
                    println("\n🔍 Testing pagination with filters and sorting...")
                    val complexQuery =
                        DatabaseQueryBuilder()
                            .filter {
                                number("Index").greaterThan(50)
                            }.sortBy("Index")
                            .pageSize(20)
                            .build()

                    val filteredResults = client.databases.query(database.id, complexQuery)
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
            } else {
                println("⏭️ Skipping pagination test - missing environment variables")
                println("   Required:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_TEST_PAGE_ID: Parent page for test database")
            }
        }
    })
