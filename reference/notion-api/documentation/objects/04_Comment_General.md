# Comment

The Comment object represents a comment on a Notion page or block. Comments can be viewed or created by an integration
that has access to the page/block and the correct capabilities. Please see the Capabilities guide for more information
on setting up your integration's capabilities.

When retrieving comments, one or more Comment objects will be returned in the form of an array, sorted in ascending
chronological order. When adding a comment to a page or discussion, the Comment object just added will always be
returned.

**Example Comment object:**

```json
{
  "object": "comment",
  "id": "7a793800-3e55-4d5e-8009-2261de026179",
  "parent": {
    "type": "page_id",
    "page_id": "5c6a2821-6bb1-4a7e-b6e1-c50111515c3d"
  },
  "discussion_id": "f4be6752-a539-4da2-a8a9-c3953e13bc0b",
  "created_time": "2022-07-15T21:17:00.000Z",
  "last_edited_time": "2022-07-15T21:17:00.000Z",
  "created_by": {
    "object": "user",
    "id": "e450a39e-9051-4d36-bc4e-8581611fc592"
  },
  "rich_text": [
    {
      "type": "text",
      "text": {
        "content": "Hello world",
        "link": null
      },
      "annotations": {
        "bold": false,
        "italic": false,
        "strikethrough": false,
        "underline": false,
        "code": false,
        "color": "default"
      },
      "plain_text": "Hello world",
      "href": null
    }
  ],
  "attachments": [
    {
      "category": "image",
      "file": {
        "url": "https://s3.us-west-2.amazonaws.com/...",
        "expiry_time": "2025-06-10T21:58:51.599Z"
      }
    }
  ],
  "display_name": {
    "type": "user",
    "resolved_name": "Avo Cado"
  }
}
```

## All comments

> 📘 **Reminder: Turn on integration comment capabilities**
>
> Integrations must have read comments or insert comments capabilities in order to interact with the Comment object
> through the API.
> For more information on integration capabilities, see the capabilities guide.

| Property           | Type                            | Description                                                                                                                                  | Example value                                                                                                                                                                  |
|--------------------|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `object`           | string                          | Always "comment"                                                                                                                             | `"comment"`                                                                                                                                                                    |
| `id`               | string (UUIDv4)                 | Unique identifier of the comment.                                                                                                            | `"ce18f8c6-ef2a-427f-b416-43531fc7c117"`                                                                                                                                       |
| `parent`           | object                          | Information about the comment's parent. See Parent object. Note that comments may only be parented by pages or blocks.                       | `{ "type": "block_id", "block_id": "5d4ca33c-d6b7-4675-93d9-84b70af45d1c" }`                                                                                                   |
| `discussion_id`    | string (UUIDv4)                 | Unique identifier of the discussion thread that the comment is associated with. See the guide for more information about discussion threads. | `"ce18f8c6-ef2a-427f-b416-43531fc7c117"`                                                                                                                                       |
| `created_time`     | string (ISO 8601 date and time) | Date and time when this comment was created. Formatted as an ISO 8601 date time string.                                                      | `"2022-07-15T21:46:00.000Z"`                                                                                                                                                   |
| `last_edited_time` | string (ISO 8601 date and time) | Date and time when this comment was updated. Formatted as an ISO 8601 date time string.                                                      | `"2022-07-15T21:46:00.000Z"`                                                                                                                                                   |
| `created_by`       | Partial User                    | User who created the comment.                                                                                                                | `{ "object": "user", "id": "e450a39e-9051-4d36-bc4e-8581611fc592" }`                                                                                                           |
| `rich_text`        | Rich text object                | Content of the comment, which supports rich text formatting, links, and mentions.                                                            | `[ { "text": { "content": "Kale", "link": { "type": "url", "url": "https://www.healthline.com/nutrition/10-proven-benefits-of-kale" } } } ]`                                   |
| `attachments`      | Comment Attachment              | File attachments on the comment                                                                                                              | `[ { "category": "image", "file": { "url": "https://s3.us-west-2.amazonaws.com/9bc6c6e0-32b8-4d55-8c12-3ae931f43a01/meow...", "expiry_time": "2025-06-10T21:58:51.599Z" } } ]` |
| `display_name`     | Comment Display Name            | Custom display name on comment                                                                                                               | `"display_name": { "type": "custom", "resolved_name": "automated response" }`                                                                                                  |