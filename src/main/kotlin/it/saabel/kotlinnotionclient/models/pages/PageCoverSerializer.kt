package it.saabel.kotlinnotionclient.models.pages

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for [PageCover] sealed class.
 *
 * This serializer handles the polymorphic serialization and deserialization of PageCover objects
 * based on the "type" discriminator field in the JSON, maintaining compatibility with the
 * Notion API's response format.
 *
 * The serializer uses a flat structure where the type determines which subclass to deserialize:
 * - "external" -> [PageCover.External]
 * - "file" -> [PageCover.File]
 * - "file_upload" -> [PageCover.FileUpload]
 */
object PageCoverSerializer : JsonContentPolymorphicSerializer<PageCover>(PageCover::class) {
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
