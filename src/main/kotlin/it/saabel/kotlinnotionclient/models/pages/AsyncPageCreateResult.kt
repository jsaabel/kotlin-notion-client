package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.asynctasks.AsyncTask

/**
 * Result of an opt-in async page create from Markdown.
 *
 * Since the Jun 29 2026 Notion changelog, `POST /v1/pages` accepts `allow_async: true`
 * when the body carries a `markdown` string. As with the markdown *write* endpoint
 * (`AsyncMarkdownResult`), opting in does not force background execution: the API
 * decides, and small writes still complete synchronously with HTTP 200. This sealed
 * type captures both outcomes so neither has to be guessed from a nullable field.
 */
sealed class AsyncPageCreateResult {
    /**
     * The page was created synchronously; [page] is the created page.
     */
    data class Completed(
        val page: Page,
    ) : AsyncPageCreateResult()

    /**
     * The create was accepted for background execution. Poll [task] via
     * `client.asyncTasks` (e.g. `waitForCompletion(task.id)`) to obtain the result.
     *
     * Notion documents the succeeded-task `result` payload only for the markdown
     * *update* operation (a `page_markdown` object). For `POST /v1/pages` the payload
     * shape is not documented, so read it through [AsyncTask.pageResultOrNull] or
     * [AsyncTask.markdownResultOrNull] — whichever returns non-null — or fall back to
     * the raw [AsyncTask.result].
     */
    data class Accepted(
        val task: AsyncTask,
    ) : AsyncPageCreateResult()
}
