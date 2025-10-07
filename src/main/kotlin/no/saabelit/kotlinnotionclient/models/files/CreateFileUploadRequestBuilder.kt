@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.files

/**
 * DSL marker to prevent nested scopes in file upload builders.
 */
@DslMarker
annotation class FileUploadDslMarker

/**
 * Builder class for creating file upload requests with a fluent DSL.
 *
 * This builder provides a convenient way to construct CreateFileUploadRequest objects
 * with significantly less boilerplate than manual construction.
 *
 * ## Single-Part Upload Example:
 * ```kotlin
 * val request = createFileUploadRequest {
 *     filename("document.pdf")
 *     contentType("application/pdf")
 * }
 * ```
 *
 * ## Multi-Part Upload Example:
 * ```kotlin
 * val request = createFileUploadRequest {
 *     multiPart()
 *     filename("large-video.mp4")
 *     contentType("video/mp4")
 *     numberOfParts(5)
 * }
 * ```
 *
 * **Note**: For automatic part calculation based on file size, use EnhancedFileUploadApi instead.
 *
 * ## External URL Import Example:
 * ```kotlin
 * val request = createFileUploadRequest {
 *     filename("remote-image.jpg")
 *     contentType("image/jpeg")
 *     externalUrl("https://example.com/image.jpg") // Sets mode and URL
 * }
 * ```
 *
 * ## Minimal Example (with defaults):
 * ```kotlin
 * val request = createFileUploadRequest {
 *     filename("simple.txt")
 *     // mode defaults to SINGLE_PART
 *     // contentType can be auto-detected
 * }
 * ```
 */
@FileUploadDslMarker
class CreateFileUploadRequestBuilder {
    private var modeValue: FileUploadMode = FileUploadMode.SINGLE_PART
    private var modeWasExplicitlySet: Boolean = false
    private var filenameValue: String? = null
    private var contentTypeValue: String? = null
    private var numberOfPartsValue: Int? = null
    private var externalUrlValue: String? = null

    /**
     * Sets the upload mode.
     *
     * - `SINGLE_PART`: Default mode for files under 20MB
     * - `MULTI_PART`: Required for files larger than 20MB
     * - `EXTERNAL_URL`: Import from a publicly accessible URL
     *
     * @param mode The file upload mode
     */
    fun mode(mode: FileUploadMode) {
        modeValue = mode
        modeWasExplicitlySet = true
    }

    /**
     * Sets the upload mode to single-part (default).
     *
     * Use for files under 20MB that can be uploaded in one request.
     */
    fun singlePart() {
        modeValue = FileUploadMode.SINGLE_PART
        modeWasExplicitlySet = true
    }

    /**
     * Sets the upload mode to multi-part.
     *
     * Required for files larger than 20MB. Must also specify numberOfParts().
     */
    fun multiPart() {
        modeValue = FileUploadMode.MULTI_PART
        modeWasExplicitlySet = true
    }

    /**
     * Sets the filename for the uploaded file.
     *
     * Required for multi-part and external URL uploads.
     * Optional for single-part uploads (used to override the filename).
     * Should include a file extension or ensure contentType is specified for proper extension inference.
     *
     * @param filename The name of the file
     */
    fun filename(filename: String) {
        filenameValue = filename
    }

    /**
     * Sets the MIME type of the file.
     *
     * Recommended for multi-part uploads and when the filename doesn't have an extension.
     * Must match the actual content type of the file being uploaded.
     *
     * @param contentType The MIME type (e.g., "application/pdf", "image/jpeg")
     */
    fun contentType(contentType: String) {
        contentTypeValue = contentType
    }

    /**
     * Sets the number of parts for multi-part uploads.
     *
     * Required when mode is MULTI_PART. Must be between 1 and 1,000.
     * This must match the actual number of parts that will be uploaded.
     *
     * **Note**: For most use cases, consider using the EnhancedFileUploadApi instead,
     * which automatically calculates optimal part counts based on file size.
     *
     * @param numberOfParts The number of parts to upload
     * @throws IllegalArgumentException if numberOfParts is not between 1 and 1,000
     */
    fun numberOfParts(numberOfParts: Int) {
        if (numberOfParts !in 1..1000) {
            throw IllegalArgumentException("Number of parts must be between 1 and 1,000, but was $numberOfParts")
        }
        numberOfPartsValue = numberOfParts
    }

    /**
     * Sets the external URL to import from and automatically sets mode to EXTERNAL_URL if no mode was explicitly set.
     *
     * This convenience method sets the URL and, if the mode is still the default SINGLE_PART,
     * automatically changes it to EXTERNAL_URL. If an explicit mode was already set, this will
     * validate that external URLs are only used with EXTERNAL_URL mode.
     *
     * Must be a publicly accessible HTTPS URL.
     *
     * @param url The HTTPS URL of the file to import
     * @throws IllegalArgumentException if the URL is not HTTPS
     * @throws IllegalStateException if mode is explicitly set to something other than EXTERNAL_URL
     */
    fun externalUrl(url: String) {
        if (!url.startsWith("https://")) {
            throw IllegalArgumentException("External URL must be HTTPS, but was: $url")
        }

        // Check if mode was explicitly set to something incompatible
        if (modeWasExplicitlySet && modeValue != FileUploadMode.EXTERNAL_URL) {
            when (modeValue) {
                FileUploadMode.SINGLE_PART -> throw IllegalStateException("External URL should not be specified for single-part uploads")
                FileUploadMode.MULTI_PART -> throw IllegalStateException("External URL should not be specified for multi-part uploads")
                else -> throw IllegalStateException("External URL can only be used with EXTERNAL_URL mode, but mode is set to $modeValue")
            }
        }

        // Auto-set mode if it's still default
        if (modeValue == FileUploadMode.SINGLE_PART && !modeWasExplicitlySet) {
            modeValue = FileUploadMode.EXTERNAL_URL
        }

        externalUrlValue = url
    }

    /**
     * Builds the CreateFileUploadRequest from the configured values.
     *
     * @return The constructed CreateFileUploadRequest
     * @throws IllegalStateException if required fields are not set for the specified mode
     */
    internal fun build(): CreateFileUploadRequest {
        // Validate mode-specific requirements
        when (modeValue) {
            FileUploadMode.MULTI_PART -> {
                if (filenameValue == null) {
                    throw IllegalStateException("Filename is required for multi-part uploads")
                }
                if (numberOfPartsValue == null) {
                    throw IllegalStateException("Number of parts is required for multi-part uploads")
                }
            }
            FileUploadMode.EXTERNAL_URL -> {
                if (filenameValue == null) {
                    throw IllegalStateException("Filename is required for external URL uploads")
                }
                if (externalUrlValue == null) {
                    throw IllegalStateException("External URL is required for external URL uploads")
                }
            }
            FileUploadMode.SINGLE_PART -> {
                // Single-part uploads are more flexible with requirements
                if (numberOfPartsValue != null) {
                    throw IllegalStateException("Number of parts should not be specified for single-part uploads")
                }
                if (externalUrlValue != null) {
                    throw IllegalStateException("External URL should not be specified for single-part uploads")
                }
            }
        }

        return CreateFileUploadRequest(
            mode = modeValue,
            filename = filenameValue,
            contentType = contentTypeValue,
            numberOfParts = numberOfPartsValue,
            externalUrl = externalUrlValue,
        )
    }
}

/**
 * Entry point function for the file upload request DSL.
 *
 * Creates a CreateFileUploadRequest using a fluent builder pattern.
 *
 * @param block The DSL block for building the file upload request
 * @return The constructed CreateFileUploadRequest
 */
fun createFileUploadRequest(block: CreateFileUploadRequestBuilder.() -> Unit): CreateFileUploadRequest =
    CreateFileUploadRequestBuilder().apply(block).build()
