package it.saabel.kotlinnotionclient.models.files

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Base for the serializers of the pending-upload sentinels, all of which refuse to serialize.
 *
 * A pending upload carries local bytes, not a file-upload id, so there is no valid JSON for it:
 * the client resolves sentinels — uploading the file and substituting the resulting reference —
 * in every suspending entry point that accepts the request. Reaching serialization with one
 * still in the request means it was serialized by hand, so [remedy] points at both ways out.
 *
 * Deserialization throws unconditionally: nothing Notion ever sends maps to these types. The
 * sentinels exist only to satisfy kotlinx.serialization's requirement that every subclass of a
 * `@Serializable sealed class` has a serializer.
 *
 * See `docs/adr/0001-deferred-file-upload-resolution.md`.
 */
internal abstract class PendingUploadRefusingSerializer<T : Any>(
    serialName: String,
    private val filename: (T) -> String,
    private val remedy: String,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName)

    override fun serialize(
        encoder: Encoder,
        value: T,
    ): Nothing =
        throw SerializationException(
            "this request contains a file pending upload (${filename(value)}); $remedy",
        )

    override fun deserialize(decoder: Decoder): Nothing =
        throw SerializationException(
            "${descriptor.serialName} is a client-side sentinel and never appears in a Notion API response",
        )
}
