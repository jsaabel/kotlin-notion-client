package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.pages.PageIcon
import kotlinx.coroutines.delay

/**
 * Integration tests for Pages API lock/unlock functionality.
 *
 * Tests the lock and unlock methods added in the v0.3 update:
 * - lock() / lock(true) / lock(false)
 * - unlock()
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update pages
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 */
@Tags("Integration", "RequiresApi")
class PageLockIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping PageLockIntegrationTest due to missing environment variables") }
        } else {

            "Should lock and unlock a page" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔒 Testing lock/unlock page functionality...")

                    // Step 1: Create a test page
                    println("📝 Creating test page...")
                    val testPage =
                        client.pages.create {
                            parent.page(parentPageId)
                            title("Lock Test Page")
                            icon.emoji("🔓")
                            content {
                                paragraph("This page will be locked and unlocked.")
                            }
                        }
                    println("✅ Test page created: ${testPage.id}")
                    println("   URL: ${testPage.url}")
                    delay(1500)

                    // Verify initial state (should be unlocked)
                    testPage.isLocked shouldBe false
                    println("✅ Initial state verified: page is unlocked")

                    // Step 2: Lock the page
                    println("🔒 Locking page...")
                    val lockedPage =
                        client.pages.update(testPage.id) {
                            lock()
                            icon.emoji("🔒")
                        }
                    delay(1000)

                    lockedPage.isLocked shouldBe true
                    (lockedPage.icon as? PageIcon.Emoji)?.emoji shouldBe "🔒"
                    println("✅ Page locked successfully!")
                    println("   URL: ${lockedPage.url}")

                    // Step 3: Verify lock persists on retrieval
                    val retrievedLocked = client.pages.retrieve(testPage.id)
                    retrievedLocked.isLocked shouldBe true
                    println("✅ Lock state persists on retrieval")

                    // Step 4: Unlock the page
                    println("🔓 Unlocking page...")
                    val unlockedPage =
                        client.pages.update(testPage.id) {
                            unlock()
                            icon.emoji("🔓")
                        }
                    delay(1000)

                    unlockedPage.isLocked shouldBe false
                    (unlockedPage.icon as? PageIcon.Emoji)?.emoji shouldBe "🔓"
                    println("✅ Page unlocked successfully!")

                    // Step 5: Test lock(true) and lock(false) syntax
                    println("🔄 Testing lock(true) syntax...")
                    val lockedAgain =
                        client.pages.update(testPage.id) {
                            lock(true)
                        }
                    lockedAgain.isLocked shouldBe true

                    println("🔄 Testing lock(false) syntax...")
                    val unlockedAgain =
                        client.pages.update(testPage.id) {
                            lock(false)
                        }
                    unlockedAgain.isLocked shouldBe false

                    println("✅ All lock/unlock variations verified!")

                    // Cleanup
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up test page...")
                        client.pages.trash(testPage.id)
                        println("✅ Test page archived")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Test page: ${testPage.id}")
                        println("   URL: ${testPage.url}")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
