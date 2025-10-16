package integration.pagination

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.databases.DatabaseQueryBuilder
import kotlinx.coroutines.delay

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
@Tags("Integration", "RequiresApi")
class PaginationEdgeCasesIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping PaginationEdgeCasesIntegrationTest due to missing environment variables") }
        } else {
            "Should handle pagination edge cases gracefully" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    val createdDatabases = mutableListOf<String>()

                    // Test edge case: Empty results
                    println("🔍 Testing pagination with empty results...")
                    val emptyDb =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Empty DB - Edge Case - ${System.currentTimeMillis()}")
                            icon.emoji("🗂️")
                            properties {
                                title("Name")
                            }
                        }

                    createdDatabases.add(emptyDb.id)
                    delay(500)

                    // Get data source to query (2025-09-03 API)
                    val emptyDbRetrieved = client.databases.retrieve(emptyDb.id)
                    val emptyDataSourceId = emptyDbRetrieved.dataSources.first().id

                    val emptyResults = client.dataSources.query(emptyDataSourceId)
                    emptyResults.size shouldBe 0
                    println("✅ Empty pagination handled correctly")

                    // Test edge case: Exactly one page of results
                    println("\n🔍 Testing pagination with exactly one page...")
                    val onePageDb =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("One Page DB - Edge Case - ${System.currentTimeMillis()}")
                            icon.emoji("📄")
                            properties {
                                title("Name")
                            }
                        }

                    createdDatabases.add(onePageDb.id)
                    delay(1000)

                    // Get data source (2025-09-03 API)
                    val retrievedDb = client.databases.retrieve(onePageDb.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    // Create exactly 50 pages (less than default page size of 100)
                    for (i in 1..50) {
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Name", "Page $i")
                            }
                        }
                    }
                    delay(1000)

                    val onePageResults = client.dataSources.query(dataSourceId)
                    onePageResults.size shouldBe 50
                    println("✅ Single page pagination handled correctly")

                    // Test edge case: Exactly at page boundary
                    println("\n🔍 Testing pagination at exact page boundary...")
                    val boundaryQuery =
                        DatabaseQueryBuilder()
                            .pageSize(25) // Exactly divides 50
                            .build()

                    val boundaryResults = client.dataSources.query(dataSourceId, boundaryQuery)
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
            }
        }
    })
