package unit.blocks

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.base.Color
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.Heading4Content
import it.saabel.kotlinnotionclient.models.blocks.Heading4RequestContent
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Tags("Unit")
class Heading4Test :
    FunSpec({
        val json = Json { ignoreUnknownKeys = true }

        context("Heading4Content deserialization") {
            test("deserializes heading_4 JSON correctly") {
                val raw =
                    """
                    {
                      "rich_text": [
                        {
                          "type": "text",
                          "text": { "content": "My H4 heading", "link": null },
                          "annotations": {
                            "bold": false, "italic": false, "strikethrough": false,
                            "underline": false, "code": false, "color": "default"
                          },
                          "plain_text": "My H4 heading",
                          "href": null
                        }
                      ],
                      "color": "default",
                      "is_toggleable": false
                    }
                    """.trimIndent()

                val content = json.decodeFromString<Heading4Content>(raw)

                content.richText.first().plainText shouldBe "My H4 heading"
                content.color shouldBe Color.DEFAULT
                content.isToggleable shouldBe false
            }
        }

        context("Block.Heading4 deserialization") {
            test("deserializes a full heading_4 block from JSON") {
                val raw =
                    """
                    {
                      "object": "block",
                      "id": "aaaa0000-0000-0000-0000-000000000001",
                      "parent": { "type": "page_id", "page_id": "bbbb0000-0000-0000-0000-000000000002" },
                      "created_time": "2026-04-06T00:00:00.000Z",
                      "last_edited_time": "2026-04-06T00:00:00.000Z",
                      "has_children": false,
                      "in_trash": false,
                      "type": "heading_4",
                      "heading_4": {
                        "rich_text": [
                          {
                            "type": "text",
                            "text": { "content": "H4 title", "link": null },
                            "annotations": {
                              "bold": false, "italic": false, "strikethrough": false,
                              "underline": false, "code": false, "color": "default"
                            },
                            "plain_text": "H4 title",
                            "href": null
                          }
                        ],
                        "color": "default",
                        "is_toggleable": false
                      }
                    }
                    """.trimIndent()

                val block = json.decodeFromString<Block>(raw)

                val h4 = block.shouldBeInstanceOf<Block.Heading4>()
                h4.type shouldBe "heading_4"
                h4.id shouldBe "aaaa0000-0000-0000-0000-000000000001"
                h4.inTrash shouldBe false
                h4.hasChildren shouldBe false
                h4.heading4.richText
                    .first()
                    .plainText shouldBe "H4 title"
                h4.heading4.color shouldBe Color.DEFAULT
                h4.heading4.isToggleable shouldBe false
            }
        }

        context("BlockRequest.Heading4 serialization") {
            test("serializes to correct JSON structure") {
                val request =
                    BlockRequest.Heading4(
                        heading4 =
                            Heading4RequestContent(
                                richText = emptyList(),
                                color = Color.DEFAULT,
                                isToggleable = false,
                            ),
                    )

                val encoded = json.encodeToString(BlockRequest.serializer(), request)
                val parsed = Json.parseToJsonElement(encoded).jsonObject

                parsed["type"]?.jsonPrimitive?.content shouldBe "heading_4"
                parsed["heading_4"] shouldBe parsed["heading_4"] // key exists
            }

            test("serializes color correctly") {
                val request =
                    BlockRequest.Heading4(
                        heading4 =
                            Heading4RequestContent(
                                richText = emptyList(),
                                color = Color.BLUE,
                                isToggleable = false,
                            ),
                    )

                val encoded = json.encodeToString(BlockRequest.serializer(), request)
                val parsed = Json.parseToJsonElement(encoded).jsonObject
                parsed["heading_4"]
                    ?.jsonObject
                    ?.get("color")
                    ?.jsonPrimitive
                    ?.content shouldBe "blue"
            }

            test("serializes is_toggleable correctly") {
                val request =
                    BlockRequest.Heading4(
                        heading4 =
                            Heading4RequestContent(
                                richText = emptyList(),
                                color = Color.DEFAULT,
                                isToggleable = true,
                            ),
                    )

                val encoded = json.encodeToString(BlockRequest.serializer(), request)
                val parsed = Json.parseToJsonElement(encoded).jsonObject
                parsed["heading_4"]
                    ?.jsonObject
                    ?.get("is_toggleable")
                    ?.jsonPrimitive
                    ?.content shouldBe "true"
            }
        }

        context("PageContentBuilder heading4 overloads") {
            test("heading4(String) produces a Heading4 block request") {
                val blocks = pageContent { heading4("Hello H4") }

                blocks shouldBe
                    listOf(
                        BlockRequest.Heading4(
                            heading4 =
                                Heading4RequestContent(
                                    richText =
                                        blocks
                                            .filterIsInstance<BlockRequest.Heading4>()
                                            .first()
                                            .heading4.richText,
                                    color = Color.DEFAULT,
                                    isToggleable = false,
                                ),
                        ),
                    )
                val h4 = blocks.filterIsInstance<BlockRequest.Heading4>().first()
                h4.heading4.richText
                    .first()
                    .plainText shouldBe "Hello H4"
            }

            test("heading4(String) with color and isToggleable") {
                val blocks = pageContent { heading4("Toggle H4", color = Color.RED, isToggleable = true) }

                val h4 = blocks.filterIsInstance<BlockRequest.Heading4>().first()
                h4.heading4.color shouldBe Color.RED
                h4.heading4.isToggleable shouldBe true
            }

            test("heading4(richText DSL) produces correct block") {
                val blocks = pageContent { heading4 { text("DSL H4") } }

                val h4 = blocks.filterIsInstance<BlockRequest.Heading4>().first()
                h4.heading4.richText
                    .first()
                    .plainText shouldBe "DSL H4"
            }
        }
    })
