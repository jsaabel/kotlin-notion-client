package it.saabel.kotlinnotionclient.models.base

import it.saabel.kotlinnotionclient.models.users.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val createdBy: User?
    val lastEditedBy: User?
    val archived: Boolean
}

/**
 * Represents a parent reference in Notion (API version 2025-09-03+).
 *
 * Objects in Notion can have different types of parents. This sealed class provides
 * type-safe access to parent information with compile-time guarantees.
 *
 * As of the 2025-09-03 API version:
 * - Pages should use [DataSourceParent] instead of database_id when parented by a database
 * - [DatabaseParent] is deprecated for page parents but still used in other contexts
 *
 * ## Example usage:
 * ```kotlin
 * when (page.parent) {
 *     is Parent.PageParent -> println("Parent page: ${page.parent.pageId}")
 *     is Parent.DataSourceParent -> println("Parent data source: ${page.parent.dataSourceId}")
 *     is Parent.DatabaseParent -> println("Parent database: ${page.parent.databaseId}")
 *     is Parent.BlockParent -> println("Parent block: ${page.parent.blockId}")
 *     is Parent.WorkspaceParent -> println("Parent is workspace")
 * }
 * ```
 */
@Serializable(with = ParentSerializer::class)
sealed class Parent {
    abstract val type: String

    /**
     * Convenience property to get the ID regardless of parent type.
     * Returns null for WorkspaceParent since it has no ID.
     *
     * This is useful when you only care about the ID value and not the specific parent type.
     *
     * ## Example:
     * ```kotlin
     * // Instead of:
     * val id = when (parent) {
     *     is Parent.PageParent -> parent.pageId
     *     is Parent.BlockParent -> parent.blockId
     *     // ...
     * }
     *
     * // You can simply use:
     * val id = parent.id
     * ```
     */
    val id: String?
        get() =
            when (this) {
                is PageParent -> this.pageId
                is DataSourceParent -> this.dataSourceId
                is DatabaseParent -> this.databaseId
                is BlockParent -> this.blockId
                is WorkspaceParent -> null
            }

    /**
     * Parent is a page.
     */
    @Serializable
    data class PageParent(
        @SerialName("page_id")
        val pageId: String,
    ) : Parent() {
        override val type: String = "page_id"
    }

    /**
     * Parent is a data source (table within a database) - API version 2025-09-03+.
     */
    @Serializable
    data class DataSourceParent(
        @SerialName("data_source_id")
        val dataSourceId: String,
    ) : Parent() {
        override val type: String = "data_source_id"
    }

    /**
     * Parent is a database (container).
     * Note: For page parents, prefer [DataSourceParent] in API version 2025-09-03+.
     */
    @Serializable
    data class DatabaseParent(
        @SerialName("database_id")
        val databaseId: String,
    ) : Parent() {
        override val type: String = "database_id"
    }

    /**
     * Parent is a block.
     */
    @Serializable
    data class BlockParent(
        @SerialName("block_id")
        val blockId: String,
    ) : Parent() {
        override val type: String = "block_id"
    }

    /**
     * Parent is the workspace root.
     */
    @Serializable
    data object WorkspaceParent : Parent() {
        override val type: String = "workspace"
    }
}

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
) {
    companion object {
        /**
         * Creates a simple RichText object from plain text.
         *
         * @param content The plain text content
         * @return A RichText object with default formatting
         */
        fun fromPlainText(content: String): RichText =
            RichText(
                type = "text",
                text =
                    TextContent(
                        content = content,
                        link = null,
                    ),
                annotations = Annotations(),
                plainText = content,
                href = null,
            )
    }
}

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
    val color: Color = Color.DEFAULT,
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
        val user: it.saabel.kotlinnotionclient.models.users.User,
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

/**
 * Empty object for conditions that don't need parameters.
 */
@Serializable
class EmptyObject
