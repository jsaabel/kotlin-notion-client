# Databases API Documentation Complete

**Date**: 2025-10-06
**Status**: ✅ Complete
**Context**: Documentation strategy Phase 1

## Summary

Completed comprehensive documentation for the Databases API following the validation-first approach established with `data-sources.md`. All examples have been validated against the live Notion API.

## What Was Completed

### Documentation
- **File**: `docs/databases.md`
- **Status**: ✅ Validated
- **Examples**: 8 comprehensive examples covering all database container operations
- **Validation**: `src/test/kotlin/examples/DatabasesExamples.kt`

### Key Sections
1. Overview of database containers vs data sources
2. Available operations (retrieve, create, archive)
3. Example code for all operations
4. Property type reference
5. Number format options
6. Common patterns and gotchas
7. Migration guide from pre-2025-09-03 API
8. Best practices

## Issues Discovered and Fixed

### 1. Relation Property Requires Both IDs
**Issue**: The Notion API requires both `database_id` AND `data_source_id` for relation properties, but our DSL only accepted `database_id`.

**Error**:
```
validation_error: body.initial_data_source.properties.Project.relation.data_source_id should be defined
```

**Fix**:
- Updated `RelationConfiguration` companion methods to require both IDs
- Updated `RelationBuilder` to accept both parameters
- Updated DSL: `relation(name, databaseId, dataSourceId) { }`
- Fixed all affected unit tests

**Files Changed**:
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/databases/DatabaseRequests.kt`
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/databases/DatabaseRequestBuilder.kt`
- `src/test/kotlin/unit/databases/CreateDatabasePropertyTest.kt`
- `src/test/kotlin/unit/dsl/DatabaseRequestBuilderTest.kt`

### 2. Database Archive Uses `in_trash` Not `archived`
**Issue**: The 2025-09-03 API uses `in_trash` field for databases (not `archived` like pages).

**Error**:
```
Expected: true
Actual: false
```

**Fix**:
- Updated `ArchiveDatabaseRequest` to use `@SerialName("in_trash")`
- Updated test assertions to check `database.inTrash`
- Updated documentation to reflect the correct field

**Files Changed**:
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/databases/DatabaseRequests.kt`
- `src/main/kotlin/no/saabelit/kotlinnotionclient/api/DatabasesApi.kt`
- `src/test/kotlin/examples/DatabasesExamples.kt`
- `docs/databases.md`

## Test Coverage

### DatabasesExamples.kt - 8 Integration Tests
1. ✅ Retrieve a database
2. ✅ Create a simple database
3. ✅ Create database with rich schema (all property types)
4. ✅ Create database with relation (dual/bidirectional)
5. ✅ Archive a database (in_trash)
6. ✅ Get data source ID from created database
7. ✅ Create database and add initial pages
8. ✅ Check if database is in trash

All tests validated against live Notion API.

## Key Learnings

1. **Relations are complex**: Relations require BOTH database ID and data source ID in 2025-09-03 API
2. **Field name differences**: Databases use `in_trash`, pages use `archived`
3. **Container vs table**: Clear distinction between database containers and data sources is critical
4. **No database queries**: `databases.query()` doesn't exist - must use `dataSources.query()`

## Documentation Quality

- Clear API distinction (Databases vs Data Sources vs Pages)
- Comprehensive property type reference
- Migration guide from older API versions
- "What You CANNOT Do" section to prevent confusion
- Common mistakes section with ✅/❌ examples
- Related APIs cross-references

## Next Steps

Continue with Phase 2 of documentation strategy:
- [ ] `blocks.md` - Next priority
- [ ] `search.md`
- [ ] `comments.md`
- [ ] `users.md`
- [ ] `rich-text-dsl.md`

## Related Journal Entries

- `2025_10_05_03_Documentation_Strategy.md` - Overall documentation plan
- `2025_10_06_icon_cover_removal_issue.md` - Icon/cover removal limitation discovered during pages.md
