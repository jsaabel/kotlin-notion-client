# Follow-ups from the orchestrated issue run

Collected from implementing-agent reports on issues #32–#42, landed on the shared
feature branch `claude/kotlin-notion-orchestrate-9ztk5a`. Every entry is something
an agent **noticed and deliberately did not fix** because it fell outside the scope
of its issue. Nothing here has been independently verified — each item is recorded
as the agent reported it.

Status: `open` · `filed` (issue created) · `done` · `wont-do`

| # | From | Follow-up | Status |
|---|------|-----------|--------|
| 1 | #32 | `docs/error-handling.md` (~lines 300–345) documents a `RateLimitConfig` API that no longer exists (`strategy`, `baseDelayMs`, `maxDelayMs`, `respectRetryAfter`, and the CONSERVATIVE/BALANCED/AGGRESSIVE presets), and never states which statuses are retried. Already stale before this run. Resync it and document the 429/529/502/503/504 retry matrix. | open |
| 2 | #32 | No typed exception for service overload — a `529` that exhausts retries surfaces as a generic `NotionException.ApiError` with `status = 529`. A dedicated subtype, or one carrying `retryAfterSeconds`, would be an improvement. | open |
| 3 | #33 | Vendored API reference `reference/notion-api/` is behind the live changelog — nothing newer than `documentation/general/12_Upgrade_Guide_2026-03-11.md`. Agents are web-fetching the live changelog to confirm field shapes. Refreshing the vendored docs would remove that dependency. **Corroborated by #34:** `filter_properties` appears nowhere in `reference/notion-api/` — not in `NotionAPI.yml`, not in the sample responses. | open |
| 4 | #33 | `SearchFilter.property` still defaults to `"object"` in the primary constructor, so a hand-written `SearchFilter(inTrash = true)` that bypasses the builder emits a spurious `"property":"object"` alongside `in_trash`. Needs a decision: flip the default to `null` (behaviour change for direct constructor callers) or add `init` validation. | open |
| 5 | #33 | No `is_archived` convenience overload on `DataSourcesApi.query`/`queryAsFlow`/`queryFirstPage`, and no trash variant of `SearchApi.search(query: String)`. Both are reachable via the request/builder objects; left alone for minimal scope, but worth filing if the ergonomics bite. | open |
| 6 | #34 | IDEAS.md #5 is **not** closed by #34. The percent-decode fix lives in `PagesApi` only (`decodePropertyId`); normalising property IDs during `DataSource` schema deserialisation — what #5 actually asks for — is untouched. If #5 is picked up, consider promoting `decodePropertyId` into a shared util, which would make the `PagesApi` helper redundant. | open |
| 7 | #34 | `filter_properties` is documented as capped at 100 IDs; no validation was added, since `RequestValidator` is body-shaped and this is a query-param concern. Add fail-fast validation if wanted. | open |
| 8 | #34 | The same `filter_properties` parameter exists on data source query (`POST /v1/data_sources/{id}/query`), where the payload saving is larger still because we auto-paginate there. Out of scope for #34 — worth filing separately. | open |

## Notes

- Issues #32–#42 are **not** auto-closed by their PRs: the PRs target this shared
  feature branch rather than `main`, so `Closes #NN` does not fire. They stay open
  until the shared branch lands on `main`.
