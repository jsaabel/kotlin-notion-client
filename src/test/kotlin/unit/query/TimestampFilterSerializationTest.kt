package unit.query

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.datasources.DataSourceFilter
import it.saabel.kotlinnotionclient.models.datasources.DateCondition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tests for timestamp filter serialization (2025-09-03 API).
 *
 * Verifies that timestamp filters serialize to the correct JSON structure,
 * particularly ensuring that they don't include a "property" field and do
 * include the "timestamp" field.
 */
@Tags("Unit")
class TimestampFilterSerializationTest :
    StringSpec({

        val json =
            Json {
                encodeDefaults = false
                prettyPrint = false
            }

        "Should serialize created_time timestamp filter correctly" {
            val filter =
                DataSourceFilter(
                    timestamp = "created_time",
                    createdTime = DateCondition(onOrBefore = "2022-10-13"),
                )

            val serialized = json.encodeToString(filter)

            // Should contain timestamp and created_time fields
            serialized shouldBe """{"timestamp":"created_time","created_time":{"on_or_before":"2022-10-13"}}"""
        }

        "Should serialize last_edited_time timestamp filter correctly" {
            val filter =
                DataSourceFilter(
                    timestamp = "last_edited_time",
                    lastEditedTime = DateCondition(after = "2023-01-01"),
                )

            val serialized = json.encodeToString(filter)

            // Should contain timestamp and last_edited_time fields
            serialized shouldBe """{"timestamp":"last_edited_time","last_edited_time":{"after":"2023-01-01"}}"""
        }

        "Should serialize timestamp filter with before condition" {
            val filter =
                DataSourceFilter(
                    timestamp = "created_time",
                    createdTime = DateCondition(before = "2024-06-15"),
                )

            val serialized = json.encodeToString(filter)

            serialized shouldBe """{"timestamp":"created_time","created_time":{"before":"2024-06-15"}}"""
        }

        "Should serialize timestamp filter with onOrAfter condition" {
            val filter =
                DataSourceFilter(
                    timestamp = "last_edited_time",
                    lastEditedTime = DateCondition(onOrAfter = "2024-01-01"),
                )

            val serialized = json.encodeToString(filter)

            serialized shouldBe """{"timestamp":"last_edited_time","last_edited_time":{"on_or_after":"2024-01-01"}}"""
        }

        "Should serialize timestamp filter with relative date condition" {
            val filter =
                DataSourceFilter(
                    timestamp = "created_time",
                    createdTime =
                        DateCondition(
                            pastWeek =
                                it.saabel.kotlinnotionclient.models.base
                                    .EmptyObject(),
                        ),
                )

            val serialized = json.encodeToString(filter)

            serialized shouldBe """{"timestamp":"created_time","created_time":{"past_week":{}}}"""
        }

        "Should serialize timestamp filter with isEmpty condition" {
            val filter =
                DataSourceFilter(
                    timestamp = "last_edited_time",
                    lastEditedTime = DateCondition(isEmpty = true),
                )

            val serialized = json.encodeToString(filter)

            serialized shouldBe """{"timestamp":"last_edited_time","last_edited_time":{"is_empty":true}}"""
        }

        "Should serialize compound filter with timestamp and property filters" {
            val timestampFilter =
                DataSourceFilter(
                    timestamp = "created_time",
                    createdTime = DateCondition(after = "2024-01-01"),
                )

            val propertyFilter =
                DataSourceFilter(
                    property = "Status",
                    checkbox =
                        it.saabel.kotlinnotionclient.models.datasources
                            .CheckboxCondition(equals = true),
                )

            val compoundFilter = DataSourceFilter(and = listOf(timestampFilter, propertyFilter))

            val serialized = json.encodeToString(compoundFilter)

            // Verify structure contains both filters
            serialized shouldBe
                """{"and":[{"timestamp":"created_time","created_time":{"after":"2024-01-01"}},{"property":"Status","checkbox":{"equals":true}}]}"""
        }

        "Should not include property field in timestamp filters" {
            val filter =
                DataSourceFilter(
                    timestamp = "created_time",
                    createdTime = DateCondition(equals = "2024-01-01"),
                )

            val serialized = json.encodeToString(filter)

            // Verify that "property" field is not present
            serialized.contains("\"property\"") shouldBe false
            serialized.contains("\"timestamp\"") shouldBe true
        }
    })
