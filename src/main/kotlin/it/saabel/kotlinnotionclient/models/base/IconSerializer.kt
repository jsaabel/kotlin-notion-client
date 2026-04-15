package it.saabel.kotlinnotionclient.models.base

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for [Icon] sealed class.
 *
 * Handles polymorphic serialization and deserialization based on the "type"
 * discriminator field in the JSON:
 * - "emoji" -> [Icon.Emoji]
 * - "custom_emoji" -> [Icon.CustomEmoji]
 * - "external" -> [Icon.External]
 * - "file" -> [Icon.File]
 * - "file_upload" -> [Icon.FileUpload]
 * - "icon" -> [Icon.NativeIcon]
 */
object IconSerializer : JsonContentPolymorphicSerializer<Icon>(Icon::class) {
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
