package query

import TestFixtures
import decode
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import mockClient
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryRequest
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryResponse

/**
 * Basic database query functionality tests.
 *
 * Tests core query operations without complex filtering or builder patterns.
 */
@Tags("Unit")
class DatabaseQueryBasicTest :
    StringSpec({

        fun createMockClient() =
            mockClient {
                addDatabaseQueryResponse()
            }

        "Should query database with no parameters and return all pages" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages = client.databases.query("test-database-id")

            pages.shouldNotBeEmpty()
            // The API now handles pagination automatically and returns all pages
        }

        "Should query database with basic pagination" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query = DatabaseQueryRequest(pageSize = 50)
            val pages = client.databases.query("test-database-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should query database with start cursor for pagination" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DatabaseQueryRequest(
                    startCursor = "test-cursor-123",
                    pageSize = 25,
                )
            val pages = client.databases.query("test-database-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should deserialize official sample response correctly" {
            val response = TestFixtures.Databases.queryDatabase().decode<DatabaseQueryResponse>()

            response.objectType shouldBe "list"
            response.results.shouldNotBeEmpty()
            response.results.first().id shouldNotBe null
            response.type shouldBe "page_or_database"
            response.hasMore shouldNotBe null
        }

        "Should handle pagination structure from API response" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages = client.databases.query("test-database-id")

            // Verify we get pages back
            pages.shouldNotBeEmpty()
            // The API now handles pagination automatically
        }

        "Should create empty query request with all null optional fields" {
            val query = DatabaseQueryRequest()

            query.filter shouldBe null
            query.sorts shouldBe null
            query.startCursor shouldBe null
            query.pageSize shouldBe null
        }
    })
