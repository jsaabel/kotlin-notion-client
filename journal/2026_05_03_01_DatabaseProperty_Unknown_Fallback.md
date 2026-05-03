# DatabaseProperty.Unknown Fallback for Forward Compatibility

**Date:** 2026-05-03  
**Type:** Hotfix  
**Version:** 0.4.2

## Problem

When `dataSources.retrieve()` is called against a workspace containing database properties
with types the client doesn't know about (e.g., `"button"`), kotlinx.serialization crashes:

```
Serializer for subclass 'button' is not found in the polymorphic scope of 'DatabaseProperty'.
JSON input: {"id":"A%5E%3Bf","name":"Create schedule","description":null,"type":"button","button":{}}
```

This was discovered in the festival-scripts project when querying a live workspace.

## Solution

Replicated the existing `PagePropertySerializer` / `PageProperty.Unknown` pattern for database
properties:

1. **`DatabasePropertySerializer.kt`** — Custom `KSerializer<DatabaseProperty>` that dispatches
   on the `type` field, handles all 20 known types, and falls back to `DatabaseProperty.Unknown`
   for anything unrecognized.

2. **`DatabaseProperty.Unknown`** — New sealed class subtype with `id`, `name`, `type`, and
   `rawContent: JsonElement` so callers can inspect unknown properties without crashing.

3. **`@Serializable(with = DatabasePropertySerializer::class)`** annotation applied to the
   sealed class, replacing the default polymorphic behaviour.

## Files Changed

- `src/main/kotlin/.../models/databases/Database.kt` — Added `Unknown` subtype, applied serializer annotation
- `src/main/kotlin/.../models/databases/DatabasePropertySerializer.kt` — New file
- `src/test/kotlin/unit/databases/DatabasePropertyUnknownTypeTest.kt` — New test file
- `gradle.properties` — Version bump 0.4.1 → 0.4.2
- `CHANGELOG.md` — Documented fix

## Testing

Unit tests verify:
- Button property deserializes as `Unknown` with correct id/name/type
- Known types still deserialize correctly
- Raw JSON is preserved for inspection
- Multiple unknown types in a single map work
- Mixed known + unknown types in a map work
