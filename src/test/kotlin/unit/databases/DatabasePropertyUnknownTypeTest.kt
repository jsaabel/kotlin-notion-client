package unit.databases

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.databases.DatabaseProperty
import kotlinx.serialization.json.Json

/**
 * Unit tests for handling unknown/unsupported database property types.
 *
 * This test ensures that when Notion returns a property type the client doesn't
 * know about (e.g., "button"), deserialization falls back to [DatabaseProperty.Unknown]
 * instead of crashing.
 */
@Tags("Unit")
class DatabasePropertyUnknownTypeTest :
    StringSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
            }

        "Should deserialize button property as Unknown type" {
            val propJson =
                """
                {
                  "id": "A%5E%3Bf",
                  "name": "Create schedule",
                  "description": null,
                  "type": "button",
                  "button": {}
                }
                """.trimIndent()

            val prop = json.decodeFromString<DatabaseProperty>(propJson)

            val unknown = prop.shouldBeInstanceOf<DatabaseProperty.Unknown>()
            unknown.type shouldBe "button"
            unknown.id shouldBe "A%5E%3Bf"
            unknown.name shouldBe "Create schedule"
        }

        "Should deserialize known property types correctly" {
            val titleJson =
                """
                {
                  "id": "title-id",
                  "name": "Name",
                  "type": "title",
                  "title": {}
                }
                """.trimIndent()

            val prop = json.decodeFromString<DatabaseProperty>(titleJson)
            prop.shouldBeInstanceOf<DatabaseProperty.Title>()
            prop.id shouldBe "title-id"
            prop.name shouldBe "Name"
        }

        "Should preserve raw JSON in Unknown property" {
            val propJson =
                """
                {
                  "id": "btn-id",
                  "name": "Action",
                  "description": null,
                  "type": "button",
                  "button": {}
                }
                """.trimIndent()

            val prop = json.decodeFromString<DatabaseProperty>(propJson)
            val unknown = prop as DatabaseProperty.Unknown

            unknown.rawContent shouldNotBe null
            unknown.id shouldBe "btn-id"
            unknown.name shouldBe "Action"
            unknown.type shouldBe "button"
        }

        "Should handle multiple unknown types in a map" {
            val mapJson =
                """
                {
                  "Button": {
                    "id": "btn-id",
                    "name": "Button",
                    "type": "button",
                    "button": {}
                  },
                  "AI Summary": {
                    "id": "ai-id",
                    "name": "AI Summary",
                    "type": "ai_summary",
                    "ai_summary": {}
                  },
                  "Status": {
                    "id": "status-id",
                    "name": "Status",
                    "type": "status",
                    "status": {
                      "options": [{"id": "opt-1", "name": "Not started", "color": "default"}],
                      "groups": [{"id": "grp-1", "name": "To-do", "color": "gray", "option_ids": ["opt-1"]}]
                    }
                  }
                }
                """.trimIndent()

            val props = json.decodeFromString<Map<String, DatabaseProperty>>(mapJson)

            props.size shouldBe 3
            props["Button"]!!.shouldBeInstanceOf<DatabaseProperty.Unknown>()
            props["AI Summary"]!!.shouldBeInstanceOf<DatabaseProperty.Unknown>()
            props["Status"]!!.shouldBeInstanceOf<DatabaseProperty.Status>()
        }

        "Should handle all known property types alongside unknown" {
            val mapJson =
                """
                {
                  "Title": {"id": "1", "name": "Title", "type": "title", "title": {}},
                  "Notes": {"id": "2", "name": "Notes", "type": "rich_text", "rich_text": {}},
                  "Count": {"id": "3", "name": "Count", "type": "number", "number": {"format": "number"}},
                  "Done": {"id": "4", "name": "Done", "type": "checkbox", "checkbox": {}},
                  "Link": {"id": "5", "name": "Link", "type": "url", "url": {}},
                  "Contact": {"id": "6", "name": "Contact", "type": "email", "email": {}},
                  "Phone": {"id": "7", "name": "Phone", "type": "phone_number", "phone_number": {}},
                  "Future": {"id": "8", "name": "Future", "type": "some_future_type", "some_future_type": {"data": true}}
                }
                """.trimIndent()

            val props = json.decodeFromString<Map<String, DatabaseProperty>>(mapJson)

            props["Title"]!!.shouldBeInstanceOf<DatabaseProperty.Title>()
            props["Notes"]!!.shouldBeInstanceOf<DatabaseProperty.RichText>()
            props["Count"]!!.shouldBeInstanceOf<DatabaseProperty.Number>()
            props["Done"]!!.shouldBeInstanceOf<DatabaseProperty.Checkbox>()
            props["Link"]!!.shouldBeInstanceOf<DatabaseProperty.Url>()
            props["Contact"]!!.shouldBeInstanceOf<DatabaseProperty.Email>()
            props["Phone"]!!.shouldBeInstanceOf<DatabaseProperty.PhoneNumber>()

            val unknown = props["Future"]!!.shouldBeInstanceOf<DatabaseProperty.Unknown>()
            unknown.type shouldBe "some_future_type"
        }
    })
