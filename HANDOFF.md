# Handoff: follow-ups and live-API verification

## What happened before you

Eleven changelog-driven issues (#32–#42) were implemented back-to-back by one agent each and
squash-merged into the shared feature branch **`claude/kotlin-notion-orchestrate-9ztk5a`**. That
branch is ~21 commits ahead of `main` and **has not been merged**; issues #32–#42 are still open,
because `Closes #NN` does not fire against a feature branch. Each issue carries a comment
summarising what landed and what the implementing agent flagged.

Work from this branch. Do not start from `main` — it has none of this.

## Your inputs

- **`FOLLOWUPS.md`** (repo root) — 38 numbered items, each sourced from an implementing agent's
  report and **not independently verified**. Treat every row as a claim to check, not a fact.
  Items are tagged with the issue that surfaced them.
- The per-issue comments on GitHub issues #32–#42, which carry the same feedback in context.
- The batched `CHANGELOG.md` entry under `## [Unreleased]` → "August 2026 API catch-up", which
  states the three source-breaking changes.

## The headline gap — item 38

**No integration test ran during the entire run.** Every feature was built from live docs plus
unit tests with mocked responses; none was exercised against a real workspace. Four features are
implemented at high confidence but completely unverified against the API:

| Feature | Issue | What specifically needs live confirmation |
|---|---|---|
| Async tasks + async markdown writes | #38 | The 202 acceptance flow, and optionality of `operation`/`created_time` (item 20). A 202 cannot be forced deterministically — the API decides — so design for a test that tolerates the `Completed` fallback. |
| Status option groups | #39 | Group assignment round-tripping on create and update (item 23). `DatabaseFeaturesIntegrationTest`'s existing "verify standard groups" test is the natural home. |
| >10k-row windowed drain | #40 | `has_more`/`next_cursor` behaviour at the cap, and whether a `unique_id` property supports property sorts (item 26). Needs a data source with >10,000 rows — the expensive one. |
| Formula writes + readable `prop()` | #42 | Verbatim `prop()` storage on write, and the read path (item 34). The readable-syntax rollout is **gradual**, so read assertions must tolerate both readable and legacy `{{notion:block_property:...}}` syntax — `FormulaConfiguration.usesInternalReferences()` detects the legacy form. |

## Test-infrastructure facts you will otherwise rediscover the hard way

- **`./gradlew integrationTest` and `./gradlew testAll` do not exist.** CLAUDE.md documents them;
  `build.gradle.kts` has only `tasks.test { useJUnitPlatform() }` with no tag-based split. This is
  follow-up item 9 and is arguably yours to fix first, since your work is integration testing.
- A bare `./gradlew test` **does** load `integration.*` specs. They are guarded by their own
  `@Tags("Integration","RequiresApi")` and env-var checks, not by the build script.
  `-Dkotest.tags.include="Unit"` loads-but-skips them; `--tests "unit.*"` does not load them at all.
- Integration tests need `NOTION_API_TOKEN` and `NOTION_TEST_PAGE_ID`. They were unset for the
  whole previous run, which is why no stray live call was possible.
- **CLAUDE.md rule that still binds you: never run all integration tests at once.** Individual
  live tests are fine when warranted.

## Known traps

- **`reference/notion-api/` is materially stale** (item 3, independently corroborated by four
  agents): nothing newer than `2026-03-11`, and no material at all on `filter_properties`,
  truncation metadata, async tasks, status groups or webhooks. Check live docs. Refreshing the
  vendored reference is itself a high-value follow-up.
- **Fixture/model mismatch, item 15:** `TestFixtures.DataSources.retrieveDataSource().decode<DataSource>()`
  throws `MissingFieldException: Field 'description'` — the model requires it, the official sample
  omits it. Nothing decodes that fixture today, so it is latent.
- **IDEAS.md #4 is still blocked** (item 22) despite #39's "bonus" claim: `NewFilterTypesIntegrationTest`
  needs a `unique_id` property, which the API still does not let you create. This also constrains
  how you test #40's `UniqueId` iteration key.
- Two unratified design choices worth a decision before release: webhook verification rejects
  uppercase-hex signatures (item 30), and `pollAsFlow` does not throw on a failed task while
  `waitForCompletion` does (item 21).

## Conventions

Conventional commits. `./gradlew formatKotlin` before every build. Branch per unit of work off the
shared branch, PR into it. Update `FOLLOWUPS.md` as you resolve items — mark them `done` with a
one-line note on what you actually found, since several rows will turn out to be wrong.
