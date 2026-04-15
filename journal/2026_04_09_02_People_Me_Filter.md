# Development Journal - April 9, 2026

## Phase 5 (Part 2): "me" Filter for People Properties

### Objective

Add support for `"me"` as a filter value in people property conditions (`contains` / `does_not_contain`).

---

## API Change

People filter conditions now accept `"me"` as a value in addition to user UUIDs. For public
integrations `"me"` resolves to the user who authorized the connection. For internal integrations
(which is what we test with) `"me"` does not resolve to a user — `contains: "me"` always returns
no results and `does_not_contain: "me"` matches all entries.

Applies to `people`, `created_by`, and `last_edited_by` property filters.

---

## Approach

`PeopleCondition` fields are already `String?`, so no model changes were needed. Exactly the same
pattern as the relative date values: the new behaviour is purely additive DSL convenience methods.

---

## Changes Made

### `DataSourceQueryBuilder.kt`

Added two methods to `PeopleFilterBuilder`:

- `containsMe()` — creates `PeopleCondition(contains = "me")`
- `doesNotContainMe()` — creates `PeopleCondition(doesNotContain = "me")`

### Unit tests — `PeopleMeFilterTest.kt` (new)

Two tests in a single `context` block:

- `containsMe()` serializes to `"contains":"me"`
- `doesNotContainMe()` serializes to `"does_not_contain":"me"`

### Integration tests — `NewFilterTypesIntegrationTest.kt`

Added two tests after the existing "contains specific user" test. This file already has a database
with a people "Assignee" property and uses `NOTION_TEST_USER_ID`, making it the natural home.

- `containsMe()` — asserts 0 results (expected for internal integration)
- `doesNotContainMe()` — asserts non-empty results (expected for internal integration)

Also added the missing `import io.kotest.matchers.shouldBe`.

---

## Note on `NewFilterTypesIntegrationTest` Manual Setup

While adding the integration tests we noticed this test relies on a manually pre-created
`NOTION_TEST_DATASOURCE_ID` rather than creating its own test data. This was necessary at the
time because `status` and `unique_id` property types couldn't be created via the API.

Once Phase 6 adds status property creation to `DatabaseRequestBuilder`, this test should be
refactored to be fully self-contained (create → populate → assert → cleanup), like
`DataSourcesIntegrationTest`. Tracked as idea #4 in `IDEAS.md`.

---

## Status

- [x] `PeopleFilterBuilder.containsMe()` and `doesNotContainMe()`
- [x] Unit tests — 2 tests, all passing
- [x] Integration tests added to `NewFilterTypesIntegrationTest` — pending live API validation
- [x] Phase 5 marked complete in `UPGRADE_PLAN.md`