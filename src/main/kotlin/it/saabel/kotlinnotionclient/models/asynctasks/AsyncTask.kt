@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.asynctasks

import it.saabel.kotlinnotionclient.models.markdown.PageMarkdownResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * An asynchronous task handle returned by async-capable Notion endpoints.
 *
 * When an operation is accepted for background execution (e.g. a markdown page write
 * requested with `allow_async: true`), the API responds with HTTP 202 and an
 * `async_task` object. The task can then be polled via `GET /v1/async_tasks/{task_id}`
 * until it reaches a terminal status.
 *
 * @property objectType Always `"async_task"`
 * @property id Unique task identifier used for polling
 * @property status Current task status; see [AsyncTaskStatus]
 * @property statusUrl Fully-qualified URL that can be polled for status
 * @property createdTime ISO 8601 timestamp of when the task was created
 * @property pollAfterSeconds Minimum number of seconds to wait before polling again.
 *   Only present while the task is in a non-terminal status.
 * @property operation Details about the operation the task is executing
 * @property result Raw result payload, present when [status] is [AsyncTaskStatus.SUCCEEDED].
 *   The shape depends on the originating operation — for markdown page writes use
 *   [markdownResultOrNull] to decode it.
 * @property error Error details, present when [status] is [AsyncTaskStatus.FAILED]
 */
@Serializable
data class AsyncTask(
    @SerialName("object")
    val objectType: String = "async_task",
    @SerialName("id")
    val id: String,
    @SerialName("status")
    val status: AsyncTaskStatus,
    @SerialName("status_url")
    val statusUrl: String? = null,
    @SerialName("created_time")
    val createdTime: String? = null,
    @SerialName("poll_after_seconds")
    val pollAfterSeconds: Int? = null,
    @SerialName("operation")
    val operation: AsyncTaskOperation? = null,
    @SerialName("result")
    val result: JsonObject? = null,
    @SerialName("error")
    val error: AsyncTaskErrorDetails? = null,
) {
    /** `true` when the task has reached a terminal status ([AsyncTaskStatus.SUCCEEDED] or [AsyncTaskStatus.FAILED]). */
    val isTerminal: Boolean
        get() = status.isTerminal

    /**
     * Decodes [result] as a [PageMarkdownResponse], the result type produced by
     * async markdown page writes.
     *
     * @return The decoded result, or `null` if the task has no result (yet)
     */
    fun markdownResultOrNull(): PageMarkdownResponse? =
        result?.let { resultJson.decodeFromJsonElement(PageMarkdownResponse.serializer(), it) }
}

private val resultJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

/**
 * Status of an [AsyncTask].
 */
@Serializable
enum class AsyncTaskStatus {
    /** The task has been accepted and persisted, but processing has not started. */
    @SerialName("queued")
    QUEUED,

    /** A worker is processing the task. */
    @SerialName("running")
    RUNNING,

    /** The task hit a retryable failure and is scheduled to retry. */
    @SerialName("retrying")
    RETRYING,

    /** The task completed successfully; the task carries a `result` object. */
    @SerialName("succeeded")
    SUCCEEDED,

    /** The task failed terminally; the task carries an `error` object. */
    @SerialName("failed")
    FAILED,

    ;

    /** `true` for [SUCCEEDED] and [FAILED] — statuses that will never change again. */
    val isTerminal: Boolean
        get() = this == SUCCEEDED || this == FAILED
}

/**
 * Describes the operation an [AsyncTask] is executing.
 *
 * @property surface The API surface the operation originated from (e.g. `"rest"`)
 * @property name The operation name (e.g. `"PATCH /v1/pages/:page_id/markdown"`)
 */
@Serializable
data class AsyncTaskOperation(
    @SerialName("surface")
    val surface: String? = null,
    @SerialName("name")
    val name: String? = null,
)

/**
 * Error details attached to a failed [AsyncTask]. Mirrors the standard Notion error object.
 */
@Serializable
data class AsyncTaskErrorDetails(
    @SerialName("object")
    val objectType: String = "error",
    @SerialName("status")
    val status: Int? = null,
    @SerialName("code")
    val code: String? = null,
    @SerialName("message")
    val message: String? = null,
)

/**
 * Errors thrown by async task polling helpers.
 *
 * Follows the same domain-specific sealed error pattern as `FileUploadError`.
 */
sealed class AsyncTaskException(
    message: String,
) : Exception(message) {
    /**
     * The task reached the [AsyncTaskStatus.FAILED] terminal status.
     *
     * @property task The failed task, including its [AsyncTask.error] details
     */
    class TaskFailed(
        val task: AsyncTask,
    ) : AsyncTaskException(
            "Async task ${task.id} failed" +
                (task.error?.let { ": ${it.code ?: "unknown_error"} - ${it.message ?: "no message"}" } ?: ""),
        )

    /**
     * The task did not reach a terminal status within the allowed wait time.
     *
     * @property taskId The ID of the task that was being awaited
     * @property waitedMs How long the helper waited before giving up
     * @property lastStatus The last observed status, or `null` if the task was never observed
     */
    class TimeoutError(
        val taskId: String,
        val waitedMs: Long,
        val lastStatus: AsyncTaskStatus? = null,
    ) : AsyncTaskException(
            "Timed out after ${waitedMs}ms waiting for async task $taskId to complete" +
                (lastStatus?.let { " (last status: $it)" } ?: ""),
        )
}
