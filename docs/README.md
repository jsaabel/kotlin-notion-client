# Documentation Index

> **Note**: These guides reflect the library's current implementation but may not always be up to date with the latest Notion API changes. The [official Notion developer documentation](https://developers.notion.com/) is always the authoritative source for API behaviour and field definitions.

## API Guides

### Core APIs
- **[Pages](pages.md)** - Create, retrieve, and update pages
- **[Data Sources](data-sources.md)** - Query and manage data sources (tables)
- **[Databases](databases.md)** - Manage database containers
- **[Blocks](blocks.md)** - Work with content blocks

### Additional APIs
- **[Search](search.md)** - Search across your Notion workspace
- **[Users](users.md)** - Retrieve user information
- **[Comments](comments.md)** - Add and retrieve comments
- **[Markdown API](markdown-api.md)** - Create and update page content with markdown
- **[Views API](views-api.md)** - Manage database views
- **[Custom Emojis](custom-emojis.md)** - List custom emojis and native icons

### Features
- **[Pagination](pagination.md)** - Handle paginated results efficiently
- **[Rich Text DSL](rich-text-dsl.md)** - Format text with mentions, links, and more
- **[File Uploads](file-uploads.md)** - Upload and manage files and images
- **[Error Handling](error-handling.md)** - Handle API errors gracefully
- **[Testing](testing.md)** - Test your Notion integrations

## Quick Links

- [Main README](../README.md)
- [Quick Start Guide](../QUICKSTART.md)

## Understanding the 2026-03-11 API

This library implements Notion API version 2026-03-11. A key concept introduced in 2025-09-03 and carried forward:

- **Databases** are containers that hold one or more **data sources**
- **Data sources** are the actual tables with properties and rows (pages)
- Most operations work on data sources, not databases

See [Databases](databases.md) and [Data Sources](data-sources.md) for details.
