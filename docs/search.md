# Search API

## Overview

The Search API allows you to search across all pages and data sources (tables) that have been shared with your Notion integration. It's ideal for building search functionality, finding content by title, or discovering what your integration has access to.

**Official Documentation**: [Notion Search API](https://developers.notion.com/reference/post-search)

## Available Operations

```kotlin
// Search with DSL builder
suspend fun search(request: SearchRequest = SearchRequest()): SearchResponse

// Quick search with just a query string
suspend fun search(query: String): SearchResponse
```

## Quick Start

```kotlin
val notion = NotionClient("your_token")

// Simple text search
val results = notion.search.search("meeting notes")

// Using DSL for advanced search
val pages = notion.search.search(searchRequest {
    query("project")
    filterPages()
    sortDescending()
    pageSize(50)
})

// Process results (they're JsonElement - can be Page or DataSource)
println("Found ${pages.results.size} results")
```

## Examples

### Basic Search

Search all accessible content:

```kotlin
// Search everything
val results = notion.search.search()
println("Found ${results.results.size} accessible items")

// Search with a query
val filtered = notion.search.search("weekly report")
println("Found ${filtered.results.size} items matching 'weekly report'")
```

### Filter by Object Type

The 2025-09-03 API uses `"data_source"` (not `"database"`) for filtering tables:

```kotlin
// Search only pages
val pages = notion.search.search(searchRequest {
    query("meeting notes")
    filterPages()
})

// Search only data sources (tables)
val dataSources = notion.search.search(searchRequest {
    query("customer database")
    filterDataSources()  // ✅ Use this for tables in 2025-09-03 API
})
```

### Sort Results

Sort by last edited time (ascending or descending):

```kotlin
// Most recently edited first
val recent = notion.search.search(searchRequest {
    query("project")
    sortDescending()
})

// Oldest first
val oldest = notion.search.search(searchRequest {
    sortAscending()
})
```

### Pagination

Handle large result sets with pagination:

```kotlin
// Basic pagination
var cursor: String? = null
var totalResults = 0

do {
    val page = notion.search.search(searchRequest {
        query("documentation")
        startCursor(cursor)
        pageSize(50)
    })

    totalResults += page.results.size
    println("Retrieved ${page.results.size} results")

    cursor = page.nextCursor
} while (page.hasMore)

println("Total results: $totalResults")
```

### Combining Filters, Sorting, and Pagination

```kotlin
// Advanced search with all options
val results = notion.search.search(searchRequest {
    query("Q4 planning")
    filterPages()           // Only pages
    sortDescending()        // Most recent first
    pageSize(25)           // 25 results per page
})

// Navigate to next page if available
if (results.hasMore && results.nextCursor != null) {
    val nextPage = notion.search.search(searchRequest {
        query("Q4 planning")
        filterPages()
        sortDescending()
        pageSize(25)
        startCursor(results.nextCursor)
    })
}
```

### Search All Accessible Content

Get all items (pages and data sources) the integration can access:

```kotlin
suspend fun getAllAccessibleContent(client: NotionClient): List<JsonElement> {
    val allItems = mutableListOf<JsonElement>()
    var cursor: String? = null

    do {
        val page = client.search.search(searchRequest {
            pageSize(100)  // Max page size
            cursor?.let { startCursor(it) }
        })

        allItems.addAll(page.results)
        cursor = page.nextCursor
    } while (page.hasMore)

    return allItems
}

val content = getAllAccessibleContent(notion)
println("Integration has access to ${content.size} items")
```

## DSL Reference

The `searchRequest` DSL provides a type-safe way to build search requests:

```kotlin
searchRequest {
    // Text query (searches in titles)
    query("search term")

    // Filter by object type
    filterPages()        // Only pages
    filterDataSources()  // Only data sources (tables)

    // Sort by last edited time
    sortAscending()      // Oldest first
    sortDescending()     // Newest first

    // Pagination
    pageSize(50)         // 1-100, default 100
    startCursor("abc")   // Resume from cursor
}
```

## Response Structure

The `SearchResponse` contains:

```kotlin
data class SearchResponse(
    val objectType: String,           // Always "list"
    val results: List<JsonElement>,   // Array of Page or DataSource objects
    val nextCursor: String?,          // Cursor for next page (null if no more)
    val hasMore: Boolean,             // Whether more results exist
    val type: String?,                // Legacy field: "page_or_database"
    val pageOrDatabase: Map<String, String>?
)
```

**Note**: Results are returned as `JsonElement` because they can be either Page or DataSource objects. You'll need to deserialize them to the appropriate type based on your needs.

## Common Patterns

### Check if Integration Has Access to Any Content

```kotlin
val results = notion.search.search()
if (results.results.isEmpty()) {
    println("No content shared with this integration yet")
} else {
    println("Integration has access to ${results.results.size}+ items")
}
```

### Search Within a Specific Timeframe

While you can't filter by date directly, you can sort and paginate to find recent items:

```kotlin
// Get most recently edited pages
val recentPages = notion.search.search(searchRequest {
    filterPages()
    sortDescending()
    pageSize(10)
})
```

### Validate Query Before Searching

```kotlin
fun searchWithValidation(client: NotionClient, query: String): SearchResponse {
    require(query.isNotBlank()) { "Search query cannot be blank" }
    require(query.length <= 1000) { "Query too long (max 1000 characters)" }

    return client.search.search(query)
}
```

## Important Limitations

### Search Indexing Delay
**Search indexing is not immediate.** If you just shared content with your integration, it may take time to appear in search results. Allow several seconds to minutes for new permissions to propagate.

### Title Search Only
The search query only matches against **titles** (page titles, database names). It does not search within page content, property values, or block text.

### Not for Database Queries
Don't use Search API to find pages within a specific database. Use the [Query Database API](databases.md#query-databases) instead, which supports filtering and sorting by properties.

### Not for Exhaustive Enumeration
The Search API is designed for user-initiated searches, not for systematically enumerating all content. For listing all pages in a database, use the Query Database API.

### API Version: data_source vs database
In the **2025-09-03 API**, use `"data_source"` when filtering:
- ✅ **Correct**: `filterDataSources()` or `filter.value = "data_source"`
- ❌ **Old API**: `filter.value = "database"` (this won't work)

## Common Gotchas

### Empty Query Returns Everything
An empty search (no query) returns all accessible pages and data sources. This is useful for discovering what the integration can access:

```kotlin
// This returns ALL accessible content (not an error)
val everything = notion.search.search()
```

### Results as JsonElement
Search results are `List<JsonElement>` rather than strongly-typed objects because they can be either pages or data sources. You'll need to deserialize based on the `object` field:

```kotlin
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

results.results.forEach { element ->
    val objectType = element.jsonObject["object"]?.jsonPrimitive?.content
    when (objectType) {
        "page" -> {
            // Deserialize as Page
        }
        "database" -> {
            // Deserialize as DataSource
        }
    }
}
```

### Page Size Validation
Page size must be between 1 and 100. The DSL validates this:

```kotlin
// This throws IllegalArgumentException
searchRequest {
    pageSize(150)  // ❌ Max is 100
}
```

### Pagination Cursor from Previous Response
Always use the `nextCursor` from the response, don't construct cursors manually:

```kotlin
// ✅ Correct
val firstPage = notion.search.search()
if (firstPage.hasMore) {
    val secondPage = notion.search.search(searchRequest {
        startCursor(firstPage.nextCursor!!)
    })
}

// ❌ Wrong - don't make up cursor values
val page = notion.search.search(searchRequest {
    startCursor("made-up-cursor")  // Will fail
})
```

## Best Practices

### ✅ DO

- **Filter by object type** when you know what you're looking for (`filterPages()` or `filterDataSources()`)
- **Sort results** to show most recent or oldest first
- **Handle empty results** gracefully - not finding anything is valid
- **Use pagination** for large result sets (page size 100 for efficiency)
- **Validate query input** if accepting user input
- **Handle indexing delays** - retry after a few seconds if expected content is missing

### ❌ DON'T

- **Don't rely on immediate indexing** - allow time after sharing content
- **Don't use for database queries** - use Query Database API instead
- **Don't search page content** - search only matches titles
- **Don't enumerate all content repeatedly** - cache results when possible
- **Don't use `"database"` filter value** - use `"data_source"` in 2025-09-03 API

## Testing

See test examples in:
- **Unit Tests**: `src/test/kotlin/unit/SearchApiTest.kt` - Mock-based tests with sample responses
- **Integration Tests**: `src/test/kotlin/integration/SearchIntegrationTest.kt` - Real API tests

**Note**: Unlike other APIs (Pages, Blocks, etc.), the Search API doesn't have a dedicated examples file (`SearchExamples.kt`). This is because:
- Search only finds content already shared with your integration
- We can't predict what content exists in a user's workspace
- The examples in this documentation are ready to use - just adapt the search queries to match your own content

Example unit test with mock:

```kotlin
@Test
fun `search with filters`() {
    val client = NotionClient.createWithClient(
        mockClient { addSearchResponse() },
        NotionConfig(apiToken = "test-token")
    )

    val response = client.search.search(searchRequest {
        query("test")
        filterPages()
        sortDescending()
    })

    response.results shouldNotBe null
}
```

To test with your own integration, simply use the code examples above with your own search queries that match content in your workspace.

## Related APIs

- **[Data Sources](data-sources.md)** - Working with tables/databases returned by search
- **[Pages](pages.md)** - Working with pages returned by search
- **[Query Database](databases.md#query-databases)** - Better for searching within a specific database
