# Kotlin Notebooks Progress

**Date**: October 17, 2025
**Status**: ✅ ALL 6 NOTEBOOKS COMPLETE

## Overview

Successfully created 6 Kotlin Jupyter notebooks demonstrating the Kotlin Notion Client library:
1. ✅ Getting Started
2. ✅ Reading Databases and Data Sources
3. ✅ Creating Pages
4. ✅ Working with Blocks
5. ✅ Rich Text DSL
6. ✅ Advanced Queries

**Total**: 50+ examples, all tested against live Notion API

## Critical Patterns for Working with Kotlin Notebooks (For Claude)

### The Essential Workflow
1. **Copy-First Strategy**: Always copy an existing working notebook as template
   ```bash
   cp notebooks/05-rich-text-dsl.ipynb notebooks/06-advanced-queries.ipynb
   ```

2. **Use NotebookEdit Tool Exclusively**:
   - ❌ NEVER use Write or Edit tools on `.ipynb` files
   - ✅ ALWAYS use NotebookEdit tool
   - ✅ ALWAYS specify `cell_type` parameter explicitly

3. **Always Specify cell_type**:
   ```python
   NotebookEdit(
       notebook_path="...",
       cell_id="cell-5",
       cell_type="code",    # or "markdown" - REQUIRED
       new_source="..."
   )
   ```

4. **Source from Integration Tests**: Use verified examples from `src/test/kotlin/examples/` and `src/test/kotlin/integration/`

5. **Test Incrementally**: Have user test in IntelliJ during development

### Why This Matters
- Write/Edit tools corrupt `.ipynb` format → notebooks won't open in IntelliJ
- NotebookEdit understands IntelliJ Kotlin Notebook format
- Integration tests provide pre-validated, production-ready examples
- User testing catches issues that automated validation misses

## Key Technical Learnings

### Rich Text DSL
- **Combined formatting**: Use `boldItalic()` or `formattedText(text, bold = true, italic = true, ...)`
- **Not this**: ❌ `bold { italic("...") }` (doesn't exist)
- **Found via**: Task tool with Explore agent

### Notion API Limitations
- Code blocks: `language` parameter sets UI hint, but API returns plain text (no syntax highlighting in responses)
- Always test features against live API to verify actual capabilities

### Database Operations
- **Archive databases**: `databases.archive(id)` (preferred over `update { archived = true }`)
- **Data source ID**: Needed for querying (not database ID)
- **API delays**: Use `delay()` after bulk operations for indexing

### Concurrent Operations
```kotlin
// Create pages concurrently for better performance
val pages = runBlocking {
    tasks.map { task ->
        async {
            notion.pages.create { /* ... */ }
        }
    }.awaitAll()
}
```

### Query DSL Patterns
```kotlin
notion.dataSources.query(dataSourceId) {
    filter {
        and(
            or(
                select("Priority").equals("High"),
                number("Score").greaterThanOrEqualTo(90.0)
            ),
            checkbox("Completed").equals(false)
        )
    }
    sortBy("Score", SortDirection.DESCENDING)
}
```

### Property Access (Type-Safe)
```kotlin
val title = page.getTitleAsPlainText("Name")
val score = page.getNumberProperty("Score")  // Returns Double?
val status = page.getSelectProperty("Status")  // Returns Select?
```

## Issues Encountered & Solutions

### Cell Type Confusion (Session 3)
- **Problem**: Cells had wrong types (code as markdown, markdown as code)
- **Solution**: Always explicitly specify `cell_type` parameter in NotebookEdit
- **Prevention**: NotebookEdit doesn't preserve original cell type without explicit parameter

### Rich Text DSL Syntax Error (Session 4)
- **Problem**: Used non-existent `bold { italic("...") }` syntax
- **Solution**: Found correct patterns via Task tool (Explore agent): `boldItalic()` and `formattedText()`
- **Prevention**: Always verify DSL syntax against existing examples

### IntelliJ Indexing Quirk (Session 2)
- **Problem**: "Unresolved reference" warnings in editor
- **Solution**: Invalidate caches (File → Invalidate Caches → Restart)
- **Note**: Code executes successfully despite warnings

## Best Practices

1. **Progressive Complexity**: Start simple → intermediate → advanced
2. **Real API**: Use live Notion API for authentic examples
3. **Cleanup Cells**: Always include cleanup to avoid cluttering test workspaces
4. **Comprehensive Summaries**: Include API reference in each notebook
5. **Concurrent Operations**: Showcase `async`/`awaitAll` for API clients
6. **Type Safety**: Use kotlinx.datetime types for dates

## Development Stats

- **Total Time**: ~2-3 hours across 6 sessions
- **Average per Notebook**: 15-25 minutes (after pattern established)
- **Session 1**: Longest (establishing pattern)
- **Sessions 2-6**: Fast (pattern well-established)

## Next Steps

- Commit all changes
- Squash temporary notebook commits into single clean commit
- Push to repository
- Consider: README for notebooks/ directory with overview and prerequisites