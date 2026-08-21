@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.users.User
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Instant

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
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("title") val title: List<RichText>,
    ) : PageProperty() {
        /** Extract plain text from the title */
        val plainText: String get() = title.firstOrNull()?.plainText ?: ""
    }

    @Serializable
    @SerialName("rich_text")
    data class RichTextProperty(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("rich_text") val richText: List<RichText>,
    ) : PageProperty() {
        /** Extract plain text from rich text */
        val plainText: String get() = richText.joinToString("") { it.plainText }
    }

    @Serializable
    @SerialName("number")
    data class Number(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("number") val number: Double?,
    ) : PageProperty()

    @Serializable
    @SerialName("checkbox")
    data class Checkbox(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("checkbox") val checkbox: Boolean,
    ) : PageProperty()

    @Serializable
    @SerialName("url")
    data class Url(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("url") val url: String?,
    ) : PageProperty()

    @Serializable
    @SerialName("email")
    data class Email(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("email") val email: String?,
    ) : PageProperty()

    @Serializable
    @SerialName("phone_number")
    data class PhoneNumber(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("phone_number") val phoneNumber: String?,
    ) : PageProperty()

    @Serializable
    @SerialName("unique_id")
    data class UniqueId(
        @SerialName("id") override val id: String = "",
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
    @SerialName("place")
    data class Place(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("place") val place: PlaceValue?,
    ) : PageProperty() {
        /**
         * Returns the formatted location string with coordinates (e.g., "Oslo Airport (60.19116, 11.10242)").
         * Returns null if the place value is not set.
         */
        val formattedLocation: String?
            get() =
                place?.let {
                    buildString {
                        if (it.name != null) {
                            append(it.name)
                        }
                        if (it.lat != null && it.lon != null) {
                            if (it.name != null) append(" ")
                            append("(${it.lat}, ${it.lon})")
                        }
                    }.takeIf { it.isNotEmpty() }
                }
    }

    @Serializable
    @SerialName("select")
    data class Select(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("select") val select: SelectOption?,
    ) : PageProperty()

    @Serializable
    @SerialName("multi_select")
    data class MultiSelect(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("multi_select") val multiSelect: List<SelectOption>,
    ) : PageProperty()

    @Serializable
    @SerialName("status")
    data class Status(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("status") val status: StatusOption?,
    ) : PageProperty()

    @Serializable
    @SerialName("date")
    data class Date(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("date") val date: DateData?,
    ) : PageProperty()

    @Serializable
    @SerialName("people")
    data class People(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("people") val people: List<User>,
    ) : PageProperty()

    @Serializable
    @SerialName("files")
    data class Files(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("files") val files: List<FileData>,
    ) : PageProperty()

    @Serializable
    @SerialName("relation")
    data class Relation(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("relation") val relation: List<PageReference>,
        @SerialName("has_more") val hasMore: Boolean = false,
    ) : PageProperty()

    @Serializable
    @SerialName("formula")
    data class Formula(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("formula") val formula: FormulaResult,
    ) : PageProperty()

    @Serializable
    @SerialName("rollup")
    data class Rollup(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("rollup") val rollup: RollupResult,
    ) : PageProperty()

    @Serializable
    @SerialName("created_time")
    data class CreatedTime(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("created_time") val createdTime: String,
    ) : PageProperty()

    @Serializable
    @SerialName("last_edited_time")
    data class LastEditedTime(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("last_edited_time") val lastEditedTime: String,
    ) : PageProperty()

    @Serializable
    @SerialName("created_by")
    data class CreatedBy(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("created_by") val createdBy: User,
    ) : PageProperty()

    @Serializable
    @SerialName("last_edited_by")
    data class LastEditedBy(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("last_edited_by") val lastEditedBy: User,
    ) : PageProperty()

    /**
     * Verification property — only available on pages in wiki databases.
     *
     * State is one of "verified", "unverified", or "expired" (when the end date is in the past).
     * [VerificationData.verifiedBy] is read-only and is automatically set by the API.
     */
    @Serializable
    @SerialName("verification")
    data class Verification(
        @SerialName("id") override val id: String = "",
        @SerialName("type") override val type: String,
        @SerialName("verification") val verification: VerificationData?,
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
        @SerialName("id") override val id: String = "",
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

/**
 * The verification state of a page in a wiki database.
 *
 * @property state One of "verified", "unverified", or "expired" (when [date]'s end is in the past).
 * @property verifiedBy The user who verified the page, or null. Read-only — set automatically by the API.
 * @property date Start (and optional end) of the verification period.
 */
@Serializable
data class VerificationData(
    @SerialName("state") val state: String,
    @SerialName("verified_by") val verifiedBy: User? = null,
    @SerialName("date") val date: DateData? = null,
)

// ========================================
// Convenience accessors for kotlinx-datetime
// ========================================

/*
 * Reading a Notion date value answers one of three genuinely different questions.
 * The accessor names below say which one is being asked, so a call site is readable
 * without opening this file:
 *
 * | Question                                     | Accessor                             |
 * |----------------------------------------------|--------------------------------------|
 * | "What does this say?"       (wall-clock)     | [wallClockDateTime]                  |
 * | "When did this happen?"     (absolute time)  | [utcInstant] / [requireUtcInstant]   |
 * | "What zone is this value in?" (stored offset)| [storedOffset]                       |
 *
 * Notion always returns a numeric offset for time-bearing values and never a named
 * `time_zone`, so [storedOffset] is the whole of the zone information a read value
 * carries. Every accessor has an `end…` twin covering the end of a date range.
 */

// ----------------------------------------
// Internal parsing of Notion date strings
// ----------------------------------------

/** Matches the UTC-offset suffix of an ISO datetime: `Z`, `+01`, `+0100` or `+01:00`. */
private val OFFSET_SUFFIX_REGEX = Regex("""(?:[Zz]|[+-]\d{2}(?::?\d{2})?)$""")

/**
 * Splits an ISO datetime into its local part and its offset suffix.
 *
 * Returns null for a date-only value (no `T`), and an offset of null for a datetime
 * that carries no offset at all.
 */
private fun splitDateTime(raw: String): Pair<String, String?>? {
    if (!raw.contains('T')) return null
    val offset = OFFSET_SUFFIX_REGEX.find(raw)?.value ?: return raw to null
    return raw.dropLast(offset.length) to offset
}

private fun parseUtcOffset(raw: String): kotlinx.datetime.UtcOffset? {
    if (raw.equals("Z", ignoreCase = true)) return kotlinx.datetime.UtcOffset.ZERO
    val sign = if (raw.startsWith('-')) -1 else 1
    val digits = raw.drop(1).replace(":", "")
    val hours = digits.take(2).toIntOrNull() ?: return null
    val minutes =
        when (digits.length) {
            2 -> 0
            4 -> digits.substring(2, 4).toIntOrNull() ?: return null
            else -> return null
        }
    return try {
        kotlinx.datetime.UtcOffset(hours = sign * hours, minutes = sign * minutes)
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun wallClockOf(raw: String?): kotlinx.datetime.LocalDateTime? {
    val local = raw?.let(::splitDateTime)?.first ?: return null
    return try {
        kotlinx.datetime.LocalDateTime.parse(local)
    } catch (e: IllegalArgumentException) {
        null
    }
}

private fun storedOffsetOf(raw: String?): kotlinx.datetime.UtcOffset? {
    val (local, offset) = raw?.let(::splitDateTime) ?: return null
    if (offset == null) return null
    // Only report an offset for a value whose local part actually parses.
    wallClockOf(local) ?: return null
    return parseUtcOffset(offset)
}

private fun utcInstantOf(raw: String?): Instant? {
    val local = wallClockOf(raw) ?: return null
    val offset = storedOffsetOf(raw) ?: return null
    return local.toInstant(offset)
}

private fun isDateOnly(raw: String): Boolean =
    !raw.contains('T') &&
        try {
            kotlinx.datetime.LocalDate.parse(raw)
            true
        } catch (e: IllegalArgumentException) {
            false
        }

/** Explains, in the terms of the caller's own data, why a value has no absolute instant. */
private fun missingInstantMessage(
    raw: String?,
    which: String,
): String =
    when {
        raw == null -> {
            "Date property has no $which value."
        }

        isDateOnly(raw) -> {
            "Date property $which value '$raw' is date-only and has no absolute instant. " +
                "Use localDateValue for date-only values."
        }

        wallClockOf(raw) == null -> {
            "Date property $which value '$raw' is not a valid ISO-8601 date or datetime."
        }

        else -> {
            "Date property $which value '$raw' carries no UTC offset, so its absolute instant is unknown. " +
                "Read wallClockDateTime for the digits as stored, or write the value back with an offset."
        }
    }

// ----------------------------------------
// Start of the value
// ----------------------------------------

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
 * The start value's **wall-clock digits** — what the value says, with its offset ignored.
 *
 * This is the right accessor for rendering a time to a human who is standing where the
 * value applies ("the set starts at 13:00"), and the wrong one for comparing two values
 * or for pushing to a system that stores absolute time — use [utcInstant] for those.
 *
 * - `"2025-03-20T14:30:00Z"` → `LocalDateTime(2025, 3, 20, 14, 30)`
 * - `"2025-03-20T14:30:00+01:00"` → `LocalDateTime(2025, 3, 20, 14, 30)`
 *
 * Returns null when there is no start value, when it is date-only, or when it does not parse.
 */
val PageProperty.Date.wallClockDateTime: kotlinx.datetime.LocalDateTime?
    get() = wallClockOf(date?.start)

/**
 * The start value's **stored UTC offset** — the zone information the value actually carries.
 *
 * Notion returns a numeric offset for every time-bearing value and never a named `time_zone`,
 * so this is the only way to ask "what zone is this value in?". Comparing it against
 * [offsetIn] is how a caller detects that a value has drifted (for example, that a writer
 * silently stored local times as UTC).
 *
 * - `"2025-03-20T14:30:00+01:00"` → `UtcOffset(hours = 1)`
 * - `"2025-03-20T14:30:00Z"` → `UtcOffset.ZERO`
 *
 * Returns null when there is no start value, when it is date-only, when it carries no offset,
 * or when it does not parse.
 */
val PageProperty.Date.storedOffset: kotlinx.datetime.UtcOffset?
    get() = storedOffsetOf(date?.start)

/**
 * The start value as an **absolute instant in UTC** — when this actually happened.
 *
 * Computed from the wall-clock digits and the value's own [storedOffset], so it is the right
 * accessor for comparing values or for pushing to a system that stores UTC.
 *
 * Returns null when there is no start value, when it is date-only, when it carries no offset
 * (an offset-less datetime has no knowable instant), or when it does not parse. Use
 * [requireUtcInstant] where a missing instant should be an error rather than an absence.
 */
val PageProperty.Date.utcInstant: Instant?
    get() = utcInstantOf(date?.start)

/**
 * The start value as an absolute instant in UTC, failing loudly instead of returning null.
 *
 * @throws IllegalArgumentException if the start value is absent, date-only, offset-less, or
 *   malformed. The message names the offending value and what to do about it.
 */
fun PageProperty.Date.requireUtcInstant(): Instant =
    utcInstant ?: throw IllegalArgumentException(missingInstantMessage(date?.start, "start"))

/**
 * The start value rendered as local time **in [timeZone]** — the same instant, different digits.
 *
 * - `"2025-03-20T14:30:00+01:00"` in `UTC` → `LocalDateTime(2025, 3, 20, 13, 30)`
 * - `"2025-03-20T14:30:00Z"` in `America/New_York` → `LocalDateTime(2025, 3, 20, 9, 30)`
 *
 * Returns null whenever [utcInstant] does.
 */
fun PageProperty.Date.localDateTimeIn(timeZone: kotlinx.datetime.TimeZone): kotlinx.datetime.LocalDateTime? =
    utcInstant?.toLocalDateTime(timeZone)

/**
 * The offset [timeZone] would have had at this value's **own wall-clock date and time** — the
 * offset the value is expected to carry if it was written as local time in that zone.
 *
 * Pair it with [storedOffset] to audit a value:
 * ```kotlin
 * val zone = TimeZone.of("Europe/Oslo")
 * if (prop.storedOffset != prop.offsetIn(zone)) {
 *     // this value is not the Oslo local time it is supposed to be
 * }
 * ```
 *
 * DST resolution follows kotlinx-datetime: for a wall-clock time that occurs twice the earlier
 * offset is used, and for one that does not exist the time is shifted forward by the gap.
 * Returns null when there is no parseable wall-clock start value.
 */
fun PageProperty.Date.offsetIn(timeZone: kotlinx.datetime.TimeZone): kotlinx.datetime.UtcOffset? =
    wallClockDateTime?.let { timeZone.offsetAt(it.toInstant(timeZone)) }

/** Returns the start date/datetime as the raw string Notion returned. */
val PageProperty.Date.stringValue: String?
    get() = date?.start

// ----------------------------------------
// End of the value (date ranges)
// ----------------------------------------

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
 * The end value's **wall-clock digits**, with its offset ignored. See [wallClockDateTime].
 */
val PageProperty.Date.endWallClockDateTime: kotlinx.datetime.LocalDateTime?
    get() = wallClockOf(date?.end)

/**
 * The end value's **stored UTC offset**. See [storedOffset].
 */
val PageProperty.Date.endStoredOffset: kotlinx.datetime.UtcOffset?
    get() = storedOffsetOf(date?.end)

/**
 * The end value as an **absolute instant in UTC**. See [utcInstant].
 */
val PageProperty.Date.endUtcInstant: Instant?
    get() = utcInstantOf(date?.end)

/**
 * The end value as an absolute instant in UTC, failing loudly instead of returning null.
 *
 * @throws IllegalArgumentException if the end value is absent, date-only, offset-less, or
 *   malformed. The message names the offending value and what to do about it.
 */
fun PageProperty.Date.requireEndUtcInstant(): Instant =
    endUtcInstant ?: throw IllegalArgumentException(missingInstantMessage(date?.end, "end"))

/**
 * The end value rendered as local time **in [timeZone]**. See [localDateTimeIn].
 */
fun PageProperty.Date.endLocalDateTimeIn(timeZone: kotlinx.datetime.TimeZone): kotlinx.datetime.LocalDateTime? =
    endUtcInstant?.toLocalDateTime(timeZone)

/**
 * The offset [timeZone] would have had at the end value's own wall-clock date and time.
 * See [offsetIn].
 */
fun PageProperty.Date.endOffsetIn(timeZone: kotlinx.datetime.TimeZone): kotlinx.datetime.UtcOffset? =
    endWallClockDateTime?.let { timeZone.offsetAt(it.toInstant(timeZone)) }

/** Returns the end date/datetime as the raw string Notion returned. */
val PageProperty.Date.endStringValue: String?
    get() = date?.end

// ----------------------------------------
// Deprecated aliases (behaviour unchanged)
// ----------------------------------------

@Deprecated(
    "Renamed: this returns the wall-clock digits as stored, which is now stated by the name.",
    ReplaceWith("wallClockDateTime"),
)
val PageProperty.Date.localDateTimeNaive: kotlinx.datetime.LocalDateTime?
    get() = wallClockDateTime

@Deprecated(
    "Renamed: this returns the wall-clock digits as stored, which is now stated by the name.",
    ReplaceWith("endWallClockDateTime"),
)
val PageProperty.Date.endLocalDateTimeNaive: kotlinx.datetime.LocalDateTime?
    get() = endWallClockDateTime

@Deprecated(
    "Renamed: this returns the absolute instant in UTC, which is now stated by the name.",
    ReplaceWith("utcInstant"),
)
val PageProperty.Date.instantValue: Instant?
    get() = utcInstant

@Deprecated(
    "Renamed: this returns the absolute instant in UTC, which is now stated by the name.",
    ReplaceWith("endUtcInstant"),
)
val PageProperty.Date.endInstantValue: Instant?
    get() = endUtcInstant

@Deprecated(
    "Renamed for symmetry with the other date accessors.",
    ReplaceWith("localDateTimeIn(timeZone)"),
)
fun PageProperty.Date.toLocalDateTime(timeZone: kotlinx.datetime.TimeZone): kotlinx.datetime.LocalDateTime? = localDateTimeIn(timeZone)

@Deprecated(
    "Renamed for symmetry with the other date accessors.",
    ReplaceWith("endLocalDateTimeIn(timeZone)"),
)
fun PageProperty.Date.endToLocalDateTime(timeZone: kotlinx.datetime.TimeZone): kotlinx.datetime.LocalDateTime? =
    endLocalDateTimeIn(timeZone)
