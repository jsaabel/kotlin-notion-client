# File Upload API

Upload files to your Notion workspace using the Kotlin client. The client provides both simple, automated uploads and advanced control options to fit different use cases.

## Quick Start

For most file upload needs, use the enhanced API which handles all the complexity automatically:

```kotlin
// Upload any file - automatic optimization
val result = notion.enhancedFileUploads.uploadFile(
    file = File("presentation.pdf")
)

when (result) {
    is FileUploadResult.Success -> {
        println("File uploaded successfully: ${result.fileUpload.id}")
        // Use result.fileUpload.id to reference the file in pages/blocks
    }
    is FileUploadResult.Failure -> {
        println("Upload failed: ${result.error.message}")
    }
}
```

## When to Use Which API

### Enhanced API (`notion.enhancedFileUploads`) - **Recommended**
✅ **Use when:**
- Uploading files from File/Path objects
- You want automatic single-part vs multi-part detection
- You need progress tracking
- You want retry logic and error recovery
- File size optimization matters to you

### Basic API (`notion.fileUploads`) - **Advanced Control**
✅ **Use when:**
- You need precise control over upload parameters
- Building your own upload logic
- Working with the low-level Notion API directly
- You prefer explicit configuration

---

## Enhanced File Upload API

The enhanced API provides intelligent, production-ready file uploads with minimal configuration.

### Upload from File Objects

```kotlin
// From File
val result = notion.enhancedFileUploads.uploadFile(
    file = File("document.pdf")
)

// From Path
val result = notion.enhancedFileUploads.uploadFile(
    path = Paths.get("images/photo.jpg")
)

// From byte array
val result = notion.enhancedFileUploads.uploadFile(
    filename = "data.json",
    data = """{"key": "value"}""".toByteArray()
)

// From InputStream (when you know the size)
val result = notion.enhancedFileUploads.uploadFile(
    filename = "stream-data.bin",
    sizeBytes = 1024,
    streamProvider = { FileInputStream("source.bin") }
)
```

### Upload Options and Progress Tracking

```kotlin
val options = FileUploadOptions(
    // Override detected content type if needed
    contentType = "application/pdf",
    
    // Validate file before upload
    validateBeforeUpload = true,
    
    // Enable concurrent parts for faster large file uploads
    enableConcurrentParts = true,
    maxConcurrentParts = 3,
    
    // Track upload progress
    progressCallback = { progress ->
        println("Uploading ${progress.filename}: ${progress.percentComplete}%")
        
        // For multi-part uploads, show part progress
        if (progress.totalParts != null) {
            println("Part ${progress.currentPart}/${progress.totalParts}")
        }
        
        // Handle different upload phases
        when (progress.status) {
            UploadProgressStatus.STARTING -> println("Starting upload...")
            UploadProgressStatus.UPLOADING -> print(".")
            UploadProgressStatus.COMPLETING -> println("\nFinalizing...")
            UploadProgressStatus.COMPLETED -> println("Done!")
        }
    },
    
    // Configure retry behavior
    retryConfig = RetryConfig(
        maxRetries = 3,
        baseDelayMs = 1000
    )
)

val result = notion.enhancedFileUploads.uploadFile(
    file = File("large-presentation.pptx"),
    options = options
)
```

### Import from External URLs

```kotlin
val result = notion.enhancedFileUploads.importExternalFile(
    filename = "remote-image.jpg",
    externalUrl = "https://example.com/images/photo.jpg",
    contentType = "image/jpeg", // optional - can be auto-detected
    options = FileUploadOptions(
        validateBeforeUpload = true,
        progressCallback = { progress ->
            println("Importing: ${progress.status}")
        }
    )
)

// Wait for external import to complete processing
if (result is FileUploadResult.Success) {
    val readyUpload = notion.enhancedFileUploads.waitForFileReady(
        fileUploadId = result.uploadId,
        maxWaitTimeMs = 30000 // 30 seconds
    )
    println("File ready for use: ${readyUpload.id}")
}
```

### Error Handling

```kotlin
val result = notion.enhancedFileUploads.uploadFile(file = File("document.pdf"))

when (result) {
    is FileUploadResult.Success -> {
        println("✅ Upload successful!")
        println("   File ID: ${result.fileUpload.id}")
        println("   Upload time: ${result.uploadTimeMs}ms")
    }
    
    is FileUploadResult.Failure -> {
        println("❌ Upload failed: ${result.filename}")
        
        when (val error = result.error) {
            is FileUploadError.ValidationError -> 
                println("   Validation: ${error.message}")
                
            is FileUploadError.FileSizeError -> 
                println("   File too large: ${error.message}")
                
            is FileUploadError.NetworkError -> 
                println("   Network issue: ${error.message}")
                
            is FileUploadError.RateLimitError -> {
                println("   Rate limited - retry after ${error.retryAfterMs}ms")
                // Could implement automatic retry here
            }
                
            is FileUploadError.TimeoutError -> 
                println("   Timeout during: ${error.operation}")
                
            else -> 
                println("   Unexpected error: ${error.message}")
        }
    }
}
```

---

## Basic File Upload API

The basic API provides direct access to Notion's file upload endpoints with clean DSL syntax.

### Single File Upload (< 20MB)

```kotlin
// Using the DSL
val fileUpload = notion.fileUploads.createFileUpload {
    filename("presentation.pdf")
    contentType("application/pdf")
}

// Send the file content
val uploadedFile = notion.fileUploads.sendFileUpload(
    fileUploadId = fileUpload.id,
    fileContent = File("presentation.pdf").readBytes()
)

println("File uploaded: ${uploadedFile.id}")
```

### Multi-Part Upload (> 20MB)

```kotlin
// IMPORTANT: Each part must be >= 5 MiB (except the last part)
val file = File("large-video.mp4")
val fileSize = file.length()

// Calculate number of parts ensuring each is >= 5 MiB
val fiveMiB = 5 * 1024 * 1024L
val numberOfParts = ((fileSize + fiveMiB - 1) / fiveMiB).toInt().coerceAtLeast(1)

// Create multi-part upload using DSL
val fileUpload = notion.fileUploads.createFileUpload {
    multiPart()
    filename("large-video.mp4")
    contentType("video/mp4")
    numberOfParts(numberOfParts)
}

// Split and upload each part
val fileBytes = file.readBytes()
val partSize = fileSize / numberOfParts

for (partNumber in 1..numberOfParts) {
    val startIdx = ((partNumber - 1) * partSize).toInt()
    val endIdx = if (partNumber == numberOfParts) fileBytes.size else (partNumber * partSize).toInt()
    val partContent = fileBytes.sliceArray(startIdx until endIdx)

    notion.fileUploads.sendFileUpload(
        fileUploadId = fileUpload.id,
        fileContent = partContent,
        partNumber = partNumber
    )

    println("Uploaded part $partNumber/$numberOfParts")
}

// Complete the multi-part upload
val completedUpload = notion.fileUploads.completeFileUpload(fileUpload.id)
```

### External URL Import

```kotlin
// Import from external URL using DSL
val fileUpload = notion.fileUploads.createFileUpload {
    filename("remote-document.pdf")
    contentType("application/pdf")
    externalUrl("https://example.com/documents/report.pdf") // Sets mode and URL
}

println("Import initiated: ${fileUpload.id}")
```

### Manual Request Construction

If you prefer not to use the DSL:

```kotlin
val request = CreateFileUploadRequest(
    mode = FileUploadMode.SINGLE_PART,
    filename = "document.txt",
    contentType = "text/plain"
)

val fileUpload = notion.fileUploads.createFileUpload(request)
```

### List and Retrieve Operations

```kotlin
// Retrieve specific upload
val fileUpload = notion.fileUploads.retrieveFileUpload("file-upload-id")
println("Status: ${fileUpload.status}")

// List recent uploads
val uploads = notion.fileUploads.listFileUploads(pageSize = 20)
uploads.results.forEach { upload ->
    println("${upload.filename}: ${upload.status} (${upload.createdTime})")
}

// Pagination
if (uploads.hasMore) {
    val nextPage = notion.fileUploads.listFileUploads(
        startCursor = uploads.nextCursor,
        pageSize = 20
    )
}
```

---

## Using Uploaded Files

Once uploaded, reference files in your pages and blocks:

```kotlin
// Upload the file first
val uploadResult = notion.enhancedFileUploads.uploadFile(
    file = File("diagram.png")
)

if (uploadResult is FileUploadResult.Success) {
    // Add to a page
    val page = notion.pages.create {
        parent { databaseId("database-id") }
        properties {
            title("Page with Diagram") {
                text("Technical Architecture")
            }
        }
        children {
            // Reference the uploaded file
            image {
                file {
                    uploadedFile(uploadResult.fileUpload.id)
                }
                caption {
                    text("System architecture diagram")
                }
            }
        }
    }
}
```

## File Size and Type Limits

### File Size Limits
- **Free workspaces**: 5 MiB per file
- **Paid workspaces**: 5 GiB per file
- **Multi-part required**: Files > 20 MiB
- **Multi-part minimum**: Each part must be >= 5 MiB (except the last part)
- **Maximum parts**: 1,000 per upload

### Supported File Types
The File Upload API supports specific file extensions organized by category:

**Audio**: `.aac`, `.adts`, `.mid`, `.midi`, `.mp3`, `.mpga`, `.m4a`, `.m4b`, `.mp4`, `.oga`, `.ogg`, `.wav`, `.wma`

**Documents**: `.pdf`, `.txt`, `.json`, `.doc`, `.dot`, `.docx`, `.dotx`, `.xls`, `.xlt`, `.xla`, `.xlsx`, `.xltx`, `.ppt`, `.pot`, `.pps`, `.ppa`, `.pptx`, `.potx`

**Images**: `.gif`, `.heic`, `.jpeg`, `.jpg`, `.png`, `.svg`, `.tif`, `.tiff`, `.webp`, `.ico`

**Video**: `.amv`, `.asf`, `.wmv`, `.avi`, `.f4v`, `.flv`, `.gifv`, `.m4v`, `.mkv`, `.webm`, `.mov`, `.qt`, `.mpeg`

> ⚠️ **Note**: Some file types that work in the Notion UI (like `.md` for Markdown) are **not supported** by the File Upload API. Always check the supported extensions list above.

### External URL Requirements
- URLs must use **HTTPS** (HTTP is not allowed)
- URL must be publicly accessible
- Notion validates the URL by fetching headers before import

### Upload Lifecycle
- New uploads have a **1-hour expiry time** from creation
- Files must be attached to a page/block within 1 hour or they'll be archived
- Once attached, the expiry time is removed (becomes permanent)
- File upload IDs can be reused after the file is attached, even if the original content is deleted

### Automatic Optimization

The enhanced API automatically:
- **Detects optimal upload strategy** based on file size
- **Calculates chunk sizes** for multi-part uploads
- **Determines content types** from file extensions
- **Handles retries** for network issues
- **Provides progress tracking** for long uploads

### Manual Control

The basic API lets you:
- **Specify exact part counts** for multi-part uploads
- **Control content types** explicitly
- **Handle upload steps** individually
- **Implement custom retry logic**
- **Access low-level upload URLs** if needed

## Best Practices

### For Most Applications
```kotlin
// ✅ Recommended: Use enhanced API
val result = notion.enhancedFileUploads.uploadFile(file)
// Handles everything automatically
```

### For Large Files with Progress
```kotlin
// ✅ Show progress for better UX
val result = notion.enhancedFileUploads.uploadFile(
    file = largeFile,
    options = FileUploadOptions(
        enableConcurrentParts = true,
        progressCallback = { progress ->
            updateProgressBar(progress.percentComplete)
        }
    )
)
```

### For Production Applications
```kotlin
// ✅ Include error recovery
val result = notion.enhancedFileUploads.uploadFile(
    file = file,
    options = FileUploadOptions(
        validateBeforeUpload = true,
        retryConfig = RetryConfig(maxRetries = 3)
    )
)

// Handle all error cases appropriately
when (result) {
    is FileUploadResult.Success -> { /* success */ }
    is FileUploadResult.Failure -> { 
        logError("Upload failed", result.error)
        showUserFriendlyError(result.error)
    }
}
```

### For External Files
```kotlin
// ✅ Wait for processing to complete
val importResult = notion.enhancedFileUploads.importExternalFile(
    filename = "external-file.pdf",
    externalUrl = "https://example.com/file.pdf"
)

if (importResult is FileUploadResult.Success) {
    // External files may need processing time
    val readyFile = notion.enhancedFileUploads.waitForFileReady(
        importResult.uploadId,
        maxWaitTimeMs = 30000
    )
}
```

## Common Patterns

### Bulk File Upload
```kotlin
val files = listOf("doc1.pdf", "doc2.pdf", "image.jpg")
val results = mutableListOf<FileUploadResult>()

for (filename in files) {
    val result = notion.enhancedFileUploads.uploadFile(File(filename))
    results.add(result)
    
    if (result is FileUploadResult.Success) {
        println("✅ $filename uploaded: ${result.uploadId}")
    } else {
        println("❌ $filename failed")
    }
}
```

### Upload with Fallback
```kotlin
fun uploadWithFallback(file: File): String? {
    // Try enhanced API first
    val enhancedResult = notion.enhancedFileUploads.uploadFile(file)
    
    if (enhancedResult is FileUploadResult.Success) {
        return enhancedResult.uploadId
    }
    
    // Fall back to basic API if needed
    try {
        val basicUpload = notion.fileUploads.uploadFile(
            filename = file.name,
            contentType = FileUploadUtils.detectContentType(file.name),
            fileContent = file.readBytes()
        )
        return basicUpload.id
    } catch (e: Exception) {
        logger.error("All upload methods failed", e)
        return null
    }
}
```

---

*This documentation covers both file upload approaches. Use the enhanced API for convenience and automation, or the basic API when you need precise control over the upload process.*