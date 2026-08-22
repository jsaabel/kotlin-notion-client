# Page Markdown

> Source: https://developers.notion.com/reference/retrieve-page-markdown, https://developers.notion.com/guides/data-apis/working-with-markdown-content
> Fetched: 2026-08-22 (API version `2026-03-11`)

> **Note on a known 404**: `developers.notion.com/reference/get-page-markdown`
> (the URL slug named in issue #60 as a known-flaky page) 404s, as does
> `.../reference/patch-page-markdown`. The correct, live slugs are
> **`retrieve-page-markdown`** and **`update-page-markdown`** respectively —
> both resolved on this fetch and are used below. Both endpoint shapes here
> are therefore fetched directly from their own reference pages (high
> confidence), cross-checked against the "Working with markdown content"
> guide. Live-shape confirmation against a real response is still tracked in
> issue #61, since no integration test exercised this.

Notion's markdown API reads and writes page content as "enhanced markdown"
(Notion-flavored Markdown) instead of the block-tree API.

## Retrieve a page as markdown

**GET** `/v1/pages/{page_id}/markdown`

| Param | In | Type | Description |
|---|---|---|---|
| `page_id` | path | string | Page (or, for a truncation follow-up, block) ID to fetch |
| `include_transcript` | query | boolean | Default `false`. When `true`, meeting-notes blocks include the full transcript instead of a placeholder + URL |

Response:

```json
{
  "object": "page_markdown",
  "id": "b55c9c91-384d-452b-81db-d1ef79372b75",
  "markdown": "# Page Title\n\nPage content as enhanced markdown...",
  "truncated": false,
  "unknown_block_ids": [],
  "unknown_block_count": 0
}
```

| Field | Type | Description |
|---|---|---|
| `object` | string | Always `"page_markdown"` |
| `id` | string (UUID) | The page or block ID that was fetched |
| `markdown` | string | Content rendered as enhanced markdown |
| `truncated` | boolean | `true` when the page exceeded roughly 20,000 block records and some subtrees were omitted |
| `unknown_block_ids` | array\<string\> | Up to 50 omitted subtree root IDs. Omitted blocks appear inline as `<unknown url="..." alt="block_type"/>` tags in `markdown`. |
| `unknown_block_count` | integer | Total count of omitted subtree roots — may exceed `unknown_block_ids.length` since that array caps at 50 |

### Paging through truncated content

Pass an omitted block's ID back into the **same endpoint** as `page_id` to
fetch that subtree specifically, and stitch the results together:

```python
all_markdown = resp["markdown"]
for block_id in resp.get("unknown_block_ids", []):
    block_resp = requests.get(
        f"https://api.notion.com/v1/pages/{block_id}/markdown",
        headers=headers,
    ).json()
    all_markdown += "\n" + block_resp["markdown"]
```

If a block ID from `unknown_block_ids` is omitted for permissions rather than
truncation, re-fetching it returns `object_not_found` instead of a subtree —
the two causes aren't distinguished in the parent response, only by what
happens when you follow up.

## Update a page's markdown content

**PATCH** `/v1/pages/{page_id}/markdown`

Body is a discriminated union on `type`:

- **`update_content`** (recommended) — search-and-replace. `content_updates`
  is an array of `{old_str, new_str}` pairs; each `old_str` must match
  exactly once unless `replace_all_matches: true` is set.
- **`replace_content`** (recommended) — replaces the entire page body with
  `new_str`.
- **`insert_content`** (legacy) — positional insert. `position` accepts
  `{type: "start"}` or `{type: "end"}` (added 2026-05-15, so page content can
  be prepended/appended without a full rewrite), or an ellipsis-based `after`
  anchor for the older form.
- **`replace_content_range`** (legacy) — replaces a selection identified by
  an ellipsis-format `content_range`.

All commands accept `allow_deleting_content: true` (default `false`),
required when the edit would delete a child page or database.

```json
{
  "type": "update_content",
  "update_content": {
    "content_updates": [
      { "old_str": "old text", "new_str": "new text" }
    ]
  }
}
```

Top-level `allow_async: true` (default `false`) makes this endpoint return
HTTP 202 with an `async_task` object instead of blocking for a 200. See
`15_Async_Tasks.md` for the shared `async_task` shape, the polling endpoint,
and status values — `POST /v1/pages` (create, with a `markdown` body) accepts
the same `allow_async` flag and returns the same task shape.

Synchronous (200) responses from this endpoint carry `truncated` and
`unknown_block_ids` like the GET endpoint above; the update endpoint's own
reference page did not show `unknown_block_count` in its sample, unlike the
GET endpoint's page, which shows all three fields explicitly. Treat
`unknown_block_count` as present on both (same `page_markdown` object type)
until a live response settles it either way.
