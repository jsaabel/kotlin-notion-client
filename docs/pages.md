# Pages API

> **📝 Example Validation**: ✅ All examples verified - validated against live Notion API (see `src/test/kotlin/examples/PagesExamples.kt`)

## Overview

**Pages** are the fundamental content units in Notion. They can exist in three contexts:

1. **Standalone pages** - Top-level pages in your workspace
2. **Child pages** - Pages nested within other pages
3. **Database rows** - Pages that live as entries in data sources (tables)

Every page has properties that can store structured data, and can contain blocks for rich content.

**Official Documentation**: [Notion Pages API](https://developers.notion.com/reference/page)

## Available Operations

```kotlin
// Retrieve a page
suspend fun retrieve(pageId: String): Page

// Create a page (DSL)
suspend fun create(block: CreatePageRequestBuilder.() -> Unit): Page

// Update a page (DSL)
suspend fun update(pageId: String, block: UpdatePageRequestBuilder.() -> Unit): Page

// Archive a page
suspend fun archive(pageId: String): Page

// Move a page to a different parent (v0.3.0+)
suspend fun move(pageId: String, parent: MovePageParent): Page
suspend fun moveToPage(pageId: String, parentPageId: String): Page
suspend fun moveToDataSource(pageId: String, dataSourceId: String): Page

// Retrieve property items (for paginated properties like relations)
suspend fun retrievePropertyItems(pageId: String, propertyId: String): List<PropertyItem>
```

## Examples

### Retrieve a Page

```kotlin
val page = notion.pages.retrieve("page-id")

// Access page metadata
println("Created: ${page.createdTime}")
println("Last edited: ${page.lastEditedTime}")
println("Archived: ${page.archived}")

// Access properties
val title = page.properties["Name"] as? PageProperty.Title
println("Title: ${title?.plainText}")
```

### Create a Page in a Data Source (Database Row)

**Important**: In the 2025-09-03 API, pages in tables are created with a `dataSourceId` parent (not `databaseId`):

```kotlin
val page = notion.pages.create {
    // Specify the data source as parent
    parent.dataSource("data-source-id")

    // Set property values
    properties {
        title("Task Name", "Complete documentation")
        select("Status", "In Progress")
        number("Priority", 8.0)
        date("Due Date", "2025-10-15")
        checkbox("Is Complete", false)
        people("Assignee", "user-id-1", "user-id-2")
        url("Link", "https://example.com")
        email("Contact", "user@example.com")
        phoneNumber("Phone", "+1-555-0123")
    }

    // Optional: Set icon and cover
    icon.emoji("📝")
    cover.external("https://example.com/cover.jpg")
}

println("Created page: ${page.id}")
```

### Create a Child Page

```kotlin
val childPage = notion.pages.create {
    // Specify another page as parent
    parent.page("parent-page-id")

    // For child pages, use title() directly (not in properties block)
    title("Meeting Notes - Oct 2025")

    // Optional: Add content blocks immediately
    content {
        heading1("Key Takeaways")

        bullet("Decision made on architecture")
        bullet("Timeline set for Q1 2026")

        paragraph("Next meeting scheduled for next month.")
    }
}
```

### Create a Page with Rich Content

```kotlin
val page = notion.pages.create {
    parent.page("parent-page-id")

    // For child pages, use title() directly
    title("Project Plan")

    // Add structured content
    content {
        heading1("Project Overview")

        paragraph {
            text("This project aims to ")
            bold("revolutionize")
            text(" how we handle ")
            italic("data processing")
            text(".")
        }

        heading2("Timeline")

        number("Phase 1: Research")
        number("Phase 2: Implementation")
        number("Phase 3: Testing")

        divider()

        callout("⚠️") {
            text("Note: Timeline subject to change based on resource availability.")
        }
    }
}
```

### Update Page Properties

```kotlin
val updated = notion.pages.update("page-id") {
    properties {
        // Update existing properties
        select("Status", "Completed")
        checkbox("Is Complete", true)
        number("Priority", 10.0)

        // Add/update a URL
        url("Documentation", "https://docs.example.com")
    }
}
```

### Update Page Icon and Cover

```kotlin
val updated = notion.pages.update("page-id") {
    // Update icon
    icon.emoji("✅")

    // Update cover
    cover.external("https://example.com/new-cover.jpg")
}
```

### Archive a Page

```kotlin
// Archive using the dedicated method
val archived = notion.pages.archive("page-id")

// Or use update
val archived = notion.pages.update("page-id") {
    archive()
}

println("Page archived: ${archived.archived}")
```

**Note**: Notion doesn't support true deletion. Archived pages are hidden from the UI but remain accessible via the API.

### Restore an Archived Page

```kotlin
val restored = notion.pages.update("page-id") {
    archive(false)  // Set archived to false
}
```

### Retrieve Paginated Property Items

Some properties like relations can have many items that require pagination:

```kotlin
// Get all items from a relation property
val relationItems = notion.pages.retrievePropertyItems(
    pageId = "page-id",
    propertyId = "property-id"  // ID of the relation property
)

relationItems.forEach { item ->
    when (item) {
        is PropertyItem.Relation -> println("Related page: ${item.relation.id}")
        else -> println("Other property item: $item")
    }
}
```

**Note**: This method automatically handles pagination and returns ALL items.

## Understanding Page Parents

Pages can have different parent types. The parent determines where the page lives:

### Data Source Parent (Database Row)

```kotlin
notion.pages.create {
    parent.dataSource("data-source-id")
    properties {
        // Properties must match the data source schema
        title("Name", "Task name")
        select("Status", "To Do")
    }
}
```

**Key point**: Use `dataSourceId` in 2025-09-03 API (not `databaseId` from older versions).

### Page Parent (Child Page)

```kotlin
notion.pages.create {
    parent.page("parent-page-id")
    properties {
        // Usually just a title for child pages
        title("Page Title", "Subpage name")
    }
}
```

### Workspace Parent

```kotlin
notion.pages.create {
    parent.workspace()  // Creates a top-level workspace page
    properties {
        title("Page Title", "Top-level page")
    }
}

// Note: This requires workspace admin permissions
```

## Working with Page Properties

### Accessing Properties from Retrieved Pages

The library provides three patterns for accessing page properties:

#### Pattern 1: Extension Functions

The cleanest and most convenient approach - use extension functions for direct access to property values:

```kotlin
val page = notion.pages.retrieve("page-id")

// Simple value access
val title = page.getTitleAsPlainText("Name") ?: "Untitled"
val status = page.getSelectPropertyName("Status") ?: "No status"
val priority = page.getNumberProperty("Priority") ?: 0.0
val dueDate = page.getDateProperty("Due Date")?.start
val assignees = page.getPeopleProperty("Assignee")

// Rich text access (preserves formatting)
val descriptionRichText = page.getRichTextProperty("Description")
val descriptionPlainText = page.getRichTextAsPlainText("Description")

// Other convenience methods
val checkboxValue = page.getCheckboxProperty("Is Complete")
val url = page.getUrlProperty("Link")
val email = page.getEmailProperty("Contact")
val phoneNumber = page.getPhoneNumberProperty("Phone")
val multiSelectNames = page.getMultiSelectPropertyNames("Tags")
val relatedPages = page.getRelationProperty("Related Items")
```

**Use when:** You want clean, concise code and only need the property value (recommended for most cases).

#### Pattern 2: Type-Safe Casting (Full Control)

Cast to the specific property type for explicit control:

```kotlin
val page = notion.pages.retrieve("page-id")

// Access specific property types
val titleProp = page.properties["Name"] as? PageProperty.Title
val title = titleProp?.plainText ?: "Untitled"

val selectProp = page.properties["Status"] as? PageProperty.Select
val status = selectProp?.select?.name ?: "No status"

val numberProp = page.properties["Priority"] as? PageProperty.Number
val priority = numberProp?.number ?: 0.0

val dateProp = page.properties["Due Date"] as? PageProperty.Date
val dueDate = dateProp?.date?.start  // ISO date string

val peopleProp = page.properties["Assignee"] as? PageProperty.People
val assignees = peopleProp?.people ?: emptyList()
```

**Use when:** You need full control over the property object or want to access multiple fields from the same property.

#### Pattern 3: Generic Plain Text Extractor

For cases where you just need a string representation of any property:

```kotlin
val page = notion.pages.retrieve("page-id")

// Works with any property type
val title = page.getPlainTextForProperty("Name")           // "My Page"
val status = page.getPlainTextForProperty("Status")        // "In Progress"
val priority = page.getPlainTextForProperty("Priority")    // "8.0"
val tags = page.getPlainTextForProperty("Tags")            // "urgent, bug"
val relations = page.getPlainTextForProperty("Related")    // "3 relation(s)"
```

**Use when:** Writing tests, debugging, or when you need a quick string representation without caring about the specific type.

### Property Type Reference

Common property types you can set when creating/updating pages:

| Property Type | Method | Example |
|--------------|--------|---------|
| Title | `title(name, text)` | `title("Name", "Task name")` |
| Rich Text | `richText(name, text)` | `richText("Description", "Details here")` |
| Number | `number(name, value)` | `number("Priority", 5.0)` |
| Select | `select(name, option)` | `select("Status", "In Progress")` |
| Multi-select | `multiSelect(name, options)` | `multiSelect("Tags", "urgent", "bug")` |
| Date | `date(name, dateString)` | `date("Due", "2025-10-15")` |
| People | `people(name, userIds)` | `people("Assignee", "user-1", "user-2")` |
| Checkbox | `checkbox(name, checked)` | `checkbox("Done", true)` |
| URL | `url(name, urlString)` | `url("Link", "https://...")` |
| Email | `email(name, emailString)` | `email("Contact", "user@...")` |
| Phone | `phoneNumber(name, phoneString)` | `phoneNumber("Phone", "+1-555-0123")` |
| Files | `files(name, ...)` | (See file upload documentation) |
| Relation | `relation(name, pageIds)` | `relation("Related", "page-1", "page-2")` |
| Place (v0.2.0+) | Read location data | Access with `getPlaceProperty()` |
| Unique ID | Read auto-incrementing ID | Access with `getUniqueIdProperty()` |

**Read-only properties** (cannot be set via create/update):
- Formula
- Rollup
- Created time
- Last edited time
- Created by
- Last edited by
- Place (read-only in API)
- Unique ID (auto-generated)

### Move a Page (v0.3.0+)

Move a page to a new parent (another page or a data source):

```kotlin
// Move to another page using convenience method
notion.pages.moveToPage("page-id", "new-parent-page-id")

// Move to a data source (make it a database row)
notion.pages.moveToDataSource("page-id", "data-source-id")

// Or use the generic method with explicit parent type
import it.saabel.kotlinnotionclient.models.pages.MovePageParent

notion.pages.move("page-id", MovePageParent.PageParent("new-parent-page-id"))
notion.pages.move("page-id", MovePageParent.DataSourceParent("data-source-id"))
```

### Lock and Unlock Pages (v0.3.0+)

Prevent or allow editing of a page. The `isLocked` field is also available on retrieved pages (`page.isLocked`):

```kotlin
// Lock a page
notion.pages.update("page-id") {
    lock()
}

// Unlock a page
notion.pages.update("page-id") {
    unlock()
}

// Lock with explicit boolean
notion.pages.update("page-id") {
    lock(true)   // Same as lock()
    lock(false)  // Same as unlock()
}

// Check if a page is locked
val page = notion.pages.retrieve("page-id")
println("Locked: ${page.isLocked}")
```

### Erase Page Content (v0.3.0+)

Clear all content (blocks) from a page while keeping its properties:

```kotlin
notion.pages.update("page-id") {
    eraseContent()
}
```

### Create a Page with Template (v0.3.0+)

Create pages using predefined templates from a data source:

```kotlin
// Use the data source's default template
val page = notion.pages.create {
    parent.dataSource("data-source-id")
    template.default()
}

// Use a specific template by ID
val page = notion.pages.create {
    parent.dataSource("data-source-id")
    template.byId("template-id")
}

// Explicitly create without any template content
val page = notion.pages.create {
    parent.dataSource("data-source-id")
    template.none()
}
```

**Note**: Template and content (children) are mutually exclusive - you cannot specify both.

You can also apply a template when updating an existing page:

```kotlin
notion.pages.update("page-id") {
    template.default()
}

// Apply template and erase existing content
notion.pages.update("page-id") {
    template.byId("template-id")
    eraseContent()
}
```

### Create a Page with Position (v0.3.0+)

Control where a new page appears within its parent:

```kotlin
// Place at the start of the parent
val page = notion.pages.create {
    parent.dataSource("data-source-id")
    position.pageStart()
}

// Place at the end of the parent
val page = notion.pages.create {
    parent.dataSource("data-source-id")
    position.pageEnd()
}

// Place after a specific block
val page = notion.pages.create {
    parent.dataSource("data-source-id")
    position.afterBlock("block-id")
}
```

## Common Patterns

### Create a Task in a Project Management Database

```kotlin
val task = notion.pages.create {
    parent.dataSource("data-source-id")

    properties {
        title("Task Name", "Implement feature X")
        select("Status", "To Do")
        select("Priority", "High")
        people("Assignee", currentUserId)
        date("Due Date", "2025-10-20")
        multiSelect("Tags", "feature", "backend")
    }

    icon.emoji("🚀")
}
```

### Batch Create Pages

```kotlin
val taskNames = listOf("Task 1", "Task 2", "Task 3")

val createdPages = taskNames.map { taskName ->
    notion.pages.create {
        parent.dataSource("data-source-id")
        properties {
            title("Task Name", taskName)
            select("Status", "To Do")
        }
    }
}

println("Created ${createdPages.size} pages")
```

### Update Multiple Properties at Once

```kotlin
notion.pages.update("page-id") {
    properties {
        select("Status", "Completed")
        checkbox("Done", true)
        date("Completed Date", "2025-10-06")
        number("Final Score", 95.0)
    }

    // Also update the icon to reflect completion
    icon.emoji("✅")
}
```

### Clone a Page's Properties

```kotlin
val original = notion.pages.retrieve("original-page-id")

// Extract properties (you'd need to map PageProperty -> PagePropertyValue)
// This is a simplified example
val cloned = notion.pages.create {
    parent.dataSource("same-data-source-id")
    properties {
        title("Name", "Copy of ${(original.properties["Name"] as? PageProperty.Title)?.plainText}")
        // ... copy other properties
    }
}
```

## Best Practices

1. **Match schema** - When creating pages in data sources, ensure properties match the schema
2. **Use type-safe properties** - Cast to specific `PageProperty` types when reading
3. **Handle nulls** - Properties can be null/empty, always provide defaults
4. **Batch carefully** - Rate limits apply, consider adding delays for large batches
5. **Use data source parents** - In 2025-09-03, pages in tables use `dataSourceId` parent
6. **Archive instead of delete** - Notion doesn't support deletion, use `archive()` instead
7. **Validate before create** - The library has built-in validation, but pre-validate complex data
8. **Property IDs for pagination** - Get property ID from page schema for `retrievePropertyItems()`

## Gotchas and Tips

### ❌ Common Mistake: Using Database ID as Parent

```kotlin
// ❌ Wrong (2025-09-03 API)
parent.database("database-id")  // This doesn't work for creating rows

// ✅ Correct
parent.dataSource("data-source-id")  // Use the data source ID
```

### Getting Data Source ID from Database

```kotlin
// Retrieve the database
val database = notion.databases.retrieve("database-id")

// Get the first data source (usually there's only one)
val dataSourceId = database.dataSources.firstOrNull()?.id
    ?: error("No data sources found")

// Now create a page
notion.pages.create {
    parent.dataSource(dataSourceId)
    // ...
}
```

### Property Names Must Match Schema

```kotlin
// If your data source has a property called "Task Name", use that exact name:
properties {
    title("Task Name", "My task")  // ✅ Exact match
    title("TaskName", "My task")   // ❌ Won't work
}
```

### Icon and Cover Options

```kotlin
// Icon options
icon.emoji("🎯")
icon.external("https://example.com/icon.png")

// Cover options
cover.external("https://example.com/cover.jpg")
```

**Note**: Icon and cover removal is not currently supported (see journal entry `2025_10_06_icon_cover_removal_issue.md`).

## Related APIs

- **[Data Sources](data-sources.md)** - Create pages as rows in tables
- **[Blocks](blocks.md)** - Add rich content to pages using blocks
- **[Rich Text DSL](rich-text-dsl.md)** - Format text in page properties and content
- **[Databases](databases.md)** - Understand the database/data source relationship
