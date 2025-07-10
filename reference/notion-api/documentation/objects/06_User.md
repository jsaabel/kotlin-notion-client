# User

The User object represents a user in a Notion workspace. Users include full workspace members, guests, and integrations. You can find more information about members and guests in this guide.

> 📘 **Provisioning users and groups using SCIM**
> 
> The SCIM API is available for workspaces in Notion's Enterprise Plan. Learn more about using SCIM with Notion.

> 📘 **Setting up single sign-on (SSO) with Notion**
> 
> Single sign-on (SSO) can be configured for workspaces in Notion's Enterprise Plan. Learn more about SSO with Notion.

## Where user objects appear in the API

User objects appear in nearly all objects returned by the API, including:

- Block object under `created_by` and `last_edited_by`.
- Page object under `created_by` and `last_edited_by` and in people property items.
- Database object under `created_by` and `last_edited_by`.
- Rich text object, as user mentions.
- Property object when the property is a people property.

User objects will always contain `object` and `id` keys, as described below. The remaining properties may appear if the user is being rendered in a rich text or page property context, and the bot has the correct capabilities to access those properties. For more about capabilities, see the Capabilities guide and the Authorization guide.

## All users

These fields are shared by all users, including people and bots. Fields marked with * are always present.

| Property | Updatable | Type | Description | Example value |
|----------|-----------|------|-------------|---------------|
| `object`* | Display-only | "user" | Always "user" | `"user"` |
| `id`* | Display-only | string (UUID) | Unique identifier for this user. | `"e79a0b74-3aba-4149-9f74-0bb5791a6ee6"` |
| `type` | Display-only | string (optional, enum) | Type of the user. Possible values are "person" and "bot". | `"person"` |
| `name` | Display-only | string (optional) | User's name, as displayed in Notion. | `"Avocado Lovelace"` |
| `avatar_url` | Display-only | string (optional) | Chosen avatar image. | `"https://secure.notion-static.com/e6a352a8-8381-44d0-a1dc-9ed80e62b53d.jpg"` |

## People

User objects that represent people have the type property set to "person". These objects also have the following properties:

| Property | Updatable | Type | Description | Example value |
|----------|-----------|------|-------------|---------------|
| `person` | Display-only | object | Properties only present for non-bot users. | |
| `person.email` | Display-only | string | Email address of person. This is only present if an integration has user capabilities that allow access to email addresses. | `"avo@example.org"` |

## Bots

A user object's type property is "bot" when the user object represents a bot. A bot user object has the following properties:

| Property | Updatable | Type | Description | Example value |
|----------|-----------|------|-------------|---------------|
| `bot` | Display-only | object | If you're using GET /v1/users/me or GET /v1/users/{{your_bot_id}}, then this field returns data about the bot, including owner, owner.type, and workspace_name. These properties are detailed below. | `{ "object": "user", "id": "9188c6a5-7381-452f-b3dc-d4865aa89bdf", "name": "Test Integration", "avatar_url": null, "type": "bot", "bot": { "owner": { "type": "workspace", "workspace": true }, "workspace_name": "Ada Lovelace's Notion" } }` |
| `owner` | Display-only | object | Information about who owns this bot. | `{ "type": "workspace", "workspace": true }` |
| `owner.type` | Display-only | string enum | The type of owner, either "workspace" or "user". | `"workspace"` |
| `workspace_name` | Display-only | string enum | If the owner.type is "workspace", then workspace.name identifies the name of the workspace that owns the bot. If the owner.type is "user", then workspace.name is null. | `"Ada Lovelace's Notion"` |
| `workspace_limits` | Display-only | object | Information about the limits and restrictions that apply to the bot's workspace. | `{"max_file_upload_size_in_bytes": 5242880}` |
| `workspace_limits.max_file_upload_size_in_bytes` | Display-only | integer | The maximum allowable size of a file upload, in bytes. | `5242880` |