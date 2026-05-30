@file:Suppress("unused")

package it.saabel.kotlinnotionclient.exceptions

import it.saabel.kotlinnotionclient.models.base.RequestStatus
import it.saabel.kotlinnotionclient.models.pages.Page

/**
 * Base exception class for all Notion API related errors.
 *
 * This sealed class hierarchy provides type-safe error handling
 * for different kinds of failures that can occur when using the Notion API.
 */
sealed class NotionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * Network-related errors (connection failures, timeouts, etc.)
     */
    data class NetworkError(
        val originalCause: Throwable,
    ) : NotionException("Network error occurred", originalCause)

    /**
     * API errors returned by the Notion API
     */
    data class ApiError(
        val code: String,
        val status: Int,
        val details: String? = null,
    ) : NotionException("API error: $code (HTTP $status)${details?.let { " - $it" } ?: ""}")

    /**
     * Authentication/authorization errors
     */
    data class AuthenticationError(
        val details: String,
    ) : NotionException("Authentication error: $details")

    /**
     * Rate limiting errors
     */
    data class RateLimitError(
        val retryAfterSeconds: Long? = null,
    ) : NotionException("Rate limit exceeded${retryAfterSeconds?.let { " (retry after $it seconds)" } ?: ""}")

    /**
     * Validation errors (invalid input, missing required fields, etc.)
     */
    data class ValidationError(
        val field: String? = null,
        val details: String,
    ) : NotionException("Validation error${field?.let { " for field '$it'" } ?: ""}: $details")

    /**
     * Unexpected errors (should not happen in normal usage)
     */
    data class UnexpectedError(
        val details: String,
        val originalCause: Throwable? = null,
    ) : NotionException("Unexpected error: $details", originalCause)

    /**
     * Thrown by auto-paginating data source queries when Notion's API truncates the
     * result set at its 10,000-row cap (`request_status.type == "incomplete"`).
     *
     * The exception carries the [partialResults] collected so far, the [nextCursor]
     * the API returned, and the raw [requestStatus] so the caller can decide how to
     * recover — for example by narrowing the query, switching to a single-page method
     * such as `queryFirstPage` / `queryPagedFlow`, or moving to a webhook flow.
     */
    data class QueryResultLimitReached(
        val partialResults: List<Page>,
        val nextCursor: String?,
        val requestStatus: RequestStatus,
    ) : NotionException(
            "Query result limit reached (Notion caps data source query results at 10,000 entries). " +
                "Collected ${partialResults.size} partial results before truncation.",
        )
}
