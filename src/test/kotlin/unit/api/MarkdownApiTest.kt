package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
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
import it.saabel.kotlinnotionclient.models.markdown.ContentUpdate
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
                  "unknown_block_ids": ["block-id-1", "block-id-2"]
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
        }
    })
