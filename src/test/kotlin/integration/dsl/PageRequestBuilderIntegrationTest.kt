package integration.dsl

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.blocks.Block
import no.saabelit.kotlinnotionclient.models.pages.PageProperty
import no.saabelit.kotlinnotionclient.models.pages.createPageRequest

/**
 * Self-contained integration test for the PageRequestBuilder DSL.
 *
 * This test validates the full workflow of creating pages using the DSL,
 * uploading them to Notion, and verifying the structure.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update pages and blocks
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 *
 * Run with: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi")
class PageRequestBuilderIntegrationTest :
    StringSpec({

        // Helper function to check if cleanup should be performed after tests
        fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"

        "Should create child page with DSL and verify structure" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    println("📄 Creating test page with PageRequestBuilder DSL...")

                    // Create page using DSL
                    val pageRequest =
                        createPageRequest {
                            parent.page(parentPageId)
                            title("DSL Integration Test Page")
                            icon.emoji("🚀")
                            cover.external("https://placehold.co/1200x400.png")
                            content {
                                heading1("PageRequestBuilder DSL Test")
                                paragraph("This page was created using the new PageRequestBuilder DSL!")
                                divider()
                                heading2("Features Demonstrated")
                                bullet("Type-safe page construction")
                                bullet("Integrated content creation")
                                bullet("Icon and cover support")
                                divider()
                                quote("The DSL significantly reduces boilerplate code while maintaining full type safety.")
                                code(
                                    language = "kotlin",
                                    code =
                                        """
                                        pageRequest {
                                            parent.page(parentId)
                                            title("My Page")
                                            content {
                                                heading1("Hello World!")
                                            }
                                        }
                                        """.trimIndent(),
                                )
                            }
                        }

                    val createdPage = client.pages.create(pageRequest)
                    createdPage.objectType shouldBe "page"
                    createdPage.archived shouldBe false

                    println("✅ Page created: ${createdPage.id}")

                    // Small delay to ensure Notion has processed the page creation
                    delay(500)

                    // Verify page properties
                    createdPage.parent.pageId shouldBe parentPageId
                    createdPage.icon?.emoji shouldBe "🚀"
                    createdPage.cover?.external?.url shouldContain "placehold"

                    // Verify the title was set correctly
                    val titleProperty = createdPage.properties["title"]
                    titleProperty.shouldNotBeNull()
                    titleProperty.shouldBeInstanceOf<PageProperty.Title>()
                    titleProperty.title.shouldHaveSize(1)
                    titleProperty.plainText shouldBe "DSL Integration Test Page"

                    println("✅ Page properties verified")

                    // Verify content was added
                    delay(1000) // Give Notion time to process content
                    val blocks = client.blocks.retrieveChildren(createdPage.id)
                    blocks.shouldNotBeNull()
                    blocks shouldHaveSize 10 // heading1, paragraph, divider, heading2, 3 bullets, divider, quote, code

                    // Verify specific content
                    val firstBlock = blocks[0] as Block.Heading1
                    firstBlock.type shouldBe "heading_1"
                    firstBlock.heading1.richText[0].plainText shouldBe "PageRequestBuilder DSL Test"

                    println("✅ Page content verified")

                    println("✅ PageRequestBuilder DSL integration test completed successfully!")
                    println("   - DSL created page with all components: ✅")
                    println("   - Content blocks working: ✅")
                    println("   - Icon and cover working: ✅")
                    println("   - Type safety enforced: ✅")

                    // Conditionally clean up
                    delay(500)
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up - archiving test page...")
                        val archivedPage = client.pages.archive(createdPage.id)
                        archivedPage.archived shouldBe true
                        println("✅ Test page archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created page: ${createdPage.id} (\"DSL Integration Test Page\")")
                        println("   Contains 8 content blocks with rich formatting")
                    }
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping PageRequestBuilder DSL integration test")
                println("   Required environment variables:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_TEST_PAGE_ID: Page where test content will be created")
                println(
                    "   Example: export NOTION_API_TOKEN='secret_...' && export NOTION_TEST_PAGE_ID='12345678-1234-1234-1234-123456789abc'",
                )
            }
        }

        "Should validate DSL constraints properly" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    println("🔍 Testing validation constraints...")

                    // Test that properties validation works as expected
                    try {
                        createPageRequest {
                            parent.page(parentPageId)
                            properties {
                                richText("Invalid", "This should fail validation")
                            }
                        }
                        throw AssertionError("Expected validation to fail but it didn't")
                    } catch (e: IllegalStateException) {
                        e.message shouldContain "Custom properties can only be set when creating pages in a database"
                        println("✅ Validation constraint working correctly")
                    }

                    // Test that title is allowed with page parent
                    val validRequest =
                        createPageRequest {
                            parent.page(parentPageId)
                            title("Valid Title Only")
                        }

                    // This should not throw an exception
                    validRequest.properties["title"].shouldNotBeNull()
                    println("✅ Title property allowed with page parent")

                    println("✅ DSL validation tests completed successfully!")
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping validation test - missing environment variables")
            }
        }
    })
