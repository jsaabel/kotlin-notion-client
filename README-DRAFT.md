# Kotlin Notion Client - README DRAFT

> **⚠️ DRAFT DOCUMENT**: This README is a work in progress. Code examples and API syntax shown here need verification against the actual implementation.

A type-safe, coroutine-based Kotlin client for the Notion API

## Overview

The Kotlin Notion Client provides a modern, idiomatic Kotlin interface for interacting with Notion's API. Built from the ground up with Kotlin's strengths in mind, it offers a delightful developer experience through type-safe DSLs, coroutine support, and comprehensive error handling.

### Key Features

- 🚀 **Fully Coroutine-Based** - Non-blocking I/O operations with suspend functions
- 🛡️ **Type-Safe DSLs** - Build pages, databases, and queries with compile-time safety
- 📝 **Rich Text DSL** - Intuitive syntax for complex text formatting
- 🔄 **Smart Rate Limiting** - Automatic retry logic with exponential backoff
- ✅ **Battle-Tested** - Comprehensive test suite using official Notion API samples
- 🎯 **Kotlin-First Design** - Leverages Kotlin's language features for cleaner code

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.username:kotlin-notion-client:0.1.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.github.username:kotlin-notion-client:0.1.0'
}
```

> **Note**: This library requires Kotlin 1.9.0 or higher and is built for JVM target 17+

## Quick Start

### Initialize the Client

```kotlin
import notion.api.NotionClient

val notion = NotionClient(token = "your-api-token")
```

### Retrieve a Page

```kotlin
val page = notion.pages.retrieve("page-id")
println(page.properties["Title"])
```

### Create a Page with DSL

```kotlin
val newPage = notion.pages.create {
    parent {
        databaseId("database-id")
    }
    properties {
        title("Task Name") {
            text("Review pull request")
        }
        select("Status") {
            name("In Progress")
        }
        date("Due Date") {
            start("2024-01-20")
        }
    }
}
```

### Query a Database

```kotlin
val results = notion.databases.query("database-id") {
    filter {
        and {
            property("Status") {
                select { equals("In Progress") }
            }
            property("Due Date") {
                date { onOrBefore("2024-01-31") }
            }
        }
    }
    sorts {
        property("Due Date") {
            direction = SortDirection.ASCENDING
        }
    }
}
```

## Core Features

### Page Operations

The client provides comprehensive support for Notion pages:

```kotlin
// Retrieve with property details
val page = notion.pages.retrieve(pageId)

// Create with rich content
val page = notion.pages.create {
    parent { databaseId("db-id") }
    
    properties {
        title("Name") {
            text("Project Plan")
        }
        
        richText("Description") {
            text("This is a ") {
                bold = true
            }
            text("comprehensive", TextColor.BLUE) {
                italic = true
            }
            text(" project plan")
        }
        
        multiSelect("Tags") {
            names("kotlin", "api", "notion")
        }
    }
    
    children {
        heading1 {
            richText {
                text("Project Overview")
            }
        }
        
        paragraph {
            richText {
                text("This project aims to deliver...")
            }
        }
    }
}

// Update properties
val updated = notion.pages.update(pageId) {
    properties {
        checkbox("Completed") {
            checked = true
        }
    }
}

// Archive pages
notion.pages.update(pageId) {
    archived = true
}
```

### Database Operations

Work with Notion databases using type-safe builders:

```kotlin
// Create a database
val database = notion.databases.create {
    parent { pageId("parent-page-id") }
    
    title {
        text("Project Tasks")
    }
    
    properties {
        title("Task")
        
        select("Priority") {
            option("High", Color.RED)
            option("Medium", Color.YELLOW)
            option("Low", Color.GREEN)
        }
        
        people("Assignee")
        
        formula("Days Until Due") {
            expression = """dateBetween(prop("Due Date"), now(), "days")"""
        }
    }
}

// Query with complex filters
val tasks = notion.databases.query(databaseId) {
    filter {
        or {
            property("Priority") {
                select { equals("High") }
            }
            property("Days Until Due") {
                formula {
                    number { lessThan(3) }
                }
            }
        }
    }
}
```

### Block Operations

Create and manipulate content blocks:

```kotlin
// Append blocks to a page
notion.blocks.append(pageId) {
    heading2 {
        richText {
            text("Section Title")
        }
    }
    
    bulletedListItem {
        richText {
            text("First point")
        }
    }
    
    code {
        richText {
            text("fun main() {\n    println(\"Hello, Notion!\")\n}")
        }
        language = "kotlin"
    }
    
    table(width = 3) {
        tableRow {
            cells("Header 1", "Header 2", "Header 3")
        }
        tableRow {
            cells("Data 1", "Data 2", "Data 3")
        }
    }
}

// Update existing blocks
notion.blocks.update(blockId) {
    paragraph {
        richText {
            text("Updated content")
        }
    }
}

// Delete blocks
notion.blocks.delete(blockId)
```

### Rich Text DSL

Create formatted text with an intuitive DSL:

```kotlin
richText {
    text("Important: ") {
        bold = true
        color = TextColor.RED
    }
    
    text("Visit our ") 
    
    link("documentation", "https://docs.example.com") {
        italic = true
        underline = true
    }
    
    text(" for more details. ")
    
    mention {
        user("user-id")
    }
    
    text(" created this on ")
    
    mention {
        date {
            start = "2024-01-15"
        }
    }
    
    equation("E = mc^2")
}
```

## Advanced Features

### Query DSL

Build complex database queries with type safety:

```kotlin
notion.databases.query(databaseId) {
    filter {
        and {
            property("Sprint") {
                relation { contains("current-sprint-id") }
            }
            
            or {
                property("Status") {
                    status { equals("In Progress") }
                }
                property("Status") {
                    status { equals("Review") }
                }
            }
            
            property("Assignee") {
                people { isNotEmpty() }
            }
        }
    }
    
    sorts {
        property("Priority") {
            direction = SortDirection.DESCENDING
        }
        timestamp("last_edited_time") {
            direction = SortDirection.DESCENDING
        }
    }
    
    pageSize = 50
}
```

### Error Handling

The client provides detailed error information:

```kotlin
try {
    val page = notion.pages.retrieve(pageId)
} catch (e: NotionError) {
    when (e) {
        is NotionError.ObjectNotFound -> println("Page not found: ${e.message}")
        is NotionError.Unauthorized -> println("Invalid API token")
        is NotionError.RateLimited -> println("Rate limited, retry after: ${e.retryAfter}")
        is NotionError.InvalidRequest -> println("Invalid request: ${e.message}")
        else -> println("Unexpected error: ${e.message}")
    }
}
```

### Testing Support

Built-in utilities for testing your Notion integrations:

```kotlin
class MyNotionServiceTest : FunSpec({
    
    test("should create task page") {
        val mockClient = mockClient {
            addPageCreateResponse {
                properties {
                    title("Task Name") {
                        text("Test Task")
                    }
                }
            }
        }
        
        val service = MyNotionService(mockClient)
        val page = service.createTask("Test Task")
        
        page.properties["Task Name"].shouldNotBeNull()
    }
})
```

## API Coverage

| Feature | Status | Notes |
|---------|--------|-------|
| **Authentication** | ✅ | Bearer token |
| **Pages** | | |
| └ Retrieve | ✅ | Full support |
| └ Create | ✅ | With children blocks |
| └ Update | ✅ | Properties & archive |
| **Databases** | | |
| └ Retrieve | ✅ | Full support |
| └ Create | ✅ | All property types |
| └ Update | ✅ | Schema changes |
| └ Query | ✅ | Complex filters |
| **Blocks** | | |
| └ Retrieve | ✅ | With children |
| └ Create | ✅ | 30+ block types |
| └ Update | ✅ | Content changes |
| └ Delete | ✅ | Full support |
| └ Append | ✅ | Batch operations |
| **Users** | ✅ | Retrieve & list |
| **Comments** | 🚧 | Coming soon |
| **Search** | ❌ | Planned |

## Examples

Explore more examples in our [Jupyter notebook showcase](notebooks/KotlinNotionClientShowcase.ipynb) demonstrating:
- Complex page hierarchies
- Database schemas and queries
- Rich text formatting
- Batch operations
- Error handling patterns

## Building from Source

### Prerequisites
- JDK 17 or higher
- Gradle 8.0 or higher

### Build Commands
```bash
# Clone the repository
git clone https://github.com/username/kotlin-notion-client.git
cd kotlin-notion-client

# Build the project
./gradlew build

# Run tests (unit tests only)
./gradlew test

# Run all tests (requires NOTION_API_TOKEN)
./gradlew testAll
```

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details on:
- Code style and conventions
- Testing requirements
- Pull request process
- Development setup

## Built with Claude

This project was developed in collaboration with Claude (Anthropic's AI assistant) as a real-world exploration of AI-assisted software development. It demonstrates how human expertise and AI capabilities can work together to create production-quality code.

The development process showcased:
- Iterative design and implementation
- Test-driven development with AI assistance
- Code review and refactoring workflows
- Documentation generation

Learn more about the development journey in our [blog post](#) (coming soon).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Notion for their excellent API and documentation
- The Kotlin community for inspiration and best practices
- Claude (Anthropic) for development assistance
- Contributors and early adopters

---

*Note: This is not an official Notion product. Notion is a trademark of Notion Labs, Inc.*