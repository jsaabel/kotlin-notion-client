package no.saabelit.kotlinnotionclient.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.files.CreateFileUploadRequest
import no.saabelit.kotlinnotionclient.models.files.FileUpload
import no.saabelit.kotlinnotionclient.models.files.FileUploadList

/**
 * API client for file upload operations.
 *
 * This class provides methods to upload files to Notion workspaces using the File Upload API.
 * It supports single-part uploads, multi-part uploads for large files, and external URL imports.
 *
 * @property client The HTTP client used for making API requests
 * @property config The Notion client configuration
 */
class FileUploadApi(
    private val client: HttpClient,
    private val config: NotionConfig,
) {
    companion object {
    }

    /**
     * Creates a file upload.
     *
     * Initiates the process of uploading a file to your Notion workspace.
     * For a successful request, the response is a FileUpload object with a status of "pending".
     *
     * @param request The file upload creation request
     * @return The created FileUpload object
     */
    suspend fun createFileUpload(request: CreateFileUploadRequest): FileUpload {
        return client.post("${config.baseUrl}/file_uploads") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${config.token}")
                append("Notion-Version", config.apiVersion)
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Sends file content to a file upload.
     *
     * Transmits file contents to Notion for a file upload.
     * This endpoint uses multipart/form-data for the content type.
     *
     * @param fileUploadId The ID of the file upload
     * @param fileContent The file content as a byte array
     * @param partNumber Optional part number for multi-part uploads (1-1000)
     * @return The updated FileUpload object
     */
    suspend fun sendFileUpload(
        fileUploadId: String,
        fileContent: ByteArray,
        partNumber: Int? = null,
    ): FileUpload {
        return client.submitFormWithBinaryData(
            url = "${config.baseUrl}/file_uploads/$fileUploadId/send",
            formData = formData {
                append("file", fileContent, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"file\"")
                })
                partNumber?.let {
                    append("part_number", it.toString())
                }
            }
        ) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${config.token}")
                append("Notion-Version", config.apiVersion)
            }
        }.body()
    }

    /**
     * Completes a multi-part file upload.
     *
     * Finalizes a multi-part file upload after all parts have been sent successfully.
     *
     * @param fileUploadId The ID of the file upload to complete
     * @return The completed FileUpload object
     */
    suspend fun completeFileUpload(fileUploadId: String): FileUpload {
        return client.post("${config.baseUrl}/file_uploads/$fileUploadId/complete") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${config.token}")
                append("Notion-Version", config.apiVersion)
            }
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>()) // Empty body for completion
        }.body()
    }

    /**
     * Retrieves a file upload.
     *
     * Gets the details of a FileUpload object.
     *
     * @param fileUploadId The ID of the file upload to retrieve
     * @return The FileUpload object
     */
    suspend fun retrieveFileUpload(fileUploadId: String): FileUpload {
        return client.get("${config.baseUrl}/file_uploads/$fileUploadId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${config.token}")
                append("Notion-Version", config.apiVersion)
            }
        }.body()
    }

    /**
     * Lists file uploads.
     *
     * Retrieves file uploads for the current bot integration, sorted by most recent first.
     *
     * @param startCursor Pagination cursor for the next page
     * @param pageSize Number of results per page (max 100)
     * @return List of FileUpload objects with pagination info
     */
    suspend fun listFileUploads(
        startCursor: String? = null,
        pageSize: Int? = null,
    ): FileUploadList {
        return client.get("${config.baseUrl}/file_uploads") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${config.token}")
                append("Notion-Version", config.apiVersion)
            }
            startCursor?.let { parameter("start_cursor", it) }
            pageSize?.let { parameter("page_size", it) }
        }.body()
    }

    /**
     * Convenience method to upload a file in a single operation.
     *
     * This method handles the complete upload flow for single-part uploads:
     * 1. Creates the file upload
     * 2. Sends the file content
     * 3. Returns the completed upload
     *
     * @param filename The name of the file
     * @param contentType The MIME type of the file
     * @param fileContent The file content as a byte array
     * @return The completed FileUpload object
     */
    suspend fun uploadFile(
        filename: String,
        contentType: String,
        fileContent: ByteArray,
    ): FileUpload {
        // Create the file upload
        val upload = createFileUpload(
            CreateFileUploadRequest(
                filename = filename,
                contentType = contentType,
            )
        )

        // Send the file content
        return sendFileUpload(
            fileUploadId = upload.id,
            fileContent = fileContent,
        )
    }

    /**
     * Convenience method to upload a large file using multi-part upload.
     *
     * This method handles the complete upload flow for multi-part uploads:
     * 1. Creates the multi-part file upload
     * 2. Sends each part
     * 3. Completes the upload
     *
     * @param filename The name of the file
     * @param contentType The MIME type of the file
     * @param parts List of file parts as byte arrays
     * @return The completed FileUpload object
     */
    suspend fun uploadFileMultiPart(
        filename: String,
        contentType: String,
        parts: List<ByteArray>,
    ): FileUpload {
        // Create the multi-part file upload
        val upload = createFileUpload(
            CreateFileUploadRequest(
                mode = no.saabelit.kotlinnotionclient.models.files.FileUploadMode.MULTI_PART,
                filename = filename,
                contentType = contentType,
                numberOfParts = parts.size,
            )
        )

        // Send each part
        parts.forEachIndexed { index, partContent ->
            sendFileUpload(
                fileUploadId = upload.id,
                fileContent = partContent,
                partNumber = index + 1, // Part numbers are 1-based
            )
        }

        // Complete the upload
        return completeFileUpload(upload.id)
    }

    /**
     * Imports a file from an external URL.
     *
     * This method creates a file upload that imports content from a publicly accessible URL.
     *
     * @param filename The name to give the imported file
     * @param externalUrl The HTTPS URL of the file to import
     * @param contentType Optional MIME type of the file
     * @return The FileUpload object
     */
    suspend fun importExternalFile(
        filename: String,
        externalUrl: String,
        contentType: String? = null,
    ): FileUpload {
        return createFileUpload(
            CreateFileUploadRequest(
                mode = no.saabelit.kotlinnotionclient.models.files.FileUploadMode.EXTERNAL_URL,
                filename = filename,
                contentType = contentType,
                externalUrl = externalUrl,
            )
        )
    }
}