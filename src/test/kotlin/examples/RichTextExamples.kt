package examples

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Color
import it.saabel.kotlinnotionclient.models.blocks.Block
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * Validation tests for all code examples in docs/rich-text-dsl.md
 *
 * These tests ensure that every code example in the documentation:
 * 1. Compiles successfully
 * 2. Runs against the real Notion API
 * 3. Produces expected results
 *
 * Prerequisites:
 * - export NOTION_RUN_INTEGRATION_TESTS=true
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="page-id"
 *
 * Run with: ./gradlew test --tests "*RichTextExamples"
 */
@Tags("Integration", "RequiresApi", "Examples")
class RichTextExamples :
    StringSpec({

        if (!integrationTestEnvVarsAreSet("NOTION_TEST_PAGE_ID")) {
            "!(Skipped) Rich text examples" {
                println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true and required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            // Test data setup
            var testPageId: String? = null

            beforeSpec {
                println("🔧 Setting up test data for rich text examples...")

                // Create a test page
                val page =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Rich Text Examples Test")
                    }

                testPageId = page.id
                delay(2000)
                println("✅ Test setup complete")
            }

            afterSpec {
                if (shouldCleanupAfterTest() && testPageId != null) {
                    println("🧹 Cleaning up test page...")
                    try {
                        notion.pages.archive(testPageId)
                        println("✅ Cleanup complete")
                    } catch (e: Exception) {
                        println("⚠️ Cleanup failed: ${e.message}")
                    }
                }
            }

            // ========================================
            // Example 1: Basic Text Formatting
            // ========================================
            "Example 1: Text formatting (bold, italic, code)" {
                println("\n📖 Running Example 1: Text formatting")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 1: Text Formatting")

                    paragraph {
                        text("This text is ")
                        bold("bold")
                        text(", this is ")
                        italic("italic")
                        text(", and this is ")
                        boldItalic("both")
                        text(".")
                    }

                    paragraph {
                        text("This is ")
                        bold("bold text")
                        text(" using convenience method")
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                children.any { it is Block.Paragraph } shouldBe true
                println("✅ Example 1 passed")
            }

            // ========================================
            // Example 2: Colors
            // ========================================
            "Example 2: Text and background colors" {
                println("\n📖 Running Example 2: Colors")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 2: Colors")

                    paragraph {
                        text("This is ")
                        colored("red text", Color.RED)
                        text(" and ")
                        colored("blue text", Color.BLUE)
                    }

                    paragraph {
                        text("This has ")
                        backgroundColored("yellow background", Color.YELLOW_BACKGROUND)
                        text(" and ")
                        backgroundColored("blue background", Color.BLUE_BACKGROUND)
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 2 passed")
            }

            // ========================================
            // Example 3: Links
            // ========================================
            "Example 3: Links" {
                println("\n📖 Running Example 3: Links")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 3: Links")

                    paragraph {
                        text("Visit our ")
                        link("https://example.com/docs", "documentation")
                    }

                    paragraph {
                        text("Direct URL: ")
                        link("https://example.com")
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 3 passed")
            }

            // ========================================
            // Example 4: Date Mentions (String)
            // ========================================
            "Example 4: Date mentions using strings" {
                println("\n📖 Running Example 4: Date mentions (strings)")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 4: Date Mentions (Strings)")

                    paragraph {
                        text("Meeting on ")
                        dateMention("2025-10-15")
                        text(" at 2pm")
                    }

                    paragraph {
                        text("Conference from ")
                        dateMention(
                            start = "2025-10-15T09:00:00",
                            end = "2025-10-17T17:00:00",
                            timeZone = "America/New_York",
                        )
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 4 passed")
            }

            // ========================================
            // Example 5: Date Mentions (LocalDate)
            // ========================================
            "Example 5: Date mentions using LocalDate" {
                println("\n📖 Running Example 5: Date mentions (LocalDate)")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 5: Date Mentions (LocalDate)")

                    paragraph {
                        text("Due ")
                        dateMention(LocalDate(2025, 10, 15))
                    }

                    // Date ranges
                    paragraph {
                        text("Project: ")
                        dateMention(
                            start = LocalDate(2025, 10, 15),
                            end = LocalDate(2025, 10, 20),
                        )
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 5 passed")
            }

            // ========================================
            // Example 6: Date Mentions (LocalDateTime)
            // ========================================
            "Example 6: Date mentions using LocalDateTime" {
                println("\n📖 Running Example 6: Date mentions (LocalDateTime)")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 6: Date Mentions (LocalDateTime)")

                    paragraph {
                        text("Meeting ")
                        dateMention(
                            start = LocalDateTime(2025, 10, 15, 14, 30),
                            timeZone = TimeZone.of("America/New_York"),
                        )
                    }

                    // Datetime ranges
                    paragraph {
                        text("Conference ")
                        dateMention(
                            start = LocalDateTime(2025, 10, 15, 9, 0),
                            end = LocalDateTime(2025, 10, 17, 17, 0),
                            timeZone = TimeZone.of("America/New_York"),
                        )
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 6 passed")
            }

            // ========================================
            // Example 7: Date Mentions (Instant)
            // ========================================
            "Example 7: Date mentions using Instant" {
                println("\n📖 Running Example 7: Date mentions (Instant)")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 7: Date Mentions (Instant)")

                    paragraph {
                        text("Deployment at ")
                        dateMention(Instant.parse("2025-10-15T14:30:00Z"))
                    }

                    paragraph {
                        text("Window: ")
                        dateMention(
                            start = Instant.parse("2025-10-15T14:00:00Z"),
                            end = Instant.parse("2025-10-15T16:00:00Z"),
                        )
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 7 passed")
            }

            // ========================================
            // Example 8: Equations
            // ========================================
            "Example 8: Equations" {
                println("\n📖 Running Example 8: Equations")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 8: Equations")

                    paragraph {
                        text("The Pythagorean theorem: ")
                        equation("x^2 + y^2 = z^2")
                    }

                    paragraph {
                        text("Einstein's famous equation: ")
                        equation("E = mc^2")
                        text(" or the quadratic formula: ")
                        equation("x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}")
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 8 passed")
            }

            // ========================================
            // Example 9: Advanced Formatting
            // ========================================
            "Example 9: Advanced formatting with formattedText" {
                println("\n📖 Running Example 9: Advanced formatting")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 9: Advanced Formatting")

                    paragraph {
                        text("This is ")
                        formattedText("bold and italic", bold = true, italic = true)
                        text(" and this is ")
                        formattedText("code with color", code = true, color = Color.BLUE)
                    }

                    paragraph {
                        formattedText(
                            "complex text",
                            bold = true,
                            italic = true,
                            code = true,
                            strikethrough = true,
                            underline = true,
                            color = Color.RED,
                        )
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 9 passed")
            }

            // ========================================
            // Example 10: Complex Rich Text in a Block
            // ========================================
            "Example 10: Complex rich text in blocks" {
                println("\n📖 Running Example 10: Complex content")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 10: Complex Content")

                    paragraph {
                        text("This paragraph has ")
                        bold("bold")
                        text(", ")
                        italic("italic")
                        text(", and ")
                        code("code")
                        text(" formatting. ")
                        link("https://example.com", "Click here")
                        text(" for more info.")
                    }

                    callout("⚠️") {
                        colored("Warning: ", Color.ORANGE)
                        text("Please review by ")
                        dateMention(LocalDate(2025, 10, 20))
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                children.any { it is Block.Callout } shouldBe true
                println("✅ Example 10 passed")
            }

            // ========================================
            // Example 11: Mixed Content
            // ========================================
            "Example 11: Mixed content with all features" {
                println("\n📖 Running Example 11: Mixed content")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 11: Mixed Content")

                    paragraph {
                        text("Team meeting ")
                        dateMention(
                            start = LocalDateTime(2025, 10, 15, 14, 0),
                            timeZone = TimeZone.of("America/New_York"),
                        )
                        text(". ")

                        bold("Key topics:")
                        text(" ")
                        colored("Budget review", Color.RED)
                        text(", ")
                        colored("Timeline updates", Color.ORANGE)
                        text(", and ")
                        colored("Next steps", Color.GREEN)
                        text(". ")
                    }

                    paragraph {
                        text("Formula: ")
                        equation("\\text{ROI} = \\frac{\\text{Gain} - \\text{Cost}}{\\text{Cost}} \\times 100\\%")
                        text(" ")
                        link("https://example.com/docs", "Full documentation")
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 11 passed")
            }

            // ========================================
            // Example 12: Method Chaining
            // ========================================
            "Example 12: Method chaining" {
                println("\n📖 Running Example 12: Method chaining")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 12: Method Chaining")

                    paragraph {
                        text("Start ")
                            .bold("chain")
                            .text(" middle ")
                            .italic("more")
                            .text(" end")
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 12 passed")
            }

            // ========================================
            // Example 13: Convenience Methods
            // ========================================
            "Example 13: Convenience methods" {
                println("\n📖 Running Example 13: Convenience methods")
                testPageId.shouldNotBeNull()

                notion.blocks.appendChildren(testPageId) {
                    heading2("Example 13: Convenience Methods")

                    paragraph {
                        boldItalic("Bold and italic together")
                        text(" ")
                        strikethrough("strikethrough")
                        text(" ")
                        underline("underline")
                    }
                }

                delay(1000)
                val children = notion.blocks.retrieveChildren(testPageId)
                children.shouldNotBeEmpty()
                println("✅ Example 13 passed")
            }
        }
    })
