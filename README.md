# Kotlin Notion Client

A modern, type-safe Kotlin client for the Notion API with comprehensive DSL support and coroutine-based operations.

> **⚠️ AI-Assisted Development Notice**
> This library was developed with significant assistance from Claude Code (AI). While it includes comprehensive testing (481+ unit tests) and validation against official Notion API samples, please be aware of potential issues:
> - Documentation examples may not perfectly match implementation
> - Edge cases may exist that weren't covered in testing
> - Some API patterns may have inconsistencies
>
> **Please report any issues you encounter!** Your feedback is invaluable for improving the library.
> See the [Development Context](#development-context) section for full transparency about the development process.

## Why This Client?

- **🆕 Latest API Support** - Built for Notion API version **2025-09-03** with full support for data sources, search, and all current features
- **🛡️ Type-Safe DSLs** - Intuitive builder patterns for pages, databases, blocks, and queries with compile-time safety
- **⚡ Kotlin-First** - Leverages coroutines for non-blocking I/O, null-safety, and functional programming patterns
- **✅ Extensively Tested** - Validated against official Notion API sample responses with comprehensive test coverage

## Installation

### Maven Central

```kotlin
// Gradle (Kotlin DSL)
dependencies {
    implementation("it.saabel:kotlin-notion-client:0.1.0")
}
```

```xml
<!-- Maven -->
<dependency>
    <groupId>it.saabel</groupId>
    <artifactId>kotlin-notion-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Requirements

- Kotlin 2.2.0 or higher
- JVM target 17+

## Quick Start

```kotlin
import it.saabel.kotlinnotionclient.NotionClient

// Initialize the client (constructor - recommended)
val notion = NotionClient("your-notion-api-token")

// Alternative: factory method (also supported)
// val notion = NotionClient.create("your-notion-api-token")

// Retrieve a page
val page = notion.pages.retrieve("page-id")

// Create a page in a data source
val newPage = notion.pages.create {
    parent { dataSourceId("data-source-id") }
    properties {
        title("Name") { text("My Project") }
        select("Status") { name("In Progress") }
    }
}

// Query a data source (table)
val results = notion.dataSources.query("data-source-id") {
    filter {
        property("Status") {
            select { equals("In Progress") }
        }
    }
}
```

### Client Initialization

You can create a `NotionClient` instance using either pattern:

```kotlin
// 1. Direct constructor (idiomatic Kotlin - recommended)
val client = NotionClient("your-api-token")

// 2. Factory method (also fully supported)
val client = NotionClient.create("your-api-token")

// With custom configuration
val client = NotionClient(
    NotionConfig(
        apiToken = "your-api-token",
        logLevel = LogLevel.INFO,
        enableRateLimit = true
    )
)
```

Both patterns are fully supported - use whichever feels more natural to you.

## Understanding Databases vs. Data Sources

**Important**: The 2025-09-03 API introduced a fundamental change to how databases work:

- **Database** = Container that holds one or more data sources
- **Data Source** = The actual table with properties and rows (pages)

Most operations you'd expect to do on a "database" (like querying, adding pages) actually work on **data sources**:

```kotlin
// ❌ In older APIs: notion.databases.query("database-id")
// ✅ In 2025-09-03: notion.dataSources.query("data-source-id")
```

The `DatabasesApi` is for container-level operations (create database, update title/icon/cover). The `DataSourcesApi` is for data operations (query, create data source, update schema).

See [docs/databases.md](docs/databases.md) and [docs/data-sources.md](docs/data-sources.md) for details.

## API Coverage

| API Category | Status | Documentation |
|--------------|--------|---------------|
| **Pages** | ✅ Complete | [docs/pages.md](docs/pages.md) |
| **Databases** | ✅ Complete | [docs/databases.md](docs/databases.md) |
| **Blocks** | ✅ Complete | [docs/blocks.md](docs/blocks.md) |
| **Data Sources** | ✅ Complete | [docs/data-sources.md](docs/data-sources.md) |
| **Search** | ✅ Complete | [docs/search.md](docs/search.md) |
| **Users** | ✅ Complete | [docs/users.md](docs/users.md) |
| **Comments** | ✅ Complete | [docs/comments.md](docs/comments.md) |
| **File Uploads** | ✅ Complete | Integrated with blocks/pages |

### Feature Highlights

- **All CRUD operations** for pages, databases, and blocks
- **Advanced query DSL** with complex filters, sorting, and pagination
- **Rich text DSL** for formatted content with mentions, equations, and links
- **30+ block types** including tables, callouts, code blocks, and embeds
- **Property types** - All database property types supported (formulas, relations, rollups, etc.)
- **File operations** - Upload and manage files/images
- **Comprehensive error handling** with detailed error types

## Documentation

- **[Quick Start Guide](QUICKSTART.md)** - Get up and running in 5 minutes
- **[API Documentation](docs/)** - Detailed guides for each API category
- **[Rich Text DSL](docs/rich-text-dsl.md)** - Working with formatted text
- **[Error Handling](docs/error-handling.md)** - Understanding and handling errors
- **[Testing](docs/testing.md)** - Testing your Notion integrations

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/kotlin-notion-client.git
cd kotlin-notion-client

# Build the project
./gradlew build

# Run all tests (unit tests run, integration tests skip without env vars)
./gradlew test

# Run integration tests (requires environment variables)
export NOTION_RUN_INTEGRATION_TESTS=true
export NOTION_API_TOKEN="secret_..."
export NOTION_TEST_PAGE_ID="page-id"
./gradlew test

# Run specific integration tests (recommended)
./gradlew test --tests "*SearchIntegrationTest"
```

**Notes**:
- Integration tests are controlled via environment variables. Without `NOTION_RUN_INTEGRATION_TESTS=true`, they are automatically skipped.
- **⚠️ Avoid running all integration tests at once** - they perform many real API operations on your workspace. Run specific test classes instead.

## Development Context

This project was developed using **Claude Code** (Anthropic's CLI for Claude) as an exploration of AI-assisted software development. The goal was to understand how human expertise and AI capabilities can collaborate on production-quality code.

**Key aspects:**
- Iterative, test-driven development with AI assistance
- Extensive use of official Notion API samples for validation
- Transparent development process documented in [journal/](journal/)

The development journals are intentionally kept in the repository for transparency and educational value for others exploring LLM-assisted development workflows.

## Project Status

This library covers virtually all aspects of the Notion API (2025-09-03 version). It has been validated against official API samples and includes both unit tests and integration tests. However, it has not yet been used extensively in production environments.

**Before using in production:**
- Review the test coverage for your specific use cases
- Test thoroughly with your Notion workspace
- Be aware that the API version support is fixed to 2025-09-03

## Contributing

Contributions are welcome! This project is in pre-release, so now is a great time to help shape its direction.

**Areas where contributions are especially valued:**
- Real-world usage feedback and bug reports
- Additional test coverage
- Documentation improvements
- Performance optimizations

## License

This project is licensed under the MIT License.

## Acknowledgments

- **Notion** for their comprehensive API and excellent documentation
- **Anthropic** for Claude and Claude Code
- **JetBrains** for Kotlin and the fantastic language ecosystem
- The **Kotlin community** for patterns and best practices

---

*This is not an official Notion product. Notion is a trademark of Notion Labs, Inc.*
