package no.saabelit.kotlinnotionclient.models.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.models.users.User

/**
 * Base interface for all Notion objects.
 *
 * All objects in the Notion API share these common properties.
 */
interface NotionObject {
    val id: String
    val objectType: String
    val createdTime: String
    val lastEditedTime: String
    val createdBy: User
    val lastEditedBy: User
    val archived: Boolean
}

/**
 * Represents a parent reference in Notion.
 *
 * Objects in Notion can have different types of parents.
 */
@Serializable
data class Parent(
    @SerialName("type")
    val type: String,
    @SerialName("database_id")
    val databaseId: String? = null,
    @SerialName("page_id")
    val pageId: String? = null,
    @SerialName("block_id")
    val blockId: String? = null,
    @SerialName("workspace")
    val workspace: Boolean? = null,
)

/**
 * Represents rich text content in Notion.
 *
 * Rich text is used throughout Notion for formatted text content.
 */
@Serializable
data class RichText(
    @SerialName("type")
    val type: String,
    @SerialName("text")
    val text: TextContent? = null,
    @SerialName("mention")
    val mention: Mention? = null,
    @SerialName("equation")
    val equation: Equation? = null,
    @SerialName("annotations")
    val annotations: Annotations,
    @SerialName("plain_text")
    val plainText: String,
    @SerialName("href")
    val href: String? = null,
)

/**
 * Represents the text content of a rich text element.
 */
@Serializable
data class TextContent(
    @SerialName("content")
    val content: String,
    @SerialName("link")
    val link: Link? = null,
)

/**
 * Represents a link in rich text.
 */
@Serializable
data class Link(
    @SerialName("url")
    val url: String,
)

/**
 * Represents text formatting annotations.
 */
@Serializable
data class Annotations(
    @SerialName("bold")
    val bold: Boolean = false,
    @SerialName("italic")
    val italic: Boolean = false,
    @SerialName("strikethrough")
    val strikethrough: Boolean = false,
    @SerialName("underline")
    val underline: Boolean = false,
    @SerialName("code")
    val code: Boolean = false,
    @SerialName("color")
    val color: String = "default",
)

/**
 * Represents a mention in rich text.
 */
@Serializable
sealed class Mention {
    @Serializable
    @SerialName("user")
    data class User(
        @SerialName("user")
        val user: no.saabelit.kotlinnotionclient.models.users.User,
    ) : Mention()

    @Serializable
    @SerialName("date")
    data class Date(
        @SerialName("date")
        val date: DateObject,
    ) : Mention()

    @Serializable
    @SerialName("page")
    data class Page(
        @SerialName("page")
        val page: PageReference,
    ) : Mention()

    @Serializable
    @SerialName("database")
    data class Database(
        @SerialName("database")
        val database: DatabaseReference,
    ) : Mention()
}

/**
 * Represents an equation in rich text.
 */
@Serializable
data class Equation(
    @SerialName("expression")
    val expression: String,
)

/**
 * Represents a date object.
 */
@Serializable
data class DateObject(
    @SerialName("start")
    val start: String,
    @SerialName("end")
    val end: String? = null,
    @SerialName("time_zone")
    val timeZone: String? = null,
)

/**
 * Represents a page reference.
 */
@Serializable
data class PageReference(
    @SerialName("id")
    val id: String,
)

/**
 * Represents a database reference.
 */
@Serializable
data class DatabaseReference(
    @SerialName("id")
    val id: String,
)
