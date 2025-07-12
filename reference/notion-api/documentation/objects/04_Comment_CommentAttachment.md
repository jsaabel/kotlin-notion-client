# Comment Attachment

The Comment Attachment object represents files that have been attached to a Comment.

> 📘 **Note**
> Comments can currently support up to 3 attachments.

## Request format (input)

### Object properties

After following the Working with files and media guide, provide an array of objects under the attachments parameter in
the Create comment API, each containing the following properties:

| Parameter        | Type              | Description                                     | Example value                            |
|------------------|-------------------|-------------------------------------------------|------------------------------------------|
| `file_upload_id` | string (UUID)     | ID of a File Upload with a status of "uploaded" | `"2e2cdb8b-9897-4a6c-a935-82922b1cfb87"` |
| `type`           | string (optional) | Possible type values are: "file_upload"         | `"file_upload"`                          |

**Example Create Comment request:**

```json
{
  "parent": {
    "page_id": "d0a1ffaf-a4d8-4acf-a1ed-abae6e110418"
  },
  "rich_text": [
    {
      "text": {
        "content": "Thanks for the helpful page!"
      }
    }
  ],
  "attachments": {
    "file_upload_id": "2e2cdb8b-9897-4a6c-a935-82922b1cfb87"
  }
}
```

In the Notion app, when viewing a comment uploaded using the API, the user experience is automatically customized based
on the detected category of the file upload. For example, uploading a .png file displays your attachment as an inline
image instead of a regular file download block.

## Response format (output)

### Object properties

The response of Comment APIs like Create comment contains attachments with the following fields:

| Field      | Type          | Description                                                                                                     | Example value                                                                                  |
|------------|---------------|-----------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| `category` | string (enum) | The category of this attachment. Possible type values are: "audio", "image", "pdf", "productivity", and "video" | `"audio"`                                                                                      |
| `file`     | object        | A file object containing type-specific configuration.                                                           | `{"url": "https://s3.us-west-2.amazonaws.com/...", "expiry_time": "2025-06-10T21:26:03.070Z"}` |

**Example attachment object in Create Comment response:**

```json
{
  "category": "video",
  "file": {
    "url": "https://s3.us-west-2.amazonaws.com/...",
    "expiry_time": "2025-06-10T21:26:03.070Z"
  }
}
```

The `file.url` is a temporary download link generated at the time of retrieving a comment. See the guide on Retrieving
existing files to learn more about accessing the files you upload.