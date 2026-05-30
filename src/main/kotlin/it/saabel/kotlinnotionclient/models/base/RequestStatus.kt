package it.saabel.kotlinnotionclient.models.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signals whether a paginated query result is complete or has been truncated by the API.
 *
 * Notion's data source query endpoint and view query endpoints cache up to 10,000
 * results per query. When the matching row count exceeds this cap, the cache is
 * truncated and the API surfaces `request_status` on every page of the response
 * with `type = "incomplete"` and `incomplete_reason = "query_result_limit_reached"`.
 *
 * When truncation occurs, callers cannot rely on `total_count` (which reflects only
 * the truncated cache size) and should either narrow the query via filters or move
 * to a webhook-driven approach.
 */
@Serializable
data class RequestStatus(
    @SerialName("type")
    val type: String,
    @SerialName("incomplete_reason")
    val incompleteReason: String? = null,
) {
    val isComplete: Boolean get() = type == TYPE_COMPLETE
    val isIncomplete: Boolean get() = type == TYPE_INCOMPLETE

    companion object {
        const val TYPE_COMPLETE = "complete"
        const val TYPE_INCOMPLETE = "incomplete"
        const val REASON_QUERY_RESULT_LIMIT_REACHED = "query_result_limit_reached"
    }
}
