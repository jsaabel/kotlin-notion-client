package integration.pagination

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.EmptyObject
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.RelationConfiguration
import it.saabel.kotlinnotionclient.models.datasources.CreateDataSourceRequest
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.PageReference
import it.saabel.kotlinnotionclient.models.pages.getRelationProperty
import it.saabel.kotlinnotionclient.models.pages.getRelationPropertyPartial
import kotlinx.coroutines.delay

/**
 * Integration tests for relation property pagination functionality.
 *
 * Notion's page object returns at most 25 references per relation (`has_more: true` beyond
 * that). These tests link a page to more than 25 targets and verify that the client never
 * hands back a silently truncated relation (#91).
 *
 * Tests cover:
 * - The raw API really truncates at 25 (`has_more: true` with completion opted out), and
 *   `getRelationProperty` fails loudly on such a page
 * - `pages.retrieve`, `pages.create`/`update` and `dataSources.query` complete the relation by default
 * - `PagesApi.retrievePropertyItems` functionality
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

            "Should complete relation properties with more than 25 linked pages" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))
                val createdDatabases = mutableListOf<String>()

                try {
                    // Check parent page status first
                    println("🔍 Checking parent page status...")
                    val parentPage = client.pages.retrieve(parentPageId)
                    println("   Parent page archived: ${parentPage.inTrash}")

                    if (parentPage.inTrash) {
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
                            CreateDataSourceRequest(
                                parent =
                                    Parent.DatabaseParent(databaseId = database.id),
                                title =
                                    listOf(
                                        RichText.fromPlainText(
                                            "Source Data Source - ${System.currentTimeMillis()}",
                                        ),
                                    ),
                                properties =
                                    mapOf(
                                        "Task Name" to
                                            CreateDatabaseProperty
                                                .Title(),
                                        "Related Items" to
                                            CreateDatabaseProperty.Relation(
                                                relation =
                                                    RelationConfiguration(
                                                        databaseId = database.id,
                                                        dataSourceId = targetDataSourceId,
                                                        singleProperty =
                                                            EmptyObject(),
                                                    ),
                                            ),
                                    ),
                            ),
                        )

                    val sourceDataSourceId = sourceDataSource.id
                    println("✅ Second data source created: $sourceDataSourceId")
                    delay(1000)

                    // Step 3: Create more target pages than Notion's 25-reference cap on the page object
                    val targetCount = 30
                    println("\n📄 Creating $targetCount target pages for relation linking...")
                    val targetPageIds = mutableListOf<String>()

                    for (i in 1..targetCount) {
                        val targetPage =
                            client.pages.create {
                                parent.dataSource(targetDataSourceId)
                                properties {
                                    title("Name", "Target Item $i")
                                }
                            }

                        targetPageIds.add(targetPage.id)

                        if (i % 10 == 0) {
                            println("   Created $i/$targetCount target pages")
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

                    // Step 5: The create response is already complete (completion is on by default)
                    sourcePage.getRelationProperty("Related Items").map { it.id }.toSet() shouldBe targetPageIds.toSet()

                    // Step 6: Opted out, the raw page object is truncated at 25 and fails loudly
                    println("\n🔍 Retrieving the raw (uncompleted) page...")
                    val rawPage = client.pages.retrieve(sourcePage.id, completeTruncatedProperties = false)
                    val rawRelation = rawPage.properties["Related Items"] as PageProperty.Relation
                    println("   Raw relation: ${rawRelation.relation.size} references, has_more=${rawRelation.hasMore}")
                    rawRelation.hasMore shouldBe true
                    rawPage.getRelationPropertyPartial("Related Items").size shouldBe 25
                    shouldThrow<IllegalStateException> { rawPage.getRelationProperty("Related Items") }

                    // Step 7: pages.retrieve completes the relation by default
                    println("\n🔍 Retrieving the page with default completion...")
                    val startTime = System.currentTimeMillis()
                    val retrievedPage = client.pages.retrieve(sourcePage.id)
                    val retrievalTime = System.currentTimeMillis() - startTime
                    val completedRelation = retrievedPage.properties["Related Items"] as PageProperty.Relation
                    completedRelation.hasMore shouldBe false
                    retrievedPage.getRelationProperty("Related Items").map { it.id }.toSet() shouldBe targetPageIds.toSet()
                    println("✅ retrieve returned all ${completedRelation.relation.size} relations in ${retrievalTime}ms")

                    // Step 8: dataSources.query completes it too
                    println("\n🔍 Querying the source data source...")
                    val queriedPage = client.dataSources.query(sourceDataSourceId).single { it.id == sourcePage.id }
                    queriedPage.getRelationProperty("Related Items").map { it.id }.toSet() shouldBe targetPageIds.toSet()
                    println("✅ query returned all relations")

                    // Step 9: The update response is complete as well
                    val updatedPage =
                        client.pages.update(sourcePage.id) {
                            properties { title("Task Name", "Page with Many Relations (updated)") }
                        }
                    updatedPage.getRelationProperty("Related Items").size shouldBe targetCount

                    // Step 10: The explicit property-item endpoint still paginates on its own
                    val relationPropertyId = completedRelation.id
                    val relationItems = client.pages.retrievePropertyItems(sourcePage.id, relationPropertyId)
                    relationItems.size shouldBeGreaterThan 25
                    relationItems.mapNotNull { it.relation?.id }.toSet() shouldBe targetPageIds.toSet()
                    println("✅ retrievePropertyItems returned ${relationItems.size} relation items")

                    // Cleanup - just delete the database, which cleans up all data sources and pages
                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test database...")
                        createdDatabases.forEach { databaseId ->
                            try {
                                client.databases.trash(databaseId)
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
                        println("   Source page with $targetCount relations: ${sourcePage.id}")
                    }

                    println("\n🎉 Relation pagination test completed successfully!")
                } finally {
                    client.close()
                }
            }
        }
    })
