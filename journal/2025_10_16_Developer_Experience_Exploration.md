# 2025-10-16: Developer Experience Exploration

**Date**: 2025-10-16
**Status**: 🚧 In Progress
**Duration**: ~2 hours planned

## Overview

This session focuses on **dogfooding** the Kotlin Notion Client library through structured, hands-on exploration. The goal is to evaluate the developer experience (DX) from an end-user perspective and identify any gaps, pain points, or areas for improvement in the DSL and API design.

## Objectives

### Primary Goals
1. **Identify DX Issues**: Find friction points, confusing patterns, or missing conveniences
2. **Test DSL Ergonomics**: Evaluate how natural and intuitive the DSL feels in practice
3. **Discover Missing Features**: Identify common use cases that are harder than they should be
4. **Validate Documentation**: Ensure documented patterns actually work well in practice

### Learning Outcomes
- Understanding of real-world usage patterns
- Insights into developer expectations vs actual API design
- List of concrete improvements for future iterations
- Validation of current design decisions

## Methodology

### Approach: Guided Exploration Tasks

**Structure**:
1. Claude prepares minimal boilerplate/scaffolding for each task
2. User implements the core logic using the library
3. User evaluates the experience (smooth vs frustrating)
4. Issues/improvements discussed and documented
5. Repeat for next task

**Why This Works**:
- Simulates real developer experience (minimal hand-holding)
- Reveals actual pain points (not theoretical ones)
- Tests documentation effectiveness
- Identifies gaps in DSL coverage

### Task Categories

We'll explore key library capabilities across different API domains:

1. **Page Operations** - CRUD with various property types
2. **Database Operations** - Schema creation and querying
3. **Block Operations** - Content creation with nested structures
4. **Rich Text DSL** - Complex formatting scenarios
5. **Query DSL** - Advanced filtering and sorting
6. **Error Handling** - Graceful failure scenarios

## Task Design Principles

Each task should be:
- ✅ **Focused**: Test 1-2 specific features
- ✅ **Realistic**: Based on common real-world needs
- ✅ **Time-Boxed**: 10-20 minutes per task
- ✅ **Minimal Boilerplate**: Focus on the library API, not setup
- ✅ **Evaluatable**: Clear success criteria

## Session Plan

### Phase 1: Setup (5 minutes)
- Create test workspace (temporary Notion page)
- Prepare task scaffolding
- Review task list and objectives

### Phase 2: Guided Exploration (~90 minutes)
Execute 6-8 focused tasks covering:
- Page property manipulation
- Database schema and queries
- Block hierarchy and content
- Rich text formatting
- Query building
- Edge cases

### Phase 3: Review & Document (25 minutes)
- Consolidate findings
- Prioritize improvements
- Document patterns that worked well
- Create issues for significant gaps

## Task List

### Task 1: Multi-Property Page Creation (15 min)
**Goal**: Create a page with diverse property types to test DSL coverage

**Focus Areas**:
- All common property types (title, text, number, select, multi-select, date, checkbox, url, email, phone, files, people, relation)
- Nested property configurations
- Property DSL discoverability

**Success Criteria**: Page created with all properties set correctly

**Evaluation Questions**:
- Was it easy to find the right DSL methods?
- Were property names/patterns intuitive?
- Any confusing type conversions?
- Missing convenience methods?

---

### Task 2: Database Schema Design (15 min)
**Goal**: Create a database with various property types and configurations

**Focus Areas**:
- Property type definitions
- Select/multi-select options with colors
- Formula and rollup properties
- Relation properties

**Success Criteria**: Database created with properly configured schema

**Evaluation Questions**:
- Was schema definition clear and type-safe?
- Were property configurations intuitive?
- Any ambiguous or confusing APIs?
- Good error messages for mistakes?

---

### Task 3: Complex Query Building (15 min)
**Goal**: Query a database with nested filters and multiple sorts

**Focus Areas**:
- Nested AND/OR logic
- Multiple filter conditions
- Property-specific filters
- Sort combinations
- Pagination

**Success Criteria**: Query returns expected results

**Evaluation Questions**:
- Was filter nesting intuitive?
- Clear how to combine conditions?
- Sort syntax straightforward?
- Pagination easy to work with?

---

### Task 4: Rich Text Formatting (10 min)
**Goal**: Create a page with extensively formatted rich text

**Focus Areas**:
- Multiple text segments with different styles
- Links with annotations
- Mentions (users, pages, dates)
- Equations
- Color and background color

**Success Criteria**: Rich text displays with all formatting

**Evaluation Questions**:
- Was DSL readable and expressive?
- Easy to combine multiple annotations?
- Mention syntax clear?
- Any verbose or repetitive patterns?

---

### Task 5: Nested Block Structures (15 min)
**Goal**: Create a page with deeply nested block hierarchy

**Focus Areas**:
- Block children DSL
- Different block types (headings, lists, toggles, callouts)
- Nested list items
- Tables with formatted cells

**Success Criteria**: Page has correct nested structure

**Evaluation Questions**:
- Was nesting syntax natural?
- Easy to build hierarchy?
- Block type methods discoverable?
- Any confusing parent-child patterns?

---

### Task 6: Update Operations (10 min)
**Goal**: Update page properties and block content

**Focus Areas**:
- Property updates (partial vs full)
- Clearing properties
- Archiving/unarchiving
- Block content updates

**Success Criteria**: Updates applied correctly

**Evaluation Questions**:
- Update syntax clear?
- Easy to partial-update?
- Clear how to clear/remove values?
- Archive operations intuitive?

---

### Task 7: Error Scenarios (10 min)
**Goal**: Trigger and handle various error conditions

**Focus Areas**:
- Invalid page/database IDs
- Permission errors
- Validation errors
- Rate limiting

**Success Criteria**: Errors caught and handled gracefully

**Evaluation Questions**:
- Error types specific and useful?
- Error messages helpful?
- Recovery patterns clear?
- Retry logic working?

---

### Task 8: Pagination & Bulk Operations (10 min)
**Goal**: Work with large result sets and bulk operations

**Focus Areas**:
- Manual pagination
- Flow-based pagination
- Bulk page creation
- Bulk block appending

**Success Criteria**: All items processed efficiently

**Evaluation Questions**:
- Pagination API intuitive?
- Flow integration smooth?
- Bulk operations convenient?
- Performance acceptable?

---

## Scaffolding Requirements

For each task, Claude should provide:
1. **Test data setup** (if needed)
2. **Boilerplate code** (imports, client init, IDs)
3. **Task skeleton** (function signature, TODOs)
4. **Success verification** (how to check it worked)

User implements:
- Core DSL usage
- Business logic
- Error handling

## Evaluation Framework

### Rating Scale
For each task, rate the experience:
- ⭐⭐⭐⭐⭐ **Excellent** - Intuitive, smooth, delightful
- ⭐⭐⭐⭐ **Good** - Works well, minor friction
- ⭐⭐⭐ **Acceptable** - Gets the job done, some confusion
- ⭐⭐ **Frustrating** - Confusing, requires trial and error
- ⭐ **Poor** - Major issues, needs redesign

### Documentation
For each task, capture:
- **What worked well**: Smooth, intuitive patterns
- **What was confusing**: Unclear APIs, unexpected behavior
- **What was missing**: Desired convenience methods
- **Ideas for improvement**: Concrete suggestions

## Findings Log

### Task 1: Multi-Property Page Creation
**Rating**: ⭐⭐⭐⭐⭐ / ⭐⭐⭐⭐ / ⭐⭐⭐ / ⭐⭐ / ⭐

**What Worked**:
- [To be filled during exploration]

**Pain Points**:
- [To be filled during exploration]

**Missing Features**:
- [To be filled during exploration]

**Ideas**:
- [To be filled during exploration]

---

### Task 2: Database Schema Design
**Rating**: ___ / 5

[To be filled during exploration]

---

### Task 3: Complex Query Building
**Rating**: ___ / 5

[To be filled during exploration]

---

### Task 4: Rich Text Formatting
**Rating**: ___ / 5

[To be filled during exploration]

---

### Task 5: Nested Block Structures
**Rating**: ___ / 5

[To be filled during exploration]

---

### Task 6: Update Operations
**Rating**: ___ / 5

[To be filled during exploration]

---

### Task 7: Error Scenarios
**Rating**: ___ / 5

[To be filled during exploration]

---

### Task 8: Pagination & Bulk Operations
**Rating**: ___ / 5

[To be filled during exploration]

---

## Improvement Backlog

### High Priority
- **[FINDING #1]** NotionClient constructor pattern not intuitive
  - **Issue**: Constructor is private, forcing `.create()` factory method
  - **Expected**: `NotionClient(apiToken)` (standard Kotlin pattern)
  - **Current**: `NotionClient.create(apiToken)` (less intuitive)
  - **Impact**: First thing developers try doesn't work
  - **Solution**: Make primary constructor public with apiToken parameter, deprecate `.create()` for backward compatibility
  - **Status**: ✅ Complete - Both `NotionClient(apiToken)` and `NotionClient(NotionConfig(...))` now work

### Medium Priority
- [Nice-to-have improvements]

### Low Priority
- [Minor polish items]

### Investigate Further
- [Unclear if issue or expected behavior]

## Post-Session Tasks
- [ ] Update all documentation to use new constructor pattern
- [ ] Update integration test patterns
- [ ] Update README examples
- [ ] Update QUICKSTART guide
- [ ] Update API documentation examples in `/docs`

## Success Criteria

**Session Success**:
- ✅ Complete at least 6/8 tasks
- ✅ Identify 3+ concrete improvement areas
- ✅ Validate core DSL patterns work well
- ✅ Document findings for future work

**Long-term Success**:
- Improved developer experience in next version
- Better documentation based on pain points
- More intuitive DSL where gaps identified
- Reduced time-to-productivity for new users

## Notes & Observations

[General observations, patterns, insights during the session]

---

## Follow-up Session: Terminology Consistency Cleanup (2025-10-16)

**Goal**: Ensure consistent use of "database" vs "data source" terminology throughout the codebase, aligning with Notion API 2025-09-03 naming conventions.

### Context & Rationale

In Notion API version 2025-09-03:
- **"Database"** = Container object (like a folder) that holds data sources
- **"Data Source"** = The actual table with properties and rows that you query

The codebase had inconsistent terminology where query-related code used "database" names (`DatabaseQueryBuilder`, `databaseQuery()`) even though they operate on data sources. This creates confusion about what's being queried.

**Impact**: Pre-1.0 cleanup - library hasn't been released yet, so this is the ideal time to fix terminology.

### Phase 1: Documentation Updates ✅

**Completed**:
1. **Constructor Pattern Consistency** (30 min)
   - Updated `docs/search.md` - Simplified from `NotionClient(NotionConfig(...))` to `NotionClient("token")`
   - Updated `docs/users.md` - Same simplification in 2 places
   - **Result**: All documentation now shows `NotionClient("token")` as primary pattern

### Phase 2: Query Terminology Cleanup 🚧

**Strategy**: Use IntelliJ's intelligent rename to ensure all references are updated correctly.

**Completed So Far**:
1. ✅ Renamed class: `DatabaseQueryBuilder` → `DataSourceQueryBuilder`
2. ✅ Renamed function: `databaseQuery()` → `dataSourceQuery()`
3. ✅ Renamed file: `DatabaseQueryDslTest.kt` → `DataSourceQueryDslTest.kt`
4. ✅ Updated docstrings in `DataSourceQueryBuilder.kt`:
   - "building database queries" → "building data source queries"
   - "construct complex database queries" → "construct complex data source queries"
   - "constructing database filters" → "constructing data source filters"

**Remaining Renames** (to be done with IntelliJ intelligent rename):

| Current Name | New Name | File Location | Impact |
|--------------|----------|---------------|--------|
| `DatabaseQueryRequest` | `DataSourceQueryRequest` | `models/databases/DatabaseQueryRequest.kt` | Used in `DataSourcesApi`, builder, tests |
| `DatabaseQueryResponse` | `DataSourceQueryResponse` | `models/databases/DatabaseQueryResponse.kt` | Used in `DataSourcesApi`, pagination, tests |
| `DatabaseFilter` | `DataSourceFilter` | `models/databases/DatabaseFilter.kt` | Used extensively in all filter builders |
| `DatabaseSort` | `DataSourceSort` | `models/databases/DatabaseSort.kt` | Used in query builder |

**Files Affected**:
- `DataSourcesApi.kt` - Main API using these models
- `DataSourceQueryBuilder.kt` - Builder using filter/sort types
- All filter builder classes - Return `DataSourceFilter` instances
- Test files - Unit and integration tests
- Pagination helpers - Use response types

**Recommended Rename Order**:
1. `DatabaseSort` → `DataSourceSort` (fewest dependencies)
2. `DatabaseFilter` → `DataSourceFilter` (used by builders)
3. `DatabaseQueryRequest` → `DataSourceQueryRequest` (used by API)
4. `DatabaseQueryResponse` → `DataSourceQueryResponse` (used by API)

### Design Decision: What NOT to Rename

**Keeping "Database" terminology for**:
- Property/column-related naming (these are database concepts)
- Condition classes: `PropertyCondition`, `NumberCondition`, `SelectCondition`, etc.
- These represent database query primitives, not Notion-specific entities

**Why this makes sense**:
- "Database filter" and "database sort" are generic query concepts
- "Data source filter" is more specific and accurate for Notion's API
- We're querying data sources, applying filters and sorts to them

### Testing Strategy

**Before each rename**:
- Ensure all tests pass: `./gradlew test`
- Format code: `./gradlew formatKotlin`

**After all renames complete**:
- Run full test suite: `./gradlew test`
- Run limited integration tests if needed
- Verify no compilation errors

### Expected Outcomes

**After completion**:
- ✅ Consistent "data source" terminology for query operations
- ✅ Code aligns with Notion API 2025-09-03 naming
- ✅ Reduced confusion about what's being queried
- ✅ Zero breaking changes (library not released)
- ✅ All tests passing
- ✅ Documentation accurate

**Time Estimate**: 45-60 minutes total
- Documentation updates: 15 min ✅
- Function/class renames: 10 min ✅
- Model renames: 20 min 🚧
- Testing & verification: 15 min ⏳

### Status: ✅ COMPLETE

**Completed Steps**:
1. ✅ Renamed class: `DatabaseQueryBuilder` → `DataSourceQueryBuilder`
2. ✅ Renamed function: `databaseQuery()` → `dataSourceQuery()`
3. ✅ Renamed class: `DatabaseSort` → `DataSourceSort`
4. ✅ Renamed class: `DatabaseFilter` → `DataSourceFilter`
5. ✅ Renamed class: `DatabaseQueryRequest` → `DataSourceQueryRequest`
6. ✅ Renamed class: `DatabaseQueryResponse` → `DataSourceQueryResponse`
7. ✅ Updated all docstrings to use "data source" terminology
8. ✅ Restructured packages:
   - Created `models/datasources/` for data source-related code
   - Moved 5 files from `models/databases/` to `models/datasources/`
   - `models/databases/` now only contains Database container objects
9. ✅ All imports automatically updated via IntelliJ refactoring
10. ✅ Tests pass: `./gradlew test` - BUILD SUCCESSFUL
11. ✅ Code formatted: `./gradlew formatKotlin`
12. ✅ Committed with comprehensive message

### Final Structure

**Before** (all mixed in one folder):
```
models/databases/
├── Database.kt (container)
├── DataSource.kt (table)
├── DatabaseQueryBuilder.kt
├── DatabaseQuery.kt
└── ... (all mixed)
```

**After** (clean separation):
```
models/
├── databases/
│   ├── Database.kt
│   ├── DatabaseRequestBuilder.kt
│   └── DatabaseRequests.kt
└── datasources/
    ├── DataSource.kt
    ├── DataSourceRequestBuilder.kt
    ├── DataSourceRequests.kt
    ├── DataSourceQuery.kt (Filter, Sort, Request, Response)
    └── DataSourceQueryBuilder.kt
```

### Results & Impact

**Terminology Consistency**:
- ✅ All query-related code now uses "DataSource" prefix
- ✅ Aligns perfectly with Notion API 2025-09-03 naming
- ✅ Zero occurrences of old "DatabaseQuery*" names remain
- ✅ Clear separation: "Database" = container, "DataSource" = table

**Package Organization**:
- ✅ Clean separation of concerns
- ✅ Easy to find what you're looking for
- ✅ Prevents future confusion about what goes where
- ✅ Matches API domain model

**Files Changed**: 31 files (all imports updated automatically)
**Test Results**: All unit tests pass (BUILD SUCCESSFUL)
**Breaking Changes**: None (library not yet released)

### Time Taken
- Total: ~60 minutes
- Documentation updates: 15 min
- Class/function renames: 20 min
- Package restructure: 15 min
- Testing & verification: 10 min

---

## Post-Session Summary

**Date Completed**: 2025-10-16
**Duration**: ~2 hours
**Tasks Completed**: 3/8 (Task 1, 3, 4 partially completed)
**Overall DX Rating**: 4/5 - Very Good

**Key Findings from User Exploration**:

1. **[FINDING #1] NotionClient Constructor** ✅ FIXED
   - Expected: `NotionClient(apiToken)`
   - Status: Implemented and working
   - **Action needed**: Remove deprecation warnings - library isn't released yet, both patterns should just work

2. **[FINDING #2] Files Property Type Missing**
   - Issue: No `files()` property type in database schema DSL
   - Location: Database properties DSL
   - Impact: Cannot create file properties on databases
   - Investigation needed: Check if API restriction or implementation gap

3. **[FINDING #3] Database Icon/Cover Not Persisting**
   - Issue: `icon.emoji()` and `cover.external()` set during database creation show briefly then disappear
   - Status: Works in API call but doesn't persist in Notion
   - Likely cause: API behavior quirk or incorrect usage pattern
   - Needs investigation

4. **[FINDING #4] Parent Lambda Pattern Not Supported**
   - Issue: `parent { databaseId(...) }` doesn't work
   - Working: `parent.dataSource(id)` works fine
   - Impact: Inconsistent API - some places use lambdas, others don't
   - Recommendation: Either support both patterns everywhere or document why not

5. **[FINDING #5] Rich Text DSL in Page Content Blocks** ✅ FIXED (Partially)
   - User expects: `pages.create { content { paragraph { richText { text("..."); bold("...") } } } }`
   - Investigation revealed: Pattern works differently than expected
   - **Reality**: Paragraph lambda IS the RichTextBuilder scope - no extra `richText {}` wrapper needed
   - Correct pattern: `paragraph { text("..."); bold("...") }` (direct calls)
   - **Issue identified**: No DSL lambda support for page **properties** (only for database richText properties)
   - **Fix Applied**: Added `richText(name, block: RichTextBuilder.() -> Unit)` to `PagePropertiesBuilder`
   - **Result**: Database rich text properties now support DSL lambda formatting
   - **Limitation Documented**: Page/database titles don't support formatting (Notion strips it)

6. **[FINDING #6] Query DSL** ⭐ Excellent!
   - Filter nesting with `and()`, `or()` works intuitively
   - Property-specific filters work well: `select("Status").equals("Active")`
   - Sorting with `sortBy()` is clear
   - Overall: Very smooth experience

7. **[FINDING #7] Property Value Setting** ⭐ Excellent!
   - Works well: `title("Name", "value")`, `select("Status", "Active")`
   - Concise and type-safe
   - Good developer experience

## Immediate Action Items (Next Session)

### Priority 1: Documentation Cleanup (30 min)
- [ ] Remove "Migrating from..." sections from documentation
  - Files: `docs/databases.md` and others
- [ ] Remove "WORK IN PROGRESS" notices from documentation
  - Files: `docs/pages.md` and others
- [ ] Update terminology: "databaseQueryBuilder" → "dataSourceQueryBuilder"
  - Search all docs and code for this pattern
  - Update variable names, function names, documentation

### Priority 2: Critical API Fixes (2-3 hours)

#### A. Rich Text DSL in Page Content Blocks
- **Current**: `paragraph { richText { ... } }` doesn't work in `pages.create { content { } }`
- **Expected**: Should work the same way as in other contexts
- **Action**: Implement or identify why it doesn't work
- **Priority**: High - this is a major DX inconsistency

#### B. NotionClient Constructor - Remove Deprecation
- **Issue**: Using `@Deprecated` on `.create()` methods before v0.1.0 release
- **Action**: Remove all deprecation annotations
- **Rationale**: Library not released yet, both patterns should be first-class citizens
- **Files to update**: `NotionClient.kt`

### Priority 3: API Gaps Investigation (1-2 hours)

#### A. Files Property Type
- Research: Check Notion API docs for file property support
- If supported: Implement `files()` method in database properties DSL
- If not: Document limitation clearly

#### B. Database Icon/Cover Issue
- Test: Try setting icon/cover via separate update call after creation
- Check: Notion API docs for known issues
- Document: Workaround or limitation

#### C. Parent DSL Consistency
- Decision: Should `parent { ... }` lambda pattern be supported?
- If yes: Implement for all parent types (page, database, dataSource)
- If no: Document why `parent.xxx()` is preferred pattern

### Priority 4: Code Cleanup (1 hour)
- [ ] Rename: "databaseQueryBuilder" → "dataSourceQueryBuilder" everywhere
  - DSL files
  - Documentation
  - Test files
  - Example code

- [ ] Update all examples to show `NotionClient(apiToken)` as primary pattern
  - README
  - QUICKSTART
  - `/docs/*.md`
  - Integration tests (optional - both patterns work)

## Future Improvements (Post-0.1.0)

### Medium Priority
- Complete remaining exploration tasks (2, 5-8) when time permits
- Additional property type convenience methods
- Review other DSL consistency issues

### Low Priority
- Create notebooks for common use cases
- Performance optimization
- More comprehensive error handling examples

## Session Notes

**What Worked Really Well**:
- New `NotionClient(apiToken)` constructor is intuitive
- Query DSL is excellent - nested filters, sorting all very clear
- Property value setting is concise and type-safe
- Database/page creation DSL generally smooth

**Critical Gaps Identified**:
- Rich text DSL doesn't work in page content blocks (major DX issue)
- Files property type missing
- Database icon/cover doesn't persist
- Parent DSL pattern inconsistency

**User Feedback**:
- "This works well and intuitively" (database creation)
- "This part worked well" (page property setting)
- "This also worked well" (queries)
- Dogfooding approach is very helpful but time-intensive
- Deprecation warnings inappropriate for unreleased library

**Design Principles Learned**:
- Consistency matters more than "one right way"
- Both patterns can coexist without deprecation pre-1.0
- Rich text DSL should work everywhere, not just some contexts
- User expectations are based on patterns they've already seen

## Follow-up Session: Unit Test Performance Fix (2025-10-16)

**Issue Discovered**: Unit tests timing out (2+ minutes instead of milliseconds)

### Root Cause Analysis
The `NotionClient.createWithClient()` method used reflection to inject mock HTTP clients, but had a critical bug:

```kotlin
// BROKEN - Created real HTTP client, then replaced it
internal fun createWithClient(httpClient: HttpClient, config: NotionConfig): NotionClient {
    val instance = NotionClient(config)  // ← Created real CIO client!
    val field = NotionClient::class.java.getDeclaredField("httpClient")
    field.isAccessible = true
    field.set(instance, httpClient)  // ← Replaced with mock
    return instance
}
```

**Problem**: Line created a real HTTP client that attempted connections, causing tests to hang waiting for timeouts. The real client was never closed.

### Solution Implemented ✅

Refactored to use constructor dependency injection instead of reflection:

```kotlin
class NotionClient
    @JvmOverloads
    constructor(
        config: NotionConfig,
        internal val client: HttpClient? = null,
    ) {
        constructor(apiToken: String) : this(NotionConfig(apiToken = apiToken), null)

        private val httpClient: HttpClient = client ?: createHttpClient(config)
        // ...
    }

// Clean factory method - no reflection!
internal fun createWithClient(httpClient: HttpClient, config: NotionConfig): NotionClient =
    NotionClient(config, httpClient)
```

### Results
- **Before**: Tests timed out after 2+ minutes
- **After**: Tests complete in **~400ms**
- ✅ No more reflection hacks
- ✅ No more unused HTTP clients
- ✅ Clean constructor-based dependency injection

### Integration Test Migration ✅

Updated all integration tests to use idiomatic constructor pattern:

```kotlin
// Before: Factory method
val client = NotionClient.create(NotionConfig(apiToken = token))

// After: Direct constructor (more idiomatic Kotlin)
val client = NotionClient(NotionConfig(apiToken = token))
```

**Files updated**: 13 integration test files
- All pagination tests (7 files)
- DataSourcesIntegrationTest, DatabaseQueryIntegrationTest
- EnhancedFileUploadIntegrationTest, NotionClientIntegrationTest
- RateLimitVerificationTest

**Note**: `NotionClient.create()` factory methods remain available as alternative pattern - both work, no deprecation.

### Documentation Updates Needed ⏳

Need to update documentation to show constructor as primary pattern and explain both methods:

**Files to update**:
- README.md - Show both instantiation patterns
- QUICKSTART guide - Update examples
- `/docs/*.md` - Update all code examples
- Add section explaining: "You can instantiate NotionClient using either the constructor `NotionClient(apiToken)` or the factory method `NotionClient.create(apiToken)` - both are fully supported."

## Next Session Action Plan

**Start Here** (in order):
1. ~~**Remove deprecation warnings** from NotionClient (5 min)~~ ✅ COMPLETE - No deprecations used
2. ~~**Fix unit test performance**~~ ✅ COMPLETE - Tests now run in ~400ms
3. ~~**Migrate integration tests to constructor pattern**~~ ✅ COMPLETE - 13 files updated
4. **Update documentation to use constructor pattern** (30-45 min) - Show both methods
5. **Fix rich text DSL in page content blocks** (1-2 hours) - Critical UX issue
6. **Documentation cleanup** (30 min) - Quick wins (remove WIP notices, migration sections)
6. **Investigate files property** (30 min) - Clarify capability
7. **Investigate database icon/cover** (30 min) - May have workaround
8. **Rename databaseQueryBuilder** (30 min) - Consistency cleanup
9. **Parent DSL decision** (30 min) - Document or implement

**Estimated remaining**: 2-3 hours

**Success Criteria**:
- ✅ Both NotionClient patterns supported without warnings
- ✅ Unit tests complete in <1 second
- ✅ Integration tests use idiomatic constructor pattern
- ✅ Rich text DSL works in page content blocks (or documented why not)
- ⏳ Documentation clean and current
- ⏳ Terminology consistent (dataSource not database)
- ⏳ Known limitations clearly documented

---

## Follow-up Session: Rich Text DSL Properties Fix (2025-10-16)

**Issue**: FINDING #5 revealed confusion about rich text DSL support in properties

### Investigation & Resolution ✅

**Initial Misconception**:
- User expected: `title("Name") { text("x"); bold("y") }` to work for page titles
- Reality: Page titles in Notion **don't support rich text formatting** (Notion strips it)

**Actual Gap Found**:
- Page content blocks (`paragraph`, `heading`, etc.) already supported DSL via lambda: `paragraph { text("x"); bold("y") }`
- Database `richText` properties did NOT support DSL lambda (inconsistency)

**Fix Implemented**:

1. **Added `richText()` Lambda Overload to `PagePropertiesBuilder`**:
   ```kotlin
   fun richText(name: String, block: RichTextBuilder.() -> Unit) {
       properties[name] = PagePropertyValue.RichTextValue(richText = richText(block))
   }
   ```

2. **Did NOT Add `title()` Lambda** - Because:
   - Notion page/database titles are **always plain text only**
   - Formatting is silently stripped by Notion
   - Would mislead users into thinking formatting works

3. **Updated Documentation**:
   - `PagePropertiesBuilder` now documents that titles only support plain text
   - `CreatePageRequestBuilder.title()` notes: "Page titles only support plain text. Notion strips any formatting from titles."

4. **Comprehensive Testing**:
   - Added 6 unit tests for `richText()` lambda DSL
   - Tests cover: simple formatting, mentions, links, colors, all formatting options
   - Removed 2 invalid `title()` lambda tests (since it doesn't make sense)
   - All tests pass ✅

### API Consistency Achieved

**Now Consistent Across APIs**:

| **Context** | **DSL Lambda Support** | **Reason** |
|------------|----------------------|-----------|
| `richText()` database property | ✅ **Supported** | Rich text fields support formatting |
| `title()` properties | ❌ **Not Supported** | Titles are always plain text in Notion |
| Content blocks (`paragraph`, etc.) | ✅ **Already Supported** | Block content supports rich formatting |

**Example Usage**:
```kotlin
pageProperties {
    // ✅ Works - database rich text property
    richText("Description") {
        text("Created by ")
        userMention(userId)
        text(" on ")
        dateMention(LocalDate.now())
    }

    // ✅ Works - plain text title
    title("Name", "Plain Text Only")

    // ❌ Removed - would be misleading
    // title("Name") { bold("text") }  // Notion strips this anyway!
}
```

### Results
- ✅ DSL lambda support added where it makes sense (`richText` properties)
- ✅ Avoided misleading API (no `title` lambda since formatting doesn't work)
- ✅ Documentation clarifies Notion's limitations
- ✅ All unit tests pass (~40 tests total)
- ✅ Zero breaking changes

### Files Modified
1. `PagePropertiesBuilder.kt` - Added `richText()` lambda, updated docs
2. `CreatePageRequestBuilder.kt` - Added note about titles being plain text only
3. `PagePropertiesBuilderTest.kt` - Added 6 tests, removed 2 invalid tests
4. `PageRequestBuilderIntegrationTest.kt` - Removed invalid integration test

**Time**: ~1 hour
**Status**: ✅ Complete

---

## Follow-up Session: Documentation Cleanup (2025-10-16)

**Goal**: Clean up documentation to remove WIP notices, migration sections, and ensure consistency

### Tasks Completed ✅

#### 1. Removed "Work in Progress" Notices
Removed all "⚠️ WORK IN PROGRESS" notices from documentation files:
- `QUICKSTART.md`
- `docs/README.md`
- `docs/pages.md`
- `docs/databases.md`
- `docs/data-sources.md`
- `docs/rich-text-dsl.md`

**Rationale**: Documentation is comprehensive and validated against live API. The library is ready for 0.1.0 release - keeping WIP notices undermines confidence in the docs.

#### 2. Removed Migration Sections
Removed all references to "migrating from older APIs" and "old way vs new way" comparisons:
- `docs/databases.md` - Removed entire "Migration from Pre-2025-09-03" section
- `docs/data-sources.md` - Rewrote "Understanding Data Sources vs. Databases" section to focus on concepts, not migration

**Rationale**: This library **only supports API version 2025-09-03**. Migration sections imply we support older versions, which is incorrect and confusing. Users should understand the current API, not compare to versions we don't support.

#### 3. Updated Terminology
Changed language from "older API" comparisons to straightforward explanations:
```markdown
# Before
**In older API versions (pre-2025-09-03)**:
- You would query a "database" to get pages
**In 2025-09-03**:
- **Databases** are containers (like folders)

# After
**Important terminology in the 2025-09-03 API**:
- **Databases** are containers (like folders) that hold data sources
- **Data sources** are the actual tables with properties and rows
```

### Files Modified
1. `QUICKSTART.md` - Removed WIP notice
2. `docs/README.md` - Removed WIP notice
3. `docs/pages.md` - Removed WIP notice
4. `docs/databases.md` - Removed WIP notice and entire migration section
5. `docs/data-sources.md` - Removed WIP notice, rewrote database comparison section
6. `docs/rich-text-dsl.md` - Removed WIP notice

### Verification
Confirmed zero remaining WIP notices:
```bash
$ grep -r "WORK IN PROGRESS" docs/ QUICKSTART.md README.md | wc -l
0
```

### Results
- ✅ All WIP notices removed (6 files)
- ✅ All migration sections removed/rewritten
- ✅ Documentation focuses solely on 2025-09-03 API
- ✅ Language is clear and non-comparative
- ✅ Zero breaking changes to examples

**Time**: ~30 minutes
**Status**: ✅ Complete

---

## Session Summary & Next Priorities

### Completed Today (2025-10-16) ✅

1. **Unit Test Performance** (~1 hour)
   - Fixed reflection-based mock client injection
   - Tests now run in ~400ms instead of 2+ minutes

2. **Integration Test Migration** (~30 min)
   - Updated 13 test files to use idiomatic constructor pattern
   - `NotionClient(NotionConfig(...))` instead of `.create()`

3. **Rich Text DSL Properties** (~1 hour)
   - Added DSL lambda support for `richText()` page properties
   - Documented that titles don't support formatting

4. **Documentation Cleanup** (~30 min)
   - Removed all WIP notices (6 files)
   - Removed migration sections
   - Focused docs on 2025-09-03 API only

5. **Terminology Consistency & Package Restructure** (~1 hour)
   - Renamed all `Database*` query classes to `DataSource*`
   - Separated `models/databases/` from `models/datasources/`
   - Updated all documentation references
   - 32 files changed, all imports updated via IntelliJ

### Key Achievements
- ✅ Terminology now aligns perfectly with Notion API 2025-09-03
- ✅ Clean package structure (databases vs datasources)
- ✅ Zero remaining old "DatabaseQuery*" references
- ✅ All documentation clean and accurate
- ✅ All tests passing

### Next Priority Tasks

**Remaining from Original Action Plan:**
1. **Files Property Type** (30 min) - FINDING #2
   - Investigate if Notion API supports files property type
   - If yes: implement, if no: document limitation

2. **Database Icon/Cover Issue** (30 min) - FINDING #3
   - Test workarounds (separate update call after creation)
   - Document findings

3. **Parent DSL Consistency** (30 min) - FINDING #4
   - Decide: support `parent { ... }` lambda or document why not
   - Ensure consistency across API

**For Next Session:**
Choose one of the above to investigate, or move on to preparing for v0.1.0 release.

### Time Investment Today
- **Total**: ~4 hours
- **High Impact Changes**: Terminology consistency, package structure, documentation cleanup
- **Library State**: Ready for v0.1.0 release pending investigation of remaining findings

---

## Follow-up Session: Strong Typing Improvements - Parent API (2025-10-16)

**Goal**: Convert Parent from string-discriminated data class to type-safe sealed class hierarchy

### Context & Rationale

The `Parent` model used runtime string discrimination (`type` field) to determine which ID field was populated. This pattern had several issues:

```kotlin
// Before - Runtime checks, nullable fields
data class Parent(
    val type: String,
    val pageId: String? = null,
    val dataSourceId: String? = null,
    // ... other nullable IDs
)

// Usage required defensive programming
if (parent.type == "page_id") {
    val id = parent.pageId  // Could still be null!
}
```

**Problems**:
- ❌ All ID fields nullable even when `type` indicates which should be present
- ❌ Runtime string checks instead of compile-time type safety
- ❌ No universal way to get "the ID" regardless of parent type
- ❌ Potential for inconsistent state (wrong type + ID combination)

### Solution Implemented ✅

Converted to sealed class hierarchy with proper type safety:

```kotlin
@Serializable
sealed class Parent {
    abstract val id: String?  // Universal accessor

    data class PageParent(val pageId: String) : Parent()
    data class DataSourceParent(val dataSourceId: String) : Parent()
    data class DatabaseParent(val databaseId: String) : Parent()
    data class BlockParent(val blockId: String) : Parent()
    data object WorkspaceParent : Parent()
}
```

### Benefits Achieved

1. **Compile-Time Type Safety**
   ```kotlin
   // After - Guaranteed non-null when pattern matched
   when (parent) {
       is Parent.PageParent -> parent.pageId  // Non-null!
       is Parent.DataSourceParent -> parent.dataSourceId
       // Compiler ensures all cases handled
   }
   ```

2. **Universal ID Access**
   ```kotlin
   val id = parent.id  // Works for any parent type!
   // Returns appropriate ID or null for WorkspaceParent
   ```

3. **Impossible to Represent Invalid States**
   - Can't have `type="page_id"` with `databaseId` set
   - Each variant only has the fields it needs

4. **Better Developer Experience**
   - Exhaustive when expressions (compiler catches missing cases)
   - IDE autocomplete shows available properties
   - No defensive null checks needed after pattern matching

### Implementation Details

**Custom Serialization**:
- Created `ParentSerializer` to maintain API compatibility
- Handles JSON with `type` field + appropriate ID field
- Handles edge case: API sometimes returns both `database_id` and `data_source_id` (prefers data source)

**Migration Scope**:
- ✅ Updated 15 builder locations across codebase
- ✅ Updated all unit tests
- ✅ Fixed malformed test fixtures
- ✅ Updated getting started notebook
- ✅ All tests passing
- ✅ Build successful

### Before & After Comparison

**Before**:
```kotlin
// Runtime checks required
if (parent.type == "page_id") {
    val id = parent.pageId  // Could be null!
}

// Getting ID required type checking
val id = when (parent.type) {
    "page_id" -> parent.pageId
    "data_source_id" -> parent.dataSourceId
    // ... etc
    else -> null
}
```

**After**:
```kotlin
// Simple universal access
val id = parent.id  // Works for any parent!

// Type-safe when needed
when (parent) {
    is Parent.PageParent -> parent.pageId  // Guaranteed non-null!
    is Parent.DataSourceParent -> parent.dataSourceId
    // Compiler ensures exhaustive
}
```

### Files Modified

**Core Models**:
1. `models/base/NotionObject.kt` - Converted Parent to sealed class
2. `models/base/ParentSerializer.kt` - **NEW** - Custom serialization for API compatibility

**Builders** (15 locations updated):
3. `models/comments/CreateCommentRequestBuilder.kt`
4. `models/databases/DatabaseRequestBuilder.kt`
5. `models/datasources/DataSourceRequestBuilder.kt`
6. `models/pages/CreatePageRequestBuilder.kt`
7. `models/requests/RequestBuilders.kt`

**Tests**:
8. `unit/dsl/CreateCommentRequestBuilderTest.kt`
9. `unit/dsl/DatabaseRequestBuilderTest.kt`
10. `unit/dsl/PageRequestBuilderTest.kt`
11. `unit/api/CommentsApiTest.kt`
12. `unit/utils/PaginationTest.kt`
13. `unit/validation/RequestValidatorTest.kt`
14. `unit/validation/ValidationMockIntegrationTest.kt`
15. `examples/CommentsExamples.kt`
16-27. Various integration tests updated

**Resources**:
28. `test/resources/api/data_sources/post_query_a_data_source.json` - Fixed malformed fixture

**Documentation**:
29. `notebooks/01-getting-started.ipynb` - Updated to showcase new API

### Test Coverage

All tests passing:
- Unit tests: Type safety, serialization, deserialization
- Integration tests: Live API compatibility
- Edge cases: Both database_id + data_source_id in response

### API Compatibility

✅ **Fully backward compatible**:
- JSON format unchanged (still uses `type` + ID fields)
- Custom serializer handles all API responses
- Notion API sees no difference

### Results

- ✅ Strong typing eliminates runtime errors
- ✅ Universal `.id` property simplifies common case
- ✅ Compile-time exhaustiveness checking
- ✅ Zero breaking changes to API consumers
- ✅ Better IDE support (autocomplete, type inference)
- ✅ Cleaner, more maintainable code

**Time**: ~2 hours
**Status**: ✅ Complete
**Impact**: Significant improvement to type safety and developer experience

This is a major improvement to the type system that makes the client more robust and easier to use! 🚀