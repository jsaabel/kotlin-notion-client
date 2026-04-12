package it.saabel.kotlinnotionclient.models.base

import it.saabel.kotlinnotionclient.utils.PaginatedResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an external file reference.
 * Used across pages, blocks, and other objects that support external file links.
 */
@Serializable
data class ExternalFile(
    @SerialName("url")
    val url: String,
)

/**
 * Represents a Notion-hosted file with an expiring URL.
 * Files uploaded via the Notion UI have temporary URLs that expire after 1 hour.
 */
@Serializable
data class NotionFile(
    @SerialName("url")
    val url: String,
    @SerialName("expiry_time")
    val expiryTime: String? = null,
)

/**
 * Represents a reference to a file uploaded via the File Upload API.
 */
@Serializable
data class FileUploadReference(
    @SerialName("id")
    val id: String,
)

/**
 * Represents a custom emoji object with metadata.
 */
@Serializable
data class CustomEmojiObject(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("url")
    val url: String? = null,
)

/**
 * Represents a native Notion icon with a name and optional color.
 *
 * When color is omitted, Notion defaults to [NativeIconColor.GRAY].
 */
@Serializable
data class NativeIconObject(
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: NativeIconColor? = null,
)

/**
 * Response model for the GET /v1/custom_emojis endpoint.
 */
@Serializable
data class CustomEmojiList(
    @SerialName("results")
    override val results: List<CustomEmojiObject>,
    @SerialName("next_cursor")
    override val nextCursor: String? = null,
    @SerialName("has_more")
    override val hasMore: Boolean = false,
) : PaginatedResponse<CustomEmojiObject>
