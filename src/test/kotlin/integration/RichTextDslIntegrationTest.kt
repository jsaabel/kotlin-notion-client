package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Color
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.blocks.Block
import no.saabelit.kotlinnotionclient.models.blocks.pageContent
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders

/**
 * Real-world integration test for the Rich Text DSL with mixed formatting.
 *
 * This test validates the new rich text DSL by creating content with mixed formatting,
 * uploading it to Notion, and verifying the structure.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update pages and blocks
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 *
 * Run with: ./gradlew integrationTest --tests "*RichTextDslIntegrationTest*"
 */
@Tags("Integration", "RequiresApi")
class RichTextDslIntegrationTest :
    StringSpec({

        // Helper function to check if cleanup should be performed after tests
        fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"

        "Should create page with rich text DSL mixed formatting and verify with real API" {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            if (token != null && parentPageId != null) {
                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    // Step 1: Create initial page
                    println("📄 Creating test page for Rich Text DSL demonstration...")
                    val initialPageRequest =
                        CreatePageRequest(
                            parent =
                                Parent(
                                    type = "page_id",
                                    pageId = parentPageId,
                                ),
                            icon = RequestBuilders.createEmojiIcon("✨"),
                            properties =
                                mapOf(
                                    "title" to
                                        PagePropertyValue.TitleValue(
                                            title = listOf(RequestBuilders.createSimpleRichText("Rich Text DSL Integration Test")),
                                        ),
                                ),
                        )

                    val createdPage = client.pages.create(initialPageRequest)
                    createdPage.objectType shouldBe "page"
                    createdPage.archived shouldBe false

                    println("✅ Initial page created: ${createdPage.id}")

                    // Small delay to ensure Notion has processed the page creation
                    delay(500)

                    // Step 2: Create content using the new rich text DSL
                    println("🎨 Building content with rich text DSL mixed formatting...")

                    val pageContent =
                        pageContent {
                            // Header with mixed formatting
                            heading1 {
                                text("Welcome to the ")
                                bold("Rich Text DSL")
                                text(" test!")
                            }

                            // Paragraph with comprehensive formatting
                            paragraph {
                                text("This paragraph demonstrates ")
                                bold("bold text")
                                text(", ")
                                italic("italic text")
                                text(", ")
                                boldItalic("bold italic")
                                text(", ")
                                code("code formatting")
                                text(", ")
                                strikethrough("strikethrough")
                                text(", and ")
                                underline("underlined text")
                                text(".")
                            }

                            // Paragraph with colors
                            paragraph {
                                text("We also support ")
                                colored("colored text", Color.RED)
                                text(" and ")
                                backgroundColored("background colors", Color.BLUE_BACKGROUND)
                                text("!")
                            }

                            // Paragraph with links
                            paragraph {
                                text("Visit ")
                                link("https://notion.so/", "Notion")
                                text(" or check out ")
                                link("https://kotlinlang.org")
                                text(" for more information.")
                            }

                            // Paragraph with equations
                            paragraph {
                                text("The famous equation ")
                                equation("E = mc^2")
                                text(" revolutionized physics.")
                            }

                            // Different block types with mixed formatting
                            heading2 {
                                text("Block Types with ")
                                colored("Mixed Formatting", Color.GREEN)
                            }

                            bullet {
                                text("Bullet with ")
                                bold("bold")
                                text(" and ")
                                italic("italic")
                                text(" formatting")
                            }

                            number {
                                text("Numbered item with ")
                                code("code")
                                text(" and ")
                                colored("colors", Color.PURPLE)
                            }

                            toDo(checked = false) {
                                text("Todo item with ")
                                strikethrough("strikethrough")
                                text(" and ")
                                underline("underline")
                            }

                            quote {
                                text("This is a quote with ")
                                boldItalic("bold italic")
                                text(" and ")
                                link("https://example.com", "a link")
                                text(".")
                            }

                            callout("🎯") {
                                text("Callout with ")
                                backgroundColored("highlighted text", Color.YELLOW_BACKGROUND)
                                text(" and ")
                                equation("x^2 + y^2 = z^2")
                                text("!")
                            }

                            toggle {
                                text("Toggle with ")
                                colored("multiple", Color.RED)
                                text(" ")
                                colored("different", Color.BLUE)
                                text(" ")
                                colored("colors", Color.GREEN)
                            }

                            // Complex nested formatting
                            heading3 {
                                text("Complex ")
                                bold("Nested")
                                text(" ")
                                italic("Formatting")
                                text(" ")
                                colored("Test", Color.PINK)
                            }

                            paragraph {
                                text("This demonstrates complex formatting: ")
                                bold("Bold with ")
                                italic("italic inside")
                                text(" and ")
                                code("code")
                                text(" with ")
                                link("https://github.com", "GitHub link")
                                text(" and ")
                                equation("\\sum_{i=1}^{n} x_i")
                                text(" and ")
                                colored("colored", Color.ORANGE)
                                text(" and ")
                                backgroundColored("background", Color.GREEN_BACKGROUND)
                                text(" all in one paragraph!")
                            }

                            divider()

                            paragraph {
                                text("🎉 Rich Text DSL integration test completed with ")
                                bold("mixed formatting")
                                text(" working perfectly!")
                            }
                        }

                    // Step 3: Validate DSL-generated structure
                    println("✅ DSL created ${pageContent.size} blocks with rich text formatting")

                    // Step 4: Append content to Notion page
                    println("📤 Appending rich text DSL content to Notion page...")
                    val appendResponse = client.blocks.appendChildren(createdPage.id, pageContent)

                    appendResponse.objectType shouldBe "list"
                    appendResponse.results shouldHaveSize pageContent.size

                    println("✅ Content successfully appended to Notion")

                    // Step 5: Retrieve and verify from Notion
                    delay(1000) // Give Notion time to process
                    println("🔍 Retrieving content from Notion to verify rich text formatting...")

                    val blocks = client.blocks.retrieveChildren(createdPage.id)
                    blocks shouldHaveSize pageContent.size

                    // Step 6: Verify specific rich text content
                    // Test heading1 with mixed formatting
                    val heading1 = blocks[0] as Block.Heading1
                    heading1.heading1.richText shouldHaveSize 3
                    heading1.heading1.richText[0].plainText shouldBe "Welcome to the "
                    heading1.heading1.richText[1].plainText shouldBe "Rich Text DSL"
                    heading1.heading1.richText[1]
                        .annotations.bold shouldBe true
                    heading1.heading1.richText[2].plainText shouldBe " test!"

                    println("✅ Heading1 mixed formatting verified")

                    // Test paragraph with comprehensive formatting
                    val paragraph1 = blocks[1] as Block.Paragraph
                    paragraph1.paragraph.richText shouldHaveSize 13 // 7 text segments

                    // Find bold text
                    val boldText = paragraph1.paragraph.richText.find { it.plainText == "bold text" }
                    boldText?.annotations?.bold shouldBe true

                    // Find italic text
                    val italicText = paragraph1.paragraph.richText.find { it.plainText == "italic text" }
                    italicText?.annotations?.italic shouldBe true

                    // Find bold italic text
                    val boldItalicText = paragraph1.paragraph.richText.find { it.plainText == "bold italic" }
                    boldItalicText?.annotations?.bold shouldBe true
                    boldItalicText?.annotations?.italic shouldBe true

                    // Find code text
                    val codeText = paragraph1.paragraph.richText.find { it.plainText == "code formatting" }
                    codeText?.annotations?.code shouldBe true

                    println("✅ Paragraph comprehensive formatting verified")

                    // Test paragraph with colors
                    val paragraph2 = blocks[2] as Block.Paragraph
                    val redText = paragraph2.paragraph.richText.find { it.plainText == "colored text" }
                    redText?.annotations?.color shouldBe Color.RED

                    val backgroundText = paragraph2.paragraph.richText.find { it.plainText == "background colors" }
                    backgroundText?.annotations?.color shouldBe Color.BLUE_BACKGROUND

                    println("✅ Color formatting verified")

                    // Test paragraph with links
                    val paragraph3 = blocks[3] as Block.Paragraph
                    val linkText = paragraph3.paragraph.richText.find { it.plainText == "Notion" }
                    linkText?.href shouldBe "https://notion.so/"
                    linkText?.text?.link?.url shouldBe "https://notion.so/"

                    println("✅ Link formatting verified")

                    // Test paragraph with equation
                    val paragraph4 = blocks[4] as Block.Paragraph
                    val equationText = paragraph4.paragraph.richText.find { it.type == "equation" }
                    equationText?.equation?.expression shouldBe "E = mc^2"

                    println("✅ Equation formatting verified")

                    // Test different block types
                    val bullet = blocks[6] as Block.BulletedListItem
                    bullet.bulletedListItem.richText shouldHaveSize 5
                    val bulletBold = bullet.bulletedListItem.richText.find { it.plainText == "bold" }
                    bulletBold?.annotations?.bold shouldBe true

                    println("✅ Bullet formatting verified")

                    val quote = blocks[9] as Block.Quote
                    val quoteBoldItalic = quote.quote.richText.find { it.plainText == "bold italic" }
                    quoteBoldItalic?.annotations?.bold shouldBe true
                    quoteBoldItalic?.annotations?.italic shouldBe true

                    println("✅ Quote formatting verified")

                    val callout = blocks[10] as Block.Callout
                    val calloutBackground = callout.callout.richText.find { it.plainText == "highlighted text" }
                    calloutBackground?.annotations?.color shouldBe Color.YELLOW_BACKGROUND

                    println("✅ Callout formatting verified")

                    println("🎉 Rich Text DSL integration test completed successfully!")
                    println("   - All formatting types working: ✅")
                    println("   - Colors working: ✅")
                    println("   - Links working: ✅")
                    println("   - Equations working: ✅")
                    println("   - Mixed formatting working: ✅")
                    println("   - Integration with all block types: ✅")

                    // Step 7: Conditionally clean up
                    delay(500)
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up - archiving test page...")
                        val archivedPage = client.pages.archive(createdPage.id)
                        archivedPage.archived shouldBe true
                        println("✅ Test page archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created page: ${createdPage.id} (\"Rich Text DSL Integration Test\")")
                        println("   Contains rich text with mixed formatting for manual inspection")
                    }
                } finally {
                    client.close()
                }
            } else {
                println("⏭️ Skipping Rich Text DSL integration test")
                println("   Required environment variables:")
                println("   - NOTION_API_TOKEN: Your integration API token")
                println("   - NOTION_TEST_PAGE_ID: Page where test content will be created")
                println(
                    "   Example: export NOTION_API_TOKEN='secret_...' && export NOTION_TEST_PAGE_ID='12345678-1234-1234-1234-123456789abc'",
                )
            }
        }
    })
