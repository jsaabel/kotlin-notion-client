package unit.dsl

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.base.FileUploadReference
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import it.saabel.kotlinnotionclient.models.comments.createCommentRequest
import it.saabel.kotlinnotionclient.models.databases.databaseRequest
import it.saabel.kotlinnotionclient.models.datasources.updateDataSourceRequest
import it.saabel.kotlinnotionclient.models.files.FileUpload
import it.saabel.kotlinnotionclient.models.pages.FileObject
import it.saabel.kotlinnotionclient.models.pages.FilesBuilder
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import it.saabel.kotlinnotionclient.models.pages.updatePageRequest
import unit.util.TestFixtures

/**
 * Covers the attach-side reach gaps closed for issue #69: `upload(...)` on every icon and cover
 * builder, and [FileUpload]-typed overloads everywhere an upload could previously only be named
 * by a bare id string.
 *
 * The [FileUpload] instance under test is decoded from the official
 * `post_send_a_file_upload` sample rather than hand-built, so the overloads are exercised
 * against the shape the API actually returns.
 */
@Tags("Unit")
class UploadAttachBuilderTest :
    DescribeSpec({

        val upload: FileUpload = TestFixtures.json.decodeFromJsonElement(FileUpload.serializer(), TestFixtures.FileUploads.sendFileUpload())
        val uploadId = upload.id

        describe("icon builders") {
            it("sets a file_upload icon on a page create request") {
                val request =
                    createPageRequest {
                        parent.page("parent-page-id")
                        icon.upload(uploadId)
                    }

                request.icon shouldBe Icon.FileUpload(fileUpload = FileUploadReference(id = uploadId))
            }

            it("sets a file_upload icon on a page update request") {
                val request = updatePageRequest { icon.upload(uploadId) }

                request.icon shouldBe Icon.FileUpload(fileUpload = FileUploadReference(id = uploadId))
            }

            it("sets a file_upload icon on a database request") {
                val request =
                    databaseRequest {
                        parent.page("parent-page-id")
                        title("Files")
                        properties { title("Name") }
                        icon.upload(uploadId)
                    }

                request.icon shouldBe Icon.FileUpload(fileUpload = FileUploadReference(id = uploadId))
            }

            it("sets a file_upload icon on a data source update request") {
                val request = updateDataSourceRequest { icon.upload(uploadId) }

                request.icon shouldBe Icon.FileUpload(fileUpload = FileUploadReference(id = uploadId))
            }

            it("accepts the FileUpload object itself") {
                createPageRequest {
                    parent.page("parent-page-id")
                    icon.upload(upload)
                }.icon shouldBe
                    createPageRequest {
                        parent.page("parent-page-id")
                        icon.upload(uploadId)
                    }.icon
            }

            it("serializes as the documented write shape") {
                val json =
                    TestFixtures.json.encodeToString(
                        Icon.serializer(),
                        createPageRequest {
                            parent.page("parent-page-id")
                            icon.upload(uploadId)
                        }.icon!!,
                    )

                json shouldContain "\"type\": \"file_upload\""
                json shouldContain "\"id\": \"$uploadId\""
            }
        }

        describe("cover builders") {
            it("sets a file_upload cover on a page create request") {
                val request =
                    createPageRequest {
                        parent.page("parent-page-id")
                        cover.upload(uploadId)
                    }

                request.cover shouldBe PageCover.FileUpload(fileUpload = FileUploadReference(id = uploadId))
            }

            it("sets a file_upload cover on a page update request") {
                val request = updatePageRequest { cover.upload(uploadId) }

                request.cover shouldBe PageCover.FileUpload(fileUpload = FileUploadReference(id = uploadId))
            }

            it("sets a file_upload cover on a database request") {
                val request =
                    databaseRequest {
                        parent.page("parent-page-id")
                        title("Files")
                        properties { title("Name") }
                        cover.upload(upload)
                    }

                request.cover shouldBe PageCover.FileUpload(fileUpload = FileUploadReference(id = uploadId))
            }
        }

        describe("content builder overloads") {
            it("routes every media block through the FileUpload overload") {
                val blocks =
                    pageContent {
                        imageFromUpload(upload, caption = "diagram")
                        videoFromUpload(upload)
                        audioFromUpload(upload)
                        pdfFromUpload(upload)
                        embedFromUpload(upload)
                    }

                blocks shouldHaveSize 5
                (blocks[0] as BlockRequest.Image).image.fileUpload shouldBe FileUploadReference(id = uploadId)
                (blocks[0] as BlockRequest.Image)
                    .image.caption
                    .single()
                    .plainText shouldBe "diagram"
                (blocks[1] as BlockRequest.Video).video.fileUpload shouldBe FileUploadReference(id = uploadId)
                (blocks[2] as BlockRequest.Audio).audio.fileUpload shouldBe FileUploadReference(id = uploadId)
                (blocks[3] as BlockRequest.PDF).pdf.fileUpload shouldBe FileUploadReference(id = uploadId)
                (blocks[4] as BlockRequest.Embed).embed.fileUpload shouldBe FileUploadReference(id = uploadId)
            }

            it("carries a caption on an embed built from a url") {
                val block = pageContent { embed("https://example.com", caption = "captioned") }.single() as BlockRequest.Embed

                block.embed.url shouldBe "https://example.com"
                block.embed.caption
                    .single()
                    .plainText shouldBe "captioned"
            }

            it("carries a caption on an embed built from an upload") {
                val block = pageContent { embedFromUpload(upload, caption = "html block") }.single() as BlockRequest.Embed

                block.embed.fileUpload shouldBe FileUploadReference(id = uploadId)
                block.embed.caption
                    .single()
                    .plainText shouldBe "html block"
            }

            it("omits the embed caption entirely when none is given") {
                val block = pageContent { embed("https://example.com") }.single() as BlockRequest.Embed

                block.embed.caption.shouldBeEmpty()
            }

            it("defaults a file block's display name to the upload's own filename") {
                val block = pageContent { fileFromUpload(upload) }.single() as BlockRequest.File

                block.file.name shouldBe upload.filename
            }

            it("lets an explicit name win over the upload's filename") {
                val block = pageContent { fileFromUpload(upload, name = "renamed.txt") }.single() as BlockRequest.File

                block.file.name shouldBe "renamed.txt"
            }
        }

        describe("files property builder") {
            it("attaches an upload object and defaults the name to its filename") {
                val files = FilesBuilder().apply { upload(upload) }.build()

                files.single() shouldBe FileObject.FileUpload(fileUpload = FileUploadReference(id = uploadId), name = upload.filename)
            }

            it("lets an explicit name win") {
                val files = FilesBuilder().apply { upload(upload, name = "renamed.txt") }.build()

                (files.single() as FileObject.FileUpload).name shouldBe "renamed.txt"
            }
        }

        describe("comment attachments") {
            it("accepts the FileUpload object itself") {
                val request =
                    createCommentRequest {
                        parent.page("page-id")
                        content { text("see attached") }
                        attachment(upload)
                    }

                request.attachments!!.single().fileUploadId shouldBe uploadId
            }
        }
    })
