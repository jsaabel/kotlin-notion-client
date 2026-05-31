# v0.5.0 Minor Release — Remaining Steps

**Prepared**: 2026-05-31
**Status at handoff**: `main` carries all v0.5.0 changes (§3, §5, §6a, §6b,
§7, §8, §9, §10) plus the Java 21 toolchain bump and the dependency refresh.
Docs are drafted. Needs commit hygiene, tag, GitHub Release, and Maven Central
publication.

---

## What changed

See `CHANGELOG.md` `[0.5.0]`. Highlights:

- New `Parent.AgentParent` variant for the `agent_id` parent type.
- `QueryResultLimitReached` exception + `RequestStatus` model for Notion's new
  10k pagination ceiling.
- Comments update / delete GA.
- Multi-value `equals` / `contains` on select / status / multi_select filters.
- Rate-limiting overhaul — every request flows through a single Ktor
  Send-phase plugin combining a continuous-refill token bucket (default 3
  req/s sustained, 20-request burst) with a typed retry classifier covering
  `429` (honouring `Retry-After`), `502` / `503` / `504`, and network errors.
  `SearchApi` and `FileUploadApi` are covered for the first time, and the
  parallel upload-retry config has been removed in favour of one
  `RateLimitConfig`.
- `FileObject.FileUpload` variant + `FilesBuilder` DSL for attaching
  freshly-uploaded files.
- `List<RichText>?.toHtml()` rich-text → HTML renderer.
- Integer-aware number plain-text rendering.
- **Breaking**: JVM toolchain + bytecode target bumped to Java 21.

---

## Step 0 — Pre-flight

- `./gradlew formatKotlin` clean.
- `./gradlew test -Dkotest.tags.include="Unit"` green.
- Spot-check a handful of integration tests for the largest changes
  (truncated queries, multi-value filters, file uploads). Do **not** run the
  full integration suite blind.
- Bump `gradle.properties` from `0.5.0-SNAPSHOT` to:
  ```
  version=0.5.0
  ```
- Confirm README install snippets and notebook footer already read `0.5.0`
  (done during release-docs prep).
- Confirm the test-count claim in README's AI-assistance notice still matches
  the latest run (currently 860+).

---

## Step 1 — Commit, tag, and push

```bash
git add CHANGELOG.md README.md RELEASE_TODO.md gradle.properties
git commit -m "chore(release): prepare v0.5.0"

git tag v0.5.0
git push origin main --tags
```

---

## Step 2 — Create the GitHub Release

```bash
gh release create v0.5.0 \
  --title "v0.5.0 — Notion API 2026-04-17 → 2026-05-15 catch-up" \
  --notes "$(cat <<'NOTES'
## Highlights

- **Rate-limiting overhaul** — outbound requests now flow through a
  proactive token-bucket throttle (default 3 req/s sustained, 20-request
  burst) and a unified retry classifier covering `429` (honouring
  `Retry-After`), retryable `5xx` (`502` / `503` / `504`), and network
  errors. `SearchApi` and `FileUploadApi` are covered for the first
  time; the parallel upload-retry config has been removed.
- **Multi-value filters** — `equals("a","b")` / `contains("a","b")` on
  select, status, and multi_select properties.
- **Files & media upload variant** — attach a freshly-uploaded file to a
  Files & media page property via `FileObject.upload(id, name?)` or the
  new `files { upload(...) }` DSL.
- **Comments update / delete** — `CommentsApi.update()` and
  `CommentsApi.delete()` now generally available.
- **Rich text → HTML** — `List<RichText>?.toHtml()` for rendering
  rich-text arrays to safe HTML (annotations, links, paragraph splitting,
  escape).
- **Truncated-query exception** — `QueryResultLimitReached` surfaces
  partial results when Notion's new 10,000-row pagination ceiling is hit.
- **Integer-aware number rendering** — `getPlainTextForProperty()` drops
  the trailing `.0` for whole numbers across Number / Formula / Rollup.
- **`agent_id` parent type** — `Parent.AgentParent` round-trips Notion's
  new agent-page parent shape.

## Breaking changes

- **JVM toolchain → Java 21** (was 17).
- **`RateLimitConfig` trimmed** — `RateLimitStrategy` + presets,
  `respectRetryAfter`, and `baseDelayMs` / `maxDelayMs` (replaced by
  `retryBaseDelay` / `retryMaxDelay` as `kotlin.time.Duration`) are gone.
  `RateLimitConfig.BALANCED` → `RateLimitConfig()`.
- **Unified retry pipeline for uploads** — `FileUploadOptions.retryConfig`,
  `models.files.RetryConfig`, and `EnhancedFileUploadApi.withRetry` are
  removed. Configure retries once via `NotionConfig.rateLimitConfig`.
- **Truncated queries now throw** — `DataSourcesApi.query()` /
  `queryAsFlow()` throw `NotionException.QueryResultLimitReached` instead
  of returning a silently-truncated list. The exception carries
  `partialResults`, `nextCursor`, and `requestStatus` so callers can
  resume.

## Installation

\`\`\`kotlin
dependencies {
    implementation("it.saabel:kotlin-notion-client:0.5.0")
}
\`\`\`

Full changelog: https://github.com/jsaabel/kotlin-notion-client/blob/main/CHANGELOG.md
NOTES
)"
```

---

## Step 3 — Pull on the publishing machine

```bash
git pull
git checkout v0.5.0
git log --oneline -3   # confirm you're at the release commit
```

---

## Step 4 — Verify signing still works locally

```bash
./gradlew publishToMavenLocal
ls ~/.m2/repository/it/saabel/kotlin-notion-client/0.5.0/*.asc
```

---

## Step 5 — Publish to Maven Central

```bash
./gradlew publishAllPublicationsToMavenCentralRepository
```

---

## Step 6 — Publish in the Maven Central Portal

1. Go to **https://central.sonatype.com/**
2. Sign in → **Deployments**
3. Find `it.saabel:kotlin-notion-client:0.5.0`
4. Review artifacts (jar, sources, javadoc, pom, .asc files)
5. Click **Publish**

---

## Step 7 — Verify

Wait ~10–30 minutes, then:

```bash
open https://repo1.maven.org/maven2/it/saabel/kotlin-notion-client/0.5.0/
```

---

## Step 8 — Post-release housekeeping

1. **Bump version for next cycle** in `gradle.properties`:
   ```
   version=0.6.0-SNAPSHOT
   ```
2. **Commit**: `chore: bump version to 0.6.0-SNAPSHOT`
3. Review `IDEAS.md` — mark any shipped ideas as `done` (likely candidates:
   integer-aware number rendering, rich text → HTML).
4. Delete `_next_release_docs/` — temporary working folder; its lifespan
   ended with this release.
5. Archive the per-task journals (`_task_*.md`) by removing the leading
   underscore once their parent plan is closed out, so they show up in the
   regular journal index.
