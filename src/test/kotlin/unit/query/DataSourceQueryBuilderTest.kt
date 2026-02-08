package unit.query

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryBuilder
import it.saabel.kotlinnotionclient.models.datasources.SortDirection
import unit.util.mockClient

/**
 * Tests for the DatabaseQueryBuilder DSL functionality (2025-09-03 API).
 *
 * Focuses on the builder pattern and DSL for constructing complex queries.
 * In 2025-09-03, queries target data sources instead of databases.
 */
@Tags("Unit")
class DataSourceQueryBuilderTest :
    StringSpec({

        fun createMockClient() =
            mockClient {
                addDataSourceQueryResponse()
            }

        "Should build query with title filter" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        title("Name").contains("Important")
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with sorting" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .sortBy("Priority", SortDirection.DESCENDING)
                    .sortBy("Name", SortDirection.ASCENDING)
                    .build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with timestamp sorting" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .sortByTimestamp("created_time", SortDirection.DESCENDING)
                    .sortByTimestamp("last_edited_time", SortDirection.ASCENDING)
                    .build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with pagination parameters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .startCursor("test-cursor-123")
                    .pageSize(10)
                    .build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should clamp page size to valid limits" {
            val queryOverLimit =
                DataSourceQueryBuilder()
                    .pageSize(150) // Over the 100 limit
                    .build()

            queryOverLimit.pageSize shouldBe 100

            val queryUnderLimit =
                DataSourceQueryBuilder()
                    .pageSize(0) // Under the 1 minimum
                    .build()

            queryUnderLimit.pageSize shouldBe 1
        }

        "Should build query with filter and sorting combined" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        title("Name").contains("Project")
                    }.sortBy("Priority", SortDirection.DESCENDING)
                    .pageSize(25)
                    .build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with created_time timestamp filter" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        createdTime().after("2024-01-01")
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with last_edited_time timestamp filter" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        lastEditedTime().onOrBefore("2024-12-31")
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query combining timestamp filter with property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            createdTime().after("2024-01-01"),
                            title("Name").contains("Important"),
                            checkbox("Completed").equals(false),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with timestamp filter using relative date methods" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val queryPastWeek =
                DataSourceQueryBuilder()
                    .filter {
                        createdTime().pastWeek()
                    }.build()

            val pagesPastWeek = client.dataSources.query("test-data-source-id", queryPastWeek)
            pagesPastWeek.shouldNotBeEmpty()

            val queryNextMonth =
                DataSourceQueryBuilder()
                    .filter {
                        lastEditedTime().nextMonth()
                    }.build()

            val pagesNextMonth = client.dataSources.query("test-data-source-id", queryNextMonth)
            pagesNextMonth.shouldNotBeEmpty()
        }
    })
