package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Color
import it.saabel.kotlinnotionclient.models.blocks.Block
import kotlinx.coroutines.delay

/**
 * Integration test for heading_4 block type.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 *
 * Run with: ./gradlew integrationTest --tests "*Heading4IntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class Heading4IntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) heading_4 integration" {
                println("Skipping Heading4IntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            "should create a page with heading_4 blocks and read them back" {
                val page =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Heading4 Integration Test")

                        content {
                            // Plain string overload
                            heading4("Simple H4 heading")
                            // Text color — API accepts and stores it; Notion does not currently render
                            // text color visually on heading_4 blocks (background color does render)
                            heading4("Red text H4", color = Color.RED)
                            // Background color
                            heading4("Blue background H4", color = Color.BLUE_BACKGROUND)
                            // is_toggleable flag
                            heading4("Toggleable H4", isToggleable = true)
                            // Rich text DSL overload — mixed italic + plain text
                            heading4 {
                                italic("Italic")
                                text(" and plain")
                            }
                        }
                    }

                println("Created page: https://notion.so/${page.id.replace("-", "")}")
                page.inTrash shouldBe false

                delay(500)

                val blocks = notion.blocks.retrieveChildren(page.id)
                val heading4Blocks = blocks.filterIsInstance<Block.Heading4>()

                heading4Blocks.size shouldBe 5

                val simple = heading4Blocks[0]
                simple.type shouldBe "heading_4"
                simple.heading4.richText
                    .first()
                    .plainText shouldBe "Simple H4 heading"
                simple.heading4.color shouldBe Color.DEFAULT
                simple.heading4.isToggleable shouldBe false

                val redText = heading4Blocks[1]
                redText.heading4.color shouldBe Color.RED

                val blueBackground = heading4Blocks[2]
                blueBackground.heading4.color shouldBe Color.BLUE_BACKGROUND

                val toggleable = heading4Blocks[3]
                toggleable.heading4.isToggleable shouldBe true

                val richText = heading4Blocks[4]
                richText.heading4.richText.size shouldBe 2
                richText.heading4.richText[0].plainText shouldBe "Italic"
                richText.heading4.richText[0]
                    .annotations.italic shouldBe true
                richText.heading4.richText[1].plainText shouldBe " and plain"
                richText.heading4.richText[1]
                    .annotations.italic shouldBe false

                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(page.id)
                    println("Cleaned up test page")
                }
            }
        }
    })
