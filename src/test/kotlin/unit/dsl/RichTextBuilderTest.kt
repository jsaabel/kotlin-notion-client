package unit.dsl

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.base.Annotations
import it.saabel.kotlinnotionclient.models.base.Color
import it.saabel.kotlinnotionclient.models.base.DateObject
import it.saabel.kotlinnotionclient.models.base.Equation
import it.saabel.kotlinnotionclient.models.base.Link
import it.saabel.kotlinnotionclient.models.base.Mention
import it.saabel.kotlinnotionclient.models.richtext.richText
import it.saabel.kotlinnotionclient.models.users.User
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class RichTextBuilderTest :
    FunSpec({

        test("richText DSL should create simple text") {
            val result =
                richText {
                    text("Hello world")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "Hello world"
            result[0].plainText shouldBe "Hello world"
            result[0].annotations shouldBe Annotations()
            result[0].href shouldBe null
        }

        test("richText DSL should create bold text") {
            val result =
                richText {
                    bold("Bold text")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "Bold text"
            result[0].plainText shouldBe "Bold text"
            result[0].annotations shouldBe Annotations(bold = true)
            result[0].href shouldBe null
        }

        test("richText DSL should create italic text") {
            val result =
                richText {
                    italic("Italic text")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "Italic text"
            result[0].plainText shouldBe "Italic text"
            result[0].annotations shouldBe Annotations(italic = true)
            result[0].href shouldBe null
        }

        test("richText DSL should create bold italic text") {
            val result =
                richText {
                    boldItalic("Bold italic text")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "Bold italic text"
            result[0].plainText shouldBe "Bold italic text"
            result[0].annotations shouldBe Annotations(bold = true, italic = true)
            result[0].href shouldBe null
        }

        test("richText DSL should create code text") {
            val result =
                richText {
                    code("code text")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "code text"
            result[0].plainText shouldBe "code text"
            result[0].annotations shouldBe Annotations(code = true)
            result[0].href shouldBe null
        }

        test("richText DSL should create strikethrough text") {
            val result =
                richText {
                    strikethrough("strikethrough text")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "strikethrough text"
            result[0].plainText shouldBe "strikethrough text"
            result[0].annotations shouldBe Annotations(strikethrough = true)
            result[0].href shouldBe null
        }

        test("richText DSL should create underlined text") {
            val result =
                richText {
                    underline("underlined text")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "underlined text"
            result[0].plainText shouldBe "underlined text"
            result[0].annotations shouldBe Annotations(underline = true)
            result[0].href shouldBe null
        }

        test("richText DSL should create colored text") {
            val result =
                richText {
                    colored("red text", Color.RED)
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "red text"
            result[0].plainText shouldBe "red text"
            result[0].annotations shouldBe Annotations(color = Color.RED)
            result[0].href shouldBe null
        }

        test("richText DSL should create background colored text") {
            val result =
                richText {
                    backgroundColored("background text", Color.BLUE_BACKGROUND)
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "background text"
            result[0].plainText shouldBe "background text"
            result[0].annotations shouldBe Annotations(color = Color.BLUE_BACKGROUND)
            result[0].href shouldBe null
        }

        test("richText DSL should create link with URL as display text") {
            val result =
                richText {
                    link("https://example.com")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "https://example.com"
            result[0].text?.link shouldBe Link(url = "https://example.com")
            result[0].plainText shouldBe "https://example.com"
            result[0].annotations shouldBe Annotations()
            result[0].href shouldBe "https://example.com"
        }

        test("richText DSL should create link with custom display text") {
            val result =
                richText {
                    link("https://example.com", "Example Site")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "Example Site"
            result[0].text?.link shouldBe Link(url = "https://example.com")
            result[0].plainText shouldBe "Example Site"
            result[0].annotations shouldBe Annotations()
            result[0].href shouldBe "https://example.com"
        }

        test("richText DSL should create user mention") {
            val result =
                richText {
                    userMention("user-123")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "mention"
            result[0].text shouldBe null
            result[0].mention shouldBe
                Mention.User(
                    User(
                        objectType = "user",
                        id = "user-123",
                        name = null,
                        avatarUrl = null,
                        type = null,
                        bot = null,
                    ),
                )
            result[0].plainText shouldBe "@Unknown User"
            result[0].annotations shouldBe Annotations()
            result[0].href shouldBe null
        }

        test("richText DSL should create equation") {
            val result =
                richText {
                    equation("x^2 + y^2 = z^2")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "equation"
            result[0].text shouldBe null
            result[0].equation shouldBe Equation(expression = "x^2 + y^2 = z^2")
            result[0].plainText shouldBe "x^2 + y^2 = z^2"
            result[0].annotations shouldBe Annotations()
            result[0].href shouldBe null
        }

        test("richText DSL should create mixed formatting") {
            val result =
                richText {
                    text("Hello ")
                    bold("bold")
                    text(" and ")
                    italic("italic")
                    text(" and ")
                    code("code")
                    text("!")
                }

            result shouldHaveSize 7

            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "Hello "
            result[0].annotations shouldBe Annotations()

            result[1].type shouldBe "text"
            result[1].text?.content shouldBe "bold"
            result[1].annotations shouldBe Annotations(bold = true)

            result[2].type shouldBe "text"
            result[2].text?.content shouldBe " and "
            result[2].annotations shouldBe Annotations()

            result[3].type shouldBe "text"
            result[3].text?.content shouldBe "italic"
            result[3].annotations shouldBe Annotations(italic = true)

            result[4].type shouldBe "text"
            result[4].text?.content shouldBe " and "
            result[4].annotations shouldBe Annotations()

            result[5].type shouldBe "text"
            result[5].text?.content shouldBe "code"
            result[5].annotations shouldBe Annotations(code = true)

            result[6].type shouldBe "text"
            result[6].text?.content shouldBe "!"
            result[6].annotations shouldBe Annotations()
        }

        test("richText DSL should create complex mixed formatting with links and colors") {
            val result =
                richText {
                    text("Visit ")
                    link("https://notion.so", "Notion")
                    text(" for more info. ")
                    colored("This is red", Color.RED)
                    text(" and ")
                    backgroundColored("this has blue background", Color.BLUE_BACKGROUND)
                    text(".")
                }

            result shouldHaveSize 7

            result[0].plainText shouldBe "Visit "
            result[0].annotations shouldBe Annotations()

            result[1].plainText shouldBe "Notion"
            result[1].href shouldBe "https://notion.so"
            result[1].text?.link shouldBe Link(url = "https://notion.so")

            result[2].plainText shouldBe " for more info. "
            result[2].annotations shouldBe Annotations()

            result[3].plainText shouldBe "This is red"
            result[3].annotations shouldBe Annotations(color = Color.RED)

            result[4].plainText shouldBe " and "
            result[4].annotations shouldBe Annotations()

            result[5].plainText shouldBe "this has blue background"
            result[5].annotations shouldBe Annotations(color = Color.BLUE_BACKGROUND)

            result[6].plainText shouldBe "."
            result[6].annotations shouldBe Annotations()
        }

        test("richText DSL should handle empty blocks") {
            val result =
                richText {
                    // Empty block
                }

            result shouldHaveSize 0
        }

        test("richText DSL should handle method chaining") {
            val result =
                richText {
                    text("Start ")
                        .bold("chain")
                        .text(" end")
                }

            result shouldHaveSize 3
            result[0].plainText shouldBe "Start "
            result[1].plainText shouldBe "chain"
            result[1].annotations.bold shouldBe true
            result[2].plainText shouldBe " end"
        }

        test("richText DSL should create text with all formatting options") {
            val result =
                richText {
                    text("normal ")
                    bold("bold ")
                    italic("italic ")
                    boldItalic("bold-italic ")
                    code("code ")
                    strikethrough("strikethrough ")
                    underline("underline ")
                    colored("colored ", Color.GREEN)
                    backgroundColored("background ", Color.YELLOW_BACKGROUND)
                    link("https://example.com", "link ")
                    userMention("user-123")
                    text(" ")
                    equation("E=mc^2")
                }

            result shouldHaveSize 13

            // Verify each type exists
            result.count { it.type == "text" } shouldBe 11
            result.count { it.type == "mention" } shouldBe 1
            result.count { it.type == "equation" } shouldBe 1

            // Verify specific formatting
            result.any { it.annotations.bold && !it.annotations.italic } shouldBe true
            result.any { it.annotations.italic && !it.annotations.bold } shouldBe true
            result.any { it.annotations.bold && it.annotations.italic } shouldBe true
            result.any { it.annotations.code } shouldBe true
            result.any { it.annotations.strikethrough } shouldBe true
            result.any { it.annotations.underline } shouldBe true
            result.any { it.annotations.color == Color.GREEN } shouldBe true
            result.any { it.annotations.color == Color.YELLOW_BACKGROUND } shouldBe true
            result.any { it.href != null } shouldBe true
        }

        test("formattedText should create text with multiple formatting options") {
            val result =
                richText {
                    formattedText("bold and italic", bold = true, italic = true)
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "bold and italic"
            result[0].plainText shouldBe "bold and italic"
            result[0].annotations shouldBe Annotations(bold = true, italic = true)
            result[0].href shouldBe null
        }

        test("formattedText should create text with all formatting options") {
            val result =
                richText {
                    formattedText(
                        "complex formatting",
                        bold = true,
                        italic = true,
                        code = true,
                        strikethrough = true,
                        underline = true,
                        color = Color.RED,
                    )
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "complex formatting"
            result[0].plainText shouldBe "complex formatting"
            result[0].annotations shouldBe
                Annotations(
                    bold = true,
                    italic = true,
                    code = true,
                    strikethrough = true,
                    underline = true,
                    color = Color.RED,
                )
            result[0].href shouldBe null
        }

        test("formattedText should create text with only color") {
            val result =
                richText {
                    formattedText("just colored", color = Color.BLUE)
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "just colored"
            result[0].plainText shouldBe "just colored"
            result[0].annotations shouldBe Annotations(color = Color.BLUE)
            result[0].href shouldBe null
        }

        test("formattedText should create text with default formatting when no options provided") {
            val result =
                richText {
                    formattedText("default text")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "text"
            result[0].text?.content shouldBe "default text"
            result[0].plainText shouldBe "default text"
            result[0].annotations shouldBe Annotations()
            result[0].href shouldBe null
        }

        test("formattedText should work in mixed formatting scenarios") {
            val result =
                richText {
                    text("Start ")
                    formattedText("bold red", bold = true, color = Color.RED)
                    text(" middle ")
                    formattedText("italic blue", italic = true, color = Color.BLUE)
                    text(" end")
                }

            result shouldHaveSize 5

            result[0].plainText shouldBe "Start "
            result[0].annotations shouldBe Annotations()

            result[1].plainText shouldBe "bold red"
            result[1].annotations shouldBe Annotations(bold = true, color = Color.RED)

            result[2].plainText shouldBe " middle "
            result[2].annotations shouldBe Annotations()

            result[3].plainText shouldBe "italic blue"
            result[3].annotations shouldBe Annotations(italic = true, color = Color.BLUE)

            result[4].plainText shouldBe " end"
            result[4].annotations shouldBe Annotations()
        }

        test("formattedText should support method chaining") {
            val result =
                richText {
                    text("Start ")
                        .formattedText("chain", bold = true)
                        .text(" end")
                }

            result shouldHaveSize 3
            result[0].plainText shouldBe "Start "
            result[1].plainText shouldBe "chain"
            result[1].annotations.bold shouldBe true
            result[2].plainText shouldBe " end"
        }

        test("dateMention should create date mention using LocalDate") {
            val result =
                richText {
                    dateMention(LocalDate(2025, 10, 15))
                }

            result shouldHaveSize 1
            result[0].type shouldBe "mention"
            result[0].text shouldBe null
            val dateMention = result[0].mention as? Mention.Date
            dateMention?.date?.start shouldBe "2025-10-15"
            dateMention?.date?.end shouldBe null
            dateMention?.date?.timeZone shouldBe null
            result[0].annotations shouldBe Annotations()
            result[0].href shouldBe null
        }

        test("dateMention should create date range using LocalDate") {
            val result =
                richText {
                    dateMention(
                        start = LocalDate(2025, 10, 15),
                        end = LocalDate(2025, 10, 20),
                    )
                }

            result shouldHaveSize 1
            result[0].type shouldBe "mention"
            val dateMention = result[0].mention as? Mention.Date
            dateMention?.date?.start shouldBe "2025-10-15"
            dateMention?.date?.end shouldBe "2025-10-20"
            dateMention?.date?.timeZone shouldBe null
        }

        test("dateMention should create datetime mention using LocalDateTime") {
            val result =
                richText {
                    dateMention(
                        start = LocalDateTime(2025, 10, 15, 14, 30),
                        timeZone = TimeZone.UTC,
                    )
                }

            result shouldHaveSize 1
            result[0].type shouldBe "mention"
            val dateMention = result[0].mention as? Mention.Date
            dateMention?.date?.start shouldContain "2025-10-15T14:30:00"
            dateMention?.date?.end shouldBe null
            dateMention?.date?.timeZone shouldBe "Z" // TimeZone.UTC.id returns "Z"
        }

        test("dateMention should create datetime range using LocalDateTime with timezone") {
            val result =
                richText {
                    dateMention(
                        start = LocalDateTime(2025, 10, 15, 9, 0),
                        end = LocalDateTime(2025, 10, 15, 17, 0),
                        timeZone = TimeZone.of("America/New_York"),
                    )
                }

            result shouldHaveSize 1
            result[0].type shouldBe "mention"
            val dateMention = result[0].mention as? Mention.Date
            dateMention?.date?.start shouldContain "2025-10-15"
            dateMention?.date?.end shouldContain "2025-10-15"
            dateMention?.date?.timeZone shouldBe "America/New_York"
        }

        test("dateMention should create instant mention using Instant") {
            val instant = Instant.parse("2025-10-15T14:30:00Z")
            val result =
                richText {
                    dateMention(instant)
                }

            result shouldHaveSize 1
            result[0].type shouldBe "mention"
            val dateMention = result[0].mention as? Mention.Date
            dateMention?.date?.start shouldBe "2025-10-15T14:30:00Z"
            dateMention?.date?.end shouldBe null
            dateMention?.date?.timeZone shouldBe null
        }

        test("dateMention should create instant range using Instant") {
            val start = Instant.parse("2025-10-15T14:00:00Z")
            val end = Instant.parse("2025-10-15T16:00:00Z")
            val result =
                richText {
                    dateMention(start = start, end = end)
                }

            result shouldHaveSize 1
            result[0].type shouldBe "mention"
            val dateMention = result[0].mention as? Mention.Date
            dateMention?.date?.start shouldBe "2025-10-15T14:00:00Z"
            dateMention?.date?.end shouldBe "2025-10-15T16:00:00Z"
            dateMention?.date?.timeZone shouldBe null
        }

        test("dateMention should create string-based date mention") {
            val result =
                richText {
                    dateMention("2025-10-15")
                }

            result shouldHaveSize 1
            result[0].type shouldBe "mention"
            val dateMention = result[0].mention as? Mention.Date
            dateMention?.date?.start shouldBe "2025-10-15"
            dateMention?.date?.end shouldBe null
            dateMention?.date?.timeZone shouldBe null
        }

        test("dateMention should create string-based datetime range with timezone") {
            val result =
                richText {
                    dateMention(
                        start = "2025-10-15T09:00:00",
                        end = "2025-10-15T17:00:00",
                        timeZone = "America/New_York",
                    )
                }

            result shouldHaveSize 1
            result[0].type shouldBe "mention"
            val dateMention = result[0].mention as? Mention.Date
            dateMention?.date?.start shouldBe "2025-10-15T09:00:00"
            dateMention?.date?.end shouldBe "2025-10-15T17:00:00"
            dateMention?.date?.timeZone shouldBe "America/New_York"
        }
    })
