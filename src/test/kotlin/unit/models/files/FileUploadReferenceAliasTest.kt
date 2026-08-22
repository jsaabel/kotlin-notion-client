package unit.models.files

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.blocks.EmbedRequestContent
import it.saabel.kotlinnotionclient.models.pages.PageCover
import kotlinx.serialization.json.Json
import it.saabel.kotlinnotionclient.models.base.FileUploadReference as BaseReference
import it.saabel.kotlinnotionclient.models.files.FileUploadReference as FilesReference

/**
 * There used to be two identical `FileUploadReference` declarations — one in `models.base` used by
 * covers and icons, one in `models.files` used by blocks and page properties. They serialized
 * identically, but a caller could not pass one where the other was expected (issue #68, item 5).
 *
 * `models.files.FileUploadReference` is now a typealias for the canonical `models.base` type, so
 * these tests are the guard against the split being reintroduced.
 */
@Tags("Unit")
class FileUploadReferenceAliasTest :
    FunSpec({
        val json = Json { encodeDefaults = true }
        val uploadId = "b52b8ed6-e029-4707-a671-832549c09de3"

        test("the two import paths resolve to the same type") {
            val fromFiles: FilesReference = FilesReference(uploadId)
            val fromBase: BaseReference = fromFiles

            fromBase shouldBe fromFiles
            BaseReference::class shouldBe FilesReference::class
        }

        test("one value is accepted everywhere a reference is expected") {
            val reference = FilesReference(uploadId)

            // Cover and icon previously required the models.base declaration...
            PageCover.FileUpload(reference).fileUpload.id shouldBe uploadId
            Icon.FileUpload(reference).fileUpload.id shouldBe uploadId

            // ...while blocks required the models.files one.
            EmbedRequestContent(fileUpload = reference).fileUpload?.id shouldBe uploadId
        }

        test("serializes to the wire shape Notion expects") {
            json.encodeToString(BaseReference(uploadId)) shouldBe """{"id":"$uploadId"}"""
        }
    })
