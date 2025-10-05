# Pages API

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

## Overview

The Pages API allows you to create, retrieve, and update pages in Notion. Pages are the fundamental content unit in Notion and can exist as:
- Standalone pages
- Children of other pages
- Rows within data sources (tables)

**Official Documentation**: [Notion Pages API](https://developers.notion.com/reference/page)

## Available Operations

```kotlin
// Retrieve a page
suspend fun retrieve(pageId: String): Page

// Create a page
suspend fun create(block: PageCreateRequestBuilder.() -> Unit): Page

// Update a page
suspend fun update(pageId: String, block: PageUpdateRequestBuilder.() -> Unit): Page

// Get page property item
suspend fun retrievePropertyItem(pageId: String, propertyId: String): PropertyItem
```

## Examples

_TODO: Add comprehensive examples_

### Retrieve a Page

```kotlin
val page = notion.pages.retrieve("page-id")
println(page.properties)
```

### Create a Page in a Data Source

**Important**: Pages in tables are created with a `dataSourceId` parent (not `databaseId`):

```kotlin
val page = notion.pages.create {
    parent {
        dataSourceId("data-source-id")  // ✅ 2025-09-03 API
        // NOT database_id - that's from older API versions
    }
    properties {
        title("Name") { text("My Task") }
        select("Status") { name("In Progress") }
    }
}
```

### Create a Page as Child of Another Page

```kotlin
// TODO: Add example
```

### Update Page Properties

```kotlin
// TODO: Add example
```

### Archive a Page

```kotlin
// TODO: Add example
```

## Common Patterns

_TODO: Add tips, gotchas, best practices_

### Parent Types

Pages can have different parent types:
- `pageId` - Page is a child of another page
- `dataSourceId` - Page is a row in a data source (table)
- `workspace` - Page is at workspace root (requires admin)

## Related APIs

- **[Data Sources](data-sources.md)** - Pages often live as rows in data sources
- **[Databases](databases.md)** - Understanding the container concept
- **[Blocks](blocks.md)** - Pages contain blocks for content
- **[Rich Text DSL](rich-text-dsl.md)** - Format text in page properties
