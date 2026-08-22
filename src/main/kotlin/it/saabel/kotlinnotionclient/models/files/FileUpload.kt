@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.files

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Represents a file upload object in Notion.
 *
 * File uploads are used to upload files to Notion workspaces via the API.
 * The upload process involves creating a file upload, sending the file content,
 * and optionally completing multi-part uploads.
 *
 * Field shapes follow the
 * [File Upload object reference](https://developers.notion.com/reference/file-upload).
 * Fields documented as conditional — [uploadUrl], [completeUrl], [fileImportResult] — are
 * absent for uploads in states that don't produce them, so they default to null.
 */
@Serializable
data class FileUpload(
    @SerialName("id")
    val id: String,
    @SerialName("object")
    val objectType: String = "file_upload",
    @SerialName("created_time")
    val createdTime: String,
    @SerialName("created_by")
    val createdBy: FileUploadCreatedBy? = null,
    @SerialName("last_edited_time")
    val lastEditedTime: String,
    @SerialName("expiry_time")
    val expiryTime: String? = null,
    @SerialName("upload_url")
    val uploadUrl: String? = null,
    /**
     * URL used to complete a multi-part upload. Only present on `pending` uploads created
     * with [FileUploadMode.MULTI_PART]; [FileUploadApi.completeFileUpload] derives the same
     * URL from the upload id, so this is modelled for parity rather than because it is required.
     */
    @SerialName("complete_url")
    val completeUrl: String? = null,
    @SerialName("in_trash")
    val inTrash: Boolean = false,
    @SerialName("status")
    val status: FileUploadStatus,
    /**
     * Nullable per the API reference: for single-part uploads created without a filename it stays
     * null until the send step, where it can be inferred from the form data.
     */
    @SerialName("filename")
    val filename: String? = null,
    /**
     * Nullable per the API reference: for single-part uploads it can remain null until the send
     * step and be inferred from the `file` part's content type.
     */
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("content_length")
    val contentLength: Long? = null,
    /**
     * Running part counts for a [FileUploadMode.MULTI_PART] upload.
     */
    @SerialName("number_of_parts")
    val numberOfParts: FileUploadPartCounts? = null,
    /**
     * Outcome of an [FileUploadMode.EXTERNAL_URL] import. Only present once such an upload has
     * reached [FileUploadStatus.UPLOADED] or [FileUploadStatus.FAILED], and the only place the
     * API reports *why* an import failed.
     */
    @SerialName("file_import_result")
    val fileImportResult: FileImportResult? = null,
) {
    /** The import error, if this upload is an external-URL import that failed. */
    val importError: FileImportError?
        get() = (fileImportResult as? FileImportResult.Error)?.error
}

/**
 * The principal that created a file upload.
 *
 * Deliberately looser than [it.saabel.kotlinnotionclient.models.users.User]: the API reference
 * documents only `id` and `type` here, and `type` admits `agent` alongside `person` and `bot`,
 * so [type] is kept as the raw string rather than an enum that would reject a future value.
 */
@Serializable
data class FileUploadCreatedBy(
    @SerialName("id")
    val id: String,
    @SerialName("object")
    val objectType: String? = null,
    @SerialName("type")
    val type: String? = null,
)

/**
 * Part counts for a multi-part file upload.
 *
 * @property total Total number of parts the upload was created with.
 * @property sent Number of parts received by Notion so far.
 */
@Serializable
data class FileUploadPartCounts(
    @SerialName("total")
    val total: Int,
    @SerialName("sent")
    val sent: Int,
)

/**
 * Status of a file upload.
 *
 * [EXPIRED] and [FAILED] are terminal: an upload in either state can no longer be used and a new
 * one must be created. [FAILED] is only produced for [FileUploadMode.EXTERNAL_URL] uploads whose
 * import was unsuccessful — see [FileUpload.fileImportResult] for the reason.
 *
 * An unrecognized status decodes to [UNKNOWN] rather than failing deserialization, mirroring the
 * `Unknown` fallbacks on `RollupResult`/`FormulaResult`, so a status Notion adds in the future
 * degrades gracefully instead of breaking every file-upload call.
 */
@Serializable(with = FileUploadStatusSerializer::class)
enum class FileUploadStatus(
    val wireValue: String,
) {
    PENDING("pending"),
    UPLOADED("uploaded"),
    EXPIRED("expired"),
    FAILED("failed"),

    /** A status this library does not know yet. */
    UNKNOWN("unknown"),
    ;

    /** True for statuses an upload can never leave — nothing is worth waiting for. */
    val isTerminal: Boolean
        get() = this == UPLOADED || this == EXPIRED || this == FAILED

    /** True for the terminal statuses that mean the upload can no longer be used. */
    val isUnusable: Boolean
        get() = this == EXPIRED || this == FAILED
}

/**
 * Mode for file upload.
 */
@Serializable
enum class FileUploadMode {
    @SerialName("single_part")
    SINGLE_PART,

    @SerialName("multi_part")
    MULTI_PART,

    @SerialName("external_url")
    EXTERNAL_URL,
}

/**
 * Outcome of importing a file from an external URL.
 *
 * Reported on [FileUpload.fileImportResult] for uploads created with
 * [FileUploadMode.EXTERNAL_URL], and the only way to learn why an import failed.
 * Unrecognized `type` values decode to [Unknown] rather than failing deserialization.
 */
@Serializable(with = FileImportResultSerializer::class)
sealed class FileImportResult {
    abstract val type: String

    /** ISO 8601 timestamp of the import attempt, when the API reports one. */
    abstract val importedTime: String?

    /** The import succeeded; the upload's status moves to [FileUploadStatus.UPLOADED]. */
    @Serializable
    data class Success(
        @SerialName("type")
        override val type: String = "success",
        @SerialName("imported_time")
        override val importedTime: String? = null,
        /** Documented as an empty object today; kept raw so added fields aren't dropped. */
        @SerialName("success")
        val success: JsonObject? = null,
    ) : FileImportResult()

    /** The import failed; the upload's status moves to [FileUploadStatus.FAILED]. */
    @Serializable
    data class Error(
        @SerialName("type")
        override val type: String = "error",
        @SerialName("imported_time")
        override val importedTime: String? = null,
        @SerialName("error")
        val error: FileImportError,
    ) : FileImportResult()

    /** A result type this library does not know yet, with the raw JSON preserved. */
    @Serializable
    data class Unknown(
        @SerialName("type")
        override val type: String,
        @SerialName("imported_time")
        override val importedTime: String? = null,
        val rawContent: JsonElement,
    ) : FileImportResult()
}

/**
 * Details of a failed external-URL import.
 *
 * @property type Error category, e.g. `validation_error`, `download_error`, `upload_error`,
 * `internal_system_error`.
 * @property code Machine-readable code, e.g. `file_upload_invalid_size`.
 * @property message Human-readable explanation from Notion.
 */
@Serializable
data class FileImportError(
    @SerialName("type")
    val type: String,
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
    @SerialName("parameter")
    val parameter: String? = null,
    @SerialName("status_code")
    val statusCode: Int? = null,
)

/**
 * Request model for creating a file upload.
 */
@Serializable
data class CreateFileUploadRequest(
    @SerialName("mode")
    val mode: FileUploadMode = FileUploadMode.SINGLE_PART,
    @SerialName("filename")
    val filename: String? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("number_of_parts")
    val numberOfParts: Int? = null,
    @SerialName("external_url")
    val externalUrl: String? = null,
)

/**
 * Reference to a file upload, used wherever an uploaded file is attached — blocks, page
 * properties, icons and covers.
 *
 * Kept as an alias so that the one canonical type lives beside the other file-object primitives
 * in `models.base`, while code that already imports it from `models.files` keeps compiling.
 */
typealias FileUploadReference = it.saabel.kotlinnotionclient.models.base.FileUploadReference

/**
 * Response model for listing file uploads.
 */
@Serializable
data class FileUploadList(
    @SerialName("object")
    val objectType: String = "list",
    @SerialName("results")
    val results: List<FileUpload>,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    @SerialName("has_more")
    val hasMore: Boolean = false,
)
