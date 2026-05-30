package it.saabel.kotlinnotionclient.utils

import it.saabel.kotlinnotionclient.models.base.Annotations
import it.saabel.kotlinnotionclient.models.base.RichText

/**
 * Renders a Notion rich-text array — as found on rich-text *properties* (title,
 * `rich_text`, callout/quote captions, etc.) — to a safe HTML string.
 *
 * Returns `null` for null, empty, or whitespace-only input so call sites can stay
 * one-liners and downstream payload assembly can elide the field.
 *
 * **In scope (v0.5.0):**
 * - Annotations: bold → `<strong>`, italic → `<em>`, underline → `<u>`,
 *   strikethrough → `<s>`, code → `<code>`.
 * - Real `text.link.url` links → `<a href="…" rel="noopener noreferrer" target="_blank">`.
 * - HTML escaping of all text content (`&`, `<`, `>`, `"`, `'`).
 * - Line breaks: a single `\n` becomes `<br>`; two or more consecutive newlines
 *   start a new paragraph. Output is always wrapped in `<p>…</p>`.
 * - Mentions, equations, and unknown segment types render as their `plainText`
 *   projection (escaped). Notion-internal mention hrefs are intentionally dropped.
 *
 * **Deferred to v0.6.0+** (rendered as escaped plain text for now):
 * - Colour annotations (inline `style=` vs. CSS class is an open design decision).
 * - Mention-aware rendering with a configurable link resolver
 *   (`page`/`user`/`date`/`database`/`template`).
 * - Equation rendering (MathML / KaTeX / `<code>` is an open design decision).
 * - A `RenderOptions` configuration object to toggle the above.
 *
 * @return the rendered HTML, or `null` when there is nothing to render.
 */
fun List<RichText>?.toHtml(): String? = RichTextHtmlRenderer.render(this)

/**
 * Backing implementation for [toHtml]. Kept `internal` — the public surface is the
 * [toHtml] extension only. Ported from the production-tested
 * `RichTextHtmlRenderer` in festival-scripts.
 */
internal object RichTextHtmlRenderer {
    fun render(richText: List<RichText>?): String? {
        if (richText.isNullOrEmpty()) return null
        val raw = richText.joinToString("") { renderSegment(it) }
        if (raw.isBlank()) return null
        return wrapInParagraphs(raw)
    }

    private fun renderSegment(segment: RichText): String {
        val content =
            when (segment.type) {
                "text" -> segment.text?.content ?: ""
                else -> segment.plainText
            }
        if (content.isEmpty()) return ""

        val escaped = htmlEscape(content)
        val withAnnotations = applyAnnotations(escaped, segment.annotations)
        return wrapInLinkIfPresent(withAnnotations, segment)
    }

    private fun applyAnnotations(
        html: String,
        a: Annotations,
    ): String {
        var result = html
        if (a.italic) result = "<em>$result</em>"
        if (a.bold) result = "<strong>$result</strong>"
        if (a.underline) result = "<u>$result</u>"
        if (a.strikethrough) result = "<s>$result</s>"
        if (a.code) result = "<code>$result</code>"
        return result
    }

    private fun wrapInLinkIfPresent(
        html: String,
        segment: RichText,
    ): String {
        // Real text-typed links only. Mention hrefs are dropped — they point at
        // Notion-internal URLs and would leak into public-facing HTML.
        val url = segment.text?.link?.url ?: return html
        return """<a href="${htmlEscape(url)}" rel="noopener noreferrer" target="_blank">$html</a>"""
    }

    private fun wrapInParagraphs(raw: String): String =
        raw
            .split(Regex("""\n\n+"""))
            .joinToString("") { paragraph ->
                "<p>${paragraph.replace("\n", "<br>")}</p>"
            }

    private fun htmlEscape(s: String): String =
        buildString(s.length) {
            for (c in s) {
                when (c) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(c)
                }
            }
        }
}
