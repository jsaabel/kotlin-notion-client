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
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.DataSourcesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import unit.util.TestFixtures

/**
 * Tests for the `filter_properties` query parameter and `is_archived` convenience overloads on
 * [DataSourcesApi.query] / [DataSourcesApi.queryAsFlow] / [DataSourcesApi.queryFirstPage].
 *
 * Mirrors [PagesFilterPropertiesTest] — `filter_properties` is a repeated query parameter capped
 * at 100 IDs by the API, following the same pattern PagesApi established.
 */
private fun dataSourcesApi(handler: MockRequestHandler): DataSourcesApi {
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
    return DataSourcesApi(httpClient, NotionConfig(apiToken = "test-token"))
}

@Tags("Unit")
class DataSourcesFilterPropertiesTest :
    StringSpec({

        val queryFixture = TestFixtures.DataSources.queryDataSourceAsString()

        fun MockRequestHandleScope.okResponse() =
            respond(
                content = queryFixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )

        // ========== filter_properties ==========

        "query without filterProperties omits the query parameter" {
            var query = ""
            val api =
                dataSourcesApi { request ->
                    query = request.url.encodedQuery
                    okResponse()
                }

            api.query("ds-id")

            query shouldBe ""
        }

        "query with filterProperties adds one repeated query parameter per property ID" {
            var params = emptyList<String>()
            var method = HttpMethod.Get
            val api =
                dataSourcesApi { request ->
                    method = request.method
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            api.query("ds-id", filterProperties = listOf("%7DVpb", "title")) {}

            method shouldBe HttpMethod.Post
            params shouldBe listOf("}Vpb", "title")
        }

        "queryFirstPage forwards filterProperties" {
            var params = emptyList<String>()
            val api =
                dataSourcesApi { request ->
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            api.queryFirstPage("ds-id", filterProperties = listOf("iAk8"))

            params shouldBe listOf("iAk8")
        }

        "queryAsFlow forwards filterProperties" {
            var params = emptyList<String>()
            val api =
                dataSourcesApi { request ->
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }

            api.queryAsFlow("ds-id", filterProperties = listOf("iAk8")) {}.toList()

            params shouldBe listOf("iAk8")
        }

        "query rejects more than 100 filterProperties IDs" {
            val api = dataSourcesApi { okResponse() }
            val tooMany = (1..101).map { "prop-$it" }

            shouldThrow<IllegalArgumentException> {
                api.query("ds-id", filterProperties = tooMany) {}
            }
        }

        "query accepts exactly 100 filterProperties IDs" {
            var params = emptyList<String>()
            val api =
                dataSourcesApi { request ->
                    params =
                        request.url.parameters
                            .getAll("filter_properties")
                            .orEmpty()
                    okResponse()
                }
            val exactly100 = (1..100).map { "prop-$it" }

            api.query("ds-id", filterProperties = exactly100) {}

            params.size shouldBe 100
        }

        // ========== is_archived convenience overloads ==========

        "query(dataSourceId, isArchived) sends is_archived in the body" {
            var body = ""
            val api =
                dataSourcesApi { request ->
                    body = request.body.toByteArray().decodeToString()
                    okResponse()
                }

            api.query("ds-id", isArchived = true)

            body.contains("\"is_archived\":true") shouldBe true
        }

        "queryFirstPage(dataSourceId, isArchived) sends is_archived in the body" {
            var body = ""
            val api =
                dataSourcesApi { request ->
                    body = request.body.toByteArray().decodeToString()
                    okResponse()
                }

            api.queryFirstPage("ds-id", isArchived = true)

            body.contains("\"is_archived\":true") shouldBe true
        }

        "queryAsFlow(dataSourceId, isArchived) sends is_archived in the body" {
            var body = ""
            val api =
                dataSourcesApi { request ->
                    body = request.body.toByteArray().decodeToString()
                    okResponse()
                }

            api.queryAsFlow("ds-id", isArchived = false).toList()

            body.contains("\"is_archived\":false") shouldBe true
        }
    })
