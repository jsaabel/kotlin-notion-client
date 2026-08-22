# ADR 0001: Deferred file uploads resolve as pending-upload sentinels in the request tree

- **Status:** accepted
- **Date:** 2026-08-22
- **Issue:** [#70](https://github.com/jsaabel/kotlin-notion-client/issues/70) (third of the #68/#69/#70 file-upload convenience series)

## Context

Attaching a local file anywhere in Notion requires a file-upload id, and the id only exists
after a network round-trip. Every builder DSL in this client is a plain (non-suspend) lambda,
so no upload can happen inside one. The result is the hoisting ceremony this series set out to
remove: upload N files above the call, unwrap N results, thread N ids back into the builder.

The one-call helpers from #69 (`blocks.appendImage(pageId, File(…))`, `pages.attachFiles`,
`pages.setIcon`, …) fix the single-attachment case against an *existing* target. They cannot
fix the compound case: a `pages.create` with mixed content is one request, so every attachment
must be resolved before the request is serialized.

The motivating constraint is Notion's **1-hour attach window**: an upload that is not attached
within an hour of creation expires. Upload-and-attach therefore wants to be one logical
operation — the client should never encourage a workflow where ids sit around waiting.

Four attachment surfaces share the problem:

1. **Content blocks** — image/video/audio/file/pdf/embed inside `content { }` /
   `appendChildren { }` / `blocks.update { }` (`PageContentBuilder`)
2. **"Files & media" page properties** at page-create time (`FilesBuilder`; the update path is
   already served by `pages.attachFiles`)
3. **Icon and cover** at page-create time (`IconBuilder`/`CoverBuilder`; update path served by
   `setIcon`/`setCover`)
4. **Comment attachments** inside `comments.create { }` (`CommentsApi.kt` carries an explicit
   deferral note pointing at #70)

### Facts about the current code that shape the decision

- `PageContentBuilder.() -> Unit` appears as a nested `children` parameter **42 times** in
  `PageContentBuilder.kt` alone; `BlockRequest` has **32 subtypes**, **17** of which carry a
  `children: List<BlockRequest>?` slot.
- Nested children are materialized **eagerly through a fresh builder**: every container block
  does `children?.let { pageContent(it) }` (`PageContentBuilder.kt:865` and siblings), then
  discards the child builder. Any state recorded on a builder instance is lost for nested
  content.
- All API entry points that consume built requests are already `suspend`
  (`BlocksApi.appendChildren`/`update`, `PagesApi.create`/`createAsync`/`update`,
  `CommentsApi.create`), and the builder overloads delegate to the raw-request overloads.
- Rate limiting and retries live in a Ktor `Send`-phase plugin (`NotionRateLimit`) installed on
  the shared `HttpClient` — every outbound request is throttled with no call-site wrapping.
- #69 established `uploadAndAwait` (`api/UploadAndAttach.kt`) as the throwing
  create → send → wait primitive, and `FileSource` (`utils/FileUploadUtils.kt`) as the
  input abstraction (`File`/`Path`/`ByteArray`/`InputStream`).

## Decision

Keep the DSLs synchronous (issue option **3b**). Builders gain overloads that accept a
`FileSource` (plus `File`/`Path`/`String`-HTML conveniences) and emit a **pending-upload
sentinel** — a new variant of the existing sealed request hierarchy that carries the
`FileSource` instead of a file-upload id:

- `BlockRequest.PendingUpload(kind, source, caption, name, options)` — one subtype with a
  `kind` enum (IMAGE, VIDEO, AUDIO, FILE, PDF, HTML), not six new subtypes
- `FileObject.PendingUpload(source, name, options)` for files properties
- `Icon.PendingUpload(source, options)` / `PageCover.PendingUpload(source, options)`
- a pending variant for comment attachments

Because the sentinel lives **in the built tree**, it survives the eager nested-builder
materialization with zero plumbing, and it travels through `CreatePageRequest` (children,
properties, icon, cover) without any signature change.

The suspend API layer resolves sentinels in the **raw-request funnels** (which the builder
overloads already delegate to): collect sentinels from the request in document order, upload
them concurrently via `uploadAndAwait`, rewrite the tree substituting
`FileUploadReference(id)`, then validate and send as before.

Each sentinel is annotated `@Serializable(with = …)` with a serializer whose `serialize()`
**throws** with an actionable message ("this content contains a file pending upload
(chart.png); pass it through a NotionClient method, or upload first and use
`imageFromUpload(id)`"). This satisfies kotlinx.serialization's sealed-hierarchy requirements
at compile time while making any un-resolved serialization path fail loudly.

### The issue's design points, answered

- **What `pageContent {}` returns:** unchanged — `List<BlockRequest>`. Sentinels are legal
  list members; every consumption path either resolves them (all API entry points are suspend)
  or fails loudly (throwing serializer, plus an "unresolved pending upload" violation added to
  `RequestValidator` as defence-in-depth). **Not source-breaking** — the distinct-return-type
  option is rejected as an unnecessary break. As a bonus, the raw path
  `pageContent { image(File(…)) }` + `appendChildren(id, blocks)` *works* instead of throwing,
  because resolution hooks the raw funnel.
- **Concurrency:** uploads within one call run concurrently via structured concurrency
  (`coroutineScope { map { async { uploadAndAwait(…) } } }.awaitAll()`). This already flows
  through the shared rate-limit pipeline — `NotionRateLimit` throttles at the `Send` phase —
  so no custom pipeline is needed. A small `Semaphore` (4) bounds concurrent multipart
  uploads' memory and open streams.
- **Partial failure:** fail fast. The first failed upload cancels its siblings (structured
  concurrency) and aborts the whole operation with a thrown `FileUploadError` *before* the
  create/append request is sent. Already-completed uploads are orphaned deliberately: Notion
  documents no delete-upload endpoint, and unattached uploads expire after 1 hour. Documented,
  not cleaned up.
- **`blocks.update`:** unchanged interaction — `require(blocks.size == 1)` first, then the
  same resolution pass on the single block.
- **Identity, not equality:** the resolver maps sentinel → resolved id by object identity
  (in-order collection / `IdentityHashMap`), so the same `File` attached twice uploads twice.
  Whether one file-upload id may legally be attached to multiple blocks is unverified against
  the live API; deduplication is an explicit non-goal until it is.

## Alternatives considered

- **3a — suspend the DSL.** `suspend PageContentBuilder.() -> Unit` forces all 42 nested
  `children` parameters (and the page/comment/icon/cover builders) to become suspend,
  effectively forking the builder API, and interleaves network I/O with what currently reads
  as pure construction. Rejected on cost and on principle.
- **Builder-held registry + placeholder ids** (the naive 3b): the builder records
  `PendingUpload`s in a side list and emits placeholder ids like `"pending:{token}"`. Killed
  by the eager nested-builder fact above — nested pendings are silently lost unless a shared
  registry is threaded through all 42 sites, and raw-list overloads remain a trapdoor where
  placeholders leak into requests.
- **`@Transient` source field on the six file content classes** (`ImageRequestContent` etc.):
  survives nesting like the sentinel does, but hand-serialization *silently drops* the field
  and emits `"type": "file_upload"` with no reference — an invalid payload with no local
  error. Fails the fail-loud requirement.
- **JSON-level substitution after typed serialization** (serialize to `JsonElement`, walk
  generically, replace placeholder ids): avoids the typed rewrite pass but complicates the
  send path, bypasses the typed validator, and still needs a typed collection walk because
  `FileSource` never reaches the JSON. More moving parts for no capability gain.

## Consequences

- New-feature CHANGELOG entry only; **no breaking change** (contrary to the issue's
  expectation that `pageContent {}`'s return type would have to change).
- Adding a `BlockRequest` subtype breaks every exhaustive `when` over the hierarchy
  (`RequestValidator`, and any converter) — a compile-time checklist, not a risk, but it is
  real implementation volume alongside the ~32-subtype rewrite walk with recursion into the
  17 `children` slots.
- The sentinel pattern repeats naturally across `FileObject`, `Icon`, `PageCover` and comment
  attachments, so one resolver serves all four surfaces; page create becomes fully one-call
  (`content` + files property + icon + cover in a single request with local files).
- `EnhancedFileUploadApi` remains the only upload implementation; the resolver composes
  `uploadAndAwait` and must not duplicate upload logic.
- Sentinels are public API (builders emit them), documented as "resolved by the client before
  sending"; constructing one by hand and serializing it yourself throws by design.

## Scope and staging

The mechanism is designed for all four surfaces. Implementation landed in two stages so each
PR stayed reviewable: **(1)** the resolver plus the content-block surface — the core of #70,
delivered in [#75](https://github.com/jsaabel/kotlin-notion-client/issues/75) — and **(2)** the
remaining surfaces (files property, icon/cover at create time, comment DSL attachments), which
reuse the stage-1 machinery and pay off the deferral note in `CommentsApi`, delivered in
[#76](https://github.com/jsaabel/kotlin-notion-client/issues/76).

Stage 2 widened resolution from a block list to a whole request: a page create pools the
sentinels from its properties, icon, cover and children into one upload pass, in that order, so
the failure boundary is the request rather than the surface. The comment surface needed
`CommentAttachmentRequest` to become a sealed hierarchy to hold its pending variant; the
resolved variant keeps the same JSON and the same construction and read syntax.
