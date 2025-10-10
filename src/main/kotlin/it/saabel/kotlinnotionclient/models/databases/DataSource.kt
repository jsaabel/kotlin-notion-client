@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.NotionObject
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.users.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a data source in Notion (API version 2025-09-03+).
 *
 * Data sources are individual tables within a database container.
 * Each data source has its own schema (properties) and contains pages as rows.
 * A database can have multiple data sources, each with different property structures.
 */
@Serializable
data class DataSource(
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
    @SerialName("archived")
    override val archived: Boolean = false,
    @SerialName("title")
    val title: List<RichText>,
    @SerialName("description")
    val description: List<RichText>,
    @SerialName("properties")
    val properties: Map<String, DatabaseProperty>,
    @SerialName("parent")
    val parent: Parent,
    @SerialName("database_parent")
    val databaseParent: Parent? = null,
    @SerialName("url")
    val url: String,
    @SerialName("public_url")
    val publicUrl: String? = null,
    @SerialName("in_trash")
    val inTrash: Boolean = false,
) : NotionObject {
    @SerialName("object")
    override val objectType: String = "data_source"
}

/**
 * Reference to a data source, used in the Database model.
 *
 * This lightweight reference is included in the Database object's data_sources array.
 */
@Serializable
data class DataSourceRef(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
)
