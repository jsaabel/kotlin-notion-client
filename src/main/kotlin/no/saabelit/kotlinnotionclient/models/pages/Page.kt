package no.saabelit.kotlinnotionclient.models.pages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import no.saabelit.kotlinnotionclient.models.base.NotionObject
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.users.User

/**
 * Represents a page in Notion.
 *
 * Pages are the fundamental building blocks of a Notion workspace.
 * They can contain text, media, and other blocks of content.
 */
@Serializable
data class Page(
    @SerialName("id")
    override val id: String,
    @SerialName("created_time")
    override val createdTime: String,
    @SerialName("last_edited_time")
    override val lastEditedTime: String,
    @SerialName("created_by")
    override val createdBy: User,
    @SerialName("last_edited_by")
    override val lastEditedBy: User,
    @SerialName("archived")
    override val archived: Boolean,
    @SerialName("parent")
    val parent: Parent,
    @SerialName("properties")
    val properties: JsonObject,
    @SerialName("url")
    val url: String,
    @SerialName("public_url")
    val publicUrl: String? = null,
    @SerialName("icon")
    val icon: PageIcon? = null,
    @SerialName("cover")
    val cover: PageCover? = null,
    @SerialName("in_trash")
    val inTrash: Boolean = false,
) : NotionObject {
    @SerialName("object")
    override val objectType: String = "page"
}

/**
 * Represents an icon for a page.
 */
@Serializable
data class PageIcon(
    @SerialName("type")
    val type: String,
    @SerialName("emoji")
    val emoji: String? = null,
    @SerialName("url")
    val url: String? = null,
    @SerialName("expiry_time")
    val expiryTime: String? = null,
)

/**
 * Represents a cover image for a page.
 */
@Serializable
data class PageCover(
    @SerialName("type")
    val type: String,
    @SerialName("url")
    val url: String,
    @SerialName("expiry_time")
    val expiryTime: String? = null,
)

/**
 * Represents the title of a page.
 */
@Serializable
data class PageTitle(
    @SerialName("title")
    val title: List<RichText>,
)
