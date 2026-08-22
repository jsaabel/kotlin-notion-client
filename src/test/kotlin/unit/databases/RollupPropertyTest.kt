package unit.databases

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.DatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.RollupConfiguration
import it.saabel.kotlinnotionclient.models.databases.RollupFunction
import it.saabel.kotlinnotionclient.models.databases.databaseRequest
import it.saabel.kotlinnotionclient.models.datasources.DataSource
import it.saabel.kotlinnotionclient.models.datasources.createDataSourceRequest
import it.saabel.kotlinnotionclient.models.datasources.updateDataSourceRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import unit.util.TestFixtures

/**
 * Unit tests for the typed rollup schema config ([RollupConfiguration]) and rollup
 * creation through the schema DSL.
 *
 * The read-side assertions use the official `get_retrieve_a_data_source` sample, which
 * carries a real rollup property ("Number of meals"). The unknown-function case is
 * hand-crafted — Notion publishes no sample for a function this library does not know.
 */
@Tags("Unit")
class RollupPropertyTest :
    StringSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }

        // ---------------------------------------------------------------------
        // Read side: typed configuration
        // ---------------------------------------------------------------------

        "Should deserialize the rollup property in the official data source sample" {
            val dataSource =
                json.decodeFromString<DataSource>(
                    TestFixtures.DataSources.retrieveDataSourceAsString(),
                )

            val rollup = dataSource.properties["Number of meals"]
            rollup.shouldBeInstanceOf<DatabaseProperty.Rollup>()
            rollup.function shouldBe RollupFunction.COUNT
            rollup.rollup.relationPropertyName shouldBe "Meals"
            rollup.rollup.relationPropertyId shouldBe "mxp^"
            rollup.rollup.rollupPropertyName shouldBe "Name"
            rollup.rollup.rollupPropertyId shouldBe "title"
            rollup.relationReference shouldBe "Meals"
            rollup.rollupReference shouldBe "Name"
        }

        "Should fall back to UNKNOWN for a rollup function this library does not know" {
            val propJson =
                """
                {
                  "id": "Z%5CEh",
                  "name": "Experimental",
                  "type": "rollup",
                  "rollup": {
                    "relation_property_name": "Meals",
                    "rollup_property_name": "Name",
                    "function": "some_future_function"
                  }
                }
                """.trimIndent()

            val property = json.decodeFromString<DatabaseProperty>(propJson)
            property.shouldBeInstanceOf<DatabaseProperty.Rollup>()
            property.function shouldBe RollupFunction.UNKNOWN
        }

        "Should prefer ids when a rollup names only ids" {
            val configuration =
                RollupConfiguration(
                    function = RollupFunction.SUM,
                    relationPropertyId = "fy:{",
                    rollupPropertyId = "\\nyY",
                )

            configuration.relationReference shouldBe "fy:{"
            configuration.rollupReference shouldBe "\\nyY"
        }

        // ---------------------------------------------------------------------
        // Write side: serialization
        // ---------------------------------------------------------------------

        "Should serialize a rollup create property with the documented field names" {
            val property =
                CreateDatabaseProperty.Rollup(
                    rollup =
                        RollupConfiguration(
                            function = RollupFunction.SUM,
                            relationPropertyName = "Tasks",
                            rollupPropertyName = "Hours",
                        ),
                )

            val encoded = json.encodeToString<CreateDatabaseProperty>(property).let(json::parseToJsonElement)
            val body = encoded.jsonObject["rollup"]!!.jsonObject

            encoded.jsonObject["type"]!!.jsonPrimitive.content shouldBe "rollup"
            body["function"]!!.jsonPrimitive.content shouldBe "sum"
            body["relation_property_name"]!!.jsonPrimitive.content shouldBe "Tasks"
            body["rollup_property_name"]!!.jsonPrimitive.content shouldBe "Hours"
        }

        // ---------------------------------------------------------------------
        // Write side: construction-time validation (per the #31/#42 precedent)
        // ---------------------------------------------------------------------

        "Should reject a rollup with no relation reference" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    CreateDatabaseProperty.Rollup(
                        rollup =
                            RollupConfiguration(
                                function = RollupFunction.SUM,
                                rollupPropertyName = "Hours",
                            ),
                    )
                }

            exception.message!! shouldContain "relationPropertyName"
        }

        "Should reject a rollup with no rolled-up property reference" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    CreateDatabaseProperty.Rollup(
                        rollup =
                            RollupConfiguration(
                                function = RollupFunction.COUNT,
                                relationPropertyName = "Tasks",
                            ),
                    )
                }

            exception.message!! shouldContain "rollupPropertyName"
        }

        "Should reject blank references" {
            shouldThrow<IllegalArgumentException> {
                CreateDatabaseProperty.Rollup(
                    rollup =
                        RollupConfiguration(
                            function = RollupFunction.SUM,
                            relationPropertyName = "   ",
                            rollupPropertyName = "Hours",
                        ),
                )
            }
        }

        "Should reject the read-only UNKNOWN function on write" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    CreateDatabaseProperty.Rollup(
                        rollup =
                            RollupConfiguration(
                                function = RollupFunction.UNKNOWN,
                                relationPropertyName = "Tasks",
                                rollupPropertyName = "Hours",
                            ),
                    )
                }

            exception.message!! shouldContain "UNKNOWN"
        }

        // ---------------------------------------------------------------------
        // DSL
        // ---------------------------------------------------------------------

        "Should create a rollup property through the database DSL" {
            val request =
                databaseRequest {
                    parent.page("parent-page-id")
                    title("Projects")
                    properties {
                        title("Name")
                        relation("Tasks", "target-db-id", "target-ds-id")
                        rollup(
                            name = "Total hours",
                            function = RollupFunction.SUM,
                            relationPropertyName = "Tasks",
                            rollupPropertyName = "Hours",
                            description = "Sum of task hours",
                        )
                    }
                }

            val property = request.initialDataSource.properties["Total hours"]
            property.shouldBeInstanceOf<CreateDatabaseProperty.Rollup>()
            property.rollup.function shouldBe RollupFunction.SUM
            property.rollup.relationPropertyName shouldBe "Tasks"
            property.description shouldBe "Sum of task hours"
        }

        "Should create a rollup from a configuration read off an existing schema" {
            val configuration =
                RollupConfiguration(
                    function = RollupFunction.COUNT,
                    relationPropertyName = "Tasks",
                    rollupPropertyName = "Name",
                )

            val request =
                createDataSourceRequest {
                    parent.database("db-id")
                    properties {
                        title("Name")
                        relation("Tasks", "target-db-id", "target-ds-id")
                        rollup("Task count", configuration)
                    }
                }

            val property = request.properties["Task count"]
            property.shouldBeInstanceOf<CreateDatabaseProperty.Rollup>()
            property.rollup shouldBe configuration
        }

        "Should reject a create whose rollup walks a relation missing from the schema" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    databaseRequest {
                        parent.page("parent-page-id")
                        title("Projects")
                        properties {
                            title("Name")
                            rollup(
                                name = "Total hours",
                                function = RollupFunction.SUM,
                                relationPropertyName = "Tasks",
                                rollupPropertyName = "Hours",
                            )
                        }
                    }
                }

            exception.message!! shouldContain "Tasks"
        }

        "Should reject a create whose rollup walks a property that is not a relation" {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    createDataSourceRequest {
                        parent.database("db-id")
                        properties {
                            title("Name")
                            richText("Tasks")
                            rollup(
                                name = "Total hours",
                                function = RollupFunction.SUM,
                                relationPropertyName = "Tasks",
                                rollupPropertyName = "Hours",
                            )
                        }
                    }
                }

            exception.message!! shouldContain "not a relation"
        }

        "Should not check rollup references on an update request" {
            // Update requests carry a partial schema; the relation may already exist
            // on the data source without appearing here.
            val request =
                updateDataSourceRequest {
                    properties {
                        rollup(
                            name = "Total hours",
                            function = RollupFunction.SUM,
                            relationPropertyName = "Tasks",
                            rollupPropertyName = "Hours",
                        )
                    }
                }

            request.properties!!["Total hours"].shouldBeInstanceOf<CreateDatabaseProperty.Rollup>()
        }
    })
