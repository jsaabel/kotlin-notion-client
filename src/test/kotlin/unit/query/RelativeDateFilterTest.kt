package unit.query

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.datasources.DataSourceFilter
import it.saabel.kotlinnotionclient.models.datasources.RelativeDateValue
import it.saabel.kotlinnotionclient.models.datasources.dataSourceQuery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Tags("Unit")
class RelativeDateFilterTest :
    FunSpec({
        val json = Json { encodeDefaults = false }

        context("RelativeDateValue.apiValue") {
            test("TODAY maps to 'today'") { RelativeDateValue.TODAY.apiValue shouldBe "today" }
            test("TOMORROW maps to 'tomorrow'") { RelativeDateValue.TOMORROW.apiValue shouldBe "tomorrow" }
            test("YESTERDAY maps to 'yesterday'") { RelativeDateValue.YESTERDAY.apiValue shouldBe "yesterday" }
            test("ONE_WEEK_AGO maps to 'one_week_ago'") { RelativeDateValue.ONE_WEEK_AGO.apiValue shouldBe "one_week_ago" }
            test("ONE_WEEK_FROM_NOW maps to 'one_week_from_now'") {
                RelativeDateValue.ONE_WEEK_FROM_NOW.apiValue shouldBe
                    "one_week_from_now"
            }
            test("ONE_MONTH_AGO maps to 'one_month_ago'") { RelativeDateValue.ONE_MONTH_AGO.apiValue shouldBe "one_month_ago" }
            test("ONE_MONTH_FROM_NOW maps to 'one_month_from_now'") {
                RelativeDateValue.ONE_MONTH_FROM_NOW.apiValue shouldBe
                    "one_month_from_now"
            }
        }

        context("DateFilterBuilder relative overloads serialization") {
            fun serializeFilter(filter: DataSourceFilter): String = json.encodeToString(filter)

            test("equals(RelativeDateValue) serializes correctly") {
                val filter =
                    dataSourceQuery {
                        filter { date("Due Date").equals(RelativeDateValue.TODAY) }
                    }.filter!!
                serializeFilter(filter) shouldContain """"equals":"today""""
            }

            test("before(RelativeDateValue) serializes correctly") {
                val filter =
                    dataSourceQuery {
                        filter { date("Due Date").before(RelativeDateValue.TOMORROW) }
                    }.filter!!
                serializeFilter(filter) shouldContain """"before":"tomorrow""""
            }

            test("after(RelativeDateValue) serializes correctly") {
                val filter =
                    dataSourceQuery {
                        filter { date("Due Date").after(RelativeDateValue.YESTERDAY) }
                    }.filter!!
                serializeFilter(filter) shouldContain """"after":"yesterday""""
            }

            test("onOrBefore(RelativeDateValue) serializes correctly") {
                val filter =
                    dataSourceQuery {
                        filter { date("Due Date").onOrBefore(RelativeDateValue.ONE_WEEK_FROM_NOW) }
                    }.filter!!
                serializeFilter(filter) shouldContain """"on_or_before":"one_week_from_now""""
            }

            test("onOrAfter(RelativeDateValue) serializes correctly") {
                val filter =
                    dataSourceQuery {
                        filter { date("Due Date").onOrAfter(RelativeDateValue.ONE_MONTH_AGO) }
                    }.filter!!
                serializeFilter(filter) shouldContain """"on_or_after":"one_month_ago""""
            }
        }

        context("TimestampFilterBuilder relative overloads serialization") {
            fun serializeFilter(filter: DataSourceFilter): String = json.encodeToString(filter)

            test("createdTime().after(RelativeDateValue) serializes correctly") {
                val filter =
                    dataSourceQuery {
                        filter { createdTime().after(RelativeDateValue.ONE_WEEK_AGO) }
                    }.filter!!
                serializeFilter(filter) shouldContain """"after":"one_week_ago""""
            }

            test("lastEditedTime().onOrAfter(RelativeDateValue) serializes correctly") {
                val filter =
                    dataSourceQuery {
                        filter { lastEditedTime().onOrAfter(RelativeDateValue.TODAY) }
                    }.filter!!
                serializeFilter(filter) shouldContain """"on_or_after":"today""""
            }

            test("createdTime().before(RelativeDateValue) serializes correctly") {
                val filter =
                    dataSourceQuery {
                        filter { createdTime().before(RelativeDateValue.ONE_MONTH_FROM_NOW) }
                    }.filter!!
                serializeFilter(filter) shouldContain """"before":"one_month_from_now""""
            }
        }
    })
