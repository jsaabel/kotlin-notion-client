package dsl

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.saabelit.kotlinnotionclient.models.base.Annotations
import no.saabelit.kotlinnotionclient.models.base.Color
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.models.blocks.pageContent

class RichTextIntegrationTest :
    FunSpec({

        test("paragraph with rich text DSL should create proper block") {
            val content =
                pageContent {
                    paragraph {
                        text("Hello ")
                        bold("world")
                        text("!")
                    }
                }

            content shouldHaveSize 1
            val paragraph = content[0] as BlockRequest.Paragraph
            paragraph.paragraph.richText shouldHaveSize 3

            paragraph.paragraph.richText[0].plainText shouldBe "Hello "
            paragraph.paragraph.richText[0].annotations shouldBe Annotations()

            paragraph.paragraph.richText[1].plainText shouldBe "world"
            paragraph.paragraph.richText[1].annotations shouldBe Annotations(bold = true)

            paragraph.paragraph.richText[2].plainText shouldBe "!"
            paragraph.paragraph.richText[2].annotations shouldBe Annotations()
        }

        test("heading1 with rich text DSL should create proper block") {
            val content =
                pageContent {
                    heading1 {
                        text("Title with ")
                        colored("color", Color.BLUE)
                    }
                }

            content shouldHaveSize 1
            val heading = content[0] as BlockRequest.Heading1
            heading.heading1.richText shouldHaveSize 2

            heading.heading1.richText[0].plainText shouldBe "Title with "
            heading.heading1.richText[0].annotations shouldBe Annotations()

            heading.heading1.richText[1].plainText shouldBe "color"
            heading.heading1.richText[1].annotations shouldBe Annotations(color = Color.BLUE)
        }

        test("heading2 with rich text DSL should create proper block") {
            val content =
                pageContent {
                    heading2 {
                        text("Subtitle with ")
                        italic("emphasis")
                    }
                }

            content shouldHaveSize 1
            val heading = content[0] as BlockRequest.Heading2
            heading.heading2.richText shouldHaveSize 2

            heading.heading2.richText[0].plainText shouldBe "Subtitle with "
            heading.heading2.richText[0].annotations shouldBe Annotations()

            heading.heading2.richText[1].plainText shouldBe "emphasis"
            heading.heading2.richText[1].annotations shouldBe Annotations(italic = true)
        }

        test("heading3 with rich text DSL should create proper block") {
            val content =
                pageContent {
                    heading3 {
                        text("Section with ")
                        code("code")
                    }
                }

            content shouldHaveSize 1
            val heading = content[0] as BlockRequest.Heading3
            heading.heading3.richText shouldHaveSize 2

            heading.heading3.richText[0].plainText shouldBe "Section with "
            heading.heading3.richText[0].annotations shouldBe Annotations()

            heading.heading3.richText[1].plainText shouldBe "code"
            heading.heading3.richText[1].annotations shouldBe Annotations(code = true)
        }

        test("bullet list with rich text DSL should create proper block") {
            val content =
                pageContent {
                    bullet {
                        text("Item with ")
                        bold("bold")
                        text(" text")
                    }
                }

            content shouldHaveSize 1
            val bullet = content[0] as BlockRequest.BulletedListItem
            bullet.bulletedListItem.richText shouldHaveSize 3

            bullet.bulletedListItem.richText[0].plainText shouldBe "Item with "
            bullet.bulletedListItem.richText[0].annotations shouldBe Annotations()

            bullet.bulletedListItem.richText[1].plainText shouldBe "bold"
            bullet.bulletedListItem.richText[1].annotations shouldBe Annotations(bold = true)

            bullet.bulletedListItem.richText[2].plainText shouldBe " text"
            bullet.bulletedListItem.richText[2].annotations shouldBe Annotations()
        }

        test("numbered list with rich text DSL should create proper block") {
            val content =
                pageContent {
                    number {
                        text("Numbered item with ")
                        strikethrough("strikethrough")
                    }
                }

            content shouldHaveSize 1
            val number = content[0] as BlockRequest.NumberedListItem
            number.numberedListItem.richText shouldHaveSize 2

            number.numberedListItem.richText[0].plainText shouldBe "Numbered item with "
            number.numberedListItem.richText[0].annotations shouldBe Annotations()

            number.numberedListItem.richText[1].plainText shouldBe "strikethrough"
            number.numberedListItem.richText[1].annotations shouldBe Annotations(strikethrough = true)
        }

        test("todo with rich text DSL should create proper block") {
            val content =
                pageContent {
                    toDo(checked = true) {
                        text("Todo item with ")
                        underline("underline")
                    }
                }

            content shouldHaveSize 1
            val todo = content[0] as BlockRequest.ToDo
            todo.toDo.richText shouldHaveSize 2
            todo.toDo.checked shouldBe true

            todo.toDo.richText[0].plainText shouldBe "Todo item with "
            todo.toDo.richText[0].annotations shouldBe Annotations()

            todo.toDo.richText[1].plainText shouldBe "underline"
            todo.toDo.richText[1].annotations shouldBe Annotations(underline = true)
        }

        test("toggle with rich text DSL should create proper block") {
            val content =
                pageContent {
                    toggle {
                        text("Toggle with ")
                        colored("colored text", Color.RED)
                    }
                }

            content shouldHaveSize 1
            val toggle = content[0] as BlockRequest.Toggle
            toggle.toggle.richText shouldHaveSize 2

            toggle.toggle.richText[0].plainText shouldBe "Toggle with "
            toggle.toggle.richText[0].annotations shouldBe Annotations()

            toggle.toggle.richText[1].plainText shouldBe "colored text"
            toggle.toggle.richText[1].annotations shouldBe Annotations(color = Color.RED)
        }

        test("quote with rich text DSL should create proper block") {
            val content =
                pageContent {
                    quote {
                        text("Quote with ")
                        boldItalic("bold italic")
                        text(" text")
                    }
                }

            content shouldHaveSize 1
            val quote = content[0] as BlockRequest.Quote
            quote.quote.richText shouldHaveSize 3

            quote.quote.richText[0].plainText shouldBe "Quote with "
            quote.quote.richText[0].annotations shouldBe Annotations()

            quote.quote.richText[1].plainText shouldBe "bold italic"
            quote.quote.richText[1].annotations shouldBe Annotations(bold = true, italic = true)

            quote.quote.richText[2].plainText shouldBe " text"
            quote.quote.richText[2].annotations shouldBe Annotations()
        }

        test("callout with rich text DSL should create proper block") {
            val content =
                pageContent {
                    callout("🚨") {
                        text("Callout with ")
                        link("https://example.com", "link")
                    }
                }

            content shouldHaveSize 1
            val callout = content[0] as BlockRequest.Callout
            callout.callout.richText shouldHaveSize 2
            callout.callout.icon?.emoji shouldBe "🚨"

            callout.callout.richText[0].plainText shouldBe "Callout with "
            callout.callout.richText[0].annotations shouldBe Annotations()

            callout.callout.richText[1].plainText shouldBe "link"
            callout.callout.richText[1].href shouldBe "https://example.com"
        }

        test("mixed content with rich text DSL should create multiple blocks") {
            val content =
                pageContent {
                    heading1 {
                        text("Title with ")
                        bold("bold")
                    }

                    paragraph {
                        text("Paragraph with ")
                        italic("italic")
                        text(" and ")
                        code("code")
                    }

                    bullet {
                        text("Bullet with ")
                        colored("color", Color.GREEN)
                    }
                }

            content shouldHaveSize 3

            // Verify heading1
            val heading = content[0] as BlockRequest.Heading1
            heading.heading1.richText shouldHaveSize 2
            heading.heading1.richText[0].plainText shouldBe "Title with "
            heading.heading1.richText[1].plainText shouldBe "bold"
            heading.heading1.richText[1]
                .annotations.bold shouldBe true

            // Verify paragraph
            val paragraph = content[1] as BlockRequest.Paragraph
            paragraph.paragraph.richText shouldHaveSize 4
            paragraph.paragraph.richText[0].plainText shouldBe "Paragraph with "
            paragraph.paragraph.richText[1].plainText shouldBe "italic"
            paragraph.paragraph.richText[1]
                .annotations.italic shouldBe true
            paragraph.paragraph.richText[2].plainText shouldBe " and "
            paragraph.paragraph.richText[3].plainText shouldBe "code"
            paragraph.paragraph.richText[3]
                .annotations.code shouldBe true

            // Verify bullet
            val bullet = content[2] as BlockRequest.BulletedListItem
            bullet.bulletedListItem.richText shouldHaveSize 2
            bullet.bulletedListItem.richText[0].plainText shouldBe "Bullet with "
            bullet.bulletedListItem.richText[1].plainText shouldBe "color"
            bullet.bulletedListItem.richText[1]
                .annotations.color shouldBe Color.GREEN
        }

        test("rich text DSL should work with color parameters") {
            val content =
                pageContent {
                    paragraph(color = Color.BLUE) {
                        text("Paragraph with ")
                        bold("formatting")
                    }
                }

            content shouldHaveSize 1
            val paragraph = content[0] as BlockRequest.Paragraph
            paragraph.paragraph.color shouldBe Color.BLUE
            paragraph.paragraph.richText shouldHaveSize 2

            paragraph.paragraph.richText[0].plainText shouldBe "Paragraph with "
            paragraph.paragraph.richText[1].plainText shouldBe "formatting"
            paragraph.paragraph.richText[1]
                .annotations.bold shouldBe true
        }

        test("rich text DSL should work with nested children") {
            val content =
                pageContent {
                    toggle {
                        text("Toggle with ")
                        bold("bold text")
                    }
                }

            content shouldHaveSize 1
            val toggle = content[0] as BlockRequest.Toggle
            toggle.toggle.richText shouldHaveSize 2

            toggle.toggle.richText[0].plainText shouldBe "Toggle with "
            toggle.toggle.richText[1].plainText shouldBe "bold text"
            toggle.toggle.richText[1]
                .annotations.bold shouldBe true
        }
    })
