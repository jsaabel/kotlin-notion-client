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

- **🆕 Latest API Support** - Built for Notion API version **2025-09-03** with full support for data sources, search, and all current features (currently the only Kotlin client supporting this API version)
- **🛡️ Type-Safe DSLs** - Intuitive builder patterns for pages, databases, blocks, and queries with compile-time safety
- **⚡ Kotlin-First** - Leverages coroutines for non-blocking I/O, null-safety, and functional programming patterns
- **✅ Extensively Tested** - Validated against official Notion API sample responses with comprehensive test coverage

## Installation

### Maven Central

```kotlin
// Gradle (Kotlin DSL)
dependencies {
    implementation("it.saabel:kotlin-notion-client:0.3.0")
}
```

```xml
<!-- Maven -->
<dependency>
    <groupId>it.saabel</groupId>
    <artifactId>kotlin-notion-client</artifactId>
    <version>0.3.0</version>
</dependency>
```

### Requirements

- Kotlin 2.3.0 or higher
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
| **File Uploads** | ✅ Complete | [docs/file-uploads.md](docs/file-uploads.md) |

### Feature Highlights

- **All CRUD operations** for pages, databases, and blocks
- **Page management** - Move pages between parents, lock/unlock pages, control page position
- **Templates API** - List data source templates and create pages from templates
- **Advanced query DSL** with complex filters, sorting, pagination, and timestamp filters
- **Rich text DSL** for formatted content with mentions, equations, and links
- **30+ block types** including tables, callouts, code blocks, and embeds
- **Property types** - Full support for the most important data source property types, with more coming up
- **Kotlin datetime types** - Native support for `kotlinx-datetime` types (`LocalDate`, `LocalDateTime`, `Instant`) with explicit timezone handling
- **File operations** - Upload and manage files/images
- **Comprehensive error handling** with detailed error types

## Documentation

- **[Quick Start Guide](QUICKSTART.md)** - Get up and running in 5 minutes
- **[Kotlin Notebooks](notebooks/)** - Interactive Jupyter notebooks with 55+ examples (recommended for learning)
- **[API Documentation](docs/)** - Detailed guides for each API category
- **[Rich Text DSL](docs/rich-text-dsl.md)** - Working with formatted text
- **[Error Handling](docs/error-handling.md)** - Understanding and handling errors
- **[Testing](docs/testing.md)** - Testing your Notion integrations

### Learning Resources

The **[Kotlin Notebooks](notebooks/)** are the best way to learn the library:
1. [Getting Started](notebooks/01-getting-started.ipynb) - Authentication and basic operations
2. [Reading Databases](notebooks/02-reading-databases.ipynb) - Querying with filters and sorting
3. [Creating Pages](notebooks/03-creating-pages.ipynb) - Pages, properties, icons, and covers
4. [Working with Blocks](notebooks/04-working-with-blocks.ipynb) - All block types and operations
5. [Rich Text DSL](notebooks/05-rich-text-dsl.ipynb) - Formatting, colors, links, dates, equations
6. [Advanced Queries](notebooks/06-advanced-queries.ipynb) - Complex filtering, AND/OR logic, pagination
7. [File Uploads](notebooks/07-file-uploads.ipynb) - Uploading files, external imports, media blocks

All notebooks use live Notion API and can be run in IntelliJ IDEA or Jupyter.

> **Note:** Notebooks currently use v0.2.0 due to a binary incompatibility between the IntelliJ Kotlin
> Notebook kernel's bundled kotlinx-serialization and Ktor 3.4.0. This is a kernel-level limitation;
> the library works correctly in all other environments.

## Building from Source

```bash
# Clone the repository
git clone https://github.com/jsaabel/kotlin-notion-client.git
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
- **Date/time properties with timezones**: While the library provides comprehensive support for `kotlinx-datetime` types with explicit timezone handling (including timezone-aware conversions via `toLocalDateTime(timeZone)`), this area may benefit from additional real-world validation, particularly around timezone edge cases and complex datetime scenarios

## Contributing

Contributions are welcome!

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
- **Previous Kotlin implementations** ([klibnotion](https://github.com/BoD/klibnotion) and [notion-sdk-kotlin](https://github.com/notionsdk/notion-sdk-kotlin)) for pioneering Kotlin-based Notion API clients and serving as valuable references during development

---

*This is not an official Notion product. Notion is a trademark of Notion Labs, Inc.*
