package unit.databases

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.CreateSelectOption
import it.saabel.kotlinnotionclient.models.databases.RelationConfiguration
import it.saabel.kotlinnotionclient.models.databases.StatusConfiguration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Unit tests for CreateDatabaseProperty functionality.
 *
 * Tests all database/data source property types including the Relation property.
 * As of 2025-09-03, these are used when creating data sources.
 * Focuses on testing the actual functionality rather than JSON formatting.
 */
@Tags("Unit")
class CreateDatabasePropertyTest :
    StringSpec({

        "Should create Title property correctly" {
            val property = CreateDatabaseProperty.Title()
            property shouldNotBe null
        }

        "Should create Number property correctly" {
            val property = CreateDatabaseProperty.Number()
            property shouldNotBe null
            property.number.format shouldBe "number"
        }

        "Should create Select property correctly" {
            val property = CreateDatabaseProperty.Select()
            property shouldNotBe null
            property.select.options shouldBe emptyList()
        }

        "Should create Status property with default options (empty configuration)" {
            val property = CreateDatabaseProperty.Status()
            property shouldNotBe null
            property.status.options shouldBe emptyList()
        }

        "Should create Status property with custom initial options" {
            val property =
                CreateDatabaseProperty.Status(
                    status =
                        StatusConfiguration(
                            options =
                                listOf(
                                    CreateSelectOption("Backlog", SelectOptionColor.GRAY),
                                    CreateSelectOption("In Progress", SelectOptionColor.BLUE),
                                    CreateSelectOption("Done", SelectOptionColor.GREEN),
                                ),
                        ),
                )
            property shouldNotBe null
            property.status.options shouldHaveSize 3
            property.status.options.map { it.name } shouldBe listOf("Backlog", "In Progress", "Done")
        }

        "Should create Relation property with single property correctly" {
            val relationConfig = RelationConfiguration.singleProperty("test-database-id", "test-datasource-id")
            val property = CreateDatabaseProperty.Relation(relationConfig)

            property shouldNotBe null
            property.relation shouldBe relationConfig
            property.relation.databaseId shouldBe "test-database-id"
            property.relation.dataSourceId shouldBe "test-datasource-id"
        }

        "Should create Relation property with dual property correctly" {
            val relationConfig =
                RelationConfiguration.dualProperty(
                    databaseId = "test-database-id",
                    dataSourceId = "test-datasource-id",
                    syncedPropertyName = "Related Items",
                )
            val property = CreateDatabaseProperty.Relation(relationConfig)

            property shouldNotBe null
            property.relation shouldBe relationConfig
            property.relation.databaseId shouldBe "test-database-id"
            property.relation.dataSourceId shouldBe "test-datasource-id"
            property.relation.dualProperty?.syncedPropertyName shouldBe "Related Items"
        }

        "Should create RelationConfiguration.singleProperty correctly" {
            val config = RelationConfiguration.singleProperty("test-database-id", "test-datasource-id")

            config.databaseId shouldBe "test-database-id"
            config.dataSourceId shouldBe "test-datasource-id"
            config.singleProperty shouldNotBe null
            config.dualProperty shouldBe null
            config.syncedPropertyName shouldBe null
            config.syncedPropertyId shouldBe null
        }

        "Should create RelationConfiguration.dualProperty correctly" {
            val config =
                RelationConfiguration.dualProperty(
                    databaseId = "test-database-id",
                    dataSourceId = "test-datasource-id",
                    syncedPropertyName = "Backlinks",
                    syncedPropertyId = "abc123",
                )

            config.databaseId shouldBe "test-database-id"
            config.dataSourceId shouldBe "test-datasource-id"
            config.singleProperty shouldBe null
            config.dualProperty?.syncedPropertyName shouldBe "Backlinks"
            config.dualProperty?.syncedPropertyId shouldBe "abc123"
            config.syncedPropertyName shouldBe null
            config.syncedPropertyId shouldBe null
        }

        "Should create RelationConfiguration.synced correctly" {
            val config =
                RelationConfiguration.synced(
                    databaseId = "test-database-id",
                    dataSourceId = "test-datasource-id",
                    syncedPropertyName = "Related Items",
                )

            config.databaseId shouldBe "test-database-id"
            config.dataSourceId shouldBe "test-datasource-id"
            config.singleProperty shouldBe null
            config.dualProperty shouldBe null
            config.syncedPropertyName shouldBe "Related Items"
            config.syncedPropertyId shouldBe null
        }

        "Should store property description on CreateDatabaseProperty subtypes" {
            val titleProp = CreateDatabaseProperty.Title(description = "The page title")
            titleProp.description shouldBe "The page title"

            val numberProp = CreateDatabaseProperty.Number(description = "A numeric score")
            numberProp.description shouldBe "A numeric score"

            val checkboxProp = CreateDatabaseProperty.Checkbox(description = null)
            checkboxProp.description shouldBe null
        }

        "Should serialize property description to JSON" {
            val json = Json { encodeDefaults = false }
            val prop = CreateDatabaseProperty.RichText(description = "Some notes")
            val encoded = json.encodeToString<CreateDatabaseProperty>(prop)
            val parsed = Json.parseToJsonElement(encoded).jsonObject
            parsed["description"]?.jsonPrimitive?.content shouldBe "Some notes"
        }

        "Should omit description from JSON when null" {
            val json = Json { encodeDefaults = false }
            val prop = CreateDatabaseProperty.Checkbox()
            val encoded = json.encodeToString<CreateDatabaseProperty>(prop)
            val parsed = Json.parseToJsonElement(encoded).jsonObject
            parsed.containsKey("description") shouldBe false
        }

        "Should reject property description longer than 280 characters" {
            val tooLong = "x".repeat(281)
            shouldThrow<IllegalArgumentException> {
                CreateDatabaseProperty.Select(description = tooLong)
            }
        }

        "Should accept property description of exactly 280 characters" {
            val exactly280 = "x".repeat(280)
            val prop = CreateDatabaseProperty.Select(description = exactly280)
            prop.description?.length shouldBe 280
        }
    })
