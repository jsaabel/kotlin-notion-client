# Error Handling

## Overview

The Kotlin Notion Client provides a structured error handling system with type-safe exceptions and comprehensive validation. Errors are caught early through client-side validation, and API errors are mapped to specific exception types for precise error handling.

## Error Types

All Notion-specific exceptions extend from the sealed class `NotionException`, providing exhaustive when expressions and type-safe error handling.

### NotionException Hierarchy

```kotlin
sealed class NotionException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data class NetworkError(val originalCause: Throwable)
    data class ApiError(val code: String, val status: Int, val details: String? = null)
    data class AuthenticationError(val details: String)
    data class RateLimitError(val retryAfterSeconds: Long? = null)
    data class ValidationError(val field: String? = null, val details: String)
    data class UnexpectedError(val details: String, val originalCause: Throwable? = null)
}
```

### Error Type Descriptions

#### NetworkError
Network-related failures such as connection timeouts, DNS resolution failures, or socket errors.

**Properties:**
- `originalCause: Throwable` - The underlying network exception

**Common causes:**
- No internet connection
- Server unreachable
- Connection timeout
- SSL/TLS errors

#### ApiError
Errors returned by the Notion API with HTTP status codes and error codes.

**Properties:**
- `code: String` - Notion API error code (e.g., "object_not_found", "invalid_request")
- `status: Int` - HTTP status code (e.g., 404, 400, 500)
- `details: String?` - Additional error details from the API

**Common HTTP status codes:**
- `400` - Bad request (invalid parameters)
- `401` - Unauthorized (invalid API token)
- `403` - Forbidden (insufficient permissions)
- `404` - Object not found
- `409` - Conflict (version mismatch)
- `429` - Rate limited
- `500` - Internal server error
- `503` - Service unavailable

#### AuthenticationError
Authentication and authorization failures.

**Properties:**
- `details: String` - Description of the authentication failure

**Common causes:**
- Invalid API token
- Expired API token
- Insufficient permissions for the requested operation
- Token doesn't have access to the specified resource

#### RateLimitError
Rate limiting errors when API quota is exceeded.

**Properties:**
- `retryAfterSeconds: Long?` - Number of seconds to wait before retrying (from `Retry-After` header)

**Rate limiting behavior:**
- Notion API uses rate limiting headers (`x-ratelimit-limit`, `x-ratelimit-remaining`, `x-ratelimit-reset`)
- The client can be configured to automatically retry with exponential backoff
- See [Rate Limiting](#rate-limiting) section for strategies

#### ValidationError
Client-side validation errors caught before making API requests.

**Properties:**
- `field: String?` - The field that failed validation
- `details: String` - Description of the validation failure

**Common validation failures:**
- Content exceeds length limits (e.g., rich text > 2000 chars)
- Too many items in arrays (e.g., > 100 blocks)
- Invalid format (URLs, emails, phone numbers)
- Missing required fields

#### UnexpectedError
Unexpected errors that don't fit other categories.

**Properties:**
- `details: String` - Description of the error
- `originalCause: Throwable?` - The underlying exception if available

## Basic Error Handling

### Simple Try-Catch

```kotlin
import no.saabelit.kotlinnotionclient.exceptions.NotionException

try {
    val page = client.pages.retrieve("page-id")
    println("Page title: ${page.properties["title"]}")
} catch (e: NotionException.ApiError) {
    when (e.status) {
        404 -> println("Page not found")
        401 -> println("Invalid API token")
        403 -> println("No access to this page")
        else -> println("API error: ${e.code} (${e.status})")
    }
} catch (e: NotionException.NetworkError) {
    println("Network error: ${e.message}")
} catch (e: NotionException) {
    println("Unexpected error: ${e.message}")
}
```

### Exhaustive When Expression

Since `NotionException` is a sealed class, you can use exhaustive when expressions:

```kotlin
try {
    val database = client.databases.retrieve("database-id")
} catch (e: NotionException) {
    when (e) {
        is NotionException.NetworkError -> {
            logger.error("Network failure", e.originalCause)
            // Retry logic or show offline UI
        }
        is NotionException.ApiError -> {
            logger.error("API error: ${e.code} (HTTP ${e.status})")
            // Handle specific API errors
        }
        is NotionException.AuthenticationError -> {
            logger.error("Auth failed: ${e.details}")
            // Prompt for new API token
        }
        is NotionException.RateLimitError -> {
            logger.warn("Rate limited, retry after ${e.retryAfterSeconds}s")
            // Wait and retry
        }
        is NotionException.ValidationError -> {
            logger.error("Validation failed: ${e.details}")
            // Fix input and retry
        }
        is NotionException.UnexpectedError -> {
            logger.error("Unexpected error", e.originalCause)
            // Report bug
        }
    }
}
```

### Handling Specific API Errors

```kotlin
try {
    val page = client.pages.retrieve(pageId)
} catch (e: NotionException.ApiError) {
    when (e.code) {
        "object_not_found" -> {
            println("Page doesn't exist or was deleted")
        }
        "unauthorized" -> {
            println("Invalid API token or insufficient permissions")
        }
        "validation_error" -> {
            println("Invalid request parameters: ${e.details}")
        }
        else -> {
            println("API error: ${e.code}")
        }
    }
}
```

## Validation Errors

The client performs validation **before** making API calls to catch errors early and provide better error messages.

### ValidationException

Validation errors throw a `ValidationException` (extends `IllegalArgumentException`) with detailed information:

```kotlin
import no.saabelit.kotlinnotionclient.validation.ValidationException

try {
    // Attempt to create too many blocks at once
    val blocks = (1..150).map {
        BlockRequest.Paragraph(
            paragraph = ParagraphRequestContent(
                richText = listOf(createNormalRichText("Block $it"))
            )
        )
    }
    client.blocks.appendChildren(pageId, blocks)
} catch (e: ValidationException) {
    println(e.validationResult.getSummary())
    // Output:
    // Validation Summary:
    //   Errors: 1
    //
    //   ARRAY_TOO_LARGE: Array too large (current: 150, limit: 100)
}
```

### Validation Result Details

Access detailed validation information:

```kotlin
try {
    client.pages.create(request)
} catch (e: ValidationException) {
    val result = e.validationResult

    // Check for errors vs warnings
    println("Has errors: ${result.hasErrors}")
    println("Has warnings: ${result.hasWarnings}")

    // Get specific violations
    result.violations.forEach { violation ->
        println("Field: ${violation.field}")
        println("Type: ${violation.violationType}")
        println("Message: ${violation.message}")
        println("Current: ${violation.currentValue}, Limit: ${violation.limit}")

        if (violation.autoFixAvailable) {
            println("Auto-fix: ${violation.suggestedAction}")
        }
    }
}
```

### Common Validation Violations

| Violation Type | Description | Limit |
|---------------|-------------|-------|
| `CONTENT_TOO_LONG` | Text content exceeds length | 2000 chars per RichText segment |
| `ARRAY_TOO_LARGE` | Too many items in array | 100 blocks, 100 select options |
| `PAYLOAD_TOO_LARGE` | Overall request too large | Varies by endpoint |
| `INVALID_URL` | URL format or length invalid | 2000 chars |
| `INVALID_EMAIL` | Email format invalid | - |
| `INVALID_PHONE` | Phone format invalid | - |
| `CONTENT_NEAR_LIMIT` | Warning: approaching limit | 90% of limit |
| `ARRAY_NEAR_LIMIT` | Warning: array nearing limit | 90 items |
| `PAYLOAD_NEAR_LIMIT` | Warning: payload nearing limit | - |

### Validation Configuration

Configure validation behavior:

```kotlin
import no.saabelit.kotlinnotionclient.validation.ValidationConfig

val config = NotionConfig(
    apiToken = "secret_...",
    validationConfig = ValidationConfig(
        autoSplitLongText = true  // Automatically split text > 2000 chars into segments
    )
)

val client = NotionClient(config)
```

**With `autoSplitLongText = true` (default):**
- Long text is automatically split into multiple RichText segments
- Each segment stays under the 2000-character limit
- All content is preserved

**With `autoSplitLongText = false`:**
- Validation throws `ValidationException` for text > 2000 chars
- You must manually split or truncate content

## Rate Limiting

The Notion API enforces rate limits to ensure service stability. The client provides automatic retry strategies and rate limit tracking.

### Rate Limit Configuration

```kotlin
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitConfig
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitStrategy

val config = NotionConfig(
    apiToken = "secret_...",
    enableRateLimit = true,
    rateLimitConfig = RateLimitConfig.BALANCED  // Predefined strategy
)

val client = NotionClient(config)
```

### Predefined Rate Limit Strategies

#### CONSERVATIVE
Most cautious approach - minimizes chance of hitting rate limits:
- **Max retries:** 5
- **Base delay:** 1000ms
- **Max delay:** 60000ms (1 minute)
- **Jitter factor:** 0.1
- **Respects `Retry-After` header:** Yes

#### BALANCED (Recommended)
Good balance between performance and safety:
- **Max retries:** 3
- **Base delay:** 500ms
- **Max delay:** 30000ms (30 seconds)
- **Jitter factor:** 0.15
- **Respects `Retry-After` header:** Yes

#### AGGRESSIVE
Fastest retry with minimal delays:
- **Max retries:** 2
- **Base delay:** 200ms
- **Max delay:** 10000ms (10 seconds)
- **Jitter factor:** 0.2
- **Respects `Retry-After` header:** Yes

### Custom Rate Limit Configuration

```kotlin
val customConfig = RateLimitConfig(
    strategy = RateLimitStrategy.CUSTOM,
    maxRetries = 4,
    baseDelayMs = 750,
    maxDelayMs = 45000,
    jitterFactor = 0.12,
    respectRetryAfter = true
)

val config = NotionConfig(
    apiToken = "secret_...",
    enableRateLimit = true,
    rateLimitConfig = customConfig
)
```

### Rate Limit Headers

Notion returns rate limiting information in response headers:

| Header | Description |
|--------|-------------|
| `x-ratelimit-limit` | Maximum requests allowed in the window |
| `x-ratelimit-remaining` | Requests remaining in current window |
| `x-ratelimit-reset` | Unix timestamp when limit resets |
| `retry-after` | Seconds to wait before retrying (429 responses) |

### Handling Rate Limit Errors Manually

```kotlin
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.exceptions.NotionException

suspend fun retrievePageWithRetry(pageId: String, maxAttempts: Int = 3): Page? {
    repeat(maxAttempts) { attempt ->
        try {
            return client.pages.retrieve(pageId)
        } catch (e: NotionException.RateLimitError) {
            if (attempt == maxAttempts - 1) {
                throw e  // Last attempt, re-throw
            }

            val waitTime = e.retryAfterSeconds ?: (1L shl attempt)  // Exponential backoff
            println("Rate limited, waiting ${waitTime}s before retry ${attempt + 1}/$maxAttempts")
            delay(waitTime * 1000)
        }
    }
    return null
}
```

### Disabling Rate Limiting

To disable automatic rate limit handling:

```kotlin
val config = NotionConfig(
    apiToken = "secret_...",
    enableRateLimit = false  // Disable rate limiting
)
```

When disabled, rate limit errors are thrown as `NotionException.ApiError` with status 429.

## Common Patterns

### Retry with Exponential Backoff

```kotlin
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 500,
    maxDelayMs: Long = 30000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: NotionException.NetworkError) {
            println("Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }
    return block()  // Last attempt
}

// Usage
val page = retryWithBackoff {
    client.pages.retrieve(pageId)
}
```

### Graceful Degradation

```kotlin
suspend fun getPageOrDefault(pageId: String): Page? {
    return try {
        client.pages.retrieve(pageId)
    } catch (e: NotionException.ApiError) {
        when (e.status) {
            404 -> null  // Page not found, return null
            403 -> null  // No access, return null
            else -> throw e  // Re-throw other errors
        }
    } catch (e: NotionException.NetworkError) {
        null  // Network issue, return null
    }
}
```

### Validation Before Batch Operations

```kotlin
import no.saabelit.kotlinnotionclient.validation.*

fun validateBeforeBatch(blocks: List<BlockRequest>) {
    if (blocks.size > 100) {
        throw ValidationException(
            ValidationResult(
                violations = listOf(
                    ValidationViolation(
                        field = "blocks",
                        violationType = ViolationType.ARRAY_TOO_LARGE,
                        message = "Too many blocks (max 100)",
                        currentValue = blocks.size,
                        limit = 100
                    )
                )
            )
        )
    }
}

// Usage - split into batches
val allBlocks = buildList {
    repeat(150) {
        add(BlockRequest.Paragraph(
            paragraph = ParagraphRequestContent(
                richText = listOf(RichText(...))
            )
        ))
    }
}

// Process in batches of 100
allBlocks.chunked(100).forEach { batch ->
    client.blocks.appendChildren(pageId, batch)
}
```

### Logging and Monitoring

```kotlin
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("NotionClient")

try {
    client.pages.create(request)
} catch (e: NotionException) {
    when (e) {
        is NotionException.NetworkError -> {
            logger.error("Network error in pages.create", e)
            // Increment metrics for network failures
        }
        is NotionException.ApiError -> {
            logger.error("API error: ${e.code} (HTTP ${e.status})", e)
            // Track API error rates by status code
        }
        is NotionException.RateLimitError -> {
            logger.warn("Rate limited, retry after ${e.retryAfterSeconds}s")
            // Monitor rate limit occurrences
        }
        else -> logger.error("Unexpected error", e)
    }
    throw e
}
```

## Gotchas and Best Practices

### ✅ DO: Use Sealed Class Exhaustiveness

```kotlin
// Compiler ensures all cases are handled
when (exception) {
    is NotionException.NetworkError -> handleNetwork()
    is NotionException.ApiError -> handleApi()
    is NotionException.AuthenticationError -> handleAuth()
    is NotionException.RateLimitError -> handleRateLimit()
    is NotionException.ValidationError -> handleValidation()
    is NotionException.UnexpectedError -> handleUnexpected()
}
```

### ✅ DO: Validate Early

Validation catches errors before making API calls, saving time and quota.

### ✅ DO: Handle Rate Limits Gracefully

Enable automatic rate limiting or implement exponential backoff for retries.

### ✅ DO: Log Errors with Context

Include request details, IDs, and timestamps in error logs for debugging.

### ❌ DON'T: Catch Generic Exceptions

```kotlin
// Bad - loses type information
try {
    client.pages.retrieve(pageId)
} catch (e: Exception) {
    // Can't distinguish between error types
}

// Good - specific error handling
try {
    client.pages.retrieve(pageId)
} catch (e: NotionException) {
    // Type-safe error handling
}
```

### ❌ DON'T: Ignore Validation Errors

Validation errors indicate issues with your request. Fix the input rather than trying to bypass validation.

### ❌ DON'T: Retry on Authentication Errors

Authentication errors won't resolve with retries - fix the API token instead.

## Testing Error Scenarios

### Unit Tests with Ktor MockEngine

The codebase uses Ktor's MockEngine for testing error scenarios without real API calls. See `src/test/kotlin/unit/api/MockedApiTest.kt` for examples:

```kotlin
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

// Mock a 404 error
val mockEngine = MockEngine { request ->
    respond(
        content = """{"object": "error", "status": 404, "code": "object_not_found"}""",
        status = HttpStatusCode.NotFound,
        headers = headersOf("Content-Type" to listOf("application/json"))
    )
}

// Test error handling
try {
    pagesApi.retrieve("invalid-id")
} catch (e: NotionException.ApiError) {
    e.status shouldBe 404
}
```

### Integration Tests

The project includes comprehensive integration tests for error scenarios:

- **Rate Limiting**: `src/test/kotlin/integration/RateLimitIntegrationTest.kt` - Tests rate limiting with mock responses and various retry strategies
- **Validation**: `src/test/kotlin/integration/ValidationIntegrationTest.kt` - Tests validation with real API calls to ensure our limits match Notion's actual behavior

## Related Topics

- [Testing](testing.md) - Testing strategies and patterns
- [Rich Text DSL](rich-text-dsl.md) - Working with rich text content
- [Databases](databases.md) - Database operations