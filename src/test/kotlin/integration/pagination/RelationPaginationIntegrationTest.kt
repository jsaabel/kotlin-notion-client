package integration.pagination

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.pages.PageReference
import kotlinx.coroutines.delay

/**
 * Integration tests for relation property pagination functionality.
 *
 * These tests verify that the client correctly handles paginated responses
 * when retrieving relation properties that reference many pages (>20).
 *
 * Tests cover:
 * - Automatic pagination for relation properties with >20 linked pages
 * - PagesApi.retrievePropertyItems functionality
 * - Performance characteristics for large relation properties
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects
 *
 * Note: This test creates two databases and many pages to test relation pagination.
 * It may take several minutes to complete and is tagged as "Slow" accordingly.
 */
@Tags("Integration", "RequiresApi", "Slow")
class RelationPaginationIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping RelationPaginationIntegrationTest due to missing environment variables") }
        } else {

            "Should automatically paginate relation properties with >20 linked pages" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient.create(NotionConfig(apiToken = token))
                val createdDatabases = mutableListOf<String>()

                try {
                    // Check parent page status first
                    println("🔍 Checking parent page status...")
                    val parentPage = client.pages.retrieve(parentPageId)
                    println("   Parent page archived: ${parentPage.archived}")

                    if (parentPage.archived) {
                        println("⚠️  Parent page is archived - tests may fail")
                        println("   You may need to unarchive the parent page in Notion")
                    }

                    // Step 1: Create a database with an initial data source
                    println("🗄️ Creating database for relation testing...")
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Relation Pagination Test DB - ${System.currentTimeMillis()}")
                            icon.emoji("🔗")
                            properties {
                                title("Name")
                                select("Category")
                            }
                        }

                    createdDatabases.add(database.id)
                    println("✅ Database created: ${database.id}")
                    delay(1000)

                    // Get the first data source (target data source)
                    val dbRetrieved = client.databases.retrieve(database.id)
                    val targetDataSourceId = dbRetrieved.dataSources.first().id
                    println("✅ Retrieved first data source (target): $targetDataSourceId")

                    // Step 2: Create a second data source in the same database
                    println("\n🗄️ Creating second data source with relation property...")
                    val sourceDataSource =
                        client.dataSources.create(
                            it.saabel.kotlinnotionclient.models.databases.CreateDataSourceRequest(
                                parent =
                                    it.saabel.kotlinnotionclient.models.base
                                        .Parent(type = "database_id", databaseId = database.id),
                                title =
                                    listOf(
                                        it.saabel.kotlinnotionclient.models.base.RichText.fromPlainText(
                                            "Source Data Source - ${System.currentTimeMillis()}",
                                        ),
                                    ),
                                properties =
                                    mapOf(
                                        "Task Name" to
                                            it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty
                                                .Title(),
                                        "Related Items" to
                                            it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty.Relation(
                                                relation =
                                                    it.saabel.kotlinnotionclient.models.databases.RelationConfiguration(
                                                        databaseId = database.id,
                                                        dataSourceId = targetDataSourceId,
                                                        singleProperty =
                                                            it.saabel.kotlinnotionclient.models.base
                                                                .EmptyObject(),
                                                    ),
                                            ),
                                    ),
                            ),
                        )

                    val sourceDataSourceId = sourceDataSource.id
                    println("✅ Second data source created: $sourceDataSourceId")
                    delay(1000)

                    // Step 3: Create many pages in the target data source (25 pages to exceed typical relation pagination limit of 20)
                    println("\n📄 Creating 25 target pages for relation linking...")
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
                            println("   Created $i/25 target pages")
                            delay(100) // Small delay to avoid rate limits
                        }
                    }

                    println("✅ Created ${targetPageIds.size} target pages")
                    delay(1000)

                    // Step 4: Create a page in the source data source and link it to ALL target pages
                    println("\n📄 Creating source page with relations to all target pages...")
                    val relationReferences = targetPageIds.map { PageReference(id = it) }

                    val sourcePage =
                        client.pages.create {
                            parent.dataSource(sourceDataSourceId)
                            properties {
                                title("Task Name", "Page with Many Relations")
                                relation("Related Items", relationReferences)
                            }
                        }

                    println("✅ Source page created: ${sourcePage.id}")
                    println("   Linked to ${relationReferences.size} target pages")
                    delay(2000) // Wait for relations to be processed

                    // Step 5: Test relation pagination by retrieving the relation property items
                    println("\n🔍 Testing relation property pagination...")

                    // First, retrieve the page to get property info
                    val retrievedPage = client.pages.retrieve(sourcePage.id)
                    val relationProperty = retrievedPage.properties["Related Items"]

                    // Find the relation property ID
                    val relationPropertyId = relationProperty?.id ?: throw IllegalStateException("Relation property not found")

                    println("   Relation property ID: $relationPropertyId")

                    // Use retrievePropertyItems to get all relation items (should trigger pagination)
                    println("   Retrieving relation property items (may trigger pagination)...")
                    val startTime = System.currentTimeMillis()
                    val relationItems = client.pages.retrievePropertyItems(sourcePage.id, relationPropertyId)
                    val retrievalTime = System.currentTimeMillis() - startTime

                    // Verify we got all the relations
                    relationItems.size shouldBe targetPageIds.size
                    relationItems.size shouldBeGreaterThan 20 // Should exceed typical page size

                    println("✅ Relation pagination successful!")
                    println("   - Retrieved ${relationItems.size} relation items")
                    println("   - Retrieval time: ${retrievalTime}ms")
                    println("   - Automatically handled pagination (>20 relations)")

                    // Verify that the relations point to the correct pages
                    val retrievedTargetIds =
                        relationItems.mapNotNull { propertyItem ->
                            propertyItem.relation?.id
                        }

                    retrievedTargetIds.size shouldBe targetPageIds.size
                    retrievedTargetIds.toSet() shouldBe targetPageIds.toSet()

                    println("   - All target page IDs correctly retrieved")
                    println("   - Relation integrity verified")

                    // Cleanup - just delete the database, which cleans up all data sources and pages
                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test database...")
                        createdDatabases.forEach { databaseId ->
                            try {
                                client.databases.archive(databaseId)
                            } catch (e: Exception) {
                                println("   Warning: Failed to clean up database $databaseId")
                            }
                        }
                        println("✅ Database archived (all data sources and pages cleaned up automatically)")
                    } else {
                        println("\n🔧 Test database preserved:")
                        println("   Database: ${database.id}")
                        println("   Target data source: $targetDataSourceId (${targetPageIds.size} pages)")
                        println("   Source data source: $sourceDataSourceId")
                        println("   Source page with ${relationItems.size} relations: ${sourcePage.id}")
                    }

                    println("\n🎉 Relation pagination test completed successfully!")
                } finally {
                    client.close()
                }
            }
        }
    })
