package it.saabel.kotlinnotionclient.models.base

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for [Parent] sealed class.
 *
 * This serializer handles the polymorphic serialization and deserialization of Parent objects
 * based on the "type" discriminator field in the JSON, maintaining compatibility with the
 * Notion API's response format.
 *
 * The serializer uses a flat structure where the type determines which subclass to deserialize:
 * - "page_id" -> [Parent.PageParent]
 * - "data_source_id" -> [Parent.DataSourceParent]
 * - "database_id" -> [Parent.DatabaseParent]
 * - "block_id" -> [Parent.BlockParent]
 * - "workspace" -> [Parent.WorkspaceParent]
 */
object ParentSerializer : JsonContentPolymorphicSerializer<Parent>(Parent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Parent> {
        val type =
            element.jsonObject["type"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing 'type' field in Parent JSON")

        return when (type) {
            "page_id" -> Parent.PageParent.serializer()
            "data_source_id" -> Parent.DataSourceParent.serializer()
            "database_id" -> Parent.DatabaseParent.serializer()
            "block_id" -> Parent.BlockParent.serializer()
            "workspace" -> Parent.WorkspaceParent.serializer()
            else -> throw SerializationException("Unknown Parent type: $type")
        }
    }
}

/**
 * Internal representation for serialization that matches the API's flat structure.
 * This allows us to serialize our sealed class hierarchy back to the expected JSON format.
 */
@Serializable
internal data class ParentSurrogate(
    @SerialName("type")
    val type: String,
    @SerialName("page_id")
    val pageId: String? = null,
    @SerialName("data_source_id")
    val dataSourceId: String? = null,
    @SerialName("database_id")
    val databaseId: String? = null,
    @SerialName("block_id")
    val blockId: String? = null,
    @SerialName("workspace")
    val workspace: Boolean? = null,
)
