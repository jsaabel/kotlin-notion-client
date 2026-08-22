package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.pages.FileData
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.utils.asFileSource

/**
 * One live end-to-end check of the pending-upload resolution from ADR 0001 (issues #75, #76).
 *
 * The unit tests pin down the mechanism — collection order, the rewrite per surface, failure
 * atomicity — against a mock engine. What they cannot answer is whether Notion accepts the
 * request the resolver produces: a page whose icon, whose files property and whose content
 * blocks all reference uploads created moments earlier in the same call, inside a single
 * `POST /v1/pages`. That is what this asserts, and a failure here is a finding about the live
 * API rather than about the walk.
 *
 * Deliberately one page create, not a matrix: uploads are slow, and both the per-kind mapping
 * and the per-surface mapping are already covered by unit tests. The row is created under a
 * data source because a files property only exists on a database row.
 *
 * Run with: `./gradlew integrationTest --tests "*PendingUploadIntegrationTest"`
 */
@Tags("Integration", "RequiresApi", "Slow")
class PendingUploadIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "(Skipped) pending upload integration — env gate not satisfied" {
                println("Skipping PendingUploadIntegrationTest — set required env vars (see .env.example)")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            /** A 1x1 transparent PNG — the smallest thing Notion will accept as an image. */
            val pngBytes =
                java.util.Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
                )

            /** A minimal but complete PDF, so the pdf block gets a file Notion will render. */
            val pdfBytes =
                (
                    "%PDF-1.1\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                        "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
                        "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 99 9]>>endobj\n" +
                        "trailer<</Root 1 0 R>>\n%%EOF\n"
                ).toByteArray()

            var createdPageId = ""
            var createdDatabaseId = ""

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    if (createdPageId.isNotEmpty()) notion.pages.trash(createdPageId)
                    if (createdDatabaseId.isNotEmpty()) notion.databases.trash(createdDatabaseId)
                    println("✅ Cleaned up created page and database")
                } else {
                    println("🔧 Cleanup skipped — page and database preserved for inspection")
                }
                notion.close()
            }

            "a page create resolves the local files in its icon, files property and content in one call" {
                // Scaffolding: a files property only exists on a database row, so the flow under
                // test needs a data source to create into.
                val database =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Pending Upload — Integration Test")
                        icon.emoji("📎")
                        properties {
                            title("Name")
                            files("Attachments")
                        }
                    }
                createdDatabaseId = database.id
                val dataSourceId = database.dataSources.firstOrNull()?.id
                dataSourceId.shouldNotBeNull()

                // The flow under test: three local files across three surfaces, one create.
                val page =
                    notion.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Pending Upload — Integration Test")
                            files("Attachments") { upload(pdfBytes.asFileSource("appendix.pdf")) }
                        }
                        icon.upload(pngBytes.asFileSource("logo.png"))
                        content {
                            paragraph("Mixed content, uploaded as part of this create.")
                            image(pngBytes.asFileSource("chart.png"), caption = "Q3")
                            html("<h1>Weekly report</h1><p>Generated by the nightly job.</p>")
                        }
                    }
                createdPageId = page.id
                println("📄 Created: ${page.url}")

                // The icon was written as file_upload and reads back Notion-hosted — the write
                // and read shapes differ, so this is Icon.File, not Icon.FileUpload.
                page.icon.shouldBeInstanceOf<Icon.File>()

                val fetched = notion.pages.retrieve(page.id)
                val attachments = fetched.properties["Attachments"]
                attachments.shouldBeInstanceOf<PageProperty.Files>()
                attachments.files shouldHaveSize 1
                attachments.files
                    .single()
                    .shouldBeInstanceOf<FileData.Uploaded>()

                val blocks = notion.blocks.retrieveChildren(page.id)
                blocks shouldHaveSize 3

                blocks[0].shouldBeInstanceOf<Block.Paragraph>()

                // Each upload was attached to its own block, and reads back as a Notion-hosted
                // file — the write shape ("file_upload") and the read shape ("file") differ.
                val image = blocks[1].shouldBeInstanceOf<Block.Image>()
                image.image.file.shouldNotBeNull()
                image.image.caption
                    .single()
                    .plainText shouldBe "Q3"

                // An uploaded .html file renders as an HTML block, which the API returns as an
                // embed carrying a Notion-hosted url rather than the file_upload it was written as.
                blocks[2].shouldBeInstanceOf<Block.Embed>()
            }
        }
    })
