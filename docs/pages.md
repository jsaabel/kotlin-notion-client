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

// Move a page to trash
suspend fun trash(pageId: String): Page

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
println("In trash: ${page.inTrash}")

// Access properties
val title = page.properties["Name"] as? PageProperty.Title
println("Title: ${title?.plainText}")
```

### Create a Page in a Data Source (Database Row)

**Important**: In the 2026-03-11 API, pages in tables are created with a `dataSourceId` parent (not `databaseId`):

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

### Create a Page with Local Files

Icon, cover, "Files & media" properties and content blocks all take a local file directly. The
client uploads them all in one concurrent pass and swaps in the resulting references before the
single `POST /v1/pages` goes out — nothing to upload above the call, no ids to thread through:

```kotlin
val page = notion.pages.create {
    parent.dataSource("data-source-id")
    properties {
        title("Name", "Q3 Report")
        files("Attachments") {
            upload(File("a.pdf"))
            upload(Paths.get("b.pdf"), name = "Appendix B")
        }
    }
    icon.upload(File("logo.png"))
    cover.upload(File("hero.png"))
    content {
        image(File("chart.png"), caption = "Q3")
    }
}
```

If any upload fails the rest are cancelled, `FileUploadError` is thrown, and the page is never
created. `pages.update` takes local files the same way for icon, cover and files properties. See
[File uploads](file-uploads.md) for the full picture.

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

Both also take a local file, uploaded as part of the update:

```kotlin
val updated = notion.pages.update("page-id") {
    icon.upload(File("logo.png"))
    cover.upload(File("hero.png"))
}
```

### Trash a Page

```kotlin
// Move a page to trash using the dedicated method
val trashed = notion.pages.trash("page-id")
println("Page in trash: ${trashed.inTrash}")  // true

// Or use update with the trash() DSL
val trashed = notion.pages.update("page-id") {
    trash()
}
```

> **Note**: Notion doesn't support permanent deletion. Pages moved to trash are hidden from the UI
> but remain accessible via the API. Use `restore()` (or `trash(false)`) to bring them back.

### Restore a Page from Trash

```kotlin
val restored = notion.pages.update("page-id") {
    trash(false)  // Restore from trash
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

**Key point**: Use `dataSourceId` in 2026-03-11 API (not `databaseId` from older versions).

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

### Reading Date Properties

A Notion date value with a time component always carries a numeric UTC offset (Notion
never returns a named `time_zone`). Reading it answers one of three different questions,
and the accessor name says which one you are asking:

| Question | Accessor | Returns |
|----------|----------|---------|
| "What does this say?" — wall-clock digits | `wallClockDateTime` | `LocalDateTime?` |
| "When did this actually happen?" — absolute time | `utcInstant` / `requireUtcInstant()` | `Instant?` / `Instant` |
| "What zone is this value in?" — the stored offset | `storedOffset` | `UtcOffset?` |

```kotlin
val prop = page.properties["Start"] as PageProperty.Date   // "2026-06-15T14:30:00.000+02:00"

// Render the time to someone standing where the value applies
prop.wallClockDateTime          // LocalDateTime(2026, 6, 15, 14, 30)

// Push it to a system that stores absolute time
prop.utcInstant                 // Instant 2026-06-15T12:30:00Z
prop.requireUtcInstant()        // same, but throws instead of returning null

// Ask what zone the value is stored in
prop.storedOffset               // UtcOffset(hours = 2)

// Render the same instant in a zone of your choosing
prop.localDateTimeIn(TimeZone.of("America/New_York"))   // LocalDateTime(2026, 6, 15, 8, 30)
```

Every accessor has an `end…` twin for the end of a date range: `endWallClockDateTime`,
`endStoredOffset`, `endUtcInstant`, `requireEndUtcInstant()`, `endLocalDateTimeIn(zone)`.
Date-only values are read with `localDateValue` / `endLocalDateValue`, and the raw strings
Notion returned are always available as `stringValue` / `endStringValue`.

#### Auditing a value's offset

`offsetIn(timeZone)` returns the offset that zone would have had at the value's **own**
wall-clock date and time — DST included. Comparing it with `storedOffset` is how you detect
a value that has drifted (for example, local times that were written to Notion as UTC):

```kotlin
val zone = TimeZone.of("Europe/Oslo")
if (prop.storedOffset != prop.offsetIn(zone)) {
    println("${prop.stringValue} is not the Oslo local time it should be")
}
```

For a wall-clock time that occurs twice (the autumn DST overlap) the earlier offset is used;
for one that does not exist (the spring gap) the time is shifted forward by the gap.

#### When the nullable accessors return null

`utcInstant` is null when the value is absent, date-only, carries no offset, or does not
parse. An offset-less datetime genuinely has no knowable instant — but a silent null is
exactly what hides that kind of bug, so use `requireUtcInstant()` wherever a missing instant
should be an error. It throws `IllegalArgumentException` naming the offending value and what
to do about it.

### Writing Date Properties

Writing a datetime states one of two different things, and the argument types say which
one — mirroring the read-side vocabulary above:

| Statement | Method | Wire value |
|-----------|--------|-----------|
| "this wall-clock time, in this zone" | `dateTime(name, LocalDateTime, TimeZone)` | `2026-10-25T13:00:00+01:00` |
| "this absolute point in time" | `dateTime(name, Instant)` | `2026-10-25T12:00:00Z` |

```kotlin
val oslo = TimeZone.of("Europe/Oslo")

notion.pages.create {
    parent.dataSource(dataSourceId)
    properties {
        // Wall-clock time: the digits are preserved and the zone's UTC offset at
        // that value's own local date is attached. DST is resolved per value:
        dateTime("Doors", LocalDateTime(2026, 10, 24, 13, 0), oslo)   // …13:00:00+02:00
        dateTime("Curfew", LocalDateTime(2026, 10, 25, 13, 0), oslo)  // …13:00:00+01:00

        // Absolute instant, written as UTC:
        dateTime("Deployed at", Clock.System.now())

        // Ranges resolve each end at its own local date, so they may span a changeover:
        dateTimeRange("Night shift", LocalDateTime(2026, 10, 24, 22, 0), LocalDateTime(2026, 10, 25, 4, 0), oslo)
    }
}
```

The `TimeZone` parameter is **required** — a `LocalDateTime` alone does not identify a
point in time, so there is no default. An ambiguous wall-clock time (the autumn DST
overlap) resolves to the earlier instant; a nonexistent one (the spring gap) is shifted
forward by the gap. Both match what Notion itself does when resolving a named zone.

String overloads are validated rather than trusted: a datetime string with neither a UTC
offset nor a `time_zone` is rejected with `IllegalArgumentException`, because Notion reads
such values as UTC — silently moving the instant while keeping the wall clock. A
`time_zone` on a date-only value, and an offset combined with a `time_zone`, are rejected
for the same fail-fast reason.

```kotlin
dateTime("Start", "2026-06-15T14:30:00+02:00")                    // ✅ offset-bearing
dateTimeWithTimeZone("Start", "2026-06-15T14:30:00", "Europe/Oslo") // ✅ naive + zone, Notion resolves it
dateTime("Start", "2026-06-15T14:30:00")                          // ❌ throws — no offset, no zone
```

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
| Verification (v0.4.0+) | `verify(name)` / `unverify(name)` | `verify("Verification")` |

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

### Verification Property (v0.4.0+)

For pages inside wiki databases, you can verify or unverify the page content:

```kotlin
// Verify a page (for pages in wiki databases)
notion.pages.update("wiki-page-id") {
    properties {
        verify("Verification")
        // Or with a 90-day expiry window:
        verify("Verification",
            start = "2026-04-14",
            end = "2026-07-13"
        )
    }
}

// Remove verification
notion.pages.update("wiki-page-id") {
    properties {
        unverify("Verification")
    }
}

// Read verification state
val page = notion.pages.retrieve("wiki-page-id")
val verification = page.properties["Verification"] as? PageProperty.Verification
println("State: ${verification?.verification?.state}")         // "verified"
println("Verified by: ${verification?.verification?.verifiedBy?.name}")
println("Expires: ${verification?.verification?.date?.end}")
```

> **Note**: Only available on pages inside wiki databases, which cannot be created programmatically.

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

### Create a Page with Markdown Content (v0.4.0+)

Create a page using markdown text instead of individual blocks:

```kotlin
val page = notion.pages.create {
    parent.page("parent-page-id")
    title("My Markdown Page")

    markdown("""
        # Introduction

        This page was created using **Markdown** via the Notion API.

        > Callout text can use blockquotes.

        - Item one
        - Item two

        ```kotlin
        val client = NotionClient("token")
        ```
    """.trimIndent())
}
```

> **Note**: `markdown` and `content {}` (children) are mutually exclusive. Also mutually exclusive with `template`.

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
5. **Use data source parents** - In 2026-03-11, pages in tables use `dataSourceId` parent
6. **Trash instead of delete** - Notion doesn't support permanent deletion, use `trash()` to move pages to trash and `trash(false)` to restore
7. **Validate before create** - The library has built-in validation, but pre-validate complex data
8. **Property IDs for pagination** - Get property ID from page schema for `retrievePropertyItems()`

## Gotchas and Tips

### ❌ Common Mistake: Using Database ID as Parent

```kotlin
// ❌ Wrong (2026-03-11 API)
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
// Native Notion icon with optional color (v0.4.0+)
icon.native("pizza")
icon.native("code", NativeIconColor.BLUE)

// Cover options
cover.external("https://example.com/cover.jpg")
```

Available `NativeIconColor` values: `GRAY` (default), `LIGHT_GRAY`, `BROWN`, `YELLOW`, `ORANGE`, `GREEN`, `BLUE`, `PURPLE`, `PINK`, `RED`.

**Note**: Icon and cover removal is not currently supported (see journal entry `2025_10_06_icon_cover_removal_issue.md`).

## Related APIs

- **[Data Sources](data-sources.md)** - Create pages as rows in tables
- **[Blocks](blocks.md)** - Add rich content to pages using blocks
- **[Rich Text DSL](rich-text-dsl.md)** - Format text in page properties and content
- **[Databases](databases.md)** - Understand the database/data source relationship
