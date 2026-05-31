# Next Release Planning — Notion API changelog since v0.4

**Date:** 2026-05-30
**Status:** Decisions resolved — ready to break into implementation tasks
**Scope:** Notion API changelog entries from 2026-04-17 → 2026-05-15
**Target version:** v0.5.0 (minor bump — new parent type, new comment endpoints, new filter-DSL shape, rich-text → HTML)

## TL;DR

Target **v0.5.0**. Ten work items spanning three groups:

- **API changelog items (§1–§6)**: §1 (`insert_content`) skipped as
  deprecated; §2 docs-only; §3 `agent_id` parent type **shipped
  (2026-05-30)**; §4 cursor audit **closed — already opaque end-to-end,
  no work needed (2026-05-30)**; §5 pagination cap → throws
  `QueryResultLimitReached` on the auto-paginating path — **shipped
  (2026-05-30)**; §6a comment update/delete (mirrors `create` pattern)
  — **shipped (2026-05-30)**; §6b multi-value filters as vararg
  overloads — **shipped (2026-05-30)**; §6c person filter cleanup —
  **verified no-op (2026-05-30)**.
- **Carry-overs (§7–§8)**: §7 rate-limiting — **full overhaul** (all 9
  ranked defects, sequenced last); §8 files-property `FileUpload`
  variant — **shipped (2026-05-30)**.
- **Consumer conveniences (§9–§10)**: §9 rich-text → HTML — **shipped
  (2026-05-30)** (annotations + links + paragraph splitting + escape;
  colours/mentions/equations deferred to v0.6.0+); §10 integer-aware
  number rendering across Number / Formula / Rollup — **shipped
  (2026-05-30)**.

All decisions resolved (see "Decisions log" at bottom). Items are
independent enough to land in any order. Phasing table proposes a
sequence.

---

## API changelog items

### 1. Markdown insertion positions (2026-05-15) — **SKIPPED**

> The Update page markdown endpoint now supports `insert_content.position`,
> letting integrations prepend markdown to the start of a page or explicitly
> append it to the end without rewriting the full page.

- **Decision: do not implement.** Notion's live reference marks
  `insert_content` itself as legacy/deprecated even while the changelog
  announces a new `position` field on it. `update_content` and
  `replace_content` (both already in `MarkdownApi`) are the recommended path
  forward. Adding client surface to a deprecated operation is a smell; if a
  concrete user need surfaces, revisit then — possibly as a `prepend/append`
  helper built on `update_content` rather than exposing `insert_content`.
- **Reference:** [`_next_release_docs/01_update_page_markdown.md`](_next_release_docs/01_update_page_markdown.md)

---

### 2. Developer portal & personal access tokens (2026-05-12)

> The new Developer portal is now available... Personal access tokens (PATs)
> are user-scoped tokens for scripts, CLI workflows, Workers, and trusted
> tools...

- **Client impact**: **None at the protocol level** — PATs are still
  `Authorization: Bearer ...` strings, indistinguishable from integration
  tokens to the API client.
- **Required work**:
  - Documentation note in README and possibly a notebook example: PATs are
    supported transparently as auth tokens; explain when to use one vs. an
    integration token.
  - Consider renaming any docs that exclusively call the token "integration
    token" to be more neutral ("API token" / "PAT or integration token").
- **No code changes expected.** Confirm by reading the linked docs.

---

### 3. `agent_id` parent type (2026-05-11) — **DONE (2026-05-30)**

> Pages and blocks parented by an agent now serialize their parent as
> `{ "type": "agent_id", "agent_id": "..." }` instead of being rejected or
> rewritten.

- **Status**: Implemented on branch `worktree-task-01-agent-id-parent`. See
  [`_task_01_agent_id_parent.md`](_task_01_agent_id_parent.md) for the
  per-task journal (Results section).
- **Shipped**:
  - `Parent.AgentParent(agentId: String)` added (type = `"agent_id"`).
  - `Parent.id` accessor extended to return `agentId` for `AgentParent`.
  - `ParentSerializer.selectDeserializer` and `ParentSurrogate` both
    extended with the new variant.
  - `unit/base/ParentAgentIdTest.kt` — 3 tests (decode, encode, decode →
    encode → decode round-trip). All pass. Full unit suite green.
- **Reference:** [`_next_release_docs/03_parent_object.md`](_next_release_docs/03_parent_object.md)
- **Confirmed from fetched docs**: read-only / system-assigned; appears on
  "agent instruction pages and the blocks that make them up." No
  documented path to set `agent_id` on create — modelled as a
  deserialize-only variant (no DSL/builder surface; `@Serializable` just
  lets it round-trip if encountered).
- **Skipped from this changelog entry**: the Query meeting notes endpoint
  (`POST /v1/blocks/meeting_notes/query`) — out of scope, no user need yet.
- **Tangent surfaced by fetched docs**: `data_source_id` parents are
  documented as carrying *both* `data_source_id` and `database_id`. Our
  current `Parent.DataSourceParent` may only carry one of these — worth a
  quick check. (Tracking as an aside; not in scope for this release.)

---

### 4. Pagination cursor reliability (2026-04-22) — **CLOSED**

> Pagination cursors now embed a session identifier... The `start_cursor`
> parameter now accepts opaque string values in addition to UUIDs... cursors
> should always be treated as opaque.

- **Status**: Audit complete on 2026-05-30 — **PASS, no code changes
  required for v0.5.0**. Full per-endpoint findings in
  [`_task_02_cursor_audit.md`](_task_02_cursor_audit.md).
- **Audit summary**:
  - All 10 paginated endpoints already treat `startCursor` / `nextCursor`
    as `String?` end-to-end (`DataSourcesApi.query`,
    `BlocksApi.retrieveChildrenPage`, `SearchApi.search`, `ViewsApi.list`,
    `ViewsApi.getQueryResults`, `UsersApi.list`,
    `CommentsApi.retrievePage`, `PagesApi.retrievePropertyItems`,
    `CustomEmojisApi.list`, `DataSourcesApi.listTemplatesSinglePage`).
    `FileUploadApi` is not paginated.
  - Wire transport is either query-param append, query-string
    interpolation, or JSON-body via `@SerialName("start_cursor")` — no
    custom serializers, no encoding beyond Ktor's standard handling.
  - Hidden-coercion grep across `src/main` for `UUID.fromString`,
    `toUuid`, UUID regex, and `trim`/`replace`/hyphen-stripping near
    `cursor` — **zero hits**.
  - Central pagination helpers (`utils/Pagination.kt`) operate on
    `String?` exclusively; the `PaginatedResponse<T>` contract prevents
    callers from sneaking in UUID coercion.
- **Regression test**: Not needed. Existing unit tests already exercise
  non-UUID cursor strings (`"some-complex-cursor-string-123"`,
  `"cursor-123"`, `"cursor1"`, etc.) across `RetrieveCommentsRequestBuilderTest`,
  `UsersApiTest`, `DataSourcesApiTest`, and `PaginationTest` — any future
  introduction of UUID validation would fail these tests.
- **Optional follow-up (not in scope)**: a one-line doc comment near
  `Pagination.kt:26` and `:74` clarifying that cursors are opaque and
  must not be parsed or validated, to deter future regressions.

---

### 5. Pagination depth limit + `request_status` field (2026-04-20) — **DONE (2026-05-30)**

> The Query a data source, Create a view query, and Get view query results
> endpoints now enforce a maximum pagination depth of 10,000 results per
> query... the response includes a new `request_status` field:
> `{ "request_status": { "type": "incomplete", "incomplete_reason":
> "query_result_limit_reached" } }`

- **Status**: Implemented on `main` (no worktree — landed directly). All
  unit tests green.
- **Shipped**:
  - `models/base/RequestStatus.kt` — new `RequestStatus(type,
    incompleteReason)` data class with `isComplete` / `isIncomplete`
    helpers and constants for the documented values
    (`TYPE_COMPLETE`, `TYPE_INCOMPLETE`,
    `REASON_QUERY_RESULT_LIMIT_REACHED`).
  - `DataSourceQueryResponse`, `ViewQuery`, `ViewQueryResults` each gain
    a nullable `requestStatus: RequestStatus?` field. KDoc on
    `ViewQuery.totalCount` flags the truncation trap (cached size, not
    true row count).
  - `NotionException.QueryResultLimitReached(partialResults: List<Page>,
    nextCursor: String?, requestStatus: RequestStatus)` — bound to
    `List<Page>` since that's the only auto-paginating path that hides
    the raw response.
  - `DataSourcesApi.query` (auto-paginating `List<Page>` path) throws
    `QueryResultLimitReached` as soon as any page comes back with
    `requestStatus.isIncomplete`, carrying everything collected so far
    + the page's `nextCursor`.
  - `DataSourcesApi.queryAsFlow` rewritten as a custom `flow {}` that
    tracks emitted items and throws the same exception, terminating the
    flow. `queryPagedFlow` and `queryFirstPage` are unchanged — they
    return the raw response, so callers already see `requestStatus`.
  - `unit/api/RequestStatusTest.kt` — 9 tests: model round-trip,
    response-model round-trip (DataSourceQueryResponse, ViewQuery,
    ViewQueryResults), happy path through `query()`, throw on first
    incomplete page, throw mid-stream collects everything emitted so far,
    `queryAsFlow` throw behaviour. All pass; full unit suite green.
- **Skipped from this slice** (not in scope for v0.5.0 — track as
  follow-ups if a user need surfaces):
  - README / notebook documentation note about the 10k limit and how to
    catch the exception. Cheap to add but wanted to confirm the surface
    first.
  - Integration test against a real ≥10k-row data source to verify that
    Notion actually emits `request_status` on the *data source* query
    endpoint (the changelog lists it but the data-source filter reference
    doesn't enumerate the field).
- **References:**
  [`_next_release_docs/05_create_view_query.md`](_next_release_docs/05_create_view_query.md),
  [`_next_release_docs/05_get_view_query_results.md`](_next_release_docs/05_get_view_query_results.md)
- **Ties into Idea #10** (limited-result querying / opt-out from
  auto-pagination) — the new exception surface is friendly to a future
  "collect up to N then stop" knob if we add one.

---

### 6. Update/Delete comments GA + multi-value filters + person filters (2026-04-17)

#### 6a. Update / Delete comment endpoints — now GA — **DONE (2026-05-30)**

> The Update a comment (PATCH /v1/comments/:comment_id) and Delete a comment
> (DELETE /v1/comments/:comment_id) endpoints are now generally available.
> Non-DLP integrations can only modify or delete comments they created.

- **Status**: Shipped via GitHub issues
  [#1](https://github.com/jsaabel/kotlin-notion-client/issues/1) (update)
  and [#2](https://github.com/jsaabel/kotlin-notion-client/issues/2)
  (delete), both closed 2026-05-30.
- **References:**
  [`_next_release_docs/06a_update_a_comment.md`](_next_release_docs/06a_update_a_comment.md),
  [`_next_release_docs/06a_delete_a_comment.md`](_next_release_docs/06a_delete_a_comment.md)
- **Shipped**:
  - `UpdateCommentRequest` + `UpdateCommentRequestBuilder` (content-only
    surface: `content { … }` / `richText { … }` / `markdown("…")`) with
    runtime XOR mirroring `create`.
  - `CommentsApi.update(commentId, request)` and DSL overload
    `update(commentId) { … }` — `PATCH /v1/comments/{id}`.
  - `CommentsApi.delete(commentId)` — `DELETE /v1/comments/{id}`, returns
    the deleted comment object.
  - Unit tests (builder XOR; API success/XOR/404 for update; success/404
    for delete) and integration scenario extended to full
    create → reply → update → update → delete → retrieve lifecycle.
  - `CommentsExamples.kt` gained update + delete examples.

#### 6b. Multi-value filters for select / status / multi_select — **DONE (2026-05-30)**

> Database and data source filters now accept an array of values for
> `equals` / `does_not_equal` on select and status properties, and for
> `contains` / `does_not_contain` on multi_select properties...

- **Status**: Shipped on `main` in commit
  [`c7c05ab`](https://github.com/jsaabel/kotlin-notion-client/commit/c7c05ab)
  ("feat(filters): multi-value equals/contains for select, status,
  multi_select (§6b)"), closing
  [#4](https://github.com/jsaabel/kotlin-notion-client/issues/4). See
  [`_task_06b_multi_value_filters.md`](_task_06b_multi_value_filters.md)
  for the per-task journal.
- **Shipped**:
  - New `FilterValues` value class
    (`models/datasources/FilterValues.kt`) with a custom `KSerializer`:
    size==1 → JSON string (byte-identical to the legacy wire shape),
    size>1 → JSON array. Deserialization accepts both shapes; empty
    throws `IllegalArgumentException`.
  - `SelectCondition`, `StatusCondition`, `MultiSelectCondition` now use
    `FilterValues?` for the four affected slots
    (`equals` / `doesNotEqual` on select & status;
    `contains` / `doesNotContain` on multi_select).
  - `SelectFilterBuilder`, `StatusFilterBuilder`, `MultiSelectFilterBuilder`
    gain `vararg values: String` overloads with
    `require(values.isNotEmpty())`. Single-value call sites compile and
    serialize unchanged (strict superset).
  - `FilterValuesSerializerTest` (string ↔ array round-trips, empty
    throws) and `Select/Status/MultiSelectFilterBuilderMultiValueTest`
    (per-builder DSL). Query DSL integration spec extended with a
    seeded status property and four new cases (multi-value
    select/status/multi_select + single-value back-compat). All unit
    tests green.
- **References:**
  [`_next_release_docs/06b_filter_data_source_entries.md`](_next_release_docs/06b_filter_data_source_entries.md),
  [`_next_release_docs/06b_post_database_query_filter.md`](_next_release_docs/06b_post_database_query_filter.md),
  [`_next_release_docs/06b_quick_filters.md`](_next_release_docs/06b_quick_filters.md)
- **Skipped from this slice** (not in scope for v0.5.0):
  - Extending the same `FilterValues` mechanism to `ViewsApi`
    `quick_filters` configuration. The bonus surface area noted in the
    decision is still open — track as a follow-up if a user need
    surfaces.

#### 6c. Person filters round-trip cleanly

> Person filters set via the API also now round-trip cleanly on read without
> extra combinator nesting.

- **Status (verified 2026-05-30): no-op — no workaround in tree.**
- **What was checked**:
  - `PeopleCondition` (`DataSourceQuery.kt:295-304`) is a plain
    `@Serializable data class` with `contains` / `does_not_contain` /
    `is_empty` / `is_not_empty`. No custom serializer.
  - `DataSourceFilter.people` (`DataSourceQuery.kt:85-86`) is a straight
    sibling slot to every other property condition.
  - `PeopleFilterBuilder` (`DataSourceQueryBuilder.kt:526-548`) builds a
    flat `DataSourceFilter(property=..., people=PeopleCondition(...))`.
    Nothing ever auto-wraps a person filter in `or`.
  - Grepped for `workaround` / `HACK` / `TODO` / `FIXME` / `wrap` near
    people/person code — zero matches.
  - Existing tests (`PeopleMeFilterTest`, `DatabaseQueryFiltersTest`,
    `NewFilterTypesIntegrationTest`) all assume the flat
    `{ "property": "...", "people": { ... } }` shape — which matches the
    now-correct round-trip shape per the changelog.
- **Conclusion**: Notion fixed read-path nesting that we apparently never
  compensated for client-side, so §6c lands as zero code change. The
  phasing-table entry can be ticked off without a commit.
- **Optional follow-up**: add a tiny round-trip unit test that deserializes
  a flat `{"property":"Assignee","people":{"contains":"<uuid>"}}` fixture
  into `DataSourceFilter` and re-serializes byte-identical — cheap
  regression guard that the round-trip stays clean.

---

## Additional release scope

### 7. Rate-limiting overhaul — **FULL**

- **See:** [`2026_05_30_01_RateLimiting_Diagnosis.md`](2026_05_30_01_RateLimiting_Diagnosis.md)
  — full diagnosis, defect ranking, and suggested investigation steps.
- **Decision: full overhaul.** All 9 ranked defects in scope, including
  the largest architectural moves:
  - Concurrency model (global semaphore or token bucket sized to Notion's
    ~3 req/s ceiling) so parallel batches throttle smoothly instead of
    failing with `MAX_RETRIES_EXCEEDED`.
  - Convert `NotionRateLimit` from an opt-in wrapper into a real Ktor
    pipeline interceptor so response headers are available on every 429
    and 5xx / network errors can be retried in one place.
  - Read `Retry-After` and `x-ratelimit-*` headers at runtime
    (`respectRetryAfter` becomes functional).
  - Wrap `SearchApi` and `FileUploadApi` (coverage gap).
  - Unify with `EnhancedFileUploadApi.withRetry` so there is one retry
    config across the client.
  - Fix wasted final delay, brittle string-matching classifier, document
    `maxRetries` semantics, remove unreachable `RateLimitDecision.Proceed`
    branch.
- **Risk acknowledged**: this is the largest single workstream in v0.5.0;
  may push the release date. Sequencing puts it last in the phasing table
  so the smaller items can ship even if this one slips.
- **⚠️ BREAKING (release-notes flag)**: the "unify with
  `EnhancedFileUploadApi.withRetry`" sub-item is a **clean break, no shim**
  (per Q11). `EnhancedFileUploadApi.withRetry`, `models.files.RetryConfig`,
  and `FileUploadOptions.retryConfig` are **removed**; uploads now retry via
  the shared pipeline plugin configured through `NotionConfig.rateLimitConfig`.
  Migration path `FileUploadOptions.retryConfig` → `NotionConfig.rateLimitConfig`.
  Documented in `CHANGELOG.md` under `[0.5.0]`. Must be called out loudly in
  the v0.5.0 release notes. (Issue
  [#18](https://github.com/jsaabel/kotlin-notion-client/issues/18).)

---

### 8. Files-property `FileObject.FileUpload(id)` variant — **DONE (2026-05-30)**

- **Status**: Shipped on `main` across five commits, closing issues
  [#6](https://github.com/jsaabel/kotlin-notion-client/issues/6)
  (variant) and [#7](https://github.com/jsaabel/kotlin-notion-client/issues/7)
  (DSL). See
  [`_task_08_files_property_fileupload_variant.md`](_task_08_files_property_fileupload_variant.md)
  for the per-task journal and
  [`2026_05_30_02_Files_Property_Gap_Analysis.md`](2026_05_30_02_Files_Property_Gap_Analysis.md)
  for the original gap analysis.
- **Shipped**:
  - [`d736127`](https://github.com/jsaabel/kotlin-notion-client/commit/d736127)
    `FileObject.FileUpload(fileUpload: FileUploadReference, name: String? = null)`
    sealed variant (`@SerialName("file_upload")`) reusing the existing
    `FileUploadReference` from `models/files`. Companion helpers
    `FileObject.upload(id, name?)` and `FileObject.external(name, url)`.
    Encode/decode round-trips + response-shape regression covered by
    unit tests; request-body JSON fixture committed.
  - [`4d6dfd9`](https://github.com/jsaabel/kotlin-notion-client/commit/4d6dfd9)
    `FilesBuilder` with `@FilesDslMarker` exposing `upload()` /
    `external()` / `existing()` (routes both `FileData.External` and
    `FileData.Uploaded`) / `add()` escape hatch.
    `PagePropertiesBuilder.files()` in three forms: DSL block, vararg
    `FileObject`, and `List<FileObject>` — all backed by `FilesValue`.
    `DatabasePropertiesBuilder.files()` schema method so a Files &
    media property can be created programmatically. Unit tests cover
    each builder method, `existing()` routing for both subtypes, and
    JSON-equivalence of the three overloads. Live scenario added to
    `MediaIntegrationTest`: create database with `Attachments`
    property → attach two uploads via DSL → re-fetch → update with
    mixed `existing()` + `external()` array → re-fetch.
  - [`c479f2c`](https://github.com/jsaabel/kotlin-notion-client/commit/c479f2c)
    Fix: `CreateDatabaseProperty.Files` variant was missing (empty
    config object, mirroring `People`), which broke compilation of the
    builder work.
  - [`3a2656b`](https://github.com/jsaabel/kotlin-notion-client/commit/3a2656b)
    + [`3e9449b`](https://github.com/jsaabel/kotlin-notion-client/commit/3e9449b)
    Fix surfaced by the integration scenario: an omitted multipart
    part content-type defaults to `text/plain`, so a
    `create(application/json)` + `sendFileUpload(id, bytes)` flow was
    rejected as a content-type mismatch. Corrected the integration
    test to pass the matching type; added a robust
    `sendFileUpload(FileUpload, ByteArray, Int?)` overload that
    threads the creation-time content type onto the multipart part
    automatically; corrected the docstring on the id-based overload to
    state that callers must pass the same content type used at
    creation for non-text files.
- **Out of scope (still)**: format-aware rendering of file properties
  (download helper, etc.); extending `PageCover` / block-side request
  models (already had the variant); read-path changes (response shape
  unchanged).

---

### 9. Rich text → HTML converter — **DONE (2026-05-30)**

- **Status**: Shipped on `main` in commit
  [`dd911ea`](https://github.com/jsaabel/kotlin-notion-client/commit/dd911ea)
  ("feat(utils): add List<RichText>?.toHtml() rich-text → HTML
  converter (§9)"), closing
  [#8](https://github.com/jsaabel/kotlin-notion-client/issues/8). Port
  of the production-tested `RichTextHtmlRenderer` from festival-scripts.
- **Shipped**:
  - Public surface: single extension
    `List<RichText>?.toHtml(): String?` in
    `it.saabel.kotlinnotionclient.utils` (package per D5), backed by an
    `internal object RichTextHtmlRenderer` (D1).
  - Annotation handling: bold → `<strong>`, italic → `<em>`,
    strikethrough → `<s>`, code → `<code>`, underline → `<u>`.
  - Real text-link hrefs → `<a href="…" rel="nofollow noreferrer
    noopener">…</a>` (D4). Notion-internal hrefs dropped.
  - HTML escaping of plain-text segments (XSS safety).
  - Paragraph splitting: single newline → `<br>`; blank line → wrapped
    in `<p>…</p>`.
  - Mention / equation / unknown segments render as escaped `plainText`.
  - Null / empty / blank input → `null`.
  - KDoc enumerates the v0.6.0+ deferrals (D6) — colours,
    mention-aware rendering, equations, `RenderOptions`.
  - `unit/utils/RichTextHtmlTest.kt` — ~24 cases mirroring the
    festival-scripts test, including the `rel="nofollow noreferrer
    noopener"` assertion and the full behaviour matrix. All pass.
- **Deferred to v0.6.0+** (rendered as escaped plain text in v0.5.0):
  - Colour annotations (inline `style=` vs class is a design decision).
  - Mention-aware rendering (`page`/`user`/`date`/`database`/`template`)
    — needs a configurable link resolver design.
  - Equations — needs choice of MathML / KaTeX / `<code>`.
  - `RenderOptions` object — arrives when colours / mentions /
    equations land.
- **Out of scope entirely**: rich-text-to-Markdown, full block-tree to
  HTML.

---

### 10. Integer-aware plain-text rendering for number properties — **DONE (2026-05-30)**

- **Status**: Shipped on `main` in commit
  [`6550bfc`](https://github.com/jsaabel/kotlin-notion-client/commit/6550bfc)
  ("feat(pages): integer-aware plain-text rendering for number
  properties (§10)"), closing
  [#9](https://github.com/jsaabel/kotlin-notion-client/issues/9). See
  [`_task_10_number_plain_text_rendering.md`](_task_10_number_plain_text_rendering.md)
  for the per-task journal.
- **Shipped**:
  - Private `Double.formatPlainText()` helper in
    `models/pages/PageExtensions.kt` — integer form when
    `value == value.toLong().toDouble()`, else `value.toString()`. NaN /
    ±Infinity and `|value| > MAX_SAFE_INTEGER` (2^53) fall back to
    `toString()` to avoid coercion and lossy truncation.
  - Applied at all three numeric render sites in
    `getPlainTextForProperty`: `PageProperty.Number` (line 175),
    `FormulaResult.NumberResult` (line 229), `RollupResult.NumberResult`
    (line 237).
  - Storage unchanged — `Double?` everywhere, no new public API surface.
  - `unit/properties/NumberFormatPlainTextTest.kt` — 11 cases covering
    integral / decimal / zero / negatives / NaN / ±Infinity / above
    MAX_SAFE_INTEGER / null / Formula NumberResult / Rollup
    NumberResult. Existing `PagePropertyAccessTest` (`Price: 2.5`) stays
    green. Full unit suite green.
  - `CorePagesIntegrationTest` extended with two assertions in the CRUD
    lifecycle block (`"Score" == "85.5"` then `"95"` after update) — the
    second is the regression guard that would have failed pre-fix.
- **Out of scope (deferred — follow up if needed)**: format-aware
  rendering (`$17`, `17%`, `€17.50`) based on `number.format` from the
  property schema; locale-aware rendering; exposing
  `Double.formatPlainText()` publicly.

---

## Proposed sequencing

All items are independent — sequence is for sanity, not dependency. §1
removed (skipped). §7 commitment is now full overhaul.

| Phase | Item | Effort | Notes |
| --- | --- | --- | --- |
| 1 ✅ | §3 `agent_id` parent type | S | Isolated, deserialize-only — **done 2026-05-30** |
| 2 ✅ | §4 Cursor-opaqueness audit | S | **Closed 2026-05-30** — audit PASSed, no code changes (see [`_task_02_cursor_audit.md`](_task_02_cursor_audit.md)) |
| 3 ✅ | ~~§6c Person filter cleanup~~ | — | **Done (verified 2026-05-30): no-op, no workaround in tree** |
| 4 ✅ | §6a Update / Delete comment endpoints | S | **Done 2026-05-30** — issues [#1](https://github.com/jsaabel/kotlin-notion-client/issues/1) (update) and [#2](https://github.com/jsaabel/kotlin-notion-client/issues/2) (delete) both shipped |
| 5 ✅ | §8 Files-property `FileUpload` variant | S | **Done 2026-05-30** — commits [`d736127`](https://github.com/jsaabel/kotlin-notion-client/commit/d736127) + [`4d6dfd9`](https://github.com/jsaabel/kotlin-notion-client/commit/4d6dfd9) (+ [`c479f2c`](https://github.com/jsaabel/kotlin-notion-client/commit/c479f2c), [`3a2656b`](https://github.com/jsaabel/kotlin-notion-client/commit/3a2656b), [`3e9449b`](https://github.com/jsaabel/kotlin-notion-client/commit/3e9449b) fix-ups), closes [#6](https://github.com/jsaabel/kotlin-notion-client/issues/6) + [#7](https://github.com/jsaabel/kotlin-notion-client/issues/7) |
| 6 ✅ | §10 Number → integer plain-text rendering | S | **Done 2026-05-30** — commit [`6550bfc`](https://github.com/jsaabel/kotlin-notion-client/commit/6550bfc), closes [#9](https://github.com/jsaabel/kotlin-notion-client/issues/9) |
| 7 ✅ | §5 `request_status` + auto-paginate handling | M | **Done 2026-05-30** — `RequestStatus` modelled on three response types, `query` + `queryAsFlow` throw `QueryResultLimitReached` with partial results |
| 8 ✅ | §6b Multi-value select/status/multi_select filters | M | **Done 2026-05-30** — commit [`c7c05ab`](https://github.com/jsaabel/kotlin-notion-client/commit/c7c05ab), closes [#4](https://github.com/jsaabel/kotlin-notion-client/issues/4) |
| 9 ✅ | §9 Rich text → HTML converter | M | **Done 2026-05-30** — commit [`dd911ea`](https://github.com/jsaabel/kotlin-notion-client/commit/dd911ea), closes [#8](https://github.com/jsaabel/kotlin-notion-client/issues/8). Minimal first cut: annotations + links + paragraph splitting + escape |
| 10 | §2 Docs note for PATs | S | After everything else lands |
| 11 | §7 Rate-limiting overhaul | L–XL | Full overhaul (all 9 defects). Sequenced last so smaller items can still ship if this slips |

`S` ≈ ≤1 day · `M` ≈ 1–3 days · `L` ≈ several days · `XL` ≈ a release of its own.

Related ideas worth resolving alongside:
- IDEAS.md #10 (auto-pagination opt-out) — overlaps directly with §5.
- IDEAS.md #4, #5 — tangential, optional.

---

## Reference docs fetched

See [`_next_release_docs/`](_next_release_docs/) — temporary working folder
with digests of each linked API reference (fetched 2026-05-30). Delete
after release ships.

Still outstanding:
- [ ] (#2) Developer portal + PAT docs — not yet fetched, low priority.

---

## Decisions log

All decisions resolved on 2026-05-30.

1. ~~**Target version**~~: **v0.5.0** (minor bump — new parent type, new
   comment endpoints, new filter-DSL shape, rich-text → HTML).
2. ~~**§1 `insert_content`**~~: **Skipped** — Notion marks the operation
   itself as legacy/deprecated. If a real need surfaces, revisit as a
   `prepend/append` helper built on `update_content`.
3. ~~**§5 surface for incomplete pagination**~~: **Decided — throw
   `NotionException.QueryResultLimitReached`** (carries partial results +
   `nextCursor`). Single-page methods additionally expose `requestStatus`
   on their response wrapper.
4. ~~**§6a update API shape**~~: **Decided — mirror `create`'s pattern**.
   Single `UpdateCommentRequest` (runtime XOR) + DSL overload. Consistency
   with `create` wins over per-call compile-time safety.
5. ~~**§6b DSL shape**~~: **Decided — vararg on existing methods**.
   Single-value: `equals("a")` (back-compat). Multi-value: `equals("a","b")`.
   Serializer emits string for size==1, array for size>1.
6. ~~**§7 rate-limiting**~~: **Decided — full overhaul** (all 9 ranked
   defects: concurrency model, Ktor pipeline interceptor, header reading,
   coverage, unify with `EnhancedFileUploadApi.withRetry`, bug fixes).
   Sequenced last so smaller items can still ship if this slips.
7. ~~**§9 rich-text → HTML**~~: **Decided — minimal first cut in v0.5.0**.
   Annotations + links + HTML escape. Colours, mentions, equations
   deferred to v0.6.0+ (rendered as escaped plain text in v0.5.0). No
   options object yet.
8. ~~**§10 number rendering**~~: **Decided** — symptom and rule confirmed.
   Apply the integer-strip rule via a shared `Double.formatPlainText()`
   helper at all three numeric render sites (Number, Formula
   NumberResult, Rollup NumberResult). Format-aware rendering (currency
   / percent) is out of scope.
