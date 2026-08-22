@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.datasources

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.FileUploadReference
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.NativeIconColor
import it.saabel.kotlinnotionclient.models.base.NativeIconObject
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.DatabasePropertiesBuilder
import it.saabel.kotlinnotionclient.models.databases.FormulaExpressions
import it.saabel.kotlinnotionclient.models.databases.RollupConfigurations
import it.saabel.kotlinnotionclient.models.files.FileUpload
import it.saabel.kotlinnotionclient.models.files.FileUploadOptions
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus
import it.saabel.kotlinnotionclient.utils.FileSource
import it.saabel.kotlinnotionclient.utils.asFileSource
import java.io.File
import java.nio.file.Path

/**
 * Builder for creating data source requests (API version 2025-09-03+).
 *
 * This builder creates requests to add new data sources to existing databases.
 *
 * Note: The description parameter is not supported when adding a data source to an existing database.
 *
 * Example:
 * ```kotlin
 * val request = createDataSourceRequest {
 *     parent.database("existing-database-id")
 *     title("Projects Data Source")
 *     properties {
 *         title("Project Name")
 *         select("Status", "Not Started", "In Progress", "Completed")
 *         date("Due Date")
 *     }
 * }
 * ```
 */
@DslMarker
annotation class DataSourceRequestDslMarker

@DataSourceRequestDslMarker
class CreateDataSourceRequestBuilder {
    private var databaseIdValue: String? = null
    private var titleValue: List<RichText>? = null
    private val properties = mutableMapOf<String, CreateDatabaseProperty>()

    /**
     * Builder for parent configuration.
     */
    val parent = ParentBuilder()

    /**
     * Configures the parent in a lambda, as an alternative to the `parent.xxx()` receiver form.
     *
     * Both forms drive the same builder and are last-call-wins; see
     * [docs/dsl-conventions.md](https://github.com/jsaabel/kotlin-notion-client/blob/main/docs/dsl-conventions.md).
     *
     * @param block Configuration block applied to the parent builder
     */
    fun parent(block: ParentBuilder.() -> Unit) {
        parent.block()
    }

    /**
     * Builder for parent configuration.
     *
     * A data source always hangs off a database container.
     */
    @DataSourceRequestDslMarker
    inner class ParentBuilder {
        /**
         * Sets the parent database this data source is added to.
         *
         * @param databaseId The database container ID
         */
        fun database(databaseId: String) {
            this@CreateDataSourceRequestBuilder.databaseIdValue = databaseId
        }
    }

    /**
     * Sets the database ID to add this data source to.
     *
     * @param id The database container ID
     */
    @Deprecated(
        message = "Parents are addressed through the parent builder in every DSL; use parent.database(id).",
        replaceWith = ReplaceWith("parent.database(id)"),
    )
    fun databaseId(id: String) {
        databaseIdValue = id
    }

    /**
     * Sets the title for the data source.
     *
     * @param text The title text
     */
    fun title(text: String) {
        titleValue = listOf(RichText.fromPlainText(text))
    }

    /**
     * Sets the title using rich text.
     *
     * @param richText List of RichText objects
     */
    fun title(richText: List<RichText>) {
        titleValue = richText
    }

    /**
     * Configures properties (schema) for the data source.
     *
     * @param block Configuration block for properties
     */
    fun properties(block: DatabasePropertiesBuilder.() -> Unit) {
        val builder = DatabasePropertiesBuilder()
        builder.block()
        properties.putAll(builder.build())
    }

    /**
     * Builds the CreateDataSourceRequest.
     *
     * @return The configured CreateDataSourceRequest
     * @throws IllegalStateException if database ID is not set or no properties defined
     */
    fun build(): CreateDataSourceRequest {
        require(databaseIdValue != null) { "Parent database must be specified - call parent.database(id)" }
        require(properties.isNotEmpty()) { "Data source must have at least one property" }
        // A create request carries the complete schema, so a prop() reference to a
        // property that is not defined here can never be valid. (Update requests may
        // reference properties that already exist on the data source, so they are not
        // checked this way.)
        FormulaExpressions.validateReferencesExist(properties)
        RollupConfigurations.validateReferencesExist(properties)

        return CreateDataSourceRequest(
            parent = Parent.DatabaseParent(databaseId = databaseIdValue!!),
            properties = properties,
            title = titleValue,
        )
    }
}

/**
 * Builder for updating data source requests (API version 2025-09-03+).
 *
 * This builder creates requests to update existing data sources.
 *
 * Example:
 * ```kotlin
 * val request = updateDataSourceRequest {
 *     title("Updated Project Tracker")
 *     properties {
 *         // Add new property
 *         number("Priority")
 *         // Modify existing (by recreating with same name)
 *         select("Status", "To Do", "Doing", "Done", "Blocked")
 *     }
 * }
 * ```
 */
@DataSourceRequestDslMarker
class UpdateDataSourceRequestBuilder {
    private var titleValue: List<RichText>? = null
    private var descriptionValue: List<RichText>? = null
    private var iconValue: Icon? = null
    private var inTrashValue: Boolean? = null
    private val properties = mutableMapOf<String, CreateDatabaseProperty>()

    // No cover builder here, deliberately. The 2025-09-03 migration guide lists `cover` as a
    // database-container attribute only (see UpdateDatabaseRequest) — but that same list omits
    // `icon`, which a data source demonstrably does carry, so the guide is not authority on this
    // point. The DataSource *response* model has a `cover` field, which says a data source can
    // hold one, not that PATCH /v1/data_sources accepts one. Settling it needs a live request,
    // and no credentials were available on this branch; adding an untested field would trade a
    // visible asymmetry for a silent 400. `DataSourceCoverProbeIntegrationTest` is that request,
    // and its KDoc says what each of the three outcomes means for this comment. Tracked as
    // IDEAS.md #11, raised by issue #82.
    val icon = IconBuilder()

    /**
     * Configures the icon in a lambda, as an alternative to the `icon.xxx()` receiver form.
     *
     * Both forms drive the same builder and are last-call-wins; see
     * [docs/dsl-conventions.md](https://github.com/jsaabel/kotlin-notion-client/blob/main/docs/dsl-conventions.md).
     *
     * @param block Configuration block applied to the icon builder
     */
    fun icon(block: IconBuilder.() -> Unit) {
        icon.block()
    }

    @DataSourceRequestDslMarker
    inner class IconBuilder {
        fun emoji(emoji: String) {
            this@UpdateDataSourceRequestBuilder.iconValue = Icon.Emoji(emoji = emoji)
        }

        fun external(url: String) {
            this@UpdateDataSourceRequestBuilder.iconValue = Icon.External(external = ExternalFile(url = url))
        }

        @Deprecated(
            message =
                "Verified live: a `type: \"file\"` icon is rejected with HTTP 400 validation_error — it is " +
                    "the read shape (a Notion-hosted expiring URL) and can never be written back. Notion " +
                    "accepts emoji, external, custom_emoji, file_upload and icon. Use external(url) for a " +
                    "publicly hosted file, or upload(id) for a file sent through the File Upload API.",
            replaceWith = ReplaceWith("external(url)"),
        )
        fun file(
            url: String,
            expiryTime: String? = null,
        ) {
            this@UpdateDataSourceRequestBuilder.iconValue =
                Icon.File(file = NotionFile(url = url, expiryTime = expiryTime))
        }

        /**
         * Sets an icon from a file uploaded via the File Upload API.
         *
         * The upload must already have reached [FileUploadStatus.UPLOADED]; attach it within its
         * one-hour expiry window or the upload is archived.
         *
         * Note the write/read asymmetry, verified live: the icon is *written* as `file_upload`
         * and *reads back* as `Icon.File` — a time-limited signed S3 URL. Round-tripping an icon
         * therefore means re-uploading or switching to `external`, not echoing back what was read.
         *
         * @param fileUploadId The ID of the uploaded file
         */
        fun upload(fileUploadId: String) {
            this@UpdateDataSourceRequestBuilder.iconValue =
                Icon.FileUpload(fileUpload = FileUploadReference(id = fileUploadId))
        }

        /**
         * Sets an icon from a file uploaded via the File Upload API.
         *
         * @param fileUpload The upload returned by the File Upload API
         */
        fun upload(fileUpload: FileUpload) {
            upload(fileUpload.id)
        }

        /**
         * Sets an icon from a local file, uploading it when the request is sent.
         *
         * Nothing is uploaded while this builder runs: the file is recorded as
         * [Icon.PendingUpload] and resolved by the client before the request goes out. See
         * `docs/adr/0001-deferred-file-upload-resolution.md`.
         *
         * ```kotlin
         * icon.upload(File("logo.png"))
         * ```
         *
         * @param source the file to upload
         * @param options upload options — content type override, progress callback, validation
         */
        fun upload(
            source: FileSource,
            options: FileUploadOptions = FileUploadOptions(),
        ) {
            this@UpdateDataSourceRequestBuilder.iconValue = Icon.PendingUpload(source = source, options = options)
        }

        /** Sets an icon from a local file, uploading it when the request is sent. See [upload]. */
        fun upload(
            file: File,
            options: FileUploadOptions = FileUploadOptions(),
        ) {
            upload(file.asFileSource(), options)
        }

        /** Sets an icon from a local file, uploading it when the request is sent. See [upload]. */
        fun upload(
            path: Path,
            options: FileUploadOptions = FileUploadOptions(),
        ) {
            upload(path.asFileSource(), options)
        }

        fun native(
            name: String,
            color: NativeIconColor? = null,
        ) {
            this@UpdateDataSourceRequestBuilder.iconValue =
                Icon.NativeIcon(NativeIconObject(name = name, color = color))
        }
    }

    /**
     * Sets the title for the data source.
     *
     * @param text The title text
     */
    fun title(text: String) {
        titleValue = listOf(RichText.fromPlainText(text))
    }

    /**
     * Sets the title using rich text.
     *
     * @param richText List of RichText objects
     */
    fun title(richText: List<RichText>) {
        titleValue = richText
    }

    /**
     * Sets the description for the data source.
     *
     * @param text The description text
     */
    fun description(text: String) {
        descriptionValue = listOf(RichText.fromPlainText(text))
    }

    /**
     * Sets the description using rich text.
     *
     * @param richText List of RichText objects
     */
    fun description(richText: List<RichText>) {
        descriptionValue = richText
    }

    /**
     * Configures properties (schema) updates for the data source.
     *
     * Note: This replaces/updates the entire property set. To remove a property,
     * simply don't include it in this configuration.
     *
     * @param block Configuration block for properties
     */
    fun properties(block: DatabasePropertiesBuilder.() -> Unit) {
        val builder = DatabasePropertiesBuilder()
        builder.block()
        properties.putAll(builder.build())
    }

    /**
     * Moves the data source to trash.
     */
    fun trash() {
        inTrashValue = true
    }

    /**
     * Restores the data source from trash.
     */
    fun restore() {
        inTrashValue = false
    }

    /**
     * Builds the UpdateDataSourceRequest.
     *
     * @return The configured UpdateDataSourceRequest
     */
    fun build(): UpdateDataSourceRequest =
        UpdateDataSourceRequest(
            properties = if (properties.isNotEmpty()) properties else null,
            title = titleValue,
            description = descriptionValue,
            icon = iconValue,
            inTrash = inTrashValue,
        )
}

/**
 * Entry point function for the create data source request DSL.
 *
 * @param block Configuration block for the data source request
 * @return The configured CreateDataSourceRequest
 */
fun createDataSourceRequest(block: CreateDataSourceRequestBuilder.() -> Unit): CreateDataSourceRequest {
    val builder = CreateDataSourceRequestBuilder()
    builder.block()
    return builder.build()
}

/**
 * Entry point function for the update data source request DSL.
 *
 * @param block Configuration block for the update request
 * @return The configured UpdateDataSourceRequest
 */
fun updateDataSourceRequest(block: UpdateDataSourceRequestBuilder.() -> Unit): UpdateDataSourceRequest {
    val builder = UpdateDataSourceRequestBuilder()
    builder.block()
    return builder.build()
}
