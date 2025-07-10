import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.saabelit.kotlinnotionclient.api.DatabasesApi
import no.saabelit.kotlinnotionclient.api.PagesApi
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException

/**
 * Unit tests using mocked HTTP responses.
 * These tests run fast and don't require network access or real API tokens.
 */
class MockedApiTest :
    StringSpec({

        "Pages API should parse valid JSON response correctly" {
            val pageJson =
                this::class.java.classLoader
                    .getResource("sample-page-response.json")!!
                    .readText()

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = pageJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val config = NotionConfig(token = "test-token")
            val pagesApi = PagesApi(httpClient, config)

            val page = pagesApi.retrieve("22bc63fd-82ed-80da-bbe4-d340cd1b97c7")

            page.id shouldBe "22bc63fd-82ed-80da-bbe4-d340cd1b97c7"
            page.objectType shouldBe "page"
            page.url shouldBe "https://www.notion.so/Test-Page-Title-22bc63fd82ed80dabbe4d340cd1b97c7"
            page.archived shouldBe false
            page.inTrash shouldBe false
            page.createdBy.name shouldBe "Test User"
            page.lastEditedBy.name shouldBe "Test User"
            page.icon shouldNotBe null
            page.cover shouldBe null

            httpClient.close()
        }

        "Databases API should parse valid JSON response correctly" {
            val databaseJson =
                this::class.java.classLoader
                    .getResource("sample-database-response.json")!!
                    .readText()

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = databaseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val config = NotionConfig(token = "test-token")
            val databasesApi = DatabasesApi(httpClient, config)

            val database = databasesApi.retrieve("22bc63fd-82ed-80d6-a648-d1433b382457")

            database.id shouldBe "22bc63fd-82ed-80d6-a648-d1433b382457"
            database.objectType shouldBe "database"
            database.url shouldBe "https://www.notion.so/22bc63fd82ed80d6a648d1433b382457"
            database.archived shouldBe false
            database.inTrash shouldBe false
            database.isInline shouldBe false
            database.title.first().plainText shouldBe "Test Database"
            database.description.isEmpty() shouldBe true
            database.properties.size shouldBe 3
            database.properties["Name"]?.type shouldBe "title"
            database.properties["Status"]?.type shouldBe "select"
            database.properties["Created"]?.type shouldBe "created_time"

            httpClient.close()
        }

        "Pages API should handle 404 error correctly" {
            // TODO: Make sure this matches / is representative of actual response
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content =
                            """{"object": "error", "status": 404, "code": "object_not_found", """ +
                                """"message": "Could not find page with ID: invalid-id"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val config = NotionConfig(token = "test-token")
            val pagesApi = PagesApi(httpClient, config)

            var caughtException: NotionException.ApiError? = null
            try {
                pagesApi.retrieve("invalid-id")
            } catch (e: NotionException.ApiError) {
                caughtException = e
            }

            caughtException shouldNotBe null
            caughtException?.status shouldBe 404
            caughtException?.code shouldBe "404"

            httpClient.close()
        }

        "Databases API should handle 400 error correctly" {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content =
                            """{"object": "error", "status": 400, "code": "validation_error", """ +
                                """"message": "Invalid database ID format"}""",
                        status = HttpStatusCode.BadRequest,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val config = NotionConfig(token = "test-token")
            val databasesApi = DatabasesApi(httpClient, config)

            var caughtException: NotionException.ApiError? = null
            try {
                databasesApi.retrieve("invalid-format")
            } catch (e: NotionException.ApiError) {
                caughtException = e
            }

            caughtException shouldNotBe null
            caughtException?.status shouldBe 400
            caughtException?.code shouldBe "400"

            httpClient.close()
        }

        "APIs should handle network errors correctly" {
            val mockEngine =
                MockEngine { request ->
                    throw Exception("Network connection failed")
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val config = NotionConfig(token = "test-token")
            val pagesApi = PagesApi(httpClient, config)

            var caughtException: NotionException.NetworkError? = null
            try {
                pagesApi.retrieve("any-id")
            } catch (e: NotionException.NetworkError) {
                caughtException = e
            }

            caughtException shouldNotBe null
            caughtException?.cause?.message shouldBe "Network connection failed"

            httpClient.close()
        }
    })
