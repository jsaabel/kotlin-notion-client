# Pagination

## Overview

The Notion API returns many results in **paginated** format - blocks, comments, users, search results, and data source queries can all return large result sets split across multiple pages. This guide explains the different ways to handle pagination in the Kotlin Notion Client.

**Key Concepts**:
- Notion API returns up to 100 items per page by default
- You control iteration using `nextCursor` and `hasMore` fields
- This library provides both automatic collection and reactive streaming approaches

## Three Ways to Handle Pagination

### 1. Automatic Collection (Simplest)

The **default approach** automatically fetches all pages and returns a complete list:

```kotlin
// Automatically fetches ALL matching pages
val pages = notion.dataSources.query("data-source-id") {
    filter { select("Status").equals("Active") }
}

println("Found ${pages.size} pages total")
pages.forEach { page ->
    // Process all results
}
```

**How it works**: The standard `query()`, `list()`, and similar methods automatically handle pagination internally, making multiple API calls if needed and returning all results as a single list.

**Advantages**:
- **Simplest**: Just call the method and get everything
- **Familiar**: Works like a regular function call
- **Complete results**: All data available immediately

**Limitations**:
- **Memory usage**: Loads all results into memory at once
- **Latency**: Must wait for all pages before processing starts

**Use when**:
- Result sets are reasonably sized (< 1000 items)
- You need all results before processing
- Simplicity is more important than memory efficiency

### 2. Flow-Based Streaming (Recommended for Large Sets)

The **Flow-based approach** processes items reactively as pages are fetched:

```kotlin
// Process items one-by-one as pages are fetched
notion.dataSources.queryAsFlow("data-source-id") {
    filter { select("Status").equals("Active") }
}.collect { page ->
    println("Processing: ${page.id}")
    // Process each page individually as it arrives
}
```

**Advantages**:
- **Memory efficient**: Processes items as they arrive
- **Reactive**: Perfect for Kotlin coroutines and reactive streams
- **Cancellable**: Stop early by canceling the Flow
- **Immediate processing**: Start processing before all results fetched

**Use when**:
- Working with potentially large result sets (1000+ items)
- Processing items individually
- Building reactive pipelines
- Memory usage is a concern

### 3. Manual Pagination (Full Control)

For complete control over pagination, handle the cursor manually:

```kotlin
var cursor: String? = null
do {
    val response = notion.dataSources.querySinglePage("data-source-id") {
        filter { select("Status").equals("Active") }
        if (cursor != null) startCursor(cursor)
    }

    // Process this page of results
    response.results.forEach { page ->
        println("Processing: ${page.id}")
    }

    cursor = response.nextCursor
} while (response.hasMore)
```

**Note**: Most APIs don't expose `*SinglePage()` methods publicly. For manual pagination, you typically use the page-level Flow (see below) or the standard methods with careful result handling.

**Advantages**:
- **Complete control**: Decide exactly when to fetch next page
- **Custom retry logic**: Handle errors per-page
- **Progress tracking**: Monitor pagination progress

**Use when**:
- Building pagination UI with page numbers
- Implementing custom retry or caching logic
- Need very fine-grained control

### 4. Page-Level Flow (Batch Processing)

For batch processing with Flow benefits, use page-level pagination:

```kotlin
// Process complete pages (including metadata) as a Flow
notion.dataSources.queryPagedFlow("data-source-id") {
    filter { select("Status").equals("Active") }
}.collect { response ->
    println("Fetched page with ${response.results.size} items")
    println("Has more: ${response.hasMore}")

    // Process the entire page as a batch
    response.results.forEach { page ->
        // ... process page
    }
}
```

**Advantages**:
- **Batch processing**: Process entire pages at once
- **Metadata access**: Access `hasMore`, `nextCursor` in the Flow
- **Flow benefits**: Still get Flow's reactive capabilities

**Use when**:
- You want Flow's benefits but need to process pages as batches
- You need access to pagination metadata
- Implementing custom buffering or batching logic

## API Coverage

All paginated Notion APIs support both automatic collection and Flow-based streaming:

### Data Sources (Queries)

```kotlin
// Automatic - returns List<Page>
val pages = notion.dataSources.query(dataSourceId) { /* query builder */ }

// Flow (item-level) - returns Flow<Page>
notion.dataSources.queryAsFlow(dataSourceId) { /* query builder */ }

// Flow (page-level) - returns Flow<DataSourceQueryResponse>
notion.dataSources.queryPagedFlow(dataSourceId) { /* query builder */ }
```

### Blocks (Children)

```kotlin
// Automatic - returns List<Block>
val blocks = notion.blocks.retrieveChildren(blockId, pageSize = 100)

// Flow (item-level) - returns Flow<Block>
notion.blocks.retrieveChildrenAsFlow(blockId, pageSize = 100)

// Flow (page-level) - returns Flow<BlockList>
notion.blocks.retrieveChildrenPagedFlow(blockId, pageSize = 100)
```

### Comments

```kotlin
// Automatic - returns List<Comment>
val comments = notion.comments.retrieve(blockOrPageId, pageSize = 100)

// Flow (item-level) - returns Flow<Comment>
notion.comments.retrieveAsFlow(blockOrPageId, pageSize = 100)

// Flow (page-level) - returns Flow<CommentList>
notion.comments.retrievePagedFlow(blockOrPageId, pageSize = 100)
```

### Users

```kotlin
// Automatic - returns List<User>
val users = notion.users.list(pageSize = 100)

// Flow (item-level) - returns Flow<User>
notion.users.listAsFlow(pageSize = 100)

// Flow (page-level) - returns Flow<UserList>
notion.users.listPagedFlow(pageSize = 100)
```

### Search

```kotlin
// Automatic - returns List<JsonElement>
val results = notion.search.search { /* search builder */ }

// Flow (item-level) - returns Flow<JsonElement>
notion.search.searchAsFlow { /* search builder */ }

// Flow (page-level) - returns Flow<SearchResponse>
notion.search.searchPagedFlow { /* search builder */ }
```

### Page Property Items

```kotlin
// Automatic - returns List<PropertyItem>
val items = notion.pages.retrievePropertyItems(pageId, propertyId, pageSize = 100)

// Flow (item-level) - returns Flow<PropertyItem>
notion.pages.retrievePropertyItemsAsFlow(pageId, propertyId, pageSize = 100)

// Flow (page-level) - returns Flow<PagePropertyItemResponse>
notion.pages.retrievePropertyItemsPagedFlow(pageId, propertyId, pageSize = 100)
```

## Common Patterns

### Simple: Get Everything

```kotlin
// Simplest approach - automatic collection
val allPages = notion.dataSources.query("data-source-id") {
    filter { /* ... */ }
}

println("Total: ${allPages.size}")
```

### Process Large Result Sets Efficiently

```kotlin
// Memory-efficient processing as items arrive
notion.dataSources.queryAsFlow("data-source-id") {
    filter { /* ... */ }
}.collect { page ->
    processPage(page) // Processed and discarded
}
```

### Early Termination

```kotlin
// Stop after finding what you need
val targetPage = notion.dataSources.queryAsFlow("data-source-id") {}
    .firstOrNull { page ->
        // Find first page matching condition
        page.properties["Name"]?.let {
            (it as? PageProperty.Title)?.plainText == "Target Page"
        } ?: false
    }
```

### Transformation and Filtering

```kotlin
// Transform and filter using Flow operators
notion.dataSources.queryAsFlow("data-source-id") {}
    .filter { page ->
        // Only process pages with status "Active"
        val status = page.properties["Status"] as? PageProperty.Select
        status?.select?.name == "Active"
    }
    .map { page ->
        // Transform to simpler representation
        PageSummary(
            id = page.id,
            name = (page.properties["Name"] as? PageProperty.Title)?.plainText ?: ""
        )
    }
    .take(10) // Limit to first 10 matching items
    .collect { summary ->
        println(summary)
    }
```

### Batch Processing

```kotlin
// Process pages in batches of 10
notion.dataSources.queryAsFlow("data-source-id") {}
    .chunked(10)
    .collect { batch ->
        println("Processing batch of ${batch.size} pages")
        processBatch(batch) // Process 10 pages at once
    }
```

### With Progress Tracking

```kotlin
var processedCount = 0
notion.dataSources.queryAsFlow("data-source-id") {}
    .onEach { page ->
        processedCount++
        if (processedCount % 100 == 0) {
            println("Processed $processedCount pages...")
        }
    }
    .collect { page ->
        processPage(page)
    }
```

### Error Handling

```kotlin
notion.dataSources.queryAsFlow("data-source-id") {}
    .catch { error ->
        // Handle errors during pagination
        logger.error("Pagination failed: ${error.message}")
        // Optionally emit fallback value or rethrow
    }
    .collect { page ->
        processPage(page)
    }
```

### Access Pagination Metadata

```kotlin
// Use page-level flow when you need metadata
notion.dataSources.queryPagedFlow("data-source-id") {}
    .collect { response ->
        println("Page ${response.results.size} items")
        println("Next cursor: ${response.nextCursor}")
        println("Has more: ${response.hasMore}")

        response.results.forEach { page ->
            processPage(page)
        }
    }
```

## Performance Considerations

### Page Size

Control how many items are fetched per API request:

```kotlin
// Default (100) - optimal for most cases
notion.blocks.retrieveChildrenAsFlow(blockId)

// Smaller pages (10-50) - faster initial response
notion.blocks.retrieveChildrenAsFlow(blockId, pageSize = 10)

// Maximum (100) - fewer API calls
notion.blocks.retrieveChildrenAsFlow(blockId, pageSize = 100)
```

**Recommendations**:
- **Default (100)**: Good for most use cases
- **Smaller (10-50)**: When you need fast initial results or might terminate early
- **Maximum (100)**: When fetching everything and minimizing API calls

### Memory Usage

```kotlin
// ✅ Memory efficient - processes items as they arrive
notion.dataSources.queryAsFlow("data-source-id") {}
    .collect { page ->
        processPage(page) // Processed and discarded
    }

// ⚠️ Moderate memory - loads all at once
val allPages = notion.dataSources.query("data-source-id") {}
// All results in memory

// ❌ Avoid with large sets - loads everything into collection
val collected = notion.dataSources.queryAsFlow("data-source-id") {}
    .toList() // Defeats the purpose of Flow!
```

**Best Practice**:
- Use `query()` for moderate result sets (< 1000 items)
- Use `queryAsFlow()` for large result sets (1000+ items)
- Avoid collecting Flow results with `.toList()` for large sets

### Rate Limiting

The Notion API has rate limits. The library handles this automatically:

- **Automatic retry**: 429 responses trigger exponential backoff
- **Flow pacing**: Naturally spaces requests as you consume items
- **Built-in**: No additional configuration needed

## Choosing the Right Approach

```kotlin
// Small result sets (< 1000 items) → Automatic collection
val pages = notion.dataSources.query("data-source-id") { /* ... */ }

// Large result sets (1000+ items) → Flow streaming
notion.dataSources.queryAsFlow("data-source-id") { /* ... */ }
    .collect { page -> /* process */ }

// Need pagination metadata → Page-level Flow
notion.dataSources.queryPagedFlow("data-source-id") { /* ... */ }
    .collect { response ->
        println("Progress: ${response.nextCursor}")
        /* process response.results */
    }

// Early termination → Flow with operators
val found = notion.dataSources.queryAsFlow("data-source-id") { /* ... */ }
    .firstOrNull { /* condition */ }
```

## Testing Pagination

### Unit Tests

Use the generic `Pagination` utilities in your tests:

```kotlin
import no.saabelit.kotlinnotionclient.utils.Pagination
import no.saabelit.kotlinnotionclient.utils.PaginatedResponse

// Your mock response type
data class MockResponse(
    override val results: List<String>,
    override val nextCursor: String?,
    override val hasMore: Boolean
) : PaginatedResponse<String>

@Test
fun testPagination() = runTest {
    val fetcher: suspend (String?) -> MockResponse = { cursor ->
        when (cursor) {
            null -> MockResponse(listOf("a", "b"), "cursor1", true)
            "cursor1" -> MockResponse(listOf("c", "d"), null, false)
            else -> error("Unexpected cursor")
        }
    }

    val allItems = Pagination.asFlow(fetcher).toList()
    allItems shouldBe listOf("a", "b", "c", "d")
}
```

### Integration Tests

See the test files for real examples:
- `src/test/kotlin/integration/pagination/DataSourceFlowPaginationIntegrationTest.kt`
- `src/test/kotlin/integration/pagination/PagesFlowPaginationIntegrationTest.kt`

## Under the Hood

### PaginatedResponse Interface

All paginated responses implement this interface:

```kotlin
interface PaginatedResponse<T> {
    val results: List<T>
    val nextCursor: String?
    val hasMore: Boolean
}
```

This enables generic pagination utilities to work with any Notion API.

### Pagination Utilities

The `Pagination` object provides three core utilities that power all pagination:

```kotlin
object Pagination {
    // Emits individual items from all pages
    fun <T, R> asFlow(fetcher: PageFetcher<T, R>): Flow<T>
        where R : PaginatedResponse<T>

    // Collects all items from all pages into a list
    suspend fun <T, R> collectAll(fetcher: PageFetcher<T, R>): List<T>
        where R : PaginatedResponse<T>

    // Emits complete page responses
    fun <T, R> asPagesFlow(fetcher: PageFetcher<T, R>): Flow<R>
        where R : PaginatedResponse<T>
}
```

**Note**: The standard `query()`, `list()`, etc. methods use similar logic internally but are optimized for their specific use cases.

## Best Practices

1. **Use automatic collection for moderate sets** - Simplest approach when results fit in memory
2. **Use Flow for large result sets** - Memory efficient and reactive
3. **Consider early termination** - Use Flow operators like `firstOrNull()`, `take(n)`
4. **Don't defeat Flow's purpose** - Avoid `.toList()` on potentially large Flows
5. **Handle errors gracefully** - Use Flow's `catch` operator
6. **Monitor progress when needed** - Use page-level Flow or Flow's `onEach`
7. **Test with different page sizes** - Ensure your code handles pagination correctly

## Quick Reference

| Method Pattern | Returns | Use Case |
|----------------|---------|----------|
| `query()` | `List<T>` | Moderate result sets, need everything at once |
| `queryAsFlow()` | `Flow<T>` | Large sets, item-by-item processing |
| `queryPagedFlow()` | `Flow<Response>` | Batch processing, need metadata |

## Related Topics

- **[Data Sources](data-sources.md)** - Querying data sources with pagination
- **[Blocks](blocks.md)** - Retrieving block children
- **[Search](search.md)** - Searching with pagination
- **[Testing](testing.md)** - Testing paginated operations