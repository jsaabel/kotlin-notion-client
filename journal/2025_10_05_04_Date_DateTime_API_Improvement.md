# Journal Entry: 2025-10-05 - Date/DateTime API Improvement

## Context
While validating documentation examples for the Data Sources API, we identified a significant usability issue: all date/datetime handling uses string representations. This is not type-safe, not idiomatic for Kotlin, and makes date manipulation difficult.

## Current Problem

### String-Based API
```kotlin
// Creating pages with dates
properties {
    date("Due Date", "2025-03-15")
    dateTime("Meeting Time", "2025-03-15T14:30:00")
    dateRange("Project Duration", "2025-03-15", "2025-03-22")
}

// Reading date properties
val dateProp = page.properties["Due Date"] as PageProperty.Date
val dateString = dateProp.date?.start // String: "2025-03-15"
```

### Issues
1. **Not type-safe** - Can pass invalid date strings at compile time
2. **Not idiomatic** - Kotlin developers expect `LocalDate`, `LocalDateTime`, `Instant`
3. **Timezone ambiguity** - What timezone is "2025-03-15T14:30:00"?
4. **Difficult to work with** - Can't do date arithmetic, comparisons without parsing
5. **Error-prone** - Easy to use wrong format (ISO-8601 required)
6. **Validation complexity** - Current validation checks for time component when timezone is present

## Proposed Solution

### Library: kotlinx-datetime
Use **kotlinx-datetime** because:
- ✅ Official JetBrains library
- ✅ Multiplatform support (future-proof)
- ✅ Modern, idiomatic Kotlin API
- ✅ Explicit timezone handling via `TimeZone`
- ✅ Clear distinction: `LocalDate`, `LocalDateTime`, `Instant`
- ✅ Small dependency footprint
- ✅ Well-maintained and documented

### Implementation Strategy

**Dual API Support**: Keep string-based API for backward compatibility, add typed overloads.

#### 1. Property DSL (Create/Update Pages)

```kotlin
// String-based (existing - keep for compatibility)
fun date(name: String, value: String)
fun dateTime(name: String, value: String)
fun dateRange(name: String, start: String, end: String?)

// New typed overloads - simple values
fun date(name: String, value: LocalDate)
fun dateTime(name: String, value: LocalDateTime, timeZone: TimeZone = TimeZone.UTC)
fun dateTime(name: String, value: Instant) // Unambiguous - always UTC

// New DSL helpers for ranges
fun dateRange(name: String, block: DateRangeBuilder.() -> Unit)
fun dateTimeRange(name: String, block: DateTimeRangeBuilder.() -> Unit)
fun dateTimeRange(name: String, timeZone: TimeZone = TimeZone.UTC, block: DateTimeRangeBuilder.() -> Unit)

// Usage examples:
dateRange("Project Duration") {
    start = LocalDate(2025, 3, 15)
    end = LocalDate(2025, 3, 22)
}

dateTimeRange("Conference", timeZone = TimeZone.of("America/New_York")) {
    start = LocalDateTime(2025, 3, 15, 9, 0)
    end = LocalDateTime(2025, 3, 15, 17, 0)
}

dateTimeRange("Deployment Window") {
    start = Instant.parse("2025-03-15T00:00:00Z")
    end = Instant.parse("2025-03-15T04:00:00Z")
}
```

#### 2. Date Range DSL Builders

```kotlin
@DslMarker
annotation class DateRangeDslMarker

@DateRangeDslMarker
class DateRangeBuilder {
    var start: LocalDate? = null
    var end: LocalDate? = null

    fun build(): Pair<LocalDate?, LocalDate?> = start to end
}

@DateRangeDslMarker
class DateTimeRangeBuilder {
    var start: LocalDateTime? = null
    var end: LocalDateTime? = null

    // Alternative: using Instant
    var startInstant: Instant? = null
    var endInstant: Instant? = null

    fun build(timeZone: TimeZone): Pair<String?, String?> {
        // Convert to ISO-8601 strings
    }
}
```

#### 3. Reading Date Properties

Add convenience accessors to `PageProperty.Date`:

```kotlin
// Extension properties on PageProperty.Date
val PageProperty.Date.localDateValue: LocalDate?
    get() = date?.start?.let { LocalDate.parse(it) }

val PageProperty.Date.localDateTimeValue: LocalDateTime?
    get() = date?.start?.let { LocalDateTime.parse(it) }

val PageProperty.Date.instantValue: Instant?
    get() = date?.start?.let { Instant.parse(it) }

val PageProperty.Date.endLocalDateValue: LocalDate?
    get() = date?.end?.let { LocalDate.parse(it) }

val PageProperty.Date.endLocalDateTimeValue: LocalDateTime?
    get() = date?.end?.let { LocalDateTime.parse(it) }

val PageProperty.Date.endInstantValue: Instant?
    get() = date?.end?.let { Instant.parse(it) }

// Keep existing string accessors
val PageProperty.Date.stringValue: String?
    get() = date?.start
```

#### 4. Filter DSL (Query Dates)

```kotlin
// String-based (existing)
date("Due Date").before("2025-10-05")
date("Due Date").after("2025-10-05")
date("Due Date").onOrAfter("2025-10-05")

// New typed overloads
date("Due Date").before(LocalDate(2025, 10, 5))
date("Due Date").after(LocalDate(2025, 10, 5))
date("Due Date").onOrAfter(Instant.parse("2025-10-05T00:00:00Z"))
```

#### 5. Internal Serialization

All typed values convert to ISO-8601 strings before sending to Notion API:
- `LocalDate` → "YYYY-MM-DD"
- `LocalDateTime` + `TimeZone` → "YYYY-MM-DDTHH:MM:SS.sss±HH:MM"
- `Instant` → "YYYY-MM-DDTHH:MM:SS.sssZ"

## Implementation Plan

### Phase 1: Setup & Core Types
1. ✅ Create this journal entry
2. Add kotlinx-datetime dependency to `gradle/libs.versions.toml`
3. Add dependency to `build.gradle.kts`

### Phase 2: Date Range DSL Builders
4. Create `DateRangeBuilder.kt` with builders for date and datetime ranges
5. Add DSL marker annotations
6. Implement conversion to ISO-8601 strings

### Phase 3: Property DSL Overloads
7. Update `PropertyValueBuilder.kt` with typed overloads
8. Add internal conversion functions (typed → string)
9. Integrate DateRangeBuilder into property DSL
10. Update validation to work with both typed and string values

### Phase 4: Reading Properties
11. Add extension properties to `PageProperty.Date`
12. Handle parsing errors gracefully (return null on invalid format)

### Phase 5: Filter DSL
13. Update date filter builders to accept typed values
14. Add conversion to ISO-8601 for API requests

### Phase 6: Testing & Documentation
15. Update `DataSourcesExamples.kt` to use typed API (including range DSL)
16. Run integration tests to verify
17. Update `docs/data-sources.md` to show both string and typed examples
18. Add note about timezone handling
19. Add examples of date range DSL

### Phase 7: Validation
20. Run all existing tests to ensure backward compatibility
21. Add new tests for typed API and range DSL
22. Format code and build

## Files to Create/Modify

### Build Configuration
- `gradle/libs.versions.toml` - Add kotlinx-datetime version
- `build.gradle.kts` - Add dependency

### New Files
- `src/main/kotlin/.../models/pages/DateRangeBuilder.kt` - DSL builders for date ranges

### Core API
- `src/main/kotlin/.../models/pages/PropertyValueBuilder.kt` - Add typed overloads
- `src/main/kotlin/.../models/pages/PageProperty.kt` - Add extension properties
- `src/main/kotlin/.../models/databases/DatabaseQueryBuilder.kt` - Update filter DSL

### Tests & Documentation
- `src/test/kotlin/examples/DataSourcesExamples.kt` - Update to use typed API
- `docs/data-sources.md` - Add typed examples

## Success Criteria

- [ ] kotlinx-datetime dependency added and building
- [ ] Date range DSL builders implemented and tested
- [ ] Typed overloads available for date properties in DSL
- [ ] Extension properties available for reading dates
- [ ] Filter DSL accepts typed date values
- [ ] All existing tests pass (backward compatibility)
- [ ] `DataSourcesExamples.kt` uses typed API and passes
- [ ] Documentation updated with both string and typed examples
- [ ] Range DSL examples in documentation
- [ ] Code formatted and builds without errors

## Benefits

1. **Type Safety** - Compile-time checking of date values
2. **Better IDE Support** - Auto-completion for date operations
3. **Easier Date Manipulation** - Use kotlinx-datetime's rich API
4. **Clear Timezone Handling** - Explicit `TimeZone` parameters
5. **Idiomatic Kotlin** - Feels natural to Kotlin developers
6. **Backward Compatible** - String API still works
7. **Better Validation** - Parsing happens in Kotlin, not at runtime API calls
8. **Elegant Range Syntax** - DSL builders make ranges clean and readable

## Example: Before vs After

### Before (String-based)
```kotlin
properties {
    date("Due Date", "2025-03-15")
    dateRange("Project Duration", "2025-03-15", "2025-03-22")
}

val dateProp = page.properties["Due Date"] as PageProperty.Date
val dateString = dateProp.date?.start // "2025-03-15" - need to parse manually
```

### After (Typed with DSL)
```kotlin
properties {
    date("Due Date", LocalDate(2025, 3, 15))

    dateRange("Project Duration") {
        start = LocalDate(2025, 3, 15)
        end = LocalDate(2025, 3, 22)
    }

    dateTimeRange("Meeting", timeZone = TimeZone.of("America/New_York")) {
        start = LocalDateTime(2025, 3, 15, 14, 0)
        end = LocalDateTime(2025, 3, 15, 15, 30)
    }
}

val dateProp = page.properties["Due Date"] as PageProperty.Date
val date = dateProp.localDateValue // LocalDate - ready to use
val isBefore = date?.let { it < LocalDate(2025, 4, 1) } // Easy comparison
```

## Notes

- This is a **non-breaking improvement** - maintains full backward compatibility
- Decision made during documentation validation - better to fix now than after release
- DSL helpers for ranges make the API feel more cohesive and Kotlin-idiomatic
- Aligns with Kotlin's philosophy of type safety and explicit APIs
- Sets good foundation for future date/time work (reminders, recurrence, etc.)
