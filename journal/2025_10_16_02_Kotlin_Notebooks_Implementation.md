# Kotlin Notebooks Implementation

## Project Information
- **Date**: October 16, 2025
- **Feature**: Interactive Kotlin Notebooks for Library Exploration
- **Status**: 🚧 In Progress (Notebook 1 Complete)
- **Phase**: 1 - Implementation

## Overview

Create Kotlin Jupyter notebooks that demonstrate the Kotlin Notion Client library functionality in an interactive, exploratory format. These notebooks will serve as both:
1. **Learning tools** - Help users understand and experiment with the library
2. **Testing tools** - Allow both developers and users to verify API behavior
3. **Living documentation** - Executable examples that stay current with the API

## Objectives

### Primary Goals
1. **Interactive Learning Experience**: Provide hands-on exploration of key library features
2. **Alternative to Integration Tests**: Give users a choice between traditional tests and notebooks
3. **API Verification**: Enable users to test against their own Notion workspaces
4. **Documentation**: Serve as executable, verifiable examples

### Learning Outcomes
- Users can experiment with API calls in real-time
- Clear understanding of library capabilities through practical examples
- Reduced barrier to entry for new users
- Point of departure for our own experiments and explorations

## Technical Approach

### Notebook Structure

**Proposed Directory Layout**:
```
notebooks/
├── README.md                           # Setup instructions and overview
├── 01-getting-started.ipynb            # Authentication, basic page retrieval
├── 02-reading-databases.ipynb          # Query and filter operations
├── 03-creating-pages.ipynb             # Page creation with properties
├── 04-working-with-blocks.ipynb        # Block operations and nesting
├── 05-rich-text-dsl.ipynb             # Rich text formatting showcase
└── 06-advanced-queries.ipynb           # Complex filters and pagination
```

### Implementation Principles

Based on insights from previous notebook development (see docs/notebooks.md prior version):

1. **Use `runBlocking` for Suspend Functions**
   - Jupyter cells are not suspend contexts
   - Wrap all API calls: `val result = runBlocking { notion.pages.retrieve(id) }`

2. **Base on Integration Tests**
   - Integration tests provide verified, working examples
   - Extract core patterns, simplify for demonstration
   - Add explanatory markdown cells

3. **Environment Setup Pattern**
   ```kotlin
   @file:DependsOn("it.saabel:kotlin-notion-client:0.1.0")

   import it.saabel.kotlinnotionclient.NotionClient
   import kotlinx.coroutines.runBlocking

   val apiToken = System.getenv("NOTION_API_TOKEN")
       ?: error("NOTION_API_TOKEN environment variable not set")
   val notion = NotionClient(apiToken)
   ```

4. **Keep Examples Focused**
   - One feature per notebook
   - Show actual output from real API calls
   - Add error handling demonstrations
   - Use real IDs (guide users to set up their own test pages)

### Notebook Flow Template

For each notebook:
1. **Introduction cell** - Markdown explaining what this notebook demonstrates
2. **Setup cell** - Dependencies and client initialization
3. **Example cells** - One concept per cell with preceding markdown explanation
4. **Output cells** - Show actual API responses (formatted as JSON or pretty-printed)
5. **Cleanup cell** (optional) - Archive test pages/databases if appropriate

## Task Breakdown

### Phase 1: Infrastructure & Setup ✅ Complete
- [x] Create journal entry (this document)
- [x] Create `notebooks/` directory
- [x] Write `notebooks/README.md` with setup instructions
- [x] Document environment variable requirements
- [x] Add `.gitignore` entries for notebook outputs (if needed)

### Phase 2: Core Notebooks (2-3 hours)

#### Notebook 1: Getting Started ✅ Complete
**Based on**: `PagesIntegrationTest.kt` - `retrieve page` test

**Content**:
- Installing dependencies
- Authentication setup
- Retrieving a page by ID
- Understanding the Page object structure
- Basic error handling

**Implementation Notes**:
- Created `01-getting-started.ipynb` with 17 cells (8 code, 9 markdown)
- Demonstrates client initialization, page retrieval, property inspection, parent handling, error handling
- All examples tested successfully against live Notion API
- Identified several strong typing gaps during development (see API Improvements section)

---

#### Notebook 2: Reading Databases (30 min)
**Based on**: `DataSourcesIntegrationTest.kt` - query operations

**Content**:
- Understanding data sources vs databases
- Querying a data source
- Filtering results
- Sorting results
- Working with property values

**Example code to include**:
```kotlin
// Query with filter
val results = runBlocking {
    notion.dataSources.query(dataSourceId) {
        filter {
            and {
                select("Status").equals("Active")
                date("Created").onOrAfter(LocalDate.now().minusDays(7))
            }
        }
        sortBy {
            property("Created", Direction.DESCENDING)
        }
    }
}
```

---

#### Notebook 3: Creating Pages (30 min)
**Based on**: `PagesIntegrationTest.kt` - create operations

**Content**:
- Creating pages in databases (data sources)
- Setting property values (title, text, select, date, etc.)
- Using the properties DSL
- Handling different property types

**Example code to include**:
```kotlin
// Create a page with properties
val newPage = runBlocking {
    notion.pages.create {
        parent.dataSource(dataSourceId)
        properties {
            title("Name", "My New Page")
            richText("Description", "A test page created from notebook")
            select("Status", "Active")
            date("Created", LocalDate.now())
        }
    }
}
```

---

#### Notebook 4: Working with Blocks (30 min)
**Based on**: `BlocksIntegrationTest.kt` - block operations

**Content**:
- Understanding block hierarchy
- Creating different block types
- Nesting blocks (children)
- Appending blocks to pages
- Updating and deleting blocks

**Example code to include**:
```kotlin
// Create nested block structure
runBlocking {
    notion.blocks.append(pageId) {
        heading1 { text("Main Section") }
        paragraph {
            text("This is a paragraph with ")
            bold("bold text")
        }
        bulletedList {
            item { text("First bullet point") }
            item { text("Second bullet point") }
        }
        toggle {
            text("Click to expand")
            children {
                paragraph { text("Hidden content!") }
            }
        }
    }
}
```

---

#### Notebook 5: Rich Text DSL (30 min)
**Based on**: `RichTextDslIntegrationTest.kt`

**Content**:
- Rich text formatting options
- Text annotations (bold, italic, color)
- Links and mentions
- Equations
- Combining multiple formatting styles

**Example code to include**:
```kotlin
// Rich text showcase
runBlocking {
    notion.blocks.append(pageId) {
        paragraph {
            text("Hello ")
            bold("world")
            text("! This is ")
            colored("colored", TextColor.BLUE)
            text(" and ")
            italic("italic")
            text(". Visit ")
            link("https://notion.so", "Notion")
            text(" for more info.")
        }

        paragraph {
            text("Created by ")
            userMention(userId)
            text(" on ")
            dateMention(LocalDate.now())
        }

        paragraph {
            text("The equation ")
            equation("E = mc^2")
            text(" is famous.")
        }
    }
}
```

---

#### Notebook 6: Advanced Queries (30 min)
**Based on**: `DatabaseQueryIntegrationTest.kt` - complex queries

**Content**:
- Complex nested filters (AND/OR logic)
- Multiple sort criteria
- Pagination (manual and flow-based)
- Performance considerations

**Example code to include**:
```kotlin
// Complex query with pagination
import kotlinx.coroutines.flow.toList

val allResults = runBlocking {
    notion.dataSources.queryFlow(dataSourceId) {
        filter {
            or {
                and {
                    select("Priority").equals("High")
                    checkbox("Completed").equals(false)
                }
                and {
                    select("Priority").equals("Critical")
                    date("DueDate").onOrBefore(LocalDate.now())
                }
            }
        }
        sortBy {
            property("Priority", Direction.ASCENDING)
            property("DueDate", Direction.ASCENDING)
        }
    }.toList()
}

println("Found ${allResults.size} matching pages")
```

### Phase 3: Documentation (30 min)
- [ ] Write `notebooks/README.md` with:
  - Prerequisites (Jupyter with Kotlin kernel)
  - Environment setup (`NOTION_API_TOKEN`, test page IDs)
  - How to run notebooks
  - Tips for exploration
  - Link to main documentation
- [ ] Update main project README to mention notebooks
- [ ] Transform `docs/notebooks.md` into user-facing guide

### Phase 4: Testing & Validation (30 min)
- [ ] Restart kernel and run all cells in each notebook
- [ ] Verify no sensitive data in outputs
- [ ] Test error handling scenarios
- [ ] Ensure cleanup cells work correctly
- [ ] Verify against live Notion API

## Success Criteria

### Notebook Quality
- ✅ All cells execute without errors
- ✅ Clear explanations in markdown cells
- ✅ Real API outputs visible
- ✅ Error handling demonstrated
- ✅ No sensitive data exposed

### User Experience
- ✅ Easy to set up (clear instructions in README)
- ✅ Immediate value (can modify and re-run examples)
- ✅ Progressive complexity (simple → advanced)
- ✅ Self-contained (each notebook independent)

### Documentation
- ✅ Notebooks complement existing docs
- ✅ Patterns consistent with written documentation
- ✅ Code examples match current API
- ✅ Clear navigation between notebooks

## Implementation Notes

### Environment Variables Required
Users will need to set:
- `NOTION_API_TOKEN` - Their Notion integration token
- `NOTION_TEST_PAGE_ID` - A test page they can modify
- `NOTION_TEST_DATABASE_ID` - A test database/data source

### Common Patterns to Use

**Error Handling**:
```kotlin
import it.saabel.kotlinnotionclient.models.errors.NotionError

try {
    val page = runBlocking { notion.pages.retrieve(pageId) }
    println("Success: ${page.id}")
} catch (e: NotionError.ObjectNotFound) {
    println("Page not found: ${e.message}")
} catch (e: NotionError.Unauthorized) {
    println("Invalid API token")
} catch (e: NotionError) {
    println("API error: ${e.message}")
}
```

**Pagination**:
```kotlin
import kotlinx.coroutines.flow.toList

val allPages = runBlocking {
    notion.dataSources.queryFlow(databaseId).toList()
}
println("Found ${allPages.size} pages")
```

### Tips for Live Demonstrations

- Use descriptive variable names for readability
- Print intermediate results to show step-by-step progress
- Keep API token safe (don't display in outputs)
- Have backup test IDs ready
- Test notebooks before presenting
- Mention rate limits when relevant

## Integration with Existing Documentation

### Current docs/notebooks.md Content
The existing `docs/notebooks.md` contains:
- Developer-focused insights about creating notebooks
- Technical patterns we discovered
- Troubleshooting tips
- Best practices for notebook development

### Proposed Changes
1. **Move to journal**: Internal development insights belong here (in this journal)
2. **Transform docs/notebooks.md**: Rewrite as user-facing guide covering:
   - "Exploring the Library with Kotlin Notebooks"
   - How to set up Jupyter with Kotlin kernel
   - How to run the provided notebooks
   - How to create your own exploration notebooks
   - Tips for working with the Notion API in notebooks
   - Troubleshooting common issues

This separation keeps:
- **Journal**: Internal development process, decisions, patterns we learned
- **Docs**: User-facing guidance on how to USE notebooks with our library

## Insights from Previous Notebook Development

Key learnings from earlier notebook work (preserved from docs/notebooks.md):

### Technical Patterns
1. **Suspend Functions**: Always use `runBlocking { }` wrapper
2. **Dependencies**: Specify with `@file:DependsOn("it.saabel:kotlin-notion-client:0.1.0")`
3. **Integration Tests as Source**: Mine integration tests for verified patterns
4. **Real Data**: Show actual API responses, not mock data

### Common Pitfalls
- **Kernel Issues**: Sometimes need to restart kernel for dependency updates
- **API Errors**:
  - 401 = Check `NOTION_API_TOKEN`
  - 404 = Verify page/database exists and integration has access
  - 429 = Rate limited, add delays
  - 400 = Check request body against API docs
- **Suspend Errors**: Missing `runBlocking` wrapper

### Design Decisions
- Don't use `-i` interactive flags (not supported in notebooks)
- Keep each notebook focused on one concept
- Show both success and error scenarios
- Clean up test data to avoid clutter

## Future Enhancements

**Possible additions for v0.2.0+**:
- Notebook demonstrating rate limiting and retries
- Notebook showing file upload operations
- Notebook for user/workspace operations
- Advanced pagination patterns notebook
- Performance optimization examples
- Batch operations notebook

### API Improvements Identified During Notebook Development

**1. Strong Typing for Parent Objects** (Priority: Medium)
- **Current State**: `Parent` is a flat data class with nullable fields and string-based `type`
- **Issue**: No compile-time safety, requires string matching in when expressions
- **Improvement**: Convert to sealed class hierarchy
  ```kotlin
  sealed class Parent {
      data class PageParent(val pageId: String) : Parent()
      data class DataSourceParent(val dataSourceId: String) : Parent()
      data class DatabaseParent(val databaseId: String) : Parent()
      data class WorkspaceParent(val workspace: Boolean) : Parent()
      data class BlockParent(val blockId: String) : Parent()
  }
  ```
- **Benefits**:
  - Exhaustive when expressions
  - Better IDE autocomplete
  - Compile-time type safety
  - More idiomatic Kotlin
- **Impact**: Would be a breaking change, but we're pre-1.0
- **Timeline**: Consider for v0.2.0 or before v1.0
- **Action**: After completing notebooks, implement strong typing and update notebook examples

**2. Strong Typing for PageIcon and PageCover** (Priority: Medium)
- **Current State**: `PageIcon` and `PageCover` are flat data classes with nullable fields and string-based `type`
- **Issue**: Similar to Parent - no compile-time safety, verbose nullable access (`page.icon?.emoji`, `page.icon?.external?.url`)
- **Improvement**: Convert to sealed class hierarchies
  ```kotlin
  sealed class PageIcon {
      data class Emoji(val emoji: String) : PageIcon()
      data class External(val url: String) : PageIcon()
      data class File(val url: String, val expiryTime: String?) : PageIcon()
  }

  sealed class PageCover {
      data class External(val url: String) : PageCover()
      data class File(val url: String, val expiryTime: String?) : PageCover()
  }
  ```
- **Benefits**:
  - Cleaner access patterns: `when (icon) { is PageIcon.Emoji -> icon.emoji }`
  - No nested nullable chains
  - Type-safe pattern matching
  - Consistent with Kotlin best practices
- **Impact**: Would be a breaking change, but we're pre-1.0
- **Timeline**: Consider for v0.2.0 or before v1.0
- **Action**: Implement together with Parent strong typing for consistency

**3. Strong Typing for Media Block Content Classes** (Priority: Medium-Low)
- **Current State**: `ImageContent`, `VideoContent`, `AudioContent`, `FileContent`, `PDFContent` all have the same flat structure with `type: String` and nullable `external`, `file`, `fileUpload` fields
- **Issue**: Same pattern - verbose nullable chains like `image.external?.url`, `video.file?.url`, etc.
- **Improvement**: Could create a sealed `MediaSource` hierarchy used by all media blocks:
  ```kotlin
  sealed class MediaSource {
      data class External(val url: String, val caption: List<RichText> = emptyList()) : MediaSource()
      data class File(val url: String, val expiryTime: String, val caption: List<RichText> = emptyList()) : MediaSource()
      data class FileUpload(val id: String, val caption: List<RichText> = emptyList()) : MediaSource()
  }

  data class ImageContent(val source: MediaSource)
  data class VideoContent(val source: MediaSource)
  // etc.
  ```
- **Benefits**:
  - Consistent pattern across all media types
  - Type-safe access: `when (content.source) { is MediaSource.External -> ... }`
  - Reduced code duplication
- **Priority**: Lower than Parent/Icon/Cover since media blocks are less commonly accessed
- **Timeline**: Consider for v0.2.0 or later
- **Action**: Evaluate if the consistency benefit outweighs the refactoring cost

**4. Strong Typing for CalloutIcon** (Priority: Low)
- **Current State**: `CalloutIcon` has the same pattern - `type: String` with nullable `emoji`, `external`, `file`
- **Issue**: Same as above
- **Improvement**: Similar to PageIcon:
  ```kotlin
  sealed class CalloutIcon {
      data class Emoji(val emoji: String) : CalloutIcon()
      data class External(val url: String) : CalloutIcon()
      data class File(val url: String, val expiryTime: String) : CalloutIcon()
  }
  ```
- **Priority**: Low - callout icons are rarely accessed directly
- **Timeline**: Consider bundling with PageIcon/PageCover changes for consistency
- **Action**: Implement if doing comprehensive icon/cover refactoring

### Summary of Strong Typing Opportunities

**High Priority**:
1. Parent (most commonly used, biggest DX impact)
2. PageIcon + PageCover (commonly accessed, clear use case in notebooks)

**Medium Priority**:
3. Media block content classes (nice for consistency, but less commonly accessed)
4. CalloutIcon (bundle with PageIcon/PageCover for consistency)

**Implementation Strategy**:
- Start with Parent, PageIcon, PageCover (most impactful)
- Consider media blocks if feedback suggests they're commonly used
- All are pre-1.0, so no compatibility concerns

## Resources

**Integration tests to reference**:
- `src/test/kotlin/integration/PagesIntegrationTest.kt`
- `src/test/kotlin/integration/DataSourcesIntegrationTest.kt`
- `src/test/kotlin/integration/BlocksIntegrationTest.kt`
- `src/test/kotlin/integration/RichTextDslIntegrationTest.kt`
- `src/test/kotlin/integration/DatabaseQueryIntegrationTest.kt`

**Documentation to align with**:
- `QUICKSTART.md`
- `docs/pages.md`
- `docs/data-sources.md`
- `docs/blocks.md`
- `docs/rich-text-dsl.md`

## Status & Next Steps

**Current Phase**: Phase 1 Complete, Phase 2 In Progress (1/6 notebooks complete) ✅

**Completed This Session**:
1. ✅ Created `notebooks/` directory structure
2. ✅ Created `notebooks/README.md` with setup instructions
3. ✅ Implemented and tested Notebook 1: Getting Started (17 cells, fully functional)
4. ✅ Identified 4 categories of strong typing improvements through notebook testing

**Immediate Next Steps**:
1. Commit current work (notebooks/, journal updates)
2. Continue with remaining notebooks (2-6)
3. Consider implementing strong typing improvements before completing all notebooks
4. Transform `docs/notebooks.md` into user-facing guide
5. Update main README to mention notebooks

**Remaining Estimated Time**: 3-4 hours
- Phase 2 (5 remaining notebooks): 2-3 hours
- Phase 3 (Documentation): 30 min
- Phase 4 (Testing): 30 min

**Success Indicators**:
- ✅ Users can easily explore library functionality (validated with Notebook 1)
- ✅ Notebooks provide immediate value for experimentation (validated)
- ⏳ Clear progression from basic to advanced usage (in progress)
- ✅ Complements existing documentation effectively (validated)
- ✅ Notebooks successfully identify API design improvements (validated)

---

## Notes & Observations

### Implementation Session 1 (October 16, 2025)

**Key Findings**:
- Notebooks are excellent tools for identifying API design gaps
- Interactive testing revealed 4 areas where strong typing would improve DX
- Library published to Maven Local works seamlessly in notebooks
- Real API integration provides valuable validation of examples

**Questions Addressed**:
- ✅ Should notebooks use v0.1.0 from Maven Central or local build? **Decision**: Use published version (Maven Central), but Maven Local works for testing
- ✅ How to handle version updates? **Decision**: Hardcode v0.1.0 for now, update with releases
- ✅ Should we include cleanup automation or leave it manual? **Decision**: Manual cleanup in final cell (optional)

**Decisions Made**:
1. Use `@file:DependsOn("it.saabel:kotlin-notion-client:0.1.0")` for dependency
2. All API calls wrapped in `runBlocking { }` for notebook context
3. Show real API outputs to demonstrate actual behavior
4. Include error handling examples in each notebook
5. Mark strong typing improvements as medium priority for future implementation

**Challenges Encountered**:
1. Had to republish to Maven Local after constructor changes
2. Several iterations needed to fix incorrect type assumptions (PagePropertyValue vs PageProperty)
3. Discovered Parent, PageIcon, PageCover use flat data classes instead of sealed classes
4. Exception hierarchy doesn't include ObjectNotFound (use ApiError with status check)

**Validation Results**:
- ✅ All 17 cells in Notebook 1 execute successfully
- ✅ No sensitive data exposed in outputs
- ✅ Error handling works as expected
- ✅ Examples accurately reflect 2025-09-03 API behavior
- ✅ Identified actionable improvements for library design

---

**Status**: Phase 1 Complete, Phase 2 In Progress (16% complete)