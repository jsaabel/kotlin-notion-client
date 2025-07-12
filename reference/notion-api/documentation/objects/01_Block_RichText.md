# Rich Text

Notion uses **rich text** to support formatted, styled content like links, colors, bold text, code spans, and inline
mentions (e.g., pages, users, dates).

Blocks that support rich text include a `rich_text` array in their block object. Each entry is a **rich text object**
with plain text and optional formatting or metadata.

---

## Example Rich Text Object

```json
{
  "type": "text",
  "text": {
    "content": "Some words ",
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
  "plain_text": "Some words ",
  "href": null
}


⸻

Common Fields

Field	Type	Description
type	string	One of "text", "mention", or "equation"
plain_text	string	Unformatted text
href	string	URL of a link (if any)
annotations	object	Formatting info like bold, italic, underline, etc.
text	object	For type: "text" only – contains content and optional link
mention	object	For type: "mention" only – contains a mention type and corresponding data
equation	object	For type: "equation" only – contains a LaTeX expression


⸻

Annotations Object

"annotations": {
  "bold": true,
  "italic": false,
  "strikethrough": false,
  "underline": true,
  "code": false,
  "color": "purple_background"
}

Field	Type	Description
bold	boolean	Text is bold
italic	boolean	Text is italic
strikethrough	boolean	Text has strikethrough
underline	boolean	Text is underlined
code	boolean	Text is styled as inline code
color	string	Text color. Examples: "default", "blue", "red_background"


⸻

Rich Text Types

1. text

{
  "type": "text",
  "text": {
    "content": "inline link",
    "link": { "url": "https://developers.notion.com/" }
  },
  "plain_text": "inline link",
  "href": "https://developers.notion.com/"
}


⸻

2. equation

{
  "type": "equation",
  "equation": {
    "expression": "E = mc^2"
  },
  "plain_text": "E = mc^2"
}


⸻

3. mention

Mention types represent inline references to other Notion objects or metadata.

Mention Type	Example Value
database	{ "id": "a1d8..." }
date	{ "start": "2022-12-16", "end": null }
link_preview	{ "url": "https://..." }
page	{ "id": "3c61..." }
template_mention	today, now, or me
user	{ "object": "user", "id": "b2e1..." }

Example: Mention – Date

{
  "type": "mention",
  "mention": {
    "type": "date",
    "date": { "start": "2022-12-16", "end": null }
  },
  "plain_text": "2022-12-16"
}

Example: Mention – User

{
  "type": "mention",
  "mention": {
    "type": "user",
    "user": {
      "object": "user",
      "id": "b2e19928-b427-4aad-9a9d-fde65479b1d9"
    }
  },
  "plain_text": "@Anonymous"
}


⸻

Notes
	•	Rich text arrays can contain up to 100 objects (see request limits).
	•	plain_text is always present and provides unformatted content.
	•	For unsupported or inaccessible objects (e.g., a page the integration lacks access to), plain_text may be "Untitled" or "@Anonymous".

