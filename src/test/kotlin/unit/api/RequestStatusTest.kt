package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.base.RequestStatus
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryResponse
import it.saabel.kotlinnotionclient.models.views.ViewQuery
import it.saabel.kotlinnotionclient.models.views.ViewQueryResults
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient as KtorHttpClient

/**
 * Tests for the `request_status` field (Notion API 2026-04-20) and the
 * `NotionException.QueryResultLimitReached` thrown by auto-paginating data
 * source queries when the API truncates at its 10,000-row cap.
 */
@Tags("Unit")
class RequestStatusTest :
    FunSpec({

        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        context("RequestStatus model") {
            test("deserializes the complete shape") {
                val raw = """{"type":"complete"}"""
                val status = json.decodeFromString(RequestStatus.serializer(), raw)

                status.type shouldBe RequestStatus.TYPE_COMPLETE
                status.incompleteReason.shouldBeNull()
                status.isComplete shouldBe true
                status.isIncomplete shouldBe false
            }

            test("deserializes the incomplete shape with reason") {
                val raw =
                    """{"type":"incomplete","incomplete_reason":"query_result_limit_reached"}"""
                val status = json.decodeFromString(RequestStatus.serializer(), raw)

                status.type shouldBe RequestStatus.TYPE_INCOMPLETE
                status.incompleteReason shouldBe RequestStatus.REASON_QUERY_RESULT_LIMIT_REACHED
                status.isIncomplete shouldBe true
            }
        }

        context("Response models carry request_status") {
            test("DataSourceQueryResponse round-trips request_status") {
                val raw =
                    """
                    {
                      "object": "list",
                      "results": [],
                      "next_cursor": "cursor-xyz",
                      "has_more": true,
                      "type": "page_or_database",
                      "page_or_database": {},
                      "request_status": {
                        "type": "incomplete",
                        "incomplete_reason": "query_result_limit_reached"
                      }
                    }
                    """.trimIndent()

                val response = json.decodeFromString(DataSourceQueryResponse.serializer(), raw)
                response.requestStatus.shouldNotBeNull()
                response.requestStatus.isIncomplete shouldBe true
                response.requestStatus.incompleteReason shouldBe
                    RequestStatus.REASON_QUERY_RESULT_LIMIT_REACHED
            }

            test("DataSourceQueryResponse without request_status leaves it null") {
                val raw =
                    """
                    {
                      "object": "list",
                      "results": [],
                      "has_more": false,
                      "type": "page_or_database",
                      "page_or_database": {}
                    }
                    """.trimIndent()
                val response = json.decodeFromString(DataSourceQueryResponse.serializer(), raw)
                response.requestStatus.shouldBeNull()
            }

            test("ViewQuery round-trips request_status") {
                val raw =
                    """
                    {
                      "object": "view_query",
                      "id": "q-1",
                      "view_id": "v-1",
                      "expires_at": "2026-05-30T00:15:00.000Z",
                      "total_count": 10000,
                      "results": [],
                      "has_more": true,
                      "request_status": {
                        "type": "incomplete",
                        "incomplete_reason": "query_result_limit_reached"
                      }
                    }
                    """.trimIndent()

                val query = json.decodeFromString(ViewQuery.serializer(), raw)
                query.requestStatus.shouldNotBeNull()
                query.requestStatus.isIncomplete shouldBe true
            }

            test("ViewQueryResults round-trips request_status") {
                val raw =
                    """
                    {
                      "object": "list",
                      "results": [],
                      "has_more": false,
                      "request_status": {
                        "type": "complete"
                      }
                    }
                    """.trimIndent()

                val results = json.decodeFromString(ViewQueryResults.serializer(), raw)
                results.requestStatus.shouldNotBeNull()
                results.requestStatus.isComplete shouldBe true
            }
        }

        context("DataSourcesApi.query auto-pagination") {
            fun pageJson(
                pageIds: List<String>,
                hasMore: Boolean,
                nextCursor: String?,
                requestStatus: String? = null,
            ): String {
                val results =
                    pageIds.joinToString(",") { id ->
                        """
                        {
                          "object": "page",
                          "id": "$id",
                          "created_time": "2026-05-30T00:00:00.000Z",
                          "last_edited_time": "2026-05-30T00:00:00.000Z",
                          "created_by": {"object":"user","id":"u-1"},
                          "last_edited_by": {"object":"user","id":"u-1"},
                          "parent": {"type":"data_source_id","data_source_id":"ds-1"},
                          "in_trash": false,
                          "properties": {},
                          "url": "https://www.notion.so/$id"
                        }
                        """.trimIndent()
                    }
                val cursorJson = if (nextCursor == null) "null" else "\"$nextCursor\""
                val rs = requestStatus?.let { ",\"request_status\":$it" } ?: ""
                return """
                    {
                      "object": "list",
                      "results": [$results],
                      "next_cursor": $cursorJson,
                      "has_more": $hasMore,
                      "type": "page_or_database",
                      "page_or_database": {}
                      $rs
                    }
                    """.trimIndent()
            }

            fun clientReturning(vararg pages: String): KtorHttpClient {
                val responses = pages.iterator()
                val engine =
                    MockEngine { _ ->
                        respond(
                            content = responses.next(),
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

            test("collects all pages when request_status is complete") {
                val httpClient =
                    clientReturning(
                        pageJson(listOf("p-1", "p-2"), hasMore = true, nextCursor = "c-1"),
                        pageJson(
                            listOf("p-3"),
                            hasMore = false,
                            nextCursor = null,
                            requestStatus = """{"type":"complete"}""",
                        ),
                    )
                val client =
                    NotionClient.createWithClient(httpClient, NotionConfig("test-token"))

                val pages = client.dataSources.query("ds-1")

                pages.map { it.id } shouldContainExactly listOf("p-1", "p-2", "p-3")
            }

            test("throws QueryResultLimitReached and carries partial results on truncation") {
                val httpClient =
                    clientReturning(
                        pageJson(listOf("p-1", "p-2"), hasMore = true, nextCursor = "c-1"),
                        pageJson(
                            listOf("p-3"),
                            hasMore = true,
                            nextCursor = "c-2",
                            requestStatus =
                                """{"type":"incomplete","incomplete_reason":"query_result_limit_reached"}""",
                        ),
                    )
                val client =
                    NotionClient.createWithClient(httpClient, NotionConfig("test-token"))

                val ex =
                    shouldThrow<NotionException.QueryResultLimitReached> {
                        client.dataSources.query("ds-1")
                    }

                ex.partialResults.map { it.id } shouldContainExactly listOf("p-1", "p-2", "p-3")
                ex.nextCursor shouldBe "c-2"
                ex.requestStatus.isIncomplete shouldBe true
                ex.requestStatus.incompleteReason shouldBe
                    RequestStatus.REASON_QUERY_RESULT_LIMIT_REACHED
            }

            test("throws on the very first incomplete page") {
                val httpClient =
                    clientReturning(
                        pageJson(
                            listOf("p-1"),
                            hasMore = true,
                            nextCursor = "c-1",
                            requestStatus =
                                """{"type":"incomplete","incomplete_reason":"query_result_limit_reached"}""",
                        ),
                    )
                val client =
                    NotionClient.createWithClient(httpClient, NotionConfig("test-token"))

                val ex =
                    shouldThrow<NotionException.QueryResultLimitReached> {
                        client.dataSources.query("ds-1")
                    }

                ex.partialResults.shouldHaveSize(1)
                ex.partialResults.first().id shouldBe "p-1"
                ex.nextCursor shouldBe "c-1"
            }

            test("queryAsFlow throws QueryResultLimitReached on truncation") {
                val httpClient =
                    clientReturning(
                        pageJson(listOf("p-1"), hasMore = true, nextCursor = "c-1"),
                        pageJson(
                            listOf("p-2"),
                            hasMore = true,
                            nextCursor = "c-2",
                            requestStatus =
                                """{"type":"incomplete","incomplete_reason":"query_result_limit_reached"}""",
                        ),
                    )
                val client =
                    NotionClient.createWithClient(httpClient, NotionConfig("test-token"))

                val ex =
                    shouldThrow<NotionException.QueryResultLimitReached> {
                        client.dataSources.queryAsFlow("ds-1").toList()
                    }

                ex.partialResults.map { it.id } shouldContainExactly listOf("p-1", "p-2")
                ex.nextCursor shouldBe "c-2"
            }
        }
    })
