package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.AsyncTasksApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTask
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTaskException
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTaskStatus
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import unit.util.TestFixtures

private val queuedFixture = TestFixtures.AsyncTasks.retrieveQueuedAsString()
private val succeededFixture = TestFixtures.AsyncTasks.retrieveSucceededAsString()
private val failedFixture = TestFixtures.AsyncTasks.retrieveFailedAsString()

/** An in-progress task without poll_after_seconds so polling tests can use tiny intervals. */
private val runningNoHint =
    """
    {
      "object": "async_task",
      "id": "task_abc123",
      "status": "running",
      "status_url": "https://api.notion.com/v1/async_tasks/task_abc123",
      "created_time": "2026-06-29T12:00:00.000Z",
      "operation": { "surface": "rest", "name": "PATCH /v1/pages/:page_id/markdown" }
    }
    """.trimIndent()

private fun asyncTasksApi(handler: MockRequestHandler): AsyncTasksApi {
    val engine = MockEngine { request -> handler(request) }
    val httpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                        explicitNulls = false
                    },
                )
            }
        }
    return AsyncTasksApi(httpClient, NotionConfig(apiToken = "test-token"))
}

private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

/** Responds with each body in [bodies] in order, repeating the last one. */
private fun sequencedApi(vararg bodies: String): AsyncTasksApi {
    var call = 0
    return asyncTasksApi { _ ->
        val body = bodies[minOf(call, bodies.size - 1)]
        call++
        respond(content = body, status = HttpStatusCode.OK, headers = jsonHeaders())
    }
}

@Tags("Unit")
class AsyncTasksApiTest :
    StringSpec({

        "retrieve should parse a queued task" {
            var capturedUrl = ""
            val api =
                asyncTasksApi { request ->
                    capturedUrl = request.url.toString()
                    respond(content = queuedFixture, status = HttpStatusCode.OK, headers = jsonHeaders())
                }

            val task = api.retrieve("task_abc123")

            capturedUrl shouldContain "/v1/async_tasks/task_abc123"
            task.objectType shouldBe "async_task"
            task.id shouldBe "task_abc123"
            task.status shouldBe AsyncTaskStatus.QUEUED
            task.statusUrl shouldBe "https://api.notion.com/v1/async_tasks/task_abc123"
            task.pollAfterSeconds shouldBe 2
            task.operation?.surface shouldBe "rest"
            task.operation?.name shouldBe "PATCH /v1/pages/:page_id/markdown"
            task.isTerminal shouldBe false
        }

        "retrieve should parse a succeeded task with markdown result" {
            val api =
                asyncTasksApi { _ ->
                    respond(content = succeededFixture, status = HttpStatusCode.OK, headers = jsonHeaders())
                }

            val task = api.retrieve("task_abc123")

            task.status shouldBe AsyncTaskStatus.SUCCEEDED
            task.isTerminal shouldBe true
            val result = task.markdownResultOrNull()
            result.shouldNotBeNull()
            result.objectType shouldBe "page_markdown"
            result.id shouldBe "59833787-2cf9-4fdf-8782-e53db20768a5"
            result.markdown shouldContain "Updated page"
            result.truncated shouldBe false
        }

        "retrieve should parse a failed task with error details" {
            val api =
                asyncTasksApi { _ ->
                    respond(content = failedFixture, status = HttpStatusCode.OK, headers = jsonHeaders())
                }

            val task = api.retrieve("task_abc123")

            task.status shouldBe AsyncTaskStatus.FAILED
            task.isTerminal shouldBe true
            task.error?.status shouldBe 400
            task.error?.code shouldBe "validation_error"
            task.error?.message shouldBe "The request body was invalid."
            task.markdownResultOrNull() shouldBe null
        }

        "retrieve should throw ApiError on error response" {
            val api = asyncTasksApi { _ -> respondError(HttpStatusCode.NotFound) }

            val exception = shouldThrow<NotionException.ApiError> { api.retrieve("missing-task") }

            exception.status shouldBe 404
        }

        "waitForCompletion should return succeeded task after polling" {
            val api = sequencedApi(runningNoHint, runningNoHint, succeededFixture)

            val task = api.waitForCompletion("task_abc123", maxWaitTimeMs = 5_000, checkIntervalMs = 10)

            task.status shouldBe AsyncTaskStatus.SUCCEEDED
            task.markdownResultOrNull().shouldNotBeNull()
        }

        "waitForCompletion should throw TaskFailed when the task fails" {
            val api = sequencedApi(runningNoHint, failedFixture)

            val exception =
                shouldThrow<AsyncTaskException.TaskFailed> {
                    api.waitForCompletion("task_abc123", maxWaitTimeMs = 5_000, checkIntervalMs = 10)
                }

            exception.task.status shouldBe AsyncTaskStatus.FAILED
            exception.task.error?.code shouldBe "validation_error"
            exception.message shouldContain "validation_error"
        }

        "waitForCompletion should throw TimeoutError when the task never completes" {
            val api = sequencedApi(runningNoHint)

            val exception =
                shouldThrow<AsyncTaskException.TimeoutError> {
                    api.waitForCompletion("task_abc123", maxWaitTimeMs = 100, checkIntervalMs = 20)
                }

            exception.taskId shouldBe "task_abc123"
            exception.waitedMs shouldBe 100
            exception.lastStatus shouldBe AsyncTaskStatus.RUNNING
        }

        "pollAsFlow should emit each snapshot and complete on the terminal one" {
            val api = sequencedApi(runningNoHint, runningNoHint, succeededFixture)

            val snapshots: List<AsyncTask> = api.pollAsFlow("task_abc123", checkIntervalMs = 10).toList()

            snapshots.size shouldBe 3
            snapshots[0].status shouldBe AsyncTaskStatus.RUNNING
            snapshots[1].status shouldBe AsyncTaskStatus.RUNNING
            snapshots[2].status shouldBe AsyncTaskStatus.SUCCEEDED
        }

        "pollAsFlow should emit a failed terminal snapshot without throwing" {
            val api = sequencedApi(runningNoHint, failedFixture)

            val snapshots = api.pollAsFlow("task_abc123", checkIntervalMs = 10).toList()

            snapshots.size shouldBe 2
            snapshots.last().status shouldBe AsyncTaskStatus.FAILED
            snapshots.last().error?.code shouldBe "validation_error"
        }
    })
