# Update Page Endpoint (2025-09-03 API)

> Source: https://developers.notion.com/reference/patch-page
> Fetched: 2026-02-01

## Endpoint

**PATCH** `https://api.notion.com/v1/pages/{page_id}`

Updates page properties, icon, cover, archived status, and can apply templates.

## Request Headers

```
Authorization: Bearer secret_xxx
Notion-Version: 2025-09-03
Content-Type: application/json
```

## Request Body Parameters

### Core Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `properties` | object | Property values to update. Supports all property types except rollup, created_by, created_time, last_edited_by, last_edited_time. |
| `icon` | object \| null | Set page icon (emoji, external URL, file upload, or custom emoji). Set to `null` to remove. |
| `cover` | object \| null | Set page cover (file upload or external URL). Set to `null` to remove. |
| `archived` | boolean | Archive the page |
| `in_trash` | boolean | Move page to trash |

### New Parameters (2025-09-03)

| Parameter | Type | Description |
|-----------|------|-------------|
| `is_locked` | boolean | Whether the page should be locked from editing in the Notion app UI. If not provided, lock state remains unchanged. |
| `template` | object | Apply a template to the page. See Template section below. |
| `erase_content` | boolean | Whether to erase all existing content from the page. When used with a template, the template content replaces existing content. **Destructive and irreversible.** |

### Template Parameter

```json
{
  "template": {
    "type": "default"
  }
}
```

Or with specific template:

```json
{
  "template": {
    "type": "template_id",
    "template_id": "87654321-4321-4321-4321-cba987654321"
  }
}
```

### Example: Apply Template and Erase Content

```json
{
  "template": {
    "type": "template_id",
    "template_id": "87654321-4321-4321-4321-cba987654321"
  },
  "erase_content": true
}
```

### Example: Lock a Page

```json
{
  "is_locked": true
}
```

## Response

Returns either:
- Full `pageObjectResponse` with all page details
- `partialPageObjectResponse` (object + id only)

## Status Codes

| Status | Description |
|--------|-------------|
| 200 | Success |
| 400 | Invalid request |
| 401 | Unauthorized |
| 403 | Insufficient capabilities |
| 404 | Page not found |
| 429 | Rate limited |
| 500/503 | Server error |

## Limitations

- Rollup property updates not supported
- Parent cannot be changed (use Move Page endpoint instead)
- Integration requires update content capabilities
