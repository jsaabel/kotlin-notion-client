@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.webhooks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A webhook event delivered by Notion to a subscribed endpoint.
 *
 * Webhooks are change *signals*, not full content: follow up with regular API calls to
 * fetch the latest state of the affected entity. Events may arrive out of order — use
 * [timestamp] to re-order if needed — and may be retried up to 8 times
 * ([attemptNumber]).
 *
 * **Always verify the `X-Notion-Signature` header against the raw request body with
 * [it.saabel.kotlinnotionclient.webhooks.WebhookSignature] before parsing.**
 *
 * The model is deliberately tolerant of API evolution: [type] is kept as the raw
 * string so unknown event types still deserialize, with [eventType] providing a typed
 * view that falls back to [WebhookEventType.UNKNOWN]. Unknown JSON keys are ignored.
 *
 * Shape source: [Event types & delivery](https://developers.notion.com/reference/webhooks-events-delivery).
 *
 * @property id Unique identifier of this event
 * @property timestamp ISO 8601 timestamp of when the event occurred
 * @property workspaceId Workspace the event originated from
 * @property workspaceName Human-readable workspace name, when provided
 * @property subscriptionId The webhook subscription that produced this delivery
 * @property integrationId The integration (connection) associated with the subscription
 * @property type Raw event type string, e.g. `"page.created"`. Prefer [eventType] for
 *   exhaustive `when` handling; keep this for logging and forward compatibility.
 * @property authors Who caused the event (person, bot, or agent)
 * @property accessibleBy Users and bots with access to the entity (public integrations only)
 * @property attemptNumber Delivery attempt, 1–8
 * @property apiVersion Notion API version the payload conforms to, when provided
 * @property entity The object the event is about (`page`, `database`, `data_source`,
 *   `comment`, or `block`)
 * @property data Event-specific details; see [WebhookEventData]
 */
@Serializable
data class WebhookEvent(
    @SerialName("id")
    val id: String,
    @SerialName("timestamp")
    val timestamp: String,
    @SerialName("workspace_id")
    val workspaceId: String? = null,
    @SerialName("workspace_name")
    val workspaceName: String? = null,
    @SerialName("subscription_id")
    val subscriptionId: String? = null,
    @SerialName("integration_id")
    val integrationId: String? = null,
    @SerialName("type")
    val type: String,
    @SerialName("authors")
    val authors: List<WebhookPrincipal> = emptyList(),
    @SerialName("accessible_by")
    val accessibleBy: List<WebhookPrincipal> = emptyList(),
    @SerialName("attempt_number")
    val attemptNumber: Int? = null,
    @SerialName("api_version")
    val apiVersion: String? = null,
    @SerialName("entity")
    val entity: WebhookEntityRef? = null,
    @SerialName("data")
    val data: WebhookEventData? = null,
) {
    /** Typed view of [type]; [WebhookEventType.UNKNOWN] for event types this library doesn't know yet. */
    val eventType: WebhookEventType
        get() = WebhookEventType.fromValue(type)

    companion object {
        internal val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            }

        /**
         * Parses a webhook event from the raw request body. Call this only **after**
         * the signature has been verified against the same raw body.
         *
         * Note: the one-time subscription-verification request has a different shape —
         * see [WebhookVerificationRequest.fromJsonOrNull].
         */
        fun fromJson(rawBody: String): WebhookEvent = json.decodeFromString(serializer(), rawBody)
    }
}

/**
 * A user, bot, or agent reference in a webhook payload.
 *
 * @property id UUID of the principal
 * @property type `"person"`, `"bot"`, or `"agent"` — kept as a string for forward compatibility
 */
@Serializable
data class WebhookPrincipal(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String? = null,
)

/**
 * A lightweight reference to a Notion object in a webhook payload.
 *
 * @property id UUID of the object
 * @property type `"page"`, `"block"`, `"database"`, `"data_source"`, `"comment"`, or
 *   `"space"` — kept as a string for forward compatibility
 */
@Serializable
data class WebhookEntityRef(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String? = null,
)

/**
 * Event-specific details of a [WebhookEvent].
 *
 * All fields are optional because their presence depends on the event type. The
 * `updated_properties` key is polymorphic in the API: for `page.properties_updated`
 * it is an array of property-id strings, while for `database.schema_updated` /
 * `data_source.schema_updated` it is an array of `{id, name, action}` objects. It is
 * therefore kept raw, with [updatedPropertyIds] and [updatedSchemaProperties] as typed
 * accessors.
 *
 * @property parent Parent of the affected entity (a page, database, block, or the
 *   workspace itself, `type = "space"`)
 * @property pageId For comment events, the page the comment lives on
 * @property updatedBlocks For `*.content_updated` events, the blocks that changed
 * @property updatedProperties Raw `updated_properties` value; prefer the typed accessors
 */
@Serializable
data class WebhookEventData(
    @SerialName("parent")
    val parent: WebhookEntityRef? = null,
    @SerialName("page_id")
    val pageId: String? = null,
    @SerialName("updated_blocks")
    val updatedBlocks: List<WebhookEntityRef> = emptyList(),
    @SerialName("updated_properties")
    val updatedProperties: JsonElement? = null,
) {
    /**
     * Property ids from `updated_properties` when it is an array of strings
     * (`page.properties_updated`). Empty for other shapes.
     */
    val updatedPropertyIds: List<String>
        get() =
            (updatedProperties as? JsonArray)
                ?.filterIsInstance<JsonPrimitive>()
                ?.mapNotNull { it.contentOrNull }
                ?: emptyList()

    /**
     * Schema changes from `updated_properties` when it is an array of objects
     * (`database.schema_updated`, `data_source.schema_updated`). Empty for other shapes.
     */
    val updatedSchemaProperties: List<WebhookSchemaPropertyChange>
        get() =
            (updatedProperties as? JsonArray)
                ?.filterIsInstance<JsonObject>()
                ?.map { WebhookEvent.json.decodeFromJsonElement(WebhookSchemaPropertyChange.serializer(), it) }
                ?: emptyList()
}

/**
 * A single property change in a `*.schema_updated` event.
 *
 * @property id Property id (URL-encoded, as elsewhere in the API)
 * @property name Property name at the time of the change
 * @property action `"created"`, `"updated"`, or `"deleted"` — kept as a string for
 *   forward compatibility
 */
@Serializable
data class WebhookSchemaPropertyChange(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("action")
    val action: String? = null,
)

/**
 * The one-time request Notion sends when a webhook subscription is created, containing
 * the `verification_token` used to sign all subsequent deliveries. Persist the token
 * securely — it is only delivered once.
 */
@Serializable
data class WebhookVerificationRequest(
    @SerialName("verification_token")
    val verificationToken: String,
) {
    companion object {
        /**
         * Returns the parsed verification request, or `null` if [rawBody] is not one
         * (i.e. it is a regular event or not valid JSON). Useful for routing the
         * initial handshake in a receiver endpoint.
         */
        fun fromJsonOrNull(rawBody: String): WebhookVerificationRequest? =
            runCatching {
                val obj = WebhookEvent.json.parseToJsonElement(rawBody).jsonObject
                val token = obj["verification_token"]?.jsonPrimitive?.contentOrNull ?: return null
                WebhookVerificationRequest(token)
            }.getOrNull()
    }
}

/**
 * Known webhook event types, per
 * [Event types & delivery](https://developers.notion.com/reference/webhooks-events-delivery).
 *
 * Unrecognized types map to [UNKNOWN] rather than failing deserialization — check
 * [WebhookEvent.type] for the raw string in that case.
 */
enum class WebhookEventType(
    val value: String,
) {
    PAGE_CREATED("page.created"),
    PAGE_CONTENT_UPDATED("page.content_updated"),
    PAGE_PROPERTIES_UPDATED("page.properties_updated"),
    PAGE_MOVED("page.moved"),
    PAGE_DELETED("page.deleted"),
    PAGE_UNDELETED("page.undeleted"),
    PAGE_LOCKED("page.locked"),
    PAGE_UNLOCKED("page.unlocked"),
    DATABASE_CREATED("database.created"),
    DATABASE_CONTENT_UPDATED("database.content_updated"),
    DATABASE_MOVED("database.moved"),
    DATABASE_DELETED("database.deleted"),
    DATABASE_UNDELETED("database.undeleted"),
    DATABASE_SCHEMA_UPDATED("database.schema_updated"),
    DATA_SOURCE_CREATED("data_source.created"),
    DATA_SOURCE_CONTENT_UPDATED("data_source.content_updated"),
    DATA_SOURCE_MOVED("data_source.moved"),
    DATA_SOURCE_DELETED("data_source.deleted"),
    DATA_SOURCE_UNDELETED("data_source.undeleted"),
    DATA_SOURCE_SCHEMA_UPDATED("data_source.schema_updated"),
    COMMENT_CREATED("comment.created"),
    COMMENT_UPDATED("comment.updated"),
    COMMENT_DELETED("comment.deleted"),

    /** An event type this library version does not know about. */
    UNKNOWN(""),
    ;

    companion object {
        private val byValue = entries.filter { it != UNKNOWN }.associateBy { it.value }

        /** Maps a raw event type string to a known type, or [UNKNOWN]. */
        fun fromValue(value: String): WebhookEventType = byValue[value] ?: UNKNOWN
    }
}
