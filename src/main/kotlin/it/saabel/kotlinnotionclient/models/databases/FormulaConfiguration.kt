package it.saabel.kotlinnotionclient.models.databases

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed configuration of a formula database property: the `formula` object with its
 * single `expression` field.
 *
 * Since the Notion API changelog of Aug 12, 2026:
 * - Schemas are (gradually) **read back** using the same readable `prop("Property Name")`
 *   syntax you write. Expressions that cannot be rendered faithfully in that syntax are
 *   still returned in Notion's internal reference syntax
 *   (`{{notion:block_property:...}}`) — [usesInternalReferences] tells the two apart.
 * - On **write**, `prop()` references are stored exactly as written, and expressions the
 *   API cannot store fail with a `validation_error` instead of being silently rewritten.
 *
 * This class is shared between the read model ([DatabaseProperty.Formula]) and the write
 * model ([CreateDatabaseProperty.Formula]). It performs no validation itself, so anything
 * Notion returns can be represented; the write model validates at construction time.
 */
@Serializable
data class FormulaConfiguration(
    @SerialName("expression")
    val expression: String,
) {
    /**
     * The property names referenced via literal `prop("...")` calls in [expression],
     * in order of first appearance, without duplicates.
     *
     * Extraction is best-effort and never throws: references inside string literals or
     * comments are ignored, escaped quotes in property names are unescaped, and malformed
     * input simply yields the references that could be read up to that point. References
     * in Notion's internal `{{notion:block_property:...}}` syntax are ids, not names, and
     * are not reported here — check [usesInternalReferences] for those.
     */
    fun propertyReferences(): List<String> = FormulaExpressions.propertyReferences(expression)

    /**
     * Whether [expression] contains references in Notion's internal
     * `{{notion:block_property:...}}` syntax rather than (only) readable `prop("...")`
     * calls. The readable syntax is rolling out gradually on the read side, so both forms
     * can be encountered.
     */
    fun usesInternalReferences(): Boolean = FormulaExpressions.containsInternalReferences(expression)
}

/**
 * Local, syntax-level checks for formula expressions on the **write side**.
 *
 * Only structural problems that are unambiguously wrong regardless of formula semantics
 * are rejected: blank expressions, unterminated string literals, unbalanced or mismatched
 * brackets, and malformed `prop()` calls. Whether Notion will actually *accept* a
 * syntactically well-formed expression (unknown functions, type errors, circular
 * references, ...) is not locally decidable and is deliberately not checked — those
 * surface as the API's own `validation_error`.
 *
 * Follows the precedent of `NotionDateStrings` (#31): pure functions throwing
 * [IllegalArgumentException] with messages that name the offending input and the fix,
 * called at construction/DSL time so bad values fail at the call site.
 */
internal object FormulaExpressions {
    private val INTERNAL_REFERENCE_REGEX = Regex("""\{\{notion:""")

    /**
     * Validates [expression], throwing [IllegalArgumentException] naming the offending
     * expression and what is wrong with it. [propertyName], when known, names the schema
     * property in the message.
     */
    fun validate(
        expression: String,
        propertyName: String? = null,
    ) {
        val subject = "Formula expression${propertyName?.let { " for property '$it'" } ?: ""}"
        require(expression.isNotBlank()) {
            "$subject is blank. Provide a formula such as \"if(prop(\\\"In stock\\\"), 0, prop(\\\"Price\\\"))\"."
        }
        val problem = scan(expression).problem
        require(problem == null) {
            "$subject is malformed: $problem. Expression: $expression"
        }
    }

    /** Best-effort extraction of literal `prop("...")` references; never throws. */
    fun propertyReferences(expression: String): List<String> = scan(expression).references.distinct()

    /** Whether the expression contains Notion's internal `{{notion:...}}` reference syntax. */
    fun containsInternalReferences(expression: String): Boolean = INTERNAL_REFERENCE_REGEX.containsMatchIn(expression)

    /**
     * Validates that every literal `prop("...")` reference in the formula properties of
     * [properties] names a property defined in the same schema. Only sound for **create**
     * requests, where the map is the complete schema; update requests may legitimately
     * reference properties that already exist on the data source but are absent from the
     * partial update, so they must not be checked this way.
     */
    fun validateReferencesExist(properties: Map<String, CreateDatabaseProperty>) {
        properties.forEach { (name, property) ->
            if (property is CreateDatabaseProperty.Formula) {
                property.formula.propertyReferences().forEach { reference ->
                    require(reference in properties) {
                        "Formula property '$name' references prop(\"$reference\"), but no property named " +
                            "'$reference' is defined in this schema. Defined properties: " +
                            "${properties.keys.sorted()}"
                    }
                }
            }
        }
    }

    private class ScanResult(
        val references: List<String>,
        val problem: String?,
    )

    /**
     * Single pass over the expression tracking string literals (double quotes, backslash
     * escapes), block comments and a bracket stack, collecting literal `prop("...")`
     * references. Returns the first structural problem found, or null.
     */
    @Suppress("ReturnCount")
    private fun scan(expression: String): ScanResult {
        val references = mutableListOf<String>()
        val brackets = ArrayDeque<Pair<Char, Int>>()
        var i = 0
        val n = expression.length

        fun problem(message: String) = ScanResult(references, message)

        while (i < n) {
            val c = expression[i]
            when {
                c == '"' -> {
                    val end =
                        skipStringLiteral(expression, i)
                            ?: return problem("unterminated string literal starting at index $i")
                    i = end
                }

                c == '/' && i + 1 < n && expression[i + 1] == '*' -> {
                    val end = expression.indexOf("*/", i + 2)
                    // An unterminated comment is left to the API to judge; treat the rest
                    // of the expression as commented out rather than guessing.
                    i = if (end == -1) n else end + 2
                }

                c == '(' || c == '[' || c == '{' -> {
                    brackets.addLast(c to i)
                    i++
                }

                c == ')' || c == ']' || c == '}' -> {
                    val (open, openIndex) =
                        brackets.removeLastOrNull()
                            ?: return problem("unmatched closing '$c' at index $i")
                    if (open != matchingOpener(c)) {
                        return problem("closing '$c' at index $i does not match '$open' opened at index $openIndex")
                    }
                    i++
                }

                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < n && (expression[i].isLetterOrDigit() || expression[i] == '_')) i++
                    if (expression.substring(start, i) == "prop") {
                        val afterCall = consumePropCall(expression, callStart = start, afterName = i, references)
                        when (afterCall) {
                            is PropCall.NotACall -> Unit

                            // bare identifier `prop`; leave it to the API
                            is PropCall.Consumed -> i = afterCall.nextIndex

                            is PropCall.Malformed -> return problem(afterCall.message)
                        }
                    }
                }

                else -> {
                    i++
                }
            }
        }

        brackets.lastOrNull()?.let { (open, openIndex) ->
            return problem("'$open' opened at index $openIndex is never closed")
        }
        return ScanResult(references, null)
    }

    private sealed interface PropCall {
        data object NotACall : PropCall

        data class Consumed(
            val nextIndex: Int,
        ) : PropCall

        data class Malformed(
            val message: String,
        ) : PropCall
    }

    /**
     * Parses a `prop("Name")` call whose identifier ends at [afterName]. `prop()` takes
     * exactly one double-quoted property name — that is the only argument form Notion's
     * formula language accepts, and the only one the Aug 2026 write path stores verbatim.
     */
    private fun consumePropCall(
        expression: String,
        callStart: Int,
        afterName: Int,
        references: MutableList<String>,
    ): PropCall {
        val n = expression.length
        var i = afterName
        while (i < n && expression[i].isWhitespace()) i++
        if (i >= n || expression[i] != '(') return PropCall.NotACall
        i++
        while (i < n && expression[i].isWhitespace()) i++
        if (i >= n || expression[i] != '"') {
            return PropCall.Malformed(
                "prop() at index $callStart must be called with a double-quoted property name, " +
                    "e.g. prop(\"Price\")",
            )
        }
        val name = StringBuilder()
        val afterLiteral =
            skipStringLiteral(expression, i, name)
                ?: return PropCall.Malformed("unterminated string literal in prop() at index $callStart")
        i = afterLiteral
        while (i < n && expression[i].isWhitespace()) i++
        if (i >= n || expression[i] != ')') {
            return PropCall.Malformed(
                "prop() at index $callStart must take exactly one double-quoted property name, " +
                    "e.g. prop(\"Price\")",
            )
        }
        if (name.isEmpty()) {
            return PropCall.Malformed("prop(\"\") at index $callStart references an empty property name")
        }
        references += name.toString()
        return PropCall.Consumed(i + 1)
    }

    /**
     * Skips the double-quoted string literal starting at [start] (which must point at the
     * opening quote), honouring backslash escapes. Returns the index just past the closing
     * quote, or null if the literal never terminates. When [content] is given, the
     * unescaped characters are appended to it.
     */
    private fun skipStringLiteral(
        expression: String,
        start: Int,
        content: StringBuilder? = null,
    ): Int? {
        var i = start + 1
        val n = expression.length
        while (i < n) {
            when (expression[i]) {
                '\\' -> {
                    if (i + 1 < n) content?.append(expression[i + 1])
                    i += 2
                }

                '"' -> {
                    return i + 1
                }

                else -> {
                    content?.append(expression[i])
                    i++
                }
            }
        }
        return null
    }

    private fun matchingOpener(closer: Char): Char =
        when (closer) {
            ')' -> '('
            ']' -> '['
            else -> '{'
        }
}
