package unit.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mock HTTP plumbing for the pending-upload tests (ADR 0001).
 *
 * The engine here mints a **distinct** upload id per create call, which the shared official
 * samples cannot do — the point of most of those tests is *which* id ends up on *which*
 * surface, so a single fixed sample id would hide exactly the bug worth catching.
 */
object UploadRecordingClient {
    /** Renders a request body so multipart filenames and JSON payloads can be asserted. */
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

    /** A file-upload object in the shape `FileUploadApi` expects back. */
    fun uploadJson(
        id: String,
        status: String,
    ) = """
        {"id":"$id","object":"file_upload","created_time":"2025-03-15T20:53:00.000Z",
         "last_edited_time":"2025-03-15T20:53:00.000Z","expiry_time":"2025-03-15T21:53:00.000Z",
         "upload_url":"https://api.notion.com/v1/file_uploads/$id/send","in_trash":false,
         "status":"$status","filename":"test.txt","content_type":"text/plain","content_length":1024}
        """.trimIndent().replace("\n", "")

    /**
     * Every request the engine saw, as `method url` to body.
     *
     * Synchronized because a call's uploads are issued concurrently.
     */
    class Recorder {
        val calls: MutableList<Pair<String, String>> = Collections.synchronizedList(mutableListOf())

        fun bodies(match: String) = calls.filter { it.first.contains(match) }.map { it.second }

        fun urls() = calls.map { it.first }

        /** How many files this call actually uploaded. */
        fun uploadCount() = urls().count { it.endsWith("/file_uploads") }
    }

    /**
     * Mock client that mints an upload id carrying the file's own name — `1-chart.png`,
     * `2-chart.png`, … — and echoes it back from the matching send call.
     *
     * Naming the id after the file is what lets these tests assert the *mapping* rather than
     * just the count: uploads run concurrently, so arrival order says nothing about which
     * sentinel a given id belongs to. The leading counter keeps two attachments of the same
     * file distinguishable. [failUploadNumber] makes the n-th create fail, for the
     * partial-failure cases.
     *
     * @param attach responds to every non-upload call; receives the request url
     */
    fun client(
        recorder: Recorder,
        failUploadNumber: Int? = null,
        attach: (String) -> String,
    ): HttpClient {
        val minted = AtomicInteger(0)
        val engine =
            MockEngine { request ->
                val url = request.url.toString()
                val body = request.body.renderToString()
                recorder.calls.add("${request.method.value} $url" to body)

                fun ok(content: String) =
                    respond(
                        content = content,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )

                when {
                    url.endsWith("/file_uploads") && request.method == HttpMethod.Post -> {
                        val n = minted.incrementAndGet()
                        if (n == failUploadNumber) {
                            respondError(
                                HttpStatusCode.BadRequest,
                                """{"object":"error","code":"validation_error","message":"nope"}""",
                            )
                        } else {
                            val filename = body.substringAfter("\"filename\":\"").substringBefore("\"")
                            ok(uploadJson("$n-$filename", "pending"))
                        }
                    }

                    url.endsWith("/send") -> {
                        ok(uploadJson(url.substringAfter("/file_uploads/").substringBefore("/send"), "uploaded"))
                    }

                    else -> {
                        ok(attach(url))
                    }
                }
            }

        return HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
}
