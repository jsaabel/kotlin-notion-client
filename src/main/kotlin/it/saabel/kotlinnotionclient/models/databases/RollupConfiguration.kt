package it.saabel.kotlinnotionclient.models.databases

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Typed configuration of a rollup database property: which relation to walk, which
 * property of the related rows to read, and how to aggregate it.
 *
 * A rollup names its two targets **either by name or by id**. Notion echoes both back on
 * read; on write, supplying either side of a pair is enough (names are the readable
 * choice, ids survive renames).
 *
 * This class is shared between the read model ([DatabaseProperty.Rollup]) and the write
 * model ([CreateDatabaseProperty.Rollup]). Following the [FormulaConfiguration] precedent
 * it performs no validation itself, so anything Notion returns can be represented; the
 * write model validates at construction time.
 *
 * @property relationPropertyName Name of the relation property to walk
 * @property relationPropertyId Id of the relation property to walk
 * @property rollupPropertyName Name of the property read on the related rows
 * @property rollupPropertyId Id of the property read on the related rows
 * @property function How the collected values are aggregated
 */
@Serializable
data class RollupConfiguration(
    @SerialName("function")
    val function: RollupFunction,
    @SerialName("relation_property_name")
    val relationPropertyName: String? = null,
    @SerialName("relation_property_id")
    val relationPropertyId: String? = null,
    @SerialName("rollup_property_name")
    val rollupPropertyName: String? = null,
    @SerialName("rollup_property_id")
    val rollupPropertyId: String? = null,
) {
    /** The relation property reference, preferring the readable name over the id. */
    val relationReference: String?
        get() = relationPropertyName ?: relationPropertyId

    /** The rolled-up property reference, preferring the readable name over the id. */
    val rollupReference: String?
        get() = rollupPropertyName ?: rollupPropertyId
}

/**
 * The aggregation applied by a rollup property to the values it collects.
 *
 * Unrecognized values deserialize to [UNKNOWN] rather than failing the whole schema —
 * Notion has added rollup functions before (`count_per_group`, `percent_per_group`), and a
 * single new one must not break reading a data source. [UNKNOWN] is rejected on write.
 */
@Serializable(with = RollupFunctionSerializer::class)
enum class RollupFunction(
    val value: String,
) {
    AVERAGE("average"),
    CHECKED("checked"),
    COUNT("count"),
    COUNT_PER_GROUP("count_per_group"),
    COUNT_VALUES("count_values"),
    DATE_RANGE("date_range"),
    EARLIEST_DATE("earliest_date"),
    EMPTY("empty"),
    LATEST_DATE("latest_date"),
    MAX("max"),
    MEDIAN("median"),
    MIN("min"),
    NOT_EMPTY("not_empty"),
    PERCENT_CHECKED("percent_checked"),
    PERCENT_EMPTY("percent_empty"),
    PERCENT_NOT_EMPTY("percent_not_empty"),
    PERCENT_PER_GROUP("percent_per_group"),
    PERCENT_UNCHECKED("percent_unchecked"),
    RANGE("range"),
    SHOW_ORIGINAL("show_original"),
    SHOW_UNIQUE("show_unique"),
    SUM("sum"),
    UNCHECKED("unchecked"),
    UNIQUE("unique"),

    /** A rollup function this library does not know yet; carries no writable value. */
    UNKNOWN(""),
    ;

    companion object {
        private val byValue = entries.filter { it != UNKNOWN }.associateBy { it.value }

        /** Maps a raw rollup function string to a known value, or [UNKNOWN]. */
        fun fromValue(value: String): RollupFunction = byValue[value] ?: UNKNOWN
    }
}

/**
 * Serializes [RollupFunction] by its wire [RollupFunction.value], mapping unrecognized
 * strings to [RollupFunction.UNKNOWN] instead of throwing.
 */
internal object RollupFunctionSerializer : KSerializer<RollupFunction> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RollupFunction", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: RollupFunction,
    ) {
        require(value != RollupFunction.UNKNOWN) {
            "RollupFunction.UNKNOWN is a read-side fallback for a function this library does not " +
                "know yet and has no value to send. Pick a concrete function."
        }
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): RollupFunction = RollupFunction.fromValue(decoder.decodeString())
}

/**
 * Local checks for rollup configurations on the **write side**.
 *
 * Only structural problems are rejected: a missing or blank relation reference, a missing
 * or blank rolled-up property reference, and the read-only [RollupFunction.UNKNOWN]
 * sentinel. Whether Notion accepts the combination (does the named relation exist? is the
 * function applicable to the target property's type?) is not locally decidable from a
 * single property definition and is left to the API's `validation_error`.
 *
 * Follows the `NotionDateStrings` (#31) / `FormulaExpressions` (#42) precedent: pure
 * functions throwing [IllegalArgumentException] at construction/DSL time, so bad values
 * fail at the call site rather than in `RequestValidator`.
 */
internal object RollupConfigurations {
    /**
     * Validates [rollup], throwing [IllegalArgumentException] naming what is missing.
     * [propertyName], when known, names the schema property in the message.
     */
    fun validate(
        rollup: RollupConfiguration,
        propertyName: String? = null,
    ) {
        val subject = "Rollup property${propertyName?.let { " '$it'" } ?: ""}"
        require(rollup.function != RollupFunction.UNKNOWN) {
            "$subject uses RollupFunction.UNKNOWN, which is a read-side fallback for a function " +
                "this library does not know yet and cannot be written. Pick a concrete function."
        }
        require(!rollup.relationPropertyName.isNullOrBlank() || !rollup.relationPropertyId.isNullOrBlank()) {
            "$subject names no relation to roll up. Set relationPropertyName (or relationPropertyId) " +
                "to the relation property that links to the related data source."
        }
        require(!rollup.rollupPropertyName.isNullOrBlank() || !rollup.rollupPropertyId.isNullOrBlank()) {
            "$subject names no property to roll up. Set rollupPropertyName (or rollupPropertyId) " +
                "to the property read on the related rows."
        }
    }

    /**
     * Validates that every rollup in [properties] walks a relation defined in the same
     * schema, and that the named property is actually a relation. Only sound for
     * **create** requests, where the map is the complete schema; update requests may
     * legitimately reference properties that already exist on the data source but are
     * absent from the partial update, so they must not be checked this way.
     *
     * Only name references are checkable — an id reference points at a property id the
     * create request has not been assigned yet, so those are left to the API.
     */
    fun validateReferencesExist(properties: Map<String, CreateDatabaseProperty>) {
        properties.forEach { (name, property) ->
            if (property is CreateDatabaseProperty.Rollup) {
                val relation = property.rollup.relationPropertyName ?: return@forEach
                val target = properties[relation]
                require(target != null) {
                    "Rollup property '$name' rolls up the relation '$relation', but no property named " +
                        "'$relation' is defined in this schema. Defined properties: ${properties.keys.sorted()}"
                }
                require(target is CreateDatabaseProperty.Relation) {
                    "Rollup property '$name' rolls up '$relation', which is not a relation property. " +
                        "A rollup can only walk a relation."
                }
            }
        }
    }
}
