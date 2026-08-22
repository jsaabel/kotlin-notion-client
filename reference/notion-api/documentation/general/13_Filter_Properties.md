# Filter Properties

> Source: https://developers.notion.com/reference/retrieve-a-page, https://developers.notion.com/reference/post-page, https://developers.notion.com/reference/patch-page, https://developers.notion.com/reference/query-a-data-source
> Fetched: 2026-08-22 (API version `2026-03-11`)

`filter_properties` is a query parameter that trims which page properties come
back in a response. It exists on four endpoints:

| Endpoint | Method | Added |
|---|---|---|
| Retrieve a page (`/v1/pages/{page_id}`) | GET | pre-existing (older than the 2026-03-11 upgrade guide; date of introduction not confirmed) |
| Create a page (`/v1/pages`) | POST | 2026-08-12 changelog |
| Update page properties (`/v1/pages/{page_id}`) | PATCH | 2026-08-12 changelog |
| Query a data source (`/v1/data_sources/{data_source_id}/query`) | POST | pre-existing, exact introduction date not confirmed by the changelog fetch |

## Shape

`filter_properties` is a **repeated query-string parameter**, not a JSON body
field — even on the POST/PATCH endpoints, it rides on the URL:

```
GET /v1/pages/{page_id}?filter_properties=title&filter_properties=J%40cS
POST /v1/pages?filter_properties=title
PATCH /v1/pages/{page_id}?filter_properties=title
POST /v1/data_sources/{data_source_id}/query?filter_properties=title
```

- Each value is a **property ID** (not a property name) — the same short IDs
  seen in a schema's `properties.<Name>.id` (e.g. `title`, `J@cS`, `%7Dji`).
  IDs with special characters must be percent-encoded in the query string.
- Repeat the parameter once per property to keep; there is no `[]` suffix and
  no comma-joined form.
- **Cap: 100 items.** Retrieve a page's reference page states this
  explicitly; treat the other three endpoints as sharing the same cap absent
  documented evidence otherwise.
- A property the underlying page doesn't have is silently omitted from the
  filtered response — not an error.
- Applies to `properties` only; it never hides `id`, `url`, `icon`, etc.

## Behavior per endpoint

- **Retrieve a page / Query a data source**: filters which properties are
  present on each returned page object. Pure a response-shaping optimization
  — smaller payloads when a caller only needs a handful of properties out of
  a wide schema.
- **Create a page / Update page properties**: filters which properties are
  echoed back in the response body. It does **not** restrict which
  properties can be *written* — the full `properties` object in the request
  is still applied; `filter_properties` only trims what comes back.

## This client

`PagesApi.retrieve/create/update` and `DataSourcesApi.query`/`queryAsFlow`/
`queryFirstPage` all take a `filterProperties: List<String>?` parameter and
validate the 100-item cap client-side before the HTTP call (see
`FilterPropertiesSupport.kt` / `validateFilterPropertiesLimit()`).
