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
 * Unit tests for the unique_id page property type.
 *
 * Tests proper deserialization of unique_id properties with and without prefixes,
 * and validates the formattedId convenience property.
 */
@Tags("Unit")
class PagePropertyUniqueIdTest :
    StringSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
            }

        "Should deserialize unique_id property with prefix" {
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
                    "ID": {
                      "id": "unique-id-prop",
                      "type": "unique_id",
                      "unique_id": {
                        "prefix": "TEST",
                        "number": 123
                      }
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"
            page.properties.size shouldBe 1

            // Verify unique_id property
            val uniqueIdProperty = page.properties["ID"]
            uniqueIdProperty shouldNotBe null
            uniqueIdProperty.shouldBeInstanceOf<PageProperty.UniqueId>()
            uniqueIdProperty!!.type shouldBe "unique_id"
            uniqueIdProperty.id shouldBe "unique-id-prop"

            // Verify unique_id value
            val uniqueId = (uniqueIdProperty as PageProperty.UniqueId)
            uniqueId.uniqueId shouldNotBe null
            uniqueId.uniqueId!!.prefix shouldBe "TEST"
            uniqueId.uniqueId!!.number shouldBe 123

            // Verify formatted ID
            uniqueId.formattedId shouldBe "TEST-123"
        }

        "Should deserialize unique_id property without prefix (null)" {
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
                    "ID": {
                      "id": "unique-id-prop",
                      "type": "unique_id",
                      "unique_id": {
                        "prefix": null,
                        "number": 42
                      }
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"
            page.properties.size shouldBe 1

            // Verify unique_id property
            val uniqueIdProperty = page.properties["ID"]
            uniqueIdProperty shouldNotBe null
            uniqueIdProperty.shouldBeInstanceOf<PageProperty.UniqueId>()

            // Verify unique_id value
            val uniqueId = (uniqueIdProperty as PageProperty.UniqueId)
            uniqueId.uniqueId shouldNotBe null
            uniqueId.uniqueId!!.prefix shouldBe null
            uniqueId.uniqueId!!.number shouldBe 42

            // Verify formatted ID (no prefix, just number)
            uniqueId.formattedId shouldBe "42"
        }

        "Should deserialize unique_id alongside other property types" {
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
                      "title": [
                        {
                          "type": "text",
                          "text": {
                            "content": "Test Task"
                          },
                          "plain_text": "Test Task",
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
                    },
                    "TaskID": {
                      "id": "task-id-prop",
                      "type": "unique_id",
                      "unique_id": {
                        "prefix": "TASK",
                        "number": 456
                      }
                    },
                    "Status": {
                      "id": "status-id",
                      "type": "select",
                      "select": {
                        "id": "select-1",
                        "name": "In Progress",
                        "color": "blue"
                      }
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"
            page.properties.size shouldBe 3

            // Verify all property types
            page.properties["Title"]!!.shouldBeInstanceOf<PageProperty.Title>()
            page.properties["TaskID"]!!.shouldBeInstanceOf<PageProperty.UniqueId>()
            page.properties["Status"]!!.shouldBeInstanceOf<PageProperty.Select>()

            // Verify unique_id specific values
            val taskId = page.properties["TaskID"] as PageProperty.UniqueId
            taskId.uniqueId!!.prefix shouldBe "TASK"
            taskId.uniqueId!!.number shouldBe 456
            taskId.formattedId shouldBe "TASK-456"
        }

        "Should handle null unique_id value" {
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
                    "ID": {
                      "id": "unique-id-prop",
                      "type": "unique_id",
                      "unique_id": null
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"

            // Verify unique_id property with null value
            val uniqueIdProperty = page.properties["ID"]
            uniqueIdProperty shouldNotBe null
            uniqueIdProperty.shouldBeInstanceOf<PageProperty.UniqueId>()

            val uniqueId = (uniqueIdProperty as PageProperty.UniqueId)
            uniqueId.uniqueId shouldBe null
            uniqueId.formattedId shouldBe null
        }
    })
