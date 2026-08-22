# ADR 0003: Nesting shape follows arity, and parents are always `parent.<object>(id)`

- **Status:** accepted
- **Date:** 2026-08-22
- **Issue:** [#81](https://github.com/jsaabel/kotlin-notion-client/issues/81) (second of the #80/#81/#82 DSL-gap series)

## Context

The request DSLs grew one builder at a time, and two conventions drifted apart without anyone
deciding they should.

**Nesting.** Collection-shaped nesting takes a lambda — `properties { … }`, `content { … }`,
`files { … }`, `richText { … }`. Single-value nesting takes a receiver property —
`icon.emoji("📄")`, `parent.dataSource(id)`. That split is defensible, but it was never written
down, and the evidence says nobody infers it: **twenty snippets across the repo wrote the lambda
form for a single-value builder, and none of them compiled.** Six were user-facing
(`README.md`, `QUICKSTART.md`, `docs/notebooks.md`, `docs/rich-text-dsl.md`,
`docs/data-sources.md`), fourteen were KDoc, and two of those fourteen were *runtime error
messages* telling a user to write `icon { upload(id) }` — a repair instruction that does not
compile. Five separate authoring passes independently reached for the lambda.

`journal/2025_10_16_Developer_Experience_Exploration.md` logged this as FINDING #4 and left it
open — "Decide: support `parent { … }` lambda or document why not." Ten months on, the drift had
kept accruing.

**Parent addressing.** Five surfaces spelled "what does this hang off" five ways:

| Surface | Before |
| --- | --- |
| `pages.create` | `parent.page/dataSource/block/workspace(…)` |
| `databases.create` | `parent.page/block/workspace(…)` |
| `comments.create` | `parent.pageId(…)` **and** `parent.page(…)` — both, aliased |
| `dataSources.create` | `databaseId(…)` — flat, no parent builder |
| `views.create` | `database(…)` / `dashboard(…)` / `createDatabase(…)` — flat |

`CreateCommentRequestBuilder` carried both spellings with a KDoc reading "alias for pageId for
consistency with other DSLs": the drift was noticed and papered over rather than converged.

## Decision

### 1. Nesting shape follows arity — and single-value builders support both forms

- **Collection-shaped nesting takes a lambda.** `properties`, `content`, `files`, `richText`.
  There is no receiver-property form and there should not be: the braces group many calls.
- **Single-value nesting supports both.** Every single-value nested builder now exposes a `val`
  *and* a same-named `fun` taking a lambda. A `val` and a `fun` of the same name coexist in
  Kotlin without a JVM signature clash — they live in different namespaces — so this is purely
  additive.

  ```kotlin
  val parent = ParentBuilder()
  fun parent(block: ParentBuilder.() -> Unit) { parent.block() }
  ```

  Both drive the same builder and are last-call-wins, so there is no semantic difference between
  them.

- **Docs teach the receiver form.** `parent.dataSource(id)`, `icon.emoji("📄")`. A single-value
  builder is called exactly once in almost every real request, and braces around a single call
  are noise. The lambda exists so that the form everyone reaches for is not a compile error, and
  for the occasional builder that does take several calls.

Applied to: `CreatePageRequestBuilder` (`parent`, `icon`, `cover`, `template`, `position`),
`UpdatePageRequestBuilder` (`icon`, `cover`, `template`), `DatabaseRequestBuilder` (`parent`,
`icon`, `cover`), `UpdateDataSourceRequestBuilder` (`icon`), `CreateCommentRequestBuilder`
(`parent`), and the two parent builders introduced below.

### 2. One parent convention: `parent.<object>(id)`

The accessor is named after the **object**, not after its id — `page`, not `pageId` — matching
the majority form and reading as one phrase.

- `CreateDataSourceRequestBuilder` gains `parent.database(id)`; flat `databaseId(id)` is
  deprecated.
- `CreateViewRequestBuilder` gains `parent.database(id, position)`,
  `parent.dashboard(id, placement)` and `parent.newDatabase(pageId, afterBlockId)`; the flat
  `database`, `dashboard` and `createDatabase` are deprecated.
- `CreateCommentRequestBuilder` keeps `page`/`block` as canonical; `pageId`/`blockId` become
  deprecated aliases rather than co-equal spellings.

**`views.create` keeps `dataSourceId(…)` flat, deliberately.** A `CreateViewRequest` carries a
`dataSourceId` *and* one of `databaseId` / `viewId` / `createDatabase`. Only the second group
answers "what does this view hang off"; `dataSourceId` names the data source the view **reads
from**. Filing it under `parent` would make the DSL assert something untrue about the request it
builds, so the convention deliberately does not reach it.

**Deprecate and keep, not rename.** These builders shipped in v0.4.0–v0.5.0. The library is
pre-1.0 and a straight rename would have been defensible, but a `@Deprecated` with `ReplaceWith`
costs one annotation and buys an IDE-assisted migration, and there is no version in which the
old spelling silently changes meaning. The aliases are scheduled for removal at 1.0.

## Consequences

- All twenty snippets are true, and were rewritten to the documented receiver form. Two of them
  were *also* stale in other ways and were corrected here: `dataSourceId` → `dataSource`, and
  `docs/notebooks.md`'s `parent { databaseId(dbId) }`, which predates 2025-09-03 — pages have
  hung off data sources since.
- The rule is written down in [`docs/dsl-conventions.md`](../dsl-conventions.md), which is the
  half of this ADR that new builders are expected to read.
- `DocumentedSnippetsTest` compiles the documented forms as real code, so a snippet in the
  README can no longer drift away from the DSL without the build noticing.
- Deprecation warnings are the migration surface for existing callers. Nothing breaks at this
  release.
