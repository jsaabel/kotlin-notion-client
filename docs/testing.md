# Testing Guide

## Overview

This guide explains how the Kotlin Notion Client organizes its tests and how to run them. The library has comprehensive test coverage with fast unit tests and real API integration tests.

## Test Organization

### Unit Tests (`src/test/kotlin/unit/`)
- **Fast**: ~481 tests run in ~200ms
- **No API calls**: Use mock responses from official Notion API samples
- **Tagged**: `@Tags("Unit")`

### Integration Tests (`src/test/kotlin/integration/`)
- **Real API**: Make actual requests to Notion
- **Require setup**: Need environment variables
- **Tagged**: `@Tags("Integration", "RequiresApi")` or with `"Slow"` for long-running tests
- **Protected**: Won't run without `NOTION_RUN_INTEGRATION_TESTS=true`

### Example Tests (`src/test/kotlin/examples/`)
- **Documentation examples**: Validate code in docs actually works
- **Also integration tests**: Tagged with `@Tags("Integration", "RequiresApi", "Examples")`

## The Master Switch

All integration tests check this environment variable:

```kotlin
// From src/test/kotlin/integration/Util.kt
fun integrationTestEnvVarsAreSet(...): Boolean {
    if (System.getenv("NOTION_RUN_INTEGRATION_TESTS")?.lowercase() != "true") {
        return false  // Integration tests skip
    }
    // ... check other required env vars
}
```

**This is your safety**: Integration tests never run without explicit opt-in via environment variable.

## Running Tests

### Fast development (recommended)

```bash
# Run only unit tests - fast, no API calls
./gradlew test

# All ~481 unit tests pass in ~200ms
```

### Running specific integration tests

```bash
# Set up once per session
export NOTION_RUN_INTEGRATION_TESTS=true
export NOTION_API_TOKEN="secret_your_token"
export NOTION_TEST_PAGE_ID="your-page-id"

# Run one integration test
./gradlew test --tests "*PagesIntegrationTest"
```

### ⚠️ Running ALL integration tests (not recommended)

```bash
# WARNING: Creates/modifies/deletes real data, takes 5-10+ minutes
export NOTION_RUN_INTEGRATION_TESTS=true
./gradlew testAll
```

## Environment Variables

| Variable | Purpose | Required For |
|----------|---------|--------------|
| `NOTION_RUN_INTEGRATION_TESTS` | Must be `"true"` to enable integration tests | Integration tests |
| `NOTION_API_TOKEN` | Your integration secret token | Integration tests |
| `NOTION_TEST_PAGE_ID` | A page where integration has permissions | Most integration tests |
| `NOTION_CLEANUP_AFTER_TEST` | Set to `"false"` to keep test data | Optional (default: true) |

## Test Infrastructure

### Official API Samples

**File**: `src/test/kotlin/unit/util/TestFixtures.kt`

Unit tests use real responses from Notion's API documentation:

```kotlin
// Load official sample responses
val pageJson = TestFixtures.Pages.retrievePage()
val page: Page = TestFixtures.Pages.retrievePage().decode()
```

### Mock Client Builder

**File**: `src/test/kotlin/unit/util/MockResponseBuilder.kt`

Create mock HTTP clients for unit tests:

```kotlin
val mockClient = mockClient {
    addPageRetrieveResponse()  // Uses official API sample
    addDatabaseCreateResponse()
}

val notion = NotionClient.createWithClient(mockClient, NotionConfig(apiToken = "test"))
```

Available mock responses: `addPageRetrieveResponse()`, `addPageCreateResponse()`, `addDatabaseRetrieveResponse()`, `addBlockRetrieveResponse()`, `addSearchResponse()`, and more - see `MockResponseBuilder.kt` for complete list.

## Testing Your Own Code

### With mock responses (unit test)

```kotlin
import io.kotest.core.spec.style.FunSpec
import unit.util.mockClient

class MyServiceTest : FunSpec({
    test("should create page") {
        val mockClient = mockClient {
            addPageCreateResponse()
        }

        val notion = NotionClient.createWithClient(mockClient, NotionConfig(apiToken = "test"))
        val service = MyService(notion)

        val page = service.createTaskPage("Buy groceries")
        page.id shouldNotBe null
    }
})
```

### With real API (integration test)

```kotlin
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest

@Tags("Integration", "RequiresApi")
class MyServiceIntegrationTest : StringSpec({

    if (!integrationTestEnvVarsAreSet()) {
        "!(Skipped)" {
            println("⏭️ Set NOTION_RUN_INTEGRATION_TESTS=true to run")
        }
    } else {
        val notion = NotionClient.create(System.getenv("NOTION_API_TOKEN"))

        "should create and retrieve task" {
            val task = MyService(notion).createTask("Test task")

            task shouldNotBe null

            if (shouldCleanupAfterTest()) {
                notion.pages.archive(task.id)
            }
        }

        afterSpec {
            notion.close()
        }
    }
})
```

## Important Warnings

### ❌ DON'T run all integration tests at once

Integration tests create/modify/delete real content in your workspace. Run them individually during development.

### ❌ DON'T test against production workspaces

Always use a dedicated test workspace or test page. Set `NOTION_TEST_PAGE_ID` to a page specifically for testing.

### ❌ DON'T commit secrets

Never hardcode API tokens in test files. Always use environment variables.

## Tag System

Tests are organized with Kotest tags:

| Tag | Meaning |
|-----|---------|
| `Unit` | Fast mock-based tests |
| `Integration` + `RequiresApi` | Tests that use real Notion API |
| `Slow` | Tests that take >30 seconds (also tagged Integration) |
| `Examples` | Documentation example tests |

Run tests by tag:
```bash
# Only unit tests
./gradlew test -Dkotest.tags.include="Unit"

# Exclude slow tests
./gradlew test -Dkotest.tags.exclude="Slow"
```

## Related Topics

- **[Error Handling](error-handling.md)** - Testing error scenarios
- **[CLAUDE.md](/CLAUDE.md)** - Development workflow and test commands
