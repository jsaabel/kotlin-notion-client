# Block Types

A block object represents a piece of content within Notion. The API translates the headings, toggles, paragraphs, lists, media, and more that you can interact with in the Notion UI as different block type objects.

For example, the following block object represents a Heading 2 in the Notion UI:

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
```

Use the Retrieve block children endpoint to list all of the blocks on a page.

## Keys

> 📘 Fields marked with an * are available to integrations with any capabilities. Other properties require read content capabilities in order to be returned from the Notion API. Consult the integration capabilities reference for details.

| Field | Type | Description | Example value |
|-------|------|-------------|---------------|
| object* | string | Always "block". | "block" |
| id* | string (UUIDv4) | Identifier for the block. | "7af38973-3787-41b3-bd75-0ed3a1edfac9" |
| parent | object | Information about the block's parent. See Parent object. | `{ "type": "block_id", "block_id": "7d50a184-5bbe-4d90-8f29-6bec57ed817b" }` |
| type | string (enum) | Type of block. Possible values are:<br/>- "bookmark"<br/>- "breadcrumb"<br/>- "bulleted_list_item"<br/>- "callout"<br/>- "child_database"<br/>- "child_page"<br/>- "column"<br/>- "column_list"<br/>- "divider"<br/>- "embed"<br/>- "equation"<br/>- "file"<br/>- "heading_1"<br/>- "heading_2"<br/>- "heading_3"<br/>- "image"<br/>- "link_preview"<br/>- "numbered_list_item"<br/>- "paragraph"<br/>- "pdf"<br/>- "quote"<br/>- "synced_block"<br/>- "table"<br/>- "table_of_contents"<br/>- "table_row"<br/>- "template"<br/>- "to_do"<br/>- "toggle"<br/>- "unsupported"<br/>- "video" | "paragraph" |
| created_time | string (ISO 8601 date time) | Date and time when this block was created. Formatted as an ISO 8601 date time string. | "2020-03-17T19:10:04.968Z" |
| created_by | Partial User | User who created the block. | `{"object": "user","id": "45ee8d13-687b-47ce-a5ca-6e2e45548c4b"}` |
| last_edited_time | string (ISO 8601 date time) | Date and time when this block was last updated. Formatted as an ISO 8601 date time string. | "2020-03-17T19:10:04.968Z" |
| last_edited_by | Partial User | User who last edited the block. | `{"object": "user","id": "45ee8d13-687b-47ce-a5ca-6e2e45548c4b"}` |
| archived | boolean | The archived status of the block. | false |
| in_trash | boolean | Whether the block has been deleted. | false |
| has_children | boolean | Whether or not the block has children blocks nested within it. | true |
| {type} | block type object | An object containing type-specific block information. | Refer to the block type object section for examples of each block type. |

## Block types that support child blocks

Some block types contain nested blocks. The following block types support child blocks:

- Bulleted list item
- Callout
- Child database
- Child page
- Column
- Heading 1, when the is_toggleable property is true
- Heading 2, when the is_toggleable property is true
- Heading 3, when the is_toggleable property is true
- Numbered list item
- Paragraph
- Quote
- Synced block
- Table
- Template
- To do
- Toggle

> 📘 The API does not support all block types.
>
> Only the block type objects listed in the reference below are supported. Any unsupported block types appear in the structure, but contain a type set to "unsupported".

## Block type objects

Every block object has a key corresponding to the value of type. Under the key is an object with type-specific block information.

> 📘 Many block types support rich text. In cases where it is supported, a rich_text object will be included in the block type object. All rich_text objects will include a plain_text property, which provides a convenient way for developers to access unformatted text from the Notion block.

### Audio

Audio block objects contain a file object detailing information about the audio file.

**Example Audio block object**

```json
{
  "type": "audio",
  //...other keys excluded
  "audio": {
    "type": "external",
    "external": {
      "url": "https://companywebsite.com/files/sample.mp3"
    }
  }
}
```

**Supported audio types**

The following file types can be attached with external URLs in the API as well as in the Notion app UI:

- .mp3
- .wav
- .ogg
- .oga
- .m4a

A wider set of audio files is supported in the File Upload API and can be attached using a file_upload ID.

**Supported file upload types**

See the file upload reference for a list of supported file extensions and content types when attaching a File Upload to a block.

Audio blocks only support file types in the "audio" section of the table.

### Bookmark

Bookmark block objects contain the following information within the bookmark property:

| Field | Type | Description |
|-------|------|-------------|
| caption | array of rich text objects text | The caption for the bookmark. |
| url | string | The link for the bookmark. |

**Example Bookmark block object**

```json
{
  //...other keys excluded
  "type": "bookmark",
  //...other keys excluded
  "bookmark": {
    "caption": [],
    "url": "https://companywebsite.com"
  }
}
```

### Breadcrumb

Breadcrumb block objects do not contain any information within the breadcrumb property.

**Example Breadcrumb block object**

```json
{
  //...other keys excluded
  "type": "breadcrumb",
  //...other keys excluded
  "breadcrumb": {}
}
```

### Bulleted list item

Bulleted list item block objects contain the following information within the bulleted_list_item property:

| Field | Type | Description |
|-------|------|-------------|
| rich_text | array of rich text objects | The rich text in the bulleted_list_item block. |
| color | string (enum) | The color of the block. Possible values are: "blue", "blue_background", "brown", "brown_background", "default", "gray", "gray_background", "green", "green_background", "orange", "orange_background", "yellow", "green", "pink", "pink_background", "purple", "purple_background", "red", "red_background", "yellow_background" |
| children | array of block objects | The nested child blocks (if any) of the bulleted_list_item block. |

**Example Bulleted list item block object**

```json
{
  //...other keys excluded
  "type": "bulleted_list_item",
  //...other keys excluded
  "bulleted_list_item": {
    "rich_text": [{
      "type": "text",
      "text": {
        "content": "Lacinato kale",
        "link": null
      }
      // ..other keys excluded
    }],
    "color": "default",
    "children":[{
      "type": "paragraph"
      // ..other keys excluded
    }]
  }
}
```

### Callout

Callout block objects contain the following information within the callout property:

| Field | Type | Description |
|-------|------|-------------|
| rich_text | array of rich text objects | The rich text in the callout block. |
| icon | object | An emoji or file object that represents the callout's icon. If the callout does not have an icon. |
| color | string (enum) | The color of the block. Possible values are: "blue", "blue_background", "brown", "brown_background", "default", "gray", "gray_background", "green", "green_background", "orange", "orange_background", "yellow", "green", "pink", "pink_background", "purple", "purple_background", "red", "red_background", "yellow_background" |

**Example Callout block object**

```json
{
  //...other keys excluded
	"type": "callout",
   // ..other keys excluded
   "callout": {
   	"rich_text": [{
      "type": "text",
      "text": {
        "content": "Lacinato kale",
        "link": null
      }
      // ..other keys excluded
    }],
     "icon": {
       "emoji": "⭐"
     },
     "color": "default"
   }
}
```

### Child database

Child database block objects contain the following information within the child_database property:

| Field | Type | Description |
|-------|------|-------------|
| title | string | The plain text title of the database. |

**Example Child database block**

```json
{
  //...other keys excluded
  "type": "child_database",
  //...other keys excluded
  "child_database": {
    "title": "My database"
  }
}
```

> 📘 **Creating and updating child_database blocks**
>
> To create or update child_database type blocks, use the Create a database and the Update a database endpoints, specifying the ID of the parent page in the parent body param.

### Child page

Child page block objects contain the following information within the child_page property:

| Field | Type | Description |
|-------|------|-------------|
| title | string | The plain text title of the page. |

**Example Child page block object**

```json
{
  //...other keys excluded
  "type": "child_page",
  //...other keys excluded
  "child_page": {
    "title": "Lacinato kale"
  }
}
```

> 📘 **Creating and updating child_page blocks**
>
> To create or update child_page type blocks, use the Create a page and the Update page endpoints, specifying the ID of the parent page in the parent body param.

### Code

Code block objects contain the following information within the code property:

| Field | Type | Description |
|-------|------|-------------|
| caption | array of Rich text object text objects | The rich text in the caption of the code block. |
| rich_text | array of Rich text object text objects | The rich text in the code block. |
| language | string (enum) | The language of the code contained in the code block. Possible values include: "abap", "arduino", "bash", "basic", "c", "clojure", "coffeescript", "c++", "c#", "css", "dart", "diff", "docker", "elixir", "elm", "erlang", "flow", "fortran", "f#", "gherkin", "glsl", "go", "graphql", "groovy", "haskell", "html", "java", "javascript", "json", "julia", "kotlin", "latex", "less", "lisp", "livescript", "lua", "makefile", "markdown", "markup", "matlab", "mermaid", "nix", "objective-c", "ocaml", "pascal", "perl", "php", "plain text", "powershell", "prolog", "protobuf", "python", "r", "reason", "ruby", "rust", "sass", "scala", "scheme", "scss", "shell", "sql", "swift", "typescript", "vb.net", "verilog", "vhdl", "visual basic", "webassembly", "xml", "yaml", "java/c/c++/c#" |

**Example Code block object**

```json
{
  // ... other keys excluded
  "type": "code",
  // ... other keys excluded
  "code": {
    "caption": [],
    "rich_text": [{
      "type": "text",
      "text": {
        "content": "const a = 3"
      }
    }],
    "language": "javascript"
  }
}
```

### Column list and column

Column lists are parent blocks for columns. They do not contain any information within the column_list property.

**Example Column list block object**

```json
{
  // ... other keys excluded
  "type": "column_list",
  // ... other keys excluded
  "column_list": {}
}
```

Columns are parent blocks for any block types listed in this reference except for other columns. They do not require any information within the column property, but a width_ratio number between 0 and 1 can be provided to customize the width of a column relative to others in the same column list. When omitted, the default is to use equal widths for all columns. When provided, width_ratios should add up to 1.

Columns can only be appended to column_lists.

**Example Column object**

```json
{
  // ... other keys excluded
  "type": "column",
  // ... other keys excluded
  "column": {
    "column_ratio": 0.25
  }
}
```

When creating a column_list block via Append block children, the column_list must have at least two columns, and each column must have at least one child.

**Retrieve the content in a column list**

Follow these steps to fetch the content in a column_list:

1. Get the column_list ID from a query to Retrieve block children for the parent page.
2. Get the column children from a query to Retrieve block children for the column_list.
3. Get the content in each individual column from a query to Retrieve block children for the unique column ID.

### Divider

Divider block objects do not contain any information within the divider property.

**Example Divider block object**

```json
{
  //...other keys excluded
  "type": "divider",
  //...other keys excluded
  "divider": {}
}
```

### Embed

Embed block objects include information about another website displayed within the Notion UI. The embed property contains the following information:

| Field | Type | Description |
|-------|------|-------------|
| url | string | The link to the website that the embed block displays. |

**Example Embed block object**

```json
{
  //...other keys excluded
  "type": "embed",
  //...other keys excluded
  "embed": {
    "url": "https://companywebsite.com"
  }
}
```

> 🚧 **Differences in embed blocks between the Notion app and the API**
>
> The Notion app uses a 3rd-party service, iFramely, to validate and request metadata for embeds given a URL. This works well in a web app because Notion can kick off an asynchronous request for URL information, which might take seconds or longer to complete, and then update the block with the metadata in the UI after receiving a response from iFramely.
>
> We chose not to call iFramely when creating embed blocks in the API because the API needs to be able to return faster than the UI, and because the response from iFramely could actually cause us to change the block type. This would result in a slow and potentially confusing experience as the block in the response would not match the block sent in the request.
>
> The result is that embed blocks created via the API may not look exactly like their counterparts created in the Notion app.

> 👍 Vimeo video links can be embedded in a Notion page via the public API using the embed block type.
>
> For example, the following object can be passed to the Append block children endpoint:
>
> ```json
> {
>   "children": [
>     {
>       "embed": {
>         "url": "https://player.vimeo.com/video/226053498?h=a1599a8ee9"
>       }
>     }
>   ]
> }
> ```
>
> For other video sources, see Supported video types.

### Equation

Equation block objects are represented as children of paragraph blocks. They are nested within a rich text object and contain the following information within the equation property:

| Field | Type | Description |
|-------|------|-------------|
| expression | string | A KaTeX compatible string. |

**Example Equation object**

```json
{
  //...other keys excluded
  "type": "equation",
  //...other keys excluded
  "equation": {
    "expression": "e=mc^2"
  }
}
```

### File

File block objects contain the following information within the file property:

| Field | Type | Description |
|-------|------|-------------|
| caption | array of rich text objects | The caption of the file block. |
| type | One of: "file", "external", "file_upload" | Type of file. This enum value indicates which of the following three objects are populated. |
| file | Notion-hosted file object | A file object that details information about the file contained in the block: a temporary download url and expiry_time. After the expiry_time, fetch the block again from the API to get a new url. Only valid as a parameter if copied verbatim from the file field of a recent block API response from Notion. To attach a file, provide a type of file_upload instead. |
| external | External file object | An object with a url property, identifying a publicly accessible URL. |
| file_upload | File upload object | An object with the id of a FileUpload to attach to the block. After attaching, the API response responds with a type of file, not file_upload, so your integration can access a download url. |
| name | string | The name of the file, as shown in the Notion UI. Note that the UI may auto-append .pdf or other extensions. When attaching a file_upload, the name parameter is not required. |

**Example File block**

```json
{
  // ... other keys excluded
  "type": "file",
  // ... other keys excluded
  "file": {
    "caption": [],
    "type": "external",
    "external": {
      "url": "https://companywebsite.com/files/doc.txt"
    },
    "name": "doc.txt"
  }
}
```

### Headings

All heading block objects, heading_1, heading_2, and heading_3, contain the following information within their corresponding objects:

| Field | Type | Description |
|-------|------|-------------|
| rich_text | array of rich text objects | The rich text of the heading. |
| color | string (enum) | The color of the block. Possible values are: "blue", "blue_background", "brown", "brown_background", "default", "gray", "gray_background", "green", "green_background", "orange", "orange_background", "yellow", "green", "pink", "pink_background", "purple", "purple_background", "red", "red_background", "yellow_background" |
| is_toggleable | boolean | Whether or not the heading block is a toggle heading or not. If true, then the heading block toggles and can support children. If false, then the heading block is a static heading block. |

**Example Heading 1 block object**

```json
{
  //...other keys excluded
  "type": "heading_1",
  //...other keys excluded
  "heading_1": {
    "rich_text": [{
      "type": "text",
      "text": {
        "content": "Lacinato kale",
        "link": null
      }
    }],
    "color": "default",
    "is_toggleable": false
  }
}
```

**Example Heading 2 block object**

```json
{
  //...other keys excluded
  "type": "heading_2",
  //...other keys excluded
  "heading_2": {
    "rich_text": [{
      "type": "text",
      "text": {
        "content": "Lacinato kale",
        "link": null
      }
    }],
    "color": "default",
    "is_toggleable": false
  }
}
```

**Example Heading 3 block object**

```json
{
  //...other keys excluded
  "type": "heading_3",
  //...other keys excluded
  "heading_3": {
    "rich_text": [{
      "type": "text",
      "text": {
        "content": "Lacinato kale",
        "link": null
      }
    }],
    "color": "default",
    "is_toggleable": false
  }
}
```

### Image

Image block objects contain a file object detailing information about the image.

**Example Image block object**

```json
{
  // ... other keys excluded
  "type": "image",
  // ... other keys excluded
  "image": {
    "type": "external",
    "external": {
      "url": "https://website.domain/images/image.png"
    }
  }
}
```

**Supported external image types**

The image must be directly hosted. In other words, the url cannot point to a service that retrieves the image. The following image types are supported:

- .bmp
- .gif
- .heic
- .jpeg
- .jpg
- .png
- .svg
- .tif
- .tiff

**Supported file upload types**

See the file upload reference for a list of supported file extensions and content types when attaching a File Upload to a block.

Image blocks only support file types in the "image" section of the table.

### Link Preview

Link Preview block objects contain the originally pasted url:

**Example Link preview block object**

```json
{
  //...other keys excluded
  "type": "link_preview",
  //...other keys excluded
  "link_preview": {
    "url": "https://github.com/example/example-repo/pull/1234"
  }
}
```

> 🚧 The link_preview block can only be returned as part of a response. The API does not support creating or appending link_preview blocks.

### Mention

A mention block object is a child of a rich text object that is nested within a paragraph block object. This block type represents any @ tag in the Notion UI, for a user, date, Notion page, Notion database, or a miniaturized version of a Link Preview.

A mention block object contains the following fields:

| Field | Type | Description |
|-------|------|-------------|
| type | "database", "date", "link_preview", "page", "user" | A constant string representing the type of the mention. |
| {type} | object | An object with type-specific information about the mention. |

**Example Mention object**

```json
{
  //...other keys excluded
  "type": "page",
  "page": {
    "id": "3c612f56-fdd0-4a30-a4d6-bda7d7426309"
  }
}
```

### Numbered list item

Numbered list item block objects contain the following information within the numbered_list_item property:

| Field | Type | Description |
|-------|------|-------------|
| rich_text | array of rich text objects | The rich text displayed in the numbered_list_item block. |
| color | string (enum) | The color of the block. Possible values are: "blue", "blue_background", "brown", "brown_background", "default", "gray", "gray_background", "green", "green_background", "orange", "orange_background", "yellow", "green", "pink", "pink_background", "purple", "purple_background", "red", "red_background", "yellow_background" |
| children | array of block objects | The nested child blocks (if any) of the numbered_list_item block. |

**Example Numbered list item block**

```json
{
  //...other keys excluded
  "type": "numbered_list_item",
  "numbered_list_item": {
    "rich_text": [
      {
        "type": "text",
        "text": {
          "content": "Finish reading the docs",
          "link": null
        }
      }
    ],
    "color": "default"
  }
}
```

### Paragraph

Paragraph block objects contain the following information within the paragraph property:

| Field | Type | Description |
|-------|------|-------------|
| rich_text | array of rich text objects | The rich text displayed in the paragraph block. |
| color | string (enum) | The color of the block. Possible values are: "blue", "blue_background", "brown", "brown_background", "default", "gray", "gray_background", "green", "green_background", "orange", "orange_background", "yellow", "green", "pink", "pink_background", "purple", "purple_background", "red", "red_background", "yellow_background" |
| children | array of block objects | The nested child blocks (if any) of the paragraph block. |

**Example Paragraph block object**

```json
{
  //...other keys excluded
  "type": "paragraph",
  //...other keys excluded
  "paragraph": {
    "rich_text": [{
      "type": "text",
      "text": {
        "content": "Lacinato kale",
        "link": null
      }
    }],
    "color": "default"
}
```

**Example Paragraph block object with a child Mention block object**

```json
{
//...other keys excluded
	"type": "paragraph",
  	"paragraph":{
  		"rich_text": [
    		{
      		"type": "mention",
      		"mention": {
        		"type": "date",
        		"date": {
          		"start": "2023-03-01",
          		"end": null,
          		"time_zone": null
        		}
      		},
      		"annotations": {
        		"bold": false,
        		"italic": false,
        		"strikethrough": false,
        		"underline": false,
        		"code": false,
        		"color": "default"
      		},
      		"plain_text": "2023-03-01",
      		"href": null
    		},
    		{
          "type": "text",
      		"text": {
        		"content": " ",
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
      		"plain_text": " ",
      		"href": null
    		}
  		],
  		"color": "default"
  	}
}
```

### PDF

A PDF block object represents a PDF that has been embedded within a Notion page. It contains the following fields:

| Property | Type | Description |
|----------|------|-------------|
| caption | array of rich text objects | A caption, if provided, for the PDF block. |
| type | One of: "file", "external", "file_upload" | A constant string representing the type of PDF. file indicates a Notion-hosted file, and external represents a third-party link. file_upload is only valid when providing parameters to attach a File Upload to a PDF block. |
| external, file, file_upload | file object | An object containing type-specific information about the PDF. |

```json
{
  //...other keys excluded
  "type": "pdf",
  //...other keys excluded
  "pdf": {
    "type": "external",
    "external": {
      "url": "https://website.domain/files/doc.pdf"
    }
  }
}
```

**Supported file upload types**

See the file upload reference for a list of supported file extensions and content types when attaching a File Upload to a block.

PDF blocks only support a type of .pdf.

### Quote

Quote block objects contain the following information within the quote property:

| Field | Type | Description |
|-------|------|-------------|
| rich_text | array of rich text objects | The rich text displayed in the quote block. |
| color | string (enum) | The color of the block. Possible values are: "blue", "blue_background", "brown", "brown_background", "default", "gray", "gray_background", "green", "green_background", "orange", "orange_background", "yellow", "green", "pink", "pink_background", "purple", "purple_background", "red", "red_background", "yellow_background" |
| children | array of block objects | The nested child blocks, if any, of the quote block. |

**Example Quote block**

```json
{
	//...other keys excluded
	"type": "quote",
   //...other keys excluded
   "quote": {
   	"rich_text": [{
      "type": "text",
      "text": {
        "content": "To be or not to be...",
        "link": null
      },
    	//...other keys excluded
    }],
    //...other keys excluded
    "color": "default"
   }
}
```

### Synced block

Similar to the Notion UI, there are two versions of a synced_block object: the original block that was created first and doesn't yet sync with anything else, and the duplicate block or blocks synced to the original.

> 📘 An original synced block must be created before corresponding duplicate block or blocks can be made.

**Original synced block**

Original synced block objects contain the following information within the synced_block property:

| Field | Type | Description |
|-------|------|-------------|
| synced_from | null | The value is always null to signify that this is an original synced block that does not refer to another block. |
| children | array of block objects | The nested child blocks, if any, of the synced_block block. These blocks will be mirrored in the duplicate synced_block. |

**Example Original synced block**

```json
{
    //...other keys excluded
  	"type": "synced_block",
    "synced_block": {
        "synced_from": null,
        "children": [
            {
                "callout": {
                    "rich_text": [
                        {
                            "type": "text",
                            "text": {
                                "content": "Callout in synced block"
                            }
                        }
                    ]
                }
            }
        ]
    }
}
```

**Duplicate synced block**

Duplicate synced block objects contain the following information within the synced_from object:

| Field | Type | Description |
|-------|------|-------------|
| type | string (enum) | The type of the synced from object. Possible values are: "block_id" |
| block_id | string (UUIDv4) | An identifier for the original synced_block. |

**Example Duplicate synced block object**

```json
{
    //...other keys excluded
  	"type": "synced_block",
    "synced_block": {
        "synced_from": {
            "block_id": "original_synced_block_id"
        }
    }
}
```

> 🚧 The API does not supported updating synced block content.

### Table

Table block objects are parent blocks for table row children. Table block objects contain the following fields within the table property:

| Field | Type | Description |
|-------|------|-------------|
| table_width | integer | The number of columns in the table. Note that this cannot be changed via the public API once a table is created. |
| has_column_header | boolean | Whether the table has a column header. If true, then the first row in the table appears visually distinct from the other rows. |
| has_row_header | boolean | Whether the table has a header row. If true, then the first column in the table appears visually distinct from the other columns. |

**Example Table block object**

```json
{
  //...other keys excluded
  "type": "table",
  "table": {
    "table_width": 2,
    "has_column_header": false,
    "has_row_header": false
  }
}
```

> 🚧 table_width can only be set when the table is first created.
>
> Note that the number of columns in a table can only be set when the table is first created. Calls to the Update block endpoint to update table_width fail.

**Table rows**

Follow these steps to fetch the table_rows of a table:

1. Get the table ID from a query to Retrieve block children for the parent page.
2. Get the table_rows from a query to Retrieve block children for the table.

A table_row block object contains the following fields within the table_row property:

| Property | Type | Description |
|----------|------|-------------|
| cells | array of array of rich text objects | An array of cell contents in horizontal display order. Each cell is an array of rich text objects. |

**Example Table row block object**

```json
{
  //...other keys excluded
  "type": "table_row",
  "table_row": {
    "cells": [
      [
        {
          "type": "text",
          "text": {
            "content": "column 1 content",
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
          "plain_text": "column 1 content",
          "href": null
        }
      ],
      [
        {
          "type": "text",
          "text": {
            "content": "column 2 content",
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
          "plain_text": "column 2 content",
          "href": null
        }
      ],
      [
        {
          "type": "text",
          "text": {
            "content": "column 3 content",
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
          "plain_text": "column 3 content",
          "href": null
        }
      ]
    ]
  }
}
```

> 📘 When creating a table block via the Append block children endpoint, the table must have at least one table_row whose cells array has the same length as the table_width.

### Table of contents

Table of contents block objects contain the following information within the table_of_contents property:

| Property | Type | Description |
|----------|------|-------------|
| color | string (enum) | The color of the block. Possible values are: "blue", "blue_background", "brown", "brown_background", "default", "gray", "gray_background", "green", "green_background", "orange", "orange_background", "yellow", "green", "pink", "pink_background", "purple", "purple_background", "red", "red_background", "yellow_background" |

**Example Table of contents block object**

```json
{
  //...other keys excluded
	"type": "table_of_contents",
  "table_of_contents": {
  	"color": "default"
  }
}
```

### Template

> ❗️ **Deprecation Notice**
>
> As of March 27, 2023 creation of template blocks will no longer be supported.

Template blocks represent template buttons in the Notion UI.

Template block objects contain the following information within the template property:

| Field | Type | Description |
|-------|------|-------------|
| rich_text | array of rich text objects | The rich text displayed in the title of the template. |
| children | array of block objects | The nested child blocks, if any, of the template block. These blocks are duplicated when the template block is used in the UI. |

**Example Template block object**

```json
{
  //...other keys excluded
  "template": {
    "rich_text": [
      {
        "type": "text",
        "text": {
          "content": "Add a new to-do",
          "link": null
        },
        "annotations": {
          //...other keys excluded
        },
        "plain_text": "Add a new to-do",
        "href": null
      }
    ]
  }
}
```

### To do

To do block objects contain the following information within the to_do property:

| Field | Type | Description |
|-------|------|-------------|
| rich_text | array of rich text objects | The rich text displayed in the To do block. |
| checked | boolean (optional) | Whether the To do is checked. |
| color | string (enum) | The color of the block. Possible values are: "blue", "blue_background", "brown", "brown_background", "default", "gray", "gray_background", "green", "green_background", "orange", "orange_background", "yellow", "green", "pink", "pink_background", "purple", "purple_background", "red", "red_background", "yellow_background" |
| children | array of block objects | The nested child blocks, if any, of the To do block. |

**Example To do block object**

```json
{
  //...other keys excluded
  "type": "to_do",
  "to_do": {
    "rich_text": [{
      "type": "text",
      "text": {
        "content": "Finish Q3 goals",
        "link": null
      }
    }],
    "checked": false,
    "color": "default",
    "children":[{
      "type": "paragraph"
      // ..other keys excluded
    }]
  }
}
```

### Toggle blocks

Toggle block objects contain the following information within the toggle property:

| Field | Type | Description |
|-------|------|-------------|
| rich_text | array of rich text objects | The rich text displayed in the Toggle block. |
| color | string (enum) | The color of the block. Possible values are: "blue", "blue_background", "brown", "brown_background", "default", "gray", "gray_background", "green", "green_background", "orange", "orange_background", "yellow", "green", "pink", "pink_background", "purple", "purple_background", "red", "red_background", "yellow_background" |
| children | array of block objects | The nested child blocks, if any, of the Toggle block. |

**Toggle Block**

```json
{
  //...other keys excluded
  "type": "toggle",
  "toggle": {
    "rich_text": [{
      "type": "text",
      "text": {
        "content": "Additional project details",
        "link": null
      }
      //...other keys excluded
    }],
    "color": "default",
    "children":[{
      "type": "paragraph"
      // ..other keys excluded
    }]
  }
}
```

### Video

Video block objects contain a file object detailing information about the video.

**Example Video block object**

```json
{
  "type": "video",
  //...other keys excluded
  "video": {
    "type": "external",
    "external": {
      "url": "https://companywebsite.com/files/video.mp4"
    }
  }
}
```

**Supported video types**

- .amv
- .asf
- .avi
- .f4v
- .flv
- .gifv
- .mkv
- .mov
- .mpg
- .mpeg
- .mpv
- .mp4
- .m4v
- .qt
- .wmv
- YouTube video links that include embed or watch.
  E.g. https://www.youtube.com/watch?v=[id], https://www.youtube.com/embed/[id]

> 📘 Vimeo video links are not currently supported by the video block type. However, they can be embedded in Notion pages using the embed block type. See Embed for more information.

**Supported file upload types**

See the file upload reference for a list of supported file extensions and content types when attaching a File Upload to a block.

Video blocks only support file types in the "video" section of the table.