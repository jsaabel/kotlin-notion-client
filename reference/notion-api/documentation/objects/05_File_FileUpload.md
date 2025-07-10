# File Upload

The File Upload object tracks the lifecycle of a file uploaded to Notion in the API.

> 📘 **Getting started**
> 
> View Working with files and media for a comprehensive, end-to-end guide to uploading and attaching files.

Once a file upload has a status of "uploaded", pass its ID in a file object with a type of file_upload to the API to attach it to blocks, pages, and databases in a Notion workspace.

## Object properties

The response of File Upload APIs like Retrieve a file upload contains FileUpload objects with the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `object` | "file_upload" | |
| `id` | UUID | ID of the FileUpload. |
| `created_time` | String | ISO 8601 timestamp when the FileUpload was created. |
| `last_edited_time` | String | ISO 8601 timestamp when the FileUpload was last modified. |
| `expiry_time` | String | Nullable. ISO 8601 timestamp when the FileUpload will expire, if the API integration that created it doesn't complete the upload and attach to at least one block or other object in a workspace. |
| `status` | One of:<br>- "pending"<br>- "uploaded"<br>- "expired"<br>- "failed" | Enum status of the file upload.<br><br>**pending** status means awaiting upload or completion of an upload.<br><br>**uploaded** status means file contents have been sent. If the expiry_time is null, that means the file upload has already been attached to a block or other object.<br><br>**expired** and **failed** file uploads can no longer be used. **failed** is only used for FileUploads with mode=external_url when the import was unsuccessful. |
| `filename` | String | Nullable. Name of the file, provided during the Create a file upload step, or, for single_part uploads, can be determined from the provided filename in the form data passed to the Send a file upload step.<br><br>A file extension is automatically added based on the content_type if the filename doesn't already have one. |
| `content_type` | String | Nullable. The MIME content type of the uploaded file. Must be provided explicitly or inferred from a filename that includes an extension.<br><br>For single_part uploads, the content type can remain null until the Send a file upload step and inferred from the file parameter's content type. |
| `content_length` | Integer | Nullable. The total size of the file, in bytes. For pending multi_part uploads, this field is a running total based on the file segments uploaded so far and recalculated at the end during the Complete a file upload step. |
| `upload_url` | String | Field only included for pending file uploads.<br>This is the URL to use for sending file contents. |
| `complete_url` | String | Field only included for pending file uploads created with a mode of multi_part.<br>This is the URL to use to complete a multi-part file upload. |
| `file_import_result` | String | Field only included for a failed or uploaded file upload created with a mode of external_url.<br>Provides details on the success or failure of importing a file into Notion using an external URL. |