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
