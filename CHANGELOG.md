# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.1] - 2026-05-02

### 🐛 Fixed

- **Rollup `show_original` deserialization**: Rollup properties using `show_original` return an array of page property items without the `id` field. Previously this caused a `SerializationException`; now all `PageProperty` subtypes default `id` to `""`, matching the API's behaviour for inline property values.

## [0.4.0] - 2026-04-15

### ⚠️ Breaking Changes

- **Notion API version**: Now targets **2026-03-11**
- **`archived` -> `inTrash`**: All models (`Page`, `Database`, `Block`, `Comment`, etc.) now expose `inTrash: Boolean` instead of `archived`
- **`archive()`/`unarchive()` -> `trash()`/`restore()`**: Renamed on `PagesApi`, `DatabasesApi`, `DataSourceRequestBuilder`, `UpdatePageRequestBuilder`
- **`appendChildren` position parameter**: The `after` parameter is replaced by a typed `BlockAppendPosition` sealed class: `AfterBlock(blockRef)`, `Start`, `End`

### ✨ Added

**Views API** — Full implementation of all 8 Views API endpoints (`client.views`):
- Create, retrieve, update, delete views and view queries
- Get view query results
- Typed `ViewConfiguration` sealed class for all 10 view types (table, board, list, calendar, timeline, gallery, chart, form, feed, AI)
- `GroupByConfig` for board/gallery/timeline/chart views

**Markdown Content API** (`client.markdown`):
- `retrieve(pageId)` — fetch a page's content as enhanced markdown
- `updateContent(pageId, ...)` — find/replace within existing content
- `replaceContent(pageId, markdown)` — replace all content with markdown
- Create pages with markdown via `CreatePageRequestBuilder.markdown()`
- Create comments with markdown

**New Block Types**:
- `heading_4` — with toggleable support
- `tab` — tab container blocks with icon support
- `meeting_notes` — read-only response model (renamed from `transcription`)

**Filter Enhancements**:
- Relative date filters: `pastWeek()`, `pastMonth()`, `nextWeek()`, `nextYear()`, etc.
- People filter: `containsMe()` / `doesNotContainMe()` (filter by the authenticated user)

**Icon Consolidation**:
- `PageIcon` renamed to `Icon` and moved to `models.base` — used consistently across pages, databases, callouts, and tabs

**Other Additions**:
- `Status` property creation in `DatabaseRequestBuilder`
- Property and option descriptions for database properties
- Native icon listing (`client.customEmojis.listNativeIcons()`)
- Custom emoji listing (`client.customEmojis.list()`)
- `Verification` property type (read model)
- `queryFirstPage()` on `DataSourcesApi` — single API call, exposes `hasMore`/`nextCursor`
- `retrieveChildrenFirstPage()` on `BlocksApi` — single API call

### 🐛 Fixed

- `dateMention(LocalDateTime, TimeZone)` was incorrectly sending the UTC instant representation — it now sends the local date-time as intended
- Database icon is now automatically propagated to the initial data source when creating a database

### 🔧 Changed

**Dependencies**:
- Kotlin 2.3.0, Ktor 3.4.0, kotlinx-datetime 0.7.1, Kotest 6.1.2

### 📊 Statistics

- **Test coverage**: 600+ unit tests

## [0.3.0] - 2026-02-08

### ✨ Added

**Pages API Enhancements**:
- **Move page**: Move pages between parents with `pages.move(pageId, parent)`, plus convenience methods `moveToPage()` and `moveToDataSource()`
- **Lock/unlock pages**: Control page editing with `lock()` and `unlock()` in the update page DSL
- **Page positioning**: Place new pages at specific positions with `position.afterBlock()`, `position.pageStart()`, and `position.pageEnd()`
- **Page templates**: Create pages from templates with `template.default()`, `template.byId()`, or `template.none()`
- **Erase content**: Clear page content with `eraseContent()` in the update page DSL

**Templates API**:
- **List templates**: Retrieve available templates for a data source with `dataSources.listTemplates(dataSourceId)`
- Automatic pagination handling for template listings
- Optional name filtering (case-insensitive substring match)

**Timestamp Filters**:
- **Filter by page timestamps**: Filter data source queries by `createdTime` and `lastEditedTime`
- Supports all date conditions: `equals()`, `before()`, `after()`, `onOrBefore()`, `onOrAfter()`
- Typed overloads for `LocalDate`, `LocalDateTime`, and `Instant`
- Relative date filters: `pastWeek()`, `pastMonth()`, `pastYear()`, `nextWeek()`, `nextMonth()`, `nextYear()`

### 🔧 Changed

**Dependencies** - Major version updates:
- Kotlin 2.2.21 → **2.3.0**
- Ktor 3.3.1 → **3.4.0**
- Kotest 6.0.4 → **6.1.2**
- kotlinx-datetime 0.6.2 → **0.7.1** (migrated to stable `kotlin.time.Instant`)
- kotlinx-serialization 1.8.1 → **1.10.0**
- logback 1.5.20 → **1.5.27**
- maven-publish plugin 0.34.0 → **0.36.0**

### ⚠️ Known Issues

- **Kotlin Notebooks**: v0.3.0 cannot be loaded in IntelliJ Kotlin Notebooks due to a binary incompatibility between the notebook kernel's bundled kotlinx-serialization and Ktor 3.4.0. Notebooks still work with v0.2.0. This is a kernel-level limitation that will be resolved in a future kernel update.

### 📊 Statistics

- **Test coverage**: 543+ unit tests (up from 514)
- **New test suites**: Page move/lock/position integration tests, Templates API unit and integration tests, timestamp filter serialization tests

## [0.2.0] - 2025-11-04

### ✨ Added

**Query Filters** - New property type filters for advanced querying:
- **Relation filter**: Filter by related pages with `relation("Property").contains(pageId)`, `doesNotContain()`, `isEmpty()`, `isNotEmpty()`
- **People filter**: Filter by users/assignees with `people("Property").contains(userId)`, `doesNotContain()`, `isEmpty()`, `isNotEmpty()`
- **Status filter**: Filter by workflow status with `status("Property").equals("Status")`, `doesNotEqual()`, `isEmpty()`, `isNotEmpty()`
- **Unique ID filter**: Filter by auto-incrementing IDs with numeric comparisons (`equals()`, `greaterThan()`, `lessThan()`, etc.)
- **Files filter**: Filter by attachment presence with `files("Property").isEmpty()`, `isNotEmpty()`

**Property Types**:
- **Place property**: Full support for location data with `PageProperty.Place` including:
  - Structured access to latitude/longitude coordinates
  - Name and address fields
  - Convenience accessors: `getPlaceProperty()`, `getPlaceAsString()`, `formattedLocation`
- **Unique ID property**: Enhanced with convenience accessors (`getUniqueIdProperty()`, properly integrated with `getPlainTextForProperty()`)

### 🐛 Fixed

- **Unknown property types**: Library now gracefully handles property types it doesn't recognize, returning `PageProperty.Unsupported` instead of failing deserialization

### 🔧 Changed

- **Dependencies**: Updated to latest versions of Kotlin, Ktor, and other dependencies
- **Gradle**: Updated Gradle wrapper to latest version

### 📊 Statistics

- **Test coverage**: 514+ unit tests (up from 481)
- **New test suites**: Integration tests for all new filter types

## [0.1.0] - 2025-10-10

### 🎉 Initial Release

This is the first public release of the Kotlin Notion Client library.

#### ✨ Features

**Core API Support**
- Complete implementation of Notion API 2025-09-03
- Full support for Pages, Blocks, Databases, Data Sources, Comments, Search, and Users APIs
- Type-safe Kotlin models for all Notion objects
- Coroutine-based async API using suspend functions

**Developer Experience**
- Type-safe DSL builders for creating pages, databases, and queries
- Rich Text DSL for formatting text with annotations
- Pagination helpers with Kotlin Flow support
- Rate limiting with automatic retry logic
- Comprehensive error handling

**Data Sources & Databases**
- Complete CRUD operations for databases and data sources
- Advanced query capabilities with type-safe filter and sort builders
- Relation properties with pagination support
- All database property types supported

**Content Management**
- Full block type support (paragraph, heading, list, code, etc.)
- Block children operations (append, retrieve, delete)
- Table blocks with row and cell management
- File upload support (single and multipart)

**Search & Users**
- Search by title with filtering
- User retrieval and listing
- Bot user information

**Testing & Quality**
- 481+ unit tests with comprehensive coverage
- Integration tests for real API verification
- Test fixtures using official Notion API samples

#### 📚 Documentation

- Complete API documentation for all endpoints
- Usage examples for common operations
- Testing guide with unit and integration test patterns
- Error handling guide
- Rich Text DSL documentation
- Pagination helpers documentation

#### 🔧 Technical Details

- **Language**: Kotlin 2.2+
- **HTTP Client**: Ktor
- **Serialization**: kotlinx.serialization
- **DateTime**: kotlinx-datetime
- **Testing**: Kotest

#### ⚠️ Known Limitations

- This library was developed with significant AI assistance (Claude Code)
- Some edge cases may not be fully covered
- Documentation examples should be verified against actual implementation
- See README for full transparency notice

#### 🙏 Acknowledgments

- Built using official Notion API documentation
- Developed with Claude Code assistance
- Inspired by official Notion SDK implementations

---

**Note**: This is an early release. Users should expect potential issues and are encouraged to report them via GitHub Issues.

[0.4.1]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/jsaabel/kotlin-notion-client/releases/tag/v0.1.0