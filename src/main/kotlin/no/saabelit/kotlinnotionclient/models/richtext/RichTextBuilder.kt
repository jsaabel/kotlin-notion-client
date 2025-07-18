@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.richtext

import no.saabelit.kotlinnotionclient.models.base.Annotations
import no.saabelit.kotlinnotionclient.models.base.Color
import no.saabelit.kotlinnotionclient.models.base.Equation
import no.saabelit.kotlinnotionclient.models.base.Link
import no.saabelit.kotlinnotionclient.models.base.Mention
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.base.TextContent
import no.saabelit.kotlinnotionclient.models.users.User

/**
 * DSL marker to prevent nested scopes in rich text builders.
 */
@DslMarker
annotation class RichTextDslMarker

/**
 * Builder class for creating rich text arrays with mixed formatting.
 *
 * This builder provides a fluent DSL for creating rich text content with different
 * formatting styles within a single paragraph or block. It dramatically reduces
 * boilerplate compared to manual RichText construction.
 *
 * ## Example Usage:
 * ```kotlin
 * val richText = richText {
 *     text("Hello ")
 *     bold("world")
 *     text("! This is ")
 *     italic("italic")
 *     text(" and this is ")
 *     code("code")
 *     text(".")
 * }
 * ```
 *
 * ## Advanced Usage:
 * ```kotlin
 * val richText = richText {
 *     text("Visit ")
 *     link("https://notion.so", "Notion")
 *     text(" for more info. ")
 *     colored("This is red", Color.RED)
 *     text(" and ")
 *     backgroundColored("this has red background", Color.RED_BACKGROUND)
 * }
 * ```
 *
 * ## Using formattedText for Complex Styling:
 * ```kotlin
 * val richText = richText {
 *     text("This text is ")
 *     formattedText("bold and italic", bold = true, italic = true)
 *     text(" and this is ")
 *     formattedText("code with color", code = true, color = Color.BLUE)
 * }
 * ```
 *
 * ## Integration with PageContentBuilder:
 * ```kotlin
 * paragraph {
 *     text("This paragraph has ")
 *     bold("mixed formatting")
 *     text(" including ")
 *     italic("italic")
 *     text(" and ")
 *     code("code")
 *     text(".")
 * }
 * ```
 */
@RichTextDslMarker
class RichTextBuilder {
    private val segments = mutableListOf<RichText>()

    /**
     * Adds plain text with no formatting.
     *
     * @param content The text content
     * @return This builder for chaining
     */
    fun text(content: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations = Annotations(),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds bold text.
     *
     * @param content The text content
     * @return This builder for chaining
     */
    fun bold(content: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations = Annotations(bold = true),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds italic text.
     *
     * @param content The text content
     * @return This builder for chaining
     */
    fun italic(content: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations = Annotations(italic = true),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds bold and italic text.
     *
     * @param content The text content
     * @return This builder for chaining
     */
    fun boldItalic(content: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations = Annotations(bold = true, italic = true),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds code-formatted text.
     *
     * @param content The text content
     * @return This builder for chaining
     */
    fun code(content: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations = Annotations(code = true),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds strikethrough text.
     *
     * @param content The text content
     * @return This builder for chaining
     */
    fun strikethrough(content: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations = Annotations(strikethrough = true),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds underlined text.
     *
     * @param content The text content
     * @return This builder for chaining
     */
    fun underline(content: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations = Annotations(underline = true),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds colored text.
     *
     * @param content The text content
     * @param color The text color
     * @return This builder for chaining
     */
    fun colored(
        content: String,
        color: Color,
    ): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations = Annotations(color = color),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds text with background color.
     *
     * @param content The text content
     * @param color The background color
     * @return This builder for chaining
     */
    fun backgroundColored(
        content: String,
        color: Color,
    ): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations = Annotations(color = color),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds a link with the URL as the display text.
     *
     * @param url The URL to link to
     * @return This builder for chaining
     */
    fun link(url: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = url, link = Link(url = url)),
                annotations = Annotations(),
                plainText = url,
                href = url,
            ),
        )
        return this
    }

    /**
     * Adds a link with custom display text.
     *
     * @param url The URL to link to
     * @param text The text to display for the link
     * @return This builder for chaining
     */
    fun link(
        url: String,
        text: String,
    ): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = text, link = Link(url = url)),
                annotations = Annotations(),
                plainText = text,
                href = url,
            ),
        )
        return this
    }

    /**
     * Adds a user mention.
     *
     * @param userId The ID of the user to mention
     * @return This builder for chaining
     */
    fun userMention(userId: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "mention",
                text = null,
                mention = Mention.User(User(objectType = "user", id = userId, name = null, avatarUrl = null, type = null, bot = null)),
                annotations = Annotations(),
                plainText = "@Unknown User", // Notion will populate this
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds an equation.
     *
     * @param expression The LaTeX expression
     * @return This builder for chaining
     */
    fun equation(expression: String): RichTextBuilder {
        segments.add(
            RichText(
                type = "equation",
                text = null,
                equation = Equation(expression = expression),
                annotations = Annotations(),
                plainText = expression,
                href = null,
            ),
        )
        return this
    }

    /**
     * Adds formatted text with multiple styling options in a single call.
     *
     * This method provides a convenient way to apply multiple formatting styles
     * without needing separate method calls for each style.
     *
     * @param content The text content
     * @param bold Whether to apply bold formatting
     * @param italic Whether to apply italic formatting
     * @param code Whether to apply code formatting
     * @param strikethrough Whether to apply strikethrough formatting
     * @param underline Whether to apply underline formatting
     * @param color The text color to apply
     * @return This builder for chaining
     */
    fun formattedText(
        content: String,
        bold: Boolean = false,
        italic: Boolean = false,
        code: Boolean = false,
        strikethrough: Boolean = false,
        underline: Boolean = false,
        color: Color = Color.DEFAULT,
    ): RichTextBuilder {
        segments.add(
            RichText(
                type = "text",
                text = TextContent(content = content, link = null),
                annotations =
                    Annotations(
                        bold = bold,
                        italic = italic,
                        code = code,
                        strikethrough = strikethrough,
                        underline = underline,
                        color = color,
                    ),
                plainText = content,
                href = null,
            ),
        )
        return this
    }

    /**
     * Builds the immutable list of rich text segments.
     *
     * @return List of rich text objects
     */
    internal fun build(): List<RichText> = segments.toList()
}

/**
 * Entry point function for the rich text DSL.
 *
 * Creates a list of RichText objects with mixed formatting using a fluent DSL.
 *
 * @param block The DSL block for building rich text
 * @return List of RichText objects
 */
fun richText(block: RichTextBuilder.() -> Unit): List<RichText> = RichTextBuilder().apply(block).build()
