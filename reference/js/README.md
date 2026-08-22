# JS SDK reference (vendored excerpt)

Prior art for large-data-source iteration patterns, cited in issue #40
(`iterateAllDataSourceRows` / `collectAllDataSourceRows`) and referenced by
`ViewsApi`/`DataSourcesApi` KDoc in this project.

## What this is

`pagination-helpers.ts` is a trimmed excerpt of one file from the official
JavaScript/TypeScript Notion SDK:

- **Source**: <https://github.com/makenotion/notion-sdk-js>
- **File**: `src/helpers.ts`
- **Commit**: `9060802d386afbb82fa7b52ba4298cce0b1e0429` (2026-08-20)
- **Package version**: `5.26.0`
- **License**: MIT — Copyright 2021 Notion Labs, Inc. (full text in
  [`LICENSE`](./LICENSE) in this directory, reproduced per the MIT license's
  own terms)

Only the generic pagination helpers (`iteratePaginatedAPI`,
`collectPaginatedAPI`) and the >10,000-row data source drain
(`iterateAllDataSourceRows`, `collectAllDataSourceRows`, and the two small
helpers they depend on) are included — not the whole SDK. The rest of
`helpers.ts` (data source template iteration, the `isFull*` type guards for
other object kinds, `extractNotionId` and friends) was left out as unrelated
to the pagination/iteration question this excerpt exists to answer. The file
is not meant to compile standalone — imports from the SDK's generated
`api-endpoints` types were dropped; see the header comment in the file for
what changed relative to the upstream source.

## Why an excerpt instead of a full local clone

`reference/python/` uses a different convention: its `.gitignore` ignores
everything except itself, so it's a placeholder directory an agent is
expected to `git clone` the Python SDK into locally, on demand, rather than a
committed vendor copy. That works well for a whole-SDK cross-reference.

This directory takes the opposite approach on purpose: only a couple of
functions from the JS SDK are actually relevant prior art (the `>10,000`-row
windowed-query pattern), so vendoring just those — committed, not
gitignored — means any agent can read them without a network fetch or a
clone step, at the cost of this excerpt drifting from upstream over time if
`notion-sdk-js` changes its approach. If a broader JS SDK cross-reference is
ever needed, prefer adding a `reference/js/.gitignore` placeholder (mirroring
`reference/python/`) rather than vendoring more files here.

## How this maps onto our implementation

Our windowed drain lives in
`src/main/kotlin/it/saabel/kotlinnotionclient/api/DataSourcesApi.kt`
(`iterateAllRows`) and is exposed identically on `ViewsApi` (draining the
view's underlying data source, since the Views API itself has no windowing
parameters — see `FOLLOWUPS.md` item 25). The core idea — partition by
`created_time` ascending, restart the query from the last-seen boundary
timestamp when `request_status.type == "incomplete"`, de-duplicate rows that
land on the boundary — matches this vendored helper. The windowing key choice
(`created_time`) was independently arrived at (item 26 in `FOLLOWUPS.md`
notes Notion's own "Query large data sources" guide 404s at every obvious
URL), and this excerpt is the closest confirmation available that it's the
same approach the official SDK settled on.
