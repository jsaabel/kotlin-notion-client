# Comments API

The Comments API allows you to create and retrieve comments on pages and discussions using a fluent DSL interface.

**Official Documentation**: [Notion Comments API](https://developers.notion.com/reference/create-a-comment)

## Available Operations

```kotlin
// Create a comment using DSL
suspend fun create(block: CreateCommentRequestBuilder.() -> Unit): Comment

// Create a comment from request object
suspend fun create(request: CreateCommentRequest): Comment

// Retrieve comments using DSL
suspend fun retrieve(block: RetrieveCommentsRequestBuilder.() -> Unit): List<Comment>

// Retrieve comments by block ID (simple)
suspend fun retrieve(blockId: String): List<Comment>
```

## Creating Comments

### Basic Comment on a Page

```kotlin
val comment = notion.comments.create {
    parent.page("12345678-1234-1234-1234-123456789abc")
    richText {
        text("This looks great!")
    }
}
```

### Comment on a Specific Block

```kotlin
val comment = notion.comments.create {
    parent.block("87654321-4321-4321-4321-210987654321")
    richText {
        text("Great point made in this paragraph!")
    }
}
```

### Reply to an Existing Discussion

```kotlin
val comment = notion.comments.create {
    parent.page("page-id")
    richText {
        text("I agree with this approach")
    }
    discussionId("existing-discussion-id") // Reply to existing thread
}
```

### Comment with Rich Formatting

```kotlin
val comment = notion.comments.create {
    parent.page("page-id")
    richText {
        text("This comment has ")
        bold("bold text")
        text(", ")
        italic("italic text")
        text(", and ")
        code("inline code")
        text("!")
    }
}
```

### Comment with Links and Mentions

```kotlin
val comment = notion.comments.create {
    parent.page("page-id")
    richText {
        text("Check out ")
        link("https://notion.so", "Notion")
        text(" and contact ")
        userMention("user-id-123")
        text(" for more details.")
    }
}
```

### Comment with Custom Display Name

```kotlin
val comment = notion.comments.create {
    parent.page("page-id")
    richText {
        text("This comment is from a bot integration")
    }
    displayName("Project Bot")
}
```

### Comment with File Attachments

```kotlin
// First upload a file
val uploadResult = notion.enhancedFileUploads.uploadFile(
    filename = "report.pdf",
    data = fileBytes
)

// Then create comment with attachment
val comment = notion.comments.create {
    parent.page("page-id")
    richText {
        text("Here's the report you requested!")
    }
    attachment(uploadResult.uploadId)
}
```

### Multiple Attachments

```kotlin
val comment = notion.comments.create {
    parent.page("page-id")
    richText {
        text("Multiple files attached")
    }
    attachments(listOf(
        CommentAttachmentRequest("file-upload-1"),
        CommentAttachmentRequest("file-upload-2"),
        CommentAttachmentRequest("file-upload-3")
    ))
}
```

## Retrieving Comments

### Basic Retrieval

```kotlin
// Get all comments for a page or block
val comments = notion.comments.retrieve {
    blockId("12345678-1234-1234-1234-123456789abc")
}

comments.forEach { comment ->
    println("${comment.createdBy.name}: ${comment.richText.joinToString("") { it.plainText }}")
}
```

### Simple Retrieval (Alternative)

```kotlin
// Direct method without DSL
val comments = notion.comments.retrieve("page-id")
```

### Paginated Retrieval

```kotlin
// Get first page with specific size
val firstPage = notion.comments.retrieve {
    blockId("page-id")
    pageSize(25)
}

// Get next page
val nextPage = notion.comments.retrieve {
    blockId("page-id") 
    pageSize(25)
    startCursor("cursor-from-previous-response")
}
```

## DSL Method Variations

The Comments DSL provides several method variations for consistency with other APIs:

### Parent Configuration

```kotlin
// These are equivalent:
parent.pageId("page-id")
parent.page("page-id")

// These are equivalent:
parent.blockId("block-id") 
parent.block("block-id")
```

### Content Configuration

```kotlin
// These are equivalent:
content {
    text("Comment text")
}

richText {
    text("Comment text")
}
```

## Common Patterns

### Error Handling

```kotlin
try {
    val comment = notion.comments.create {
        parent.page("page-id")
        richText {
            text("My comment")
        }
    }
} catch (e: NotionException.ApiError) {
    println("API Error: ${e.message}")
} catch (e: IllegalArgumentException) {
    println("Validation Error: ${e.message}")
}
```

### Building Complex Comments

```kotlin
val comment = notion.comments.create {
    parent.block("target-block-id")
    
    richText {
        text("📊 Analysis complete! ")
        bold("Key findings:")
        text("\n• Performance improved by 25%")
        text("\n• User satisfaction up 15%")
        text("\n\nFull report: ")
        link("https://company.com/report", "View Report")
        text("\n\ncc: ")
        userMention("manager-user-id")
    }
    
    displayName("Analytics Bot")
    discussionId("existing-discussion-123")
}
```

### Iterating Through Comments

```kotlin
val comments = notion.comments.retrieve("page-id")

comments.forEach { comment ->
    println("Comment ${comment.id}:")
    println("  Author: ${comment.createdBy.name}")
    println("  Created: ${comment.createdTime}")
    println("  Discussion: ${comment.discussionId}")
    
    // Process rich text content
    comment.richText.forEach { richTextItem ->
        when (richTextItem.type) {
            "text" -> println("  Text: ${richTextItem.plainText}")
            "mention" -> println("  Mention: ${richTextItem.mention}")
            else -> println("  ${richTextItem.type}: ${richTextItem.plainText}")
        }
    }
    
    // Process attachments if any
    comment.attachments?.forEach { attachment ->
        println("  Attachment: ${attachment.name}")
    }
    
    println()
}
```

## Validation and Limits

### Comment Content
- Comments must have non-empty rich text content
- Rich text supports all standard formatting (bold, italic, code, links, mentions)

### Attachments
- Maximum 3 attachments per comment
- Attachments must be uploaded first using the File Uploads API

### Parent Requirements
- Comments must specify either a page ID or block ID as parent
- Cannot comment on archived pages or blocks

## Best Practices

1. **Use Meaningful Content**: Write descriptive comments that add value to discussions
2. **Handle Errors**: Always wrap comment creation in try-catch blocks
3. **Batch Operations**: When creating multiple comments, consider rate limiting
4. **Rich Formatting**: Use rich text features to improve readability
5. **Mention Users**: Use `userMention()` to notify relevant team members
6. **File Management**: Clean up uploaded files that are no longer needed

## Related APIs

- **[Pages](pages.md)** - Comments are attached to pages
- **[Users](users.md)** - Comments are created by users
- **[Rich Text DSL](rich-text-dsl.md)** - Format comment text
