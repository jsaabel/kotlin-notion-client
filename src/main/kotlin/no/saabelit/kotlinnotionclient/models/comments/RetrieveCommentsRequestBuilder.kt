@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.comments

/**
 * DSL marker to prevent nested scopes in retrieve comment builders.
 */
@DslMarker
annotation class RetrieveCommentsDslMarker

/**
 * Builder class for retrieving comments with a fluent DSL.
 *
 * This builder provides a convenient way to construct retrieve comment requests
 * with a consistent DSL interface.
 *
 * ## Basic Usage:
 * ```kotlin
 * val comments = client.comments.retrieve {
 *     blockId("12345678-1234-1234-1234-123456789abc")
 * }
 * ```
 *
 * ## With Pagination:
 * ```kotlin
 * val comments = client.comments.retrieve {
 *     blockId("12345678-1234-1234-1234-123456789abc")
 *     pageSize(50)
 *     startCursor("cursor-string")
 * }
 * ```
 */
@RetrieveCommentsDslMarker
class RetrieveCommentsRequestBuilder {
    private var blockIdValue: String? = null
    private var pageSizeValue: Int? = null
    private var startCursorValue: String? = null

    /**
     * Sets the block ID to retrieve comments for.
     * This can be either a page ID or block ID.
     *
     * @param blockId The ID of the block to retrieve comments for
     */
    fun blockId(blockId: String) {
        blockIdValue = blockId
    }

    /**
     * Sets the page size for pagination (max 100).
     *
     * @param size The number of comments to return per page
     */
    fun pageSize(size: Int) {
        require(size in 1..100) { "Page size must be between 1 and 100, got $size" }
        pageSizeValue = size
    }

    /**
     * Sets the start cursor for pagination.
     *
     * @param cursor The cursor to start retrieving from
     */
    fun startCursor(cursor: String) {
        startCursorValue = cursor
    }

    /**
     * Builds the retrieve comments request parameters.
     *
     * @return Triple of (blockId, pageSize, startCursor)
     * @throws IllegalStateException if blockId is not set
     */
    internal fun build(): Triple<String, Int?, String?> {
        val blockId = blockIdValue ?: throw IllegalStateException("Block ID must be specified")
        return Triple(blockId, pageSizeValue, startCursorValue)
    }
}

/**
 * Entry point function for the retrieve comments request DSL.
 *
 * Creates parameters for retrieving comments using a fluent builder pattern.
 *
 * @param block The DSL block for building the retrieve request
 * @return Triple of (blockId, pageSize, startCursor)
 */
fun retrieveCommentsRequest(block: RetrieveCommentsRequestBuilder.() -> Unit): Triple<String, Int?, String?> =
    RetrieveCommentsRequestBuilder().apply(block).build()
