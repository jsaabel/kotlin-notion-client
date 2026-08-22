package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import it.saabel.kotlinnotionclient.api.BlocksApi
import it.saabel.kotlinnotionclient.api.PagesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.PendingUploadKind
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import it.saabel.kotlinnotionclient.models.files.FileUploadError
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import it.saabel.kotlinnotionclient.utils.asFileSource
import it.saabel.kotlinnotionclient.validation.RequestValidator
import it.saabel.kotlinnotionclient.validation.ValidationException
import it.saabel.kotlinnotionclient.validation.ViolationType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import unit.util.TestFixtures
import unit.util.UploadRecordingClient.Recorder
import unit.util.UploadRecordingClient.client

/**
 * Covers the pending-upload machinery from ADR 0001: the sentinel the file-taking builder
 * overloads emit, and the resolution pass every suspending entry point runs before it sends.
 *
 * Stage 1 of the ADR — the block surface. The remaining surfaces (files properties, icon,
 * cover, comment attachments) and whole-request pooling live in
 * [RequestPendingUploadResolutionTest].
 *
 * The mock engine mints a **distinct** upload id per create call, naming it after the file it
 * carried, so these tests can assert *which* id ends up in *which* block. See
 * [unit.util.UploadRecordingClient].
 */
@Tags("Unit")
class PendingUploadResolutionTest :
    FunSpec({
        val config = NotionConfig(apiToken = "test-token")

        fun blocksClient(
            recorder: Recorder,
            failUploadNumber: Int? = null,
        ) = client(recorder, failUploadNumber) { TestFixtures.Blocks.appendBlockChildrenAsString() }

        fun bytes(name: String) = byteArrayOf(1, 2, 3).asFileSource(name)

        context("the sentinel") {
            test("a file-taking builder overload records the file instead of uploading it") {
                val blocks = pageContent { image(bytes("chart.png"), caption = "Q3") }

                val sentinel = blocks.single() as BlockRequest.PendingUpload
                sentinel.kind shouldBe PendingUploadKind.IMAGE
                sentinel.source.filename shouldBe "chart.png"
                sentinel.caption.single().plainText shouldBe "Q3"
            }

            test("file() defaults the block's display name to the source's filename") {
                val sentinel = pageContent { file(bytes("appendix.pdf")) }.single() as BlockRequest.PendingUpload

                sentinel.name shouldBe "appendix.pdf"
            }

            test("the File and Path overloads record the file behind them") {
                val temp =
                    kotlin.io.path
                        .createTempFile("chart", ".png")
                        .toFile()
                temp.writeBytes(byteArrayOf(1, 2, 3))
                temp.deleteOnExit()

                try {
                    val blocks =
                        pageContent {
                            image(temp)
                            pdf(temp.toPath())
                            file(temp)
                        }

                    blocks.map { (it as BlockRequest.PendingUpload).kind } shouldContainExactly
                        listOf(PendingUploadKind.IMAGE, PendingUploadKind.PDF, PendingUploadKind.FILE)
                    blocks.forEach { (it as BlockRequest.PendingUpload).source.filename shouldBe temp.name }
                    (blocks[2] as BlockRequest.PendingUpload).name shouldBe temp.name
                } finally {
                    temp.delete()
                }
            }

            test("html() names the upload so Notion renders it as an HTML block") {
                val sentinel =
                    pageContent { html("<h1>Report</h1>", filename = "release-notes") }
                        .single() as BlockRequest.PendingUpload

                sentinel.kind shouldBe PendingUploadKind.HTML
                sentinel.source.filename shouldBe "release-notes.html"
            }

            test("serializing an unresolved sentinel throws with an actionable message") {
                val blocks = pageContent { image(bytes("chart.png")) }

                val error = shouldThrow<SerializationException> { Json.encodeToString(blocks) }

                error.message.orEmpty() shouldContain "chart.png"
                error.message.orEmpty() shouldContain "imageFromUpload"
            }

            test("the validator reports an unresolved sentinel, nested ones included") {
                val blocks = pageContent { toggle("Details") { pdf(bytes("appendix.pdf")) } }

                val error = shouldThrow<ValidationException> { RequestValidator().validateOrThrow("children", blocks) }

                error.validationResult.violations
                    .single()
                    .violationType shouldBe ViolationType.UNRESOLVED_PENDING_UPLOAD
                error.message.orEmpty() shouldContain "appendix.pdf"
            }
        }

        context("resolution") {
            test("every kind is rewritten to its file_upload block") {
                val recorder = Recorder()
                val client = blocksClient(recorder)

                try {
                    BlocksApi(client, config).appendChildren("page-id") {
                        image(bytes("chart.png"))
                        video(bytes("clip.mp4"))
                        audio(bytes("theme.mp3"))
                        file(bytes("data.csv"))
                        pdf(bytes("appendix.pdf"))
                        html("<h1>Report</h1>")
                    }
                } finally {
                    client.close()
                }

                val children =
                    Json
                        .parseToJsonElement(recorder.bodies("/children").single())
                        .jsonObject["children"]!!
                        .jsonArray

                // Document order is preserved and every kind is rewritten to its file_upload form,
                // each block carrying the upload of the file its own sentinel held.
                listOf(
                    "image" to "chart.png",
                    "video" to "clip.mp4",
                    "audio" to "theme.mp3",
                    "file" to "data.csv",
                    "pdf" to "appendix.pdf",
                ).forEachIndexed { index, (type, filename) ->
                    val content = children[index].jsonObject[type]!!.jsonObject
                    children[index].jsonObject["type"]!!.jsonPrimitive.content shouldBe type
                    content["type"]!!.jsonPrimitive.content shouldBe "file_upload"
                    content["file_upload"]!!.jsonObject["id"]!!.jsonPrimitive.content shouldEndWith "-$filename"
                }

                // The file block keeps the display name the builder defaulted from the source.
                children[3]
                    .jsonObject["file"]!!
                    .jsonObject["name"]!!
                    .jsonPrimitive.content shouldBe "data.csv"

                // HTML resolves to an embed whose file_upload reference stands in for the url.
                val embed = children[5].jsonObject["embed"]!!.jsonObject
                children[5].jsonObject["type"]!!.jsonPrimitive.content shouldBe "embed"
                embed["url"] shouldBe null
                embed["file_upload"]!!.jsonObject["id"]!!.jsonPrimitive.content shouldEndWith "-embed.html"
            }

            test("sentinels nested inside containers are collected in document order") {
                val recorder = Recorder()
                val client = blocksClient(recorder)

                try {
                    BlocksApi(client, config).appendChildren("page-id") {
                        image(bytes("first.png"))
                        toggle("Details") {
                            columnList {
                                column { image(bytes("second.png")) }
                                column { image(bytes("third.png")) }
                            }
                        }
                        image(bytes("fourth.png"))
                    }
                } finally {
                    client.close()
                }

                // All four files are uploaded, however deeply the sentinel was nested.
                recorder.bodies("POST").count { it.contains("\"filename\"") } shouldBe 4

                // Each upload lands back on its own block, and the blocks keep document order.
                val sent = recorder.bodies("/children").single()
                listOf("first.png", "second.png", "third.png", "fourth.png")
                    .map { filename ->
                        val at = sent.indexOf(filename)
                        at shouldBeGreaterThan -1
                        at
                    }.zipWithNext()
                    .forEach { (earlier, later) -> later shouldBeGreaterThan earlier }
            }

            test("the same file attached twice uploads twice and gets two distinct ids") {
                val recorder = Recorder()
                val client = blocksClient(recorder)
                val source = bytes("chart.png")

                try {
                    BlocksApi(client, config).appendChildren("page-id") {
                        image(source)
                        image(source)
                    }
                } finally {
                    client.close()
                }

                recorder.urls().count { it.endsWith("/file_uploads") } shouldBe 2
                val sent = recorder.bodies("/children").single()
                sent shouldContain "1-chart.png"
                sent shouldContain "2-chart.png"
            }

            test("content without sentinels makes no upload calls at all") {
                val recorder = Recorder()
                val client = blocksClient(recorder)

                try {
                    BlocksApi(client, config).appendChildren("page-id") {
                        paragraph("No files here")
                        image("https://example.com/chart.png")
                    }
                } finally {
                    client.close()
                }

                recorder.urls().none { it.contains("/file_uploads") } shouldBe true
            }

            test("blocks.update resolves the single block it sends") {
                val recorder = Recorder()
                val client = client(recorder) { TestFixtures.Blocks.retrieveBlockAsString() }

                try {
                    BlocksApi(client, config).update("block-id") { image(bytes("chart.png")) }
                } finally {
                    client.close()
                }

                val update = recorder.bodies("PATCH").single { !it.contains("\"children\"") }
                update shouldContain """"type":"image""""
                update shouldContain "1-chart.png"
            }

            test("pages.create resolves children before sending the page") {
                val recorder = Recorder()
                val client = client(recorder) { TestFixtures.Pages.retrievePageAsString() }

                try {
                    PagesApi(client, config).create(
                        CreatePageRequest(
                            parent = Parent.PageParent("parent-id"),
                            properties = emptyMap(),
                            children = pageContent { image(bytes("chart.png")) },
                        ),
                    )
                } finally {
                    client.close()
                }

                val create = recorder.bodies("POST").single { it.contains("\"parent\"") }
                create shouldContain """"type":"image""""
                create shouldContain "1-chart.png"
            }
        }

        context("partial failure") {
            test("a failed upload aborts the call before any block request is sent") {
                val recorder = Recorder()
                val client = blocksClient(recorder, failUploadNumber = 2)

                try {
                    shouldThrow<FileUploadError> {
                        BlocksApi(client, config).appendChildren("page-id") {
                            image(bytes("first.png"))
                            image(bytes("second.png"))
                        }
                    }
                } finally {
                    client.close()
                }

                recorder.urls().none { it.contains("/children") } shouldBe true
            }

            test("a failed upload aborts pages.create before the page is created") {
                val recorder = Recorder()
                val client = client(recorder, failUploadNumber = 1) { TestFixtures.Pages.retrievePageAsString() }

                try {
                    shouldThrow<FileUploadError> {
                        PagesApi(client, config).create(
                            CreatePageRequest(
                                parent = Parent.PageParent("parent-id"),
                                properties = emptyMap(),
                                children = pageContent { image(bytes("chart.png")) },
                            ),
                        )
                    }
                } finally {
                    client.close()
                }

                recorder.urls().none { it.endsWith("/pages") } shouldBe true
            }
        }
    })
