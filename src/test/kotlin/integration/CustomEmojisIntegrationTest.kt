package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.CustomEmojiObject
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import kotlinx.coroutines.delay

/**
 * Integration tests for custom emoji listing and usage as icons.
 *
 * - Lists all custom emojis in the workspace and prints the count.
 * - If any exist, picks the first and uses it as a page icon, callout icon,
 *   and tab pane icon — all on a single test page under a container page.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Run with: ./gradlew integrationTest --tests "*CustomEmojisIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class CustomEmojisIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) custom emoji integration" {
                println("Skipping CustomEmojisIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Custom Emoji Integration Test")
                        icon.emoji("🎭")
                    }
                containerPageId = container.id
                println("Container: https://notion.so/${container.id.replace("-", "")}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("Cleaned up container page (all children trashed)")
                } else {
                    println("Cleanup skipped — container page: $containerPageId")
                }
            }

            "should list custom emojis and use one as page icon, callout icon, and tab pane icon" {
                val result = notion.customEmojis.list()
                println("Custom emojis in workspace: ${result.results.size}")
                result.results.forEach { println("  - ${it.name} (${it.id})") }

                if (result.results.isEmpty()) {
                    println("No custom emojis found — skipping usage assertions")
                } else {
                    val emoji: CustomEmojiObject = result.results.first()
                    val iconValue = Icon.CustomEmoji(customEmoji = emoji)

                    // Create one page that exercises all three icon contexts
                    val page =
                        notion.pages.create(
                            createPageRequest {
                                parent.page(containerPageId)
                                title("Custom emoji: ${emoji.name}")
                            }.copy(icon = iconValue),
                        )

                    println("Created page: https://notion.so/${page.id.replace("-", "")}")

                    // Verify page icon round-trips correctly
                    val pageIcon = page.icon.shouldBeInstanceOf<Icon.CustomEmoji>()
                    pageIcon.customEmoji.id shouldBe emoji.id

                    // Add callout + tab blocks
                    notion.blocks.appendChildren(page.id) {
                        callout(
                            icon = iconValue,
                            richText = listOf(RequestBuilders.createSimpleRichText("Callout with custom emoji icon")),
                        )
                        tab {
                            pane("No icon") { paragraph("Plain pane") }
                            pane("Custom emoji", icon = iconValue) { paragraph("Pane with custom emoji icon") }
                        }
                    }

                    delay(500)

                    val blocks = notion.blocks.retrieveChildren(page.id)

                    // Verify callout icon
                    val callout =
                        blocks
                            .filterIsInstance<Block.Callout>()
                            .firstOrNull()
                            .shouldNotBeNull()
                    val calloutIcon = callout.callout.icon.shouldBeInstanceOf<Icon.CustomEmoji>()
                    calloutIcon.customEmoji.id shouldBe emoji.id

                    // Verify tab pane icon
                    val tabBlock =
                        blocks
                            .filterIsInstance<Block.Tab>()
                            .firstOrNull()
                            .shouldNotBeNull()

                    val panes = notion.blocks.retrieveChildren(tabBlock.id).filterIsInstance<Block.Paragraph>()
                    panes.size shouldBe 2
                    panes[0].paragraph.icon shouldBe null
                    val paneIcon = panes[1].paragraph.icon.shouldBeInstanceOf<Icon.CustomEmoji>()
                    paneIcon.customEmoji.id shouldBe emoji.id
                } // end if (result.results.isNotEmpty())
            }
        }
    })
