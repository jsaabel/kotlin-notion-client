# Versioning

The Notion API is **versioned**, and versions are named using the **release date** (e.g., `2022-06-28`).

You **must** specify the version in each request using the `Notion-Version` header.

---

## Setting the Version

Use the `Notion-Version` header in your API requests:

### Example (cURL)

```bash
curl https://api.notion.com/v1/users/01da9b00-e400-4959-91ce-af55307647e5 \
  -H "Authorization: Bearer secret_t1CdN9S8yicG5eWLUOfhcWaOscVnFXns" \
  -H "Notion-Version: 2022-06-28"

🚧 Required Header
The Notion-Version header must be included in all REST API requests to ensure the response matches what your integration expects.

⸻

When a New Version is Released

A new version is released when a backwards-incompatible change is made.

Example: Property Type Name Change

Before version 2021-05-13

"properties": {
  "Description": {
    "type": "text",
    "text": [/* ... */]
  }
}

On or after version 2021-05-13

"properties": {
  "Description": {
    "type": "rich_text",
    "rich_text": [/* ... */]
  }
}

If you don’t upgrade your version, you’ll continue using the text property.
After upgrading, you’ll need to use rich_text.

⸻

Notes

📘 Versioning is only for breaking changes
Backwards-compatible changes (e.g., new endpoints or new fields) do not require a new version.

ℹ️ v1 in URL is not the same as API versioning
The /v1/ in Notion API URLs is unrelated to this versioning scheme. It will remain unchanged.

