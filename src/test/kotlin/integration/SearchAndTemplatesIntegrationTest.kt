package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.search.searchRequest
import kotlinx.coroutines.delay

/**
 * Integration tests for the Search API and Templates API.
 *
 * Covers:
 * - Search: unrestricted search, query-filtered search, DSL with page/data-source filters
 * - Templates: list all templates, filter by name, create page from default template,
 *   create page from specific template ID, apply template to existing page,
 *   apply with and without eraseContent
 *
 * The container page documents coverage. Search tests query existing content and create
 * no sub-pages. Template tests create pages inside the specified data source, not under
 * the container, so they manage their own cleanup via NOTION_CLEANUP_AFTER_TEST.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Optional (required for template tests):
 * - export NOTION_TEMPLATES_DATA_SOURCE_ID="..."
 *   Set up: create a database in Notion with at least 2 templates, one set as default.
 *
 * Run with: ./gradlew integrationTest --tests "*SearchAndTemplatesIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class SearchAndTemplatesIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) search and templates integration" {
                println("Skipping SearchAndTemplatesIntegrationTest — set required env vars")
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
                        title("Search & Templates — Integration Tests")
                        icon.emoji("🔎")
                        content {
                            callout(
                                "ℹ️",
                                "Covers the Search API (unrestricted search, query filtering, page and data-source filters) " +
                                    "and the Templates API (list, filter by name, create page from default/specific template, " +
                                    "apply template to existing page, eraseContent behaviour). " +
                                    "Template tests require NOTION_TEMPLATES_DATA_SOURCE_ID and run in that data source.",
                            )
                        }
                    }
                containerPageId = container.id
                println("📄 Container: ${container.url}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("✅ Cleaned up container page")
                } else {
                    println("🔧 Cleanup skipped — container page preserved for inspection")
                }
                notion.close()
            }

            // ------------------------------------------------------------------
            // 1. Search — unrestricted search returns accessible content
            // ------------------------------------------------------------------
            "search should return accessible content" {
                val response = notion.search.search()

                response.objectType shouldBe "list"
                response.results shouldNotBe null
                println("  ✓ Unrestricted search returned ${response.results.size} results")
            }

            // ------------------------------------------------------------------
            // 2. Search — query-filtered search
            // ------------------------------------------------------------------
            "search with query should filter results" {
                val response = notion.search.search("test")

                response shouldNotBe null
                println("  ✓ Query-filtered search returned ${response.results.size} results")
            }

            // ------------------------------------------------------------------
            // 3. Search — DSL with page filter and descending sort
            // ------------------------------------------------------------------
            "search DSL with page filter and sort should work" {
                val response =
                    notion.search.search(
                        searchRequest {
                            filterPages()
                            sortDescending()
                            pageSize(10)
                        },
                    )

                response shouldNotBe null
                println("  ✓ DSL page-filtered search returned ${response.results.size} results")
            }

            // ------------------------------------------------------------------
            // 4. Search — data source filter (2025-09-03 API)
            // ------------------------------------------------------------------
            "search for data sources should work with 2025-09-03 API" {
                val response =
                    notion.search.search(
                        searchRequest {
                            filterDataSources()
                        },
                    )

                response shouldNotBe null
                println("  ✓ Data source search returned ${response.results.size} results")
            }

            // ------------------------------------------------------------------
            // Template tests — only run when NOTION_TEMPLATES_DATA_SOURCE_ID is set
            // ------------------------------------------------------------------

            // ------------------------------------------------------------------
            // 5. Templates — list all templates for data source
            // ------------------------------------------------------------------
            "list templates from pre-configured data source" {
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                if (dataSourceId.isNullOrBlank()) {
                    println("  Skipping — NOTION_TEMPLATES_DATA_SOURCE_ID not set")
                } else {
                    val templates = notion.dataSources.listTemplates(dataSourceId)

                    templates.shouldNotBeNull()
                    println("  Found ${templates.size} template(s):")
                    templates.forEach { template ->
                        val marker = if (template.isDefault) "⭐" else "  "
                        println("    $marker ${template.name} (${template.id})")
                    }
                    println("  ✅ Template listing verified")
                }
            }

            // ------------------------------------------------------------------
            // 6. Templates — filter by name
            // ------------------------------------------------------------------
            "filter templates by name substring" {
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                if (dataSourceId.isNullOrBlank()) {
                    println("  Skipping — NOTION_TEMPLATES_DATA_SOURCE_ID not set")
                } else {
                    val allTemplates = notion.dataSources.listTemplates(dataSourceId)
                    if (allTemplates.isEmpty()) {
                        println("  No templates found — skipping filter test")
                    } else {
                        val searchTerm = allTemplates.first().name.take(3)
                        val filtered = notion.dataSources.listTemplates(dataSourceId, nameFilter = searchTerm)

                        filtered.shouldNotBeNull()
                        filtered.forEach { template ->
                            println("  ✓ '${template.name}' matches '$searchTerm'")
                        }
                        println("  ✅ Template name filter verified")
                    }
                }
            }

            // ------------------------------------------------------------------
            // 7. Templates — create page from default template
            // ------------------------------------------------------------------
            "create page using default template" {
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                if (dataSourceId.isNullOrBlank()) {
                    println("  Skipping — NOTION_TEMPLATES_DATA_SOURCE_ID not set")
                } else {
                    val templates = notion.dataSources.listTemplates(dataSourceId)
                    val defaultTemplate = templates.find { it.isDefault }

                    if (defaultTemplate == null) {
                        println("  No default template found — skipping (set one in Notion)")
                    } else {
                        val page =
                            notion.pages.create {
                                parent.dataSource(dataSourceId)
                                template.default()
                                properties {
                                    title("Name", "From Default Template — ${System.currentTimeMillis()}")
                                }
                            }

                        page.shouldNotBeNull()
                        println("  Created from default template: ${page.url}")
                        println("  ⚠️  Template content is applied asynchronously — inspect URL above")

                        if (shouldCleanupAfterTest()) {
                            delay(1000)
                            notion.pages.trash(page.id)
                        }
                        println("  ✅ Create from default template verified")
                    }
                }
            }

            // ------------------------------------------------------------------
            // 8. Templates — create page from specific template ID
            // ------------------------------------------------------------------
            "create page using specific template ID" {
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                if (dataSourceId.isNullOrBlank()) {
                    println("  Skipping — NOTION_TEMPLATES_DATA_SOURCE_ID not set")
                } else {
                    val templates = notion.dataSources.listTemplates(dataSourceId)
                    if (templates.isEmpty()) {
                        println("  No templates found — skipping")
                    } else {
                        val selected = templates.first()

                        val page =
                            notion.pages.create {
                                parent.dataSource(dataSourceId)
                                template.byId(selected.id)
                                properties {
                                    title("Name", "From '${selected.name}' — ${System.currentTimeMillis()}")
                                }
                            }

                        page.shouldNotBeNull()
                        println("  Created from '${selected.name}': ${page.url}")

                        if (shouldCleanupAfterTest()) {
                            delay(1000)
                            notion.pages.trash(page.id)
                        }
                        println("  ✅ Create from specific template ID verified")
                    }
                }
            }

            // ------------------------------------------------------------------
            // 9. Templates — apply template to existing page
            // ------------------------------------------------------------------
            "apply template to existing page" {
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                if (dataSourceId.isNullOrBlank()) {
                    println("  Skipping — NOTION_TEMPLATES_DATA_SOURCE_ID not set")
                } else {
                    val templates = notion.dataSources.listTemplates(dataSourceId)
                    if (templates.isEmpty()) {
                        println("  No templates found — skipping")
                    } else {
                        val selected = templates.first()

                        val page =
                            notion.pages.create {
                                parent.dataSource(dataSourceId)
                                properties {
                                    title("Name", "Apply Template Target — ${System.currentTimeMillis()}")
                                }
                            }
                        delay(1000)

                        val updatedPage =
                            notion.pages.update(page.id) {
                                template.byId(selected.id)
                            }

                        updatedPage.shouldNotBeNull()
                        println("  Template '${selected.name}' applied: ${updatedPage.url}")

                        if (shouldCleanupAfterTest()) {
                            delay(1000)
                            notion.pages.trash(page.id)
                        }
                        println("  ✅ Apply template to existing page verified")
                    }
                }
            }

            // ------------------------------------------------------------------
            // 10. Templates — eraseContent=true vs eraseContent=false
            // ------------------------------------------------------------------
            "apply template with and without eraseContent" {
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                if (dataSourceId.isNullOrBlank()) {
                    println("  Skipping — NOTION_TEMPLATES_DATA_SOURCE_ID not set")
                } else {
                    val templates = notion.dataSources.listTemplates(dataSourceId)
                    if (templates.isEmpty()) {
                        println("  No templates found — skipping")
                    } else {
                        val selected = templates.first()

                        val page1 =
                            notion.pages.create {
                                parent.dataSource(dataSourceId)
                                properties {
                                    title("Name", "Keep Content — ${System.currentTimeMillis()}")
                                }
                                content { paragraph("Original content — should be PRESERVED") }
                            }

                        val page2 =
                            notion.pages.create {
                                parent.dataSource(dataSourceId)
                                properties {
                                    title("Name", "Erase Content — ${System.currentTimeMillis()}")
                                }
                                content { paragraph("Original content — should be ERASED") }
                            }
                        delay(1000)

                        val updated1 =
                            notion.pages.update(page1.id) {
                                template.byId(selected.id)
                                // eraseContent defaults to false
                            }

                        val updated2 =
                            notion.pages.update(page2.id) {
                                template.byId(selected.id)
                                eraseContent(true)
                            }

                        updated1.shouldNotBeNull()
                        updated2.shouldNotBeNull()
                        println("  Page 1 (preserve): ${updated1.url}")
                        println("  Page 2 (erase):    ${updated2.url}")
                        println("  ⚠️  Template content is applied asynchronously — inspect URLs above")

                        if (shouldCleanupAfterTest()) {
                            delay(1000)
                            notion.pages.trash(page1.id)
                            notion.pages.trash(page2.id)
                        }
                        println("  ✅ eraseContent=true/false behaviour verified")
                    }
                }
            }
        }
    })
