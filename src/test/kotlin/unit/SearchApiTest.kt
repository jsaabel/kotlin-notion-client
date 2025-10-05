package unit

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.search.searchRequest
import unit.util.mockClient

@Tags("Unit")
class SearchApiTest :
    FunSpec({
        test("search with query should return results") {
            val client =
                NotionClient.createWithClient(
                    mockClient { addSearchResponse() },
                    NotionConfig(apiToken = "test-token"),
                )

            val response = client.search.search("Tuscan kale")

            response.objectType shouldBe "list"
            response.results shouldNotBe null
            response.results.size shouldBe 2
            response.hasMore shouldBe false
            response.nextCursor shouldBe null
        }

        test("search with filter should apply filter") {
            val client =
                NotionClient.createWithClient(
                    mockClient { addSearchResponse() },
                    NotionConfig(apiToken = "test-token"),
                )

            val request =
                searchRequest {
                    query("test")
                    filterPages()
                }

            val response = client.search.search(request)

            response.results shouldNotBe null
        }

        test("search with sort and pagination should work") {
            val client =
                NotionClient.createWithClient(
                    mockClient { addSearchResponse() },
                    NotionConfig(apiToken = "test-token"),
                )

            val request =
                searchRequest {
                    sortDescending()
                    pageSize(50)
                }

            val response = client.search.search(request)

            response.results shouldNotBe null
        }

        test("search DSL should support data source filter") {
            val client =
                NotionClient.createWithClient(
                    mockClient { addSearchResponse() },
                    NotionConfig(apiToken = "test-token"),
                )

            val request =
                searchRequest {
                    query("database")
                    filterDataSources()
                    sortAscending()
                }

            val response = client.search.search(request)

            response shouldNotBe null
        }

        test("empty search should return all accessible content") {
            val client =
                NotionClient.createWithClient(
                    mockClient { addSearchResponse() },
                    NotionConfig(apiToken = "test-token"),
                )

            val response = client.search.search()

            response.objectType shouldBe "list"
            response.results shouldNotBe null
        }
    })
