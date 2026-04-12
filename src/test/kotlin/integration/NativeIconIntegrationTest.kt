package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.NativeIconColor
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.nativeIcon
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import kotlinx.coroutines.delay

/**
 * Integration tests for native Notion icons (`type: "icon"`).
 *
 * Covers all contexts where icons can appear:
 * - Page icon (create + update)
 * - Callout block icon
 * - Tab pane icon
 *
 * Tests each supported color variant and verifies round-trip deserialization
 * from the live API response.
 *
 * All artifacts are created under a single container page and cleaned up
 * together via afterSpec — trashing the container cascades to all children.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Run with: ./gradlew integrationTest --tests "*NativeIconIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class NativeIconIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) native icon integration" {
                println("Skipping NativeIconIntegrationTest — set required env vars")
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
                        title("Native Icon Integration Test")
                        icon.native("pizza", NativeIconColor.GRAY)
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

            // ---------------------------------------------------------------
            // Test 1: Page icon — create with a native icon
            // ---------------------------------------------------------------
            "should create a subpage with a native icon and read it back" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Page Icon — blue pizza")
                        icon.native("pizza", NativeIconColor.BLUE)
                    }

                val native = page.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                native.icon.color shouldBe NativeIconColor.BLUE
                native.type shouldBe "icon"
            }

            // ---------------------------------------------------------------
            // Test 2: Page icon — update from emoji to native icon
            // ---------------------------------------------------------------
            "should update a subpage icon from emoji to native icon" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Page Icon — update test")
                        icon.emoji("🍕")
                    }

                (page.icon as? Icon.Emoji)?.emoji shouldBe "🍕"

                delay(300)

                val updated =
                    notion.pages.update(page.id) {
                        icon.native("star", NativeIconColor.YELLOW)
                    }

                val native = updated.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "star"
                native.icon.color shouldBe NativeIconColor.YELLOW
            }

            // ---------------------------------------------------------------
            // Test 3: Page icon — all supported colors round-trip
            // ---------------------------------------------------------------
            "should create subpages with native icons in every supported color" {
                for (color in NativeIconColor.entries) {
                    val page =
                        notion.pages.create {
                            parent.page(containerPageId)
                            title("Color — $color")
                            icon.native("circle", color)
                        }

                    val native = page.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                    native.icon.name shouldBe "circle"
                    native.icon.color shouldBe color

                    println("  $color ✓")
                }
            }

            // ---------------------------------------------------------------
            // Test 4: Native icon without color (defaults to gray server-side)
            // ---------------------------------------------------------------
            "should create a subpage with a native icon and no explicit color" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Page Icon — no color")
                        icon.native("pizza")
                    }

                val native = page.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                // color may be null or "gray" depending on what Notion echoes back — both are acceptable
                println("  Echoed color: ${native.icon.color}")
            }

            // ---------------------------------------------------------------
            // Test 5: Callout block with a native icon
            // ---------------------------------------------------------------
            "should create callout blocks with native icons and read them back" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Callout blocks")
                        content {
                            // Emoji callout for contrast
                            callout("💡", "Emoji callout for comparison")

                            // Native icon callouts
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
            }

            // ---------------------------------------------------------------
            // Test 6: Tab pane with a native icon
            // ---------------------------------------------------------------
            "should create a tab block with native icon panes and read them back" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Tab panes")
                        content {
                            tab {
                                pane("Overview") {
                                    paragraph("Plain pane, no icon")
                                }
                                pane("Tasks", icon = nativeIcon("checklist", NativeIconColor.BLUE)) {
                                    paragraph("Pane with native blue checklist icon")
                                }
                                pane("Notes", icon = nativeIcon("pencil", NativeIconColor.RED)) {
                                    paragraph("Pane with native red pencil icon")
                                }
                            }
                        }
                    }

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
            }
        }
    })
