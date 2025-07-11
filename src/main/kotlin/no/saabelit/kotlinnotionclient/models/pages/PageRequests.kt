package no.saabelit.kotlinnotionclient.models.pages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.blocks.Block

/**
 * Request model for creating a new page.
 *
 * This model represents the data structure required to create a page
 * in Notion. Pages can be created as children of other pages or as
 * entries in databases.
 */
@Serializable
data class CreatePageRequest(
    @SerialName("parent")
    val parent: Parent,
    @SerialName("properties")
    val properties: Map<String, PagePropertyValue>,
    @SerialName("icon")
    val icon: PageIcon? = null,
    @SerialName("cover")
    val cover: PageCover? = null,
    @SerialName("children")
    val children: List<Block>? = null,
)

/**
 * Request model for archiving a page.
 *
 * Notion doesn't support true deletion - objects are archived instead.
 */
@Serializable
data class ArchivePageRequest(
    @SerialName("archived")
    val archived: Boolean = true,
)

/**
 * Request model for updating page properties.
 *
 * This allows updating properties, icon, cover, and archived status.
 */
@Serializable
data class UpdatePageRequest(
    @SerialName("properties")
    val properties: Map<String, PagePropertyValue>? = null,
    @SerialName("icon")
    val icon: PageIcon? = null,
    @SerialName("cover")
    val cover: PageCover? = null,
    @SerialName("archived")
    val archived: Boolean? = null,
)

/**
 * Page property values for requests.
 *
 * These models represent the actual values that can be set when creating
 * or updating pages. They are named with "Value" suffix to distinguish them from:
 * - DatabaseProperty (property definitions/schemas in databases)
 * - no.saabelit.kotlinnotionclient.models.base.RichText (the actual rich text content structure)
 *
 * Only includes writable properties (excludes read-only computed properties
 * like formula, rollup, created_time, etc.)
 */
@Serializable
sealed class PagePropertyValue {
    /**
     * Title property value - used for page titles and database title columns.
     */
    @Serializable
    @SerialName("title")
    data class TitleValue(
        @SerialName("title")
        val title: List<RichText>,
    ) : PagePropertyValue()

    /**
     * Rich text property value for formatted text content.
     * Note: Uses base.RichText for the actual rich text content structure.
     */
    @Serializable
    @SerialName("rich_text")
    data class RichTextValue(
        @SerialName("rich_text")
        val richText: List<no.saabelit.kotlinnotionclient.models.base.RichText>,
    ) : PagePropertyValue()

    /**
     * Number property value for numeric values.
     */
    @Serializable
    @SerialName("number")
    data class NumberValue(
        @SerialName("number")
        val number: Double?,
    ) : PagePropertyValue()

    /**
     * Checkbox property value for boolean values.
     */
    @Serializable
    @SerialName("checkbox")
    data class CheckboxValue(
        @SerialName("checkbox")
        val checkbox: Boolean,
    ) : PagePropertyValue()

    /**
     * URL property value for web links.
     */
    @Serializable
    @SerialName("url")
    data class UrlValue(
        @SerialName("url")
        val url: String?,
    ) : PagePropertyValue()

    /**
     * Email property value for email addresses.
     */
    @Serializable
    @SerialName("email")
    data class EmailValue(
        @SerialName("email")
        val email: String?,
    ) : PagePropertyValue()

    /**
     * Phone number property value.
     */
    @Serializable
    @SerialName("phone_number")
    data class PhoneNumberValue(
        @SerialName("phone_number")
        val phoneNumber: String?,
    ) : PagePropertyValue()

    /**
     * Select property value for single-choice dropdown.
     */
    @Serializable
    @SerialName("select")
    data class SelectValue(
        @SerialName("select")
        val select: SelectOption?,
    ) : PagePropertyValue()

    /**
     * Multi-select property value for multiple-choice dropdown.
     */
    @Serializable
    @SerialName("multi_select")
    data class MultiSelectValue(
        @SerialName("multi_select")
        val multiSelect: List<SelectOption>,
    ) : PagePropertyValue()

    /**
     * Status property value for status workflows.
     */
    @Serializable
    @SerialName("status")
    data class StatusValue(
        @SerialName("status")
        val status: StatusOption?,
    ) : PagePropertyValue()

    /**
     * Date property value for date or date range values.
     */
    @Serializable
    @SerialName("date")
    data class DateValue(
        @SerialName("date")
        val date: DateData?,
    ) : PagePropertyValue()

    /**
     * People property value for user references.
     */
    @Serializable
    @SerialName("people")
    data class PeopleValue(
        @SerialName("people")
        val people: List<UserReference>,
    ) : PagePropertyValue()

    /**
     * Files property value for file attachments.
     */
    @Serializable
    @SerialName("files")
    data class FilesValue(
        @SerialName("files")
        val files: List<FileObject>,
    ) : PagePropertyValue()

    /**
     * Relation property value for page references.
     */
    @Serializable
    @SerialName("relation")
    data class RelationValue(
        @SerialName("relation")
        val relation: List<PageReference>,
    ) : PagePropertyValue()
}

/**
 * Supporting data classes for page properties.
 */
@Serializable
data class SelectOption(
    @SerialName("id")
    val id: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: String = "default",
)

@Serializable
data class StatusOption(
    @SerialName("id")
    val id: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: String = "default",
)

@Serializable
data class DateData(
    @SerialName("start")
    val start: String,
    @SerialName("end")
    val end: String? = null,
    @SerialName("time_zone")
    val timeZone: String? = null,
)

@Serializable
data class UserReference(
    @SerialName("id")
    val id: String,
)

@Serializable
data class PageReference(
    @SerialName("id")
    val id: String,
)

@Serializable
sealed class FileObject {
    @Serializable
    @SerialName("external")
    data class External(
        @SerialName("name")
        val name: String,
        @SerialName("external")
        val external: ExternalFileUrl,
    ) : FileObject()

    @Serializable
    @SerialName("file")
    data class Uploaded(
        @SerialName("name")
        val name: String,
        @SerialName("file")
        val file: UploadedFileUrl,
    ) : FileObject()
}

@Serializable
data class ExternalFileUrl(
    @SerialName("url")
    val url: String,
)

@Serializable
data class UploadedFileUrl(
    @SerialName("url")
    val url: String,
    @SerialName("expiry_time")
    val expiryTime: String? = null,
)
