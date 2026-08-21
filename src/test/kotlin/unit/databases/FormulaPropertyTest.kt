package unit.databases

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.DatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.FormulaConfiguration
import it.saabel.kotlinnotionclient.models.databases.databaseRequest
import it.saabel.kotlinnotionclient.models.datasources.createDataSourceRequest
import it.saabel.kotlinnotionclient.models.datasources.updateDataSourceRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Unit tests for typed formula expressions (Aug 12, 2026 API changelog: readable
 * `prop("Property Name")` syntax on read, verbatim storage + validation errors on write).
 *
 * Covers the typed read model ([DatabaseProperty.Formula]), the write model
 * ([CreateDatabaseProperty.Formula]) with its fail-fast syntax validation, the
 * `formula(...)` DSL, and the create-time check that `prop()` references name properties
 * defined in the same schema.
 */
@Tags("Unit")
class FormulaPropertyTest :
    StringSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }

        // ---------------------------------------------------------------------
        // Read side: typed expression
        // ---------------------------------------------------------------------

        "Should deserialize formula property with readable prop() expression" {
            val propJson =
                """
                {
                  "id": "YU%7C%40",
                  "name": "Updated price",
                  "type": "formula",
                  "formula": {
                    "expression": "if(prop(\"In stock\"), 0, prop(\"Price\"))"
                  }
                }
                """.trimIndent()

            val property = json.decodeFromString<DatabaseProperty>(propJson)

            val formula = property.shouldBeInstanceOf<DatabaseProperty.Formula>()
            formula.expression shouldBe "if(prop(\"In stock\"), 0, prop(\"Price\"))"
            formula.formula.propertyReferences() shouldBe listOf("In stock", "Price")
            formula.formula.usesInternalReferences() shouldBe false
        }

        "Should deserialize formula property that still uses internal reference syntax" {
            // The readable syntax is rolling out gradually; expressions that cannot be
            // rendered faithfully keep the internal syntax (vendored API docs shape).
            val propJson =
                """
                {
                  "id": "p:sC",
                  "name": "Half price",
                  "type": "formula",
                  "formula": {
                    "expression": "{{notion:block_property:BtVS:00000000-0000-0000-0000-000000000000:8994905a-074a-415f-9bcf-d1f8b4fa38e4}}/2"
                  }
                }
                """.trimIndent()

            val formula = json.decodeFromString<DatabaseProperty>(propJson).shouldBeInstanceOf<DatabaseProperty.Formula>()
            formula.formula.usesInternalReferences() shouldBe true
            formula.formula.propertyReferences() shouldBe emptyList()
        }

        "Should extract references with escaped quotes, deduplicated, ignoring strings and comments" {
            val config =
                FormulaConfiguration(
                    expression =
                        """concat(prop("Say \"hi\""), "prop(\"not a ref\")", /* prop("commented") */ prop("Say \"hi\""), prop("Other"))""",
                )
            config.propertyReferences() shouldBe listOf("Say \"hi\"", "Other")
        }

        // ---------------------------------------------------------------------
        // Write side: serialization
        // ---------------------------------------------------------------------

        "Should serialize CreateDatabaseProperty.Formula with formula.expression" {
            val property: CreateDatabaseProperty =
                CreateDatabaseProperty.Formula(
                    formula = FormulaConfiguration(expression = "prop(\"Price\") * 2"),
                )

            val encoded = json.encodeToString(property)
            val element = json.parseToJsonElement(encoded).jsonObject
            element["type"]?.jsonPrimitive?.content shouldBe "formula"
            element["formula"]!!.jsonObject["expression"]!!.jsonPrimitive.content shouldBe "prop(\"Price\") * 2"
        }

        "Should accept internal reference syntax on write (round-trip of a read expression)" {
            val expression = "{{notion:block_property:BtVS:00000000-0000-0000-0000-000000000000:8994905a-074a-415f-9bcf-d1f8b4fa38e4}}/2"
            val property = CreateDatabaseProperty.Formula(formula = FormulaConfiguration(expression))
            property.formula.expression shouldBe expression
        }

        // ---------------------------------------------------------------------
        // Write side: fail-fast syntax validation
        // ---------------------------------------------------------------------

        "Should reject blank expression" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    CreateDatabaseProperty.Formula(formula = FormulaConfiguration(expression = "   "))
                }
            exception.message shouldContain "blank"
        }

        "Should reject unterminated string literal" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    CreateDatabaseProperty.Formula(formula = FormulaConfiguration(expression = "concat(\"open, 1)"))
                }
            exception.message shouldContain "unterminated string literal"
            exception.message shouldContain "concat(\"open, 1)"
        }

        "Should reject unbalanced brackets" {
            shouldThrow<IllegalArgumentException> {
                CreateDatabaseProperty.Formula(formula = FormulaConfiguration(expression = "if(prop(\"A\"), 1, 2"))
            }.message shouldContain "never closed"

            shouldThrow<IllegalArgumentException> {
                CreateDatabaseProperty.Formula(formula = FormulaConfiguration(expression = "prop(\"A\") + 1)"))
            }.message shouldContain "unmatched closing ')'"

            shouldThrow<IllegalArgumentException> {
                CreateDatabaseProperty.Formula(formula = FormulaConfiguration(expression = "min([1, 2)]"))
            }.message shouldContain "does not match"
        }

        "Should reject malformed prop() calls" {
            shouldThrow<IllegalArgumentException> {
                CreateDatabaseProperty.Formula(formula = FormulaConfiguration(expression = "prop(Price) * 2"))
            }.message shouldContain "double-quoted property name"

            shouldThrow<IllegalArgumentException> {
                CreateDatabaseProperty.Formula(formula = FormulaConfiguration(expression = "prop(\"A\", \"B\")"))
            }.message shouldContain "exactly one"

            shouldThrow<IllegalArgumentException> {
                CreateDatabaseProperty.Formula(formula = FormulaConfiguration(expression = "prop(\"\") + 1"))
            }.message shouldContain "empty property name"
        }

        "Should not reject quotes or brackets inside strings and comments" {
            // These are structurally fine: the suspicious characters are quoted/commented.
            CreateDatabaseProperty
                .Formula(
                    formula =
                        FormulaConfiguration(
                            expression = "concat(\"a ) [ } quote: \\\" ok\", \"b\") /* ( unclosed comment bracket */",
                        ),
                ).formula.expression shouldContain "unclosed comment bracket"
        }

        "Should validate description length on formula properties" {
            shouldThrow<IllegalArgumentException> {
                CreateDatabaseProperty.Formula(
                    formula = FormulaConfiguration(expression = "1 + 1"),
                    description = "x".repeat(281),
                )
            }.message shouldContain "280"
        }

        // ---------------------------------------------------------------------
        // DSL + create-time reference checking
        // ---------------------------------------------------------------------

        "Should build database request with formula property via DSL" {
            val request =
                databaseRequest {
                    parent.page("test-page-id")
                    title("Inventory")
                    properties {
                        title("Name")
                        number("Price", format = "dollar")
                        checkbox("In stock")
                        formula("Updated price", """if(prop("In stock"), 0, prop("Price"))""")
                    }
                }

            val formula =
                request.initialDataSource.properties["Updated price"]
                    .shouldBeInstanceOf<CreateDatabaseProperty.Formula>()
            formula.formula.expression shouldBe """if(prop("In stock"), 0, prop("Price"))"""
        }

        "Should name the property in DSL validation errors" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    databaseRequest {
                        parent.page("test-page-id")
                        title("Inventory")
                        properties {
                            title("Name")
                            formula("Broken", "prop(Price)")
                        }
                    }
                }
            exception.message shouldContain "'Broken'"
            exception.message shouldContain "prop(Price)"
        }

        "Should reject create requests whose formulas reference undefined properties" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    databaseRequest {
                        parent.page("test-page-id")
                        title("Inventory")
                        properties {
                            title("Name")
                            formula("Updated price", """prop("Pricee") * 2""")
                        }
                    }
                }
            exception.message shouldContain "'Updated price'"
            exception.message shouldContain "Pricee"
            exception.message shouldContain "Name"
        }

        "Should reject create data source requests whose formulas reference undefined properties" {
            shouldThrow<IllegalArgumentException> {
                createDataSourceRequest {
                    databaseId("db-id")
                    properties {
                        title("Name")
                        formula("Total", """prop("Missing") + 1""")
                    }
                }
            }.message shouldContain "Missing"
        }

        "Should allow update requests to reference properties absent from the partial schema" {
            // An update carries a partial schema; prop() may reference properties that
            // already exist on the data source, so existence is not locally checkable.
            val request =
                updateDataSourceRequest {
                    properties {
                        formula("Total", """prop("Existing elsewhere") + 1""")
                    }
                }
            request.properties!!["Total"].shouldBeInstanceOf<CreateDatabaseProperty.Formula>()
        }
    })
