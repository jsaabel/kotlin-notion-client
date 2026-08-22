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
    data class ServiceOverloadedError(val retryAfterSeconds: Long? = null, val details: String? = null)
    data class ValidationError(val field: String? = null, val details: String)
    data class UnexpectedError(val details: String, val originalCause: Throwable? = null)
    // Plus QueryResultLimitReached and IterationStalled, thrown only by the auto-paginating
    // and windowed data source query helpers — see their KDoc for when each applies.
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
- `429` - Rate limited (retried automatically — see [Rate Limiting](#rate-limiting); surfaces as `ApiError` with `status = 429` only once retries are exhausted)
- `500` - Internal server error (not retried — see [Rate Limiting](#rate-limiting))
- `502` / `503` / `504` - Bad gateway / service unavailable / gateway timeout (retried automatically)

`529` (service overloaded) is retried the same way as `429`, but once retries are exhausted it
surfaces as the dedicated [`ServiceOverloadedError`](#serviceoverloadederror) below instead of
`ApiError`.

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
- Notion signals rate limiting with an HTTP `429` and a `Retry-After` header (seconds to wait)
- The client automatically retries `429` responses honouring that header — see [Rate Limiting](#rate-limiting)
- In current builds, once retries are exhausted a `429` surfaces as `NotionException.ApiError` with `status = 429`, not as `RateLimitError` — `RateLimitError` is reserved on the hierarchy for callers doing their own manual retry bookkeeping (e.g. after catching `ApiError` and checking `status == 429`)

#### ServiceOverloadedError
Thrown when Notion returns `529` (service overloaded) and the client's automatic retries (see [Rate Limiting](#rate-limiting)) are exhausted.

**Properties:**
- `retryAfterSeconds: Long?` - Seconds the API asked the client to wait, from the final failed attempt's `Retry-After` header (`null` when absent)
- `details: String?` - Raw error details from the final failed response

```kotlin
try {
    client.pages.retrieve(pageId)
} catch (e: NotionException.ServiceOverloadedError) {
    logger.warn("Notion overloaded even after retries, backing off ${e.retryAfterSeconds ?: "a while"}s")
}
```

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
import it.saabel.kotlinnotionclient.exceptions.NotionException

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
        is NotionException.ServiceOverloadedError -> {
            logger.warn("Notion overloaded even after automatic retries, retry after ${e.retryAfterSeconds}s")
            // Back off further and retry
        }
        is NotionException.ValidationError -> {
            logger.error("Validation failed: ${e.details}")
            // Fix input and retry
        }
        is NotionException.UnexpectedError -> {
            logger.error("Unexpected error", e.originalCause)
            // Report bug
        }
        is NotionException.QueryResultLimitReached, is NotionException.IterationStalled -> {
            // Thrown only by data source query/iteration helpers hitting Notion's 10,000-row
            // cap — see their KDoc for recovery strategies (narrower query, UniqueId iteration key).
            logger.error(e.message)
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
import it.saabel.kotlinnotionclient.validation.ValidationException

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
import it.saabel.kotlinnotionclient.validation.ValidationConfig

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

The Notion API enforces rate limits to ensure service stability. The client handles this
transparently through [`NotionRateLimit`](../src/main/kotlin/it/saabel/kotlinnotionclient/ratelimit/NotionRateLimit.kt),
a Ktor client plugin hooked into the `Send` pipeline phase — every outbound request flows through
it automatically, so API methods never wrap calls in retry logic themselves. It handles two
concerns:

- **Proactive throttling** — a continuous-refill token bucket paces outbound requests at
  `sustainedRate` req/s, allowing short bursts up to `burstCapacity`.
- **Reactive retry** — on retryable HTTP statuses and transient network failures, the pipeline
  retries with a delay strategy chosen per status (see the [retry matrix](#retry-matrix) below).

### Rate Limit Configuration

```kotlin
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.ratelimit.RateLimitConfig
import kotlin.time.Duration.Companion.seconds

val config = NotionConfig(
    apiToken = "secret_...",
    enableRateLimit = true, // default
    rateLimitConfig = RateLimitConfig(
        sustainedRate = 3.0,       // req/s; matches Notion's documented sustained ceiling
        burstCapacity = 20,        // requests allowed to proceed immediately before pacing kicks in
        maxRetries = 3,            // retries *after* the initial attempt (up to 4 calls total)
        retryBaseDelay = 1.seconds,
        retryMaxDelay = 30.seconds,
        jitterFactor = 0.1,        // 0.0–1.0, randomizes each backoff delay
    ),
)

val client = NotionClient(config)
```

`RateLimitConfig` validates its own fields in an `init` block (e.g. `maxRetries >= 0`,
`retryMaxDelay >= retryBaseDelay`, `jitterFactor in 0.0..1.0`) — invalid combinations fail fast
with an `IllegalArgumentException` at construction time. There are no named presets
(`CONSERVATIVE`/`BALANCED`/`AGGRESSIVE`) — tune the fields directly for your workload; raise
`burstCapacity` for heavy-concurrency fan-out rather than `sustainedRate`, which is pinned to
Notion's documented ceiling.

### Retry Matrix

The plugin classifies failures by type — inspecting the `HttpStatusCode` or exception class
directly, never by string-matching an error message — and picks one of two delay strategies:

| Status / failure | Retried? | Delay strategy |
|---|---|---|
| `429` (rate limited) | Yes | `Retry-After` header (seconds) + 1s safety margin; falls back to exponential backoff if the header is absent |
| `529` (service overloaded) | Yes | Same as `429` — Notion ships the same `Retry-After` contract for both |
| `502` / `503` / `504` (bad gateway / unavailable / gateway timeout) | Yes | Exponential backoff with jitter (`retryBaseDelay * 2^attempt`, capped at `retryMaxDelay`) |
| `500` (internal server error) | **No** | Typically a non-transient server-side fault, not a blip |
| Other `4xx` | No | Not retried |
| Network failures (`IOException` and subtypes — timeouts, connection resets, DNS failures) | Yes | Same exponential backoff as `502`/`503`/`504` |
| Success | — | Returned immediately |

The `Retry-After`-driven delay deliberately does **not** stack with the exponential schedule —
it replaces it. After the final permitted attempt, a failure is returned/thrown immediately with
no extra delay.

**Once retries are exhausted:**
- A `429` currently surfaces as `NotionException.ApiError` with `status = 429`.
- A `529` surfaces as the dedicated [`NotionException.ServiceOverloadedError`](#serviceoverloadederror), carrying `retryAfterSeconds` from the final attempt's header when present.
- Every other retried-and-still-failing status surfaces as `NotionException.ApiError` with the corresponding `status`.
- A persisting network failure surfaces as `NotionException.NetworkError`.

### Disabling Rate Limiting

To disable the throttle + retry pipeline entirely (every request is sent exactly once, with no
pacing):

```kotlin
val config = NotionConfig(
    apiToken = "secret_...",
    enableRateLimit = false,
)
```

With rate limiting disabled, a `429` or `529` is not retried and surfaces immediately as
`NotionException.ApiError` (or `NotionException.ServiceOverloadedError` for `529`) on the very
first attempt.

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
import it.saabel.kotlinnotionclient.validation.*

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
        is NotionException.ServiceOverloadedError -> {
            logger.warn("Notion overloaded, retry after ${e.retryAfterSeconds}s")
            // Monitor overload occurrences separately from rate limiting
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
    is NotionException.ServiceOverloadedError -> handleServiceOverloaded()
    is NotionException.ValidationError -> handleValidation()
    is NotionException.UnexpectedError -> handleUnexpected()
    is NotionException.QueryResultLimitReached -> handleQueryLimit()
    is NotionException.IterationStalled -> handleIterationStall()
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