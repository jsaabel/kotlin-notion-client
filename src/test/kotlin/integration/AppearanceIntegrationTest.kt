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
import it.saabel.kotlinnotionclient.models.base.NativeIconColor
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.nativeIcon
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import kotlinx.coroutines.delay

/**
 * Integration tests for page/block appearance — native Notion icons and custom workspace emojis.
 *
 * Covers:
 * - Native icons: create page icon, update emoji → native, all color variants, no-color fallback
 * - Native icons in callout blocks and tab pane blocks
 * - Custom workspace emojis: list, then use as page icon, callout icon, and tab pane icon
 *
 * All sub-pages are created under a single container page. Trashing the container
 * (NOTION_CLEANUP_AFTER_TEST=true) cascades to all children.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Run with: ./gradlew integrationTest --tests "*AppearanceIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class AppearanceIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) appearance integration" {
                println("Skipping AppearanceIntegrationTest — set required env vars")
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
                        title("Appearance — Integration Tests")
                        icon.emoji("🎨")
                        content {
                            callout(
                                "ℹ️",
                                "Covers native Notion icons (all color variants, page icon, callout icon, tab pane icon) " +
                                    "and custom workspace emoji icons (page icon, callout icon, tab pane icon). " +
                                    "Each sub-page demonstrates a specific icon context or variant.",
                            )
                        }
                    }
                containerPageId = container.id
                println("📄 Container: ${container.url}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("✅ Cleaned up container page (all children trashed)")
                } else {
                    println("🔧 Cleanup skipped — container page preserved for inspection")
                }
                notion.close()
            }

            // ------------------------------------------------------------------
            // 1. Native icon — page icon, create and read back
            // ------------------------------------------------------------------
            "should create a page with a native icon and read it back" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Native Icon — page icon (blue pizza)")
                        icon.native("pizza", NativeIconColor.BLUE)
                    }
                println("  Page icon: ${page.url}")

                val native = page.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                native.icon.color shouldBe NativeIconColor.BLUE
                native.type shouldBe "icon"

                println("  ✅ Native page icon round-trip verified")
            }

            // ------------------------------------------------------------------
            // 2. Native icon — update from emoji to native
            // ------------------------------------------------------------------
            "should update a page icon from emoji to native icon" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Native Icon — emoji → native update")
                        icon.emoji("🍕")
                    }
                println("  Update test: ${page.url}")

                (page.icon as? Icon.Emoji)?.emoji shouldBe "🍕"
                delay(300)

                val updated =
                    notion.pages.update(page.id) {
                        icon.native("star", NativeIconColor.YELLOW)
                    }

                val native = updated.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "star"
                native.icon.color shouldBe NativeIconColor.YELLOW

                println("  ✅ Emoji → native icon update verified")
            }

            // ------------------------------------------------------------------
            // 3. Native icon — all supported colors, round-trip
            // ------------------------------------------------------------------
            "should round-trip native icons in every supported color" {
                for (color in NativeIconColor.entries) {
                    val page =
                        notion.pages.create {
                            parent.page(containerPageId)
                            title("Native Icon — $color")
                            icon.native("circle", color)
                        }

                    val native = page.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                    native.icon.name shouldBe "circle"
                    native.icon.color shouldBe color

                    println("  $color ✓ ${page.url}")
                }
            }

            // ------------------------------------------------------------------
            // 4. Native icon — no explicit color (defaults server-side)
            // ------------------------------------------------------------------
            "should create a page with a native icon and no explicit color" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Native Icon — no explicit color")
                        icon.native("pizza")
                    }
                println("  No-color icon: ${page.url}")

                val native = page.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                // color may be null or "gray" depending on what Notion echoes back — both are acceptable
                println("  Echoed color: ${native.icon.color} ✓")
            }

            // ------------------------------------------------------------------
            // 5. Native icon — callout block icons
            // ------------------------------------------------------------------
            "should create callout blocks with native icons and read them back" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Native Icon — callout block icons")
                        icon.emoji("📣")
                        content {
                            callout("💡", "Emoji callout for comparison")
                            callout(
                                icon = nativeIcon("pizza", NativeIconColor.ORANGE),
                                richText = listOf(RequestBuilders.createSimpleRichText("Native icon callout — orange pizza")),
                            )
                            callout(
                                icon = nativeIcon("star", NativeIconColor.PURPLE),
                                richText = listOf(RequestBuilders.createSimpleRichText("Native icon callout — purple star")),
                            )
                        }
                    }
                println("  Callout icons: ${page.url}")
                delay(500)

                val callouts = notion.blocks.retrieveChildren(page.id).filterIsInstance<Block.Callout>()
                callouts.size shouldBe 3

                callouts[0].callout.icon.shouldBeInstanceOf<Icon.Emoji>()

                val n1 = callouts[1].callout.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                n1.icon.name shouldBe "pizza"
                n1.icon.color shouldBe NativeIconColor.ORANGE

                val n2 = callouts[2].callout.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                n2.icon.name shouldBe "star"
                n2.icon.color shouldBe NativeIconColor.PURPLE

                println("  ✅ Native callout icons verified")
            }

            // ------------------------------------------------------------------
            // 6. Native icon — tab pane icons
            // ------------------------------------------------------------------
            "should create a tab block with native icon panes and read them back" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Native Icon — tab pane icons")
                        icon.emoji("📑")
                        content {
                            tab {
                                pane("Overview") { paragraph("Plain pane, no icon") }
                                pane("Tasks", icon = nativeIcon("checklist", NativeIconColor.BLUE)) {
                                    paragraph("Pane with native blue checklist icon")
                                }
                                pane("Notes", icon = nativeIcon("pencil", NativeIconColor.RED)) {
                                    paragraph("Pane with native red pencil icon")
                                }
                            }
                        }
                    }
                println("  Tab panes: ${page.url}")
                delay(500)

                val tabBlock =
                    notion.blocks
                        .retrieveChildren(page.id)
                        .filterIsInstance<Block.Tab>()
                        .firstOrNull()
                        .shouldNotBeNull()

                val panes = notion.blocks.retrieveChildren(tabBlock.id).filterIsInstance<Block.Paragraph>()
                panes.size shouldBe 3

                panes[0].paragraph.icon shouldBe null

                val p1Icon = panes[1].paragraph.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                p1Icon.icon.name shouldBe "checklist"
                p1Icon.icon.color shouldBe NativeIconColor.BLUE

                val p2Icon = panes[2].paragraph.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                p2Icon.icon.name shouldBe "pencil"
                p2Icon.icon.color shouldBe NativeIconColor.RED

                println("  ✅ Native tab pane icons verified")
            }

            // ------------------------------------------------------------------
            // 7. Custom emoji — list workspace emojis and use one in all icon contexts
            // ------------------------------------------------------------------
            "should list custom emojis and use one as page icon, callout icon, and tab pane icon" {
                val result = notion.customEmojis.list()
                println("  Custom emojis in workspace: ${result.results.size}")
                result.results.forEach { println("    - ${it.name} (${it.id})") }

                if (result.results.isEmpty()) {
                    println("  No custom emojis found — skipping usage assertions")
                } else {
                    val emoji: CustomEmojiObject = result.results.first()
                    val iconValue = Icon.CustomEmoji(customEmoji = emoji)

                    val page =
                        notion.pages.create(
                            createPageRequest {
                                parent.page(containerPageId)
                                title("Custom Emoji — ${emoji.name}")
                            }.copy(icon = iconValue),
                        )
                    println("  Custom emoji page: ${page.url}")

                    val pageIcon = page.icon.shouldBeInstanceOf<Icon.CustomEmoji>()
                    pageIcon.customEmoji.id shouldBe emoji.id

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

                    val callout =
                        blocks
                            .filterIsInstance<Block.Callout>()
                            .firstOrNull()
                            .shouldNotBeNull()
                    val calloutIcon = callout.callout.icon.shouldBeInstanceOf<Icon.CustomEmoji>()
                    calloutIcon.customEmoji.id shouldBe emoji.id

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

                    println("  ✅ Custom emoji icons verified in page, callout, and tab pane")
                }
            }
        }
    })
