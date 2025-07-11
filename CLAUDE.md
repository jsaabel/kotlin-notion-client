# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the Notion Kotlin Client project.

## Project Overview

A Kotlin-based client library for the Notion API, designed to provide a type-safe, idiomatic Kotlin interface for 
interacting with Notion's REST API. This project aims to match and exceed the functionality of the existing Python 
implementation while leveraging Kotlin's strong typing and coroutines for better performance and developer experience.
Also an opportunity to explore more heavily AI-assisted development and learning about common architectural patterns
and best practices in Kotlin.

## Development Philosophy

### Core Principles
1. **Type Safety First**: Leverage Kotlin's type system to prevent runtime errors
2. **Coroutine-Based**: All API calls should be suspend functions for non-blocking I/O
3. **Builder Pattern**: Use DSL-style builders for complex objects (pages, blocks, databases)
4. **Fail-Fast with Clear Errors**: Validate inputs early and provide helpful error messages
5. **Incremental Development**: Build features in small, testable chunks

### Implementation Strategy
- Start with read operations (simpler, no side effects)
- Add write operations once read is stable
- Implement rate limiting and retry logic early
- Design for testability with interface-based architecture

## Development Tips

### When Adding New Features
1. Study the Python implementation in `/reference/python/`
2. Review both Kotlin implementations for patterns
3. Check Notion API docs for endpoint details
4. Write tests first (TDD approach)
5. Implement incrementally with refactoring
6. **When considering options, remember to also check the existing implementations in the subfolders of @reference/ . They shouldn't be understood as the "right" solution, but critically evaluated in terms of their features/approaches and what they might add to our own implementation.**

### Common Pitfalls
- Notion IDs can be UUIDs with or without hyphens
- Some properties are read-only from the API
- Rate limits vary by endpoint
- Archived objects need special handling
- Rich text arrays can be empty

### Debugging
- Enable Ktor logging for HTTP details
- Use `.also { println(it) }` for quick debugging
- Check `X-Notion-Request-Id` header for support
- Validate JSON payloads against API examples

### Development Workflow Reminder
- Always lint/format code using gradlew before trying to build

### Testing Commands
The project uses Kotest with tag-based test organization for efficient development workflows:

#### Standard Commands
- `./gradlew test` - Run only unit tests (fast, no API calls, ~32 tests in ~200ms)
- `./gradlew integrationTest` - Run only integration tests (requires NOTION_API_TOKEN and NOTION_PARENT_PAGE_ID)  
- `./gradlew testAll` - Run all tests (unit + integration)

#### Test Organization
- **Unit Tests**: Tagged with `@Tags("Unit")` - use mocked responses, run fast
- **Integration Tests**: Tagged with `@Tags("Integration", "RequiresApi")` - hit live Notion API
- **Slow Tests**: Additionally tagged with `@Tags("Slow")` for tests that take longer

#### Environment Variables for Integration Tests
```bash
export NOTION_API_TOKEN="secret_..."
export NOTION_PARENT_PAGE_ID="12345678-1234-1234-1234-123456789abc"
```

#### Advanced Usage
- Run specific tag combinations: `./gradlew test -Dkotest.tags.include="Unit & !Slow"`
- Exclude specific tags: `./gradlew test -Dkotest.tags.exclude="RequiresApi"`

### Version Management
- Use `libs.toml` for centralized dependency version management
- Leverage Gradle's version catalog to handle library versions consistently
- Ensure version numbers are updated in the `libs.toml` file for all project dependencies

## Testing Infrastructure

### Overview
We've built a comprehensive testing foundation that uses **official Notion API sample responses** to ensure our models accurately reflect real-world API behavior. This testing infrastructure is a core part of our development workflow and should be continuously evolved as we add new features.

### Core Components

#### TestFixtures (`src/test/kotlin/TestFixtures.kt`)
Provides easy access to official API samples organized by category:
```kotlin
// Load official sample responses
val pageData = TestFixtures.Pages.retrievePage()
val databaseData = TestFixtures.Databases.retrieveDatabase()

// Direct deserialization with type safety
val page: Page = TestFixtures.Pages.retrievePage().decode()
val database: Database = TestFixtures.Databases.retrieveDatabase().decode()
```

#### MockResponseBuilder (`src/test/kotlin/MockResponseBuilder.kt`)
DSL for creating mock HTTP clients with official data:
```kotlin
val client = mockClient {
    addPageRetrieveResponse()
    addDatabaseRetrieveResponse()
    addErrorResponse(HttpMethod.Get, "/v1/pages/", HttpStatusCode.NotFound)
}

// Or use convenient presets
val client = MockPresets.standardCrudOperations()
```

#### Official Sample Resources (`src/test/resources/api/`)
Organized collection of official Notion API responses:
- `databases/` - Database API responses
- `pages/` - Page API responses  
- `blocks/` - Block API responses
- `comments/` - Comment API responses

### Development Workflow Integration

#### When Adding New API Endpoints
1. **Copy Official Samples**: Add the official API response to `src/test/resources/api/{category}/`
2. **Extend TestFixtures**: Add helper methods for the new endpoint
3. **Add Mock Builders**: Create mock response builders for the endpoint
4. **Write Comprehensive Tests**: Test both success and error scenarios
5. **Validate Models**: Ensure our Kotlin models match the API structure exactly

#### Continuous Evolution Mindset
- **Feedback Loop**: Use test failures as immediate feedback about model accuracy
- **Iterative Improvement**: Regularly enhance the test infrastructure based on development needs
- **Pattern Consistency**: Follow established patterns (TestFixtures → MockBuilder → Tests) for all new features
- **Sample Updates**: When Notion API evolves, update samples and let tests validate our models

#### Best Practices
- **Test First**: Write tests using official samples before implementing features
- **Real Data**: Always prefer official samples over hand-crafted mock data
- **Fast Feedback**: Use mock tests (~180ms) for rapid development, integration tests for final validation
- **Model Validation**: Let serialization errors guide model improvements
- **Error Scenarios**: Test both success paths and various HTTP error conditions

#### Maintaining the Test Infrastructure
- **Regular Reviews**: Periodically review and refactor test utilities for better developer experience
- **Documentation**: Keep test patterns documented and examples up-to-date
- **Efficiency**: Optimize test performance while maintaining comprehensive coverage
- **Consistency**: Ensure all API endpoints follow the same testing patterns

### Why This Matters
This testing approach ensures our models can handle the complexity of real Notion workspaces with rich text, nested objects, and all property types. It catches API drift immediately and provides confidence that our code works with production data structures.

**Remember**: The test infrastructure is not just a validation tool—it's a core part of our development feedback loop that helps us build better, more reliable code faster.

## Commit Guidelines

### Commit Message Style
- We use "conventional commits" for our git commits.

## Reference Materials

### Notion API Documentation
- Comprehensive information and documentation about the Notion API is located under `@reference/notion-api/`
- Always refer to these files before relying on other mechanisms to retrieve information about the Notion API
- Sample responses from the official API documentation are available under `@reference/notion-api/sample_responses/` and its subfolders
- These sample responses can and should be used for mock responses and in other cases where appropriate