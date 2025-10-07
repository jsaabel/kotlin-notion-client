# Documentation Strategy for Public Release

**Date**: 2025-10-05
**Status**: Planning
**Context**: Preparing project for public release after completing core API implementation

## Background

With the project now covering virtually all aspects of the Notion API (pages, databases, blocks, data sources, search, comments, users, file uploads), we're approaching a point where public release makes sense. However, the codebase is large and comprehensive, making a single monolithic README impractical.

Additionally, this project has been developed heavily using LLM assistance (Claude Code), which presents both a transparency opportunity and a documentation challenge.

## Documentation Philosophy

**Progressive Disclosure**: Users should be able to get oriented quickly, then dive deeper as needed.

**Show, Don't Tell**: Every example should be runnable code demonstrating real capabilities.

**Transparency**: Being honest about development approach (LLM-assisted) and maturity level.

## Proposed Structure

### Main README.md (~150-200 lines)

Navigation hub with:
1. **Hero section** - One-liner + badges
2. **Key differentiators** (3-4 bullets):
   - Latest API version (2025-09-03) with full data sources support
   - Comprehensive type-safe DSLs for all operations
   - Kotlin-first (coroutines, null-safety, functional builders)
   - Extensively tested against official Notion samples
3. **Quick installation** (Gradle snippets)
4. **Minimal quick-start** (3-5 lines)
5. **API Coverage Table** (simplified with links to detailed docs)
6. **Documentation Links** - Navigation to detailed guides
7. **Development Context** - "Built with Claude Code" section
8. **Build/Contribute/License** (minimal)

### Detailed API Documentation (`docs/` folder)

Individual guides for each major API area:
- `pages.md`
- `databases.md`
- `blocks.md`
- `data-sources.md`
- `search.md`
- `comments.md`
- `users.md`
- `rich-text-dsl.md`
- `error-handling.md`
- `testing.md`

**Template for each**:
1. What it does (2-3 sentences + link to official docs)
2. Available operations (method signatures)
3. Hands-on examples (simple → DSL → complex)
4. Common patterns (tips, gotchas, best practices)
5. Related APIs (cross-links)

Target: ~100-200 lines each

### Supporting Documentation

- **QUICKSTART.md**: Zero-to-first-API-call in <5 minutes
- **ARCHITECTURE.md**: For contributors - design decisions, patterns, testing strategy

### Standard WIP Notice

All documentation files should include this notice at the top until ready for public release:

```markdown
> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.
```

## Key Strategy Points

1. **Progressive disclosure**: README → Quick-start → Detailed docs
2. **Show, don't tell**: Every example runnable
3. **Highlight DSL advantages**: Show JSON vs. Kotlin DSL comparisons where impactful
4. **Practical examples**: Real use cases (task management, CMS, etc.)
5. **Link liberally**: Cross-reference and link to official Notion docs
6. **Honest about maturity**: Clear on what's battle-tested vs. new
7. **Claude Code transparency**: Brief, factual, non-marketing
8. **WIP notices**: All documentation should have "Work In Progress" notice until ready for public release
9. **Database vs. Data Source clarity**: CRITICAL - Always distinguish between databases (containers) and data sources (tables with properties)
10. **Validated examples**: Every code example must be verified to actually work before marking doc as complete

## Critical: Database vs. Data Source Distinction

**This is the most important concept to communicate clearly in all documentation.**

### The 2025-09-03 API Change

In Notion API version 2025-09-03, the fundamental model changed:

**Before (2022-06-28)**:
- Database = single table with properties
- Pages lived directly in databases
- Query database → get pages

**After (2025-09-03)**:
- **Database** = container that holds one or more data sources
- **Data Source** = the actual table with properties and schema
- Pages live in data sources (not databases)
- Query data source → get pages

**Hierarchy**:
```
Database (container)
├── Data Source 1 (properties: Name, Status, Tags)
│   ├── Page (row)
│   └── Page (row)
└── Data Source 2 (properties: Title, Priority, Owner)
    ├── Page (row)
    └── Page (row)
```

### Impact on Documentation

**Every documentation file must**:
1. Use "database" only for the container concept
2. Use "data source" for the table/schema/properties concept
3. Explain that most operations (query, create pages, etc.) work on **data sources**, not databases
4. Show that `DatabasesApi` is for container operations, `DataSourcesApi` is for data operations
5. Clarify that pages have `data_source_id` as parent, not `database_id`

**Example Documentation Pattern**:
```markdown
### Querying Data

To query pages from a Notion table, you query a **data source**, not the database container:

```kotlin
// Query a data source (the table)
val pages = notion.dataSources.query("data-source-id") {
    // ...
}
```

Note: In older API versions, you would query a "database". In 2025-09-03,
databases are containers - you query the data sources within them.
```

### Where This Matters Most

1. **databases.md** - Must explain container concept clearly
2. **data-sources.md** - Must explain this is where the actual data lives
3. **pages.md** - Must show parent is `data_source_id`
4. **search.md** - Must show filter is by `data_source`, not `database`
5. **QUICKSTART.md** - Should introduce this distinction early
6. **README.md** - Should mention this is 2025-09-03 with data sources

## Documentation Validation Strategy

**Critical principle**: All code examples in documentation must be proven to work before marking documentation as complete.

### The Problem

Documentation with broken examples is worse than no documentation. Common issues:
- API signatures change but docs don't update
- Examples use non-existent methods
- DSL syntax shown doesn't actually compile
- Property names or types are wrong

### The Solution

For each completed documentation file, create executable validation code that:
1. Uses every code example from that doc
2. Compiles successfully
3. Can be run against real Notion API (as integration test)
4. Serves as both validation and as runnable examples for users

### Implementation

**Location**: `src/test/kotlin/examples/` directory structure:
```
src/test/kotlin/examples/
├── DataSourcesExamples.kt
├── DatabasesExamples.kt
├── PagesExamples.kt
├── BlocksExamples.kt
└── ... (one file per API doc)
```

**Structure**: Each file contains:
```kotlin
@Tags("Integration", "RequiresApi", "Examples")
class DataSourcesExamples : StringSpec({
    if (!integrationTestEnvVarsAreSet()) {
        "!(Skipped) Data sources examples" {
            println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true")
        }
    } else {
        val client = NotionClient.create(System.getenv("NOTION_API_TOKEN"))

        "Example 1: Retrieve a data source" {
            // Exact code from docs/data-sources.md
            val dataSource = notion.dataSources.retrieve("data-source-id")
            println("Name: ${dataSource.name}")
            // ... etc
        }

        "Example 2: Query pages from a data source" {
            // Next example from the docs
        }

        // ... all other examples from the doc
    }
})
```

**Benefits**:
1. **Validation** - Proves examples work
2. **Maintenance** - Breaking changes caught by test failures
3. **User value** - Runnable examples in codebase
4. **CI integration** - Can run in CI (with mocked API or test workspace)
5. **Documentation sync** - Forces docs to stay in sync with code

### Workflow

For each documentation file:

1. **Draft the doc** - Write examples based on API knowledge
2. **Create validation file** - Extract all examples into test file
3. **Run validation** - Execute against real API
4. **Fix issues** - Update both doc and validation code
5. **Mark complete** - Only when all examples pass
6. **Ongoing** - Re-run validation when API changes

### Running Validations

```bash
# Run all example validations
export NOTION_RUN_INTEGRATION_TESTS=true
./gradlew test --tests "*Examples"

# Run specific doc validation
./gradlew test --tests "*DataSourcesExamples"
```

### Example Validation Status Tracking

Add to each doc file (after WIP notice):

```markdown
> **📝 Example Validation**: ✅ All examples verified | ⏳ Pending validation | ❌ Contains unvalidated examples
```

### Integration with Documentation Process

**Phase 1 (Draft)**:
- Write doc with examples
- Mark as WIP

**Phase 2 (Validation)**:
- Create examples test file
- Run and fix until all pass
- Update validation status

**Phase 3 (Complete)**:
- Remove WIP notice (or keep until full release)
- Mark examples as validated
- Ready for user consumption

## Decisions Made

### Notebooks
- **Decision: Postpone until post-Maven Central publication**
- Current notebook uses fat JAR approach (`shadowJar`) not in our build
- Options considered:
  1. Add shadow plugin (fat JARs not ideal for libraries)
  2. Add maven-publish plugin (proper approach, but requires local publishing)
  3. Postpone notebooks (simplest, focus on core docs)
  4. Executable .kts scripts (alternative)
- **Rationale**:
  - Once published to Maven Central, notebooks become trivial (`@DependsOn("coordinates")`)
  - Text-based examples in markdown often clearer than notebooks
  - Focus limited pre-release time on polishing core documentation
  - Can extract good examples from existing notebook into API docs
- **Post-launch**: Notebooks will be easy to add with proper Maven coordinates

### Code Samples
- **No separate repository** - include examples in main repo
- Keeps everything together, easier maintenance

### API Documentation
- **No auto-generation from KDoc** - keep it manual/narrative
- More control over presentation and examples
- KDoc still valuable for IDE support

### Versioning
- **Single API version support** (latest only: 2025-09-03)
- Simplifies documentation and maintenance
- Clear upgrade path when new versions release

### Development Transparency
- **Keep journals in repo** - maximum transparency
- Shows real development process with LLM assistance
- Educational value for others exploring AI-assisted development
- Demonstrates iterative approach

## Implementation Phases

### Phase 1 (Minimum Viable) ✅ COMPLETED
- ✅ Polish main README
- ✅ Complete: `data-sources.md` (fully documented as template)
- ✅ Placeholder structure for all API docs
- ✅ QUICKSTART.md
- ⏳ Next: Validate `data-sources.md` examples

### Phase 2 (Pre-Launch)
- Complete remaining API docs following `data-sources.md` template
  - `pages.md` → create `PagesExamples.kt` → validate
  - `databases.md` → create `DatabasesExamples.kt` → validate
  - `blocks.md` → create `BlocksExamples.kt` → validate
  - And so on for each doc
- ARCHITECTURE.md
- Verify all examples pass validation
- Create `examples/README.md` explaining validation approach

### Phase 3 (Post-Launch)
- Jupyter notebook examples (easy with Maven coordinates)
- Maven Central publication setup
- Video walkthroughs (optional)
- Blog post about development journey

## Next Steps

1. **Begin README polish** - Start with main README based on template
2. **Create documentation templates** - Establish pattern for API docs
3. **Extract runnable examples** - From tests/existing code, including notebook examples
4. **Set up docs/ directory structure** - Create placeholder files for API docs

## Open Questions

- Should journals be in root or `docs/development-journal/`?
- Include build badges from GitHub Actions?
- Host docs on GitHub Pages or keep in repo?
- Create a "Why Kotlin?" section comparing to Python client?

## Success Metrics

Documentation is successful if:
1. New user can make first API call in <5 minutes
2. Users can find relevant examples without asking
3. DSL syntax is immediately understandable from examples
4. Development approach (LLM-assisted) is clear and credible
