package integration.pagination

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.pages.PageReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList

/**
 * Integration tests for PagesApi Flow-based pagination helpers.
 *
 * These tests validate the new pagination helper methods:
 * - retrievePropertyItemsAsFlow() - Item-level Flow pagination
 * - retrievePropertyItemsPagedFlow() - Page-level Flow pagination
 *
 * The tests verify that these helpers correctly handle paginated responses
 * from the Notion API when retrieving property items (e.g., large relation lists).
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 */
@Tags("Integration", "RequiresApi", "Slow")
class PagesFlowPaginationIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping PagesFlowPaginationIntegrationTest due to missing environment variables") }
        } else {

            "retrievePropertyItemsAsFlow should emit all relation items" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient.create(NotionConfig(apiToken = token))
                val createdDatabases = mutableListOf<String>()

                try {
                    println("🔍 Setting up test databases for Flow pagination...")

                    // Create database
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Pages Flow Test - ${System.currentTimeMillis()}")
                            icon.emoji("🌊")
                            properties {
                                title("Name")
                            }
                        }

                    createdDatabases.add(database.id)
                    println("✅ Database created: ${database.id}")
                    delay(1000)

                    // Get the target data source
                    val dbRetrieved = client.databases.retrieve(database.id)
                    val targetDataSourceId = dbRetrieved.dataSources.first().id
                    println("✅ Retrieved target data source: $targetDataSourceId")

                    // Create a second data source with relation property
                    println("\n🗄️ Creating data source with relation property...")
                    val sourceDataSource =
                        client.dataSources.create {
                            databaseId(database.id)
                            title("Source Data Source")
                            properties {
                                title("Task Name")
                                relation("Related Items", database.id, targetDataSourceId)
                            }
                        }

                    val sourceDataSourceId = sourceDataSource.id
                    println("✅ Data source created: $sourceDataSourceId")
                    delay(1000)

                    // Create 25 target pages (to test pagination beyond typical limit of 20)
                    println("\n📄 Creating 25 target pages...")
                    val targetPageIds = mutableListOf<String>()

                    for (i in 1..25) {
                        val targetPage =
                            client.pages.create {
                                parent.dataSource(targetDataSourceId)
                                properties {
                                    title("Name", "Target Item $i")
                                }
                            }

                        targetPageIds.add(targetPage.id)

                        if (i % 10 == 0) {
                            println("   Created $i/25 pages...")
                            delay(100)
                        }
                    }

                    println("✅ Created ${targetPageIds.size} target pages")
                    delay(1000)

                    // Create a page with relations to all target pages
                    println("\n📄 Creating page with ${targetPageIds.size} relations...")
                    val relationReferences = targetPageIds.map { PageReference(id = it) }

                    val sourcePage =
                        client.pages.create {
                            parent.dataSource(sourceDataSourceId)
                            properties {
                                title("Task Name", "Page with Many Relations")
                                relation("Related Items", relationReferences)
                            }
                        }

                    println("✅ Page created: ${sourcePage.id}")
                    delay(2000) // Wait for relations to be processed

                    // Get the relation property ID
                    println("\n🌊 Testing retrievePropertyItemsAsFlow...")
                    val retrievedPage = client.pages.retrieve(sourcePage.id)
                    val relationProperty = retrievedPage.properties["Related Items"]
                    val relationPropertyId =
                        relationProperty?.id ?: throw IllegalStateException("Relation property not found")

                    // Test 1: retrievePropertyItemsAsFlow
                    val flowItems =
                        client.pages
                            .retrievePropertyItemsAsFlow(sourcePage.id, relationPropertyId)
                            .toList()

                    flowItems.size shouldBe targetPageIds.size
                    flowItems.size shouldBeGreaterThan 20 // Should exceed typical page size
                    println("✅ retrievePropertyItemsAsFlow successful!")
                    println("   - Emitted ${flowItems.size} property items")
                    println("   - Flow collected all items reactively")

                    // Verify the relation IDs match
                    val flowRelationIds = flowItems.mapNotNull { it.relation?.id }
                    flowRelationIds.size shouldBe targetPageIds.size
                    flowRelationIds.toSet() shouldContainExactlyInAnyOrder targetPageIds.toSet()
                    println("   - All relation IDs correctly retrieved")

                    // Test 2: retrievePropertyItemsPagedFlow
                    println("\n🌊 Testing retrievePropertyItemsPagedFlow...")
                    val pagedResponses =
                        client.pages
                            .retrievePropertyItemsPagedFlow(sourcePage.id, relationPropertyId)
                            .toList()

                    val totalFromPages = pagedResponses.sumOf { it.results.size }
                    totalFromPages shouldBe targetPageIds.size
                    println("✅ retrievePropertyItemsPagedFlow successful!")
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
                        println("   - Single page response")
                        pagedResponses.first().hasMore shouldBe false
                    }

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test database...")
                        createdDatabases.forEach { databaseId ->
                            try {
                                client.databases.archive(databaseId)
                            } catch (e: Exception) {
                                println("   Warning: Failed to clean up database $databaseId")
                            }
                        }
                        println("✅ Database archived")
                    } else {
                        println("\n🔧 Test database preserved: ${database.id}")
                    }

                    println("\n🎉 Pages Flow pagination test completed successfully!")
                } finally {
                    client.close()
                }
            }

            "retrievePropertyItemsAsFlow should handle single page results" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient.create(NotionConfig(apiToken = token))
                val createdDatabases = mutableListOf<String>()

                try {
                    println("🔍 Testing Flow with small result set...")

                    // Create database
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Small Flow Test - ${System.currentTimeMillis()}")
                            icon.emoji("🌊")
                            properties {
                                title("Name")
                            }
                        }

                    createdDatabases.add(database.id)
                    delay(1000)

                    val dbRetrieved = client.databases.retrieve(database.id)
                    val targetDataSourceId = dbRetrieved.dataSources.first().id

                    // Create data source with relation
                    val sourceDataSource =
                        client.dataSources.create {
                            databaseId(database.id)
                            title("Source")
                            properties {
                                title("Name")
                                relation("Links", database.id, targetDataSourceId)
                            }
                        }

                    delay(1000)

                    // Create only 5 target pages (well below pagination limit)
                    val targetPageIds = mutableListOf<String>()
                    for (i in 1..5) {
                        val page =
                            client.pages.create {
                                parent.dataSource(targetDataSourceId)
                                properties {
                                    title("Name", "Item $i")
                                }
                            }
                        targetPageIds.add(page.id)
                    }

                    println("   Created ${targetPageIds.size} target pages")
                    delay(1000)

                    // Create page with small number of relations
                    val relationReferences = targetPageIds.map { PageReference(id = it) }
                    val sourcePage =
                        client.pages.create {
                            parent.dataSource(sourceDataSource.id)
                            properties {
                                title("Name", "Small Test")
                                relation("Links", relationReferences)
                            }
                        }

                    delay(2000)

                    // Get property ID
                    val retrievedPage = client.pages.retrieve(sourcePage.id)
                    val relationProperty = retrievedPage.properties["Links"]
                    val propertyId = relationProperty?.id ?: throw IllegalStateException("Property not found")

                    // Test Flow with small result
                    val flowItems =
                        client.pages
                            .retrievePropertyItemsAsFlow(sourcePage.id, propertyId)
                            .toList()

                    flowItems.size shouldBe targetPageIds.size
                    println("✅ Small result set handled correctly!")
                    println("   - Emitted ${flowItems.size} items")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        createdDatabases.forEach { client.databases.archive(it) }
                        println("🧹 Cleaned up test database")
                    }

                    println("🎉 Small result set test completed!")
                } finally {
                    client.close()
                }
            }
        }
    })
