# Hand-built File Upload fixtures

The official Notion documentation ships no sample response for an **expired** upload, an upload
carrying **`file_import_result`**, or a pending **multi-part** upload showing `complete_url` /
`number_of_parts`. The fixtures here are hand-built from the documented shapes so those paths are
still covered by unit tests:

| Fixture | Covers |
|---------|--------|
| `get_retrieve_an_expired_file_upload.json` | `status: "expired"` |
| `get_retrieve_a_failed_import_file_upload.json` | `file_import_result` error variant |
| `get_retrieve_an_imported_file_upload.json` | `file_import_result` success variant |
| `post_create_a_multi_part_file_upload.json` | `complete_url`, `number_of_parts`, null `filename`/`content_type` |
| `get_retrieve_a_file_upload_unknown_status.json` | forward-compat fallbacks for an unknown status and an unknown import-result type |

Shapes are taken from:

- [File Upload object reference](https://developers.notion.com/reference/file-upload) and the
  `fileUploadObjectResponse` OpenAPI schema on
  [Retrieve a file upload](https://developers.notion.com/reference/retrieve-a-file-upload)
- [Importing external files](https://developers.notion.com/docs/importing-external-files),
  which carries the only published `file_import_result` example (the error variant)

Genuine, vendored official samples stay in `reference/notion-api/sample_responses/file_uploads/`.
