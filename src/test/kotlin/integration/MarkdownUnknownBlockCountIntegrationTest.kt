package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

/**
 * Integration test for the REST shape of `unknown_block_count` on page-markdown
 * responses (FOLLOWUPS item 10; issue #61).
 *
 * The field was inferred: the REST reference page 404s and the Aug 7 2026 changelog
 * frames truncation metadata as an MCP change. Our model defaults `unknownBlockCount`
 * to 0, so a typed read cannot distinguish "API sent 0" from "field absent" — this test
 * therefore fetches the **raw JSON** of `GET /v1/pages/{id}/markdown` and checks for the
 * key itself. Either outcome is recorded as a finding:
 * - key present → the inferred REST shape is real
 * - key absent  → the field is MCP-only after all; the default-0 modelling stays safe,
 *   but the KDoc should be amended
 *
 * The typed path is exercised too (deserialization + markdown round-trip).
 *
 * Run with: ./gradlew integrationTest --tests "*MarkdownUnknownBlockCountIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class MarkdownUnknownBlockCountIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "(Skipped) unknown_block_count integration — env gate not satisfied" {
                println("Skipping MarkdownUnknownBlockCountIntegrationTest — set required env vars (see .env.example)")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val config = NotionConfig(apiToken = token)
            val notion = NotionClient.create(config)

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("unknown_block_count — Integration Tests")
                        icon.emoji("❓")
                        content {
                            heading1("Markdown fixture")
                            paragraph("A paragraph so the markdown response is non-trivial.")
                            bullet("one")
                            bullet("two")
                            bullet("three")
                        }
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

            "raw page-markdown response reveals whether unknown_block_count exists in REST" {
                val raw =
                    withContext(Dispatchers.IO) {
                        val connection =
                            URI("${config.baseUrl}/pages/$containerPageId/markdown")
                                .toURL()
                                .openConnection() as HttpURLConnection
                        try {
                            connection.setRequestProperty("Authorization", "Bearer $token")
                            connection.setRequestProperty("Notion-Version", config.apiVersion)
                            val status = connection.responseCode
                            val body =
                                (if (status in 200..299) connection.inputStream else connection.errorStream)
                                    .bufferedReader()
                                    .readText()
                            status to body
                        } finally {
                            connection.disconnect()
                        }
                    }

                println("🔎 raw GET /pages/{id}/markdown → HTTP ${raw.first}")
                raw.first shouldBe 200
                // Log the response minus the (long) markdown payload for shape inspection.
                println("🔎 raw response (truncated): ${raw.second.take(600)}")

                val hasKey = "\"unknown_block_count\"" in raw.second
                if (hasKey) {
                    println("🔎 FINDING: unknown_block_count IS present in the REST response — inferred shape confirmed")
                } else {
                    println(
                        "🔎 FINDING: unknown_block_count is ABSENT from this (non-truncated) REST response — " +
                            "possibly truncation-only or MCP-only",
                    )
                }

                // Typed path: deserializes and round-trips regardless.
                val typed = notion.markdown.retrieve(containerPageId)
                println("🔎 typed unknownBlockCount = ${typed.unknownBlockCount}, markdown length = ${typed.markdown.length}")
                (typed.markdown.contains("Markdown fixture")) shouldBe true
            }
        }
    })
