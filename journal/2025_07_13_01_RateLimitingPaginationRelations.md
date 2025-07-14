# 2025-07-13 - Rate Limiting, Pagination, and Relations

## Accomplishments

- **Rate Limiting System**: Implemented comprehensive rate limiting with automatic retry logic including exponential backoff and jitter
- **API Limits Model**: Created centralized Notion API limits and pagination models for consistent handling across the codebase
- **Automatic Pagination**: Built a comprehensive pagination system with sequence-based iteration and automatic page fetching
- **Relation Properties**: Added full support for relation property types in the data model

## Technical Decisions

- Chose exponential backoff with jitter for rate limit retries to avoid thundering herd
- Implemented pagination as Kotlin sequences for memory efficiency with large datasets
- Centralized all Notion API limits in a single model class for maintainability

## Challenges

- Handling different rate limit scenarios (per-second vs per-minute limits)
- Designing pagination to work seamlessly with both databases and pages APIs
- Managing relation properties which can reference multiple objects

## Key Discoveries

- **Rich Text Segment Limits**: Notion API enforces a 2000-character limit PER SEGMENT in rich text arrays, not on the total array length. This means we can split long text into multiple segments to bypass the limit (confirmed in test "CONFIRMED: per-segment limit allows splitting long text" line 543)
- **Validation Strategy**: Our current validation enforces limits on individual segments, which is correct, but we could enhance the API to automatically split long text into multiple segments instead of truncating

## Files Modified

- `src/main/kotlin/no/saabelit/kotlinnotionclient/api/BlocksApi.kt`
- `src/main/kotlin/no/saabelit/kotlinnotionclient/api/DatabasesApi.kt`
- `src/main/kotlin/no/saabelit/kotlinnotionclient/api/PagesApi.kt`
- `src/test/kotlin/integration/ValidationIntegrationTest.kt` (added tests to confirm per-segment limits)
- `src/main/kotlin/no/saabelit/kotlinnotionclient/validation/` (new validation infrastructure)

## Implementation Considerations

- **Text Splitting Algorithm**: When implementing auto-split for long text:
  - Split at word boundaries to avoid breaking words
  - Preserve formatting/annotations across segments
  - Consider natural break points (sentences, paragraphs)
  - Each segment must be < 2000 chars including any formatting overhead
- **API Design**: Could add option to CreatePageRequest/UpdatePageRequest to control behavior:
  - `autoSplitLongText: Boolean` - split into segments vs truncate
  - `splitStrategy: SplitStrategy` - enum for different splitting approaches

## Next Steps

- Implement automatic text splitting for rich text arrays exceeding 2000 chars per segment
- Add configuration options to control truncation vs splitting behavior
- Integration testing for pagination edge cases