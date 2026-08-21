package unit.query

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryBuilder
import it.saabel.kotlinnotionclient.models.datasources.dataSourceQuery
import it.saabel.kotlinnotionclient.models.search.SearchRequest
import it.saabel.kotlinnotionclient.models.search.searchRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tests for reaching archived data source rows and trashed search results.
 *
 * Covers the `is_archived` top-level body parameter on the data source query
 * endpoint and the `filter.in_trash` option on the search endpoint
 * (Notion changelog, Jul 15 2026).
 */
@Tags("Unit")
class TrashAndArchiveQueryTest :
    StringSpec({

        // Mirrors the serialization settings used by NotionClient's HTTP client.
        val json =
            Json {
                encodeDefaults = true
                explicitNulls = false
                prettyPrint = false
            }

        "Data source query omits is_archived by default" {
            val request = dataSourceQuery { }

            request.isArchived shouldBe null
            json.encodeToString(request) shouldBe "{}"
        }

        "Data source query builder sets is_archived to true by default" {
            val request =
                DataSourceQueryBuilder()
                    .isArchived()
                    .build()

            request.isArchived shouldBe true
            json.encodeToString(request) shouldBe """{"is_archived":true}"""
        }

        "Data source query builder can request non-archived rows explicitly" {
            val request = dataSourceQuery { isArchived(false) }

            request.isArchived shouldBe false
            json.encodeToString(request) shouldBe """{"is_archived":false}"""
        }

        "Data source query serializes is_archived alongside filters and sorts" {
            val request =
                dataSourceQuery {
                    filter { checkbox("Done").equals(true) }
                    isArchived(true)
                    pageSize(25)
                }

            val serialized = json.encodeToString(request)

            serialized.contains(""""is_archived":true""") shouldBe true
            serialized.contains(""""page_size":25""") shouldBe true
        }

        "Search request omits filter by default" {
            val request = searchRequest { query("kale") }

            request.filter shouldBe null
            json.encodeToString(request) shouldBe """{"query":"kale"}"""
        }

        "Search builder emits an in_trash-only filter" {
            val request = searchRequest { inTrash() }

            request.filter?.inTrash shouldBe true
            request.filter?.value shouldBe null
            json.encodeToString(request) shouldBe """{"filter":{"in_trash":true}}"""
        }

        "Search builder can combine object filter with in_trash" {
            val request =
                searchRequest {
                    query("notes")
                    filterPages()
                    inTrash(true)
                }

            request.filter?.value shouldBe "page"
            request.filter?.property shouldBe "object"
            request.filter?.inTrash shouldBe true

            json.encodeToString(request) shouldBe
                """{"query":"notes","filter":{"value":"page","property":"object","in_trash":true}}"""
        }

        "Search builder can request non-trashed results explicitly" {
            val request = searchRequest { inTrash(false) }

            json.encodeToString(request) shouldBe """{"filter":{"in_trash":false}}"""
        }

        "Existing object-only search filters keep their property field" {
            val request = searchRequest { filterDataSources() }

            json.encodeToString(request) shouldBe """{"filter":{"value":"data_source","property":"object"}}"""
        }

        "SearchRequest with no arguments serializes to an empty object" {
            json.encodeToString(SearchRequest()) shouldBe "{}"
        }
    })
