# Typed Date/DateTime API: Timezone-Aware Accessors

**Date:** October 6, 2025
**Status:** Completed ✅
**Type:** Feature Enhancement (Breaking Change)

## Summary

Enhanced the typed date/datetime property API to properly handle timezone-aware datetime values. The previous `localDateTimeValue` accessor was ambiguous and lost timezone information without clearly indicating this behavior. The new API provides explicit accessors for different use cases.

## Problem

When retrieving datetime properties from the Notion API, the response includes timezone information:
```json
{
  "Meeting Time": {
    "id": "%5DT%7Bp",
    "type": "date",
    "date": {
      "start": "2025-03-20T14:30:00.000+01:00",
      "end": null,
      "time_zone": null
    }
  }
}
```

The previous `localDateTimeValue` accessor would:
1. Strip timezone information without documenting this behavior
2. Return the same `LocalDateTime` value regardless of the actual timezone
3. Fail to parse timestamps with timezone indicators (Z, +HH:MM)

This was semantically incorrect and could lead to bugs when working with timestamps across timezones.

## Solution

Implemented a multi-accessor API pattern providing three ways to access datetime values:

### 1. `instantValue` (unchanged)
Returns the absolute point in time as `Instant`:
```kotlin
val instant = property.instantValue  // Instant representing exact moment
```

### 2. `localDateTimeNaive` (renamed from `localDateTimeValue`)
Extracts date/time components as shown in the ISO string, **ignoring timezone**:
```kotlin
val naive = property.localDateTimeNaive
// "2025-03-20T14:30:00Z" → LocalDateTime(2025, 3, 20, 14, 30)
// "2025-03-20T14:30:00+01:00" → LocalDateTime(2025, 3, 20, 14, 30)
```

⚠️ **Warning:** This loses timezone context. Use only when you don't care about the actual moment in time.

### 3. `toLocalDateTime(timeZone)` (new)
Properly converts the instant to LocalDateTime in any specified timezone:
```kotlin
val utcTime = property.toLocalDateTime(TimeZone.UTC)
// "2025-03-20T14:30:00+01:00" → LocalDateTime(2025, 3, 20, 13, 30)

val nyTime = property.toLocalDateTime(TimeZone.of("America/New_York"))
// "2025-03-20T14:30:00+01:00" → LocalDateTime(2025, 3, 20, 8, 30)
```

### Range Support

Added matching accessors for date range end values:
- `endLocalDateTimeNaive` - Extract end datetime components (naive)
- `endToLocalDateTime(timeZone)` - Convert end to specific timezone

## Changes Made

### API Changes (Breaking)
- **Renamed:** `localDateTimeValue` → `localDateTimeNaive`
- **Renamed:** `endLocalDateTimeValue` → `endLocalDateTimeNaive`
- **Added:** `toLocalDateTime(timeZone: TimeZone): LocalDateTime?`
- **Added:** `endToLocalDateTime(timeZone: TimeZone): LocalDateTime?`
- **Enhanced:** Fixed timezone stripping in naive accessors to handle Z and +HH:MM offsets

### Files Modified
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/pages/PageProperty.kt`
  - Renamed accessors with improved KDoc warnings
  - Added timezone conversion functions
  - Added import for `kotlinx.datetime.toLocalDateTime`
- `src/test/kotlin/integration/TypedDatePropertiesIntegrationTest.kt`
  - Updated to use `toLocalDateTime(TimeZone.UTC)`
  - Updated imports
- `src/test/kotlin/unit/properties/PagePropertyDateAccessorsTest.kt`
  - Updated all test cases to use new accessor names
  - Updated imports

## Testing

All tests pass:
- **389 tests total** (100% success rate)
- **14 unit tests** for PagePropertyDateAccessorsTest specifically
- **Integration test** validates real API behavior (skipped in CI, passes with env vars)

## Migration Guide

### Before
```kotlin
val dateTime = property.localDateTimeValue
// Unclear: does this handle timezone? What gets returned?
```

### After
```kotlin
// If you need the exact moment in time, converted to a specific timezone:
val utcTime = property.toLocalDateTime(TimeZone.UTC)

// If you only care about the date/time components shown (rare):
val naive = property.localDateTimeNaive

// If you need the absolute instant:
val instant = property.instantValue
```

## Future Work

### Documentation Needs
The date/datetime API is complex enough to warrant dedicated documentation:

1. **Property-Specific Guides**
   - When to use each accessor
   - Common pitfalls and warnings
   - Real-world examples

2. **Documentation Architecture**
   - Should complex properties get their own doc files?
   - How to organize: by property type vs use case vs API surface?
   - Where do advanced examples live vs basic API docs?

3. **Consider for Other Complex Properties**
   - Relations (pagination, has_more handling)
   - Formulas (type discrimination)
   - Rollups (aggregation semantics)

### Potential Enhancements
- Add `TimeZone` parameter to `NotionConfig` for default conversion?
- Helper functions for common timezone operations?
- Validation warnings when using naive accessors on timestamped data?

## Lessons Learned

1. **Naming Matters:** Adding "Naive" to the accessor name makes the timezone loss explicit
2. **Multiple Accessors > One Size Fits All:** Different use cases need different semantics
3. **Integration Tests Catch Real Issues:** The test failure revealed actual API behavior
4. **KDoc Warnings Are Critical:** Developers need to understand the implications of using naive accessors

## References

- kotlinx-datetime documentation: https://github.com/Kotlin/kotlinx-datetime
- Notion API date property format: ISO 8601 with optional timezone
- Related journal entries:
  - `2025_10_05_04_Date_DateTime_API_Improvement.md` (initial typed date API implementation)
