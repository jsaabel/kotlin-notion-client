package unit.query

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.datasources.DataSourceFilter
import it.saabel.kotlinnotionclient.models.datasources.dataSourceQuery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Tags("Unit")
class MultiSelectFilterBuilderMultiValueTest :
    FunSpec({
        val json = Json { encodeDefaults = false }

        fun serialize(filter: DataSourceFilter): String = json.encodeToString(filter)

        context("contains") {
            test("single value serializes to a string (back-compat)") {
                val filter = dataSourceQuery { filter { multiSelect("Tags").contains("X") } }.filter!!
                serialize(filter) shouldContain """"contains":"X""""
            }

            test("multiple values serialize to an array") {
                val filter = dataSourceQuery { filter { multiSelect("Tags").contains("X", "Y") } }.filter!!
                serialize(filter) shouldContain """"contains":["X","Y"]"""
            }
        }

        context("doesNotContain") {
            test("single value serializes to a string") {
                val filter = dataSourceQuery { filter { multiSelect("Tags").doesNotContain("X") } }.filter!!
                serialize(filter) shouldContain """"does_not_contain":"X""""
            }

            test("multiple values serialize to an array") {
                val filter = dataSourceQuery { filter { multiSelect("Tags").doesNotContain("X", "Y") } }.filter!!
                serialize(filter) shouldContain """"does_not_contain":["X","Y"]"""
            }
        }

        context("empty varargs") {
            test("contains with no values throws") {
                shouldThrow<IllegalArgumentException> {
                    dataSourceQuery { filter { multiSelect("Tags").contains() } }
                }
            }

            test("doesNotContain with no values throws") {
                shouldThrow<IllegalArgumentException> {
                    dataSourceQuery { filter { multiSelect("Tags").doesNotContain() } }
                }
            }
        }
    })
