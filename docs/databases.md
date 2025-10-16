# Databases API

> **📝 Example Validation**: ✅ All examples verified - validated against live Notion API (see `src/test/kotlin/examples/DatabasesExamples.kt`)

## Overview

In the 2025-09-03 Notion API, **databases** are **containers** that hold one or more data sources (tables).

**Think of it like this**:
- **Database** = Folder/Container
- **Data Source** = Spreadsheet/Table inside that folder
- **Pages** = Rows in the spreadsheet

**Official Documentation**: [Notion Databases API](https://developers.notion.com/reference/retrieve-a-database)

## Key Distinction

This is the most important concept to understand:

| API | Purpose | What it does |
|-----|---------|--------------|
| **DatabasesApi** | Container management | Create database containers, retrieve metadata, archive |
| **DataSourcesApi** | Data & schema | Query pages, update schema (add/modify properties), create additional tables |
| **PagesApi** | Row operations | Create/update/retrieve individual rows (pages) |

## Available Operations

```kotlin
// Retrieve a database container
suspend fun retrieve(databaseId: String): Database

// Create a new database with initial data source
suspend fun create(block: DatabaseRequestBuilder.() -> Unit): Database

// Archive a database
suspend fun archive(databaseId: String): Database
```

**Note**: There is NO `databases.update()` or `databases.query()` in the 2025-09-03 API.
- To update schema → use `dataSources.update()`
- To query data → use `dataSources.query()`

## Examples

### Retrieve a Database

```kotlin
val database = notion.databases.retrieve("database-id")

// Access container properties
val title = database.title.firstOrNull()?.plainText ?: "Untitled"
println("Database: $title")
println("Created: ${database.createdTime}")
println("Archived: ${database.archived}")

// Get data sources within this database
database.dataSources.forEach { dataSourceRef ->
    println("Data source ID: ${dataSourceRef.id}")
}

// Typically you'll want the first data source ID
val dataSourceId = database.dataSources.firstOrNull()?.id
```

### Create a Simple Database

```kotlin
val database = notion.databases.create {
    // Specify parent page
    parent.page("parent-page-id")

    // Set database title
    title("Project Tracker")

    // Define initial schema (the first table/data source)
    properties {
        title("Task Name")
        select("Status") {
            option("To Do")
            option("In Progress")
            option("Done")
        }
        people("Assignee")
        date("Due Date")
        checkbox("Completed")
    }
}

println("Created database: ${database.id}")

// Get the data source ID to start adding pages
val dataSourceId = database.dataSources.firstOrNull()?.id
```

### Create a Database with Rich Schema

```kotlin
val database = notion.databases.create {
    parent.page("parent-page-id")

    title("Product Roadmap")
    description("Track features and releases")

    properties {
        // Title property (required - usually first)
        title("Feature Name")

        // Text properties
        richText("Description")

        // Number properties
        number("Story Points")
        number("Budget", format = "dollar")

        // Selection properties
        select("Priority") {
            option("Critical", SelectOptionColor.RED)
            option("High", SelectOptionColor.ORANGE)
            option("Medium", SelectOptionColor.YELLOW)
            option("Low", SelectOptionColor.GREEN)
        }

        multiSelect("Tags") {
            option("Frontend", SelectOptionColor.BLUE)
            option("Backend", SelectOptionColor.PURPLE)
            option("Infrastructure", SelectOptionColor.GRAY)
        }

        // Date properties
        date("Target Date")
        date("Completed Date")

        // People properties
        people("Owner")
        people("Team Members")

        // Other properties
        checkbox("Is Launched")
        url("Documentation Link")
        email("Contact")
    }

    // Optional: Add icon and cover
    icon.emoji("🗺️")
    cover.external("https://images.unsplash.com/photo-1557683316-973673baf926")
}
```

### Create a Database with Relation

```kotlin
// First, create a Projects database
val projectsDb = notion.databases.create {
    parent.page("parent-page-id")
    title("Projects")
    properties {
        title("Project Name")
        select("Status") {
            option("Active")
            option("Completed")
        }
    }
}

// Get the data source ID from the projects database (needed for relation)
val projectsDataSourceId = projectsDb.dataSources.first().id

// Then create a Tasks database with a relation to Projects
val tasksDb = notion.databases.create {
    parent.page("parent-page-id")
    title("Tasks")
    properties {
        title("Task Name")

        // Relation to the Projects database (requires both database and data source IDs)
        relation("Project", projectsDb.id, projectsDataSourceId) {
            dual("Related Tasks") // Creates bidirectional relation
        }

        select("Status") {
            option("To Do")
            option("Done")
        }
    }
}
```

### Archive a Database

```kotlin
val archived = notion.databases.archive("database-id")

println("Database in trash: ${archived.inTrash}")
```

**Note**: In the 2025-09-03 API, databases use the `in_trash` field (not `archived`). Databases in trash are hidden from the UI but remain accessible via the API. Notion doesn't support true deletion.

## Common Patterns

### Get Data Source ID from Created Database

Every database is created with an initial data source. Here's how to get its ID:

```kotlin
val database = notion.databases.create {
    parent.page("parent-page-id")
    title("My Database")
    properties {
        title("Name")
    }
}

// Get the first (and usually only) data source
val dataSourceId = database.dataSources.firstOrNull()?.id
    ?: error("Database has no data sources")

// Now you can query it or create pages in it
val pages = notion.dataSources.query(dataSourceId) {}
```

### Create Database and Add Initial Pages

```kotlin
// Step 1: Create database
val database = notion.databases.create {
    parent.page("parent-page-id")
    title("Team Tasks")
    properties {
        title("Task")
        select("Status") {
            option("To Do")
            option("Done")
        }
    }
}

val dataSourceId = database.dataSources.first().id

// Step 2: Add some initial pages
val tasks = listOf("Setup project", "Write docs", "Deploy")
tasks.forEach { taskName ->
    notion.pages.create {
        parent.dataSource(dataSourceId)
        properties {
            title("Task", taskName)
            select("Status", "To Do")
        }
    }
}
```

### Check If Database is in Trash

```kotlin
val database = notion.databases.retrieve("database-id")

if (database.inTrash) {
    println("This database is in trash")
} else {
    println("Database is active")
}
```

## Property Type Reference

When defining the initial schema in `properties { }`, you can use:

| Property Type | Method | Example |
|--------------|--------|---------|
| Title | `title(name)` | `title("Name")` |
| Rich Text | `richText(name)` | `richText("Description")` |
| Number | `number(name, format)` | `number("Score")` or `number("Price", "dollar")` |
| Select | `select(name) { }` | See examples above |
| Multi-select | `multiSelect(name) { }` | See examples above |
| Date | `date(name)` | `date("Due Date")` |
| People | `people(name)` | `people("Assignee")` |
| Checkbox | `checkbox(name)` | `checkbox("Is Complete")` |
| URL | `url(name)` | `url("Link")` |
| Email | `email(name)` | `email("Contact")` |
| Phone | `phoneNumber(name)` | `phoneNumber("Phone")` |
| Relation | `relation(name, targetDbId) { }` | See relation example above |

**Note**: Formula and Rollup properties cannot be created via the API - they must be added through the Notion UI.

## Number Formats

For `number()` properties, you can specify various formats:

```kotlin
number("Amount", format = "dollar")         // $1,234.56
number("Percentage", format = "percent")     // 12.34%
number("Count", format = "number")           // 1234 (default)
number("Count", format = "number_with_commas") // 1,234
```

Supported formats: `"number"`, `"number_with_commas"`, `"percent"`, `"dollar"`, `"euro"`, `"pound"`, `"yen"`, and many other currencies.

## What You CANNOT Do with DatabasesApi

❌ **Update database schema** - Use `dataSources.update()` instead
❌ **Query pages** - Use `dataSources.query()` instead
❌ **Add properties** - Use `dataSources.update()` instead
❌ **Remove properties** - Use `dataSources.update()` instead
❌ **Create pages** - Use `pages.create()` instead

## Best Practices

1. **Always get data source ID** - After creating a database, extract the data source ID for future operations
2. **Use meaningful names** - Choose clear property names that describe the data
3. **Define title property first** - Databases must have at least one property, typically a title
4. **Pre-plan your schema** - Think through your data model before creating the database
5. **Use appropriate property types** - Match Notion property types to your data (e.g., `select` for status, `people` for assignments)
6. **Consider relations early** - If you need to link databases, plan the relation structure upfront
7. **Archive instead of delete** - Notion doesn't support deletion, use `archive()` instead

## Gotchas and Tips

### ❌ Common Mistake: Trying to Query a Database

```kotlin
// ❌ Wrong - this method doesn't exist in 2025-09-03
notion.databases.query("database-id")

// ✅ Correct - query the data source instead
val database = notion.databases.retrieve("database-id")
val dataSourceId = database.dataSources.first().id
notion.dataSources.query(dataSourceId) {}
```

### ❌ Common Mistake: Missing Data Source ID

```kotlin
// ❌ Wrong - can't use database ID directly for pages
notion.pages.create {
    parent.database("database-id")  // This won't work!
    // ...
}

// ✅ Correct - get data source ID first
val db = notion.databases.retrieve("database-id")
val dataSourceId = db.dataSources.first().id
notion.pages.create {
    parent.dataSource(dataSourceId)
    // ...
}
```

### Relation Configuration Types

When creating relations, you have three options:

```kotlin
// Important: Relations require BOTH database ID and data source ID
val targetDataSourceId = targetDb.dataSources.first().id

// Single (one-way) relation
relation("Project", targetDb.id, targetDataSourceId) {
    single()
}

// Dual (two-way) relation - creates backlink property
relation("Project", targetDb.id, targetDataSourceId) {
    dual("Related Tasks")  // Creates "Related Tasks" in target DB
}

// Synced (legacy format)
relation("Project", targetDb.id, targetDataSourceId) {
    synced("Related Items")
}
```

### Parent Types

Databases can have different parent types:

```kotlin
// Child of a page (most common)
parent.page("page-id")

// Child of a block
parent.block("block-id")

// At workspace root (requires admin permissions)
parent.workspace()
```

## Related APIs

- **[Data Sources](data-sources.md)** - The tables within database containers (query, update schema)
- **[Pages](pages.md)** - Rows within data sources (create, update, retrieve)
- **[Search](search.md)** - Search for databases and data sources across workspace
