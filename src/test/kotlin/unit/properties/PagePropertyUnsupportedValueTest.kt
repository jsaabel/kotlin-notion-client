package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.pages.FormulaResult
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.RollupResult
import it.saabel.kotlinnotionclient.models.pages.getPlainTextForProperty
import kotlinx.serialization.json.Json

/**
 * Unit tests for Notion's own `"unsupported"` formula/rollup value.
 *
 * Notion returns `formula.type` / `rollup.type` = `"unsupported"` (with an empty
 * `unsupported` object) when it cannot compute a value because it depends on too
 * many related pages or nested formulas/rollups. That is a *server-side* condition
 * and must be distinguishable from [PageProperty.Unknown], which is this client's
 * own forward-compatibility fallback for property types it has not modelled yet.
 */
@Tags("Unit")
class PagePropertyUnsupportedValueTest :
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

        "Should deserialize an uncomputable formula as FormulaResult.UnsupportedResult" {
            val pageJson =
                pageWith(
                    """
                    {
                      "Days until launch": {
                        "id": "CSoE",
                        "type": "formula",
                        "formula": {
                          "type": "unsupported",
                          "unsupported": {}
                        }
                      }
                    }
                    """.trimIndent(),
                )

            val page = json.decodeFromString<Page>(pageJson)

            val property = page.properties["Days until launch"]
            property shouldNotBe null
            val formula = property.shouldBeInstanceOf<PageProperty.Formula>()
            formula.id shouldBe "CSoE"

            val result = formula.formula.shouldBeInstanceOf<FormulaResult.UnsupportedResult>()
            result.type shouldBe "unsupported"
        }

        "Should deserialize an uncomputable rollup as RollupResult.UnsupportedResult" {
            val pageJson =
                pageWith(
                    """
                    {
                      "Number of units": {
                        "id": "hgMz",
                        "type": "rollup",
                        "rollup": {
                          "type": "unsupported",
                          "unsupported": {},
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

            val result = rollup.rollup.shouldBeInstanceOf<RollupResult.UnsupportedResult>()
            result.type shouldBe "unsupported"
            result.function shouldBe "count"
        }

        "Notion's unsupported value is NOT the client's Unknown fallback" {
            val pageJson =
                pageWith(
                    """
                    {
                      "Uncomputable": {
                        "id": "CSoE",
                        "type": "formula",
                        "formula": { "type": "unsupported", "unsupported": {} }
                      },
                      "Brand new type": {
                        "id": "btn",
                        "type": "some_future_type",
                        "some_future_type": {}
                      }
                    }
                    """.trimIndent(),
                )

            val page = json.decodeFromString<Page>(pageJson)

            // Notion says "I could not compute this" -> a modelled formula result.
            page.properties["Uncomputable"].shouldBeInstanceOf<PageProperty.Formula>()

            // We say "I do not know this property type yet" -> the client's fallback.
            val unknown = page.properties["Brand new type"].shouldBeInstanceOf<PageProperty.Unknown>()
            unknown.type shouldBe "some_future_type"
        }

        "Plain text of an unsupported formula/rollup value is null" {
            val pageJson =
                pageWith(
                    """
                    {
                      "Formula": {
                        "id": "CSoE",
                        "type": "formula",
                        "formula": { "type": "unsupported", "unsupported": {} }
                      },
                      "Rollup": {
                        "id": "hgMz",
                        "type": "rollup",
                        "rollup": { "type": "unsupported", "unsupported": {}, "function": "count" }
                      }
                    }
                    """.trimIndent(),
                )

            val page = json.decodeFromString<Page>(pageJson)

            page.getPlainTextForProperty("Formula") shouldBe null
            page.getPlainTextForProperty("Rollup") shouldBe null
        }
    })
