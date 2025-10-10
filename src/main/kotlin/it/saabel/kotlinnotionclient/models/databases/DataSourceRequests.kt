package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for creating a new data source in an existing database (API version 2025-09-03+).
 *
 * This adds a new table/data source to an existing database container.
 * Each data source can have its own independent schema (properties).
 *
 * Note: The description parameter is not supported when adding a data source to an existing database.
 * It can only be set when creating the initial data source via the database creation endpoint.
 */
@Serializable
data class CreateDataSourceRequest(
    @SerialName("parent")
    val parent: Parent,
    @SerialName("properties")
    val properties: Map<String, CreateDatabaseProperty>,
    @SerialName("title")
    val title: List<RichText>? = null,
)

/**
 * Request model for updating an existing data source (API version 2025-09-03+).
 *
 * Allows updating:
 * - Properties (schema) - adding, removing, or modifying property definitions
 * - Title
 * - Description
 * - Archive status (in_trash)
 */
@Serializable
data class UpdateDataSourceRequest(
    @SerialName("properties")
    val properties: Map<String, CreateDatabaseProperty>? = null,
    @SerialName("title")
    val title: List<RichText>? = null,
    @SerialName("description")
    val description: List<RichText>? = null,
    @SerialName("in_trash")
    val inTrash: Boolean? = null,
)
