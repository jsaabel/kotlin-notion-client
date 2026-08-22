package it.saabel.kotlinnotionclient.api

import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.blocks.AudioRequestContent
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.EmbedRequestContent
import it.saabel.kotlinnotionclient.models.blocks.FileRequestContent
import it.saabel.kotlinnotionclient.models.blocks.ImageRequestContent
import it.saabel.kotlinnotionclient.models.blocks.PDFRequestContent
import it.saabel.kotlinnotionclient.models.blocks.PendingUploadKind
import it.saabel.kotlinnotionclient.models.blocks.VideoRequestContent
import it.saabel.kotlinnotionclient.models.blocks.childrenOf
import it.saabel.kotlinnotionclient.models.blocks.collectPendingUploads
import it.saabel.kotlinnotionclient.models.blocks.withChildren
import it.saabel.kotlinnotionclient.models.comments.CommentAttachmentRequest
import it.saabel.kotlinnotionclient.models.comments.CreateCommentRequest
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseRequest
import it.saabel.kotlinnotionclient.models.databases.UpdateDatabaseRequest
import it.saabel.kotlinnotionclient.models.datasources.UpdateDataSourceRequest
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.files.FileUploadReference
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import it.saabel.kotlinnotionclient.models.pages.FileObject
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.UpdatePageRequest
import it.saabel.kotlinnotionclient.utils.FileSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * How many of a call's files are uploaded at once.
 *
 * Request pacing is already handled globally by the `NotionRateLimit` plugin, so this bounds
 * something else: the memory and open streams held by concurrent multipart bodies.
 */
private const val MAX_CONCURRENT_UPLOADS = 4

/**
 * Uploads every [BlockRequest.PendingUpload] in [blocks] and returns the tree with each one
 * replaced by the equivalent `file_upload` block.
 *
 * This is the resolution step behind the file-taking builder overloads: sentinels travel
 * through the synchronous DSL inside the block tree, and every suspending entry point that
 * accepts blocks runs them through here before validating and sending. See
 * `docs/adr/0001-deferred-file-upload-resolution.md`.
 *
 * Uploads run concurrently, bounded by [MAX_CONCURRENT_UPLOADS]. Structured concurrency makes
 * failure atomic from the caller's perspective: the first failed upload cancels its siblings
 * and this function throws
 * [FileUploadError][it.saabel.kotlinnotionclient.models.files.FileUploadError] before the
 * request that would have carried the blocks is ever sent. Uploads that had already completed
 * are orphaned deliberately — Notion exposes no delete-upload endpoint and unattached uploads
 * expire an hour after creation.
 *
 * Blocks with no sentinel in them are returned untouched (identical instance), so this costs
 * one tree walk on the overwhelmingly common path.
 */
internal suspend fun EnhancedFileUploadApi.resolvePendingUploads(blocks: List<BlockRequest>): List<BlockRequest> {
    val pending = collectPendingUploads(blocks).map { PendingFile(it.source, it.options) }
    if (pending.isEmpty()) return blocks

    return substitute(blocks, uploadAll(pending).iterator())
}

/**
 * One local file waiting to be uploaded, lifted out of whichever sentinel recorded it.
 *
 * Flattening the five sentinel types to this pair is what lets a single page create pool the
 * files from its icon, its cover, its files properties and its content blocks into one upload
 * pass with one failure boundary.
 */
private data class PendingFile(
    val source: FileSource,
    val options: FileUploadOptions,
)

/**
 * Uploads [pending] concurrently and returns the resulting file-upload ids, in the same order.
 *
 * Concurrency is bounded by [MAX_CONCURRENT_UPLOADS]. Structured concurrency makes failure
 * atomic from the caller's perspective: the first failed upload cancels its siblings — across
 * every surface in the pool, so a failed cover aborts the files-property uploads too — and this
 * function throws [FileUploadError][it.saabel.kotlinnotionclient.models.files.FileUploadError]
 * before the request that would have carried them is ever sent. Uploads that had already
 * completed are orphaned deliberately: Notion exposes no delete-upload endpoint and unattached
 * uploads expire an hour after creation.
 */
private suspend fun EnhancedFileUploadApi.uploadAll(pending: List<PendingFile>): List<String> {
    val semaphore = Semaphore(MAX_CONCURRENT_UPLOADS)

    return coroutineScope {
        pending
            .map { file ->
                async { semaphore.withPermit { uploadAndAwait(file.source, file.options).id } }
            }.awaitAll()
    }
}

/**
 * Rewrites [blocks], replacing each sentinel with its resolved block.
 *
 * Walks in exactly the order [collectPendingUploads] used, consuming [uploadIds] as it goes —
 * so sentinels are matched by position in the walk rather than by data-class equality, and two
 * sentinels carrying the same file still get their own upload each.
 */
private fun substitute(
    blocks: List<BlockRequest>,
    uploadIds: Iterator<String>,
): List<BlockRequest> =
    blocks.map { block ->
        if (block is BlockRequest.PendingUpload) {
            block.resolveTo(uploadIds.next())
        } else {
            childrenOf(block)?.let { withChildren(block, substitute(it, uploadIds)) } ?: block
        }
    }

/** Builds the block this sentinel stood in for, now that its file lives at [uploadId]. */
private fun BlockRequest.PendingUpload.resolveTo(uploadId: String): BlockRequest {
    val reference = FileUploadReference(id = uploadId)

    return when (kind) {
        PendingUploadKind.IMAGE -> {
            BlockRequest.Image(
                image = ImageRequestContent(caption = caption, type = "file_upload", fileUpload = reference),
            )
        }

        PendingUploadKind.VIDEO -> {
            BlockRequest.Video(
                video = VideoRequestContent(caption = caption, type = "file_upload", fileUpload = reference),
            )
        }

        PendingUploadKind.AUDIO -> {
            BlockRequest.Audio(
                audio = AudioRequestContent(caption = caption, type = "file_upload", fileUpload = reference),
            )
        }

        PendingUploadKind.FILE -> {
            BlockRequest.File(
                file = FileRequestContent(caption = caption, name = name, type = "file_upload", fileUpload = reference),
            )
        }

        PendingUploadKind.PDF -> {
            BlockRequest.PDF(
                pdf = PDFRequestContent(caption = caption, type = "file_upload", fileUpload = reference),
            )
        }

        PendingUploadKind.HTML -> {
            BlockRequest.Embed(
                embed = EmbedRequestContent(fileUpload = reference, caption = caption),
            )
        }
    }
}

// =============================================================================
// WHOLE-REQUEST RESOLUTION
//
// A page create is one request, so every local file it carries — icon, cover, files
// properties, content blocks — has to be uploaded before it can be serialized. These
// entry points pool the sentinels from all of a request's surfaces into a single
// concurrent pass, so one failed upload aborts the whole create rather than leaving a
// half-attached page behind.
//
// Collection and substitution are separate walks over the same surfaces in the same
// order, so a sentinel is matched to its upload by position rather than by data-class
// equality — the same file attached twice uploads twice, exactly as for blocks.
// =============================================================================

/**
 * Uploads every pending file in [request] and returns it carrying the resulting references.
 *
 * Covers all four surfaces a page create can attach from — files properties, icon, cover and
 * content blocks — in one pooled pass. Requests without local files are returned untouched
 * (identical instance), so this costs one walk on the overwhelmingly common path.
 */
internal suspend fun EnhancedFileUploadApi.resolvePendingUploads(request: CreatePageRequest): CreatePageRequest {
    // Surface order — properties, icon, cover, children — is the contract the substitution
    // below consumes the ids in. Keep the two lists in step.
    val pending =
        collectPendingFiles(request.properties) +
            collectPendingFiles(request.icon) +
            collectPendingFiles(request.cover) +
            collectPendingFiles(request.children)
    if (pending.isEmpty()) return request

    val ids = uploadAll(pending).iterator()

    return request.copy(
        properties = substitute(request.properties, ids),
        icon = substitute(request.icon, ids),
        cover = substitute(request.cover, ids),
        children = request.children?.let { substitute(it, ids) },
    )
}

/**
 * Uploads every pending file in [request] and returns it carrying the resulting references.
 *
 * The update endpoint takes no children, so the surfaces here are files properties, icon and
 * cover. See [resolvePendingUploads] for a page create.
 */
internal suspend fun EnhancedFileUploadApi.resolvePendingUploads(request: UpdatePageRequest): UpdatePageRequest {
    val pending =
        collectPendingFiles(request.properties) +
            collectPendingFiles(request.icon) +
            collectPendingFiles(request.cover)
    if (pending.isEmpty()) return request

    val ids = uploadAll(pending).iterator()

    return request.copy(
        properties = request.properties?.let { substitute(it, ids) },
        icon = substitute(request.icon, ids),
        cover = substitute(request.cover, ids),
    )
}

/** Uploads the pending icon and cover of a database create. See [resolvePendingUploads]. */
internal suspend fun EnhancedFileUploadApi.resolvePendingUploads(request: CreateDatabaseRequest): CreateDatabaseRequest {
    val pending = collectPendingFiles(request.icon) + collectPendingFiles(request.cover)
    if (pending.isEmpty()) return request

    val ids = uploadAll(pending).iterator()

    return request.copy(icon = substitute(request.icon, ids), cover = substitute(request.cover, ids))
}

/** Uploads the pending icon and cover of a database update. See [resolvePendingUploads]. */
internal suspend fun EnhancedFileUploadApi.resolvePendingUploads(request: UpdateDatabaseRequest): UpdateDatabaseRequest {
    val pending = collectPendingFiles(request.icon) + collectPendingFiles(request.cover)
    if (pending.isEmpty()) return request

    val ids = uploadAll(pending).iterator()

    return request.copy(icon = substitute(request.icon, ids), cover = substitute(request.cover, ids))
}

/** Uploads the pending icon of a data-source update. See [resolvePendingUploads]. */
internal suspend fun EnhancedFileUploadApi.resolvePendingUploads(request: UpdateDataSourceRequest): UpdateDataSourceRequest {
    val pending = collectPendingFiles(request.icon)
    if (pending.isEmpty()) return request

    return request.copy(icon = substitute(request.icon, uploadAll(pending).iterator()))
}

/**
 * Uploads every pending attachment in [request] and returns it carrying the resulting ids.
 *
 * Resolution runs before `CommentsApi.create` counts attachments, so Notion's cap of three is
 * checked against what actually goes on the wire.
 */
internal suspend fun EnhancedFileUploadApi.resolvePendingUploads(request: CreateCommentRequest): CreateCommentRequest {
    val attachments = request.attachments ?: return request
    val pending = attachments.filterIsInstance<CommentAttachmentRequest.PendingUpload>().map { PendingFile(it.source, it.options) }
    if (pending.isEmpty()) return request

    val ids = uploadAll(pending).iterator()

    return request.copy(
        attachments =
            attachments.map { attachment ->
                when (attachment) {
                    is CommentAttachmentRequest.FileUpload -> attachment
                    is CommentAttachmentRequest.PendingUpload -> CommentAttachmentRequest(ids.next())
                }
            },
    )
}

// ---------------------------------------------------------------------------
// Per-surface collection
// ---------------------------------------------------------------------------

private fun collectPendingFiles(properties: Map<String, PagePropertyValue>?): List<PendingFile> =
    properties.orEmpty().values.flatMap { value ->
        when (value) {
            is PagePropertyValue.FilesValue -> {
                value.files.filterIsInstance<FileObject.PendingUpload>().map { PendingFile(it.source, it.options) }
            }

            else -> {
                emptyList()
            }
        }
    }

private fun collectPendingFiles(icon: Icon?): List<PendingFile> =
    if (icon is Icon.PendingUpload) listOf(PendingFile(icon.source, icon.options)) else emptyList()

private fun collectPendingFiles(cover: PageCover?): List<PendingFile> =
    if (cover is PageCover.PendingUpload) listOf(PendingFile(cover.source, cover.options)) else emptyList()

private fun collectPendingFiles(blocks: List<BlockRequest>?): List<PendingFile> =
    collectPendingUploads(blocks.orEmpty()).map { PendingFile(it.source, it.options) }

// ---------------------------------------------------------------------------
// Per-surface substitution
//
// Each consumes exactly as many ids as its collect counterpart produced, in the same order.
// ---------------------------------------------------------------------------

private fun substitute(
    properties: Map<String, PagePropertyValue>,
    ids: Iterator<String>,
): Map<String, PagePropertyValue> =
    properties.mapValues { (_, value) ->
        when (value) {
            is PagePropertyValue.FilesValue -> {
                value.copy(
                    files =
                        value.files.map { file ->
                            if (file is FileObject.PendingUpload) {
                                FileObject.FileUpload(
                                    fileUpload = FileUploadReference(id = ids.next()),
                                    name = file.name ?: file.source.filename,
                                )
                            } else {
                                file
                            }
                        },
                )
            }

            else -> {
                value
            }
        }
    }

private fun substitute(
    icon: Icon?,
    ids: Iterator<String>,
): Icon? =
    if (icon is Icon.PendingUpload) {
        Icon.FileUpload(fileUpload = FileUploadReference(id = ids.next()))
    } else {
        icon
    }

private fun substitute(
    cover: PageCover?,
    ids: Iterator<String>,
): PageCover? =
    if (cover is PageCover.PendingUpload) {
        PageCover.FileUpload(fileUpload = FileUploadReference(id = ids.next()))
    } else {
        cover
    }
