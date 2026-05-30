package unit.models.pages

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.files.FileUploadReference
import it.saabel.kotlinnotionclient.models.pages.FileObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import unit.util.TestFixtures

@Tags("Unit")
class FileObjectFileUploadTest :
    FunSpec({
        // Mirrors the client's serialization config (NotionClient.kt) where it matters:
        // explicitNulls = false so an absent name is omitted entirely.
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }

        val uploadId = "c2c5c4a4-1f1f-4f1f-9f1f-1f1f1f1f1f1f"

        context("FileObject.FileUpload serialization") {
            test("encodes to the documented request shape (type, file_upload, name)") {
                val value: FileObject = FileObject.FileUpload(FileUploadReference(uploadId), name = "x.pdf")

                val encoded = json.encodeToString<FileObject>(value)

                encoded shouldBe """{"type":"file_upload","file_upload":{"id":"$uploadId"},"name":"x.pdf"}"""
            }

            test("omits the name field entirely when name is null") {
                val value: FileObject = FileObject.FileUpload(FileUploadReference(uploadId), name = null)

                val encoded = json.encodeToString<FileObject>(value)

                encoded shouldBe """{"type":"file_upload","file_upload":{"id":"$uploadId"}}"""
            }

            test("round-trips with a name through encode/decode") {
                val original: FileObject = FileObject.FileUpload(FileUploadReference(uploadId), name = "x.pdf")

                val decoded = json.decodeFromString<FileObject>(json.encodeToString(original))

                decoded shouldBe original
            }

            test("round-trips without a name through encode/decode") {
                val original: FileObject = FileObject.FileUpload(FileUploadReference(uploadId))

                val decoded = json.decodeFromString<FileObject>(json.encodeToString(original))

                decoded shouldBe original
            }
        }

        context("FileObject.Uploaded (response shape) still decodes") {
            test("decodes type=file with file.url and expiry_time into Uploaded") {
                val raw =
                    """
                    {
                      "type": "file",
                      "name": "blueprint.pdf",
                      "file": {
                        "url": "https://example.com/blueprint.pdf",
                        "expiry_time": "2026-05-30T20:00:00.000Z"
                      }
                    }
                    """.trimIndent()

                val decoded = json.decodeFromString<FileObject>(raw)

                val uploaded = decoded.shouldBeInstanceOf<FileObject.Uploaded>()
                uploaded.name shouldBe "blueprint.pdf"
                uploaded.file.url shouldBe "https://example.com/blueprint.pdf"
                uploaded.file.expiryTime shouldBe "2026-05-30T20:00:00.000Z"
            }
        }

        context("FileObject companion helpers") {
            test("upload(id, name) builds a FileUpload variant") {
                FileObject.upload(uploadId, name = "x.pdf") shouldBe
                    FileObject.FileUpload(FileUploadReference(uploadId), name = "x.pdf")
            }

            test("upload(id) defaults name to null") {
                FileObject.upload(uploadId) shouldBe
                    FileObject.FileUpload(FileUploadReference(uploadId), name = null)
            }

            test("external(name, url) builds an External variant") {
                val external = FileObject.external(name = "blueprint", url = "https://example.com/b.pdf")

                external.name shouldBe "blueprint"
                external.external.url shouldBe "https://example.com/b.pdf"
            }
        }

        context("committed request-body fixture") {
            test("decodes the file object inside patch_files_property_with_upload.json") {
                val body =
                    TestFixtures.loadSampleResponse("pages", "patch_files_property_with_upload") as JsonObject

                val fileElement =
                    body["properties"]!!
                        .jsonObject["Blueprint"]!!
                        .jsonObject["files"]!!
                        .jsonArray
                        .first()

                val decoded = json.decodeFromJsonElement(FileObject.serializer(), fileElement)

                val fileUpload = decoded.shouldBeInstanceOf<FileObject.FileUpload>()
                fileUpload.fileUpload.id shouldBe "c2c5c4a4-1f1f-4f1f-9f1f-1f1f1f1f1f1f"
                fileUpload.name shouldBe "project-alpha-blueprint.pdf"
            }
        }
    })
