package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import it.saabel.kotlinnotionclient.models.files.CreateFileUploadRequest
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus
import kotlinx.coroutines.delay

/**
 * Integration test for HTML blocks via embed + file upload (Jul 3 2026 changelog;
 * discussed under issue #61).
 *
 * The changelog says: upload a `.html` file via the File Upload API, then attach it via
 * `embed.file_upload` when appending block children. **Both the request and response
 * shapes are inferred** — the reference documents only the `url` embed form — so this
 * test's job is to adjudicate:
 * - whether `{"embed": {"file_upload": {"id": ...}}}` is accepted at all
 * - what the created block looks like when read back (type? url? file? file_upload?)
 *
 * A failure here is a finding, not (necessarily) a bug: it likely means the inferred
 * wire shape is wrong and the model needs adjusting to whatever the API actually wants.
 *
 * Run with: ./gradlew integrationTest --tests "*HtmlEmbedIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class HtmlEmbedIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "(Skipped) html embed integration — env gate not satisfied" {
                println("Skipping HtmlEmbedIntegrationTest — set required env vars (see .env.example)")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("HTML Embed — Integration Tests")
                        icon.emoji("🌐")
                    }
                containerPageId = container.id
                println("📄 Container: ${container.url}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("✅ Cleaned up container page (all children trashed)")
                } else {
                    println("🔧 Cleanup skipped — container page preserved for inspection")
                }
                notion.close()
            }

            "uploaded html file attaches as an embed block" {
                val html =
                    """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head><meta charset="utf-8"><title>HTML block fixture</title></head>
                    <body>
                      <h1>HTML block integration fixture</h1>
                      <p>Uploaded via the File Upload API and attached via embed.file_upload.</p>
                    </body>
                    </html>
                    """.trimIndent()

                val upload =
                    notion.fileUploads.createFileUpload(
                        CreateFileUploadRequest(filename = "html-block-fixture.html", contentType = "text/html"),
                    )
                println("🔎 upload created: id=${upload.id}, status=${upload.status}")
                notion.fileUploads.sendFileUpload(upload, html.toByteArray())
                delay(2000)
                val uploaded = notion.fileUploads.retrieveFileUpload(upload.id)
                println("🔎 upload after send: status=${uploaded.status}")
                uploaded.status shouldBe FileUploadStatus.UPLOADED

                val response =
                    notion.blocks.appendChildren(
                        containerPageId,
                        pageContent {
                            heading2("HTML block below")
                            embedFromUpload(uploaded.id)
                        },
                    )
                println("🔎 FINDING: embed.file_upload ACCEPTED — appended ${response.results.size} blocks")
                response.results shouldHaveSize 2

                delay(1000)
                val children = notion.blocks.retrieveChildren(containerPageId)
                val embedLike =
                    children.filterNot { it is Block.Heading2 }.also { candidates ->
                        candidates.forEach { block ->
                            println("🔎 FINDING: created block read back as: $block")
                        }
                    }
                embedLike shouldHaveSize 1

                val block = embedLike.single()
                when (block) {
                    is Block.Embed -> {
                        println(
                            "🔎 FINDING: read back as embed — url=${block.embed.url}, " +
                                "file=${block.embed.file}, fileUpload=${block.embed.fileUpload}",
                        )
                        (block.embed.url ?: block.embed.file ?: block.embed.fileUpload).shouldNotBeNull()
                    }

                    else -> {
                        // Not necessarily wrong — the API may materialize an uploaded html
                        // embed as a different (possibly unknown) block type. The println
                        // above is the finding; fail softly so the shape gets discussed.
                        println("🔎 FINDING: created block is not Block.Embed but ${block::class.simpleName}")
                    }
                }
            }
        }
    })
