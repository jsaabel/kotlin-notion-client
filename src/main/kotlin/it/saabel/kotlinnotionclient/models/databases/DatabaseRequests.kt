package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.EmptyObject
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.PageIcon
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for creating a new database (API version 2025-09-03+).
 *
 * As of 2025-09-03, creating a database creates both the database container
 * and its initial data source. Properties are now nested under initial_data_source.
 *
 * This model represents the data structure required to create a database
 * in Notion. It contains only the fields that are sent in the request,
 * not the computed fields returned in responses.
 */
@Serializable
data class CreateDatabaseRequest(
    @SerialName("parent")
    val parent: Parent,
    @SerialName("title")
    val title: List<RichText>,
    @SerialName("initial_data_source")
    val initialDataSource: InitialDataSource,
    @SerialName("icon")
    val icon: PageIcon? = null,
    @SerialName("cover")
    val cover: PageCover? = null,
    @SerialName("description")
    val description: List<RichText>? = null,
)

/**
 * Configuration for the initial data source when creating a database.
 *
 * This contains the properties (schema) for the first data source in the database.
 */
@Serializable
data class InitialDataSource(
    @SerialName("properties")
    val properties: Map<String, CreateDatabaseProperty>,
)

/**
 * Request model for archiving a database.
 *
 * Notion doesn't support true deletion - objects are moved to trash instead.
 * In the 2025-09-03 API, databases use "in_trash" field (not "archived").
 */
@Serializable
data class ArchiveDatabaseRequest(
    @SerialName("in_trash")
    val inTrash: Boolean = true,
)

private fun requirePropertyDescriptionLength(description: String?) {
    require(description == null || description.length <= 280) {
        "Property description must be 280 characters or fewer (was ${description!!.length})"
    }
}

/**
 * Property definitions for database creation requests.
 *
 * These are simpler than the response properties since they only contain
 * the configuration needed to create the property, not the metadata.
 */
@Serializable
sealed class CreateDatabaseProperty {
    /**
     * Title property - required for all databases.
     */
    @Serializable
    @SerialName("title")
    data class Title(
        @SerialName("title")
        val title: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Rich text property for formatted text content.
     */
    @Serializable
    @SerialName("rich_text")
    data class RichText(
        @SerialName("rich_text")
        val richText: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Number property with optional formatting.
     */
    @Serializable
    @SerialName("number")
    data class Number(
        @SerialName("number")
        val number: NumberConfiguration = NumberConfiguration(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Select property for single-choice dropdown.
     */
    @Serializable
    @SerialName("select")
    data class Select(
        @SerialName("select")
        val select: SelectConfiguration = SelectConfiguration(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Multi-select property for multiple-choice dropdown.
     */
    @Serializable
    @SerialName("multi_select")
    data class MultiSelect(
        @SerialName("multi_select")
        val multiSelect: SelectConfiguration = SelectConfiguration(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Date property for date/datetime values.
     */
    @Serializable
    @SerialName("date")
    data class Date(
        @SerialName("date")
        val date: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Checkbox property for boolean values.
     */
    @Serializable
    @SerialName("checkbox")
    data class Checkbox(
        @SerialName("checkbox")
        val checkbox: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * URL property for web links.
     */
    @Serializable
    @SerialName("url")
    data class Url(
        @SerialName("url")
        val url: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Email property for email addresses.
     */
    @Serializable
    @SerialName("email")
    data class Email(
        @SerialName("email")
        val email: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Phone number property.
     */
    @Serializable
    @SerialName("phone_number")
    data class PhoneNumber(
        @SerialName("phone_number")
        val phoneNumber: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * People property for user mentions.
     */
    @Serializable
    @SerialName("people")
    data class People(
        @SerialName("people")
        val people: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Status property for workflow-style statuses with options and groups.
     *
     * Pass an empty [StatusConfiguration] (the default) to let Notion create the standard options
     * ("Not started", "In progress", "Done") and groups ("To-do", "In progress", "Complete").
     * Custom initial options can be provided via [StatusConfiguration.options].
     *
     * Groups are auto-created by Notion and cannot be configured via the API. Only options
     * (name + color) can be specified at creation time. Use the Notion UI to reorganise options
     * into groups after creation.
     *
     * **Note**: Status properties cannot be updated via the API (unlike select/multi-select).
     */
    @Serializable
    @SerialName("status")
    data class Status(
        @SerialName("status")
        val status: StatusConfiguration = StatusConfiguration(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Relation property for linking to pages in another database.
     */
    @Serializable
    @SerialName("relation")
    data class Relation(
        @SerialName("relation")
        val relation: RelationConfiguration,
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }
}

/**
 * Configuration for number properties.
 */
@Serializable
data class NumberConfiguration(
    @SerialName("format")
    val format: String = "number",
)

/**
 * Configuration for select/multi-select properties.
 */
@Serializable
data class SelectConfiguration(
    @SerialName("options")
    val options: List<CreateSelectOption> = emptyList(),
)

/**
 * Option for select/multi-select/status properties in creation requests.
 */
@Serializable
data class CreateSelectOption(
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: SelectOptionColor = SelectOptionColor.DEFAULT,
    @SerialName("description")
    val description: String? = null,
)

/**
 * Configuration for status properties in creation requests.
 *
 * Only options (name + color) can be specified. Groups are auto-created by Notion
 * and cannot be configured via the API.
 */
@Serializable
data class StatusConfiguration(
    @SerialName("options")
    val options: List<CreateSelectOption> = emptyList(),
)

/**
 * Configuration for relation properties (API version 2025-09-03+).
 *
 * Relation properties connect pages to other databases/data sources.
 * The target must be shared with your integration for the relation to work.
 *
 * As of 2025-09-03:
 * - Both database_id and data_source_id should be provided when possible
 * - At minimum, provide data_source_id for proper targeting
 */
@Serializable
data class RelationConfiguration(
    @SerialName("database_id")
    val databaseId: String,
    @SerialName("data_source_id")
    val dataSourceId: String? = null,
    @SerialName("single_property")
    val singleProperty: EmptyObject? = null,
    @SerialName("dual_property")
    val dualProperty: DualPropertyConfiguration? = null,
    @SerialName("synced_property_name")
    val syncedPropertyName: String? = null,
    @SerialName("synced_property_id")
    val syncedPropertyId: String? = null,
) {
    companion object {
        /**
         * Creates a simple unidirectional relation to another database.
         *
         * @param databaseId The ID of the target database
         * @param dataSourceId The ID of the target data source
         * @return RelationConfiguration for a single property relation
         */
        fun singleProperty(
            databaseId: String,
            dataSourceId: String,
        ): RelationConfiguration =
            RelationConfiguration(
                databaseId = databaseId,
                dataSourceId = dataSourceId,
                singleProperty = EmptyObject(),
            )

        /**
         * Creates a bidirectional relation with a specific synced property.
         *
         * @param databaseId The ID of the target database
         * @param dataSourceId The ID of the target data source
         * @param syncedPropertyName The name of the property in the target database
         * @param syncedPropertyId The ID of the property in the target database (optional)
         * @return RelationConfiguration for a dual property relation
         */
        fun dualProperty(
            databaseId: String,
            dataSourceId: String,
            syncedPropertyName: String,
            syncedPropertyId: String? = null,
        ): RelationConfiguration =
            RelationConfiguration(
                databaseId = databaseId,
                dataSourceId = dataSourceId,
                dualProperty =
                    DualPropertyConfiguration(
                        syncedPropertyName = syncedPropertyName,
                        syncedPropertyId = syncedPropertyId,
                    ),
            )

        /**
         * Creates a simple synced relation (legacy format).
         *
         * @param databaseId The ID of the target database
         * @param dataSourceId The ID of the target data source
         * @param syncedPropertyName The name of the synced property
         * @return RelationConfiguration for a synced relation
         */
        fun synced(
            databaseId: String,
            dataSourceId: String,
            syncedPropertyName: String,
        ): RelationConfiguration =
            RelationConfiguration(
                databaseId = databaseId,
                dataSourceId = dataSourceId,
                syncedPropertyName = syncedPropertyName,
            )
    }
}

/**
 * Configuration for dual/bidirectional relation properties.
 */
@Serializable
data class DualPropertyConfiguration(
    @SerialName("synced_property_name")
    val syncedPropertyName: String,
    @SerialName("synced_property_id")
    val syncedPropertyId: String? = null,
)
