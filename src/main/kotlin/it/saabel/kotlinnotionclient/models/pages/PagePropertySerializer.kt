package it.saabel.kotlinnotionclient.models.pages

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for [PageProperty] sealed class.
 *
 * This serializer handles the polymorphic serialization and deserialization of PageProperty objects
 * based on the "type" discriminator field in the JSON, maintaining compatibility with the
 * Notion API's response format.
 *
 * Unlike other serializers in the codebase, this one gracefully handles unknown property types
 * by deserializing them as [PageProperty.Unknown], ensuring forward compatibility as Notion
 * adds new property types (e.g., "button", "verification", etc.).
 *
 * ## Supported Property Types
 * - title, rich_text, number, checkbox, url, email, phone_number, unique_id, place
 * - select, multi_select, status
 * - date, people, files
 * - relation, formula, rollup
 * - created_time, last_edited_time, created_by, last_edited_by
 *
 * ## Unknown Property Types
 * Any property type not in the list above will be deserialized as [PageProperty.Unknown],
 * with the raw JSON preserved in the `rawContent` field for inspection or manual handling.
 */
object PagePropertySerializer : KSerializer<PageProperty> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PageProperty")

    override fun deserialize(decoder: Decoder): PageProperty {
        require(decoder is JsonDecoder) { "PagePropertySerializer can only deserialize JSON" }

        val element = decoder.decodeJsonElement()
        val jsonObject = element.jsonObject

        val type =
            jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in PageProperty JSON")

        val id =
            jsonObject["id"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'id' field in PageProperty JSON")

        return when (type) {
            "title" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Title.serializer(), element)
            }

            "rich_text" -> {
                decoder.json.decodeFromJsonElement(PageProperty.RichTextProperty.serializer(), element)
            }

            "number" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Number.serializer(), element)
            }

            "checkbox" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Checkbox.serializer(), element)
            }

            "url" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Url.serializer(), element)
            }

            "email" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Email.serializer(), element)
            }

            "phone_number" -> {
                decoder.json.decodeFromJsonElement(PageProperty.PhoneNumber.serializer(), element)
            }

            "unique_id" -> {
                decoder.json.decodeFromJsonElement(PageProperty.UniqueId.serializer(), element)
            }

            "place" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Place.serializer(), element)
            }

            "select" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Select.serializer(), element)
            }

            "multi_select" -> {
                decoder.json.decodeFromJsonElement(PageProperty.MultiSelect.serializer(), element)
            }

            "status" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Status.serializer(), element)
            }

            "date" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Date.serializer(), element)
            }

            "people" -> {
                decoder.json.decodeFromJsonElement(PageProperty.People.serializer(), element)
            }

            "files" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Files.serializer(), element)
            }

            "relation" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Relation.serializer(), element)
            }

            "formula" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Formula.serializer(), element)
            }

            "rollup" -> {
                decoder.json.decodeFromJsonElement(PageProperty.Rollup.serializer(), element)
            }

            "created_time" -> {
                decoder.json.decodeFromJsonElement(PageProperty.CreatedTime.serializer(), element)
            }

            "last_edited_time" -> {
                decoder.json.decodeFromJsonElement(PageProperty.LastEditedTime.serializer(), element)
            }

            "created_by" -> {
                decoder.json.decodeFromJsonElement(PageProperty.CreatedBy.serializer(), element)
            }

            "last_edited_by" -> {
                decoder.json.decodeFromJsonElement(PageProperty.LastEditedBy.serializer(), element)
            }

            else -> {
                // Fallback to Unknown for unsupported property types
                // Manually construct Unknown with the raw JSON element
                PageProperty.Unknown(
                    id = id,
                    type = type,
                    rawContent = element,
                )
            }
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: PageProperty,
    ) {
        // For serialization, delegate to the appropriate serializer
        when (value) {
            is PageProperty.Title -> {
                encoder.encodeSerializableValue(PageProperty.Title.serializer(), value)
            }

            is PageProperty.RichTextProperty -> {
                encoder.encodeSerializableValue(
                    PageProperty.RichTextProperty.serializer(),
                    value,
                )
            }

            is PageProperty.Number -> {
                encoder.encodeSerializableValue(PageProperty.Number.serializer(), value)
            }

            is PageProperty.Checkbox -> {
                encoder.encodeSerializableValue(PageProperty.Checkbox.serializer(), value)
            }

            is PageProperty.Url -> {
                encoder.encodeSerializableValue(PageProperty.Url.serializer(), value)
            }

            is PageProperty.Email -> {
                encoder.encodeSerializableValue(PageProperty.Email.serializer(), value)
            }

            is PageProperty.PhoneNumber -> {
                encoder.encodeSerializableValue(PageProperty.PhoneNumber.serializer(), value)
            }

            is PageProperty.UniqueId -> {
                encoder.encodeSerializableValue(PageProperty.UniqueId.serializer(), value)
            }

            is PageProperty.Place -> {
                encoder.encodeSerializableValue(PageProperty.Place.serializer(), value)
            }

            is PageProperty.Select -> {
                encoder.encodeSerializableValue(PageProperty.Select.serializer(), value)
            }

            is PageProperty.MultiSelect -> {
                encoder.encodeSerializableValue(PageProperty.MultiSelect.serializer(), value)
            }

            is PageProperty.Status -> {
                encoder.encodeSerializableValue(PageProperty.Status.serializer(), value)
            }

            is PageProperty.Date -> {
                encoder.encodeSerializableValue(PageProperty.Date.serializer(), value)
            }

            is PageProperty.People -> {
                encoder.encodeSerializableValue(PageProperty.People.serializer(), value)
            }

            is PageProperty.Files -> {
                encoder.encodeSerializableValue(PageProperty.Files.serializer(), value)
            }

            is PageProperty.Relation -> {
                encoder.encodeSerializableValue(PageProperty.Relation.serializer(), value)
            }

            is PageProperty.Formula -> {
                encoder.encodeSerializableValue(PageProperty.Formula.serializer(), value)
            }

            is PageProperty.Rollup -> {
                encoder.encodeSerializableValue(PageProperty.Rollup.serializer(), value)
            }

            is PageProperty.CreatedTime -> {
                encoder.encodeSerializableValue(PageProperty.CreatedTime.serializer(), value)
            }

            is PageProperty.LastEditedTime -> {
                encoder.encodeSerializableValue(
                    PageProperty.LastEditedTime.serializer(),
                    value,
                )
            }

            is PageProperty.CreatedBy -> {
                encoder.encodeSerializableValue(PageProperty.CreatedBy.serializer(), value)
            }

            is PageProperty.LastEditedBy -> {
                encoder.encodeSerializableValue(
                    PageProperty.LastEditedBy.serializer(),
                    value,
                )
            }

            is PageProperty.Unknown -> {
                encoder.encodeSerializableValue(PageProperty.Unknown.serializer(), value)
            }
        }
    }
}
