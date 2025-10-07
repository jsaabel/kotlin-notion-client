# Testing Your Notion Integrations

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

## Overview

This guide covers strategies for testing code that uses the Kotlin Notion Client, including how the library's own tests are structured.

## How This Library Handles Tests

This library uses a unified test approach where integration tests are controlled by environment variables:

```kotlin
// Integration tests check env vars and skip if not set
if (!integrationTestEnvVarsAreSet()) {
    "!(Skipped) Integration test" {
        println("⏭️ Skipping - missing environment variables")
    }
} else {
    // actual test code
}
```

### Running Tests

```bash
# Run unit tests only (integration tests automatically skip)
./gradlew test

# Run with integration tests enabled
export NOTION_RUN_INTEGRATION_TESTS=true
export NOTION_API_TOKEN="secret_..."
export NOTION_TEST_PAGE_ID="page-id"

# Run specific integration test (recommended)
./gradlew test --tests "*SearchIntegrationTest"

# ⚠️ Avoid running ALL integration tests at once
# This will perform many real operations on your workspace
```

### Environment Variables

- **`NOTION_RUN_INTEGRATION_TESTS`**: Must be `"true"` to enable integration tests
- **`NOTION_API_TOKEN`**: Your Notion integration API token
- **`NOTION_TEST_PAGE_ID`**: A test page ID where the integration has permissions
- **`NOTION_CLEANUP_AFTER_TEST`**: Set to `"false"` to keep test data (default: cleanup enabled)

## Testing Your Own Code

### Unit Testing with Mocks

_TODO: Add mocking examples showing how to test code that uses NotionClient_

```kotlin
// Example using mock client (to be added)
class MyNotionServiceTest : FunSpec({
    test("should create task page") {
        // TODO: Add complete example
    }
})
```

### Integration Testing Pattern

You can use the same pattern this library uses:

```kotlin
import io.kotest.core.spec.style.StringSpec
import integration.integrationTestEnvVarsAreSet

class MyIntegrationTest : StringSpec({
    if (!integrationTestEnvVarsAreSet()) {
        "!(Skipped) My integration test" {
            println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true")
        }
    } else {
        val client = NotionClient.create(System.getenv("NOTION_API_TOKEN"))

        "my test case" {
            // Your test code here
        }
    }
})
```

### Test Data Management

_TODO: Add guidance on managing test data in Notion workspace_

## Common Patterns

_TODO: Add tips, gotchas, best practices_

### Testing Pagination

_TODO: Add pagination testing examples_

### Testing Error Scenarios

_TODO: Add error scenario testing examples_

## Related Topics

- [Error Handling](error-handling.md) - Understanding errors to test for
