# Importing external files

Learn how to migrate files from an external URL to Notion.

## Step 1: Start a file upload

To initiate the process of transferring a temporarily-hosted public file into your Notion workspace, use
the [Create a file upload](/reference/create-a-file-upload) with a `mode` of `"external_url"`, a `filename`, and the
`external_url` itself:

```bash
curl --request POST \
  --url 'https://api.notion.com/v1/file_uploads' \
  -H 'Authorization: Bearer ntn_****' \
  -H 'Content-Type: application/json' \
  -H 'Notion-Version: <<latestNotionVersion>>' \
  --data '{
    "mode": "external_url",
    "external_url": "https://example.com/image.png",
    "filename": "image.png"
  }'
```

At this step, Notion will return a `validation_error` (HTTP 400) if any of the following are true:

- The URL is not SSL-enabled, or not publicly accessible.
- The URL doesn't expose the `Content-Type` header for Notion to verify as part of a quick `HEAD` HTTPS request.
- The `Content-Length` header (size) of the file at the external URL exceeds your workspace's per-file size limit.
- You don't provide a valid filename and a supported MIME content type or extension.

## Step 2: Wait for the import to complete

After Step 1, Notion begins processing the file import asynchronously. To wait for the upload to finish, your
integration can do one of the following:

- **Polling.** Set up your integration to wait a sequence of intervals (e.g. 5, 15, 30, and 45 seconds, or an
  exponential backoff sequence) after creating the File Upload and poll
  the [Retrieve a file upload](/reference/retrieve-a-file-upload) until the `status` changes from `pending` to
  `uploaded` (or `failed`).
- **Listen to webhooks.** Notion will send one of the following types of [integration webhook](/reference/webhooks)
  events:
    - `file_upload.complete` - The import is complete, and your integration can proceed to using the FileUpload ID in
      Step 3.
    - `file_upload.upload_failed` - The import failed. This is typically due to:
        - File size is too large for your workspace (per-file limit exceeded).
        - The external service temporarily hosting the file you're importing is experiencing an outage, timing out, or
          requires authentication or additional headers at the time Notion's systems retrieve your file.
        - The file storage service Notion uses is experiencing an outage (rare).
        - Check the `data[file_import_result]` object for error codes and messages to help troubleshoot.
        - Try again later or with a smaller file. You won't be able to attach the failed File Upload to any blocks.
    - For both success and failure, the `entity` of the webhook payload will contain a `type` of `"file_upload"` and an
      `id` containing the ID of the FileUpload from Step 1.

![Screenshot of webhook settings in the Notion creator profile integration settings page.](https://files.readme.io/0413bdbb8e6e8351c9d7fd9c4e855c79f258a643e4a3f51d4468e31810faba5b-image.png)

*Screenshot of webhook settings in the Notion creator profile integration settings page.*

The outcome of the file import is recorded on the [File Upload](/reference/file-upload) object. If the import fails, the
status changes to `failed`. If it succeeds, the status changes to `uploaded`.

For example, in response to a `file_upload.upload_failed` webhook, your system can read the
`data.file_import_result.error` from the webhook response, or use
the [Retrieve a file upload](/reference/retrieve-a-file-upload) API and check the `file_import_result.error` to debug
the import failure:

```javascript
// GET /v1/file_uploads/:file_upload_id
// --- RETURNS -->
{
    "object"
:
    "file_upload",
        // ...
        "status"
:
    "failed",
        "file_import_result"
:
    {
        "type"
    :
        "error",
            "error"
    :
        {
            "type"
        :
            "validation_error",
                "code"
        :
            "file_upload_invalid_size",
                "message"
        :
            "The file size is not within the allowed limit of 5 MiB. Please try again with a new file upload.",
                "parameter"
        :
            null,
                "status_code"
        :
            null
        }
    ,
    }
}
```

The `file_import_result` object contains details on the `success` or `error`. In this example, the problem is a file
size validation issue that wasn't caught during Step 1—potentially because the external host did not provide a
`Content-Length` header for Notion to validate with a `HEAD` request. The same file size limits of 5 MiB for a free
workspace and 5 GiB for a paid workspace apply to external URL mode.

A file upload with a status of `failed` cannot be reused, and a new one must be created.

## Step 3: Attach the file upload

Using its ID, attach the File Upload (for example, to a block, page, or database) within one hour of creating it to
avoid expiry.

---

*Updated 4 months ago*