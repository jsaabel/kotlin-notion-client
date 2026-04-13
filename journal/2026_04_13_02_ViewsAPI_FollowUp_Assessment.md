# Development Journal - April 13, 2026 (Session 2)

## Views API Follow-Up: Filter/Sorts/Configuration Assessment

After completing the initial Views API implementation, we reviewed the three fields currently stored as raw
`JsonObject?`/`JsonArray?` in the `View` model: `filter`, `sorts`, and `configuration`.

---

## Findings

### Filter and Sorts — Reuse DataSource types directly

The Notion docs explicitly state that the view filter **uses the same format as the data source query filter**.
Confirmed via full schema comparison:

- `filter` → identical shape to `DataSourceFilter` (property filters, timestamp filters, compound `and`/`or`)
- `sorts` → identical shape to `DataSourceSort` (`{property, direction}` or `{timestamp, direction}`)

**Decision**: Change `View.filter: JsonObject?` → `DataSourceFilter?` and `View.sorts: JsonArray?` →
`List<DataSourceSort>?`. Same change in `ViewRequests.kt`. No new code needed — pure type upgrade.

Minor note: the update endpoint doc describes a `viewPropertySortsRequest` that may exclude timestamp sorts,
but the shape is otherwise the same. Worth verifying against live API but unlikely to be an issue.

### Configuration — Fully implementable, now well-documented

The docs in `docs/views-api/` contain complete OpenAPI schemas for all 10 view type configurations.
The sealed class hierarchy is well-defined:

#### View types and their required fields

| Type | Required (beyond `type`) | Notable optionals |
|---|---|---|
| `table` | — | `properties`, `group_by`, `wrap_cells`, `frozen_column_index`, `show_vertical_lines` |
| `board` | `group_by` | `sub_group_by`, `properties`, `cover`, `cover_size`, `cover_aspect`, `card_layout` |
| `calendar` | `date_property_id` | `date_property_name`, `view_range`, `show_weekends`, `properties` |
| `timeline` | `date_property_id` | `end_date_property_id`, `show_table`, `table_properties`, `preference`, `arrows_by`, `color_by` |
| `gallery` | — | `properties`, `cover`, `cover_size`, `cover_aspect`, `card_layout` |
| `list` | — | `properties` |
| `map` | — | `map_by`, `map_by_property_name`, `height`, `properties` |
| `form` | — | `is_form_closed`, `anonymous_submissions`, `submission_permissions` |
| `chart` | `chart_type` | `x_axis`, `y_axis`, many optional display fields, `reference_lines` |
| `dashboard` | `rows` | complex nested widget layout |

#### Shared sub-types needed

- **`GroupByConfig`** — discriminated union by property type:
  - `select`/`multi_select`, `status`, `person`/`created_by`/`last_edited_by`, `relation`,
    `date`/`created_time`/`last_edited_time`, `text`/`title`/`url`/`email`/`phone_number`,
    `number`, `checkbox`, `formula`
- **`CoverConfig`** — `type: enum [page_cover, page_content, property]` + `property_id?`
- **`SubtaskConfig`** — `property_id`, `display_mode` enum, `filter_scope` enum, `toggle_column_id?`
- **`TimelinePreference`** — `zoom_level` enum + `center_timestamp?` (Unix ms)
- **`TimelineArrowsBy`** — `property_id: String?`
- **`ChartAggregation`** — `aggregator` enum (count, sum, average, median, min, max, ...) + `property_id?`
- **`ChartReferenceLine`** — `value`, `label`, `color` enum, `dash_style` enum, `id?`
- **`DashboardRow`** / **`DashboardWidget`** — layout grid structure

`GroupByConfig` is the most complex sub-type (itself a discriminated union). It is shared by board, table,
chart, and timeline — so investing in it correctly pays off across view types.

`viewPropertyConfigRequest` also has more fields than our current `ViewPropertyConfig`:
- `property_name?` (convenience)
- `status_show_as?` enum (`select` | `checkbox`)
- `card_property_width_mode?` enum (`full_line` | `inline`)
- `date_format?` enum (6 values)
- `time_format?` enum (`12_hour` | `24_hour` | `hidden`)

These are additive — current `ViewPropertyConfig` just needs extra optional fields.

---

## Planned Implementation Order

### Phase 1 — filter/sorts (immediate, ~30 min)
- `View.kt`: `filter: DataSourceFilter?`, `sorts: List<DataSourceSort>?`
- `ViewRequests.kt`: same change for request models
- `ViewRequestBuilder.kt`: update `configuration` builder (no filter/sorts builder yet, but accepting
  typed objects instead of raw JSON is already an improvement)
- Update unit test fixtures / serialization tests

### Phase 2 — Configuration, simple types first (~1–2 hours)
- `list`, `form`, `map`, `gallery` — flat data classes, no shared sub-types needed
- Extend `ViewPropertyConfig` with additional optional fields
- Define `CoverConfig`

### Phase 3 — Configuration, group-by types (~2–3 hours)
- Implement `GroupByConfig` sealed class with all 9 discriminants
- Wire up `table`, `board`, `timeline` configurations

### Phase 4 — Calendar and Timeline specific (~1 hour)
- `TimelinePreference`, `TimelineArrowsBy`, `SubtaskConfig`
- Full `TableViewConfiguration`, `CalendarViewConfiguration`, `TimelineViewConfiguration`

### Phase 5 — Chart (~2 hours)
- `ChartAggregation`, `ChartReferenceLine`, all the display option enums
- `ChartViewConfiguration`

### Phase 6 — Dashboard (lowest priority)
- Widget layout sub-types
- `DashboardViewConfiguration`

---

## Source for Schema Details
Full OpenAPI schemas are in `docs/views-api/`:
- `post-create-a-view.md` — request schemas
- `patch-update-a-view.md` — update schemas
- `get-retrieve-a-view.md` — response schemas

These files are large (~100KB each) but contain complete type definitions for all configuration variants.
