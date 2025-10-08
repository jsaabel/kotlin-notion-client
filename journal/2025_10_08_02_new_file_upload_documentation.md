# New File Upload Documentation Discovery and Validation Improvements

**Date:** 2025-10-08
**Status:** In Progress
**Related:** File Upload API, Validation, Documentation

## Discovery

Notion has recently updated their file upload documentation with comprehensive guides in:
`reference/notion-api/documentation/notion_api/working_with_files_and_media/`

This new documentation provides crucial information that was previously unclear or missing from the API reference.

## Key Findings

### 1. **Supported File Types (FINALLY DOCUMENTED!)**

The API explicitly lists supported file types by category:

**Audio:**
- Extensions: `.aac`, `.adts`, `.mid`, `.midi`, `.mp3`, `.mpga`, `.m4a`, `.m4b`, `.mp4`, `.oga`, `.ogg`, `.wav`, `.wma`
- MIME types: `audio/aac`, `audio/midi`, `audio/mpeg`, `audio/mp4`, `audio/ogg`, `audio/wav`, `audio/x-ms-wma`

**Document:**
- Extensions: `.pdf`, `.txt`, `.json`, `.doc`, `.dot`, `.docx`, `.dotx`, `.xls`, `.xlt`, `.xla`, `.xlsx`, `.xltx`, `.ppt`, `.pot`, `.pps`, `.ppa`, `.pptx`, `.potx`
- MIME types: `application/pdf`, `text/plain`, `application/json`, `application/msword`, `application/vnd.openxmlformats-officedocument.*`

**Image:**
- Extensions: `.gif`, `.heic`, `.jpeg`, `.jpg`, `.png`, `.svg`, `.tif`, `.tiff`, `.webp`, `.ico`
- MIME types: `image/gif`, `image/heic`, `image/jpeg`, `image/png`, `image/svg+xml`, `image/tiff`, `image/webp`, `image/vnd.microsoft.icon`

**Video:**
- Extensions: `.amv`, `.asf`, `.wmv`, `.avi`, `.f4v`, `.flv`, `.gifv`, `.m4v`, `.mp4`, `.mkv`, `.webm`, `.mov`, `.qt`, `.mpeg`
- MIME types: `video/x-amv`, `video/x-ms-asf`, `video/x-msvideo`, `video/x-f4v`, `video/x-flv`, `video/mp4`, `application/mp4`, `video/webm`, `video/quicktime`, `video/mpeg`

**Notable Exclusion:** `.md` (Markdown) files are NOT supported by the File Upload API, even though they can be uploaded via the Notion UI. This explains the validation error we encountered in the FileUploadsExamples tests.

### 2. **File Size Limits**

- Free workspaces: 5 MiB per file
- Paid workspaces: 5 GiB per file
- Files > 20 MiB must use multi-part upload
- Workspace limits can be retrieved via the User API:
  ```json
  {
    "bot": {
      "workspace_limits": {
        "max_file_upload_size_in_bytes": 5242880
      }
    }
  }
  ```

### 3. **Filename Length Limit**

Maximum 900 bytes (including extension). The docs recommend shorter names for performance and easier management.

### 4. **Upload Lifecycle**

- Files have a 1-hour `expiry_time` from creation
- Must be attached within 1 hour or they'll be archived
- Once attached, `expiry_time` is removed and file becomes permanent
- File upload IDs can be reused after attachment (even if original content is deleted)
- No way to delete or revoke a file upload after creation

### 5. **Download URLs**

- Temporary download URLs expire after 1 hour
- Must re-fetch the page/block/database to refresh URLs

## Implications for Our Implementation

### Current State
Our implementation has:
- ✅ Multi-part upload support
- ✅ External URL import support
- ✅ Basic validation (empty filename, HTTPS URLs)
- ✅ Proper error handling (as of today's fix)
- ❌ No file type validation
- ❌ No file size validation
- ❌ No filename length validation
- ❌ No workspace limit awareness

### Recommended Improvements

#### 1. File Type Validation

Create a comprehensive file type validator:

```kotlin
// In FileUploadUtils.kt or new FileTypeValidator.kt
object FileTypeValidator {
    private val SUPPORTED_EXTENSIONS = setOf(
        // Audio
        "aac", "adts", "mid", "midi", "mp3", "mpga", "m4a", "m4b", "mp4",
        "oga", "ogg", "wav", "wma",
        // Document
        "pdf", "txt", "json", "doc", "dot", "docx", "dotx", "xls", "xlt",
        "xla", "xlsx", "xltx", "ppt", "pot", "pps", "ppa", "pptx", "potx",
        // Image
        "gif", "heic", "jpeg", "jpg", "png", "svg", "tif", "tiff", "webp", "ico",
        // Video
        "amv", "asf", "wmv", "avi", "f4v", "flv", "gifv", "m4v", "mkv",
        "webm", "mov", "qt", "mpeg"
    )

    private val MIME_TO_EXTENSIONS = mapOf(
        "text/plain" to "txt",
        "application/json" to "json",
        "application/pdf" to "pdf",
        // ... etc
    )

    fun validateFileExtension(filename: String): ValidationResult {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return if (extension in SUPPORTED_EXTENSIONS) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                "File extension '.$extension' is not supported by Notion's File Upload API. " +
                "Supported extensions: ${SUPPORTED_EXTENSIONS.sorted().joinToString(", ") { ".$it" }}"
            )
        }
    }

    fun inferExtensionFromContentType(contentType: String): String? {
        return MIME_TO_EXTENSIONS[contentType]
    }
}
```

#### 2. Enhanced Filename Validation

Update `FileUploadUtils.validateFilename()` to check:
- Not empty ✅ (already done)
- Contains extension
- Extension is supported (NEW)
- Total length ≤ 900 bytes (NEW)

#### 3. File Size Validation

```kotlin
// Add to FileUploadUtils
fun validateFileSize(
    sizeBytes: Long,
    maxSizeBytes: Long = 5 * 1024 * 1024 // 5 MiB default for free tier
): ValidationResult {
    return when {
        sizeBytes <= 0 -> ValidationResult.Invalid("File size must be greater than 0")
        sizeBytes > maxSizeBytes -> ValidationResult.Invalid(
            "File size (${sizeBytes.formatBytes()}) exceeds workspace limit (${maxSizeBytes.formatBytes()}). " +
            "For paid workspaces, the limit is 5 GiB."
        )
        else -> ValidationResult.Valid
    }
}
```

#### 4. Workspace Limits Support

Add methods to retrieve and cache workspace limits:
```kotlin
// In NotionClient or new WorkspaceInfo API
suspend fun getWorkspaceLimits(): WorkspaceLimits {
    // Retrieve bot user info and extract limits
}
```

#### 5. Documentation Updates

Add a prominent note in our documentation:
- List supported file types
- Explain that `.md` files work in UI but not API
- Document size limits
- Explain expiry and lifecycle

## Test Fixes Required

The FileUploadsExamples tests were failing because:
1. Tests used `.md` files (not supported by API)
2. No validation was catching this before the API call

**Solution:** Changed test files from `.md` to `.txt` (supported type)

**Better Solution:** Add validation that would have caught this client-side with a clear error message.

## Next Steps

1. ✅ Fix FileUploadsExamples tests (done - changed .md to .txt)
2. 🔲 Add comprehensive file type validation
3. 🔲 Add filename length validation
4. 🔲 Add file size validation with workspace awareness
5. 🔲 Update documentation to include supported file types
6. 🔲 Consider adding a helper to suggest correct extensions
7. 🔲 Add validation tests for unsupported file types

## Implementation Priority

**High Priority:**
- File type validation (prevents confusing API errors)
- Filename length validation (prevents 400 errors)

**Medium Priority:**
- File size validation (helpful but API also validates)
- Workspace limit awareness (advanced feature)

**Low Priority:**
- Extension inference from MIME type (nice-to-have)

## Documentation Location

All new file upload documentation is in:
```
reference/notion-api/documentation/notion_api/working_with_files_and_media/
├── 01_Intro.md (supported types, limits, overview)
├── 02_Uploading_Small_Files.md (single-part upload flow)
├── 03_Retrieving_Existing_Files.md
├── 04_Uploading_Larger_Files.md (multi-part upload)
└── 05_Importing_External_Files.md (external URL import)
```

## Benefits of These Improvements

1. **Better Developer Experience:** Clear errors before hitting the API
2. **Reduced API Calls:** Catch invalid requests client-side
3. **Educational:** Help developers understand Notion's limitations
4. **Alignment with API:** Match validation logic with actual API behavior
5. **Documentation Accuracy:** Our docs can reference official supported types

## Open Questions

1. Should we add a "strict" vs "permissive" validation mode?
2. Should validation be opt-in or opt-out? (Currently it's opt-in via `validateBeforeUpload`)
3. Should we auto-add extensions based on content type?
4. Should we provide warnings for files near size limits?

## References

- New Documentation: `reference/notion-api/documentation/notion_api/working_with_files_and_media/01_Intro.md`
- File Upload API: `src/main/kotlin/no/saabelit/kotlinnotionclient/api/FileUploadApi.kt`
- Enhanced API: `src/main/kotlin/no/saabelit/kotlinnotionclient/api/EnhancedFileUploadApi.kt`
- Validation Utils: `src/main/kotlin/no/saabelit/kotlinnotionclient/utils/FileUploadUtils.kt`