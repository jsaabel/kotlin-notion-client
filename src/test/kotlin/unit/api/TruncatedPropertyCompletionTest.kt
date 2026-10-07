package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.DataSourcesApi
import it.saabel.kotlinnotionclient.api.PagesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.getPeopleProperty
import it.saabel.kotlinnotionclient.models.pages.getRelationProperty
import it.saabel.kotlinnotionclient.models.pages.getRelationPropertyPartial
import it.saabel.kotlinnotionclient.serialization.NotionJson
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import unit.util.TestFixtures

/**
 * Notion's page object caps relation (and people) properties at 25 references. The client must
 * never hand back a silently truncated relation (#91): by default it completes the property via
 * *Retrieve a page property item*; when completion is opted out, the accessor fails loudly.
 */
@Tags("Unit")
class TruncatedPropertyCompletionTest :
    StringSpec({

        val pageId = "59833787-2cf9-4fdf-8782-e53db20768a5"
        val relationName = "Recipes"
        val relationPropertyId = "YfIu"
        val truncatedPage = TestFixtures.Pages.retrievePageWithTruncatedRelationAsString()

        fun ref(i: Int) = "00000000-0000-0000-0000-%012x".format(0x1000 + i)

        // ---- mock plumbing ----

        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        fun MockRequestHandleScope.ok(body: String) = respond(body, HttpStatusCode.OK, jsonHeaders)

        fun HttpRequestData.isPropertyItemRequest() = url.encodedPath.contains("/properties/")

        /** Builds a property-item list response from the documented sample, holding refs [range]. */
        fun relationItems(
            range: IntRange,
            nextCursor: String?,
        ): String {
            val sample = TestFixtures.Pages.retrievePagePropertyItemRelationList().jsonObject
            val template =
                sample
                    .getValue("results")
                    .jsonArray
                    .first()
                    .jsonObject
            return JsonObject(
                sample +
                    mapOf(
                        "results" to
                            JsonArray(
                                range.map { i ->
                                    JsonObject(template + ("relation" to buildJsonObject { put("id", ref(i)) }))
                                },
                            ),
                        "next_cursor" to JsonPrimitive(nextCursor),
                        "has_more" to JsonPrimitive(nextCursor != null),
                    ),
            ).toString()
        }

        /** Answers property-item requests: 100 refs on the first page, the rest after `cursor-2`. */
        fun relationPages(total: Int): (HttpRequestData) -> String =
            { request ->
                when (request.url.parameters["start_cursor"]) {
                    null -> relationItems(0 until minOf(100, total), if (total > 100) "cursor-2" else null)
                    "cursor-2" -> relationItems(100 until total, null)
                    else -> error("unexpected cursor")
                }
            }

        fun httpClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
            HttpClient(MockEngine { request -> handler(request) }) {
                install(ContentNegotiation) { json(NotionJson.default) }
            }

        fun config(complete: Boolean = true) = NotionConfig(apiToken = "test-token", completeTruncatedProperties = complete)

        // ========== pages.retrieve ==========

        "retrieve completes a relation with has_more across several property-item pages" {
            val requests = mutableListOf<HttpRequestData>()
            val itemsFor = relationPages(total = 130)
            val api =
                PagesApi(
                    httpClient { request ->
                        requests += request
                        if (request.isPropertyItemRequest()) ok(itemsFor(request)) else ok(truncatedPage)
                    },
                    config(),
                )

            val page = api.retrieve(pageId)

            val relation = page.properties.getValue(relationName) as PageProperty.Relation
            relation.hasMore shouldBe false
            page.getRelationProperty(relationName).map { it.id } shouldBe (0 until 130).map(::ref)

            val itemRequests = requests.filter { it.isPropertyItemRequest() }
            itemRequests shouldHaveSize 2
            itemRequests.forEach {
                it.url.pathSegments.takeLast(4) shouldBe listOf("pages", pageId, "properties", relationPropertyId)
                it.url.parameters["page_size"] shouldBe "100"
            }
            itemRequests[1].url.parameters["start_cursor"] shouldBe "cursor-2"
        }

        "retrieve leaves untruncated properties alone and makes no extra request" {
            var calls = 0
            val api =
                PagesApi(
                    httpClient {
                        calls++
                        ok(TestFixtures.Pages.retrievePageAsString())
                    },
                    config(),
                )

            val page = api.retrieve(pageId)

            calls shouldBe 1
            page.getRelationProperty(relationName) shouldHaveSize 2
        }

        "retrieve never returns a partial relation when completing it fails" {
            val api =
                PagesApi(
                    httpClient { request ->
                        if (request.isPropertyItemRequest()) {
                            respond(
                                """{"object":"error","status":500,"code":"internal_server_error","message":"boom"}""",
                                HttpStatusCode.InternalServerError,
                                jsonHeaders,
                            )
                        } else {
                            ok(truncatedPage)
                        }
                    },
                    config(),
                )

            shouldThrow<NotionException> { api.retrieve(pageId) }
        }

        "retrieve with completion opted out keeps has_more and the accessor fails loudly" {
            val requests = mutableListOf<HttpRequestData>()
            val api =
                PagesApi(
                    httpClient { request ->
                        requests += request
                        ok(truncatedPage)
                    },
                    config(),
                )

            val page = api.retrieve(pageId, completeTruncatedProperties = false)

            requests.filter { it.isPropertyItemRequest() }.shouldBeEmpty()
            (page.properties.getValue(relationName) as PageProperty.Relation).hasMore shouldBe true
            val error = shouldThrow<IllegalStateException> { page.getRelationProperty(relationName) }
            error.message shouldContain "truncated"
            error.message shouldContain relationPropertyId
            page.getRelationPropertyPartial(relationName) shouldHaveSize 25
        }

        "NotionConfig.completeTruncatedProperties = false disables completion client-wide" {
            val requests = mutableListOf<HttpRequestData>()
            val api =
                PagesApi(
                    httpClient { request ->
                        requests += request
                        ok(truncatedPage)
                    },
                    config(complete = false),
                )

            val page = api.retrieve(pageId)

            requests.filter { it.isPropertyItemRequest() }.shouldBeEmpty()
            shouldThrow<IllegalStateException> { page.getRelationProperty(relationName) }
        }

        "a percent-encoded property ID addresses the decoded property" {
            val encodedPage =
                truncatedPage.replace("\"id\": \"$relationPropertyId\"", "\"id\": \"%3AUPp\"")
            var itemPath: List<String> = emptyList()
            val api =
                PagesApi(
                    httpClient { request ->
                        if (request.isPropertyItemRequest()) {
                            itemPath = request.url.pathSegments
                            ok(relationItems(0 until 30, null))
                        } else {
                            ok(encodedPage)
                        }
                    },
                    config(),
                )

            api.retrieve(pageId).getRelationProperty(relationName) shouldHaveSize 30
            itemPath.last() shouldBe ":UPp"
        }

        "create and update responses are completed too" {
            val itemsFor = relationPages(total = 40)
            val api =
                PagesApi(
                    httpClient { request ->
                        if (request.isPropertyItemRequest()) ok(itemsFor(request)) else ok(truncatedPage)
                    },
                    config(),
                )

            api.update(pageId) { properties { checkbox("In stock", true) } }.getRelationProperty(relationName) shouldHaveSize 40
            api
                .create {
                    parent.dataSource("ds-id")
                    properties { title("Name", "x") }
                }.getRelationProperty(relationName) shouldHaveSize 40
        }

        // ========== people (no has_more flag: 25 entries are treated as possibly truncated) ==========

        "a people property with 25 entries is re-read through the property-item endpoint" {
            val page = NotionJson.default.parseToJsonElement(TestFixtures.Pages.retrievePageAsString()).jsonObject
            val people =
                page
                    .getValue("properties")
                    .jsonObject
                    .getValue("Responsible Person")
                    .jsonObject
            val user = people.getValue("people").jsonArray.first()
            val peoplePage =
                JsonObject(
                    page +
                        (
                            "properties" to
                                JsonObject(
                                    page.getValue("properties").jsonObject +
                                        ("Responsible Person" to JsonObject(people + ("people" to JsonArray(List(25) { user })))),
                                )
                        ),
                ).toString()
            val peopleItems =
                buildJsonObject {
                    put("object", "list")
                    put(
                        "results",
                        buildJsonArray {
                            repeat(27) {
                                add(
                                    buildJsonObject {
                                        put("object", "property_item")
                                        put("id", "Iowm")
                                        put("type", "people")
                                        put("people", user)
                                    },
                                )
                            }
                        },
                    )
                    put("has_more", false)
                    put("type", "property_item")
                    put(
                        "property_item",
                        buildJsonObject {
                            put("id", "Iowm")
                            put("type", "people")
                            put("people", JsonObject(emptyMap()))
                        },
                    )
                }.toString()
            val api =
                PagesApi(
                    httpClient { request ->
                        if (request.isPropertyItemRequest()) ok(peopleItems) else ok(peoplePage)
                    },
                    config(),
                )

            api.retrieve(pageId).getPeopleProperty("Responsible Person") shouldHaveSize 27
        }

        // ========== data source queries ==========

        fun queryResponse(nextCursor: String? = null): String {
            val sample = TestFixtures.DataSources.queryDataSource().jsonObject
            val truncated = NotionJson.default.parseToJsonElement(truncatedPage)
            val untruncated = NotionJson.default.parseToJsonElement(TestFixtures.Pages.retrievePageAsString())
            return JsonObject(
                sample +
                    mapOf(
                        "results" to JsonArray(listOf(truncated, untruncated)),
                        "next_cursor" to JsonPrimitive(nextCursor),
                        "has_more" to JsonPrimitive(nextCursor != null),
                    ),
            ).toString()
        }

        fun dataSourcesApi(
            requests: MutableList<HttpRequestData>,
            complete: Boolean = true,
        ): DataSourcesApi {
            val itemsFor = relationPages(total = 130)
            return DataSourcesApi(
                httpClient { request ->
                    requests += request
                    if (request.isPropertyItemRequest()) ok(itemsFor(request)) else ok(queryResponse())
                },
                config(complete),
            )
        }

        "query completes truncated relations on every returned page" {
            val requests = mutableListOf<HttpRequestData>()

            val pages = dataSourcesApi(requests).query("ds-id")

            pages shouldHaveSize 2
            pages[0].getRelationProperty(relationName) shouldHaveSize 130
            pages[1].getRelationProperty(relationName) shouldHaveSize 2
            requests.count { it.isPropertyItemRequest() } shouldBe 2
        }

        "queryAsFlow and collectAllRows complete truncated relations" {
            val requests = mutableListOf<HttpRequestData>()
            val api = dataSourcesApi(requests)

            api.queryAsFlow("ds-id").toList()[0].getRelationProperty(relationName) shouldHaveSize 130
            api.collectAllRows("ds-id")[0].getRelationProperty(relationName) shouldHaveSize 130
        }

        "query with completion opted out returns truncated pages that fail loudly" {
            val requests = mutableListOf<HttpRequestData>()

            val pages = dataSourcesApi(requests).query("ds-id", completeTruncatedProperties = false)

            requests.filter { it.isPropertyItemRequest() }.shouldBeEmpty()
            shouldThrow<IllegalStateException> { pages[0].getRelationProperty(relationName) }
        }

        "queryFirstPage stays a single raw call, and its truncated relation fails loudly" {
            val requests = mutableListOf<HttpRequestData>()

            val response = dataSourcesApi(requests).queryFirstPage("ds-id")

            requests shouldHaveSize 1
            shouldThrow<IllegalStateException> { response.results[0].getRelationProperty(relationName) }
            response.results[0].getRelationPropertyPartial(relationName) shouldHaveSize 25
        }
    })
