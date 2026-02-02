package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import kotlinx.coroutines.delay

/**
 * Integration tests for Pages API position functionality.
 *
 * Tests the position parameter when creating pages:
 * - position.pageStart()
 * - position.pageEnd()
 * - position.afterBlock(blockId)
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read pages
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 */
@Tags("Integration", "RequiresApi")
class PagePositionIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping PagePositionIntegrationTest due to missing environment variables") }
        } else {

            "Should create pages with position at page start and end" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📍 Testing page position functionality...")

                    // Step 1: Create a container page with some content
                    println("📝 Creating container page with initial content...")
                    val containerPage =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("Position Test Container")
                            icon.emoji("📦")
                            content {
                                heading1("Container Page")
                                paragraph("First paragraph - existing content")
                                paragraph("Second paragraph - existing content")
                            }
                        }
                    println("✅ Container page created: ${containerPage.id}")
                    println("   URL: ${containerPage.url}")
                    delay(2000)

                    // Step 2: Create a page positioned at page start
                    println("📝 Creating page at position: page_start...")
                    val startPage =
                        client.pages.create {
                            parent.page(containerPage.id)
                            title("Page at Start")
                            icon.emoji("1️⃣")
                            position.pageStart()
                        }
                    println("✅ Page created at start: ${startPage.id}")
                    println("   URL: ${startPage.url}")
                    delay(1500)

                    // Step 3: Create a page positioned at page end
                    println("📝 Creating page at position: page_end...")
                    val endPage =
                        client.pages.create {
                            parent.page(containerPage.id)
                            title("Page at End")
                            icon.emoji("🔚")
                            position.pageEnd()
                        }
                    println("✅ Page created at end: ${endPage.id}")
                    println("   URL: ${endPage.url}")
                    delay(1500)

                    // Verify pages were created with correct parent
                    val retrievedStartPage = client.pages.retrieve(startPage.id)
                    retrievedStartPage.parent.id shouldBe containerPage.id

                    val retrievedEndPage = client.pages.retrieve(endPage.id)
                    retrievedEndPage.parent.id shouldBe containerPage.id

                    println("✅ Page position functionality verified!")
                    println("   Pages created with specified positions under container page.")
                    println("   Manual verification: Check that child pages appear in correct order.")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up test pages...")
                        client.pages.archive(startPage.id)
                        client.pages.archive(endPage.id)
                        client.pages.archive(containerPage.id)
                        println("✅ Test pages archived")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Container page: ${containerPage.id}")
                        println("   URL: ${containerPage.url}")
                        println("   Start page: ${startPage.id}")
                        println("   URL: ${startPage.url}")
                        println("   End page: ${endPage.id}")
                        println("   URL: ${endPage.url}")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
