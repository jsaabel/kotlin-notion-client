# Page

The Page object contains the page property values of a single Notion page.

## Example page object

```json
{
  "object": "page",
  "id": "be633bf1-dfa0-436d-b259-571129a590e5",
  "created_time": "2022-10-24T22:54:00.000Z",
  "last_edited_time": "2023-03-08T18:25:00.000Z",
  "created_by": {
    "object": "user",
    "id": "c2f20311-9e54-4d11-8c79-7398424ae41e"
  },
  "last_edited_by": {
    "object": "user",
    "id": "9188c6a5-7381-452f-b3dc-d4865aa89bdf"
  },
  "cover": null,
  "icon": {
    "type": "emoji",
    "emoji": "🐞"
  },
  "parent": {
    "type": "database_id",
    "database_id": "a1d8501e-1ac1-43e9-a6bd-ea9fe6c8822b"
  },
  "archived": true,
  "in_trash": true,
  "properties": {
    "Due date": {
      "id": "M%3BBw",
      "type": "date",
      "date": {
        "start": "2023-02-23",
        "end": null,
        "time_zone": null
      }
    },
    "Status": {
      "id": "Z%3ClH",
      "type": "status",
      "status": {
        "id": "86ddb6ec-0627-47f8-800d-b65afd28be13",
        "name": "Not started",
        "color": "default"
      }
    },
    "Title": {
      "id": "title",
      "type": "title",
      "title": [
        {
          "type": "text",
          "text": {
            "content": "Bug bash",
            "link": null
          },
          "annotations": {
            "bold": false,
            "italic": false,
            "strikethrough": false,
            "underline": false,
            "code": false,
            "color": "default"
          },
          "plain_text": "Bug bash",
          "href": null
        }
      ]
    }
  },
  "url": "https://www.notion.so/Bug-bash-be633bf1dfa0436db259571129a590e5",
  "public_url": "https://jm-testing.notion.site/p1-6df2c07bfc6b4c46815ad205d132e22d"
}
```

All pages have a Parent. If the parent is a database, the property values conform to the schema laid out database's
properties. Otherwise, the only property value is the title.

Page content is available as blocks. The content can be read using retrieve block children and appended using append
block children.

## Page object properties

> 📘 **Note**
> Properties marked with an * are available to integrations with any capabilities. Other properties require read content
> capabilities in order to be returned from the Notion API. For more information on integration capabilities, see the
> capabilities guide.

| Property           | Type                                                                            | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | Example value                                                                      |
|--------------------|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| `object`*          | string                                                                          | Always "page".                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | `"page"`                                                                           |
| `id`*              | string (UUIDv4)                                                                 | Unique identifier of the page.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | `"45ee8d13-687b-47ce-a5ca-6e2e45548c4b"`                                           |
| `created_time`     | string (ISO 8601 date and time)                                                 | Date and time when this page was created. Formatted as an ISO 8601 date time string.                                                                                                                                                                                                                                                                                                                                                                                                                                                  | `"2020-03-17T19:10:04.968Z"`                                                       |
| `created_by`       | Partial User                                                                    | User who created the page.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | `{"object": "user","id": "45ee8d13-687b-47ce-a5ca-6e2e45548c4b"}`                  |
| `last_edited_time` | string (ISO 8601 date and time)                                                 | Date and time when this page was updated. Formatted as an ISO 8601 date time string.                                                                                                                                                                                                                                                                                                                                                                                                                                                  | `"2020-03-17T19:10:04.968Z"`                                                       |
| `last_edited_by`   | Partial User                                                                    | User who last edited the page.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | `{"object": "user","id": "45ee8d13-687b-47ce-a5ca-6e2e45548c4b"}`                  |
| `archived`         | boolean                                                                         | The archived status of the page.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | `false`                                                                            |
| `in_trash`         | boolean                                                                         | Whether the page is in Trash.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | `false`                                                                            |
| `icon`             | File Object (type of "external" or "file_upload" are supported) or Emoji object | Page icon.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |                                                                                    |
| `cover`            | File object (type of "external" or "file_upload" are supported)                 | Page cover image.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |                                                                                    |
| `properties`       | object                                                                          | Property values of this page. As of version 2022-06-28, properties only contains the ID of the property; in prior versions properties contained the values as well.<br><br>If parent.type is "page_id" or "workspace", then the only valid key is title.<br><br>If parent.type is "database_id", then the keys and values of this field are determined by the properties of the database this page belongs to.<br><br>**key** string: Name of a property as it appears in Notion.<br><br>**value** object: See Property value object. | `{ "id": "A%40Hk" }`                                                               |
| `parent`           | object                                                                          | Information about the page's parent. See Parent object.                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | `{ "type": "database_id", "database_id": "d9824bdc-8445-4327-be8b-5b47500af6ce" }` |
| `url`              | string                                                                          | The URL of the Notion page.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | `"https://www.notion.so/Avocado-d093f1d200464ce78b36e58a3f0d8043"`                 |
| `public_url`       | string                                                                          | The public page URL if the page has been published to the web. Otherwise, null.                                                                                                                                                                                                                                                                                                                                                                                                                                                       | `"https://jm-testing.notion.site/p1-6df2c07bfc6b4c46815ad205d132e22d"`             |