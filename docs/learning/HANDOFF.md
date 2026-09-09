# Handoff: learning series for `kotlin-notion-client`

A brief for an agent session whose whole job is to write six standalone HTML lessons about
this repository. Nothing here asks for production code changes.

## Who the reader is

The repository's author. An experienced software engineer, intermediate-to-strong Kotlin,
several years in the JVM world. He has built this library largely through AI-assisted
development and wants to close the gap between *what exists in the repo* and *what he
could rebuild or defend from memory*.

Consequences for tone:

- **Never explain what a coroutine is, what a data class is, or what generics are.** Explain
  what `Mutex` held *across* a `delay` does to fairness, and why that choice was made here.
- Assume he can read Kotlin faster than prose. Lead with the code, then say why.
- He knows JVM/JSON/HTTP fundamentals. He does *not* necessarily know
  `kotlinx.serialization`'s encoder API, Ktor's client plugin pipeline phases, or
  `kotlinx-coroutines-test` virtual time in detail. Those are fair game to teach properly.
- This library is **published to Maven Central**. Public API surface is a real constraint,
  not a hypothetical. Treat it as one.

## Non-negotiable rules

1. **Every code excerpt must exist at the cited path.** Read the file. Quote it. If you
   shorten an excerpt, mark the elision with `// …`. Never compose a plausible-looking
   snippet.
2. **Every behavioural claim must be traceable** to code, an ADR under `docs/adr/`, a
   `journal/` entry, or `CHANGELOG.md`. Cite which. Where the repo's own commentary is
   already excellent (ADR-0002 in particular), quote it rather than paraphrasing it worse.
3. **Lessons are independently readable, in any order.** No "as we saw in lesson 3". A
   one-line cross-link is fine; a dependency is not.
4. **Where a Kotlin/JVM analogy honestly helps, use it. Where it would mislead, say so
   explicitly.** A wrong analogy is worse than none — flag the places where an
   intuition from ordinary JVM serialization or from Jackson actively breaks here.
5. **Do not fix bugs, refactor, or open issues.** If a lesson turns up something genuinely
   wrong, note it in a `## Findings` section at the end of this file and move on.

## Per-lesson skeleton

Follow this structure in every lesson. Target **~2,000 words** of prose (code excerpts on
top of that).

1. **The hook** — a concrete symptom, a real commit, an ADR's opening paragraph. Something
   that happened, not "in this lesson we will learn".
2. **The concept, plainly** — 300–500 words. The mental model, in the author's own words,
   not copied from the official docs.
3. **This code, annotated** — real excerpts with `path:line` references, walked through.
   This is the centre of the lesson and should be the longest part.
4. **The analogy, and its limits** — where a familiar JVM/Kotlin pattern maps, and where it
   stops mapping.
5. **See it yourself** — a command to run, a test to watch fail, a payload to print. Must
   work in this repo as it stands today.
6. **Exercises** — two, tiered:
   - **Verifiable**: has a checkable end state (a test that goes green, output that matches,
     a compile error that appears). State the expected end state explicitly so the reader
     can self-check without an agent.
   - **Extension**: open-ended, no answer key. A design question or a "try it another way".
7. **Further reading** — 2–4 *specific* links: a named doc section, an ADR, a KEEP, a blog
   post that is actually about the mechanism. No broad tutorials, no "the Kotlin docs".

## Output format

- One self-contained HTML file per lesson at `docs/learning/<nn>-<slug>.html`.
- Plus `docs/learning/index.html` — a plain contents page linking all six, with the
  one-sentence summary and difficulty of each.
- **Plain, minimal CSS**, inlined in a `<style>` block. No web fonts, no CDN links, no
  JavaScript. It must render correctly opened as a `file://` URL with no network.
- Use the same small stylesheet in all six so the series is visually consistent. A readable
  measure (~70ch), a monospace block with a light background and horizontal scroll for code,
  visible `<h2>`/`<h3>` hierarchy, and a distinct callout style for the exercises. That is
  the whole design budget — do not spend time on it.
- Syntax highlighting is **not** required. If you want it, hand-span the tokens; do not pull
  in a library.
- Every file path mentioned in prose should appear as `<code>`.

## The six lessons

Ordered by file number, not by difficulty. Difficulty is stated so the index can show it.

---

### 01 — The wire is the contract: `kotlinx.serialization` past `@Serializable`
**Difficulty: medium**

How this library models a JSON shape it does not control.

Anchors:
- `src/main/kotlin/it/saabel/kotlinnotionclient/models/pages/PageProperty.kt` — the sealed
  hierarchy, 20+ variants, `@SerialName` discriminators, the response-vs-request naming
  convention documented in its KDoc header.
- `src/main/kotlin/it/saabel/kotlinnotionclient/models/pages/PagePropertySerializer.kt` — a
  hand-written `KSerializer` that dispatches on `"type"` and falls back to
  `PageProperty.Unknown` with raw JSON preserved.
- Contrast with `JsonContentPolymorphicSerializer` usage elsewhere in the repo
  (`models/base/IconSerializer.kt`, `models/base/ParentSerializer.kt`) — find them and say
  *why* the two approaches coexist.
- `src/main/kotlin/it/saabel/kotlinnotionclient/serialization/NotionJson.kt` — the single
  `Json` configuration the client installs.

Points worth making:
- Notion's discriminator is a sibling `"type"` field naming another key in the same object.
  That is not what `@JsonClassDiscriminator` does, and the reason a hand-written serializer
  exists at all.
- The `Unknown` fallback is a forward-compatibility decision with a cost: unknown data
  round-trips but cannot be acted on. `PagePropertySerializer`'s KDoc is careful to
  distinguish it from Notion's own `"unsupported"` formula/rollup results
  (`FormulaResult.UnsupportedResult`) — that distinction is the lesson's sharpest point.
- Sealed hierarchy + `when` exhaustiveness is the payoff the reader already understands;
  spend the words on what it costs a *published library* (see lesson 06).

Exercises:
- *Verifiable*: add a new `PageProperty` variant end-to-end (model, serializer branch,
  fixture, test) and get it decoding from a sample response. Then delete the serializer
  branch and predict, before running, which test fails and with what message.
- *Extension*: what would it take to make `Unknown` re-encode losslessly, and is that
  actually desirable?

---

### 02 — `explicitNulls = false` and the sentinel pattern
**Difficulty: hard. This is the most valuable lesson in the repo — write it first.**

`docs/adr/0002-explicit-null-payloads.md` is the source text and it is already very well
written. Quote it liberally; the lesson's job is to teach the *mechanism* the ADR assumes.

The story: `icon.remove()` compiled, ran, returned a `Page`, and changed nothing — for
months — while a test asserted `request.icon shouldBe null`, byte-identical to the assertion
for a request that never mentioned the icon. The sibling case (clearing a `select`) failed
*loudly* with an HTTP 400. Two failure modes from one root cause.

Anchors:
- `docs/adr/0002-explicit-null-payloads.md`
- `src/main/kotlin/it/saabel/kotlinnotionclient/serialization/ExplicitNullSerializers.kt` —
  `RemovalSentinelSerializer`, `ClearableValueSerializer`.
- `src/main/kotlin/it/saabel/kotlinnotionclient/models/base/IconSerializer.kt` and
  `models/pages/PageCoverSerializer.kt` — the explicit-dispatch-on-encode decision, and the
  `object`-variant-emits-`{}` hazard it avoids.
- The tests the ADR names: `ExplicitNullPayloadSerializationTest`,
  `ExplicitNullPayloadIntegrationTest`. Find them and show what a wire-level assertion looks
  like next to the model-level one that was blind.

Points worth making:
- `encodeNullableSerializableElement` is where `explicitNulls = false` does its dropping.
  `encodeSerializableElement` with a nullable payload serializer is not. That single
  distinction is the whole fix; make sure the reader could re-derive it.
- Three meanings collapsed into one Kotlin `null`: "absent from this request", "set this to
  JSON null", "the API returned null". The sentinel restores the distinction *as a type*.
- The generalisable lesson: **a test that asserts on a model cannot see a bug that lives
  between the model and the wire.** Name the class of bug. It applies far beyond this repo.
- Cover the alternatives the ADR rejected and *why* — the blast-radius argument against
  flipping `explicitNulls = true` is a good worked example of scoping a fix.

Exercises:
- *Verifiable*: write a failing test that pins the *old* broken payload for one of the seven
  clearable setters, then make it pass with the current code. You should see the exact
  moment the assertion changes from "the field is null" to "the encoded bytes are `…`".
- *Extension*: ADR-0002 says a third sentinel instance would be cheap. Design one for a case
  that does not exist yet. Where does the mechanism start to strain?

---

### 03 — Type-safe builders, and why five authors got the DSL wrong
**Difficulty: easy–medium**

`docs/adr/0003-dsl-nesting-and-parent-addressing.md` is the source. The evidence is
extraordinary and should open the lesson: **twenty snippets across the repo used
`icon { … }` where only `icon.emoji(…)` compiled** — six user-facing, fourteen in KDoc, and
two of those fourteen were *runtime error messages* telling a user to write code that does
not compile. Five independent authoring passes reached for the same wrong form.

Anchors:
- `docs/adr/0003-dsl-nesting-and-parent-addressing.md` and `docs/dsl-conventions.md`
- `src/main/kotlin/it/saabel/kotlinnotionclient/models/datasources/DataSourceQueryBuilder.kt`
  — `dataSourceQuery { }` as `Builder().apply(block).build()`
- `src/main/kotlin/it/saabel/kotlinnotionclient/models/pages/CreatePageRequestBuilder.kt`
- `src/main/kotlin/it/saabel/kotlinnotionclient/models/views/ViewRequestBuilder.kt` — the
  **only** `@DslMarker` in the codebase. Interrogate that: what does it buy there, what does
  its absence cost everywhere else, and would applying it repo-wide be a breaking change?

Points worth making:
- `Builder.() -> Unit` (function type with receiver) is the whole mechanism; make sure the
  reader could write one from scratch.
- The arity convention: collections take a lambda, single values take a receiver property
  *and* a same-named function. A `val` and a `fun` of one name coexist because they live in
  different namespaces — worth a sentence on JVM signatures.
- The real lesson is about **affordance design**: if five careful authors reach for a form,
  the API is wrong, not the authors. That generalises well beyond DSLs.

Exercises:
- *Verifiable*: pick a single-value builder that still exposes only one form (if any remain)
  and add the other; or write the compile-error reproduction for the pre-ADR shape and
  confirm the message a user would have seen.
- *Extension*: `@DslMarker` everywhere — sketch the migration and the cases where it would
  reject code that is currently legal and reasonable.

---

### 04 — Coroutine primitives in anger: the rate limiter
**Difficulty: hard**

Anchors:
- `src/main/kotlin/it/saabel/kotlinnotionclient/ratelimit/TokenBucket.kt`
- `src/main/kotlin/it/saabel/kotlinnotionclient/ratelimit/NotionRateLimit.kt`
- `src/main/kotlin/it/saabel/kotlinnotionclient/ratelimit/RateLimitConfig.kt`
- `src/test/kotlin/unit/ratelimit/` — `TokenBucketTest`, `TokenBucketPluginTest`,
  `RetryAfterPluginTest`, `VirtualTimeProbeTest`, `MaxRetriesWallTimeTest`
- `journal/2026_05_30_01_RateLimiting_Diagnosis.md`

Points worth making:
- **Continuous refill**, not periodic batches — read the arithmetic in `refill()` and say why
  fractional tokens matter.
- The `Mutex` is held **across the `delay`**. That is what makes acquisition FIFO-fair and
  starvation-free, and it is the sort of decision that looks like a bug to a reviewer who
  has not thought about it. Explain both readings.
- Ktor's client plugin API: `createClientPlugin` and the `Send` phase, and why hooking the
  pipeline beats wrapping every call site. Note that retries re-acquire a token.
- Failure classification **by type, never by string-matching `error.message`** — the KDoc
  says so explicitly. Contrast with what a naive `catch` would do. `500` deliberately not
  retried; `529` takes the header-driven path.
- The testing half is half the lesson: `timeSourceMillis` as an injected seam,
  `kotlinx-coroutines-test` virtual time, and what `VirtualTimeProbeTest` is actually
  guarding (that the test clock is really virtual — the same species of vacuous-pass guard
  that festival-scripts uses in its architecture tests).

Exercises:
- *Verifiable*: drive the bucket in a `runTest` and predict, before running, the exact
  virtual timestamps at which acquisitions 1..N return for a given `sustainedRate` /
  `burstCapacity`. Assert them.
- *Extension*: one bucket per `NotionClient` is deliberate ("never as a process-global
  singleton"). What breaks if two `NotionClient`s share a Notion integration token? What
  would a correct shared-bucket design look like?

---

### 05 — Pagination as a cold Flow
**Difficulty: medium**

Anchors:
- `src/main/kotlin/it/saabel/kotlinnotionclient/utils/Pagination.kt` — `PaginatedResponse<T>`,
  the `PageFetcher<T, R>` typealias, `flow { }`
- `src/main/kotlin/it/saabel/kotlinnotionclient/api/WindowedRowIteration.kt` and
  `models/datasources/RowIterationKey.kt`
- `reference/js/` — the vendored `iterateAllDataSourceRows` helper from `notion-sdk-js`, with
  origin and licence noted in its README. Cross-SDK comparison is genuinely interesting here.
- `docs/pagination.md`, `journal/2026_04_15_02_Pagination_Limit_Investigation.md`

Points worth making:
- Cold vs hot, concretely: nothing is fetched until collection, each collector re-fetches,
  and cancelling the collector cancels the HTTP call in flight. Show what that means for a
  cursor chain.
- Why `Flow` rather than `Sequence` (suspension) and rather than returning `List` (memory,
  and the ability to stop early).
- When `collectAll` *is* the right answer — most callers want the list; be honest that the
  Flow is the smaller use case.
- Windowed iteration exists because a long cursor chain over a mutating data source can miss
  or duplicate rows. Explain the failure it prevents.

Exercises:
- *Verifiable*: use `MockEngine` (see the repo's mock builders) to serve a three-page cursor
  chain, then collect with `take(1)` and assert only one HTTP request was made.
- *Extension*: what would a resumable iteration look like — one that survives a process
  restart? What would it need from `RowIterationKey`?

---

### 06 — Being a library, not an application
**Difficulty: medium**

The constraint the reader actually lives under and probably reasons about least explicitly.

Anchors:
- `build.gradle.kts` — Dokka, signing, `mavenCentralPublishing`, the `-PskipSigning` local
  path
- `CHANGELOG.md` — read the 0.6.0 entries; the README openly says several fixes there
  *correct bugs earlier AI-assisted releases introduced*. That candour is the lesson's spine.
- `docs/adr/0002-explicit-null-payloads.md` §Consequences — "Icon and PageCover gain a
  variant. Both are sealed and public, so an exhaustive `when` in user code stops compiling."
- The `@file:Suppress("unused")` file headers scattered through `models/` — name the habit
  and what it hides.
- `IDEAS.md`, `FOLLOWUPS.md`, `CONTRIBUTING.md`

Points worth making:
- **Adding a variant to a public sealed hierarchy is a source-breaking change.** The reader
  has already done this once. Walk the actual blast radius, and what a deprecation cycle
  would have looked like instead.
- `internal` is a real tool and this library uses it sparingly (`TokenBucket` is `internal` —
  note the contrast). What *should* be internal that currently is not?
- Binary compatibility: `binary-compatibility-validator` is not configured here. Explain what
  it would catch that the compiler and the tests do not, and let the reader decide whether to
  adopt it. (This is a recommendation, not a task — do not add it.)
- Semver for a 0.x library: what promises are actually being made, and to whom.
- The AI-assisted-development notice in `README.md` is a genuinely unusual piece of API
  documentation. Discuss it as a design decision about trust.

Exercises:
- *Verifiable*: pick one public type that looks like it should be internal, mark it
  `internal`, and compile. The errors are the map of your real API surface.
- *Extension*: draft the deprecation cycle ADR-0002 could have used instead of a hard break.
  What does it cost, and was the hard break right?

---

## Findings

*(Leave empty. Append anything the lessons turn up that looks genuinely wrong, with a file
reference and one sentence. Do not fix it.)*
