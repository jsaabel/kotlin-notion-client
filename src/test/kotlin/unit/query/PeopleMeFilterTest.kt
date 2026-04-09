package unit.query

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.datasources.DataSourceFilter
import it.saabel.kotlinnotionclient.models.datasources.dataSourceQuery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Tags("Unit")
class PeopleMeFilterTest :
    FunSpec({
        val json = Json { encodeDefaults = false }

        fun serializeFilter(filter: DataSourceFilter): String = json.encodeToString(filter)

        context("PeopleFilterBuilder 'me' overloads") {
            test("containsMe() serializes contains value as 'me'") {
                val filter =
                    dataSourceQuery {
                        filter { people("Assignee").containsMe() }
                    }.filter!!
                serializeFilter(filter) shouldContain """"contains":"me""""
            }

            test("doesNotContainMe() serializes does_not_contain value as 'me'") {
                val filter =
                    dataSourceQuery {
                        filter { people("Assignee").doesNotContainMe() }
                    }.filter!!
                serializeFilter(filter) shouldContain """"does_not_contain":"me""""
            }
        }
    })
