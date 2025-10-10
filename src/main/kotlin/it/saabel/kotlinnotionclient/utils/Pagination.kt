@file:Suppress("unused")

package it.saabel.kotlinnotionclient.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Represents a paginated response from the Notion API.
 *
 * All paginated Notion API responses follow this pattern with results, a cursor for the next page,
 * and a flag indicating whether more results are available.
 *
 * @param T The type of items in the results list
 */
interface PaginatedResponse<T> {
    /**
     * The list of results for this page.
     */
    val results: List<T>

    /**
     * Cursor for fetching the next page of results.
     * Null if there are no more results.
     */
    val nextCursor: String?

    /**
     * Whether there are more results available beyond this page.
     */
    val hasMore: Boolean
}

/**
 * A fetcher function that retrieves a page of results given an optional cursor.
 *
 * The fetcher should accept an optional cursor (null for the first page) and return
 * a paginated response. The response type must implement [PaginatedResponse].
 */
typealias PageFetcher<T, R> = suspend (cursor: String?) -> R

/**
 * Core pagination utilities for working with Notion API paginated responses.
 *
 * This object provides generic helpers that work with any paginated Notion API response,
 * enabling both Flow-based reactive pagination and simple "collect all" operations.
 */
object Pagination {
    /**
     * Creates a Flow that emits individual items from paginated responses.
     *
     * The flow automatically handles pagination by following the cursor chain until no more
     * results are available. Items are emitted as they become available, enabling reactive
     * processing without loading all results into memory at once.
     *
     * Example usage:
     * ```kotlin
     * // Collect items reactively
     * notion.dataSources.queryAsFlow("data-source-id") {
     *     filter { property("Status") { select { equals("Active") } } }
     * }.collect { page ->
     *     println("Processing: ${page.properties}")
     * }
     * ```
     *
     * @param T The type of items being paginated
     * @param R The specific paginated response type
     * @param fetcher A suspend function that fetches a page given an optional cursor
     * @return A Flow that emits individual items from all pages
     */
    fun <T, R> asFlow(fetcher: PageFetcher<T, R>): Flow<T>
        where R : PaginatedResponse<T> =
        flow {
            var cursor: String? = null
            do {
                val page = fetcher(cursor)
                page.results.forEach { emit(it) }
                cursor = page.nextCursor
            } while (page.hasMore)
        }

    /**
     * Collects all items from paginated responses into a single list.
     *
     * This function automatically handles pagination by following the cursor chain until no more
     * results are available, then returns all items as a single list. Note that this loads all
     * results into memory, so use with caution for potentially large result sets.
     *
     * For large result sets where you don't need all items in memory at once, consider using
     * [asFlow] instead for reactive processing.
     *
     * Example usage:
     * ```kotlin
     * // Collect all results at once
     * val allPages = notion.dataSources.queryAll("data-source-id") {
     *     filter { property("Status") { select { equals("Active") } } }
     * }
     * println("Found ${allPages.size} matching pages")
     * ```
     *
     * @param T The type of items being paginated
     * @param R The specific paginated response type
     * @param fetcher A suspend function that fetches a page given an optional cursor
     * @return A list containing all items from all pages
     */
    suspend fun <T, R> collectAll(fetcher: PageFetcher<T, R>): List<T>
        where R : PaginatedResponse<T> {
        val allResults = mutableListOf<T>()
        var cursor: String? = null
        do {
            val page = fetcher(cursor)
            allResults.addAll(page.results)
            cursor = page.nextCursor
        } while (page.hasMore)
        return allResults
    }

    /**
     * Creates a Flow that emits entire pages from paginated responses.
     *
     * Unlike [asFlow] which emits individual items, this emits complete page responses,
     * allowing access to pagination metadata (cursor, hasMore, etc.) alongside the results.
     *
     * This is useful when you need page-level information or want to process results in batches.
     *
     * Example usage:
     * ```kotlin
     * // Process results in page batches
     * notion.dataSources.queryAsPagesFlow("data-source-id") {
     *     filter { /* ... */ }
     * }.collect { page ->
     *     println("Processing ${page.results.size} items (has more: ${page.hasMore})")
     *     page.results.forEach { /* process item */ }
     * }
     * ```
     *
     * @param T The type of items being paginated
     * @param R The specific paginated response type
     * @param fetcher A suspend function that fetches a page given an optional cursor
     * @return A Flow that emits complete page responses
     */
    fun <T, R> asPagesFlow(fetcher: PageFetcher<T, R>): Flow<R>
        where R : PaginatedResponse<T> =
        flow {
            var cursor: String? = null
            do {
                val page = fetcher(cursor)
                emit(page)
                cursor = page.nextCursor
            } while (page.hasMore)
        }
}
