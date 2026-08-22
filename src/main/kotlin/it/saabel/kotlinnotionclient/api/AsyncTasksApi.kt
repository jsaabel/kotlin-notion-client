package it.saabel.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.exceptions.toNotionApiError
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTask
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTaskException
import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTaskStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * API client for the Notion async task endpoint.
 *
 * Async-capable operations (such as markdown page writes requested with `allow_async: true`)
 * return an [AsyncTask] handle instead of the final result. This API retrieves the current
 * status of such tasks and provides polling helpers to await their completion.
 *
 * Example usage:
 * ```kotlin
 * // Start an async markdown write
 * val result = client.markdown.replaceContentAsync("page-id", largeMarkdown)
 * when (result) {
 *     is AsyncMarkdownResult.Completed -> println(result.response.markdown)
 *     is AsyncMarkdownResult.Accepted -> {
 *         val task = client.asyncTasks.waitForCompletion(result.task.id)
 *         println(task.markdownResultOrNull()?.markdown)
 *     }
 * }
 * ```
 *
 * @property httpClient The HTTP client for making requests
 * @property config The Notion API configuration
 */
class AsyncTasksApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Retrieves the current status of an async task.
     *
     * @param taskId The ID of the async task to retrieve
     * @return The task, including a `result` when succeeded or an `error` when failed
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun retrieve(taskId: String): AsyncTask =
        try {
            val response: HttpResponse = httpClient.get("${config.baseUrl}/async_tasks/$taskId")

            if (response.status.isSuccess()) {
                response.body<AsyncTask>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e
        } catch (e: ClientRequestException) {
            throw clientError(e)
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Waits for an async task to reach the [AsyncTaskStatus.SUCCEEDED] status.
     *
     * Polls the task status until it succeeds, fails, or the wait time is exhausted.
     * This mirrors the polling idiom of `EnhancedFileUploadApi.waitForFileReady`.
     * When the API suggests a longer polling interval via [AsyncTask.pollAfterSeconds],
     * that suggestion takes precedence over [checkIntervalMs].
     *
     * @param taskId The ID of the async task to wait for
     * @param maxWaitTimeMs Maximum time to wait in milliseconds (default: 60 seconds)
     * @param checkIntervalMs Minimum interval between status checks in milliseconds (default: 1 second)
     * @return The task once it reaches the "succeeded" status, including its `result`
     * @throws AsyncTaskException.TaskFailed if the task reaches the "failed" status
     * @throws AsyncTaskException.TimeoutError if the task doesn't complete within the timeout
     */
    suspend fun waitForCompletion(
        taskId: String,
        maxWaitTimeMs: Long = 60_000,
        checkIntervalMs: Long = 1_000,
    ): AsyncTask {
        val startTime = System.currentTimeMillis()
        var lastStatus: AsyncTaskStatus? = null

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            val task = retrieve(taskId)
            lastStatus = task.status

            if (task.status == AsyncTaskStatus.SUCCEEDED) {
                return task
            }

            if (task.status == AsyncTaskStatus.FAILED) {
                throw AsyncTaskException.TaskFailed(task)
            }

            delay(pollDelayMs(task, checkIntervalMs))
        }

        throw AsyncTaskException.TimeoutError(taskId, maxWaitTimeMs, lastStatus)
    }

    /**
     * Polls an async task and emits each observed status snapshot as a [Flow].
     *
     * The flow completes after emitting the first terminal snapshot (succeeded or failed).
     * Unlike [waitForCompletion] it does not throw on failure — consumers inspect the
     * terminal [AsyncTask.status] themselves. Consistent with the library's Flow-based
     * pagination helpers; apply standard Flow operators (e.g. `kotlinx.coroutines.withTimeout`)
     * for time limits.
     *
     * Example usage:
     * ```kotlin
     * client.asyncTasks.pollAsFlow(taskId).collect { task ->
     *     println("Status: ${task.status}")
     * }
     * ```
     *
     * @param taskId The ID of the async task to poll
     * @param checkIntervalMs Minimum interval between status checks in milliseconds (default: 1 second).
     *   [AsyncTask.pollAfterSeconds] takes precedence when it suggests a longer wait.
     * @return A cold flow emitting every observed task snapshot, ending with a terminal one
     */
    fun pollAsFlow(
        taskId: String,
        checkIntervalMs: Long = 1_000,
    ): Flow<AsyncTask> =
        flow {
            while (true) {
                val task = retrieve(taskId)
                emit(task)
                if (task.isTerminal) {
                    return@flow
                }
                delay(pollDelayMs(task, checkIntervalMs))
            }
        }

    private fun pollDelayMs(
        task: AsyncTask,
        checkIntervalMs: Long,
    ): Long = maxOf(checkIntervalMs, (task.pollAfterSeconds ?: 0) * 1_000L)

    private suspend fun clientError(e: ClientRequestException): NotionException = e.response.toNotionApiError()
}
