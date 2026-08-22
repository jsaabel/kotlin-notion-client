package it.saabel.kotlinnotionclient.models.datasources

/**
 * The monotonic key used by [it.saabel.kotlinnotionclient.api.DataSourcesApi.iterateAllRows]
 * and [it.saabel.kotlinnotionclient.api.ViewsApi.iterateAllRows] to window past Notion's
 * 10,000-row query result cap.
 *
 * The iteration sorts rows ascending by this key and, whenever the API truncates the
 * result set (`request_status.type == "incomplete"`), re-queries from the last seen
 * key value to open the next window.
 *
 * `last_edited_time` is deliberately not offered as a key: it changes whenever a row
 * is edited, so a row edited mid-drain moves within the ordering and can be skipped
 * or duplicated. Detecting such duplicates would require remembering every emitted
 * row id, which does not scale to the data sources this helper exists for.
 */
sealed interface RowIterationKey {
    /**
     * Windows on the page's `created_time` timestamp (the default).
     *
     * Works on every data source without schema requirements, and `created_time`
     * never changes after a row is created. However, Notion rounds `created_time`
     * to the nearest minute, so ties are common: windows re-open with an
     * `on_or_after` filter at the boundary timestamp and rows already emitted at
     * that timestamp are de-duplicated by page id.
     *
     * Limitation: if more than 10,000 rows share a single `created_time` value
     * (e.g. a bulk import within one minute), the iteration cannot advance past
     * that value and throws
     * [it.saabel.kotlinnotionclient.exceptions.NotionException.IterationStalled].
     * Use [UniqueId] in that case.
     */
    data object CreatedTime : RowIterationKey

    /**
     * Windows on a `unique_id` property of the data source.
     *
     * Unique IDs are strictly increasing and immutable, so windows re-open with a
     * strict `greater_than` filter — no ties, no de-duplication, and guaranteed
     * progress. This is the most robust key, but requires the data source to have
     * a unique ID property.
     *
     * Limitation (observed live): when the unique ID property was recently added to
     * an existing data source, Notion backfills IDs asynchronously and rows still
     * awaiting theirs carry a `null` number. Such rows are **silently excluded** by
     * the `greater_than` window filter (and sort last within the first window), so a
     * drain during backfill undercounts without any error. Drain only once the ID
     * column is fully populated, or use [CreatedTime].
     *
     * @property propertyName The name of the `unique_id` property (e.g. "ID").
     */
    data class UniqueId(
        val propertyName: String,
    ) : RowIterationKey
}
