package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
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
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.datasources.RowIterationKey
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
 * Tests for [it.saabel.kotlinnotionclient.api.DataSourcesApi.iterateAllRows] and
 * [it.saabel.kotlinnotionclient.api.DataSourcesApi.collectAllRows] — the windowed
 * iteration helpers that page past Notion's 10,000-row query result cap.
 */
@Tags("Unit")
class LargeDataSourceIterationTest :
    FunSpec({

        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        fun pageObject(
            id: String,
            createdTime: String,
            uniqueIdProperty: Pair<String, Int>? = null,
        ): String {
            val properties =
                uniqueIdProperty?.let { (name, number) ->
                    """
                    {"$name": {"id": "prop-uid", "type": "unique_id", "unique_id": {"prefix": "ROW", "number": $number}}}
                    """.trimIndent()
                } ?: "{}"
            return """
                {
                  "object": "page",
                  "id": "$id",
                  "created_time": "$createdTime",
                  "last_edited_time": "$createdTime",
                  "created_by": {"object":"user","id":"u-1"},
                  "last_edited_by": {"object":"user","id":"u-1"},
                  "parent": {"type":"data_source_id","data_source_id":"ds-1"},
                  "in_trash": false,
                  "properties": $properties,
                  "url": "https://www.notion.so/$id"
                }
                """.trimIndent()
        }

        fun responseJson(
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

        // The client serializes absent optional fields as explicit JSON nulls;
        // normalize both to Kotlin null for assertions.
        fun JsonObject.field(name: String): JsonElement? = this[name]?.takeUnless { it is JsonNull }

        val incomplete = """{"type":"incomplete","incomplete_reason":"query_result_limit_reached"}"""
        val complete = """{"type":"complete"}"""

        /**
         * Builds a mock client returning canned responses in order and recording
         * each request body (parsed as JSON) into [capturedBodies].
         */
        fun clientReturning(
            capturedBodies: MutableList<JsonObject>,
            vararg responses: String,
        ): KtorHttpClient {
            val iterator = responses.iterator()
            val engine =
                MockEngine { request ->
                    val bodyText = request.body.toByteArray().decodeToString()
                    capturedBodies.add(json.parseToJsonElement(bodyText).jsonObject)
                    respond(
                        content = iterator.next(),
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                }
            return KtorHttpClient(engine) {
                install(ContentNegotiation) { json(json) }
            }
        }

        fun notionClient(
            capturedBodies: MutableList<JsonObject>,
            vararg responses: String,
        ) = NotionClient.createWithClient(
            clientReturning(capturedBodies, *responses),
            it.saabel.kotlinnotionclient.config
                .NotionConfig("test-token"),
        )

        context("iterateAllRows without truncation") {
            test("follows cursors within a single window and emits all rows") {
                val bodies = mutableListOf<JsonObject>()
                val client =
                    notionClient(
                        bodies,
                        responseJson(
                            listOf(
                                pageObject("p-1", "2026-01-01T10:00:00.000Z"),
                                pageObject("p-2", "2026-01-01T10:01:00.000Z"),
                            ),
                            hasMore = true,
                            nextCursor = "c-1",
                        ),
                        responseJson(
                            listOf(pageObject("p-3", "2026-01-01T10:02:00.000Z")),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = complete,
                        ),
                    )

                val rows = client.dataSources.iterateAllRows("ds-1").toList()

                rows.map { it.id } shouldContainExactly listOf("p-1", "p-2", "p-3")
                bodies shouldHaveSize 2

                // First request: no filter, sort injected on created_time ascending
                bodies[0].field("filter").shouldBeNull()
                val sorts = bodies[0]["sorts"]!!.jsonArray
                sorts shouldHaveSize 1
                sorts[0].jsonObject["timestamp"]!!.jsonPrimitive.content shouldBe "created_time"
                sorts[0].jsonObject["direction"]!!.jsonPrimitive.content shouldBe "ascending"

                // Second request continues the cursor chain, same window (no filter)
                bodies[1]["start_cursor"]!!.jsonPrimitive.content shouldBe "c-1"
                bodies[1].field("filter").shouldBeNull()
            }

            test("empty result completes without emitting") {
                val bodies = mutableListOf<JsonObject>()
                val client =
                    notionClient(
                        bodies,
                        responseJson(emptyList(), hasMore = false, nextCursor = null),
                    )

                val rows = client.dataSources.iterateAllRows("ds-1").toList()

                rows.shouldBeEmpty()
                bodies shouldHaveSize 1
            }
        }

        context("iterateAllRows crossing the truncation boundary (created_time key)") {
            test("re-windows from the last seen created_time and de-duplicates boundary ties") {
                val bodies = mutableListOf<JsonObject>()
                val boundary = "2026-01-01T10:01:00.000Z"
                val client =
                    notionClient(
                        bodies,
                        // Window 1, page 1: truncated result set
                        responseJson(
                            listOf(
                                pageObject("p-1", "2026-01-01T10:00:00.000Z"),
                                pageObject("p-2", boundary),
                            ),
                            hasMore = true,
                            nextCursor = "c-1",
                            requestStatus = incomplete,
                        ),
                        // Window 1, page 2: end of the truncated 10k cache
                        responseJson(
                            listOf(pageObject("p-3", boundary)),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = incomplete,
                        ),
                        // Window 2: re-query from the boundary; p-2/p-3 are served again (ties)
                        responseJson(
                            listOf(
                                pageObject("p-2", boundary),
                                pageObject("p-3", boundary),
                                pageObject("p-4", boundary),
                                pageObject("p-5", "2026-01-01T10:02:00.000Z"),
                            ),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = complete,
                        ),
                    )

                val rows = client.dataSources.iterateAllRows("ds-1").toList()

                rows.map { it.id } shouldContainExactly listOf("p-1", "p-2", "p-3", "p-4", "p-5")
                bodies shouldHaveSize 3

                // Window 2 starts fresh (no cursor) with an on_or_after window filter
                bodies[2].field("start_cursor").shouldBeNull()
                val filter = bodies[2]["filter"]!!.jsonObject
                filter["timestamp"]!!.jsonPrimitive.content shouldBe "created_time"
                filter["created_time"]!!.jsonObject["on_or_after"]!!.jsonPrimitive.content shouldBe boundary
            }

            test("combines the caller's filter with the window filter via and") {
                val bodies = mutableListOf<JsonObject>()
                val boundary = "2026-01-01T10:00:00.000Z"
                val client =
                    notionClient(
                        bodies,
                        responseJson(
                            listOf(pageObject("p-1", boundary)),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = incomplete,
                        ),
                        responseJson(
                            listOf(
                                pageObject("p-1", boundary),
                                pageObject("p-2", "2026-01-01T10:05:00.000Z"),
                            ),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = complete,
                        ),
                    )

                val rows =
                    client.dataSources
                        .iterateAllRows("ds-1") {
                            filter { checkbox("Done").equals(false) }
                        }.toList()

                rows.map { it.id } shouldContainExactly listOf("p-1", "p-2")

                // Window 1 uses the caller filter as-is
                bodies[0]["filter"]!!.jsonObject["property"]!!.jsonPrimitive.content shouldBe "Done"

                // Window 2 wraps caller filter + window filter in an and-compound
                val andFilter = bodies[1]["filter"]!!.jsonObject["and"]!!.jsonArray
                andFilter shouldHaveSize 2
                andFilter[0].jsonObject["property"]!!.jsonPrimitive.content shouldBe "Done"
                andFilter[1].jsonObject["timestamp"]!!.jsonPrimitive.content shouldBe "created_time"
                andFilter[1]
                    .jsonObject["created_time"]!!
                    .jsonObject["on_or_after"]!!
                    .jsonPrimitive.content shouldBe boundary
            }

            test("throws IterationStalled when a truncated window yields no new rows") {
                val bodies = mutableListOf<JsonObject>()
                val tied = "2026-01-01T10:00:00.000Z"
                val client =
                    notionClient(
                        bodies,
                        responseJson(
                            listOf(
                                pageObject("p-1", tied),
                                pageObject("p-2", tied),
                            ),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = incomplete,
                        ),
                        // Re-windowing serves only rows we already emitted, still truncated
                        responseJson(
                            listOf(
                                pageObject("p-1", tied),
                                pageObject("p-2", tied),
                            ),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = incomplete,
                        ),
                    )

                val ex =
                    shouldThrow<NotionException.IterationStalled> {
                        client.dataSources.iterateAllRows("ds-1").toList()
                    }

                ex.boundaryValue shouldBe tied
                ex.message!! shouldContain "created_time"
            }
        }

        context("iterateAllRows with a unique_id key") {
            test("re-windows with a strict greater_than filter on the unique_id property") {
                val bodies = mutableListOf<JsonObject>()
                val client =
                    notionClient(
                        bodies,
                        responseJson(
                            listOf(
                                pageObject("p-1", "2026-01-01T10:00:00.000Z", "ID" to 1),
                                pageObject("p-2", "2026-01-01T10:00:00.000Z", "ID" to 2),
                            ),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = incomplete,
                        ),
                        responseJson(
                            listOf(pageObject("p-3", "2026-01-01T10:00:00.000Z", "ID" to 3)),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = complete,
                        ),
                    )

                val rows =
                    client.dataSources
                        .iterateAllRows("ds-1", key = RowIterationKey.UniqueId("ID"))
                        .toList()

                rows.map { it.id } shouldContainExactly listOf("p-1", "p-2", "p-3")
                bodies shouldHaveSize 2

                // Sort by the unique_id property, ascending
                val sorts = bodies[0]["sorts"]!!.jsonArray
                sorts[0].jsonObject["property"]!!.jsonPrimitive.content shouldBe "ID"
                sorts[0].jsonObject["direction"]!!.jsonPrimitive.content shouldBe "ascending"

                // Window 2 filters strictly past the last seen unique_id number
                val filter = bodies[1]["filter"]!!.jsonObject
                filter["property"]!!.jsonPrimitive.content shouldBe "ID"
                filter["unique_id"]!!.jsonObject["greater_than"]!!.jsonPrimitive.content shouldBe "2"
            }

            test("fails fast when a row is missing the unique_id property") {
                val bodies = mutableListOf<JsonObject>()
                val client =
                    notionClient(
                        bodies,
                        responseJson(
                            listOf(pageObject("p-1", "2026-01-01T10:00:00.000Z")),
                            hasMore = false,
                            nextCursor = null,
                        ),
                    )

                val ex =
                    shouldThrow<NotionException.ValidationError> {
                        client.dataSources
                            .iterateAllRows("ds-1", key = RowIterationKey.UniqueId("ID"))
                            .toList()
                    }

                ex.message!! shouldContain "ID"
            }
        }

        context("iterateAllRows input validation") {
            test("rejects a request that already carries sorts") {
                val bodies = mutableListOf<JsonObject>()
                val client = notionClient(bodies)

                shouldThrow<IllegalArgumentException> {
                    client.dataSources.iterateAllRows("ds-1") {
                        sortBy("Priority")
                    }
                }
            }

            test("rejects a request that already carries a start cursor") {
                val bodies = mutableListOf<JsonObject>()
                val client = notionClient(bodies)

                shouldThrow<IllegalArgumentException> {
                    client.dataSources.iterateAllRows("ds-1") {
                        startCursor("c-99")
                    }
                }
            }
        }

        context("collectAllRows") {
            test("collects rows across windows into a list") {
                val bodies = mutableListOf<JsonObject>()
                val client =
                    notionClient(
                        bodies,
                        responseJson(
                            listOf(pageObject("p-1", "2026-01-01T10:00:00.000Z")),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = incomplete,
                        ),
                        responseJson(
                            listOf(
                                pageObject("p-1", "2026-01-01T10:00:00.000Z"),
                                pageObject("p-2", "2026-01-01T10:03:00.000Z"),
                            ),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = complete,
                        ),
                    )

                val rows = client.dataSources.collectAllRows("ds-1")

                rows.map { it.id } shouldContainExactly listOf("p-1", "p-2")
            }
        }
    })
