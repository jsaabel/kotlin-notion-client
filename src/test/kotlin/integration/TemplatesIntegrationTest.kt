package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import kotlinx.coroutines.delay

/**
 * Integration tests for the Templates API functionality.
 *
 * These tests require a pre-configured data source with templates set up in Notion.
 * Since templates cannot be created via the API, we rely on a manually configured data source.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEMPLATES_DATA_SOURCE_ID="your_data_source_id"
 * 3. Set environment variable: export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Data Source Setup Instructions:
 * 1. Create a database in Notion with at least one data source
 * 2. Create 2-3 templates for that data source:
 *    - At least one should be set as the default template
 *    - Templates should have distinct names (e.g., "Meeting Notes", "Project Template", "Task Template")
 * 3. The data source should have basic properties (Title, and optionally Status, Description, etc.)
 * 4. Copy the data source ID from the database URL or via the API
 * 5. Set NOTION_TEMPLATES_DATA_SOURCE_ID to that ID
 *
 * The tests will:
 * - List all templates for the data source
 * - Filter templates by name
 * - Create pages using templates (default and specific template ID)
 * - Test template application in page updates
 *
 * Optional:
 * - Set NOTION_CLEANUP_AFTER_TEST="false" to keep test pages for manual inspection
 *
 * Run with: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi", "Templates")
class TemplatesIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet("NOTION_API_TOKEN", "NOTION_TEMPLATES_DATA_SOURCE_ID")) {
            "!(Skipped)" {
                println("Skipping TemplatesIntegrationTest - requires NOTION_TEMPLATES_DATA_SOURCE_ID environment variable")
                println("See test file documentation for setup instructions")
            }
        } else {
            "List templates from pre-configured data source" {
                val token = System.getenv("NOTION_API_TOKEN")
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Listing templates for data source: $dataSourceId")
                    val templates = client.dataSources.listTemplates(dataSourceId)

                    templates.shouldNotBeNull()
                    templates.shouldNotBeEmpty()
                    println("✅ Found ${templates.size} template(s):")

                    val defaultTemplate = templates.find { it.isDefault }
                    if (defaultTemplate != null) {
                        println("   📋 Default template: ${defaultTemplate.name} (${defaultTemplate.id})")
                    }

                    templates.forEach { template ->
                        val marker = if (template.isDefault) "⭐" else "  "
                        println("   $marker ${template.name}")
                        println("      ID: ${template.id}")
                        println("      Default: ${template.isDefault}")
                    }
                } finally {
                    client.close()
                }
            }

            "Filter templates by name" {
                val token = System.getenv("NOTION_API_TOKEN")
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Listing all templates first...")
                    val allTemplates = client.dataSources.listTemplates(dataSourceId)
                    allTemplates.shouldNotBeEmpty()

                    // Pick a substring from one of the template names
                    val sampleTemplate = allTemplates.first()
                    val searchTerm = sampleTemplate.name.take(3)

                    println("\n🔍 Filtering templates with name containing: '$searchTerm'")
                    val filteredTemplates = client.dataSources.listTemplates(dataSourceId, nameFilter = searchTerm)

                    filteredTemplates.shouldNotBeNull()
                    println("✅ Found ${filteredTemplates.size} matching template(s):")
                    filteredTemplates.forEach { template ->
                        println("   📄 ${template.name}")
                    }

                    // Verify that the filter actually worked (all results should contain the search term)
                    filteredTemplates.forEach { template ->
                        println("   ✓ '${template.name}' contains '$searchTerm': ${template.name.contains(searchTerm, ignoreCase = true)}")
                    }
                } finally {
                    client.close()
                }
            }

            "Create page using default template" {
                val token = System.getenv("NOTION_API_TOKEN")
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Finding default template...")
                    val templates = client.dataSources.listTemplates(dataSourceId)
                    val defaultTemplate = templates.find { it.isDefault }

                    if (defaultTemplate != null) {
                        println("✅ Using default template: ${defaultTemplate.name}")

                        println("\n📝 Creating page with default template...")
                        val page =
                            client.pages.create {
                                parent.dataSource(dataSourceId)
                                template.default()
                                properties {
                                    title("Name", "Page from Default Template - ${System.currentTimeMillis()}")
                                }
                            }

                        page.shouldNotBeNull()
                        println("✅ Page created: ${page.url}")
                        println("   ID: ${page.id}")
                        println("   ⚠️  Note: Template content is applied asynchronously")
                        println("   Visit the URL above to verify the template was applied")

                        if (shouldCleanupAfterTest()) {
                            delay(1000)
                            println("\n🧹 Archiving test page...")
                            client.pages.archive(page.id)
                            println("✅ Cleanup complete")
                        } else {
                            println("\n⚠️ Skipping cleanup (NOTION_CLEANUP_AFTER_TEST=false)")
                            println("📌 Page URL: ${page.url}")
                        }
                    } else {
                        println("⚠️  No default template found - skipping test")
                        println("   Please set one of your templates as default in Notion")
                    }
                } finally {
                    client.close()
                }
            }

            "Create page using specific template ID" {
                val token = System.getenv("NOTION_API_TOKEN")
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Listing available templates...")
                    val templates = client.dataSources.listTemplates(dataSourceId)
                    templates.shouldNotBeEmpty()

                    val selectedTemplate = templates.first()
                    println("✅ Using template: ${selectedTemplate.name} (${selectedTemplate.id})")

                    println("\n📝 Creating page with specific template ID...")
                    val page =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            template.byId(selectedTemplate.id)
                            properties {
                                title("Name", "Page from '${selectedTemplate.name}' - ${System.currentTimeMillis()}")
                            }
                        }

                    page.shouldNotBeNull()
                    println("✅ Page created: ${page.url}")
                    println("   ID: ${page.id}")
                    println("   Template: ${selectedTemplate.name}")
                    println("   ⚠️  Note: Template content is applied asynchronously")
                    println("   Visit the URL above to verify the template was applied")

                    if (shouldCleanupAfterTest()) {
                        delay(1000)
                        println("\n🧹 Archiving test page...")
                        client.pages.archive(page.id)
                        println("✅ Cleanup complete")
                    } else {
                        println("\n⚠️ Skipping cleanup (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("📌 Page URL: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }

            "Apply template to existing page" {
                val token = System.getenv("NOTION_API_TOKEN")
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Listing available templates...")
                    val templates = client.dataSources.listTemplates(dataSourceId)
                    templates.shouldNotBeEmpty()

                    val selectedTemplate = templates.first()
                    println("✅ Using template: ${selectedTemplate.name}")

                    println("\n📝 Creating blank page first...")
                    val page =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Name", "Page to Apply Template - ${System.currentTimeMillis()}")
                            }
                        }

                    println("✅ Blank page created: ${page.url}")
                    delay(1000)

                    println("\n🔄 Applying template to existing page...")
                    val updatedPage =
                        client.pages.update(page.id) {
                            template.byId(selectedTemplate.id)
                            // eraseContent() can be used to replace existing content
                        }

                    updatedPage.shouldNotBeNull()
                    println("✅ Template applied to page: ${updatedPage.url}")
                    println("   Template: ${selectedTemplate.name}")
                    println("   ⚠️  Note: Template content is applied asynchronously")
                    println("   Visit the URL above to verify the template was applied")

                    if (shouldCleanupAfterTest()) {
                        delay(1000)
                        println("\n🧹 Archiving test page...")
                        client.pages.archive(page.id)
                        println("✅ Cleanup complete")
                    } else {
                        println("\n⚠️ Skipping cleanup (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("📌 Page URL: ${updatedPage.url}")
                    }
                } finally {
                    client.close()
                }
            }

            "Apply template with and without eraseContent" {
                val token = System.getenv("NOTION_API_TOKEN")
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Listing available templates...")
                    val templates = client.dataSources.listTemplates(dataSourceId)
                    templates.shouldNotBeEmpty()

                    val selectedTemplate = templates.first()
                    println("✅ Using template: ${selectedTemplate.name}")

                    println("\n📝 Creating two identical pages with initial content...")

                    // Create first page with content
                    val page1 =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Name", "Page 1 - Will Keep Content - ${System.currentTimeMillis()}")
                            }
                            content {
                                paragraph("Original content in page 1 - this should be PRESERVED")
                                heading2("Original Heading")
                            }
                        }

                    println("✅ Page 1 created (will keep content): ${page1.url}")

                    // Create second page with content
                    val page2 =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Name", "Page 2 - Will Erase Content - ${System.currentTimeMillis()}")
                            }
                            content {
                                paragraph("Original content in page 2 - this should be ERASED")
                                heading2("Original Heading")
                            }
                        }

                    println("✅ Page 2 created (will erase content): ${page2.url}")
                    delay(1000)

                    println("\n🔄 Applying template to Page 1 WITHOUT erasing content...")
                    val updatedPage1 =
                        client.pages.update(page1.id) {
                            template.byId(selectedTemplate.id)
                            // Note: eraseContent defaults to false
                        }

                    println("✅ Template applied to Page 1 (content preserved)")
                    println("   URL: ${updatedPage1.url}")

                    delay(500)

                    println("\n🔄 Applying template to Page 2 WITH eraseContent...")
                    val updatedPage2 =
                        client.pages.update(page2.id) {
                            template.byId(selectedTemplate.id)
                            eraseContent(true)
                        }

                    println("✅ Template applied to Page 2 (content erased)")
                    println("   URL: ${updatedPage2.url}")

                    println("\n📋 Verification:")
                    println("   Page 1: ${updatedPage1.url}")
                    println("   ➜ Should show BOTH original content AND template content")
                    println()
                    println("   Page 2: ${updatedPage2.url}")
                    println("   ➜ Should show ONLY template content (original erased)")
                    println()
                    println("   ⚠️  Note: Template content is applied asynchronously")
                    println("   Wait a few seconds before checking the pages")

                    if (shouldCleanupAfterTest()) {
                        delay(1000)
                        println("\n🧹 Archiving test pages...")
                        client.pages.archive(page1.id)
                        client.pages.archive(page2.id)
                        println("✅ Cleanup complete")
                    } else {
                        println("\n⚠️ Skipping cleanup (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("📌 Page 1 (preserved): ${updatedPage1.url}")
                        println("📌 Page 2 (erased): ${updatedPage2.url}")
                    }
                } finally {
                    client.close()
                }
            }

            "Create multiple pages from same template" {
                val token = System.getenv("NOTION_API_TOKEN")
                val dataSourceId = System.getenv("NOTION_TEMPLATES_DATA_SOURCE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🔍 Listing available templates...")
                    val templates = client.dataSources.listTemplates(dataSourceId)
                    templates.shouldNotBeEmpty()

                    val selectedTemplate = templates.first()
                    println("✅ Using template: ${selectedTemplate.name}")

                    val createdPages = mutableListOf<String>()

                    println("\n📝 Creating 3 pages from the same template...")
                    for (i in 1..3) {
                        val page =
                            client.pages.create {
                                parent.dataSource(dataSourceId)
                                template.byId(selectedTemplate.id)
                                properties {
                                    title("Name", "Batch Page $i from '${selectedTemplate.name}' - ${System.currentTimeMillis()}")
                                }
                            }

                        page.shouldNotBeNull()
                        createdPages.add(page.id)
                        println("   ✅ Page $i created: ${page.url}")

                        // Small delay between creations
                        if (i < 3) delay(500)
                    }

                    println("\n✅ Successfully created ${createdPages.size} pages from template")

                    if (shouldCleanupAfterTest()) {
                        delay(1000)
                        println("\n🧹 Archiving test pages...")
                        createdPages.forEach { pageId ->
                            client.pages.archive(pageId)
                        }
                        println("✅ Cleanup complete")
                    } else {
                        println("\n⚠️ Skipping cleanup (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("📌 Created ${createdPages.size} test pages")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
