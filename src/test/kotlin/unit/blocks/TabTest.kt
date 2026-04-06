package unit.blocks

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.base.Color
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.CalloutIcon
import it.saabel.kotlinnotionclient.models.blocks.ParagraphContent
import it.saabel.kotlinnotionclient.models.blocks.ParagraphRequestContent
import it.saabel.kotlinnotionclient.models.blocks.TabContent
import it.saabel.kotlinnotionclient.models.blocks.TabRequestContent
import it.saabel.kotlinnotionclient.models.blocks.emoji
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Tags("Unit")
class TabTest :
    FunSpec({
        val json = Json { ignoreUnknownKeys = true }

        context("TabContent deserialization") {
            test("deserializes empty tab object") {
                val content = json.decodeFromString<TabContent>("{}")
                content.shouldNotBeNull()
            }
        }

        context("ParagraphContent icon field") {
            test("deserializes paragraph with emoji icon") {
                val raw =
                    """
                    {
                      "rich_text": [
                        {
                          "type": "text",
                          "text": { "content": "Tab label", "link": null },
                          "annotations": {
                            "bold": false, "italic": false, "strikethrough": false,
                            "underline": false, "code": false, "color": "default"
                          },
                          "plain_text": "Tab label",
                          "href": null
                        }
                      ],
                      "color": "default",
                      "icon": { "type": "emoji", "emoji": "📋" }
                    }
                    """.trimIndent()

                val content = json.decodeFromString<ParagraphContent>(raw)

                content.richText.first().plainText shouldBe "Tab label"
                val icon = content.icon.shouldNotBeNull()
                icon.type shouldBe "emoji"
                icon.emoji shouldBe "📋"
            }

            test("deserializes paragraph without icon (icon is null)") {
                val raw =
                    """
                    {
                      "rich_text": [],
                      "color": "default"
                    }
                    """.trimIndent()

                val content = json.decodeFromString<ParagraphContent>(raw)
                content.icon.shouldBeNull()
            }
        }

        context("ParagraphRequestContent icon field") {
            test("serializes paragraph request with emoji icon") {
                val request =
                    ParagraphRequestContent(
                        richText = emptyList(),
                        icon = CalloutIcon(type = "emoji", emoji = "📋"),
                    )

                val encoded = json.encodeToString(ParagraphRequestContent.serializer(), request)
                val parsed = Json.parseToJsonElement(encoded).jsonObject

                parsed["icon"]
                    ?.jsonObject
                    ?.get("emoji")
                    ?.jsonPrimitive
                    ?.content shouldBe "📋"
                parsed["icon"]
                    ?.jsonObject
                    ?.get("type")
                    ?.jsonPrimitive
                    ?.content shouldBe "emoji"
            }

            test("serializes paragraph request without icon (icon omitted)") {
                val request = ParagraphRequestContent(richText = emptyList())

                val encoded = json.encodeToString(ParagraphRequestContent.serializer(), request)
                val parsed = Json.parseToJsonElement(encoded).jsonObject

                // icon key should be absent when null
                parsed.containsKey("icon") shouldBe false
            }
        }

        context("Block.Tab deserialization") {
            test("deserializes a full tab block from JSON") {
                val raw =
                    """
                    {
                      "object": "block",
                      "id": "aaaa0000-0000-0000-0000-000000000001",
                      "parent": { "type": "page_id", "page_id": "bbbb0000-0000-0000-0000-000000000002" },
                      "created_time": "2026-04-06T00:00:00.000Z",
                      "last_edited_time": "2026-04-06T00:00:00.000Z",
                      "has_children": true,
                      "in_trash": false,
                      "type": "tab",
                      "tab": {}
                    }
                    """.trimIndent()

                val block = json.decodeFromString<Block>(raw)

                val tab = block.shouldBeInstanceOf<Block.Tab>()
                tab.type shouldBe "tab"
                tab.id shouldBe "aaaa0000-0000-0000-0000-000000000001"
                tab.inTrash shouldBe false
                tab.hasChildren shouldBe true
                tab.tab.shouldNotBeNull()
            }
        }

        context("BlockRequest.Tab serialization") {
            test("serializes to correct JSON structure with children") {
                val request =
                    BlockRequest.Tab(
                        tab = TabRequestContent(children = emptyList()),
                    )

                val encoded = json.encodeToString(BlockRequest.serializer(), request)
                val parsed = Json.parseToJsonElement(encoded).jsonObject

                parsed["type"]?.jsonPrimitive?.content shouldBe "tab"
                parsed.containsKey("tab") shouldBe true
            }

            test("serializes children correctly") {
                val paragraphChild =
                    BlockRequest.Paragraph(
                        paragraph =
                            ParagraphRequestContent(
                                richText = emptyList(),
                                icon = CalloutIcon(type = "emoji", emoji = "📋"),
                            ),
                    )
                val request =
                    BlockRequest.Tab(
                        tab = TabRequestContent(children = listOf(paragraphChild)),
                    )

                val encoded = json.encodeToString(BlockRequest.serializer(), request)
                val parsed = Json.parseToJsonElement(encoded).jsonObject

                val tabObject = parsed["tab"]?.jsonObject.shouldNotBeNull()
                val children = tabObject["children"]?.jsonArray.shouldNotBeNull()
                children.size shouldBe 1
                children[0]
                    .jsonObject["type"]
                    ?.jsonPrimitive
                    ?.content shouldBe "paragraph"
            }
        }

        context("TabBuilder DSL") {
            test("tab with single pane (string overload) produces correct structure") {
                val blocks =
                    pageContent {
                        tab {
                            pane("Overview") {
                                paragraph("Content 1")
                            }
                        }
                    }

                blocks.size shouldBe 1
                val tab = blocks.first().shouldBeInstanceOf<BlockRequest.Tab>()
                val panes = tab.tab.children.shouldNotBeNull()
                panes.size shouldBe 1
                val pane = panes.first().shouldBeInstanceOf<BlockRequest.Paragraph>()
                pane.paragraph.richText
                    .first()
                    .plainText shouldBe "Overview"
                pane.paragraph.icon.shouldBeNull()
            }

            test("tab pane with emoji icon") {
                val blocks =
                    pageContent {
                        tab {
                            pane("Overview", icon = emoji("📋")) {
                                paragraph("Content")
                            }
                        }
                    }

                val tab = blocks.first().shouldBeInstanceOf<BlockRequest.Tab>()
                val pane =
                    tab.tab.children!!
                        .first()
                        .shouldBeInstanceOf<BlockRequest.Paragraph>()
                val icon = pane.paragraph.icon.shouldNotBeNull()
                icon.emoji shouldBe "📋"
                icon.type shouldBe "emoji"
            }

            test("tab with multiple panes") {
                val blocks =
                    pageContent {
                        tab {
                            pane("Tab 1") { paragraph("Content 1") }
                            pane("Tab 2") { paragraph("Content 2") }
                            pane("Tab 3") { paragraph("Content 3") }
                        }
                    }

                val tab = blocks.first().shouldBeInstanceOf<BlockRequest.Tab>()
                tab.tab.children!!.size shouldBe 3
            }

            test("pane with rich text overload") {
                val blocks =
                    pageContent {
                        tab {
                            pane(
                                richText =
                                    listOf(
                                        it.saabel.kotlinnotionclient.models.requests.RequestBuilders
                                            .createSimpleRichText("Rich pane"),
                                    ),
                                icon = emoji("🔖"),
                            ) {
                                paragraph("Content")
                            }
                        }
                    }

                val tab = blocks.first().shouldBeInstanceOf<BlockRequest.Tab>()
                val pane =
                    tab.tab.children!!
                        .first()
                        .shouldBeInstanceOf<BlockRequest.Paragraph>()
                pane.paragraph.richText
                    .first()
                    .plainText shouldBe "Rich pane"
                pane.paragraph.icon?.emoji shouldBe "🔖"
            }

            test("pane with rich text DSL overload") {
                val blocks =
                    pageContent {
                        tab {
                            pane(icon = emoji("⚙️"), block = { text("DSL pane") }) {
                                paragraph("Content")
                            }
                        }
                    }

                val tab = blocks.first().shouldBeInstanceOf<BlockRequest.Tab>()
                val pane =
                    tab.tab.children!!
                        .first()
                        .shouldBeInstanceOf<BlockRequest.Paragraph>()
                pane.paragraph.richText
                    .first()
                    .plainText shouldBe "DSL pane"
                pane.paragraph.icon?.emoji shouldBe "⚙️"
            }

            test("pane nested content is placed as children of the paragraph") {
                val blocks =
                    pageContent {
                        tab {
                            pane("Pane") {
                                paragraph("Child paragraph")
                                heading4("Child heading")
                            }
                        }
                    }

                val tab = blocks.first().shouldBeInstanceOf<BlockRequest.Tab>()
                val pane =
                    tab.tab.children!!
                        .first()
                        .shouldBeInstanceOf<BlockRequest.Paragraph>()
                val children = pane.paragraph.children.shouldNotBeNull()
                children.size shouldBe 2
                children[0].shouldBeInstanceOf<BlockRequest.Paragraph>()
                children[1].shouldBeInstanceOf<BlockRequest.Heading4>()
            }

            test("validation fails for tab with no panes") {
                val blocks =
                    pageContent {
                        tab {}
                    }

                val builder =
                    it.saabel.kotlinnotionclient.models.blocks
                        .PageContentBuilder()
                builder.tab {}
                val errors = builder.validate()
                errors.any { it.contains("Tab blocks must have at least one pane") } shouldBe true
            }

            test("emoji() helper creates correct CalloutIcon") {
                val icon = emoji("🎯")
                icon.type shouldBe "emoji"
                icon.emoji shouldBe "🎯"
            }
        }
    })
