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
     * Thrown when Notion returns `529` (service overloaded) and the request's retries — handled
     * transparently by the [it.saabel.kotlinnotionclient.ratelimit.NotionRateLimit] plugin, which
     * retries `529` on the `Retry-After`-driven schedule — are exhausted.
     *
     * Kept distinct from [ApiError] so callers can pattern-match on transient overload without
     * string- or status-code-sniffing a generic error, and so [retryAfterSeconds] (when Notion sent
     * one on the final failed attempt) is available as a typed field rather than buried in [details].
     *
     * @property retryAfterSeconds Seconds the API asked the client to wait, from the final attempt's
     *   `Retry-After` header — `null` when the header was absent.
     * @property details Raw error details from the final failed response, if any.
     */
    data class ServiceOverloadedError(
        val retryAfterSeconds: Long? = null,
        val details: String? = null,
    ) : NotionException(
            "Notion API is temporarily overloaded (HTTP 529), retries exhausted" +
                (retryAfterSeconds?.let { " (retry after $it seconds)" } ?: "") +
                (details?.let { " - $it" } ?: ""),
        )

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

    /**
     * Thrown by windowed iteration (`DataSourcesApi.iterateAllRows` /
     * `collectAllRows`) when a full truncated window yields no rows that were not
     * already emitted, meaning the iteration cannot advance past the current key
     * value. With the `created_time` key this happens when more than 10,000 rows
     * share a single `created_time` value (Notion rounds it to the nearest minute,
     * so bulk imports can produce such buckets).
     *
     * Rows emitted before the stall have already been delivered through the flow.
     * To drain such a data source, switch to
     * [it.saabel.kotlinnotionclient.models.datasources.RowIterationKey.UniqueId],
     * which is strictly increasing and cannot stall.
     *
     * @property keyDescription Human-readable description of the iteration key.
     * @property boundaryValue The key value the iteration could not advance past.
     */
    data class IterationStalled(
        val keyDescription: String,
        val boundaryValue: String?,
    ) : NotionException(
            "Windowed iteration stalled: a full truncated window at $keyDescription >= " +
                "\"$boundaryValue\" contained no new rows. More than 10,000 rows likely share " +
                "this $keyDescription value. Use RowIterationKey.UniqueId with a unique_id " +
                "property to iterate such data sources.",
        )
}
