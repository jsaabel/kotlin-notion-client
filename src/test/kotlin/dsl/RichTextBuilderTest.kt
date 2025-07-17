package no.saabelit.kotlinnotionclient.dsl

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.saabelit.kotlinnotionclient.models.base.Annotations
import no.saabelit.kotlinnotionclient.models.base.Color
import no.saabelit.kotlinnotionclient.models.base.Equation
import no.saabelit.kotlinnotionclient.models.base.Link
import no.saabelit.kotlinnotionclient.models.base.Mention
import no.saabelit.kotlinnotionclient.models.richtext.richText
import no.saabelit.kotlinnotionclient.models.users.User

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
    })
