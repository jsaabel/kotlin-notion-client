# Journal Entry: 2025-10-05 - Search API Implementation

## Context
Implementing the Search API to make the client feature-complete. The Search API allows searching all pages and data sources (formerly databases) that have been shared with an integration.

## 2025-09-03 API Changes for Search

### Filter Parameter Change
- **Old (2022-06-28)**: `filter.value = "page" | "database"`
- **New (2025-09-03)**: `filter.value = "page" | "data_source"`

### Response Changes
- Returns data source IDs and objects instead of database objects
- Multiple data sources per database appear separately in results
- Each data source has its own ID

## Implementation Plan

### 1. Models to Create
- `SearchRequest` - Request body with:
  - `query`: Optional string for text search
  - `filter`: Optional filter object (value: "page" | "data_source", property: "object")
  - `sort`: Optional sort object (direction: "ascending" | "descending", timestamp: "last_edited_time")
  - `start_cursor`: Optional pagination cursor
  - `page_size`: Optional page size (default 100, max 100)

- `SearchResponse` - Paginated response with:
  - `results`: List of Page or DataSource objects
  - `next_cursor`: Optional pagination cursor
  - `has_more`: Boolean for pagination
  - `type`: "page_or_database" (legacy field)

- `SearchFilter` - Filter criteria
- `SearchSort` - Sort criteria

### 2. SearchApi Implementation
- `search(request: SearchRequest): SearchResponse` - Main search function
- Support for pagination
- Follow existing API patterns (PagesApi, DatabasesApi, etc.)

### 3. Testing
- **Unit Tests**: Mock-based tests using official sample responses
- **Integration Tests**: Real API tests with proper env var checking
- Follow established patterns from other API implementations

## Files to Create/Modify

### New Files
1. `src/main/kotlin/no/saabelit/kotlinnotionclient/api/SearchApi.kt`
2. `src/main/kotlin/no/saabelit/kotlinnotionclient/models/search/SearchModels.kt`
3. `src/test/kotlin/unit/SearchApiTest.kt`
4. `src/test/kotlin/integration/SearchIntegrationTest.kt`
5. `src/test/resources/api/search/post_search_by_title.json` (copy from reference)

### Modified Files
1. `src/main/kotlin/no/saabelit/kotlinnotionclient/NotionClient.kt` - Add `val search: SearchApi`
2. `src/test/kotlin/unit/util/TestFixtures.kt` - Add Search fixtures
3. `src/test/kotlin/unit/util/MockResponseBuilder.kt` - Add search mock builders

## Key Implementation Notes

### Search Limitations (from docs)
- Not optimized for exhaustive enumeration of all documents
- Not for searching within a specific database (use Query API instead)
- Search indexing is not immediate - results may be delayed
- Best when filtering by object type and providing text query

### Pagination Support
- Standard Notion pagination with `start_cursor` and `has_more`
- Default page_size is 100, maximum is 100

### Important: 2025-09-03 Compatibility
- Use `"data_source"` in filter values, not `"database"`
- Results can include both Page and DataSource objects
- Parent fields will use `data_source_id` instead of `database_id`

## Success Criteria
- ✅ SearchApi implemented with all request parameters
- ✅ Models support both Page and DataSource results
- ✅ Unit tests with mock responses (5 tests created)
- ✅ Integration test with real API (4 test scenarios)
- ✅ Pagination support working
- ✅ Added to NotionClient facade
- ✅ TestFixtures and MockResponseBuilder updated

## Implementation Summary

### Files Created
1. `src/main/kotlin/no/saabelit/kotlinnotionclient/api/SearchApi.kt`
2. `src/main/kotlin/no/saabelit/kotlinnotionclient/models/search/SearchModels.kt`
3. `src/test/kotlin/unit/SearchApiTest.kt`
4. `src/test/kotlin/integration/SearchIntegrationTest.kt`
5. `src/test/resources/api/search/post_search_by_title.json`

### Files Modified
1. `src/main/kotlin/no/saabelit/kotlinnotionclient/NotionClient.kt` - Added `val search: SearchApi`
2. `src/test/kotlin/unit/util/TestFixtures.kt` - Added Search fixtures
3. `src/test/kotlin/unit/util/MockResponseBuilder.kt` - Added search mock builders

### Models Created
- `SearchRequest` - Request with query, filter, sort, pagination params
- `SearchFilter` - Filter by "page" or "data_source"
- `SearchSort` - Sort by last_edited_time ascending/descending
- `SearchResponse` - Paginated response with JsonElement results

### DSL Support
```kotlin
searchRequest {
    query("meeting notes")
    filterPages()  // or filterDataSources()
    sortDescending()  // or sortAscending()
    pageSize(50)
    startCursor("cursor-value")
}
```

### Test Coverage
- **Unit Tests** (SearchApiTest): 5 tests
  - Search with query
  - Search with filter (pages)
  - Search with sort and pagination
  - Search with data source filter
  - Empty search

- **Integration Tests** (SearchIntegrationTest): 4 scenarios
  - Basic search
  - Search with query filter
  - Search with DSL (pages filter, sort, page size)
  - Data source search (2025-09-03 API)

## Status: ✅ COMPLETE

The Search API is now fully implemented and integrated into the Notion client. All tests passing.
