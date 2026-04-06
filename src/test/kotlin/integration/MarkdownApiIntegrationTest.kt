package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.markdown.ContentUpdate
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import kotlinx.coroutines.delay

/**
 * Integration tests for the Markdown Content API.
 *
 * Covers all three supported operations:
 *   1. Retrieve page as markdown (GET /v1/pages/:id/markdown)
 *   2. Create page with markdown content (POST /v1/pages with `markdown` field)
 *   3. Update page content via markdown (PATCH /v1/pages/:id/markdown)
 *      - update_content (search-and-replace, list overload)
 *      - update_content (DSL builder overload)
 *      - replace_content (full-page replace, string overload)
 *
 * Prerequisites:
 *   export NOTION_API_TOKEN="secret_..."
 *   export NOTION_TEST_PAGE_ID="<uuid of a page your integration can write to>"
 *   export NOTION_RUN_INTEGRATION_TESTS=true
 *
 * Optional:
 *   export NOTION_CLEANUP_AFTER_TEST=false   # keep created pages for manual inspection
 *
 * Run with: ./gradlew integrationTest
 * Run only this file: ./gradlew integrationTest --tests "*.MarkdownApiIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class MarkdownApiIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping MarkdownApiIntegrationTest — set NOTION_RUN_INTEGRATION_TESTS=true and env vars to run") }
        } else {

            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

            // ─────────────────────────────────────────────────
            // 1. Retrieve page as markdown
            // ─────────────────────────────────────────────────

            "retrieve: should return page content as markdown" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    // Create a page with known content via the block DSL so we have something to retrieve
                    println("📄 Creating test page for markdown retrieval...")
                    val page =
                        client.pages.create(
                            createPageRequest {
                                parent.page(parentPageId)
                                title("Markdown Retrieve Test")
                                icon.emoji("📖")
                                content {
                                    heading1("Hello from Kotlin")
                                    paragraph("This page was created to test the markdown retrieve endpoint.")
                                    bullet("Item one")
                                    bullet("Item two")
                                }
                            },
                        )

                    println("   Created page: ${page.id}")
                    println("   URL: ${page.url}")
                    delay(1000) // give Notion time to index the blocks

                    val response = client.markdown.retrieve(page.id)

                    response.objectType shouldBe "page_markdown"
                    response.id.shouldNotBeBlank()
                    response.markdown.shouldNotBeBlank()
                    response.truncated.shouldBeFalse()
                    response.unknownBlockIds.shouldBeEmpty()

                    // The heading we created should appear in the markdown
                    response.markdown shouldContain "Hello from Kotlin"

                    println("✅ Retrieved markdown (${response.markdown.length} chars):")
                    println(response.markdown.prependIndent("   "))

                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("🧹 Test page trashed")
                    } else {
                        println("🔧 Cleanup skipped — page: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }

            // ─────────────────────────────────────────────────
            // 2. Create page with markdown field
            // ─────────────────────────────────────────────────

            "create with markdown: should create page content from markdown string" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    val markdownContent =
                        """
                        # Markdown Creation Test

                        This page was created using the `markdown` field on the create page request.

                        ## Features

                        - Heading support
                        - Paragraph support
                        - Bullet lists
                        - **Bold** and *italic* text

                        ## Code example

                        ```kotlin
                        val response = client.markdown.retrieve(pageId)
                        println(response.markdown)
                        ```
                        """.trimIndent()

                    println("📄 Creating page via markdown field...")
                    val page =
                        client.pages.create(
                            createPageRequest {
                                parent.page(parentPageId)
                                icon.emoji("✍️")
                                markdown(markdownContent)
                            },
                        )

                    page.objectType shouldBe "page"
                    page.inTrash shouldBe false
                    println("✅ Page created: ${page.id}")
                    println("   URL: ${page.url}")

                    delay(1000)

                    // Round-trip: retrieve the page back as markdown and verify key content survived.
                    // Note: the first # h1 becomes the page title (not body content), so we assert
                    // on content that is definitely in the body, not the title heading.
                    val retrieved = client.markdown.retrieve(page.id)
                    retrieved.markdown shouldContain "Features"
                    retrieved.markdown shouldContain "kotlin"

                    println("✅ Round-trip markdown content verified")
                    println("   Retrieved markdown (${retrieved.markdown.length} chars):")
                    println(retrieved.markdown.prependIndent("   "))

                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("🧹 Test page trashed")
                    } else {
                        println("🔧 Cleanup skipped — page: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }

            // ─────────────────────────────────────────────────
            // 3a. update_content — list overload (search-and-replace)
            // ─────────────────────────────────────────────────

            "updateContent (list overload): should apply search-and-replace operations" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📄 Creating test page for updateContent...")
                    val page =
                        client.pages.create(
                            createPageRequest {
                                parent.page(parentPageId)
                                title("updateContent Test")
                                icon.emoji("🔍")
                                content {
                                    heading1("Original Heading")
                                    paragraph("This is the original paragraph text.")
                                    paragraph("Another original paragraph.")
                                }
                            },
                        )

                    println("   Created page: ${page.id}")
                    println("   URL: ${page.url}")
                    delay(1000)

                    val updates =
                        listOf(
                            ContentUpdate(
                                oldStr = "Original Heading",
                                newStr = "Updated Heading",
                            ),
                            ContentUpdate(
                                oldStr = "original paragraph text",
                                newStr = "updated paragraph text",
                            ),
                        )

                    val response = client.markdown.updateContent(page.id, updates)

                    response.objectType shouldBe "page_markdown"
                    response.markdown shouldContain "Updated Heading"
                    response.markdown shouldContain "updated paragraph text"

                    println("✅ update_content applied successfully")
                    println("   Result markdown:")
                    println(response.markdown.prependIndent("   "))

                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("🧹 Test page trashed")
                    } else {
                        println("🔧 Cleanup skipped — page: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }

            // ─────────────────────────────────────────────────
            // 3b. update_content — DSL builder overload
            // ─────────────────────────────────────────────────

            "updateContent (DSL builder): should apply search-and-replace via builder" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📄 Creating test page for DSL updateContent...")
                    val page =
                        client.pages.create(
                            createPageRequest {
                                parent.page(parentPageId)
                                title("updateContent DSL Test")
                                icon.emoji("🛠️")
                                content {
                                    heading1("Before DSL Update")
                                    paragraph("Replace this text with DSL.")
                                    paragraph("Also replace this sentence.")
                                }
                            },
                        )

                    println("   Created page: ${page.id}")
                    println("   URL: ${page.url}")
                    delay(1000)

                    val response =
                        client.markdown.updateContent(page.id) {
                            replace("Before DSL Update", "After DSL Update")
                            replace("Replace this text with DSL", "This text was replaced via DSL")
                            replace("Also replace this sentence", "This sentence was also replaced")
                        }

                    response.objectType shouldBe "page_markdown"
                    response.markdown shouldContain "After DSL Update"
                    response.markdown shouldContain "replaced via DSL"

                    println("✅ update_content DSL applied successfully")
                    println("   Result markdown:")
                    println(response.markdown.prependIndent("   "))

                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("🧹 Test page trashed")
                    } else {
                        println("🔧 Cleanup skipped — page: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }

            // ─────────────────────────────────────────────────
            // 3c. replace_content — full-page replace
            // ─────────────────────────────────────────────────

            "replaceContent: should replace entire page content with new markdown" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📄 Creating test page for replaceContent...")
                    val page =
                        client.pages.create(
                            createPageRequest {
                                parent.page(parentPageId)
                                title("replaceContent Test")
                                icon.emoji("♻️")
                                content {
                                    heading1("Old Content")
                                    paragraph("This content will be completely replaced.")
                                }
                            },
                        )

                    println("   Created page: ${page.id}")
                    println("   URL: ${page.url}")
                    delay(1000)

                    val newContent =
                        """
                        # New Content

                        This page content was completely replaced using the replace_content command.

                        - Point one
                        - Point two
                        - Point three
                        """.trimIndent()

                    val response = client.markdown.replaceContent(page.id, newContent)

                    response.objectType shouldBe "page_markdown"
                    response.markdown shouldContain "New Content"
                    response.markdown shouldContain "completely replaced"
                    response.markdown shouldContain "Point one"

                    println("✅ replace_content applied successfully")
                    println("   Result markdown:")
                    println(response.markdown.prependIndent("   "))

                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("🧹 Test page trashed")
                    } else {
                        println("🔧 Cleanup skipped — page: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }

            // ─────────────────────────────────────────────────
            // 4. Enhanced markdown — Notion-specific block types
            // ─────────────────────────────────────────────────

            "enhanced markdown: should create and round-trip Notion-specific block types" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    // Build a markdown string that exercises the Notion-specific extensions:
                    //   callout, toggle, to-do, table, equation, columns, colors, inline formatting.
                    // Tabs are used for child block indentation as required by the spec.
                    val enhancedMarkdown =
                        "## Callout\n\n" +
                            "<callout icon=\"💡\" color=\"yellow_bg\">\n" +
                            "This is a **callout block** with a yellow background.\n" +
                            "Use these for tips, warnings, or highlighted notes.\n" +
                            "</callout>\n\n" +
                            "## Toggle\n\n" +
                            "<details>\n" +
                            "<summary>Click to expand</summary>\n" +
                            "Hidden content inside the toggle.\n" +
                            "- Nested bullet one\n" +
                            "- Nested bullet two\n" +
                            "</details>\n\n" +
                            "## To-Do List\n\n" +
                            "- [ ] Buy groceries\n" +
                            "- [x] Write integration tests\n" +
                            "- [ ] Deploy to production\n\n" +
                            "## Table\n\n" +
                            "<table header-row=\"true\">\n" +
                            "<tr>\n<td>Name</td>\n<td>Role</td>\n<td>Status</td>\n</tr>\n" +
                            "<tr>\n<td>Alice</td>\n<td>Engineer</td>\n<td>Active</td>\n</tr>\n" +
                            "<tr>\n<td>Bob</td>\n<td>Designer</td>\n<td>Active</td>\n</tr>\n" +
                            "</table>\n\n" +
                            "## Equation\n\n" +
                            "\$\$\n" +
                            "E = mc^2\n" +
                            "\$\$\n\n" +
                            "## Inline Formatting\n\n" +
                            "<span color=\"blue\">Blue text</span>, " +
                            "<span color=\"red\">red text</span>, " +
                            "~~strikethrough~~, " +
                            "`inline code`, " +
                            "and [a link](https://developers.notion.com).\n\n" +
                            "## Columns\n\n" +
                            "<columns>\n" +
                            "<column>\n" +
                            "### Left Column\n\n" +
                            "Content in the left column.\n" +
                            "</column>\n" +
                            "<column>\n" +
                            "### Right Column\n\n" +
                            "Content in the right column.\n" +
                            "</column>\n" +
                            "</columns>\n"

                    println("📄 Creating page with enhanced markdown content...")
                    val page =
                        client.pages.create(
                            createPageRequest {
                                parent.page(parentPageId)
                                title("Enhanced Markdown Test")
                                icon.emoji("✨")
                                markdown(enhancedMarkdown)
                            },
                        )

                    page.objectType shouldBe "page"
                    page.inTrash shouldBe false
                    println("✅ Page created: ${page.id}")
                    println("   URL: ${page.url}")

                    delay(1500) // give Notion extra time to process complex content

                    // Round-trip: retrieve as markdown and verify enhanced elements survived
                    val retrieved = client.markdown.retrieve(page.id)

                    retrieved.objectType shouldBe "page_markdown"
                    retrieved.markdown.shouldNotBeBlank()

                    // Callout should be present
                    retrieved.markdown shouldContain "<callout"
                    retrieved.markdown shouldContain "callout block"

                    // Toggle should be present
                    retrieved.markdown shouldContain "<details"
                    retrieved.markdown shouldContain "Click to expand"

                    // To-do items should be present
                    retrieved.markdown shouldContain "- [ ]"
                    retrieved.markdown shouldContain "- [x]"

                    // Table structure should be present
                    retrieved.markdown shouldContain "<table"
                    retrieved.markdown shouldContain "Alice"

                    // Equation should be present
                    retrieved.markdown shouldContain "\$\$"
                    retrieved.markdown shouldContain "mc^2"

                    // Columns should be present
                    retrieved.markdown shouldContain "<columns"

                    println("✅ Enhanced markdown round-trip verified")
                    println("   Retrieved markdown (${retrieved.markdown.length} chars):")
                    println(retrieved.markdown.prependIndent("   "))

                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("🧹 Test page trashed")
                    } else {
                        println("🔧 Cleanup skipped — page retains full enhanced markdown content for inspection: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }

            // ─────────────────────────────────────────────────
            // 5. replaceContent with enhanced markdown
            // ─────────────────────────────────────────────────

            "replaceContent with enhanced markdown: should accept Notion-specific block syntax" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📄 Creating test page for enhanced replaceContent...")
                    val page =
                        client.pages.create(
                            createPageRequest {
                                parent.page(parentPageId)
                                title("replaceContent Enhanced Markdown Test")
                                icon.emoji("♻️")
                                content { paragraph("Initial content — will be replaced.") }
                            },
                        )

                    println("   Created page: ${page.id}")
                    println("   URL: ${page.url}")
                    delay(1000)

                    val enhancedReplacement =
                        "## Replaced with Enhanced Markdown\n\n" +
                            "<callout icon=\"✅\" color=\"green_bg\">\n" +
                            "Content was successfully replaced using enhanced markdown.\n" +
                            "</callout>\n\n" +
                            "- [ ] Verify the callout rendered\n" +
                            "- [x] Called replaceContent\n" +
                            "- [ ] Check the page in Notion\n"

                    val updated = client.markdown.replaceContent(page.id, enhancedReplacement)

                    updated.objectType shouldBe "page_markdown"
                    updated.markdown shouldContain "<callout"
                    updated.markdown shouldContain "successfully replaced"
                    updated.markdown shouldContain "- [x]"

                    println("✅ replaceContent with enhanced markdown verified")
                    println("   Result markdown:")
                    println(updated.markdown.prependIndent("   "))

                    if (shouldCleanupAfterTest()) {
                        client.pages.trash(page.id)
                        println("🧹 Test page trashed")
                    } else {
                        println("🔧 Cleanup skipped — page: ${page.url}")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
