# Upgrade Guide: 2026-03-11

> Learn how to upgrade your integrations to 2026-03-11.

Retrieved: 2026-04-06

Notion API version `2026-03-11` introduces three breaking changes that affect block operations, trash/archive semantics, and the `transcription` block type. Most integrations will need only minor find-and-replace updates.

**Breaking changes**

If your integration uses any of the following, it will break when you upgrade to `2026-03-11`:

* The `after` parameter in Append block children
* The `archived` field in any request or response
* The `transcription` block type

## What's changing

| Change                | Before (`2025-09-03`)                  | After (`2026-03-11`)                              |
| :-------------------- | :------------------------------------- | :------------------------------------------------ |
| **Block positioning** | `after` string parameter               | `position` object (`after_block`, `start`, `end`) |
| **Trash status**      | `archived` field                       | `in_trash` field                                  |
| **Block type rename** | `transcription` block type             | `meeting_notes` block type                        |

## Step 1: Replace `after` with `position`

The Append block children endpoint no longer accepts a flat `after` parameter. Instead, use the `position` object to specify where new blocks should be inserted.

The `position` object supports three placement types:

* `after_block` — insert after a specific block (replaces the old `after` parameter)
* `start` — insert at the beginning of the parent
* `end` — insert at the end of the parent (the default when `position` is omitted)

**After (`2026-03-11`):**
```json
// PATCH /v1/blocks/{block_id}/children
// Notion-Version: 2026-03-11
{
  "position": {
    "type": "after_block",
    "after_block": { "id": "b5d8fd79-..." }
  },
  "children": [
    {
      "paragraph": {
        "rich_text": [{ "text": { "content": "New paragraph" } }]
      }
    }
  ]
}
```

**Before (`2025-09-03`):**
```json
// PATCH /v1/blocks/{block_id}/children
// Notion-Version: 2025-09-03
{
  "after": "b5d8fd79-...",
  "children": [
    {
      "paragraph": {
        "rich_text": [{ "text": { "content": "New paragraph" } }]
      }
    }
  ]
}
```

## Step 2: Replace `archived` with `in_trash`

The `archived` field has been renamed to `in_trash` across all API responses and request parameters. This applies to pages, databases, blocks, and data sources.

The `archived` field was deprecated in April 2024. If your integration already reads `in_trash` from responses, you only need to update your request parameters.

**Response body after (`2026-03-11`):**
```json
{
  "object": "page",
  "id": "59b8df07-...",
  "in_trash": false,
  "created_time": "2025-08-07T10:11:07.504Z",
  "last_edited_time": "2025-08-10T15:53:11.386Z",
  "parent": {
    "type": "page_id",
    "page_id": "255104cd-..."
  },
  "properties": {}
}
```

**Trashing a page after (`2026-03-11`):**
```json
// PATCH /v1/pages/{page_id}
// Notion-Version: 2026-03-11
{
  "in_trash": true
}
```

## Step 3: Replace `transcription` with `meeting_notes`

The `transcription` block type has been renamed to `meeting_notes`. Update any code that creates, reads, or filters by this block type.

**After (`2026-03-11`):**
```json
{
  "object": "block",
  "id": "a1c2d3e4-...",
  "type": "meeting_notes",
  "meeting_notes": {
    "rich_text": [
      { "text": { "content": "Meeting transcript content..." } }
    ]
  },
  "created_time": "2025-10-01T12:00:00.000Z",
  "last_edited_time": "2025-10-01T12:30:00.000Z",
  "in_trash": false
}
```

**Before (`2025-09-03`):**
```json
{
  "object": "block",
  "id": "a1c2d3e4-...",
  "type": "transcription",
  "transcription": {
    "rich_text": [
      { "text": { "content": "Meeting transcript content..." } }
    ]
  },
  "created_time": "2025-10-01T12:00:00.000Z",
  "last_edited_time": "2025-10-01T12:30:00.000Z",
  "in_trash": false
}
```

## Notes on webhook payloads

The `archived → in_trash` rename only applies to REST API request/response bodies, **not** webhook payloads. Webhook event payloads are identical between `2025-09-03` and `2026-03-11`.