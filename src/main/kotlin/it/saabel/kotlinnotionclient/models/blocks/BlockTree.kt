package it.saabel.kotlinnotionclient.models.blocks

/**
 * The nested children of [block], or `null` for the block types that cannot nest.
 *
 * Seventeen of the [BlockRequest] subtypes carry a `children` slot on their content object,
 * each under a different name. These two helpers are the single place that knows which, so
 * tree walks (pending-upload resolution, validation) do not each repeat the exhaustive `when`.
 */
internal fun childrenOf(block: BlockRequest): List<BlockRequest>? =
    when (block) {
        is BlockRequest.Paragraph -> block.paragraph.children

        is BlockRequest.Heading1 -> block.heading1.children

        is BlockRequest.Heading2 -> block.heading2.children

        is BlockRequest.Heading3 -> block.heading3.children

        is BlockRequest.Heading4 -> block.heading4.children

        is BlockRequest.BulletedListItem -> block.bulletedListItem.children

        is BlockRequest.NumberedListItem -> block.numberedListItem.children

        is BlockRequest.ToDo -> block.toDo.children

        is BlockRequest.Toggle -> block.toggle.children

        is BlockRequest.Quote -> block.quote.children

        is BlockRequest.Callout -> block.callout.children

        is BlockRequest.Table -> block.table.children

        is BlockRequest.ColumnList -> block.columnList.children

        is BlockRequest.Column -> block.column.children

        is BlockRequest.SyncedBlock -> block.syncedBlock.children

        is BlockRequest.Template -> block.template.children

        is BlockRequest.Tab -> block.tab.children

        is BlockRequest.Code,
        is BlockRequest.Image,
        is BlockRequest.Video,
        is BlockRequest.Audio,
        is BlockRequest.File,
        is BlockRequest.PDF,
        is BlockRequest.Divider,
        is BlockRequest.TableRow,
        is BlockRequest.Bookmark,
        is BlockRequest.Embed,
        is BlockRequest.ChildPage,
        is BlockRequest.ChildDatabase,
        is BlockRequest.Breadcrumb,
        is BlockRequest.TableOfContents,
        is BlockRequest.Equation,
        is BlockRequest.PendingUpload,
        -> null
    }

/**
 * Returns a copy of [block] with its nested children replaced by [children].
 *
 * Only meaningful for the blocks [childrenOf] returns a non-null list for; every other subtype
 * is returned unchanged. See [childrenOf].
 */
internal fun withChildren(
    block: BlockRequest,
    children: List<BlockRequest>?,
): BlockRequest =
    when (block) {
        is BlockRequest.Paragraph -> block.copy(paragraph = block.paragraph.copy(children = children))

        is BlockRequest.Heading1 -> block.copy(heading1 = block.heading1.copy(children = children))

        is BlockRequest.Heading2 -> block.copy(heading2 = block.heading2.copy(children = children))

        is BlockRequest.Heading3 -> block.copy(heading3 = block.heading3.copy(children = children))

        is BlockRequest.Heading4 -> block.copy(heading4 = block.heading4.copy(children = children))

        is BlockRequest.BulletedListItem -> block.copy(bulletedListItem = block.bulletedListItem.copy(children = children))

        is BlockRequest.NumberedListItem -> block.copy(numberedListItem = block.numberedListItem.copy(children = children))

        is BlockRequest.ToDo -> block.copy(toDo = block.toDo.copy(children = children))

        is BlockRequest.Toggle -> block.copy(toggle = block.toggle.copy(children = children))

        is BlockRequest.Quote -> block.copy(quote = block.quote.copy(children = children))

        is BlockRequest.Callout -> block.copy(callout = block.callout.copy(children = children))

        is BlockRequest.Table -> block.copy(table = block.table.copy(children = children))

        is BlockRequest.ColumnList -> block.copy(columnList = block.columnList.copy(children = children))

        is BlockRequest.Column -> block.copy(column = block.column.copy(children = children))

        is BlockRequest.SyncedBlock -> block.copy(syncedBlock = block.syncedBlock.copy(children = children))

        is BlockRequest.Template -> block.copy(template = block.template.copy(children = children))

        is BlockRequest.Tab -> block.copy(tab = block.tab.copy(children = children))

        is BlockRequest.Code,
        is BlockRequest.Image,
        is BlockRequest.Video,
        is BlockRequest.Audio,
        is BlockRequest.File,
        is BlockRequest.PDF,
        is BlockRequest.Divider,
        is BlockRequest.TableRow,
        is BlockRequest.Bookmark,
        is BlockRequest.Embed,
        is BlockRequest.ChildPage,
        is BlockRequest.ChildDatabase,
        is BlockRequest.Breadcrumb,
        is BlockRequest.TableOfContents,
        is BlockRequest.Equation,
        is BlockRequest.PendingUpload,
        -> block
    }

/**
 * Collects every [BlockRequest.PendingUpload] in [blocks] in document order, depth-first,
 * recursing into nested children.
 *
 * The same walk order is used to substitute resolved uploads back in, which is what makes
 * position — rather than value equality — the identity of a sentinel: the same file attached
 * twice yields two entries and two uploads.
 */
internal fun collectPendingUploads(blocks: List<BlockRequest>): List<BlockRequest.PendingUpload> {
    val collected = mutableListOf<BlockRequest.PendingUpload>()

    fun walk(current: List<BlockRequest>) {
        current.forEach { block ->
            if (block is BlockRequest.PendingUpload) {
                collected.add(block)
            } else {
                childrenOf(block)?.let { walk(it) }
            }
        }
    }

    walk(blocks)
    return collected
}
