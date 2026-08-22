package it.saabel.kotlinnotionclient.models.base

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for [Icon] sealed class.
 *
 * Decoding picks a subclass from the "type" discriminator field in the JSON:
 * - "emoji" -> [Icon.Emoji]
 * - "custom_emoji" -> [Icon.CustomEmoji]
 * - "external" -> [Icon.External]
 * - "file" -> [Icon.File]
 * - "file_upload" -> [Icon.FileUpload]
 * - "icon" -> [Icon.NativeIcon]
 *
 * Encoding dispatches on the subclass explicitly rather than looking its serializer up
 * reflectively, so [Icon.Removed] reliably reaches [IconRemovedSerializer] and is written as
 * JSON `null` — a reflective lookup would fall back to the generic object serializer and emit
 * `{}` instead. See `docs/adr/0002-explicit-null-payloads.md`.
 */
object IconSerializer : KSerializer<Icon> {
    private object ContentSerializer : JsonContentPolymorphicSerializer<Icon>(Icon::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Icon> {
            val type =
                element.jsonObject["type"]?.jsonPrimitive?.content
                    ?: throw SerializationException("Missing 'type' field in Icon JSON")

            return when (type) {
                "emoji" -> Icon.Emoji.serializer()
                "custom_emoji" -> Icon.CustomEmoji.serializer()
                "external" -> Icon.External.serializer()
                "file" -> Icon.File.serializer()
                "file_upload" -> Icon.FileUpload.serializer()
                "icon" -> Icon.NativeIcon.serializer()
                else -> throw SerializationException("Unknown Icon type: $type")
            }
        }
    }

    override val descriptor: SerialDescriptor = ContentSerializer.descriptor

    override fun deserialize(decoder: Decoder): Icon = ContentSerializer.deserialize(decoder)

    override fun serialize(
        encoder: Encoder,
        value: Icon,
    ) {
        when (value) {
            is Icon.Emoji -> encoder.encodeSerializableValue(Icon.Emoji.serializer(), value)
            is Icon.CustomEmoji -> encoder.encodeSerializableValue(Icon.CustomEmoji.serializer(), value)
            is Icon.External -> encoder.encodeSerializableValue(Icon.External.serializer(), value)
            is Icon.File -> encoder.encodeSerializableValue(Icon.File.serializer(), value)
            is Icon.FileUpload -> encoder.encodeSerializableValue(Icon.FileUpload.serializer(), value)
            is Icon.NativeIcon -> encoder.encodeSerializableValue(Icon.NativeIcon.serializer(), value)
            is Icon.PendingUpload -> encoder.encodeSerializableValue(IconPendingUploadSerializer, value)
            is Icon.Removed -> encoder.encodeSerializableValue(IconRemovedSerializer, value)
        }
    }
}
