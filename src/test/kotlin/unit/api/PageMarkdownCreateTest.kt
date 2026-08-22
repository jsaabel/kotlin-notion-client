package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.PagesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTaskStatus
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.pages.AsyncPageCreateResult
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Tests for the markdown page-create path on [PagesApi] — `POST /v1/pages` with a
 * `markdown` body, and the `allow_async` opt-in enabled for it by the Jun 29 2026
 * Notion changelog.
 *
 * The 200 fixture is the official "retrieve a page" sample response — the official
 * "create a page" sample returns id-only properties, which the page model cannot decode.
 * The 202 fixture is hand-crafted (`create_page_async_task_queued.json`): Notion documents
 * the async task object and states that `POST /v1/pages` is an async-capable operation,
 * but publishes no 202 sample for this endpoint, so the fixture mirrors the documented
 * `async_task` shape with `operation.name = "POST /v1/pages"`.
 */
@Tags("Unit")
class PageMarkdownCreateTest :
    StringSpec({

        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }

        val pageFixture =
            object {}
                .javaClass
                .getResourceAsStream("/api/pages/get_retrieve_a_page.json")!!
                .bufferedReader()
                .readText()

        val asyncTaskFixture =
            object {}
                .javaClass
                .getResourceAsStream("/api/async_tasks/create_page_async_task_queued.json")!!
                .bufferedReader()
                .readText()

        fun pagesApi(handler: MockRequestHandler): PagesApi {
            val engine = MockEngine { request -> handler(request) }
            val httpClient =
                HttpClient(engine) {
                    install(ContentNegotiation) { json(json) }
                }
            return PagesApi(httpClient, NotionConfig(apiToken = "test-token"))
        }

        fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        "createFromMarkdown sends a markdown body without children or allow_async" {
            var body: JsonObject? = null
            val api =
                pagesApi { request ->
                    body = json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                    respond(pageFixture, HttpStatusCode.OK, jsonHeaders())
                }

            api.createFromMarkdown(
                parent = Parent.PageParent(pageId = "59833787-2cf9-4fdf-8782-e53db20768a5"),
                markdown = "# Meeting notes\n\n- [ ] Draft proposal",
                title = "Meeting notes",
            )

            val sent = body.shouldNotBeNull()
            sent["markdown"]!!.jsonPrimitive.content shouldContain "# Meeting notes"
            sent["children"] shouldBe null
            sent["allow_async"] shouldBe null
            sent["parent"]!!
                .jsonObject["page_id"]!!
                .jsonPrimitive.content shouldBe "59833787-2cf9-4fdf-8782-e53db20768a5"
            sent["properties"]!!
                .jsonObject["title"]!!
                .jsonObject["title"]!!
                .toString() shouldContain "Meeting notes"
        }

        "createFromMarkdown omits properties when no title is given" {
            var body: JsonObject? = null
            val api =
                pagesApi { request ->
                    body = json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                    respond(pageFixture, HttpStatusCode.OK, jsonHeaders())
                }

            api.createFromMarkdown(
                parent = Parent.PageParent(pageId = "59833787-2cf9-4fdf-8782-e53db20768a5"),
                markdown = "# Derived title\n\nBody.",
            )

            body.shouldNotBeNull()["properties"]!!.jsonObject.isEmpty() shouldBe true
        }

        "createFromMarkdownAsync sends allow_async and returns Accepted on 202" {
            var body: JsonObject? = null
            val api =
                pagesApi { request ->
                    body = json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                    respond(asyncTaskFixture, HttpStatusCode.Accepted, jsonHeaders())
                }

            val result =
                api.createFromMarkdownAsync(
                    parent = Parent.PageParent(pageId = "59833787-2cf9-4fdf-8782-e53db20768a5"),
                    markdown = "# Migration plan\n\nLots of content.",
                )

            body.shouldNotBeNull()["allow_async"]!!.jsonPrimitive.content shouldBe "true"
            result.shouldBeInstanceOf<AsyncPageCreateResult.Accepted>()
            result.task.status shouldBe AsyncTaskStatus.QUEUED
            result.task.operation?.name shouldBe "POST /v1/pages"
        }

        "createAsync returns Completed when the API answers synchronously" {
            val api = pagesApi { respond(pageFixture, HttpStatusCode.OK, jsonHeaders()) }

            val result =
                api.createAsync {
                    parent.page("59833787-2cf9-4fdf-8782-e53db20768a5")
                    markdown("# Small page")
                }

            result.shouldBeInstanceOf<AsyncPageCreateResult.Completed>()
            result.page.id.isNotBlank() shouldBe true
        }

        "createAsync rejects a request without a markdown body" {
            val api = pagesApi { respond(pageFixture, HttpStatusCode.OK, jsonHeaders()) }

            val exception =
                shouldThrow<NotionException.ValidationError> {
                    api.createAsync {
                        parent.page("59833787-2cf9-4fdf-8782-e53db20768a5")
                        title("No markdown here")
                    }
                }

            exception.field shouldBe "allow_async"
            exception.details shouldContain "markdown"
        }

        "create rejects a request that opted into async" {
            val api = pagesApi { respond(pageFixture, HttpStatusCode.OK, jsonHeaders()) }

            val exception =
                shouldThrow<NotionException.ValidationError> {
                    api.create(
                        CreatePageRequest(
                            parent = Parent.PageParent(pageId = "59833787-2cf9-4fdf-8782-e53db20768a5"),
                            properties = emptyMap(),
                            markdown = "# Big page",
                            allowAsync = true,
                        ),
                    )
                }

            exception.details shouldContain "createAsync"
        }

        "createAsync surfaces API errors" {
            val api =
                pagesApi {
                    respond(
                        """{"object":"error","status":400,"code":"validation_error","message":"bad"}""",
                        HttpStatusCode.BadRequest,
                        jsonHeaders(),
                    )
                }

            shouldThrow<NotionException.ApiError> {
                api.createFromMarkdownAsync(
                    parent = Parent.PageParent(pageId = "59833787-2cf9-4fdf-8782-e53db20768a5"),
                    markdown = "# Content",
                )
            }
        }
    })
