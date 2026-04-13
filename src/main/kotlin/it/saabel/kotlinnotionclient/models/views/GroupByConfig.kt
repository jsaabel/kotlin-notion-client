@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.views

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ---------------------------------------------------------------------------
// Shared sub-types
// ---------------------------------------------------------------------------

/** Sort order for group columns. Used in all [GroupByConfig] variants. */
@Serializable
enum class GroupSortType {
    @SerialName("manual")
    MANUAL,

    @SerialName("ascending")
    ASCENDING,

    @SerialName("descending")
    DESCENDING,
}

/** Sort configuration attached to a grouping. */
@Serializable
data class GroupSort(
    @SerialName("type")
    val type: GroupSortType,
)

/** How a status property is split into groups. */
@Serializable
enum class StatusGroupingMode {
    /** Each status group (To Do / In Progress / Done) becomes a column. */
    @SerialName("group")
    GROUP,

    /** Each individual status option becomes a column. */
    @SerialName("option")
    OPTION,
}

/** How a date property is bucketed into groups. */
@Serializable
enum class DateGroupingGranularity {
    @SerialName("relative")
    RELATIVE,

    @SerialName("day")
    DAY,

    @SerialName("week")
    WEEK,

    @SerialName("month")
    MONTH,

    @SerialName("year")
    YEAR,
}

/** How a text property is split into groups. */
@Serializable
enum class TextGroupingMode {
    /** Each distinct value becomes its own group. */
    @SerialName("exact")
    EXACT,

    /** Groups by the first letter of the value. */
    @SerialName("alphabet_prefix")
    ALPHABET_PREFIX,
}

// ---------------------------------------------------------------------------
// FormulaSubGroupBy — nested discriminated union inside Formula group-by
// ---------------------------------------------------------------------------

/**
 * Sub-grouping configuration for formula properties.
 *
 * The formula result type determines which variant is used:
 * - date formula  → [Date]
 * - text formula  → [Text]
 * - number formula → [Number]
 * - checkbox formula → [Checkbox]
 */
@Serializable(with = FormulaSubGroupBySerializer::class)
sealed class FormulaSubGroupBy {
    @Serializable
    data class Date(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "date",
        @SerialName("group_by") val groupBy: DateGroupingGranularity,
        @SerialName("sort") val sort: GroupSort,
        /** 0 = Sunday, 1 = Monday. */
        @SerialName("start_day_of_week") val startDayOfWeek: Int? = null,
    ) : FormulaSubGroupBy()

    @Serializable
    data class Text(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "text",
        @SerialName("group_by") val groupBy: TextGroupingMode,
        @SerialName("sort") val sort: GroupSort,
    ) : FormulaSubGroupBy()

    @Serializable
    data class Number(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "number",
        @SerialName("sort") val sort: GroupSort,
        @SerialName("range_start") val rangeStart: Int? = null,
        @SerialName("range_end") val rangeEnd: Int? = null,
        /** Minimum: 1. */
        @SerialName("range_size") val rangeSize: Int? = null,
    ) : FormulaSubGroupBy()

    @Serializable
    data class Checkbox(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "checkbox",
        @SerialName("sort") val sort: GroupSort,
    ) : FormulaSubGroupBy()

    /** Catch-all for unrecognised formula sub-group-by types. */
    data class Unknown(
        val rawContent: JsonElement,
    ) : FormulaSubGroupBy()
}

/** Custom serializer for [FormulaSubGroupBy]. Dispatches on the `"type"` field. */
object FormulaSubGroupBySerializer : KSerializer<FormulaSubGroupBy> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FormulaSubGroupBy")

    override fun deserialize(decoder: Decoder): FormulaSubGroupBy {
        require(decoder is JsonDecoder) { "FormulaSubGroupBySerializer can only deserialize JSON" }
        val element = decoder.decodeJsonElement()
        val type =
            element.jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in FormulaSubGroupBy JSON")
        return when (type) {
            "date" -> decoder.json.decodeFromJsonElement(FormulaSubGroupBy.Date.serializer(), element)
            "text" -> decoder.json.decodeFromJsonElement(FormulaSubGroupBy.Text.serializer(), element)
            "number" -> decoder.json.decodeFromJsonElement(FormulaSubGroupBy.Number.serializer(), element)
            "checkbox" -> decoder.json.decodeFromJsonElement(FormulaSubGroupBy.Checkbox.serializer(), element)
            else -> FormulaSubGroupBy.Unknown(rawContent = element)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: FormulaSubGroupBy,
    ) {
        require(encoder is JsonEncoder) { "FormulaSubGroupBySerializer can only serialize JSON" }
        val element: JsonElement =
            when (value) {
                is FormulaSubGroupBy.Date -> {
                    encoder.json.encodeToJsonElement(FormulaSubGroupBy.Date.serializer(), value)
                }

                is FormulaSubGroupBy.Text -> {
                    encoder.json.encodeToJsonElement(FormulaSubGroupBy.Text.serializer(), value)
                }

                is FormulaSubGroupBy.Number -> {
                    encoder.json.encodeToJsonElement(FormulaSubGroupBy.Number.serializer(), value)
                }

                is FormulaSubGroupBy.Checkbox -> {
                    encoder.json.encodeToJsonElement(FormulaSubGroupBy.Checkbox.serializer(), value)
                }

                is FormulaSubGroupBy.Unknown -> {
                    value.rawContent
                }
            }
        encoder.encodeJsonElement(element)
    }
}

// ---------------------------------------------------------------------------
// GroupByConfig — main discriminated union
// ---------------------------------------------------------------------------

/**
 * Grouping configuration for view columns (board, table sub-grouping, chart axes).
 *
 * The property type determines which variant is used:
 *
 * | Variant | `type` values |
 * |---------|---------------|
 * | [Select] | `select`, `multi_select` |
 * | [Status] | `status` |
 * | [Person] | `person`, `created_by`, `last_edited_by` |
 * | [Relation] | `relation` |
 * | [Date] | `date`, `created_time`, `last_edited_time` |
 * | [Text] | `text`, `title`, `url`, `email`, `phone_number` |
 * | [Number] | `number` |
 * | [Checkbox] | `checkbox` |
 * | [Formula] | `formula` |
 *
 * ## Common fields
 * All concrete variants have:
 * - `type` — the property type string (required, always encoded)
 * - `propertyId` — the property being grouped by
 * - `sort` — the group column sort order
 * - `hideEmptyGroups` — whether columns with no items are hidden (optional)
 * - `propertyName` — convenience name returned in responses (optional, response-only)
 */
@Serializable(with = GroupByConfigSerializer::class)
sealed class GroupByConfig {
    /**
     * Groups by a `select` or `multi_select` property.
     *
     * @param type Either `"select"` or `"multi_select"`.
     */
    @Serializable
    data class Select(
        @SerialName("type") val type: String,
        @SerialName("property_id") val propertyId: String,
        @SerialName("sort") val sort: GroupSort,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        @SerialName("property_name") val propertyName: String? = null,
    ) : GroupByConfig()

    /**
     * Groups by a `status` property.
     *
     * @param groupBy Whether to group by status group (To Do/In Progress/Done) or individual option.
     */
    @Serializable
    data class Status(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "status",
        @SerialName("property_id") val propertyId: String,
        @SerialName("group_by") val groupBy: StatusGroupingMode,
        @SerialName("sort") val sort: GroupSort,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        @SerialName("property_name") val propertyName: String? = null,
    ) : GroupByConfig()

    /**
     * Groups by a `person`, `created_by`, or `last_edited_by` property.
     *
     * @param type One of `"person"`, `"created_by"`, `"last_edited_by"`.
     */
    @Serializable
    data class Person(
        @SerialName("type") val type: String,
        @SerialName("property_id") val propertyId: String,
        @SerialName("sort") val sort: GroupSort,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        @SerialName("property_name") val propertyName: String? = null,
    ) : GroupByConfig()

    /**
     * Groups by a `relation` property.
     */
    @Serializable
    data class Relation(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "relation",
        @SerialName("property_id") val propertyId: String,
        @SerialName("sort") val sort: GroupSort,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        @SerialName("property_name") val propertyName: String? = null,
    ) : GroupByConfig()

    /**
     * Groups by a `date`, `created_time`, or `last_edited_time` property.
     *
     * @param type One of `"date"`, `"created_time"`, `"last_edited_time"`.
     * @param groupBy The time bucket granularity (day, week, month, etc.).
     * @param startDayOfWeek 0 = Sunday, 1 = Monday. Only relevant for week grouping.
     */
    @Serializable
    data class Date(
        @SerialName("type") val type: String,
        @SerialName("property_id") val propertyId: String,
        @SerialName("group_by") val groupBy: DateGroupingGranularity,
        @SerialName("sort") val sort: GroupSort,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        /** 0 = Sunday, 1 = Monday. */
        @SerialName("start_day_of_week") val startDayOfWeek: Int? = null,
        @SerialName("property_name") val propertyName: String? = null,
    ) : GroupByConfig()

    /**
     * Groups by a `text`, `title`, `url`, `email`, or `phone_number` property.
     *
     * @param type One of `"text"`, `"title"`, `"url"`, `"email"`, `"phone_number"`.
     * @param groupBy Whether to group by exact value or by first letter.
     */
    @Serializable
    data class Text(
        @SerialName("type") val type: String,
        @SerialName("property_id") val propertyId: String,
        @SerialName("group_by") val groupBy: TextGroupingMode,
        @SerialName("sort") val sort: GroupSort,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        @SerialName("property_name") val propertyName: String? = null,
    ) : GroupByConfig()

    /**
     * Groups a `number` property into numeric ranges.
     *
     * @param rangeStart Optional lower bound of the range bucket.
     * @param rangeEnd Optional upper bound of the range bucket.
     * @param rangeSize Optional bucket size (minimum: 1).
     */
    @Serializable
    data class Number(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "number",
        @SerialName("property_id") val propertyId: String,
        @SerialName("sort") val sort: GroupSort,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        @SerialName("range_start") val rangeStart: Int? = null,
        @SerialName("range_end") val rangeEnd: Int? = null,
        @SerialName("range_size") val rangeSize: Int? = null,
        @SerialName("property_name") val propertyName: String? = null,
    ) : GroupByConfig()

    /**
     * Groups by a `checkbox` property (checked / unchecked).
     */
    @Serializable
    data class Checkbox(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "checkbox",
        @SerialName("property_id") val propertyId: String,
        @SerialName("sort") val sort: GroupSort,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        @SerialName("property_name") val propertyName: String? = null,
    ) : GroupByConfig()

    /**
     * Groups by a `formula` property.
     *
     * The formula result type determines the [groupBy] sub-variant ([FormulaSubGroupBy]).
     */
    @Serializable
    data class Formula(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "formula",
        @SerialName("property_id") val propertyId: String,
        @SerialName("group_by") val groupBy: FormulaSubGroupBy,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        @SerialName("property_name") val propertyName: String? = null,
    ) : GroupByConfig()

    /** Catch-all for unrecognised group-by types. The raw JSON is preserved in [rawContent]. */
    data class Unknown(
        val rawContent: JsonElement,
    ) : GroupByConfig()
}

// ---------------------------------------------------------------------------
// GroupByConfigSerializer
// ---------------------------------------------------------------------------

/**
 * Custom serializer for [GroupByConfig]. Dispatches on the `"type"` field.
 *
 * Multiple `type` values map to the same subclass where the field structure is identical:
 * - `"select"` / `"multi_select"` → [GroupByConfig.Select]
 * - `"person"` / `"created_by"` / `"last_edited_by"` → [GroupByConfig.Person]
 * - `"date"` / `"created_time"` / `"last_edited_time"` → [GroupByConfig.Date]
 * - `"text"` / `"title"` / `"url"` / `"email"` / `"phone_number"` → [GroupByConfig.Text]
 */
object GroupByConfigSerializer : KSerializer<GroupByConfig> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GroupByConfig")

    override fun deserialize(decoder: Decoder): GroupByConfig {
        require(decoder is JsonDecoder) { "GroupByConfigSerializer can only deserialize JSON" }
        val element = decoder.decodeJsonElement()
        val type =
            element.jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in GroupByConfig JSON")
        return when (type) {
            "select", "multi_select" -> {
                decoder.json.decodeFromJsonElement(GroupByConfig.Select.serializer(), element)
            }

            "status" -> {
                decoder.json.decodeFromJsonElement(GroupByConfig.Status.serializer(), element)
            }

            "person", "created_by", "last_edited_by" -> {
                decoder.json.decodeFromJsonElement(GroupByConfig.Person.serializer(), element)
            }

            "relation" -> {
                decoder.json.decodeFromJsonElement(GroupByConfig.Relation.serializer(), element)
            }

            "date", "created_time", "last_edited_time" -> {
                decoder.json.decodeFromJsonElement(GroupByConfig.Date.serializer(), element)
            }

            "text", "title", "url", "email", "phone_number" -> {
                decoder.json.decodeFromJsonElement(GroupByConfig.Text.serializer(), element)
            }

            "number" -> {
                decoder.json.decodeFromJsonElement(GroupByConfig.Number.serializer(), element)
            }

            "checkbox" -> {
                decoder.json.decodeFromJsonElement(GroupByConfig.Checkbox.serializer(), element)
            }

            "formula" -> {
                decoder.json.decodeFromJsonElement(GroupByConfig.Formula.serializer(), element)
            }

            else -> {
                GroupByConfig.Unknown(rawContent = element)
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: GroupByConfig,
    ) {
        require(encoder is JsonEncoder) { "GroupByConfigSerializer can only serialize JSON" }
        val element: JsonElement =
            when (value) {
                is GroupByConfig.Select -> {
                    encoder.json.encodeToJsonElement(GroupByConfig.Select.serializer(), value)
                }

                is GroupByConfig.Status -> {
                    encoder.json.encodeToJsonElement(GroupByConfig.Status.serializer(), value)
                }

                is GroupByConfig.Person -> {
                    encoder.json.encodeToJsonElement(GroupByConfig.Person.serializer(), value)
                }

                is GroupByConfig.Relation -> {
                    encoder.json.encodeToJsonElement(GroupByConfig.Relation.serializer(), value)
                }

                is GroupByConfig.Date -> {
                    encoder.json.encodeToJsonElement(GroupByConfig.Date.serializer(), value)
                }

                is GroupByConfig.Text -> {
                    encoder.json.encodeToJsonElement(GroupByConfig.Text.serializer(), value)
                }

                is GroupByConfig.Number -> {
                    encoder.json.encodeToJsonElement(GroupByConfig.Number.serializer(), value)
                }

                is GroupByConfig.Checkbox -> {
                    encoder.json.encodeToJsonElement(GroupByConfig.Checkbox.serializer(), value)
                }

                is GroupByConfig.Formula -> {
                    encoder.json.encodeToJsonElement(GroupByConfig.Formula.serializer(), value)
                }

                is GroupByConfig.Unknown -> {
                    value.rawContent
                }
            }
        encoder.encodeJsonElement(element)
    }
}
