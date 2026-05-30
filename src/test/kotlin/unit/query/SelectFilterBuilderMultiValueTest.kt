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
class SelectFilterBuilderMultiValueTest :
    FunSpec({
        val json = Json { encodeDefaults = false }

        fun serialize(filter: DataSourceFilter): String = json.encodeToString(filter)

        context("equals") {
            test("single value serializes to a string (back-compat)") {
                val filter = dataSourceQuery { filter { select("Status").equals("Done") } }.filter!!
                serialize(filter) shouldContain """"equals":"Done""""
            }

            test("multiple values serialize to an array") {
                val filter = dataSourceQuery { filter { select("Status").equals("ToDo", "Done") } }.filter!!
                serialize(filter) shouldContain """"equals":["ToDo","Done"]"""
            }
        }

        context("doesNotEqual") {
            test("single value serializes to a string") {
                val filter = dataSourceQuery { filter { select("Status").doesNotEqual("Done") } }.filter!!
                serialize(filter) shouldContain """"does_not_equal":"Done""""
            }

            test("multiple values serialize to an array") {
                val filter = dataSourceQuery { filter { select("Status").doesNotEqual("ToDo", "Done") } }.filter!!
                serialize(filter) shouldContain """"does_not_equal":["ToDo","Done"]"""
            }
        }

        context("empty varargs") {
            test("equals with no values throws") {
                shouldThrow<IllegalArgumentException> {
                    dataSourceQuery { filter { select("Status").equals() } }
                }
            }

            test("doesNotEqual with no values throws") {
                shouldThrow<IllegalArgumentException> {
                    dataSourceQuery { filter { select("Status").doesNotEqual() } }
                }
            }
        }
    })
