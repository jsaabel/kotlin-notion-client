# Icon/Cover Removal Issue

**Date**: 2025-10-06
**Status**: Known limitation / To investigate
**Context**: Pages API documentation validation

## Issue

The `icon.remove()` and `cover.remove()` DSL methods are implemented and set the icon/cover to `null` in the request, but the Notion API doesn't actually remove the icon/cover when null is sent.

## Current Behavior

```kotlin
notion.pages.update("page-id") {
    icon.remove()  // Sets icon to null in request
    cover.remove() // Sets cover to null in request
}
```

After this update, the icon and cover remain on the page (not removed).

## Expected Behavior

Based on the Notion API documentation (which shows `icon: null` and `cover: null` in examples), passing null should remove the icon/cover.

## Potential Causes

1. **Serialization issue**: Our JSON serialization might be excluding null values instead of including them explicitly
2. **API behavior**: The Notion API might not support removal despite documentation suggesting it
3. **Request format**: There might be a special format required for removal that we're missing

## Investigation Needed

- Check how we serialize null values in requests (Kotlinx Serialization settings)
- Verify if the API actually supports icon/cover removal
- Check if other SDKs (Python, JS) successfully handle this
- Review API changelog for any notes about this behavior

## Workaround

Currently no workaround - icon/cover cannot be removed via API once set.

## Priority

Low - not a critical use case. Most users will update icons/covers rather than remove them.

## Related Files

- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/pages/UpdatePageRequestBuilder.kt` (lines 144-152, 190-198)
- `src/test/kotlin/pages/UpdatePageRequestBuilderTest.kt` (lines 105-116)
- `src/test/kotlin/integration/dsl/UpdatePageRequestBuilderIntegrationTest.kt` (line 209)
