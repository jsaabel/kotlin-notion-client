# Blocks API

## Overview

Blocks are the content elements that make up Notion pages. The Blocks API supports 30+ block types including paragraphs, headings, lists, code blocks, tables, and more.

**Official Documentation**: [Notion Blocks API](https://developers.notion.com/reference/block)

## Available Operations

```kotlin
// Retrieve a block
suspend fun retrieve(blockId: String): Block

// Retrieve children of a block
suspend fun retrieveChildren(blockId: String): List<Block>

// Append children to a block
suspend fun appendChildren(blockId: String, builder: PageContentBuilder.() -> Unit): BlockList

// Update a block
suspend fun update(blockId: String, builder: PageContentBuilder.() -> Unit): Block

// Delete a block (archives it)
suspend fun delete(blockId: String): Block
```

## Supported Block Types

### Text Blocks
- **Paragraph** - Basic text content
- **Headings** - H1, H2, H3 for structure
- **Quote** - Quoted text
- **Callout** - Highlighted content with an icon

### List Blocks
- **Bulleted List** - Unordered lists
- **Numbered List** - Ordered lists
- **To-Do List** - Checkable task items
- **Toggle** - Collapsible content

### Media & Code
- **Code** - Syntax-highlighted code blocks
- **Image** - Embedded images
- **Video** - Embedded videos
- **Audio** - Audio files
- **File** - File attachments
- **PDF** - PDF documents

### Layout
- **Divider** - Horizontal separator
- **Table** - Structured tabular data
- **Column List** - Multi-column layouts
- **Table of Contents** - Auto-generated ToC

### Advanced
- **Bookmark** - Link previews
- **Link Preview** - Rich link cards
- **Embed** - External embeds
- **Equation** - LaTeX equations
- **Synced Block** - Content synced across pages
- **Template** - Reusable templates
- **Breadcrumb** - Navigation breadcrumbs
- **Child Page** - Nested pages
- **Child Database** - Nested databases

## Examples

### Example 1: Retrieve Block Children

Get all blocks that are children of a page or block:

```kotlin
val blocks = notion.blocks.retrieveChildren(pageId)
blocks.forEach { block ->
    println("${block.type}: ${block.id}")
}
```

### Example 2: Append Simple Blocks

Add basic content blocks to a page:

```kotlin
notion.blocks.appendChildren(pageId) {
    heading1("Project Overview")

    paragraph("This project aims to...")

    bullet("First key point")
}
```

### Example 3: Rich Text Formatting

Create blocks with formatted text:

```kotlin
notion.blocks.appendChildren(pageId) {
    paragraph {
        text("This text is ")
        bold("bold")
        text(", this is ")
        italic("italic")
        text(", and this is ")
        code("code")
        text(".")
    }
}
```

### Example 4: Different List Types

Create various types of lists:

```kotlin
notion.blocks.appendChildren(pageId) {
    heading2("Task List")

    toDo("Review pull requests", checked = false)
    toDo("Update documentation", checked = true)
    toDo("Deploy to production", checked = false)

    heading2("Features")

    bullet("Type-safe API")
    bullet("Coroutine support")
    bullet("Comprehensive error handling")

    heading2("Steps")

    number("Initialize the client")
    number("Configure authentication")
    number("Make API calls")
}
```

### Example 5: Code Blocks

Add syntax-highlighted code:

```kotlin
notion.blocks.appendChildren(pageId) {
    code(
        language = "kotlin",
        code = """
            fun main() {
                println("Hello, Notion!")
            }
        """.trimIndent()
    )
}
```

### Example 6: Callouts

Create highlighted callout blocks:

```kotlin
notion.blocks.appendChildren(pageId) {
    callout("⚠️") {
        text("Important: Make sure to read the documentation before proceeding.")
    }
}
```

### Example 7: Quotes and Dividers

Add quotes and visual separators:

```kotlin
notion.blocks.appendChildren(pageId) {
    quote("The best way to predict the future is to invent it.")

    divider()

    paragraph("Content after the divider.")
}
```

### Example 8: Retrieve a Single Block

Get detailed information about a specific block:

```kotlin
val block = notion.blocks.retrieve(blockId)

println("Block type: ${block.type}")
println("Created: ${block.createdTime}")
println("Has children: ${block.hasChildren}")
```

### Example 9: Update a Block

Modify the content of an existing block:

```kotlin
val updated = notion.blocks.update(blockId) {
    paragraph("Updated text with new content")
}
```

**Note**: You cannot change a block's type. The block type in the update must match the existing block's type.

### Example 10: Delete a Block

Archive a block (blocks are not permanently deleted, just archived):

```kotlin
val deleted = notion.blocks.delete(blockId)
println("Block archived: ${deleted.archived}") // true
```

### Example 11: Nested Blocks

Create blocks with nested children:

```kotlin
// Create a parent block
val result = notion.blocks.appendChildren(pageId) {
    bullet("Parent item")
}
val parentBlockId = result.results.first().id

// Add nested items
notion.blocks.appendChildren(parentBlockId) {
    bullet("Nested item 1")
    bullet("Nested item 2")
}
```

### Example 12: Toggle Blocks

Create collapsible toggle blocks with hidden content:

```kotlin
// Create a toggle block
val result = notion.blocks.appendChildren(pageId) {
    toggle("Click to expand")
}
val toggleBlockId = result.results.first().id

// Add content inside the toggle
notion.blocks.appendChildren(toggleBlockId) {
    paragraph("This content is hidden inside the toggle.")
    bullet("Hidden bullet point 1")
    bullet("Hidden bullet point 2")
}
```

### Example 13: Complete Document Structure

Create a comprehensive document with multiple block types:

```kotlin
val page = notion.pages.create {
    parent.page(parentPageId)
    title("Complete Document Example")

    content {
        heading1("Introduction")
        paragraph("This document demonstrates all block types.")

        divider()

        heading2("Code Example")
        code(
            language = "kotlin",
            code = """
                val client = NotionClient(config)
                val page = client.pages.retrieve(pageId)
            """.trimIndent()
        )

        heading2("Important Notes")
        callout("💡") {
            text("Remember to handle errors appropriately.")
        }

        heading2("Tasks")
        toDo("Complete implementation", checked = false)
        toDo("Write tests", checked = false)
        toDo("Update documentation", checked = true)

        heading2("Resources")
        bullet("Official Notion API documentation")
        bullet("Kotlin coroutines guide")
        bullet("Best practices for API clients")

        divider()

        quote("Well-documented code is as important as the code itself.")
    }
}
```

## Working with Block Types

### Type Checking

Use Kotlin's `is` operator to check block types:

```kotlin
val blocks = notion.blocks.retrieveChildren(pageId)

blocks.forEach { block ->
    when (block) {
        is Block.Heading1 -> println("H1: ${block.heading1.richText.first().plainText}")
        is Block.Paragraph -> println("Paragraph: ${block.paragraph.richText.first().plainText}")
        is Block.Code -> println("Code (${block.code.language})")
        is Block.ToDo -> println("Task: ${block.toDo.richText.first().plainText} (${block.toDo.checked})")
        else -> println("Other: ${block.type}")
    }
}
```

### Accessing Block Content

Each block type has a corresponding property with its content:

```kotlin
val block = notion.blocks.retrieve(blockId)

when (block) {
    is Block.Paragraph -> {
        val text = block.paragraph.richText.firstOrNull()?.plainText
        val color = block.paragraph.color
    }
    is Block.Code -> {
        val code = block.code.richText.firstOrNull()?.plainText
        val language = block.code.language
    }
    is Block.Callout -> {
        val icon = block.callout.icon?.emoji
        val text = block.callout.richText.firstOrNull()?.plainText
    }
}
```

## Common Patterns

### Building Complex Content

Use the DSL builder to create structured content efficiently:

```kotlin
notion.blocks.appendChildren(pageId) {
    heading1("Project Documentation")

    // Introduction section
    paragraph("Welcome to the project.")

    // Features with nested structure
    toggle("Features") {
        bullet("Feature 1")
        bullet("Feature 2")
        bullet("Feature 3")
    }

    // Code examples
    heading2("Quick Start")
    code(
        language = "kotlin",
        code = "val client = NotionClient(config)"
    )
}
```

### Pagination Handling

Retrieving block children supports multiple pagination approaches:

#### 1. Automatic Collection (Simple)

`retrieveChildren()` automatically fetches all children:

```kotlin
// Retrieves ALL children, handling pagination internally
val allBlocks = notion.blocks.retrieveChildren(pageId)
println("Total blocks: ${allBlocks.size}")
```

**Use when**: You need all children and the count is reasonable (< 1000 blocks).

#### 2. Flow-Based Streaming

For pages with many blocks, use Flow for efficient processing:

```kotlin
// Memory-efficient - processes blocks as they're fetched
notion.blocks.retrieveChildrenAsFlow(pageId).collect { block ->
    println("Processing ${block.type}: ${block.id}")
    // Process each block individually
}
```

**Use when**: Working with pages that have many blocks (1000+) or memory efficiency matters.

#### 3. Page-Level Flow

Access pagination metadata while processing:

```kotlin
// Get complete responses with pagination info
notion.blocks.retrieveChildrenPagedFlow(pageId).collect { response ->
    println("Fetched ${response.results.size} blocks (has more: ${response.hasMore})")
    response.results.forEach { block ->
        // Process blocks in this batch
    }
}
```

See **[Pagination](pagination.md)** for comprehensive guide and best practices.

### Error Handling

Handle common block API errors:

```kotlin
try {
    val block = notion.blocks.retrieve(blockId)
} catch (e: NotionException.ApiError) {
    when (e.status) {
        404 -> println("Block not found")
        403 -> println("No permission to access block")
        else -> println("API error: ${e.details}")
    }
} catch (e: NotionException.NetworkError) {
    println("Network error: ${e.message}")
}
```

## Best Practices

1. **Batch Operations** - Use `appendChildren()` to add multiple blocks at once instead of individual calls
2. **Block Type Validation** - Always check block types before accessing type-specific properties
3. **Nested Content** - Remember that blocks can have children; use `hasChildren` to check
4. **Archive vs Delete** - Understand that "delete" actually archives blocks, not permanently removing them
5. **Rich Text** - Use the rich text DSL for formatted content instead of plain strings
6. **Code Language** - Specify the language for code blocks to enable syntax highlighting in Notion

## Related APIs

- **[Pages](pages.md)** - Blocks belong to pages; use the `content {}` builder to add blocks when creating pages
- **[Rich Text DSL](rich-text-dsl.md)** - Format text within blocks with bold, italic, links, and more
