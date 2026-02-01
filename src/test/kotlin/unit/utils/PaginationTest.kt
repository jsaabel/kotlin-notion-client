package unit.utils

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.base.EmptyObject
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryResponse
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.utils.PaginatedResponse
import it.saabel.kotlinnotionclient.utils.Pagination
import kotlinx.coroutines.flow.toList

/**
 * Unit tests for pagination utilities.
 *
 * Tests the generic pagination helpers (asFlow, collectAll, asPagesFlow)
 * using mock paginated responses.
 */
@Tags("Unit")
class PaginationTest :
    StringSpec({
        "collectAll should return all items from single page" {
            val mockPages = listOf(createMockPage("1"), createMockPage("2"), createMockPage("3"))
            val mockResponse = createMockQueryResponse(mockPages, hasMore = false, nextCursor = null)

            val fetcher: suspend (String?) -> DataSourceQueryResponse = { mockResponse }

            val result = Pagination.collectAll(fetcher)

            result.size shouldBe 3
            result.map { it.id } shouldContainExactly listOf("1", "2", "3")
        }

        "collectAll should return all items from multiple pages" {
            var callCount = 0
            val page1Items = listOf(createMockPage("1"), createMockPage("2"))
            val page2Items = listOf(createMockPage("3"), createMockPage("4"))
            val page3Items = listOf(createMockPage("5"))

            val fetcher: suspend (String?) -> DataSourceQueryResponse = { cursor ->
                when (callCount++) {
                    0 -> {
                        cursor shouldBe null
                        createMockQueryResponse(page1Items, hasMore = true, nextCursor = "cursor1")
                    }

                    1 -> {
                        cursor shouldBe "cursor1"
                        createMockQueryResponse(page2Items, hasMore = true, nextCursor = "cursor2")
                    }

                    else -> {
                        cursor shouldBe "cursor2"
                        createMockQueryResponse(page3Items, hasMore = false, nextCursor = null)
                    }
                }
            }

            val result = Pagination.collectAll(fetcher)

            result.size shouldBe 5
            result.map { it.id } shouldContainExactly listOf("1", "2", "3", "4", "5")
            callCount shouldBe 3
        }

        "collectAll should handle empty results" {
            val mockResponse = createMockQueryResponse(emptyList(), hasMore = false, nextCursor = null)
            val fetcher: suspend (String?) -> DataSourceQueryResponse = { mockResponse }

            val result = Pagination.collectAll(fetcher)

            result.size shouldBe 0
        }

        "asFlow should emit all items from single page" {
            val mockPages = listOf(createMockPage("1"), createMockPage("2"), createMockPage("3"))
            val mockResponse = createMockQueryResponse(mockPages, hasMore = false, nextCursor = null)

            val fetcher: suspend (String?) -> DataSourceQueryResponse = { mockResponse }

            val result = Pagination.asFlow(fetcher).toList()

            result.size shouldBe 3
            result.map { it.id } shouldContainExactly listOf("1", "2", "3")
        }

        "asFlow should emit all items from multiple pages" {
            var callCount = 0
            val page1Items = listOf(createMockPage("1"), createMockPage("2"))
            val page2Items = listOf(createMockPage("3"), createMockPage("4"))
            val page3Items = listOf(createMockPage("5"))

            val fetcher: suspend (String?) -> DataSourceQueryResponse = { cursor ->
                when (callCount++) {
                    0 -> {
                        cursor shouldBe null
                        createMockQueryResponse(page1Items, hasMore = true, nextCursor = "cursor1")
                    }

                    1 -> {
                        cursor shouldBe "cursor1"
                        createMockQueryResponse(page2Items, hasMore = true, nextCursor = "cursor2")
                    }

                    else -> {
                        cursor shouldBe "cursor2"
                        createMockQueryResponse(page3Items, hasMore = false, nextCursor = null)
                    }
                }
            }

            val result = Pagination.asFlow(fetcher).toList()

            result.size shouldBe 5
            result.map { it.id } shouldContainExactly listOf("1", "2", "3", "4", "5")
            callCount shouldBe 3
        }

        "asFlow should handle empty results" {
            val mockResponse = createMockQueryResponse(emptyList(), hasMore = false, nextCursor = null)
            val fetcher: suspend (String?) -> DataSourceQueryResponse = { mockResponse }

            val result = Pagination.asFlow(fetcher).toList()

            result.size shouldBe 0
        }

        "asPagesFlow should emit all pages" {
            var callCount = 0
            val page1Items = listOf(createMockPage("1"), createMockPage("2"))
            val page2Items = listOf(createMockPage("3"), createMockPage("4"))
            val page3Items = listOf(createMockPage("5"))

            val fetcher: suspend (String?) -> DataSourceQueryResponse = { cursor ->
                when (callCount++) {
                    0 -> {
                        cursor shouldBe null
                        createMockQueryResponse(page1Items, hasMore = true, nextCursor = "cursor1")
                    }

                    1 -> {
                        cursor shouldBe "cursor1"
                        createMockQueryResponse(page2Items, hasMore = true, nextCursor = "cursor2")
                    }

                    else -> {
                        cursor shouldBe "cursor2"
                        createMockQueryResponse(page3Items, hasMore = false, nextCursor = null)
                    }
                }
            }

            val pages = Pagination.asPagesFlow(fetcher).toList()

            pages.size shouldBe 3
            pages[0].results.size shouldBe 2
            pages[0].hasMore shouldBe true
            pages[0].nextCursor shouldBe "cursor1"

            pages[1].results.size shouldBe 2
            pages[1].hasMore shouldBe true
            pages[1].nextCursor shouldBe "cursor2"

            pages[2].results.size shouldBe 1
            pages[2].hasMore shouldBe false
            pages[2].nextCursor shouldBe null

            callCount shouldBe 3
        }

        "asPagesFlow should emit single page when no more results" {
            val mockPages = listOf(createMockPage("1"), createMockPage("2"))
            val mockResponse = createMockQueryResponse(mockPages, hasMore = false, nextCursor = null)

            val fetcher: suspend (String?) -> DataSourceQueryResponse = { mockResponse }

            val pages = Pagination.asPagesFlow(fetcher).toList()

            pages.size shouldBe 1
            pages[0].results.size shouldBe 2
            pages[0].hasMore shouldBe false
        }

        "generic pagination should work with custom PaginatedResponse implementation" {
            // Test that our interface works with any implementation
            data class CustomResponse(
                override val results: List<String>,
                override val nextCursor: String?,
                override val hasMore: Boolean,
            ) : PaginatedResponse<String>

            var callCount = 0
            val fetcher: suspend (String?) -> CustomResponse = { cursor ->
                when (callCount++) {
                    0 -> CustomResponse(listOf("a", "b"), "cursor1", true)
                    else -> CustomResponse(listOf("c"), null, false)
                }
            }

            val result = Pagination.collectAll(fetcher)

            result shouldContainExactly listOf("a", "b", "c")
            callCount shouldBe 2
        }
    })

/**
 * Creates a mock Page for testing.
 * Only includes the minimal required fields.
 */
private fun createMockPage(id: String): Page =
    Page(
        id = id,
        createdTime = "2025-01-01T00:00:00.000Z",
        lastEditedTime = "2025-01-01T00:00:00.000Z",
        archived = false,
        properties = emptyMap(),
        parent = it.saabel.kotlinnotionclient.models.base.Parent.WorkspaceParent,
        url = "https://notion.so/$id",
        publicUrl = null,
    )

/**
 * Creates a mock DatabaseQueryResponse for testing.
 */
private fun createMockQueryResponse(
    pages: List<Page>,
    hasMore: Boolean,
    nextCursor: String?,
): DataSourceQueryResponse =
    DataSourceQueryResponse(
        objectType = "list",
        results = pages,
        nextCursor = nextCursor,
        hasMore = hasMore,
        type = "page_or_database",
        pageOrDatabase =
            EmptyObject(),
    )
