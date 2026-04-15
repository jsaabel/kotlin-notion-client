# Development Journal - April 14, 2026 (Session 2)

## Timezone Round-Trip Testing and `dateMention` Builder Fix

### Motivation

IDEAS.md item #8 flagged that timezone functionality might need a proper test and adjustments, based on an
example in `05-rich-text-dsl.ipynb`. The goal was to verify how dates with timezone information round-trip
through the Notion API — specifically whether named timezones are preserved and whether datetime values
come back in the expected format.

---

### What Was Built

A new `TimezoneIntegrationTest` covering all paths by which date/datetime values with timezone
information reach the Notion API:

**Part 1 — Database date properties:**
- A: `LocalDate` — plain date string, no timezone
- B: `LocalDateTime + UTC` — converted to UTC instant via builder
- C: `LocalDateTime + named TZ` via `dateTime()` builder — converted to UTC instant (named TZ lost)
- D: Explicit `time_zone` field via `dateTimeWithTimeZone()` — named TZ sent explicitly
- E: Date range (`LocalDate..LocalDate`)

The database has a "Sent Value" rich text column alongside the date column so each row shows
what was sent and the resulting date chip side by side in the Notion table view.

**Part 2 — Rich text date mentions:**
Same scenarios applied to `dateMention()` in paragraph blocks. The comparison results are
appended directly to the mention sub-page so chips and sent/actual values are visible together.

---

### Findings from Live API

1. **Notion never preserves named timezones in responses.** The `time_zone` field is always `null`
   in API responses regardless of what was sent. Notion converts the naive local time to a timestamp
   with a numeric UTC offset (e.g. `-04:00`) and drops the timezone name.

2. **Datetime strings come back with milliseconds and a numeric offset**, e.g.:
   - `"2026-06-15T14:30:00"` + `time_zone="America/New_York"` → `"2026-06-15T14:30:00.000-04:00"`
   - `"2026-06-15T14:30:00Z"` (UTC) → `"2026-06-15T14:30:00.000+00:00"`

3. **Bug found in `dateMention(LocalDateTime, TimeZone)` builder.**
   The builder was converting `LocalDateTime + TimeZone` to a UTC `Instant` before sending.
   This caused Notion to misinterpret the time: Notion receives the UTC timestamp
   (e.g. `"2026-06-15T18:30:00Z"`) plus a `time_zone` field, strips the `Z`, and applies the
   timezone offset — returning `"2026-06-15T18:30:00.000-04:00"` instead of the correct
   `"2026-06-15T14:30:00.000-04:00"`.

   The correct approach: pass the **local datetime string** + `timezone.id` directly to the API,
   without converting to an instant first. Notion then correctly interprets the local time and
   applies the UTC offset.

4. **`dateTime(LocalDateTime, namedTZ)` for database properties** (scenario C) still converts to a
   UTC instant and loses the named timezone. This is by design for the property builder — the stored
   absolute moment in time is correct, but Notion will display it in UTC rather than the user's
   local timezone. Callers who want Notion to display in a specific timezone should use
   `dateTimeWithTimeZone()` instead.

---

### Changes Made

**`RichTextBuilder.kt`** — fixed `dateMention(LocalDateTime, TimeZone)`:
```kotlin
// Before (incorrect):
val startInstant = start.toInstant(timeZone)
return dateMention(start = startInstant.toString(), ..., timeZone = timeZone.id)

// After (correct):
return dateMention(start = start.toString(), ..., timeZone = timeZone.id)
```

**`RichTextBuilderTest.kt`** — updated unit test to expect the local datetime string format
(`"2025-10-15T14:30"`) instead of the old instant format (`"2025-10-15T14:30:00Z"`).

**`DatabasesApi.kt`** — fixed a line-length violation in the icon-patch error message string.

**`IDEAS.md`** — item #8 marked as done with a summary of findings.

---

### Test Results (Live API)

```
=== Database date property round-trip ===
A  start=2026-06-15  tz=null
B  start=2026-06-15T14:30:00.000+00:00  tz=null
C  start=2026-06-15T18:30:00.000+00:00  tz=null
D  start=2026-06-15T14:30:00.000-04:00  tz=null
E  start=2026-06-15  end=2026-06-20  tz=null
✅ Database date property round-trip verified

=== Rich text date mention round-trip ===
M1  start=2026-06-15  tz=null
M2  start=2026-06-15T14:30:00.000-04:00  tz=null
M3  start=2026-06-15  tz=null
M4  start=2026-06-15T14:30:00.000-04:00  tz=null
M5  start=2026-06-15T18:30:00.000+00:00  tz=null
✅ Rich text date mention round-trip verified
```

All assertions pass. M4 confirms the builder fix: `14:30-04:00` (correct) vs the previous
incorrect `18:30-04:00`.