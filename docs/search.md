# Search API

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

## Overview

The Search API allows you to search across pages and data sources in your Notion workspace.

**Official Documentation**: [Notion Search API](https://developers.notion.com/reference/post-search)

## Available Operations

```kotlin
// Search with query and filters
suspend fun search(block: SearchRequestBuilder.() -> Unit = {}): PaginatedList<SearchResult>
```

## Examples

_TODO: Add comprehensive examples_

### Basic Search

```kotlin
val results = notion.search.search {
    query = "project"
}

results.results.forEach { result ->
    when (result) {
        is Page -> println("Page: ${result.id}")
        is DataSource -> println("Data Source: ${result.id}")
    }
}
```

### Filter by Object Type

**Important**: In the 2025-09-03 API, use `"data_source"` to filter for tables (not `"database"`):

```kotlin
// Search only for data sources (tables)
val dataSources = notion.search.search {
    query = "tasks"
    filter {
        value = "data_source"  // ✅ 2025-09-03 API
        property = "object"
    }
}

// Search only for pages
val pages = notion.search.search {
    query = "meeting notes"
    filter {
        value = "page"
        property = "object"
    }
}
```

### Sort Results

```kotlin
// TODO: Add sorting examples
```

### Pagination

```kotlin
// TODO: Add pagination examples
```

## Common Patterns

_TODO: Add tips, gotchas, best practices_

## Related APIs

- **[Data Sources](data-sources.md)** - Search returns data sources
- **[Pages](pages.md)** - Search returns pages
