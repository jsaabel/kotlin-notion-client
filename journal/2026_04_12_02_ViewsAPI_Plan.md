# Development Journal - April 12, 2026

## Views API Implementation Plan (Phase 3)

This journal was written as a dense implementation reference to use when resuming work in a future session.
All information was extracted directly from the fetched API docs in `docs/views-api/`.

---

## Endpoints (8 total)

| Method | Path | Operation |
|---|---|---|
| `POST` | `/v1/views` | Create a view |
| `GET` | `/v1/views/{view_id}` | Retrieve a view |
| `PATCH` | `/v1/views/{view_id}` | Update a view |
| `DELETE` | `/v1/views/{view_id}` | Delete a view |
| `GET` | `/v1/views` | List views (paginated) |
| `POST` | `/v1/views/{view_id}/queries` | Create a view query (executes + caches) |
| `GET` | `/v1/views/{view_id}/queries/{query_id}` | Get cached query results |
| `DELETE` | `/v1/views/{view_id}/queries/{query_id}` | Delete cached query (idempotent) |

---

## Response Shapes

### `View` (full — `dataSourceViewObjectResponse`)
Required fields: `object`, `id`, `parent`, `name`, `type`, `created_time`, `last_edited_time`, `url`

```
object:           String        // always "view"
id:               String        // UUID
parent:           ViewParent    // { type: "database_id", database_id: UUID }
name:             String
type:             ViewType      // see enum below
created_time:     String        // ISO date-time
last_edited_time: String        // ISO date-time
url:              String        // canonical Notion deep link
data_source_id:   String?       // null for dashboard views
created_by:       PartialUser?
last_edited_by:   PartialUser?
filter:           (opaque)?     // same shape as DataSource query filter
sorts:            List<(opaque)>? // same shape as DataSource query sorts
quick_filters:    Map<String,(opaque)>? // key=property name/ID, value=filter condition w/o "property" field
configuration:    ViewConfiguration?
dashboard_view_id: String?      // only present when this is a widget inside a dashboard
```

### `PartialView` (minimal — `partialDataSourceViewObjectResponse`)
Returned by `DELETE /v1/views/{view_id}`. Required: `object`, `id`, `parent`, `type`.

### `ViewReference` (list result — `dataSourceViewReferenceResponse`)
Returned by `GET /v1/views`. Just `{ object: "view", id }`. No parent, no type.

### `ViewQuery` (create query response)
```
object:      String        // "view_query"
id:          String        // query ID (use for pagination + deletion)
view_id:     String
expires_at:  String        // ISO date-time; cache lives 15 minutes
total_count: Int           // total pages in the full result set
results:     List<{ object: String, id: String }>   // first page of page references
next_cursor: String?
has_more:    Boolean
```

### `ViewQueryResults` (GET cached results)
```
object:      String   // "list"
results:     List<{ object: "page", id: String }>
next_cursor: String?
has_more:    Boolean
```
Note: `type: "page"` and `page: {}` also appear in the JSON — ignored via `ignoreUnknownKeys`.

### `DeletedViewQuery`
```
object:  String   // "view_query"
id:      String
deleted: Boolean
```

---

## View Types (enum)

10 values: `table`, `board`, `list`, `calendar`, `timeline`, `gallery`, `form`, `chart`, `map`, `dashboard`

```kotlin
enum class ViewType {
    @SerialName("table") TABLE,
    @SerialName("board") BOARD,
    @SerialName("list") LIST,
    @SerialName("calendar") CALENDAR,
    @SerialName("timeline") TIMELINE,
    @SerialName("gallery") GALLERY,
    @SerialName("form") FORM,
    @SerialName("chart") CHART,
    @SerialName("map") MAP,
    @SerialName("dashboard") DASHBOARD,
}
```

---

## Request Shapes

### `CreateViewRequest` — `POST /v1/views`
Required: `data_source_id`, `name`, `type`. Exactly one of `database_id`, `view_id`, `create_database` must be provided.

```kotlin
@Serializable
data class CreateViewRequest(
    @SerialName("data_source_id") val dataSourceId: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: ViewType,
    @SerialName("database_id") val databaseId: String? = null,
    @SerialName("view_id") val viewId: String? = null,
    @SerialName("create_database") val createDatabase: CreateDatabaseForView? = null,
    @SerialName("filter") val filter: JsonObject? = null,
    @SerialName("sorts") val sorts: JsonArray? = null,
    @SerialName("quick_filters") val quickFilters: Map<String, JsonObject>? = null,
    @SerialName("configuration") val configuration: ViewConfiguration? = null,
    @SerialName("position") val position: ViewPosition? = null,    // tab bar position; only for database_id
    @SerialName("placement") val placement: WidgetPlacement? = null, // only for view_id (dashboard widget)
)
```

Validate in `ViewsApi.create()`: exactly one of `databaseId`, `viewId`, `createDatabase` is non-null.

### `UpdateViewRequest` — `PATCH /v1/views/{view_id}`
All fields optional. Pass `null` to clear a field. Unmentioned quick filters preserved.

```kotlin
@Serializable
data class UpdateViewRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("filter") val filter: JsonObject? = null,
    @SerialName("sorts") val sorts: JsonArray? = null,
    @SerialName("quick_filters") val quickFilters: Map<String, JsonObject?>? = null,
    @SerialName("configuration") val configuration: ViewConfiguration? = null,
)
```

### `ViewPosition` — tab bar placement when creating via `database_id`

```kotlin
@Serializable
sealed class ViewPosition {
    @Serializable @SerialName("start")
    object Start : ViewPosition()

    @Serializable @SerialName("end")
    object End : ViewPosition()

    @Serializable @SerialName("after_view")
    data class AfterView(@SerialName("view_id") val viewId: String) : ViewPosition()
}
```

JSON: `{ "type": "start" }` / `{ "type": "end" }` / `{ "type": "after_view", "view_id": "..." }`

### `WidgetPlacement` — dashboard widget placement when creating via `view_id`

```kotlin
@Serializable
sealed class WidgetPlacement {
    @Serializable @SerialName("new_row")
    data class NewRow(@SerialName("row_index") val rowIndex: Int? = null) : WidgetPlacement()

    @Serializable @SerialName("existing_row")
    data class ExistingRow(@SerialName("row_index") val rowIndex: Int) : WidgetPlacement()
}
```

### `CreateDatabaseForView` — creates a new linked database on a page

```kotlin
@Serializable
data class CreateDatabaseForView(
    @SerialName("parent") val parent: PageIdParent,            // { type: "page_id", page_id }
    @SerialName("position") val position: AfterBlockPosition? = null,  // { type: "after_block", block_id }
)
```

`PageIdParent` and `AfterBlockPosition` may already exist in the codebase — check before creating new ones.

### `CreateViewQueryRequest` — `POST /v1/views/{view_id}/queries`

```kotlin
@Serializable
data class CreateViewQueryRequest(
    @SerialName("page_size") val pageSize: Int? = null,
)
```

---

## View Configuration (`ViewConfiguration`)

A sealed class discriminated by `type`. Used in both request and response (same field names for both).

**Serialization approach:** Use a custom serializer (same pattern as `Block`, `DatabaseProperty`).
The discriminator field is `"type"`.

### Subtypes and their fields

#### `Table` (`type: "table"`)
```
properties:          List<ViewPropertyConfig>?
group_by:            GroupByConfig?
subtasks:            SubtaskConfig?
wrap_cells:          Boolean?
frozen_column_index: Int?
show_vertical_lines: Boolean?
```

#### `Board` (`type: "board"`)
```
group_by:    GroupByConfig      (required on create)
sub_group_by: GroupByConfig?
properties:  List<ViewPropertyConfig>?
cover:       CoverConfig?
cover_size:  "small"|"medium"|"large" (nullable)
cover_aspect: "contain"|"cover" (nullable)
card_layout: "list"|"compact" (nullable)
```

#### `Calendar` (`type: "calendar"`)
```
date_property_id: String       (required on create)
properties:       List<ViewPropertyConfig>?
view_range:       "week"|"month" (nullable)
show_weekends:    Boolean?
```

#### `Timeline` (`type: "timeline"`)
```
date_property_id:     String   (required on create)
end_date_property_id: String?
properties:           List<ViewPropertyConfig>?
show_table:           Boolean?
table_properties:     List<ViewPropertyConfig>?
preference:           TimelinePreference?
arrows_by:            TimelineArrowsBy?
color_by:             Boolean?
```

#### `Gallery` (`type: "gallery"`)
```
properties:  List<ViewPropertyConfig>?
cover:       CoverConfig?
cover_size:  "small"|"medium"|"large" (nullable)
cover_aspect: "contain"|"cover" (nullable)
card_layout: "list"|"compact" (nullable)
```

#### `List` (`type: "list"`)
```
properties: List<ViewPropertyConfig>?
```

#### `Map` (`type: "map"`)
```
height:     "small"|"medium"|"large"|"extra_large" (nullable)
map_by:     String?          // property ID of location property
properties: List<ViewPropertyConfig>?
```

#### `Form` (`type: "form"`)
```
is_form_closed:         Boolean?
anonymous_submissions:  Boolean?
submission_permissions: "none"|"comment_only"|"reader"|"read_and_write"|"editor" (nullable)
```

#### `Chart` (`type: "chart"`)
```
chart_type:      "column"|"bar"|"line"|"donut"|"number"   (required)
x_axis:          GroupByConfig?
y_axis:          ChartAggregation?
x_axis_property_id: String?
y_axis_property_id: String?
value:           ChartAggregation?     // for number charts
sort:            "manual"|"x_ascending"|"x_descending"|"y_ascending"|"y_descending" (nullable)
color_theme:     "gray"|"blue"|"yellow"|"green"|"purple"|"teal"|"orange"|"pink"|"red"|"auto"|"colorful" (nullable)
height:          "small"|"medium"|"large"|"extra_large" (nullable)
hide_empty_groups: Boolean?
legend_position: "off"|"bottom"|"side" (nullable)
show_data_labels: Boolean?
axis_labels:     "none"|"x_axis"|"y_axis"|"both" (nullable)
grid_lines:      "none"|"horizontal"|"vertical"|"both" (nullable)
cumulative:      Boolean?           // line only
smooth_line:     Boolean?           // line only
hide_line_fill_area: Boolean?       // line only
group_style:     "normal"|"percent"|"side_by_side" (nullable)
y_axis_min:      Double?
y_axis_max:      Double?
donut_labels:    "none"|"value"|"name"|"name_and_value" (nullable)
hide_title:      Boolean?           // number only
stack_by:        GroupByConfig?
reference_lines: List<ChartReferenceLine>?
caption:         String?
color_by_value:  Boolean?
```

#### `Dashboard` (`type: "dashboard"`)
Shape only appears in response (`dashboardViewConfigResponse`) — omitted from create request options.
For v0.4, represent as an opaque type or a minimal data class with just `type`.

### Helper types (used by config subtypes)

**`ViewPropertyConfig`** — controls visibility and display of a property on a view:
```
// Shape not fully spelled out in docs — likely { property_id, visible, width?, etc. }
// Use JsonObject for now or read more carefully from existing DataSource ViewProperty models
```

**`GroupByConfig`** — group-by configuration:
```
// Used by table, board, timeline, chart; shape not expanded in docs
// Likely { property_id, ... } — check DataSource API models for existing GroupBy types
```

**`CoverConfig`**, **`SubtaskConfig`**, **`TimelinePreference`**, **`TimelineArrowsBy`**, **`ChartAggregation`**, **`ChartReferenceLine`** — shapes not expanded in fetched docs. Either:
- Implement as `JsonObject` initially
- Or fetch the View object reference doc (`/reference/view`) for full shapes

### Serialization complexity

The `ViewConfiguration` sealed class has 9+ subtypes each with many nullable fields and nested helper types. Recommended approach:

**Option A (pragmatic for v0.4):** Represent `configuration` as `JsonObject?` in `View` and request classes. This means callers can pass raw JSON or use a DSL helper. No custom serializer needed. Downside: not type-safe.

**Option B (ideal):** Full sealed class with custom serializer matching `Block` pattern. Significant but well-understood work (~200 lines). Enables type-safe configuration reading.

**Recommendation:** Start with Option A to get the API live, add Option B as a follow-up. The existing filter/sort fields are already `JsonObject?` so this is consistent.

---

## Sort shape (used in filter/sorts fields)

Sorts returned from the API are one of:
- Property sort: `{ "property": "Name", "direction": "ascending"|"descending" }`
- Timestamp sort: `{ "timestamp": "created_time"|"last_edited_time", "direction": "ascending"|"descending" }`

These are the same as data source query sorts. Check if `DataSourceSort` or similar already exists and reuse it.

---

## Kotlin Model File Plan

### New files

| File | Contents |
|---|---|
| `models/views/View.kt` | `View`, `PartialView`, `ViewReference`, `ViewParent`, `ViewType`, `ViewList`, `ViewQuery`, `ViewQueryResults`, `DeletedViewQuery`, `ViewQueryPageReference` |
| `models/views/ViewRequests.kt` | `CreateViewRequest`, `UpdateViewRequest`, `CreateViewQueryRequest`, `ViewPosition`, `WidgetPlacement`, `CreateDatabaseForView` |
| `models/views/ViewConfiguration.kt` | `ViewConfiguration` sealed class (or `JsonObject` alias if Option A) |
| `api/ViewsApi.kt` | All 8 endpoint methods |

### Modified files

| File | Change |
|---|---|
| `NotionClient.kt` | Add `val views = ViewsApi(httpClient, config)` |

---

## `ViewsApi` Method Signatures

```kotlin
class ViewsApi(private val httpClient: HttpClient, private val config: NotionConfig) {

    // View CRUD
    suspend fun create(request: CreateViewRequest): View
    suspend fun retrieve(viewId: String): View
    suspend fun update(viewId: String, request: UpdateViewRequest): View
    suspend fun delete(viewId: String): PartialView

    // Listing
    suspend fun list(
        databaseId: String? = null,
        dataSourceId: String? = null,
        startCursor: String? = null,
        pageSize: Int? = null,
    ): ViewList   // at least one of databaseId/dataSourceId required

    fun listAsFlow(databaseId: String? = null, dataSourceId: String? = null): Flow<ViewReference>
    fun listPagedFlow(databaseId: String? = null, dataSourceId: String? = null): Flow<ViewList>

    // Queries (cached execution)
    suspend fun createQuery(viewId: String, pageSize: Int? = null): ViewQuery
    suspend fun getQueryResults(
        viewId: String,
        queryId: String,
        startCursor: String? = null,
        pageSize: Int? = null,
    ): ViewQueryResults
    suspend fun deleteQuery(viewId: String, queryId: String): DeletedViewQuery
}
```

### URL mapping

```
create             POST   /v1/views                                     body: CreateViewRequest
retrieve           GET    /v1/views/{viewId}
update             PATCH  /v1/views/{viewId}                            body: UpdateViewRequest
delete             DELETE /v1/views/{viewId}
list               GET    /v1/views?database_id=...&data_source_id=...&start_cursor=...&page_size=...
createQuery        POST   /v1/views/{viewId}/queries                    body: { page_size }
getQueryResults    GET    /v1/views/{viewId}/queries/{queryId}?start_cursor=...&page_size=...
deleteQuery        DELETE /v1/views/{viewId}/queries/{queryId}
```

### Validations

- `create()`: exactly one of `request.databaseId`, `request.viewId`, `request.createDatabase` must be non-null
- `list()`: at least one of `databaseId`, `dataSourceId` must be non-null
- `pageSize` (all endpoints): 1..100

---

## Test Plan

### Unit tests (`unit/api/ViewsApiTest.kt`)

- `retrieve()` deserializes full `View` from fixture
- `list()` deserializes `ViewList` with pagination fields
- `list()` sends `database_id` query param when provided
- `list()` sends `data_source_id` query param when provided
- `list()` throws when neither `database_id` nor `data_source_id` provided
- `create()` serializes `CreateViewRequest` body correctly
- `create()` throws when multiple of `database_id`/`view_id`/`create_database` provided
- `delete()` returns `PartialView`
- `createQuery()` deserializes `ViewQuery` with `expires_at`, `total_count`
- `getQueryResults()` deserializes `ViewQueryResults`
- `deleteQuery()` returns `DeletedViewQuery` with `deleted: true`
- Error responses (404, 400)

### Sample fixtures needed

Create under `src/test/resources/api/views/`:

- `retrieve_view.json` — a full `dataSourceViewObjectResponse` (table view with filter, sorts)
- `list_views.json` — a `ViewList` with 2 `ViewReference` items, `has_more: false`
- `create_view.json` — same as retrieve fixture (response is same shape)
- `partial_view.json` — `partialDataSourceViewObjectResponse` (for delete response)
- `create_view_query.json` — a `viewQueryResponse` with 1 result
- `view_query_results.json` — a paginated list of page references
- `deleted_view_query.json` — `{ object: "view_query", id: "...", deleted: true }`

Minimal retrieve fixture:
```json
{
  "object": "view",
  "id": "a3f1b2c4-5678-4def-abcd-1234567890ab",
  "parent": { "type": "database_id", "database_id": "248104cd-477e-80fd-b757-e945d38000bd" },
  "name": "All tasks",
  "type": "table",
  "created_time": "2026-03-01T12:00:00.000Z",
  "last_edited_time": "2026-04-01T09:00:00.000Z",
  "url": "https://www.notion.so/...",
  "data_source_id": "248104cd-477e-80af-bc30-000bd28de8f9",
  "created_by": null,
  "last_edited_by": null,
  "filter": null,
  "sorts": null,
  "quick_filters": null,
  "configuration": null
}
```

### Integration tests (`integration/ViewsIntegrationTest.kt`)

Scope: one container page → one database → tests create/retrieve/update/delete/list/query on that database.

- `list()` returns at least one view for the test database
- `retrieve()` retrieves a view by ID
- `create()` creates a new table view and returns it
- `update()` renames the view
- `delete()` removes the view (if not last)
- `createQuery()` executes a query, returns results with `expires_at`
- `getQueryResults()` paginates through cached results
- `deleteQuery()` deletes the cached query

---

## Implementation Order

1. `ViewType` enum + `ViewParent` data class
2. `View`, `PartialView`, `ViewReference`, `ViewList` (read models)
3. `ViewQuery`, `ViewQueryResults`, `DeletedViewQuery`
4. `ViewPosition`, `WidgetPlacement`, `CreateDatabaseForView` (aux request types)
5. `CreateViewRequest`, `UpdateViewRequest`, `CreateViewQueryRequest`
6. (Optional) `ViewConfiguration` sealed class — or keep as `JsonObject?`
7. `ViewsApi` — all 8 methods
8. Register on `NotionClient`
9. Sample fixtures
10. `TestFixtures.Views` helper object
11. Unit tests
12. `./gradlew formatKotlin && ./gradlew test`
13. Integration tests

---

## Open Questions / Caveats

1. **`ViewPropertyConfig`, `GroupByConfig`, etc.** — shapes not fully documented in the fetched docs. Before implementing sealed config types, fetch `/reference/view` from the Notion docs for the full field list.

2. **Reuse opportunity**: Check if `DataSource`/`DataSourcesApi` already defines filter/sort types that can be reused by `View`. The docs say the filter shape is "the same format as data source query filter."

3. **`configuration` on response**: The API docs say this field can be null even for views that visually have configuration (Notion's UI stores defaults implicitly). Don't assume it's always populated.

4. **`list()` returns `ViewReference` (just id+object)**, not `View`. If callers want full details they must call `retrieve()` for each. This is by design (API paginates references, not full objects).

5. **Query cache expiry**: `ViewQuery.expiresAt` is a datetime string. Consider parsing to `Instant`/`LocalDateTime` using our existing datetime handling patterns.

6. **Dashboard views**: `create()` with `view_id` creates a widget inside a dashboard view — a different use case than creating a regular database view. The same endpoint handles both via the mutually-exclusive parent fields.