# Custom Emojis API

The Custom Emojis API lets you enumerate the custom emojis available in your workspace.

**Entry point**: `client.customEmojis`

## Available Operations

```kotlin
suspend fun list(
    startCursor: String? = null,
    pageSize: Int? = null,
    name: String? = null,
): CustomEmojiList

fun listAsFlow(name: String? = null): Flow<CustomEmojiObject>
fun listPagedFlow(name: String? = null): Flow<CustomEmojiList>
```

## Examples

### List All Custom Emojis

```kotlin
notion.customEmojis.listAsFlow().collect { emoji ->
    println("${emoji.name}: ${emoji.url}")
}
```

### Find a Custom Emoji by Name

```kotlin
val results = notion.customEmojis.list(name = "bufo")
val emoji = results.results.firstOrNull()
    ?: error("Emoji not found")

// Use it as a page icon
notion.pages.create {
    parent.page("page-id")
    title("Bufo Page")
    icon.customEmoji(emoji.id)
}
```

### Paginated Listing

```kotlin
val firstPage = notion.customEmojis.list(pageSize = 50)
println("Custom emojis (first page): ${firstPage.results.size}")
```

## Notes

- Custom emojis are workspace-scoped — available to any integration with workspace-level access
- The `name` filter performs an exact match
- This API is read-only; custom emojis must be created in the Notion UI

## Related APIs

- **[Pages](pages.md)** — Use `icon.customEmoji(emojiId)` to set a custom emoji as a page icon
- **[Databases](databases.md)** — Same `icon.customEmoji(emojiId)` pattern applies to databases
