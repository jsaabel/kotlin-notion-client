# v0.6.0 — Release notes (draft)

> Draft of the concise, highlights-only changelog for the v0.6.0 GitHub Release, and the
> source for the README/announcement blurb. The full technical record — every change with
> migration notes — lives in [`CHANGELOG.md`](CHANGELOG.md). Publish plan: `0.6.0-SNAPSHOT`
> first, battle-tested in a downstream consumer (festival-scripts), then the public release.

---

## Highlights

- **Local files everywhere.** Pass a `File` or `Path` straight into the content DSL, icons,
  covers, "Files & media" properties and comment attachments — one call, one concurrent
  upload pass, atomic failure. Plus one-call helpers (`blocks.appendImage(pageId, file)`,
  `pages.setIcon`, `pages.attachFiles`, …) and HTML blocks via `appendHtml`.
- **`databases.update`.** Retitle, re-icon and *move* a database at last — with the
  icon/cover set-vs-clear support matrix established against the live API.
- **Beyond 10,000 rows.** `iterateAllRows`/`collectAllRows` drain data sources *and* views
  past Notion's pagination ceiling via windowed queries.
- **Async support.** Task polling (`client.asyncTasks`), async markdown writes, and page
  creation from Markdown — sync or async.
- **Webhooks.** Constant-time HMAC signature verification and typed event models.
- **Formulas & rollups can be written** — typed configurations, `prop()` expression
  validation, and creatable formula/rollup properties for the first time.
- **API catch-up (through Aug 2026):** HTTP 529 retry, `filter_properties`, trash/archive
  queries, status option groups, `app.notion.com` links, and more.
- **A correctness pass over our own output** (see the AI note below): timezone-safe date
  writes, `icon.remove()` and property clearing that actually reach the wire, a `FileUpload`
  model matching the documented shape, and uncomputable formula/rollup values that no longer
  crash page deserialization.
- **One DSL convention**, documented in `docs/dsl-conventions.md` — and every documented
  snippet now compiles, enforced by a test.

## ⚠️ Breaking changes (headlines)

Full migration notes per item in [`CHANGELOG.md`](CHANGELOG.md).

- **Date writes are offset-preserving, zone-explicit and validating.** `LocalDateTime`
  overloads require a `TimeZone` (the silent UTC default is gone), naive datetime strings
  are rejected, `dateWithTimeZone` is removed.
- **`FileUpload`**: `filename`/`contentType` nullable; `FileUploadStatus` gains
  `EXPIRED`/`UNKNOWN`; `FileUploadError` gains `UploadUnusableError`.
- **Sealed-class additions** (exhaustive `when`s need new arms): `FormulaResult`/
  `RollupResult` gain `UnsupportedResult`, `IncompleteResult` (rollup) and `Unknown`;
  `Icon`/`PageCover` gain `Removed`.
- **Typed schema configs**: `DatabaseProperty.Formula.formula` and
  `DatabaseProperty.Rollup.rollup` are no longer raw `JsonObject`s.
- **`StatusConfiguration.options`** takes `List<CreateStatusOption>`.
- **`SearchFilter.property`** defaults to `null` instead of `"object"`.
- **Behaviour change:** `icon.remove()`/`cover.remove()` and property clearing
  (`select("Status", null)` etc.) now actually work — code that relied on them doing
  nothing will see things disappear.

## Built by AI, verified against the live API

This release was implemented almost entirely by AI agents (Claude Code): issue-driven,
one agent per issue, PR-reviewed onto a shared release branch, and finished with a
dedicated live-API verification pass against a real Notion workspace. We are deliberately
transparent about this — including the fact that several fixes in this release correct bugs
that earlier, equally AI-assisted releases shipped (most seriously the timezone write bug,
and DSL calls whose payloads never reached the wire). The live verification pass exists for
exactly that reason, and itself forced two further model fixes. The full trail — issues,
agent reports, ADRs, `FOLLOWUPS.md` — is in the repository. Please report anything that
looks off: 1,200+ unit tests and a live pass are still not a substitute for your workload.

## Installation

```kotlin
dependencies {
    implementation("it.saabel:kotlin-notion-client:0.6.0")
}
```

Full changelog: https://github.com/jsaabel/kotlin-notion-client/blob/main/CHANGELOG.md
