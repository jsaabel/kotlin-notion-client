@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.views

import it.saabel.kotlinnotionclient.models.datasources.DataSourceFilter
import it.saabel.kotlinnotionclient.models.datasources.DataSourceSort
import it.saabel.kotlinnotionclient.models.users.User
import it.saabel.kotlinnotionclient.utils.PaginatedResponse
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ---------------------------------------------------------------------------
// View type
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Enums used in view property config
// ---------------------------------------------------------------------------

/** How a status property is displayed in a view. */
@Serializable
enum class StatusShowAs {
    @SerialName("select")
    SELECT,

    @SerialName("checkbox")
    CHECKBOX,
}

/** Width mode for a property inside compact card layouts (board/gallery). */
@Serializable
enum class CardPropertyWidthMode {
    @SerialName("full_line")
    FULL_LINE,

    @SerialName("inline")
    INLINE,
}

/** Date display format for date properties in a view. */
@Serializable
enum class DateFormat {
    @SerialName("full")
    FULL,

    @SerialName("short")
    SHORT,

    @SerialName("month_day_year")
    MONTH_DAY_YEAR,

    @SerialName("day_month_year")
    DAY_MONTH_YEAR,

    @SerialName("year_month_day")
    YEAR_MONTH_DAY,

    @SerialName("relative")
    RELATIVE,
}

/** Time display format for date properties in a view. */
@Serializable
enum class TimeFormat {
    @SerialName("12_hour")
    HOUR_12,

    @SerialName("24_hour")
    HOUR_24,

    @SerialName("hidden")
    HIDDEN,
}

// ---------------------------------------------------------------------------
// Enums used in configuration
// ---------------------------------------------------------------------------

/** Source of the cover image in board/gallery views. */
@Serializable
enum class CoverType {
    @SerialName("page_cover")
    PAGE_COVER,

    @SerialName("page_content")
    PAGE_CONTENT,

    @SerialName("property")
    PROPERTY,
}

/** Size of the cover image on cards (board/gallery). */
@Serializable
enum class CoverSize {
    @SerialName("small")
    SMALL,

    @SerialName("medium")
    MEDIUM,

    @SerialName("large")
    LARGE,
}

/** Aspect ratio mode for card cover images. */
@Serializable
enum class CoverAspect {
    @SerialName("contain")
    CONTAIN,

    @SerialName("cover")
    COVER,
}

/** Card layout density in board/gallery views. */
@Serializable
enum class CardLayout {
    @SerialName("list")
    LIST,

    @SerialName("compact")
    COMPACT,
}

/** Calendar view range. */
@Serializable
enum class ViewRange {
    @SerialName("week")
    WEEK,

    @SerialName("month")
    MONTH,
}

/**
 * Display height for map and chart views.
 * Values: small, medium, large, extra_large.
 */
@Serializable
enum class ViewHeight {
    @SerialName("small")
    SMALL,

    @SerialName("medium")
    MEDIUM,

    @SerialName("large")
    LARGE,

    @SerialName("extra_large")
    EXTRA_LARGE,
}

/** Permission granted to the page created by a form submission. */
@Serializable
enum class SubmissionPermissions {
    @SerialName("none")
    NONE,

    @SerialName("comment_only")
    COMMENT_ONLY,

    @SerialName("reader")
    READER,

    @SerialName("read_and_write")
    READ_AND_WRITE,

    @SerialName("editor")
    EDITOR,
}

// ---------------------------------------------------------------------------
// Chart-specific enums
// ---------------------------------------------------------------------------

/** Chart layout type. */
@Serializable
enum class ChartType {
    @SerialName("column")
    COLUMN,

    @SerialName("bar")
    BAR,

    @SerialName("line")
    LINE,

    @SerialName("donut")
    DONUT,

    @SerialName("number")
    NUMBER,
}

/** Sort order applied to chart groups. */
@Serializable
enum class ChartSort {
    @SerialName("manual")
    MANUAL,

    @SerialName("x_ascending")
    X_ASCENDING,

    @SerialName("x_descending")
    X_DESCENDING,

    @SerialName("y_ascending")
    Y_ASCENDING,

    @SerialName("y_descending")
    Y_DESCENDING,
}

/** Color palette for chart series. */
@Serializable
enum class ChartColorTheme {
    @SerialName("gray")
    GRAY,

    @SerialName("blue")
    BLUE,

    @SerialName("yellow")
    YELLOW,

    @SerialName("green")
    GREEN,

    @SerialName("purple")
    PURPLE,

    @SerialName("teal")
    TEAL,

    @SerialName("orange")
    ORANGE,

    @SerialName("pink")
    PINK,

    @SerialName("red")
    RED,

    @SerialName("auto")
    AUTO,

    @SerialName("colorful")
    COLORFUL,
}

/** Position of the legend in a chart. */
@Serializable
enum class LegendPosition {
    @SerialName("off")
    OFF,

    @SerialName("bottom")
    BOTTOM,

    @SerialName("side")
    SIDE,
}

/** Which axes show labels in a chart. */
@Serializable
enum class AxisLabels {
    @SerialName("none")
    NONE,

    @SerialName("x_axis")
    X_AXIS,

    @SerialName("y_axis")
    Y_AXIS,

    @SerialName("both")
    BOTH,
}

/** Which grid lines are shown in a chart. */
@Serializable
enum class GridLines {
    @SerialName("none")
    NONE,

    @SerialName("horizontal")
    HORIZONTAL,

    @SerialName("vertical")
    VERTICAL,

    @SerialName("both")
    BOTH,
}

/** How multiple series are grouped visually in a bar/column chart. */
@Serializable
enum class GroupStyle {
    @SerialName("normal")
    NORMAL,

    @SerialName("percent")
    PERCENT,

    @SerialName("side_by_side")
    SIDE_BY_SIDE,
}

/** Labels shown on donut chart segments. */
@Serializable
enum class DonutLabels {
    @SerialName("none")
    NONE,

    @SerialName("value")
    VALUE,

    @SerialName("name")
    NAME,

    @SerialName("name_and_value")
    NAME_AND_VALUE,
}

// ---------------------------------------------------------------------------
// Shared configuration sub-types
// ---------------------------------------------------------------------------

/**
 * Cover image configuration for board and gallery card views.
 *
 * When [type] is [CoverType.PROPERTY], [propertyId] must also be set.
 */
@Serializable
data class CoverConfig(
    @SerialName("type")
    val type: CoverType,
    @SerialName("property_id")
    val propertyId: String? = null,
)

// ---------------------------------------------------------------------------
// ViewPropertyConfig
// ---------------------------------------------------------------------------

/**
 * Controls the visibility and display of a single property within a view.
 *
 * Used in [CreateViewRequest] and [UpdateViewRequest] via the `configuration.properties` array,
 * and returned as part of the view configuration in GET responses.
 *
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
    /** How to display status properties: as a select chip or a checkbox. */
    @SerialName("status_show_as")
    val statusShowAs: StatusShowAs? = null,
    /** Property width mode in compact card layouts (board/gallery). */
    @SerialName("card_property_width_mode")
    val cardPropertyWidthMode: CardPropertyWidthMode? = null,
    /** Date display format (date properties only). */
    @SerialName("date_format")
    val dateFormat: DateFormat? = null,
    /** Time display format (date properties only). */
    @SerialName("time_format")
    val timeFormat: TimeFormat? = null,
)

// ---------------------------------------------------------------------------
// ViewConfiguration sealed class
// ---------------------------------------------------------------------------

/**
 * Typed configuration for a Notion view.
 *
 * Each view type carries its own set of configuration fields. All subtypes include
 * a `type` field that matches the [ViewType] serialisation name.
 *
 * ## Partially-typed subtypes
 * [Table], [Board], [Timeline], and [Chart] include complex nested sub-objects
 * (`group_by`, `preference`, chart aggregations, etc.) as raw [JsonObject] fields.
 * These will be upgraded to fully-typed models in a future release.
 *
 * ## Unknown types
 * Any configuration type not yet modelled by this library is deserialized into
 * [Unknown], preserving the raw JSON for inspection. This ensures forward compatibility
 * as Notion adds new view types.
 */
@Serializable(with = ViewConfigurationSerializer::class)
sealed class ViewConfiguration {
    /**
     * List view configuration.
     *
     * @param properties Property visibility and display settings (up to 100 items).
     */
    @Serializable
    data class List(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "list",
        @SerialName("properties") val properties: kotlin.collections.List<ViewPropertyConfig>? = null,
    ) : ViewConfiguration()

    /**
     * Form view configuration.
     *
     * @param isFormClosed Whether the form is closed for new submissions.
     * @param anonymousSubmissions Whether non-logged-in users can submit.
     * @param submissionPermissions Permission granted to the submitter on the created page.
     */
    @Serializable
    data class Form(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "form",
        @SerialName("is_form_closed") val isFormClosed: Boolean? = null,
        @SerialName("anonymous_submissions") val anonymousSubmissions: Boolean? = null,
        @SerialName("submission_permissions") val submissionPermissions: SubmissionPermissions? = null,
    ) : ViewConfiguration()

    /**
     * Map view configuration.
     *
     * @param height Display height of the map area.
     * @param mapBy Property ID of the location property used to position items.
     * @param properties Property visibility settings for map pin cards.
     */
    @Serializable
    data class Map(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "map",
        @SerialName("height") val height: ViewHeight? = null,
        @SerialName("map_by") val mapBy: String? = null,
        @SerialName("properties") val properties: kotlin.collections.List<ViewPropertyConfig>? = null,
    ) : ViewConfiguration()

    /**
     * Gallery view configuration.
     *
     * @param properties Property visibility settings for gallery cards.
     * @param cover Cover image configuration.
     * @param coverSize Size of the cover image.
     * @param coverAspect Aspect ratio mode for the cover.
     * @param cardLayout Card density (full cards vs compact).
     */
    @Serializable
    data class Gallery(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "gallery",
        @SerialName("properties") val properties: kotlin.collections.List<ViewPropertyConfig>? = null,
        @SerialName("cover") val cover: CoverConfig? = null,
        @SerialName("cover_size") val coverSize: CoverSize? = null,
        @SerialName("cover_aspect") val coverAspect: CoverAspect? = null,
        @SerialName("card_layout") val cardLayout: CardLayout? = null,
    ) : ViewConfiguration()

    /**
     * Table view configuration.
     *
     * @param properties Property visibility and column-width settings.
     * @param groupBy Row grouping configuration (raw JSON — typed model planned for Phase 3).
     * @param subtasks Sub-item display configuration (raw JSON — typed model planned for Phase 3).
     * @param wrapCells Whether to wrap cell content onto multiple lines.
     * @param frozenColumnIndex Number of columns frozen from the left edge.
     * @param showVerticalLines Whether to render vertical grid lines.
     */
    @Serializable
    data class Table(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "table",
        @SerialName("properties") val properties: kotlin.collections.List<ViewPropertyConfig>? = null,
        @SerialName("group_by") val groupBy: JsonObject? = null,
        @SerialName("subtasks") val subtasks: JsonObject? = null,
        @SerialName("wrap_cells") val wrapCells: Boolean? = null,
        @SerialName("frozen_column_index") val frozenColumnIndex: Int? = null,
        @SerialName("show_vertical_lines") val showVerticalLines: Boolean? = null,
    ) : ViewConfiguration()

    /**
     * Board view configuration.
     *
     * @param groupBy Column grouping configuration (raw JSON — typed model planned for Phase 3).
     * @param subGroupBy Secondary grouping within columns (raw JSON — typed model planned for Phase 3).
     * @param properties Property visibility settings for board cards.
     * @param cover Cover image configuration.
     * @param coverSize Size of the cover image.
     * @param coverAspect Aspect ratio mode for the cover.
     * @param cardLayout Card density.
     */
    @Serializable
    data class Board(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "board",
        @SerialName("group_by") val groupBy: JsonObject? = null,
        @SerialName("sub_group_by") val subGroupBy: JsonObject? = null,
        @SerialName("properties") val properties: kotlin.collections.List<ViewPropertyConfig>? = null,
        @SerialName("cover") val cover: CoverConfig? = null,
        @SerialName("cover_size") val coverSize: CoverSize? = null,
        @SerialName("cover_aspect") val coverAspect: CoverAspect? = null,
        @SerialName("card_layout") val cardLayout: CardLayout? = null,
    ) : ViewConfiguration()

    /**
     * Calendar view configuration.
     *
     * @param datePropertyId Property ID used to position items on the calendar.
     * @param datePropertyName Convenience name of the date property (response only).
     * @param properties Property visibility settings shown on calendar event cards.
     * @param viewRange Whether to show a week or month view.
     * @param showWeekends Whether weekend columns are visible.
     */
    @Serializable
    data class Calendar(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "calendar",
        @SerialName("date_property_id") val datePropertyId: String? = null,
        @SerialName("date_property_name") val datePropertyName: String? = null,
        @SerialName("properties") val properties: kotlin.collections.List<ViewPropertyConfig>? = null,
        @SerialName("view_range") val viewRange: ViewRange? = null,
        @SerialName("show_weekends") val showWeekends: Boolean? = null,
    ) : ViewConfiguration()

    /**
     * Timeline view configuration.
     *
     * @param datePropertyId Start date property ID for positioning items.
     * @param datePropertyName Convenience name (response only).
     * @param endDatePropertyId Optional end date property ID.
     * @param endDatePropertyName Convenience name (response only).
     * @param properties Property visibility settings on timeline item cards.
     * @param showTable Whether to show the table panel alongside the timeline.
     * @param tableProperties Properties shown in the table panel.
     * @param preference Zoom level and scroll position (raw JSON — typed model planned for Phase 4).
     * @param arrowsBy Dependency-arrow configuration (raw JSON — typed model planned for Phase 4).
     * @param colorBy Whether to color items by a property value.
     */
    @Serializable
    data class Timeline(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "timeline",
        @SerialName("date_property_id") val datePropertyId: String? = null,
        @SerialName("date_property_name") val datePropertyName: String? = null,
        @SerialName("end_date_property_id") val endDatePropertyId: String? = null,
        @SerialName("end_date_property_name") val endDatePropertyName: String? = null,
        @SerialName("properties") val properties: kotlin.collections.List<ViewPropertyConfig>? = null,
        @SerialName("show_table") val showTable: Boolean? = null,
        @SerialName("table_properties") val tableProperties: kotlin.collections.List<ViewPropertyConfig>? = null,
        @SerialName("preference") val preference: JsonObject? = null,
        @SerialName("arrows_by") val arrowsBy: JsonObject? = null,
        @SerialName("color_by") val colorBy: Boolean? = null,
    ) : ViewConfiguration()

    /**
     * Chart view configuration.
     *
     * ## Partially-typed fields
     * `xAxis`, `yAxis`, `value`, `stackBy`, and `referenceLines` are raw [JsonObject] /
     * [List]`<JsonObject>` fields until `GroupByConfig` and `ChartAggregation` are
     * fully implemented in Phase 5.
     *
     * @param chartType The chart layout (column, bar, line, donut, number).
     * @param xAxisPropertyId Property ID for the x-axis in results mode.
     * @param yAxisPropertyId Property ID for the y-axis in results mode.
     * @param sort Sort order applied to chart groups.
     * @param colorTheme Color palette for chart series.
     * @param height Display height of the chart block.
     */
    @Serializable
    data class Chart(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "chart",
        @SerialName("chart_type") val chartType: ChartType? = null,
        @SerialName("x_axis") val xAxis: JsonObject? = null,
        @SerialName("y_axis") val yAxis: JsonObject? = null,
        @SerialName("x_axis_property_id") val xAxisPropertyId: String? = null,
        @SerialName("y_axis_property_id") val yAxisPropertyId: String? = null,
        @SerialName("value") val value: JsonObject? = null,
        @SerialName("sort") val sort: ChartSort? = null,
        @SerialName("color_theme") val colorTheme: ChartColorTheme? = null,
        @SerialName("height") val height: ViewHeight? = null,
        @SerialName("hide_empty_groups") val hideEmptyGroups: Boolean? = null,
        @SerialName("legend_position") val legendPosition: LegendPosition? = null,
        @SerialName("show_data_labels") val showDataLabels: Boolean? = null,
        @SerialName("axis_labels") val axisLabels: AxisLabels? = null,
        @SerialName("grid_lines") val gridLines: GridLines? = null,
        @SerialName("cumulative") val cumulative: Boolean? = null,
        @SerialName("smooth_line") val smoothLine: Boolean? = null,
        @SerialName("hide_line_fill_area") val hideLineFillArea: Boolean? = null,
        @SerialName("group_style") val groupStyle: GroupStyle? = null,
        @SerialName("y_axis_min") val yAxisMin: Double? = null,
        @SerialName("y_axis_max") val yAxisMax: Double? = null,
        @SerialName("donut_labels") val donutLabels: DonutLabels? = null,
        @SerialName("hide_title") val hideTitle: Boolean? = null,
        @SerialName("stack_by") val stackBy: JsonObject? = null,
        @SerialName("reference_lines") val referenceLines: kotlin.collections.List<JsonObject>? = null,
        @SerialName("caption") val caption: String? = null,
        @SerialName("color_by_value") val colorByValue: Boolean? = null,
    ) : ViewConfiguration()

    /**
     * Dashboard view configuration (response only).
     *
     * Dashboard configurations cannot be set via the API — this object is returned
     * when retrieving a dashboard view. The `rows` field holds the widget layout
     * as raw JSON until the full widget model is implemented.
     */
    @Serializable
    data class Dashboard(
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("type") val type: String = "dashboard",
        @SerialName("rows") val rows: kotlin.collections.List<JsonObject>? = null,
    ) : ViewConfiguration()

    /**
     * Catch-all for view configuration types not yet modelled by this library.
     *
     * The entire configuration object is preserved in [rawContent] so callers can
     * inspect or manually parse it. This ensures the library remains forward-compatible
     * as Notion adds new view types.
     */
    data class Unknown(
        val rawContent: JsonElement,
    ) : ViewConfiguration()
}

// ---------------------------------------------------------------------------
// Parent / top-level view models
// ---------------------------------------------------------------------------

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
    val sorts: kotlin.collections.List<DataSourceSort>? = null,
    @SerialName("quick_filters")
    val quickFilters: kotlin.collections.Map<String, JsonObject>? = null,
    @SerialName("configuration")
    val configuration: ViewConfiguration? = null,
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
    override val results: kotlin.collections.List<ViewReference>,
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
    val results: kotlin.collections.List<ViewQueryPageReference>,
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
    val results: kotlin.collections.List<ViewQueryPageReference>,
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
 * A page reference inside a view query result.
 */
@Serializable
data class ViewQueryPageReference(
    @SerialName("object")
    val objectType: String,
    @SerialName("id")
    val id: String,
)
