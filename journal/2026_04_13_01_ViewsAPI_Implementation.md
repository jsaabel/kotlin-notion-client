# Development Journal - April 13, 2026

## Views API Implementation — Complete

This entry documents the full Views API implementation executed in one session on April 13, 2026,
following the plan laid out in `2026_04_12_02_ViewsAPI_Plan.md`.

---

## What Was Built

### New Production Files

| File | Purpose |
|---|---|
| `src/main/kotlin/.../models/views/View.kt` | All response models + `ViewPropertyConfig` |
| `src/main/kotlin/.../models/views/ViewRequests.kt` | All request models |
| `src/main/kotlin/.../models/views/ViewRequestBuilder.kt` | DSL builders for create + update |
| `src/main/kotlin/.../api/ViewsApi.kt` | All 8 endpoint methods + DSL overloads |

### Modified Production Files

| File | Change |
|---|---|
| `NotionClient.kt` | Added `val views = ViewsApi(httpClient, config)` and import |

### New Test Files

| File | Purpose |
|---|---|
| `src/test/resources/api/views/retrieve_view.json` | Full View fixture |
| `src/test/resources/api/views/list_views.json` | ViewList fixture (2 references) |
| `src/test/resources/api/views/partial_view.json` | PartialView fixture (delete response) |
| `src/test/resources/api/views/create_view_query.json` | ViewQuery fixture |
| `src/test/resources/api/views/view_query_results.json` | ViewQueryResults fixture |
| `src/test/resources/api/views/deleted_view_query.json` | DeletedViewQuery fixture |

### Modified Test Infrastructure Files

| File | Change |
|---|---|
| `TestFixtures.kt` | Added `Views` nested object with fixture loaders |
| `MockResponseBuilder.kt` | Added 7 `addView*Response()` helpers for all endpoint types |
| `unit/api/ViewsApiTest.kt` | New — comprehensive unit tests including DSL builder coverage |
| `integration/ViewsIntegrationTest.kt` | New — 4 integration test scenarios including property visibility |

---

## Design Decisions

### 1. DSL builders (`ViewRequestBuilder.kt`)

`CreateViewRequestBuilder` and `UpdateViewRequestBuilder` follow the established codebase pattern
(`@DslMarker`, builder class, top-level entry-point function, API overload delegates to direct method).

Key design choices for the view DSL specifically:
- **Three mutually exclusive parent methods** — `database(id)`, `dashboard(id)`, `createDatabase(pageId)` —
  validated in `build()` with a `require(parentCount == 1)` check. This maps cleanly to the API's
  one-of constraint without forcing callers to understand the underlying `CreateViewRequest` field names.
- **`showProperties()` / `hideProperties()`** — accept vararg property IDs and accumulate into a
  `List<ViewPropertyConfig>`. When any visibility is set, `build()` auto-constructs the
  `configuration` `JsonObject` (wrapping entries under the view type key, e.g. `{ "type": "table", "properties": [...] }`).
  This means callers never need to touch raw JSON for the common case of property show/hide.
- **`UpdateViewRequestBuilder.type()`** — required when using `showProperties`/`hideProperties` on
  update, since the configuration envelope must know the view type. A `requireNotNull` enforces this
  at `build()` time with a clear message.

### 2. `ViewPropertyConfig` data class

Added to `View.kt` as a serializable data class with `propertyId`, `visible`, `width`, `wrap`.
Matches the `viewPropertyConfigRequest` schema exactly. Kept in the response model file because it
appears in both request and response contexts (same shape in both directions per the API spec).

### 3. `configuration` as `JsonObject?` (Plan Option A)

Per the plan, `ViewConfiguration` is represented as `JsonObject?` in both `View` and request models.
The full sealed class hierarchy (table, board, calendar, etc.) would require a large custom serializer
and the full shapes of ~10 nested helper types (`GroupByConfig`, `CoverConfig`, etc.) that weren't
fully documented in the fetched API docs.

This keeps the API live and functional while remaining consistent with how `filter` and `sorts` are
already handled in the codebase. A follow-up can add typed `ViewConfiguration` as Plan Option B.

### 2. `filter` and `sorts` as `JsonObject?` / `JsonArray?`

Same pattern as the data source query models. Callers can pass raw JSON or build their own helper.

### 3. `ViewList` implements `PaginatedResponse<ViewReference>`

This enables `Pagination.asFlow` and `Pagination.asPagesFlow` to work, giving callers both
`listAsFlow()` (emits `ViewReference`) and `listPagedFlow()` (emits `ViewList`).

### 4. `create()` validation — mutual exclusion of parent fields

The API requires exactly one of `databaseId`, `viewId`, or `createDatabase`. This is enforced in
`ViewsApi.create()` with a `require()` before the HTTP call, matching the fail-fast philosophy.

### 5. `list()` validation

At least one of `databaseId` or `dataSourceId` must be provided. Enforced with `require()`.

### 6. `ViewPosition` / `WidgetPlacement` as sealed classes

These are serialized by type discriminator (`type: "start"` / `type: "end"` / etc.) which is the
standard Kotlin sealed class `@SerialName` pattern used throughout the codebase.

### 7. `PageIdParent` / `AfterBlockPosition` defined locally in `ViewRequests.kt`

The plan noted to check if these existed in the codebase first. After checking, neither type
existed in production code. Rather than reusing page-layer types from a different module, they
are defined directly in `ViewRequests.kt` to keep the views module self-contained.

### 8. `toApiError()` extension on `HttpResponse`

Rather than duplicating 5 lines of error-body-reading boilerplate in every method (as done in
`DataSourcesApi`), the pattern was refactored into a private extension function on `HttpResponse`.
This is purely local to `ViewsApi` — no codebase-wide change needed.

---

## Test Coverage

### Unit Tests (`ViewsApiTest.kt`)

Tests are organized into `context` blocks per method:

- **Model serialization**: All 6 response types deserialized from fixture JSON and verified field-by-field
- **`retrieve()`**: happy path + 404 ApiError
- **`list()`**: happy path, `database_id` param sent, `data_source_id` param sent, throws when neither provided, 400 ApiError
- **`create()`**: happy path, throws when multiple parent fields, throws when zero parent fields
- **`update()`**: happy path
- **`delete()`**: returns PartialView
- **`createQuery()`**: ViewQuery shape with `expiresAt`, `totalCount`
- **`getQueryResults()`**: ViewQueryResults with pagination fields
- **`deleteQuery()`**: DeletedViewQuery with `deleted: true`
- **DSL builders**: `database()`, `dashboard()`, `createDatabase()`, `createDatabase()` with `afterBlockId`,
  throws when no parent, throws when `dataSourceId` missing, `updateViewRequest` with/without fields,
  `create()` DSL overload, `update()` DSL overload — 9 tests total
- **Error handling**: 403 Forbidden

### Integration Tests (`ViewsIntegrationTest.kt`)

Four test scenarios:

1. **Full workflow** — Creates a database, lists views, retrieves a view, creates a new table view
   via DSL, renames it via DSL, creates a query, retrieves query results, deletes the query, lists
   via Flow (asserts created view appears), deletes the view, cleans up the database.

2. **Property visibility** — Creates a 9-property database (Name, Status, Priority, Due Date,
   Start Date, Completed, Effort, Notes, Contact). Retrieves the data source schema to get stable
   property IDs. Creates three view types — Table, Gallery, List — each with a different selective
   property configuration via `showProperties()`/`hideProperties()`. Retrieves each view to verify
   type/name round-trips and inspect whether `configuration` was returned. Updates the Table view
   to surface an additional column. Confirms all three views appear in `listAsFlow()`. Verified
   working against the live API.

3. **List by data_source_id** — Verifies the `data_source_id` query param works as a filter.

4. **Query pagination** — Creates 3 pages, creates a query with `pageSize=2`, follows pagination
   to collect all results, cleans up.

Integration tests follow the established pattern:
- Tagged `@Tags("Integration", "RequiresApi")`
- Guarded by `integrationTestEnvVarsAreSet()`
- Respect `NOTION_CLEANUP_AFTER_TEST`
- Use `StringSpec` (matching `DataSourcesIntegrationTest`)
- Full diagnostic `println` statements at each step

---

## Build / Test Status

```
./gradlew formatKotlin  → BUILD SUCCESSFUL (no lint issues)
./gradlew test          → BUILD SUCCESSFUL (all unit tests pass, no warnings)
```

Unit test count contribution: ~20 new unit tests in `ViewsApiTest.kt`.

---

## Open Items / Follow-Ups

1. **`ViewConfiguration` sealed class** — implement full Option B (table, board, calendar, etc.)
   once the full helper type shapes are confirmed from live API responses.
2. **`filter`/`sorts` typed models** — consider reusing existing `DataSourceFilter` / sort types
   once it's confirmed the shapes are truly identical.
3. **`ViewQuery.expiresAt` as `Instant`** — parse to a typed datetime using an existing pattern
   in the codebase if datetime handling utilities are added project-wide.
