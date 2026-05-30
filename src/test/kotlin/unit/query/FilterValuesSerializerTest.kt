package unit.query

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.datasources.FilterValues
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Tags("Unit")
class FilterValuesSerializerTest :
    FunSpec({
        val json = Json { encodeDefaults = false }

        context("encoding") {
            test("size == 1 encodes to a bare JSON string") {
                json.encodeToString(FilterValues(listOf("X"))) shouldBe "\"X\""
            }

            test("size > 1 encodes to a JSON array of strings") {
                json.encodeToString(FilterValues(listOf("X", "Y"))) shouldBe """["X","Y"]"""
            }

            test("size > 2 encodes to a JSON array preserving order") {
                json.encodeToString(FilterValues(listOf("a", "b", "c"))) shouldBe """["a","b","c"]"""
            }
        }

        context("decoding") {
            test("a bare JSON string decodes to a single-element FilterValues") {
                json.decodeFromString<FilterValues>("\"X\"") shouldBe FilterValues(listOf("X"))
            }

            test("a JSON array decodes to a multi-element FilterValues") {
                json.decodeFromString<FilterValues>("""["X","Y"]""") shouldBe FilterValues(listOf("X", "Y"))
            }
        }

        context("round-trip") {
            test("single value round-trips through string form") {
                val original = FilterValues(listOf("only"))
                json.decodeFromString<FilterValues>(json.encodeToString(original)) shouldBe original
            }

            test("multi value round-trips through array form") {
                val original = FilterValues(listOf("one", "two", "three"))
                json.decodeFromString<FilterValues>(json.encodeToString(original)) shouldBe original
            }
        }

        context("empty values") {
            test("constructing FilterValues with an empty list throws") {
                shouldThrow<IllegalArgumentException> { FilterValues(emptyList()) }
            }

            test("decoding an empty JSON array throws") {
                shouldThrow<IllegalArgumentException> { json.decodeFromString<FilterValues>("[]") }
            }
        }
    })
