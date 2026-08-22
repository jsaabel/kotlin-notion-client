package integration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.utils.asFileSource

/**
 * Live-API regression tests for the three findings in issue #69 that the reference documentation
 * could not settle, plus a smoke test of the one-call helpers against a real workspace.
 *
 * All three were adjudicated on 2026-08-22 (API version 2026-03-11); this file now guards the
 * answers rather than asking the questions:
 * 1. **`icon`/`cover` accept `type: "file_upload"` on write** — and read back as `Icon.File` /
 *    `PageCover.File`, a time-limited signed S3 URL. The write shape and the read shape differ.
 * 2. **A `type: "file"` icon is rejected on write** with HTTP 400 `validation_error`, which
 *    names `emoji`, `external`, `custom_emoji`, `file_upload` and `icon` as the accepted set.
 *    That is why `icon.file(url)` / `cover.file(url)` are deprecated.
 * 3. **An embed accepts a `caption`** — undocumented, but written and echoed back with the same
 *    rich-text shape every other caption uses. `EmbedRequestContent`, `EmbedContent` and the
 *    `embed`/`embedFromUpload` builders carry it now.
 *
 * A failure here is a finding: it means the live API moved away from one of the above. Run with:
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

            "setIcon writes an uploaded file as the page icon, which reads back as Icon.File" {
                val page = notion.pages.setIcon(containerPageId, pngBytes.asFileSource("icon.png"))

                // Written as file_upload, read back as the Notion-hosted read shape.
                val icon = page.icon.shouldBeInstanceOf<Icon.File>()
                icon.file.url shouldContain "icon.png"
                icon.file.expiryTime.shouldNotBeNull()
            }

            "setCover writes an uploaded file as the page cover, which reads back as PageCover.File" {
                val page = notion.pages.setCover(containerPageId, pngBytes.asFileSource("cover.png"))

                val cover = page.cover.shouldBeInstanceOf<PageCover.File>()
                cover.file.url shouldContain "cover.png"
            }

            "a type:\"file\" icon is rejected on write, which is why icon.file is deprecated" {
                val error =
                    shouldThrow<NotionException.ApiError> {
                        @Suppress("DEPRECATION")
                        notion.pages.update(containerPageId) {
                            icon.file("https://www.notion.so/images/favicon.ico")
                        }
                    }

                error.message shouldContain "validation_error"
                // The 400 enumerates what icon does accept — no `file` among them.
                error.message shouldContain "body.icon.file_upload should be defined"
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

            "an embed block accepts a caption and echoes it back" {
                // Undocumented — the embed reference lists only `url` — but accepted, and the
                // reason EmbedRequestContent/EmbedContent carry a caption at all.
                val response =
                    notion.blocks.appendChildren(containerPageId) {
                        embed("https://example.com", caption = "captioned embed")
                    }

                val block = response.results.single().shouldBeInstanceOf<Block.Embed>()
                block.embed.caption
                    .single()
                    .plainText shouldBe "captioned embed"
            }

            "appendHtml carries a caption onto the HTML block" {
                val response =
                    notion.blocks.appendHtml(
                        containerPageId,
                        "<p>captioned html</p>",
                        filename = "captioned",
                        caption = "generated nightly",
                    )

                val block = response.results.single().shouldBeInstanceOf<Block.Embed>()
                block.embed.caption
                    .single()
                    .plainText shouldBe "generated nightly"
            }
        }
    })
