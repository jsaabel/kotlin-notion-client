# Session: Unsupported Property Types Investigation

**Date:** 2025-11-03
**Focus:** Investigate and fix deserialization failures when pages contain unsupported property types

## Problem Statement

During production integration in `festival-scripts`, discovered that queries fail when pages contain unsupported property types (confirmed: button properties). The client throws a misleading "Network error occurred" message instead of gracefully handling unknown properties.

### Reproduction
1. Query data source with standard properties → ✅ Works
2. Add button property to any page → ❌ Fails with "Network error occurred"
3. Remove button property → ✅ Works again

### Impact
- **Severity:** High - blocks production use
- Real workspaces commonly use button properties and modern features
- Workaround (removing properties) not viable

### Confirmed Problematic Types
- Button properties (confirmed)
- Likely: AI properties, unique ID, verification, etc.

## Investigation Plan

1. **Find deserialization code:**
   - Locate where API responses convert to Page objects
   - Find property type mapping/parsing logic
   - Identify source of "Network error occurred" message

2. **Understand current implementation:**
   - How are property types defined?
   - Is there strict vs lenient deserialization?
   - What's the error handling flow?

3. **Design solution:**
   - Option A: Skip unknown properties (best for forward compatibility)
   - Option B: Generic "Unknown" property type with raw JSON preserved
   - Option C: At minimum, clear error messages

4. **Test with real data:**
   - User can create pages with button properties
   - Verify fix handles unknown types gracefully
   - Ensure no regression on existing property types

## Session Log

### Initial Investigation

**Root Cause Identified:**

1. **Problem location:** `PageProperty.kt` (sealed class with specific subtypes)
   - Uses `@Serializable` with `@SerialName` for each property type
   - No fallback for unknown types like "button", "unique_id", "verification", etc.

2. **Deserialization flow:**
   - API returns page with properties: `Map<String, PageProperty>`
   - kotlinx.serialization tries to deserialize each property based on "type" field
   - When encountering unknown type (e.g., "button"), no matching `@SerialName` exists
   - Throws `SerializationException`

3. **Error handling wrapping:**
   - `DataSourcesApi.kt:211-214` catches all `Exception` types (not just `NotionException`)
   - Wraps deserialization errors as `NotionException.NetworkError`
   - Results in misleading "Network error occurred" message

4. **Current serializer setup:**
   - NotionClient.kt:145 - Uses `ignoreUnknownKeys = true`
   - This only ignores unknown JSON keys, NOT unknown sealed class subtypes
   - Sealed classes need custom serializers for graceful fallback

### Solution Design

**Approach:** Custom serializer with fallback to Unknown type

**Pattern identified:** Existing custom serializers in codebase:
- `ParentSerializer` - Uses `JsonContentPolymorphicSerializer`
- `PageIconSerializer` - Same pattern
- Both currently throw on unknown types, but we'll improve this

**Implementation plan:**
1. Add `Unknown` property type to `PageProperty` sealed class
   - Stores raw JSON as `JsonElement`
   - Includes `id`, `type`, and `rawContent` fields

2. Create `PagePropertySerializer`
   - Extends `JsonContentPolymorphicSerializer<PageProperty>`
   - Maps known types to their serializers
   - Falls back to `Unknown` for unrecognized types

3. Register serializer with `@Serializable(with = PagePropertySerializer::class)`

4. Add unit tests
   - Test deserialization of button property
   - Test other unknown property types
   - Verify existing property types still work

**Benefits:**
- Forward compatibility - New Notion property types won't break the client
- Preserves data - Raw JSON available for inspection
- Better UX - No cryptic "Network error" messages
- Allows gradual addition of property type support

### Implementation Complete

**Files Modified:**
1. `PageProperty.kt` - Added `PageProperty.Unknown` data class
2. `PagePropertySerializer.kt` - New custom serializer with fallback handling
3. `PagePropertyUnknownTypeTest.kt` - 4 unit tests (100% passing)
4. `UnknownPropertyTypesIntegrationTest.kt` - Manual verification integration test

**Test Results:**
- Unit tests: 4 new tests, 100% passing
- Full test suite: 503 tests, 100% passing (no regressions)
- Integration test: ✅ Verified with real button, unique_id, and place properties
- Real-world verification: ✅ Fixed original issue in festival-scripts

**Integration Test Results:**
- Tested with 3 unknown types: button, unique_id, place
- Tested with 13 supported types in same page
- All unknown types correctly deserialized as `PageProperty.Unknown`
- All supported types continue to work normally

**Real-World Verification:**
- Published to Maven Local (0.1.0)
- Tested in festival-scripts project
- ✅ Original failing query now succeeds
- ✅ Pages with button properties load successfully
- ✅ Can access supported properties normally

## Outcome

✅ **Issue Resolved**: Pages with unsupported property types now deserialize successfully as `PageProperty.Unknown`

✅ **Production Ready**: Client can handle any Notion workspace regardless of property types used

✅ **Tests Passing**: 100% test success rate (503 unit tests + integration test)

✅ **Real-World Verified**: Fixed original issue in festival-scripts production use case

## Next Steps / Future Considerations

### Candidate for Proper Implementation: Unique ID

The `unique_id` property type has high utility and should be considered for proper implementation:

**Why it's valuable:**
- Auto-incrementing numeric IDs with customizable prefixes
- Common for tracking items, tickets, invoices, etc.
- Structured data that's useful to access programmatically

**Example structure:**
```json
{
  "id": "prop-id",
  "type": "unique_id",
  "unique_id": {
    "prefix": "TASK",
    "number": 123
  }
}
```

**Potential implementation:**
```kotlin
@Serializable
@SerialName("unique_id")
data class UniqueId(
    @SerialName("id") override val id: String,
    @SerialName("type") override val type: String,
    @SerialName("unique_id") val uniqueId: UniqueIdValue?,
) : PageProperty()

@Serializable
data class UniqueIdValue(
    @SerialName("prefix") val prefix: String?,
    @SerialName("number") val number: Int,
)
```

**Other unknown types** (button, place) have limited utility and can remain as `Unknown`:
- **Button**: Just triggers actions, no data to retrieve
- **Place**: Location data, but without structured access, raw JSON is sufficient

This could be tackled in a future session with fresh context.
