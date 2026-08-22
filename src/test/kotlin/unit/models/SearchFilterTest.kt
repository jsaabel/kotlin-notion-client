package unit.models

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.search.SearchFilter
import it.saabel.kotlinnotionclient.models.search.searchRequest
import kotlinx.serialization.json.Json

/**
 * Unit tests for [SearchFilter]'s `property` default and `init` validation.
 *
 * `property` used to default to `"object"` in the primary constructor, so a hand-written
 * `SearchFilter(inTrash = true)` bypassing [it.saabel.kotlinnotionclient.models.search.SearchRequestBuilder]
 * emitted a spurious `"property":"object"` alongside `in_trash`. `property` now defaults to
 * `null` and is only ever set (to `"object"`) alongside a non-null `value`.
 */
@Tags("Unit")
class SearchFilterTest :
    StringSpec({
        val json =
            Json {
                encodeDefaults = true
                explicitNulls = false
            }

        "SearchFilter(inTrash = true) omits property from the serialized JSON" {
            val filter = SearchFilter(inTrash = true)

            filter.property shouldBe null
            val encoded = json.encodeToString(SearchFilter.serializer(), filter)
            encoded.contains("property") shouldBe false
            encoded.contains("\"in_trash\":true") shouldBe true
        }

        "SearchFilter(value = \"page\") defaults property to null, not \"object\"" {
            SearchFilter(value = "page").property shouldBe null
        }

        "SearchFilter(value = \"page\", property = \"object\") is valid" {
            val filter = SearchFilter(value = "page", property = "object")
            filter.property shouldBe "object"
        }

        "SearchFilter rejects a non-null property when value is null" {
            shouldThrow<IllegalArgumentException> {
                SearchFilter(value = null, property = "object")
            }
        }

        "SearchFilter rejects a property value other than \"object\"" {
            shouldThrow<IllegalArgumentException> {
                SearchFilter(value = "page", property = "something_else")
            }
        }

        "SearchRequestBuilder.inTrash() alone produces no spurious property field" {
            val request = searchRequest { inTrash() }

            request.filter?.property shouldBe null
            request.filter?.inTrash shouldBe true
        }

        "SearchRequestBuilder.filterPages() sets both value and property" {
            val request = searchRequest { filterPages() }

            request.filter?.value shouldBe "page"
            request.filter?.property shouldBe "object"
        }
    })
