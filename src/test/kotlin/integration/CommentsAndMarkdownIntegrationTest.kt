package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import it.saabel.kotlinnotionclient.models.comments.CommentAttachmentRequest
import it.saabel.kotlinnotionclient.models.comments.CreateCommentRequest
import it.saabel.kotlinnotionclient.models.markdown.ContentUpdate
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import kotlinx.coroutines.delay

/**
 * Integration tests for the Comments API and Markdown Content API.
 *
 * Covers:
 * - Comments: page-level comments, discussion threads, comment validation
 * - Comments: block-level comments, file attachment comments, user mention comments
 * - Comments: markdown content in comments (via CreateCommentRequest.markdown and DSL)
 * - Markdown API: retrieve page as markdown, create page with markdown field
 * - Markdown API: updateContent (list overload, DSL builder overload)
 * - Markdown API: replaceContent (string and enhanced markdown)
 *
 * All sub-pages are created under a single container page. Trashing the container
 * (NOTION_CLEANUP_AFTER_TEST=true) cascades to all children.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Optional:
 * - export NOTION_TEST_USER_ID="..." (required for user mention comments test)
 *
 * Run with: ./gradlew integrationTest --tests "*CommentsAndMarkdownIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class CommentsAndMarkdownIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) comments and markdown integration" {
                println("Skipping CommentsAndMarkdownIntegrationTest — set required env vars")
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
                        title("Comments & Markdown — Integration Tests")
                        icon.emoji("💬")
                        content {
                            callout(
                                "ℹ️",
                                "Covers the Comments API (page comments, block comments, validation, " +
                                    "file attachments, user mentions, markdown-in-comments) and the Markdown Content API " +
                                    "(retrieve as markdown, create with markdown, updateContent, replaceContent, " +
                                    "enhanced Notion-specific markdown syntax).",
                            )
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

            // ------------------------------------------------------------------
            // 1. Comments — page-level workflow: create, reply, retrieve
            // ------------------------------------------------------------------
            "should create page comments, reply in the same discussion, and retrieve them" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Comments — Page-level workflow")
                        icon.emoji("💬")
                    }
                println("  Comments page: ${page.url}")
                delay(500)

                val firstComment =
                    notion.comments.create(
                        CreateCommentRequest(
                            parent = Parent.PageParent(page.id),
                            richText = listOf(RequestBuilders.createSimpleRichText("First integration test comment")),
                        ),
                    )

                firstComment.id.shouldNotBeBlank()
                firstComment.objectType shouldBe "comment"
                firstComment.discussionId.shouldNotBeBlank()
                firstComment.richText.shouldNotBeEmpty()
                val parentId =
                    when (val c = firstComment.parent) {
                        is Parent.PageParent -> c.pageId
                        else -> error("Unexpected parent type: ${c::class.simpleName}")
                    }
                parentId shouldBe page.id

                delay(500)

                val secondComment =
                    notion.comments.create(
                        CreateCommentRequest(
                            parent = Parent.PageParent(pageId = page.id),
                            discussionId = firstComment.discussionId,
                            richText = listOf(RequestBuilders.createSimpleRichText("Reply in the same discussion thread")),
                        ),
                    )

                secondComment.discussionId shouldBe firstComment.discussionId
                secondComment.parent.id shouldBe page.id

                delay(1000)

                val allComments = notion.comments.retrieve(page.id)
                println("  Retrieved ${allComments.size} comments (eventual consistency may delay appearance)")

                println("  ✅ Page-level comments and discussion thread verified")
            }

            // ------------------------------------------------------------------
            // 2. Comments — validation: empty rich text rejected
            // ------------------------------------------------------------------
            "should reject a comment with empty rich text" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Comments — Validation")
                        icon.emoji("🚫")
                    }
                println("  Validation page: ${page.url}")

                var caughtValidationError = false
                try {
                    notion.comments.create(
                        CreateCommentRequest(
                            parent = Parent.PageParent(pageId = page.id),
                            richText = emptyList(),
                        ),
                    )
                } catch (e: IllegalArgumentException) {
                    caughtValidationError = true
                    println("  ✅ Correctly caught validation error: ${e.message}")
                }

                caughtValidationError shouldBe true
            }

            // ------------------------------------------------------------------
            // 3. Comments — block-level comments
            // ------------------------------------------------------------------
            "should create comments on individual blocks within a page" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Comments — Block-level comments")
                        icon.emoji("🧱")
                    }
                println("  Block comments page: ${page.url}")
                delay(500)

                val pageContent =
                    pageContent {
                        paragraph("First paragraph block.")
                        paragraph("Second paragraph block.")
                    }

                val blockChildren = notion.blocks.appendChildren(page.id, pageContent)
                blockChildren.results.shouldNotBeEmpty()

                val firstBlock = blockChildren.results.first()
                val secondBlock = blockChildren.results[1]

                delay(500)

                val blockComment =
                    notion.comments.create(
                        CreateCommentRequest(
                            parent = Parent.BlockParent(blockId = firstBlock.id),
                            richText = listOf(RequestBuilders.createSimpleRichText("Comment on first paragraph")),
                        ),
                    )

                blockComment.id.shouldNotBeBlank()
                blockComment.objectType shouldBe "comment"
                blockComment.parent.id shouldBe firstBlock.id
                blockComment.richText.shouldNotBeEmpty()

                val secondBlockComment =
                    notion.comments.create(
                        CreateCommentRequest(
                            parent = Parent.BlockParent(blockId = secondBlock.id),
                            richText = listOf(RequestBuilders.createSimpleRichText("Comment on second paragraph")),
                        ),
                    )

                secondBlockComment.parent.id shouldBe secondBlock.id

                delay(500)
                val blockComments = notion.comments.retrieve(firstBlock.id)
                println("  Retrieved ${blockComments.size} comments on first block")

                println("  ✅ Block-level comments verified")
            }

            // ------------------------------------------------------------------
            // 4. Comments — file attachment limit validation
            // ------------------------------------------------------------------
            "should reject a comment with more than 3 file attachments" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Comments — Attachment limit validation")
                        icon.emoji("📎")
                    }
                println("  Attachment validation page: ${page.url}")

                var caughtAttachmentError = false
                try {
                    notion.comments.create(
                        CreateCommentRequest(
                            parent = Parent.PageParent(pageId = page.id),
                            richText = listOf(RequestBuilders.createSimpleRichText("Too many attachments")),
                            attachments =
                                listOf(
                                    CommentAttachmentRequest(fileUploadId = "file1"),
                                    CommentAttachmentRequest(fileUploadId = "file2"),
                                    CommentAttachmentRequest(fileUploadId = "file3"),
                                    CommentAttachmentRequest(fileUploadId = "file4"),
                                ),
                        ),
                    )
                } catch (e: IllegalArgumentException) {
                    caughtAttachmentError = true
                    e.message shouldBe "Comments can have a maximum of 3 attachments, but 4 were provided"
                    println("  ✅ Correctly caught attachment limit error: ${e.message}")
                }

                caughtAttachmentError shouldBe true
            }

            // ------------------------------------------------------------------
            // 5. Comments — user mentions (requires NOTION_TEST_USER_ID)
            // ------------------------------------------------------------------
            "should create comments with user mentions when NOTION_TEST_USER_ID is set" {
                val testUserId = System.getenv("NOTION_TEST_USER_ID")

                if (testUserId.isNullOrBlank()) {
                    println("  Skipping user mention test — NOTION_TEST_USER_ID not set")
                } else {
                    val page =
                        notion.pages.create {
                            parent.page(containerPageId)
                            title("Comments — User mentions")
                            icon.emoji("👤")
                        }
                    println("  User mention page: ${page.url}")
                    delay(500)

                    val commentWithMention =
                        notion.comments.create(
                            CreateCommentRequest(
                                parent = Parent.PageParent(pageId = page.id),
                                richText =
                                    listOf(
                                        RequestBuilders.createSimpleRichText("Hey "),
                                        RequestBuilders.createUserMention(testUserId, "Test User"),
                                        RequestBuilders.createSimpleRichText(", check out this comment!"),
                                    ),
                            ),
                        )

                    commentWithMention.id.shouldNotBeBlank()
                    commentWithMention.richText.shouldNotBeEmpty()
                    val mentionElement = commentWithMention.richText.find { it.type == "mention" }
                    mentionElement shouldNotBe null

                    delay(500)

                    val complexComment =
                        notion.comments.create(
                            CreateCommentRequest(
                                parent = Parent.PageParent(pageId = page.id),
                                richText =
                                    listOf(
                                        RequestBuilders.createSimpleRichText("Hello "),
                                        RequestBuilders.createUserMention(testUserId, "Test User"),
                                        RequestBuilders.createSimpleRichText("! Thanks for your review."),
                                    ),
                            ),
                        )

                    complexComment.richText.size shouldBe 3
                    complexComment.richText[0].type shouldBe "text"
                    complexComment.richText[1].type shouldBe "mention"
                    complexComment.richText[2].type shouldBe "text"

                    println("  ✅ User mention comments verified")
                }
            }

            // ------------------------------------------------------------------
            // 6. Comments — markdown content in comments (CreateCommentRequest and DSL)
            // ------------------------------------------------------------------
            "should create comments with markdown content via request and DSL" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Comments — Markdown content")
                        icon.emoji("📝")
                    }
                println("  Markdown comment page: ${page.url}")
                delay(500)

                val markdownComment =
                    notion.comments.create(
                        CreateCommentRequest(
                            parent = Parent.PageParent(pageId = page.id),
                            markdown = "**Bold** and _italic_ text with `inline code`.",
                        ),
                    )

                markdownComment.id.shouldNotBeBlank()
                markdownComment.objectType shouldBe "comment"
                delay(500)

                val dslMarkdownComment =
                    notion.comments.create {
                        parent.page(page.id)
                        markdown("Reply via DSL: **done** reviewing!")
                        discussionId(markdownComment.discussionId)
                    }

                dslMarkdownComment.id.shouldNotBeBlank()
                dslMarkdownComment.discussionId shouldBe markdownComment.discussionId

                println("  ✅ Markdown comments verified (request and DSL)")
            }

            // ------------------------------------------------------------------
            // 7. Markdown API — retrieve page as markdown
            // ------------------------------------------------------------------
            "retrieve: should return page content as markdown" {
                val page =
                    notion.pages.create(
                        createPageRequest {
                            parent.page(containerPageId)
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
                println("  Markdown retrieve: ${page.url}")
                delay(1000)

                val response = notion.markdown.retrieve(page.id)

                response.objectType shouldBe "page_markdown"
                response.id.shouldNotBeBlank()
                response.markdown.shouldNotBeBlank()
                response.truncated.shouldBeFalse()
                response.unknownBlockIds.shouldBeEmpty()
                response.markdown shouldContain "Hello from Kotlin"

                println("  Retrieved markdown (${response.markdown.length} chars)")
                println("  ✅ Markdown retrieval verified")
            }

            // ------------------------------------------------------------------
            // 8. Markdown API — create page with markdown field
            // ------------------------------------------------------------------
            "create with markdown: should create page content from markdown string" {
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

                val page =
                    notion.pages.create(
                        createPageRequest {
                            parent.page(containerPageId)
                            icon.emoji("✍️")
                            markdown(markdownContent)
                        },
                    )

                page.objectType shouldBe "page"
                page.inTrash shouldBe false
                println("  Markdown create: ${page.url}")
                delay(1000)

                val retrieved = notion.markdown.retrieve(page.id)
                retrieved.markdown shouldContain "Features"
                retrieved.markdown shouldContain "kotlin"

                println("  ✅ Create with markdown and round-trip verified")
            }

            // ------------------------------------------------------------------
            // 9. Markdown API — updateContent (list overload)
            // ------------------------------------------------------------------
            "updateContent (list overload): should apply search-and-replace operations" {
                val page =
                    notion.pages.create(
                        createPageRequest {
                            parent.page(containerPageId)
                            title("updateContent Test")
                            icon.emoji("🔍")
                            content {
                                heading1("Original Heading")
                                paragraph("This is the original paragraph text.")
                                paragraph("Another original paragraph.")
                            }
                        },
                    )
                println("  updateContent (list): ${page.url}")
                delay(1000)

                val updates =
                    listOf(
                        ContentUpdate(oldStr = "Original Heading", newStr = "Updated Heading"),
                        ContentUpdate(oldStr = "original paragraph text", newStr = "updated paragraph text"),
                    )

                val response = notion.markdown.updateContent(page.id, updates)

                response.objectType shouldBe "page_markdown"
                response.markdown shouldContain "Updated Heading"
                response.markdown shouldContain "updated paragraph text"

                println("  ✅ updateContent (list overload) verified")
            }

            // ------------------------------------------------------------------
            // 10. Markdown API — updateContent (DSL builder overload)
            // ------------------------------------------------------------------
            "updateContent (DSL builder): should apply search-and-replace via builder" {
                val page =
                    notion.pages.create(
                        createPageRequest {
                            parent.page(containerPageId)
                            title("updateContent DSL Test")
                            icon.emoji("🛠️")
                            content {
                                heading1("Before DSL Update")
                                paragraph("Replace this text with DSL.")
                                paragraph("Also replace this sentence.")
                            }
                        },
                    )
                println("  updateContent (DSL): ${page.url}")
                delay(1000)

                val response =
                    notion.markdown.updateContent(page.id) {
                        replace("Before DSL Update", "After DSL Update")
                        replace("Replace this text with DSL", "This text was replaced via DSL")
                        replace("Also replace this sentence", "This sentence was also replaced")
                    }

                response.objectType shouldBe "page_markdown"
                response.markdown shouldContain "After DSL Update"
                response.markdown shouldContain "replaced via DSL"

                println("  ✅ updateContent (DSL builder) verified")
            }

            // ------------------------------------------------------------------
            // 11. Markdown API — replaceContent (full-page replace)
            // ------------------------------------------------------------------
            "replaceContent: should replace entire page content with new markdown" {
                val page =
                    notion.pages.create(
                        createPageRequest {
                            parent.page(containerPageId)
                            title("replaceContent Test")
                            icon.emoji("♻️")
                            content {
                                heading1("Old Content")
                                paragraph("This content will be completely replaced.")
                            }
                        },
                    )
                println("  replaceContent: ${page.url}")
                delay(1000)

                val newContent =
                    """
                    # New Content

                    This page content was completely replaced using the replace_content command.

                    - Point one
                    - Point two
                    - Point three
                    """.trimIndent()

                val response = notion.markdown.replaceContent(page.id, newContent)

                response.objectType shouldBe "page_markdown"
                response.markdown shouldContain "New Content"
                response.markdown shouldContain "completely replaced"
                response.markdown shouldContain "Point one"

                println("  ✅ replaceContent verified")
            }

            // ------------------------------------------------------------------
            // 12. Markdown API — enhanced Notion-specific block syntax
            // ------------------------------------------------------------------
            "enhanced markdown: should create and round-trip Notion-specific block types" {
                val enhancedMarkdown =
                    "## Callout\n\n" +
                        "<callout icon=\"💡\" color=\"yellow_bg\">\n" +
                        "This is a **callout block** with a yellow background.\n" +
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
                        "</table>\n\n" +
                        "## Equation\n\n" +
                        "\$\$\n" +
                        "E = mc^2\n" +
                        "\$\$\n\n" +
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

                val page =
                    notion.pages.create(
                        createPageRequest {
                            parent.page(containerPageId)
                            title("Enhanced Markdown Test")
                            icon.emoji("✨")
                            markdown(enhancedMarkdown)
                        },
                    )

                page.objectType shouldBe "page"
                page.inTrash shouldBe false
                println("  Enhanced markdown: ${page.url}")
                delay(1500)

                val retrieved = notion.markdown.retrieve(page.id)

                retrieved.objectType shouldBe "page_markdown"
                retrieved.markdown.shouldNotBeBlank()
                retrieved.markdown shouldContain "<callout"
                retrieved.markdown shouldContain "callout block"
                retrieved.markdown shouldContain "<details"
                retrieved.markdown shouldContain "Click to expand"
                retrieved.markdown shouldContain "- [ ]"
                retrieved.markdown shouldContain "- [x]"
                retrieved.markdown shouldContain "<table"
                retrieved.markdown shouldContain "Alice"
                retrieved.markdown shouldContain "\$\$"
                retrieved.markdown shouldContain "mc^2"
                retrieved.markdown shouldContain "<columns"

                println("  ✅ Enhanced markdown round-trip verified (${retrieved.markdown.length} chars)")
            }

            // ------------------------------------------------------------------
            // 13. Markdown API — line-break behaviour investigation
            //     Single \n vs \n\n, across create/replaceContent/updateContent
            // ------------------------------------------------------------------
            "line breaks: single \\n vs \\n\\n behaviour across all three write paths" {
                // Three inputs to probe: single LF, double LF, and a mix
                val singleNewline = "First line\nSecond line"
                val doubleNewline = "First paragraph\n\nSecond paragraph"
                val mixedNewlines = "Line A\nLine B\n\nLine C\nLine D"

                // --- create path ---
                val createPage =
                    notion.pages.create(
                        createPageRequest {
                            parent.page(containerPageId)
                            title("Line Break Test — create")
                            icon.emoji("↩️")
                            markdown("$singleNewline\n\n---\n\n$doubleNewline\n\n---\n\n$mixedNewlines")
                        },
                    )
                println("  Line break (create): ${createPage.url}")
                delay(1000)

                val createRetrieved = notion.markdown.retrieve(createPage.id)
                println("  [create] retrieved markdown:\n${createRetrieved.markdown}")

                // --- replaceContent path ---
                val replacePage =
                    notion.pages.create(
                        createPageRequest {
                            parent.page(containerPageId)
                            title("Line Break Test — replaceContent")
                            icon.emoji("♻️")
                            content { paragraph("placeholder") }
                        },
                    )
                delay(1000)

                val replaceResponse =
                    notion.markdown.replaceContent(
                        replacePage.id,
                        "$singleNewline\n\n---\n\n$doubleNewline\n\n---\n\n$mixedNewlines",
                    )
                println("  [replaceContent] returned markdown:\n${replaceResponse.markdown}")

                // --- updateContent path ---
                val updatePage =
                    notion.pages.create(
                        createPageRequest {
                            parent.page(containerPageId)
                            title("Line Break Test — updateContent")
                            icon.emoji("🔧")
                            content { paragraph("PLACEHOLDER_TEXT") }
                        },
                    )
                delay(1000)

                val updateResponse =
                    notion.markdown.updateContent(updatePage.id) {
                        replace("PLACEHOLDER_TEXT", "$singleNewline\n\n---\n\n$doubleNewline\n\n---\n\n$mixedNewlines")
                    }
                println("  [updateContent] returned markdown:\n${updateResponse.markdown}")

                // Minimal assertion — the real finding is in the printed output above.
                // Inspect the pages in Notion and the printed markdown to determine actual behaviour.
                createRetrieved.markdown.shouldNotBeBlank()
                replaceResponse.markdown.shouldNotBeBlank()
                updateResponse.markdown.shouldNotBeBlank()

                println("  ✅ Line-break behaviour logged — inspect output and Notion pages for findings")
            }
        }
    })
