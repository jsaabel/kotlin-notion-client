package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.PagesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import it.saabel.kotlinnotionclient.models.pages.updatePageRequest
import kotlinx.serialization.json.Json
import unit.util.TestFixtures

/**
 * Tests for the `filter_properties` query parameter on page create, update and retrieve.
 *
 * `filter_properties` restricts which page properties come back in the response. It is a
 * repeated query parameter carrying property IDs. Property IDs are exposed by Notion in two
 * shapes — percent-encoded in the data source schema (e.g. `%7DVpb`) and decoded in view
 * filter/sort responses (e.g. `}Vpb`) — so both shapes must produce the same request.
 */
private fun pagesApi(handler: MockRequestHandler): PagesApi {
    val engine = MockEngine { request -> handler(request) }
    val httpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                        explicitNulls = false
                    },
                )
            }
        }
    return PagesApi(httpClient, NotionConfig(apiToken = "test-token"))
}

@Tags("Unit")
class PagesFilterPropertiesTest :
    StringSpec({

        val pageFixture = TestFixtures.Pages.retrievePageAsString()

        fun MockRequestHandleScope.okResponse() =
            respond(
                content = pageFixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )

        // ========== retrieve ==========

        "retrieve without filterProperties omits the query parameter" {
            var query = ""
            val api =
                pagesApi { request ->
                    query = request.url.encodedQuery
                    okResponse()
                }

            api.retrieve("page-id")

            query shouldBe ""
        }

        "retrieve with filterProperties adds one repeated query parameter per property ID" {
            var params = emptyList<String>()
            var method = HttpMethod.Post
            val api =
                pagesApi { request ->
                    method = request.method
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            api.retrieve("page-id", filterProperties = listOf("iAk8", "title"))

            method shouldBe HttpMethod.Get
            params shouldBe listOf("iAk8", "title")
        }

        "retrieve with an empty filterProperties list omits the query parameter" {
            var query = ""
            val api =
                pagesApi { request ->
                    query = request.url.encodedQuery
                    okResponse()
                }

            api.retrieve("page-id", filterProperties = emptyList())

            query shouldBe ""
        }

        "retrieve percent-encodes property IDs containing URL-unsafe characters" {
            var encodedQuery = ""
            var params = emptyList<String>()
            val api =
                pagesApi { request ->
                    encodedQuery = request.url.encodedQuery
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            api.retrieve("page-id", filterProperties = listOf("}Vpb", "ue\\l"))

            params shouldBe listOf("}Vpb", "ue\\l")
            encodedQuery.contains("%7DVpb") shouldBe true
            encodedQuery.contains("ue%5Cl") shouldBe true
        }

        "retrieve accepts already percent-encoded property IDs without double-encoding them" {
            var encodedQuery = ""
            var params = emptyList<String>()
            val api =
                pagesApi { request ->
                    encodedQuery = request.url.encodedQuery
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            // Shape returned by the data source schema for the same properties as above.
            api.retrieve("page-id", filterProperties = listOf("%7DVpb", "ue%5Cl"))

            params shouldBe listOf("}Vpb", "ue\\l")
            encodedQuery.contains("%257D") shouldBe false
            encodedQuery.contains("%255C") shouldBe false
        }

        "retrieve leaves a property ID with a stray percent sign untouched" {
            var params = emptyList<String>()
            val api =
                pagesApi { request ->
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            api.retrieve("page-id", filterProperties = listOf("100%done"))

            params shouldBe listOf("100%done")
        }

        "retrieve rejects more than 100 filterProperties IDs" {
            val api = pagesApi { okResponse() }
            val tooMany = (1..101).map { "prop-$it" }

            shouldThrow<IllegalArgumentException> {
                api.retrieve("page-id", filterProperties = tooMany)
            }
        }

        "retrieve accepts exactly 100 filterProperties IDs" {
            var params = emptyList<String>()
            val api =
                pagesApi { request ->
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }
            val exactly100 = (1..100).map { "prop-$it" }

            api.retrieve("page-id", filterProperties = exactly100)

            params.size shouldBe 100
        }

        // ========== create ==========

        "create with filterProperties adds the query parameter to the POST" {
            var params = emptyList<String>()
            var method = HttpMethod.Get
            var path = ""
            val api =
                pagesApi { request ->
                    method = request.method
                    path = request.url.encodedPath
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            val request =
                createPageRequest {
                    parent.page("parent-id")
                    title("Test Page")
                }
            api.create(request, filterProperties = listOf("%7DVpb", "title"))

            method shouldBe HttpMethod.Post
            path shouldBe "/v1/pages"
            params shouldBe listOf("}Vpb", "title")
        }

        "create without filterProperties omits the query parameter" {
            var query = ""
            val api =
                pagesApi { request ->
                    query = request.url.encodedQuery
                    okResponse()
                }

            api.create(
                createPageRequest {
                    parent.page("parent-id")
                    title("Test Page")
                },
            )

            query shouldBe ""
        }

        "create DSL overload forwards filterProperties" {
            var params = emptyList<String>()
            val api =
                pagesApi { request ->
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            api.create(filterProperties = listOf("iAk8")) {
                parent.page("parent-id")
                title("Test Page")
            }

            params shouldBe listOf("iAk8")
        }

        // ========== update ==========

        "update with filterProperties adds the query parameter to the PATCH" {
            var params = emptyList<String>()
            var method = HttpMethod.Get
            var path = ""
            val api =
                pagesApi { request ->
                    method = request.method
                    path = request.url.encodedPath
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            val request =
                updatePageRequest {
                    properties {
                        checkbox("Done", true)
                    }
                }
            api.update("page-id", request, filterProperties = listOf("ue%5Cl"))

            method shouldBe HttpMethod.Patch
            path shouldBe "/v1/pages/page-id"
            params shouldBe listOf("ue\\l")
        }

        "update DSL overload forwards filterProperties" {
            var params = emptyList<String>()
            val api =
                pagesApi { request ->
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            api.update("page-id", filterProperties = listOf("iAk8")) {
                properties {
                    checkbox("Done", true)
                }
            }

            params shouldBe listOf("iAk8")
        }

        "update without filterProperties omits the query parameter" {
            var query = ""
            val api =
                pagesApi { request ->
                    query = request.url.encodedQuery
                    okResponse()
                }

            api.update("page-id") {
                properties {
                    checkbox("Done", true)
                }
            }

            query shouldBe ""
        }
    })
