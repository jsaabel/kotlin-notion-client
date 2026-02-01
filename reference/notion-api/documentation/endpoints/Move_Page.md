# Move Page Endpoint

> Source: https://developers.notion.com/reference/move-page
> Fetched: 2026-02-01

## Endpoint

**POST** `https://api.notion.com/v1/pages/{page_id}/move`

Moves a page to a new parent location.

## Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `page_id` | string (UUIDv4) | Yes | The ID of the page to move. Must be a regular page, not a database. |

## Request Headers

```
Authorization: Bearer secret_xxx
Notion-Version: 2022-06-28
Content-Type: application/json
```

## Request Body

The `parent` parameter specifies the destination.

### Move to Page Parent

Move page under another page:

```json
{
  "parent": {
    "type": "page_id",
    "page_id": "<parent-page-id>"
  }
}
```

### Move to Database/Data Source

Move page into a database:

```json
{
  "parent": {
    "type": "data_source_id",
    "data_source_id": "<database-data-source-id>"
  }
}
```

## Response

**Success (HTTP 200)** returns either:
- `partialPageObjectResponse`: Minimal page object with id and type
- `pageObjectResponse`: Complete page object with all properties

## Error Responses

| Status | Description |
|--------|-------------|
| 400 | Invalid JSON, validation errors |
| 401 | Unauthorized/missing credentials |
| 403 | Insufficient permissions |
| 404 | Page not found |
| 409 | Conflict error |
| 429 | Rate limited |
| 500/503 | Server errors |

## Notes

- The page being moved must be a regular Notion page, not a database
- UUIDs can be with or without dashes
- Integration must have appropriate permissions on both source and destination
