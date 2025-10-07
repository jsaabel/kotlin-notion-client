# Comments API

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

## Overview

The Comments API allows you to create and retrieve comments on pages and discussions.

**Official Documentation**: [Notion Comments API](https://developers.notion.com/reference/create-a-comment)

## Available Operations

```kotlin
// Create a comment
suspend fun create(block: CreateCommentRequestBuilder.() -> Unit): Comment

// Retrieve comments
suspend fun retrieve(block: RetrieveCommentsRequestBuilder.() -> Unit): PaginatedList<Comment>
```

## Examples

_TODO: Add comprehensive examples_

### Create a Comment on a Page

```kotlin
val comment = notion.comments.create {
    parent { page(pageId = "page-id") }
    richText {
        text("This looks great!")
    }
}
```

### Create a Comment in a Discussion

```kotlin
val comment = notion.comments.create {
    parent { discussionId("discussion-id") }
    richText {
        text("I agree with this approach")
    }
}
```

### Retrieve Comments

```kotlin
val comments = notion.comments.retrieve {
    blockId("block-id")
}

comments.results.forEach { comment ->
    println("${comment.createdBy}: ${comment.richText}")
}
```

## Common Patterns

_TODO: Add tips, gotchas, best practices_

## Related APIs

- **[Pages](pages.md)** - Comments are attached to pages
- **[Users](users.md)** - Comments are created by users
- **[Rich Text DSL](rich-text-dsl.md)** - Format comment text
