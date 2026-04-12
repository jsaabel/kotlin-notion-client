package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.emoji
import kotlinx.coroutines.delay

/**
 * Integration test for the `tab` block type.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 *
 * Run with: ./gradlew integrationTest --tests "*TabIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class TabIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) tab integration" {
                println("Skipping TabIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            "should create a page with a tab block and read it back" {
                val page =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Tab Block Integration Test")

                        content {
                            tab {
                                // Pane without icon
                                pane("Overview") {
                                    paragraph("Overview content")
                                }
                                // Pane with emoji icon
                                pane("Details", icon = emoji("📋")) {
                                    paragraph("Details content")
                                    heading4("A sub-section")
                                }
                                // Pane with icon and plain string label
                                pane("Settings", icon = emoji("⚙️")) {
                                    paragraph("Settings content")
                                }
                            }
                        }
                    }

                println("Created page: https://notion.so/${page.id.replace("-", "")}")
                page.inTrash shouldBe false

                delay(500)

                // The tab block itself is a child of the page
                val pageBlocks = notion.blocks.retrieveChildren(page.id)
                val tabBlock =
                    pageBlocks
                        .filterIsInstance<Block.Tab>()
                        .firstOrNull()
                        .shouldNotBeNull()

                tabBlock.type shouldBe "tab"
                tabBlock.hasChildren shouldBe true

                // The panes are children of the tab block (paragraph blocks)
                val panes = notion.blocks.retrieveChildren(tabBlock.id)
                val paneParagraphs = panes.filterIsInstance<Block.Paragraph>()
                paneParagraphs.size shouldBe 3

                // Pane 1 — no icon
                val pane1 = paneParagraphs[0]
                pane1.paragraph.richText
                    .first()
                    .plainText shouldBe "Overview"
                pane1.paragraph.icon shouldBe null

                // Pane 2 — emoji icon
                val pane2 = paneParagraphs[1]
                pane2.paragraph.richText
                    .first()
                    .plainText shouldBe "Details"
                val pane2Icon = pane2.paragraph.icon.shouldNotBeNull()
                pane2Icon.type shouldBe "emoji"
                (pane2Icon as? Icon.Emoji)?.emoji shouldBe "📋"

                // Pane 3 — plain string label + emoji icon
                val pane3 = paneParagraphs[2]
                pane3.paragraph.richText
                    .first()
                    .plainText shouldBe "Settings"
                val pane3Icon = pane3.paragraph.icon.shouldNotBeNull()
                (pane3Icon as? Icon.Emoji)?.emoji shouldBe "⚙️"

                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(page.id)
                    println("Cleaned up test page")
                }
            }
        }
    })
