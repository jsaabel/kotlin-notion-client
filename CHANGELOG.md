# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2025-10-10

### 🎉 Initial Release

This is the first public release of the Kotlin Notion Client library.

#### ✨ Features

**Core API Support**
- Complete implementation of Notion API 2025-09-03
- Full support for Pages, Blocks, Databases, Data Sources, Comments, Search, and Users APIs
- Type-safe Kotlin models for all Notion objects
- Coroutine-based async API using suspend functions

**Developer Experience**
- Type-safe DSL builders for creating pages, databases, and queries
- Rich Text DSL for formatting text with annotations
- Pagination helpers with Kotlin Flow support
- Rate limiting with automatic retry logic
- Comprehensive error handling

**Data Sources & Databases**
- Complete CRUD operations for databases and data sources
- Advanced query capabilities with type-safe filter and sort builders
- Relation properties with pagination support
- All database property types supported

**Content Management**
- Full block type support (paragraph, heading, list, code, etc.)
- Block children operations (append, retrieve, delete)
- Table blocks with row and cell management
- File upload support (single and multipart)

**Search & Users**
- Search by title with filtering
- User retrieval and listing
- Bot user information

**Testing & Quality**
- 481+ unit tests with comprehensive coverage
- Integration tests for real API verification
- Test fixtures using official Notion API samples

#### 📚 Documentation

- Complete API documentation for all endpoints
- Usage examples for common operations
- Testing guide with unit and integration test patterns
- Error handling guide
- Rich Text DSL documentation
- Pagination helpers documentation

#### 🔧 Technical Details

- **Language**: Kotlin 1.9+
- **HTTP Client**: Ktor
- **Serialization**: kotlinx.serialization
- **DateTime**: kotlinx-datetime
- **Testing**: Kotest

#### ⚠️ Known Limitations

- This library was developed with significant AI assistance (Claude Code)
- Some edge cases may not be fully covered
- Documentation examples should be verified against actual implementation
- See README for full transparency notice

#### 🙏 Acknowledgments

- Built using official Notion API documentation
- Developed with Claude Code assistance
- Inspired by official Notion SDK implementations

---

**Note**: This is an early release (0.1.0). While comprehensive testing has been performed, users should expect potential issues and are encouraged to report them via GitHub Issues.

[0.1.0]: https://github.com/yourusername/kotlin-notion-client/releases/tag/v0.1.0