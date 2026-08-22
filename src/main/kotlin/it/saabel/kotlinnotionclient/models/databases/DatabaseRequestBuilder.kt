@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.FileUploadReference
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.NativeIconColor
import it.saabel.kotlinnotionclient.models.base.NativeIconObject
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.files.FileUpload
import it.saabel.kotlinnotionclient.models.files.FileUploadStatus
import it.saabel.kotlinnotionclient.models.pages.PageCover

/**
 * Builder class for creating database requests with a fluent DSL.
 *
 * This builder provides a convenient way to construct CreateDatabaseRequest objects
 * with significantly less boilerplate than manual construction.
 *
 * ## Example Usage:
 * ```kotlin
 * val request = databaseRequest {
 *     parent.page(parentPageId)
 *     title("My Database")
 *     description("A comprehensive database for tracking tasks")
 *     properties {
 *         title("Name")
 *         richText("Description")
 *         number("Score", format = "number")
 *         checkbox("Completed")
 *         select("Status") {
 *             option("To Do", "red")
 *             option("In Progress", "yellow")
 *             option("Done", "green")
 *         }
 *         date("Due Date")
 *         people("Assignee")
 *         relation("Related Tasks", targetDatabaseId)
 *     }
 *     icon.emoji("📊")
 *     cover.external("https://example.com/cover.jpg")
 * }
 * ```
 *
 * **Important**: All databases must have at least one property, and typically
 * include a title property as the primary identifier.
 */
@DatabaseRequestDslMarker
class DatabaseRequestBuilder {
    private var parentValue: Parent? = null
    private var titleValue: List<RichText>? = null
    private var descriptionValue: List<RichText>? = null
    private var properties = mutableMapOf<String, CreateDatabaseProperty>()
    private var iconValue: Icon? = null
    private var coverValue: PageCover? = null

    /**
     * Builder for parent configuration.
     */
    val parent = ParentBuilder()

    /**
     * Builder for icon configuration.
     */
    val icon = IconBuilder()

    /**
     * Builder for cover configuration.
     */
    val cover = CoverBuilder()

    /**
     * Sets the database title.
     *
     * @param titleText The title text
     */
    fun title(titleText: String) {
        titleValue = listOf(RichText.fromPlainText(titleText))
    }

    /**
     * Sets the database description.
     *
     * @param descriptionText The description text
     */
    fun description(descriptionText: String) {
        descriptionValue = listOf(RichText.fromPlainText(descriptionText))
    }

    /**
     * Configures database properties using the DatabasePropertiesBuilder DSL.
     *
     * @param block Configuration block for properties
     */
    fun properties(block: DatabasePropertiesBuilder.() -> Unit) {
        val builder = DatabasePropertiesBuilder()
        builder.block()
        properties.putAll(builder.build())
    }

    /**
     * Builds the CreateDatabaseRequest (API version 2025-09-03+).
     *
     * Properties are automatically wrapped in an InitialDataSource object
     * to match the 2025-09-03 API structure.
     *
     * @return The configured CreateDatabaseRequest
     * @throws IllegalStateException if parent or title is not set, or if no properties are defined
     */
    fun build(): CreateDatabaseRequest {
        require(parentValue != null) { "Parent must be specified" }
        require(titleValue != null) { "Title must be specified" }
        require(properties.isNotEmpty()) { "Database must have at least one property" }
        // A create request carries the complete schema, so a prop() reference (or a rollup
        // relation reference) to a property that is not defined here can never be valid.
        FormulaExpressions.validateReferencesExist(properties)
        RollupConfigurations.validateReferencesExist(properties)

        return CreateDatabaseRequest(
            parent = parentValue!!,
            title = titleValue!!,
            initialDataSource = InitialDataSource(properties = properties),
            icon = iconValue,
            cover = coverValue,
            description = descriptionValue,
        )
    }

    /**
     * Builder for parent configuration.
     */
    @DatabaseRequestDslMarker
    inner class ParentBuilder {
        /**
         * Sets the parent to a page.
         *
         * @param pageId The parent page ID
         */
        fun page(pageId: String) {
            this@DatabaseRequestBuilder.parentValue = Parent.PageParent(pageId = pageId)
        }

        /**
         * Sets the parent to a block.
         *
         * @param blockId The parent block ID
         */
        fun block(blockId: String) {
            this@DatabaseRequestBuilder.parentValue = Parent.BlockParent(blockId = blockId)
        }

        /**
         * Sets the parent to workspace.
         */
        fun workspace() {
            this@DatabaseRequestBuilder.parentValue = Parent.WorkspaceParent
        }
    }

    /**
     * Builder for icon configuration.
     */
    @DatabaseRequestDslMarker
    inner class IconBuilder {
        /**
         * Sets an emoji icon.
         *
         * @param emoji The emoji character(s)
         */
        fun emoji(emoji: String) {
            this@DatabaseRequestBuilder.iconValue = Icon.Emoji(emoji = emoji)
        }

        /**
         * Sets an external image icon.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@DatabaseRequestBuilder.iconValue = Icon.External(external = ExternalFile(url = url))
        }

        /**
         * Sets a Notion-hosted file icon from its expiring URL.
         *
         * This emits the *read* shape (`type: "file"`), which Notion rejects on write. Verified
         * live: the request fails with HTTP 400 `validation_error`, naming `emoji`, `external`,
         * `custom_emoji`, `file_upload` and `icon` as the shapes it will accept. Kept for source
         * compatibility only — there is no input for which this call succeeds.
         *
         * @param url The uploaded file URL
         * @param expiryTime Optional expiry time
         */
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
            this@DatabaseRequestBuilder.iconValue =
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
            this@DatabaseRequestBuilder.iconValue =
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
         * Sets a native Notion icon.
         *
         * @param name The icon name (e.g. "pizza")
         * @param color Optional color. Defaults to [NativeIconColor.GRAY] when omitted.
         */
        fun native(
            name: String,
            color: NativeIconColor? = null,
        ) {
            this@DatabaseRequestBuilder.iconValue = Icon.NativeIcon(NativeIconObject(name = name, color = color))
        }
    }

    /**
     * Builder for cover configuration.
     */
    @DatabaseRequestDslMarker
    inner class CoverBuilder {
        /**
         * Sets an external image cover.
         *
         * @param url The external image URL
         */
        fun external(url: String) {
            this@DatabaseRequestBuilder.coverValue = PageCover.External(external = ExternalFile(url = url))
        }

        /**
         * Sets a Notion-hosted file cover from its expiring URL.
         *
         * This emits the *read* shape (`type: "file"`), which Notion rejects on write. Verified
         * live: the request fails with HTTP 400 `validation_error`, naming `emoji`, `external`,
         * `custom_emoji`, `file_upload` and `icon` as the shapes it will accept. Kept for source
         * compatibility only — there is no input for which this call succeeds.
         *
         * @param url The uploaded file URL
         * @param expiryTime Optional expiry time
         */
        @Deprecated(
            message =
                "Verified live: a `type: \"file\"` cover is rejected with HTTP 400 validation_error — it is " +
                    "the read shape (a Notion-hosted expiring URL) and can never be written back. Notion " +
                    "accepts emoji, external, custom_emoji, file_upload and icon. Use external(url) for a " +
                    "publicly hosted file, or upload(id) for a file sent through the File Upload API.",
            replaceWith = ReplaceWith("external(url)"),
        )
        fun file(
            url: String,
            expiryTime: String? = null,
        ) {
            this@DatabaseRequestBuilder.coverValue =
                PageCover.File(file = NotionFile(url = url, expiryTime = expiryTime))
        }

        /**
         * Sets a cover from a file uploaded via the File Upload API.
         *
         * The upload must already have reached [FileUploadStatus.UPLOADED]; attach it within its
         * one-hour expiry window or the upload is archived.
         *
         * Note the write/read asymmetry, verified live: the cover is *written* as `file_upload`
         * and *reads back* as `PageCover.File` — a time-limited signed S3 URL. Round-tripping a
         * cover therefore means re-uploading or switching to `external`, not echoing back what
         * was read.
         *
         * @param fileUploadId The ID of the uploaded file
         */
        fun upload(fileUploadId: String) {
            this@DatabaseRequestBuilder.coverValue =
                PageCover.FileUpload(fileUpload = FileUploadReference(id = fileUploadId))
        }

        /**
         * Sets a cover from a file uploaded via the File Upload API.
         *
         * @param fileUpload The upload returned by the File Upload API
         */
        fun upload(fileUpload: FileUpload) {
            upload(fileUpload.id)
        }
    }
}

/**
 * Builder class for database properties with a fluent DSL.
 *
 * This builder provides convenient methods to create database properties
 * with their specific configurations.
 */
@DatabaseRequestDslMarker
class DatabasePropertiesBuilder {
    private val properties = mutableMapOf<String, CreateDatabaseProperty>()

    /**
     * Adds a title property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     */
    fun title(
        name: String,
        description: String? = null,
    ) {
        properties[name] = CreateDatabaseProperty.Title(description = description)
    }

    /**
     * Adds a rich text property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     */
    fun richText(
        name: String,
        description: String? = null,
    ) {
        properties[name] = CreateDatabaseProperty.RichText(description = description)
    }

    /**
     * Adds a number property to the database.
     *
     * @param name The property name
     * @param format The number format ("number", "number_with_commas", "percent", "dollar", "canadian_dollar", "euro", "pound", "yen", "ruble", "rupee", "won", "yuan", "real", "lira", "rupiah", "franc", "hong_kong_dollar", "new_zealand_dollar", "krona", "norwegian_krone", "mexican_peso", "rand", "new_taiwan_dollar", "danish_krone", "zloty", "baht", "forint", "koruna", "shekel", "chilean_peso", "philippine_peso", "dirham", "colombian_peso", "riyal", "ringgit", "leu", "argentine_peso", "uruguayan_peso")
     * @param description Optional description (max 280 characters)
     */
    fun number(
        name: String,
        format: String = "number",
        description: String? = null,
    ) {
        properties[name] =
            CreateDatabaseProperty.Number(
                number = NumberConfiguration(format = format),
                description = description,
            )
    }

    /**
     * Adds a formula property to the database.
     *
     * Reference other properties with `prop("Property Name")`, e.g.
     * `formula("Updated price", """if(prop("In stock"), 0, prop("Price"))""")`.
     * Since the Aug 2026 API update, `prop()` references are stored exactly as written.
     *
     * Fails fast with [IllegalArgumentException] if the expression is structurally
     * broken (blank, unterminated string, unbalanced brackets, malformed `prop()` call).
     * On **create** requests, `prop()` references to properties that are not defined in
     * the same schema are also rejected at build time. Whether Notion accepts the
     * expression semantically is not locally decidable and surfaces as the API's own
     * `validation_error`.
     *
     * @param name The property name
     * @param expression The formula expression
     * @param description Optional description (max 280 characters)
     */
    fun formula(
        name: String,
        expression: String,
        description: String? = null,
    ) {
        FormulaExpressions.validate(expression, propertyName = name)
        properties[name] =
            CreateDatabaseProperty.Formula(
                formula = FormulaConfiguration(expression = expression),
                description = description,
            )
    }

    /**
     * Adds a select property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     * @param block Configuration block for select options
     */
    fun select(
        name: String,
        description: String? = null,
        block: SelectBuilder.() -> Unit = {},
    ) {
        val builder = SelectBuilder()
        builder.block()
        properties[name] =
            CreateDatabaseProperty.Select(
                select = SelectConfiguration(options = builder.build()),
                description = description,
            )
    }

    /**
     * Adds a multi-select property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     * @param block Configuration block for multi-select options
     */
    fun multiSelect(
        name: String,
        description: String? = null,
        block: SelectBuilder.() -> Unit = {},
    ) {
        val builder = SelectBuilder()
        builder.block()
        properties[name] =
            CreateDatabaseProperty.MultiSelect(
                multiSelect = SelectConfiguration(options = builder.build()),
                description = description,
            )
    }

    /**
     * Adds a status property to the database.
     *
     * When no options are specified, Notion creates the default options ("Not started",
     * "In progress", "Done") and groups ("To-do", "In progress", "Complete"). Custom initial
     * options can be provided via [StatusBuilder.option], each optionally assigned to one of
     * the predefined groups via its `group` parameter.
     *
     * Status properties can also be updated via the API. On update, options with no group
     * keep their current group, and new options default to the "To-do" group.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     * @param block Configuration block for status options (optional)
     */
    fun status(
        name: String,
        description: String? = null,
        block: StatusBuilder.() -> Unit = {},
    ) {
        val builder = StatusBuilder()
        builder.block()
        properties[name] = CreateDatabaseProperty.Status(status = builder.build(), description = description)
    }

    /**
     * Adds a date property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     */
    fun date(
        name: String,
        description: String? = null,
    ) {
        properties[name] = CreateDatabaseProperty.Date(description = description)
    }

    /**
     * Adds a checkbox property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     */
    fun checkbox(
        name: String,
        description: String? = null,
    ) {
        properties[name] = CreateDatabaseProperty.Checkbox(description = description)
    }

    /**
     * Adds a URL property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     */
    fun url(
        name: String,
        description: String? = null,
    ) {
        properties[name] = CreateDatabaseProperty.Url(description = description)
    }

    /**
     * Adds an email property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     */
    fun email(
        name: String,
        description: String? = null,
    ) {
        properties[name] = CreateDatabaseProperty.Email(description = description)
    }

    /**
     * Adds a phone number property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     */
    fun phoneNumber(
        name: String,
        description: String? = null,
    ) {
        properties[name] = CreateDatabaseProperty.PhoneNumber(description = description)
    }

    /**
     * Adds a people property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     */
    fun people(
        name: String,
        description: String? = null,
    ) {
        properties[name] = CreateDatabaseProperty.People(description = description)
    }

    /**
     * Adds a relation property to the database.
     *
     * @param name The property name
     * @param targetDatabaseId The ID of the target database
     * @param targetDataSourceId The ID of the target data source
     * @param description Optional description (max 280 characters)
     * @param block Configuration block for relation options
     */
    fun relation(
        name: String,
        targetDatabaseId: String,
        targetDataSourceId: String,
        description: String? = null,
        block: RelationBuilder.() -> Unit = {},
    ) {
        val builder = RelationBuilder(targetDatabaseId, targetDataSourceId)
        builder.block()
        properties[name] =
            CreateDatabaseProperty.Relation(
                relation = builder.build(),
                description = description,
            )
    }

    /**
     * Adds a rollup property to the database.
     *
     * A rollup walks a relation property and aggregates one property of the rows it
     * reaches. Reference the relation and the rolled-up property by name (readable) or by
     * id (rename-proof) — at least one of each pair is required:
     * ```kotlin
     * relation("Tasks", tasksDatabaseId, tasksDataSourceId)
     * rollup("Total hours", relationPropertyName = "Tasks", rollupPropertyName = "Hours", function = RollupFunction.SUM)
     * ```
     *
     * Fails fast with [IllegalArgumentException] if either reference is missing or blank,
     * or if [function] is the read-only [RollupFunction.UNKNOWN] fallback. On **create**
     * requests, a `relationPropertyName` that is not a relation property defined in the
     * same schema is also rejected at build time. Whether Notion accepts the combination
     * (is the function applicable to the target property's type?) surfaces as the API's
     * own `validation_error`.
     *
     * @param name The property name
     * @param function The aggregation applied to the collected values
     * @param relationPropertyName Name of the relation property to walk
     * @param rollupPropertyName Name of the property read on the related rows
     * @param relationPropertyId Id of the relation property, as an alternative to its name
     * @param rollupPropertyId Id of the rolled-up property, as an alternative to its name
     * @param description Optional description (max 280 characters)
     */
    @Suppress("LongParameterList")
    fun rollup(
        name: String,
        function: RollupFunction,
        relationPropertyName: String? = null,
        rollupPropertyName: String? = null,
        relationPropertyId: String? = null,
        rollupPropertyId: String? = null,
        description: String? = null,
    ) {
        val configuration =
            RollupConfiguration(
                function = function,
                relationPropertyName = relationPropertyName,
                relationPropertyId = relationPropertyId,
                rollupPropertyName = rollupPropertyName,
                rollupPropertyId = rollupPropertyId,
            )
        RollupConfigurations.validate(configuration, propertyName = name)
        properties[name] =
            CreateDatabaseProperty.Rollup(
                rollup = configuration,
                description = description,
            )
    }

    /**
     * Adds a rollup property to the database from a prepared [RollupConfiguration].
     *
     * Useful when copying a rollup read off an existing schema
     * (`DatabaseProperty.Rollup.rollup`) into a new one. See the other [rollup] overload
     * for the validation performed.
     *
     * @param name The property name
     * @param configuration The rollup configuration
     * @param description Optional description (max 280 characters)
     */
    fun rollup(
        name: String,
        configuration: RollupConfiguration,
        description: String? = null,
    ) {
        RollupConfigurations.validate(configuration, propertyName = name)
        properties[name] =
            CreateDatabaseProperty.Rollup(
                rollup = configuration,
                description = description,
            )
    }

    /**
     * Adds a "Files & media" property to the database.
     *
     * @param name The property name
     * @param description Optional description (max 280 characters)
     */
    fun files(
        name: String,
        description: String? = null,
    ) {
        properties[name] = CreateDatabaseProperty.Files(description = description)
    }

    /**
     * Builds the properties map.
     *
     * @return The configured properties map
     */
    fun build(): Map<String, CreateDatabaseProperty> = properties.toMap()
}

/**
 * Builder class for select/multi-select options.
 */
@DatabaseRequestDslMarker
class SelectBuilder {
    private val options = mutableListOf<CreateSelectOption>()

    /**
     * Adds an option to the select/multi-select property.
     *
     * @param name The option name
     * @param color The option color ("default", "gray", "brown", "red", "orange", "yellow", "green", "blue", "purple", "pink")
     * @param description Optional description for the option
     */
    fun option(
        name: String,
        color: SelectOptionColor = SelectOptionColor.DEFAULT,
        description: String? = null,
    ) {
        options.add(CreateSelectOption(name = name, color = color, description = description))
    }

    /**
     * Builds the options list.
     *
     * @return The configured options list
     */
    fun build(): List<CreateSelectOption> = options.toList()
}

/**
 * Builder class for status property configuration.
 *
 * Options can be specified and optionally assigned to one of the predefined groups
 * ("To-do", "In progress", "Complete") via [StatusOptionGroup]. Custom groups cannot be
 * created — the three predefined groups are fixed.
 */
@DatabaseRequestDslMarker
class StatusBuilder {
    private val options = mutableListOf<CreateStatusOption>()

    /**
     * Adds an option to the status property.
     *
     * @param name The option name
     * @param color The option color
     * @param description Optional description for the option
     * @param group Optional predefined group to assign the option to. When omitted, existing
     *   options keep their current group on update, and new options default to "To-do".
     */
    fun option(
        name: String,
        color: SelectOptionColor = SelectOptionColor.DEFAULT,
        description: String? = null,
        group: StatusOptionGroup? = null,
    ) {
        options.add(CreateStatusOption(name = name, color = color, description = description, group = group))
    }

    internal fun build(): StatusConfiguration = StatusConfiguration(options = options.toList())
}

/**
 * Builder class for relation configuration.
 */
@DatabaseRequestDslMarker
class RelationBuilder(
    private val targetDatabaseId: String,
    private val targetDataSourceId: String,
) {
    private var configurationType: RelationConfigurationType = RelationConfigurationType.Single
    private var syncedPropertyName: String? = null
    private var syncedPropertyId: String? = null

    /**
     * Configures the relation as a single (unidirectional) relation.
     */
    fun single() {
        configurationType = RelationConfigurationType.Single
    }

    /**
     * Configures the relation as a dual (bidirectional) relation.
     *
     * @param syncedPropertyName The name of the property in the target database
     * @param syncedPropertyId The ID of the property in the target database (optional)
     */
    fun dual(
        syncedPropertyName: String,
        syncedPropertyId: String? = null,
    ) {
        configurationType = RelationConfigurationType.Dual
        this.syncedPropertyName = syncedPropertyName
        this.syncedPropertyId = syncedPropertyId
    }

    /**
     * Configures the relation with a synced property (legacy format).
     *
     * @param syncedPropertyName The name of the synced property
     */
    fun synced(syncedPropertyName: String) {
        configurationType = RelationConfigurationType.Synced
        this.syncedPropertyName = syncedPropertyName
    }

    /**
     * Builds the relation configuration.
     *
     * @return The configured RelationConfiguration
     */
    fun build(): RelationConfiguration =
        when (configurationType) {
            RelationConfigurationType.Single -> {
                RelationConfiguration.singleProperty(
                    targetDatabaseId,
                    targetDataSourceId,
                )
            }

            RelationConfigurationType.Dual -> {
                RelationConfiguration.dualProperty(
                    targetDatabaseId,
                    targetDataSourceId,
                    syncedPropertyName!!,
                    syncedPropertyId,
                )
            }

            RelationConfigurationType.Synced -> {
                RelationConfiguration.synced(
                    targetDatabaseId,
                    targetDataSourceId,
                    syncedPropertyName!!,
                )
            }
        }

    private enum class RelationConfigurationType {
        Single,
        Dual,
        Synced,
    }
}

/**
 * DSL marker to prevent nested scopes.
 */
@DslMarker
annotation class DatabaseRequestDslMarker

/**
 * Entry point function for the database request DSL.
 *
 * @param block Configuration block for the database request
 * @return The configured CreateDatabaseRequest
 */
fun databaseRequest(block: DatabaseRequestBuilder.() -> Unit): CreateDatabaseRequest {
    val builder = DatabaseRequestBuilder()
    builder.block()
    return builder.build()
}
