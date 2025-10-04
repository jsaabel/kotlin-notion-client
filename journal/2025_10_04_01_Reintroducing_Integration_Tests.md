# Journal Entry: 2025-10-04 - Reintroducing Integration Tests

## Context
After implementing the Data Sources API migration (2025-09-03), we temporarily moved integration tests to `src/temp/integration/` to avoid build failures. Now we're systematically moving them back and ensuring they work with the new API.

## Objectives
1. Move integration tests back from `src/temp/integration/` to `src/test/kotlin/integration/`
2. Update tests to use new utility functions for environment variable checking
3. Ensure all tests use `integrationTestEnvVarsAreSet()` and `shouldCleanupAfterTest()` from `Util.kt`
4. Fix any compatibility issues with the new 2025-09-03 API
5. Verify each test passes before moving to the next

## Utility Functions (src/test/kotlin/integration/Util.kt)
Two helper functions are available for all integration tests:

```kotlin
fun integrationTestEnvVarsAreSet(vararg envVars: String = arrayOf("NOTION_API_TOKEN", "NOTION_TEST_PAGE_ID")): Boolean
fun shouldCleanupAfterTest(): Boolean
```

**Pattern to use in all integration tests:**
```kotlin
@Tags("Integration", "RequiresApi")
class SomeIntegrationTest : StringSpec({

    if (!integrationTestEnvVarsAreSet()) {
        "(Skipped)" { println("Skipping SomeIntegrationTest due to missing environment variables") }
    } else {
        "actual test name" {
            // test implementation

            if (shouldCleanupAfterTest()) {
                // cleanup code
            } else {
                // optionally print IDs for manual inspection
            }
        }
    }
})
```

## Progress

### Completed - Main Integration Tests (10/19)
- ✅ Created `src/test/kotlin/integration/Util.kt` with helper functions
- ✅ NotionClientIntegrationTest.kt - BehaviorSpec with `xGiven` skip pattern
- ✅ DataSourcesIntegrationTest.kt - New test for Data Sources API, StringSpec with `!` skip
- ✅ ContentDslIntegrationTest.kt - StringSpec with `!` skip
- ✅ TableBlockIntegrationTest.kt - StringSpec with `!` skip
- ✅ RateLimitVerificationTest.kt - FunSpec with `xtest` skip
- ✅ RateLimitIntegrationTest.kt - Mock-based, no env vars needed, no tags
- ✅ CommentsIntegrationTest.kt - BehaviorSpec with `xGiven` skip
- ✅ ValidationIntegrationTest.kt - FunSpec with `xtest` skip, **updated for 2025-09-03 API**
- ✅ MediaIntegrationTest.kt - StringSpec with `!` skip, multiple tests per file
- ✅ EnhancedFileUploadIntegrationTest.kt - StringSpec with `!` skip

### Completed - Migrated DSL Tests (8/11 DSL + Pagination Tests)
- ✅ `dsl/RichTextDslIntegrationTest.kt` - Moved to `src/test/kotlin/integration/dsl/`
- ✅ `dsl/ApiOverloadsIntegrationTest.kt` - Updated for data sources, UUID normalization
- ✅ `dsl/PageRequestBuilderIntegrationTest.kt` - Updated validation message
- ✅ `dsl/UpdatePageRequestBuilderIntegrationTest.kt` - Updated for data sources
- ✅ `dsl/DatabaseRequestBuilderIntegrationTest.kt` - Updated for data sources, documented icon/cover issue
- ✅ `pagination/BlockPaginationIntegrationTest.kt` - No changes needed
- ✅ `pagination/CommentPaginationIntegrationTest.kt` - No changes needed
- ✅ `pagination/PaginationEdgeCasesIntegrationTest.kt` - Updated for data sources

### Tests Requiring API Updates (5 remaining)
Located in `src/temp/integration/` - need database → data source migration:
- [ ] `DatabaseQueryIntegrationTest.kt` - Uses old database parent, needs data source updates
- [ ] `SelfContainedIntegrationTest.kt` - Creates databases, needs DSL + data source updates
- [ ] `dsl/DatabaseQueryDslIntegrationTest.kt` - Needs data source updates
- [ ] `pagination/DatabasePaginationIntegrationTest.kt` - Needs data source updates
- [ ] `pagination/RelationPaginationIntegrationTest.kt` - Needs data source updates

### Migration Strategy
For each test file:
1. Read the test file from `src/temp/integration/`
2. Check if it needs updates for 2025-09-03 API (especially parent types, data sources)
3. Ensure it uses `integrationTestEnvVarsAreSet()` pattern
4. Ensure it uses `shouldCleanupAfterTest()` for cleanup logic
5. Move to `src/test/kotlin/integration/`
6. Run the specific test to verify it works
7. Mark as completed in this journal

### Known API Changes to Watch For
Based on 2025-09-03 migration:
- Database creation now automatically creates a data source
- Pages can have `parent.dataSource()` instead of `parent.database()`
- Database queries may need to target data sources (use `client.dataSources.query()` instead of `client.databases.query()`)
- Database properties are now in the data source, not the database object
- UUID normalization required: API returns UUIDs with hyphens, need to use `.replace("-", "")` for comparisons
- Validation error messages changed from "database" to "data source"

### Known Issues & Future Investigation

#### Database Icon/Cover Persistence Issue (2025-09-03 API)
**Status**: Documented, needs further investigation
**Observed Behavior**: When creating databases with icon and cover properties:
- Icon and cover are correctly sent in the creation request
- They are returned in the creation response
- They appear briefly in the Notion UI
- Shortly after, they are cleared/reset to default

**Evidence**:
- Our code correctly constructs and sends icon/cover in `CreateDatabaseRequest`
- Test output confirms icon/cover are present in the response: `Icon: emoji = 🚀`, `Cover: external = https://placehold.co/1200x400.png`
- Both `DatabaseRequestBuilderIntegrationTest` and `ApiOverloadsIntegrationTest` pass assertions checking icon/cover in the response
- However, user reports indicate the icon disappears in the Notion UI shortly after creation

**Hypothesis**:
This may be API-level behavior in the 2025-09-03 version related to the new database/data-source model. Since databases are now containers and data sources are the actual tables, there may be:
1. A timing issue where icon/cover are processed asynchronously and fail
2. A bug in the Notion API where icon/cover aren't properly persisted on databases
3. Intended behavior where icon/cover should be set on data sources instead of (or in addition to) databases

**Next Steps**:
- Monitor Notion API changelog for any mention of this behavior
- Test if setting icon/cover on the data source (via update) works better
- Consider filing a bug report with Notion if this persists
- Keep our implementation as-is since it correctly follows the API documentation

**Related Files**:
- `src/test/kotlin/integration/dsl/DatabaseRequestBuilderIntegrationTest.kt` (lines 83-98)
- `src/test/kotlin/integration/dsl/ApiOverloadsIntegrationTest.kt` (lines 161-166)

#### Database Relation Property Configuration (2025-09-03 API)
**Status**: Workaround implemented, DSL improvement needed

**Issue**: When creating database properties with relations in the 2025-09-03 API, both `database_id` and `data_source_id` must be provided in the `RelationConfiguration`. The current `DatabaseRequestBuilder`'s `RelationBuilder` only accepts a `databaseId` parameter and doesn't support passing a `dataSourceId`.

**Current Workaround**: Tests that need to create relation properties use the manual `CreateDataSourceRequest` approach to create a second data source with proper relation configuration:
```kotlin
// Workaround example from RelationPaginationIntegrationTest
val sourceDataSource = client.dataSources.create(
    CreateDataSourceRequest(
        parent = Parent(type = "database_id", databaseId = database.id),
        title = listOf(RichText.fromPlainText("Source Data Source")),
        properties = mapOf(
            "Related Items" to CreateDatabaseProperty.Relation(
                relation = RelationConfiguration(
                    databaseId = database.id,
                    dataSourceId = targetDataSourceId,  // Required in 2025-09-03
                    singleProperty = EmptyObject()
                )
            )
        )
    )
)
```

**TODO**:
- Update `DatabaseRequestBuilder`'s `RelationBuilder` class to accept an optional `dataSourceId` parameter
- Update the `relation()` DSL method in `DatabasePropertiesBuilder` to support: `relation("prop", databaseId, dataSourceId) { }`
- Once implemented, update `RelationPaginationIntegrationTest` to use the DSL instead of manual request construction

**Related Files**:
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/databases/DatabaseRequestBuilder.kt` (RelationBuilder class)
- `src/test/kotlin/integration/pagination/RelationPaginationIntegrationTest.kt` (uses workaround)

## Notes
- Integration tests should only run when environment variables are set
- Tests should be runnable individually: `./gradlew test -Dkotest.tags.include="Integration" --tests "*SomeTest"`
- Keep cleanup optional via `NOTION_CLEANUP_AFTER_TEST` for debugging
- Each test should manage its own NotionClient lifecycle (create in test, close in finally block)

## Next Steps
1. Start with simpler tests first (e.g., `NotionClientIntegrationTest.kt`)
2. Progress to more complex DSL and pagination tests
3. Document any patterns or issues discovered during migration
4. Update this journal as we progress
