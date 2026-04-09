# Development Journal - April 9, 2026

## Phase 5 (Part 1): Relative Date Filter Values

### Objective

Add support for the seven relative date values newly accepted by Notion's date filter conditions.

---

## API Change

Date filter conditions that accept an ISO 8601 date string (`equals`, `before`, `after`,
`on_or_before`, `on_or_after`) now also accept the following string values, resolved at query
time relative to the current date:

`"today"`, `"tomorrow"`, `"yesterday"`, `"one_week_ago"`, `"one_week_from_now"`,
`"one_month_ago"`, `"one_month_from_now"`

---

## Approach

The `DateCondition` model fields stay `String?` — both ISO dates and relative keywords are plain
strings at the JSON level, so no model changes were needed. The feature is purely additive:

1. A typed `enum class RelativeDateValue` exposes the seven values with their `apiValue` strings.
2. New builder overloads accept `RelativeDateValue` and delegate to the existing `String` overloads
   via `.apiValue`.

---

## Changes Made

### `DataSourceQuery.kt`

- Added `enum class RelativeDateValue` with `apiValue: String` property.
  Seven entries: `TODAY`, `TOMORROW`, `YESTERDAY`, `ONE_WEEK_AGO`, `ONE_WEEK_FROM_NOW`,
  `ONE_MONTH_AGO`, `ONE_MONTH_FROM_NOW`.

### `DataSourceQueryBuilder.kt`

- Added `import it.saabel.kotlinnotionclient.models.datasources.RelativeDateValue`.
- `DateFilterBuilder`: new overloads for `equals`, `before`, `after`, `onOrBefore`, `onOrAfter`
  accepting `RelativeDateValue` — each delegates to the `String` overload via `.apiValue`.
- `TimestampFilterBuilder`: same five overloads added.

### Unit tests — `RelativeDateFilterTest.kt` (new)

17 tests in three `context` blocks:

- `RelativeDateValue.apiValue` — 7 tests, one per enum entry, asserting the exact API string.
- `DateFilterBuilder relative overloads serialization` — 5 tests, one per operator, checking that
  the serialized JSON contains the correct key/value pair (e.g. `"on_or_before":"one_week_from_now"`).
- `TimestampFilterBuilder relative overloads serialization` — 3 tests covering `after`, `onOrAfter`,
  and `before` on timestamp builders.

### Integration test — `DataSourcesIntegrationTest.kt`

Extended the existing `"Data source query with filters"` test:

- Added `date("Due Date")` property to the test database.
- Creates 7 dated pages, one for each relative value (computed with `java.time.LocalDate.now()`
  arithmetic at test runtime).
- Assertions:
  - `equals(X)` for all 7 relative values → 1 result each (covers every enum entry)
  - `after(YESTERDAY)` → 4 results (today, tomorrow, +1 week, +1 month)
  - `before(TODAY)` → 3 results (yesterday, −1 week, −1 month)
  - `onOrAfter(TODAY)` → 4 results (today, tomorrow, +1 week, +1 month)
  - `onOrBefore(TODAY)` → 4 results (today, yesterday, −1 week, −1 month)
  - `onOrAfter(ONE_WEEK_FROM_NOW)` → 2 results (+1 week, +1 month)

#### Note on `java.time.LocalDate`

`kotlinx.datetime.Clock` triggers an `Unresolved reference 'System'` error with Kotlin 2.3 /
kotlinx-datetime 0.7.1, likely due to `kotlin.time.Clock` being added to the stdlib in Kotlin 2.1
and shadowing the datetime library's type. Used `java.time.LocalDate.now()` directly instead —
no extra imports, always available on JVM.

---

## Status

- [x] `RelativeDateValue` enum — all 7 values
- [x] `DateFilterBuilder` overloads — all 5 operators
- [x] `TimestampFilterBuilder` overloads — all 5 operators
- [x] Unit tests — 17 tests, all passing
- [x] Integration test — pending live API validation