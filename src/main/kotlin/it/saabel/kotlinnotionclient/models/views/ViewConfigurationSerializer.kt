package it.saabel.kotlinnotionclient.models.views

import kotlinx.serialization.KSerializer
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

/**
 * Custom serializer for [ViewConfiguration] sealed class.
 *
 * Dispatches deserialization on the `"type"` field present in the configuration JSON object:
 * - `"list"`      → [ViewConfiguration.List]
 * - `"form"`      → [ViewConfiguration.Form]
 * - `"map"`       → [ViewConfiguration.Map]
 * - `"gallery"`   → [ViewConfiguration.Gallery]
 * - `"table"`     → [ViewConfiguration.Table]
 * - `"board"`     → [ViewConfiguration.Board]
 * - `"calendar"`  → [ViewConfiguration.Calendar]
 * - `"timeline"`  → [ViewConfiguration.Timeline]
 * - `"chart"`     → [ViewConfiguration.Chart]
 * - `"dashboard"` → [ViewConfiguration.Dashboard]
 * - anything else → [ViewConfiguration.Unknown] (raw JSON preserved)
 *
 * Serialization converts each subtype to a [JsonElement] via its own serializer, then
 * writes that element directly. [ViewConfiguration.Unknown] re-encodes its raw content as-is.
 */
object ViewConfigurationSerializer : KSerializer<ViewConfiguration> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ViewConfiguration")

    override fun deserialize(decoder: Decoder): ViewConfiguration {
        require(decoder is JsonDecoder) { "ViewConfigurationSerializer can only deserialize JSON" }

        val element = decoder.decodeJsonElement()
        val type =
            element.jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in ViewConfiguration JSON")

        return when (type) {
            "list" -> decoder.json.decodeFromJsonElement(ViewConfiguration.List.serializer(), element)
            "form" -> decoder.json.decodeFromJsonElement(ViewConfiguration.Form.serializer(), element)
            "map" -> decoder.json.decodeFromJsonElement(ViewConfiguration.Map.serializer(), element)
            "gallery" -> decoder.json.decodeFromJsonElement(ViewConfiguration.Gallery.serializer(), element)
            "table" -> decoder.json.decodeFromJsonElement(ViewConfiguration.Table.serializer(), element)
            "board" -> decoder.json.decodeFromJsonElement(ViewConfiguration.Board.serializer(), element)
            "calendar" -> decoder.json.decodeFromJsonElement(ViewConfiguration.Calendar.serializer(), element)
            "timeline" -> decoder.json.decodeFromJsonElement(ViewConfiguration.Timeline.serializer(), element)
            "chart" -> decoder.json.decodeFromJsonElement(ViewConfiguration.Chart.serializer(), element)
            "dashboard" -> decoder.json.decodeFromJsonElement(ViewConfiguration.Dashboard.serializer(), element)
            else -> ViewConfiguration.Unknown(rawContent = element)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: ViewConfiguration,
    ) {
        require(encoder is JsonEncoder) { "ViewConfigurationSerializer can only serialize JSON" }

        // Convert to JsonElement first to avoid encoder-state issues with sealed class member types,
        // then write the element directly into the stream.
        val element: JsonElement =
            when (value) {
                is ViewConfiguration.List -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.List.serializer(), value)
                }

                is ViewConfiguration.Form -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.Form.serializer(), value)
                }

                is ViewConfiguration.Map -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.Map.serializer(), value)
                }

                is ViewConfiguration.Gallery -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.Gallery.serializer(), value)
                }

                is ViewConfiguration.Table -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.Table.serializer(), value)
                }

                is ViewConfiguration.Board -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.Board.serializer(), value)
                }

                is ViewConfiguration.Calendar -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.Calendar.serializer(), value)
                }

                is ViewConfiguration.Timeline -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.Timeline.serializer(), value)
                }

                is ViewConfiguration.Chart -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.Chart.serializer(), value)
                }

                is ViewConfiguration.Dashboard -> {
                    encoder.json.encodeToJsonElement(ViewConfiguration.Dashboard.serializer(), value)
                }

                is ViewConfiguration.Unknown -> {
                    value.rawContent
                }
            }

        encoder.encodeJsonElement(element)
    }
}
