# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the Notion Kotlin Client project.

## Project Overview

A Kotlin-based client library for the Notion API, designed to provide a type-safe, idiomatic Kotlin interface for 
interacting with Notion's REST API. This project aims to match and exceed the functionality of the existing Python 
implementation while leveraging Kotlin's strong typing and coroutines for better performance and developer experience.
Also an opportunity to explore more heavily AI-assisted development and learning about common architectural patterns
and best practices in Kotlin.

## Development Philosophy

### Core Principles
1. **Type Safety First**: Leverage Kotlin's type system to prevent runtime errors
2. **Coroutine-Based**: All API calls should be suspend functions for non-blocking I/O
3. **Builder Pattern**: Use DSL-style builders for complex objects (pages, blocks, databases)
4. **Fail-Fast with Clear Errors**: Validate inputs early and provide helpful error messages
5. **Incremental Development**: Build features in small, testable chunks

### Implementation Strategy
- Start with read operations (simpler, no side effects)
- Add write operations once read is stable
- Implement rate limiting and retry logic early
- Design for testability with interface-based architecture

## Development Tips

### When Adding New Features
1. Study the Python implementation in `/reference/python/`
2. Review both Kotlin implementations for patterns
3. Check Notion API docs for endpoint details
4. Write tests first (TDD approach)
5. Implement incrementally with refactoring

### Common Pitfalls
- Notion IDs can be UUIDs with or without hyphens
- Some properties are read-only from the API
- Rate limits vary by endpoint
- Archived objects need special handling
- Rich text arrays can be empty

### Debugging
- Enable Ktor logging for HTTP details
- Use `.also { println(it) }` for quick debugging
- Check `X-Notion-Request-Id` header for support
- Validate JSON payloads against API examples

### Development Workflow Reminder
- Always lint/format code using gradlew before trying to build

### Version Management
- Use `libs.toml` for centralized dependency version management
- Leverage Gradle's version catalog to handle library versions consistently
- Ensure version numbers are updated in the `libs.toml` file for all project dependencies

## Commit Guidelines

### Commit Message Style
- We use "conventional commits" for our git commits.

## Reference Materials

### Notion API Documentation
- Comprehensive information and documentation about the Notion API is located under `@reference/notion-api/`
- Always refer to these files before relying on other mechanisms to retrieve information about the Notion API
- Sample responses from the official API documentation are available under `@reference/notion-api/sample_responses/` and its subfolders
- These sample responses can and should be used for mock responses and in other cases where appropriate