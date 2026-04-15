# Development Journal - April 8, 2026

## Comment Markdown Support

### Objective

Add support for the new `markdown` field on the Create Comment endpoint. The Notion API now accepts
either `rich_text` (array) or `markdown` (string) as the comment body — exactly one must be provided.

---

## API Change

The `POST /v1/comments` body now accepts:

| Field | Type | Notes |
|---|---|---|
| `rich_text` | `List<RichTextObject>` | Existing format |
| `markdown` | `String` | New — inline formatting, equations, mentions |

Exactly one of the two must be provided. Providing both or neither returns a validation error from the API.

Supported inline markdown: bold, italic, strikethrough, inline code, links, inline equations, mentions.

---

## Changes Made

### Models (`Comment.kt`)

- `CreateCommentRequest.richText`: `List<RichText>` → `List<RichText>? = null`
- `CreateCommentRequest.markdown`: new `String? = null` field
- Both fields are omitted from JSON when null (`explicitNulls = false` in the client config)

### API (`CommentsApi.create()`)

Replaced the old `richText.isEmpty()` guard with mutual-exclusion validation:
- Both set → `IllegalArgumentException("Comment content must use either rich_text or markdown, not both")`
- Neither set → `IllegalArgumentException("Comment content cannot be empty — provide either rich_text or markdown")`

### Builder (`CreateCommentRequestBuilder`)

- Added `markdownValue: String?` field
- Added `markdown(content: String)` DSL method
- `build()` enforces mutual exclusion; passes `richText = null` when markdown is used

### Tests

- `CreateCommentRequestBuilderTest`: updated error-message assertions; added `markdown content` describe block (markdown creation, markdown + optional fields, mutual exclusion rejection)
- `CommentsApiTest`: updated "empty rich text" test to match new message; added mutual-exclusion rejection test and markdown JSON serialization test
- `CommentsIntegrationTest`: updated old exact-message assertion; added `testing markdown comments` scenario covering typed request + DSL overload
- `CommentsExamples`: added Example 3b using the `markdown()` DSL method

### Kotlin compiler quirk encountered

`request.richText` (typed `List<RichText>?`) triggered contradictory compiler behaviour: `!!` was required to compile, but triggered "unnecessary !!" warnings (because `.shouldNotBeNull()` earlier in the test smart-casts the property). Fixed by replacing `request.richText!![N]` with `checkNotNull(request.richText)[N]` — no warning, compiles cleanly.

---

## Status

- [x] `CreateCommentRequest` — `richText` nullable, `markdown` field added
- [x] `CommentsApi.create()` — mutual-exclusion validation
- [x] `CreateCommentRequestBuilder` — `markdown()` DSL method, `build()` guards
- [x] Unit tests — all passing, no warnings
- [x] Integration test scenario added — pending live API validation
- [x] Example added
