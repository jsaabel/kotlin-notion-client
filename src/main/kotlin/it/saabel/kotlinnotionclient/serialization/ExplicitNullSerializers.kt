package it.saabel.kotlinnotionclient.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

// Serializers for the two places in the request models where JSON `null` is the payload rather
// than the absence of one.
//
// The client encodes with `explicitNulls = false` (see [NotionJson]), which drops a nullable field
// holding Kotlin `null` instead of writing `"field": null`. That is right for "this PATCH does not
// touch that field" and wrong for "this PATCH clears that field" — and the serializer cannot tell
// the two apart from a `null` alone. So an intended `null` is never modelled as a Kotlin `null` on
// a nullable field; it is modelled as a value whose serializer writes `null` on purpose.
//
// See `docs/adr/0002-explicit-null-payloads.md`.

/**
 * Serializer for a removal sentinel — an object that exists only to be encoded as JSON `null`.
 *
 * Used for `icon` and `cover`, where Notion documents `null` as the removal instruction. The
 * sentinel is a non-null Kotlin value, so `explicitNulls = false` leaves it alone, and this
 * serializer then writes the `null` the API is waiting for.
 *
 * Decoding never reaches here: a JSON `null` in a nullable field is turned into a Kotlin `null` by
 * the decoder before any element serializer runs, so a removed icon reads back as `icon == null`.
 *
 * @param serialName Name reported by the descriptor, for diagnostics only — it never reaches the wire
 */
internal abstract class RemovalSentinelSerializer<T : Any>(
    serialName: String,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(
        encoder: Encoder,
        value: T,
    ) = encoder.encodeNull()

    override fun deserialize(decoder: Decoder): T =
        throw SerializationException(
            "${descriptor.serialName} is a write-only removal sentinel and is never decoded; " +
                "a removed icon or cover reads back as null",
        )
}

/**
 * Serializer for a single-payload property value whose payload key must survive as JSON `null`.
 *
 * Notion clears a property by receiving the key with a `null` value — `{"Status":{"select":null}}`.
 * The generated serializer would drop `select` entirely under `explicitNulls = false`, leaving
 * `{"type":"select"}`: the discriminator without the instruction. This writes the key
 * unconditionally, so a cleared value and a set one differ only in the payload.
 *
 * The key is always present on the way out, which also matches what Notion sends on the way in.
 *
 * @param serialName The polymorphic discriminator value for this property value (e.g. `select`)
 * @param payloadKey The JSON key carrying the value (e.g. `select`)
 * @param payloadSerializer Serializer for the payload when it is present
 */
internal abstract class ClearableValueSerializer<T : Any, P : Any>(
    serialName: String,
    payloadKey: String,
    payloadSerializer: KSerializer<P>,
) : KSerializer<T> {
    private val nullablePayloadSerializer: KSerializer<P?> = payloadSerializer.nullable

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(serialName) {
            element(payloadKey, nullablePayloadSerializer.descriptor)
        }

    /** The payload currently held by [value], or `null` when the property is being cleared. */
    protected abstract fun payloadOf(value: T): P?

    /** Rebuilds the property value around a decoded [payload]. */
    protected abstract fun withPayload(payload: P?): T

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) = encoder.encodeStructure(descriptor) {
        // encodeSerializableElement, not encodeNullableSerializableElement: the latter is where
        // explicitNulls = false drops the key. The nullable serializer writes the null instead.
        encodeSerializableElement(descriptor, 0, nullablePayloadSerializer, payloadOf(value))
    }

    override fun deserialize(decoder: Decoder): T =
        decoder.decodeStructure(descriptor) {
            var payload: P? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> payload = decodeSerializableElement(descriptor, 0, nullablePayloadSerializer)

                    CompositeDecoder.DECODE_DONE -> break

                    else -> throw SerializationException(
                        "Unexpected element index $index while decoding ${descriptor.serialName}",
                    )
                }
            }
            withPayload(payload)
        }
}
