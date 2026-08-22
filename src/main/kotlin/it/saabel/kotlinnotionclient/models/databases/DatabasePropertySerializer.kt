package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.utils.PropertyIds
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for [DatabaseProperty] sealed class.
 *
 * This serializer handles polymorphic deserialization based on the "type" discriminator field,
 * with a fallback to [DatabaseProperty.Unknown] for unrecognized property types (e.g., "button").
 * This ensures forward compatibility as Notion adds new property types.
 *
 * It also normalises the `id` field to its decoded form (Notion returns data source schema
 * property IDs percent-encoded, e.g. `%7DVpb`, while every other response shape — page
 * properties, view filter/sort payloads — uses the decoded form, e.g. `}Vpb`). See [PropertyIds].
 */
object DatabasePropertySerializer : KSerializer<DatabaseProperty> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DatabaseProperty")

    override fun deserialize(decoder: Decoder): DatabaseProperty {
        require(decoder is JsonDecoder) { "DatabasePropertySerializer can only deserialize JSON" }

        val rawElement = decoder.decodeJsonElement()
        val rawObject = rawElement.jsonObject

        val type =
            rawObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in DatabaseProperty JSON")

        val rawId = rawObject["id"]?.jsonPrimitive?.content
        val id = rawId?.let { PropertyIds.decode(it) } ?: ""
        val name = rawObject["name"]?.jsonPrimitive?.content ?: ""

        // Re-emit the object with the normalised id so every concrete DatabaseProperty variant
        // (decoded below via its own @Serializable data class) ends up with a decoded id, not
        // just the Unknown fallback branch that reads id/name directly off the raw JSON.
        val element =
            if (rawId != null && rawId != id) {
                JsonObject(rawObject.toMutableMap().apply { this["id"] = JsonPrimitive(id) })
            } else {
                rawElement
            }

        return when (type) {
            "title" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Title.serializer(), element)
            }

            "rich_text" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.RichText.serializer(), element)
            }

            "number" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Number.serializer(), element)
            }

            "select" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Select.serializer(), element)
            }

            "multi_select" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.MultiSelect.serializer(), element)
            }

            "date" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Date.serializer(), element)
            }

            "checkbox" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Checkbox.serializer(), element)
            }

            "url" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Url.serializer(), element)
            }

            "email" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Email.serializer(), element)
            }

            "phone_number" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.PhoneNumber.serializer(), element)
            }

            "created_time" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.CreatedTime.serializer(), element)
            }

            "created_by" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.CreatedBy.serializer(), element)
            }

            "last_edited_time" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.LastEditedTime.serializer(), element)
            }

            "last_edited_by" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.LastEditedBy.serializer(), element)
            }

            "people" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.People.serializer(), element)
            }

            "relation" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Relation.serializer(), element)
            }

            "rollup" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Rollup.serializer(), element)
            }

            "formula" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Formula.serializer(), element)
            }

            "files" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Files.serializer(), element)
            }

            "status" -> {
                decoder.json.decodeFromJsonElement(DatabaseProperty.Status.serializer(), element)
            }

            else -> {
                DatabaseProperty.Unknown(
                    id = id,
                    name = name,
                    type = type,
                    rawContent = element,
                )
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: DatabaseProperty,
    ) {
        when (value) {
            is DatabaseProperty.Title -> encoder.encodeSerializableValue(DatabaseProperty.Title.serializer(), value)
            is DatabaseProperty.RichText -> encoder.encodeSerializableValue(DatabaseProperty.RichText.serializer(), value)
            is DatabaseProperty.Number -> encoder.encodeSerializableValue(DatabaseProperty.Number.serializer(), value)
            is DatabaseProperty.Select -> encoder.encodeSerializableValue(DatabaseProperty.Select.serializer(), value)
            is DatabaseProperty.MultiSelect -> encoder.encodeSerializableValue(DatabaseProperty.MultiSelect.serializer(), value)
            is DatabaseProperty.Date -> encoder.encodeSerializableValue(DatabaseProperty.Date.serializer(), value)
            is DatabaseProperty.Checkbox -> encoder.encodeSerializableValue(DatabaseProperty.Checkbox.serializer(), value)
            is DatabaseProperty.Url -> encoder.encodeSerializableValue(DatabaseProperty.Url.serializer(), value)
            is DatabaseProperty.Email -> encoder.encodeSerializableValue(DatabaseProperty.Email.serializer(), value)
            is DatabaseProperty.PhoneNumber -> encoder.encodeSerializableValue(DatabaseProperty.PhoneNumber.serializer(), value)
            is DatabaseProperty.CreatedTime -> encoder.encodeSerializableValue(DatabaseProperty.CreatedTime.serializer(), value)
            is DatabaseProperty.CreatedBy -> encoder.encodeSerializableValue(DatabaseProperty.CreatedBy.serializer(), value)
            is DatabaseProperty.LastEditedTime -> encoder.encodeSerializableValue(DatabaseProperty.LastEditedTime.serializer(), value)
            is DatabaseProperty.LastEditedBy -> encoder.encodeSerializableValue(DatabaseProperty.LastEditedBy.serializer(), value)
            is DatabaseProperty.People -> encoder.encodeSerializableValue(DatabaseProperty.People.serializer(), value)
            is DatabaseProperty.Relation -> encoder.encodeSerializableValue(DatabaseProperty.Relation.serializer(), value)
            is DatabaseProperty.Rollup -> encoder.encodeSerializableValue(DatabaseProperty.Rollup.serializer(), value)
            is DatabaseProperty.Formula -> encoder.encodeSerializableValue(DatabaseProperty.Formula.serializer(), value)
            is DatabaseProperty.Files -> encoder.encodeSerializableValue(DatabaseProperty.Files.serializer(), value)
            is DatabaseProperty.Status -> encoder.encodeSerializableValue(DatabaseProperty.Status.serializer(), value)
            is DatabaseProperty.Unknown -> encoder.encodeSerializableValue(DatabaseProperty.Unknown.serializer(), value)
        }
    }
}
