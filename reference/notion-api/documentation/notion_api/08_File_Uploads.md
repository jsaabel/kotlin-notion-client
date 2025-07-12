# File Uploads API

This document describes the File Uploads API endpoints for uploading files to Notion workspaces.

## Create a file upload

**POST** `https://api.notion.com/v1/file_uploads`

Use this API to initiate the process of uploading a file to your Notion workspace.

For a successful request, the response is a File Upload object with a status of "pending".

### Body Parameters

| Parameter         | Type   | Default       | Description                                                                                                                                                                                                                     |
|-------------------|--------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `mode`            | string | `single_part` | How the file is being sent. Use `multi_part` for files larger than 20MB. Use `external_url` for files that are temporarily hosted publicly elsewhere.                                                                           |
| `filename`        | string | -             | Name of the file to be created. Required when mode is `multi_part` or `external_url`. Otherwise optional, and used to override the filename. Must include an extension, or have one inferred from the `content_type` parameter. |
| `content_type`    | string | -             | MIME type of the file to be created. Recommended when sending the file in multiple parts. Must match the content type of the file that's sent, and the extension of the `filename` parameter if any.                            |
| `number_of_parts` | int32  | -             | When mode is `multi_part`, the number of parts you are uploading. Must be between 1 and 1,000. This must match the number of parts as well as the final `part_number` you send.                                                 |
| `external_url`    | string | -             | When mode is `external_url`, provide the HTTPS URL of a publicly accessible file to import into your workspace.                                                                                                                 |

### Headers

| Header           | Type   | Required | Description                                                                  |
|------------------|--------|----------|------------------------------------------------------------------------------|
| `Notion-Version` | string | Yes      | The API version to use for this request. The latest version is `2022-06-28`. |

## Send a file upload

**POST** `https://api.notion.com/v1/file_uploads/{file_upload_id}/send`

Use this API to transmit file contents to Notion for a file upload.

For this endpoint, use a Content-Type of `multipart/form-data`, and provide your file contents under the `file` key.

> **📘 Note**
>
> The use of multipart form data is unique to this endpoint. Other Notion APIs, including Create a file upload and
> Complete a file upload, use JSON parameters.

Include a boundary with the Content-Type header of your request as per RFC 2388. Most request libraries (e.g. fetch, ky)
automatically handle this as long as you provide a form data object but don't overwrite the Content-Type explicitly.

For more tips and examples, view the file upload guide.

When `mode=multi_part`, each part must include a form field `part_number` to indicate which part is being sent. Parts
may be sent concurrently up to standard Notion API rate limits, and may be sent out of order as long as all parts (
1, ..., part_number) are successfully sent before calling the complete file upload API.

### Body Parameters (Multipart Form Data)

| Parameter     | Type   | Required | Description                                                                                                                                                                                   |
|---------------|--------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `file`        | file   | Yes      | The raw binary file contents to upload.                                                                                                                                                       |
| `part_number` | string | No       | When using a `mode=multi_part` File Upload to send files greater than 20 MB in parts, this is the current part number. Must be an integer between 1 and 1000 provided as a string form field. |

## Complete a file upload

**POST** `https://api.notion.com/v1/file_uploads/{file_upload_id}/complete`

Use this API to finalize a `mode=multi_part` file upload after all of the parts have been sent successfully.

### Headers

| Header           | Type   | Required | Description                                                                  |
|------------------|--------|----------|------------------------------------------------------------------------------|
| `Notion-Version` | string | Yes      | The API version to use for this request. The latest version is `2022-06-28`. |

## Retrieve a file upload

**GET** `https://api.notion.com/v1/file_uploads/{file_upload_id}`

Use this API to get the details of a File Upload object.

### Headers

| Header           | Type   | Required | Description                                                                  |
|------------------|--------|----------|------------------------------------------------------------------------------|
| `Notion-Version` | string | Yes      | The API version to use for this request. The latest version is `2022-06-28`. |

## List file uploads

**GET** `https://api.notion.com/v1/file_uploads`

Use this API to retrieve file uploads for the current bot integration, sorted by most recent first.

### Headers

| Header           | Type   | Required | Description                                                                  |
|------------------|--------|----------|------------------------------------------------------------------------------|
| `Notion-Version` | string | Yes      | The API version to use for this request. The latest version is `2022-06-28`. |