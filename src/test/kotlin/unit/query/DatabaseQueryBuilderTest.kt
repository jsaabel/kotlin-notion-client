package unit.query

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryBuilder
import no.saabelit.kotlinnotionclient.models.databases.SortDirection
import unit.util.mockClient

/**
 * Tests for the DatabaseQueryBuilder DSL functionality (2025-09-03 API).
 *
 * Focuses on the builder pattern and DSL for constructing complex queries.
 * In 2025-09-03, queries target data sources instead of databases.
 */
@Tags("Unit")
class DatabaseQueryBuilderTest :
    StringSpec({

        fun createMockClient() =
            mockClient {
                addDataSourceQueryResponse()
            }

        "Should build query with title filter" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DatabaseQueryBuilder()
                    .filter {
                        title("Name").contains("Important")
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with sorting" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DatabaseQueryBuilder()
                    .sortBy("Priority", SortDirection.DESCENDING)
                    .sortBy("Name", SortDirection.ASCENDING)
                    .build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with timestamp sorting" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DatabaseQueryBuilder()
                    .sortByTimestamp("created_time", SortDirection.DESCENDING)
                    .sortByTimestamp("last_edited_time", SortDirection.ASCENDING)
                    .build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with pagination parameters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DatabaseQueryBuilder()
                    .startCursor("test-cursor-123")
                    .pageSize(10)
                    .build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should clamp page size to valid limits" {
            val queryOverLimit =
                DatabaseQueryBuilder()
                    .pageSize(150) // Over the 100 limit
                    .build()

            queryOverLimit.pageSize shouldBe 100

            val queryUnderLimit =
                DatabaseQueryBuilder()
                    .pageSize(0) // Under the 1 minimum
                    .build()

            queryUnderLimit.pageSize shouldBe 1
        }

        "Should build query with filter and sorting combined" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DatabaseQueryBuilder()
                    .filter {
                        title("Name").contains("Project")
                    }.sortBy("Priority", SortDirection.DESCENDING)
                    .pageSize(25)
                    .build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }
    })
