package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.utils.asFileSource

/**
 * Live-API adjudication for the three findings in issue #69 that the reference documentation
 * cannot settle, plus a smoke test of the one-call helpers against a real workspace.
 *
 * Three questions this test exists to answer:
 * 1. **Does `icon`/`cover` accept `type: "file_upload"` on write?** The models existed but were
 *    unreachable from every DSL, so nothing had ever sent one.
 * 2. **What does Notion do with a `type: "file"` icon on a write?** The Page object reference
 *    says icon accepts only `external` or `file_upload`, which is why `icon.file(url)` is now
 *    deprecated — this test records what the API actually does with it.
 * 3. **Does an embed accept a `caption`?** `EmbedRequestContent` deliberately still has no
 *    caption field: the reference documents only `url` for embeds, and adding an unverified
 *    field to a public builder is worse than leaving the gap open. The probe below sends a
 *    caption as raw JSON instead, so the answer can be had without shipping the field first.
 *    If Notion accepts it, add `caption` to `EmbedRequestContent`, to `embed`/`embedFromUpload`,
 *    and to `EmbedContent` on the read side.
 *
 * A failure here is a finding, not necessarily a bug. Run with:
 * `./gradlew integrationTest --tests "*FileAttachIntegrationTest"`
 */
@Tags("Integration", "RequiresApi")
class FileAttachIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "(Skipped) file attach integration — env gate not satisfied" {
                println("Skipping FileAttachIntegrationTest — set required env vars (see .env.example)")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            /** A 1x1 transparent PNG — the smallest thing Notion will accept as an icon. */
            val pngBytes =
                java.util.Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
                )

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("File Attach — Integration Tests")
                        icon.emoji("📎")
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

            "setIcon writes an uploaded file as the page icon" {
                val page = notion.pages.setIcon(containerPageId, pngBytes.asFileSource("icon.png"))

                println("🔎 FINDING: icon after setIcon = ${page.icon}")
                // Notion resolves a written file_upload icon into its read shape on the way back,
                // so accept either — what matters is that the write was accepted and stuck.
                (page.icon is Icon.File || page.icon is Icon.FileUpload) shouldBe true
            }

            "setCover writes an uploaded file as the page cover" {
                val page = notion.pages.setCover(containerPageId, pngBytes.asFileSource("cover.png"))

                println("🔎 FINDING: cover after setCover = ${page.cover}")
                (page.cover is PageCover.File || page.cover is PageCover.FileUpload) shouldBe true
            }

            "a type:\"file\" icon on write is rejected or ignored" {
                // The deprecated icon.file(url) path. The Page object reference says icon accepts
                // only external or file_upload on write; this records what actually happens.
                val outcome =
                    runCatching {
                        @Suppress("DEPRECATION")
                        notion.pages.update(containerPageId) {
                            icon.file("https://www.notion.so/images/favicon.ico")
                        }
                    }

                outcome.fold(
                    onSuccess = { println("🔎 FINDING: type:\"file\" icon ACCEPTED on write — icon is now ${it.icon}") },
                    onFailure = { println("🔎 FINDING: type:\"file\" icon REJECTED on write — ${it.message}") },
                )
                // Either outcome is information, not a failure — the deprecation stands on the
                // documented contract regardless.
            }

            "appendHtml turns a raw HTML string into an HTML block in one call" {
                val response =
                    notion.blocks.appendHtml(
                        containerPageId,
                        """
                        <!DOCTYPE html>
                        <html lang="en"><head><meta charset="utf-8"><title>One-call HTML</title></head>
                        <body><h1>Uploaded and attached in one call</h1></body></html>
                        """.trimIndent(),
                        filename = "one-call",
                    )

                val block = response.results.single()
                println("🔎 FINDING: appendHtml created: $block")
                block.shouldBeInstanceOf<Block.Embed>()
                (block.embed.url ?: block.embed.file ?: block.embed.fileUpload).shouldNotBeNull()
            }

            "appendImage and appendFile attach uploads in one call" {
                val image = notion.blocks.appendImage(containerPageId, pngBytes.asFileSource("pixel.png"), caption = "one-call image")
                image.results.single().shouldBeInstanceOf<Block.Image>()

                val file = notion.blocks.appendFile(containerPageId, "hello".toByteArray().asFileSource("note.txt"))
                file.results.single().shouldBeInstanceOf<Block.File>()
            }

            "PROBE: does an embed block accept a caption?" {
                // Sent as raw JSON on purpose: EmbedRequestContent has no caption field yet, and
                // this probe is what decides whether it should get one.
                val raw = HttpClient(CIO)
                val body =
                    """
                    {
                      "children": [
                        {
                          "object": "block",
                          "type": "embed",
                          "embed": {
                            "url": "https://example.com",
                            "caption": [{ "type": "text", "text": { "content": "probe caption" } }]
                          }
                        }
                      ]
                    }
                    """.trimIndent()

                val response =
                    raw.patch("https://api.notion.com/v1/blocks/$containerPageId/children") {
                        header("Authorization", "Bearer $token")
                        header("Notion-Version", NotionConfig(apiToken = token).apiVersion)
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                val text = response.bodyAsText()
                raw.close()

                println("🔎 FINDING: embed caption probe -> HTTP ${response.status.value}")
                println("🔎 FINDING: embed caption probe body -> ${text.take(1200)}")
                // No assertion: the printed response is the finding. A 200 whose echoed block
                // carries the caption means EmbedRequestContent should gain the field.
            }
        }
    })
