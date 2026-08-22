package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import it.saabel.kotlinnotionclient.api.BlocksApi
import it.saabel.kotlinnotionclient.api.CommentsApi
import it.saabel.kotlinnotionclient.api.PagesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.files.FileUploadError
import it.saabel.kotlinnotionclient.utils.asFileSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import unit.util.TestFixtures

/**
 * Covers the one-call upload-and-attach helpers added for issue #69.
 *
 * Every helper is a composition of two halves that are each already tested elsewhere, so what
 * these tests pin down is the seam: that the upload id produced by the first half reaches the
 * right field of the attach request, that the HTML helper names its upload so Notion renders it
 * as an HTML block, and that a failed upload throws instead of silently attaching nothing.
 *
 * The upload leg replies with the official `post_create_a_file_upload` /
 * `post_send_a_file_upload` samples, so the helpers see the real `pending` → `uploaded`
 * transition.
 */
@Tags("Unit")
class UploadAndAttachTest :
    FunSpec({
        val config = NotionConfig(apiToken = "test-token")

        /** Renders a multipart body so the uploaded filename can be asserted. */
        suspend fun OutgoingContent.renderToString(): String =
            when (this) {
                is OutgoingContent.ByteArrayContent -> {
                    bytes().decodeToString()
                }

                is OutgoingContent.WriteChannelContent -> {
                    val channel = ByteChannel()
                    lateinit var bytes: ByteArray
                    coroutineScope {
                        val writer =
                            launch {
                                writeTo(channel)
                                channel.flushAndClose()
                            }
                        bytes = channel.readRemaining().readByteArray()
                        writer.join()
                    }
                    bytes.decodeToString()
                }

                else -> {
                    ""
                }
            }

        /**
         * Mock engine that plays the upload legs from the official samples and hands every other
         * request to [attach], recording each request body along the way.
         */
        fun engine(
            recorded: MutableList<Pair<String, String>>,
            uploadFails: Boolean = false,
            attach: (HttpRequestData) -> String,
        ): HttpClient {
            val mockEngine =
                MockEngine { request ->
                    val url = request.url.toString()
                    val body = request.body.renderToString()
                    recorded.add(url to body)

                    fun ok(content: String) =
                        respond(
                            content = content,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )

                    when {
                        url.endsWith("/file_uploads") && request.method == HttpMethod.Post -> {
                            if (uploadFails) {
                                respondError(HttpStatusCode.BadRequest, """{"object":"error","code":"validation_error","message":"nope"}""")
                            } else {
                                ok(TestFixtures.FileUploads.createFileUploadAsString())
                            }
                        }

                        url.contains("/file_uploads/") && url.endsWith("/send") -> {
                            ok(TestFixtures.FileUploads.sendFileUploadAsString())
                        }

                        else -> {
                            ok(attach(request))
                        }
                    }
                }
            return HttpClient(mockEngine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        }

        /** The id carried by the official create/send file-upload samples. */
        val sampleUploadId = "b52b8ed6-e029-4707-a671-832549c09de3"

        context("blocks") {
            test("appendImage attaches the upload id to an image block") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Blocks.appendBlockChildrenAsString() }

                try {
                    BlocksApi(client, config).appendImage("page-id", byteArrayOf(1, 2, 3).asFileSource("diagram.png"), caption = "arch")
                } finally {
                    client.close()
                }

                val append = recorded.single { it.first.contains("/blocks/page-id/children") }.second
                append shouldContain "\"file_upload\""
                append shouldContain sampleUploadId
                append shouldContain "arch"
            }

            test("appendFile defaults the block's display name to the upload's filename") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Blocks.appendBlockChildrenAsString() }

                try {
                    BlocksApi(client, config).appendFile("page-id", byteArrayOf(1).asFileSource("report.pdf"))
                } finally {
                    client.close()
                }

                // "test.txt" is the filename on the official send-file-upload sample.
                recorded.single { it.first.contains("/children") }.second shouldContain "\"name\":\"test.txt\""
            }

            test("appendHtml uploads under a .html name and attaches it as an embed") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Blocks.appendBlockChildrenAsString() }

                try {
                    BlocksApi(client, config).appendHtml("page-id", "<h1>Report</h1>")
                } finally {
                    client.close()
                }

                val create = recorded.first { it.first.endsWith("/file_uploads") }.second
                create shouldContain "embed.html"
                create shouldContain "text/html"

                val append = recorded.single { it.first.contains("/children") }.second
                append shouldContain "\"embed\""
                append shouldContain sampleUploadId
            }

            test("appendHtml carries a caption onto the embed block") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Blocks.appendBlockChildrenAsString() }

                try {
                    BlocksApi(client, config).appendHtml("page-id", "<p>hi</p>", caption = "generated nightly")
                } finally {
                    client.close()
                }

                recorded.single { it.first.contains("/children") }.second shouldContain "generated nightly"
            }

            test("appendHtml appends the .html extension to a caller-supplied name") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Blocks.appendBlockChildrenAsString() }

                try {
                    BlocksApi(client, config).appendHtml("page-id", "<p>hi</p>", filename = "release-notes")
                } finally {
                    client.close()
                }

                recorded.first { it.first.endsWith("/file_uploads") }.second shouldContain "release-notes.html"
            }

            test("appendHtml leaves an existing .htm extension alone") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Blocks.appendBlockChildrenAsString() }

                try {
                    BlocksApi(client, config).appendHtml("page-id", "<p>hi</p>", filename = "legacy.htm")
                } finally {
                    client.close()
                }

                val create = recorded.first { it.first.endsWith("/file_uploads") }.second
                create shouldContain "legacy.htm"
                create.contains("legacy.htm.html") shouldBe false
            }

            test("a failed upload throws instead of attaching") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded, uploadFails = true) { TestFixtures.Blocks.appendBlockChildrenAsString() }

                try {
                    shouldThrow<FileUploadError> {
                        BlocksApi(client, config).appendImage("page-id", byteArrayOf(1).asFileSource("diagram.png"))
                    }
                } finally {
                    client.close()
                }

                recorded.none { it.first.contains("/children") } shouldBe true
            }
        }

        context("pages") {
            test("setIcon writes a file_upload icon") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Pages.retrievePageAsString() }

                try {
                    PagesApi(client, config).setIcon("page-id", byteArrayOf(1).asFileSource("logo.png"))
                } finally {
                    client.close()
                }

                val patch = recorded.single { it.first.endsWith("/pages/page-id") }.second
                patch shouldContain "\"icon\""
                patch shouldContain "\"file_upload\""
                patch shouldContain sampleUploadId
            }

            test("setCover writes a file_upload cover") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Pages.retrievePageAsString() }

                try {
                    PagesApi(client, config).setCover("page-id", byteArrayOf(1).asFileSource("hero.png"))
                } finally {
                    client.close()
                }

                val patch = recorded.single { it.first.endsWith("/pages/page-id") }.second
                patch shouldContain "\"cover\""
                patch shouldContain sampleUploadId
            }

            test("attachFiles with replace=true skips the read and sends only the new uploads") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Pages.retrievePageAsString() }

                try {
                    PagesApi(client, config).attachFiles(
                        pageId = "page-id",
                        propertyName = "Attachments",
                        sources = listOf(byteArrayOf(1).asFileSource("a.pdf")),
                        replace = true,
                    )
                } finally {
                    client.close()
                }

                recorded.none { it.first.endsWith("/pages/page-id") && it.second.isEmpty() } shouldBe true
                val patch = recorded.last { it.first.endsWith("/pages/page-id") }.second
                patch shouldContain "Attachments"
                patch shouldContain sampleUploadId
            }

            test("attachFiles reads the page first so existing entries survive the write") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Pages.retrievePageAsString() }

                try {
                    PagesApi(client, config).attachFiles(
                        pageId = "page-id",
                        propertyName = "Attachments",
                        sources = listOf(byteArrayOf(1).asFileSource("a.pdf")),
                    )
                } finally {
                    client.close()
                }

                // The GET carries no body; the PATCH does.
                recorded.count { it.first.endsWith("/pages/page-id") } shouldBe 2
                recorded.last { it.first.endsWith("/pages/page-id") }.second shouldContain sampleUploadId
            }

            test("attachFiles refuses a property that is not files") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Pages.retrievePageAsString() }

                try {
                    val error =
                        shouldThrow<IllegalArgumentException> {
                            PagesApi(client, config).attachFiles(
                                pageId = "page-id",
                                propertyName = "Description",
                                sources = listOf(byteArrayOf(1).asFileSource("a.pdf")),
                            )
                        }
                    error.message shouldContain "not files"
                } finally {
                    client.close()
                }

                recorded.none { it.first.endsWith("/file_uploads") } shouldBe true
            }

            test("attachFiles rejects an empty file list before uploading anything") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Pages.retrievePageAsString() }

                try {
                    shouldThrow<IllegalArgumentException> {
                        PagesApi(client, config).attachFiles("page-id", "Attachments", emptyList())
                    }
                } finally {
                    client.close()
                }

                recorded.shouldBeEmptyRecording()
            }
        }

        context("comments") {
            test("create uploads the attachments and merges them into the request") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Comments.createCommentAsString() }

                try {
                    CommentsApi(client, config).create(listOf(byteArrayOf(1).asFileSource("trace.txt"))) {
                        parent.pageId("page-id")
                        content { text("see attached") }
                    }
                } finally {
                    client.close()
                }

                val post = recorded.single { it.first.endsWith("/comments") }.second
                post shouldContain "\"attachments\""
                post shouldContain sampleUploadId
            }

            test("create rejects more than three attachments before uploading anything") {
                val recorded = mutableListOf<Pair<String, String>>()
                val client = engine(recorded) { TestFixtures.Comments.createCommentAsString() }

                try {
                    shouldThrow<IllegalArgumentException> {
                        CommentsApi(client, config).create(
                            List(4) { byteArrayOf(1).asFileSource("f$it.txt") },
                        ) {
                            parent.pageId("page-id")
                            content { text("too many") }
                        }
                    }
                } finally {
                    client.close()
                }

                recorded.shouldBeEmptyRecording()
            }
        }
    })

private fun List<Pair<String, String>>.shouldBeEmptyRecording() {
    check(isEmpty()) { "expected no HTTP calls, but saw ${map { it.first }}" }
}
