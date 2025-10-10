# Documentation Index

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

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

### Features
- **[Pagination](pagination.md)** - Handle paginated results efficiently
- **[Rich Text DSL](rich-text-dsl.md)** - Format text with mentions, links, and more
- **[File Uploads](file-uploads.md)** - Upload and manage files and images
- **[Error Handling](error-handling.md)** - Handle API errors gracefully
- **[Testing](testing.md)** - Test your Notion integrations

## Quick Links

- [Main README](../README.md)
- [Quick Start Guide](../QUICKSTART.md)

## Understanding the 2025-09-03 API

This library implements Notion API version 2025-09-03, which introduced important changes:

- **Databases** are now containers that hold one or more **data sources**
- **Data sources** are the actual tables with properties and rows (pages)
- Most operations work on data sources, not databases

See [Databases](databases.md) and [Data Sources](data-sources.md) for details.
