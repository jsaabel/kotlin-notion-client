# Blocks API

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

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
suspend fun append(blockId: String, block: BlocksRequestBuilder.() -> Unit): List<Block>

// Update a block
suspend fun update(blockId: String, block: BlockUpdateRequestBuilder.() -> Unit): Block

// Delete a block
suspend fun delete(blockId: String): Block
```

## Supported Block Types

_TODO: List all 30+ supported block types_

- Paragraph
- Headings (H1, H2, H3)
- Bulleted list, Numbered list, To-do list
- Code blocks
- Tables
- Callouts
- And many more...

## Examples

_TODO: Add comprehensive examples_

### Retrieve Block Children

```kotlin
val blocks = notion.blocks.retrieveChildren("page-id")
blocks.forEach { block ->
    println("${block.type}: ${block.id}")
}
```

### Append Blocks to a Page

```kotlin
notion.blocks.append("page-id") {
    heading1 {
        richText {
            text("Project Overview")
        }
    }

    paragraph {
        richText {
            text("This project aims to...")
        }
    }

    bulletedListItem {
        richText {
            text("First key point")
        }
    }
}
```

### Working with Code Blocks

```kotlin
// TODO: Add example
```

### Creating Tables

```kotlin
// TODO: Add example
```

### Update a Block

```kotlin
// TODO: Add example
```

### Delete a Block

```kotlin
// TODO: Add example
```

## Common Patterns

_TODO: Add tips, gotchas, best practices_

## Related APIs

- **[Pages](pages.md)** - Blocks belong to pages
- **[Rich Text DSL](rich-text-dsl.md)** - Format text within blocks
