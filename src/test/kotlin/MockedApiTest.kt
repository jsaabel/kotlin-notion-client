import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.saabelit.kotlinnotionclient.*
import no.saabelit.kotlinnotionclient.api.DatabasesApi
import no.saabelit.kotlinnotionclient.api.PagesApi
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.databases.Database
import no.saabelit.kotlinnotionclient.models.pages.Page

/**
 * Unit tests using mocked HTTP responses.
 * These tests run fast and don't require network access or real API tokens.
 */
class MockedApiTest :
    StringSpec({

        "Pages API should parse official sample response correctly" {
            val httpClient = mockClient {
                addPageRetrieveResponse()
            }

            val config = NotionConfig(token = "test-token")
            val pagesApi = PagesApi(httpClient, config)

            val page = pagesApi.retrieve("59833787-2cf9-4fdf-8782-e53db20768a5")

            // Test using official sample data - much more comprehensive
            page.id shouldBe "59833787-2cf9-4fdf-8782-e53db20768a5"
            page.objectType shouldBe "page"
            page.url shouldBe "https://www.notion.so/Tuscan-kale-598337872cf94fdf8782e53db20768a5"
            page.archived shouldBe false
            page.createdBy?.id shouldBe "ee5f0f84-409a-440f-983a-a5315961c6e4"
            page.lastEditedBy?.id shouldBe "0c3e9826-b8f7-4f73-927d-2caaf86f1103"
            page.icon shouldNotBe null
            page.cover shouldNotBe null
            
            // Test complex properties from official sample - verify they exist
            page.properties.containsKey("Name") shouldBe true
            page.properties.containsKey("Food group") shouldBe true
            page.properties.containsKey("Store availability") shouldBe true
            page.properties.containsKey("Price") shouldBe true
            page.properties.containsKey("Description") shouldBe true
            page.properties.containsKey("Recipes") shouldBe true
            page.properties.containsKey("Number of meals") shouldBe true

            httpClient.close()
        }

        "Databases API should parse official sample response correctly" {
            val httpClient = mockClient {
                addDatabaseRetrieveResponse()
            }

            val config = NotionConfig(token = "test-token")
            val databasesApi = DatabasesApi(httpClient, config)

            val database = databasesApi.retrieve("bc1211ca-e3f1-4939-ae34-5260b16f627c")

            // Test using official sample data - much more comprehensive
            database.id shouldBe "bc1211ca-e3f1-4939-ae34-5260b16f627c"
            database.objectType shouldBe "database"
            database.url shouldBe "https://www.notion.so/bc1211cae3f14939ae34260b16f627c"
            database.archived shouldBe false
            database.isInline shouldBe false
            database.title.first().plainText shouldBe "Grocery List"
            database.description.first().plainText shouldBe "Grocery list for just kale 🥬"
            
            // Test comprehensive property schema from official sample - verify they exist
            database.properties.containsKey("+1") shouldBe true
            database.properties.containsKey("In stock") shouldBe true
            database.properties.containsKey("Price") shouldBe true
            database.properties.containsKey("Description") shouldBe true
            database.properties.containsKey("Last ordered") shouldBe true
            database.properties.containsKey("Meals") shouldBe true
            database.properties.containsKey("Number of meals") shouldBe true
            database.properties.containsKey("Store availability") shouldBe true
            database.properties.containsKey("Photo") shouldBe true
            database.properties.containsKey("Food group") shouldBe true
            database.properties.containsKey("Name") shouldBe true
            
            // Test that we have all the complex properties
            database.properties.size shouldBe 11

            httpClient.close()
        }

        "Pages API should handle 404 error correctly" {
            val httpClient = mockClient {
                addErrorResponse(
                    HttpMethod.Get,
                    "*/v1/pages/*",
                    HttpStatusCode.NotFound,
                    "Could not find page with ID: invalid-id"
                )
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
            val httpClient = mockClient {
                addErrorResponse(
                    HttpMethod.Get,
                    "*/v1/databases/*",
                    HttpStatusCode.BadRequest,
                    "Invalid database ID format"
                )
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

        "MockPresets should provide convenient test setups" {
            // Test the standard CRUD operations preset
            val httpClient = MockPresets.standardCrudOperations()

            val config = NotionConfig(token = "test-token")
            val pagesApi = PagesApi(httpClient, config)
            val databasesApi = DatabasesApi(httpClient, config)

            // Should work with any ID thanks to wildcard matching
            val page = pagesApi.retrieve("any-page-id")
            page.objectType shouldBe "page"

            val database = databasesApi.retrieve("any-database-id")
            database.objectType shouldBe "database"

            httpClient.close()
        }

        "TestFixtures should provide easy access to sample data" {
            // Test that we can load samples directly
            val pageJson = TestFixtures.Pages.retrievePage()
            pageJson.toString().isNotEmpty() shouldBe true

            val databaseJson = TestFixtures.Databases.retrieveDatabase()
            databaseJson.toString().isNotEmpty() shouldBe true

            // Test direct decoding
            val page: Page = TestFixtures.Pages.retrievePage().decode()
            page.objectType shouldBe "page"

            val database: Database = TestFixtures.Databases.retrieveDatabase().decode()
            database.objectType shouldBe "database"
        }
    })
