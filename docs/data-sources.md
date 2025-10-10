# Data Sources API

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

> **📝 Example Validation**: ✅ All examples verified - validated against live Notion API (see `src/test/kotlin/examples/DataSourcesExamples.kt`)

## Overview

**Data sources** are the core concept for working with structured data in the 2025-09-03 Notion API. A data source is essentially a **table** with:
- A schema (properties/columns)
- Rows (pages)
- Query capabilities

**Key Concept**: In the 2025-09-03 API, what you might think of as a "database" is actually a **data source**. Databases are now containers that can hold multiple data sources.

**Official Documentation**: [Notion Data Sources](https://developers.notion.com/reference/retrieve-a-data-source)

## Available Operations

```kotlin
// Retrieve a data source
suspend fun retrieve(dataSourceId: String): DataSource

// Query pages from a data source
suspend fun query(
    dataSourceId: String,
    block: QueryRequestBuilder.() -> Unit = {}
): PaginatedList<Page>

// Create a new data source in a database
suspend fun create(
    databaseId: String,
    block: CreateDataSourceRequestBuilder.() -> Unit
): DataSource

// Update a data source schema
suspend fun update(
    dataSourceId: String,
    block: UpdateDataSourceRequestBuilder.() -> Unit
): DataSource
```

## Examples

### Retrieve a Data Source

```kotlin
val dataSource = notion.dataSources.retrieve("data-source-id")

val name = dataSource.title.firstOrNull()?.plainText ?: "Untitled"
println("Name: $name")
println("Properties:")
dataSource.properties.forEach { (propName, config) ->
    println("  - $propName: ${config.type}")
}
```

### Query Pages from a Data Source

This is one of the most common operations - getting rows (pages) from a table:

```kotlin
// Simple query - get all pages
val allPages = notion.dataSources.query("data-source-id") {}

allPages.forEach { page ->
    println(page.properties["Task Name"])
}
```

#### With Filters and Sorting

```kotlin
val filteredPages = notion.dataSources.query("data-source-id") {
    filter {
        and(
            select("Status").equals("In Progress"),
            number("Priority").greaterThan(5.0),
        )
    }

    sortBy("Due Date", SortDirection.ASCENDING)
    sortBy("Priority", SortDirection.DESCENDING)
}
```

**Note**: The `query()` method automatically handles pagination and returns ALL matching pages.

### Create a Data Source

You can add additional tables (data sources) to an existing database:

```kotlin
val dataSource = notion.dataSources.create {
    databaseId("existing-database-id")
    title("Projects") // Name of the new table

    properties {
        title("Project Name")

        select("Status") {
            option("Planning")
            option("Active")
            option("Completed")
        }

        date("Start Date")
        date("End Date")

        people("Team Members")

        number("Budget")
    }
}

println("Created data source: ${dataSource.id}")
```

### Update Data Source Schema

You can modify the schema (properties) of a data source:

```kotlin
val updated = notion.dataSources.update("data-source-id") {
    // Update the title
    title("Updated Projects")

    // Modify properties
    properties {
        // Re-define existing properties to keep them
        title("Task Name")
        select("Status") {
            option("To Do")
            option("In Progress") // Added
            option("On Hold")     // Added
            option("Done")
        }
        number("Priority")
        date("Due Date")
        checkbox("Completed")

        // Add a new property
        checkbox("Is Critical")
    }
}
```

**Important**: Property updates follow these rules:
- **You must re-define ALL existing properties** you want to keep
- Adding new properties: Include them in the properties block
- Updating existing properties: Re-define them with new configuration
- Removing properties: Simply omit them from the properties block (they will be removed)

## Understanding Data Sources vs. Databases

**In older API versions (pre-2025-09-03)**:
- You would query a "database" to get pages
- You would add pages to a "database"

**In 2025-09-03**:
- **Databases** are containers (like folders)
- **Data sources** are the tables inside those containers
- You query **data sources** to get pages
- You add pages to **data sources** (using `dataSourceId` as parent)

```kotlin
// ❌ This was the old way:
// notion.databases.query("database-id")

// ✅ This is the new way:
notion.dataSources.query("data-source-id")
```

## Common Patterns

### Getting a Data Source ID from a Database

When you create or retrieve a database, you can access its data sources:

```kotlin
// After creating a database
val database = notion.databases.create {
    parent { pageId("parent-page-id") }
    title("My Database")
    // ... properties in initialDataSource
}

// Get the first (and usually only) data source
val dataSourceId = database.dataSources?.firstOrNull()?.id
    ?: error("Database has no data sources")

// Or from an existing database
val existingDb = notion.databases.retrieve("database-id")
val dataSourceId = existingDb.dataSources?.firstOrNull()?.id
```

### Working with Pagination

Data source queries can return large result sets. The library provides three ways to handle pagination:

#### 1. Automatic Collection (Simple)

The `query()` method automatically fetches all matching pages:

```kotlin
// This will fetch ALL pages, even if there are thousands
val allPages = notion.dataSources.query("data-source-id") {
    filter {
        select("Status").equals("To Do")
    }
}

println("Total matching pages: ${allPages.size}")
```

**Use when**: Result sets are reasonably sized (< 1000 items) and you need all results.

#### 2. Flow-Based Streaming (Recommended for Large Sets)

For large result sets, use Flow to process items as they arrive:

```kotlin
// Memory-efficient - processes pages as they're fetched
notion.dataSources.queryAsFlow("data-source-id") {
    filter {
        select("Status").equals("To Do")
    }
}.collect { page ->
    println("Processing: ${page.id}")
    // Process each page individually
}
```

**Use when**: Working with 1000+ items or when memory efficiency matters.

#### 3. Page-Level Flow (Batch Processing)

Access pagination metadata while processing:

```kotlin
// Get complete responses with pagination info
notion.dataSources.queryPagedFlow("data-source-id") {
    filter { /* ... */ }
}.collect { response ->
    println("Fetched ${response.results.size} pages (has more: ${response.hasMore})")
    response.results.forEach { page ->
        // Process pages in this batch
    }
}
```

**Use when**: You need pagination metadata or want to process pages in batches.

See **[Pagination](pagination.md)** for comprehensive guide and best practices.

### Complex Filters

```kotlin
// Combine multiple conditions
val results = notion.dataSources.query("data-source-id") {
    filter {
        or(
            // High priority tasks
            and(
                number("Priority").greaterThanOrEqualTo(8.0),
                select("Status").equals("To Do"),
            ),
            // Overdue tasks
            date("Due Date").before("2025-10-05"),
        )
    }
}
```

### Working with Properties

After querying, access page properties:

```kotlin
val pages = notion.dataSources.query("data-source-id") {}

pages.forEach { page ->
    // Access different property types
    val titleProp = page.properties["Task Name"] as? PageProperty.Title
    val title = titleProp?.plainText

    val selectProp = page.properties["Status"] as? PageProperty.Select
    val status = selectProp?.select?.name

    println("$title - $status")
}
```

### Best Practices

1. **Use filters** - Don't query all pages if you only need some
2. **Limit in application** - Apply limits in your code, not via `pageSize` (which is per-page, not total)
3. **Handle empty results** - Always check if `pages` is empty before processing
4. **Cache data source IDs** - Don't repeatedly fetch database just to get data source ID
5. **Use specific queries** - The more specific your filter, the faster and cheaper the query

## Related APIs

- **[Databases](databases.md)** - Understanding the container that holds data sources
- **[Pages](pages.md)** - Pages are rows in data sources
- **[Search](search.md)** - Search for data sources across workspace
- **[Query DSL](../reference/query-dsl.md)** - Advanced filtering and sorting
