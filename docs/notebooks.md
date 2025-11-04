# Jupyter Notebook Development Guide

## Overview

This guide documents best practices for creating Jupyter notebooks with Kotlin that demonstrate the Kotlin Notion Client library.

## Prerequisites

- Jupyter with Kotlin kernel installed
- Notion API token (`NOTION_API_TOKEN` environment variable)
- Test page/database IDs for live API testing

## Notebook Creation Approach

### 1. Use `runBlocking` for Suspend Functions

Since Jupyter notebook cells are not suspend contexts, wrap API calls with `runBlocking`:

```kotlin
import kotlinx.coroutines.runBlocking

val page = runBlocking {
    notion.pages.retrieve(pageId)
}
```

### 2. Base Notebooks on Integration Tests

Our integration tests provide excellent, verified examples:
- Located in: `src/test/kotlin/it/saabel/kotlinnotionclient/integration/`
- Already tested against live API
- Demonstrate correct usage patterns
- Include proper error handling

**Workflow**:
1. Find relevant integration test (e.g., `PagesIntegrationTest.kt`)
2. Extract the core API usage pattern
3. Simplify for demonstration purposes
4. Wrap suspend calls in `runBlocking`
5. Add explanatory markdown cells

### 3. Environment Setup in Notebooks

```kotlin
// Load dependencies
@file:DependsOn("it.saabel:kotlin-notion-client:0.2.0")

// Imports
import it.saabel.kotlinnotionclient.NotionClient
import kotlinx.coroutines.runBlocking

// Initialize client
val apiToken = System.getenv("NOTION_API_TOKEN")
    ?: error("NOTION_API_TOKEN environment variable not set")
val notion = NotionClient(apiToken)
```

### 4. Notebook Structure

**Recommended flow**:
1. **Introduction cell** - What this notebook demonstrates
2. **Setup cell** - Dependencies and client initialization
3. **Example cells** - One concept per cell with explanation
4. **Output cells** - Show actual API responses
5. **Cleanup cell** (optional) - Archive test pages/databases

### 5. Good Practices

- **Keep examples focused**: One feature per notebook
- **Show actual output**: Let users see real API responses
- **Add error handling**: Demonstrate try/catch patterns
- **Use real IDs**: Work with actual Notion pages/databases
- **Clean up after**: Archive or delete test data when appropriate
- **Add comments**: Explain non-obvious API behavior

## Example Notebooks

### Rich Text DSL Showcase

Demonstrates the Rich Text DSL with various formatting options:
- Basic text with annotations (bold, italic, color)
- Links and mentions
- Date mentions
- Equations
- Code snippets

**Based on**: `RichTextDslIntegrationTest.kt`

### Hands-On Integration Test Demo

Live demonstration of running integration tests against the Notion API:
- Page CRUD operations
- Database queries
- Block manipulation
- Real-time API interaction

**Based on**: `PagesIntegrationTest.kt`, `DatabasesIntegrationTest.kt`

### Published Library Test

Verifies that the published library works correctly:
- Install from Maven Central
- Initialize client
- Run basic operations
- Useful for post-publication verification

## Testing Notebooks

Before committing notebooks:
1. **Restart kernel and run all cells** - Ensure reproducibility
2. **Check outputs** - Verify no sensitive data exposed
3. **Test error paths** - Ensure error handling works
4. **Verify cleanup** - Test data properly archived/deleted

## Common Patterns

### Error Handling

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

### DSL Building

```kotlin
val page = runBlocking {
    notion.pages.create {
        parent { databaseId(dbId) }
        properties {
            title("Name") {
                text("Demo Page")
            }
            richText("Description") {
                text("This is ") { bold = true }
                text("awesome!", TextColor.BLUE)
            }
        }
    }
}
```

### Pagination

```kotlin
import kotlinx.coroutines.flow.toList

val allPages = runBlocking {
    notion.databases.queryFlow(databaseId).toList()
}
println("Found ${allPages.size} pages")
```

## Tips for Live Demos

- **Use descriptive variable names**: Makes code easier to follow
- **Print intermediate results**: Show what's happening step by step
- **Keep token safe**: Don't display API token in output
- **Have backup IDs**: Keep test page/database IDs handy
- **Test beforehand**: Run through the notebook before presenting
- **Explain rate limits**: Mention API constraints when relevant

## Troubleshooting

### Kernel Issues

If the Kotlin kernel doesn't recognize imports:
- Restart kernel
- Check dependency specification (`@file:DependsOn`)
- Verify library is published/available in Maven Local

### API Errors

Common issues:
- **401 Unauthorized**: Check `NOTION_API_TOKEN` is set correctly
- **404 Not Found**: Verify page/database ID exists and integration has access
- **429 Rate Limited**: Add delays between requests or use built-in retry logic
- **400 Bad Request**: Check request body against API documentation

### Suspend Function Errors

If you see "Suspend function called outside coroutine":
- Wrap the call in `runBlocking { }`
- Ensure all API calls are inside coroutine contexts