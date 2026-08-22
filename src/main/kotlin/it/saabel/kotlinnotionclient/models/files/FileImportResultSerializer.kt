package it.saabel.kotlinnotionclient.models.files

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for the [FileImportResult] sealed class.
 *
 * This is the default deserializer for the hierarchy: it dispatches on the "type" discriminator
 * field to the matching known subtype, and falls back to [FileImportResult.Unknown] for any
 * "type" it does not recognize — following the `RollupResult`/`FormulaResult` precedent, so a
 * result type Notion adds in the future degrades gracefully instead of failing file-upload
 * deserialization.
 */
object FileImportResultSerializer : KSerializer<FileImportResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FileImportResult")

    override fun deserialize(decoder: Decoder): FileImportResult {
        require(decoder is JsonDecoder) { "FileImportResultSerializer can only deserialize JSON" }

        val element = decoder.decodeJsonElement()
        val jsonObject = element.jsonObject

        val type =
            jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in FileImportResult JSON")

        return when (type) {
            "success" -> {
                decoder.json.decodeFromJsonElement(FileImportResult.Success.serializer(), element)
            }

            "error" -> {
                decoder.json.decodeFromJsonElement(FileImportResult.Error.serializer(), element)
            }

            else -> {
                // Default deserializer: fall back to Unknown for unrecognized import result types
                FileImportResult.Unknown(
                    type = type,
                    importedTime = jsonObject["imported_time"]?.jsonPrimitive?.contentOrNull,
                    rawContent = element,
                )
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: FileImportResult,
    ) {
        when (value) {
            is FileImportResult.Success -> {
                encoder.encodeSerializableValue(FileImportResult.Success.serializer(), value)
            }

            is FileImportResult.Error -> {
                encoder.encodeSerializableValue(FileImportResult.Error.serializer(), value)
            }

            is FileImportResult.Unknown -> {
                encoder.encodeSerializableValue(FileImportResult.Unknown.serializer(), value)
            }
        }
    }
}
