# Users API

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

## Overview

The Users API allows you to retrieve information about users and bots in your Notion workspace.

**Official Documentation**: [Notion Users API](https://developers.notion.com/reference/get-user)

## Available Operations

```kotlin
// Retrieve a user by ID
suspend fun retrieve(userId: String): User

// List all users
suspend fun list(): PaginatedList<User>

// Get current bot user
suspend fun getCurrentUser(): User
```

## Examples

_TODO: Add comprehensive examples_

### Get Current User

```kotlin
val user = notion.users.getCurrentUser()
println("Bot name: ${user.name}")
println("User ID: ${user.id}")
```

### Retrieve a Specific User

```kotlin
val user = notion.users.retrieve("user-id")
println("Name: ${user.name}")
println("Type: ${user.type}")
```

### List All Users

```kotlin
val users = notion.users.list()
users.results.forEach { user ->
    println("${user.name} (${user.type})")
}
```

## User Types

Users can be:
- `person` - A human user
- `bot` - A bot/integration

## Common Patterns

_TODO: Add tips, gotchas, best practices_

## Related APIs

- **[Pages](pages.md)** - Users can be mentioned in pages
- **[Comments](comments.md)** - Users can create comments
