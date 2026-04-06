# Notion API Upgrade Plan

Tracks features from the Notion API changelog that need to be added or updated in this client library.
Reference: [Notion API Changelog](https://developers.notion.com/page/changelog)

Current API version in use: `2025-09-03`
Target API version: `2026-03-11`

> **Note on documentation:** The local reference docs under `reference/notion-api/` were written against older API versions and do not cover most of the features in this plan. Before implementing each phase, fetch the current live documentation for the relevant endpoints — the links below point to the right pages but the content may have changed significantly. When new docs are retrieved, consider saving them to `reference/notion-api/` to keep the local reference up to date.

---

## Phase 1 — API Version Upgrade (Breaking Changes in `2026-03-11`)

These three changes are breaking in the new API version and should be done together as a single version bump.

### Docs
- [Versioning guide](https://developers.notion.com/reference/versioning) — local copy: `reference/notion-api/documentation/general/06_Versioning.md`
- [Changelog entry: 2026-03-11](https://developers.notion.com/page/changelog)

### Tasks

- [ ] **Bump default API version** to `2026-03-11` in `NotionConfig`
- [ ] **Replace `archived` with `in_trash`** across all models
  - `Block` — all subtypes still declare `archived`
  - `Page` — needs `in_trash` field
  - `Database` — needs `in_trash` field
  - `DataSource` already has `in_trash` ✅
- [ ] **Replace `after` with `position`** in `BlocksApi.appendChildren`
  - The `after` parameter (block ID after which to insert) is now called `position`
  - Ref: [Append block children](https://developers.notion.com/reference/patch-block-children)

---

## Phase 2 — New Block Types

### Docs
- [Block object reference](https://developers.notion.com/reference/block) — local copy: `reference/notion-api/documentation/objects/01_Block_Types.md`

### Tasks

- [ ] **Add `heading_4` block type**
  - Add to `Block` sealed class
  - Add to `BlockRequest` sealed class
  - Add builder method in `PageContentBuilder`
- [ ] **Add `tab` block type**
  - New block for organizing content into labeled sections
  - Child paragraphs support an optional `icon` field
  - Add to `Block`, `BlockRequest`, and `PageContentBuilder`
- [ ] **Add `meeting_notes` block type** (renamed from `transcription`)
  - Was announced in Feb 2026 as `transcription`, renamed in the `2026-03-11` version
  - Neither name currently exists in our models — add as `meeting_notes`
  - Ref: [Changelog entry: 2026-02-26](https://developers.notion.com/page/changelog)

---

## Phase 3 — Views API

Eight entirely new endpoints for programmatically managing database views. No `ViewsApi` exists yet.

### Docs
- [Changelog entry: 2026-03-19](https://developers.notion.com/page/changelog)
- Views API reference: https://developers.notion.com/reference/create-a-view *(fetch to confirm exact paths)*

### Tasks

- [ ] **Create `View` model** with all relevant fields
- [ ] **Create `ViewsApi`** with these endpoints:
  - `create(databaseId, ...)` — `POST /v1/databases/{database_id}/views`
  - `retrieve(viewId)` — `GET /v1/views/{view_id}`
  - `update(viewId, ...)` — `PATCH /v1/views/{view_id}`
  - `delete(viewId)` — `DELETE /v1/views/{view_id}`
  - `list(databaseId)` — `GET /v1/databases/{database_id}/views`
  - `query(viewId, ...)` — `POST /v1/views/{view_id}/query`
- [ ] **Register `ViewsApi`** on the `NotionClient`
- [ ] **Add webhook event types** `view.created`, `view.updated`, `view.deleted` if we model webhook events

---

## Phase 4 — Markdown Content API

Three new endpoints for reading and writing page content using enhanced markdown. No markdown endpoints exist yet.

### Docs
- [Changelog entry: 2026-02-26](https://developers.notion.com/page/changelog)
- Markdown API reference: https://developers.notion.com/reference/get-page-markdown *(fetch to confirm)*

### Tasks

- [ ] **Create `MarkdownApi`** with:
  - `get(pageId)` — `GET /v1/pages/{page_id}/markdown`
  - `create(pageId, markdown)` — `POST /v1/pages/{page_id}/markdown`
  - `update(pageId, markdown)` — `PATCH /v1/pages/{page_id}/markdown` (supports `update_content` and `replace_content` commands)
- [ ] **Register `MarkdownApi`** on the `NotionClient`

---

## Phase 5 — Filter Enhancements

### Docs
- [Filter database entries](https://developers.notion.com/reference/post-database-query-filter) — local copy: `reference/notion-api/documentation/general/07_Filter_Database_Entries.md`

### Tasks

- [ ] **Add relative date filter values** to `DataSourceQueryBuilder`
  - Accepted values: `"today"`, `"tomorrow"`, `"yesterday"`, `"one_week_ago"`, `"one_week_from_now"`, `"one_month_ago"`, `"one_month_from_now"`
  - These are used in date property filter conditions
- [ ] **Add `"me"` filter for people properties**
  - People properties now accept `"me"` as value in `contains`/`does_not_contain` filter conditions
  - Resolves to the integration's own user at query time

---

## Phase 6 — Minor Additions + Dependency Upgrades

### Tasks

- [ ] **Status property creation in `DatabaseRequestBuilder`**
  - Can already *read* `Status` page properties — need to be able to *create* the property on a database
  - Ref: [Database properties](https://developers.notion.com/reference/property-schema-object) — local copy: `reference/notion-api/documentation/objects/03_Database_DatabaseProperties.md`
- [ ] **Writable verification property**
  - Wiki database verification property can now be set via Create/Update page endpoints
  - Ref: [Update page](https://developers.notion.com/reference/patch-page) — local copy: `reference/notion-api/documentation/endpoints/Update_Page_2025.md`
- [ ] **Custom emoji listing endpoint**
  - New `GET /v1/custom_emojis` endpoint — `CustomEmojiObject` model already exists
  - Ref: [Changelog entry: 2026-03-25](https://developers.notion.com/page/changelog)
- [ ] **Native icons as structured type**
  - Notion native icons now return as a structured object type rather than a plain string
  - Need to verify current `PageIcon` model handles this correctly
- [ ] **Upgrade all dependencies** before the 0.4.0 release
  - Review `gradle/libs.versions.toml` for outdated versions (Ktor, Kotlin, kotlinx-serialization, kotlinx-datetime, Kotest, etc.)
  - Run `./gradlew dependencyUpdates` or check manually, then update and verify all tests pass

---

## Already Implemented ✅

| Feature | Location |
|---------|----------|
| Move page API | `PagesApi.move`, `moveToPage`, `moveToDataSource` |
| Page positioning (new page placement) | `PagesApi` |
| Template APIs (data source templates) | `DataSource.Template`, `TemplatesResponse`, `DataSourcesApi` |
| `is_locked` parameter | `PagesApi` |
| `in_trash` field | `DataSource` model |
| Status property reading | `PageProperty.Status` |
| Custom emoji types | `CustomEmojiObject`, `PageIcon.CustomEmoji` |
| Data sources API | `DataSourcesApi` (full implementation) |
| `ntn_` token prefix | No code change needed |
| Timestamp filters for data source queries | `DataSourceQueryBuilder` |