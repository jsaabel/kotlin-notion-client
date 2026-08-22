package it.saabel.kotlinnotionclient.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import it.saabel.kotlinnotionclient.config.NotionApiLimits
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.exceptions.toNotionApiError
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.BlockAppendPosition
import it.saabel.kotlinnotionclient.models.blocks.BlockList
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.PageContentBuilder
import it.saabel.kotlinnotionclient.models.blocks.pageContent
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.utils.FileSource
import it.saabel.kotlinnotionclient.utils.Pagination
import it.saabel.kotlinnotionclient.utils.asFileSource
import it.saabel.kotlinnotionclient.validation.RequestValidator
import it.saabel.kotlinnotionclient.validation.ValidationConfig
import it.saabel.kotlinnotionclient.validation.ValidationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Path

/**
 * API client for Notion Blocks endpoints.
 *
 * Handles operations related to blocks in Notion pages,
 * including retrieving block information and managing block content.
 *
 * Features proactive validation to prevent API errors and provide helpful feedback
 * about content that exceeds Notion's API limits before making HTTP requests.
 */
class BlocksApi(
    private val httpClient: HttpClient,
    private val config: NotionConfig,
    private val validationConfig: ValidationConfig = ValidationConfig.default(),
) {
    private val validator = RequestValidator(validationConfig)

    /**
     * Retrieves a block object using the ID specified.
     *
     * @param blockId The ID of the block to retrieve
     * @return Block object with all properties and content
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieve(blockId: String): Block =
        try {
            val response: HttpResponse = httpClient.get("${config.baseUrl}/blocks/$blockId")

            if (response.status.isSuccess()) {
                response.body<Block>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Retrieves all child blocks for the specified block.
     *
     * Automatically fetches all child blocks by handling pagination transparently.
     * Returns all child blocks in a single list.
     *
     * @param blockId The ID of the parent block
     * @return List of all child blocks across all result pages
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun retrieveChildren(blockId: String): List<Block> {
        val allBlocks = mutableListOf<Block>()
        var currentCursor: String? = null
        var pageCount = 0

        do {
            val response = retrieveChildrenPage(blockId, currentCursor, NotionApiLimits.Response.MAX_PAGE_SIZE)
            allBlocks.addAll(response.results)

            currentCursor = response.nextCursor
            pageCount++

            // Safety check to prevent infinite loops
            val maxPages = 100 // Should be plenty for block children
            if (pageCount >= maxPages) {
                throw NotionException.ApiError(
                    code = "PAGINATION_LIMIT_EXCEEDED",
                    status = 500,
                    details =
                        "Block children retrieval exceeded $maxPages pages. " +
                            "This may indicate an infinite loop or an extremely large block structure.",
                )
            }
        } while (response.hasMore)

        return allBlocks
    }

    /**
     * Retrieves a single page of child blocks.
     *
     * This is the low-level method that handles a single API request. Most users should
     * use the `retrieveChildren` method instead, which automatically handles pagination.
     *
     * @param blockId The ID of the parent block
     * @param startCursor Pagination cursor for retrieving next page of results
     * @param pageSize Number of blocks to return (max 100)
     * @return BlockList containing a single page of child blocks
     */
    private suspend fun retrieveChildrenPage(
        blockId: String,
        startCursor: String? = null,
        pageSize: Int? = null,
    ): BlockList =
        try {
            val url =
                buildString {
                    append("${config.baseUrl}/blocks/$blockId/children")
                    val params = mutableListOf<String>()
                    startCursor?.let { params.add("start_cursor=$it") }
                    pageSize?.let { params.add("page_size=$it") }
                    if (params.isNotEmpty()) {
                        append("?${params.joinToString("&")}")
                    }
                }

            val response: HttpResponse = httpClient.get(url)

            if (response.status.isSuccess()) {
                response.body<BlockList>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    /**
     * Appends child blocks to a parent block or page using a fluent DSL builder.
     *
     * This is a convenience method that accepts a DSL builder lambda for more natural
     * Kotlin-style API usage. The builder provides a fluent API for constructing blocks.
     *
     * @param blockId The ID of the parent block or page
     * @param builder DSL builder lambda for constructing the block content
     * @return BlockList containing the created child blocks
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails in strict mode
     */
    suspend fun appendChildren(
        blockId: String,
        position: BlockAppendPosition? = null,
        builder: PageContentBuilder.() -> Unit,
    ): BlockList {
        val children = pageContent(builder)
        return appendChildren(blockId, children, position)
    }

    /**
     * Appends child blocks to a parent block or page.
     *
     * This method performs proactive validation to check for content that exceeds
     * Notion's API limits before making the HTTP request. Depending on the validation
     * configuration, violations will either cause an exception or be automatically fixed.
     *
     * @param blockId The ID of the parent block or page
     * @param children List of BlockRequest objects to append as children
     * @return BlockList containing the created child blocks
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails in strict mode
     */
    suspend fun appendChildren(
        blockId: String,
        children: List<BlockRequest>,
        position: BlockAppendPosition? = null,
    ): BlockList {
        validator.validateOrThrow("children", children)

        return try {
            val request = AppendBlockChildrenRequest(children = children, position = position)
            val response: HttpResponse =
                httpClient.patch("${config.baseUrl}/blocks/$blockId/children") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (response.status.isSuccess()) {
                response.body<BlockList>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }
    }

    /**
     * Updates the content of an existing block using a fluent DSL builder.
     *
     * This is a convenience method for updating a single block type. The builder provides
     * a fluent API for constructing the block update, but it must result in exactly one block.
     *
     * Note: You cannot change a block's type. The block type in the builder must match
     * the existing block's type.
     *
     * @param blockId The ID of the block to update
     * @param builder DSL builder lambda for constructing the block update
     * @return The updated Block object
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails in strict mode
     * @throws IllegalArgumentException if the builder produces zero or multiple blocks
     */
    suspend fun update(
        blockId: String,
        builder: PageContentBuilder.() -> Unit,
    ): Block {
        val blocks = pageContent(builder)
        require(blocks.size == 1) {
            "Block update builder must produce exactly one block, but produced ${blocks.size} blocks"
        }
        return update(blockId, blocks.first())
    }

    /**
     * Updates the content of an existing block.
     *
     * Note: You cannot change a block's type. Attempting to update a block with a different
     * type will result in an error. Only the content properties of the block can be updated.
     *
     * @param blockId The ID of the block to update
     * @param request The block request with updated content
     * @return The updated Block object
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     * @throws ValidationException if validation fails in strict mode
     */
    suspend fun update(
        blockId: String,
        request: BlockRequest,
    ): Block {
        validator.validateOrThrow("block", listOf(request))

        return try {
            val response: HttpResponse =
                httpClient.patch("${config.baseUrl}/blocks/$blockId") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (response.status.isSuccess()) {
                response.body<Block>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }
    }

    /**
     * Deletes a block by archiving it.
     *
     * In the Notion API, blocks are not permanently deleted but are instead archived.
     * The block will have its `archived` property set to true and can potentially be restored.
     *
     * @param blockId The ID of the block to delete/archive
     * @return The archived Block object with `archived = true`
     * @throws NotionException.NetworkError for network-related failures
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     * @throws NotionException.AuthenticationError for authentication failures
     */
    suspend fun delete(blockId: String): Block =
        try {
            val request = TrashBlockRequest(inTrash = true)
            val response: HttpResponse =
                httpClient.patch("${config.baseUrl}/blocks/$blockId") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            if (response.status.isSuccess()) {
                response.body<Block>()
            } else {
                throw response.toNotionApiError()
            }
        } catch (e: NotionException) {
            throw e // Re-throw our own exceptions
        } catch (e: Exception) {
            throw NotionException.NetworkError(e)
        }

    // ========== Pagination Helper Methods ==========

    /**
     * Retrieves child blocks as a Flow for reactive processing.
     *
     * This method emits individual blocks as they become available, enabling
     * efficient memory usage for large block structures and reactive processing patterns.
     *
     * Example usage:
     * ```kotlin
     * client.blocks.retrieveChildrenAsFlow("block-id").collect { block ->
     *     println("Processing block: ${block.id}")
     *     // Process each block individually
     * }
     * ```
     *
     * @param blockId The ID of the parent block
     * @return Flow<Block> that emits individual child blocks from all result pages
     */
    fun retrieveChildrenAsFlow(blockId: String): Flow<Block> =
        Pagination.asFlow { cursor ->
            retrieveChildrenPage(
                blockId,
                startCursor = cursor,
                pageSize = NotionApiLimits.Response.MAX_PAGE_SIZE,
            )
        }

    /**
     * Retrieves child blocks and returns response pages as a Flow.
     *
     * Unlike [retrieveChildrenAsFlow], this emits complete [BlockList] objects,
     * allowing access to pagination metadata alongside results.
     *
     * Example usage:
     * ```kotlin
     * client.blocks.retrieveChildrenPagedFlow("block-id").collect { response ->
     *     println("Got ${response.results.size} blocks (has more: ${response.hasMore})")
     *     response.results.forEach { block -> /* process block */ }
     * }
     * ```
     *
     * @param blockId The ID of the parent block
     * @return Flow<BlockList> that emits complete response pages
     */
    fun retrieveChildrenPagedFlow(blockId: String): Flow<BlockList> =
        Pagination.asPagesFlow { cursor ->
            retrieveChildrenPage(
                blockId,
                startCursor = cursor,
                pageSize = NotionApiLimits.Response.MAX_PAGE_SIZE,
            )
        }

    /**
     * Retrieves a single page of child blocks without auto-paginating.
     *
     * Unlike [retrieveChildren], which transparently fetches all child blocks, this method
     * makes exactly one API call and returns the raw [BlockList] — including the cursor and
     * [hasMore] flag so the caller can decide whether and how to continue.
     *
     * Use this when you only need the first N blocks (e.g. a preview of page content)
     * and do not want to load the entire block tree.
     *
     * Example:
     * ```kotlin
     * val response = notion.blocks.retrieveChildrenFirstPage(pageId, pageSize = 5)
     * val preview = response.results     // at most 5 blocks
     * val hasMore = response.hasMore     // true if more blocks exist
     * val cursor = response.nextCursor   // use for manual follow-up calls if needed
     * ```
     *
     * @param blockId The ID of the parent block or page
     * @param pageSize Number of blocks to return (1–100; defaults to 100)
     * @param startCursor Cursor from a previous response to continue from a specific position
     * @return [BlockList] for the requested page of child blocks
     */
    suspend fun retrieveChildrenFirstPage(
        blockId: String,
        pageSize: Int = NotionApiLimits.Response.MAX_PAGE_SIZE,
        startCursor: String? = null,
    ): BlockList = retrieveChildrenPage(blockId, startCursor, pageSize)

    // ---------------------------------------------------------------------
    // Upload-and-attach helpers
    //
    // Thin compositions over EnhancedFileUploadApi plus the existing append
    // path: upload the bytes, wait until Notion reports the upload as ready,
    // then append the matching block. They throw like the rest of the client
    // rather than returning FileUploadResult — see FileUploadResult.getOrThrow.
    // ---------------------------------------------------------------------

    private val uploads by lazy { EnhancedFileUploadApi(httpClient, config) }

    /**
     * Notion picks the embed's renderer from the uploaded file's extension, so an HTML payload
     * saved under any other name silently becomes a plain file attachment.
     */
    private fun String.withHtmlExtension(): String =
        if (endsWith(".html", ignoreCase = true) || endsWith(".htm", ignoreCase = true)) this else "$this.html"

    /**
     * Uploads a file and appends it to a page or block as an image block, in one call.
     *
     * Replaces the manual create → send → wait → attach dance:
     * ```kotlin
     * notion.blocks.appendImage(pageId, File("diagram.png"), caption = "…")
     * ```
     *
     * @param blockId The ID of the parent block or page
     * @param source The file to upload
     * @param caption Optional caption text
     * @param position Optional insertion position; appends at the end when omitted
     * @param options Upload options — content type override, progress callback, validation
     * @return BlockList containing the created block
     * @throws it.saabel.kotlinnotionclient.models.files.FileUploadError if the upload fails
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun appendImage(
        blockId: String,
        source: FileSource,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList {
        val upload = uploads.uploadAndAwait(source, options)
        return appendChildren(blockId, position) { imageFromUpload(upload, caption) }
    }

    /** Uploads [file] and appends it as an image block. See [appendImage]. */
    suspend fun appendImage(
        blockId: String,
        file: File,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendImage(blockId, file.asFileSource(), caption, position, options)

    /** Uploads the file at [path] and appends it as an image block. See [appendImage]. */
    suspend fun appendImage(
        blockId: String,
        path: Path,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendImage(blockId, path.asFileSource(), caption, position, options)

    /**
     * Uploads a file and appends it to a page or block as a video block, in one call.
     *
     * Replaces the manual create → send → wait → attach dance:
     * ```kotlin
     * notion.blocks.appendVideo(pageId, File("demo.mp4"), caption = "…")
     * ```
     *
     * @param blockId The ID of the parent block or page
     * @param source The file to upload
     * @param caption Optional caption text
     * @param position Optional insertion position; appends at the end when omitted
     * @param options Upload options — content type override, progress callback, validation
     * @return BlockList containing the created block
     * @throws it.saabel.kotlinnotionclient.models.files.FileUploadError if the upload fails
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun appendVideo(
        blockId: String,
        source: FileSource,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList {
        val upload = uploads.uploadAndAwait(source, options)
        return appendChildren(blockId, position) { videoFromUpload(upload, caption) }
    }

    /** Uploads [file] and appends it as a video block. See [appendVideo]. */
    suspend fun appendVideo(
        blockId: String,
        file: File,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendVideo(blockId, file.asFileSource(), caption, position, options)

    /** Uploads the file at [path] and appends it as a video block. See [appendVideo]. */
    suspend fun appendVideo(
        blockId: String,
        path: Path,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendVideo(blockId, path.asFileSource(), caption, position, options)

    /**
     * Uploads a file and appends it to a page or block as an audio block, in one call.
     *
     * Replaces the manual create → send → wait → attach dance:
     * ```kotlin
     * notion.blocks.appendAudio(pageId, File("narration.mp3"), caption = "…")
     * ```
     *
     * @param blockId The ID of the parent block or page
     * @param source The file to upload
     * @param caption Optional caption text
     * @param position Optional insertion position; appends at the end when omitted
     * @param options Upload options — content type override, progress callback, validation
     * @return BlockList containing the created block
     * @throws it.saabel.kotlinnotionclient.models.files.FileUploadError if the upload fails
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun appendAudio(
        blockId: String,
        source: FileSource,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList {
        val upload = uploads.uploadAndAwait(source, options)
        return appendChildren(blockId, position) { audioFromUpload(upload, caption) }
    }

    /** Uploads [file] and appends it as an audio block. See [appendAudio]. */
    suspend fun appendAudio(
        blockId: String,
        file: File,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendAudio(blockId, file.asFileSource(), caption, position, options)

    /** Uploads the file at [path] and appends it as an audio block. See [appendAudio]. */
    suspend fun appendAudio(
        blockId: String,
        path: Path,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendAudio(blockId, path.asFileSource(), caption, position, options)

    /**
     * Uploads a file and appends it to a page or block as a PDF block, in one call.
     *
     * Replaces the manual create → send → wait → attach dance:
     * ```kotlin
     * notion.blocks.appendPdf(pageId, File("report.pdf"), caption = "…")
     * ```
     *
     * @param blockId The ID of the parent block or page
     * @param source The file to upload
     * @param caption Optional caption text
     * @param position Optional insertion position; appends at the end when omitted
     * @param options Upload options — content type override, progress callback, validation
     * @return BlockList containing the created block
     * @throws it.saabel.kotlinnotionclient.models.files.FileUploadError if the upload fails
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun appendPdf(
        blockId: String,
        source: FileSource,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList {
        val upload = uploads.uploadAndAwait(source, options)
        return appendChildren(blockId, position) { pdfFromUpload(upload, caption) }
    }

    /** Uploads [file] and appends it as a PDF block. See [appendPdf]. */
    suspend fun appendPdf(
        blockId: String,
        file: File,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendPdf(blockId, file.asFileSource(), caption, position, options)

    /** Uploads the file at [path] and appends it as a PDF block. See [appendPdf]. */
    suspend fun appendPdf(
        blockId: String,
        path: Path,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendPdf(blockId, path.asFileSource(), caption, position, options)

    /**
     * Uploads a file and appends it as a file block, in one call.
     *
     * ```kotlin
     * notion.blocks.appendFile(pageId, Paths.get("report.pdf"))
     * ```
     *
     * @param blockId The ID of the parent block or page
     * @param source The file to upload
     * @param name Optional display name; defaults to the upload's own filename
     * @param caption Optional caption text
     * @param position Optional insertion position; appends at the end when omitted
     * @param options Upload options — content type override, progress callback, validation
     * @return BlockList containing the created block
     * @throws it.saabel.kotlinnotionclient.models.files.FileUploadError if the upload fails
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun appendFile(
        blockId: String,
        source: FileSource,
        name: String? = null,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList {
        val upload = uploads.uploadAndAwait(source, options)
        return appendChildren(blockId, position) { fileFromUpload(upload, name, caption) }
    }

    /** Uploads [file] and appends it as a file block. See [appendFile]. */
    suspend fun appendFile(
        blockId: String,
        file: File,
        name: String? = null,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendFile(blockId, file.asFileSource(), name, caption, position, options)

    /** Uploads the file at [path] and appends it as a file block. See [appendFile]. */
    suspend fun appendFile(
        blockId: String,
        path: Path,
        name: String? = null,
        caption: String? = null,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendFile(blockId, path.asFileSource(), name, caption, position, options)

    /**
     * Uploads raw HTML and appends it as an HTML block, in one call.
     *
     * Notion has no `html` block type: an HTML block is an `embed` whose `file_upload` points at
     * an uploaded `.html` file (Jul 3 2026 changelog). Nothing about that is guessable from the
     * API surface, which is exactly why this helper exists:
     * ```kotlin
     * notion.blocks.appendHtml(pageId, "<h1>Report</h1><p>…</p>")
     * ```
     *
     * @param blockId The ID of the parent block or page
     * @param html The HTML document or fragment to upload
     * @param filename Name for the uploaded file; `.html` is appended unless the name already
     *   ends in `.html` or `.htm`, because Notion decides how to render the embed from the
     *   file's extension
     * @param position Optional insertion position; appends at the end when omitted
     * @param options Upload options — content type override, progress callback, validation
     * @return BlockList containing the created block
     * @throws it.saabel.kotlinnotionclient.models.files.FileUploadError if the upload fails
     * @throws NotionException.ApiError for API-related errors (4xx, 5xx responses)
     */
    suspend fun appendHtml(
        blockId: String,
        html: String,
        filename: String = "embed.html",
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList =
        appendHtml(
            blockId = blockId,
            source = html.toByteArray().asFileSource(filename.withHtmlExtension()),
            position = position,
            options = options,
        )

    /**
     * Uploads an HTML file and appends it as an HTML block. See [appendHtml].
     *
     * @param blockId The ID of the parent block or page
     * @param source The `.html` file to upload
     * @param position Optional insertion position; appends at the end when omitted
     * @param options Upload options — content type override, progress callback, validation
     * @return BlockList containing the created block
     */
    suspend fun appendHtml(
        blockId: String,
        source: FileSource,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList {
        val upload = uploads.uploadAndAwait(source, options)
        return appendChildren(blockId, position) { embedFromUpload(upload) }
    }

    /** Uploads the HTML file [file] and appends it as an HTML block. See [appendHtml]. */
    suspend fun appendHtml(
        blockId: String,
        file: File,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendHtml(blockId, file.asFileSource(), position, options)

    /** Uploads the HTML file at [path] and appends it as an HTML block. See [appendHtml]. */
    suspend fun appendHtml(
        blockId: String,
        path: Path,
        position: BlockAppendPosition? = null,
        options: FileUploadOptions = FileUploadOptions(),
    ): BlockList = appendHtml(blockId, path.asFileSource(), position, options)
}

/**
 * Request body for appending children to a block.
 */
@Serializable
private data class AppendBlockChildrenRequest(
    val children: List<BlockRequest>,
    val position: BlockAppendPosition? = null,
)

/**
 * Request body for trashing/deleting a block.
 */
@Serializable
private data class TrashBlockRequest(
    @SerialName("in_trash")
    val inTrash: Boolean,
)
