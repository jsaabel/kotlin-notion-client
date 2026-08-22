package integration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTask
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTaskStatus
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.markdown.AsyncMarkdownResult
import it.saabel.kotlinnotionclient.models.pages.AsyncPageCreateResult

/**
 * Integration tests for the async task flow (`allow_async: true` + `GET /v1/async_tasks/{id}`).
 *
 * Purpose (FOLLOWUPS items 19, 20; issue #61): the 202 acceptance flow and the async task
 * object shape were implemented from docs without ever being exercised live. These tests
 * adjudicate that. A 202 cannot be forced — the API decides — so every assertion tolerates
 * the synchronous `Completed` fallback and *logs* which branch actually ran. The field
 * observations printed on a real 202 (`operation`, `created_time`, `status_url`,
 * `poll_after_seconds`) are the finding; read the output, not just the checkmark.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Run with: ./gradlew integrationTest --tests "*AsyncTasksIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class AsyncTasksIntegrationTest :
    StringSpec({

        // A markdown body large enough to plausibly tip the API into background execution.
        fun largeMarkdown(sections: Int): String =
            buildString {
                appendLine("# Async task integration fixture")
                appendLine()
                for (i in 1..sections) {
                    appendLine("## Section $i")
                    appendLine()
                    appendLine(
                        "Paragraph $i. This body exists only to be heavy enough that Notion may " +
                            "choose asynchronous execution for the write. It carries no meaning.",
                    )
                    appendLine()
                    appendLine("- bullet one of section $i")
                    appendLine("- bullet two of section $i")
                    appendLine()
                }
            }

        fun describeTask(
            label: String,
            task: AsyncTask,
        ) {
            println("🔎 $label:")
            println("   id                 = ${task.id}")
            println("   status             = ${task.status}")
            println("   status_url         = ${task.statusUrl ?: "ABSENT"}")
            println("   created_time       = ${task.createdTime ?: "ABSENT"}")
            println("   poll_after_seconds = ${task.pollAfterSeconds ?: "ABSENT"}")
            println("   operation          = ${task.operation ?: "ABSENT"}")
            println("   result present     = ${task.result != null}")
            println("   error              = ${task.error ?: "none"}")
        }

        if (!integrationTestEnvVarsAreSet()) {
            "(Skipped) async tasks integration — env gate not satisfied" {
                fun visible(name: String): String =
                    when {
                        System.getenv(name) == null -> "ABSENT"
                        System.getenv(name).isBlank() -> "BLANK"
                        else -> "set"
                    }
                println("Skipping AsyncTasksIntegrationTest — env seen by the test JVM:")
                println("  NOTION_RUN_INTEGRATION_TESTS = ${System.getenv("NOTION_RUN_INTEGRATION_TESTS") ?: "ABSENT"}")
                println("  NOTION_API_TOKEN             = ${visible("NOTION_API_TOKEN")}")
                println("  NOTION_TEST_PAGE_ID          = ${visible("NOTION_TEST_PAGE_ID")}")
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
                        title("Async Tasks — Integration Tests")
                        icon.emoji("⏳")
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
            // 1. Async page create from markdown (items 19–20)
            // ------------------------------------------------------------------
            "async markdown page create returns 202 task or synchronous page" {
                val markdown = largeMarkdown(sections = 300)
                println("📝 Sending markdown body of ${markdown.length} chars")

                val result =
                    notion.pages.createFromMarkdownAsync(
                        parent = Parent.PageParent(pageId = containerPageId),
                        markdown = markdown,
                        title = "Async create target",
                    )

                when (result) {
                    is AsyncPageCreateResult.Completed -> {
                        println("ℹ️ FINDING: API answered synchronously (HTTP 200) despite allow_async")
                        result.page.id.shouldNotBeBlank()
                        println("   created page: ${result.page.url}")
                    }

                    is AsyncPageCreateResult.Accepted -> {
                        println("🎉 FINDING: API accepted for background execution (HTTP 202)")
                        describeTask("202 response task", result.task)
                        result.task.id.shouldNotBeBlank()
                        result.task.isTerminal shouldBe false

                        val polled = notion.asyncTasks.retrieve(result.task.id)
                        describeTask("first GET /async_tasks poll", polled)
                        polled.id shouldBe result.task.id

                        val done = notion.asyncTasks.waitForCompletion(result.task.id, maxWaitTimeMs = 120_000)
                        describeTask("terminal task", done)
                        done.status shouldBe AsyncTaskStatus.SUCCEEDED
                        done.result.shouldNotBeNull()

                        val page = done.pageResultOrNull()
                        val md = done.markdownResultOrNull()
                        println("   result decoded as Page: ${page != null}, as PageMarkdownResponse: ${md != null}")
                        println("   raw result keys: ${done.result?.keys}")
                        (page != null || md != null).shouldBeTrue()
                    }
                }
            }

            // ------------------------------------------------------------------
            // 2. Async markdown replace on an existing page (item 19)
            // ------------------------------------------------------------------
            "async markdown replace returns 202 task or synchronous markdown" {
                val target =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Async replace target")
                    }
                val markdown = largeMarkdown(sections = 300)
                println("📝 Replacing content with markdown body of ${markdown.length} chars")

                val result = notion.markdown.replaceContentAsync(target.id, markdown)

                when (result) {
                    is AsyncMarkdownResult.Completed -> {
                        println("ℹ️ FINDING: API answered synchronously (HTTP 200) despite allow_async")
                        println("   returned markdown length: ${result.response.markdown.length}")
                        println("   unknown_block_count: ${result.response.unknownBlockCount}")
                    }

                    is AsyncMarkdownResult.Accepted -> {
                        println("🎉 FINDING: API accepted for background execution (HTTP 202)")
                        describeTask("202 response task", result.task)
                        result.task.id.shouldNotBeBlank()

                        val done = notion.asyncTasks.waitForCompletion(result.task.id, maxWaitTimeMs = 120_000)
                        describeTask("terminal task", done)
                        done.status shouldBe AsyncTaskStatus.SUCCEEDED
                        val md = done.markdownResultOrNull()
                        println("   result decoded as PageMarkdownResponse: ${md != null}")
                        md.shouldNotBeNull()
                    }
                }
            }

            // ------------------------------------------------------------------
            // 3. Retrieving an unknown task id maps to a Notion API error
            // ------------------------------------------------------------------
            "retrieving a nonexistent async task throws ApiError" {
                val exception =
                    shouldThrow<NotionException.ApiError> {
                        notion.asyncTasks.retrieve("00000000-0000-0000-0000-000000000000")
                    }
                println("🔎 FINDING: nonexistent task → HTTP ${exception.status}, code=${exception.code}")
            }
        }
    })
