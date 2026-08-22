package it.saabel.kotlinnotionclient.models.pages

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
 * Custom serializer for [PageCover] sealed class.
 *
 * Decoding picks a subclass from the "type" discriminator field in the JSON, maintaining
 * compatibility with the Notion API's response format:
 * - "external" -> [PageCover.External]
 * - "file" -> [PageCover.File]
 * - "file_upload" -> [PageCover.FileUpload]
 *
 * Encoding dispatches on the subclass explicitly rather than looking its serializer up
 * reflectively, so [PageCover.Removed] reliably reaches [PageCoverRemovedSerializer] and is
 * written as JSON `null` — a reflective lookup would fall back to the generic object serializer
 * and emit `{}` instead. See `docs/adr/0002-explicit-null-payloads.md`.
 */
object PageCoverSerializer : KSerializer<PageCover> {
    private object ContentSerializer : JsonContentPolymorphicSerializer<PageCover>(PageCover::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PageCover> {
            val type =
                element.jsonObject["type"]?.jsonPrimitive?.content
                    ?: throw SerializationException("Missing 'type' field in PageCover JSON")

            return when (type) {
                "external" -> PageCover.External.serializer()
                "file" -> PageCover.File.serializer()
                "file_upload" -> PageCover.FileUpload.serializer()
                else -> throw SerializationException("Unknown PageCover type: $type")
            }
        }
    }

    override val descriptor: SerialDescriptor = ContentSerializer.descriptor

    override fun deserialize(decoder: Decoder): PageCover = ContentSerializer.deserialize(decoder)

    override fun serialize(
        encoder: Encoder,
        value: PageCover,
    ) {
        when (value) {
            is PageCover.External -> encoder.encodeSerializableValue(PageCover.External.serializer(), value)
            is PageCover.File -> encoder.encodeSerializableValue(PageCover.File.serializer(), value)
            is PageCover.FileUpload -> encoder.encodeSerializableValue(PageCover.FileUpload.serializer(), value)
            is PageCover.PendingUpload -> encoder.encodeSerializableValue(PageCoverPendingUploadSerializer, value)
            is PageCover.Removed -> encoder.encodeSerializableValue(PageCoverRemovedSerializer, value)
        }
    }
}
