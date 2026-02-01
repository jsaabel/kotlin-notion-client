# Creating Pages from Templates

> Source: https://developers.notion.com/docs/creating-pages-from-templates
> Fetched: 2026-02-01

## Overview

Templates allow creating pages with pre-populated content and structure. Template application is asynchronous - the API returns immediately with a blank page, and content is populated in the background.

## Endpoints

### List Data Source Templates

**GET** `/v1/data_sources/{data_source_id}/templates`

Retrieves available templates for a data source.

**Headers:**
- `Notion-Version: 2025-09-03`
- `Authorization: Bearer {NOTION_API_KEY}`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `name` | string (optional) | Filter templates by substring match (case-insensitive) |
| `page_size` | integer 1-100 (optional) | Pagination limit |
| `start_cursor` | string (optional) | Pagination cursor |

**Response:**
```json
{
  "templates": [
    {
      "id": "uuid",
      "name": "string",
      "is_default": boolean
    }
  ],
  "has_more": boolean,
  "next_cursor": null|string
}
```

## Create Page with Template

### POST `/v1/pages`

Use the `template` parameter to apply a template when creating a page.

**Template Parameter Options:**

| Parameter | Value | Behavior |
|-----------|-------|----------|
| `template[type]` | `none` (or omitted) | No template applied; children/properties set immediately |
| `template[type]` | `default` | Applies data source's default template; cannot specify children |
| `template[type]` | `template_id` | Applies specific template; cannot specify children |
| `template[template_id]` | UUID string | Template identifier (dashes optional) |

**Parent Type for Data Sources:**

| Parameter | Value |
|-----------|-------|
| `parent[type]` | `data_source_id` |
| `parent[data_source_id]` | UUID string |

**Example Request:**
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
      "title": [{"text": {"content": "New Page from Template"}}]
    }
  }
}
```

**Key Behaviors:**
- API returns immediately with blank page (aside from initial properties)
- Template application occurs asynchronously in background
- "Created by" user is the API bot, not a person
- Placeholder values auto-populate during processing
- **Cannot specify `children` when using templates**

## Update Page with Template

### PATCH `/v1/pages/{page_id}`

Apply a template to an existing page.

**Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `template[type]` | string | `default` or `template_id` |
| `template[template_id]` | UUID | Specific template ID (required if type is `template_id`) |
| `erase_content` | boolean | Set `true` to replace existing content with template content (destructive, irreversible) |

**Example Request:**
```json
{
  "template": {
    "type": "template_id",
    "template_id": "87654321-4321-4321-4321-cba987654321"
  },
  "erase_content": true
}
```

## Webhook Events for Template Status

Since template application is asynchronous, use webhooks to detect completion:

| Event | Description |
|-------|-------------|
| `page.created` | Fired when page instantiated; may contain final content if aggregated |
| `page.content_updated` | Fired when template application completes |

**Implementation Logic:**
1. `page.content_updated` confirms template finished applying
2. For `page.created`, call `GET /v1/blocks/{page_id}/children` to verify content populated
3. Empty children array means template still processing; wait for `page.content_updated`

## Validation Errors

**HTTP 400 `validation_error` scenarios:**
- Invalid template ID format (must be UUID v4)
- Integration lacks access to template
- Attempting `default` template when none exists on data source
- Mismatched `data_source_id` in parent parameters
- Specifying `children` parameter when using a template

## SDK/API Version Requirements

- **Minimum API Version:** `2025-09-03`
- **Notion TypeScript SDK:** v5.3.0+

## Template ID Sources

1. Response from List Data Source Templates endpoint
2. Manual extraction from Notion URL (last UUID segment)
3. Any page in same workspace accessible to bot (though same data source recommended)
