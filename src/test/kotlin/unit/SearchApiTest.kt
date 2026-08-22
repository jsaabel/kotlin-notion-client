package unit

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.search.searchRequest
import kotlinx.serialization.json.Json
import unit.util.TestFixtures
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

        test("search with query and inTrash should apply a trash filter without a spurious property field") {
            val client =
                NotionClient.createWithClient(
                    mockClient { addSearchResponse() },
                    NotionConfig(apiToken = "test-token"),
                )

            val response = client.search.search("Tuscan kale", inTrash = true)

            response.results shouldNotBe null
        }

        test("search with query and inTrash sends in_trash without a spurious property field on the wire") {
            var capturedBody = ""
            val engine =
                MockEngine { request ->
                    capturedBody = request.body.toByteArray().decodeToString()
                    respond(
                        content = TestFixtures.Search.searchByTitleAsString(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val client =
                NotionClient.createWithClient(
                    HttpClient(engine) {
                        install(ContentNegotiation) {
                            json(Json { ignoreUnknownKeys = true })
                        }
                    },
                    NotionConfig(apiToken = "test-token"),
                )

            client.search.search("Tuscan kale", inTrash = true)

            capturedBody.contains("\"in_trash\":true") shouldBe true
            capturedBody.contains("\"property\"") shouldBe false
        }
    })
