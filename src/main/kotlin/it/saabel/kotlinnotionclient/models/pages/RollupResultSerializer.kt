package it.saabel.kotlinnotionclient.models.pages

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for [RollupResult] sealed class.
 *
 * This is the default deserializer for the hierarchy: it dispatches on the
 * "type" discriminator field to the matching known subtype, and falls back to
 * [RollupResult.Unknown] for any "type" it does not recognize. This mirrors
 * [PagePropertySerializer] one level down, so a rollup result type Notion adds
 * in the future degrades gracefully instead of failing page deserialization.
 */
object RollupResultSerializer : KSerializer<RollupResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RollupResult")

    override fun deserialize(decoder: Decoder): RollupResult {
        require(decoder is JsonDecoder) { "RollupResultSerializer can only deserialize JSON" }

        val element = decoder.decodeJsonElement()
        val jsonObject = element.jsonObject

        val type =
            jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in RollupResult JSON")

        return when (type) {
            "number" -> {
                decoder.json.decodeFromJsonElement(RollupResult.NumberResult.serializer(), element)
            }

            "date" -> {
                decoder.json.decodeFromJsonElement(RollupResult.DateResult.serializer(), element)
            }

            "array" -> {
                decoder.json.decodeFromJsonElement(RollupResult.ArrayResult.serializer(), element)
            }

            "unsupported" -> {
                decoder.json.decodeFromJsonElement(RollupResult.UnsupportedResult.serializer(), element)
            }

            "incomplete" -> {
                decoder.json.decodeFromJsonElement(RollupResult.IncompleteResult.serializer(), element)
            }

            else -> {
                // Default deserializer: fall back to Unknown for unrecognized rollup result types
                RollupResult.Unknown(
                    type = type,
                    rawContent = element,
                )
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: RollupResult,
    ) {
        when (value) {
            is RollupResult.NumberResult -> {
                encoder.encodeSerializableValue(RollupResult.NumberResult.serializer(), value)
            }

            is RollupResult.DateResult -> {
                encoder.encodeSerializableValue(RollupResult.DateResult.serializer(), value)
            }

            is RollupResult.ArrayResult -> {
                encoder.encodeSerializableValue(RollupResult.ArrayResult.serializer(), value)
            }

            is RollupResult.UnsupportedResult -> {
                encoder.encodeSerializableValue(RollupResult.UnsupportedResult.serializer(), value)
            }

            is RollupResult.IncompleteResult -> {
                encoder.encodeSerializableValue(RollupResult.IncompleteResult.serializer(), value)
            }

            is RollupResult.Unknown -> {
                encoder.encodeSerializableValue(RollupResult.Unknown.serializer(), value)
            }
        }
    }
}
