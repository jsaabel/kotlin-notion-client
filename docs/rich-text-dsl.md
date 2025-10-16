# Rich Text DSL

## Overview

The Rich Text DSL provides an intuitive Kotlin builder for creating formatted text with annotations, mentions, links, and equations.

## Basic Usage

```kotlin
richText {
    text("Hello, world!")
}
```

## Text Formatting

### Using the Standalone richText {} DSL

When using the standalone `richText {}` builder (for page properties or comments), you can use lambda syntax:

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

### Using Blocks DSL (paragraphs, headings, etc.)

When adding rich text inside blocks, use convenience methods directly:

```kotlin
paragraph {
    text("This text is ")
    bold("bold")
    text(", this is ")
    italic("italic")
    text(", and this is ")
    boldItalic("both")
    text(".")
}
```

## Colors

Apply text and background colors using the `Color` enum:

```kotlin
richText {
    text("This is ")
    colored("red text", Color.RED)
    text(" and ")
    colored("blue text", Color.BLUE)
}
```

Available colors: `DEFAULT`, `GRAY`, `BROWN`, `ORANGE`, `YELLOW`, `GREEN`, `BLUE`, `PURPLE`, `PINK`, `RED`

### Background Colors

```kotlin
richText {
    text("This has ")
    backgroundColored("yellow background", Color.YELLOW_BACKGROUND)
    text(" and ")
    backgroundColored("blue background", Color.BLUE_BACKGROUND)
}
```

Available background colors: `GRAY_BACKGROUND`, `BROWN_BACKGROUND`, `ORANGE_BACKGROUND`, `YELLOW_BACKGROUND`, `GREEN_BACKGROUND`, `BLUE_BACKGROUND`, `PURPLE_BACKGROUND`, `PINK_BACKGROUND`, `RED_BACKGROUND`

## Links

```kotlin
richText {
    text("Visit our ")
    link("documentation", "https://example.com/docs")
}
```

## Mentions

### User Mentions

Reference users in your workspace:

```kotlin
richText {
    text("Hey ")
    userMention("user-id-123")
    text(", can you review this?")
}
```

### Page Mentions

Link to other pages:

```kotlin
richText {
    text("See ")
    pageMention("page-id-456")
    text(" for details")
}
```

### Database Mentions

Reference databases:

```kotlin
richText {
    text("Check the ")
    databaseMention("database-id-789")
    text(" for all records")
}
```

### Date Mentions

Add date references using strings or typed dates:

**Using strings (ISO 8601 format):**

```kotlin
richText {
    text("Meeting on ")
    dateMention("2025-10-15")
    text(" at 2pm")
}
```

**Using LocalDate (kotlinx-datetime):**

```kotlin
import kotlinx.datetime.LocalDate

richText {
    text("Due ")
    dateMention(LocalDate(2025, 10, 15))
}

// Date ranges
richText {
    text("Project: ")
    dateMention(
        start = LocalDate(2025, 10, 15),
        end = LocalDate(2025, 10, 20)
    )
}
```

**Using LocalDateTime with timezone:**

```kotlin
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

richText {
    text("Meeting ")
    dateMention(
        start = LocalDateTime(2025, 10, 15, 14, 30),
        timeZone = TimeZone.of("America/New_York")
    )
}

// Datetime ranges
richText {
    text("Conference ")
    dateMention(
        start = LocalDateTime(2025, 10, 15, 9, 0),
        end = LocalDateTime(2025, 10, 17, 17, 0),
        timeZone = TimeZone.of("America/New_York")
    )
}
```

**Using Instant (timezone-unambiguous):**

```kotlin
import kotlinx.datetime.Instant

richText {
    text("Deployment at ")
    dateMention(Instant.parse("2025-10-15T14:30:00Z"))
}
```

## Equations

Add mathematical expressions using LaTeX syntax:

```kotlin
richText {
    text("The Pythagorean theorem: ")
    equation("x^2 + y^2 = z^2")
}
```

Complex equations:

```kotlin
richText {
    text("Einstein's famous equation: ")
    equation("E = mc^2")
    text(" or the quadratic formula: ")
    equation("x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}")
}
```

## Advanced Formatting

### Multiple Styles with formattedText

Apply multiple formatting options at once:

```kotlin
richText {
    text("This is ")
    formattedText("bold and italic", bold = true, italic = true)
    text(" and this is ")
    formattedText("code with color", code = true, color = Color.BLUE)
}
```

All formatting options:

```kotlin
richText {
    formattedText(
        "complex text",
        bold = true,
        italic = true,
        code = true,
        strikethrough = true,
        underline = true,
        color = Color.RED
    )
}
```

## Examples

### Formatted Text in a Page Property

Creating a page with rich text in the title:

```kotlin
val page = notion.pages.create {
    parent { dataSourceId("data-source-id") }

    properties {
        title("Name") {
            text("Project: ")
            bold("Q4 Goals")
            text(" - ")
            colored("High Priority", Color.RED)
        }

        richText("Description") {
            text("See ")
            pageMention("related-page-id")
            text(" for background")
        }
    }
}
```

### Complex Rich Text in a Block

Adding formatted content to a page:

```kotlin
notion.blocks.append("page-id") {
    paragraph {
        richText {
            text("This paragraph has ")
            bold("bold")
            text(", ")
            italic("italic")
            text(", and ")
            code("code")
            text(" formatting. ")
            link("https://example.com", "Click here")
            text(" for more info.")
        }
    }

    callout {
        icon { emoji = "⚠️" }
        richText {
            colored("Warning: ", Color.ORANGE)
            text("Please review ")
            pageMention("page-id-123")
            text(" by ")
            dateMention(LocalDate(2025, 10, 20))
        }
    }
}
```

### Mixed Content Example

Combining all rich text features:

```kotlin
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

richText {
    text("Team meeting ")
    dateMention(
        start = LocalDateTime(2025, 10, 15, 14, 0),
        timeZone = TimeZone.of("America/New_York")
    )
    text(" - ")
    userMention("user-id-123")
    text(" will present ")
    pageMention("presentation-page-id")
    text(". ")

    bold("Key topics:")
    text(" ")
    colored("Budget review", Color.RED)
    text(", ")
    colored("Timeline updates", Color.ORANGE)
    text(", and ")
    colored("Next steps", Color.GREEN)
    text(". ")

    text("Formula: ")
    equation("\\text{ROI} = \\frac{\\text{Gain} - \\text{Cost}}{\\text{Cost}} \\times 100\\%")
    text(" ")

    link("https://example.com/docs", "Full documentation")
}
```

## Common Patterns

### Method Chaining

All rich text methods return the builder for fluent chaining:

```kotlin
richText {
    text("Start ")
        .bold("chain")
        .text(" middle ")
        .italic("more")
        .text(" end")
}
```

### Convenience Methods

Use specialized methods for common combinations:

```kotlin
richText {
    boldItalic("Bold and italic together")
    // Instead of: formattedText("text", bold = true, italic = true)
}
```

Available convenience methods:
- `bold(text)` - Bold text
- `italic(text)` - Italic text
- `boldItalic(text)` - Both bold and italic
- `code(text)` - Inline code
- `strikethrough(text)` - Strikethrough text
- `underline(text)` - Underlined text

### Best Practices

1. **Use text() for plain segments** - More efficient than formattedText with defaults
2. **Batch similar formatting** - Group similarly formatted text together
3. **Keep it readable** - Break long rich text into multiple lines
4. **Notion populates mention names** - Don't worry about the display text for mentions, Notion fills it in

### Common Gotchas

1. **Empty rich text blocks** - Always include at least one text segment
   ```kotlin
   // ❌ Empty
   richText { }

   // ✅ At least one segment
   richText { text("") }
   ```

2. **Date format** - `dateMention` supports both strings and typed dates
   ```kotlin
   import kotlinx.datetime.LocalDate
   import kotlinx.datetime.LocalDateTime
   import kotlinx.datetime.TimeZone

   // ✅ Correct - using typed dates (recommended)
   dateMention(LocalDate(2025, 10, 15))
   dateMention(
       start = LocalDateTime(2025, 10, 15, 14, 30),
       timeZone = TimeZone.of("America/New_York")
   )

   // ✅ Also correct - using ISO 8601 strings
   dateMention("2025-10-15")
   dateMention("2025-10-15T14:30:00", timeZone = "America/New_York")

   // ❌ Incorrect format
   dateMention("10/15/2025")
   ```

3. **Background colors** - Use the `_BACKGROUND` suffix
   ```kotlin
   // ✅ Text color
   colored("text", Color.RED)

   // ✅ Background color
   backgroundColored("text", Color.RED_BACKGROUND)

   // ❌ Wrong - will color text, not background
   colored("text", Color.RED_BACKGROUND)
   ```

4. **Link display text** - Second parameter is the display text
   ```kotlin
   // Display URL
   link("https://example.com")

   // Custom display text
   link("https://example.com", "Click here")
   ```

## Related APIs

- **[Pages](pages.md)** - Rich text in page properties
- **[Blocks](blocks.md)** - Rich text in block content
- **[Comments](comments.md)** - Rich text in comments
