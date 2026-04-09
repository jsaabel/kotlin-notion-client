# Development Journal - April 9, 2026

## Phase 6 (Part 1): Writable Verification Property

### Objective

Implement read and write support for the `verification` page property, which is available on pages
in wiki databases. Pages can be verified, unverified, or expired (when the verification end date is
in the past).

---

## API Notes

- `verification` is read/write via Create Page and Update Page endpoints
- `verified_by` is **read-only** — automatically set to the acting integration; must not appear in write requests
- `state` can be `"verified"`, `"unverified"`, or `"expired"` (expired is API-returned only, not settable)
- Only available on pages inside wiki databases — wiki databases cannot be created programmatically,
  which constrains integration testing (see below)

---

## Changes Made

### `PageProperty.kt`

- Added `PageProperty.Verification` subclass with nullable `VerificationData`
- Added `VerificationData(state, verifiedBy, date)`:
  - `verifiedBy: User?` — read-only, nullable
  - `date: DateData?` — reuses the existing `DateData` class (start/end/time_zone)

### `PageRequests.kt`

- Added `PagePropertyValue.VerificationValue` wrapping `VerificationRequest`
- Added `VerificationRequest(state, date?)` — `verified_by` deliberately omitted (read-only)

### `PagePropertySerializer.kt`

- `"verification"` now routes to `PageProperty.Verification` in both `deserialize` and `serialize`
- Previously fell through to `PageProperty.Unknown`

### `PagePropertiesBuilder.kt`

- `verify(name, start?, end?)` — sets state to `"verified"` with optional ISO 8601 date range
- `unverify(name)` — sets state to `"unverified"`

### Unit tests — `VerificationPropertyTest.kt` (new)

9 tests across deserialization and serialization:

- Unverified state: state, null verified_by, null date
- Verified without expiry: state, verified_by user id, date.start, null date.end
- Verified with 90-day expiry: date.start and date.end both present
- Null verification object (forward-compat edge case)
- `unverify()` serializes `state: "unverified"` with no date
- `verify()` without dates serializes correctly
- `verify()` with start date only
- `verify()` with both start and end
- `VerificationValue` serializes under the `"verification"` key

### `PagePropertyUnknownTypeTest.kt` — updated

The existing mixed-types test expected `Verification` to deserialize as `Unknown`. Updated to
assert `PageProperty.Verification` instead.

### Integration test — `WikiVerificationIntegrationTest.kt` (new)

Throwaway test gated on `NOTION_TEST_WIKI_PAGE_ID`. Steps:

1. Read current state
2. Verify with a 90-day window (assert state, date.start, date.end)
3. Unverify (assert state = "unverified")
4. Re-verify (assert state = "verified") — leaves the page verified for visual inspection

Run with:
```
export NOTION_TEST_WIKI_PAGE_ID="<id>"
./gradlew integrationTest --tests "*WikiVerificationIntegrationTest*"
```

---

## Integration Testing Constraint

Wiki databases cannot be created programmatically, so this test cannot be self-contained. It
requires a manually pre-created wiki page, similar to `NewFilterTypesIntegrationTest`. The
`NOTION_TEST_WIKI_PAGE_ID` environment variable gates the test so it skips gracefully when not set.

---

## Status

- [x] Read model — `PageProperty.Verification` + `VerificationData`
- [x] Write model — `PagePropertyValue.VerificationValue` + `VerificationRequest`
- [x] Serializer wired for both directions
- [x] DSL — `verify()` and `unverify()` in `PagePropertiesBuilder`
- [x] Unit tests — 9 tests, all passing
- [x] Integration test — pending live API validation against a wiki page