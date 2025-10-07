# Notion API 2025-09-03 Migration Summary

## Core Concept Changes

### Database → Data Source Separation

**Before**: Database = single table with properties
**After**: Database = container with 1+ data sources (each with own properties)

```
Database (container)
├── Data Source 1 (properties, schema)
│   ├── Page (row)
│   └── Page (row)
└── Data Source 2 (different properties)
    ├── Page (row)
    └── Page (row)
```

### Key ID Changes

- **Database ID**: Now identifies the container (same as URL)
- **Data Source ID**: Identifies specific table within database (not in URL)
- **View ID**: Still in URL query param (not exposed in API)

## Critical API Changes

### 1. Retrieve Database

**Old (2022-06-28)**: Returns properties and schema
**New (2025-09-03)**: Returns `data_sources` array

```json
{
  "object": "database",
  "id": "...",
  "data_sources": [
    {"id": "...", "name": "Tasks DB"},
    {"id": "...", "name": "Projects DB"}
  ]
}
```

### 2. Retrieve Data Source (NEW)

Replaces old database retrieve for getting schema:

```
GET /v1/data_sources/{data_source_id}
```

Returns object with `"object": "data_source"` and all properties.

### 3. Query Database → Query Data Source

**Old**: `POST /v1/databases/{database_id}/query`
**New**: `POST /v1/data_sources/{data_source_id}/query`

Same query structure, different endpoint.

### 4. Create Database

Properties now nested under `initial_data_source`:

```json
{
  "initial_data_source": {
    "properties": {
      "Name": {"title": {}},
      "Status": {"select": {...}}
    }
  },
  "parent": {"type": "page_id", "page_id": "..."},
  "title": [...]
}
```

Database-level: `title`, `icon`, `cover`, `parent`
Data source-level: `properties`

### 5. Update Database vs Update Data Source

**Update Database** (`PATCH /v1/databases/{database_id}`):
- `parent` - move database to different page
- `title`, `icon`, `cover` - database container settings
- `is_inline`, `in_trash`

**Update Data Source** (`PATCH /v1/data_sources/{data_source_id}`):
- `properties` - schema changes
- `title`, `description` - data source specific
- `in_trash` - archive specific data source

### 6. Create Page Parent

**Old**:
```json
{"parent": {"type": "database_id", "database_id": "..."}}
```

**New**:
```json
{"parent": {"type": "data_source_id", "data_source_id": "..."}}
```

### 7. Relation Properties

Now include both IDs in read/write:

```json
"relation": {
  "database_id": "...",
  "data_source_id": "...",
  "dual_property": {...}
}
```

### 8. Search API

**Old**: Filter by `"database"`
**New**: Filter by `"data_source"`

Results return `"object": "data_source"` instead of `"object": "database"`.

## Parent Relationships (2025-09-03)

```
Page → Database → Data Source → Page (row)
```

**Page object**:
```json
{"parent": {"type": "data_source_id", "data_source_id": "..."}}
```

**Data Source object**:
```json
{
  "parent": {"type": "database_id", "database_id": "..."},
  "database_parent": {"type": "page_id", "page_id": "..."}
}
```

## Error Handling

### Multiple Data Sources Error

When old API version hits multi-source database:

```json
{
  "code": "validation_error",
  "status": 400,
  "message": "Databases with multiple data sources are not supported in this API version.",
  "additional_data": {
    "error_type": "multiple_data_sources_for_database",
    "database_id": "...",
    "child_data_source_ids": ["...", "..."],
    "minimum_api_version": "2025-09-03"
  }
}
```

## Webhook Changes

### Event Renames

| Old (2022-06-28) | New (2025-09-03) |
|------------------|------------------|
| `database.content_updated` | `data_source.content_updated` |
| `database.schema_updated` | `data_source.schema_updated` |
| N/A | `data_source.created` (NEW) |
| N/A | `data_source.moved` (NEW) |
| N/A | `data_source.deleted` (NEW) |
| N/A | `data_source.undeleted` (NEW) |
| `database.created` | (unchanged - container) |
| `database.moved` | (unchanged - container) |

### Parent Data in Events

All page/data source events now include:

```json
{
  "data": {
    "parent": {
      "id": "...",
      "type": "database",
      "data_source_id": "..."  // NEW field
    }
  }
}
```

## Implementation Checklist for Kotlin Client

### Model Changes

- [ ] Create `DataSource` data class (similar to `Database`)
- [ ] Add `dataSources: List<DataSourceRef>` to `Database` model
  - `DataSourceRef` = `{id: String, name: String}`
- [ ] Update `PageParent` to support `data_source_id` type
- [ ] Update `RelationPropertyConfig` to include `data_source_id`
- [ ] Update `SearchFilter` enum: `database` → `data_source`

### API Endpoints

**New DataSourcesApi**:
- [ ] `retrieve(dataSourceId: String): DataSource`
- [ ] `query(dataSourceId: String, filter?, sorts?, pagination?): PaginatedList<Page>`
- [ ] `create(databaseId: String, properties, title?, description?): DataSource`
- [ ] `update(dataSourceId: String, properties?, title?, description?, in_trash?): DataSource`

**Modified DatabasesApi**:
- [ ] `retrieve(databaseId: String): Database` - now returns container with `data_sources`
- [ ] `create(...)` - add `initial_data_source` parameter
- [ ] `update(...)` - remove `properties`, keep container-level fields

**Modified PagesApi**:
- [ ] Update `create()` to accept `data_source_id` parent
- [ ] **BREAKING**: Remove support for `database_id` parent (2025-09-03 only)

**Modified SearchApi**:
- [ ] Update filter to accept `"data_source"` value (remove `"database"`)
- [ ] Handle `data_source` objects in results

### Client Configuration

- [ ] Set `Notion-Version: 2025-09-03` header in HTTP client (hardcoded)
- [ ] No version configuration needed (always 2025-09-03)

### DSL Builders

**New**:
- [ ] `CreateDataSourceRequestBuilder` (for adding sources to existing DB)
- [ ] `UpdateDataSourceRequestBuilder`
- [ ] Update `CreateDatabaseRequestBuilder` to use `initial_data_source`

**Modified**:
- [ ] Update parent builders to support `data_source_id` (remove `database_id`)

### Testing

**Unit Tests**:
- [ ] DataSource model serialization/deserialization
- [ ] Parent types with `data_source_id`
- [ ] Database model with `data_sources` array
- [ ] All new DSL builders

**Integration Tests** (NEW - specifically requested):
- [ ] Create database with initial data source
- [ ] Retrieve database and access data sources
- [ ] Query data source (not database)
- [ ] Create page with data_source_id parent
- [ ] Update data source properties
- [ ] Create second data source for existing database
- [ ] Update database container (move parent, change icon)
- [ ] Search for data sources
- [ ] Relation properties with data source IDs

**Mock Responses**:
- [ ] Add 2025-09-03 sample responses to `src/test/resources/api/`
- [ ] Update TestFixtures with data source samples
- [ ] Add MockResponseBuilder helpers for data sources

### Migration Utilities

Helper functions for common patterns:
```kotlin
// Get first/default data source from database
suspend fun Database.getDefaultDataSource(): DataSource

// Get all data sources for a database
suspend fun NotionClient.getDataSources(databaseId: String): List<DataSource>
```

## Notes

- **No backward compatibility**: Client will only support 2025-09-03 API version
- **Breaking change**: Existing code using `database_id` parents will need updates
- **No changes** to: Blocks, Comments, File Uploads, Users, Authentication APIs
- **Wikis**: Won't support multiple data sources initially
- **Permissions**: Managed at database level, not per data source
- **Database mentions in rich text**: Still reference database, not data source

## Next Steps

1. Update models (`Database`, `DataSource`, `PageParent`)
2. Implement `DataSourcesApi` with full CRUD
3. Update `DatabasesApi` for 2025-09-03 structure
4. Update `PagesApi` to use `data_source_id` parents
5. Add comprehensive integration tests for data source behavior
6. Update DSL builders
