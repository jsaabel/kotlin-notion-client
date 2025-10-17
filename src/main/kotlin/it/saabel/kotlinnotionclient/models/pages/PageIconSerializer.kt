package it.saabel.kotlinnotionclient.models.pages

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for [PageIcon] sealed class.
 *
 * This serializer handles the polymorphic serialization and deserialization of PageIcon objects
 * based on the "type" discriminator field in the JSON, maintaining compatibility with the
 * Notion API's response format.
 *
 * The serializer uses a flat structure where the type determines which subclass to deserialize:
 * - "emoji" -> [PageIcon.Emoji]
 * - "custom_emoji" -> [PageIcon.CustomEmoji]
 * - "external" -> [PageIcon.External]
 * - "file" -> [PageIcon.File]
 * - "file_upload" -> [PageIcon.FileUpload]
 */
object PageIconSerializer : JsonContentPolymorphicSerializer<PageIcon>(PageIcon::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PageIcon> {
        val type =
            element.jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in PageIcon JSON")

        return when (type) {
            "emoji" -> PageIcon.Emoji.serializer()
            "custom_emoji" -> PageIcon.CustomEmoji.serializer()
            "external" -> PageIcon.External.serializer()
            "file" -> PageIcon.File.serializer()
            "file_upload" -> PageIcon.FileUpload.serializer()
            else -> throw SerializationException("Unknown PageIcon type: $type")
        }
    }
}
