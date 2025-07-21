package integration.pagination

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseProperty
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseRequest
import no.saabelit.kotlinnotionclient.models.databases.RelationConfiguration
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.pages.PageReference
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

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

        fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"

        "Should automatically paginate relation properties with >20 linked pages" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
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

                    // Step 1: Create a target database (what we'll link TO)
                    println("🗄️ Creating target database for relation testing...")
                    val targetDbRequest =
                        CreateDatabaseRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            title =
                                listOf(
                                    RequestBuilders.createSimpleRichText("Target DB - Relation Test - ${System.currentTimeMillis()}"),
                                ),
                            icon = RequestBuilders.createEmojiIcon("🎯"),
                            properties =
                                mapOf(
                                    "Name" to CreateDatabaseProperty.Title(),
                                    "Category" to CreateDatabaseProperty.Select(),
                                ),
                        )

                    val targetDb = client.databases.create(targetDbRequest)
                    createdDatabases.add(targetDb.id)
                    println("✅ Target database created: ${targetDb.id}")
                    delay(500)

                    // Step 2: Create many pages in the target database (25 pages to exceed typical relation pagination limit of 20)
                    println("\n📄 Creating 25 target pages for relation linking...")
                    val targetPageIds = mutableListOf<String>()

                    for (i in 1..25) {
                        val targetPage =
                            client.pages.create(
                                CreatePageRequest(
                                    parent = Parent(type = "database_id", databaseId = targetDb.id),
                                    properties =
                                        mapOf(
                                            "Name" to
                                                PagePropertyValue.TitleValue(
                                                    title = listOf(RequestBuilders.createSimpleRichText("Target Item $i")),
                                                ),
                                        ),
                                ),
                            )
                        targetPageIds.add(targetPage.id)

                        if (i % 10 == 0) {
                            println("   Created $i/25 target pages")
                            delay(100) // Small delay to avoid rate limits
                        }
                    }

                    println("✅ Created ${targetPageIds.size} target pages")
                    delay(1000)

                    // Step 3: Create a source database with a relation property pointing to the target database
                    println("\n🗄️ Creating source database with relation property...")
                    val sourceDbRequest =
                        CreateDatabaseRequest(
                            parent = Parent(type = "page_id", pageId = parentPageId),
                            title =
                                listOf(
                                    RequestBuilders.createSimpleRichText("Source DB - Relation Test - ${System.currentTimeMillis()}"),
                                ),
                            icon = RequestBuilders.createEmojiIcon("📊"),
                            properties =
                                mapOf(
                                    "Name" to CreateDatabaseProperty.Title(),
                                    "Related Items" to
                                        CreateDatabaseProperty.Relation(
                                            RelationConfiguration.singleProperty(targetDb.id),
                                        ),
                                ),
                        )

                    val sourceDb = client.databases.create(sourceDbRequest)
                    createdDatabases.add(sourceDb.id)
                    println("✅ Source database created: ${sourceDb.id}")
                    delay(500)

                    // Step 4: Create a page in the source database and link it to ALL target pages
                    println("\n📄 Creating source page with relations to all target pages...")
                    val relationReferences = targetPageIds.map { PageReference(id = it) }

                    val sourcePage =
                        client.pages.create(
                            CreatePageRequest(
                                parent = Parent(type = "database_id", databaseId = sourceDb.id),
                                properties =
                                    mapOf(
                                        "Name" to
                                            PagePropertyValue.TitleValue(
                                                title = listOf(RequestBuilders.createSimpleRichText("Page with Many Relations")),
                                            ),
                                        "Related Items" to
                                            PagePropertyValue.RelationValue(
                                                relation = relationReferences,
                                            ),
                                    ),
                            ),
                        )

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

                    // Cleanup - just delete the databases, which cleans up all pages
                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test databases...")
                        createdDatabases.forEach { databaseId ->
                            try {
                                client.databases.archive(databaseId)
                            } catch (e: Exception) {
                                println("   Warning: Failed to clean up database $databaseId")
                            }
                        }
                        println("✅ Databases archived (all pages cleaned up automatically)")
                    } else {
                        println("\n🔧 Test databases preserved:")
                        println("   Source DB: ${sourceDb.id}")
                        println("   Target DB: ${targetDb.id}")
                        println("   Source page with ${relationItems.size} relations: ${sourcePage.id}")
                    }

                    println("\n🎉 Relation pagination test completed successfully!")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping relation pagination test - missing environment variables")
                println("   Required:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_TEST_PAGE_ID: Parent page for test databases")
            }
        }
    })
