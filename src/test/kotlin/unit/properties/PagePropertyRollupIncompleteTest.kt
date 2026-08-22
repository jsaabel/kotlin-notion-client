package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.RollupResult
import it.saabel.kotlinnotionclient.models.pages.getPlainTextForProperty
import kotlinx.serialization.json.Json

/**
 * Unit test for Notion's `rollup.type = "incomplete"` value.
 *
 * Notion documents rollup value types as `array | date | incomplete | number |
 * unsupported`. `"incomplete"` is returned while a rollup over a large or
 * slow-to-resolve relation is still being calculated in the background — distinct
 * from `"unsupported"`, which means Notion has given up computing the value.
 *
 * No official sample response in `reference/notion-api/sample_responses/` or
 * `src/test/resources/api/` covers this value, so the fixture JSON below is
 * hand-crafted from the documented shape (mirroring the existing `"unsupported"`
 * shape, per `reference/notion-api/documentation/objects/02_Page_PageProperties.md`).
 */
@Tags("Unit")
class PagePropertyRollupIncompleteTest :
    StringSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
            }

        fun pageWith(properties: String) =
            """
            {
              "object": "page",
              "id": "test-page-id",
              "created_time": "2025-01-01T00:00:00.000Z",
              "last_edited_time": "2025-01-01T00:00:00.000Z",
              "archived": false,
              "in_trash": false,
              "parent": { "type": "workspace", "workspace": true },
              "properties": $properties,
              "url": "https://www.notion.so/test-page-id"
            }
            """.trimIndent()

        "Should deserialize a still-computing rollup as RollupResult.IncompleteResult" {
            val pageJson =
                pageWith(
                    """
                    {
                      "Number of units": {
                        "id": "hgMz",
                        "type": "rollup",
                        "rollup": {
                          "type": "incomplete",
                          "incomplete": {},
                          "function": "count"
                        }
                      }
                    }
                    """.trimIndent(),
                )

            val page = json.decodeFromString<Page>(pageJson)

            val property = page.properties["Number of units"]
            property shouldNotBe null
            val rollup = property.shouldBeInstanceOf<PageProperty.Rollup>()
            rollup.id shouldBe "hgMz"

            val result = rollup.rollup.shouldBeInstanceOf<RollupResult.IncompleteResult>()
            result.type shouldBe "incomplete"
            result.function shouldBe "count"
        }

        "Plain text of an incomplete rollup value is null" {
            val pageJson =
                pageWith(
                    """
                    {
                      "Rollup": {
                        "id": "hgMz",
                        "type": "rollup",
                        "rollup": { "type": "incomplete", "incomplete": {}, "function": "count" }
                      }
                    }
                    """.trimIndent(),
                )

            val page = json.decodeFromString<Page>(pageJson)

            page.getPlainTextForProperty("Rollup") shouldBe null
        }
    })
