# Databases API

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

## Overview

In the 2025-09-03 Notion API, **databases** are **containers** that hold one or more data sources (tables).

**Important Distinction**:
- **DatabasesApi** = Container-level operations (create database, update title/icon/cover, retrieve database info)
- **DataSourcesApi** = Data operations (query pages, manage schema, add/update data)

Think of it like: Database is the folder, Data Sources are the spreadsheets inside that folder.

**Official Documentation**: [Notion Databases API](https://developers.notion.com/reference/retrieve-a-database)

## Available Operations

```kotlin
// Retrieve a database (container)
suspend fun retrieve(databaseId: String): Database

// Create a new database container
suspend fun create(block: CreateDatabaseRequestBuilder.() -> Unit): Database

// Update database container properties
suspend fun update(
    databaseId: String,
    block: UpdateDatabaseRequestBuilder.() -> Unit
): Database
```

**Note**: There is NO `databases.query()` method in the 2025-09-03 API. To query data, use `dataSources.query()`.

## Examples

_TODO: Add comprehensive examples_

### Retrieve a Database

```kotlin
val database = notion.databases.retrieve("database-id")
println("Title: ${database.title}")
println("Data sources: ${database.dataSources?.size ?: 0}")

// Access the data sources within this database
database.dataSources?.forEach { dataSourceRef ->
    println("Data source ID: ${dataSourceRef.id}")
}
```

### Create a Database Container

When you create a database, you typically create it with an initial data source:

```kotlin
val database = notion.databases.create {
    parent { pageId("parent-page-id") }

    title {
        text("Project Tracker")
    }

    // The initial data source (table) with its schema
    initialDataSource {
        name = "Main Table"

        properties {
            title("Task")
            select("Priority") {
                option("High", Color.RED)
                option("Medium", Color.YELLOW)
                option("Low", Color.GREEN)
            }
            people("Assignee")
            date("Due Date")
        }
    }
}

// The database is created with one data source
val firstDataSourceId = database.dataSources?.firstOrNull()?.id
```

### Update Database Container

You can update container-level properties like title, icon, and cover:

```kotlin
// TODO: Add example
```

## What Operations Go Where?

### Use DatabasesApi for:
- ✅ Creating a new database (with initial data source)
- ✅ Updating database title, icon, cover
- ✅ Retrieving database metadata and list of data sources

### Use DataSourcesApi for:
- ✅ Querying pages (rows)
- ✅ Creating additional data sources in a database
- ✅ Updating data source schema (adding/modifying properties)
- ✅ Retrieving data source details

### Use PagesApi for:
- ✅ Creating pages (rows) in a data source
- ✅ Updating page properties
- ✅ Archiving pages

## Migration from Older APIs

If you're familiar with pre-2025-09-03 APIs:

| Old API (2022-06-28) | New API (2025-09-03) |
|---------------------|---------------------|
| `databases.query(databaseId)` | `dataSources.query(dataSourceId)` |
| `databases.create` with properties | `databases.create` with `initialDataSource` |
| `databases.update` to change schema | `dataSources.update` to change schema |
| `parent { database_id = "..." }` | `parent { dataSourceId("...") }` |

## Common Patterns

_TODO: Add tips, gotchas, best practices_

## Related APIs

- **[Data Sources](data-sources.md)** - The tables within database containers
- **[Pages](pages.md)** - Rows within data sources
- **[Search](search.md)** - Search for databases and data sources
