package it.saabel.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import it.saabel.kotlinnotionclient.config.NotionApiLimits
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.exceptions.toNotionApiError
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryResponse
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.PagePropertyItemResponse
import it.saabel.kotlinnotionclient.models.pages.PageReference
import it.saabel.kotlinnotionclient.models.pages.PropertyItem
import it.saabel.kotlinnotionclient.utils.PropertyIds

/**
 * Notion's page object returns at most this many references for a relation or people property
 * (and for inline references in title / rich_text / formula / rollup values). Anything beyond it
 * has to be fetched from the *Retrieve a page property item* endpoint.
 */
internal const val PAGE_PROPERTY_REFERENCE_CAP = 25

/**
 * Low-level access to `GET /pages/{page_id}/properties/{property_id}`, shared by [PagesApi] (its
 * public `retrievePropertyItems*` methods) and [TruncatedPropertyCompleter].
 */
internal class PropertyItemsClient(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
) {
    /**
     * Fetches one page of property items.
     *
     * The property ID is decoded first and then path-encoded by Ktor, so both the percent-encoded
     * shape found in page responses (e.g. `%3AUPp`) and the decoded shape (e.g. `:UPp`) address
     * the same property.
     */
    suspend fun fetchPage(
        pageId: String,
        propertyId: String,
        cursor: String?,
    ): PagePropertyItemResponse =
        try {
            val url =
                URLBuilder(config.baseUrl)
                    .appendPathSegments("pages", pageId, "properties", PropertyIds.decode(propertyId))
                    .buildString()
            val response: HttpResponse =
                httpClient.get(url) {
                    parameter("page_size", NotionApiLimits.Response.MAX_PAGE_SIZE)
                    cursor?.let { parameter("start_cursor", it) }
                }

            if (response.status.isSuccess()) {
                response.body<PagePropertyItemResponse>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: ClientRequestException) {
            // Handle HTTP client errors (4xx)
            throw e.response.toNotionApiError()
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Fetches every property item by following the cursor chain.
     */
    suspend fun fetchAll(
        pageId: String,
        propertyId: String,
    ): List<PropertyItem> {
        val allItems = mutableListOf<PropertyItem>()
        var cursor: String? = null
        var pageCount = 0

        do {
            val response = fetchPage(pageId, propertyId, cursor)
            allItems.addAll(response.results)

            cursor = response.nextCursor
            pageCount++

            // Safety check to prevent infinite loops
            if (pageCount >= MAX_PAGES) {
                throw NotionException.ApiError(
                    code = "PAGINATION_LIMIT_EXCEEDED",
                    status = 500,
                    details =
                        "Property retrieval exceeded $MAX_PAGES pages. " +
                            "This may indicate an infinite loop or an extremely large property.",
                )
            }
        } while (response.hasMore && cursor != null)

        return allItems
    }

    private companion object {
        /** 1,000 pages of 100 items — far beyond any realistic relation or people list. */
        const val MAX_PAGES = 1000
    }
}

/**
 * Completes page properties that Notion truncated at [PAGE_PROPERTY_REFERENCE_CAP] references,
 * so callers never see a silently partial list (#91).
 *
 * - **relation**: completed when the page reports `has_more: true`.
 * - **people**: Notion exposes no `has_more` flag, so a list of exactly
 *   [PAGE_PROPERTY_REFERENCE_CAP] (or more) people is treated as possibly truncated and re-read.
 *
 * Properties are completed sequentially, one property-item pagination per truncated property,
 * so the client's rate limiter paces the extra requests. Any failure propagates — a page is
 * either returned complete or not at all.
 */
internal class TruncatedPropertyCompleter(
    private val propertyItems: PropertyItemsClient,
) {
    constructor(httpClient: HttpClient, config: NotionConfig) : this(PropertyItemsClient(httpClient, config))

    suspend fun complete(page: Page): Page {
        val truncated = page.properties.filterValues { it.isPossiblyTruncated() }
        if (truncated.isEmpty()) return page

        val completed = page.properties.toMutableMap()
        for ((name, property) in truncated) {
            if (property.id.isBlank()) {
                throw NotionException.UnexpectedError(
                    "Property '$name' on page ${page.id} is truncated but carries no property ID, " +
                        "so it cannot be completed.",
                )
            }
            val items = propertyItems.fetchAll(page.id, property.id)
            completed[name] =
                when (property) {
                    is PageProperty.Relation -> {
                        property.copy(
                            relation =
                                items.map { item ->
                                    // PropertyItem uses the base PageReference; Page properties use the pages one
                                    PageReference(id = (item.relation ?: throw unexpectedItem(page, name, item)).id)
                                },
                            hasMore = false,
                        )
                    }

                    is PageProperty.People -> {
                        property.copy(people = items.map { it.people ?: throw unexpectedItem(page, name, it) })
                    }

                    else -> {
                        property
                    }
                }
        }
        return page.copy(properties = completed)
    }

    suspend fun complete(response: DataSourceQueryResponse): DataSourceQueryResponse {
        if (response.results.none { page -> page.properties.values.any { it.isPossiblyTruncated() } }) {
            return response
        }
        return response.copy(results = response.results.map { complete(it) })
    }

    private fun unexpectedItem(
        page: Page,
        name: String,
        item: PropertyItem,
    ) = NotionException.UnexpectedError(
        "Property '$name' on page ${page.id} returned a '${item.type}' property item while being completed.",
    )
}

private fun PageProperty.isPossiblyTruncated(): Boolean =
    when (this) {
        is PageProperty.Relation -> hasMore
        is PageProperty.People -> people.size >= PAGE_PROPERTY_REFERENCE_CAP
        else -> false
    }
