# Development Journal - November 4, 2025

## Missing Query Filter Property Types

### 🎯 Objective
Identify and implement missing property type filters in the DataSourceQueryBuilder. During integration with another project, we discovered that relation properties (a very common use case) are not supported. This investigation aims to catalog all missing types and implement them.

### 🔍 Discovery

#### Investigation Location
- **Builder Implementation**: `src/main/kotlin/it/saabel/kotlinnotionclient/models/datasources/DataSourceQueryBuilder.kt`
- **Data Models**: `src/main/kotlin/it/saabel/kotlinnotionclient/models/datasources/DataSourceQuery.kt`
- **API Documentation**: `reference/notion-api/documentation/general/07_Filter_Database_Entries.md`

#### Currently Supported Property Types (10 types)
1. ✅ **title** - text operations (equals, contains, starts_with, ends_with, is_empty, etc.)
2. ✅ **rich_text** - text operations (same as title)
3. ✅ **number** - numeric comparisons (equals, greater_than, less_than, etc.)
4. ✅ **select** - equals, does_not_equal, is_empty, is_not_empty
5. ✅ **multi_select** - contains, does_not_contain, is_empty, is_not_empty
6. ✅ **date** - temporal comparisons (equals, before, after, past_week, next_month, etc.)
7. ✅ **checkbox** - boolean comparisons (equals, does_not_equal)
8. ✅ **url** - text operations (same as rich_text)
9. ✅ **email** - text operations (same as rich_text)
10. ✅ **phone_number** - text operations (same as rich_text)

#### Missing Property Types (8 types)

According to the Notion API documentation (line 85 in the filter docs), the following property types support filtering but are **NOT YET IMPLEMENTED**:

##### High Priority (Common Use Cases)
1. ❌ **relation** - Filter by related pages
   - Operations: `contains`, `does_not_contain`, `is_empty`, `is_not_empty`
   - Value type: UUID string
   - Use case: Very common for filtering by related entities
   - API docs: lines 286-306

2. ❌ **people** - Filter by users (also applies to created_by, last_edited_by)
   - Operations: `contains`, `does_not_contain`, `is_empty`, `is_not_empty`
   - Value type: UUID string
   - Use case: Filter by assignee, creator, or editor
   - API docs: lines 260-284

3. ❌ **status** - Filter by status property
   - Operations: `equals`, `does_not_equal`, `is_empty`, `is_not_empty`
   - Value type: string
   - Use case: Common for workflow/project management databases
   - API docs: lines 429-449

4. ❌ **unique_id** - Filter by auto-incrementing ID property
   - Operations: `equals`, `does_not_equal`, `greater_than`, `less_than`, `greater_than_or_equal_to`, `less_than_or_equal_to`
   - Value type: number (NOT a UUID, despite the name)
   - Use case: Range queries on sequential IDs
   - API docs: lines 475-509
   - Note: Uses "unique_id" as the filter key, not "ID"

##### Medium Priority
5. ❌ **files** - Filter by file attachment presence
   - Operations: `is_empty`, `is_not_empty`
   - Use case: Find pages with/without attachments
   - API docs: lines 166-184

6. ❌ **timestamp** - Filter by created_time or last_edited_time
   - Special filter type (doesn't use "property" field)
   - Operations: Uses date filter conditions
   - Use case: Find recently created/edited pages
   - API docs: lines 452-473
   - Note: This is distinct from date properties; applies to page metadata

##### Lower Priority (Complex Types)
7. ❌ **formula** - Filter by formula results
   - Nested filter structure based on formula return type
   - Sub-filters: `checkbox`, `date`, `number`, `string` (using rich_text conditions)
   - Use case: Filter by computed values
   - API docs: lines 186-210

8. ❌ **rollup** - Filter by rollup aggregate values
   - Complex nested filter structure
   - For arrays: `any`, `every`, `none` (with nested type filters)
   - For aggregates: `date` or `number` (with corresponding filter conditions)
   - Use case: Filter by aggregated relation data
   - API docs: lines 334-405

### 📋 Implementation Plan

#### Phase 1: Core Missing Types (High Priority)
Implement the four most commonly used missing types:
1. Relation filters
2. People filters
3. Status filters
4. Unique ID filters

These follow the same DSL patterns as existing filters and have straightforward implementations.

#### Phase 2: Simple Utility Type
5. Files filters (very simple, only is_empty/is_not_empty)

#### Phase 3: Special Cases
6. Timestamp filters (requires special handling, no property name)

#### Phase 4: Complex Types (if needed)
7. Formula filters (nested structure)
8. Rollup filters (most complex, nested structure with multiple modes)

### 🔧 Implementation Notes

#### Pattern Consistency
All new filter builders should follow the established pattern:
- FilterBuilder extension method (e.g., `fun relation(propertyName: String)`)
- Dedicated builder class (e.g., `RelationFilterBuilder`)
- Condition data class (e.g., `RelationCondition`)
- DataSourceFilter extension in DataSourceQuery.kt

#### Testing Requirements
For each new filter type:
1. Unit tests in `DatabaseQueryFiltersTest.kt` for all operations
2. Builder pattern tests in `DataSourceQueryBuilderTest.kt`
3. JSON serialization validation
4. Integration test examples (if applicable)

#### Documentation Updates
- Update examples in test files
- Add to any relevant documentation

### ✅ Implementation Complete

#### Phase 1-2: Core Missing Types Implemented
All high-priority and medium-priority filter types have been successfully implemented:

1. ✅ **Relation filters** - `relation(propertyName)`
   - Operations: `contains(pageId)`, `doesNotContain(pageId)`, `isEmpty()`, `isNotEmpty()`
   - Use case: Filter by related pages/entities

2. ✅ **People filters** - `people(propertyName)`
   - Operations: `contains(userId)`, `doesNotContain(userId)`, `isEmpty()`, `isNotEmpty()`
   - Use case: Filter by assignees, collaborators, creators

3. ✅ **Status filters** - `status(propertyName)`
   - Operations: `equals(value)`, `doesNotEqual(value)`, `isEmpty()`, `isNotEmpty()`
   - Use case: Filter by workflow status

4. ✅ **Unique ID filters** - `uniqueId(propertyName)`
   - Operations: `equals(value)`, `doesNotEqual(value)`, `greaterThan(value)`, `lessThan(value)`, `greaterThanOrEqualTo(value)`, `lessThanOrEqualTo(value)`
   - Use case: Range queries on auto-incrementing IDs

5. ✅ **Files filters** - `files(propertyName)`
   - Operations: `isEmpty()`, `isNotEmpty()`
   - Use case: Check for attachment presence

#### Implementation Details

**Data Models Added** (`src/main/kotlin/it/saabel/kotlinnotionclient/models/datasources/DataSourceQuery.kt`):
- `RelationCondition` - for relation property filters
- `PeopleCondition` - for people property filters
- `StatusCondition` - for status property filters
- `UniqueIdCondition` - for unique_id property filters (uses Int, not UUID)
- `FilesCondition` - for files property filters

**Builder Classes Added** (`src/main/kotlin/it/saabel/kotlinnotionclient/models/datasources/DataSourceQueryBuilder.kt`):
- `RelationFilterBuilder` - DSL builder for relation filters
- `PeopleFilterBuilder` - DSL builder for people filters
- `StatusFilterBuilder` - DSL builder for status filters
- `UniqueIdFilterBuilder` - DSL builder for unique_id filters
- `FilesFilterBuilder` - DSL builder for files filters

**FilterBuilder Extensions**:
- Added `relation()`, `people()`, `status()`, `uniqueId()`, `files()` methods to FilterBuilder

**Tests Added**:
- `src/test/kotlin/unit/query/DatabaseQueryFiltersTest.kt` - 7 new unit test cases covering all new filter types
- `src/test/kotlin/integration/NewFilterTypesIntegrationTest.kt` - Comprehensive integration test suite for manual verification

#### Usage Examples

```kotlin
// Filter by relation
dataSourceQuery {
    filter {
        relation("Project").contains("12345678-1234-1234-1234-123456789abc")
    }
}

// Filter by people (assignees)
dataSourceQuery {
    filter {
        and(
            people("Assignee").contains("user-uuid"),
            people("Collaborators").isNotEmpty()
        )
    }
}

// Filter by status
dataSourceQuery {
    filter {
        status("Status").equals("In Progress")
    }
}

// Filter by unique_id (range query)
dataSourceQuery {
    filter {
        and(
            uniqueId("ID").greaterThan(100),
            uniqueId("ID").lessThanOrEqualTo(999)
        )
    }
}

// Filter by files presence
dataSourceQuery {
    filter {
        files("Attachments").isNotEmpty()
    }
}

// Complex query with multiple new filter types
dataSourceQuery {
    filter {
        and(
            title("Task Name").contains("Feature"),
            status("Status").equals("Active"),
            people("Assignee").isNotEmpty(),
            relation("Epic").contains("epic-page-id"),
            uniqueId("ID").greaterThan(1),
            files("Screenshots").isNotEmpty()
        )
    }
}
```

#### Testing & Verification

**Unit Tests**: ✅ All passing
- 7 new test cases in `DatabaseQueryFiltersTest.kt`
- Tests for individual filter types and complex combinations
- Run with: `./gradlew test`

**Integration Tests**: ✅ Manually verified against live Notion API
- Created comprehensive test suite with 3-phase workflow:
  1. Setup: Creates test databases with all new property types
  2. Populate: Creates sample pages with test data
  3. Verify: Tests each filter type against real API
- All filter types verified working correctly with live data
- Run with: `./gradlew test --tests "*NewFilterTypesIntegrationTest*Verify*"`

**Build Results**:
- ✅ Code formatting passed
- ✅ Build successful
- ✅ All tests passing

#### Remaining Work (Lower Priority)
- Timestamp filters (special case - no property name, filters by created_time/last_edited_time)
- Formula filters (nested structure based on formula result type)
- Rollup filters (most complex - nested with multiple aggregation modes)

These can be implemented later as needed. The most commonly used filter types are now fully supported.

### 📚 Reference
- Notion API Filter Documentation: `reference/notion-api/documentation/general/07_Filter_Database_Entries.md`
- Implementation: `src/main/kotlin/it/saabel/kotlinnotionclient/models/datasources/`
- Unit Tests: `src/test/kotlin/unit/query/DatabaseQueryFiltersTest.kt`
- Integration Tests: `src/test/kotlin/integration/NewFilterTypesIntegrationTest.kt`