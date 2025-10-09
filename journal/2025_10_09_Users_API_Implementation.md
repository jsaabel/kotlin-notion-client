# 2025-10-09: Users API Implementation

## Summary

Completed full implementation of the Users API with all three endpoints: `getCurrentUser()`, `retrieve(userId)`, and `list()`. Added comprehensive tests, documentation, and examples. All unit tests and integration tests passing.

## Work Completed

### 1. Model Enhancements

**File**: `src/main/kotlin/no/saabelit/kotlinnotionclient/models/users/User.kt`

Added support for person users and paginated list responses:

- **PersonInfo** data class - Contains optional email field for person users
- **UserList** data class - Paginated list response with results, cursor, and hasMore flag
- Made `BotInfo.owner` optional - API sometimes returns bot users without owner information
- Updated `User` model to include `person: PersonInfo?` field

```kotlin
data class PersonInfo(
    val email: String? = null  // Only present with user information capabilities
)

data class UserList(
    val objectType: String = "list",
    val results: List<User>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

data class BotInfo(
    val owner: Owner? = null  // Made optional to handle API edge cases
)
```

### 2. API Implementation

**File**: `src/main/kotlin/no/saabelit/kotlinnotionclient/api/UsersApi.kt`

Implemented two new methods following established patterns:

#### retrieve(userId: String): User
- Retrieves a specific user by ID
- Requires user information capabilities (403 Forbidden without them)
- Includes proper error handling and rate limiting
- Validates user ID format through API

#### list(startCursor: String? = null, pageSize: Int? = null): UserList
- Lists all users in workspace (excluding guests)
- Supports pagination with cursor and page size (max 100)
- Validates page size before making request (throws IllegalArgumentException)
- Requires user information capabilities

**Design Decision: No DSL**
- Kept API simple with direct parameters
- Only 2 optional pagination parameters don't warrant DSL complexity
- Consistent with other simple APIs in the project

### 3. Test Infrastructure

#### Test Fixtures
**File**: `src/test/kotlin/unit/util/TestFixtures.kt`

Added Users helper object:
```kotlin
object Users {
    fun retrieveBotUser()
    fun retrievePersonUser()
    fun retrieveABotUser()
    fun listUsers()
    // ... with AsString() variants
}
```

#### Sample JSON Files
Created official API sample responses in `src/test/resources/api/users/`:
- `get_retrieve_bot_user.json` - Bot user with workspace owner
- `get_retrieve_a_person_user.json` - Person user with email
- `get_retrieve_a_bot_user.json` - Bot user variant
- `get_list_all_users.json` - Paginated list with person and bot users

### 4. Unit Tests

**File**: `src/test/kotlin/unit/api/UsersApiTest.kt`

Comprehensive test coverage (15 tests total):

**Model Serialization Tests:**
- Deserialize bot user response
- Deserialize person user response
- Deserialize user list response

**retrieve() Tests:**
- Successful user retrieval
- Handle 404 Not Found
- Handle 403 Forbidden (missing capabilities)

**list() Tests:**
- List users successfully
- List with page size parameter
- List with start cursor
- Validate page size bounds (0 and 101 should fail)
- Handle 403 Forbidden

**getCurrentUser() Tests:**
- Get current bot user successfully
- Handle 401 Unauthorized

All 472 unit tests passing (~200ms execution time).

### 5. Integration Tests

**File**: `src/test/kotlin/integration/UsersIntegrationTest.kt`

Real API tests with graceful capability handling:

**Key Features:**
- UUID normalization for env vars (handles with/without hyphens)
- Graceful 403 handling when missing user information capabilities
- Uses `NOTION_TEST_USER_ID` for testing real user retrieval
- All tests skip gracefully if capabilities missing

**Tests:**
1. getCurrentUser returns bot user
2. Retrieve user by NOTION_TEST_USER_ID
3. Retrieve bot by its own ID
4. List users with pagination
5. Handle pagination with cursor
6. Validate page size bounds
7. Handle invalid user ID

**Bug Fix During Testing:**
- Made `BotInfo.owner` optional after discovering API returns bots without owner in some cases
- Updated `NotionClientIntegrationTest.kt` to handle nullable owner
- All integration tests now passing

### 6. Documentation

**File**: `docs/users.md`

Comprehensive documentation with WIP notice removed:

**Sections:**
- Overview of three operations and capability requirements
- User model reference with all types
- 6 detailed examples with runnable code
- Integration capabilities table
- Common patterns (graceful error handling, pagination)
- Limitations (no filtering, guests excluded, read-only)
- Tips and best practices
- Error handling patterns
- Testing references
- Related APIs

**Examples Cover:**
- Get current bot user (basic)
- Retrieve specific user by ID
- List all users in workspace
- List with pagination
- Working with person vs bot users
- Validate API token at startup

### 7. Example Tests

**File**: `src/test/kotlin/examples/UsersExamples.kt`

9 executable examples matching documentation exactly:

1. **Get current bot user** - Basic getCurrentUser() usage
2. **Validate API token at startup** - Token validation pattern
3. **Retrieve specific user by ID** - Using NOTION_TEST_USER_ID
4. **List all users** - Basic list with error handling
5. **List with pagination** - Multi-page navigation
6. **Person vs bot users** - Type checking and field access
7. **Graceful capability handling** - Reusable getUserInfo() helper
8. **Iterate through all users** - Complete pagination pattern
9. **Error handling** - Comprehensive catch blocks

All examples:
- Use `integrationTestEnvVarsAreSet()` utility
- Gracefully skip when NOTION_TEST_USER_ID not set
- Handle 403 Forbidden for missing capabilities
- Match documentation code exactly

### 8. Bug Fixes

**Issue**: Serialization failure when listing users
```
Field 'owner' is required for type 'BotInfo', but it was missing
```

**Root Cause**: API returns bot users without owner information in certain cases (likely for security/privacy)

**Solution**:
- Made `BotInfo.owner: Owner?` optional with default null
- Updated all tests to handle nullable owner
- Updated integration test assertions

**Files Modified:**
- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/users/User.kt`
- `src/test/kotlin/integration/UsersIntegrationTest.kt`
- `src/test/kotlin/integration/NotionClientIntegrationTest.kt`

## Testing Results

### Unit Tests
```bash
./gradlew test -Dkotest.tags.include="Unit"
```
**Result**: ✅ 472 tests passed in ~200ms

### Integration Tests
```bash
NOTION_API_TOKEN="..." \
NOTION_TEST_USER_ID="..." \
NOTION_RUN_INTEGRATION_TESTS=true \
./gradlew integrationTest
```
**Result**: ✅ All tests passed
- UUID normalization working (env var without hyphens)
- Graceful 403 handling for missing capabilities
- Pagination working correctly

### Example Tests
```bash
./gradlew test --tests "examples.UsersExamples"
```
**Result**: ✅ All 9 examples passed

## API Coverage

| Notion API Endpoint | Status | Method |
|---------------------|--------|--------|
| GET /v1/users/me | ✅ Complete | `getCurrentUser()` |
| GET /v1/users/{id} | ✅ Complete | `retrieve(userId)` |
| GET /v1/users | ✅ Complete | `list(cursor, pageSize)` |

## Key Design Decisions

### 1. No DSL for list()
**Rationale**: Only 2 optional parameters (startCursor, pageSize) don't justify DSL complexity. Kept API simple and direct.

**Comparison**: Comments API has DSL for retrieve() because it combines blockId + pagination, but Users list() is simpler.

### 2. Made BotInfo.owner Optional
**Rationale**: API returns bots without owner in production. Better to handle gracefully than fail deserialization.

**Impact**: All code now safely accesses `user.bot?.owner?.type`

### 3. UUID Normalization in Tests
**Rationale**: Environment variables might be copy-pasted without hyphens. Tests normalize for comparison.

**Implementation**:
```kotlin
val normalizedUserId = user.id.replace("-", "")
val normalizedTestUserId = testUserId.replace("-", "")
normalizedUserId shouldBe normalizedTestUserId
```

### 4. Graceful Capability Handling
**Rationale**: Most integrations won't have user information capabilities. Tests and examples should demonstrate graceful degradation.

**Pattern**:
```kotlin
try {
    client.users.list()
} catch (e: NotionException.ApiError) {
    if (e.status == 403) {
        // Missing capabilities - handle gracefully
    }
}
```

## Files Created

- `src/test/resources/api/users/get_retrieve_bot_user.json`
- `src/test/resources/api/users/get_retrieve_a_person_user.json`
- `src/test/resources/api/users/get_retrieve_a_bot_user.json`
- `src/test/resources/api/users/get_list_all_users.json`
- `src/test/kotlin/unit/api/UsersApiTest.kt`
- `src/test/kotlin/integration/UsersIntegrationTest.kt`
- `src/test/kotlin/examples/UsersExamples.kt`
- `journal/2025_10_09_Users_API_Implementation.md`

## Files Modified

- `src/main/kotlin/no/saabelit/kotlinnotionclient/models/users/User.kt` - Added PersonInfo, UserList, made owner optional
- `src/main/kotlin/no/saabelit/kotlinnotionclient/api/UsersApi.kt` - Added retrieve() and list() methods
- `src/test/kotlin/unit/util/TestFixtures.kt` - Added Users helper object
- `src/test/kotlin/integration/NotionClientIntegrationTest.kt` - Fixed nullable owner access
- `docs/users.md` - Complete rewrite with comprehensive documentation

## Next Steps

The Users API is now feature-complete and fully documented. Possible future enhancements:

1. **Cache getCurrentUser() result** - Since bot info doesn't change, could cache in client
2. **Batch user lookup** - Helper function to retrieve multiple users efficiently
3. **User mention DSL** - For creating mentions in rich text (separate from Users API)

## Lessons Learned

1. **Always make optional what the API makes optional** - BotInfo.owner being required caused issues
2. **UUID format flexibility** - Normalize UUIDs in tests for robustness
3. **Capability-based testing** - Design tests to gracefully handle missing permissions
4. **Simple APIs stay simple** - Not everything needs a DSL
5. **Documentation feedback loop** - Examples tests validate documentation accuracy

## Related Documentation

- [Users API Documentation](../docs/users.md)
- [Notion API Reference](../reference/notion-api/documentation/users_api.md)
- [Error Handling](../docs/error-handling.md)
- [Testing Guide](../docs/testing.md)
