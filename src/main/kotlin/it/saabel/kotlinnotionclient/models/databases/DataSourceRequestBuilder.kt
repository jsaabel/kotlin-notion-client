@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText

/**
 * Builder for creating data source requests (API version 2025-09-03+).
 *
 * This builder creates requests to add new data sources to existing databases.
 *
 * Note: The description parameter is not supported when adding a data source to an existing database.
 *
 * Example:
 * ```kotlin
 * val request = createDataSourceRequest {
 *     databaseId("existing-database-id")
 *     title("Projects Data Source")
 *     properties {
 *         title("Project Name")
 *         select("Status", "Not Started", "In Progress", "Completed")
 *         date("Due Date")
 *     }
 * }
 * ```
 */
@DslMarker
annotation class DataSourceRequestDslMarker

@DataSourceRequestDslMarker
class CreateDataSourceRequestBuilder {
    private var databaseIdValue: String? = null
    private var titleValue: List<RichText>? = null
    private val properties = mutableMapOf<String, CreateDatabaseProperty>()

    /**
     * Sets the database ID to add this data source to.
     *
     * @param id The database container ID
     */
    fun databaseId(id: String) {
        databaseIdValue = id
    }

    /**
     * Sets the title for the data source.
     *
     * @param text The title text
     */
    fun title(text: String) {
        titleValue = listOf(RichText.fromPlainText(text))
    }

    /**
     * Sets the title using rich text.
     *
     * @param richText List of RichText objects
     */
    fun title(richText: List<RichText>) {
        titleValue = richText
    }

    /**
     * Configures properties (schema) for the data source.
     *
     * @param block Configuration block for properties
     */
    fun properties(block: DatabasePropertiesBuilder.() -> Unit) {
        val builder = DatabasePropertiesBuilder()
        builder.block()
        properties.putAll(builder.build())
    }

    /**
     * Builds the CreateDataSourceRequest.
     *
     * @return The configured CreateDataSourceRequest
     * @throws IllegalStateException if database ID is not set or no properties defined
     */
    fun build(): CreateDataSourceRequest {
        require(databaseIdValue != null) { "Database ID must be specified" }
        require(properties.isNotEmpty()) { "Data source must have at least one property" }

        return CreateDataSourceRequest(
            parent = Parent(type = "database_id", databaseId = databaseIdValue),
            properties = properties,
            title = titleValue,
        )
    }
}

/**
 * Builder for updating data source requests (API version 2025-09-03+).
 *
 * This builder creates requests to update existing data sources.
 *
 * Example:
 * ```kotlin
 * val request = updateDataSourceRequest {
 *     title("Updated Project Tracker")
 *     properties {
 *         // Add new property
 *         number("Priority")
 *         // Modify existing (by recreating with same name)
 *         select("Status", "To Do", "Doing", "Done", "Blocked")
 *     }
 * }
 * ```
 */
@DataSourceRequestDslMarker
class UpdateDataSourceRequestBuilder {
    private var titleValue: List<RichText>? = null
    private var descriptionValue: List<RichText>? = null
    private var inTrashValue: Boolean? = null
    private val properties = mutableMapOf<String, CreateDatabaseProperty>()

    /**
     * Sets the title for the data source.
     *
     * @param text The title text
     */
    fun title(text: String) {
        titleValue = listOf(RichText.fromPlainText(text))
    }

    /**
     * Sets the title using rich text.
     *
     * @param richText List of RichText objects
     */
    fun title(richText: List<RichText>) {
        titleValue = richText
    }

    /**
     * Sets the description for the data source.
     *
     * @param text The description text
     */
    fun description(text: String) {
        descriptionValue = listOf(RichText.fromPlainText(text))
    }

    /**
     * Sets the description using rich text.
     *
     * @param richText List of RichText objects
     */
    fun description(richText: List<RichText>) {
        descriptionValue = richText
    }

    /**
     * Configures properties (schema) updates for the data source.
     *
     * Note: This replaces/updates the entire property set. To remove a property,
     * simply don't include it in this configuration.
     *
     * @param block Configuration block for properties
     */
    fun properties(block: DatabasePropertiesBuilder.() -> Unit) {
        val builder = DatabasePropertiesBuilder()
        builder.block()
        properties.putAll(builder.build())
    }

    /**
     * Archives the data source.
     */
    fun archive() {
        inTrashValue = true
    }

    /**
     * Unarchives the data source.
     */
    fun unarchive() {
        inTrashValue = false
    }

    /**
     * Builds the UpdateDataSourceRequest.
     *
     * @return The configured UpdateDataSourceRequest
     */
    fun build(): UpdateDataSourceRequest =
        UpdateDataSourceRequest(
            properties = if (properties.isNotEmpty()) properties else null,
            title = titleValue,
            description = descriptionValue,
            inTrash = inTrashValue,
        )
}

/**
 * Entry point function for the create data source request DSL.
 *
 * @param block Configuration block for the data source request
 * @return The configured CreateDataSourceRequest
 */
fun createDataSourceRequest(block: CreateDataSourceRequestBuilder.() -> Unit): CreateDataSourceRequest {
    val builder = CreateDataSourceRequestBuilder()
    builder.block()
    return builder.build()
}

/**
 * Entry point function for the update data source request DSL.
 *
 * @param block Configuration block for the update request
 * @return The configured UpdateDataSourceRequest
 */
fun updateDataSourceRequest(block: UpdateDataSourceRequestBuilder.() -> Unit): UpdateDataSourceRequest {
    val builder = UpdateDataSourceRequestBuilder()
    builder.block()
    return builder.build()
}
