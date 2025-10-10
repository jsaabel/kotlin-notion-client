package unit.api

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.BlocksApi
import it.saabel.kotlinnotionclient.api.CommentsApi
import it.saabel.kotlinnotionclient.api.DatabasesApi
import it.saabel.kotlinnotionclient.api.PagesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.base.Color
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.comments.CommentList
import it.saabel.kotlinnotionclient.models.pages.Page
import kotlinx.serialization.json.Json
import unit.util.MockPresets
import unit.util.TestFixtures
import unit.util.decode
import unit.util.mockClient

/**
 * Unit tests using mocked HTTP responses.
 * These tests run fast and don't require network access or real API tokens.
 */
@Tags("Unit")
class MockedApiTest :
    StringSpec({

        "Pages API should parse official sample response correctly" {
            val httpClient =
                mockClient {
                    addPageRetrieveResponse()
                }

            val config = NotionConfig(apiToken = "test-token")
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

        "Databases API should parse official sample response correctly (2025-09-03 API)" {
            val httpClient =
                mockClient {
                    addDatabaseRetrieveResponse()
                }

            val config = NotionConfig(apiToken = "test-token")
            val databasesApi = DatabasesApi(httpClient, config)

            val database = databasesApi.retrieve("248104cd-477e-80fd-b757-e945d38000bd")

            // Test using official 2025-09-03 sample data
            database.id shouldBe "248104cd-477e-80fd-b757-e945d38000bd"
            database.objectType shouldBe "database"
            database.archived shouldBe false
            database.isInline shouldBe false
            database.title.first().plainText shouldBe "My Task Tracker"

            // Verify data sources array (new in 2025-09-03)
            database.dataSources.shouldNotBe(null)
            database.dataSources.size shouldBe 1
            database.dataSources.first().id shouldBe "248104cd-477e-80af-bc30-000bd28de8f9"
            database.dataSources.first().name shouldBe "My Task Tracker"

            httpClient.close()
        }

        "Pages API should handle 404 error correctly" {
            val httpClient =
                mockClient {
                    addErrorResponse(
                        HttpMethod.Get,
                        "*/v1/pages/*",
                        HttpStatusCode.NotFound,
                        "Could not find page with ID: invalid-id",
                    )
                }

            val config = NotionConfig(apiToken = "test-token")
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
            val httpClient =
                mockClient {
                    addErrorResponse(
                        HttpMethod.Get,
                        "*/v1/databases/*",
                        HttpStatusCode.BadRequest,
                        "Invalid database ID format",
                    )
                }

            val config = NotionConfig(apiToken = "test-token")
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
                MockEngine.Companion { request ->
                    throw Exception("Network connection failed")
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                            },
                        )
                    }
                }

            val config = NotionConfig(apiToken = "test-token")
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

        "unit.util.MockPresets should provide convenient test setups" {
            // Test the standard CRUD operations preset
            val httpClient = MockPresets.standardCrudOperations()

            val config = NotionConfig(apiToken = "test-token")
            val pagesApi = PagesApi(httpClient, config)
            val databasesApi = DatabasesApi(httpClient, config)

            // Should work with any ID thanks to wildcard matching
            val page = pagesApi.retrieve("any-page-id")
            page.objectType shouldBe "page"

            val database = databasesApi.retrieve("any-database-id")
            database.objectType shouldBe "database"

            httpClient.close()
        }

        "Blocks API should parse official sample response correctly" {
            val httpClient =
                mockClient {
                    addBlockRetrieveResponse()
                }

            val config = NotionConfig(apiToken = "test-token")
            val blocksApi = BlocksApi(httpClient, config)

            val block = blocksApi.retrieve("c02fc1d3-db8b-45c5-a222-27595b15aea7")

            // Test using official sample data
            block.id shouldBe "c02fc1d3-db8b-45c5-a222-27595b15aea7"
            block.objectType shouldBe "block"
            block.type shouldBe "heading_2"
            block.hasChildren shouldBe false
            block.archived shouldBe false

            // Test specific heading_2 content
            if (block is Block.Heading2) {
                block.heading2.richText
                    .first()
                    .plainText shouldBe "Lacinato kale"
                block.heading2.color shouldBe Color.DEFAULT
                block.heading2.isToggleable shouldBe false
            }

            httpClient.close()
        }

        "Blocks API should retrieve children correctly" {
            val httpClient =
                mockClient {
                    addBlockChildrenRetrieveResponse()
                }

            val config = NotionConfig(apiToken = "test-token")
            val blocksApi = BlocksApi(httpClient, config)

            val blocks = blocksApi.retrieveChildren("parent-block-id")

            // Test using official sample data
            blocks.size shouldBe 2 // heading_2 and paragraph

            // Test first block (heading_2)
            val heading = blocks[0]
            heading.type shouldBe "heading_2"

            // Test second block (paragraph)
            val paragraph = blocks[1]
            paragraph.type shouldBe "paragraph"

            httpClient.close()
        }

        "Comments API should parse official sample response correctly" {
            val httpClient =
                mockClient {
                    addCommentsRetrieveResponse()
                }

            val config = NotionConfig(apiToken = "test-token")
            val commentsApi = CommentsApi(httpClient, config)

            val comments = commentsApi.retrieve("block-id")

            // Test using official sample data
            comments.isNotEmpty() shouldBe true

            // Test first comment
            val comment = comments.first()
            comment.objectType shouldBe "comment"
            comment.id shouldBe "94cc56ab-9f02-409d-9f99-1037e9fe502f"
            comment.discussionId shouldBe "f1407351-36f5-4c49-a13c-49f8ba11776d"
            comment.richText.first().plainText shouldBe "Single comment"

            httpClient.close()
        }

        "unit.util.TestFixtures should provide easy access to sample data" {
            // Test that we can load samples directly
            val pageJson = TestFixtures.Pages.retrievePage()
            pageJson.toString().isNotEmpty() shouldBe true

            val databaseJson = TestFixtures.Databases.retrieveDatabase()
            databaseJson.toString().isNotEmpty() shouldBe true

            val blockJson = TestFixtures.Blocks.retrieveBlock()
            blockJson.toString().isNotEmpty() shouldBe true

            val commentsJson = TestFixtures.Comments.retrieveComments()
            commentsJson.toString().isNotEmpty() shouldBe true

            // Test direct decoding
            val page: Page = TestFixtures.Pages.retrievePage().decode()
            page.objectType shouldBe "page"

            val database: it.saabel.kotlinnotionclient.models.databases.Database = TestFixtures.Databases.retrieveDatabase().decode()
            database.objectType shouldBe "database"

            val block: Block = TestFixtures.Blocks.retrieveBlock().decode()
            block.objectType shouldBe "block"

            val commentList: CommentList = TestFixtures.Comments.retrieveComments().decode()
            commentList.objectType shouldBe "list"
        }
    })
