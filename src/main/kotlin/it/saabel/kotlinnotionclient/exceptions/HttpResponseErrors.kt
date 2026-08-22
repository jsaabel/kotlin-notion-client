package it.saabel.kotlinnotionclient.exceptions

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders

/**
 * Maps a non-success [HttpResponse] to the appropriate [NotionException].
 *
 * Centralises the "read the error body, build a details string, pick an exception type" dance that
 * every API class otherwise repeats inline. `529` (service overloaded) maps to the dedicated
 * [NotionException.ServiceOverloadedError] — carrying `Retry-After` as [NotionException
 * .ServiceOverloadedError.retryAfterSeconds] when present — so callers can distinguish transient
 * overload from a generic [NotionException.ApiError]. Every other non-success status maps to
 * [NotionException.ApiError] as before.
 *
 * Call this only on a response already known to be non-success (`!status.isSuccess()`).
 */
internal suspend fun HttpResponse.toNotionApiError(): NotionException {
    val errorBody =
        try {
            body<String>()
        } catch (e: Exception) {
            "Could not read error response body"
        }
    val details = "HTTP ${status.value}: ${status.description}. Response: $errorBody"

    return if (status.value == 529) {
        NotionException.ServiceOverloadedError(
            retryAfterSeconds = headers[HttpHeaders.RetryAfter]?.toLongOrNull(),
            details = details,
        )
    } else {
        NotionException.ApiError(
            code = status.value.toString(),
            status = status.value,
            details = details,
        )
    }
}
