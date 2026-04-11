@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.NotionObject
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.datasources.DataSourceRef
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.PageIcon
import it.saabel.kotlinnotionclient.models.users.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents a database in Notion (API version 2025-09-03+).
 *
 * As of the 2025-09-03 API version, databases are containers that can hold
 * multiple data sources (tables), each with their own schema (properties).
 * The database object now includes a list of data source references, and
 * the properties field is deprecated in favor of accessing properties through
 * the individual data sources.
 */
@Serializable
data class Database(
    @SerialName("id")
    override val id: String,
    @SerialName("created_time")
    override val createdTime: String,
    @SerialName("last_edited_time")
    override val lastEditedTime: String,
    @SerialName("created_by")
    override val createdBy: User? = null,
    @SerialName("last_edited_by")
    override val lastEditedBy: User? = null,
    @SerialName("title")
    val title: List<RichText>,
    @SerialName("description")
    val description: List<RichText> = emptyList(),
    @SerialName("icon")
    val icon: PageIcon? = null,
    @SerialName("cover")
    val cover: PageCover? = null,
    @SerialName("data_sources")
    val dataSources: List<DataSourceRef>,
    @SerialName("parent")
    val parent: Parent,
    // TODO: Verify if url should be nullable - official sample doesn't include it, but FAQ mentions database URLs exist
    // See: journal/2025_10_04_03_API_Model_Assumptions.md
    @SerialName("url")
    val url: String? = null,
    @SerialName("public_url")
    val publicUrl: String? = null,
    @SerialName("is_inline")
    val isInline: Boolean = false,
    @SerialName("in_trash")
    override val inTrash: Boolean = false,
) : NotionObject {
    @SerialName("object")
    override val objectType: String = "database"
}

/**
 * Represents a property definition in a database.
 *
 * Database properties define the schema and data types for columns in the database.
 * These are different from PagePropertyValue classes which represent actual values
 * that can be set when creating/updating pages.
 *
 * Naming convention:
 * - DatabaseProperty: Property definitions/schemas (this file)
 * - PagePropertyValue: Property values for requests (PageRequests.kt)
 * - base.RichText: Actual rich text content structure (base package)
 */
@Serializable
sealed class DatabaseProperty {
    abstract val id: String
    abstract val name: String
    abstract val type: String

    @Serializable
    @SerialName("title")
    data class Title(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("title")
        val title: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "title"
    }

    @Serializable
    @SerialName("rich_text")
    data class RichText(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("rich_text")
        val richText: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "rich_text"
    }

    @Serializable
    @SerialName("number")
    data class Number(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("number")
        val number: NumberFormat,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "number"
    }

    @Serializable
    @SerialName("select")
    data class Select(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("select")
        val select: SelectOptions,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "select"
    }

    @Serializable
    @SerialName("multi_select")
    data class MultiSelect(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("multi_select")
        val multiSelect: SelectOptions,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "multi_select"
    }

    @Serializable
    @SerialName("date")
    data class Date(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("date")
        val date: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "date"
    }

    @Serializable
    @SerialName("checkbox")
    data class Checkbox(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("checkbox")
        val checkbox: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "checkbox"
    }

    @Serializable
    @SerialName("url")
    data class Url(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("url")
        val url: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "url"
    }

    @Serializable
    @SerialName("email")
    data class Email(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("email")
        val email: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "email"
    }

    @Serializable
    @SerialName("phone_number")
    data class PhoneNumber(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("phone_number")
        val phoneNumber: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "phone_number"
    }

    @Serializable
    @SerialName("created_time")
    data class CreatedTime(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("created_time")
        val createdTime: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "created_time"
    }

    @Serializable
    @SerialName("created_by")
    data class CreatedBy(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("created_by")
        val createdBy: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "created_by"
    }

    @Serializable
    @SerialName("last_edited_time")
    data class LastEditedTime(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("last_edited_time")
        val lastEditedTime: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "last_edited_time"
    }

    @Serializable
    @SerialName("last_edited_by")
    data class LastEditedBy(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("last_edited_by")
        val lastEditedBy: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "last_edited_by"
    }

    @Serializable
    @SerialName("people")
    data class People(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("people")
        val people: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "people"
    }

    @Serializable
    @SerialName("relation")
    data class Relation(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("relation")
        val relation: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "relation"
    }

    @Serializable
    @SerialName("rollup")
    data class Rollup(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("rollup")
        val rollup: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "rollup"
    }

    @Serializable
    @SerialName("formula")
    data class Formula(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("formula")
        val formula: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "formula"
    }

    @Serializable
    @SerialName("files")
    data class Files(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("files")
        val files: JsonObject,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "files"
    }

    @Serializable
    @SerialName("status")
    data class Status(
        @SerialName("id")
        override val id: String,
        @SerialName("name")
        override val name: String,
        @SerialName("description")
        val description: String? = null,
        @SerialName("status")
        val status: StatusPropertyOptions,
    ) : DatabaseProperty() {
        @SerialName("type")
        override val type: String = "status"
    }
}

/**
 * Represents number formatting options.
 */
@Serializable
data class NumberFormat(
    @SerialName("format")
    val format: String,
)

/**
 * Represents select/multi-select options.
 */
@Serializable
data class SelectOptions(
    @SerialName("options")
    val options: List<SelectOption>,
)

/**
 * Represents a single select option.
 */
@Serializable
data class SelectOption(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: SelectOptionColor,
    @SerialName("description")
    val description: String? = null,
)

/**
 * Represents the options and groups for a status database property.
 */
@Serializable
data class StatusPropertyOptions(
    @SerialName("options")
    val options: List<SelectOption>,
    @SerialName("groups")
    val groups: List<StatusGroup>,
)

/**
 * Represents a group in a status property, collecting related options.
 */
@Serializable
data class StatusGroup(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: SelectOptionColor,
    @SerialName("option_ids")
    val optionIds: List<String>,
)
