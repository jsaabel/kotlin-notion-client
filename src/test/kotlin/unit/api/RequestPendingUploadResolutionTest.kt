package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import it.saabel.kotlinnotionclient.api.CommentsApi
import it.saabel.kotlinnotionclient.api.DataSourcesApi
import it.saabel.kotlinnotionclient.api.DatabasesApi
import it.saabel.kotlinnotionclient.api.PagesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.comments.CommentAttachmentRequest
import it.saabel.kotlinnotionclient.models.comments.createCommentRequest
import it.saabel.kotlinnotionclient.models.files.FileUploadError
import it.saabel.kotlinnotionclient.models.pages.FileObject
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import it.saabel.kotlinnotionclient.models.pages.updatePageRequest
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
 * Stage 2 of ADR 0001: the pending-upload sentinel across the remaining attachment surfaces —
 * files properties, icon, cover and comment attachments — and the whole-request resolution
 * that pools them.
 *
 * The block surface and the resolver's core contract are covered by
 * [PendingUploadResolutionTest]; these tests are about what a *request* carries, so the
 * assertions look at the JSON that reaches the wire rather than at the block tree.
 */
@Tags("Unit")
class RequestPendingUploadResolutionTest :
    FunSpec({
        val config = NotionConfig(apiToken = "test-token")

        fun bytes(name: String) = byteArrayOf(1, 2, 3).asFileSource(name)

        fun pagesClient(
            recorder: Recorder,
            failUploadNumber: Int? = null,
        ) = client(recorder, failUploadNumber) { TestFixtures.Pages.retrievePageAsString() }

        /** The single page create or update the recorder saw, parsed. */
        fun Recorder.pageRequest() = Json.parseToJsonElement(bodies("/pages").single()).jsonObject

        context("the sentinels") {
            test("a files property records the local file instead of uploading it") {
                val request =
                    createPageRequest {
                        parent.dataSource("data-source-id")
                        properties { files("Attachments") { upload(bytes("report.pdf")) } }
                    }

                val files = (request.properties["Attachments"] as PagePropertyValue.FilesValue).files
                val sentinel = files.single() as FileObject.PendingUpload
                sentinel.source.filename shouldBe "report.pdf"
                sentinel.name shouldBe null
            }

            test("icon and cover record the local file instead of uploading it") {
                val request =
                    createPageRequest {
                        parent.page("parent-id")
                        icon.upload(bytes("logo.png"))
                        cover.upload(bytes("hero.png"))
                    }

                (request.icon as Icon.PendingUpload).source.filename shouldBe "logo.png"
                (request.cover as PageCover.PendingUpload).source.filename shouldBe "hero.png"
            }

            test("a comment attachment records the local file instead of uploading it") {
                val request =
                    createCommentRequest {
                        parent.page("page-id")
                        content { text("Trace attached") }
                        attachment(bytes("trace.txt"))
                    }

                val sentinel = request.attachments!!.single() as CommentAttachmentRequest.PendingUpload
                sentinel.source.filename shouldBe "trace.txt"
                sentinel.fileUploadId shouldBe null
            }

            test("the File and Path overloads record the file behind them") {
                val temp =
                    kotlin.io.path
                        .createTempFile("logo", ".png")
                        .toFile()
                temp.writeBytes(byteArrayOf(1, 2, 3))
                temp.deleteOnExit()

                try {
                    val request =
                        createPageRequest {
                            parent.dataSource("data-source-id")
                            icon.upload(temp)
                            cover.upload(temp.toPath())
                            properties {
                                files("Attachments") {
                                    upload(temp)
                                    upload(temp.toPath(), name = "Renamed.png")
                                }
                            }
                        }

                    (request.icon as Icon.PendingUpload).source.filename shouldBe temp.name
                    (request.cover as PageCover.PendingUpload).source.filename shouldBe temp.name

                    val files = (request.properties["Attachments"] as PagePropertyValue.FilesValue).files
                    files.map { (it as FileObject.PendingUpload).name } shouldContainExactly listOf(null, "Renamed.png")
                } finally {
                    temp.delete()
                }
            }

            test("serializing an unresolved files-property sentinel throws with an actionable message") {
                val request =
                    createPageRequest {
                        parent.dataSource("data-source-id")
                        properties { files("Attachments") { upload(bytes("report.pdf")) } }
                    }

                val error = shouldThrow<SerializationException> { Json.encodeToString(request) }

                error.message.orEmpty() shouldContain "report.pdf"
                error.message.orEmpty() shouldContain "pages.create"
            }

            test("serializing an unresolved icon sentinel throws with an actionable message") {
                val icon: Icon = Icon.PendingUpload(source = bytes("logo.png"))

                val error = shouldThrow<SerializationException> { Json.encodeToString(icon) }

                error.message.orEmpty() shouldContain "logo.png"
            }

            test("serializing an unresolved cover sentinel throws with an actionable message") {
                val cover: PageCover = PageCover.PendingUpload(source = bytes("hero.png"))

                val error = shouldThrow<SerializationException> { Json.encodeToString(cover) }

                error.message.orEmpty() shouldContain "hero.png"
            }

            test("serializing an unresolved comment attachment throws with an actionable message") {
                val request =
                    createCommentRequest {
                        parent.page("page-id")
                        content { text("Trace attached") }
                        attachment(bytes("trace.txt"))
                    }

                val error = shouldThrow<SerializationException> { Json.encodeToString(request) }

                error.message.orEmpty() shouldContain "trace.txt"
                error.message.orEmpty() shouldContain "comments.create"
            }

            test("the validator reports unresolved sentinels on every page surface") {
                val request =
                    createPageRequest {
                        parent.dataSource("data-source-id")
                        icon.upload(bytes("logo.png"))
                        cover.upload(bytes("hero.png"))
                        properties { files("Attachments") { upload(bytes("report.pdf")) } }
                    }

                val error = shouldThrow<ValidationException> { RequestValidator().validateOrFix(request) }

                val violations = error.validationResult.violations
                violations.map { it.violationType }.toSet() shouldBe setOf(ViolationType.UNRESOLVED_PENDING_UPLOAD)
                violations.map { it.field } shouldContainExactly
                    listOf("Attachments.files[0]", "icon", "cover")
                violations.map { it.currentValue } shouldContainExactly
                    listOf("report.pdf", "logo.png", "hero.png")
            }

            test("the validator reports an unresolved icon on a page update too") {
                val request = updatePageRequest { icon.upload(bytes("logo.png")) }

                val error = shouldThrow<ValidationException> { RequestValidator().validateOrFix(request) }

                error.validationResult.violations
                    .single()
                    .violationType shouldBe ViolationType.UNRESOLVED_PENDING_UPLOAD
            }
        }

        context("whole-request resolution") {
            test("one page create resolves icon, cover, a files property and content in a single pass") {
                val recorder = Recorder()
                val client = pagesClient(recorder)

                try {
                    PagesApi(client, config).create {
                        parent.dataSource("data-source-id")
                        title("Q3 Report")
                        icon.upload(bytes("logo.png"))
                        cover.upload(bytes("hero.png"))
                        properties {
                            files("Attachments") {
                                upload(bytes("a.pdf"))
                                upload(bytes("b.pdf"))
                            }
                        }
                        content { image(bytes("chart.png")) }
                    }
                } finally {
                    client.close()
                }

                // Five files across four surfaces, one pooled upload pass, one page create.
                recorder.uploadCount() shouldBe 5
                recorder.bodies("/pages").size shouldBe 1

                val body = recorder.pageRequest()
                body["icon"]!!
                    .jsonObject["file_upload"]!!
                    .jsonObject["id"]!!
                    .jsonPrimitive.content shouldEndWith "-logo.png"
                body["cover"]!!
                    .jsonObject["file_upload"]!!
                    .jsonObject["id"]!!
                    .jsonPrimitive.content shouldEndWith "-hero.png"

                val files =
                    body["properties"]!!
                        .jsonObject["Attachments"]!!
                        .jsonObject["files"]!!
                        .jsonArray
                files
                    .map {
                        it.jsonObject["file_upload"]!!
                            .jsonObject["id"]!!
                            .jsonPrimitive.content
                    }.map { it.substringAfter("-") } shouldContainExactly listOf("a.pdf", "b.pdf")

                body["children"]!!
                    .jsonArray
                    .single()
                    .jsonObject["image"]!!
                    .jsonObject["file_upload"]!!
                    .jsonObject["id"]!!
                    .jsonPrimitive.content shouldEndWith "-chart.png"

                // Nothing pending survived into the payload.
                body.toString() shouldContain "file_upload"
                body.toString().contains("pending_upload") shouldBe false
            }

            test("a files property keeps the display name it was given, and defaults to the filename") {
                val recorder = Recorder()
                val client = pagesClient(recorder)

                try {
                    PagesApi(client, config).create {
                        parent.dataSource("data-source-id")
                        properties {
                            files("Attachments") {
                                upload(bytes("a.pdf"))
                                upload(bytes("b.pdf"), name = "Appendix B")
                            }
                        }
                    }
                } finally {
                    client.close()
                }

                val files = recorder.pageRequest()["properties"]!!.jsonObject["Attachments"]!!.jsonObject["files"]!!

                files.toString() shouldContain """"name":"a.pdf""""
                files.toString() shouldContain """"name":"Appendix B""""
            }

            test("pages.update resolves icon, cover and files properties") {
                val recorder = Recorder()
                val client = pagesClient(recorder)

                try {
                    PagesApi(client, config).update("page-id") {
                        icon.upload(bytes("logo.png"))
                        cover.upload(bytes("hero.png"))
                        properties { files("Attachments") { upload(bytes("report.pdf")) } }
                    }
                } finally {
                    client.close()
                }

                recorder.uploadCount() shouldBe 3
                val body = recorder.pageRequest().toString()
                body shouldContain "-logo.png"
                body shouldContain "-hero.png"
                body shouldContain "-report.pdf"
            }

            test("databases.create resolves its icon and cover") {
                val recorder = Recorder()
                val client = client(recorder) { TestFixtures.Databases.retrieveDatabaseAsString() }

                try {
                    DatabasesApi(client, config).create {
                        parent.page("parent-id")
                        title("Tasks")
                        icon.upload(bytes("logo.png"))
                        cover.upload(bytes("hero.png"))
                        properties { title("Name") }
                    }
                } finally {
                    client.close()
                }

                recorder.uploadCount() shouldBe 2
                val body = recorder.bodies("/databases").first()
                body shouldContain "-logo.png"
                body shouldContain "-hero.png"
            }

            test("dataSources.update resolves its icon") {
                val recorder = Recorder()
                val client = client(recorder) { TestFixtures.DataSources.retrieveDataSourceAsString() }

                try {
                    DataSourcesApi(client, config).update("data-source-id") {
                        icon.upload(bytes("logo.png"))
                    }
                } finally {
                    client.close()
                }

                recorder.uploadCount() shouldBe 1
                recorder.bodies("/data_sources").single() shouldContain "-logo.png"
            }

            test("a request without local files makes no upload calls at all") {
                val recorder = Recorder()
                val client = pagesClient(recorder)

                try {
                    PagesApi(client, config).create {
                        parent.dataSource("data-source-id")
                        title("Plain")
                        icon.emoji("📄")
                        properties { files("Attachments") { external("Spec", "https://example.com/spec.pdf") } }
                    }
                } finally {
                    client.close()
                }

                recorder.uploadCount() shouldBe 0
            }

            test("the same file on two surfaces uploads twice and gets two distinct ids") {
                val recorder = Recorder()
                val client = pagesClient(recorder)
                val source = bytes("logo.png")

                try {
                    PagesApi(client, config).create {
                        parent.page("parent-id")
                        icon.upload(source)
                        cover.upload(source)
                    }
                } finally {
                    client.close()
                }

                recorder.uploadCount() shouldBe 2
                val body = recorder.pageRequest()
                val iconId =
                    body["icon"]!!
                        .jsonObject["file_upload"]!!
                        .jsonObject["id"]!!
                        .jsonPrimitive.content
                val coverId =
                    body["cover"]!!
                        .jsonObject["file_upload"]!!
                        .jsonObject["id"]!!
                        .jsonPrimitive.content
                (iconId == coverId) shouldBe false
            }
        }

        context("comment attachments") {
            test("comments.create uploads the DSL's local files and sends their ids") {
                val recorder = Recorder()
                val client = client(recorder) { TestFixtures.Comments.createCommentAsString() }

                try {
                    CommentsApi(client, config).create {
                        parent.page("page-id")
                        content { text("Trace attached") }
                        attachment("existing-upload-id")
                        attachment(bytes("trace.txt"))
                    }
                } finally {
                    client.close()
                }

                recorder.uploadCount() shouldBe 1

                val attachments =
                    Json
                        .parseToJsonElement(recorder.bodies("/comments").single())
                        .jsonObject["attachments"]!!
                        .jsonArray

                // Order is preserved: the id-based attachment stays first.
                val ids = attachments.map { it.jsonObject["file_upload_id"]!!.jsonPrimitive.content }
                ids.first() shouldBe "existing-upload-id"
                ids.last() shouldEndWith "-trace.txt"
                attachments.forEach { it.jsonObject["type"]!!.jsonPrimitive.content shouldBe "file_upload" }
            }

            test("the max-3 rule counts pending attachments as the files they will become") {
                val error =
                    shouldThrow<IllegalArgumentException> {
                        createCommentRequest {
                            parent.page("page-id")
                            content { text("Too many") }
                            attachment(bytes("one.txt"))
                            attachment(bytes("two.txt"))
                            attachment("existing-upload-id")
                            attachment(bytes("four.txt"))
                        }
                    }

                error.message.orEmpty() shouldContain "maximum of 3 attachments"
            }
        }

        context("partial failure across surfaces") {
            test("a failed files-property upload aborts the page create") {
                val recorder = Recorder()
                // The files property is collected first, so its upload is the first minted.
                val client = pagesClient(recorder, failUploadNumber = 1)

                try {
                    shouldThrow<FileUploadError> {
                        PagesApi(client, config).create {
                            parent.dataSource("data-source-id")
                            icon.upload(bytes("logo.png"))
                            properties { files("Attachments") { upload(bytes("report.pdf")) } }
                        }
                    }
                } finally {
                    client.close()
                }

                recorder.urls().none { it.endsWith("/pages") } shouldBe true
            }

            test("a failed cover upload aborts the page create with content blocks pending too") {
                val recorder = Recorder()
                val client = pagesClient(recorder, failUploadNumber = 2)

                try {
                    shouldThrow<FileUploadError> {
                        PagesApi(client, config).create {
                            parent.page("parent-id")
                            icon.upload(bytes("logo.png"))
                            cover.upload(bytes("hero.png"))
                            content { image(bytes("chart.png")) }
                        }
                    }
                } finally {
                    client.close()
                }

                recorder.urls().none { it.endsWith("/pages") } shouldBe true
            }

            test("a failed attachment upload aborts the comment create") {
                val recorder = Recorder()
                val client =
                    client(recorder, failUploadNumber = 1) { TestFixtures.Comments.createCommentAsString() }

                try {
                    shouldThrow<FileUploadError> {
                        CommentsApi(client, config).create {
                            parent.page("page-id")
                            content { text("Trace attached") }
                            attachment(bytes("trace.txt"))
                        }
                    }
                } finally {
                    client.close()
                }

                recorder.urls().none { it.endsWith("/comments") } shouldBe true
            }
        }
    })
