package unit.utils

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.base.Annotations
import it.saabel.kotlinnotionclient.models.base.DateObject
import it.saabel.kotlinnotionclient.models.base.Equation
import it.saabel.kotlinnotionclient.models.base.Link
import it.saabel.kotlinnotionclient.models.base.Mention
import it.saabel.kotlinnotionclient.models.base.PageReference
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.TextContent
import it.saabel.kotlinnotionclient.utils.toHtml

/**
 * Fixture helpers — keep tests focused on the behaviour, not the wire format.
 */
private fun text(
    content: String,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    strikethrough: Boolean = false,
    code: Boolean = false,
    linkUrl: String? = null,
): RichText =
    RichText(
        type = "text",
        text = TextContent(content = content, link = linkUrl?.let { Link(it) }),
        annotations =
            Annotations(
                bold = bold,
                italic = italic,
                underline = underline,
                strikethrough = strikethrough,
                code = code,
            ),
        plainText = content,
        href = linkUrl,
    )

private fun pageMention(
    name: String,
    pageId: String = "abc",
): RichText =
    RichText(
        type = "mention",
        mention = Mention.Page(page = PageReference(id = pageId)),
        annotations = Annotations(),
        plainText = name,
        href = "https://www.notion.so/$pageId",
    )

private fun dateMention(
    rendered: String,
    isoStart: String = "2026-05-26",
): RichText =
    RichText(
        type = "mention",
        mention =
            Mention.Date(
                date = DateObject(start = isoStart, end = null, timeZone = null),
            ),
        annotations = Annotations(),
        plainText = rendered,
        href = null,
    )

private fun equation(
    expression: String,
    rendered: String = expression,
): RichText =
    RichText(
        type = "equation",
        equation = Equation(expression = expression),
        annotations = Annotations(),
        plainText = rendered,
        href = null,
    )

@Tags("Unit")
class RichTextHtmlTest :
    FunSpec({

        context("empty / blank inputs") {
            test("null input → null") {
                (null as List<RichText>?).toHtml().shouldBeNull()
            }

            test("empty list → null") {
                emptyList<RichText>().toHtml().shouldBeNull()
            }

            test("whitespace-only segments → null") {
                listOf(text("   "), text("\t \n ")).toHtml().shouldBeNull()
            }
        }

        context("plain text") {
            test("single plain segment wraps in <p>") {
                listOf(text("hello")).toHtml() shouldBe "<p>hello</p>"
            }

            test("adjacent plain + bold segments share one outer <p>") {
                val rt = listOf(text("foo "), text("bar", bold = true))
                rt.toHtml() shouldBe "<p>foo <strong>bar</strong></p>"
            }
        }

        context("single-annotation cases") {
            test("bold") {
                listOf(text("x", bold = true)).toHtml() shouldBe
                    "<p><strong>x</strong></p>"
            }

            test("italic") {
                listOf(text("x", italic = true)).toHtml() shouldBe
                    "<p><em>x</em></p>"
            }

            test("underline") {
                listOf(text("x", underline = true)).toHtml() shouldBe
                    "<p><u>x</u></p>"
            }

            test("strikethrough") {
                listOf(text("x", strikethrough = true)).toHtml() shouldBe
                    "<p><s>x</s></p>"
            }

            test("code") {
                listOf(text("x", code = true)).toHtml() shouldBe
                    "<p><code>x</code></p>"
            }
        }

        context("multi-annotation nesting (outer→inner = a/code/s/u/strong/em)") {
            test("bold + italic → <strong><em>x</em></strong>") {
                listOf(text("x", bold = true, italic = true)).toHtml() shouldBe
                    "<p><strong><em>x</em></strong></p>"
            }

            test("all five annotations nest in the documented order") {
                val rt =
                    text(
                        "x",
                        bold = true,
                        italic = true,
                        underline = true,
                        strikethrough = true,
                        code = true,
                    )
                listOf(rt).toHtml() shouldBe
                    "<p><code><s><u><strong><em>x</em></strong></u></s></code></p>"
            }
        }

        context("links") {
            test("text segment with link.url → <a> with rel + target=_blank") {
                val rt = text("click", linkUrl = "https://example.com")
                rt.toHtmlList() shouldBe
                    """<p><a href="https://example.com" rel="noopener noreferrer" target="_blank">click</a></p>"""
            }

            test("anchor carries the rel=\"noopener noreferrer\" attribute") {
                val rt = listOf(text("click", linkUrl = "https://example.com"))
                rt.toHtml() shouldContain """rel="noopener noreferrer""""
            }

            test("annotations nest inside the anchor") {
                val rt = text("click", bold = true, linkUrl = "https://example.com")
                rt.toHtmlList() shouldBe
                    """<p><a href="https://example.com" rel="noopener noreferrer" target="_blank"><strong>click</strong></a></p>"""
            }
        }

        context("mentions") {
            test("page mention renders as plain text, no Notion <a>") {
                listOf(pageMention("Some Film")).toHtml() shouldBe
                    "<p>Some Film</p>"
            }

            test("date mention renders its plainText projection") {
                listOf(dateMention("May 26, 2026")).toHtml() shouldBe
                    "<p>May 26, 2026</p>"
            }

            test("equation renders its plainText projection") {
                listOf(equation("E=mc^2")).toHtml() shouldBe
                    "<p>E=mc^2</p>"
            }
        }

        context("line-break semantics") {
            test("single \\n within a segment → <br> inside the same <p>") {
                listOf(text("line one\nline two")).toHtml() shouldBe
                    "<p>line one<br>line two</p>"
            }

            test("\\n\\n splits paragraphs") {
                listOf(text("para one\n\npara two")).toHtml() shouldBe
                    "<p>para one</p><p>para two</p>"
            }

            test("mixed \\n and \\n\\n produces correct block + inline structure") {
                val rt = listOf(text("a\nb\n\nc\nd"))
                rt.toHtml() shouldBe
                    "<p>a<br>b</p><p>c<br>d</p>"
            }

            test("3+ consecutive newlines collapse to a single paragraph break") {
                listOf(text("a\n\n\n\nb")).toHtml() shouldBe
                    "<p>a</p><p>b</p>"
            }
        }

        context("HTML escaping") {
            test("&, <, > in content are escaped") {
                listOf(text("a & b < c > d")).toHtml() shouldBe
                    "<p>a &amp; b &lt; c &gt; d</p>"
            }

            test("\" and ' inside link.url are escaped in the href attribute") {
                val rt = text("x", linkUrl = """https://example.com/?q="a'b""")
                rt.toHtmlList() shouldBe
                    """<p><a href="https://example.com/?q=&quot;a&#39;b" rel="noopener noreferrer" target="_blank">x</a></p>"""
            }

            test("annotations apply on escaped content (no double-escaping of tag chars)") {
                listOf(text("<b>", bold = true)).toHtml() shouldBe
                    "<p><strong>&lt;b&gt;</strong></p>"
            }
        }
    })

/** Convenience: render a single segment through the public [toHtml] surface. */
private fun RichText.toHtmlList(): String? = listOf(this).toHtml()
