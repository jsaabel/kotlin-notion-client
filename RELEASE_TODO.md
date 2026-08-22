# v0.6.0 Release — Remaining Steps

**Prepared**: 2026-08-22
**Status**: All feature work is merged into `claude/kotlin-notion-orchestrate-9ztk5a`
(1,233 unit tests green). Release PR #79 targets `main`. The consolidated changelog is in
`CHANGELOG.md` `[Unreleased]`; the concise release-notes draft is `RELEASE_NOTES.md`.

**Strategy**: publish a `0.6.0-SNAPSHOT` first and battle-test it in
**festival-scripts** (a heavy consumer of this library) to catch blatant issues —
especially around the breaking date/DSL changes — before the proper public `0.6.0`.

---

## Step 0 — Land the release branch

- [ ] Walk the open decisions in issue #62 (design ratifications). None block the
      snapshot; the webhook/docs items can land as follow-ups.
- [ ] Review and merge PR #79 (`claude/kotlin-notion-orchestrate-9ztk5a` → `main`).
- [ ] Decide PR #43 (`chore/dependency-refresh-and-backlog`): its diff **predates and
      would revert** the `api`-scope dependency fix from #44, and its CHANGELOG edits
      conflict with the consolidation. Recommendation: close it as superseded and redo
      the version bumps (kotest 6.2.4, coroutines 1.11.0, ktor 3.5.2, logback 1.6.3,
      kotlinter 5.7.0, maven-publish 0.37.0, ben-manes plugin id) freshly on `main`
      after the merge.

## Step 1 — Branch housekeeping

Remote branch deletion is blocked from the agent environment (push credentials are
scoped per-branch), so run this locally. Every branch below is verified merged — via a
merged PR into the release branch or `main` — or superseded with its content confirmed
present on `main`:

```bash
git push origin --delete \
  claude/issue-32-retry-529 claude/issue-33-trash-archive-queries \
  claude/issue-34-filter-properties claude/issue-35-unknown-block-count \
  claude/issue-36-unsupported-value claude/issue-37-app-notion-domain \
  claude/issue-38-async-tasks claude/issue-39-status-groups \
  claude/issue-40-large-iteration claude/issue-41-webhook-verification \
  claude/issue-42-formula-expressions claude/kotlin-notion-issue-56-k0talr \
  claude/issue-57-rollup-incomplete claude/issue-58-polish-batch \
  claude/issue-59-sibling-surfaces claude/issue-60-reference-refresh \
  claude/kotlin-notion-client-68-08xxap claude/kotlin-notion-issue-69-m7txdo \
  claude/kotlin-notion-issue-70-mku5i7 claude/issue-75-z4e28s claude/issue-76-gmfav9 \
  claude/friendly-heisenberg-p6j441 claude/kotlin-notion-client-81-tk3033 \
  claude/kotlin-notion-issue-82-l390xp claude/kotlin-notion-followups-triage-vam3ys \
  claude/kotlin-notion-client-deps-8qe77j claude/kotlin-notion-client-26-hlnwp1 \
  claude/kotlin-notion-issue-27-i0j2by claude/kotlin-notion-client-28-iwc8bz \
  claude/gallant-einstein-FfiyI claude/serene-lamport-1twK3 \
  claude/vigilant-brahmagupta-QokTn claude/tender-wright-upK4L \
  claude/dazzling-ramanujan-JCzOJ claude/sharp-gauss-hDzBo \
  claude/practical-johnson-IVWJr claude/brave-ptolemy-iRwoq \
  claude/loving-faraday-5dRpO claude/kind-hopper-9OR8v \
  claude/beautiful-brown-C6i2u claude/upbeat-tesla-alqIf claude/epic-ritchie-qrlw8 \
  feature/v0.4
```

Kept: `main`, `claude/kotlin-notion-orchestrate-9ztk5a` (delete after #79 merges),
`chore/dependency-refresh-and-backlog` (until the PR #43 decision above).

## Step 2 — Snapshot into festival-scripts

`gradle.properties` already reads `version=0.6.0-SNAPSHOT`.

- [ ] From `main` after the merge: `./gradlew publishToMavenLocal`
      (or publish the snapshot to Central's snapshot repository if preferred).
- [ ] In festival-scripts, depend on `it.saabel:kotlin-notion-client:0.6.0-SNAPSHOT`
      (with `mavenLocal()` first in `repositories` if using the local publish).
- [ ] Migration hot-spots to exercise there, in order of risk:
      1. **Date writes** — every `dateTime`/`dateTimeRange`/`dateMention` call now needs
         an explicit `TimeZone`; naive datetime strings throw.
      2. Exhaustive `when`s over `FormulaResult`/`RollupResult`/`Icon`/`PageCover`/
         `FileUploadStatus`/`FileUploadError` — new variants.
      3. Any code that called `icon.remove()`/`cover.remove()` or cleared properties —
         these now take effect.
      4. `FileUpload.filename`/`contentType` nullability.
- [ ] Feed anything broken back as issues; fix on `main` before tagging.

## Step 3 — Finalize the release

Follow the proven v0.5.0 flow (see git history of this file for the long form):

- [ ] `./gradlew formatKotlin` clean; `./gradlew test` green; spot-check the highest-risk
      integration tests individually (dates, uploads, drain) — not the full live suite.
- [ ] Set `version=0.6.0` in `gradle.properties`; move `CHANGELOG.md` `[Unreleased]` to
      `[0.6.0] - <date>`; final-read `RELEASE_NOTES.md` and update the README install
      snippets and notebook footers to `0.6.0`.
- [ ] Commit `chore(release): prepare v0.6.0`, tag `v0.6.0`, push `main --tags`.
- [ ] `gh release create v0.6.0` with the body taken from `RELEASE_NOTES.md`.
- [ ] `./gradlew publishToMavenLocal` (verify `.asc` signatures) →
      `./gradlew publishAllPublicationsToMavenCentralRepository` → publish in the
      Central Portal → verify on repo1.maven.org.

## Step 4 — Post-release housekeeping

- [ ] Bump `gradle.properties` to `0.7.0-SNAPSHOT`; commit
      `chore: bump version to 0.7.0-SNAPSHOT`.
- [ ] Delete `claude/kotlin-notion-orchestrate-9ztk5a` and, per the #43 decision,
      `chore/dependency-refresh-and-backlog`.
- [ ] Delete `RELEASE_NOTES.md` (its content lives on in the GitHub Release) or park the
      next draft in its place.
- [ ] Re-triage what remains open: #62 items that didn't get ratified, `IDEAS.md`
      rows 4, 7, 9, 10.
