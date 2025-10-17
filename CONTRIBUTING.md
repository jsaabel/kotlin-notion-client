# Contributing to Kotlin Notion Client

Thank you for your interest in contributing to the Kotlin Notion Client! This document provides guidelines and information for contributors.

## Project Philosophy

This library aims to provide a modern, type-safe, idiomatic Kotlin interface for the Notion API. Contributions should align with these principles:

- **Type Safety First**: Leverage Kotlin's type system to prevent errors
- **Kotlin Idioms**: Follow Kotlin best practices and conventions
- **DSL-Style APIs**: Prefer builder patterns for complex objects
- **Clear Documentation**: Code should be self-documenting with helpful KDoc
- **Comprehensive Testing**: All features should have thorough test coverage

## Development Context

This project was developed with significant AI assistance (Claude Code). This is disclosed openly in the README. We welcome contributions from all developers, whether human-written, AI-assisted, or collaborative approaches.

## How to Contribute

### Reporting Issues

- **Bug Reports**: Use GitHub Issues with clear reproduction steps
- **Feature Requests**: Explain the use case and how it aligns with project goals
- **Documentation Issues**: Point out unclear or incorrect documentation

### Code Contributions

1. **Fork and Clone**
   ```bash
   git fork https://github.com/jsaabel/kotlin-notion-client.git
   git clone https://github.com/YOUR_USERNAME/kotlin-notion-client.git
   cd kotlin-notion-client
   ```

2. **Create a Branch**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```

3. **Make Changes**
   - Follow existing code style (Kotlinter enforces formatting)
   - Add tests for new functionality
   - Update documentation as needed
   - Keep commits focused and atomic

4. **Test Your Changes**
   ```bash
   # Format code
   ./gradlew formatKotlin

   # Run unit tests
   ./gradlew test -Dkotest.tags.include="Unit"

   # Build project
   ./gradlew build
   ```

5. **Commit**
   - Use [Conventional Commits](https://www.conventionalcommits.org/) format
   - Examples:
     - `feat: add support for database templates`
     - `fix: resolve null pointer in rich text parsing`
     - `docs: update examples in pages.md`
     - `test: add unit tests for filter DSL`

6. **Push and Create PR**
   ```bash
   git push origin feature/your-feature-name
   ```
   Then open a Pull Request on GitHub with:
   - Clear description of changes
   - Link to related issues
   - Test results if applicable

## Development Setup

### Prerequisites
- JDK 17 or higher
- Gradle (wrapper included)
- Git

### Building from Source
```bash
./gradlew build
```

### Running Tests
```bash
# Unit tests only (fast, no API calls)
./gradlew test -Dkotest.tags.include="Unit"

# Integration tests (requires NOTION_API_TOKEN)
export NOTION_RUN_INTEGRATION_TESTS=true
export NOTION_API_TOKEN="secret_..."
export NOTION_TEST_PAGE_ID="page-id"
./gradlew test --tests "*YourIntegrationTest"
```

**Important**: Do not run all integration tests at once - they make many real API calls.

### Code Style

The project uses [Kotlinter](https://github.com/jeremymailen/kotlinter-gradle) for code formatting:

```bash
# Format all code
./gradlew formatKotlin

# Check formatting
./gradlew lintKotlin
```

Always run `formatKotlin` before committing.

## Testing Guidelines

### Unit Tests
- Use MockResponseBuilder for HTTP mocking
- Use TestFixtures for official API sample responses
- Tag with `@Tags("Unit")`
- Should run in <1 second
- Example:
  ```kotlin
  @Tags("Unit")
  class MyFeatureTest : FunSpec({
      test("should parse official API response") {
          val response = TestFixtures.Pages.retrievePage()
          val page = response.decode<Page>()
          page.id shouldBe "expected-id"
      }
  })
  ```

### Integration Tests
- Tag with `@Tags("Integration", "RequiresApi")`
- Check for environment variables before running
- Clean up resources after test
- Example:
  ```kotlin
  @Tags("Integration", "RequiresApi")
  class MyIntegrationTest : FunSpec({
      test("should create page via API").config(enabledIf = shouldRunIntegrationTests()) {
          // Test implementation
      }
  })
  ```

## Documentation

### Code Documentation
- Public APIs should have KDoc comments
- Include `@param`, `@return`, `@throws` as appropriate
- Provide usage examples in KDoc

### Guides
- Update relevant docs in `docs/` directory
- Keep examples accurate and tested
- Use clear, beginner-friendly language

## What We're Looking For

**High Priority**:
- Bug fixes with test cases
- Improved error messages
- Documentation improvements
- Real-world usage feedback
- Performance optimizations

**Medium Priority**:
- New features aligned with Notion API capabilities
- Additional test coverage
- Code quality improvements

**Low Priority**:
- Major architectural changes (discuss first)
- Dependencies updates (unless fixing a specific issue)

## Questions?

- Check [CLAUDE.md](CLAUDE.md) for project guidelines
- Review existing code for patterns
- Open a discussion issue before major changes
- Reach out via GitHub Issues for questions

## Code of Conduct

Be respectful, constructive, and collaborative. We're all here to build better software.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to Kotlin Notion Client!