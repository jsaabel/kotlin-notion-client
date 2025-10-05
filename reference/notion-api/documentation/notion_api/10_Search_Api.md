# Search by title

Searches all parent or child pages and databases that have been shared with an integration.

Returns all [pages](/reference/page) or [databases](/reference/database), excluding duplicated linked databases, that have titles that include the `query` param. If no `query` param is provided, then the response contains all pages or databases that have been shared with the integration. The results adhere to any limitations related to an [integration's capabilities](/reference/capabilities).

To limit the request to search only pages or to search only databases, use the `filter` param.

## Errors

Each Public API endpoint can return several possible error codes. See the [Error codes section](/reference/status-codes#error-codes) of the Status codes documentation for more information.

## Body Params

### query
**Type:** `string`

The text that the API compares page and data_source titles against.

### sort
**Type:** `object`

A set of criteria, `direction` and `timestamp` keys, that orders the results. The **only** supported timestamp value is `"last_edited_time"`. Supported `direction` values are `"ascending"` and `"descending"`. If `sort` is not provided, then the most recently edited results are returned first.

#### SORT OBJECT

- **direction** (`string`)  
  The direction to sort. Possible values include `ascending` and `descending`.

- **timestamp** (`string`)  
  The name of the timestamp to sort against. Possible values include `last_edited_time`.

### filter
**Type:** `object`

A set of criteria, `value` and `property` keys, that limits the results to either only pages or only databases. Possible `value` values are `"page"` or `"data_source"`. The only supported `property` value is `"object"`.

#### FILTER OBJECT

- **value** (`string`)  
  The value of the property to filter the results by. Possible values for object type include `page` or `database`. **Limitation:** Currently the only filter allowed is `object` which will filter by type of object (either `page` or `database`)

- **property** (`string`)  
  The name of the property to filter by. Currently the only property you can filter by is the object type. Possible values include `object`. **Limitation:** Currently the only filter allowed is `object` which will filter by type of object (either `page` or `database`)

### start_cursor
**Type:** `string`

A `cursor` value returned in a previous response that if supplied, limits the response to results starting after the `cursor`. If not supplied, then the first page of results is returned. Refer to [pagination](#) for more details.

### page_size
**Type:** `int32` | **Defaults to** `100`

The number of items from the full list to include in the response. Maximum: `100`.

## Notes

📘 **Pagination Support**  
The Search endpoint supports pagination. To learn more about working with paginated responses, see the pagination section of the Notion API Introduction.

🚧 **Database-Specific Searches**  
To search a specific database — not all databases shared with the integration — use the [Query a database] endpoint instead.

## Search optimizations and limitations

### Optimizations

Search works best when the request is as specific as possible. We recommend filtering by object (such as `page` or `database`) and providing a text `query` to narrow down results.

To speed up results, try reducing the `page_size`. The default `page_size` is 100.

Our implementation of the search endpoint includes an optimization where any pages or databases that are directly shared with an integration are guaranteed to be returned. If your use case requires pages or databases to immediately be available in search without an indexing delay, we recommend that you share relevant pages/databases with your integration directly.

### Limitations

The search endpoint works best when it's being used to query for pages and databases by name. It is not optimized for the following use cases:

* **Exhaustively enumerating through all the documents that a bot has access to in a workspace.** Search is not guaranteed to return everything, and the index may change as your integration iterates through pages and databases.
* **Searching or filtering within a particular database.** This use case is much better served by finding the database ID and using the Query a database endpoint.
* **Immediate and complete results.** Search indexing is not immediate. If an integration performs a search quickly after a page is shared with the integration (such as immediately after a user performs OAuth), then the response may not contain the page.
    * When an integration needs to present a user interface that depends on search results, we recommend including a *Refresh* button to retry the search. This will allow users to determine if the expected result is present or not, and give them a way to try again.
