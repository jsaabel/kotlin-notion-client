# Kotlin Notion Client - Interactive Notebooks

Interactive Jupyter notebooks for exploring the Kotlin Notion Client library. These provide hands-on examples and serve as an alternative way to understand and test the library's capabilities.

## Setup

### Environment Variables

Set these before running the notebooks:

```bash
export NOTION_API_TOKEN="secret_your_token_here"
export NOTION_TEST_PAGE_ID="12345678-1234-1234-1234-123456789abc"
export NOTION_TEST_DATABASE_ID="87654321-4321-4321-4321-cba987654321"
```

### Notion Integration Setup

1. Create an integration at [https://www.notion.so/my-integrations](https://www.notion.so/my-integrations)
2. Copy the "Internal Integration Token"
3. Create test page(s) and database(s) in your Notion workspace
4. Share them with your integration (Share → Invite → select your integration)

## Notebooks

### [01-getting-started.ipynb](./01-getting-started.ipynb)
Introduction to the library: authentication, retrieving pages, basic error handling.

**Prerequisites**: `NOTION_API_TOKEN`, `NOTION_TEST_PAGE_ID`

---

### [02-reading-databases.ipynb](./02-reading-databases.ipynb)
Working with databases and data sources: querying, filtering, sorting.

**Prerequisites**: `NOTION_API_TOKEN`, `NOTION_TEST_DATABASE_ID`

---

### [03-creating-pages.ipynb](./03-creating-pages.ipynb)
Creating pages programmatically with various property types using the DSL.

**Prerequisites**: `NOTION_API_TOKEN`, `NOTION_TEST_DATABASE_ID`

---

### [04-working-with-blocks.ipynb](./04-working-with-blocks.ipynb)
Building page content: block hierarchy, nesting, different block types.

**Prerequisites**: `NOTION_API_TOKEN`, `NOTION_TEST_PAGE_ID`

---

### [05-rich-text-dsl.ipynb](./05-rich-text-dsl.ipynb)
Text formatting with the Rich Text DSL: annotations, links, mentions, equations.

**Prerequisites**: `NOTION_API_TOKEN`, `NOTION_TEST_PAGE_ID`

---

### [06-advanced-queries.ipynb](./06-advanced-queries.ipynb)
Advanced querying: complex nested filters, multiple sorts, pagination.

**Prerequisites**: `NOTION_API_TOKEN`, `NOTION_TEST_DATABASE_ID`

## Notes

- All notebooks use `runBlocking { }` to wrap suspend functions
- Notebooks are designed to be modified and experimented with
- Use a test workspace, not your production Notion workspace

## Resources

- [Main Documentation](../docs/README.md)
- [QUICKSTART Guide](../QUICKSTART.md)
- [Notion API Documentation](https://developers.notion.com/)