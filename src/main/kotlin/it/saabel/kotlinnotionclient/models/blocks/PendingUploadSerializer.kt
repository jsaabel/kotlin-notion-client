package it.saabel.kotlinnotionclient.models.blocks

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [BlockRequest.PendingUpload] that refuses to serialize.
 *
 * A pending upload carries local bytes, not a file-upload id, so there is no valid JSON for it:
 * the client resolves sentinels — uploading the file and substituting the resulting reference —
 * in every suspending entry point that accepts blocks. Reaching serialization with one still in
 * the tree means the blocks were serialized by hand, so the message points at both ways out.
 *
 * Deserialization throws unconditionally: nothing Notion ever sends maps to this type. The
 * sentinel exists only to satisfy kotlinx.serialization's requirement that every subclass of a
 * `@Serializable sealed class` has a serializer.
 */
internal object PendingUploadSerializer : KSerializer<BlockRequest.PendingUpload> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("pending_upload")

    override fun serialize(
        encoder: Encoder,
        value: BlockRequest.PendingUpload,
    ): Nothing =
        throw SerializationException(
            "this content contains a file pending upload (${value.source.filename}); " +
                "pass it through a NotionClient method (pages.create, blocks.appendChildren, " +
                "blocks.update), or upload first and use ${value.kind.fromUploadName}(id)",
        )

    override fun deserialize(decoder: Decoder): Nothing =
        throw SerializationException(
            "pending_upload is a client-side sentinel and never appears in a Notion API response",
        )
}

/** The `*FromUpload` builder function that takes an already-uploaded id for this kind. */
private val PendingUploadKind.fromUploadName: String
    get() =
        when (this) {
            PendingUploadKind.IMAGE -> "imageFromUpload"
            PendingUploadKind.VIDEO -> "videoFromUpload"
            PendingUploadKind.AUDIO -> "audioFromUpload"
            PendingUploadKind.FILE -> "fileFromUpload"
            PendingUploadKind.PDF -> "pdfFromUpload"
            PendingUploadKind.HTML -> "embedFromUpload"
        }
