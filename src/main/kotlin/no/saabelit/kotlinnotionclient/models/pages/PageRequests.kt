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
    ) : PagePropertyValue() {
        companion object {
            /**
             * Creates a title value from plain text.
             *
             * @param text The title text
             * @return TitleValue with simple rich text
             */
            fun fromPlainText(text: String): TitleValue =
                TitleValue(
                    title =
                        listOf(
                            no.saabelit.kotlinnotionclient.models.requests.RequestBuilders
                                .createSimpleRichText(text),
                        ),
                )
        }
    }

    /**
     * Rich text property value for formatted text content.
     * Note: Uses base.RichText for the actual rich text content structure.
     */
    @Serializable
    @SerialName("rich_text")
    data class RichTextValue(
        @SerialName("rich_text")
        val richText: List<no.saabelit.kotlinnotionclient.models.base.RichText>,
    ) : PagePropertyValue() {
        companion object {
            /**
             * Creates a rich text value from plain text.
             *
             * @param text The text content
             * @return RichTextValue with simple rich text
             */
            fun fromPlainText(text: String): RichTextValue =
                RichTextValue(
                    richText =
                        listOf(
                            no.saabelit.kotlinnotionclient.models.requests.RequestBuilders
                                .createSimpleRichText(text),
                        ),
                )
        }
    }

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
    ) : PagePropertyValue() {
        companion object {
            /**
             * Creates a select value by option name.
             *
             * @param name The option name
             * @return SelectValue with option
             */
            fun byName(name: String): SelectValue = SelectValue(select = SelectOption(name = name))
        }
    }

    /**
     * Multi-select property value for multiple-choice dropdown.
     */
    @Serializable
    @SerialName("multi_select")
    data class MultiSelectValue(
        @SerialName("multi_select")
        val multiSelect: List<SelectOption>,
    ) : PagePropertyValue() {
        companion object {
            /**
             * Creates a multi-select value by option names.
             *
             * @param names The option names
             * @return MultiSelectValue with options
             */
            fun byNames(vararg names: String): MultiSelectValue = MultiSelectValue(multiSelect = names.map { SelectOption(name = it) })

            /**
             * Creates a multi-select value by option names list.
             *
             * @param names The option names
             * @return MultiSelectValue with options
             */
            fun byNames(names: List<String>): MultiSelectValue = MultiSelectValue(multiSelect = names.map { SelectOption(name = it) })
        }
    }

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
    ) : PagePropertyValue() {
        companion object {
            /**
             * Creates a date value from a date string.
             *
             * @param dateString The date string in ISO format (YYYY-MM-DD)
             * @return DateValue with date
             */
            fun fromDateString(dateString: String): DateValue = DateValue(date = DateData(start = dateString))

            /**
             * Creates a datetime value from a datetime string.
             *
             * @param datetimeString The datetime string in ISO format (YYYY-MM-DDTHH:MM:SS or with timezone)
             * @return DateValue with datetime
             */
            fun fromDateTimeString(datetimeString: String): DateValue = DateValue(date = DateData(start = datetimeString))

            /**
             * Creates a date range value.
             *
             * @param startDate The start date string in ISO format (YYYY-MM-DD)
             * @param endDate The end date string in ISO format (YYYY-MM-DD)
             * @return DateValue with date range
             */
            fun fromDateRange(
                startDate: String,
                endDate: String,
            ): DateValue = DateValue(date = DateData(start = startDate, end = endDate))

            /**
             * Creates a datetime range value.
             *
             * @param startDateTime The start datetime string in ISO format
             * @param endDateTime The end datetime string in ISO format
             * @return DateValue with datetime range
             */
            fun fromDateTimeRange(
                startDateTime: String,
                endDateTime: String,
            ): DateValue = DateValue(date = DateData(start = startDateTime, end = endDateTime))

            /**
             * Creates a date value with timezone.
             *
             * @param dateString The date string in ISO format (YYYY-MM-DD)
             * @param timeZone The timezone (e.g., "America/Los_Angeles", "UTC")
             * @return DateValue with date and timezone
             */
            fun fromDateWithTimeZone(
                dateString: String,
                timeZone: String,
            ): DateValue = DateValue(date = DateData(start = dateString, timeZone = timeZone))

            /**
             * Creates a datetime value with timezone.
             *
             * @param datetimeString The datetime string in ISO format
             * @param timeZone The timezone (e.g., "America/Los_Angeles", "UTC")
             * @return DateValue with datetime and timezone
             */
            fun fromDateTimeWithTimeZone(
                datetimeString: String,
                timeZone: String,
            ): DateValue = DateValue(date = DateData(start = datetimeString, timeZone = timeZone))
        }
    }

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
