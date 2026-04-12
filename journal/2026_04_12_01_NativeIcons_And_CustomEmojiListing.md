# Development Journal - April 12, 2026

## Native Icons + Custom Emoji Listing (Phase 6 continued)

### Context

This journal captures the implementation plan for two Phase 6 features:
1. **Native Notion icons** (`type: "icon"`) — new icon variant for pages, databases, and callout/tab blocks
2. **Custom emoji listing** — new `GET /v1/custom_emojis` endpoint

---

## Prerequisite Completed: `PageIcon` → `Icon` rename + `CalloutIcon` elimination

### What was done (April 12, 2026)

Renamed `PageIcon` → `Icon` and eliminated `CalloutIcon` as a unified refactor before implementing the new features.

**Key decisions made during implementation:**

- **Name chosen: `Icon` (not `NotionIcon`)** — accepted the minor import-collision tradeoff for cleaner call sites. Pre-1.0 library, right time for this.
- **`type` serialization** — `Icon` subtypes use a secondary constructor for the clean public API (e.g. `Icon.Emoji("🥑")`) while the primary constructor keeps `@SerialName("type")` as a required field, ensuring the discriminator is always serialized without needing `@EncodeDefault` (experimental).
- **`FileReference` kept** — used for block media content (image, video, audio, file, PDF blocks); only `CalloutIcon`'s icon usage was removed.
- **`RequestBuilders.createEmojiIcon()` / `createExternalIcon()` removed** — redundant wrappers; all 18 call sites replaced with direct `Icon.Emoji(emoji = "...")` construction.

**Files created:**
- `models/base/Icon.kt` — unified sealed class (moved from `models/pages/PageIcon.kt`)
- `models/base/IconSerializer.kt` — moved from `models/pages/PageIconSerializer.kt`

**Files deleted:**
- `models/pages/PageIcon.kt`
- `models/pages/PageIconSerializer.kt`

**Files updated:** 13 source files + 11 test files

**Build status:** All unit tests passing ✅

---

It also documents the prerequisite rename: **`PageIcon` → `Icon`** (eliminating `CalloutIcon` as a side effect).

This journal was written *before* implementation began so that context is not lost when starting a fresh session.

---

## Prerequisite: `PageIcon` → `Icon` rename + `CalloutIcon` elimination

### Why

There are currently two icon types in the library:

| Type | Shape | Used for |
|---|---|---|
| `PageIcon` | Sealed class with proper subtypes | Pages, databases |
| `CalloutIcon` | Flat data class with nullable `type: String` | Callout blocks, tab paragraph children |

Both names are wrong: `PageIcon` is also used for databases; `CalloutIcon` is also used for tab panes. The JSON structure for icons is **identical** everywhere in the Notion API — same `type` discriminator, same shapes — so there is no reason for two separate types.

**Decision:** Rename `PageIcon` → `Icon`, use it everywhere, delete `CalloutIcon`.

The name `Icon` (not `NotionIcon`) was chosen deliberately, accepting the minor import-collision tradeoff in user projects in favour of cleaner call sites (`Icon.Emoji`, `Icon.NativeIcon`, `Icon.External`). This is a pre-1.0 library, and 0.4 is not yet published.

### Files to rename

- `models/pages/PageIcon.kt` → `models/base/Icon.kt`
  - Move to `base/` since it's no longer page-specific
- `models/pages/PageIconSerializer.kt` → `models/base/IconSerializer.kt`

### Files to update (all call sites)

Replace `PageIcon` → `Icon` and `CalloutIcon` → `Icon` throughout:

| File | Change |
|---|---|
| `models/pages/Page.kt` | `val icon: PageIcon?` → `val icon: Icon?` |
| `models/pages/PageRequests.kt` | `val icon: PageIcon?` (×2) → `val icon: Icon?` |
| `models/pages/CreatePageRequestBuilder.kt` | `PageIcon` → `Icon` throughout |
| `models/pages/UpdatePageRequestBuilder.kt` | `PageIcon` → `Icon` throughout |
| `models/databases/Database.kt` | `val icon: PageIcon?` → `val icon: Icon?` |
| `models/databases/DatabaseRequests.kt` | `val icon: PageIcon?` → `val icon: Icon?` |
| `models/databases/DatabaseRequestBuilder.kt` | `PageIcon` → `Icon` throughout |
| `models/requests/RequestBuilders.kt` | `PageIcon` → `Icon` in helpers |
| `models/blocks/Block.kt` | `CalloutIcon` → `Icon`; delete `CalloutIcon` data class definition |
| `models/blocks/BlockRequest.kt` | `CalloutIcon` → `Icon`; remove comment about `CalloutIcon` |
| `models/blocks/PageContentBuilder.kt` | `CalloutIcon` → `Icon`; update `emoji()` helper return type |

Also update all test files that reference `PageIcon` or `CalloutIcon`.

### `CalloutIcon` → `Icon` structural change

`CalloutIcon` is currently a flat struct:
```kotlin
data class CalloutIcon(
    val type: String,
    val emoji: String? = null,
    val external: ExternalFile? = null,
    val file: FileReference? = null,
)
```

After the rename, block icon fields switch to the sealed `Icon` class. The `emoji("💡")` top-level DSL helper changes its return type:
```kotlin
// Before
fun emoji(emojiChar: String): CalloutIcon = CalloutIcon(type = "emoji", emoji = emojiChar)

// After
fun emoji(emojiChar: String): Icon = Icon.Emoji(emoji = emojiChar)
```

`FileReference` (currently used only by `CalloutIcon`) can be removed if `Icon.File` already uses `NotionFile`. Check and consolidate.

---

## Feature 1: Native Notion Icons (`type: "icon"`)

### API shape (from docs)

```json
{
  "type": "icon",
  "icon": {
    "name": "pizza",
    "color": "blue"
  }
}
```

Valid colors: `"gray"` (default), `"lightgray"`, `"brown"`, `"yellow"`, `"orange"`, `"green"`, `"blue"`, `"purple"`, `"pink"`, `"red"`.

When writing: `color` is optional (defaults to `"gray"`). `name` is required.

### Step 1 — `NativeIconObject` data class (`models/base/FileTypes.kt` or `models/base/Icon.kt`)

```kotlin
@Serializable
data class NativeIconObject(
    @SerialName("name") val name: String,
    @SerialName("color") val color: String? = null,
)
```

Place alongside `CustomEmojiObject` in `FileTypes.kt`, or co-locate in `Icon.kt` — either works.

### Step 2 — `Icon.NativeIcon` subtype (`models/base/Icon.kt`)

Add to the `Icon` sealed class:
```kotlin
@Serializable
data class NativeIcon(
    @SerialName("icon") val icon: NativeIconObject,
) : Icon() {
    override val type: String = "icon"
}
```

### Step 3 — Update `IconSerializer`

Add one branch to the `when`:
```kotlin
"icon" -> Icon.NativeIcon.serializer()
```

### Step 4 — DSL helpers

Add a top-level `nativeIcon()` function in `PageContentBuilder.kt` (alongside `emoji()`):
```kotlin
fun nativeIcon(name: String, color: String? = null): Icon =
    Icon.NativeIcon(NativeIconObject(name = name, color = color))
```

Add `native(name, color?)` method to the `IconBuilder` inner class in the three page/database builders:
- `CreatePageRequestBuilder.IconBuilder`
- `UpdatePageRequestBuilder.IconBuilder`
- `DatabaseRequestBuilder.IconBuilder`

```kotlin
fun native(name: String, color: String? = null) {
    this@XxxBuilder.iconValue = Icon.NativeIcon(NativeIconObject(name = name, color = color))
}
```

### Step 5 — Unit tests

Add to existing icon tests (or a new `NativeIconTest`):
- `Icon.NativeIcon` serializes to `{"type":"icon","icon":{"name":"pizza","color":"blue"}}`
- `Icon.NativeIcon` with no color serializes without `color` key (or `null`)
- Round-trip deserialization from the doc example JSON
- `nativeIcon()` DSL helper returns the correct `Icon.NativeIcon` instance

---

## Feature 2: Custom Emoji Listing (`GET /v1/custom_emojis`)

### API shape (from docs)

Request: `GET /v1/custom_emojis?name=bufo` (optional `name` for exact-match, plus standard `start_cursor` / `page_size`)

Response:
```json
{
  "object": "list",
  "type": "custom_emoji",
  "results": [
    {
      "id": "45ce454c-d427-4f53-9489-e5d0f3d1db6b",
      "name": "bufo",
      "url": "https://..."
    }
  ],
  "has_more": false,
  "next_cursor": null
}
```

`CustomEmojiObject` (in `FileTypes.kt`) already has `id`, `name?`, `url?` — this matches perfectly, except `name` and `url` should perhaps be non-optional here since the list endpoint always returns them. Keep them as `String?` for safety (consistent with existing model).

### Step 1 — `CustomEmojiList` response model

Add to `FileTypes.kt` (or a new file alongside `CustomEmojiObject`):
```kotlin
@Serializable
data class CustomEmojiList(
    @SerialName("results") val results: List<CustomEmojiObject>,
    @SerialName("next_cursor") override val nextCursor: String? = null,
    @SerialName("has_more") override val hasMore: Boolean = false,
) : PaginatedResponse<CustomEmojiObject>
```

### Step 2 — `CustomEmojisApi` (`api/CustomEmojisApi.kt`)

New API class following the same pattern as `UsersApi`:
```kotlin
class CustomEmojisApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    suspend fun list(
        startCursor: String? = null,
        pageSize: Int? = null,
        name: String? = null,
    ): CustomEmojiList

    fun listAsFlow(name: String? = null): Flow<CustomEmojiObject>
    fun listPagedFlow(name: String? = null): Flow<CustomEmojiList>
}
```

URL construction: `GET /v1/custom_emojis?start_cursor=...&page_size=...&name=...`

`pageSize` validation: must be between 1 and 100 (consistent with `UsersApi`).

### Step 3 — Register on `NotionClient`

In `NotionClient.kt`, alongside the other APIs:
```kotlin
val customEmojis = CustomEmojisApi(httpClient, config)
```

### Step 4 — Unit tests (`unit/api/CustomEmojisApiTest.kt`)

- Deserialize the doc example response correctly into `CustomEmojiList`
- `name` query param is added to URL when provided
- `name` query param is absent when not provided
- Pagination fields (`hasMore`, `nextCursor`) deserialize correctly
- Empty results list deserializes correctly

---

## Implementation Order

1. **Rename pass** — `PageIcon` → `Icon`, eliminate `CalloutIcon`, move files to `base/`
2. **Build + format + unit tests** — confirm clean baseline
3. **`NativeIconObject` + `Icon.NativeIcon`** — model + serializer
4. **DSL helpers** — `nativeIcon()` in `PageContentBuilder`, `native()` in three `IconBuilder`s
5. **Icon unit tests**
6. **`CustomEmojiList` model**
7. **`CustomEmojisApi`** — `list()` + flow helpers
8. **Register on `NotionClient`**
9. **Custom emoji unit tests**
10. **Format + build + unit test run** — confirm clean
11. **Integration test** (optional — if workspace has custom emojis) — `list()` returns results

---

## Open Questions / Notes

- `FileReference` (currently only used by `CalloutIcon` in `Block.kt`) — check if it can be removed after the rename, or if `Icon.File` already uses it/something equivalent.
- Native icon `color` when reading: if Notion returns `"gray"` explicitly vs. omitting it entirely, the nullable `color: String?` handles both.
- The `object` and `type` fields in the `CustomEmojiList` response (`"object": "list"`, `"type": "custom_emoji"`) — add to the model or ignore? `PaginatedResponse` interface doesn't require them; ignoring is fine since `ignoreUnknownKeys = true`.