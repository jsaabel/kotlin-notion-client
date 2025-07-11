package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseProperty
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseRequest
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryBuilder
import no.saabelit.kotlinnotionclient.models.databases.SortDirection
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.pages.getNumberProperty
import no.saabelit.kotlinnotionclient.models.pages.getTitleAsPlainText
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Integration tests for database query functionality.
 *
 * These tests create real databases and pages, then query them to validate
 * that filtering, sorting, and pagination work correctly with live API data.
 *
 * Run with: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi", "Slow")
class DatabaseQueryIntegrationTest :
    StringSpec({

        "Should query database and return created pages with real API" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_PARENT_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(token = token))

                try {
                    // Create test database
                    println("🗄️ Creating test database for query testing...")
                    val databaseRequest =
                        CreateDatabaseRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            title = listOf(RequestBuilders.createSimpleRichText("Query Test Database - Kotlin Client")),
                            properties =
                                mapOf(
                                    "Name" to CreateDatabaseProperty.Title(),
                                    "Priority" to CreateDatabaseProperty.Select(),
                                    "Completed" to CreateDatabaseProperty.Checkbox(),
                                    "Score" to CreateDatabaseProperty.Number(),
                                    "Category" to CreateDatabaseProperty.Select(),
                                ),
                        )

                    val database = client.databases.create(databaseRequest)
                    println("✅ Database created: ${database.id}")
                    delay(500)

                    // Create multiple test pages with different properties
                    println("📄 Creating test pages...")
                    val pages =
                        listOf(
                            CreatePageRequest(
                                parent = Parent(type = "database_id", databaseId = database.id),
                                properties =
                                    mapOf(
                                        "Name" to
                                            PagePropertyValue.TitleValue(
                                                title = listOf(RequestBuilders.createSimpleRichText("High Priority Task")),
                                            ),
                                        "Completed" to PagePropertyValue.CheckboxValue(checkbox = false),
                                        "Score" to PagePropertyValue.NumberValue(number = 95.0),
                                    ),
                            ),
                            CreatePageRequest(
                                parent = Parent(type = "database_id", databaseId = database.id),
                                properties =
                                    mapOf(
                                        "Name" to
                                            PagePropertyValue.TitleValue(
                                                title = listOf(RequestBuilders.createSimpleRichText("Completed Task")),
                                            ),
                                        "Completed" to PagePropertyValue.CheckboxValue(checkbox = true),
                                        "Score" to PagePropertyValue.NumberValue(number = 75.0),
                                    ),
                            ),
                            CreatePageRequest(
                                parent = Parent(type = "database_id", databaseId = database.id),
                                properties =
                                    mapOf(
                                        "Name" to
                                            PagePropertyValue.TitleValue(
                                                title = listOf(RequestBuilders.createSimpleRichText("Low Score Task")),
                                            ),
                                        "Completed" to PagePropertyValue.CheckboxValue(checkbox = false),
                                        "Score" to PagePropertyValue.NumberValue(number = 45.0),
                                    ),
                            ),
                        )

                    val createdPages = pages.map { client.pages.create(it) }
                    println("✅ Created ${createdPages.size} test pages")
                    delay(1000) // Wait for pages to be indexed

                    // Test 1: Query all pages (no filter)
                    println("🔍 Testing query all pages...")
                    val allPages = client.databases.query(database.id)
                    allPages.results.size shouldBe 3
                    allPages.objectType shouldBe "list"
                    allPages.type shouldBe "page_or_database"
                    println("✅ Query all pages: ${allPages.results.size} results")

                    // Test 2: Query with checkbox filter
                    println("🔍 Testing checkbox filter...")
                    val completedQuery =
                        DatabaseQueryBuilder()
                            .filter {
                                checkbox("Completed").equals(true)
                            }.build()

                    val completedPages = client.databases.query(database.id, completedQuery)
                    completedPages.results.size shouldBe 1
                    println("✅ Checkbox filter: ${completedPages.results.size} completed tasks")

                    // Test 3: Query with number filter
                    println("🔍 Testing number filter...")
                    val highScoreQuery =
                        DatabaseQueryBuilder()
                            .filter {
                                number("Score").greaterThan(80)
                            }.build()

                    val highScorePages = client.databases.query(database.id, highScoreQuery)
                    highScorePages.results.size shouldBe 1
                    println("✅ Number filter: ${highScorePages.results.size} high score tasks")

                    // Test 4: Query with title filter
                    println("🔍 Testing title filter...")
                    val titleQuery =
                        DatabaseQueryBuilder()
                            .filter {
                                title("Name").contains("Priority")
                            }.build()

                    val priorityPages = client.databases.query(database.id, titleQuery)
                    priorityPages.results.size shouldBe 1
                    println("✅ Title filter: ${priorityPages.results.size} priority tasks")

                    // Test 5: Query with AND filter
                    println("🔍 Testing AND filter...")
                    val andQuery =
                        DatabaseQueryBuilder()
                            .filter {
                                and(
                                    checkbox("Completed").equals(false),
                                    number("Score").greaterThan(50),
                                )
                            }.build()

                    val andResults = client.databases.query(database.id, andQuery)
                    andResults.results.size shouldBe 1 // Only "High Priority Task" matches (Score=95, Completed=false)

                    // Verify the correct page was returned
                    val resultPage = andResults.results.first()
                    val title = resultPage.getTitleAsPlainText("Name")
                    title shouldBe "High Priority Task"
                    println("✅ AND filter: ${andResults.results.size} results - correct page: '$title'")

                    // Test 6: Query with sorting
                    println("🔍 Testing sorting...")
                    val sortQuery =
                        DatabaseQueryBuilder()
                            .sortBy("Score", SortDirection.DESCENDING)
                            .build()

                    val sortedResults = client.databases.query(database.id, sortQuery)
                    sortedResults.results.shouldNotBeEmpty()
                    sortedResults.results.size shouldBe 3

                    // Verify results are actually sorted by Score in descending order
                    val scores =
                        sortedResults.results.map { page ->
                            page.getNumberProperty("Score") ?: 0.0
                        }
                    scores shouldBe listOf(95.0, 75.0, 45.0) // Expected descending order
                    println("✅ Sorting: ${sortedResults.results.size} results correctly sorted by Score (${scores.joinToString(" → ")})")

                    // Test 7: Query with pagination
                    println("🔍 Testing pagination...")
                    val pageQuery =
                        DatabaseQueryBuilder()
                            .pageSize(2)
                            .build()

                    val pagedResults = client.databases.query(database.id, pageQuery)
                    pagedResults.results.size shouldBe 2

                    // With 3 total pages and page size 2, we should have more pages available
                    pagedResults.hasMore shouldBe true
                    pagedResults.nextCursor shouldNotBe null
                    println("✅ Pagination: ${pagedResults.results.size} results per page, hasMore=${pagedResults.hasMore}")

                    // Cleanup
                    println("🧹 Cleaning up test data...")
                    createdPages.forEach { page ->
                        client.pages.archive(page.id)
                        delay(100)
                    }
                    client.databases.archive(database.id)
                    println("✅ Cleanup completed")

                    println("🎉 Database query integration test completed successfully!")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping database query integration test")
                println("   Required environment variables:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_PARENT_PAGE_ID: Page where test database will be created")
            }
        }

        "Should handle empty query results gracefully" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_PARENT_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(token = token))

                try {
                    // Create empty database (no pages)
                    println("🗄️ Creating empty database for query testing...")
                    val emptyDbRequest =
                        CreateDatabaseRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            title = listOf(RequestBuilders.createSimpleRichText("Empty Query Test DB")),
                            properties =
                                mapOf(
                                    "Name" to CreateDatabaseProperty.Title(),
                                ),
                        )

                    val emptyDb = client.databases.create(emptyDbRequest)
                    delay(500)

                    // Query empty database
                    val results = client.databases.query(emptyDb.id)
                    results.objectType shouldBe "list"
                    results.results.size shouldBe 0
                    results.hasMore shouldBe false

                    // Query with filter that matches nothing
                    val noMatchQuery =
                        DatabaseQueryBuilder()
                            .filter {
                                title("Name").contains("NonexistentText12345")
                            }.build()

                    val noResults = client.databases.query(emptyDb.id, noMatchQuery)
                    noResults.results.size shouldBe 0

                    // Cleanup
                    client.databases.archive(emptyDb.id)
                    println("✅ Empty query test completed")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping empty query test - missing environment variables")
            }
        }
    })
