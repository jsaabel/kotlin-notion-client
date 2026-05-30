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
  (2026-05-30)**; §6a comment
  update/delete (mirrors `create` pattern); §6b multi-value filters as
  vararg overloads; §6c person filter cleanup — **verified no-op
  (2026-05-30)**.
- **Carry-overs (§7–§8)**: §7 rate-limiting — **full overhaul** (all 9
  ranked defects, sequenced last); §8 files-property `FileUpload` variant.
- **Consumer conveniences (§9–§10)**: §9 rich-text → HTML — **minimal
  first cut** (annotations + links + escape; colours/mentions/equations
  deferred); §10 integer-aware number rendering across Number / Formula /
  Rollup.

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

#### 6a. Update / Delete comment endpoints — now GA

> The Update a comment (PATCH /v1/comments/:comment_id) and Delete a comment
> (DELETE /v1/comments/:comment_id) endpoints are now generally available.
> Non-DLP integrations can only modify or delete comments they created.

- **References:**
  [`_next_release_docs/06a_update_a_comment.md`](_next_release_docs/06a_update_a_comment.md),
  [`_next_release_docs/06a_delete_a_comment.md`](_next_release_docs/06a_delete_a_comment.md)
- **Current state**: `CommentsApi.create` exists; `update`/`delete` do not
  (`grep` confirms).
- **Confirmed from fetched docs**:
  - **Update**: `PATCH /v1/comments/{id}` with **exactly one** of
    `rich_text` (max 100 items) or `markdown`. Both/neither → 400.
  - **Delete**: `DELETE /v1/comments/{id}`, no body. Returns the deleted
    comment object.
  - Both: caller can only modify/delete comments **it created**, else 404.
- **Required work**:
  - `CommentsApi.update(commentId, request: UpdateCommentRequest)` + DSL
    overload `update(commentId) { content { … } }`. Mirrors the existing
    `create` pattern (single request object with **runtime** XOR
    validation between `richText` and `markdown`, see CommentsApi.kt:189).
    Chosen for consistency with `create` over per-call compile-time
    safety; if we ever want compile-time XOR we should do `create` and
    `update` together as a deliberate API improvement (out of scope).
  - `CommentsApi.delete(commentId)`.
  - Unit + integration tests. Integration test creates → updates → deletes
    a comment owned by the integration.

#### 6b. Multi-value filters for select / status / multi_select

> Database and data source filters now accept an array of values for
> `equals` / `does_not_equal` on select and status properties, and for
> `contains` / `does_not_contain` on multi_select properties...

- **Current state**: `DataSourceQueryBuilder.kt`:
  - `SelectFilterBuilder.equals/doesNotEqual` take `String` (single value).
  - `StatusFilterBuilder.equals/doesNotEqual` take `String`.
  - `MultiSelectFilterBuilder.contains/doesNotContain` take `String`.
- **References:**
  [`_next_release_docs/06b_filter_data_source_entries.md`](_next_release_docs/06b_filter_data_source_entries.md),
  [`_next_release_docs/06b_post_database_query_filter.md`](_next_release_docs/06b_post_database_query_filter.md),
  [`_next_release_docs/06b_quick_filters.md`](_next_release_docs/06b_quick_filters.md)
- **Wire format confirmed**: array as value under the same key, **not** a
  new `equals_any`/`contains_any` key. Spec is `"string" or "string[]"`
  for `equals` / `does_not_equal` (select, status) and `contains` /
  `does_not_contain` (multi_select).
- **Bonus**: the same multi-value support extends to `ViewsApi`
  quick_filters configuration — single implementation, multiple call
  sites benefit.
- **Required work**:
  - **DSL shape (decided)**: vararg on existing methods —
    `equals(vararg values: String)`, `doesNotEqual(vararg values: String)`,
    `contains(vararg values: String)`, `doesNotContain(vararg values: String)`.
    Strict superset of current API. Empty varargs throws.
  - **Wire serialization**: size==1 → single JSON string (preserves
    existing wire shape, back-compat for fixtures), size>1 → JSON array.
    Implement via custom serializer on the value type used by
    `SelectCondition` / `StatusCondition` / `MultiSelectCondition`.
  - Update `SelectCondition` / `StatusCondition` / `MultiSelectCondition`:
    change the `equals`/`doesNotEqual`/`contains`/`doesNotContain` fields
    from `String?` to the new union value type.
  - Unit tests against the sample JSON from the fetched references
    (single + multi shapes); integration test exercising both shapes
    against a real data source.

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

---

### 8. Files-property `FileObject.FileUpload(id)` variant

- **See:** [`2026_05_30_02_Files_Property_Gap_Analysis.md`](2026_05_30_02_Files_Property_Gap_Analysis.md)
  — proposed work itemised at the bottom of that journal (sealed-class
  variant, `PagePropertiesBuilder.files(...)` DSL, integration test,
  serialization fixture/unit test).
- **One-line summary** (for orientation only): the gap blocks scenario (b)
  — uploading a local file then attaching it to a Files & media property
  on a database row. The block-side analogues already have the variant; the
  page-property side is the odd one out.
- **Recommend including in full** — small, self-contained, closes a real
  user-visible gap.

---

### 9. Rich text → HTML converter — **MINIMAL FIRST CUT**

- **Status**: New utility for v0.5.0. No existing renderer to extend —
  `RichText.plainText` is a pre-rendered string Notion sends back, not the
  output of a walker we control.
- **Decision: ship a minimal first cut.** Pure function
  `List<RichText>.toHtml(): String`. Iterate via follow-ups.
- **In scope (v0.5.0)**:
  - Annotation handling: bold → `<strong>`, italic → `<em>`, strikethrough
    → `<s>`, code → `<code>`, underline → `<u>`.
  - Link rendering: `RichText` with a `href` → `<a href="…">…</a>`.
  - HTML escape of plain-text segments (XSS safety — non-negotiable).
- **Deferred to v0.6.0+** (rendered as plain escaped text in v0.5.0):
  - Colour annotations (inline `style=` vs class is a design decision).
  - Mentions (`page`/`user`/`date`/`database`/`template`) — needs a
    configurable link resolver design.
  - Equations — needs choice of MathML / KaTeX / `<code>`.
- **No HTTP client involvement.** No `options` object yet (kept
  zero-config to keep the surface small; options arrive when colours /
  mentions / equations land).
- **Out of scope entirely**: rich-text-to-Markdown, full block-tree to
  HTML.

---

### 10. Integer-aware plain-text rendering for number properties

- **Status**: New utility for v0.5.0. Symptom confirmed (value `17`
  renders as `"17.0"`).
- **Root cause confirmed**:
  - Storage type is `Double?` (`PageProperty.kt:76`).
  - Plain-text path is `property.number?.toString()`
    (`PageExtensions.kt:175`).
  - Same `.toString()` is used at three sites: `PageProperty.Number`
    (line 175), `FormulaResult.NumberResult` (line 229), and
    `RollupResult.NumberResult` (line 237). All exhibit the same
    symptom.
- **Decision**:
  - **Rule**: render as integer iff `value == value.toLong().toDouble()`.
  - **Scope**: apply at all three numeric render sites via a shared
    `Double.formatPlainText()` helper. Notion's UI displays integral
    rollups and formula numbers as integers too, so this matches user
    expectations across the board.
  - **Storage unchanged**: `Double?` stays. Callers wanting the raw
    double use the typed accessor (`property.number`).
  - **Out of scope**: format-aware rendering (`$17`, `17%`, `€17.50`)
    based on `number.format` from the property schema — requires schema
    access that the page-level helper doesn't have today. Track as a
    follow-up if a user need surfaces.
- **Edge cases to handle in the helper**:
  - NaN / Infinity → fall back to `toString()` (don't call `toLong()`).
  - Values outside the safe integer range (~|value| > 2^53) → fall back
    to `toString()` to avoid lossy truncation.

---

## Proposed sequencing

All items are independent — sequence is for sanity, not dependency. §1
removed (skipped). §7 commitment is now full overhaul.

| Phase | Item | Effort | Notes |
| --- | --- | --- | --- |
| 1 ✅ | §3 `agent_id` parent type | S | Isolated, deserialize-only — **done 2026-05-30** |
| 2 ✅ | §4 Cursor-opaqueness audit | S | **Closed 2026-05-30** — audit PASSed, no code changes (see [`_task_02_cursor_audit.md`](_task_02_cursor_audit.md)) |
| 3 | ~~§6c Person filter cleanup~~ | — | **Done (verified 2026-05-30): no-op, no workaround in tree** |
| 4 | §6a Update / Delete comment endpoints | S | Mirrors existing `create` |
| 5 | §8 Files-property `FileUpload` variant | S | Closes a known gap; scope already documented |
| 6 | §10 Number → integer plain-text rendering | S | Tiny helper, three call sites |
| 7 ✅ | §5 `request_status` + auto-paginate handling | M | **Done 2026-05-30** — `RequestStatus` modelled on three response types, `query` + `queryAsFlow` throw `QueryResultLimitReached` with partial results |
| 8 | §6b Multi-value select/status/multi_select filters | M | Vararg DSL + custom serializer (string \| array) |
| 9 | §9 Rich text → HTML converter | M | Minimal first cut: annotations + links + escape |
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
