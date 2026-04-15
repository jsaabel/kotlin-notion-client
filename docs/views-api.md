# Views API

Views are display configurations for databases — they control how data is shown (table, board, gallery, calendar, etc.). The Views API lets you create, update, delete, and query views programmatically.

**Entry point**: `client.views`

Views belong to a database (via a data source) or to a dashboard.

## Available Operations

```kotlin
// Retrieve and list
suspend fun retrieve(viewId: String): View
suspend fun list(databaseId: String? = null, dataSourceId: String? = null): ViewList
fun listAsFlow(databaseId: String? = null, dataSourceId: String? = null): Flow<ViewReference>

// Create, update, delete
suspend fun create(block: CreateViewRequestBuilder.() -> Unit): View
suspend fun update(viewId: String, block: UpdateViewRequestBuilder.() -> Unit): View
suspend fun delete(viewId: String): PartialView

// Queries (saved filter/sort snapshots)
suspend fun createQuery(viewId: String): ViewQuery
suspend fun getQueryResults(viewQueryId: String, pageSize: Int? = null, startCursor: String? = null): ViewQueryResults
suspend fun deleteQuery(viewQueryId: String): DeletedViewQuery
```

## Examples

### List Views for a Database

```kotlin
notion.views.listAsFlow(databaseId = "db-id").collect { viewRef ->
    println("${viewRef.name}: ${viewRef.type}")
}

// Or paginated:
val viewList = notion.views.list(databaseId = "db-id")
viewList.views.forEach { viewRef ->
    println("${viewRef.name}: ${viewRef.type}")
}
```

### Retrieve a View

```kotlin
val view = notion.views.retrieve("view-id")
println("View: ${view.name} (${view.type})")
```

### Create a New View

```kotlin
val view = notion.views.create {
    database("data-source-id")  // or dashboard("dashboard-id")
    type("table")
    name("My Table View")
}
```

### Create a View with Property Visibility

```kotlin
val view = notion.views.create {
    database("data-source-id")
    type("table")
    name("Compact Table")
    // Show only specific properties (by property ID):
    showProperties("prop-id-1", "prop-id-2")
    // Or hide specific ones:
    // hideProperties("prop-id-3", "prop-id-4")
}
```

### Create a View with Typed Configuration

Full control via the `ViewConfiguration` sealed class:

```kotlin
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

### Update a View

```kotlin
val updated = notion.views.update("view-id") {
    name("Renamed View")
}
```

### Delete a View

```kotlin
val deleted = notion.views.delete("view-id")
```

## Supported ViewConfiguration Subtypes

All 10 view types have a corresponding typed `ViewConfiguration` subclass:

| Type | Class | Notes |
|------|-------|-------|
| `table` | `ViewConfiguration.Table` | |
| `board` | `ViewConfiguration.Board` | |
| `calendar` | `ViewConfiguration.Calendar` | |
| `timeline` | `ViewConfiguration.Timeline` | |
| `gallery` | `ViewConfiguration.Gallery` | `coverType`, `coverSize`, `coverAspect`, `fitImage` |
| `list` | `ViewConfiguration.List` | |
| `chart` | `ViewConfiguration.Chart` | |
| `map` | `ViewConfiguration.Map` | |
| `form` | `ViewConfiguration.Form` | |
| `dashboard` | `ViewConfiguration.Dashboard` | Read-only from API |

## View Queries

A `ViewQuery` is a saved snapshot of a filtered/sorted view. Use it to share a pre-configured results URL or fetch a paginated list of matching pages.

```kotlin
// Create a query (saves current filter/sort state)
val query = notion.views.createQuery("view-id")

// Fetch results
val results = notion.views.getQueryResults(
    viewQueryId = query.id,
    pageSize = 20,
)
results.pages.forEach { page ->
    println(page.id)
}

// Delete the query when done
notion.views.deleteQuery(query.id)
```

## Notes

- `filter` and `sorts` in views use the same format as `DataSourceFilter` / `DataSourceSort`
- `Dashboard` views are read-only from the API (no `dashboardViewConfigRequest` is supported)
- `showProperties()` / `hideProperties()` throw `IllegalArgumentException` for view types without a `properties` field (`form`, `chart`, `dashboard`)

## Related APIs

- **[Data Sources](data-sources.md)** — Query and filter data within a database
- **[Databases](databases.md)** — The containers that hold views
