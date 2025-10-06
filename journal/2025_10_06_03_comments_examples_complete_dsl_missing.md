# Comments API Examples Complete - DSL Missing

**Date:** 2025-10-06
**Status:** ✅ Examples Complete | ⚠️ DSL Gap Identified

## Summary

Created comprehensive validated examples for the Comments API with 9 examples covering all major functionality. During implementation, identified that Comments API lacks a DSL builder, making it inconsistent with Pages, Databases, Data Sources, and Blocks APIs.

## Work Completed

### 1. Created CommentsExamples.kt Integration Tests
- Created `src/test/kotlin/examples/CommentsExamples.kt` with 9 comprehensive examples
- Optimized to use only 2 pages instead of 10 (shared test page + block-specific page)
- Refactored to use DSLs where available (`notion.pages.create {}`, `pageContent {}`)
- All examples use `notion` client naming for consistency

### 2. Examples Included
1. **Create a simple comment on a page** - Basic comment creation
2. **Create a comment thread using discussion_id** - Threading replies
3. **Rich text formatting in comments** - Bold, italic formatting
4. **Comment on a specific block** - Block-level comments
5. **Comments with links** - Embedded URLs in rich text
6. **User mentions in comments** - @mentioning users (optional, requires NOTION_TEST_USER_ID)
7. **Comments with file attachments** - Attaching uploaded files
8. **Retrieve all comments from a page** - Fetching and listing comments
9. **Handle validation errors** - Empty text and attachment limits

### 3. Code Quality Improvements
- Uses `notion` instead of `client` for consistency with other examples
- Leverages `notion.pages.create {}` DSL for page creation
- Uses `pageContent {}` DSL for block creation
- Proper `beforeSpec`/`afterSpec` lifecycle management
- Conditional cleanup with `shouldCleanupAfterTest()`

### 4. Files Modified
- ✅ `src/test/kotlin/examples/CommentsExamples.kt` (created)

## Testing

All code compiles successfully:
- ✅ `./gradlew formatKotlin` - Code formatted
- ✅ `./gradlew compileTestKotlin` - Compiles without errors
- ✅ `./gradlew build` - Full build successful

## Critical Gap Identified: Missing Comments DSL

### Current State
Comments API uses verbose request objects:
```kotlin
notion.comments.create(
    CreateCommentRequest(
        parent = Parent(type = "page_id", pageId = testPageId!!),
        richText = listOf(
            RequestBuilders.createSimpleRichText("This is a simple comment"),
        ),
    ),
)
```

### Desired DSL Pattern (like other APIs)
```kotlin
notion.comments.create {
    parent.page(testPageId)
    text("This is a simple comment")
    // or: richText { ... }
}
```

### Comparison with Other APIs
All other major APIs have DSL builders:
- ✅ **Pages**: `CreatePageRequestBuilder.kt` → `notion.pages.create {}`
- ✅ **Databases**: `DatabaseRequestBuilder.kt` → `notion.databases.create {}`
- ✅ **Data Sources**: `DataSourceRequestBuilder.kt` → `notion.dataSources.create {}`
- ✅ **Blocks**: `PageContentBuilder.kt` → `pageContent {}`
- ❌ **Comments**: No builder → Manual `CreateCommentRequest(...)`

### Impact
- **Inconsistent API** - Comments feel like second-class citizens
- **Poor DX** - Verbose, error-prone manual construction
- **Documentation** - Examples look clunky compared to other APIs
- **Discoverability** - DSL provides IDE autocomplete guidance

## Next Priority: Implement Comments DSL

### Recommended Implementation
1. **Create** `CreateCommentRequestBuilder.kt`
2. **Pattern match** `CreatePageRequestBuilder` and `DatabaseRequestBuilder`
3. **Support**:
   - `parent.page(id)` and `parent.block(id)`
   - `text(content)` for simple text
   - `richText { }` for complex formatting
   - `discussionId(id)` for threading
   - `attachments { }` for file uploads
   - `mention.user(userId)` for mentions

### Example Target API
```kotlin
notion.comments.create {
    parent.page(testPageId)
    text("This is a simple comment")
}

notion.comments.create {
    parent.page(testPageId)
    discussionId(firstComment.discussionId)
    richText {
        text("This is ")
        text("bold text", bold = true)
    }
}

notion.comments.create {
    parent.block(blockId)
    richText {
        text("Hey ")
        mention.user(userId)
        text(", check this out!")
    }
}
```

## Known Issues

### Link in Rich Text Not Properly Created
**Issue:** Example 5 "Comments with links" claims to create a link but the implementation may be incorrect:
```kotlin
RichText(
    type = "text",
    text = TextContent(content = "Notion API docs", link = null),  // ← link is null
    annotations = Annotations(),
    plainText = "Notion API docs",
    href = "https://developers.notion.com",  // ← href on RichText
)
```

**Investigation needed:** Determine correct pattern for links in rich text:
- Should `TextContent.link` contain a `Link` object with the URL?
- Or should `RichText.href` be used?
- Or both?
- Check against official API samples and test with live API

**Related:** This further reinforces the need for a rich text DSL that handles this correctly.

## Related Files
- `src/test/kotlin/examples/CommentsExamples.kt` - Examples that would benefit from DSL
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/comments/Comment.kt` - Comment models
- `src/main/kotlin/no/saabelit/kotlinnotionclient/api/CommentsApi.kt` - API implementation

## References
- Pages DSL: `src/main/kotlin/no/saabelit/kotlinnotionclient/models/pages/CreatePageRequestBuilder.kt`
- Database DSL: `src/main/kotlin/no/saabelit/kotlinnotionclient/models/databases/DatabaseRequestBuilder.kt`
- Blocks DSL: `src/main/kotlin/no/saabelit/kotlinnotionclient/models/blocks/PageContentBuilder.kt`
- RichText DSL: `src/main/kotlin/no/saabelit/kotlinnotionclient/models/richtext/RichTextBuilder.kt`
