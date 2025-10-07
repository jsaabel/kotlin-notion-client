package unit.query

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryRequest
import unit.util.mockClient

/**
 * Basic data source query functionality tests (2025-09-03 API).
 *
 * Tests core query operations without complex filtering or builder patterns.
 * In 2025-09-03, queries target data sources instead of databases.
 */
@Tags("Unit")
class DatabaseQueryBasicTest :
    StringSpec({

        fun createMockClient() =
            mockClient {
                addDataSourceQueryResponse()
            }

        "Should query data source with no parameters and return all pages" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages = client.dataSources.query("test-data-source-id")

            pages.shouldNotBeEmpty()
            // The API now handles pagination automatically and returns all pages
        }

        "Should query data source with basic pagination" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query = DatabaseQueryRequest(pageSize = 50)
            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should query data source with start cursor for pagination" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DatabaseQueryRequest(
                    startCursor = "test-cursor-123",
                    pageSize = 25,
                )
            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }
    })
