# Quick Start Guide

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

Get started with the Kotlin Notion Client in under 5 minutes.

## Prerequisites

1. **Notion Integration**: Create an integration at [notion.so/my-integrations](https://www.notion.so/my-integrations)
2. **API Token**: Copy your integration's API token (starts with `secret_`)
3. **Share Content**: Share a page or database with your integration
4. **Kotlin Project**: Kotlin 2.2+ with JVM target 17+

## Installation

> **Note**: Not yet published to Maven Central. See [Building from Source](README.md#building-from-source)

```kotlin
dependencies {
    implementation("it.saabel:kotlin-notion-client:0.1.0")
}
```

## Your First API Call

```kotlin
import it.saabel.kotlinnotionclient.NotionClient

fun main() = runBlocking {
    // Initialize the client
    val notion = NotionClient("your-api-token")

    // Get current user (verifies token works)
    val user = notion.users.getCurrentUser()
    println("Connected as: ${user.name}")

    // Don't forget to close when done
    notion.close()
}
```

### Client Initialization

You can create a `NotionClient` instance using either pattern:

```kotlin
// 1. Direct constructor (recommended - idiomatic Kotlin)
val notion = NotionClient("your-api-token")

// 2. Factory method (also supported)
val notion = NotionClient.create("your-api-token")

// With custom configuration
val notion = NotionClient(
    NotionConfig(
        apiToken = "your-api-token",
        logLevel = LogLevel.INFO
    )
)
```

## Understanding the 2025-09-03 API

**Important**: Before going further, understand this key concept:

- **Database** = Container (like a folder)
- **Data Source** = Table with properties and rows (the actual data)

In older Notion APIs, "database" meant the table. In 2025-09-03, databases *contain* data sources.

Most operations work on **data sources**, not databases:

```kotlin
// ❌ Old API: notion.databases.query("database-id")
// ✅ New API: notion.dataSources.query("data-source-id")
```

## Common Operations

### Retrieve a Page

```kotlin
val page = notion.pages.retrieve("page-id")
println("Page title: ${page.properties["title"]}")
println("Created: ${page.createdTime}")
println("URL: ${page.url}")
```

### Query a Data Source (Table)

```kotlin
// Get all rows from a table
val pages = notion.dataSources.query("data-source-id")

pages.results.forEach { page ->
    println(page.properties["Name"])
}
```

### Create a Page in a Data Source

```kotlin
val newPage = notion.pages.create {
    // Parent is the data source (table), not the database
    parent { dataSourceId("data-source-id") }

    properties {
        title("Name") {
            text("My New Task")
        }
        select("Status") {
            name("In Progress")
        }
        date("Due Date") {
            start("2025-10-15")
        }
    }
}

println("Created page: ${newPage.url}")
```

### Query with Filters

```kotlin
val urgentTasks = notion.dataSources.query("data-source-id") {
    filter {
        and {
            property("Status") {
                select { equals("In Progress") }
            }
            property("Priority") {
                select { equals("High") }
            }
        }
    }

    sorts {
        property("Due Date") {
            direction = SortDirection.ASCENDING
        }
    }
}
```

### Add Content to a Page

```kotlin
notion.blocks.append("page-id") {
    heading1 {
        richText {
            text("Project Overview")
        }
    }

    paragraph {
        richText {
            text("This is a ")
            text("bold") { bold = true }
            text(" statement.")
        }
    }

    bulletedListItem {
        richText {
            text("First key point")
        }
    }
}
```

### Search Your Workspace

```kotlin
// Search for data sources (tables)
val results = notion.search.search {
    query = "tasks"
    filter {
        value = "data_source"  // Note: "data_source", not "database"
        property = "object"
    }
}
```

## Error Handling

```kotlin
try {
    val page = notion.pages.retrieve("page-id")
} catch (e: NotionError.ObjectNotFound) {
    println("Page not found")
} catch (e: NotionError.Unauthorized) {
    println("Invalid API token")
} catch (e: NotionError.RateLimited) {
    println("Rate limited, retry after: ${e.retryAfter}")
}
```

## Getting IDs

### Page ID

From a Notion page URL:
- URL: `https://notion.so/My-Page-12345678123456781234567812345678`
- ID: `12345678-1234-5678-1234-567812345678` (add hyphens)

### Data Source ID

```kotlin
// Option 1: Get it from a database
val database = notion.databases.retrieve("database-id")
val firstDataSource = database.dataSources?.firstOrNull()
val dataSourceId = firstDataSource?.id

// Option 2: Use search to find data sources
val results = notion.search.search {
    filter {
        value = "data_source"
        property = "object"
    }
}
```

## Next Steps

- **[Pages API](docs/pages.md)** - Learn all page operations
- **[Data Sources API](docs/data-sources.md)** - Master querying and filtering
- **[Databases API](docs/databases.md)** - Understand container operations
- **[Blocks API](docs/blocks.md)** - Add rich content to pages
- **[Rich Text DSL](docs/rich-text-dsl.md)** - Format text beautifully

## Need Help?

- Review [official Notion API docs](https://developers.notion.com/)
- Check the [API documentation](docs/) for detailed guides
- See [error handling guide](docs/error-handling.md) for troubleshooting

---

**Remember**: Always close the client when done to release resources:

```kotlin
notion.close()
```

Or use `.use` for automatic cleanup:

```kotlin
NotionClient("token").use { notion ->
    // Your code here
}
```
