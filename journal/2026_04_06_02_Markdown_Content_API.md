# Development Journal - April 6, 2026

## Phase 4: Markdown Content API

### Objective

Implement the Markdown Content API introduced in Notion API `2026-03-11`. Three capabilities:

1. **Retrieve page as markdown** — `GET /v1/pages/:page_id/markdown`
2. **Create page with markdown content** — `POST /v1/pages` (via `markdown` body param, mutually exclusive with `children`)
3. **Update page content via markdown** — `PATCH /v1/pages/:page_id/markdown` (four commands: `update_content`, `replace_content`, and two legacy ones)

---

## Reference Documentation

Retrieved during this session:

- `https://developers.notion.com/reference/retrieve-page-markdown` — GET endpoint spec
- `https://developers.notion.com/reference/update-page-markdown` — PATCH endpoint spec
- `https://developers.notion.com/guides/data-apis/enhanced-markdown` — full enhanced markdown syntax reference
- `https://developers.notion.com/guides/data-apis/working-with-markdown-content` — workflow guide, request/response shapes, all four PATCH commands

Not saved to disk (too volatile to cache); key findings captured below.

---

## Key Findings from Docs

### There is no `POST /v1/pages/:page_id/markdown`

The "create page with markdown" feature is not a separate endpoint. It works by passing a `markdown` field to the existing `POST /v1/pages` endpoint, mutually exclusive with `children`.

### Response shape (`page_markdown` object)

```json
{
  "object": "page_markdown",
  "id": "string",
  "markdown": "string",
  "truncated": boolean,
  "unknown_block_ids": ["string"]
}
```

Returned by both GET and PATCH endpoints. Truncation occurs at ~20,000 blocks.

### PATCH command types (discriminated union on `type`)

| Command | Status | Notes |
|---|---|---|
| `update_content` | Recommended | Search-and-replace, up to 100 operations |
| `replace_content` | Recommended | Full page replace |
| `insert_content` | Legacy/deprecated | Insert at location (ellipsis-based `after` param) |
| `replace_content_range` | Legacy/deprecated | Replace a matched range |

All commands support `allow_deleting_content` (guards against accidentally deleting child pages/databases).

### Enhanced Markdown format

A superset of standard Markdown — supports custom HTML-like tags for Notion-specific blocks (callouts, toggles, synced blocks, columns, mentions, etc.). Tab-indented child blocks. Full spec at the guide link above.

---

## Implementation Plan

### Changes Made (Session 1)

#### Already implemented before journal created

- **`models/markdown/MarkdownModels.kt`** (new): `PageMarkdownResponse`, `ContentUpdate`, `UpdateContentBody`, `ReplaceContentBody`, `UpdateContentRequest`, `ReplaceContentRequest`
- **`api/MarkdownApi.kt`** (new): `retrieve()` with optional `includeTranscript`, `updateContent()` (three overloads: typed, list, DSL builder), `replaceContent()` (two overloads: typed, string), `ContentUpdateBuilder` DSL
- **`NotionClient.kt`**: registered `val markdown = MarkdownApi(httpClient, config)`
- **`src/test/resources/api/markdown/`**: `retrieve_page_markdown.json`, `update_page_markdown.json`
- **`unit/api/MarkdownApiTest.kt`** (new): 8 unit tests covering all public methods and error paths

#### Still needed (identified after docs review)

1. `CreatePageRequest.markdown: String?` — new field, mutually exclusive with `children`
2. `CreatePageRequestBuilder.markdown(content)` — DSL method, mutual-exclusion guard in `build()`
3. Tests for new items (unit + integration)

**Decision:** Legacy PATCH commands (`insert_content`, `replace_content_range`) will not be supported — they are deprecated by Notion and add no value to the library surface.

---

## Session Log

### Session 1 (April 6, 2026) — Initial implementation + gaps identified

#### First pass (before journal)

Fetched the two reference pages (retrieve + update endpoints) and implemented the core `MarkdownApi` class and models. All 8 unit tests passing.

**Key design decisions:**

- **Separate data classes per command** (not a sealed class) for the PATCH request body. The API uses a flat `type` discriminator with a sibling key for the command body — Kotlin's polymorphic sealed class serialization would work here, but the explicit `data class` approach with a hardcoded `type: String` default is simpler, more transparent in IntelliJ, and easier to read in calling code.
- **Three overloads for `updateContent()`**: typed request (full control), list of `ContentUpdate` (convenient), DSL builder (`ContentUpdateBuilder`). DSL builder is the most ergonomic for the common case.
- **`ContentUpdateBuilder` DSL**: `replace()` and `replaceAll()` as the two primitives, mirroring the API's `replace_all_matches` boolean.
- **`retrieve()` query param**: built with `buildString` — avoids a dependency on Ktor's URL builder for a single optional boolean param.

**Gap identified during doc review (working-with-markdown guide):**

- `POST /v1/pages` supports a `markdown` field — this is the "create with markdown" path, not a separate endpoint. Our existing `CreatePageRequest` doesn't have this field yet.
- Two legacy PATCH commands not yet implemented: `insert_content`, `replace_content_range`.

#### Session 1 continued — Closing the gaps

**`CreatePageRequest.markdown` field:**

Added `val markdown: String? = null` to `CreatePageRequest`. The builder's `build()` enforces mutual exclusion:
- `markdown + children` → `IllegalStateException`
- `markdown + template` → `IllegalStateException`

**DSL — `CreatePageRequestBuilder.markdown(content: String)`:**

Added `markdown()` method. The builder holds a `markdownValue` field separate from `children`; `build()` is the enforcement point.

**Legacy PATCH commands — not implemented:**

`insert_content` and `replace_content_range` are deprecated by Notion. Skipped entirely to keep the library surface clean.

**Integration tests:**

Added `MarkdownApiIntegrationTest` with 5 tests covering the full round-trip:
1. `retrieve` — create page via block DSL, retrieve as markdown, verify content
2. `create with markdown` — create page via `markdown` field, round-trip retrieve to verify
3. `updateContent` (list overload) — search-and-replace with `List<ContentUpdate>`
4. `updateContent` (DSL builder) — search-and-replace via `ContentUpdateBuilder`
5. `replaceContent` — full page replace, verify new content in response

---

## Files Created

- `src/main/kotlin/.../models/markdown/MarkdownModels.kt`
- `src/main/kotlin/.../api/MarkdownApi.kt`
- `src/test/resources/api/markdown/retrieve_page_markdown.json`
- `src/test/resources/api/markdown/update_page_markdown.json`
- `src/test/kotlin/unit/api/MarkdownApiTest.kt`
- `src/test/kotlin/integration/MarkdownApiIntegrationTest.kt`

## Files Modified

- `src/main/kotlin/.../NotionClient.kt` — added `val markdown = MarkdownApi(httpClient, config)`
- `src/main/kotlin/.../models/pages/PageRequests.kt` — added `markdown: String?` to `CreatePageRequest`
- `src/main/kotlin/.../models/pages/CreatePageRequestBuilder.kt` — added `markdown()` method, mutual-exclusion guards in `build()`
- `src/test/kotlin/unit/dsl/PageRequestBuilderTest.kt` — added 5 tests for `markdown()` DSL method

---

## Status

- [x] `MarkdownApi`: `retrieve()` (GET) + `includeTranscript` query param
- [x] `MarkdownApi`: `updateContent()` — typed request, list, and DSL builder overloads (PATCH)
- [x] `MarkdownApi`: `replaceContent()` — typed request and string overloads (PATCH)
- [x] `NotionClient` registration (`client.markdown`)
- [x] `CreatePageRequest.markdown` field
- [x] `CreatePageRequestBuilder.markdown()` DSL method with mutual-exclusion guards
- [x] Unit tests — `MarkdownApiTest` (8 tests)
- [x] Unit tests — `PageRequestBuilderTest` markdown section (5 tests)
- [x] Integration tests — `MarkdownApiIntegrationTest` (6 tests, all passing against live API)
- [x] Live API validated ✅

## Live API Findings

- **Table row format matters**: sending all `<td>` tags on a single line (`<tr><td>A</td><td>B</td></tr>`) caused rows to be silently dropped by the API. Fixed by sending each `<td>` on its own line — matching the format the API itself uses in responses.
- **First `# h1` becomes page title**: when creating a page via the `markdown` field without an explicit `properties.title`, the API promotes the first `# h1` heading to the page title, so it does not appear in the retrieved markdown body. Documented in the integration test with a comment.
- **Enhanced markdown round-trip**: all tested Notion-specific block types survive the round-trip — callout, toggle/details, to-do checkboxes, table, block equation, columns.
