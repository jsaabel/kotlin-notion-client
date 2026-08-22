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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Unit tests for the catch-all fallback on [FormulaResult] and [RollupResult].
 *
 * Before this, an unrecognized `formula.type` / `rollup.type` had no matching
 * sealed subtype and threw during deserialization, taking down the whole page —
 * the same crash class #36 fixed for [PageProperty] with [PageProperty.Unknown].
 * [FormulaResult.Unknown] and [RollupResult.Unknown] close that gap one level
 * down, using an invented future type name that Notion has never actually sent.
 */
@Tags("Unit")
class FormulaRollupUnknownFallbackTest :
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

        "Should deserialize an unrecognized formula result type as FormulaResult.Unknown" {
            val pageJson =
                pageWith(
                    """
                    {
                      "Future formula": {
                        "id": "CSoE",
                        "type": "formula",
                        "formula": {
                          "type": "vector",
                          "vector": [1, 2, 3]
                        }
                      }
                    }
                    """.trimIndent(),
                )

            val page = json.decodeFromString<Page>(pageJson)

            val property = page.properties["Future formula"]
            property shouldNotBe null
            val formula = property.shouldBeInstanceOf<PageProperty.Formula>()

            val result = formula.formula.shouldBeInstanceOf<FormulaResult.Unknown>()
            result.type shouldBe "vector"
            result.rawContent.jsonObject["type"]
                ?.jsonPrimitive
                ?.content shouldBe "vector"
        }

        "Should deserialize an unrecognized rollup result type as RollupResult.Unknown" {
            val pageJson =
                pageWith(
                    """
                    {
                      "Future rollup": {
                        "id": "hgMz",
                        "type": "rollup",
                        "rollup": {
                          "type": "vector",
                          "vector": [1, 2, 3],
                          "function": "sum"
                        }
                      }
                    }
                    """.trimIndent(),
                )

            val page = json.decodeFromString<Page>(pageJson)

            val property = page.properties["Future rollup"]
            property shouldNotBe null
            val rollup = property.shouldBeInstanceOf<PageProperty.Rollup>()

            val result = rollup.rollup.shouldBeInstanceOf<RollupResult.Unknown>()
            result.type shouldBe "vector"
            result.rawContent.jsonObject["function"]
                ?.jsonPrimitive
                ?.content shouldBe "sum"
        }

        "Plain text of an unrecognized formula/rollup result type is null" {
            val pageJson =
                pageWith(
                    """
                    {
                      "Formula": {
                        "id": "CSoE",
                        "type": "formula",
                        "formula": { "type": "vector", "vector": [1, 2, 3] }
                      },
                      "Rollup": {
                        "id": "hgMz",
                        "type": "rollup",
                        "rollup": { "type": "vector", "vector": [1, 2, 3], "function": "sum" }
                      }
                    }
                    """.trimIndent(),
                )

            val page = json.decodeFromString<Page>(pageJson)

            page.getPlainTextForProperty("Formula") shouldBe null
            page.getPlainTextForProperty("Rollup") shouldBe null
        }
    })
