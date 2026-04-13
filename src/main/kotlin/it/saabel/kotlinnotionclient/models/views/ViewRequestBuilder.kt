@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.views

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * DSL marker for View request builders. Prevents nested builder calls from leaking into
 * outer builder scopes.
 */
@DslMarker
annotation class ViewRequestDslMarker

/**
 * Builder for [CreateViewRequest].
 *
 * Exactly one of [database], [dashboard], or [createDatabase] must be called to specify
 * where the view lives.
 *
 * Example — new view tab on an existing database:
 * ```kotlin
 * client.views.create {
 *     dataSourceId("ds-id")
 *     name("My Board")
 *     type(ViewType.BOARD)
 *     database("db-id")
 * }
 * ```
 *
 * Example — widget inside a dashboard view:
 * ```kotlin
 * client.views.create {
 *     dataSourceId("ds-id")
 *     name("Revenue Chart")
 *     type(ViewType.CHART)
 *     dashboard("dashboard-view-id")
 * }
 * ```
 *
 * Example — create a new linked database on a page together with the view:
 * ```kotlin
 * client.views.create {
 *     dataSourceId("ds-id")
 *     name("Tasks")
 *     type(ViewType.TABLE)
 *     createDatabase(pageId = "parent-page-id")
 * }
 * ```
 */
@ViewRequestDslMarker
class CreateViewRequestBuilder {
    private var dataSourceIdValue: String? = null
    private var nameValue: String? = null
    private var typeValue: ViewType? = null

    // Exactly one of the following three may be set
    private var databaseIdValue: String? = null
    private var viewIdValue: String? = null
    private var createDatabaseValue: CreateDatabaseForView? = null

    private var positionValue: ViewPosition? = null
    private var placementValue: WidgetPlacement? = null
    private val propertyConfigs = mutableListOf<ViewPropertyConfig>()

    /**
     * The data source ID this view reads from.
     */
    fun dataSourceId(id: String) {
        dataSourceIdValue = id
    }

    /**
     * The display name of the view.
     */
    fun name(text: String) {
        nameValue = text
    }

    /**
     * The layout type of the view.
     */
    fun type(viewType: ViewType) {
        typeValue = viewType
    }

    /**
     * Creates a new view tab on an existing database.
     *
     * @param id The database UUID
     * @param position Optional tab-bar position for the new view
     */
    fun database(
        id: String,
        position: ViewPosition? = null,
    ) {
        databaseIdValue = id
        positionValue = position
    }

    /**
     * Creates this view as a widget inside a dashboard view.
     *
     * @param id The dashboard view UUID
     * @param placement Optional widget placement within the dashboard
     */
    fun dashboard(
        id: String,
        placement: WidgetPlacement? = null,
    ) {
        viewIdValue = id
        placementValue = placement
    }

    /**
     * Marks a set of properties as visible in this view.
     *
     * Calling this (or [hideProperties]) causes the builder to emit a `configuration`
     * object that includes a `properties` array. Properties listed here get `visible=true`;
     * any property not mentioned in either call is left at the API's default visibility.
     *
     * @param propertyIds The property IDs to show (use the ID from the data source schema)
     */
    fun showProperties(vararg propertyIds: String) {
        propertyIds.forEach { propertyConfigs.add(ViewPropertyConfig(propertyId = it, visible = true)) }
    }

    /**
     * Marks a set of properties as hidden in this view.
     *
     * @param propertyIds The property IDs to hide (use the ID from the data source schema)
     */
    fun hideProperties(vararg propertyIds: String) {
        propertyIds.forEach { propertyConfigs.add(ViewPropertyConfig(propertyId = it, visible = false)) }
    }

    /**
     * Creates a new linked database on a page and attaches this view to it.
     *
     * @param pageId The parent page UUID where the database will be created
     * @param afterBlockId Optional block UUID — places the database after this block
     */
    fun createDatabase(
        pageId: String,
        afterBlockId: String? = null,
    ) {
        createDatabaseValue =
            CreateDatabaseForView(
                parent = PageIdParent(pageId = pageId),
                position = afterBlockId?.let { AfterBlockPosition(blockId = it) },
            )
    }

    /**
     * Builds the [CreateViewRequest], validating that required fields are set.
     */
    fun build(): CreateViewRequest {
        requireNotNull(dataSourceIdValue) { "dataSourceId must be specified" }
        requireNotNull(nameValue) { "name must be specified" }
        requireNotNull(typeValue) { "type must be specified" }

        val parentCount = listOfNotNull(databaseIdValue, viewIdValue, createDatabaseValue).size
        require(parentCount == 1) {
            "Exactly one of database(), dashboard(), or createDatabase() must be called (got $parentCount)"
        }

        val configuration =
            if (propertyConfigs.isNotEmpty()) {
                buildJsonObject {
                    put("type", typeValue!!.name.lowercase())
                    put(
                        "properties",
                        buildJsonArray {
                            propertyConfigs.forEach { cfg ->
                                add(
                                    buildJsonObject {
                                        put("property_id", cfg.propertyId)
                                        cfg.visible?.let { put("visible", it) }
                                        cfg.width?.let { put("width", it) }
                                        cfg.wrap?.let { put("wrap", it) }
                                    },
                                )
                            }
                        },
                    )
                }
            } else {
                null
            }

        return CreateViewRequest(
            dataSourceId = dataSourceIdValue!!,
            name = nameValue!!,
            type = typeValue!!,
            databaseId = databaseIdValue,
            viewId = viewIdValue,
            createDatabase = createDatabaseValue,
            position = positionValue,
            placement = placementValue,
            configuration = configuration,
        )
    }
}

/**
 * Builder for [UpdateViewRequest].
 *
 * All fields are optional — only those explicitly set will be included in the request.
 *
 * Example:
 * ```kotlin
 * client.views.update("view-id") {
 *     name("Renamed View")
 * }
 * ```
 */
@ViewRequestDslMarker
class UpdateViewRequestBuilder {
    private var nameValue: String? = null
    private val propertyConfigs = mutableListOf<ViewPropertyConfig>()
    private var viewTypeHint: ViewType? = null

    /**
     * Sets a new display name for the view.
     */
    fun name(text: String) {
        nameValue = text
    }

    /**
     * Provides the view type so the builder can construct the correct configuration envelope.
     *
     * Required when calling [showProperties] or [hideProperties].
     */
    fun type(viewType: ViewType) {
        viewTypeHint = viewType
    }

    /**
     * Marks a set of properties as visible in the updated view configuration.
     *
     * [type] must also be called so the builder knows the configuration envelope to use.
     */
    fun showProperties(vararg propertyIds: String) {
        propertyIds.forEach { propertyConfigs.add(ViewPropertyConfig(propertyId = it, visible = true)) }
    }

    /**
     * Marks a set of properties as hidden in the updated view configuration.
     *
     * [type] must also be called so the builder knows the configuration envelope to use.
     */
    fun hideProperties(vararg propertyIds: String) {
        propertyIds.forEach { propertyConfigs.add(ViewPropertyConfig(propertyId = it, visible = false)) }
    }

    /**
     * Builds the [UpdateViewRequest].
     */
    fun build(): UpdateViewRequest {
        val configuration =
            if (propertyConfigs.isNotEmpty()) {
                requireNotNull(viewTypeHint) {
                    "Call type() when using showProperties()/hideProperties() so the configuration envelope is correct"
                }
                buildJsonObject {
                    put("type", viewTypeHint!!.name.lowercase())
                    put(
                        "properties",
                        buildJsonArray {
                            propertyConfigs.forEach { cfg ->
                                add(
                                    buildJsonObject {
                                        put("property_id", cfg.propertyId)
                                        cfg.visible?.let { put("visible", it) }
                                        cfg.width?.let { put("width", it) }
                                        cfg.wrap?.let { put("wrap", it) }
                                    },
                                )
                            }
                        },
                    )
                }
            } else {
                null
            }

        return UpdateViewRequest(
            name = nameValue,
            configuration = configuration,
        )
    }
}

/**
 * Entry-point function for the create view request DSL.
 */
fun createViewRequest(block: CreateViewRequestBuilder.() -> Unit): CreateViewRequest = CreateViewRequestBuilder().apply(block).build()

/**
 * Entry-point function for the update view request DSL.
 */
fun updateViewRequest(block: UpdateViewRequestBuilder.() -> Unit): UpdateViewRequest = UpdateViewRequestBuilder().apply(block).build()
