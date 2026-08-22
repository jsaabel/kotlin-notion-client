package it.saabel.kotlinnotionclient.models.databases

import it.saabel.kotlinnotionclient.models.base.EmptyObject
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.pages.PageCover
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for creating a new database (API version 2025-09-03+).
 *
 * As of 2025-09-03, creating a database creates both the database container
 * and its initial data source. Properties are now nested under initial_data_source.
 *
 * This model represents the data structure required to create a database
 * in Notion. It contains only the fields that are sent in the request,
 * not the computed fields returned in responses.
 */
@Serializable
data class CreateDatabaseRequest(
    @SerialName("parent")
    val parent: Parent,
    @SerialName("title")
    val title: List<RichText>,
    @SerialName("initial_data_source")
    val initialDataSource: InitialDataSource,
    @SerialName("icon")
    val icon: Icon? = null,
    @SerialName("cover")
    val cover: PageCover? = null,
    @SerialName("description")
    val description: List<RichText>? = null,
)

/**
 * Configuration for the initial data source when creating a database.
 *
 * This contains the properties (schema) for the first data source in the database.
 */
@Serializable
data class InitialDataSource(
    @SerialName("properties")
    val properties: Map<String, CreateDatabaseProperty>,
)

/**
 * Request model for updating a database container (API version 2025-09-03+).
 *
 * Under 2025-09-03 the database container and its data sources own disjoint halves of what
 * used to be one object, and each half is reachable only through its own endpoint. This
 * request carries the container's half — see
 * `reference/notion-api/upgrading_to_2025_09_03/upgrade_guide.md`:
 *
 * > Continue to use the Update Database API for attributes that apply to the database:
 * > `parent`, `title`, `is_inline`, `icon`, `cover`, `in_trash`
 *
 * The other half — `properties` (the schema), `description`, and a data source's own `title`,
 * `icon` and `in_trash` — goes through [UpdateDataSourceRequest][it.saabel.kotlinnotionclient.models.datasources.UpdateDataSourceRequest].
 * `title` and `in_trash` appear on both because the container and each data source carry their
 * own; setting one does not set the other.
 *
 * Every field is optional and a `null` field is omitted from the payload, so a request touches
 * only what it names.
 *
 * Unlike a page update, an icon or cover here can be **replaced but not removed**. Verified live
 * on 2026-08-22: `PATCH /v1/databases` answers `"icon": null` with
 * `HTTP 400 validation_error — body.icon should be an object or `undefined`, instead was `null``.
 * The [Icon.Removed]/[PageCover.Removed] sentinels of
 * `docs/adr/0002-explicit-null-payloads.md` encode correctly but are not accepted here, so
 * [UpdateDatabaseRequestBuilder] does not offer a `remove()` on this surface. Setting one of them
 * on this model by hand produces that 400.
 *
 * @property parent Moves the database to a different parent. New in the 2025-09-03 API.
 * @property title The container title
 * @property icon The container icon. Cannot be cleared — see above.
 * @property cover The container cover. Cannot be cleared, and Notion does not support one on an
 *   inline database.
 * @property isInline Whether the database renders inline in its parent page
 * @property inTrash Whether the database is in the trash
 */
@Serializable
data class UpdateDatabaseRequest(
    @SerialName("parent")
    val parent: Parent? = null,
    @SerialName("title")
    val title: List<RichText>? = null,
    @SerialName("icon")
    val icon: Icon? = null,
    @SerialName("cover")
    val cover: PageCover? = null,
    @SerialName("is_inline")
    val isInline: Boolean? = null,
    @SerialName("in_trash")
    val inTrash: Boolean? = null,
)

/**
 * Request model for archiving a database.
 *
 * Notion doesn't support true deletion - objects are moved to trash instead.
 * In the 2025-09-03 API, databases use "in_trash" field (not "archived").
 *
 * `in_trash` is one of the container attributes [UpdateDatabaseRequest] carries, and
 * [it.saabel.kotlinnotionclient.api.DatabasesApi.trash] now goes through that instead. This
 * narrower shape is kept because it shipped as public API and encodes to the same payload.
 */
@Serializable
data class ArchiveDatabaseRequest(
    @SerialName("in_trash")
    val inTrash: Boolean = true,
)

private fun requirePropertyDescriptionLength(description: String?) {
    require(description == null || description.length <= 280) {
        "Property description must be 280 characters or fewer (was ${description!!.length})"
    }
}

/**
 * Property definitions for database creation requests.
 *
 * These are simpler than the response properties since they only contain
 * the configuration needed to create the property, not the metadata.
 */
@Serializable
sealed class CreateDatabaseProperty {
    /**
     * Title property - required for all databases.
     */
    @Serializable
    @SerialName("title")
    data class Title(
        @SerialName("title")
        val title: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Rich text property for formatted text content.
     */
    @Serializable
    @SerialName("rich_text")
    data class RichText(
        @SerialName("rich_text")
        val richText: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Number property with optional formatting.
     */
    @Serializable
    @SerialName("number")
    data class Number(
        @SerialName("number")
        val number: NumberConfiguration = NumberConfiguration(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Select property for single-choice dropdown.
     */
    @Serializable
    @SerialName("select")
    data class Select(
        @SerialName("select")
        val select: SelectConfiguration = SelectConfiguration(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Multi-select property for multiple-choice dropdown.
     */
    @Serializable
    @SerialName("multi_select")
    data class MultiSelect(
        @SerialName("multi_select")
        val multiSelect: SelectConfiguration = SelectConfiguration(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Date property for date/datetime values.
     */
    @Serializable
    @SerialName("date")
    data class Date(
        @SerialName("date")
        val date: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Checkbox property for boolean values.
     */
    @Serializable
    @SerialName("checkbox")
    data class Checkbox(
        @SerialName("checkbox")
        val checkbox: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * URL property for web links.
     */
    @Serializable
    @SerialName("url")
    data class Url(
        @SerialName("url")
        val url: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Email property for email addresses.
     */
    @Serializable
    @SerialName("email")
    data class Email(
        @SerialName("email")
        val email: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Phone number property.
     */
    @Serializable
    @SerialName("phone_number")
    data class PhoneNumber(
        @SerialName("phone_number")
        val phoneNumber: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * People property for user mentions.
     */
    @Serializable
    @SerialName("people")
    data class People(
        @SerialName("people")
        val people: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Status property for workflow-style statuses with options and groups.
     *
     * Pass an empty [StatusConfiguration] (the default) to let Notion create the standard options
     * ("Not started", "In progress", "Done") and groups ("To-do", "In progress", "Complete").
     * Custom initial options can be provided via [StatusConfiguration.options].
     *
     * Options can be assigned to one of the predefined groups ("To-do", "In progress",
     * "Complete") via [CreateStatusOption.group]. Custom groups cannot be created — the three
     * predefined groups are fixed.
     *
     * Status properties can also be updated via the API. On update, options with no
     * [CreateStatusOption.group] keep their current group, and new options default to the
     * "To-do" group.
     */
    @Serializable
    @SerialName("status")
    data class Status(
        @SerialName("status")
        val status: StatusConfiguration = StatusConfiguration(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Relation property for linking to pages in another database.
     */
    @Serializable
    @SerialName("relation")
    data class Relation(
        @SerialName("relation")
        val relation: RelationConfiguration,
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }

    /**
     * Formula property computing a value from an expression.
     *
     * Reference other properties with `prop("Property Name")` — since the Aug 2026 API
     * update those references are stored exactly as written, and expressions Notion
     * cannot store fail with a `validation_error` instead of being silently rewritten.
     *
     * Construction fails fast (with [IllegalArgumentException]) on expressions that are
     * structurally broken no matter what they mean: blank expressions, unterminated
     * string literals, unbalanced brackets, and malformed `prop()` calls. Semantic
     * validity (unknown functions, type errors, circular references, ...) is not locally
     * decidable and is left to the API.
     */
    @Serializable
    @SerialName("formula")
    data class Formula(
        @SerialName("formula")
        val formula: FormulaConfiguration,
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
            FormulaExpressions.validate(formula.expression)
        }
    }

    /**
     * Rollup property aggregating a property of the rows reached through a relation.
     *
     * Name the relation to walk and the property to read on the related rows — by name
     * (readable) or by id (rename-proof) — plus the [RollupFunction] to apply:
     * ```kotlin
     * rollup("Total hours", relationPropertyName = "Tasks", rollupPropertyName = "Hours", function = RollupFunction.SUM)
     * ```
     *
     * Construction fails fast (with [IllegalArgumentException]) when the configuration is
     * structurally incomplete: no relation reference, no rolled-up property reference, or
     * the read-only [RollupFunction.UNKNOWN] sentinel. Whether Notion accepts the
     * combination (does the relation exist, is the function applicable to the target
     * property's type) is left to the API's `validation_error`.
     */
    @Serializable
    @SerialName("rollup")
    data class Rollup(
        @SerialName("rollup")
        val rollup: RollupConfiguration,
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
            RollupConfigurations.validate(rollup)
        }
    }

    /**
     * Files & media property for file attachments (uploaded or external).
     *
     * The schema config is an empty object; per-row file values are set via
     * `PagePropertiesBuilder.files(...)`.
     */
    @Serializable
    @SerialName("files")
    data class Files(
        @SerialName("files")
        val files: EmptyObject = EmptyObject(),
        @SerialName("description")
        val description: String? = null,
    ) : CreateDatabaseProperty() {
        init {
            requirePropertyDescriptionLength(description)
        }
    }
}

/**
 * Configuration for number properties.
 */
@Serializable
data class NumberConfiguration(
    @SerialName("format")
    val format: String = "number",
)

/**
 * Configuration for select/multi-select properties.
 */
@Serializable
data class SelectConfiguration(
    @SerialName("options")
    val options: List<CreateSelectOption> = emptyList(),
)

/**
 * Option for select/multi-select/status properties in creation requests.
 */
@Serializable
data class CreateSelectOption(
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: SelectOptionColor = SelectOptionColor.DEFAULT,
    @SerialName("description")
    val description: String? = null,
)

/**
 * Option for status properties in creation and update requests.
 *
 * Unlike select/multi-select options, a status option can be assigned to one of the
 * predefined groups ("To-do", "In progress", "Complete") via [group]. When [group] is
 * omitted, existing options keep their current group on update, and new options default
 * to the "To-do" group.
 */
@Serializable
data class CreateStatusOption(
    @SerialName("name")
    val name: String,
    @SerialName("color")
    val color: SelectOptionColor = SelectOptionColor.DEFAULT,
    @SerialName("description")
    val description: String? = null,
    @SerialName("group")
    val group: StatusOptionGroup? = null,
)

/**
 * The predefined groups a status option can be assigned to.
 *
 * Notion only supports these three groups — custom groups cannot be created via the API.
 */
@Serializable
enum class StatusOptionGroup {
    @SerialName("To-do")
    TO_DO,

    @SerialName("In progress")
    IN_PROGRESS,

    @SerialName("Complete")
    COMPLETE,
}

/**
 * Configuration for status properties in creation and update requests.
 *
 * Options (name + color) can be specified, and each option may be assigned to one of the
 * predefined groups via [CreateStatusOption.group].
 */
@Serializable
data class StatusConfiguration(
    @SerialName("options")
    val options: List<CreateStatusOption> = emptyList(),
)

/**
 * Configuration for relation properties (API version 2025-09-03+).
 *
 * Relation properties connect pages to other databases/data sources.
 * The target must be shared with your integration for the relation to work.
 *
 * As of 2025-09-03:
 * - Both database_id and data_source_id should be provided when possible
 * - At minimum, provide data_source_id for proper targeting
 */
@Serializable
data class RelationConfiguration(
    @SerialName("database_id")
    val databaseId: String,
    @SerialName("data_source_id")
    val dataSourceId: String? = null,
    @SerialName("single_property")
    val singleProperty: EmptyObject? = null,
    @SerialName("dual_property")
    val dualProperty: DualPropertyConfiguration? = null,
    @SerialName("synced_property_name")
    val syncedPropertyName: String? = null,
    @SerialName("synced_property_id")
    val syncedPropertyId: String? = null,
) {
    companion object {
        /**
         * Creates a simple unidirectional relation to another database.
         *
         * @param databaseId The ID of the target database
         * @param dataSourceId The ID of the target data source
         * @return RelationConfiguration for a single property relation
         */
        fun singleProperty(
            databaseId: String,
            dataSourceId: String,
        ): RelationConfiguration =
            RelationConfiguration(
                databaseId = databaseId,
                dataSourceId = dataSourceId,
                singleProperty = EmptyObject(),
            )

        /**
         * Creates a bidirectional relation with a specific synced property.
         *
         * @param databaseId The ID of the target database
         * @param dataSourceId The ID of the target data source
         * @param syncedPropertyName The name of the property in the target database
         * @param syncedPropertyId The ID of the property in the target database (optional)
         * @return RelationConfiguration for a dual property relation
         */
        fun dualProperty(
            databaseId: String,
            dataSourceId: String,
            syncedPropertyName: String,
            syncedPropertyId: String? = null,
        ): RelationConfiguration =
            RelationConfiguration(
                databaseId = databaseId,
                dataSourceId = dataSourceId,
                dualProperty =
                    DualPropertyConfiguration(
                        syncedPropertyName = syncedPropertyName,
                        syncedPropertyId = syncedPropertyId,
                    ),
            )

        /**
         * Creates a simple synced relation (legacy format).
         *
         * @param databaseId The ID of the target database
         * @param dataSourceId The ID of the target data source
         * @param syncedPropertyName The name of the synced property
         * @return RelationConfiguration for a synced relation
         */
        fun synced(
            databaseId: String,
            dataSourceId: String,
            syncedPropertyName: String,
        ): RelationConfiguration =
            RelationConfiguration(
                databaseId = databaseId,
                dataSourceId = dataSourceId,
                syncedPropertyName = syncedPropertyName,
            )
    }
}

/**
 * Configuration for dual/bidirectional relation properties.
 */
@Serializable
data class DualPropertyConfiguration(
    @SerialName("synced_property_name")
    val syncedPropertyName: String,
    @SerialName("synced_property_id")
    val syncedPropertyId: String? = null,
)
