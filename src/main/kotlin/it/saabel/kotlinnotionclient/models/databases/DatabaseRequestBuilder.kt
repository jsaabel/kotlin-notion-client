@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.NativeIconColor
import it.saabel.kotlinnotionclient.models.base.NativeIconObject
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
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
         * Sets an uploaded file icon.
         *
         * @param url The uploaded file URL
         * @param expiryTime Optional expiry time
         */
        fun file(
            url: String,
            expiryTime: String? = null,
        ) {
            this@DatabaseRequestBuilder.iconValue =
                Icon.File(file = NotionFile(url = url, expiryTime = expiryTime))
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
         * Sets an uploaded file cover.
         *
         * @param url The uploaded file URL
         * @param expiryTime Optional expiry time
         */
        fun file(
            url: String,
            expiryTime: String? = null,
        ) {
            this@DatabaseRequestBuilder.coverValue =
                PageCover.File(file = NotionFile(url = url, expiryTime = expiryTime))
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
     * options can be provided via [StatusBuilder.option].
     *
     * Groups are auto-created by Notion and cannot be configured via the API. Only options
     * (name + color) can be specified at creation time.
     *
     * **Note**: Status properties cannot be updated via the API (unlike select/multi-select).
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
 * Only options can be specified. Groups are auto-created by Notion and cannot be configured
 * via the API.
 */
@DatabaseRequestDslMarker
class StatusBuilder {
    private val options = mutableListOf<CreateSelectOption>()

    /**
     * Adds an option to the status property.
     *
     * @param name The option name
     * @param color The option color
     * @param description Optional description for the option
     */
    fun option(
        name: String,
        color: SelectOptionColor = SelectOptionColor.DEFAULT,
        description: String? = null,
    ) {
        options.add(CreateSelectOption(name = name, color = color, description = description))
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
