package it.saabel.kotlinnotionclient.models.datasources

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * One-or-many string values for select/status/multi_select filter conditions.
 *
 * Per the Notion 2026-04-17 changelog, `equals`/`does_not_equal` (select, status) and
 * `contains`/`does_not_contain` (multi_select) accept either a single string or an array of
 * strings under the same key — Notion did **not** introduce new `equals_any`/`contains_any` keys.
 *
 * The wire format is decided purely by size:
 * - exactly one value  → JSON string (e.g. `"equals": "X"`) — byte-identical to the legacy shape
 * - two or more values → JSON array of strings (e.g. `"equals": ["X","Y"]`)
 *
 * [raw] must be non-empty; an empty list throws [IllegalArgumentException].
 */
@Serializable(with = FilterValuesSerializer::class)
@JvmInline
value class FilterValues(
    val raw: List<String>,
) {
    init {
        require(raw.isNotEmpty()) { "FilterValues must contain at least one value" }
    }

    constructor(vararg values: String) : this(values.toList())
}

/**
 * Serializes [FilterValues] to a JSON string when there is exactly one value, or a JSON array of
 * strings when there are several. Deserialization accepts both a bare string and an array of
 * strings; an empty array throws [IllegalArgumentException] via the [FilterValues] constructor.
 */
object FilterValuesSerializer : KSerializer<FilterValues> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("it.saabel.kotlinnotionclient.models.datasources.FilterValues", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: FilterValues,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("FilterValues can only be serialized to JSON")
        val element =
            if (value.raw.size == 1) {
                JsonPrimitive(value.raw.single())
            } else {
                JsonArray(value.raw.map { JsonPrimitive(it) })
            }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): FilterValues {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("FilterValues can only be deserialized from JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                FilterValues(listOf(element.content))
            }

            is JsonArray -> {
                FilterValues(
                    element.map { item ->
                        (item as? JsonPrimitive)?.content
                            ?: throw SerializationException("FilterValues array must contain only strings")
                    },
                )
            }

            else -> {
                throw SerializationException("FilterValues must be a string or an array of strings")
            }
        }
    }
}
