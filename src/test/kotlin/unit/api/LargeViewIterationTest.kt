package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
import it.saabel.kotlinnotionclient.exceptions.NotionException
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.ktor.client.HttpClient as KtorHttpClient

/**
 * Tests for [it.saabel.kotlinnotionclient.api.ViewsApi.iterateAllRows] and
 * [it.saabel.kotlinnotionclient.api.ViewsApi.collectAllRows].
 *
 * A view query is a cached, already-capped result set and takes no filter, so it cannot
 * be windowed. Per Notion's "Query large data sources" guide the drain instead resolves
 * the view to its data source and re-uses the view's filter — these tests assert exactly
 * that request shape.
 *
 * The view fixture is the repo's official-shaped `views/retrieve_view.json`; variants
 * carrying a filter or no `data_source_id` are hand-crafted here, since the published
 * samples show neither.
 */
@Tags("Unit")
class LargeViewIterationTest :
    FunSpec({

        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        val viewId = "a3f1b2c4-5678-4def-abcd-1234567890ab"
        val dataSourceId = "248104cd-477e-80af-bc30-000bd28de8f9"

        fun viewJson(
            filter: String? = null,
            dataSource: String? = dataSourceId,
        ): String {
            val dataSourceJson = dataSource?.let { "\"$it\"" } ?: "null"
            val filterJson = filter ?: "null"
            return """
                {
                  "object": "view",
                  "id": "$viewId",
                  "parent": {"type": "database_id", "database_id": "248104cd-477e-80fd-b757-e945d38000bd"},
                  "name": "All tasks",
                  "type": "table",
                  "created_time": "2026-03-01T12:00:00.000Z",
                  "last_edited_time": "2026-04-01T09:00:00.000Z",
                  "url": "https://www.notion.so/$viewId",
                  "data_source_id": $dataSourceJson,
                  "filter": $filterJson,
                  "sorts": [{"property": "Name", "direction": "descending"}]
                }
                """.trimIndent()
        }

        fun pageObject(
            id: String,
            createdTime: String,
        ) = """
            {
              "object": "page",
              "id": "$id",
              "created_time": "$createdTime",
              "last_edited_time": "$createdTime",
              "created_by": {"object":"user","id":"u-1"},
              "last_edited_by": {"object":"user","id":"u-1"},
              "parent": {"type":"data_source_id","data_source_id":"$dataSourceId"},
              "in_trash": false,
              "properties": {},
              "url": "https://www.notion.so/$id"
            }
            """.trimIndent()

        fun queryResponse(
            pages: List<String>,
            hasMore: Boolean,
            nextCursor: String?,
            requestStatus: String? = null,
        ): String {
            val cursorJson = if (nextCursor == null) "null" else "\"$nextCursor\""
            val rs = requestStatus?.let { ",\"request_status\":$it" } ?: ""
            return """
                {
                  "object": "list",
                  "results": [${pages.joinToString(",")}],
                  "next_cursor": $cursorJson,
                  "has_more": $hasMore,
                  "type": "page_or_database",
                  "page_or_database": {}
                  $rs
                }
                """.trimIndent()
        }

        val incomplete = """{"type":"incomplete","incomplete_reason":"query_result_limit_reached"}"""
        val complete = """{"type":"complete"}"""

        fun JsonObject.field(name: String): JsonElement? = this[name]?.takeUnless { it is JsonNull }

        /**
         * Mock client answering GET requests with [viewResponse] and POST (query) requests
         * with [queryResponses] in order, recording each query body.
         */
        fun notionClient(
            queryBodies: MutableList<JsonObject>,
            requestedPaths: MutableList<String>,
            viewResponse: String,
            vararg queryResponses: String,
        ): NotionClient {
            val iterator = queryResponses.iterator()
            val engine =
                MockEngine { request ->
                    requestedPaths.add(request.url.encodedPath)
                    val content =
                        if (request.url.encodedPath.contains("/data_sources/")) {
                            queryBodies.add(
                                json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject,
                            )
                            iterator.next()
                        } else {
                            viewResponse
                        }
                    respond(
                        content = content,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            return NotionClient.createWithClient(
                KtorHttpClient(engine) { install(ContentNegotiation) { json(json) } },
                NotionConfig("test-token"),
            )
        }

        context("iterateAllRows resolves the view to its data source") {
            test("queries the view's data source and imposes the key sort") {
                val bodies = mutableListOf<JsonObject>()
                val paths = mutableListOf<String>()
                val client =
                    notionClient(
                        bodies,
                        paths,
                        viewJson(),
                        queryResponse(
                            listOf(pageObject("p-1", "2026-01-01T10:00:00.000Z")),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = complete,
                        ),
                    )

                val rows = client.views.iterateAllRows(viewId).toList()

                rows.map { it.id } shouldContainExactly listOf("p-1")
                paths[0] shouldBe "/v1/views/$viewId"
                paths[1] shouldBe "/v1/data_sources/$dataSourceId/query"

                // No view query is created — the cached query endpoint cannot be windowed.
                paths.none { it.contains("/queries") } shouldBe true

                // The view's own descending sort is replaced by the ascending key sort.
                val sorts = bodies[0]["sorts"]!!.jsonArray
                sorts shouldHaveSize 1
                sorts[0].jsonObject["timestamp"]!!.jsonPrimitive.content shouldBe "created_time"
                sorts[0].jsonObject["direction"]!!.jsonPrimitive.content shouldBe "ascending"
                bodies[0].field("filter").shouldBeNull()
            }

            test("carries the view's filter into every window and ands the window filter onto it") {
                val bodies = mutableListOf<JsonObject>()
                val paths = mutableListOf<String>()
                val boundary = "2026-01-01T10:00:00.000Z"
                val client =
                    notionClient(
                        bodies,
                        paths,
                        viewJson(filter = """{"property":"Done","checkbox":{"equals":false}}"""),
                        queryResponse(
                            listOf(pageObject("p-1", boundary)),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = incomplete,
                        ),
                        queryResponse(
                            listOf(
                                pageObject("p-1", boundary),
                                pageObject("p-2", "2026-01-01T10:05:00.000Z"),
                            ),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = complete,
                        ),
                    )

                val rows = client.views.iterateAllRows(viewId).toList()

                rows.map { it.id } shouldContainExactly listOf("p-1", "p-2")

                // Window 1 sends the view's filter unchanged.
                bodies[0]["filter"]!!
                    .jsonObject["property"]!!
                    .jsonPrimitive.content shouldBe "Done"

                // Window 2 wraps view filter + window filter in an and-compound.
                val andFilter = bodies[1]["filter"]!!.jsonObject["and"]!!.jsonArray
                andFilter shouldHaveSize 2
                andFilter[0].jsonObject["property"]!!.jsonPrimitive.content shouldBe "Done"
                andFilter[1]
                    .jsonObject["created_time"]!!
                    .jsonObject["on_or_after"]!!
                    .jsonPrimitive.content shouldBe boundary
            }

            test("retrieves the view once, not once per window") {
                val bodies = mutableListOf<JsonObject>()
                val paths = mutableListOf<String>()
                val boundary = "2026-01-01T10:00:00.000Z"
                val client =
                    notionClient(
                        bodies,
                        paths,
                        viewJson(),
                        queryResponse(
                            listOf(pageObject("p-1", boundary)),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = incomplete,
                        ),
                        queryResponse(
                            listOf(
                                pageObject("p-1", boundary),
                                pageObject("p-2", "2026-01-01T10:09:00.000Z"),
                            ),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = complete,
                        ),
                    )

                client.views.iterateAllRows(viewId).toList()

                paths.count { it == "/v1/views/$viewId" } shouldBe 1
                bodies shouldHaveSize 2
            }
        }

        context("iterateAllRows input validation") {
            test("rejects a view with no data source, such as a dashboard") {
                val bodies = mutableListOf<JsonObject>()
                val paths = mutableListOf<String>()
                val client = notionClient(bodies, paths, viewJson(dataSource = null))

                val exception =
                    shouldThrow<NotionException.ValidationError> {
                        client.views.iterateAllRows(viewId).toList()
                    }

                exception.field shouldBe "data_source_id"
                exception.message!! shouldContain "Dashboard"
            }
        }

        context("collectAllRows") {
            test("collects rows across windows into a list") {
                val bodies = mutableListOf<JsonObject>()
                val paths = mutableListOf<String>()
                val boundary = "2026-01-01T10:00:00.000Z"
                val client =
                    notionClient(
                        bodies,
                        paths,
                        viewJson(),
                        queryResponse(
                            listOf(pageObject("p-1", boundary)),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = incomplete,
                        ),
                        queryResponse(
                            listOf(
                                pageObject("p-1", boundary),
                                pageObject("p-2", "2026-01-01T10:03:00.000Z"),
                            ),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = complete,
                        ),
                    )

                client.views.collectAllRows(viewId).map { it.id } shouldContainExactly listOf("p-1", "p-2")
            }
        }
    })
