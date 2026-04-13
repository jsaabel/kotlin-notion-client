@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.views

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Request body for POST /v1/views.
 *
 * Exactly one of [databaseId], [viewId], or [createDatabase] must be provided:
 * - [databaseId]: creates a new view tab on an existing database
 * - [viewId]: creates a widget inside a dashboard (requires [placement])
 * - [createDatabase]: creates a new linked database on a page and adds a view to it
 */
@Serializable
data class CreateViewRequest(
    @SerialName("data_source_id")
    val dataSourceId: String,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: ViewType,
    @SerialName("database_id")
    val databaseId: String? = null,
    @SerialName("view_id")
    val viewId: String? = null,
    @SerialName("create_database")
    val createDatabase: CreateDatabaseForView? = null,
    @SerialName("filter")
    val filter: JsonObject? = null,
    @SerialName("sorts")
    val sorts: JsonArray? = null,
    @SerialName("quick_filters")
    val quickFilters: Map<String, JsonObject>? = null,
    @SerialName("configuration")
    val configuration: JsonObject? = null,
    @SerialName("position")
    val position: ViewPosition? = null,
    @SerialName("placement")
    val placement: WidgetPlacement? = null,
)

/**
 * Request body for PATCH /v1/views/{view_id}.
 *
 * All fields are optional. Pass `null` to clear a field (except for name).
 * Unmentioned quick_filters entries are preserved.
 */
@Serializable
data class UpdateViewRequest(
    @SerialName("name")
    val name: String? = null,
    @SerialName("filter")
    val filter: JsonObject? = null,
    @SerialName("sorts")
    val sorts: JsonArray? = null,
    @SerialName("quick_filters")
    val quickFilters: Map<String, JsonObject?>? = null,
    @SerialName("configuration")
    val configuration: JsonObject? = null,
)

/**
 * Request body for POST /v1/views/{view_id}/queries.
 */
@Serializable
data class CreateViewQueryRequest(
    @SerialName("page_size")
    val pageSize: Int? = null,
)

/**
 * Position of a new view in the database's tab bar.
 * Only applicable when creating a view via [CreateViewRequest.databaseId].
 */
@Serializable
sealed class ViewPosition {
    @Serializable
    @SerialName("start")
    data object Start : ViewPosition()

    @Serializable
    @SerialName("end")
    data object End : ViewPosition()

    @Serializable
    @SerialName("after_view")
    data class AfterView(
        @SerialName("view_id")
        val viewId: String,
    ) : ViewPosition()
}

/**
 * Placement of a dashboard widget.
 * Only applicable when creating a view via [CreateViewRequest.viewId].
 */
@Serializable
sealed class WidgetPlacement {
    @Serializable
    @SerialName("new_row")
    data class NewRow(
        @SerialName("row_index")
        val rowIndex: Int? = null,
    ) : WidgetPlacement()

    @Serializable
    @SerialName("existing_row")
    data class ExistingRow(
        @SerialName("row_index")
        val rowIndex: Int,
    ) : WidgetPlacement()
}

/**
 * Creates a new linked database on a page and attaches it to the view being created.
 * Used in [CreateViewRequest.createDatabase].
 */
@Serializable
data class CreateDatabaseForView(
    @SerialName("parent")
    val parent: PageIdParent,
    @SerialName("position")
    val position: AfterBlockPosition? = null,
)

/**
 * A parent reference by page ID, used in [CreateDatabaseForView].
 */
@Serializable
data class PageIdParent(
    @SerialName("type")
    val type: String = "page_id",
    @SerialName("page_id")
    val pageId: String,
)

/**
 * A position reference for placing content after a specific block.
 */
@Serializable
data class AfterBlockPosition(
    @SerialName("type")
    val type: String = "after_block",
    @SerialName("block_id")
    val blockId: String,
)
