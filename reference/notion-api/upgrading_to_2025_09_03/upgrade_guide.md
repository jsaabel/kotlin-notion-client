# Upgrading to 2025-09-03

Learn how to upgrade your Notion API integration to 2025-09-03 for better database support.

## Overview

In September 2025, Notion is launching several features to improve what you can do with databases. This includes support for multiple data sources under a single database, each of which can have different sets of properties (schemas). The database becomes a container for one or more data sources.

The concept of a database ID in the Notion app stays the same, and continues to be shown in the URL for a database followed by the ID of the specific view you're looking at. For example, in a link like `https://notion.so/workspace/248104cd477e80fdb757e945d38000bd?v=148104cd477e80bb928f000ce197ddf2`:

- `248104cd-477e-80fd-b757-e945d38000bd` is the database (container) ID.
- `148104cd477e80bb928f000ce197ddf2` is the database view (managing views is not currently supported in the API).
- **Note:** The ID of the specific data source you're looking at isn't embedded in the URL, but will be listed in a separate dropdown menu.

Prior to this release, databases were limited to one data source, so the data source ID was hidden. Now that multiple data sources are supported, we need a way to identify the specific data source for a request. Starting from the `2025-09-03` API version, Notion is providing a new set of APIs under `/v1/data_sources` for managing each data source. Most of your integration's existing database operations should move to this set of APIs.

The `/v1/databases` family of endpoints now refers to the database (container) as of `2025-09-03`. To discover the data sources available for a database, the database object includes a `data_sources` array, each having an `id` and a `name`. The data source ID can then by used with the `/v1/data_sources` APIs.

As a reminder, API versioning is determined by providing a mandatory `Notion-Version` HTTP header with each API request. If you're using the TypeScript SDK, you might be configuring the version in one place where the Notion client is instantiated, or passing it explicitly for each request. You can follow the rest of this guide incrementally, upgrading each use of the API at a time at your convenience.

We're also extending the concept of API versioning to integration webhooks to allow Notion to introduce backwards-incompatible changes without affecting your endpoint until you upgrade the API version in the integration settings. Ensure your webhook URL can handle events of both the old and new shape for a short period of time before making the upgrade.

## ⚠️ Existing API versions

The `2022-06-28` API versions (and older) will continue to work with existing databases in Notion that have a single data source. Webhooks will also generally continue to be delivered without any changes to the format.

However, if any Notion users create a second data source for a database in a workspace that's connected to your integration (starting on September 3, 2025), your database IDs are no longer precise enough for Notion to process the request.

Until you follow this guide to upgrade, Notion responds to requests involving a database ID with multiple data sources with validation errors that look like:

```json
{
  "code": "validation_error",
  "status": 400,
  "message": "Databases with multiple data sources are not supported in this API version.",
  "object": "error",
  "additional_data": {
    "error_type": "multiple_data_sources_for_database",
    "database_id": "27a5d30a-1728-4a1e-a788-71341f22fb97",
    "child_data_source_ids": [
      "164b19c5-58e5-4a47-a3a9-c905d9519c65",
      "25c104cd-477e-8047-836b-000b4aa4bc94"
    ],
    "minimum_api_version": "2025-09-03"
  }
}
```

The `additional_data` in the response can help you identify the relevant data source IDs to use instead as you upgrade your integration.

## API changes

### Step 1: Identify the data source IDs under a database

**📘 Only database and data source APIs are affected**

The `2025-09-03` API version upgrade does not make any backwards-incompatible changes from `2022-06-28` aside from the `/v1/databases` family of endpoints presented in this section, as well as Search results, and parameters for `parent` objects that accept `database_id`s.

This means that you may directly upgrade the `Notion-Version` header your integration provides for all other usage of Notion APIs (e.g. Comments, Users, OAuth). Taking this step first and working incrementally can reduce the surfaces with which you need to exercise care while upgrading.

First, identify the parts of your system that process database IDs. These may include:

- Responses of list and search APIs, e.g. Search.
- Database IDs provided directly by users of your system, or hard-coded based on URLs in the Notion app.
- Events for integration webhooks (covered in the Webhook changes section below).

For each entry point that uses database IDs, start your migration process by introducing an API call to the new Get Database API (`GET /v1/databases/:database_id`) endpoint to retrieve a list of child `data_sources`. For this new call, make sure to use the `2025-09-03` version in the `Notion-Version` header, even if the rest of your API calls haven't been updated yet.

```javascript
// GET /v1/databases/{database_id}
// -- RETURNS -->
{
  "object": "database",
  "id": "{database_id}",
  // ...
  "data_sources": [
    {
      "id": "{data_source_id}",
      "name": "Tasks DB"
    },
    // ...
  ]
}
```

```javascript
let notion = new Client({
  auth: "{ACCESS_TOKEN}",
  notionVersion: "2025-09-03",
})

const DATABASE_ID = "/* ... */"

try {
  const response = await notion.request({
    method: "GET",
    path: `databases/${DATABASE_ID}`,
  })
  
  const dataSources = response.data_sources
  // [{ id: "...", name: "..." }, ...]
  console.log(dataSources)
  
  // In the existing, single-source database case, there will only
  // be one data source.
  const dataSource = dataSources[0]
} catch (error) {
  // Handle `APIResponseError`
  console.error(error)
}

// ... Remaining code, not migrated yet.
notion = new Client({
  auth: "{ACCESS_TOKEN}",
  notionVersion: "2022-06-28",
})
// ...
```

To get a data source ID in the Notion app, the settings menu for a database includes a "Copy data source ID" button under "Manage data sources".

Having access to the data source ID (or rather, IDs, once Notion users start adding 2nd sources for their existing databases) for a database lets you continue onto the next few steps.

### Step 2: Provide data source IDs when creating pages or relations

Some APIs that accept `database_id` in the body parameters now support providing a specific `data_source_id` instead. This works for any API version, meaning you can switch over at your convenience, before or after upgrading these API requests to use `2025-09-03`:

- Creating a page with a database parent
- Defining a database relation property that points to another database

#### Create page

In the Create a page API, look for calls that look like this:

```javascript
// POST /v1/pages
{
  "parent": {
    "type": "database_id",
    "database_id": "..."
  }
}
```

```javascript
const response = await notion.pages.create({
  parent: {
    type: "database_id",
    database_id: DATABASE_ID,
  }
})
```

Change these to use `data_source_id` parents instead, using the code from Step 1 to get the ID of a database's data source:

```javascript
// POST /v1/pages
{
  "parent": {
    "type": "data_source_id",
    "data_source_id": "..."
  }
}
```

```javascript
// Get dataSource from Step 1
const response = await notion.request({
  method: "POST",
  path: "pages",
  body: {
    parent: {
      type: "data_source_id",
      data_source_id: dataSource.id,
    },
  }
})
```

#### Create or update database

For database relation properties, the API will include both a `database_id` and `data_source_id` fields in the read path instead of just a `database_id`.

In the write path, provide both, or at least just the `data_source_id`, as parameters to the API.

```json
"Projects": {
  "id": "~pex",
  "name": "Projects",
  "type": "relation",
  "relation": {
    "database_id": "6c4240a9-a3ce-413e-9fd0-8a51a4d0a49b",
    "data_source_id": "a42a62ed-9b51-4b98-9dea-ea6d091bc508",
    "dual_property": {
      "synced_property_name": "Tasks",
      "synced_property_id": "JU]K"
    }
  }
}
```

Note that database mentions in rich text will continue to reference the database, not the data source.

### Step 3: Migrate database endpoints to data sources

The next step is to migrate each existing use of database APIs to their new data source equivalents, taking into account the differences between the old `/v1/databases` APIs and new `/v1/data_sources` APIs:

- Return very similar responses, but with `object: "data_source"`, starting from `2025-09-03`
- Accept a specific data source ID in query, body, and path parameters, not a database ID
- Exist under the `/v1/data_sources` namespace, starting from version `2025-09-03`
- Require a custom API request with `notion.request` if you're using the TypeScript SDK, since we won't upgrade to SDK v5 until you get to Step 4 (below).

The following APIs are affected. Each of them is covered by a sub-section below, with more specific Before vs. After explanations and code snippets:

#### Query databases

**Before (2022-06-28):**

```javascript
// PATCH /v1/databases/:database_id/query
{
  // ...
}
```

```javascript
const response = await notion.databases.query({
  database_id: "...",
  // ...
})
```

**After (2025-09-03):**

When you update the API version, the path of this API changes, and now accepts a data source ID. With the TS SDK, you'll have to switch this to temporarily use a custom `notion.request(...)`, until you upgrade to the next major version as part of Step 4.

```javascript
// PATCH /v1/data_sources/:data_source_id/query
{
  // ...
}
```

```javascript
// Get dataSource from Step 1
const response = await notion.request({
  method: "POST",
  path: `data_sources/${dataSource.id}/query`,
  // ...
})

// After upgrading TS SDK:
const response = await notion.dataSources.query({
  data_source_id: dataSource.id,
  // ...
})
```

#### Retrieve database

**Before (2022-06-28):**

- Retrieving a database with multiple data sources fails with a `validation_error` message.

```javascript
// GET /v1/databases/:database_id
{
  // ...
}
```

```javascript
const response = await notion.databases.retrieve({
  database_id: "...",
  // ...
})
```

**After (2025-09-03):**

- The Retrieve Database API is now repurposed to return a list of `data_sources` (each with an `id` and `name`, as described in Step 1).
- The Retrieve Data Source API is the new home for getting up-to-date information on the properties (schema) of each data source under a database.

```javascript
// PATCH /v1/data_sources/:data_source_id/query
{
  // ...
}
```

```javascript
// Get dataSource from Step 1
const response = await notion.request({
  method: "GET",
  path: `data_sources/${dataSource.id}`,
  // ...
})

// After upgrading TS SDK:
const response = await notion.dataSources.retrieve({
  data_source_id: dataSource.id,
})
```

#### Create database

**Before (2022-06-28):**

```javascript
// POST /v1/databases
{
  "parent": {"type": "page_id", "page_id": "..."},
  "properties": {...},
  // ...
}
```

```javascript
const response = await notion.databases.create({
  parent: {type: "page_id", page_id: "..."},
  properties: {...},
  // ...
})
```

**After (2025-09-03):**

- Continue to use the Create Database API even after upgrading, when you want to create both a database and its initial data source
- `properties` for the initial data source you're creating now go under `initial_data_source[properties]` to better separate data source specific properties vs. ones that apply to the entire database
- Other parameters apply to the database and continue to be specified at the top-level when creating a database (`icon`, `cover`, `title`)
- Only use the new Create Data Source API to add a new data source (with a new set of `properties`) to an existing database

```javascript
// POST /v1/databases
{
  "initial_data_source": {
    "properties": {
      // ... (Data source properties behave the same as database properties previously)
    }
  },
  "parent": {"type": "workspace", "workspace": true} | {"type": "page_id", "page_id": "..."},
  "title": [...],
  "icon": {"type": "emoji", "emoji": "🚀"} | ...
}
```

```javascript
const response = await notion.request({
  method: "POST",
  path: "databases",
  body: {
    initial_data_source: {
      properties: {
        // ... (Data source properties behave the same as database properties previously)
      }
    },
  },
  parent: {type: "workspace", workspace: true} | {type: "page_id", page_id: "..."},
  title: [...],
  icon: {type: "emoji", emoji: "🚀"} | ...
})

// After upgrading TS SDK:
const response = await notion.databases.create({
  data_source_id: dataSource.id,
})
```

#### Update data source

**Before (2022-06-28):**

```javascript
// PATCH /v1/databases/:database_id
{
  // ...
}
```

```javascript
const response = await notion.databases.update({
  database_id: "...",
  // ...
})
```

**After (2025-09-03):**

- Continue to use the Update Database API for attributes that apply to the database: `parent`, `title`, `is_inline`, `icon`, `cover`, `in_trash`
    - `parent` can be used to move an existing database to a different page, or (for public integrations), to the workspace level as a private page. This is a new feature in Notion's API
    - `cover` is not supported when `is_inline` is `true`
- Switch over to the Update Data Source API to modify attributes that apply to a specific data source: `properties` (to change database schema), `in_trash` (to archive or unarchive a specific data source under a database), `title`, `description`
    - Changes to one data source's `properties` doesn't affect the schema for other data source, even if they share a common database

Example for updating a data source's properties and title:

```javascript
// PATCH /v1/data_sources/:data_source_id
{
  "properties": {...},
  "title": [...]
}
```

```javascript
const response = await notion.request({
  method: "PATCH",
  path: `data_sources/${dataSource.id}`,
  // ...
})

// After upgrading TS SDK:
const response = await notion.dataSources.update({
  properties: {...},
  title: [...]
})
```

Example for updating a database's parent (to move it), and switch it to be inline under the parent page:

```javascript
// PATCH /v1/databases/:database_id
{
  "parent": {"type": "page_id", "page_id": "NEW-PAGE-ID"},
  "is_inline": true
}
```

```javascript
const response = await notion.request({
  method: "PATCH",
  path: `databases/${DATABASE_ID}`,
  body: {
    parent: {type: "page_id", page_id: "NEW-PAGE-ID"},
    is_inline: true,
  }
})

// After upgrading TS SDK:
const response = await notion.dataSources.update({
  parent: {type: "page_id", page_id: "NEW-PAGE-ID"},
  is_inline: true,
})
```

#### Search

**Before (2022-06-28):**

- If any Notion users add a second data source to a database, existing integrations will not see any search results for that database.

**After (2025-09-03):**

- The Search API now only accepts `filter["value"] = "page" | "data_source"` instead of `"page" | "database"` when providing a `filter["type"] = "object"`. Make sure to update the body parameters accordingly when upgrading to `2025-09-03`.
- Similarly, the search API response returns data source IDs & objects.
- Aside from the IDs and `object: "data_source"` in these entries, the rest of the object shape of search is unchanged.
- Since results operate at the data source level, they continue to include `properties` (database schema) as before.
- If there are multiple data sources, all of them are included in the search response. Each of them will have a different data source ID.

### Step 4: Upgrade SDK (if applicable)

**📘 v5.0.0-rc.0**

v5 of the SDK is available as a release candidate:
- https://www.npmjs.com/package/@notionhq/client/v/5.0.0-rc.0
- https://github.com/makenotion/notion-sdk-js/releases/tag/v5.0.0-rc.0

The NPM package is published with the tag `next` instead of `latest`, so you can reference it using `@notionhq/client@next`.

If you're using Notion's TypeScript SDK, and have completed all of the steps above to rework your usage of Notion's endpoints to fit the `2025-09-03` suite of endpoints manually, we recommend completing the migration by upgrading to the next major version release, v5.0.0, via your `package.json` file (or other version management toolchain.)

The code snippets under Step 3 include the relevant syntax for the new `notion.dataSources.*` and `notion.databases.*` methods to assist in your upgrade. Go through each area where you used a manual `notion.request(...)` call, and switch it over to use one of the dedicated methods. Make sure you're setting the Notion version at initialization time to `2025-09-03`.

Note that the List databases (deprecated) endpoint, which has been removed since version `2022-02-22`, is no longer included as of v5 of the SDK.

## Webhook changes

### Introducing webhook versioning

When creating, editing, or viewing an integration webhook subscription in Notion's integration settings, there's a new option to set the API version that applies to events delivered to your webhook URL.

For new webhook endpoints, we recommend starting with the most recent version. For existing webhook subscriptions, you'll need to carefully introduce support for the added and changed webhook types. Ensure your webhook handler can accept both old & new event payloads before using the "Edit subscription" form to upgrade to the `2025-09-03` API version.

After you've tested your webhook endpoint to ensure the new events are being handled correctly for some period of time (for example, a few hours), you can clean up your system to only expect events with the updated shape. Read on below for specific details on what's changed in `2025-09-03`.

### New and modified event types

New `data_source` specific events have been added, and the corresponding existing `database` events now apply at the database level.

Here's a breakdown of how event types change names or behavior when upgraded to `2025-09-03`:

| Old Name | New Name | Description |
|----------|----------|-------------|
| `database.content_updated` | `data_source.content_updated` | Data source's content updates |
| `database.schema_updated` | `data_source.schema_updated` | Data source's schema updates |
| N/A (new event) | `data_source.created` | New data source is added to an existing database<br>`entity.type` is "data_source" |
| N/A (new event) | `data_source.moved` | Data source is moved to a different database<br>`entity.type` is "data_source" |
| N/A (new event) | `data_source.deleted` | Data source is deleted from a database<br>`entity.type` is "data_source" |
| N/A (new event) | `data_source.undeleted` | Data source is undeleted<br>`entity.type` is "data_source" |
| `database.created` | (unchanged) | New database is created with a default data source |
| `database.moved` | (unchanged) | Database is moved to different parent (i.e. page) |
| `database.deleted` | (unchanged) | Database is deleted from its parent |
| `database.undeleted` | (unchanged) | Database is undeleted |

### Updates to parent data

With the `2025-09-03` version, all webhooks for entities that can have data sources as parents now include a new field `data_source_id` under the `data.parent` object.

This applies to:

- Page events (`page.*`)
- Data source events (the `data_source.*` ones listed above)
- Database events (`database.*`), but only in rarer cases where databases are directly parented by another database (i.e. wikis)

For example, when a Notion user creates a page within a data source using the Notion app, the resulting `page.created` event has the following example shape (note the new `data.parent.data_source_id` field):

```json
{
  "id": "367cba44-b6f3-4c92-81e7-6a2e9659efd4",
  "timestamp": "2024-12-05T23:55:34.285Z",
  "workspace_id": "13950b26-c203-4f3b-b97d-93ec06319565",
  "workspace_name": "Quantify Labs",
  "subscription_id": "29d75c0d-5546-4414-8459-7b7a92f1fc4b",
  "integration_id": "0ef2e755-4912-8096-91c1-00376a88a5ca",
  "type": "page.created",
  "authors": [
    {
      "id": "c7c11cca-1d73-471d-9b6e-bdef51470190",
      "type": "person"
    }
  ],
  "accessible_by": [
    {
      "id": "556a1abf-4f08-40c6-878a-75890d2a88ba",
      "type": "person"
    },
    {
      "id": "1edc05f6-2702-81b5-8408-00279347f034",
      "type": "bot"
    }
  ],
  "attempt_number": 1,
  "entity": {
    "id": "153104cd-477e-809d-8dc4-ff2d96ae3090",
    "type": "page"
  },
  "data": {
    "parent": {
      "id": "36cc9195-760f-4fff-a67e-3a46c559b176",
      "type": "database",
      "data_source_id": "98024f3c-b1d3-4aec-a301-f01e0dacf023"
    }
  }
}
```

For compatibility with multi-source databases, use the provided `parent.data_source_id` to distinguish which data source the page lives in.

## Frequently asked questions

### When can I start using the 2025-09-03 version?

The API version is already available to use for Notion API requests as of late August. We recommend starting the upgrade process detailed above at your earliest convenience if your integration is affected by the changes.

There's no need to wait for `2025-09-03`. However:

- Please note that the Create Data Source API (`POST /v1/data_sources`) will return an error when adding new data sources to existing databases until Notion app changes are released around September 3.
    - **Early Access Available:** If you'd like to test multiple data sources before the official feature release as a part of your API upgrade process, please contact the Notion team at [email protected] with your Workspace ID (it looks like `4b350e73-eb5c-421b-b79b-9906294cfce9`). You can locate your workspace ID:
        - **Via API:** Make a `GET` request to `/v1/users/me` → the workspace ID will be in the response
        - **Via Settings Page:** Navigate to Settings & Members → Workspace → the ID is displayed in the workspace details section at the bottom
- Some of the UI flows displayed above, such as integration webhook versions, as well as the Notion app experience of viewing multiple data sources and copying their IDs, might not be available until on or around September 3.
- Some of the API Reference & Guides documentation (aside from this page), or other help content, might not be updated yet to consistently reference "data sources" for the individual tables under a "database".

### How long will the 2022-06-28 version continue to work?

We don't currently have any process for halting support of old Notion API versions. If we introduce a "minimum versioning" program in the future, we'll communicate this with all affected users with ample notice period (e.g. 6 months) and start with versions that came before `2022-06-28`.

However, even though API integrations continue to work, we recommend upgrading to `2025-09-03` as soon as possible. That way, your system is ready for in-app creation of data sources, gains new functionality when working with databases, and you can help Notion's support teams better handle any questions or requests you have by making sure you're up-to-date.

### Why is this the first version upgrade since 2022?

We aim to improve functionality in our API through backwards-compatible features first and foremost. We've shipped several changes since 2022, including the File Upload API, but generally aim to avoid having large sets of users have to go through a detailed upgrade progress when possible.

With the upcoming changes to the Notion app, we want our integration partners, developer community, ambassadors, and everyone else making great tools to have access to multi-source database functionality. This involves rethinking what a "database ID" in the API can do and repurposing API endpoints, necessitating the `2025-09-03` version release.

---

*Updated about 10 hours ago*