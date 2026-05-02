# v0.4.1 Hotfix Release — Remaining Steps

**Prepared**: 2026-05-02  
**Status at handoff**: `main` has the code fix and doc updates staged. Needs commit, tag, GitHub Release, and Maven Central publication.

---

## What changed

**Bug fix**: Rollup properties with `show_original` return an array of page properties without `id` fields, causing deserialization to fail with `Missing 'id' field in PageProperty JSON`. Fixed by defaulting `id` to `""` in all `PageProperty` subtypes and in `PagePropertySerializer`.

---

## Step 1 — Commit, tag, and push

```bash
# Commit (from this machine)
git add -A
git commit -m "fix: handle missing id field in rollup array page properties

Rollup properties with show_original return page property items without
an id field. Default id to empty string instead of throwing.

Closes #<issue> (if applicable)"

git tag v0.4.1
git push origin main --tags
```

---

## Step 2 — Create the GitHub Release

```bash
gh release create v0.4.1 \
  --title "v0.4.1 — Fix rollup show_original deserialization" \
  --notes "$(cat <<'NOTES'
## Bug Fix

Rollup properties using `show_original` return an array of page properties that lack the `id` field. Previously this caused a `SerializationException`. Now all `PageProperty` subtypes default `id` to `""`, matching the API's behaviour for inline property values.

## Installation

\`\`\`kotlin
dependencies {
    implementation("it.saabel:kotlin-notion-client:0.4.1")
}
\`\`\`
NOTES
)"
```

---

## Step 3 — Pull on the publishing machine

```bash
git pull
git checkout v0.4.1
git log --oneline -3   # confirm you're at the hotfix commit
```

---

## Step 4 — Verify signing still works locally

```bash
./gradlew publishToMavenLocal
ls ~/.m2/repository/it/saabel/kotlin-notion-client/0.4.1/*.asc
```

---

## Step 5 — Publish to Maven Central

```bash
./gradlew publishAllPublicationsToMavenCentralRepository
```

---

## Step 6 — Publish in the Maven Central Portal

1. Go to **https://central.sonatype.com/**
2. Sign in -> **Deployments**
3. Find `it.saabel:kotlin-notion-client:0.4.1`
4. Review artifacts (jar, sources, javadoc, pom, .asc files)
5. Click **Publish**

---

## Step 7 — Verify

Wait ~10-30 minutes, then:

```bash
open https://repo1.maven.org/maven2/it/saabel/kotlin-notion-client/0.4.1/
```

---

## Step 8 — Post-release housekeeping

1. **Bump version for next cycle** in `gradle.properties`:
   ```
   version=0.5.0
   ```
2. **Commit**: `chore: bump version to 0.5.0`
3. Review `IDEAS.md` — mark any shipped ideas as `done`
