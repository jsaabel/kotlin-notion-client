# Data Sources API

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

> **📝 Example Validation**: ⏳ Pending validation - examples need to be verified against live API

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

println("Name: ${dataSource.name}")
println("Database ID: ${dataSource.databaseId}")
println("Properties:")
dataSource.properties.forEach { (name, config) ->
    println("  - $name: ${config.type}")
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
        and {
            property("Status") {
                select { equals("In Progress") }
            }
            property("Priority") {
                number { greaterThan(5.0) }
            }
        }
    }

    sorts {
        property("Due Date") {
            direction = SortDirection.ASCENDING
        }
        property("Priority") {
            direction = SortDirection.DESCENDING
        }
    }
}
```

**Note**: The `query()` method automatically handles pagination and returns ALL matching pages.

### Create a Data Source

You can add additional tables (data sources) to an existing database:

```kotlin
val dataSource = notion.dataSources.create {
    databaseId("existing-database-id")
    name("Projects") // Name of the new table

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
    // Update the name
    name("Updated Projects")

    // Modify properties
    properties {
        // Add a new property
        checkbox("Is Critical")

        // Update an existing select property
        select("Status") {
            option("Planning")
            option("In Progress") // Added
            option("On Hold")     // Added
            option("Completed")
        }
    }
}
```

**Important**: Property updates follow these rules:
- Adding new properties: They appear in the schema
- Updating existing properties: Define them with new configuration
- Removing properties: Not directly supported - archive pages or create new data source

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

### Automatic Pagination

The `query()` method automatically fetches all matching pages:

```kotlin
// This will fetch ALL pages, even if there are thousands
val allPages = notion.dataSources.query("data-source-id") {
    filter {
        property("Status") {
            select { equals("Active") }
        }
    }
}

println("Total matching pages: ${allPages.size}")
```

The library handles pagination transparently, fetching up to 100,000 records (1,000 pages × 100 records/page safety limit).

### Complex Filters

```kotlin
// Combine multiple conditions
val results = notion.dataSources.query("data-source-id") {
    filter {
        or {
            // High priority tasks
            and {
                property("Priority") {
                    number { greaterThanOrEqual(8.0) }
                }
                property("Status") {
                    select { equals("To Do") }
                }
            }

            // Overdue tasks
            property("Due Date") {
                date { before("2025-10-05") }
            }
        }
    }
}
```

### Working with Properties

After querying, access page properties:

```kotlin
val pages = notion.dataSources.query("data-source-id") {}

pages.forEach { page ->
    // Access different property types
    val titleProp = page.properties["Task Name"] as? TitlePropertyValue
    val title = titleProp?.title?.firstOrNull()?.plainText

    val selectProp = page.properties["Status"] as? SelectPropertyValue
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
