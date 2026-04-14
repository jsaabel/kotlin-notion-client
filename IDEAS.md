# Ideas & Future Improvements

A running log of improvement ideas captured during development. These are asides that don't belong to any specific task but are worth revisiting.

Use `/capture-idea <your idea>` to add entries. Use `/implement-idea <row number>` to mark one as implemented.

**Status values:** `open` · `in progress` · `done` · `wont-do`

---

| # | Date | Idea | Status |
|---|------|------|--------|
| 1 | 2026-04-06 | Integration tests should print out links to created pages, to make verification more convenient | done   |
| 2 | 2026-04-06 | Reconsider the `PageIcon` sealed class name — icons are used across pages, databases, callouts, tabs, and (soon) native icons. A more neutral name like `NotionIcon` or `BlockIcon` might better reflect the concept's scope. Worth evaluating before the 0.4.0 release since it's a breaking rename. | done  |
| 3 | 2026-04-06 | the integration tests should be consolidated at some point. right now we have too many doing very dedicated things, which has its merits, but for verification it would be more convenient to reduce on the number of tests, combining functionality rather than removing tested features | done   |
| 4 | 2026-04-09 | `NewFilterTypesIntegrationTest` relies on manual setup (persistent `NOTION_TEST_DATASOURCE_ID`) because `status` and `unique_id` properties couldn't be created via API when it was written. Once Phase 6 adds status property creation to `DatabaseRequestBuilder`, this test should be refactored to be self-contained (create database → populate → assert → cleanup) like `DataSourcesIntegrationTest`. | open   |
| 5 | 2026-04-13 | Normalize property IDs from DataSource schema: the API returns them percent-encoded (e.g. `ue%5Cl`) in the data source schema but decoded (e.g. `ue\l`) in view filter/sort responses. Consider URL-decoding IDs transparently during deserialization of DataSource properties so callers always get consistent IDs. | open   |
| 6 | 2026-04-13 | Database icons set via `databases.create { icon.emoji(...) }` were not persisting in the UI because Notion renders the *data source* view (not the container). Fixed 2026-04-14: `DatabasesApi.create()` now auto-propagates the icon to the initial data source via a follow-up PATCH to `/data_sources/{id}` immediately after creation. | done   |
| 7 | 2026-04-14 | Kotlin Notebooks seem to work again (tested with 0.3.0). Update them to use 0.4.0, make any adjustments that new 0.3/0.4 functionality requires. Consider dedicated notebooks introducing 0.3.0 and 0.4.0 features respectively, and reference them in the README/release notes. Add a note on which IntelliJ IDEA version is currently in use. | open |
| 8 | 2026-04-14 | Timezone functionality may need a proper test and adjustments — see example in `05-rich-text-dsl.ipynb`. Fixed 2026-04-14: `dateMention(LocalDateTime, TimeZone)` was converting to a UTC instant before sending, causing Notion to misinterpret the time. Fixed to pass the local datetime string + `timezone.id` directly. Notion never preserves named timezones in responses (always returns `time_zone=null` with a numeric offset like `-04:00`). Verified via `TimezoneIntegrationTest`. | done |
| 9 | 2026-04-14 | Comprehensive analysis of and update plan for README/documentation before 0.4.0 release. | open |