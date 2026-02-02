@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.datasources

import it.saabel.kotlinnotionclient.models.base.NotionObject
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.databases.DatabaseProperty
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

/**
 * Represents a template available for a data source.
 *
 * Templates allow creating pages with pre-populated content and structure.
 * Template application is asynchronous - the API returns immediately with a blank page,
 * and content is populated in the background.
 */
@Serializable
data class Template(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("is_default")
    val isDefault: Boolean,
)

/**
 * Response from listing templates for a data source.
 *
 * Returned by GET /v1/data_sources/{data_source_id}/templates
 */
@Serializable
data class TemplatesResponse(
    @SerialName("object")
    val objectType: String = "list",
    @SerialName("templates")
    val templates: List<Template>,
    @SerialName("has_more")
    val hasMore: Boolean,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
)
