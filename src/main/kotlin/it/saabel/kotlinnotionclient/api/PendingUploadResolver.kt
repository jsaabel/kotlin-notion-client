package it.saabel.kotlinnotionclient.api

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
import it.saabel.kotlinnotionclient.models.files.FileUploadReference
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
    val pending = collectPendingUploads(blocks)
    if (pending.isEmpty()) return blocks

    val semaphore = Semaphore(MAX_CONCURRENT_UPLOADS)
    val uploadIds =
        coroutineScope {
            pending
                .map { sentinel ->
                    async { semaphore.withPermit { uploadAndAwait(sentinel.source, sentinel.options).id } }
                }.awaitAll()
        }

    return substitute(blocks, uploadIds.iterator())
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
