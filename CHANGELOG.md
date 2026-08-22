# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] — targeting 0.6.0

The largest release this library has had: a catch-up with every Notion API change through
August 2026, a file-upload overhaul that makes local files first-class across the whole
request surface, and a deliberate correctness pass over our own past output.

**How this release was built — and a note on AI.** This project has been open about its
heavy use of AI from the start, and this release leans into it further: the work was carried
out almost entirely by AI agents (Claude Code) in an orchestrated run — twenty-plus GitHub
issues (#32–#42, #56–#62, #68–#70, #75–#76, #80–#82) implemented one agent per issue,
reviewed and merged as PRs (#45–#85) onto a shared release branch, followed by a dedicated
live-API verification pass (#61) against a real workspace. That openness cuts both ways:
several fixes in this release correct bugs that earlier, equally AI-assisted releases
introduced — most seriously a timezone bug where offset-less datetimes were silently stored
shifted (see the breaking dates section below), and an `icon.remove()`/property-clearing DSL
whose payloads never reached the wire. The live verification pass exists precisely because
high-confidence implementation from documentation alone is not the same as observed
behaviour; it forced two further model fixes of its own. Findings, agent reports and
decisions are recorded per issue, in `FOLLOWUPS.md`, and in `docs/adr/`.

### Highlights

- **Local files are first-class everywhere.** `content { image(File(…)) }`,
  `icon.upload(File(…))`, `files("Attachments") { upload(File(…)) }`, comment attachments —
  one call, one bounded concurrent upload pass, atomic failure ([ADR 0001](docs/adr/0001-deferred-file-upload-resolution.md)).
- **`databases.update` at last** — retitle, re-icon, and *move* a database; the icon/cover
  set-vs-clear support matrix established live.
- **Past the 10,000-row ceiling** — windowed drains for data sources *and* views.
- **Async tasks** — polling, flows, async markdown writes and async page creation.
- **Webhook signature verification** with typed event models.
- **Formulas and rollups can finally be written**, with typed configurations on the read side.
- **A correctness pass over our own output**: timezone-safe date writes, `null` payloads that
  actually clear things, a `FileUpload` model matching the documented shape, and uncomputable
  formula/rollup values that no longer crash page deserialization.
- **One DSL convention, documented and compile-tested** — every documented snippet now
  compiles, enforced by `DocumentedSnippetsTest`.

### ⚠️ Breaking changes

#### Dates: the write side is now offset-preserving, zone-explicit and validating (#31)

A consumer shipped a bug in which offset-less datetimes written through this library were
read by Notion as UTC, silently moving every touched event by the local UTC offset while
still displaying the original wall clock. The write-side date surface now makes that
mistake impossible to write silently. Migration for each break:

- **`LocalDateTime` + `TimeZone` writes preserve the wall clock and offset instead of
  converting to a UTC instant.** `dateTime("Start", LocalDateTime(2026, 10, 25, 13, 0),
  TimeZone.of("Europe/Oslo"))` now sends `2026-10-25T13:00:00+01:00` (previously
  `2026-10-25T12:00:00Z`). The stored *instant* is the same; the stored offset and digits
  now match the caller's zone. Applies to `dateTime`, `dateTimeRange` (both forms and the
  DSL), `dateMention`, and the `LocalDateTime` overloads of the date/timestamp query
  filters. The offset is resolved at each value's **own** local date, so DST changeovers
  are handled per value — a range may carry `+02:00` on one end and `+01:00` on the
  other. Ambiguous local times (autumn overlap) resolve to the earlier instant;
  nonexistent ones (spring gap) shift forward by the gap — both matching Notion's own
  resolution of a named `time_zone`, and the IANA tz database via `java.time`.
  *Migration:* none for "I meant that local time" callers — this is the fix. Callers who
  deliberately wanted the UTC instant should pass an `Instant`:
  `dateTime("Start", localDateTime.toInstant(zone))`.

- **The `TimeZone.UTC` defaults are gone; the zone parameter is required.** A
  `LocalDateTime` carries no zone, so defaulting it to UTC was silently wrong for every
  caller outside UTC. Affects `dateTime(name, LocalDateTime)`, both `dateTimeRange`
  forms, `dateMention(LocalDateTime, …)`, and the `LocalDateTime` query filter overloads.
  *Migration:* state the zone — `dateTime("Start", value, TimeZone.of("Europe/Oslo"))`,
  or `TimeZone.UTC` if UTC was truly meant.

- **`dateMention(LocalDateTime, …)` parameter order changed** to
  `dateMention(start, timeZone, end = null)` so the now-required zone sits next to the
  value. It also now sends an offset-bearing string (`…T14:30:00-04:00`, `time_zone`
  omitted) instead of the naive string + `time_zone` id — the stored result in Notion is
  identical, and `dateMention` and `dateTime` now agree for the same input.
  *Migration:* calls using named arguments are unaffected; positional calls with an `end`
  swap the last two arguments.

- **Datetime strings are validated.** A string with a time component but neither a UTC
  offset nor a `time_zone` is rejected with `IllegalArgumentException` — this is exactly
  the input that caused the consumer bug. Also rejected: a `time_zone` on a date-only
  value, an offset *combined* with a `time_zone` (Notion strips the offset and re-applies
  the zone), and an unknown `time_zone` id. Applies to `date`/`dateTime`/`dateRange`/
  `dateTimeRange`/`verify` string overloads, the `DateValue` factory methods, the
  `date(name, DateData)` overload, `dateMention` string overloads, and the string date/
  timestamp query filters (relative values like `"today"` still pass through).
  *Migration:* append the offset you mean (`…T14:30:00+02:00`), or use the typed
  `LocalDateTime` + `TimeZone` overloads, or pair the naive string with a zone via
  `dateTimeWithTimeZone`.

- **`dateWithTimeZone(name, date, timeZone)` is removed** (with its
  `DateValue.fromDateWithTimeZone` factory). A `time_zone` on a date-only value has no
  time to interpret and is now rejected everywhere. *Migration:* use `date(name, date)`;
  if a time was actually intended, use `dateTimeWithTimeZone`.

#### `FileUpload` now matches the documented shape (#68)

Three model-layer bugs meant valid, documented API responses failed to deserialize. Fixing
them changes types callers may have relied on:

- **`FileUpload.filename` and `FileUpload.contentType` are now `String?`.** The API reference
  marks both nullable, and they genuinely are: a single-part upload created without a filename
  keeps `filename` null until the send step, and `content_type` likewise stays null until it is
  inferred from the form data. Declaring them non-null meant `createFileUpload { }` — a
  documented, valid call — crashed on deserialization.
  *Migration:* handle the null (`upload.filename ?: "untitled"`). `sendFileUpload(fileUpload,
  …)` threads the nullable content type through unchanged, which is correct in both cases;
  pass one explicitly via the `fileUploadId` overload if you need to.

- **`FileUploadStatus` gained `EXPIRED` and `UNKNOWN`.** `expired` is one of four documented
  statuses and previously failed to decode outright, so `retrieveFileUpload` threw and
  `listFileUploads` threw if any result had expired. `UNKNOWN` is the forward-compat fallback,
  matching the `Unknown` variants on `RollupResult`/`FormulaResult`.
  *Migration:* exhaustive `when` blocks over `FileUploadStatus` need the two new branches. The
  `isTerminal` and `isUnusable` properties cover the common cases without enumerating.

- **`FileUploadError` gained `UploadUnusableError`.** Thrown by
  `EnhancedFileUploadApi.waitForFileReady` when an upload reaches `EXPIRED` or `FAILED`,
  replacing an `UnknownError` wrapping a synthetic exception. It carries the status and, for a
  failed external-URL import, the `FileImportError` explaining why.
  *Migration:* exhaustive `when` blocks over `FileUploadError` need the new branch.

(`models.files.FileUploadReference` is now a typealias for the canonical
`models.base.FileUploadReference`, resolving a duplicate declaration; both import paths keep
compiling, so this one is not breaking.)

#### Formula, rollup and status models (#36, #39, #42, #57, #59)

- **`FormulaResult` and `RollupResult` gained an `UnsupportedResult` subclass** (#36). An
  exhaustive `when` over either sealed class with no `else` branch no longer compiles.
  *This fixes a latent crash:* Notion returns `type: "unsupported"` for formulas and rollups
  that depend on too many related pages, and the missing subclass previously failed
  deserialization of the **entire page** with `JsonDecodingException`. Runtime behaviour for
  input that already worked is unchanged.
- **`RollupResult` gained an `IncompleteResult` subclass, and both `FormulaResult` and
  `RollupResult` gained an `Unknown` fallback subclass** (#57). Same effect on an exhaustive
  `when`: another arm is now required (or an `else`). `IncompleteResult` fixes the same crash
  class as #36 for Notion's documented `rollup.type: "incomplete"` value (a rollup still being
  computed); `Unknown` closes the class of bug rather than one instance — any future
  `formula.type`/`rollup.type` Notion adds now degrades to `Unknown` (raw JSON preserved)
  instead of failing page deserialization, mirroring `PageProperty.Unknown`.
- **`StatusConfiguration.options` now takes `List<CreateStatusOption>`** instead of
  `List<CreateSelectOption>` (#39). Source-breaking only for callers constructing the model
  directly; DSL users (`status { option(...) }`) are unaffected. The dedicated type exists so
  `group` cannot be set on select/multi-select options, which the API rejects.
- **`DatabaseProperty.Formula.formula` is now `FormulaConfiguration`**, not `JsonObject` (#42).
  Source-breaking for consumers reading the raw object; use `.expression` instead.
- **`DatabaseProperty.Rollup.rollup` is now `RollupConfiguration`**, not `JsonObject` (#59).
  Source-breaking for consumers reading the raw object; use `.function`,
  `.relationPropertyName`/`.relationPropertyId` and `.rollupPropertyName`/`.rollupPropertyId`
  (or the `.relationReference`/`.rollupReference` shorthands on the property) instead. Same
  shape of change as the `Formula.formula` break above.
- **`SearchFilter.property` now defaults to `null`, not `"object"`** (#58). A hand-written
  `SearchFilter(inTrash = true)` bypassing `SearchRequestBuilder` previously emitted a spurious
  `"property":"object"` alongside `in_trash` — `property` is now omitted unless a non-null
  `value` is also set, and an `init` block rejects the invalid combination
  (`property` set with `value` null, or `property` set to anything but `"object"`) with
  `IllegalArgumentException`. `SearchRequestBuilder`/`searchRequest { }` callers are
  unaffected — the builder already set both fields together.

#### ⚠️ Behaviour change: icon/cover removal and property clearing now reach the wire (#80)

Two DSL affordances lost the `null` that carried their intent, because the client encodes with
`explicitNulls = false` and the serializer cannot tell "this request does not touch that field"
from "this request clears that field". Verified live, they failed differently — one silently,
one loudly:

- **`icon.remove()` and `cover.remove()` remove the icon and cover.** Notion removes them when
  the request carries `"icon": null`; both builders set the field to Kotlin `null`, which the
  encoder drops — `updatePageRequest { icon.remove() }` encoded to `{}`, an empty PATCH that
  Notion accepts and ignores. The call returned a `Page` and changed nothing. Both now record
  `Icon.Removed` / `PageCover.Removed`, write-only sentinels that serialize to an explicit
  `null`.
  *Migration:* none to call sites, but this is the behaviour change — code that called
  `remove()` and relied on it doing nothing will now see the icon or cover actually disappear.
  Code that reads a *built* request and expected `icon == null` after `remove()` should compare
  against `Icon.Removed`; a request that never mentions the icon still has `icon == null`.

- **Clearing a property value now sends the payload key, and no longer fails.** `select`,
  `status`, `date`, `dateTime`, `number`, `url`, `email` and `phoneNumber` all document "null
  for empty" and built a value whose payload field was `null`; the encoder emitted
  `{"type":"select"}` — the discriminator without the instruction. Notion rejects that shape
  with **HTTP 400 `validation_error`** and leaves the property untouched, so
  `select("Status", null)` threw rather than clearing anything. It now encodes as
  `{"type":"select","select":null}`, which is what Notion clears on. List-valued properties
  (`multi_select`, `people`, `relation`, `files`) were never affected — they clear with `[]`.
  *Migration:* none — these calls previously could not succeed.

- **`Icon` and `PageCover` each gained a variant** (`Removed`). Both are sealed and public, so
  an exhaustive `when` over either needs a new branch. The sentinels are write-only: a removed
  icon reads back as `null` and never decodes to `Removed`.

The mechanism, the rejected alternative of flipping `explicitNulls`, and why model-level tests
could not see any of this are recorded in
[ADR 0002](docs/adr/0002-explicit-null-payloads.md). The client's JSON configuration now lives
in `serialization/NotionJson` so tests can assert on the exact bytes a request produces.

### Added

#### Files and attachments (#68, #69, #70, #75, #76 — [ADR 0001](docs/adr/0001-deferred-file-upload-resolution.md))

- **Local files inside the content DSL (#75).** The block builders take a local file wherever
  they took a URL or an upload id — `image`, `video`, `audio`, `file` and `pdf` gain
  `FileSource`/`File`/`Path` overloads, and `html(markup, filename, caption)` uploads the
  markup under an `.html` name and resolves to an embed, the shape Notion renders as an HTML
  block:

  ```kotlin
  notion.pages.create {
      parent.page(parentId)
      title("Q3 report")
      content {
          image(File("chart.png"), caption = "Q3")
          html(reportHtml)
          pdf(Paths.get("appendix.pdf"))
      }
  }
  ```

  Builder lambdas are synchronous, so nothing uploads while one runs: the builder records a
  `BlockRequest.PendingUpload` sentinel, and every suspending entry point that consumes
  blocks — `pages.create`/`createAsync`, `blocks.appendChildren`, `blocks.update` — uploads
  and substitutes before validating and sending. Building blocks separately works too:
  `pageContent { image(File(…)) }` handed to `appendChildren(id, blocks)` uploads on the way
  out.

- **Local files on every attachment surface (#76).** The same mechanism covers a whole
  request, not just its block list — a page create can carry local files in its icon, cover,
  "Files & media" properties and content at once, resolved in a **single** upload pass before
  one `POST /v1/pages`. New overloads, each taking `FileSource`/`File`/`Path`:
  `FilesBuilder.upload(source, name = null)`; `upload(source)` on every icon and cover
  builder (`CreatePageRequestBuilder`, `UpdatePageRequestBuilder`, `DatabaseRequestBuilder`,
  `UpdateDataSourceRequestBuilder`); and `CreateCommentRequestBuilder.attachment(source)`.
  `comments.create` resolves before counting attachments, so Notion's cap of three is checked
  against what actually goes on the wire.

- **Upload semantics, shared by both stages:** uploads within one call run concurrently
  (bounded at four); the first failure cancels the rest and throws `FileUploadError`
  **before** the create or append is sent, so no partially populated page is left behind
  (already-completed uploads are left to expire — Notion has no delete-upload endpoint).
  The same file attached twice uploads twice; reusing one upload id across blocks is
  unverified against the live API, so the client does not deduplicate. Serializing an
  unresolved sentinel throws a `SerializationException` naming the file and both ways out,
  and `RequestValidator` reports `UNRESOLVED_PENDING_UPLOAD` as defence in depth.
  (`CommentAttachmentRequest` became a sealed class to hold its pending variant; constructing
  one and reading `.fileUploadId`/`.type` still compile, with `fileUploadId` now `String?`,
  null only while pending. Wire JSON unchanged.)

- **One-call upload-and-attach helpers (#69).** Attaching a file used to cost a four-step
  dance — create an upload, send the bytes, wait for ready, reference the id. New suspend
  helpers do all four, living on the API that owns the target:

  ```kotlin
  notion.blocks.appendImage(pageId, File("diagram.png"), caption = "Architecture")
  notion.blocks.appendFile(pageId, Paths.get("report.pdf"))
  notion.blocks.appendHtml(pageId, htmlString)          // uploads .html, attaches as embed
  notion.pages.setIcon(pageId, File("logo.png"))
  notion.pages.setCover(pageId, File("hero.png"))
  notion.pages.attachFiles(pageId, "Attachments", File("a.pdf"), File("b.pdf"))
  notion.comments.create(File("trace.txt")) { parent.page(pageId); content { text("…") } }
  ```

  Each delegates the upload half to `EnhancedFileUploadApi` and **throws** like the rest of
  the client rather than returning a result to unwrap. `attachFiles` is additive by default —
  it reads the page first so existing entries survive — with `replace = true` for overwrite.

- **Icons and covers can be set from an upload (#69).** `Icon.FileUpload` and
  `PageCover.FileUpload` existed as models but were unreachable from every DSL. All four
  `IconBuilder`s and all three `CoverBuilder`s now expose `upload(fileUploadId)` and
  `upload(fileUpload)`. Verified live, and worth knowing: an icon or cover is *written* as
  `file_upload` but *reads back* as `Icon.File`/`PageCover.File` — a signed S3 URL with about
  an hour of life. Copying appearance between pages means re-uploading or switching to
  `external`, not echoing back what was read.

- **`FileUpload`-typed overloads everywhere an upload is attached (#69):**
  `imageFromUpload`/`videoFromUpload`/`audioFromUpload`/`fileFromUpload`/`pdfFromUpload`/
  `embedFromUpload`, `FilesBuilder.upload`, `CreateCommentRequestBuilder.attachment`, and the
  new icon/cover `upload` — no more passing bare id strings. The file-block and files-property
  overloads default the display name to the upload's own filename.

- **HTML blocks via embed + file upload** (Jul 3 2026 Notion changelog): `EmbedRequestContent`
  accepts a `fileUpload` reference as an alternative to `url` (exactly one required), with
  `PageContentBuilder.embedFromUpload(fileUploadId)`. Verified live:
  `{"embed": {"file_upload": {"id": …}}}` is accepted with no type discriminator, and the
  created block reads back as an `embed` whose `url` is a time-limited signed S3 URL.
  `EmbedContent.url` is now nullable (with defensive `file`/`file_upload` fields) to match the
  undocumented read shape.

- **Embed blocks take a caption (#69).** Undocumented — the embed reference lists only
  `url` — but verified live: Notion accepts a caption on an embed and echoes it back. Added to
  `EmbedRequestContent`, `EmbedContent`, `embed(url, caption)`, `embedFromUpload(id, caption)`
  and `BlocksApi.appendHtml(..., caption)`.

- **`file_import_result` is now modelled (#68)** as a `FileImportResult` sealed class with
  `Success`, `Error` and forward-compat `Unknown` variants — the only way the API reports
  *why* an `external_url` import failed. `FileUpload.importError` is the shortcut to the
  `FileImportError` (`type`, `code`, `message`, `parameter`, `status_code`). Also modelled:
  `complete_url`, `number_of_parts` as `FileUploadPartCounts` (`total`/`sent`), and
  `created_by` as a deliberately loose `FileUploadCreatedBy` (its `type` admits `agent`
  alongside `person`/`bot`).

- **`FileUploadResult.getOrThrow()`** bridges the enhanced upload API's sealed result to the
  throwing contract every other API uses; **`File`/`Path`/`ByteArray.asFileSource()`**
  extensions let anything be handed to the upload and attach APIs without naming a
  `FileSource` subclass.

#### Databases, data sources and schema (#39, #42, #57, #59, #82)

- **`databases.update(id, request)` and `databases.update(id) { … }` (#82)** — the container
  attributes are reachable at last. `DatabasesApi` had `retrieve`, `create` and `trash` but no
  `update`, so an icon set at create time could never be changed and moving a database had no
  client-side path. The new surface carries the container's half of the 2025-09-03 split:
  `parent`, `title`, `icon`, `cover`, `is_inline` and `in_trash` (schema, description and a
  data source's own title stay on `dataSources.update`).

  ```kotlin
  notion.databases.update(databaseId) {
      title("Q3 Planning")
      icon.emoji("📊")
      parent.page(newParentId)   // moving a database is now possible
      inline(true)
  }
  ```

  `UpdateDatabaseRequest`/`UpdateDatabaseRequestBuilder` mirror the page update pair;
  `icon.upload(File(…))`/`cover.upload(File(…))` resolve through the pending-upload pass;
  `databases.trash(id)` is now a wrapper over the update path, and `update(id) { restore() }`
  brings a database back. Setting a cover together with `is_inline = true` fails fast at build
  time — Notion does not support the combination.

- **The icon/cover set-vs-clear support matrix, established live** and pinned by
  `IconCoverSupportIntegrationTest`. There is deliberately no `icon.remove()`/`cover.remove()`
  on the database container surface: the endpoint rejects `"icon": null`
  (`HTTP 400 … should be an object or `undefined``), so a container icon/cover can be replaced
  but not cleared. The removal that *does* work — and the one the UI renders — is
  **`dataSources.update(id) { icon.remove() }`**, also new. A data source `cover` is rejected
  outright ("Use the Update Database API instead"), which is why
  `UpdateDataSourceRequestBuilder` has no cover builder:

  | Endpoint | Attribute | Set | Clear |
  | --- | --- | --- | --- |
  | `PATCH /v1/databases` | `icon` | yes | no |
  | `PATCH /v1/databases` | `cover` | yes | no |
  | `PATCH /v1/data_sources` | `icon` | yes | **yes** |
  | `PATCH /v1/data_sources` | `cover` | no | — |

- **Formula properties can be written (#42).** `CreateDatabaseProperty.Formula` and a
  `formula(name, expression, description)` DSL method — previously formulas could not be
  created through this client at all. Expressions are validated at the call site for blank
  input, unterminated string literals, unbalanced brackets, malformed `prop()` calls, and (on
  create) `prop()` references to properties absent from the schema being written. Semantic
  validity — function names, arity, types, cycles — is left to the API's `validation_error`.

- **Rollup properties are typed and can be written (#59).** `RollupConfiguration` replaces the
  untyped `JsonObject` on read and write, `RollupFunction` covers all 24 documented functions
  with an `UNKNOWN` read-side fallback, and `CreateDatabaseProperty.Rollup` plus `rollup(...)`
  DSL overloads make rollups creatable for the first time. Validation lives at construction
  time per the #31/#42 precedent; create requests (which carry the complete schema)
  additionally reject a rollup naming a relation that is absent or not a relation property.

- **Status options can be assigned to groups** on create and update (#39), via `group` on
  `StatusBuilder.option(...)`. An omitted `group` preserves the option's current group on
  update; new options default to "To-do". Verified live in the #61 pass, including moving an
  existing option between groups.

#### Query, search and pagination (#33, #34, #40, #58, #59)

- **Resumable iteration past the 10,000-row pagination ceiling (#40).** Opt-in
  `dataSources.iterateAllRows(...)` (`Flow<Page>`) and `collectAllRows(...)` drain a large
  data source by windowing on a monotonic key — `RowIterationKey.CreatedTime` by default, or
  `UniqueId` for guaranteed progress. Plain `query` still throws `QueryResultLimitReached`.
  The drain is **not a snapshot**: rows created, deleted or edited mid-drain may be missed or
  included, and a single `created_time` bucket holding over 10,000 rows raises
  `IterationStalled` rather than looping. Verified live against an 11,000-row data source:
  all rows drained past the cap, zero duplicates, `unique_id` property sorts confirmed.

- **`views.iterateAllRows(...)`/`collectAllRows(...)` drain every row behind a view (#59).**
  A view query cannot be windowed (`POST /v1/views/{id}/queries` accepts nothing but
  `page_size`), so, following Notion's own guidance, the drain resolves the view to its
  `data_source_id` and re-runs the windowed data source query with the view's `filter`. The
  view's `sorts` are replaced by the iteration-key sort; `quick_filters` and group/sub-item
  scoping are not replicated (such a view yields *more* rows here than it displays); full
  `Page` objects are emitted. #40's limitations carry over. A view with no `data_source_id`
  (a dashboard) raises `ValidationError`. The shared engine is `WindowedRowIteration`.

- **`is_archived` on data source query and `filter.in_trash` on search (#33)** — reaching
  trashed and archived rows for the first time, with `isArchived()`/`inTrash()` DSL surface,
  plus convenience overloads (#58): `query`/`queryAsFlow`/`queryFirstPage(dataSourceId,
  isArchived)` and `SearchApi.search(query, inTrash)`.

- **`filter_properties` on page create, update and retrieve (#34)** and on data source
  `query`/`queryAsFlow`/`queryFirstPage` (#58), emitted as repeated query parameters and
  validated client-side against the API's documented 100-ID cap. Property IDs are
  percent-decoded before transmission, so the schema shape (`%7DVpb`) and the view-response
  shape (`}Vpb`) produce identical requests.

#### Async operations (#38, #59)

- **Async task polling (#38).** New `client.asyncTasks` with `retrieve`, `waitForCompletion`
  (honouring the server's `poll_after_seconds` hint when it is longer than the configured
  interval) and `pollAsFlow`. New `replaceContentAsync`/`updateContentAsync` return a sealed
  `AsyncMarkdownResult`, since the API only *may* go async. Errors surface as
  `AsyncTaskException`. Real 202s were observed live in the #61 pass, on both markdown PATCH
  and page create.

- **Pages can be created from Markdown, synchronously or asynchronously (#59).** New
  `createFromMarkdown(parent, markdown, title)`, plus `createFromMarkdownAsync(...)`/
  `createAsync(request)`/`createAsync { }` returning a sealed `AsyncPageCreateResult`
  (`Accepted(task)` on 202, `Completed(page)` on 200 — the API decides, a 202 cannot be
  forced). `create` rejects `allowAsync = true` and `createAsync` rejects a request with no
  `markdown` body. `AsyncTask` gained `pageResultOrNull()`, and both result accessors check
  the payload's `object` field.

#### Webhooks (#41)

- **`verifyWebhookSignature(...)`** computes HMAC-SHA256 over the **raw** body and compares
  with `MessageDigest.isEqual`; it accepts only `ByteArray`/`String`, so a re-serialized
  model — which silently breaks the HMAC — cannot be passed by accident. Typed `WebhookEvent`
  models with an `UNKNOWN` event-type fallback, plus a worked Ktor receiver in
  `docs/webhooks.md`. Note the scheme has no replay protection: Notion sends no timestamp
  header (mitigation guidance: dedupe on event `id`).

#### Dates: read accessors that say which time they give you (#30)

- Reading a Notion date value answers one of three different questions, and the accessor name
  now states which one:

  | Question | Accessor |
  |----------|----------|
  | wall-clock digits, as stored | `wallClockDateTime` / `endWallClockDateTime` |
  | absolute instant in UTC | `utcInstant` / `endUtcInstant` |
  | the value's own stored UTC offset | `storedOffset` / `endStoredOffset` |

  `storedOffset` is new capability, not a rename: nothing previously exposed the offset a
  value actually carries — the question you have to ask to detect that a value has drifted.

- **`offsetIn(timeZone)` / `endOffsetIn(timeZone)`** return the offset a named zone would
  have had at the value's *own* wall-clock date and time, DST included. Comparing it against
  `storedOffset` is the audit: a mismatch means the value is not the local time in that zone
  it is supposed to be.

- **`requireUtcInstant()` / `requireEndUtcInstant()`** return the UTC instant or throw
  `IllegalArgumentException` naming the offending value, for call sites where a missing
  instant is a bug rather than an absence.

#### DSL conventions: one nesting rule, one parent convention (#81)

The documented form of the DSL did not compile: twenty snippets across the repo wrote
`parent { … }` / `icon { … }` / `cover { … }` for builders exposed only as receiver
properties — six user-facing, fourteen in KDoc, two of those inside *runtime error messages*.
Nothing breaks here; everything is additive or deprecated-and-kept:

- **Every single-value nested builder accepts a lambda as well as the receiver form.**
  `parent { dataSource(id) }` and `parent.dataSource(id)` both compile and are
  last-call-wins. Covers `parent`/`icon`/`cover`/`template`/`position` across the page,
  database, data source, comment and view request builders.
- **`parent` builders on the two surfaces that lacked one:** `dataSources.create` gains
  `parent.database(id)`; `views.create` gains `parent.database(id, position)`,
  `parent.dashboard(id, placement)` and `parent.newDatabase(pageId, afterBlockId)`.
- **[`docs/dsl-conventions.md`](docs/dsl-conventions.md)** — the rule, written down:
  collection-shaped nesting takes a lambda, single-value nesting supports both (docs teach
  the receiver form), parents are always `parent.<object>(id)`.
- **`DocumentedSnippetsTest`** compiles the documented forms as real code, so a builder change
  that would falsify a README snippet fails the build instead of the reader.
- All twenty snippets rewritten; the decision (deprecate-and-keep rather than rename on a
  pre-1.0 surface) is recorded in
  [ADR 0003](docs/adr/0003-dsl-nesting-and-parent-addressing.md).

#### Errors and resilience (#32, #58)

- **HTTP 529 Service Overload is retried (#32)**, on the same `Retry-After`-honouring path as
  429 rather than falling through to the caller as a bare `ApiError`.
- **`NotionException.ServiceOverloadedError` (#58)** — a dedicated exception for a 529 that
  exhausts `NotionRateLimit`'s retries, carrying `retryAfterSeconds` from the final attempt.
  The per-API error-mapping boilerplate was centralized into a shared
  `HttpResponse.toNotionApiError()` so the new type reaches every endpoint.

#### Everything else

- **`property(name, PagePropertyValue)`** on the page-properties builder: a deliberate,
  documented escape hatch that sets a raw pre-built value, bypassing validation.
- Generated record links and documentation examples use `app.notion.com` (#37); parsing
  remains tolerant of legacy `notion.so` URLs, pinned by tests.
- **`integrationTest` and `testAll` Gradle tasks exist now (#56)** — CLAUDE.md had documented
  them for months while `build.gradle.kts` had only `test`. `test` excludes `RequiresApi`,
  `integrationTest` includes it and forwards credentials from a gitignored `.env` (see
  `.env.example`), setting `NOTION_RUN_INTEGRATION_TESTS=true` itself.
- **A live-API verification pass (#61)** over the catch-up features, as new integration
  specs: `AsyncTasksIntegrationTest`, `StatusGroupsIntegrationTest`,
  `WindowedDrainIntegrationTest`, `FormulaWritesIntegrationTest`, `HtmlEmbedIntegrationTest`,
  `MarkdownUnknownBlockCountIntegrationTest`, `IconCoverSupportIntegrationTest`.
- **Vendored Notion API reference refreshed (#60):** `filter_properties`, page-markdown
  endpoints, async tasks, status option groups, the full 2026-03-11→08-20 changelog, and the
  webhooks reference — agents no longer need to web-fetch the live changelog to confirm field
  shapes.

### Fixed

- **`UniqueIdValue.number` is now nullable.** Observed live: while Notion asynchronously
  backfills IDs after a `unique_id` property is added to an existing data source, rows carry
  `{"prefix": null, "number": null}` — previously this crashed deserialization of the entire
  query page. Also documented on `RowIterationKey.UniqueId`: rows still awaiting backfill are
  silently excluded from a `unique_id`-keyed drain by the server-side window filter.
- **Uncomputable formula and rollup values no longer break page deserialization** (#36, #57) —
  see the breaking-changes section.
- **`DatabaseProperty.id` comes back percent-decoded from a `DataSource`'s schema** (#58,
  closes `IDEAS.md` #5) — the fix now lives at the deserialization source
  (`DatabasePropertySerializer`), with the old `PagesApi` workaround promoted into a shared
  `PropertyIds.decode` util so IDs copied from raw pre-fix responses still work.
- **`EnhancedFileUploadApi.waitForFileReady` fails fast on `EXPIRED` and `FAILED`** (#68)
  instead of polling to the timeout.
- **`unknown_block_count` corrected to inferred-and-unobserved (#35, #61).** The raw REST
  page-markdown response carries `truncated` and `unknown_block_ids` but no
  `unknown_block_count` (verified live on a non-truncated page); the field stays as a
  defensive default-0 with amended KDoc. `unknown_block_ids` is capped at 50 by the API, so
  its size is not a substitute for the count.
- **`DataSource.description` defaults to `emptyList()`** (#56) — the official
  `partialDataSourceObjectResponse` shape genuinely omits it, and decoding the vendored
  fixture previously threw `MissingFieldException`.
- **Documentation that showed a DSL that does not exist:** `docs/file-uploads.md`'s
  fictional `image { file { uploadedFile(id) } }` corrected to the real
  `imageFromUpload(id, caption)`; `docs/error-handling.md`'s Rate Limiting section resynced
  against the actual `RateLimitConfig`/`NotionRateLimit` shape with the 429/529/502/503/504
  retry matrix (#58); stale KDoc claiming status groups cannot be configured via the API
  corrected (#39).
- **Vendored file-upload samples used a nonexistent `archived` key** — the real key is
  `in_trash`; the model's existing `@SerialName("in_trash")` was right all along. Likewise
  the stale `patch_update_a_database.json` sample (`value` → `expression`, plus three JSON
  syntax bugs).

### Changed

- Date values with sub-second precision now keep it. The old accessors stripped fractional
  seconds before parsing, so `…T14:30:00.123Z` read back as `14:30:00`. Notion returns
  `.000` for every value it stores, so this is invisible in practice.

### Deprecated

All still compile and behave identically, all carry `ReplaceWith`, and all are scheduled for
removal at 1.0.

- **`icon.file(url, expiryTime)` and `cover.file(url, expiryTime)` on every request builder
  (#69).** These emit `type: "file"` — the *read* shape — which the write side rejects with
  HTTP 400 for every input; there is no input for which these calls succeed. Use
  `external(url)` or the new `upload(...)`.
- **Date read accessors renamed (#30):**

  | Deprecated | Replacement |
  |------------|-------------|
  | `localDateTimeNaive` | `wallClockDateTime` |
  | `endLocalDateTimeNaive` | `endWallClockDateTime` |
  | `instantValue` | `utcInstant` |
  | `endInstantValue` | `endUtcInstant` |
  | `toLocalDateTime(timeZone)` | `localDateTimeIn(timeZone)` |
  | `endToLocalDateTime(timeZone)` | `endLocalDateTimeIn(timeZone)` |

- **Parent addressing brought onto the one convention (#81):**

  | Deprecated | Use |
  | --- | --- |
  | `parent.pageId(id)` / `parent.blockId(id)` (comments) | `parent.page(id)` / `parent.block(id)` |
  | `databaseId(id)` (data sources) | `parent.database(id)` |
  | `database(id, position)` (views) | `parent.database(id, position)` |
  | `dashboard(id, placement)` (views) | `parent.dashboard(id, placement)` |
  | `createDatabase(pageId, afterBlockId)` (views) | `parent.newDatabase(pageId, afterBlockId)` |

  `views.create` keeps `dataSourceId(id)` flat on purpose — it names the data source a view
  *reads from*, not what the view hangs off.

### Unchanged, deliberately

`utcInstant` still returns null — rather than throwing — for a value that is absent,
date-only, offset-less or malformed. Notion returns an offset for every time-bearing value,
so in practice null means "no time here", and throwing from a property getter would break
every existing caller. The strictness is opt-in via `requireUtcInstant()`, which
distinguishes the four cases in its message.

## [0.5.0] - 2026-05-31

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