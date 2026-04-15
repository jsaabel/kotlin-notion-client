# Notebook Fixes and Date API Note

**Date**: 2026-04-15

## Summary

Worked through notebooks 01 and 02 to fix issues discovered while testing against the locally-published 0.4.0 jar (`./gradlew publishToMavenLocal -PskipSigning` + `@file:Repository("*mavenLocal")`).

## Fixes Applied

### `01-getting-started.ipynb` — Example 6
- `PageIcon` was renamed to `Icon` (moved to `models.base`) in v0.4.0; updated all references.
- Added missing `is Icon.NativeIcon` branch to the `when` expression.

### `02-reading-databases.ipynb`
- Replaced `select("Status")` with `status("Status")` throughout (database schema, page creation, query filters) to showcase the v0.4.0 Status property type.
- Added `date("Due Date", LocalDate)` to all created test pages so the relative date filter example (Example 8) actually returns results.
- Spread due dates so both filter boundaries exclude tasks: 2 past/far-future tasks are excluded, 3 this-week tasks are included.
- Fixed deprecated `kotlinx.datetime` operator usage: `today + DateTimeUnit.DAY * N` → `today.plus(N, DateTimeUnit.DAY)` (and `.minus`).
- Fixed `notion.databases.archive()` → `notion.databases.trash()`.

## Follow-up: Review Date Examples Elsewhere

While fixing the `DateTimeUnit` deprecation in the notebooks, we noticed the correct form for `kotlinx.datetime` `LocalDate` arithmetic is the explicit method call:

```kotlin
today.plus(N, DateTimeUnit.DAY)   // ✓ current API
today.minus(N, DateTimeUnit.DAY)  // ✓ current API
today + DateTimeUnit.DAY * N      // ✗ deprecated
```

**Action needed**: Do a pass over any other date/datetime usage examples — in the remaining notebooks, in the docs (`docs/data-sources.md`, `docs/pages.md`), and in integration test helper code — to confirm they use the current API form or plain ISO string literals (which are unaffected).
