package no.saabelit.kotlinnotionclient.models.files

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a file upload object in Notion.
 *
 * File uploads are used to upload files to Notion workspaces via the API.
 * The upload process involves creating a file upload, sending the file content,
 * and optionally completing multi-part uploads.
 */
@Serializable
data class FileUpload(
    @SerialName("id")
    val id: String,
    @SerialName("object")
    val objectType: String = "file_upload",
    @SerialName("created_time")
    val createdTime: String,
    @SerialName("last_edited_time")
    val lastEditedTime: String,
    @SerialName("expiry_time")
    val expiryTime: String,
    @SerialName("upload_url")
    val uploadUrl: String? = null,
    @SerialName("archived")
    val archived: Boolean = false,
    @SerialName("status")
    val status: FileUploadStatus,
    @SerialName("filename")
    val filename: String,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("content_length")
    val contentLength: Long? = null,
)

/**
 * Status of a file upload.
 */
@Serializable
enum class FileUploadStatus {
    @SerialName("pending")
    PENDING,
    @SerialName("uploaded")
    UPLOADED,
    @SerialName("failed")
    FAILED,
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
 * Reference to a file upload for use in blocks.
 * This is used when referencing uploaded files in blocks, not the full FileUpload object.
 */
@Serializable
data class FileUploadReference(
    @SerialName("id")
    val id: String,
)

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