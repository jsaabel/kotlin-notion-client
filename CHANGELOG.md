# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - Unreleased

### ⚠️ Breaking Changes

- **JVM target bumped to 21**: The library's Kotlin toolchain and JVM bytecode
  target are now Java 21 (was 17). Consumers compiling against this library
  must run a JDK 21+ toolchain.

- **`RateLimitConfig` surface trimmed to the load-bearing knobs**: the config is
  now `RateLimitConfig(sustainedRate, burstCapacity, maxRetries, retryBaseDelay,
  retryMaxDelay, jitterFactor)`. Removed in this pass:
  - the `RateLimitStrategy` enum and the `CONSERVATIVE` / `BALANCED` /
    `AGGRESSIVE` presets — redundant once the token bucket landed;
  - the `respectRetryAfter` flag — `Retry-After` is now always honoured on `429`
    (it is Notion's published contract);
  - `baseDelayMs` / `maxDelayMs` (`Long`, milliseconds) — replaced by
    `retryBaseDelay` / `retryMaxDelay` (`kotlin.time.Duration`);
  - the header-derived `RateLimitState` type and all `x-ratelimit-*` parsing —
    Notion does not emit those headers.

  **Migration**: `RateLimitConfig.BALANCED` → `RateLimitConfig()`;
  `baseDelayMs = 1000` → `retryBaseDelay = 1.seconds`;
  `maxDelayMs = 30000` → `retryMaxDelay = 30.seconds`; drop `strategy` and
  `respectRetryAfter`.

- **Unified retry pipeline for file uploads**: The file-upload subsystem no
  longer has its own retry mechanism. `EnhancedFileUploadApi.withRetry`, the
  `RetryConfig` class in `models.files`, and the `FileUploadOptions.retryConfig`
  field have all been **removed**. Uploads now route through the same
  rate-limit pipeline plugin as every other request, so there is a single
  retry configuration for the whole client.

  **Migration**: configure retry behaviour via `NotionConfig.rateLimitConfig`
  (a `RateLimitConfig` with `sustainedRate`, `burstCapacity`, `maxRetries`,
  `retryBaseDelay`, `retryMaxDelay`, and `jitterFactor`) instead of passing a
  per-upload `FileUploadOptions.retryConfig`. There is no compatibility shim —
  this is a clean break.

  ```kotlin
  // Before
  client.enhancedFileUploads.uploadFile(
      file,
      FileUploadOptions(retryConfig = RetryConfig(maxRetries = 5)),
  )

  // After — retry is configured once, on the client
  val config = NotionConfig(
      token = token,
      rateLimitConfig = RateLimitConfig(maxRetries = 5),
  )
  ```

  Note: chunked-upload mid-stream resume of partially-uploaded multipart
  chunks remains in `EnhancedFileUploadApi`. The pipeline plugin handles
  HTTP/network retry of a single request; it does not replace application-level
  multipart resume logic.

- **Truncated queries now throw**: When a `DataSourcesApi.query()` or
  `queryAsFlow()` auto-paginating call receives a response with
  `request_status.incomplete_reason == "query_result_limit_reached"` (the new
  Notion 10,000-row ceiling), the call now throws
  `NotionException.QueryResultLimitReached(partialResults, nextCursor,
  requestStatus)` instead of silently returning a truncated list. Single-page
  variants (`queryFirstPage`, `queryPagedFlow`) are unchanged — they expose
  `requestStatus` directly on their response wrapper.

### ✨ Added

**Rate-limit pipeline plugin** — proactive throttle + reactive retry on
every outbound request:
- `NotionRateLimit` is now a real `createClientPlugin` registered on Ktor's
  `Send` pipeline phase. Every request flows through it automatically — no
  more per-call `executeWithRateLimit { … }` wrapping. `SearchApi` and
  `FileUploadApi` (previously uncovered) gain rate-limit handling for free.
- New continuous-refill **token bucket** scoped per `NotionClient`:
  `sustainedRate` tokens/sec (default `3.0`, Notion's documented ceiling)
  refilled into a bucket clamped at `burstCapacity` (default `20`, ≈6.7s of
  headroom). Up to `burstCapacity` requests proceed immediately; further
  requests pace at `sustainedRate`. A single `Mutex` around the bucket math
  gives FIFO fairness across coroutines.
- `Retry-After` on `429` responses is now **load-bearing**: the plugin
  sleeps `Retry-After + 1s` before retrying instead of falling through to the
  exponential schedule. A `429` missing the header still gets the
  exponential schedule as defence-in-depth.
- Retry classifier is now typed `HttpStatusCode` / exception-class checks.
  Retried automatically: `429`, `502` / `503` / `504`, and the `IOException`
  family (`SocketTimeoutException`, `ConnectException`,
  `UnknownHostException`). Plain `500`, other `4xx`, and
  `CancellationException` propagate immediately.

**Multi-value filters** — array form for `select` / `status` /
`multi_select`:
- `SelectFilterBuilder`, `StatusFilterBuilder`, `MultiSelectFilterBuilder`
  gain `vararg values: String` overloads on `equals` / `doesNotEqual` (select,
  status) and `contains` / `doesNotContain` (multi_select).
- Single-value call sites continue to compile and serialize unchanged. The
  new `FilterValues` value class emits a JSON string for size==1 and a JSON
  array for size>1, matching the Notion 2026-04-17 changelog.

**Files & media `FileUpload` variant** — attach freshly-uploaded files to a
Files & media page property:
- `FileObject.FileUpload(fileUpload: FileUploadReference, name: String? = null)`
  sealed variant (`@SerialName("file_upload")`).
- Companion helpers `FileObject.upload(id, name?)` and
  `FileObject.external(name, url)`.
- New `FilesBuilder` DSL with `upload()` / `external()` / `existing()` /
  `add()` exposed via `PagePropertiesBuilder.files()` (DSL block, vararg, and
  `List<FileObject>` overloads).
- `DatabasePropertiesBuilder.files()` schema method so the property type can
  be created programmatically (previously absent — see Fixed below).
- `FileUploadApi.sendFileUpload(FileUpload, ByteArray, partNumber?)` overload
  that threads the creation-time content type onto the multipart part
  automatically, so the round-trip for non-text uploads no longer requires
  manually re-specifying the content type.

**Comments update / delete**:
- `CommentsApi.update(commentId, request)` — `PATCH /v1/comments/{id}`. Both
  the data-class form and an `update(commentId) { … }` DSL overload are
  supported (content-only surface mirroring `create`'s XOR pattern).
- `CommentsApi.delete(commentId)` — `DELETE /v1/comments/{id}`. Returns the
  deleted comment object.
- Non-DLP integrations can only modify or delete comments they themselves
  created.

**Rich text → HTML** — `List<RichText>?.toHtml(): String?`:
- New extension in `it.saabel.kotlinnotionclient.utils` that renders a
  rich-text array to HTML with bold/italic/strikethrough/code/underline,
  paragraph splitting (single newline → `<br>`, blank line → wrapped in
  `<p>…</p>`), text-link hrefs (rendered as
  `<a href="…" rel="nofollow noreferrer noopener">…</a>` with the
  Notion-internal hrefs dropped), and HTML escaping on all plain-text
  segments.
- Null / empty / blank input returns `null`.
- Colours, mention-aware rendering, and equations are intentionally rendered
  as escaped plain text in this first cut — full support is deferred to
  v0.6.0+ alongside a `RenderOptions` object.

**Truncated-query surface** — `RequestStatus` model and exception:
- New `RequestStatus(type, incompleteReason)` data class with `isComplete` /
  `isIncomplete` helpers and the documented constants (`TYPE_COMPLETE`,
  `TYPE_INCOMPLETE`, `REASON_QUERY_RESULT_LIMIT_REACHED`).
- Surfaced on `DataSourceQueryResponse`, `ViewQuery`, and `ViewQueryResults`.
- `DataSourcesApi.query` and `queryAsFlow` throw
  `NotionException.QueryResultLimitReached(partialResults, nextCursor,
  requestStatus)` when Notion's 10k pagination ceiling is hit, so callers can
  recover what was collected and decide whether to resume.

**Integer-aware number rendering**:
- `getPlainTextForProperty()` on `Number`, `FormulaResult.NumberResult`, and
  `RollupResult.NumberResult` now drops the trailing `.0` for whole-valued
  doubles (e.g. `95.0` → `"95"`, `2.5` → `"2.5"` unchanged). NaN / ±Infinity
  and values outside the safe-integer range fall back to `Double.toString()`
  to avoid lossy coercion.

**New parent type** — `Parent.AgentParent(agentId)`:
- Round-trips the new `{ "type": "agent_id", "agent_id": "..." }` parent shape
  introduced by Notion on 2026-05-11.
- Deserialize-only / read-path support — Notion sets `agent_id` parents
  server-side on agent instruction pages and the blocks that make them up.
  `Parent.id` returns the agent ID for this variant.

### 🐛 Fixed

- **`SearchApi` and `FileUploadApi` were bypassing the rate limiter entirely**.
  Both endpoints now flow through the same pipeline plugin as the rest of the
  API.
- **`Retry-After` is no longer dead config**: the value was previously modelled
  but never consulted at runtime; the plugin now actually reads it on every
  `429` response.
- **No wasted backoff sleep on exhausted retries**: when every attempt fails and
  `maxRetries` is reached, the client now throws/returns immediately instead of
  sleeping one final, unused backoff interval (~8s with default settings) before
  giving up.
- **Files property schema gap**: `CreateDatabaseProperty.Files` variant was
  missing entirely (the schema-side equivalent of `People`), which blocked
  creating a Files & media property programmatically. Now present.
- **Multipart content type defaults**: an omitted multipart part content type
  defaults to `text/plain`, causing a `create(application/json)` +
  `sendFileUpload(id, bytes)` flow to be rejected with a content-type
  mismatch. The new `sendFileUpload(FileUpload, ByteArray, …)` overload
  threads the creation-time type onto the part automatically; the id-based
  overload's docstring was corrected to state that callers must pass the
  same content type used at creation for non-text files.
- **Brittle string-matching error classifier replaced with typed
  `HttpStatusCode` / exception-class checks**, eliminating the latent
  brittleness of inspecting `error.message`.
- **`maxRetries` KDoc spells out the off-by-one**: `maxRetries = 3` permits up
  to **4** HTTP calls (1 initial + 3 retries). `0` disables retrying.
- **Unreachable `RateLimitDecision.Proceed` branch removed**.

### 🔧 Changed

**Dependencies**:
- Kotlin **2.3.0 → 2.3.21**
- Ktor **3.4.0 → 3.5.0**
- kotlinx-datetime **0.7.1 → 0.8.0**
- (plus other minor bumps in the same batch)

**Rate-limiting architecture** — internal restructuring captured under
Breaking / Added / Fixed above. Net code impact: `ratelimit/` collapsed from
~600 LOC across three files to 286 LOC across three files
(`NotionRateLimit.kt`, `RateLimitConfig.kt`, `TokenBucket.kt`), with
`BackoffCalculator`, `RateLimitState`, and `RetryAttempt` inlined into the
plugin.

### 📊 Statistics

- **Test coverage**: 864 unit tests (up from 600+ in v0.4.0)

## [0.4.2] - 2026-05-03

### 🐛 Fixed

- **DatabaseProperty forward compatibility**: Unknown database property types (e.g., `"button"`) now deserialize as `DatabaseProperty.Unknown` with raw JSON preserved, instead of crashing with a `SerializationException`. This mirrors the existing `PageProperty.Unknown` fallback pattern.

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

[0.5.0]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.4.2...v0.5.0
[0.4.2]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.4.1...v0.4.2
[0.4.1]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/jsaabel/kotlin-notion-client/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/jsaabel/kotlin-notion-client/releases/tag/v0.1.0