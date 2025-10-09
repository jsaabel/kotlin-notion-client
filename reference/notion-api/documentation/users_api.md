# Notion API - User Endpoints Documentation

This document covers three user-related endpoints in the Notion API.

---

## 1. Retrieve a User

Retrieves a User object using the ID specified.

### Endpoint

```
GET https://api.notion.com/v1/users/{user_id}
```

### Path Parameters

- `user_id` (string, required) - Identifier for a Notion user

### Headers

- `Authorization` (string, required) - Bearer token (your Notion integration token)
- `Notion-Version` (string, required) - The version of the Notion API to use (e.g., `2022-06-28`)

### Example Request

```bash
curl -X GET https://api.notion.com/v1/users/d40e767c-d7af-4b18-a86d-55c61f1e39a4 \
  -H 'Authorization: Bearer '"$NOTION_API_KEY"'' \
  -H 'Notion-Version: 2022-06-28'
```

### Example Response - Person User

```json
{
  "object": "user",
  "id": "d40e767c-d7af-4b18-a86d-55c61f1e39a4",
  "type": "person",
  "person": {
    "email": "avo@example.org"
  },
  "name": "Avocado Lovelace",
  "avatar_url": "https://secure.notion-static.com/e6a352a8-8381-44d0-a1dc-9ed80e62b53d.jpg"
}
```

### Example Response - Bot User

```json
{
  "object": "user",
  "id": "9a3b5ae0-c6e6-482d-b0e1-ed315ee6dc57",
  "type": "bot",
  "bot": {
    "owner": {
      "type": "workspace",
      "workspace": true
    }
  },
  "name": "Doug Engelbot",
  "avatar_url": "https://secure.notion-static.com/6720d746-3402-4171-8ebb-28d15144923c.jpg"
}
```

### Integration Capabilities

**Important:** This endpoint requires an integration to have **user information capabilities**. Attempting to call this API without user information capabilities will return an HTTP response with a **403 status code**.

### Errors

Each Public API endpoint can return several possible error codes. See the Error codes section of the Status codes documentation for more information.

---

## 2. List All Users

Returns a paginated list of Users for the workspace.

### Endpoint

```
GET https://api.notion.com/v1/users
```

### Query Parameters

- `start_cursor` (string, optional) - If supplied, this endpoint will return a page of results starting after the cursor provided. If not supplied, this endpoint will return the first page of results.
- `page_size` (integer, optional) - The number of items from the full list desired in the response. Maximum: 100. Default: 100.

### Headers

- `Authorization` (string, required) - Bearer token (your Notion integration token)
- `Notion-Version` (string, required) - The version of the Notion API to use (e.g., `2022-06-28`)

### Example Request

```bash
curl -X GET 'https://api.notion.com/v1/users?page_size=100' \
  -H 'Authorization: Bearer '"$NOTION_API_KEY"'' \
  -H 'Notion-Version: 2022-06-28'
```

### Example Response

```json
{
  "object": "list",
  "results": [
    {
      "object": "user",
      "id": "d40e767c-d7af-4b18-a86d-55c61f1e39a4",
      "type": "person",
      "person": {
        "email": "avo@example.org"
      },
      "name": "Avocado Lovelace",
      "avatar_url": "https://secure.notion-static.com/e6a352a8-8381-44d0-a1dc-9ed80e62b53d.jpg"
    },
    {
      "object": "user",
      "id": "9a3b5ae0-c6e6-482d-b0e1-ed315ee6dc57",
      "type": "bot",
      "bot": {
        "owner": {
          "type": "workspace",
          "workspace": true
        }
      },
      "name": "Doug Engelbot",
      "avatar_url": "https://secure.notion-static.com/6720d746-3402-4171-8ebb-28d15144923c.jpg"
    }
  ],
  "next_cursor": "fe2cc560-036c-44cd-90e8-294d5a74cebc",
  "has_more": true
}
```

### Response Properties

- `object` (string) - Always `"list"`
- `results` (array) - Array of User objects
- `next_cursor` (string, nullable) - Cursor for the next page of results. Use this value in the `start_cursor` parameter to retrieve the next page.
- `has_more` (boolean) - Whether there are more results available

### Important Notes

- The response may contain fewer than `page_size` results
- **Guests are not included in the response**
- See Pagination documentation for details about how to use a cursor to iterate through the list
- The API does **not** currently support filtering users by their email and/or name
- Maximum of 100 results per request. If results exceed 100, use the `next_cursor` value to retrieve additional pages

### Integration Capabilities

**Important:** This endpoint requires an integration to have **user information capabilities**. Attempting to call this API without user information capabilities will return an HTTP response with a **403 status code**.

### Errors

Each Public API endpoint can return several possible error codes. See the Error codes section of the Status codes documentation for more information.

---

## 3. Retrieve Your Token's Bot User

Retrieves the bot User associated with the API token provided in the authorization header. The bot will have an `owner` field with information about the person who authorized the integration.

### Endpoint

```
GET https://api.notion.com/v1/users/me
```

### Headers

- `Authorization` (string, required) - Bearer token (your Notion integration token)
- `Notion-Version` (string, required) - The version of the Notion API to use (e.g., `2022-06-28`)

### Example Request

```bash
curl -X GET https://api.notion.com/v1/users/me \
  -H 'Authorization: Bearer '"$NOTION_API_KEY"'' \
  -H 'Notion-Version: 2022-06-28'
```

### Example Response

```json
{
  "object": "user",
  "id": "4666301e-ddb5-45de-b2f9-88eec463052b",
  "type": "bot",
  "bot": {
    "owner": {
      "type": "workspace",
      "workspace": true
    }
  },
  "name": "My Integration Bot",
  "avatar_url": "https://secure.notion-static.com/xxxx/bot-avatar.png"
}
```

### Example Response with User Owner

```json
{
  "object": "user",
  "id": "4666301e-ddb5-45de-b2f9-88eec463052b",
  "type": "bot",
  "bot": {
    "owner": {
      "type": "user",
      "user": {
        "object": "user",
        "id": "d40e767c-d7af-4b18-a86d-55c61f1e39a4",
        "type": "person",
        "person": {
          "email": "owner@example.org"
        },
        "name": "Integration Owner"
      }
    }
  },
  "name": "My Integration Bot",
  "avatar_url": "https://secure.notion-static.com/xxxx/bot-avatar.png"
}
```

### Response Properties

The bot user object includes:
- `object` (string) - Always `"user"`
- `id` (string) - Unique identifier for the bot
- `type` (string) - Always `"bot"`
- `bot` (object) - Contains owner information
    - `owner` (object) - Information about who owns/authorized the integration
        - `type` (string) - Either `"workspace"` or `"user"`
        - `workspace` (boolean) - `true` if workspace-owned (only present if type is `"workspace"`)
        - `user` (object) - Full user object if owned by a specific user (only present if type is `"user"`)
- `name` (string) - Name of the bot
- `avatar_url` (string, nullable) - URL of the bot's avatar image

### Integration Capabilities

**Important:** This endpoint is accessible by integrations with **any level of capabilities**. The User object returned will adhere to the limitations of the integration's capabilities.

### Errors

Each Public API endpoint can return several possible error codes. See the Error codes section of the Status codes documentation for more information.

---

## User Object Structure

All user endpoints return User objects with the following structure:

### Person User Type

```json
{
  "object": "user",
  "id": "string (UUID)",
  "type": "person",
  "person": {
    "email": "string"
  },
  "name": "string",
  "avatar_url": "string (nullable)"
}
```

### Bot User Type

```json
{
  "object": "user",
  "id": "string (UUID)",
  "type": "bot",
  "bot": {
    "owner": {
      "type": "workspace" | "user",
      "workspace": true // if type is "workspace"
      // OR
      "user": { /* user object */ } // if type is "user"
    }
  },
  "name": "string",
  "avatar_url": "string (nullable)"
}
```

---

## Common Error Responses

### 401 Unauthorized

```json
{
  "object": "error",
  "status": 401,
  "code": "unauthorized",
  "message": "API token is invalid."
}
```

### 403 Forbidden

```json
{
  "object": "error",
  "status": 403,
  "code": "restricted_resource",
  "message": "This integration does not have user information capabilities."
}
```

### 404 Not Found

```json
{
  "object": "error",
  "status": 404,
  "code": "object_not_found",
  "message": "Could not find user with ID: d40e767c-d7af-4b18-a86d-55c61f1e39a4"
}
```

### 429 Rate Limited

```json
{
  "object": "error",
  "status": 429,
  "code": "rate_limited",
  "message": "Rate limit exceeded. Please try again later."
}
```

---

## Additional Resources

- [Notion API Reference Documentation](https://developers.notion.com/reference)
- [User Object Documentation](https://developers.notion.com/reference/user)
- [Integration Capabilities Guide](https://developers.notion.com/docs/capabilities)
- [Status Codes and Error Handling](https://developers.notion.com/reference/status-codes)
- [Pagination Guide](https://developers.notion.com/reference/intro#pagination)