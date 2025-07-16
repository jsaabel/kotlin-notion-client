# Table Block Implementation & Missing Features Analysis

**Date:** July 16, 2025  
**Focus:** Implementing table blocks and addressing other implementation gaps

## Context

Comprehensive analysis of the current codebase revealed several implementation gaps. While people database properties
are actually fully implemented (contrary to initial concern), table blocks represent the most significant missing
feature.

## Current Implementation Status

### ✅ Database Properties - Excellent Coverage

**18 out of 19** official Notion database property types fully implemented:

**Fully Implemented:**

- Title, Rich Text, Number, Checkbox, URL, Email, Phone Number
- Select, Multi-select, Date, Relation, Formula, Rollup, Files
- Created Time, Last Edited Time, Created By, Last Edited By
- **People** - Fully implemented across all layers (read/write/create)

**Minor Gaps:**

- Status property: Read/write support exists, but missing database schema classes
- People property: Missing `people()` method in `DatabaseRequestBuilder` DSL

### ❌ Block Types - Significant Gaps

**17 out of ~30** block types implemented. **Table blocks are the biggest gap.**

**Currently Implemented:**

- Text blocks: Heading1/2/3, Paragraph, Quote
- Lists: BulletedListItem, NumberedListItem, ToDo
- Special: Callout, Code, Toggle, Divider
- Media: Image, Video, Audio, File, PDF

**Missing Block Types (Priority Ranked):**

**🔴 High Priority:**

- **Table** + **TableRow** - Most critical gap, commonly used
- **Bookmark** - External website links
- **Embed** - External content embedding
- **Column** + **ColumnList** - Layout blocks
- **Equation** - Mathematical expressions
- **Unsupported** - Graceful handling of unknown blocks

**🟡 Medium Priority:**

- **ChildDatabase** + **ChildPage** - Nested content references
- **Breadcrumb** - Navigation elements
- **LinkPreview** - URL previews (read-only API response only)
- **TableOfContents** - Page navigation
- **SyncedBlock** - Content synchronization

**🟢 Low Priority:**

- **Template** - Deprecated (as of March 2023) but may exist in legacy content

## Table Block Implementation Plan

### Requirements from API Documentation

**Table Block Properties:**

```json
{
  "type": "table",
  "table": {
    "table_width": 2,
    // integer: number of columns (immutable after creation)
    "has_column_header": false,
    // boolean: first row appears as header
    "has_row_header": false
    // boolean: first column appears as header  
  }
}
```

**TableRow Block Properties:**

```json
{
  "type": "table_row",
  "table_row": {
    "cells": [
      // array of array of rich text objects
      [
        {
          "type": "text",
          "text": {
            "content": "cell 1"
          }
        }
      ],
      [
        {
          "type": "text",
          "text": {
            "content": "cell 2"
          }
        }
      ]
    ]
  }
}
```

### Implementation Phases

**Phase 1: Response Models**

1. Add `Table` class to `Block.kt` sealed class
2. Add `TableRow` class to `Block.kt` sealed class
3. Follow existing patterns for rich text support

**Phase 2: Request Models**

1. Add `TableRequest` class to `BlockRequest.kt`
2. Add `TableRowRequest` class to `BlockRequest.kt`
3. Implement validation for table_width constraints

**Phase 3: DSL Builder Support**

1. Add `table()` method to `PageContentBuilder.kt`
2. Add `tableRow()` method with cells parameter
3. Support for header configurations

**Phase 4: Testing & Validation**

1. Add official API samples to test resources
2. Create comprehensive test coverage
3. Test table creation, row manipulation, edge cases

### Key API Constraints

- `table_width` can only be set when table is first created (immutable)
- Table must have at least one `table_row` when created via API
- Each `table_row` must have same number of cells as `table_width`
- Tables support child blocks (table_rows are children of table blocks)

## Future Implementation Priorities

After table blocks, recommended implementation order:

1. **Bookmark** blocks - Simple external URL references
2. **Embed** blocks - External content embedding
3. **Column/ColumnList** - Layout capabilities
4. **Equation** blocks - Mathematical expression support
5. **Unsupported** blocks - Graceful degradation for unknown types

## Status Property Enhancement

While not critical, the Status property could benefit from:

- `DatabaseProperty.Status` class for database schema support
- Though API doesn't support creating/modifying status properties anyway

## Integration Test Implementation

### ✅ Real API Integration Test (COMPLETED)

Created comprehensive integration test `TableBlockIntegrationTest.kt` that validates:

**Test Features:**

- **Single Page Creation** - Uses `pageRequest` DSL for clean page creation
- **5 Different Table Configurations** - Comprehensive coverage of all table scenarios:
    - Simple table (no headers)
    - Column headers only
    - Row headers only
    - Both headers
    - Data table example (5 columns)
- **DSL-First Approach** - Leverages `pageContent` DSL for consistent test patterns
- **Comprehensive Validation** - Verifies both DSL structure and Notion API responses
- **Proper Test Lifecycle** - Conditional cleanup based on environment variable

**Test Coverage:**

- Table creation with various header configurations
- Row content validation
- Block structure verification
- Real API integration with proper error handling
- Follows established integration test patterns

**Usage:**

```bash
export NOTION_API_TOKEN="secret_..."
export NOTION_TEST_PAGE_ID="12345678-1234-1234-1234-123456789abc"
./gradlew integrationTest --tests "*TableBlockIntegrationTest*"
```

## Implementation Status

### ✅ Phase 1: Response Models (COMPLETED)

- Added `Table` class to `Block.kt` with `TableContent` properties
- Added `TableRow` class to `Block.kt` with `TableRowContent` properties
- Implemented proper serialization with `@SerialName` annotations
- Added comprehensive validation for both block types

### ✅ Phase 2: Request Models (COMPLETED)

- Added `TableRequest` class to `BlockRequest.kt` with `TableRequestContent`
- Added `TableRowRequest` class to `BlockRequest.kt` with `TableRowRequestContent`
- Implemented proper validation for table_width constraints
- Added support for children blocks in table request model

### ✅ Phase 3: DSL Builder Support (COMPLETED)

- Added `table()` method to `PageContentBuilder.kt` with nested row support
- Added `tableRow()` methods for both rich text and simple string content
- Created `TableRowBuilder` class for clean DSL table construction
- Added comprehensive validation for table structure

### ✅ Phase 4: Validation Integration (COMPLETED)

- Updated `RequestValidator.kt` to handle table blocks
- Added validation for table width constraints
- Added validation for table row cell content
- Integrated table validation into the main validation pipeline

## Example Usage

The implementation now supports intuitive table creation:

```kotlin
val content = pageContent {
    table(
        tableWidth = 3,
        hasColumnHeader = true,
        hasRowHeader = false
    ) {
        row("Name", "Age", "City")
        row("John", "25", "New York")
        row("Jane", "30", "London")
    }
}
```

## Testing Status

- ✅ All existing unit tests pass
- ✅ Code compiles successfully
- ✅ Kotlin formatting applied and verified
- ✅ Validation system properly integrated

## Impact

Starting with Phase 1 - implementing Table and TableRow response models in `Block.kt`, following the established
patterns in the codebase.
This implementation adds **table block support** to the Kotlin Notion Client, addressing the most significant gap in
block type coverage. The client now supports:

- **19 out of ~30** block types (previously 17)
- **Complete table functionality** with proper validation
- **Type-safe DSL** for table construction
- **Comprehensive validation** for table constraints

The implementation follows all established patterns in the codebase and maintains backward compatibility while adding
this critical missing functionality.
