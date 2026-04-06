package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import kotlinx.coroutines.delay

/**
 * Integration tests for Pages API move functionality.
 *
 * Tests the move page endpoint and convenience methods:
 * - moveToPage(pageId, parentPageId)
 * - moveToDataSource(pageId, dataSourceId)
 * - move(pageId, parent)
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update/move pages
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 */
@Tags("Integration", "RequiresApi")
class PageMoveIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping PageMoveIntegrationTest due to missing environment variables") }
        } else {

            "Should move a page to a new parent page" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📦 Testing move page to parent page functionality...")

                    // Step 1: Create a source page
                    println("📝 Creating source page...")
                    val sourcePage =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("Move Test - Source Page")
                            icon.emoji("📄")
                            content {
                                paragraph("This page will be moved to a new parent.")
                            }
                        }
                    println("✅ Source page created: ${sourcePage.id}")
                    println("   URL: ${sourcePage.url}")
                    delay(1500)

                    // Step 2: Create a destination parent page
                    println("📝 Creating destination parent page...")
                    val destinationParent =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("Move Test - Destination Parent")
                            icon.emoji("📁")
                            content {
                                heading1("Destination Folder")
                                paragraph("The source page will be moved here.")
                            }
                        }
                    println("✅ Destination parent created: ${destinationParent.id}")
                    println("   URL: ${destinationParent.url}")
                    delay(1500)

                    // Step 3: Move the source page to the destination parent
                    println("🔄 Moving page to new parent...")
                    val movedPage = client.pages.moveToPage(sourcePage.id, destinationParent.id)
                    println("✅ Page moved: ${movedPage.id}")
                    println("   URL: ${movedPage.url}")
                    delay(1500)

                    // Step 4: Verify the page was moved
                    val retrievedPage = client.pages.retrieve(sourcePage.id)
                    retrievedPage.parent.id shouldBe destinationParent.id

                    println("✅ Move page functionality verified!")
                    println("   Source page is now a child of the destination parent.")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up test pages...")
                        client.pages.trash(sourcePage.id)
                        client.pages.trash(destinationParent.id)
                        println("✅ Test pages archived")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Source page: ${sourcePage.id}")
                        println("   URL: ${sourcePage.url}")
                        println("   Destination parent: ${destinationParent.id}")
                        println("   URL: ${destinationParent.url}")
                    }
                } finally {
                    client.close()
                }
            }

            "Should move a page into a database (data source)" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📦 Testing move page to data source functionality...")

                    // Step 1: Create a standalone page
                    println("📝 Creating standalone page...")
                    val standalonePage =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("Page to Move to Database")
                            icon.emoji("📄")
                            content {
                                paragraph("This page will be moved into a database.")
                            }
                        }
                    println("✅ Standalone page created: ${standalonePage.id}")
                    println("   URL: ${standalonePage.url}")
                    delay(1500)

                    // Step 2: Create a database
                    println("📝 Creating destination database...")
                    val destinationDb =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Move Destination Database")
                            icon.emoji("🗃️")
                            properties {
                                title("Name")
                            }
                        }
                    println("✅ Destination database created: ${destinationDb.id}")
                    println("   URL: ${destinationDb.url}")
                    delay(2000)

                    // Get data source
                    val retrievedDb = client.databases.retrieve(destinationDb.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    // Step 3: Move the page into the database
                    println("🔄 Moving page to database...")
                    val movedPage = client.pages.moveToDataSource(standalonePage.id, dataSourceId)
                    println("✅ Page moved to database: ${movedPage.id}")
                    println("   URL: ${movedPage.url}")
                    delay(1500)

                    // Step 4: Verify the page is now in the database
                    val retrievedPage = client.pages.retrieve(standalonePage.id)
                    retrievedPage.parent.id shouldBe dataSourceId

                    println("✅ Move to data source functionality verified!")
                    println("   Page is now a database entry.")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up...")
                        client.pages.trash(standalonePage.id)
                        client.databases.trash(destinationDb.id)
                        println("✅ Test database and page archived")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Page: ${standalonePage.id}")
                        println("   URL: ${standalonePage.url}")
                        println("   Database: ${destinationDb.id}")
                        println("   URL: ${destinationDb.url}")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
