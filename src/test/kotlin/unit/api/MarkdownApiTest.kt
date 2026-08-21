package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.MarkdownApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTaskStatus
import it.saabel.kotlinnotionclient.models.markdown.AsyncMarkdownResult
import it.saabel.kotlinnotionclient.models.markdown.ContentUpdate
import it.saabel.kotlinnotionclient.models.markdown.ReplaceContentBody
import it.saabel.kotlinnotionclient.models.markdown.ReplaceContentRequest
import it.saabel.kotlinnotionclient.models.markdown.UpdateContentBody
import it.saabel.kotlinnotionclient.models.markdown.UpdateContentRequest
import kotlinx.serialization.json.Json

private val retrieveFixture =
    object {}
        .javaClass
        .getResourceAsStream("/api/markdown/retrieve_page_markdown.json")!!
        .bufferedReader()
        .readText()

private val updateFixture =
    object {}
        .javaClass
        .getResourceAsStream("/api/markdown/update_page_markdown.json")!!
        .bufferedReader()
        .readText()

private val acceptedFixture =
    object {}
        .javaClass
        .getResourceAsStream("/api/async_tasks/retrieve_async_task_queued.json")!!
        .bufferedReader()
        .readText()

private fun markdownApi(handler: MockRequestHandler): MarkdownApi {
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
    return MarkdownApi(httpClient, NotionConfig(apiToken = "test-token"))
}

@Tags("Unit")
class MarkdownApiTest :
    StringSpec({

        "retrieve should parse response correctly" {
            val api =
                markdownApi { request ->
                    if (request.method == HttpMethod.Get &&
                        request.url.toString().contains("/v1/pages/") &&
                        request.url.toString().contains("/markdown")
                    ) {
                        respond(
                            content = retrieveFixture,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    } else {
                        respondError(HttpStatusCode.NotFound)
                    }
                }

            val response = api.retrieve("59833787-2cf9-4fdf-8782-e53db20768a5")

            response.objectType shouldBe "page_markdown"
            response.id shouldBe "59833787-2cf9-4fdf-8782-e53db20768a5"
            response.truncated shouldBe false
            response.unknownBlockIds shouldBe emptyList()
            response.unknownBlockCount shouldBe 0
            response.markdown.contains("Tuscan Kale") shouldBe true
        }

        "retrieve with includeTranscript appends query param" {
            var capturedUrl = ""
            val api =
                markdownApi { request ->
                    capturedUrl = request.url.toString()
                    respond(
                        content = retrieveFixture,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            api.retrieve("some-page-id", includeTranscript = true)

            capturedUrl.contains("include_transcript=true") shouldBe true
        }

        "retrieve without includeTranscript omits query param" {
            var capturedUrl = ""
            val api =
                markdownApi { request ->
                    capturedUrl = request.url.toString()
                    respond(
                        content = retrieveFixture,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            api.retrieve("some-page-id")

            capturedUrl.contains("include_transcript") shouldBe false
        }

        "retrieve should throw ApiError on 404" {
            val api =
                markdownApi { _ ->
                    respondError(HttpStatusCode.NotFound, "Not found")
                }

            shouldThrow<NotionException.ApiError> {
                api.retrieve("missing-page-id")
            }
        }

        "replaceContent convenience method sends correct request" {
            var capturedMethod = HttpMethod.Get
            val api =
                markdownApi { request ->
                    capturedMethod = request.method
                    respond(
                        content = updateFixture,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val response = api.replaceContent("59833787-2cf9-4fdf-8782-e53db20768a5", "# New Content\n\nHello!")

            capturedMethod shouldBe HttpMethod.Patch
            response.objectType shouldBe "page_markdown"
            response.id shouldBe "59833787-2cf9-4fdf-8782-e53db20768a5"
            response.markdown.contains("updated") shouldBe true
        }

        "updateContent convenience method sends PATCH request" {
            var capturedMethod = HttpMethod.Get
            val api =
                markdownApi { request ->
                    capturedMethod = request.method
                    respond(
                        content = updateFixture,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val updates = listOf(ContentUpdate(oldStr = "dark leafy green", newStr = "updated dark leafy green"))
            val response = api.updateContent("59833787-2cf9-4fdf-8782-e53db20768a5", updates)

            capturedMethod shouldBe HttpMethod.Patch
            response.objectType shouldBe "page_markdown"
        }

        "updateContent DSL builder produces correct request" {
            var capturedMethod = HttpMethod.Get
            val api =
                markdownApi { request ->
                    capturedMethod = request.method
                    respond(
                        content = updateFixture,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val response =
                api.updateContent("59833787-2cf9-4fdf-8782-e53db20768a5") {
                    replace("dark leafy green", "updated dark leafy green")
                    replaceAll("Kale", "Kale")
                }

            capturedMethod shouldBe HttpMethod.Patch
            response.objectType shouldBe "page_markdown"
        }

        "update should throw ApiError on 403" {
            val api =
                markdownApi { _ ->
                    respondError(HttpStatusCode.Forbidden, "Forbidden")
                }

            shouldThrow<NotionException.ApiError> {
                api.replaceContent("some-page-id", "# Content")
            }
        }

        "truncated response with unknown_block_ids is handled" {
            val truncatedJson =
                """
                {
                  "object": "page_markdown",
                  "id": "abc123",
                  "markdown": "# Part 1\n\nContent here...",
                  "truncated": true,
                  "unknown_block_ids": ["block-id-1", "block-id-2"],
                  "unknown_block_count": 2
                }
                """.trimIndent()

            val api =
                markdownApi { _ ->
                    respond(
                        content = truncatedJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val response = api.retrieve("abc123")

            response.truncated shouldBe true
            response.unknownBlockIds shouldBe listOf("block-id-1", "block-id-2")
            response.unknownBlockCount shouldBe 2
        }

        "unknown_block_count may exceed the capped unknown_block_ids list" {
            val truncatedJson =
                """
                {
                  "object": "page_markdown",
                  "id": "abc123",
                  "markdown": "# Part 1\n\nContent here...",
                  "truncated": true,
                  "unknown_block_ids": ["block-id-1"],
                  "unknown_block_count": 137
                }
                """.trimIndent()

            val api =
                markdownApi { _ ->
                    respond(
                        content = truncatedJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val response = api.retrieve("abc123")

            response.unknownBlockIds shouldBe listOf("block-id-1")
            response.unknownBlockCount shouldBe 137
        }

        "unknown_block_count defaults to zero when absent" {
            val legacyJson =
                """
                {
                  "object": "page_markdown",
                  "id": "abc123",
                  "markdown": "# Part 1",
                  "truncated": false,
                  "unknown_block_ids": []
                }
                """.trimIndent()

            val api =
                markdownApi { _ ->
                    respond(
                        content = legacyJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val response = api.retrieve("abc123")

            response.truncated shouldBe false
            response.unknownBlockCount shouldBe 0
        }

        "replaceContentAsync sends allow_async and returns Accepted on 202" {
            var capturedMethod = HttpMethod.Get
            var capturedBody = ""
            val api =
                markdownApi { request ->
                    capturedMethod = request.method
                    capturedBody = request.body.toByteArray().decodeToString()
                    respond(
                        content = acceptedFixture,
                        status = HttpStatusCode.Accepted,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val result = api.replaceContentAsync("59833787-2cf9-4fdf-8782-e53db20768a5", "# Big rewrite")

            capturedMethod shouldBe HttpMethod.Patch
            capturedBody.contains("\"allow_async\":true") shouldBe true
            capturedBody.contains("\"replace_content\"") shouldBe true
            result.shouldBeInstanceOf<AsyncMarkdownResult.Accepted>()
            result.task.id shouldBe "task_abc123"
            result.task.status shouldBe AsyncTaskStatus.QUEUED
            result.task.pollAfterSeconds shouldBe 2
        }

        "replaceContentAsync returns Completed when the API responds synchronously" {
            val api =
                markdownApi { _ ->
                    respond(
                        content = updateFixture,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val result = api.replaceContentAsync("59833787-2cf9-4fdf-8782-e53db20768a5", "# Small rewrite")

            result.shouldBeInstanceOf<AsyncMarkdownResult.Completed>()
            result.response.objectType shouldBe "page_markdown"
            result.response.id shouldBe "59833787-2cf9-4fdf-8782-e53db20768a5"
        }

        "updateContentAsync DSL sends allow_async and returns Accepted on 202" {
            var capturedBody = ""
            val api =
                markdownApi { request ->
                    capturedBody = request.body.toByteArray().decodeToString()
                    respond(
                        content = acceptedFixture,
                        status = HttpStatusCode.Accepted,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val result =
                api.updateContentAsync("59833787-2cf9-4fdf-8782-e53db20768a5") {
                    replace("old text", "new text")
                }

            capturedBody.contains("\"allow_async\":true") shouldBe true
            capturedBody.contains("\"update_content\"") shouldBe true
            result.shouldBeInstanceOf<AsyncMarkdownResult.Accepted>()
            result.task.status shouldBe AsyncTaskStatus.QUEUED
        }

        "updateContentAsync with content update list returns Completed on 200" {
            val api =
                markdownApi { _ ->
                    respond(
                        content = updateFixture,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val updates = listOf(ContentUpdate(oldStr = "old", newStr = "new"))
            val result = api.updateContentAsync("59833787-2cf9-4fdf-8782-e53db20768a5", updates)

            result.shouldBeInstanceOf<AsyncMarkdownResult.Completed>()
        }

        "replaceContentAsync throws ApiError on error response" {
            val api =
                markdownApi { _ ->
                    respondError(HttpStatusCode.Forbidden, "Forbidden")
                }

            shouldThrow<NotionException.ApiError> {
                api.replaceContentAsync("some-page-id", "# Content")
            }
        }

        "synchronous replaceContent rejects a request with allow_async = true" {
            val api =
                markdownApi { _ ->
                    respondError(HttpStatusCode.BadRequest)
                }

            val exception =
                shouldThrow<NotionException.ValidationError> {
                    api.replaceContent(
                        "some-page-id",
                        ReplaceContentRequest(
                            replaceContent = ReplaceContentBody(newStr = "# Content"),
                            allowAsync = true,
                        ),
                    )
                }

            exception.field shouldBe "allow_async"
            exception.details.contains("replaceContentAsync") shouldBe true
        }

        "synchronous updateContent rejects a request with allow_async = true" {
            val api =
                markdownApi { _ ->
                    respondError(HttpStatusCode.BadRequest)
                }

            shouldThrow<NotionException.ValidationError> {
                api.updateContent(
                    "some-page-id",
                    UpdateContentRequest(
                        updateContent = UpdateContentBody(contentUpdates = listOf(ContentUpdate("a", "b"))),
                        allowAsync = true,
                    ),
                )
            }
        }
    })
