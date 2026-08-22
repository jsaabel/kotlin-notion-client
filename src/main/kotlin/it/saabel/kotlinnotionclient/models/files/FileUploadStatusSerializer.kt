package it.saabel.kotlinnotionclient.models.files

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializer for [FileUploadStatus].
 *
 * Dispatches on the wire string to the matching known status and falls back to
 * [FileUploadStatus.UNKNOWN] for anything it does not recognize, so a status Notion adds in
 * the future degrades gracefully instead of failing deserialization of the whole file upload.
 * This mirrors the `Unknown` fallbacks on `RollupResult`/`FormulaResult`, adapted to an enum.
 */
object FileUploadStatusSerializer : KSerializer<FileUploadStatus> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FileUploadStatus", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): FileUploadStatus {
        val raw = decoder.decodeString()
        return FileUploadStatus.entries.firstOrNull { it.wireValue == raw }
            ?: FileUploadStatus.UNKNOWN
    }

    override fun serialize(
        encoder: Encoder,
        value: FileUploadStatus,
    ) {
        encoder.encodeString(value.wireValue)
    }
}
