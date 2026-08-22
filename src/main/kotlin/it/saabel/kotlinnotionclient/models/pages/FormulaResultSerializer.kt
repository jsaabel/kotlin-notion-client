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
 * Custom serializer for [FormulaResult] sealed class.
 *
 * This is the default deserializer for the hierarchy: it dispatches on the
 * "type" discriminator field to the matching known subtype, and falls back to
 * [FormulaResult.Unknown] for any "type" it does not recognize. This mirrors
 * [PagePropertySerializer] one level down, so a formula result type Notion adds
 * in the future degrades gracefully instead of failing page deserialization.
 */
object FormulaResultSerializer : KSerializer<FormulaResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FormulaResult")

    override fun deserialize(decoder: Decoder): FormulaResult {
        require(decoder is JsonDecoder) { "FormulaResultSerializer can only deserialize JSON" }

        val element = decoder.decodeJsonElement()
        val jsonObject = element.jsonObject

        val type =
            jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in FormulaResult JSON")

        return when (type) {
            "string" -> {
                decoder.json.decodeFromJsonElement(FormulaResult.StringResult.serializer(), element)
            }

            "number" -> {
                decoder.json.decodeFromJsonElement(FormulaResult.NumberResult.serializer(), element)
            }

            "boolean" -> {
                decoder.json.decodeFromJsonElement(FormulaResult.BooleanResult.serializer(), element)
            }

            "date" -> {
                decoder.json.decodeFromJsonElement(FormulaResult.DateResult.serializer(), element)
            }

            "unsupported" -> {
                decoder.json.decodeFromJsonElement(FormulaResult.UnsupportedResult.serializer(), element)
            }

            else -> {
                // Default deserializer: fall back to Unknown for unrecognized formula result types
                FormulaResult.Unknown(
                    type = type,
                    rawContent = element,
                )
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: FormulaResult,
    ) {
        when (value) {
            is FormulaResult.StringResult -> {
                encoder.encodeSerializableValue(FormulaResult.StringResult.serializer(), value)
            }

            is FormulaResult.NumberResult -> {
                encoder.encodeSerializableValue(FormulaResult.NumberResult.serializer(), value)
            }

            is FormulaResult.BooleanResult -> {
                encoder.encodeSerializableValue(FormulaResult.BooleanResult.serializer(), value)
            }

            is FormulaResult.DateResult -> {
                encoder.encodeSerializableValue(FormulaResult.DateResult.serializer(), value)
            }

            is FormulaResult.UnsupportedResult -> {
                encoder.encodeSerializableValue(FormulaResult.UnsupportedResult.serializer(), value)
            }

            is FormulaResult.Unknown -> {
                encoder.encodeSerializableValue(FormulaResult.Unknown.serializer(), value)
            }
        }
    }
}
