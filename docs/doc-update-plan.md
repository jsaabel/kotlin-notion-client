# Documentation Update Plan — v0.4.0

> **Purpose**: This document catalogues every documentation change needed to bring the library docs
> up to date with the v0.4.0 development branch (`feature/v0.4`). It is the primary input for the
> next documentation-update pass. **No existing files are modified here** — this is review only.
>
> **Status legend**: `[ ]` = not yet done · `[x]` = completed
>
> Last updated: 2026-04-14 — **All documentation items completed.**

---

## Summary of post-0.3.0 Changes to Document

The following features/breaking changes were shipped since v0.3.0 and need documentation:

| Feature | Type | Affects |
|---------|------|---------|
| API version bump `2025-09-03` → `2026-03-11` | Breaking | README, all docs |
| `archived` / `archive()` → `in_trash` / `trash()` | Breaking | README, pages.md, blocks.md |
| `BlockAppendPosition` (`after` → `position`) | Breaking | blocks.md |
| `PageIcon` → `Icon`, `CalloutIcon` eliminated | Breaking | pages.md, databases.md, blocks.md |
| `heading_4` block type | New | blocks.md, notebooks |
| `tab` block type | New | blocks.md |
| `meeting_notes` block type (read-only) | New | blocks.md |
| Markdown Content API (`client.markdown`) | New | README, NEW markdown-api.md |
| Markdown comment creation | New | comments.md |
| Create page with markdown content | New | pages.md |
| Relative date filter values (`RelativeDateValue`) | New | data-sources.md |
| People `containsMe()` / `doesNotContainMe()` | New | data-sources.md |
| Verification property (`verify()` / `unverify()`) | New | pages.md |
| Status property creation in `DatabaseRequestBuilder` | New | databases.md |
| Option-level `description` field | New | databases.md |
| Property-level `description` field | New | databases.md |
| Native icons (`Icon.NativeIcon`, `NativeIconColor`) | New | pages.md, databases.md, blocks.md |
| Custom emoji listing API (`client.customEmojis`) | New | README, NEW custom-emojis.md |
| Views API (`client.views`) | New | README, NEW views-api.md |
| `ViewConfiguration` typed sealed class | New | views-api.md |
| Notebooks regained compatibility | Fix | README, all notebooks |

---

## 1. `README.md`

### 1.1 Version bump
- **Line 29 and 37**: Change `0.3.0` → `0.4.0` in both the Gradle and Maven dependency snippets.

### 1.2 API version references
- **"Why This Client?" section (line 17)**: Update `2025-09-03` → `2026-03-11` in the bullet that says "Built for Notion API version **2025-09-03**".

### 1.3 API Coverage table (lines 122–132)
Add three new rows:

| API Category | Status | Documentation |
|---|---|---|
| **Markdown** | ✅ Complete | docs/markdown-api.md |
| **Custom Emojis** | ✅ Complete | (inline in icons section or new file) |
| **Views** | ✅ Complete | docs/views-api.md |

### 1.4 Feature Highlights (lines 135–145)
Update the bullet list to reflect v0.4.0 additions:
- Change "30+ block types" → "33+ block types" (added `heading_4`, `tab`, `meeting_notes`)
- Add bullet: **Markdown Content API** — retrieve, create, and update page content as Markdown
- Add bullet: **Views API** — full CRUD for database views with typed `ViewConfiguration`
- Add bullet: **Native icons** — Notion's built-in icon library with 10 color options
- Add bullet: **Custom emoji listing** — enumerate workspace custom emojis

### 1.5 Notebook version note (lines 168–171)
Remove the current note:
> "Notebooks currently use v0.2.0 due to a binary incompatibility…"

Replace with a simple note that notebooks use the current version (v0.4.0), and list the kernel requirement (IntelliJ IDEA with Kotlin Notebook plugin or Jupyter with kotlin-jupyter-kernel).

### 1.6 Learning Resources list (lines 157–165)
- Update all notebook descriptions to note they use v0.4.0.
- Consider adding a new entry: `8. [Markdown & Views](notebooks/08-markdown-and-views.ipynb) — Markdown Content API and Views API` (if notebook 08 is created — see Notebooks section).

---

## 2. `docs/pages.md`

### 2.1 "Archive a Page" section → rename and fix (lines 174–188)
This section still uses the old API surface and must be updated entirely.

**Rename**: "Archive a Page" → "Trash a Page"

**Replace the code block** (which shows `notion.pages.archive()` and `archive()` DSL and checks `archived.archived`):
```kotlin
// Move a page to trash using the dedicated method
val trashed = notion.pages.trash("page-id")
println("Page in trash: ${trashed.inTrash}")  // true

// Or use update with the trash() DSL
val trashed = notion.pages.update("page-id") {
    trash()
}
```
Remove the old note "Notion doesn't support true deletion. Archived pages…" — replace with:
> **Note**: Notion doesn't support permanent deletion. Pages moved to trash are hidden from the UI
> but remain accessible via the API. Use `restore()` (or `trash(false)`) to bring them back.

### 2.2 "Restore an Archived Page" section → rename and fix (lines 190–196)
**Rename**: "Restore an Archived Page" → "Restore a Page from Trash"

**Replace code**:
```kotlin
val restored = notion.pages.update("page-id") {
    trash(false)  // Restore from trash
}
```

[//]: # (TODO: Confirm whether this works (lacking \n for line breaks?)
### 2.3 Add section: "Create a Page with Markdown Content (v0.4.0+)"
Insert after the "Create a Page with Position" section. Content:
```kotlin
// Create a page using markdown content instead of blocks
val page = notion.pages.create {
    parent.page("parent-page-id")
    title("My Markdown Page")

    markdown("""
        # Introduction

        This page was created using **Markdown** via the Notion API.

        > Callout text can use blockquotes.

        - Item one
        - Item two

        ```kotlin
        val client = NotionClient("token")
        ```
    """.trimIndent())
}
```
Add note: `markdown` and `content {}` (children) are mutually exclusive. Also mutually exclusive with `template`.

### 2.4 Add section: "Verification Property (v0.4.0+)"
Insert in the "Working with Page Properties" section (after existing property examples). Content:
```kotlin
// Verify a page (for pages in wiki databases)
notion.pages.update("wiki-page-id") {
    properties {
        verify("Verification")
        // Or with a 90-day expiry window:
        verify("Verification",
            start = "2026-04-14",
            end = "2026-07-13"
        )
    }
}

// Remove verification
notion.pages.update("wiki-page-id") {
    properties {
        unverify("Verification")
    }
}

// Read verification state
val page = notion.pages.retrieve("wiki-page-id")
val verification = page.properties["Verification"] as? PageProperty.Verification
println("State: ${verification?.verification?.state}")         // "verified"
println("Verified by: ${verification?.verification?.verifiedBy?.name}")
println("Expires: ${verification?.verification?.date?.end}")
```
Note: only available on pages inside wiki databases, which cannot be created programmatically.

### 2.5 Icon options: add native icon
In the "Icon and Cover Options" tip box (near the end of the file), add:
```kotlin
// Native Notion icon with optional color
icon.native("pizza")
icon.native("code", NativeIconColor.BLUE)
```
List the 10 available `NativeIconColor` values: `GRAY` (default), `LIGHT_GRAY`, `BROWN`, `YELLOW`, `ORANGE`, `GREEN`, `BLUE`, `PURPLE`, `PINK`, `RED`.

### 2.6 Property Type Reference table
Add a row for `Verification`:

| Property Type | Method | Example |
|---|---|---|
| Verification | `verify(name)` / `unverify(name)` | `verify("Verification")` |

### 2.7 Best Practices #6 (line 569)
Change: "Archive instead of delete — Notion doesn't support deletion, use `archive()` instead"
→ "**Trash instead of delete** — Notion doesn't support permanent deletion, use `trash()` to move pages to trash and `trash(false)` to restore"

### 2.8 API version note cleanup
Line 60 and 236 still say "2025-09-03 API". Since the library now targets `2026-03-11`, update these to "2026-03-11 API" (or just remove the specific version reference where it's not essential).

---

## 3. `docs/databases.md`

### 3.1 Property Type Reference table: add Status
The table (around line 270) is missing `Status`. Add:

| Status | `status(name) { option(...) }` | `status("State") { option("Not started") }` |

### 3.2 Add property-level `description` parameter
After the Property Type Reference table, add a note and example:
```kotlin
// All property builder methods accept an optional `description` parameter (max 280 chars)
properties {
    title("Task Name", description = "The name of the task")
    select("Status", description = "Current workflow state") {
        option("To Do")
        option("In Progress")
        option("Done")
    }
    date("Due Date", description = "When the task must be completed")
}
```

### 3.3 Add option-level `description` parameter
In the Select/Multi-select/Status examples, show the `description` parameter on `option()`:
```kotlin
select("Priority") {
    option("Critical", SelectOptionColor.RED, description = "Must ship this sprint")
    option("High", SelectOptionColor.ORANGE)
    option("Low", SelectOptionColor.GREEN)
}
```
Same pattern applies to `multiSelect` and `status` options.

### 3.4 Status property creation example
Add a dedicated "Create a Database with Status Property" code example:
```kotlin
val database = notion.databases.create {
    parent.page("parent-page-id")
    title("Sprint Board")
    properties {
        title("Task")
        status("State") {
            option("Backlog", SelectOptionColor.GRAY, description = "Work not yet started")
            option("In Progress", SelectOptionColor.YELLOW)
            option("Done", SelectOptionColor.GREEN)
        }
        // Note: groups cannot be configured via the API — Notion auto-creates them
    }
}
```

### 3.5 Icon options: add native icon
In the icon/cover example, add:
```kotlin
icon.native("database")
icon.native("chart", NativeIconColor.BLUE)
```

### 3.6 API version cleanup
Line 7 and 39 still reference `2025-09-03`. Update to `2026-03-11`.

---

## 4. `docs/data-sources.md`

### 4.1 Add section: Relative Date Filter Values (v0.4.0+)
Insert a new subsection after the existing "Timestamp Filters" section. Title: **Relative Date Filter Values (v0.4.0+)**

Content:
```kotlin
// Date property filters can use relative values via RelativeDateValue enum
val results = notion.dataSources.query("data-source-id") {
    filter {
        date("Due Date").equals(RelativeDateValue.TODAY)
        // Other relative values:
        // RelativeDateValue.TOMORROW
        // RelativeDateValue.YESTERDAY
        // RelativeDateValue.ONE_WEEK_AGO
        // RelativeDateValue.ONE_WEEK_FROM_NOW
        // RelativeDateValue.ONE_MONTH_AGO
        // RelativeDateValue.ONE_MONTH_FROM_NOW
    }
}

// Relative values work with all date condition operators:
date("Due Date").after(RelativeDateValue.YESTERDAY)
date("Due Date").before(RelativeDateValue.ONE_MONTH_FROM_NOW)
date("Due Date").onOrAfter(RelativeDateValue.TODAY)
date("Due Date").onOrBefore(RelativeDateValue.ONE_WEEK_FROM_NOW)
```
Note: Relative date values are resolved server-side at query time, not by the client. They can
also be used with timestamp filters (`createdTime()`, `lastEditedTime()`).

### 4.2 Add section: People "Me" Filter (v0.4.0+)
Insert after the relative date section. Title: **Filtering by the Current User (v0.4.0+)**

Content:
```kotlin
// Filter to find pages assigned to "me" (the integration's authorized user)
val myTasks = notion.dataSources.query("data-source-id") {
    filter {
        people("Assignee").containsMe()
    }
}

// Or find tasks NOT assigned to me
val othersWork = notion.dataSources.query("data-source-id") {
    filter {
        people("Assignee").doesNotContainMe()
    }
}
```
Note: Works with `people`, `created_by`, and `last_edited_by` property filters.
For internal integrations, `"me"` does not resolve to a specific user — `containsMe()` returns
no results and `doesNotContainMe()` matches all entries. This is expected Notion behaviour.

### 4.3 Update "Additional Filter Types" note (line 357)
Append to the existing note:
> **v0.4.0** added `RelativeDateValue` enum for relative date conditions (`TODAY`, `TOMORROW`,
> `YESTERDAY`, `ONE_WEEK_AGO`, `ONE_WEEK_FROM_NOW`, `ONE_MONTH_AGO`, `ONE_MONTH_FROM_NOW`) usable
> with `equals`, `before`, `after`, `onOrBefore`, `onOrAfter` on date and timestamp filters.
> Also added `containsMe()` / `doesNotContainMe()` on people property filters.

---

## 5. `docs/comments.md`

### 5.1 Available Operations: add markdown overload
Add to the operations block:
```kotlin
// Create a comment using markdown content (v0.4.0+)
suspend fun create(block: CreateCommentRequestBuilder.() -> Unit): Comment
// (Existing rich_text DSL still works — markdown() is a new alternative in the same builder)
```

### 5.2 Add section: "Create a Comment with Markdown (v0.4.0+)"
Insert after the "Comment with Links and Mentions" example:
```kotlin
val comment = notion.comments.create {
    parent.page("page-id")
    markdown("This comment was written in **Markdown** with _inline formatting_ and `code`.")
}

// Markdown with mentions (using Notion's enhanced markdown format):
val comment = notion.comments.create {
    parent.page("page-id")
    markdown("Task completed! See details in the report.")
}
```
Note: `markdown()` and `richText {}` are mutually exclusive — exactly one must be provided.
Supported inline formats: bold, italic, strikethrough, inline code, links, inline equations, mentions.

### 5.3 Validation and Limits: update content rule
Change: "Comments must have non-empty rich text content"
→ "Comments must specify content via exactly one of: `richText {}` or `markdown()`. Providing both or neither raises an `IllegalArgumentException`."

---

## 6. `docs/blocks.md`

### 6.1 Supported Block Types: add heading_4, tab, meeting_notes

**Text Blocks section**: Add `heading_4`:
- **Heading 4** - H4 sub-section heading (same capabilities as H1–H3: toggleable, colors)

**Advanced section**: Add `tab`:
- **Tab** - Container for tab-pane layouts; each pane is a paragraph child with optional icon and label

**New "Read-only" subsection** or note at the end of the list:
- **Meeting Notes** — Transcript/meeting notes block (`meeting_notes`); returned by the API but cannot be created programmatically

### 6.2 Fix "Delete a block" description
**Line 26**: Change "Delete a block (archives it)" → "Delete a block (moves it to trash)"

### 6.3 Update block count
**Line 5**: Change "30+ block types" → "33+ block types".

### 6.4 Add examples for heading_4 and tab
After the existing "Example 13: Complete Document Structure", add:

**Example: Heading 4**
```kotlin
notion.blocks.appendChildren(pageId) {
    heading1("Architecture")
    heading2("Frontend")
    heading3("Components")
    heading4("Atoms")  // New in v0.4.0
    paragraph("The smallest reusable elements.")
}
```

**Example: Tab block**
```kotlin
notion.blocks.appendChildren(pageId) {
    tab {
        // Each pane() call creates a paragraph child tab
        pane("Overview") {
            paragraph("Project summary goes here.")
        }
        pane("Details", icon = emoji("📋")) {
            bullet("Key detail one")
            bullet("Key detail two")
        }
        pane("Settings", icon = nativeIcon("gear", NativeIconColor.GRAY))
    }
}
```

---

## 7. New file: `docs/markdown-api.md`

A new documentation file is needed for the Markdown Content API, which has no existing coverage.
The file should follow the same structure as other API docs.

### Sections to include:

**Overview**
- What the Markdown Content API does: retrieve, create, and update Notion page content as text/markdown
- `client.markdown` is the entry point
- API endpoint: `2026-03-11`+

**Available Operations**
```kotlin
// Retrieve a page's content as markdown
suspend fun retrieve(pageId: String, includeTranscript: Boolean = false): PageMarkdownResponse

// Update content via search-and-replace operations
suspend fun updateContent(pageId: String, builder: ContentUpdateBuilder.() -> Unit): PageMarkdownResponse
suspend fun updateContent(pageId: String, updates: List<ContentUpdate>): PageMarkdownResponse
suspend fun updateContent(pageId: String, request: UpdateContentRequest): PageMarkdownResponse

// Replace full page content
suspend fun replaceContent(pageId: String, markdown: String): PageMarkdownResponse
suspend fun replaceContent(pageId: String, request: ReplaceContentRequest): PageMarkdownResponse
```

**Response shape** (`PageMarkdownResponse`):
- `id` — page ID
- `markdown` — the page content as markdown text
- `truncated` — `true` if the page exceeded ~20,000 blocks
- `unknownBlockIds` — block IDs that could not be represented in markdown

**Create a page with markdown content** (links to pages.md)

**Example: Retrieve page as markdown**
```kotlin
val response = notion.markdown.retrieve("page-id")
println(response.markdown)
println("Truncated: ${response.truncated}")
```

**Example: Search-and-replace (DSL)**
```kotlin
val updated = notion.markdown.updateContent("page-id") {
    replace("old text", "new text")
    replaceAll("every occurrence", "replacement")
}
```

[//]: # (TODO: Verify lack of \n')
**Example: Full page replace**
```kotlin
notion.markdown.replaceContent("page-id", """
    # New Content

    Everything above was replaced.
""".trimIndent())
```

**Enhanced Markdown format**
- Superset of standard Markdown
- Notion-specific block types use custom HTML-like tags: callouts, toggles, columns, etc.
- Tab-indented children
- Reference: Notion's enhanced markdown guide

**Known behaviours / gotchas**
- The first `# h1` heading in a `markdown`-created page becomes the page title (not part of body)
- Table rows must have `<td>` tags on separate lines (the library handles this correctly)
- `includeTranscript: true` also includes meeting notes transcript blocks

---

## 8. New file: `docs/views-api.md`

The `docs/views-api/` directory currently contains raw OpenAPI reference files (not user documentation). A proper user-facing guide is needed.

### Sections to include:

**Overview**
- Views are display configurations for databases (table, board, gallery, calendar, timeline, list, chart, map, form, dashboard)
- `client.views` is the entry point
- Views belong to a database (or a dashboard)

**Available Operations**
```kotlin
// Retrieve, list
suspend fun retrieve(viewId: String): View
suspend fun list(databaseId: String? = null, dataSourceId: String? = null): ViewList
fun listAsFlow(databaseId: String? = null, dataSourceId: String? = null): Flow<ViewReference>

// Create, update, delete
suspend fun create(block: CreateViewRequestBuilder.() -> Unit): View
suspend fun update(viewId: String, block: UpdateViewRequestBuilder.() -> Unit): View
suspend fun delete(viewId: String): PartialView

// Queries (saved filter/sort combinations)
suspend fun createQuery(viewId: String): ViewQuery
suspend fun getQueryResults(viewQueryId: String, ...): ViewQueryResults
suspend fun deleteQuery(viewQueryId: String): DeletedViewQuery
```

**Example: List views for a database**
```kotlin
notion.views.listAsFlow(databaseId = "db-id").collect { viewRef ->
    println("${viewRef.name}: ${viewRef.type}")
}
```

**Example: Create a new view**
```kotlin
val view = notion.views.create {
    database("data-source-id")  // or dashboard("dashboard-id")
    type("table")
    name("My Table View")
}
```

**Example: Create a view with property visibility**
```kotlin
val view = notion.views.create {
    database("data-source-id")
    type("table")
    name("Compact Table")
    // Show only key properties
    showProperties("prop-id-1", "prop-id-2")
    // Or hide specific ones:
    // hideProperties("prop-id-3", "prop-id-4")
}
```

**Example: Typed ViewConfiguration**
```kotlin
// Full control via typed configuration
val view = notion.views.create {
    database("data-source-id")
    type("gallery")
    name("Gallery View")
    configuration(ViewConfiguration.Gallery(
        coverType = CoverType.PAGE_COVER,
        coverSize = CoverSize.MEDIUM,
        coverAspect = CoverAspect.CONTAIN,
        fitImage = true,
    ))
}
```

**Supported ViewConfiguration subtypes** — table of all 10:
`Table`, `Board`, `Calendar`, `Timeline`, `Gallery`, `List`, `Chart`, `Map`, `Form`, `Dashboard`

**Queries**
- A `ViewQuery` is a saved snapshot of a filtered/sorted view
- Create → `createQuery(viewId)` → returns a query with results URL
- Fetch results → `getQueryResults(queryId, pageSize = …)` → paginated page list
- Delete → `deleteQuery(queryId)`

**Notes**
- `filter` and `sorts` in views use the same format as `DataSourceFilter` / `DataSourceSort`
- `Dashboard` views are read-only from the API (no `dashboardViewConfigRequest`)
- `showProperties()` / `hideProperties()` throw `IllegalArgumentException` for view types without a `properties` field (form, chart, dashboard)

---

## 9. `docs/testing.md`

### 9.1 Update unit test count
**Line 12**: "~481 tests run in ~200ms" → update to current count. As of the consolidation commit
(`471714a`), unit tests are 600+. Use "600+ tests" (or check the exact count with `./gradlew test`
and update).

### 9.2 Fix NOTION_CLEANUP_AFTER_TEST default
**Line 77 (Environment Variables table)**:

Current: `NOTION_CLEANUP_AFTER_TEST` | "Set to `"false"` to keep test data" | Optional (default: true)

The integration tests now default to **not** cleaning up (pages are preserved as live examples).
Update the row:

| `NOTION_CLEANUP_AFTER_TEST` | Set to `"true"` to delete test data after each run | Optional (default: false — pages are preserved) |

### 9.3 Update integration test file references
The testing doc may reference individual old integration test files (e.g., "PagesIntegrationTest").
After the consolidation into 10 merged specs, update any listed test file names:
- `CorePagesIntegrationTest`, `BlockTypesIntegrationTest`, `DatabaseFeaturesIntegrationTest`,
  `FiltersIntegrationTest`, `ViewsIntegrationTest`, `MediaIntegrationTest`,
  `CommentsIntegrationTest`, `AppearanceIntegrationTest`, `SearchAndTemplatesIntegrationTest`,
  `UsersIntegrationTest`

### 9.4 Add NOTION_TEST_WIKI_PAGE_ID env var
Add a row in the Environment Variables table:

| `NOTION_TEST_WIKI_PAGE_ID` | A page inside a wiki database | `WikiVerificationIntegrationTest` (optional) |

---

## 10. Notebooks

### 10.1 All notebooks: version bump (dependency cell)

All 7 notebooks need updating. Current state:
- `01-getting-started.ipynb` → `0.3.0` (updated in workspace, not yet `0.4.0`)
- `02-reading-databases.ipynb` → `0.3.0` (updated in workspace, not yet `0.4.0`)
- `03-creating-pages.ipynb` → `0.2.0` (not yet touched)
- `04-working-with-blocks.ipynb` → `0.2.0` (not yet touched)
- `05-rich-text-dsl.ipynb` → `0.3.0` (updated in workspace, not yet `0.4.0`)
- `06-advanced-queries.ipynb` → `0.2.0` (not yet touched)
- `07-file-uploads.ipynb` → `0.2.0` (not yet touched)

All dependency cells should read:

```kotlin
@file:DependsOn("it.saabel:kotlin-notion-client:0.4.0")
```

### 10.2 Notebook intro notes: remove binary incompatibility warning

In the first cell of each notebook (e.g., notebook 01 currently says "these notebooks currently
use v0.2.0 due to a binary incompatibility…"), remove or replace that caveat.

The user confirmed notebooks work correctly now. Replace with a simple statement such as:
> These notebooks use v0.4.0. Run each cell in order; set the required environment variables before
> starting.

### 10.3 `notebooks/02-reading-databases.ipynb` — add relative date filter examples

This notebook already covers filters. Add a new cell demonstrating relative date values:

```kotlin
// Relative date filter — tasks due "this week"
val upcoming = runBlocking {
    notion.dataSources.query(dataSourceId) {
        filter {
            date("Due Date").onOrAfter(RelativeDateValue.TODAY)
            date("Due Date").onOrBefore(RelativeDateValue.ONE_WEEK_FROM_NOW)
        }
    }
}
println("Tasks due this week: ${upcoming.size}")
```

### 10.4 `notebooks/README.md` — version reference

The notebook README likely still mentions the old version. Update to v0.4.0 and remove the
binary-incompatibility note.

### 10.5 New notebook: `08-whats-new-in-v0.4.ipynb`

A new notebook in the same style as the existing notebooks, explicitly introducing all new v0.4.0
capabilities. This should feel like a guided tour of what changed — someone upgrading from v0.3.0
should be able to read through it and immediately understand what's new and how to use it.

Suggested structure (each section = one markdown cell + one or more code cells):

**Setup** — dependency (`0.4.0`), client init, test page setup (same pattern as other notebooks)

**Section 1 — Breaking changes from v0.3.x**
- Briefly note `inTrash` replacing `archived`, `trash()` replacing `archive()`
- Show `BlockAppendPosition` usage for the `position` parameter
- Show `Icon.NativeIcon` replacing `PageIcon` / `CalloutIcon`

**Section 2 — New block types**
- `heading4()` in a document
- `tab { pane(...) { … } }` with icon variants
- Note that `meeting_notes` blocks are read-only

**Section 3 — Markdown Content API**
- Retrieve a page as markdown (show the response shape)
- Create a page using `markdown()` DSL
- Search-and-replace via `updateContent { replace(…) }`
- Full page replace via `replaceContent`

**Section 4 — Markdown in comments**
- Create a comment using `markdown()` instead of `richText {}`

**Section 5 — Views API**
- List existing views for a database (show types and names)
- Create a table view with `showProperties`
- Create a gallery view with `ViewConfiguration.Gallery`
- Create a view query and retrieve its results

**Section 6 — New filter and property features**
- Relative date filter: `date("Due Date").equals(RelativeDateValue.TODAY)`
- People `containsMe()` filter
- `verify()` / `unverify()` on a wiki page (gated on `NOTION_TEST_WIKI_PAGE_ID`)
- Status property in a new database

**Section 7 — Native icons and custom emojis**
- `icon.native("star", NativeIconColor.YELLOW)` on a page
- `nativeIcon()` in a callout block
- `notion.customEmojis.list()` (if workspace has custom emojis)

**README entry to add** (in Learning Resources):
> `8. What's New in v0.4.0` — Tour of all breaking changes and new features added in v0.4.0

---

## 11. Icon and Cover docs: cross-cutting note

`pages.md`, `databases.md`, and `blocks.md` all have icon/cover examples. All three need the
following addition:

**Native icon DSL**:
```kotlin
// In page/database builders:
icon.native("pizza")           // uses GRAY by default
icon.native("star", NativeIconColor.YELLOW)

// In block builders (callout, tab paragraph):
callout(nativeIcon("gear", NativeIconColor.GRAY)) {
    text("Settings note")
}
```

Available `NativeIconColor` values: `GRAY`, `LIGHT_GRAY`, `BROWN`, `YELLOW`, `ORANGE`, `GREEN`,
`BLUE`, `PURPLE`, `PINK`, `RED`.

The existing note "Icon and cover removal is not currently supported" on `pages.md` can remain as-is.

---

## 12. Consider: `docs/custom-emojis.md` (new, optional)

The custom emoji listing API (`client.customEmojis`) is small enough that it might not need a
dedicated file — it could be mentioned in passing in `README.md`'s API Coverage table and included
as a brief section in a future "Icons and Media" guide. However, if the existing `file-uploads.md`
is a good analogue for small-scope API docs, a dedicated file could follow the same structure:

**Available Operations**
```kotlin
suspend fun list(
    startCursor: String? = null,
    pageSize: Int? = null,
    name: String? = null,
): CustomEmojiList

fun listAsFlow(name: String? = null): Flow<CustomEmojiObject>
fun listPagedFlow(name: String? = null): Flow<CustomEmojiList>
```

**Example: List all custom emojis**
```kotlin
notion.customEmojis.listAsFlow().collect { emoji ->
    println("${emoji.name}: ${emoji.url}")
}
```

**Example: Find a specific custom emoji by name**
```kotlin
val results = notion.customEmojis.list(name = "bufo")
val emoji = results.results.firstOrNull()
    ?: error("Emoji not found")

// Use it as a page icon
notion.pages.create {
    parent.page("page-id")
    title("Bufo Page")
    icon.customEmoji(emoji.id)
}
```

---

## 13. Quick cross-checks before applying changes

Before making the actual edits in the next pass:

- [x] Confirm the exact unit test count via `./gradlew test` and use that number in `testing.md` — used 600+
- [x] Confirm v0.4.0 is the intended published version — yes, all docs use 0.4.0
- [x] Notebooks 03, 04, 06, 07 confirmed still on v0.2.0; all 7 notebooks need updating to v0.4.0 — done
- [ ] Decide whether `docs/views-api/` directory (raw OpenAPI files) should be kept alongside the new `views-api.md`, or the directory renamed to something like `docs/views-api-reference/` — **open, deferred to next pass**
- [x] Decide whether custom emojis get their own doc file — created `docs/custom-emojis.md`
- [ ] Check if any example files in `src/test/kotlin/examples/` reference `archive()` — **open, deferred**

---

## 14. Release Notes Draft — v0.4.0

> **Note**: This is a draft of release notes for the v0.4.0 GitHub release. The authoritative
> source is the commit history. Adjust wording to taste before publishing.

---

### kotlin-notion-client v0.4.0

**API version**: `2026-03-11` (upgraded from `2025-09-03`)

---

#### Breaking Changes

**⚠️ API version `2026-03-11` — migration required if upgrading from v0.3.x**

1. **`archived` → `in_trash` / `inTrash`** — The `archived` field on pages, databases, blocks,
   comments, and file uploads has been replaced by `inTrash` (`@SerialName("in_trash")`). Update
   any code that reads `.archived` to read `.inTrash`.

2. **`archive()` → `trash()` / `restore()`** — The `archive()` / `unarchive()` DSL methods have
   been renamed to match official Notion terminology:
   - `notion.pages.trash("page-id")` (was `archive`)
   - `notion.databases.trash("database-id")` (was `archive`)
   - In update builders: `trash()` / `trash(false)` (was `archive()` / `archive(false)`)
   - In data source builders: `trash()` / `restore()` (was `archive()` / `unarchive()`)

3. **`appendChildren` position parameter** — The `after` parameter has been replaced by a typed
   `position: BlockAppendPosition?` sealed class:
   ```kotlin
   // v0.4.0
   notion.blocks.appendChildren(pageId, position = BlockAppendPosition.Start) { … }
   notion.blocks.appendChildren(pageId, position = BlockAppendPosition.AfterBlock(BlockReference(id))) { … }
   ```

4. **`PageIcon` → `Icon`, `CalloutIcon` eliminated** — All icon fields now use the unified `Icon`
   sealed class from `models.base`. `PageIcon` and `CalloutIcon` no longer exist. Icon construction:
   ```kotlin
   Icon.Emoji("🎯")          // was PageIcon.Emoji / CalloutIcon(type="emoji", …)
   Icon.External(url)         // was PageIcon.External / CalloutIcon(type="external", …)
   Icon.NativeIcon(…)         // new in v0.4.0
   ```

5. **`transcription` block type → `meeting_notes`** — The `Block.Transcription` / `Block.MeetingNotes`
   type is now serialized under the key `meeting_notes`. This is handled automatically; no code
   changes needed unless you pattern-matched on the string `"transcription"`.

---

#### New Features

**Markdown Content API** (`client.markdown`)

A new API for working with page content as Markdown text:
```kotlin
// Retrieve page as markdown
val md = notion.markdown.retrieve("page-id")
println(md.markdown)

// Replace all content
notion.markdown.replaceContent("page-id", "# New content\n\nHello!")

// Search-and-replace
notion.markdown.updateContent("page-id") {
    replace("old phrase", "new phrase")
}

// Create page with markdown (in CreatePageRequestBuilder)
notion.pages.create {
    parent.page("parent-id")
    markdown("# Title\n\nPage content here.")
}
```

**Markdown Comments** — `notion.comments.create` now accepts `markdown()` as an alternative to
`richText {}`:
```kotlin
notion.comments.create {
    parent.page("page-id")
    markdown("Comment with **bold** and `code`.")
}
```

**Views API** (`client.views`)

Full CRUD for database views with typed configuration:
```kotlin
// List views
notion.views.listAsFlow(databaseId = "db-id").collect { … }

// Create a view
val view = notion.views.create {
    database("data-source-id")
    type("gallery")
    name("My Gallery")
    configuration(ViewConfiguration.Gallery(
        coverType = CoverType.PAGE_COVER,
        coverSize = CoverSize.MEDIUM,
    ))
}

// Show/hide properties
notion.views.create {
    database("data-source-id")
    type("table")
    showProperties("prop-id-1", "prop-id-2")
}
```
All 10 view types are supported with typed `ViewConfiguration` subtypes: `Table`, `Board`,
`Calendar`, `Timeline`, `Gallery`, `List`, `Chart`, `Map`, `Form`, `Dashboard`.

**New block types**
- `heading4` / `heading_4` — H4 section heading (same capabilities as H1–H3)
- `tab` — tab-pane container block
- `meeting_notes` — read-only transcript block (can be read, not created)

**Relative date filter values** (`RelativeDateValue`)

```kotlin
notion.dataSources.query("data-source-id") {
    filter {
        date("Due Date").equals(RelativeDateValue.TODAY)
        date("Due Date").after(RelativeDateValue.YESTERDAY)
        date("Due Date").onOrBefore(RelativeDateValue.ONE_WEEK_FROM_NOW)
    }
}
```
Seven relative values: `TODAY`, `TOMORROW`, `YESTERDAY`, `ONE_WEEK_AGO`, `ONE_WEEK_FROM_NOW`,
`ONE_MONTH_AGO`, `ONE_MONTH_FROM_NOW`. Works on both date properties and timestamp filters.

**People "me" filter**
```kotlin
filter { people("Assignee").containsMe() }
filter { people("Assignee").doesNotContainMe() }
```

**Verification property** (wiki databases only)
```kotlin
notion.pages.update("wiki-page-id") {
    properties {
        verify("Verification")                           // mark as verified
        verify("Verification", start = "2026-04-14", end = "2026-07-13")  // with expiry
        unverify("Verification")                         // remove verification
    }
}
```

**Status property creation**

The `status()` property builder is now available in `DatabasePropertiesBuilder`. Options (name +
color) can be specified; groups are auto-created by Notion:
```kotlin
notion.databases.create {
    parent.page("parent-id")
    title("Sprint Board")
    properties {
        title("Task")
        status("State") {
            option("Backlog", SelectOptionColor.GRAY)
            option("In Progress", SelectOptionColor.YELLOW)
            option("Done", SelectOptionColor.GREEN)
        }
    }
}
```

**Property and option descriptions**

All `CreateDatabaseProperty` subtypes and `option()` calls now accept an optional `description`:
```kotlin
properties {
    select("Priority", description = "Task urgency level") {
        option("High", SelectOptionColor.RED, description = "Must ship this sprint")
        option("Low", SelectOptionColor.GREEN)
    }
}
```
Property descriptions are limited to 280 characters (validated at build time).

**Native icons** (`Icon.NativeIcon`)

Notion's built-in icon library is now supported:
```kotlin
// In page/database builders
icon.native("pizza")
icon.native("star", NativeIconColor.YELLOW)

// In block builders (callout, tab)
callout(nativeIcon("gear")) { text("Settings") }
```
Ten colors: `GRAY` (default), `LIGHT_GRAY`, `BROWN`, `YELLOW`, `ORANGE`, `GREEN`, `BLUE`,
`PURPLE`, `PINK`, `RED`.

**Custom emoji listing** (`client.customEmojis`)

```kotlin
// List all workspace custom emojis
notion.customEmojis.listAsFlow().collect { emoji ->
    println("${emoji.name}: ${emoji.id}")
}

// Filter by name (exact match)
val results = notion.customEmojis.list(name = "bufo")
```

---

#### Other Improvements

- **Integration test consolidation** — ~30 integration test files merged into 10 well-structured
  specs. Each spec creates a single container page with a descriptive callout, making live test
  pages legible in the Notion workspace. Container page URLs are printed during test runs.
- **`dateMention(LocalDateTime, TimeZone)` fix** — Previously passed the UTC instant; now
  correctly passes the local time as formatted in the given timezone.
- **Dependency upgrades** — All Gradle dependencies reviewed and updated to current versions.
