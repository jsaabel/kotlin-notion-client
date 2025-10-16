# Users API

## Overview

The Users API allows you to retrieve information about users and bots in your Notion workspace. The API provides three main operations:

1. **Get Current Bot User** - Always available, returns information about your integration
2. **Retrieve User by ID** - Requires user information capabilities
3. **List All Users** - Requires user information capabilities, returns paginated list

**Official Documentation**: [Notion Users API](https://developers.notion.com/reference/get-users)

## Available Operations

```kotlin
// Get current bot user (always available)
suspend fun getCurrentUser(): User

// Retrieve a specific user by ID (requires user information capabilities)
suspend fun retrieve(userId: String): User

// List all users in workspace (requires user information capabilities)
suspend fun list(startCursor: String? = null, pageSize: Int? = null): UserList
```

## User Model

```kotlin
data class User(
    val objectType: String,        // Always "user"
    val id: String,                 // UUID of the user
    val name: String?,              // Display name
    val avatarUrl: String?,         // Avatar image URL
    val type: UserType?,            // PERSON or BOT
    val person: PersonInfo?,        // Present for person users
    val bot: BotInfo?               // Present for bot users
)

enum class UserType {
    PERSON,  // A human user
    BOT      // A bot/integration
}
```

## Examples

### Basic: Get Current Bot User

Always available, doesn't require special capabilities:

```kotlin
val notion = NotionClient("your-secret-token")

val botUser = notion.users.getCurrentUser()
println("Bot name: ${botUser.name}")
println("Bot ID: ${botUser.id}")
println("Type: ${botUser.type}") // Will be UserType.BOT
```

### Retrieve a Specific User by ID

Requires **user information capabilities**:

```kotlin
try {
    val user = notion.users.retrieve("d40e767c-d7af-4b18-a86d-55c61f1e39a4")

    when (user.type) {
        UserType.PERSON -> {
            println("Person: ${user.name}")
            user.person?.email?.let { email ->
                println("Email: $email")
            }
        }
        UserType.BOT -> {
            println("Bot: ${user.name}")
        }
        else -> println("Unknown user type")
    }
} catch (e: NotionException.ApiError) {
    if (e.status == 403) {
        println("Integration lacks user information capabilities")
    }
}
```

### List All Users in Workspace

Requires **user information capabilities**:

```kotlin
try {
    val userList = notion.users.list()

    userList.results.forEach { user ->
        println("${user.name} (${user.type})")
    }

    if (userList.hasMore) {
        println("More users available, cursor: ${userList.nextCursor}")
    }
} catch (e: NotionException.ApiError) {
    if (e.status == 403) {
        println("Integration lacks user information capabilities")
    }
}
```

### List with Pagination

Control page size and navigate through results:

```kotlin
// Get first page
val firstPage = notion.users.list(pageSize = 50)
firstPage.results.forEach { user ->
    println(user.name)
}

// Get next page if available
if (firstPage.hasMore && firstPage.nextCursor != null) {
    val secondPage = notion.users.list(
        startCursor = firstPage.nextCursor,
        pageSize = 50
    )
}
```

### Working with Person vs Bot Users

```kotlin
val user = notion.users.retrieve(userId)

when (user.type) {
    UserType.PERSON -> {
        println("This is a person user")
        val email = user.person?.email ?: "Email not available"
        println("Email: $email")
    }
    UserType.BOT -> {
        println("This is a bot integration")
        user.bot?.owner?.let { owner ->
            println("Owner type: ${owner.type}")
        }
    }
    null -> println("User type not specified")
}
```

### Validate API Token at Startup

```kotlin
suspend fun initializeNotionClient(token: String): NotionClient {
    val client = NotionClient(token)

    try {
        val botUser = client.users.getCurrentUser()
        println("✓ Authenticated as: ${botUser.name}")
        return client
    } catch (e: NotionException.AuthenticationError) {
        throw IllegalStateException("Invalid API token", e)
    }
}
```

## Integration Capabilities

The Users API has different requirements for different operations:

| Operation | Required Capability | Error if Missing |
|-----------|---------------------|------------------|
| `getCurrentUser()` | None | N/A |
| `retrieve(userId)` | User information | 403 Forbidden |
| `list()` | User information | 403 Forbidden |

To add user information capabilities to your integration:
1. Go to your integration settings in Notion
2. Navigate to the "Capabilities" section
3. Enable "Read user information including email addresses"

## Common Patterns

### Graceful Handling of Missing Capabilities

```kotlin
suspend fun getUserInfo(client: NotionClient, userId: String): User? {
    return try {
        client.users.retrieve(userId)
    } catch (e: NotionException.ApiError) {
        when (e.status) {
            403 -> {
                println("Integration lacks user capabilities")
                null
            }
            404 -> {
                println("User not found")
                null
            }
            else -> throw e
        }
    }
}
```

### Iterate Through All Users

```kotlin
suspend fun getAllUsers(client: NotionClient): List<User> {
    val allUsers = mutableListOf<User>()
    var cursor: String? = null

    do {
        val page = client.users.list(startCursor = cursor, pageSize = 100)
        allUsers.addAll(page.results)
        cursor = page.nextCursor
    } while (page.hasMore)

    return allUsers
}
```

## Limitations

- **No email/name filtering**: The API doesn't support searching users by email or name
- **Guests not included**: `list()` doesn't return guest users
- **Read-only**: You cannot create or modify users through the API
- **Email access restricted**: Email addresses only visible with specific capabilities

## Tips and Best Practices

### ✅ Best Practices

1. **Call getCurrentUser() at startup** to validate your API token
2. **Cache bot user info** since it doesn't change during runtime
3. **Handle null properties** - name, avatarUrl, and person/bot fields are optional
4. **Gracefully handle 403 errors** for retrieve() and list()
5. **Use page size of 100** for efficient pagination (maximum allowed)

### ⚠️ Common Gotchas

1. **getCurrentUser() only returns your bot** - never returns person users
2. **Limited user data** - API has strict privacy controls
3. **403 means missing capabilities** - not an authentication error
4. **Guests aren't in list()** - only full workspace members and bots
5. **Email may be null** - even with capabilities, not all users have emails

## Error Handling

```kotlin
try {
    val user = notion.users.retrieve(userId)
    // ... use user
} catch (e: NotionException.AuthenticationError) {
    println("Authentication failed: Check your API token")
} catch (e: NotionException.ApiError) {
    when (e.status) {
        403 -> println("Missing user information capabilities")
        404 -> println("User not found")
        429 -> println("Rate limited - wait and retry")
        else -> println("API error: ${e.details}")
    }
} catch (e: NotionException.NetworkError) {
    println("Network error: ${e.cause?.message}")
} catch (e: IllegalArgumentException) {
    println("Invalid parameter: ${e.message}")
}
```

See **[Error Handling](error-handling.md)** for comprehensive error handling patterns.

## Testing

- **Unit tests**: `src/test/kotlin/unit/api/UsersApiTest.kt`
- **Integration tests**: `src/test/kotlin/integration/UsersIntegrationTest.kt`
- **Examples**: `src/test/kotlin/examples/UsersExamples.kt`

See **[Testing](testing.md)** for more information.

## Related APIs

- **[Pages](pages.md)** - Users appear in `created_by` and `last_edited_by` fields
- **[Databases](databases.md)** - Users appear in `created_by` and `last_edited_by` fields
- **[Blocks](blocks.md)** - Users appear in `created_by` and `last_edited_by` fields
- **[Comments](comments.md)** - Users create and own comments
- **[Error Handling](error-handling.md)** - Handling API errors and validation
