@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

/**
 * DSL marker for the files builder to prevent accidental nesting of outer-scope
 * builders (e.g. [PagePropertiesBuilder]) inside a `files { ... }` block.
 */
@DslMarker
annotation class FilesDslMarker

/**
 * Builder for constructing the list of [FileObject]s backing a "Files & media"
 * page-property value.
 *
 * Mirrors the conventions of [RichTextBuilder][it.saabel.kotlinnotionclient.models.richtext.RichTextBuilder]
 * and [DateRangeBuilder]: collect entries via fluent calls, then materialise with [build].
 *
 * Example:
 * ```kotlin
 * files("Attachments") {
 *     upload("upload-abc-123", name = "report.pdf")
 *     upload("upload-def-456")
 *     external("Spec doc", "https://example.com/spec.pdf")
 * }
 *
 * // Partial update: keep one existing file, add a new upload
 * files("Attachments") {
 *     existing(previousPage.attachments[0]) // routes FileData.External | FileData.Uploaded
 *     upload("new-upload-id")
 * }
 * ```
 */
@FilesDslMarker
class FilesBuilder {
    private val files = mutableListOf<FileObject>()

    /**
     * Attaches a freshly-uploaded file by its File Upload ID.
     *
     * @param id the ID of a file created via the File Upload API
     * @param name optional display name; defaults to the original upload filename when omitted
     */
    fun upload(
        id: String,
        name: String? = null,
    ) {
        files.add(FileObject.upload(id, name))
    }

    /**
     * Attaches a file hosted at a public URL.
     *
     * @param name display name (required for external files per the Notion docs)
     * @param url the public URL of the file
     */
    fun external(
        name: String,
        url: String,
    ) {
        files.add(FileObject.external(name, url))
    }

    /**
     * Re-attaches a file from a prior GET response unchanged.
     *
     * Routes [FileData.External] → [FileObject.External] and
     * [FileData.Uploaded] → [FileObject.Uploaded], preserving name and URL so an
     * existing file survives a partial-update round-trip.
     *
     * @param file a file read back from a [PageProperty.Files] response
     */
    fun existing(file: FileData) {
        files.add(
            when (file) {
                is FileData.External -> {
                    FileObject.External(name = file.name, external = file.external)
                }

                is FileData.Uploaded -> {
                    FileObject.Uploaded(name = file.name, file = file.file)
                }
            },
        )
    }

    /**
     * Escape hatch for advanced cases: adds a pre-built [FileObject] directly.
     *
     * @param file the file object to attach
     */
    fun add(file: FileObject) {
        files.add(file)
    }

    /**
     * Builds and returns the accumulated list of file objects.
     */
    fun build(): List<FileObject> = files.toList()
}
