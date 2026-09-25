package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.getTitleAsPlainText
import kotlinx.serialization.json.Json

/**
 * Unit tests for the title page property type.
 *
 * Notion splits a title into several rich-text segments wherever the formatting changes,
 * so plainText must join all segments rather than reading only the first.
 */
@Tags("Unit")
class PagePropertyTitleTest :
    StringSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
            }

        fun pageWithTitleSegments(segments: String): Page =
            json.decodeFromString<Page>(
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
                    "Name": {
                      "id": "title",
                      "type": "title",
                      "title": [$segments]
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent(),
            )

        fun textSegment(
            content: String,
            bold: Boolean = false,
        ): String =
            """
            {
              "type": "text",
              "text": { "content": "$content", "link": null },
              "annotations": {
                "bold": $bold,
                "italic": false,
                "strikethrough": false,
                "underline": false,
                "code": false,
                "color": "default"
              },
              "plain_text": "$content",
              "href": null
            }
            """.trimIndent()

        "Should join all segments of a multi-segment title" {
            val page =
                pageWithTitleSegments(
                    listOf(
                        textSegment("Nordic-Baltic", bold = true),
                        textSegment(" Competition: Student Film"),
                    ).joinToString(","),
                )

            val title = page.properties["Name"].shouldBeInstanceOf<PageProperty.Title>()
            title.title.size shouldBe 2
            title.plainText shouldBe "Nordic-Baltic Competition: Student Film"
            page.getTitleAsPlainText("Name") shouldBe "Nordic-Baltic Competition: Student Film"
        }

        "Should return the text of a single-segment title" {
            val page = pageWithTitleSegments(textSegment("Simple title"))

            page.getTitleAsPlainText("Name") shouldBe "Simple title"
        }

        "Should return an empty string for an empty title" {
            val page = pageWithTitleSegments("")

            page.getTitleAsPlainText("Name") shouldBe ""
        }
    })
