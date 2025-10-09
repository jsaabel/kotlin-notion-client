# Rich Text DSL Documentation and Typed Date Support

**Date**: 2025-10-09
**Status**: ✅ Complete

## Overview

Completed comprehensive documentation for the Rich Text DSL and added typed date support to `dateMention()`. This work included cleaning up documentation references, enhancing the API with type-safe date handling, creating integration test examples, and updating documentation to reflect all changes.

## Changes Made

### 1. Documentation Cleanup

**Files Modified:**
- `README.md`
- `docs/README.md`

**Changes:**
- Removed references to `ARCHITECTURE.md` and `CONTRIBUTING.md` (decided not to include these at this stage)
- Removed reference to `LICENSE` file
- Added `file-uploads.md` to the docs index under Features section

### 2. Enhanced Rich Text DSL with Typed Date Support

**Problem:**
The `dateMention()` function only accepted string parameters, while page properties had overloads for `LocalDate`, `LocalDateTime`, and `Instant`. This inconsistency forced users to manually convert dates to strings.

**Solution:**
Added three new overloads to `RichTextBuilder.kt`:

```kotlin
// LocalDate support
fun dateMention(start: LocalDate, end: LocalDate? = null): RichTextBuilder

// LocalDateTime with timezone support
fun dateMention(start: LocalDateTime, end: LocalDateTime? = null, timeZone: TimeZone = TimeZone.UTC): RichTextBuilder

// Instant support (timezone-unambiguous)
fun dateMention(start: Instant, end: Instant? = null): RichTextBuilder
```

**Files Modified:**
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/richtext/RichTextBuilder.kt`
  - Added imports for `kotlinx.datetime.*`
  - Added 3 new overloaded methods
  - All overloads delegate to the existing string-based method after converting dates to ISO 8601 format

**Files Modified (Tests):**
- `src/test/kotlin/unit/dsl/RichTextBuilderTest.kt`
  - Added 8 new unit tests covering all date mention variations
  - Tests verify proper ISO 8601 formatting
  - Note: `TimeZone.UTC.id` returns "Z" not "UTC" - tests account for this

**Test Results:**
- All 32 tests in RichTextBuilderTest pass
- Total project: 481 unit tests passing

### 3. Complete Rich Text DSL Documentation

**File Modified:** `docs/rich-text-dsl.md`

**Sections Completed:**

1. **Text Formatting** - Distinguished between standalone `richText {}` DSL (with lambda syntax) and block DSL (with convenience methods)
2. **Colors** - Text colors and background colors with all available enum values
3. **Links** - Both display text and URL-only variations
4. **Mentions**:
   - User mentions
   - Page mentions
   - Database mentions
   - Date mentions (with all 3 type variants: strings, LocalDate, LocalDateTime, Instant)
5. **Equations** - LaTeX syntax support with examples
6. **Advanced Formatting** - Using `formattedText()` with multiple style options
7. **Examples**:
   - Formatted text in page properties
   - Complex rich text in blocks
   - Mixed content combining all features
8. **Common Patterns**:
   - Method chaining
   - Convenience methods overview
   - Best practices
9. **Common Gotchas**:
   - Empty rich text blocks
   - Date format requirements (with typed vs string examples)
   - Background color suffix usage
   - Link parameter order

**Key Documentation Insight:**
Added clarification distinguishing two contexts for rich text:
- **Standalone `richText {}`**: Used in page properties/comments, supports lambda syntax like `text("foo") { bold = true }`
- **Block DSL**: Used in paragraphs/headings/etc., uses convenience methods like `bold("foo")`

### 4. Integration Test Examples

**File Created:** `src/test/kotlin/examples/RichTextExamples.kt`

**Structure:**
- Follows established pattern from `BlocksExamples.kt`, `PagesExamples.kt`, etc.
- Tagged with `@Tags("Integration", "RequiresApi", "Examples")`
- Uses `integrationTestEnvVarsAreSet()` to skip when env vars not present
- 13 example tests covering all documentation features:
  1. Text formatting (bold, italic, code)
  2. Text and background colors
  3. Links
  4. Date mentions using strings
  5. Date mentions using LocalDate
  6. Date mentions using LocalDateTime
  7. Date mentions using Instant
  8. Equations
  9. Advanced formatting with formattedText
  10. Complex rich text in blocks (including callout)
  11. Mixed content with all features
  12. Method chaining
  13. Convenience methods

**Test Infrastructure:**
- Creates test page in `beforeSpec`
- Cleans up in `afterSpec` if `NOTION_CLEANUP_AFTER_TEST != "false"`
- Each test appends blocks and verifies children were created
- All tests compile and load successfully

**Environment Variables Required:**
```bash
export NOTION_RUN_INTEGRATION_TESTS=true
export NOTION_API_TOKEN="secret_..."
export NOTION_TEST_PAGE_ID="page-id"
```

**Run Command:**
```bash
./gradlew test --tests "*RichTextExamples"
```

## Technical Details

### Date Mention Implementation

The implementation converts typed dates to ISO 8601 strings before passing to the base `dateMention(String, String?, String?)` method:

**LocalDate → String:**
```kotlin
LocalDate(2025, 10, 15).toString() // "2025-10-15"
```

**LocalDateTime → Instant → String:**
```kotlin
LocalDateTime(2025, 10, 15, 14, 30)
    .toInstant(TimeZone.of("America/New_York"))
    .toString() // "2025-10-15T18:30:00Z"
```

**Instant → String:**
```kotlin
Instant.parse("2025-10-15T14:30:00Z")
    .toString() // "2025-10-15T14:30:00Z"
```

### Timezone Handling

- `TimeZone.UTC.id` returns `"Z"` (not `"UTC"`)
- For LocalDateTime, timezone is passed via `timeZone.id` parameter
- For Instant, no timezone parameter needed (already unambiguous)

### Block DSL vs Standalone Rich Text DSL

**Block DSL** (in paragraphs, headings, callouts, etc.):
```kotlin
paragraph {
    text("Hello")
    bold("world")
}
```

**Standalone Rich Text DSL** (in page properties, comments):
```kotlin
richText {
    text("Hello") { bold = true }
}
```

The block DSL extends `RichTextBuilder` and provides the same methods, but the context differs.

## Build Verification

```bash
./gradlew formatKotlin  # ✅ No issues
./gradlew build         # ✅ Success
./gradlew test -Dkotest.tags.include="Unit"  # ✅ 481 tests passing
```

## Files Modified

### Source Code
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/richtext/RichTextBuilder.kt` (+75 lines)

### Tests
- `src/test/kotlin/unit/dsl/RichTextBuilderTest.kt` (+155 lines)
- `src/test/kotlin/examples/RichTextExamples.kt` (new file, 502 lines)

### Documentation
- `README.md` (removed 3 references)
- `docs/README.md` (added file-uploads.md, removed ARCHITECTURE.md)
- `docs/rich-text-dsl.md` (completed all TODO sections, ~400 lines)

## Next Steps

The rich text DSL is now fully documented and feature-complete. Remaining documentation TODOs exist in:
- `docs/search.md` - Missing examples, pagination, best practices
- `docs/testing.md` - Missing mocking examples, test data management

These could be addressed in future sessions if needed.

## Notes

- The formatter automatically changed one instance in RichTextBuilder.kt to use expression body syntax
- Integration tests were not run against live API (requires env vars), but all compile and load correctly
- Documentation now clearly distinguishes between the two rich text contexts to avoid confusion
