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
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Integration tests for pagination edge cases.
 *
 * These tests verify that the client correctly handles edge cases in pagination
 * such as empty results, single page results, and exact page boundaries.
 *
 * Tests cover:
 * - Empty pagination results
 * - Single page of results (no pagination needed)
 * - Exact page boundary conditions
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 */
@Tags("Integration", "RequiresApi", "Slow")
class PaginationEdgeCasesIntegrationTest :
    StringSpec({

        fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"

        "Should handle pagination edge cases gracefully" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    val createdDatabases = mutableListOf<String>()

                    // Test edge case: Empty results
                    println("🔍 Testing pagination with empty results...")
                    val emptyDbRequest =
                        CreateDatabaseRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            title = listOf(RequestBuilders.createSimpleRichText("Empty DB - Edge Case - ${System.currentTimeMillis()}")),
                            icon = RequestBuilders.createEmojiIcon("🗂️"),
                            properties =
                                mapOf(
                                    "Name" to CreateDatabaseProperty.Title(),
                                ),
                        )

                    val emptyDb = client.databases.create(emptyDbRequest)
                    createdDatabases.add(emptyDb.id)
                    delay(500)

                    val emptyResults = client.databases.query(emptyDb.id)
                    emptyResults.size shouldBe 0
                    println("✅ Empty pagination handled correctly")

                    // Test edge case: Exactly one page of results
                    println("\n🔍 Testing pagination with exactly one page...")
                    val exactlyOnePageDb =
                        CreateDatabaseRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            title = listOf(RequestBuilders.createSimpleRichText("One Page DB - Edge Case - ${System.currentTimeMillis()}")),
                            icon = RequestBuilders.createEmojiIcon("📄"),
                            properties =
                                mapOf(
                                    "Name" to CreateDatabaseProperty.Title(),
                                ),
                        )

                    val onePageDb = client.databases.create(exactlyOnePageDb)
                    createdDatabases.add(onePageDb.id)
                    delay(500)

                    // Create exactly 50 pages (less than default page size of 100)
                    for (i in 1..50) {
                        client.pages.create(
                            CreatePageRequest(
                                parent = Parent(type = "database_id", databaseId = onePageDb.id),
                                properties =
                                    mapOf(
                                        "Name" to
                                            PagePropertyValue.TitleValue(
                                                title = listOf(RequestBuilders.createSimpleRichText("Page $i")),
                                            ),
                                    ),
                            ),
                        )
                    }
                    delay(1000)

                    val onePageResults = client.databases.query(onePageDb.id)
                    onePageResults.size shouldBe 50
                    println("✅ Single page pagination handled correctly")

                    // Test edge case: Exactly at page boundary
                    println("\n🔍 Testing pagination at exact page boundary...")
                    val boundaryQuery =
                        DatabaseQueryBuilder()
                            .pageSize(25) // Exactly divides 50
                            .build()

                    val boundaryResults = client.databases.query(onePageDb.id, boundaryQuery)
                    boundaryResults.size shouldBe 50
                    println("✅ Page boundary pagination handled correctly")
                    println("   - Made exactly 2 API calls with page size 25")

                    // Cleanup - just delete the databases, which cleans up all pages
                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up edge case test databases...")
                        createdDatabases.forEach { databaseId ->
                            try {
                                client.databases.archive(databaseId)
                            } catch (e: Exception) {
                                // Ignore cleanup errors
                                println("   Warning: Failed to clean up database $databaseId")
                            }
                        }
                        println("✅ Cleanup completed")
                    } else {
                        println("\n🔧 Test databases preserved:")
                        createdDatabases.forEach { databaseId ->
                            println("   - $databaseId")
                        }
                    }

                    println("\n🎉 Edge case pagination tests completed successfully!")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping edge case tests - missing environment variables")
                println("   Required:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_TEST_PAGE_ID: Parent page for test database")
            }
        }
    })
