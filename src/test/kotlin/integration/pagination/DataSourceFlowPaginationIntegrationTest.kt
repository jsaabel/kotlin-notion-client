package integration.pagination

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.datasources.SortDirection
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList

/**
 * Integration tests for DataSourcesApi Flow-based pagination helpers.
 *
 * These tests validate the new pagination helper methods:
 * - queryAsFlow() - Item-level Flow pagination
 * - queryPagedFlow() - Page-level Flow pagination
 *
 * The tests verify that these helpers correctly handle paginated responses
 * from the Notion API using reactive Flow patterns.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 */
@Tags("Integration", "RequiresApi", "Slow")
class DataSourceFlowPaginationIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping DataSourceFlowPaginationIntegrationTest due to missing environment variables") }
        } else {

            "queryAsFlow should emit all items across multiple pages" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Setting up test database for Flow pagination...")

                    // Create test database
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Flow Pagination Test - ${System.currentTimeMillis()}")
                            icon.emoji("🌊")
                            properties {
                                title("Name")
                                number("Index")
                            }
                        }

                    println("✅ Database created: ${database.id}")
                    delay(1000)

                    // Get data source
                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id
                    println("✅ Retrieved data source: $dataSourceId")

                    // Create 25 pages to test pagination with smaller page size
                    val totalPages = 25
                    println("📄 Creating $totalPages test pages...")

                    for (index in 0 until totalPages) {
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Name", "Flow Test Page ${index + 1}")
                                number("Index", (index + 1).toDouble())
                            }
                        }

                        if ((index + 1) % 10 == 0) {
                            println("   Created ${index + 1}/$totalPages pages...")
                            delay(200) // Small delay to avoid rate limits
                        }
                    }

                    println("✅ Created $totalPages test pages")
                    delay(2000) // Wait for indexing

                    // Test 1: queryAsFlow with DSL builder
                    println("\n🌊 Testing queryAsFlow with DSL builder...")
                    val flowPages =
                        client.dataSources
                            .queryAsFlow(dataSourceId) {
                                // No filter - get all pages
                            }.toList()

                    flowPages.size shouldBe totalPages
                    println("✅ queryAsFlow (DSL) successful!")
                    println("   - Emitted ${flowPages.size} individual pages")
                    println("   - Flow collected all items reactively")

                    // Test 2: queryAsFlow with filter
                    println("\n🌊 Testing queryAsFlow with filter...")
                    val filteredFlowPages =
                        client.dataSources
                            .queryAsFlow(dataSourceId) {
                                filter {
                                    number("Index").greaterThan(15)
                                }
                            }.toList()

                    filteredFlowPages.size shouldBe 10 // Pages 16-25
                    println("✅ queryAsFlow with filter successful!")
                    println("   - Filter: Index > 15")
                    println("   - Emitted ${filteredFlowPages.size} filtered pages")

                    // Verify the filtered results (order not guaranteed without explicit sorting)
                    val filteredIndices =
                        filteredFlowPages.map { page ->
                            page.properties["Index"]?.let { prop ->
                                when (prop) {
                                    is PageProperty.Number -> prop.number?.toInt() ?: 0
                                    else -> 0
                                }
                            } ?: 0
                        }
                    filteredIndices shouldContainExactlyInAnyOrder (16..25).toList()

                    // Test 3: queryPagedFlow to access pagination metadata
                    println("\n🌊 Testing queryPagedFlow for metadata access...")
                    val pagedResponses =
                        client.dataSources
                            .queryPagedFlow(dataSourceId) {
                                // Get all pages
                            }.toList()

                    val totalFromPages = pagedResponses.sumOf { it.results.size }
                    totalFromPages shouldBe totalPages
                    println("✅ queryPagedFlow successful!")
                    println("   - Received ${pagedResponses.size} page response(s)")
                    println("   - Total items across all pages: $totalFromPages")

                    // Verify pagination metadata
                    if (pagedResponses.size > 1) {
                        println("   - Multiple pages detected:")
                        pagedResponses.forEachIndexed { index, response ->
                            println(
                                "     Page ${index + 1}: ${response.results.size} items, " +
                                    "hasMore=${response.hasMore}, cursor=${response.nextCursor?.take(10)}...",
                            )
                        }
                        // Last page should have hasMore = false
                        pagedResponses.last().hasMore shouldBe false
                    } else {
                        println("   - Single page response (all results fit in one page)")
                        pagedResponses.first().hasMore shouldBe false
                    }

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test database...")
                        client.databases.archive(database.id)
                        println("✅ Database archived")
                    } else {
                        println("\n🔧 Test database preserved: ${database.id}")
                    }

                    println("\n🎉 Flow-based pagination test completed successfully!")
                } finally {
                    client.close()
                }
            }

            "queryAsFlow should handle empty results" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Testing queryAsFlow with empty results...")

                    // Create empty database
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Empty Flow Test - ${System.currentTimeMillis()}")
                            icon.emoji("🌊")
                            properties {
                                title("Name")
                            }
                        }

                    println("✅ Empty database created: ${database.id}")
                    delay(1000)

                    // Get data source
                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    // Query empty data source
                    val emptyFlowPages =
                        client.dataSources
                            .queryAsFlow(dataSourceId) {
                                // No filter
                            }.toList()

                    emptyFlowPages.size shouldBe 0
                    println("✅ Empty results handled correctly!")
                    println("   - Flow emitted 0 items as expected")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        client.databases.archive(database.id)
                        println("🧹 Cleaned up empty database")
                    }

                    println("🎉 Empty results test completed!")
                } finally {
                    client.close()
                }
            }

            "queryPagedFlow should work with sorting and pagination" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Testing queryPagedFlow with sorting...")

                    // Create test database
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Sorted Flow Test - ${System.currentTimeMillis()}")
                            icon.emoji("🔢")
                            properties {
                                title("Name")
                                number("Priority")
                            }
                        }

                    println("✅ Database created: ${database.id}")
                    delay(1000)

                    // Get data source
                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    // Create pages with different priorities
                    val priorities = listOf(5.0, 1.0, 3.0, 4.0, 2.0)
                    println("📄 Creating ${priorities.size} pages with priorities: $priorities")

                    priorities.forEachIndexed { index, priority ->
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Name", "Item ${index + 1}")
                                number("Priority", priority)
                            }
                        }
                    }

                    println("✅ Created ${priorities.size} test pages")
                    delay(2000)

                    // Query with sorting using Flow
                    println("\n🌊 Testing queryAsFlow with descending sort...")
                    val sortedPages =
                        client.dataSources
                            .queryAsFlow(dataSourceId) {
                                sortBy("Priority", SortDirection.DESCENDING)
                            }.toList()

                    sortedPages.size shouldBe priorities.size

                    // Verify sort order (should be 5, 4, 3, 2, 1)
                    val sortedPriorities =
                        sortedPages.map { page ->
                            page.properties["Priority"]?.let { prop ->
                                when (prop) {
                                    is PageProperty.Number -> prop.number ?: 0.0
                                    else -> 0.0
                                }
                            } ?: 0.0
                        }

                    sortedPriorities shouldContainExactly listOf(5.0, 4.0, 3.0, 2.0, 1.0)
                    println("✅ Sorting with Flow successful!")
                    println("   - Original priorities: $priorities")
                    println("   - Sorted priorities: $sortedPriorities")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        client.databases.archive(database.id)
                        println("🧹 Cleaned up sorted database")
                    }

                    println("🎉 Sorting test completed!")
                } finally {
                    client.close()
                }
            }
        }
    })
