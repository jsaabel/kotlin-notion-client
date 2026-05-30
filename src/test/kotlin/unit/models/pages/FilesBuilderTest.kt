package unit.models.pages

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.pages.ExternalFileUrl
import it.saabel.kotlinnotionclient.models.pages.FileData
import it.saabel.kotlinnotionclient.models.pages.FileObject
import it.saabel.kotlinnotionclient.models.pages.FilesBuilder
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.UploadedFileUrl
import it.saabel.kotlinnotionclient.models.pages.pageProperties
import kotlinx.serialization.json.Json

@Tags("Unit")
class FilesBuilderTest :
    FunSpec({
        // Mirrors the client's serialization config (NotionClient.kt) where it matters.
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }

        val uploadId = "c2c5c4a4-1f1f-4f1f-9f1f-1f1f1f1f1f1f"

        context("FilesBuilder methods produce the correct FileObject subtype") {
            test("upload(id, name) produces a FileUpload with the given name") {
                val files = FilesBuilder().apply { upload(uploadId, name = "report.pdf") }.build()

                files shouldHaveSize 1
                val fileUpload = files.first().shouldBeInstanceOf<FileObject.FileUpload>()
                fileUpload.fileUpload.id shouldBe uploadId
                fileUpload.name shouldBe "report.pdf"
            }

            test("upload(id) defaults the name to null") {
                val files = FilesBuilder().apply { upload(uploadId) }.build()

                val fileUpload = files.first().shouldBeInstanceOf<FileObject.FileUpload>()
                fileUpload.fileUpload.id shouldBe uploadId
                fileUpload.name shouldBe null
            }

            test("external(name, url) produces an External file object") {
                val files = FilesBuilder().apply { external("Spec doc", "https://example.com/spec.pdf") }.build()

                val external = files.first().shouldBeInstanceOf<FileObject.External>()
                external.name shouldBe "Spec doc"
                external.external.url shouldBe "https://example.com/spec.pdf"
            }

            test("add(file) appends a pre-built FileObject unchanged") {
                val prebuilt = FileObject.upload(uploadId, name = "manual.pdf")
                val files = FilesBuilder().apply { add(prebuilt) }.build()

                files shouldHaveSize 1
                files.first() shouldBe prebuilt
            }

            test("methods accumulate in insertion order") {
                val files =
                    FilesBuilder()
                        .apply {
                            upload(uploadId, name = "a.pdf")
                            external("b", "https://example.com/b.pdf")
                        }.build()

                files shouldHaveSize 2
                files[0].shouldBeInstanceOf<FileObject.FileUpload>()
                files[1].shouldBeInstanceOf<FileObject.External>()
            }
        }

        context("existing() routes both FileData subtypes to the matching FileObject subtype") {
            test("existing(FileData.External) routes to FileObject.External preserving name and url") {
                val source =
                    FileData.External(
                        name = "Spec doc",
                        type = "external",
                        external = ExternalFileUrl("https://example.com/spec.pdf"),
                    )

                val files = FilesBuilder().apply { existing(source) }.build()

                val external = files.first().shouldBeInstanceOf<FileObject.External>()
                external.name shouldBe "Spec doc"
                external.external shouldBe source.external
            }

            test("existing(FileData.Uploaded) routes to FileObject.Uploaded preserving name and url") {
                val source =
                    FileData.Uploaded(
                        name = "data.txt",
                        type = "file",
                        file = UploadedFileUrl(url = "https://example.com/data.txt", expiryTime = "2026-05-30T20:00:00.000Z"),
                    )

                val files = FilesBuilder().apply { existing(source) }.build()

                val uploaded = files.first().shouldBeInstanceOf<FileObject.Uploaded>()
                uploaded.name shouldBe "data.txt"
                uploaded.file shouldBe source.file
            }
        }

        context("PagePropertiesBuilder.files entry points") {
            test("files(name) { ... } DSL produces the expected FilesValue") {
                val properties =
                    pageProperties {
                        files("Attachments") {
                            upload(uploadId, name = "report.pdf")
                            external("Spec doc", "https://example.com/spec.pdf")
                        }
                    }

                properties shouldContainKey "Attachments"
                val filesValue = properties["Attachments"].shouldBeInstanceOf<PagePropertyValue.FilesValue>()
                filesValue.files shouldHaveSize 2
                filesValue.files[0].shouldBeInstanceOf<FileObject.FileUpload>()
                filesValue.files[1].shouldBeInstanceOf<FileObject.External>()
            }

            test("vararg overload emits JSON equivalent to the DSL form for the same files") {
                val dsl =
                    pageProperties {
                        files("Attachments") {
                            upload(uploadId, name = "report.pdf")
                            external("Spec doc", "https://example.com/spec.pdf")
                        }
                    }

                val vararg =
                    pageProperties {
                        files(
                            "Attachments",
                            FileObject.upload(uploadId, name = "report.pdf"),
                            FileObject.external("Spec doc", "https://example.com/spec.pdf"),
                        )
                    }

                json.encodeToString(vararg["Attachments"]!!) shouldBe json.encodeToString(dsl["Attachments"]!!)
            }

            test("List overload emits JSON equivalent to the DSL form for the same files") {
                val dsl =
                    pageProperties {
                        files("Attachments") {
                            upload(uploadId, name = "report.pdf")
                            external("Spec doc", "https://example.com/spec.pdf")
                        }
                    }

                val fromList =
                    pageProperties {
                        files(
                            "Attachments",
                            listOf(
                                FileObject.upload(uploadId, name = "report.pdf"),
                                FileObject.external("Spec doc", "https://example.com/spec.pdf"),
                            ),
                        )
                    }

                json.encodeToString(fromList["Attachments"]!!) shouldBe json.encodeToString(dsl["Attachments"]!!)
            }
        }
    })
