# 2026-05-30: Gap analysis — programmatically filling a files/media page property

### Context
Investigated end-to-end support for setting a "Files & media" property on a database row from
code. Triggered while exploring the file-upload notebook (`notebooks/07-file-uploads.ipynb`).
This entry is a scoping note for a future release — no code changes yet.

### What works today
- **External URLs**: `PagePropertyValue.FilesValue(files = listOf(FileObject.External(name, ExternalFileUrl(url))))`
  serializes to the shape the API accepts on POST/PATCH page property updates.
  Verified against `reference/notion-api/documentation/objects/02_Page_PageProperties.md:184-198`.
- **File Upload API**: `FileUploadApi` (`src/main/kotlin/.../api/FileUploadApi.kt`) is complete —
  `createFileUpload` → `sendFileUpload` → `completeFileUpload`, plus single-part and multi-part
  `uploadFile()` helpers. So we *can* obtain a `file_upload` ID.
- **Reading**: `PageProperty.Files` + `FileData.External` / `FileData.Uploaded`
  (`PageProperty.kt:201-382`) deserialize file properties returned by the API correctly.

### Critical gap — cannot attach an uploaded file to a files property
The API expects this shape when attaching a freshly-uploaded file to a files property
(docs `02_Page_PageProperties.md:182` plus the analogous icon example at `:301-309`):

```json
{ "type": "file_upload", "file_upload": { "id": "<upload-id>" }, "name": "..." }
```

Our `FileObject` sealed class (`PageRequests.kt:457-476`) only has:
- `External` → `{ "type": "external", "external": { url } }` ✓
- `Uploaded` → `{ "type": "file", "file": { url, expiry_time } }` — this is the **response**
  shape (Notion-hosted URL with expiry), not a valid request shape for attaching a new upload.
  It only works for echoing an existing file back unchanged on update; it cannot reference
  a freshly-uploaded `file_upload` ID because we don't have the URL yet.

There is **no `FileObject.FileUpload(id)` variant** to emit `{ "type": "file_upload",
"file_upload": { "id" } }`. For comparison, the block models *do* have this — see
`ImageRequestContent` / `FileRequestContent` etc. in `BlockRequest.kt:548-612`, each carrying
a `fileUpload: FileUploadReference?` field. `PageCover` (`PageCover.kt:56-59`) and `Icon`
(`Icon.kt:88-91`) also have the variant. The page-property files type is the odd one out.

### Secondary gaps
- **No DSL builder method**: `PagePropertiesBuilder` covers ~20 property types but has no
  `files(...)` method. Users must construct `PagePropertyValue.FilesValue(...)` manually.
- **No test coverage**: `MediaIntegrationTest.kt` exercises file uploads into *blocks*, not
  into a files property. The filter test (`NewFilterTypesIntegrationTest.kt:88`) comments
  that the property must be populated via the UI — no end-to-end programmatic-fill test exists.

### Bottom line — three user scenarios
- (a) Attach an external URL → **works**.
- (b) Upload a local file and attach it to a files property → **half-works**: upload succeeds,
  attachment is blocked by the missing `FileObject.FileUpload(id)` variant.
- (c) Reference an already-uploaded Notion file → only by echoing back the full file object
  from a prior GET (preservation on update). Cannot reference by ID alone.

### Proposed work for next release
1. Add `FileObject.FileUpload(id: String, name: String? = null)` as a third sealed subtype,
   serializing to `{ "type": "file_upload", "file_upload": { "id" }, "name"? }`. Reuse the
   existing `FileUploadReference` data class from the blocks side if shape matches.
2. Add a `files(name: String, block: FilesBuilder.() -> Unit)` (or simpler overload) to
   `PagePropertiesBuilder`, with helpers for external URL, file-upload ID, and re-attaching
   an existing `FileData.Uploaded`.
3. Integration test: upload a small file via `FileUploadApi`, then create/update a database
   row attaching the upload to a files property, then re-fetch and assert the file appears.
   Mirror the pattern in `MediaIntegrationTest` but target a database row instead of a block.
4. Sample fixture in `src/test/resources/api/pages/` showing the request body, plus a unit
   test for serialization of the new `FileObject.FileUpload` variant.

### References
- Models: `src/main/kotlin/.../models/pages/PageRequests.kt:340-490`
- Builder: `src/main/kotlin/.../models/pages/PagePropertiesBuilder.kt`
- Block analogue: `src/main/kotlin/.../models/blocks/BlockRequest.kt:548-612`
- Upload API: `src/main/kotlin/.../api/FileUploadApi.kt`
- Notion docs: `reference/notion-api/documentation/objects/02_Page_PageProperties.md:169-227`
