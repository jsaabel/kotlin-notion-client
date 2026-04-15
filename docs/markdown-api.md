# Markdown Content API

The Markdown Content API lets you retrieve, create, and update Notion page content as plain Markdown text, without working with individual blocks.

**Requires**: Notion API version `2026-03-11`+

**Entry point**: `client.markdown`

## Available Operations

```kotlin
// Retrieve a page's content as markdown
suspend fun retrieve(pageId: String, includeTranscript: Boolean = false): PageMarkdownResponse

// Update content via search-and-replace operations
suspend fun updateContent(pageId: String, builder: ContentUpdateBuilder.() -> Unit): PageMarkdownResponse
suspend fun updateContent(pageId: String, updates: List<ContentUpdate>): PageMarkdownResponse
suspend fun updateContent(pageId: String, request: UpdateContentRequest): PageMarkdownResponse

// Replace full page content
suspend fun replaceContent(pageId: String, markdown: String): PageMarkdownResponse
suspend fun replaceContent(pageId: String, request: ReplaceContentRequest): PageMarkdownResponse
```

## Response Shape

`PageMarkdownResponse` has the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | The page ID |
| `markdown` | `String` | The page content as markdown text |
| `truncated` | `Boolean` | `true` if the page exceeded ~20,000 blocks |
| `unknownBlockIds` | `List<String>` | Block IDs that could not be represented in markdown |

## Examples

### Retrieve a Page as Markdown

```kotlin
val response = notion.markdown.retrieve("page-id")
println(response.markdown)
println("Truncated: ${response.truncated}")

// Include meeting notes transcript blocks:
val withTranscript = notion.markdown.retrieve("page-id", includeTranscript = true)
```

### Search-and-Replace (DSL)

```kotlin
val updated = notion.markdown.updateContent("page-id") {
    replace("old text", "new text")
    replaceAll("every occurrence", "replacement")
}
```

### Full Page Replace

```kotlin
notion.markdown.replaceContent("page-id", """
    # New Content

    Everything above was replaced.
""".trimIndent())
```

### Create a Page with Markdown Content

Pass `markdown()` in the page creation builder instead of `content {}`:

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

See [pages.md](pages.md#create-a-page-with-markdown-content-v040) for more detail.

## Enhanced Markdown Format

Notion uses a superset of standard Markdown:

- Notion-specific block types use HTML-like tags (callouts, toggles, columns, etc.)
- Tab-indented children create nested blocks
- Standard inline formatting (bold, italic, strikethrough, inline code, links, equations) is fully supported

Refer to Notion's enhanced markdown guide for the full syntax reference.

## Known Behaviours / Gotchas

- The first `# h1` heading in a `markdown`-created page becomes the page title (it is not part of the body)
- Table rows must have `<td>` tags on separate lines (the library handles this correctly)
- `includeTranscript: true` also includes meeting notes transcript blocks in the output
- Both single `\n` and double `\n\n` are treated as paragraph separators by the Notion API — there is no "soft line break" (line break within a paragraph); every newline starts a new paragraph block

## Related APIs

- **[Pages](pages.md)** — Create pages with markdown content
- **[Comments](comments.md)** — Create comments with markdown
- **[Blocks](blocks.md)** — Work with individual page blocks
