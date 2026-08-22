// Vendored excerpt — see reference/js/README.md for origin, version, and license.
//
// Source: https://github.com/makenotion/notion-sdk-js
// File:   src/helpers.ts
// Commit: 9060802d386afbb82fa7b52ba4298cce0b1e0429 (2026-08-20), package version 5.26.0
// License: MIT — Copyright 2021 Notion Labs, Inc. (see reference/js/LICENSE)
//
// This is a trimmed excerpt: only the generic pagination helpers
// (iteratePaginatedAPI / collectPaginatedAPI) and the >10,000-row data source
// drain (iterateAllDataSourceRows / collectAllDataSourceRows, plus the
// createdTimeLowerBound and isFullPageOrDataSource helpers they depend on) are
// included. Everything else in the upstream file (data source templates,
// isFull* type guards for other object kinds, extractNotionId and friends) was
// left out as not relevant to the pagination/iteration question this excerpt
// exists to answer. Comments and code below are reproduced verbatim from the
// source line ranges noted; only import paths were dropped since this file is
// not meant to compile standalone.

type PaginatedArgs = {
  start_cursor?: string | null
}

type PaginatedList<T> = {
  object: "list"
  results: T[]
  next_cursor: string | null
  has_more: boolean
}

/**
 * Returns an async iterator over the results of any paginated Notion API.
 *
 * Example (given a notion Client called `notion`):
 *
 * ```
 * for await (const block of iteratePaginatedAPI(notion.blocks.children.list, {
 *   block_id: parentBlockId,
 * })) {
 *   // Do something with block.
 * }
 * ```
 *
 * @param listFn A bound function on the Notion client that represents a conforming paginated
 *   API. Example: `notion.blocks.children.list`.
 * @param firstPageArgs Arguments that should be passed to the API on the first and subsequent
 *   calls to the API. Any necessary `next_cursor` will be automatically populated by
 *   this function. Example: `{ block_id: "<my block id>" }`
 */
export async function* iteratePaginatedAPI<Args extends PaginatedArgs, Item>(
  listFn: (args: Args) => Promise<PaginatedList<Item>>,
  firstPageArgs: Args
): AsyncIterableIterator<Item> {
  let nextCursor: string | null | undefined = firstPageArgs.start_cursor
  do {
    const response: PaginatedList<Item> = await listFn({
      ...firstPageArgs,
      start_cursor: nextCursor,
    })
    yield* response.results
    nextCursor = response.next_cursor
  } while (nextCursor)
}

/**
 * Collect all of the results of paginating an API into an in-memory array.
 *
 * Example (given a notion Client called `notion`):
 *
 * ```
 * const blocks = await collectPaginatedAPI(notion.blocks.children.list, {
 *   block_id: parentBlockId,
 * })
 * // Do something with blocks.
 * ```
 *
 * @param listFn A bound function on the Notion client that represents a conforming paginated
 *   API. Example: `notion.blocks.children.list`.
 * @param firstPageArgs Arguments that should be passed to the API on the first and subsequent
 *   calls to the API. Any necessary `next_cursor` will be automatically populated by
 *   this function. Example: `{ block_id: "<my block id>" }`
 */
export async function collectPaginatedAPI<Args extends PaginatedArgs, Item>(
  listFn: (args: Args) => Promise<PaginatedList<Item>>,
  firstPageArgs: Args
): Promise<Item[]> {
  const results: Item[] = []
  for await (const item of iteratePaginatedAPI(listFn, firstPageArgs)) {
    results.push(item)
  }
  return results
}

// --- >10,000-row data source drain -----------------------------------------
// (DataSourceRow, FullDataSourceQueryFilter, FullDataSourceQueryArgs are the
// upstream file's own type aliases over its generated API types; reproduced
// here only in comment form since those generated types are not vendored.)
//
// type DataSourceRow = QueryDataSourceResponse["results"][number]

/**
 * A filter the full-query helpers can combine with a created_time window bound.
 *
 * A top-level `or` filter is intentionally excluded: the helpers add a
 * created_time lower bound with `and`, and Notion only supports two levels of
 * filter nesting, so an `or` at the root leaves no room for the bound. Express
 * such queries as an `and` group, or run the helper once per `or` branch and
 * merge the results.
 */
export type FullDataSourceQueryFilter =
  | PropertyFilter
  | TimestampFilter
  | { and: GroupFilterOperatorArray }

/**
 * Arguments for the full-query helpers. The same as `dataSources.query`, minus
 * the fields the helper controls: `start_cursor` (pagination is automatic) and
 * `sorts` (the helper sorts by created_time to partition). `filter` is narrowed
 * to {@link FullDataSourceQueryFilter}.
 */
export type FullDataSourceQueryArgs = Omit<
  QueryDataSourceParameters,
  "start_cursor" | "sorts" | "filter"
> & {
  filter?: FullDataSourceQueryFilter
}

/**
 * Iterate over every row of a data source, including rows past the per-query
 * result limit that `dataSources.query` enforces on large data sources.
 *
 * A single query (one filter and sort) returns at most a fixed number of rows
 * (10,000 by default). Once that limit is reached, `has_more` becomes `false`
 * and the response carries `request_status.type === "incomplete"`. Plain
 * pagination such as {@link iteratePaginatedAPI} stops there and silently
 * misses the rest of the data source.
 *
 * This helper works around the limit by partitioning the data source into
 * created_time windows. It sorts by created_time ascending; whenever a window
 * reaches the limit, it starts a fresh query from the last row's created_time.
 * Each fresh query has a different filter, so it gets its own result budget.
 * Rows that share a boundary timestamp are de-duplicated by id, so every row is
 * yielded exactly once.
 *
 * created_time is used because it never changes. last_edited_time would shift
 * rows between windows as they are edited, causing gaps or duplicates.
 *
 * Throws if a single created_time value holds more rows than the limit, since
 * the window cannot be narrowed by time alone. Add a filter in that case so
 * each window stays under the limit.
 *
 * Example (given a notion Client called `notion`):
 *
 * ```
 * for await (const row of iterateAllDataSourceRows(notion, {
 *   data_source_id: dataSourceId,
 * })) {
 *   // Do something with row.
 * }
 * ```
 *
 * @param client A Notion client instance.
 * @param args Query arguments. `start_cursor` and `sorts` are managed by the
 *   helper; `filter` is combined with the created_time window bound.
 */
export async function* iterateAllDataSourceRows(
  client: Client,
  args: FullDataSourceQueryArgs
): AsyncIterableIterator<DataSourceRow> {
  const seenRowIds = new Set<string>()
  let windowStart: string | undefined = undefined

  for (;;) {
    let limitReached = false
    let lastCreatedTime: string | undefined = undefined
    let cursor: string | undefined = undefined

    do {
      const response = await client.dataSources.query({
        ...args,
        sorts: [{ timestamp: "created_time", direction: "ascending" }],
        filter: createdTimeLowerBound(args.filter, windowStart),
        start_cursor: cursor,
      })
      for (const row of response.results) {
        // Wiki data sources can return child data-source rows alongside pages.
        // Both carry created_time and either can be the window's boundary row,
        // so advance on either; reading only pages would stall a window made up
        // of data-source rows and throw spuriously.
        if (isFullPageOrDataSource(row)) {
          lastCreatedTime = row.created_time
        }
        if (!seenRowIds.has(row.id)) {
          seenRowIds.add(row.id)
          yield row
        }
      }
      if (response.request_status?.type === "incomplete") {
        limitReached = true
      }
      cursor = response.next_cursor ?? undefined
    } while (cursor)

    if (!limitReached) {
      return
    }
    if (lastCreatedTime === undefined || lastCreatedTime === windowStart) {
      throw new Error(
        "iterateAllDataSourceRows cannot make progress: the per-query result " +
          "limit was reached but the created_time window could not advance " +
          `past ${String(lastCreatedTime)}. More rows share this timestamp ` +
          "than the limit allows. Add a filter to narrow the query."
      )
    }
    windowStart = lastCreatedTime
  }
}

/**
 * Collect every row of a data source into an in-memory array, including rows
 * past the per-query result limit. See {@link iterateAllDataSourceRows} for how
 * the limit is handled.
 *
 * Before using this, check that the full data source fits in memory. For very
 * large data sources, prefer {@link iterateAllDataSourceRows} and process rows
 * as they stream.
 *
 * Example (given a notion Client called `notion`):
 *
 * ```
 * const rows = await collectAllDataSourceRows(notion, {
 *   data_source_id: dataSourceId,
 * })
 * // Do something with rows.
 * ```
 *
 * @param client A Notion client instance.
 * @param args Query arguments. See {@link iterateAllDataSourceRows}.
 */
export async function collectAllDataSourceRows(
  client: Client,
  args: FullDataSourceQueryArgs
): Promise<DataSourceRow[]> {
  const rows: DataSourceRow[] = []
  for await (const row of iterateAllDataSourceRows(client, args)) {
    rows.push(row)
  }
  return rows
}

/**
 * Combine a caller-provided filter with the created_time lower bound used to
 * advance the partition window. Returns the filter unchanged for the first
 * window, which has no bound yet.
 */
function createdTimeLowerBound(
  filter: FullDataSourceQueryFilter | undefined,
  windowStart: string | undefined
): FullDataSourceQueryFilter | undefined {
  if (windowStart === undefined) {
    return filter
  }
  const bound: TimestampFilter = {
    timestamp: "created_time",
    created_time: { on_or_after: windowStart },
  }
  if (filter === undefined) {
    return bound
  }
  if ("and" in filter) {
    return { and: [...filter.and, bound] }
  }
  return { and: [filter, bound] }
}

/**
 * Type guard for `isFullPageOrDataSource` — abbreviated here to its signature
 * and dispatch only; the underlying `isFullPage`/`isFullDataSource` guards it
 * calls are upstream boilerplate (object-key presence checks) not reproduced
 * in this excerpt.
 */
export function isFullPageOrDataSource(
  response: ObjectResponse
): response is DataSourceObjectResponse | PageObjectResponse {
  if (response.object === "data_source") {
    return isFullDataSource(response)
  } else {
    return isFullPage(response)
  }
}
