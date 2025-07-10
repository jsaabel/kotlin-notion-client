# Comment Display Name

The Comment Display Name object represents the author name that shows up for a comment. This overrides the default author name when specified.

## Request format (input)

### Object properties

| Parameter | Type | Description | Example value |
|-----------|------|-------------|---------------|
| `type` | string (enum) | Possible type values are: "integration", "user", or "custom" | `"user"` |
| `custom` | object | If the type is "custom", include a custom object specifying the custom name<br><br>`"custom": { "name": <Custom Name> }` | `"custom": { "name": "Notion Bot" }` |

**Display name types:**
- **"integration"**: name of the integration
- **"user"**: name of the user who authenticated the integration (only for Public Integrations)
- **"custom"**: any custom name

**Example of a Create Comment request with custom display name:**

```json
{
  "parent": {
    "page_id": "d0a1ffaf-a4d8-4acf-a1ed-abae6e110418"
  },
  "rich_text": [
    {
      "text": {
        "content": "Thanks for checking us out!"
      }
    }
  ],
  "display_name": {
    "type": "custom",
    "custom": {
      "name": "Notion Bot"
    }
  }
}
```

## Response format (output)

### Object properties

The response of Comment APIs like Create comment contains attachments with the following fields:

| Field | Type | Description | Example value |
|-------|------|-------------|---------------|
| `type` | string (enum) | Possible type values are: "integration", "user", or "custom" | `"custom"` |
| `resolved_name` | string | The custom display name shown in a comment | `"Notion Bot"` |

**API Response**

```json
{
  ...existing parameters omitted,
  "display_name": {
    "type": "custom",
    "resolved_name": "Notion Bot"
  }
}
```