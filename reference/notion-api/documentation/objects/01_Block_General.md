# Block

A **block object** represents a piece of content in Notion. The API translates headings, toggles, paragraphs, lists, media, and more into different block types.

Use the `Retrieve block children` endpoint to list all blocks on a page.

---

## Example Block Object

```json
{
  "object": "block",
  "id": "c02fc1d3-db8b-45c5-a222-27595b15aea7",
  "parent": {
    "type": "page_id",
    "page_id": "59833787-2cf9-4fdf-8782-e53db20768a5"
  },
  "created_time": "2022-03-01T19:05:00.000Z",
  "last_edited_time": "2022-07-06T19:41:00.000Z",
  "created_by": {
    "object": "user",
    "id": "ee5f0f84-409a-440f-983a-a5315961c6e4"
  },
  "last_edited_by": {
    "object": "user",
    "id": "ee5f0f84-409a-440f-983a-a5315961c6e4"
  },
  "has_children": false,
  "archived": false,
  "in_trash": false,
  "type": "heading_2",
  "heading_2": {
    "rich_text": [
      {
        "type": "text",
        "text": {
          "content": "Lacinato kale",
          "link": null
        },
        "annotations": {
          "bold": false,
          "italic": false,
          "strikethrough": false,
          "underline": false,
          "code": false,
          "color": "green"
        },
        "plain_text": "Lacinato kale",
        "href": null
      }
    ],
    "color": "default",
    "is_toggleable": false
  }
}


⸻

Keys

📘 Fields marked with an * are always available. Others require read content capabilities.

Field	Type	Description	Example
object*	string	Always "block"	"block"
id*	string (UUIDv4)	Unique identifier for the block	"7af38973-3787-41b3-bd75-0ed3a1edfac9"
parent	object	Parent information. See Parent object.	{ "type": "block_id", "block_id": "..." }
type	string (enum)	Type of block (see list below)	"paragraph"
created_time	string (ISO 8601)	When the block was created	"2020-03-17T19:10:04.968Z"
created_by	Partial User	User who created the block	{ "object": "user", "id": "..." }
last_edited_time	string (ISO 8601)	When the block was last edited	"2020-03-17T19:10:04.968Z"
last_edited_by	Partial User	User who last edited the block	{ "object": "user", "id": "..." }
archived	boolean	If the block is archived	false
in_trash	boolean	If the block is deleted	false
has_children	boolean	Whether the block has nested blocks	true
{type}	block type object	Object containing type-specific block information	See reference


⸻

Supported Block Types
	•	bookmark
	•	breadcrumb
	•	bulleted_list_item
	•	callout
	•	child_database
	•	child_page
	•	column
	•	column_list
	•	divider
	•	embed
	•	equation
	•	file
	•	heading_1
	•	heading_2
	•	heading_3
	•	image
	•	link_preview
	•	numbered_list_item
	•	paragraph
	•	pdf
	•	quote
	•	synced_block
	•	table
	•	table_of_contents
	•	table_row
	•	template
	•	to_do
	•	toggle
	•	unsupported
	•	video

⸻

Block Types That Support Children

Some block types may contain nested blocks:
	•	bulleted_list_item
	•	callout
	•	child_database
	•	child_page
	•	column
	•	heading_1 (if is_toggleable: true)
	•	heading_2 (if is_toggleable: true)
	•	heading_3 (if is_toggleable: true)
	•	numbered_list_item
	•	paragraph
	•	quote
	•	synced_block
	•	table
	•	template
	•	to_do
	•	toggle

📘 Note: Unsupported block types appear in the API response with "type": "unsupported", but their structure may still be returned.

