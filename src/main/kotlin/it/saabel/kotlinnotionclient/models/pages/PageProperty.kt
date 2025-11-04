@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.users.User
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Typed models for page properties as returned from the Notion API.
 *
 * ## Naming Convention
 * - `PageProperty` - Properties when **retrieving** pages from the API (this class)
 * - `PagePropertyValue` - Property values when **creating/updating** pages via the API
 * - `DatabaseProperty` - Database property schema definitions
 * - `CreateDatabaseProperty` - Property definitions when creating databases
 *
 * ## Structure Differences
 * API responses include metadata that requests don't:
 * ```json
 * {
 *   "id": "property_id",           // ← Metadata (response only)
 *   "type": "property_type",       // ← Metadata (response only)
 *   "{property_type}": { ... }     // ← Actual value
 * }
 * ```
 *
 * ## Usage
 * ```kotlin
 * // Instead of manual JSON parsing:
 * val scoreProperty = page.properties["Score"] as? JsonObject
 * val score = scoreProperty?.get("number")?.jsonPrimitive?.double ?: 0.0
 *
 * // Use type-safe property access:
 * val score = page.getProperty<PageProperty.Number>("Score")?.number ?: 0.0
 * // or with helper methods:
 * val score = page.getNumberProperty("Score") ?: 0.0
 * ```
 */
@Serializable(with = PagePropertySerializer::class)
sealed class PageProperty {
    abstract val id: String
    abstract val type: String

    @Serializable
    @SerialName("title")
    data class Title(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("title") val title: List<RichText>,
    ) : PageProperty() {
        /** Extract plain text from the title */
        val plainText: String get() = title.firstOrNull()?.plainText ?: ""
    }

    @Serializable
    @SerialName("rich_text")
    data class RichTextProperty(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("rich_text") val richText: List<RichText>,
    ) : PageProperty() {
        /** Extract plain text from rich text */
        val plainText: String get() = richText.joinToString("") { it.plainText }
    }

    @Serializable
    @SerialName("number")
    data class Number(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("number") val number: Double?,
    ) : PageProperty()

    @Serializable
    @SerialName("checkbox")
    data class Checkbox(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("checkbox") val checkbox: Boolean,
    ) : PageProperty()

    @Serializable
    @SerialName("url")
    data class Url(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("url") val url: String?,
    ) : PageProperty()

    @Serializable
    @SerialName("email")
    data class Email(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("email") val email: String?,
    ) : PageProperty()

    @Serializable
    @SerialName("phone_number")
    data class PhoneNumber(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("phone_number") val phoneNumber: String?,
    ) : PageProperty()

    @Serializable
    @SerialName("unique_id")
    data class UniqueId(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("unique_id") val uniqueId: UniqueIdValue?,
    ) : PageProperty() {
        /**
         * Returns the formatted unique ID string (e.g., "TEST-123" or "123" if no prefix).
         * Returns null if the unique_id value is not set.
         */
        val formattedId: String?
            get() =
                uniqueId?.let {
                    if (it.prefix != null) {
                        "${it.prefix}-${it.number}"
                    } else {
                        it.number.toString()
                    }
                }
    }

    @Serializable
    @SerialName("select")
    data class Select(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("select") val select: SelectOption?,
    ) : PageProperty()

    @Serializable
    @SerialName("multi_select")
    data class MultiSelect(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("multi_select") val multiSelect: List<SelectOption>,
    ) : PageProperty()

    @Serializable
    @SerialName("status")
    data class Status(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("status") val status: StatusOption?,
    ) : PageProperty()

    @Serializable
    @SerialName("date")
    data class Date(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("date") val date: DateData?,
    ) : PageProperty()

    @Serializable
    @SerialName("people")
    data class People(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("people") val people: List<User>,
    ) : PageProperty()

    @Serializable
    @SerialName("files")
    data class Files(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("files") val files: List<FileData>,
    ) : PageProperty()

    @Serializable
    @SerialName("relation")
    data class Relation(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("relation") val relation: List<PageReference>,
        @SerialName("has_more") val hasMore: Boolean = false,
    ) : PageProperty()

    @Serializable
    @SerialName("formula")
    data class Formula(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("formula") val formula: FormulaResult,
    ) : PageProperty()

    @Serializable
    @SerialName("rollup")
    data class Rollup(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("rollup") val rollup: RollupResult,
    ) : PageProperty()

    @Serializable
    @SerialName("created_time")
    data class CreatedTime(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("created_time") val createdTime: String,
    ) : PageProperty()

    @Serializable
    @SerialName("last_edited_time")
    data class LastEditedTime(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("last_edited_time") val lastEditedTime: String,
    ) : PageProperty()

    @Serializable
    @SerialName("created_by")
    data class CreatedBy(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("created_by") val createdBy: User,
    ) : PageProperty()

    @Serializable
    @SerialName("last_edited_by")
    data class LastEditedBy(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        @SerialName("last_edited_by") val lastEditedBy: User,
    ) : PageProperty()

    /**
     * Represents an unsupported or unknown property type.
     *
     * This type is used as a fallback when the Notion API returns a property type
     * that isn't explicitly supported by the client (e.g., "button", "unique_id",
     * "verification", etc.). This ensures forward compatibility as Notion adds new
     * property types.
     *
     * The raw JSON is preserved so users can inspect it or handle it manually.
     */
    @Serializable
    data class Unknown(
        @SerialName("id") override val id: String,
        @SerialName("type") override val type: String,
        val rawContent: JsonElement,
    ) : PageProperty()
}

/**
 * Formula result value - matches the structure from the API
 */
@Serializable
sealed class FormulaResult {
    @Serializable
    @SerialName("string")
    data class StringResult(
        @SerialName("type") val type: String,
        @SerialName("string") val string: String?,
    ) : FormulaResult()

    @Serializable
    @SerialName("number")
    data class NumberResult(
        @SerialName("type") val type: String,
        @SerialName("number") val number: Double?,
    ) : FormulaResult()

    @Serializable
    @SerialName("boolean")
    data class BooleanResult(
        @SerialName("type") val type: String,
        @SerialName("boolean") val boolean: Boolean?,
    ) : FormulaResult()

    @Serializable
    @SerialName("date")
    data class DateResult(
        @SerialName("type") val type: String,
        @SerialName("date") val date: DateData?,
    ) : FormulaResult()
}

/**
 * Rollup result value - matches the structure from the API
 */
@Serializable
sealed class RollupResult {
    @Serializable
    @SerialName("number")
    data class NumberResult(
        @SerialName("type") val type: String,
        @SerialName("number") val number: Double?,
        @SerialName("function") val function: String,
    ) : RollupResult()

    @Serializable
    @SerialName("date")
    data class DateResult(
        @SerialName("type") val type: String,
        @SerialName("date") val date: DateData?,
        @SerialName("function") val function: String,
    ) : RollupResult()

    @Serializable
    @SerialName("array")
    data class ArrayResult(
        @SerialName("type") val type: String,
        @SerialName("array") val array: List<PageProperty>,
        @SerialName("function") val function: String,
    ) : RollupResult()
}

/**
 * File data structure for files property responses
 */
@Serializable
sealed class FileData {
    abstract val name: String

    @Serializable
    @SerialName("external")
    data class External(
        @SerialName("name") override val name: String,
        @SerialName("type") val type: String,
        @SerialName("external") val external: ExternalFileUrl,
    ) : FileData()

    @Serializable
    @SerialName("file")
    data class Uploaded(
        @SerialName("name") override val name: String,
        @SerialName("type") val type: String,
        @SerialName("file") val file: UploadedFileUrl,
    ) : FileData()
}

// ========================================
// Convenience accessors for kotlinx-datetime
// ========================================

/** Returns the start date as LocalDate, or null if not set or parsing fails. */
val PageProperty.Date.localDateValue: kotlinx.datetime.LocalDate?
    get() =
        date?.start?.let {
            try {
                kotlinx.datetime.LocalDate.parse(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

/**
 * Returns the start datetime components as LocalDateTime, ignoring timezone information.
 *
 * This extracts the date/time components as shown in the ISO string, regardless of timezone:
 * - "2025-03-20T14:30:00Z" → LocalDateTime(2025, 3, 20, 14, 30)
 * - "2025-03-20T14:30:00+01:00" → LocalDateTime(2025, 3, 20, 14, 30)
 *
 * **Warning**: This loses timezone context. For timezone-aware conversion, use [toLocalDateTime].
 */
val PageProperty.Date.localDateTimeNaive: kotlinx.datetime.LocalDateTime?
    get() =
        date?.start?.let {
            try {
                val normalized =
                    it
                        .replace(Regex("\\.\\d+"), "") // Remove milliseconds .000
                        .removeSuffix("Z") // Remove UTC indicator
                        .replace(Regex("[+-]\\d{2}:\\d{2}$"), "") // Remove timezone offset
                kotlinx.datetime.LocalDateTime.parse(normalized)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

/** Returns the start instant as Instant, or null if not set or parsing fails. */
val PageProperty.Date.instantValue: Instant?
    get() =
        date?.start?.let {
            try {
                val normalized = it.replace(Regex("\\.\\d+"), "") // TODO: temp fix?
                Instant.parse(normalized)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

/**
 * Converts the start datetime to LocalDateTime in the specified timezone.
 *
 * This properly handles timezone conversion:
 * - "2025-03-20T14:30:00+01:00" in UTC → LocalDateTime(2025, 3, 20, 13, 30)
 * - "2025-03-20T14:30:00Z" in America/New_York → LocalDateTime(2025, 3, 20, 9, 30)
 *
 * Returns null if the start value is a date-only (no time component) or parsing fails.
 */
fun PageProperty.Date.toLocalDateTime(timeZone: kotlinx.datetime.TimeZone): kotlinx.datetime.LocalDateTime? =
    instantValue?.toLocalDateTime(timeZone)

/** Returns the end date as LocalDate, or null if not set or parsing fails. */
val PageProperty.Date.endLocalDateValue: kotlinx.datetime.LocalDate?
    get() =
        date?.end?.let {
            try {
                kotlinx.datetime.LocalDate.parse(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

/**
 * Returns the end datetime components as LocalDateTime, ignoring timezone information.
 *
 * This extracts the date/time components as shown in the ISO string, regardless of timezone.
 * **Warning**: This loses timezone context. For timezone-aware conversion, use [endToLocalDateTime].
 */
val PageProperty.Date.endLocalDateTimeNaive: kotlinx.datetime.LocalDateTime?
    get() =
        date?.end?.let {
            try {
                val normalized =
                    it
                        .replace(Regex("\\.\\d+"), "") // Remove milliseconds .000
                        .removeSuffix("Z") // Remove UTC indicator
                        .replace(Regex("[+-]\\d{2}:\\d{2}$"), "") // Remove timezone offset
                kotlinx.datetime.LocalDateTime.parse(normalized)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

/** Returns the end instant as Instant, or null if not set or parsing fails. */
val PageProperty.Date.endInstantValue: Instant?
    get() =
        date?.end?.let {
            try {
                val normalized = it.replace(Regex("\\.\\d+"), "")
                Instant.parse(normalized)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

/**
 * Converts the end datetime to LocalDateTime in the specified timezone.
 *
 * This properly handles timezone conversion for the range end value.
 * Returns null if the end value is not set, is date-only, or parsing fails.
 */
fun PageProperty.Date.endToLocalDateTime(timeZone: kotlinx.datetime.TimeZone): kotlinx.datetime.LocalDateTime? =
    endInstantValue?.toLocalDateTime(timeZone)

/** Returns the start date/datetime as a string (for backward compatibility). */
val PageProperty.Date.stringValue: String?
    get() = date?.start

/** Returns the end date/datetime as a string (for backward compatibility). */
val PageProperty.Date.endStringValue: String?
    get() = date?.end
