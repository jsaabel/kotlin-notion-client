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

## Reference Code Structure

### `/reference` Directory Organization
```
reference/
├── python/
│   ├── notion_client.py      # Current Python implementation
│   ├── models/               # Python data models
│   └── examples/             # Python usage examples
├── kotlin/
│   ├── klibnotion/      # First Kotlin reference implementation
│   │   ├── README.md        # Notes on this implementation
│   │   └── src/             # Source code
│   ├── notion-sdk-kotlin/      # Second Kotlin reference implementation
│   │   ├── README.md        # Notes on this implementation
│   │   └── src/             # Source code
│   └── comparison.md        # Analysis of different approaches
└── notion-api/
    ├── api-reference.md     # Key Notion API concepts
    └── examples/            # API response examples
    └── NotionAPI.yml/       # OpenAPI spec for the Notion API (unoffical)
```

## Development Commands

### Build & Run
```bash
./gradlew build              # Compile and test
./gradlew assemble          # Compile only
./gradlew clean             # Clean build artifacts
./gradlew jar               # Create JAR
./gradlew run               # Run main application
```

### Testing
```bash
./gradlew test              # Run all tests
./gradlew test --tests "*DatabaseTest*"  # Run specific tests
./gradlew check             # Run all checks including tests
./gradlew jacocoTestReport  # Generate code coverage report
```

### Code Quality
```bash
./gradlew lintKotlin        # Check code style
./gradlew formatKotlin      # Auto-format code
./gradlew detekt            # Run static analysis (if configured)
```

### Dependency Management
```bash
./gradlew dependencies      # Show dependency tree
./gradlew dependencyUpdates # Check for updates
```

## Architecture

### Technology Stack
- **Language**: Kotlin 2.2.0
- **Build System**: Gradle 8.x with Kotlin DSL
- **HTTP Client**: Ktor 3.2.1 (async, coroutine-based)
- **Serialization**: kotlinx-serialization-json 1.9.0
- **Testing**: Kotest with BehaviorSpec
- **Logging**: SLF4J + Logback
- **Code Quality**: Kotlinter (ktlint wrapper)

### Package Structure
```
no.saabelit.kotlinnotionclient/
├── NotionClient.kt          # Main client class
├── config/
│   ├── NotionConfig.kt      # Configuration data class
│   └── AuthConfig.kt        # Authentication configuration
├── models/
│   ├── base/               # Base types (NotionObject, etc.)
│   ├── blocks/             # Block types
│   ├── databases/          # Database-related models
│   ├── pages/              # Page models
│   └── users/              # User models
├── api/
│   ├── BlocksApi.kt        # Blocks endpoint
│   ├── DatabasesApi.kt     # Databases endpoint
│   ├── PagesApi.kt         # Pages endpoint
│   └── UsersApi.kt         # Users endpoint
├── dsl/
│   ├── PageBuilder.kt      # DSL for building pages
│   └── BlockBuilder.kt     # DSL for building blocks
├── exceptions/
│   ├── NotionException.kt  # Base exception
│   └── RateLimitException.kt
└── utils/
    ├── RetryPolicy.kt      # Retry logic
    └── Pagination.kt       # Pagination helpers
```

### Key Design Patterns

#### 1. Client Architecture
```kotlin
// Facade pattern with delegated APIs
class NotionClient(config: NotionConfig) {
    val blocks = BlocksApi(httpClient)
    val databases = DatabasesApi(httpClient)
    val pages = PagesApi(httpClient)
}
```

#### 2. DSL for Object Creation
```kotlin
// Type-safe builders for complex objects
val page = notionPage {
    title = "My Page"
    properties {
        text("Name") { "John Doe" }
        number("Age") { 30 }
    }
    children {
        heading1 { "Welcome" }
        paragraph { "This is content" }
    }
}
```

#### 3. Error Handling
```kotlin
// Sealed class hierarchy for errors
sealed class NotionError {
    data class RateLimit(val retryAfter: Duration) : NotionError()
    data class ApiError(val code: String, val message: String) : NotionError()
    data class NetworkError(val cause: Throwable) : NotionError()
}
```

## Implementation Roadmap

### Phase 1: Foundation (Current)
- [x] Project setup with dependencies
- [ ] Basic HTTP client configuration
- [ ] Authentication mechanism
- [ ] Error handling framework
- [ ] First API call (get user)

### Phase 2: Read Operations
- [ ] Retrieve page
- [ ] Retrieve database
- [ ] Query database with filters
- [ ] Retrieve blocks
- [ ] Handle pagination

### Phase 3: Write Operations
- [ ] Create page
- [ ] Update page properties
- [ ] Create/update blocks
- [ ] Database operations

### Phase 4: Advanced Features
- [ ] Rate limiting with backoff
- [ ] Batch operations
- [ ] Webhook support
- [ ] Full DSL implementation
- [ ] Caching layer

## Testing Strategy

### Test Categories
1. **Unit Tests**: Model serialization, business logic
2. **Integration Tests**: HTTP client with mock server
3. **Contract Tests**: Verify against Notion API contracts
4. **End-to-End Tests**: Real API calls (in a separate test set)

### Test Patterns
```kotlin
// BehaviorSpec example
class DatabaseApiTest : BehaviorSpec({
    given("a database ID") {
        `when`("retrieving the database") {
            then("should return database object") {
                // test implementation
            }
        }
    }
})
```

## Common Notion API Patterns

### Pagination
All list endpoints use cursor-based pagination:
```kotlin
data class PaginatedResponse<T>(
    val results: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean
)
```

### Timestamps
All objects have standard timestamps:
```kotlin
interface Timestamped {
    val createdTime: Instant
    val lastEditedTime: Instant
}
```

### Parent References
Objects reference their parents via sealed class:
```kotlin
sealed class Parent {
    data class Database(val id: String) : Parent()
    data class Page(val id: String) : Parent()
    data class Workspace(val id: String) : Parent()
}
```

## Development Tips

### When Adding New Features
1. Study the Python implementation in `/reference/python/`
2. Review both Kotlin implementations for patterns
3. Check Notion API docs for endpoint details
4. Write tests first (TDD approach)
5. Implement incrementally with refactoring

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

## Resources

### Internal
- `/reference/` - Implementation examples
- `/src/test/resources/` - Mock API responses
- `gradle/libs.versions.toml` - Dependency versions

### External
- [Notion API Reference](https://developers.notion.com/reference)
- [Ktor Documentation](https://ktor.io/docs/)
- [Kotlin Serialization Guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md)
- [Kotest Documentation](https://kotest.io/)

## Dependency Management Notes
- We are using a `libs.toml` file for centralized dependency and version management
- This approach provides a single source of truth for all project dependencies
- Allows for easier version updates and consistency across the project