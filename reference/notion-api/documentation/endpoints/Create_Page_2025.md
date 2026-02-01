# Create Page Endpoint (2025-09-03 API)

> Source: https://developers.notion.com/reference/post-page
> Fetched: 2026-02-01

## Endpoint

**POST** `https://api.notion.com/v1/pages`

Creates a new page as a child of an existing page, database, or data source.

## Request Headers

```
Authorization: Bearer secret_xxx
Notion-Version: 2025-09-03
Content-Type: application/json
```

## Parent Parameter

The `parent` parameter accepts four types:

| Type | Description |
|------|-------------|
| `page_id` | Create page under an existing page |
| `database_id` | Create page under an existing database |
| `data_source_id` | Create page under a data source (new in 2025-09-03) |
| `workspace` | `"workspace": true` - Creates private workspace-level page (public integrations only) |

### Example: Data Source Parent

```json
{
  "parent": {
    "type": "data_source_id",
    "data_source_id": "12345678-1234-1234-1234-123456789abc"
  }
}
```

## Properties Parameter

- For pages under a **page parent**: Only `title` is valid
- For pages under a **data source parent**: Keys must match the data source's properties

**Unsupported properties** (cannot be set):
- `rollup`
- `created_by`
- `created_time`
- `last_edited_by`
- `last_edited_time`

## Template Parameter

Controls content population when creating a page.

| Type | Behavior |
|------|----------|
| `type: "none"` (default) | No template applied |
| `type: "default"` | Uses data source's default template |
| `type: "template_id"` | Uses specific page as blueprint |

**Important:** When using templates, the `children` parameter is prohibited. Template application occurs asynchronously.

### Example: Create with Template

```json
{
  "parent": {
    "type": "data_source_id",
    "data_source_id": "12345678-1234-1234-1234-123456789abc"
  },
  "template": {
    "type": "template_id",
    "template_id": "87654321-4321-4321-4321-cba987654321"
  },
  "properties": {
    "Title": {
      "title": [{"text": {"content": "New Page"}}]
    }
  }
}
```

## Position Parameter (New)

Controls page placement within the parent.

| Type | Description |
|------|-------------|
| `type: "after_block"` | Place after specific block. Requires `after_block: {id}` |
| `type: "page_start"` | Place at beginning of parent |
| `type: "page_end"` | Place at end of parent |

### Example: Position After Block

```json
{
  "parent": {
    "type": "page_id",
    "page_id": "..."
  },
  "position": {
    "type": "after_block",
    "after_block": "block-uuid-here"
  },
  "properties": {
    "title": {
      "title": [{"text": {"content": "New Subpage"}}]
    }
  }
}
```

## Additional Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `icon` | object | Page emoji, external URL, or file upload |
| `cover` | object | External URL or file upload |
| `children` | array | Block content (max 100 items). **Incompatible with templates** |
| `content` | array | Alias for children |

## Response

Returns either:
- Full `pageObjectResponse`
- Minimal `partialPageObjectResponse` (contains only object type and ID)

## Requirements

Integration must have "Insert Content capabilities" on target parent.
