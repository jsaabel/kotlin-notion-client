# Pagination Limit Investigation & Fix

**Date**: 2026-04-15
**Status**: ✅ Complete

## Background

While updating notebook `06-advanced-queries.ipynb` for v0.4.0, Example 11 was written to showcase
"fetching only the first N results" using `pageSize(N)`. This turned out to misrepresent the current
behaviour: `pageSize(N)` sets the Notion API's `page_size` parameter per request, but the client
**always auto-paginates** until all results are collected. The Notion API supports stopping after a
single page of results — we just don't expose that option.

Captured as **Idea #10** in `IDEAS.md`.

## Problem Statement

`dataSources.query { pageSize(5) }` was doing the following:

1. Sends `POST /data_sources/{id}/query` with `page_size=5`
2. Checks `has_more` in the response — if `true`, fetches the next page via cursor
3. Repeats until `has_more=false`
4. Returns **all** collected results

`pageSize(N)` controlled the *batch size* per API call, not the *total result count*. A caller who
genuinely wants only the top 5 results (e.g. a "most recently edited tasks" widget) had no way to
opt out of auto-pagination. `search()` was already single-call; `BlocksApi.retrieveChildren()`
had the same always-auto-paginate gap as `DataSourcesApi.query()`.

## Investigation Findings

- **`DataSourcesApi.query()`**: Always overrides `pageSize` with `MAX_PAGE_SIZE=100` and loops;
  the private `querySinglePage()` is the correct primitive — it was just not accessible.
- **`BlocksApi.retrieveChildren()`**: Same pattern; private `retrieveChildrenPage()` is the primitive.
- **`SearchApi.search()`**: Already single-call — returns a raw `SearchResponse` with cursor/`hasMore`.
  No change needed here.
- **`Pagination.kt`**: Generic helpers (`collectAll`, `asFlow`, `asPagesFlow`) are correct as-is;
  the gap is at the API layer, not in the utilities.

## Solution

Added two new public methods that each make exactly one API call, returning the raw response so the
caller has access to `results`, `hasMore`, and `nextCursor`:

### `DataSourcesApi.queryFirstPage(dataSourceId, builder)`

```kotlin
val response = notion.dataSources.queryFirstPage("data-source-id") {
    sort { property("Created").descending() }
    pageSize(5)
}
val top5 = response.results       // at most 5 pages
val hasMore = response.hasMore    // true if more results exist
val cursor = response.nextCursor  // for manual follow-up if needed
```

- Returns `DataSourceQueryResponse`
- Respects `pageSize()` and `startCursor()` from the DSL builder
- Default (no `pageSize()`) uses Notion's own default of 100

### `BlocksApi.retrieveChildrenFirstPage(blockId, pageSize, startCursor)`

```kotlin
val response = notion.blocks.retrieveChildrenFirstPage(pageId, pageSize = 5)
val preview = response.results    // at most 5 blocks
val cursor = response.nextCursor  // for manual follow-up
```

- Returns `BlockList`
- `pageSize` defaults to 100; `startCursor` defaults to null (first page)

Both methods leave the existing `query()` / `retrieveChildren()` behaviour completely unchanged.

## Relevant Files Modified

- `src/main/kotlin/it/saabel/kotlinnotionclient/api/DataSourcesApi.kt` — `queryFirstPage()` added
- `src/main/kotlin/it/saabel/kotlinnotionclient/api/BlocksApi.kt` — `retrieveChildrenFirstPage()` added
- `src/test/kotlin/integration/DatabaseFeaturesIntegrationTest.kt` — test 14
- `src/test/kotlin/integration/BlockTypesIntegrationTest.kt` — test 5

## Notebook Follow-up Needed

`notebooks/06-advanced-queries.ipynb` Example 11 still describes `pageSize(5)` as "fetch first 5
results". It should be updated to use `queryFirstPage { pageSize(5) }` and explain the distinction
from `query()`.

## Commits

1. **`build: skip GPG signing when -PskipSigning property is set`**
   - `build.gradle.kts`

2. **`feat: add typed Icon callout overload and Status property extension helpers`**
   - `src/main/kotlin/.../models/blocks/PageContentBuilder.kt`
   - `src/main/kotlin/.../models/pages/PageExtensions.kt`

3. **`docs: comprehensive v0.4.0 documentation update`**
   - `README.md`
   - `docs/blocks.md`, `docs/comments.md`, `docs/data-sources.md`, `docs/databases.md`,
     `docs/pages.md`, `docs/testing.md`
   - `docs/markdown-api.md`, `docs/views-api.md`, `docs/custom-emojis.md`, `docs/doc-update-plan.md` (new)
   - `journal/2026_04_14_03_Documentation_Update_v0.4.md` (new)
   - `journal/2026_04_15_01_Notebook_Fixes_And_Date_API_Note.md` (new)
   - `journal/2026_04_15_02_Pagination_Limit_Investigation.md` (this file — new)
   - `IDEAS.md`

4. **`feat(notebooks): update all notebooks to v0.4.0 and add what's-new notebook`**
   - `notebooks/01` through `notebooks/07` (updated to 0.4.0)
   - `notebooks/08-whats-new-in-v0.4.ipynb` (new)
   - `notebooks/README.md`

5. **`feat: add queryFirstPage and retrieveChildrenFirstPage for single-page access`**
   - `src/main/kotlin/.../api/DataSourcesApi.kt`
   - `src/main/kotlin/.../api/BlocksApi.kt`
   - `src/test/kotlin/integration/DatabaseFeaturesIntegrationTest.kt`
   - `src/test/kotlin/integration/BlockTypesIntegrationTest.kt`

6. **`test: add Markdown API line-break behaviour investigation`**
   - `src/test/kotlin/integration/CommentsAndMarkdownIntegrationTest.kt`