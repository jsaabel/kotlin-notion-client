# Next Release Planning — Notion API changelog since v0.4

**Date:** 2026-05-30
**Status:** Decisions resolved — ready to break into implementation tasks
**Scope:** Notion API changelog entries from 2026-04-17 → 2026-05-15
**Target version:** v0.5.0 (minor bump — new parent type, new comment endpoints, new filter-DSL shape, rich-text → HTML)

## TL;DR

Target **v0.5.0**. Ten work items spanning three groups:

- **API changelog items (§1–§6)**: §1 (`insert_content`) skipped as
  deprecated; §2 docs-only; §3 `agent_id` parent type; §4 cursor audit;
  §5 pagination cap → throws `QueryResultLimitReached` on the
  auto-paginating path; §6a comment update/delete (mirrors `create`
  pattern); §6b multi-value filters as vararg overloads; §6c person
  filter cleanup.
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

### 3. `agent_id` parent type (2026-05-11)

> Pages and blocks parented by an agent now serialize their parent as
> `{ "type": "agent_id", "agent_id": "..." }` instead of being rejected or
> rewritten.

- **Current state**: `ParentSerializer` (`models/base/ParentSerializer.kt`)
  handles `page_id`, `data_source_id`, `database_id`, `block_id`, `workspace`
  — no `agent_id`. Pages whose parent is an agent will currently fail
  deserialization with `Unknown Parent type: agent_id`.
- **Required work**:
  - Add `Parent.AgentParent(agentId: String)`.
  - Extend `ParentSerializer` + `ParentSurrogate`.
  - Unit test: round-trip the new variant.
- **Reference:** [`_next_release_docs/03_parent_object.md`](_next_release_docs/03_parent_object.md)
- **Confirmed from fetched docs**: read-only / system-assigned; appears on
  "agent instruction pages and the blocks that make them up." No
  documented path to set `agent_id` on create. Model it as a
  deserialize-only variant.
- **Skipped from this changelog entry**: the Query meeting notes endpoint
  (`POST /v1/blocks/meeting_notes/query`) — out of scope, no user need yet.
- **Tangent surfaced by fetched docs**: `data_source_id` parents are
  documented as carrying *both* `data_source_id` and `database_id`. Our
  current `Parent.DataSourceParent` may only carry one of these — worth a
  quick check. (Tracking as an aside; not in scope for this release.)

---

### 4. Pagination cursor reliability (2026-04-22)

> Pagination cursors now embed a session identifier... The `start_cursor`
> parameter now accepts opaque string values in addition to UUIDs... cursors
> should always be treated as opaque.

- **Client impact**: **Should be a no-op** if our code already treats cursors
  as opaque strings.
- **Required work**:
  - Audit usages of `next_cursor` / `start_cursor` in:
    - `DataSourcesApi` query pagination
    - `BlocksApi.children`
    - `SearchApi`
    - `ViewsApi` query
    - `UsersApi.list`
    - `CommentsApi.list`
    - Any other paginated endpoint.
  - Confirm cursor is typed as `String?` end-to-end with no UUID parsing,
    validation, or coercion.
  - Add a regression unit test using a non-UUID opaque-string cursor in a
    fixture.
- **Low risk, quick audit.**

---

### 5. Pagination depth limit + `request_status` field (2026-04-20)

> The Query a data source, Create a view query, and Get view query results
> endpoints now enforce a maximum pagination depth of 10,000 results per
> query... the response includes a new `request_status` field:
> `{ "request_status": { "type": "incomplete", "incomplete_reason":
> "query_result_limit_reached" } }`

- **Current state**: We don't model `request_status` at all (`grep` confirms).
  Our auto-pagination in `DataSourcesApi.query` will silently iterate until
  `has_more=false`, hitting the new 10k cap without surfacing it.
- **Required work**:
  - **Model**: `RequestStatus { type: "complete" | "incomplete",
    incompleteReason: String? }` added to:
    - `QueryDataSourceResponse`
    - View query response (create + get results)
    - Anything else the docs list.
  - **Behavior (decided)**: when auto-pagination terminates because of
    `request_status.type == "incomplete"`, **throw
    `NotionException.QueryResultLimitReached`**. The exception carries the
    partial `List<T>` collected so far and the `nextCursor`/`requestStatus`
    so callers can recover.
    - *Rationale*: high-level methods currently return `List<T>` with no
      wrapper, so there is no field-on-result to repurpose. Truncation at
      10k means the caller is getting incomplete data — silent log lines
      would lose that signal. An exception forces the caller to make a
      conscious choice.
    - *Single-page methods* (which already return `*Response` wrappers)
      additionally expose `requestStatus` directly — the exception only
      applies to the auto-paginating path.
  - **Docs**: README / notebook example must explain the 10k limit, how to
    catch the new exception, and how to chunk via filters or webhooks.
- **References:**
  [`_next_release_docs/05_create_view_query.md`](_next_release_docs/05_create_view_query.md),
  [`_next_release_docs/05_get_view_query_results.md`](_next_release_docs/05_get_view_query_results.md)
- **Confirmed from fetched docs**:
  - Only one `incomplete_reason` is documented: `query_result_limit_reached`.
  - `request_status` is surfaced on **every page** of paginated results
    for a truncated query (not just the last) — callers can short-circuit.
  - Trap: when truncated, `total_count` reflects **only the truncated
    cache size**, not the true matching row count. We should document this
    prominently on whichever model exposes `totalCount`.
  - The 10k limit is documented on the **view query** endpoints. The
    changelog also names the *data source* query endpoint — the data
    source filter reference (fetched separately) doesn't enumerate
    `request_status`, so we should verify in an integration test whether
    it actually appears there too.
- **Ties into Idea #10** (limited-result querying / opt-out from
  auto-pagination) — worth thinking about together because both touch the
  same pagination loop.

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

- **Required work**:
  - Spot-check `PeopleCondition` deserialization — if Notion previously
    wrapped person filters in an extra `or` combinator on read and we had
    a workaround for that, the workaround can be removed.
  - Otherwise no-op.

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
| 1 | §3 `agent_id` parent type | S | Isolated, deserialize-only |
| 2 | §4 Cursor-opaqueness audit | S | Cheap audit; likely already correct |
| 3 | §6c Person filter cleanup | S | Likely a deletion, not an addition |
| 4 | §6a Update / Delete comment endpoints | S | Mirrors existing `create` |
| 5 | §8 Files-property `FileUpload` variant | S | Closes a known gap; scope already documented |
| 6 | §10 Number → integer plain-text rendering | S | Tiny helper, three call sites |
| 7 | §5 `request_status` + auto-paginate handling | M | Throws `QueryResultLimitReached` on the auto-paginating path |
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
