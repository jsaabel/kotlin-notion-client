@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.views

import it.saabel.kotlinnotionclient.models.datasources.DataSourceFilter
import it.saabel.kotlinnotionclient.models.datasources.DataSourceSort
import it.saabel.kotlinnotionclient.models.users.User
import it.saabel.kotlinnotionclient.utils.PaginatedResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Enum representing all supported Notion view types.
 */
@Serializable
enum class ViewType {
    @SerialName("table")
    TABLE,

    @SerialName("board")
    BOARD,

    @SerialName("list")
    LIST,

    @SerialName("calendar")
    CALENDAR,

    @SerialName("timeline")
    TIMELINE,

    @SerialName("gallery")
    GALLERY,

    @SerialName("form")
    FORM,

    @SerialName("chart")
    CHART,

    @SerialName("map")
    MAP,

    @SerialName("dashboard")
    DASHBOARD,
}

/**
 * Parent reference for a View — always a database.
 */
@Serializable
data class ViewParent(
    @SerialName("type")
    val type: String = "database_id",
    @SerialName("database_id")
    val databaseId: String,
)

/**
 * Full view object returned by create, retrieve, and update endpoints.
 */
@Serializable
data class View(
    @SerialName("object")
    val objectType: String = "view",
    @SerialName("id")
    val id: String,
    @SerialName("parent")
    val parent: ViewParent,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: ViewType,
    @SerialName("created_time")
    val createdTime: String,
    @SerialName("last_edited_time")
    val lastEditedTime: String,
    @SerialName("url")
    val url: String,
    @SerialName("data_source_id")
    val dataSourceId: String? = null,
    @SerialName("created_by")
    val createdBy: User? = null,
    @SerialName("last_edited_by")
    val lastEditedBy: User? = null,
    @SerialName("filter")
    val filter: DataSourceFilter? = null,
    @SerialName("sorts")
    val sorts: List<DataSourceSort>? = null,
    @SerialName("quick_filters")
    val quickFilters: Map<String, JsonObject>? = null,
    @SerialName("configuration")
    val configuration: JsonObject? = null,
    @SerialName("dashboard_view_id")
    val dashboardViewId: String? = null,
)

/**
 * Minimal view object returned by the DELETE endpoint.
 */
@Serializable
data class PartialView(
    @SerialName("object")
    val objectType: String = "view",
    @SerialName("id")
    val id: String,
    @SerialName("parent")
    val parent: ViewParent? = null,
    @SerialName("type")
    val type: ViewType? = null,
)

/**
 * Minimal view reference returned by the LIST endpoint.
 * Only contains `object` and `id`.
 */
@Serializable
data class ViewReference(
    @SerialName("object")
    val objectType: String = "view",
    @SerialName("id")
    val id: String,
)

/**
 * Paginated list of view references returned by GET /v1/views.
 */
@Serializable
data class ViewList(
    @SerialName("object")
    val objectType: String = "list",
    @SerialName("results")
    override val results: List<ViewReference>,
    @SerialName("next_cursor")
    override val nextCursor: String? = null,
    @SerialName("has_more")
    override val hasMore: Boolean = false,
) : PaginatedResponse<ViewReference>

/**
 * Response from creating a view query (POST /v1/views/{view_id}/queries).
 * Contains the first page of results and a query ID for fetching subsequent pages.
 */
@Serializable
data class ViewQuery(
    @SerialName("object")
    val objectType: String = "view_query",
    @SerialName("id")
    val id: String,
    @SerialName("view_id")
    val viewId: String,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("results")
    val results: List<ViewQueryPageReference>,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    @SerialName("has_more")
    val hasMore: Boolean = false,
)

/**
 * Response from retrieving cached query results (GET /v1/views/{view_id}/queries/{query_id}).
 */
@Serializable
data class ViewQueryResults(
    @SerialName("object")
    val objectType: String = "list",
    @SerialName("results")
    val results: List<ViewQueryPageReference>,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    @SerialName("has_more")
    val hasMore: Boolean = false,
)

/**
 * Response from deleting a cached query (DELETE /v1/views/{view_id}/queries/{query_id}).
 */
@Serializable
data class DeletedViewQuery(
    @SerialName("object")
    val objectType: String = "view_query",
    @SerialName("id")
    val id: String,
    @SerialName("deleted")
    val deleted: Boolean,
)

/**
 * Controls the visibility and display of a single property within a view.
 *
 * Used in [CreateViewRequest] and [UpdateViewRequest] via the `configuration.properties` array.
 * Only [propertyId] is required; all other fields are optional display overrides.
 */
@Serializable
data class ViewPropertyConfig(
    @SerialName("property_id")
    val propertyId: String,
    @SerialName("visible")
    val visible: Boolean? = null,
    @SerialName("width")
    val width: Int? = null,
    @SerialName("wrap")
    val wrap: Boolean? = null,
)

/**
 * A page reference inside a view query result.
 */
@Serializable
data class ViewQueryPageReference(
    @SerialName("object")
    val objectType: String,
    @SerialName("id")
    val id: String,
)
