# Retrieving existing files

Learn how to get a download link for files in the Notion API.

Files, images, and other media enrich your Notion workspace — from embedded screenshots and PDFs to page covers, icons,
and file properties in databases.

The Notion API makes it easy to retrieve existing files, so your integration can read and reference media
programmatically.

This guide walks you through how to retrieve files that already exist in your workspace (typically added via the UI).

## 🔍 What are file objects in Notion?

In the Notion API, files are represented as [file objects](/reference/file-object). These can appear in blocks (like
images, files, videos), page covers or icons, or as part of a `files` property in a database.

Each file object has a `type`, which is determined by how the file is stored:

- `external`: A public URL to a file hosted elsewhere (e.g., CDN)
- `file`: A file manually uploaded via the Notion UI
- `file_upload`: A file uploaded programmatically via the API (which becomes a `file` after attachment)

You can retrieve these file objects through API endpoints
like [Retrieve a page](/reference/retrieve-a-page), [Retrieve block children](/reference/get-block-children),
or [Retrieve page property item](/changelog/retrieve-page-property-values). Let's start there.

## Retrieve files in your workspace

Most files already added in your Notion workspace (like uploaded images, PDF blocks, or file properties) are `file` type
objects. These include a temporary URL you can use to download the file.

To retrieve files:

### A. From page content

Use the [Retrieve block children](/reference/get-block-children) endpoint to list blocks on a page:

```bash
curl --request GET \
  --url 'https://api.notion.com/v1/blocks/{block_id}/children' \
  --header 'Authorization: Bearer {YOUR_API_KEY}' \
  --header 'Notion-Version: 2022-06-28'
```

If the page has image, video, or file blocks, they'll look like this:

```json
{
  "type": "file",
  "file": {
    "url": "https://s3.us-west-2.amazonaws.com/secure.notion-static.com/...",
    "expiry_time": "2025-04-24T22:49:22.765Z"
  }
}
```

**Note:** The `url` is a temporary signed link that expires after 1 hour. Re-fetch the page to refresh it.

### B. From database properties

Use the [Retrieve a page](/reference/retrieve-a-page) endpoint to get a database item with file properties:

```bash
curl --request GET \
  --url 'https://api.notion.com/v1/pages/{page_id}' \
  --header 'Authorization: Bearer {YOUR_API_KEY}' \
  --header 'Notion-Version: 2022-06-28'
```

The `properties` field will include any file attachments in the `files` type:

```json
"Files & media": {
"type": "files",
"files": [
{
"type": "file",
"file": {
"url": "https://s3.us-west-2.amazonaws.com/...",
"expiry_time": "2025-04-24T22:49:22.765Z"
},
"name": "Resume.pdf"
}
]
}
```

---

*Updated 4 months ago*

For files larger than 20 MB, split them up and upload using multi-part mode: