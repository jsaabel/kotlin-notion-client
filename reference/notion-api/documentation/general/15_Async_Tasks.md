# Async Tasks

> Source: https://developers.notion.com/reference/retrieve-async-task, https://developers.notion.com/guides/data-apis/working-with-markdown-content
> Fetched: 2026-08-22 (API version `2026-03-11`)

Long-running writes — large markdown page creates/updates — can be run
asynchronously instead of blocking the HTTP request until they finish.

## Triggering an async task

Set `"allow_async": true` on:

- `POST /v1/pages` when the body includes `markdown` (page create from
  markdown)
- `PATCH /v1/pages/{page_id}/markdown` (page markdown update)

When the write would exceed the normal request timeout, the endpoint returns
**HTTP 202** with an `async_task` object instead of the usual 200 body. If
the write finishes within the timeout anyway, both endpoints may still
return their normal 200 response synchronously even with `allow_async: true`
set — the flag permits async, it doesn't force it.

## Retrieving a task

**GET** `/v1/async_tasks/{task_id}`

> Note: issue #60 named this endpoint as `GET /v1/tasks/{id}` — that path is
> incorrect. The confirmed live path, from both the dedicated reference page
> and a web search cross-check, is `/v1/async_tasks/{task_id}` (underscore,
> not `/v1/tasks/`). This client's `AsyncTasksApi` already calls
> `${baseUrl}/async_tasks/$taskId`, i.e. it already matches the corrected
> path.

| Field | Type | Presence | Description |
|---|---|---|---|
| `object` | string | always | `"async_task"` |
| `id` | string | always | Task identifier |
| `status` | string | always | One of `queued`, `running`, `retrying`, `succeeded`, `failed` |
| `status_url` | string | always | Same as this endpoint's own URL, for convenience |
| `created_time` | string (ISO 8601) | always per the dedicated reference page (modeled defensively as nullable in this client at medium confidence — see `FOLLOWUPS.md` item 20) | When the task was created |
| `operation` | object | always | `{surface: "rest" \| "mcp", name: string}` — e.g. `{"surface": "rest", "name": "PATCH /v1/pages/:page_id/markdown"}` |
| `poll_after_seconds` | integer (>= 0) | non-terminal statuses only (`queued`/`running`/`retrying`) | Minimum wait before polling again |
| `result` | object | only when `status == "succeeded"` | The operation's normal success payload — e.g. a `page_markdown` object for a markdown write |
| `error` | object | only when `status == "failed"` | Standard error shape: `{object: "error", status, code, message}` |

### Sample responses

Running:
```json
{
  "object": "async_task",
  "id": "task_abc123",
  "status": "running",
  "status_url": "https://api.notion.com/v1/async_tasks/task_abc123",
  "created_time": "2026-06-29T12:00:00.000Z",
  "poll_after_seconds": 2,
  "operation": { "surface": "rest", "name": "PATCH /v1/pages/:page_id/markdown" }
}
```

Succeeded:
```json
{
  "object": "async_task",
  "id": "task_abc123",
  "status": "succeeded",
  "status_url": "https://api.notion.com/v1/async_tasks/task_abc123",
  "created_time": "2026-06-29T12:00:00.000Z",
  "operation": { "surface": "rest", "name": "PATCH /v1/pages/:page_id/markdown" },
  "result": {
    "object": "page_markdown",
    "id": "page-uuid",
    "markdown": "# Updated page\n\nThe update is complete.",
    "truncated": false,
    "unknown_block_ids": []
  }
}
```

Failed:
```json
{
  "object": "async_task",
  "id": "task_abc123",
  "status": "failed",
  "status_url": "https://api.notion.com/v1/async_tasks/task_abc123",
  "created_time": "2026-06-29T12:00:00.000Z",
  "operation": { "surface": "rest", "name": "PATCH /v1/pages/:page_id/markdown" },
  "error": { "object": "error", "status": 400, "code": "validation_error", "message": "The request body was invalid." }
}
```

## Polling guidance

Wait at least `poll_after_seconds` between polls; polling faster risks 429s.
`queued`, `running`, and `retrying` are non-terminal; `succeeded` and
`failed` are terminal. `@notionhq/client` v5.23.0+ ships
`notion.asyncTasks.retrieve({ task_id })` as a typed convenience wrapper —
this client's equivalent is `AsyncTasksApi.retrieve` / `waitForCompletion` /
`pollAsFlow`.
