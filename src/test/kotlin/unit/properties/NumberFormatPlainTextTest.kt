package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.pages.FormulaResult
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.RollupResult
import it.saabel.kotlinnotionclient.models.pages.getPlainTextForProperty

/**
 * Unit tests for §10 — integer-aware plain-text rendering of number properties.
 *
 * `Double.formatPlainText()` is private, so we exercise it through the public
 * [getPlainTextForProperty] against a constructed [Page] holding a single property.
 */
@Tags("Unit")
class NumberFormatPlainTextTest :
    StringSpec({

        // Builds a minimal Page carrying a single property named "Value".
        fun pageWith(property: PageProperty): Page =
            Page(
                id = "00000000-0000-0000-0000-000000000000",
                createdTime = "2026-05-30T00:00:00.000Z",
                lastEditedTime = "2026-05-30T00:00:00.000Z",
                parent = Parent.WorkspaceParent,
                properties = mapOf("Value" to property),
                url = "https://www.notion.so/test",
            )

        fun numberPage(value: Double?): Page = pageWith(PageProperty.Number(type = "number", number = value))

        fun formulaNumberPage(value: Double?): Page =
            pageWith(
                PageProperty.Formula(
                    type = "formula",
                    formula = FormulaResult.NumberResult(type = "number", number = value),
                ),
            )

        fun rollupNumberPage(value: Double?): Page =
            pageWith(
                PageProperty.Rollup(
                    type = "rollup",
                    rollup =
                        RollupResult.NumberResult(
                            type = "number",
                            number = value,
                            function = "sum",
                        ),
                ),
            )

        "Number with integral value renders without trailing .0" {
            numberPage(17.0).getPlainTextForProperty("Value") shouldBe "17"
        }

        "Number with decimal value renders verbatim" {
            numberPage(17.5).getPlainTextForProperty("Value") shouldBe "17.5"
        }

        "Number with zero renders as 0" {
            numberPage(0.0).getPlainTextForProperty("Value") shouldBe "0"
        }

        "Number with negative integral value renders without trailing .0" {
            numberPage(-42.0).getPlainTextForProperty("Value") shouldBe "-42"
        }

        "Number with negative decimal value renders verbatim" {
            numberPage(-3.14).getPlainTextForProperty("Value") shouldBe "-3.14"
        }

        "Number with NaN falls back to toString without crashing" {
            numberPage(Double.NaN).getPlainTextForProperty("Value") shouldBe "NaN"
        }

        "Number with positive infinity falls back to toString" {
            numberPage(Double.POSITIVE_INFINITY).getPlainTextForProperty("Value") shouldBe "Infinity"
        }

        "Number with negative infinity falls back to toString" {
            numberPage(Double.NEGATIVE_INFINITY).getPlainTextForProperty("Value") shouldBe "-Infinity"
        }

        "Number at/above MAX_SAFE_INTEGER renders without lossy truncation" {
            // This literal rounds to exactly 2^53 (the next representable Double above
            // it is 2^53 + 2), so it is still faithfully representable. Don't pin the
            // exact string — assert only that the rendered value round-trips back to
            // the original Double with no loss.
            val value = 9_007_199_254_740_993.0
            val rendered = numberPage(value).getPlainTextForProperty("Value")
            rendered.shouldNotBeNull()
            rendered.toDouble() shouldBe value
        }

        "Number with null value renders as null" {
            numberPage(null).getPlainTextForProperty("Value").shouldBeNull()
        }

        "Formula NumberResult with integral value renders as integer" {
            formulaNumberPage(5.0).getPlainTextForProperty("Value") shouldBe "5"
        }

        "Rollup NumberResult with integral value renders as integer" {
            rollupNumberPage(5.0).getPlainTextForProperty("Value") shouldBe "5"
        }

        "Floating-point precision is preserved (pinning test)" {
            // Guards against a future reader "fixing" the precision out of the helper.
            numberPage(0.1 + 0.2).getPlainTextForProperty("Value") shouldBe "0.30000000000000004"
        }
    })
