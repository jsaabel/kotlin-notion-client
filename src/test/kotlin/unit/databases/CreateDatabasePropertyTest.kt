package unit.databases

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseProperty
import no.saabelit.kotlinnotionclient.models.databases.RelationConfiguration

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
    })
