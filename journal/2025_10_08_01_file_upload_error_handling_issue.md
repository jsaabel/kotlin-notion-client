# File Upload API Error Handling Issue

**Date:** 2025-10-08
**Status:** Resolved ✅
**See also:** `2025_10_08_02_new_file_upload_documentation.md` for documentation discoveries

## Problem

The `FileUploadsExamples` tests were failing with multiple issues:

1. **JSON deserialization errors** when Notion API returns error responses (HTTP 400)
2. **Unsupported file types** (`.md` files not supported by File Upload API)
3. **Invalid external URLs** (placeholder.com URLs that don't exist)
4. **Multi-part upload validation** (parts too small - must be >= 5 MiB each)
5. **Model mismatch** (`expiryTime` should be nullable)

## Root Causes

### Issue 1: JSON Deserialization of Error Responses

The error occurred because:
1. Notion API error responses have `"status": 400` (integer)
2. Ktor's JSON deserializer tried to deserialize error response as `FileUpload`
3. The `FileUpload.status` field expects a `FileUploadStatus` enum, not an integer
4. This caused: `JsonConvertException` before error handling could catch it

**Root Cause:** `FileUploadApi` was calling `.body<FileUpload>()` directly on the response without first checking the HTTP status code. This is different from other working API classes (`PagesApi`, `BlocksApi`, etc.) which use:
```kotlin
val response: HttpResponse = client.post(...)
if (response.status.isSuccess()) {
    response.body<T>()
} else {
    // Handle error
}
```

### Issue 2: Unsupported File Type

Tests used `.md` (Markdown) files, which are **not supported** by Notion's File Upload API, even though they can be uploaded via the Notion UI. The API returned:
```json
{"message": "Provided `filename` has an extension that is not supported for the File Upload API."}
```

### Issue 3: External URL Failures

Tests used `https://via.placeholder.com/` which doesn't exist. Notion couldn't fetch headers from the URL.

### Issue 4: Multi-Part Upload Size Validation

The API requires:
- Each part must be **at least 5 MiB** (except the last part)
- Files > 20 MiB require multi-part upload

The test was creating tiny parts (~1.5 KiB), violating this requirement.

### Issue 5: Nullable `expiryTime`

According to the documentation, once a file upload is attached to a page/block, the `expiry_time` field is removed (becomes `null`). Our model incorrectly defined it as non-nullable.

## Solutions Implemented

### 1. Fixed Error Handling in FileUploadApi ✅

Updated all methods in `FileUploadApi` to use the same pattern as other working API classes:

**Before:**
```kotlin
suspend fun createFileUpload(request: CreateFileUploadRequest): FileUpload =
    client.post("${config.baseUrl}/file_uploads") {
        // headers, body...
    }.body()
```

**After:**
```kotlin
suspend fun createFileUpload(request: CreateFileUploadRequest): FileUpload {
    val response: HttpResponse = client.post("${config.baseUrl}/file_uploads") {
        // headers, body...
    }

    return if (response.status.isSuccess()) {
        response.body<FileUpload>()
    } else {
        val errorBody = try {
            response.body<String>()
        } catch (e: Exception) {
            "Could not read error response body"
        }
        throw NotionException.ApiError(
            code = response.status.value.toString(),
            status = response.status.value,
            details = errorBody,
        )
    }
}
```

Applied this pattern to:
- `createFileUpload()`
- `sendFileUpload()`
- `completeFileUpload()`
- `retrieveFileUpload()`
- `listFileUploads()`

### 2. Made `contentType` Optional in `sendFileUpload()` ✅

**Issue:** Making `contentType` required would break backwards compatibility with existing tests.

**Solution:** Made it optional with smart behavior:
```kotlin
suspend fun sendFileUpload(
    fileUploadId: String,
    fileContent: ByteArray,
    contentType: String? = null,  // Optional!
    partNumber: Int? = null,
): FileUpload
```

- If provided: Explicitly sets Content-Type header
- If omitted: Notion infers from the `createFileUpload` request
- Maintains backwards compatibility with `MediaIntegrationTest`

### 3. Fixed Test File Types ✅

Changed unsupported file types to supported ones:
- `.md` → `.txt` (text/plain is supported)
- Used realistic external URLs: `https://placehold.co/150x150.png`

### 4. Fixed Multi-Part Upload Test ✅

Created properly sized parts:
```kotlin
val fiveMB = 5 * 1024 * 1024
val part1Content = "Part 1 content.\n".repeat(fiveMB / 16).toByteArray() // ~5 MiB
val part2Content = "Part 2 content.\n".repeat(fiveMB / 16).toByteArray() // ~5 MiB
val part3Content = "Part 3 content.\n".repeat((fiveMB + oneMB) / 16).toByteArray() // ~6 MiB
```

### 5. Made `expiryTime` Nullable ✅

Updated the model to reflect API behavior:
```kotlin
@SerialName("expiry_time")
val expiryTime: String? = null,
```

Once a file is attached, `expiry_time` is removed from the response.

## Key Learnings

### Notion API File Upload Constraints

1. **Supported File Extensions:** Only specific file types are supported (see `2025_10_08_02_new_file_upload_documentation.md`)
   - `.md` files are NOT supported (even though UI allows them)
   - `.txt`, `.pdf`, `.jpg`, `.png`, etc. ARE supported

2. **Multi-Part Upload Requirements:**
   - Files > 20 MiB must use multi-part
   - Each part >= 5 MiB (except last)
   - Maximum 1000 parts

3. **External URL Import:**
   - Must use HTTPS
   - URL must be publicly accessible
   - Notion fetches headers first to validate

4. **File Upload Lifecycle:**
   - New uploads have `expiry_time` (1 hour)
   - After attachment, `expiry_time` becomes `null`
   - Can't delete/revoke uploads after creation

### Error Handling Pattern

The correct Ktor pattern for handling API errors:
1. Get `HttpResponse` first (don't call `.body()` immediately)
2. Check `response.status.isSuccess()`
3. Deserialize as success type OR error type based on status
4. This prevents JSON deserialization errors

### Backwards Compatibility Considerations

When adding parameters to existing APIs:
- Make new parameters optional with sensible defaults
- Test with existing integration tests to verify compatibility
- Document the optional behavior clearly

## Test Results

All `FileUploadsExamples` tests now pass:
- ✅ Enhanced API - Upload from File object
- ✅ Enhanced API - Upload from Path
- ✅ Enhanced API - Upload from byte array
- ✅ Enhanced API - Upload with progress tracking
- ✅ Enhanced API - Import from external URL
- ✅ Basic API - Single-part upload using DSL
- ✅ Basic API - External URL import using DSL
- ✅ Basic API - Multi-part upload using DSL
- ✅ Basic API - List and retrieve operations
- ✅ Using uploaded files in pages
- ✅ Error handling - File validation
- ✅ Error handling - Invalid external URL
- ✅ Basic API - Convenience upload methods

## Files Modified

### API Layer
- `src/main/kotlin/no/saabelit/kotlinnotionclient/api/FileUploadApi.kt`
  - Added proper error handling to all methods
  - Made `contentType` optional in `sendFileUpload()`

### Models
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/files/FileUpload.kt`
  - Made `expiryTime` nullable

### Tests
- `src/test/kotlin/examples/FileUploadsExamples.kt`
  - Changed `.md` files to `.txt`
  - Updated external URLs to use `placehold.co`
  - Fixed multi-part upload to use properly sized parts

### Integration Tests (Verified Still Working)
- `src/test/kotlin/integration/MediaIntegrationTest.kt` ✅
- `src/test/kotlin/unit/api/EnhancedFileUploadApiTest.kt` ✅

## Impact

- **Better Error Messages:** API errors now throw proper `NotionException.ApiError` with details
- **Model Accuracy:** `FileUpload` model correctly handles attached files
- **Test Reliability:** All file upload examples now work against live API
- **Documentation Ready:** Tests can now serve as validated examples for documentation

## Next Steps

1. Update user-facing documentation with supported file types
2. Consider adding file type validation with helpful error messages
3. Document the multi-part upload size requirements
4. Add note about `.md` file limitation (UI vs API discrepancy)