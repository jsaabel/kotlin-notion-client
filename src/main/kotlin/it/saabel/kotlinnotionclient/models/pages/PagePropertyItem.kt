package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.PageReference
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.utils.PaginatedResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for paginated property item retrieval.
 *
 * When a property contains more items than can be returned in a single response
 * (e.g., relation properties with >20 items), this structure is used for pagination.
 */
@Serializable
data class PagePropertyItemResponse(
    @SerialName("object")
    val objectType: String, // Always "list"
    @SerialName("results")
    override val results: List<PropertyItem>,
    @SerialName("next_cursor")
    override val nextCursor: String? = null,
    @SerialName("has_more")
    override val hasMore: Boolean,
    @SerialName("next_url")
    val nextUrl: String? = null,
    @SerialName("property_item")
    val propertyItem: PropertyItemMetadata,
) : PaginatedResponse<PropertyItem>

/**
 * Individual property item within a paginated response.
 */
@Serializable
data class PropertyItem(
    @SerialName("object")
    val objectType: String, // Always "property_item"
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String,
    @SerialName("rich_text")
    val richText: RichText? = null,
    @SerialName("title")
    val title: RichText? = null,
    @SerialName("relation")
    val relation: PageReference? = null,
    @SerialName("people")
    val people: it.saabel.kotlinnotionclient.models.users.User? = null,
    // Add other property types as needed
)

/**
 * Metadata about the property being paginated.
 */
@Serializable
data class PropertyItemMetadata(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String,
    @SerialName("next_url")
    val nextUrl: String? = null,
    // Add type-specific metadata as needed
)
