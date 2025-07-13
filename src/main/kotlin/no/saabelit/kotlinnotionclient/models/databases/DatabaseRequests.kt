package no.saabelit.kotlinnotionclient.models.databases

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.models.base.EmptyObject
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.pages.PageCover
import no.saabelit.kotlinnotionclient.models.pages.PageIcon

/**
 * Request model for creating a new database.
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
    @SerialName("properties")
    val properties: Map<String, CreateDatabaseProperty>,
    @SerialName("icon")
    val icon: PageIcon? = null,
    @SerialName("cover")
    val cover: PageCover? = null,
    @SerialName("description")
    val description: List<RichText>? = null,
)

/**
 * Request model for archiving a database.
 *
 * Notion doesn't support true deletion - objects are archived instead.
 */
@Serializable
data class ArchiveDatabaseRequest(
    @SerialName("archived")
    val archived: Boolean = true,
)

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
    ) : CreateDatabaseProperty()

    /**
     * Rich text property for formatted text content.
     */
    @Serializable
    @SerialName("rich_text")
    data class RichText(
        @SerialName("rich_text")
        val richText: EmptyObject = EmptyObject(),
    ) : CreateDatabaseProperty()

    /**
     * Number property with optional formatting.
     */
    @Serializable
    @SerialName("number")
    data class Number(
        @SerialName("number")
        val number: NumberConfiguration = NumberConfiguration(),
    ) : CreateDatabaseProperty()

    /**
     * Select property for single-choice dropdown.
     */
    @Serializable
    @SerialName("select")
    data class Select(
        @SerialName("select")
        val select: SelectConfiguration = SelectConfiguration(),
    ) : CreateDatabaseProperty()

    /**
     * Multi-select property for multiple-choice dropdown.
     */
    @Serializable
    @SerialName("multi_select")
    data class MultiSelect(
        @SerialName("multi_select")
        val multiSelect: SelectConfiguration = SelectConfiguration(),
    ) : CreateDatabaseProperty()

    /**
     * Date property for date/datetime values.
     */
    @Serializable
    @SerialName("date")
    data class Date(
        @SerialName("date")
        val date: EmptyObject = EmptyObject(),
    ) : CreateDatabaseProperty()

    /**
     * Checkbox property for boolean values.
     */
    @Serializable
    @SerialName("checkbox")
    data class Checkbox(
        @SerialName("checkbox")
        val checkbox: EmptyObject = EmptyObject(),
    ) : CreateDatabaseProperty()

    /**
     * URL property for web links.
     */
    @Serializable
    @SerialName("url")
    data class Url(
        @SerialName("url")
        val url: EmptyObject = EmptyObject(),
    ) : CreateDatabaseProperty()

    /**
     * Email property for email addresses.
     */
    @Serializable
    @SerialName("email")
    data class Email(
        @SerialName("email")
        val email: EmptyObject = EmptyObject(),
    ) : CreateDatabaseProperty()

    /**
     * Phone number property.
     */
    @Serializable
    @SerialName("phone_number")
    data class PhoneNumber(
        @SerialName("phone_number")
        val phoneNumber: EmptyObject = EmptyObject(),
    ) : CreateDatabaseProperty()

    /**
     * Relation property for linking to pages in another database.
     */
    @Serializable
    @SerialName("relation")
    data class Relation(
        @SerialName("relation")
        val relation: RelationConfiguration,
    ) : CreateDatabaseProperty()
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
 * Option for select/multi-select properties in creation requests.
 */
@Serializable
data class CreateSelectOption(
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: String = "default",
)

/**
 * Configuration for relation properties.
 *
 * Relation properties connect pages to other databases. The target database
 * must be shared with your integration for the relation to work.
 */
@Serializable
data class RelationConfiguration(
    @SerialName("database_id")
    val databaseId: String,
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
         * @return RelationConfiguration for a single property relation
         */
        fun singleProperty(databaseId: String): RelationConfiguration =
            RelationConfiguration(
                databaseId = databaseId,
                singleProperty = EmptyObject(),
            )

        /**
         * Creates a bidirectional relation with a specific synced property.
         *
         * @param databaseId The ID of the target database
         * @param syncedPropertyName The name of the property in the target database
         * @param syncedPropertyId The ID of the property in the target database (optional)
         * @return RelationConfiguration for a dual property relation
         */
        fun dualProperty(
            databaseId: String,
            syncedPropertyName: String,
            syncedPropertyId: String? = null,
        ): RelationConfiguration =
            RelationConfiguration(
                databaseId = databaseId,
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
         * @param syncedPropertyName The name of the synced property
         * @return RelationConfiguration for a synced relation
         */
        fun synced(
            databaseId: String,
            syncedPropertyName: String,
        ): RelationConfiguration =
            RelationConfiguration(
                databaseId = databaseId,
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
