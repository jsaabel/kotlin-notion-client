package it.saabel.kotlinnotionclient.api

import it.saabel.kotlinnotionclient.config.NotionApiLimits
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.datasources.DataSourceFilter
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryRequest
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryResponse
import it.saabel.kotlinnotionclient.models.datasources.DataSourceSort
import it.saabel.kotlinnotionclient.models.datasources.DateCondition
import it.saabel.kotlinnotionclient.models.datasources.RowIterationKey
import it.saabel.kotlinnotionclient.models.datasources.SortDirection
import it.saabel.kotlinnotionclient.models.datasources.UniqueIdCondition
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.getUniqueIdProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Instant

/**
 * Windowed iteration over data sources larger than Notion's 10,000-row query cap.
 *
 * Strategy: sort rows ascending by a monotonic key ([RowIterationKey]) and follow the
 * normal cursor chain. When the API reports a truncated result set
 * (`request_status.type == "incomplete"`), open a fresh query ("window") filtered from
 * the last key value seen, and keep going until a window completes without truncation.
 *
 * Each window's filter and sort are fixed when the window opens; cursors within a
 * window always refer to the same query.
 */
internal object DataSourceRowIteration {
    /**
     * Safety valve against a misbehaving API: a single window is capped at 10,000
     * rows server-side, i.e. at most 100 pages of 100 — allow generous slack.
     */
    private const val MAX_PAGES_PER_WINDOW = 500

    fun iterateAllRows(
        request: DataSourceQueryRequest,
        key: RowIterationKey,
        fetchPage: suspend (DataSourceQueryRequest) -> DataSourceQueryResponse,
    ): Flow<Page> {
        require(request.sorts.isNullOrEmpty()) {
            "iterateAllRows imposes its own sort on the iteration key; " +
                "remove sorts from the query. Results are delivered ordered by the key."
        }
        require(request.startCursor == null) {
            "iterateAllRows manages pagination cursors itself; remove startCursor from the query."
        }
        return flow {
            val tracker = newTracker(key)
            while (true) {
                val windowRequest =
                    request.copy(
                        filter = combineFilters(request.filter, tracker.windowFilter()),
                        sorts = listOf(tracker.sort()),
                        startCursor = null,
                        pageSize = NotionApiLimits.Response.MAX_PAGE_SIZE,
                    )
                var newRowsInWindow = 0
                var truncated = false
                var cursor: String? = null
                var pageCount = 0
                do {
                    val response = fetchPage(windowRequest.copy(startCursor = cursor))
                    response.results.forEach { page ->
                        if (tracker.accept(page)) {
                            emit(page)
                            newRowsInWindow++
                        }
                    }
                    if (response.requestStatus?.isIncomplete == true) {
                        truncated = true
                    }
                    cursor = response.nextCursor
                    pageCount++
                    if (pageCount >= MAX_PAGES_PER_WINDOW) {
                        throw NotionException.ApiError(
                            code = "PAGINATION_LIMIT_EXCEEDED",
                            status = 500,
                            details =
                                "A single iteration window exceeded $MAX_PAGES_PER_WINDOW pages, " +
                                    "although Notion caps windows at 10,000 rows. " +
                                    "This may indicate an infinite pagination loop.",
                        )
                    }
                } while (response.hasMore && cursor != null)

                if (!truncated) {
                    // The window completed without hitting the cap: everything is drained.
                    return@flow
                }
                if (newRowsInWindow == 0) {
                    throw NotionException.IterationStalled(
                        keyDescription = tracker.keyDescription,
                        boundaryValue = tracker.boundaryValue,
                    )
                }
            }
        }
    }

    private fun combineFilters(
        base: DataSourceFilter?,
        window: DataSourceFilter?,
    ): DataSourceFilter? =
        when {
            window == null -> base
            base == null -> window
            else -> DataSourceFilter(and = listOf(base, window))
        }

    private fun newTracker(key: RowIterationKey): KeyTracker =
        when (key) {
            is RowIterationKey.CreatedTime -> CreatedTimeTracker()
            is RowIterationKey.UniqueId -> UniqueIdTracker(key.propertyName)
        }

    /**
     * Tracks the iteration key across windows: produces the sort and window filter,
     * and decides for each row whether it is new (emit) or a boundary repeat (skip).
     */
    private sealed interface KeyTracker {
        val keyDescription: String
        val boundaryValue: String?

        fun sort(): DataSourceSort

        fun windowFilter(): DataSourceFilter?

        /** Returns `true` if the row has not been emitted before and should be emitted. */
        fun accept(page: Page): Boolean
    }

    /**
     * Windows on `created_time` (immutable, but rounded to the minute — ties are
     * common). Re-opens windows with `on_or_after` the boundary timestamp and
     * de-duplicates the boundary bucket by page id.
     */
    private class CreatedTimeTracker : KeyTracker {
        override val keyDescription = "created_time"
        override var boundaryValue: String? = null
            private set

        private var boundaryInstant: Instant? = null
        private val boundaryIds = mutableSetOf<String>()

        override fun sort() = DataSourceSort(timestamp = "created_time", direction = SortDirection.ASCENDING)

        override fun windowFilter(): DataSourceFilter? =
            boundaryValue?.let {
                DataSourceFilter(
                    timestamp = "created_time",
                    createdTime = DateCondition(onOrAfter = it),
                )
            }

        override fun accept(page: Page): Boolean {
            val instant = Instant.parse(page.createdTime)
            val boundary = boundaryInstant
            return if (boundary == null || instant > boundary) {
                boundaryInstant = instant
                boundaryValue = page.createdTime
                boundaryIds.clear()
                boundaryIds.add(page.id)
                true
            } else {
                // Same created_time as the boundary (or, defensively, earlier):
                // emit only ids not already seen in this bucket.
                boundaryIds.add(page.id)
            }
        }
    }

    /**
     * Windows on a `unique_id` property (strictly increasing, immutable — no ties).
     * Re-opens windows with a strict `greater_than` filter on the last seen number.
     */
    private class UniqueIdTracker(
        private val propertyName: String,
    ) : KeyTracker {
        override val keyDescription get() = "unique_id property \"$propertyName\""
        override val boundaryValue get() = lastNumber?.toString()

        private var lastNumber: Int? = null

        override fun sort() = DataSourceSort(property = propertyName, direction = SortDirection.ASCENDING)

        override fun windowFilter(): DataSourceFilter? =
            lastNumber?.let {
                DataSourceFilter(
                    property = propertyName,
                    uniqueId = UniqueIdCondition(greaterThan = it),
                )
            }

        override fun accept(page: Page): Boolean {
            val number =
                page.getUniqueIdProperty(propertyName)?.number
                    ?: throw NotionException.ValidationError(
                        field = propertyName,
                        details =
                            "Row ${page.id} has no unique_id value for property \"$propertyName\". " +
                                "RowIterationKey.UniqueId requires a unique_id property present on every row.",
                    )
            val last = lastNumber
            if (last != null && number <= last) {
                // Should not happen with a strict greater_than window; skip defensively.
                return false
            }
            lastNumber = number
            return true
        }
    }
}
