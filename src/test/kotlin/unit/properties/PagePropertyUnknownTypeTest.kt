package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import kotlinx.serialization.json.Json

/**
 * Unit tests for handling unknown/unsupported page property types.
 *
 * This test ensures that when Notion adds new property types (like "button",
 * "verification", etc.), the client gracefully handles them as
 * [PageProperty.Unknown] instead of failing deserialization entirely.
 *
 * This is critical for forward compatibility and production use.
 */
@Tags("Unit")
class PagePropertyUnknownTypeTest :
    StringSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
            }

        "Should deserialize button property as Unknown type" {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Action": {
                      "id": "button-prop-id",
                      "type": "button",
                      "button": {}
                    },
                    "Title": {
                      "id": "title-prop-id",
                      "type": "title",
                      "title": [
                        {
                          "type": "text",
                          "text": {
                            "content": "Test Page"
                          },
                          "plain_text": "Test Page",
                          "href": null,
                          "annotations": {
                            "bold": false,
                            "italic": false,
                            "strikethrough": false,
                            "underline": false,
                            "code": false,
                            "color": "default"
                          }
                        }
                      ]
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"
            page.properties.size shouldBe 2

            // Verify button property was handled as Unknown
            val buttonProperty = page.properties["Action"]
            buttonProperty shouldNotBe null
            buttonProperty.shouldBeInstanceOf<PageProperty.Unknown>()
            buttonProperty!!.type shouldBe "button"
            buttonProperty.id shouldBe "button-prop-id"

            // Verify title property still works normally
            val titleProperty = page.properties["Title"]
            titleProperty shouldNotBe null
            titleProperty.shouldBeInstanceOf<PageProperty.Title>()
        }

        "Should deserialize page with multiple unknown property types" {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Button": {
                      "id": "button-id",
                      "type": "button",
                      "button": {}
                    },
                    "Verification": {
                      "id": "verification-id",
                      "type": "verification",
                      "verification": null
                    },
                    "Price": {
                      "id": "price-id",
                      "type": "number",
                      "number": 42.0
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully with mixed property types
            page.id shouldBe "test-page-id"
            page.properties.size shouldBe 3

            // Verify unknown types
            page.properties["Button"]!!.shouldBeInstanceOf<PageProperty.Unknown>()
            page.properties["Verification"]!!.shouldBeInstanceOf<PageProperty.Unknown>()

            // Verify known type still works
            val priceProperty = page.properties["Price"]
            priceProperty.shouldBeInstanceOf<PageProperty.Number>()
            (priceProperty as PageProperty.Number).number shouldBe 42.0
        }

        "Should preserve raw JSON in Unknown property for inspection" {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Action": {
                      "id": "button-prop-id",
                      "type": "button",
                      "button": {}
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)
            val buttonProperty = page.properties["Action"] as PageProperty.Unknown

            // Verify raw JSON is available
            buttonProperty.rawContent shouldNotBe null

            // Verify we can access basic information
            buttonProperty.id shouldBe "button-prop-id"
            buttonProperty.type shouldBe "button"
        }

        "Should handle all supported property types correctly" {
            // Test that all explicitly supported types still work
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Title": {
                      "id": "title-id",
                      "type": "title",
                      "title": []
                    },
                    "Number": {
                      "id": "number-id",
                      "type": "number",
                      "number": null
                    },
                    "Checkbox": {
                      "id": "checkbox-id",
                      "type": "checkbox",
                      "checkbox": false
                    },
                    "Select": {
                      "id": "select-id",
                      "type": "select",
                      "select": null
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify all known types are deserialized correctly
            page.properties["Title"]!!.shouldBeInstanceOf<PageProperty.Title>()
            page.properties["Number"]!!.shouldBeInstanceOf<PageProperty.Number>()
            page.properties["Checkbox"]!!.shouldBeInstanceOf<PageProperty.Checkbox>()
            page.properties["Select"]!!.shouldBeInstanceOf<PageProperty.Select>()
        }
    })
