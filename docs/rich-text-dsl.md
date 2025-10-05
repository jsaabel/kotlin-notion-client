# Rich Text DSL

> **⚠️ WORK IN PROGRESS**: This documentation is being actively developed and may be incomplete or subject to change.

## Overview

The Rich Text DSL provides an intuitive Kotlin builder for creating formatted text with annotations, mentions, links, and equations.

## Basic Usage

```kotlin
richText {
    text("Hello, world!")
}
```

## Text Formatting

```kotlin
richText {
    text("This is bold") {
        bold = true
    }

    text(" and this is ")

    text("italic") {
        italic = true
    }

    text(" and this is ")

    text("both") {
        bold = true
        italic = true
    }
}
```

## Colors

_TODO: Add color examples_

## Links

```kotlin
richText {
    text("Visit our ")
    link("documentation", "https://example.com/docs")
}
```

## Mentions

_TODO: Add mention examples (users, pages, dates)_

## Equations

_TODO: Add equation examples_

## Examples

_TODO: Add comprehensive examples_

### Formatted Text in a Page Property

```kotlin
// TODO: Add example
```

### Complex Rich Text in a Block

```kotlin
// TODO: Add example
```

## Common Patterns

_TODO: Add tips, gotchas, best practices_

## Related APIs

- **[Pages](pages.md)** - Rich text in page properties
- **[Blocks](blocks.md)** - Rich text in block content
- **[Comments](comments.md)** - Rich text in comments
