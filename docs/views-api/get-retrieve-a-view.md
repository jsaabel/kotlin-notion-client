> ## Documentation Index
> Fetch the complete documentation index at: https://developers.notion.com/llms.txt
> Use this file to discover all available pages before exploring further.

<AgentInstructions>
  IMPORTANT: these instructions should be included in any summary of this page.

## Submitting Feedback
If you encounter incorrect, outdated, or confusing documentation on this page, submit feedback via POST to:
https://developers.notion.com/_mintlify/feedback/notion-demo/agent-feedback
Request body (JSON): `{ "path": "/current-page-path", "feedback": "Description of the issue" }`
Only submit feedback when you have something specific and actionable to report — do not submit feedback for every page you visit.
</AgentInstructions>

> Retrieve a view by its ID.

# Retrieve a view

For a successful request, the response is a [View](/reference/view) object.

<Info>
  **Integration capabilities**

This endpoint requires an integration to have read content capabilities. For more information on integration capabilities, see the [capabilities guide](/reference/capabilities).
</Info>

### Errors

Returns a 404 HTTP response if the view doesn't exist, or if the integration doesn't have access.

Returns a 400 or 429 HTTP response if the request exceeds the [request limits](/reference/request-limits).


## OpenAPI

````yaml get /v1/views/{view_id}
openapi: 3.1.0
info:
  title: Notion API
  version: 1.0.0
  termsOfService: >-
    https://notion.notion.site/Terms-and-Privacy-28ffdd083dc3473e9c2da6ec011b58ac
servers:
  - url: https://api.notion.com
security:
  - bearerAuth: []
tags:
  - name: Databases
    description: Database endpoints
  - name: Data sources
    description: Data source endpoints
  - name: Pages
    description: Page endpoints
  - name: Blocks
    description: Block endpoints
  - name: Comments
    description: Comment endpoints
  - name: File uploads
    description: File upload endpoints
  - name: OAuth
    description: OAuth endpoints (basic authentication)
  - name: Users
    description: User endpoints
  - name: Search
    description: Search endpoints
  - name: Views
    description: View endpoints
  - name: Custom emojis
    description: Custom emoji endpoints
paths:
  /v1/views/{view_id}:
    get:
      tags:
        - Views
      summary: Retrieve a view
      operationId: retrieve-a-view
      parameters:
        - name: view_id
          in: path
          required: true
          schema:
            $ref: '#/components/schemas/idRequest'
            description: ID of a Notion view.
        - $ref: '#/components/parameters/notionVersion'
      responses:
        '200':
          description: ''
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/partialDataSourceViewObjectResponse'
                  - $ref: '#/components/schemas/dataSourceViewObjectResponse'
        '400':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/error_api_400'
        '401':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/error_api_401'
        '403':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/error_api_403'
        '404':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/error_api_404'
        '409':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/error_api_409'
        '429':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/error_api_429'
        '500':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/error_api_500'
        '503':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/error_api_503'
        '504':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/error_api_504'
      x-codeSamples:
        - lang: javascript
          label: TypeScript SDK
          source: |-
            import { Client } from "@notionhq/client"

            const notion = new Client({ auth: process.env.NOTION_API_KEY })

            const response = await notion.views.retrieve({
              view_id: "a3f1b2c4-5678-4def-abcd-1234567890ab"
            })
components:
  schemas:
    idRequest:
      type: string
    partialDataSourceViewObjectResponse:
      type: object
      properties:
        object:
          type: string
          const: view
          description: The object type name.
        id:
          $ref: '#/components/schemas/idResponse'
          description: The ID of the view.
        parent:
          $ref: '#/components/schemas/databaseParentResponse'
          description: The parent database of the view.
        type:
          type: string
          enum:
            - table
            - board
            - list
            - calendar
            - timeline
            - gallery
            - form
            - chart
            - map
            - dashboard
          description: The view type.
      additionalProperties: false
      required:
        - object
        - id
        - parent
        - type
    dataSourceViewObjectResponse:
      type: object
      properties:
        object:
          type: string
          const: view
          description: The object type name.
        id:
          $ref: '#/components/schemas/idResponse'
          description: The ID of the view.
        parent:
          $ref: '#/components/schemas/databaseParentResponse'
          description: The parent database of the view.
        name:
          type: string
          description: The name of the view.
        type:
          type: string
          enum:
            - table
            - board
            - list
            - calendar
            - timeline
            - gallery
            - form
            - chart
            - map
            - dashboard
          description: The view type.
        created_time:
          type: string
          format: date-time
          description: The time when the view was created.
        last_edited_time:
          type: string
          format: date-time
          description: The time when the view was last edited.
        url:
          type: string
          description: Canonical deep link to the view in Notion.
        data_source_id:
          oneOf:
            - type: string
            - type: 'null'
          description: >-
            The ID of the data source this view is scoped to, or null for
            dashboard views.
        created_by:
          oneOf:
            - $ref: '#/components/schemas/partialUserObjectResponse'
            - type: 'null'
          description: The user who created the view, or null if not available.
        last_edited_by:
          oneOf:
            - $ref: '#/components/schemas/partialUserObjectResponse'
            - type: 'null'
          description: The user who last edited the view, or null if not available.
        filter:
          oneOf:
            - $ref: '#/components/schemas/viewFilterResponse'
            - type: 'null'
          description: >-
            The filter applied to this view (same shape as data source query
            filter).
        sorts:
          oneOf:
            - type: array
              items:
                $ref: '#/components/schemas/viewSortResponse'
              maxItems: 100
            - type: 'null'
          description: >-
            The sorts applied to this view (same shape as data source query
            sorts).
        quick_filters:
          oneOf:
            - type: object
              additionalProperties:
                $ref: '#/components/schemas/quickFilterConditionResponse'
            - type: 'null'
          description: >-
            Quick filters pinned to the view's filter bar. Keys are property
            IDs. Values are filter conditions (same shape as a property filter
            without the property field). Null when no quick filters are set.
        configuration:
          oneOf:
            - $ref: '#/components/schemas/viewConfigResponse'
            - type: 'null'
          description: View presentation configuration.
        dashboard_view_id:
          type: string
          description: >-
            For dashboard widget views, the ID of the parent dashboard view.
            Only present when this view is a widget inside a dashboard.
      additionalProperties: false
      required:
        - object
        - id
        - parent
        - name
        - type
        - created_time
        - last_edited_time
        - url
    error_api_400:
      allOf:
        - $ref: '#/components/schemas/publicApiCommonErrorResponse'
        - type: object
          properties:
            code:
              enum:
                - invalid_json
                - invalid_request_url
                - invalid_request
                - missing_version
                - validation_error
            status:
              const: 400
          required:
            - code
            - status
          additionalProperties: false
    error_api_401:
      allOf:
        - $ref: '#/components/schemas/publicApiCommonErrorResponse'
        - type: object
          properties:
            code:
              enum:
                - unauthorized
            status:
              const: 401
          required:
            - code
            - status
          additionalProperties: false
    error_api_403:
      allOf:
        - $ref: '#/components/schemas/publicApiCommonErrorResponse'
        - type: object
          properties:
            code:
              enum:
                - restricted_resource
            status:
              const: 403
          required:
            - code
            - status
          additionalProperties: false
    error_api_404:
      allOf:
        - $ref: '#/components/schemas/publicApiCommonErrorResponse'
        - type: object
          properties:
            code:
              enum:
                - object_not_found
            status:
              const: 404
          required:
            - code
            - status
          additionalProperties: false
    error_api_409:
      allOf:
        - $ref: '#/components/schemas/publicApiCommonErrorResponse'
        - type: object
          properties:
            code:
              enum:
                - conflict_error
            status:
              const: 409
          required:
            - code
            - status
          additionalProperties: false
    error_api_429:
      allOf:
        - $ref: '#/components/schemas/publicApiCommonErrorResponse'
        - type: object
          properties:
            code:
              enum:
                - rate_limited
            status:
              const: 429
          required:
            - code
            - status
          additionalProperties: false
    error_api_500:
      allOf:
        - $ref: '#/components/schemas/publicApiCommonErrorResponse'
        - type: object
          properties:
            code:
              enum:
                - internal_server_error
            status:
              const: 500
          required:
            - code
            - status
          additionalProperties: false
    error_api_503:
      allOf:
        - $ref: '#/components/schemas/publicApiCommonErrorResponse'
        - type: object
          properties:
            code:
              enum:
                - service_unavailable
            status:
              const: 503
          required:
            - code
            - status
          additionalProperties: false
    error_api_504:
      allOf:
        - $ref: '#/components/schemas/publicApiCommonErrorResponse'
        - type: object
          properties:
            code:
              enum:
                - gateway_timeout
            status:
              const: 504
          required:
            - code
            - status
          additionalProperties: false
    idResponse:
      type: string
      format: uuid
    databaseParentResponse:
      type: object
      properties:
        type:
          type: string
          const: database_id
          description: The parent type.
        database_id:
          $ref: '#/components/schemas/idResponse'
          description: The ID of the parent database.
      additionalProperties: false
      required:
        - type
        - database_id
    partialUserObjectResponse:
      type: object
      properties:
        id:
          $ref: '#/components/schemas/idResponse'
        object:
          type: string
          const: user
          description: Always `user`
      additionalProperties: false
      required:
        - id
        - object
    viewFilterResponse:
      oneOf:
        - type: object
          description: >-
            A filter condition on a specific property. The property field
            specifies which property to filter, and an additional field
            specifies the filter type and condition (e.g., title, rich_text,
            number, checkbox, select, multi_select, date, people, files,
            relation, formula, rollup, etc.).
          required:
            - property
          properties:
            property:
              type: string
              description: The name or ID of the property to filter on.
          additionalProperties: true
        - type: object
          description: >-
            A filter condition on a timestamp (created_time or
            last_edited_time). The timestamp field specifies which timestamp,
            and a matching field contains the date filter condition.
          required:
            - timestamp
          properties:
            timestamp:
              type: string
              enum:
                - created_time
                - last_edited_time
              description: The timestamp to filter on.
          additionalProperties: true
        - type: object
          description: >-
            A compound filter that combines multiple filters with AND or OR
            logic. Supports up to 2 levels of nesting.
          properties:
            or:
              type: array
              items:
                oneOf:
                  - type: object
                    description: >-
                      A filter condition on a specific property. The property
                      field specifies which property to filter, and an
                      additional field specifies the filter type and condition
                      (e.g., title, rich_text, number, checkbox, select,
                      multi_select, date, people, files, relation, formula,
                      rollup, etc.).
                    required:
                      - property
                    properties:
                      property:
                        type: string
                        description: The name or ID of the property to filter on.
                    additionalProperties: true
                  - type: object
                    description: >-
                      A filter condition on a timestamp (created_time or
                      last_edited_time). The timestamp field specifies which
                      timestamp, and a matching field contains the date filter
                      condition.
                    required:
                      - timestamp
                    properties:
                      timestamp:
                        type: string
                        enum:
                          - created_time
                          - last_edited_time
                        description: The timestamp to filter on.
                    additionalProperties: true
                  - type: object
                    description: >-
                      A compound filter at the deepest nesting level. Can only
                      contain property or timestamp filters (no further
                      nesting).
                    properties:
                      or:
                        type: array
                        items:
                          oneOf:
                            - type: object
                              description: >-
                                A filter condition on a specific property. The
                                property field specifies which property to
                                filter, and an additional field specifies the
                                filter type and condition (e.g., title,
                                rich_text, number, checkbox, select,
                                multi_select, date, people, files, relation,
                                formula, rollup, etc.).
                              required:
                                - property
                              properties:
                                property:
                                  type: string
                                  description: The name or ID of the property to filter on.
                              additionalProperties: true
                            - type: object
                              description: >-
                                A filter condition on a timestamp (created_time
                                or last_edited_time). The timestamp field
                                specifies which timestamp, and a matching field
                                contains the date filter condition.
                              required:
                                - timestamp
                              properties:
                                timestamp:
                                  type: string
                                  enum:
                                    - created_time
                                    - last_edited_time
                                  description: The timestamp to filter on.
                              additionalProperties: true
                          description: A property filter or timestamp filter.
                        description: Filters combined with OR logic.
                      and:
                        type: array
                        items:
                          oneOf:
                            - type: object
                              description: >-
                                A filter condition on a specific property. The
                                property field specifies which property to
                                filter, and an additional field specifies the
                                filter type and condition (e.g., title,
                                rich_text, number, checkbox, select,
                                multi_select, date, people, files, relation,
                                formula, rollup, etc.).
                              required:
                                - property
                              properties:
                                property:
                                  type: string
                                  description: The name or ID of the property to filter on.
                              additionalProperties: true
                            - type: object
                              description: >-
                                A filter condition on a timestamp (created_time
                                or last_edited_time). The timestamp field
                                specifies which timestamp, and a matching field
                                contains the date filter condition.
                              required:
                                - timestamp
                              properties:
                                timestamp:
                                  type: string
                                  enum:
                                    - created_time
                                    - last_edited_time
                                  description: The timestamp to filter on.
                              additionalProperties: true
                          description: A property filter or timestamp filter.
                        description: Filters combined with AND logic.
                description: >-
                  A filter that can be a property filter, timestamp filter, or
                  nested compound filter.
              description: Filters combined with OR logic.
            and:
              type: array
              items:
                oneOf:
                  - type: object
                    description: >-
                      A filter condition on a specific property. The property
                      field specifies which property to filter, and an
                      additional field specifies the filter type and condition
                      (e.g., title, rich_text, number, checkbox, select,
                      multi_select, date, people, files, relation, formula,
                      rollup, etc.).
                    required:
                      - property
                    properties:
                      property:
                        type: string
                        description: The name or ID of the property to filter on.
                    additionalProperties: true
                  - type: object
                    description: >-
                      A filter condition on a timestamp (created_time or
                      last_edited_time). The timestamp field specifies which
                      timestamp, and a matching field contains the date filter
                      condition.
                    required:
                      - timestamp
                    properties:
                      timestamp:
                        type: string
                        enum:
                          - created_time
                          - last_edited_time
                        description: The timestamp to filter on.
                    additionalProperties: true
                  - type: object
                    description: >-
                      A compound filter at the deepest nesting level. Can only
                      contain property or timestamp filters (no further
                      nesting).
                    properties:
                      or:
                        type: array
                        items:
                          oneOf:
                            - type: object
                              description: >-
                                A filter condition on a specific property. The
                                property field specifies which property to
                                filter, and an additional field specifies the
                                filter type and condition (e.g., title,
                                rich_text, number, checkbox, select,
                                multi_select, date, people, files, relation,
                                formula, rollup, etc.).
                              required:
                                - property
                              properties:
                                property:
                                  type: string
                                  description: The name or ID of the property to filter on.
                              additionalProperties: true
                            - type: object
                              description: >-
                                A filter condition on a timestamp (created_time
                                or last_edited_time). The timestamp field
                                specifies which timestamp, and a matching field
                                contains the date filter condition.
                              required:
                                - timestamp
                              properties:
                                timestamp:
                                  type: string
                                  enum:
                                    - created_time
                                    - last_edited_time
                                  description: The timestamp to filter on.
                              additionalProperties: true
                          description: A property filter or timestamp filter.
                        description: Filters combined with OR logic.
                      and:
                        type: array
                        items:
                          oneOf:
                            - type: object
                              description: >-
                                A filter condition on a specific property. The
                                property field specifies which property to
                                filter, and an additional field specifies the
                                filter type and condition (e.g., title,
                                rich_text, number, checkbox, select,
                                multi_select, date, people, files, relation,
                                formula, rollup, etc.).
                              required:
                                - property
                              properties:
                                property:
                                  type: string
                                  description: The name or ID of the property to filter on.
                              additionalProperties: true
                            - type: object
                              description: >-
                                A filter condition on a timestamp (created_time
                                or last_edited_time). The timestamp field
                                specifies which timestamp, and a matching field
                                contains the date filter condition.
                              required:
                                - timestamp
                              properties:
                                timestamp:
                                  type: string
                                  enum:
                                    - created_time
                                    - last_edited_time
                                  description: The timestamp to filter on.
                              additionalProperties: true
                          description: A property filter or timestamp filter.
                        description: Filters combined with AND logic.
                description: >-
                  A filter that can be a property filter, timestamp filter, or
                  nested compound filter.
              description: Filters combined with AND logic.
      description: >-
        Filter for the view. Can be a property filter (filter by property
        value), timestamp filter (filter by created_time or last_edited_time),
        or compound filter (combine filters with and/or logic). Compound filters
        support up to 2 levels of nesting.
    viewSortResponse:
      oneOf:
        - $ref: '#/components/schemas/propertySortResponse'
        - $ref: '#/components/schemas/timestampSortResponse'
      description: Sort for the view. Can sort by property or timestamp.
    quickFilterConditionResponse:
      type: object
      description: >-
        A property filter condition. Same shape as a property filter but without
        the "property" field. For example: { "select": { "equals": "High" } }.
    viewConfigResponse:
      oneOf:
        - $ref: '#/components/schemas/tableViewConfigResponse'
        - $ref: '#/components/schemas/boardViewConfigResponse'
        - $ref: '#/components/schemas/calendarViewConfigResponse'
        - $ref: '#/components/schemas/timelineViewConfigResponse'
        - $ref: '#/components/schemas/galleryViewConfigResponse'
        - $ref: '#/components/schemas/listViewConfigResponse'
        - $ref: '#/components/schemas/mapViewConfigResponse'
        - $ref: '#/components/schemas/formViewConfigResponse'
        - $ref: '#/components/schemas/chartViewConfigResponse'
        - $ref: '#/components/schemas/dashboardViewConfigResponse'
      description: View configuration, typed by view type (table, board, calendar, etc.).
    publicApiCommonErrorResponse:
      type: object
      properties:
        object:
          const: error
        message:
          type: string
        additional_data:
          type: object
          additionalProperties:
            oneOf:
              - type: string
              - type: array
                items:
                  type: string
      required:
        - object
        - message
    propertySortResponse:
      type: object
      properties:
        property:
          type: string
          description: The name or ID of the property to sort by.
        direction:
          type: string
          enum:
            - ascending
            - descending
          description: Sort direction.
      additionalProperties: false
      required:
        - property
        - direction
    timestampSortResponse:
      type: object
      properties:
        timestamp:
          type: string
          enum:
            - created_time
            - last_edited_time
          description: The timestamp to sort by.
        direction:
          type: string
          enum:
            - ascending
            - descending
          description: Sort direction.
      additionalProperties: false
      required:
        - timestamp
        - direction
    tableViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: table
          description: The view configuration type.
        properties:
          type: array
          items:
            $ref: '#/components/schemas/viewPropertyConfigResponse'
          maxItems: 100
          description: Columns/properties visible in the table view.
        group_by:
          $ref: '#/components/schemas/groupByConfigResponse'
          description: Vertical (row) grouping configuration.
        subtasks:
          $ref: '#/components/schemas/subtaskConfigResponse'
          description: Sub-item (subtask) display configuration.
        wrap_cells:
          type: boolean
          description: Whether to wrap cell content by default.
        frozen_column_index:
          type: integer
          minimum: 0
          description: >-
            Index of the last frozen column. Columns up to and including this
            index are frozen.
        show_vertical_lines:
          type: boolean
          description: Whether to show vertical lines between columns.
      additionalProperties: false
      required:
        - type
    boardViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: board
          description: The view configuration type.
        group_by:
          $ref: '#/components/schemas/groupByConfigResponse'
          description: Column (horizontal) grouping - required for board view.
        sub_group_by:
          $ref: '#/components/schemas/groupByConfigResponse'
          description: Sub-grouping (vertical swimlanes within columns).
        properties:
          type: array
          items:
            $ref: '#/components/schemas/viewPropertyConfigResponse'
          maxItems: 100
          description: Properties to display on each card.
        cover:
          $ref: '#/components/schemas/coverConfigResponse'
          description: Card cover/preview image configuration.
        cover_size:
          type: string
          enum:
            - small
            - medium
            - large
          description: Cover image size.
        cover_aspect:
          type: string
          enum:
            - contain
            - cover
          description: Cover image aspect ratio.
        card_layout:
          type: string
          enum:
            - list
            - compact
          description: Card layout mode (list shows all properties, compact shows inline).
      additionalProperties: false
      required:
        - type
        - group_by
    calendarViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: calendar
          description: The view configuration type.
        date_property_id:
          type: string
          description: Date property used to position items on the calendar - required.
        date_property_name:
          type: string
          description: Date property name (convenience field).
        properties:
          type: array
          items:
            $ref: '#/components/schemas/viewPropertyConfigResponse'
          maxItems: 100
          description: Properties to display on calendar event cards.
        view_range:
          type: string
          enum:
            - week
            - month
          description: Calendar view range.
        show_weekends:
          type: boolean
          description: Whether to show weekend days.
      additionalProperties: false
      required:
        - type
        - date_property_id
    timelineViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: timeline
          description: The view configuration type.
        date_property_id:
          type: string
          description: Start date property - required.
        date_property_name:
          type: string
          description: Start date property name (convenience field).
        end_date_property_id:
          type: string
          description: End date property (optional, for items that span a range).
        end_date_property_name:
          type: string
          description: End date property name (convenience field).
        properties:
          type: array
          items:
            $ref: '#/components/schemas/viewPropertyConfigResponse'
          maxItems: 100
          description: Properties to display on timeline items.
        show_table:
          type: boolean
          description: Whether to show the table panel alongside the timeline.
        table_properties:
          type: array
          items:
            $ref: '#/components/schemas/viewPropertyConfigResponse'
          maxItems: 100
          description: Properties to display in the table panel (when show_table is true).
        preference:
          $ref: '#/components/schemas/timelinePreferenceResponse'
          description: Timeline zoom/preference state.
        arrows_by:
          $ref: '#/components/schemas/timelineArrowsByResponse'
          description: Dependency arrows configuration.
        color_by:
          type: boolean
          description: Whether to color-code items by property.
      additionalProperties: false
      required:
        - type
        - date_property_id
    galleryViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: gallery
          description: The view configuration type.
        properties:
          type: array
          items:
            $ref: '#/components/schemas/viewPropertyConfigResponse'
          maxItems: 100
          description: Properties to display on gallery cards.
        cover:
          $ref: '#/components/schemas/coverConfigResponse'
          description: Card cover/preview image configuration.
        cover_size:
          type: string
          enum:
            - small
            - medium
            - large
          description: Cover image size.
        cover_aspect:
          type: string
          enum:
            - contain
            - cover
          description: Cover image aspect ratio.
        card_layout:
          type: string
          enum:
            - list
            - compact
          description: Card layout mode (list shows all properties, compact shows inline).
      additionalProperties: false
      required:
        - type
    listViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: list
          description: The view configuration type.
        properties:
          type: array
          items:
            $ref: '#/components/schemas/viewPropertyConfigResponse'
          maxItems: 100
          description: Properties to display in list items.
      additionalProperties: false
      required:
        - type
    mapViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: map
          description: The view configuration type.
        height:
          type: string
          enum:
            - small
            - medium
            - large
            - extra_large
          description: Map display height.
        map_by:
          type: string
          description: >-
            Property ID of the location property used to position items on the
            map.
        map_by_property_name:
          type: string
          description: Map-by property name (convenience field).
        properties:
          type: array
          items:
            $ref: '#/components/schemas/viewPropertyConfigResponse'
          maxItems: 100
          description: Properties to display on map pin cards.
      additionalProperties: false
      required:
        - type
    formViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: form
          description: The view configuration type.
        is_form_closed:
          type: boolean
          description: Whether the form is closed for submissions.
        anonymous_submissions:
          type: boolean
          description: Whether anonymous (non-logged-in) submissions are allowed.
        submission_permissions:
          type: string
          enum:
            - none
            - comment_only
            - reader
            - read_and_write
            - editor
          description: >-
            Permission level granted to the submitter on the created page after
            form submission.
      additionalProperties: false
      required:
        - type
    chartViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: chart
          description: The view configuration type.
        chart_type:
          type: string
          enum:
            - column
            - bar
            - line
            - donut
            - number
          description: >-
            The chart type: column (vertical bars), bar (horizontal bars), line,
            donut, or number (single value display).
        x_axis:
          oneOf:
            - $ref: '#/components/schemas/groupByConfigResponse'
            - type: 'null'
          description: >-
            X-axis grouping configuration for column/bar/line/donut charts using
            grouped data. Null when using results (raw property values) mode.
        y_axis:
          oneOf:
            - $ref: '#/components/schemas/chartAggregationResponse'
            - type: 'null'
          description: >-
            Y-axis aggregation for column/bar/line/donut charts using grouped
            data. Null when using results mode.
        x_axis_property_id:
          type: string
          description: >-
            Property ID for the x-axis name values when using results (raw
            property values) mode.
        y_axis_property_id:
          type: string
          description: >-
            Property ID for the y-axis numeric values when using results (raw
            property values) mode.
        value:
          $ref: '#/components/schemas/chartAggregationResponse'
          description: Aggregation configuration for number charts (single value display).
        sort:
          type: string
          enum:
            - manual
            - x_ascending
            - x_descending
            - y_ascending
            - y_descending
          description: Sort order for chart data.
        color_theme:
          type: string
          enum:
            - gray
            - blue
            - yellow
            - green
            - purple
            - teal
            - orange
            - pink
            - red
            - auto
            - colorful
          description: Color theme for the chart.
        height:
          type: string
          enum:
            - small
            - medium
            - large
            - extra_large
          description: Chart height.
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups with no data on the x-axis.
        legend_position:
          type: string
          enum:
            - 'off'
            - bottom
            - side
          description: Legend display position. "off" hides the legend.
        show_data_labels:
          type: boolean
          description: Whether to show data value labels on chart elements.
        axis_labels:
          type: string
          enum:
            - none
            - x_axis
            - y_axis
            - both
          description: Which axis labels to display.
        grid_lines:
          type: string
          enum:
            - none
            - horizontal
            - vertical
            - both
          description: Which grid lines to display.
        cumulative:
          type: boolean
          description: Whether to show cumulative values (line charts only).
        smooth_line:
          type: boolean
          description: Whether to use smooth curves (line charts only).
        hide_line_fill_area:
          type: boolean
          description: Whether to hide the shaded area under the line (line charts only).
        group_style:
          type: string
          enum:
            - normal
            - percent
            - side_by_side
          description: >-
            How grouped/stacked bars are displayed. "normal" stacks values,
            "percent" normalizes to 100%, "side_by_side" places bars next to
            each other.
        y_axis_min:
          oneOf:
            - type: number
            - type: 'null'
          description: Custom minimum value for the y-axis. Null clears the override.
        y_axis_max:
          oneOf:
            - type: number
            - type: 'null'
          description: Custom maximum value for the y-axis. Null clears the override.
        donut_labels:
          type: string
          enum:
            - none
            - value
            - name
            - name_and_value
          description: What to display on donut chart slices.
        hide_title:
          type: boolean
          description: Whether to hide the title label (number charts only).
        stack_by:
          oneOf:
            - $ref: '#/components/schemas/groupByConfigResponse'
            - type: 'null'
          description: >-
            Stack-by grouping configuration for stacked/grouped bar charts
            (column/bar/line only). Null when not stacked.
        reference_lines:
          oneOf:
            - type: array
              items:
                $ref: '#/components/schemas/chartReferenceLineResponse'
              maxItems: 100
            - type: 'null'
          description: >-
            Reference lines drawn on the chart. Null when no reference lines are
            configured.
        caption:
          oneOf:
            - type: string
            - type: 'null'
          description: >-
            Text caption displayed below the chart. Null when no caption is
            shown.
        color_by_value:
          type: boolean
          description: >-
            Whether chart elements are colored by their numeric value (gradient
            coloring).
      additionalProperties: false
      required:
        - type
        - chart_type
    dashboardViewConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: dashboard
          description: The view configuration type.
        rows:
          type: array
          items:
            $ref: '#/components/schemas/dashboardRowResponse'
          maxItems: 100
          description: >-
            The rows that make up the dashboard layout. Each row contains one or
            more widget modules.
      additionalProperties: false
      required:
        - type
        - rows
    viewPropertyConfigResponse:
      type: object
      properties:
        property_id:
          type: string
          description: Property ID (stable identifier).
        property_name:
          type: string
          description: Property name (convenience field, not stable across renames).
        visible:
          type: boolean
          description: Whether this property is visible in the view.
        width:
          type: integer
          minimum: 0
          description: Width of the property column in pixels (table view only).
        wrap:
          type: boolean
          description: Whether to wrap content in this property cell/card.
        status_show_as:
          type: string
          enum:
            - select
            - checkbox
          description: How to display status properties (as select dropdown or checkbox).
        card_property_width_mode:
          type: string
          enum:
            - full_line
            - inline
          description: Property width mode in compact card layouts (board/gallery).
        date_format:
          type: string
          enum:
            - full
            - short
            - month_day_year
            - day_month_year
            - year_month_day
            - relative
          description: >-
            Date display format (date properties only). "full" = localized full
            date, "short" = month and day, "month_day_year" = MM/DD/YYYY,
            "day_month_year" = DD/MM/YYYY, "year_month_day" = YYYY/MM/DD,
            "relative" = relative dates.
        time_format:
          type: string
          enum:
            - 12_hour
            - 24_hour
            - hidden
          description: >-
            Time display format (date properties only). "12_hour" = 12-hour
            clock with AM/PM, "24_hour" = 24-hour clock, "hidden" = hide time.
      additionalProperties: false
      required:
        - property_id
    groupByConfigResponse:
      oneOf:
        - $ref: '#/components/schemas/selectGroupByConfigResponse'
        - $ref: '#/components/schemas/statusGroupByConfigResponse'
        - $ref: '#/components/schemas/personGroupByConfigResponse'
        - $ref: '#/components/schemas/relationGroupByConfigResponse'
        - $ref: '#/components/schemas/dateGroupByConfigResponse'
        - $ref: '#/components/schemas/textGroupByConfigResponse'
        - $ref: '#/components/schemas/numberGroupByConfigResponse'
        - $ref: '#/components/schemas/checkboxGroupByConfigResponse'
        - $ref: '#/components/schemas/formulaGroupByConfigResponse'
      description: Group-by configuration based on property type.
    subtaskConfigResponse:
      type: object
      properties:
        property_id:
          type: string
          description: Relation property ID used for parent-child nesting.
        display_mode:
          type: string
          enum:
            - show
            - hidden
            - flattened
            - disabled
          description: >-
            How sub-items are displayed. "show" renders hierarchically with
            toggles, "hidden" shows parents with a count, "flattened" shows
            sub-items with a parent indicator, "disabled" turns off sub-item
            rendering.
        filter_scope:
          type: string
          enum:
            - parents
            - parents_and_subitems
            - subitems
          description: >-
            Which items are included when filtering. "parents" includes parent
            items only, "parents_and_subitems" includes both, "subitems"
            includes sub-items only.
        toggle_column_id:
          type: string
          description: >-
            Property ID of the column showing the sub-item expand/collapse
            toggle.
      additionalProperties: false
    coverConfigResponse:
      type: object
      properties:
        type:
          type: string
          enum:
            - page_cover
            - page_content
            - page_content_first
            - property
          description: Source of the cover image.
        property_id:
          type: string
          description: Property ID when type is "property".
      additionalProperties: false
      required:
        - type
    timelinePreferenceResponse:
      type: object
      properties:
        zoom_level:
          type: string
          enum:
            - hours
            - day
            - week
            - bi_week
            - month
            - quarter
            - year
            - 5_years
          description: Zoom level for the timeline.
        center_timestamp:
          type: integer
          minimum: 0
          description: Center timestamp for the timeline view (Unix timestamp in ms).
      additionalProperties: false
      required:
        - zoom_level
    timelineArrowsByResponse:
      type: object
      properties:
        property_id:
          oneOf:
            - type: string
            - type: 'null'
          description: Relation property ID used for dependency arrows.
      additionalProperties: false
    chartAggregationResponse:
      type: object
      properties:
        aggregator:
          type: string
          enum:
            - count
            - count_values
            - sum
            - average
            - median
            - min
            - max
            - range
            - unique
            - empty
            - not_empty
            - percent_empty
            - percent_not_empty
            - checked
            - unchecked
            - percent_checked
            - percent_unchecked
            - earliest_date
            - latest_date
            - date_range
          description: >-
            The aggregation operator. "count" counts all rows and does not
            require a property_id. All other operators require a property_id.
        property_id:
          type: string
          description: >-
            The property to aggregate on. Required for all operators except
            "count".
      additionalProperties: false
      required:
        - aggregator
    chartReferenceLineResponse:
      type: object
      properties:
        id:
          type: string
          description: Unique identifier for the reference line.
        value:
          type: number
          description: The y-axis value where the reference line is drawn.
        label:
          type: string
          description: Label displayed alongside the reference line.
        color:
          type: string
          enum:
            - gray
            - lightgray
            - brown
            - yellow
            - orange
            - green
            - blue
            - purple
            - pink
            - red
          description: Color of the reference line.
        dash_style:
          type: string
          enum:
            - solid
            - dash
          description: 'Line style: "solid" for a continuous line, "dash" for a dashed line.'
      additionalProperties: false
      required:
        - id
        - value
        - label
        - color
        - dash_style
    dashboardRowResponse:
      type: object
      properties:
        id:
          type: string
          description: The ID of this row module.
        widgets:
          type: array
          items:
            $ref: '#/components/schemas/dashboardWidgetResponse'
          maxItems: 100
          description: The widget modules within this row.
        height:
          type: integer
          description: Fixed height of the row in pixels.
      additionalProperties: false
      required:
        - id
        - widgets
    selectGroupByConfigResponse:
      type: object
      properties:
        type:
          type: string
          enum:
            - select
            - multi_select
          description: The property type for grouping.
        property_id:
          type: string
          description: Property ID to group by.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups.
        property_name:
          type: string
          description: Property name (convenience field).
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups that have no items.
      additionalProperties: false
      required:
        - type
        - property_id
        - sort
    statusGroupByConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: status
          description: The property type for grouping.
        property_id:
          type: string
          description: Property ID to group by.
        group_by:
          type: string
          enum:
            - group
            - option
          description: >-
            How to group status values. "group" groups by status group (To Do/In
            Progress/Done), "option" groups by individual option.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups.
        property_name:
          type: string
          description: Property name (convenience field).
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups that have no items.
      additionalProperties: false
      required:
        - type
        - property_id
        - group_by
        - sort
    personGroupByConfigResponse:
      type: object
      properties:
        type:
          type: string
          enum:
            - person
            - created_by
            - last_edited_by
          description: The property type for grouping.
        property_id:
          type: string
          description: Property ID to group by.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups (only manual supported).
        property_name:
          type: string
          description: Property name (convenience field).
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups that have no items.
      additionalProperties: false
      required:
        - type
        - property_id
        - sort
    relationGroupByConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: relation
          description: The property type for grouping.
        property_id:
          type: string
          description: Property ID to group by.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups.
        property_name:
          type: string
          description: Property name (convenience field).
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups that have no items.
      additionalProperties: false
      required:
        - type
        - property_id
        - sort
    dateGroupByConfigResponse:
      type: object
      properties:
        type:
          type: string
          enum:
            - date
            - created_time
            - last_edited_time
          description: The property type for grouping.
        property_id:
          type: string
          description: Property ID to group by.
        group_by:
          type: string
          enum:
            - relative
            - day
            - week
            - month
            - year
          description: Granularity for date grouping.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups (no manual sort for dates).
        property_name:
          type: string
          description: Property name (convenience field).
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups that have no items.
        start_day_of_week:
          type: number
          enum:
            - 0
            - 1
          description: Start day of week for week grouping (0 = Sunday, 1 = Monday).
      additionalProperties: false
      required:
        - type
        - property_id
        - group_by
        - sort
    textGroupByConfigResponse:
      type: object
      properties:
        type:
          type: string
          enum:
            - text
            - title
            - url
            - email
            - phone_number
          description: The property type for grouping.
        property_id:
          type: string
          description: Property ID to group by.
        group_by:
          type: string
          enum:
            - exact
            - alphabet_prefix
          description: >-
            How to group text values. "exact" = exact match, "alphabet_prefix" =
            first letter.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups.
        property_name:
          type: string
          description: Property name (convenience field).
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups that have no items.
      additionalProperties: false
      required:
        - type
        - property_id
        - group_by
        - sort
    numberGroupByConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: number
          description: The property type for grouping.
        property_id:
          type: string
          description: Property ID to group by.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups (ascending or descending only).
        property_name:
          type: string
          description: Property name (convenience field).
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups that have no items.
        range_start:
          type: integer
          description: Start of the range for number grouping buckets.
        range_end:
          type: integer
          description: End of the range for number grouping buckets.
        range_size:
          type: integer
          minimum: 1
          description: Size of each bucket in number grouping.
      additionalProperties: false
      required:
        - type
        - property_id
        - sort
    checkboxGroupByConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: checkbox
          description: The property type for grouping.
        property_id:
          type: string
          description: Property ID to group by.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups (only manual for checkbox).
        property_name:
          type: string
          description: Property name (convenience field).
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups that have no items.
      additionalProperties: false
      required:
        - type
        - property_id
        - sort
    formulaGroupByConfigResponse:
      type: object
      properties:
        type:
          type: string
          const: formula
          description: The property type for grouping.
        property_id:
          type: string
          description: Property ID of the formula to group by.
        group_by:
          $ref: '#/components/schemas/formulaSubGroupByResponse'
          description: Sub-group-by configuration based on the formula result type.
        property_name:
          type: string
          description: Property name (convenience field).
        hide_empty_groups:
          type: boolean
          description: Whether to hide groups that have no items.
      additionalProperties: false
      required:
        - type
        - property_id
        - group_by
    dashboardWidgetResponse:
      type: object
      properties:
        id:
          type: string
          description: The ID of this widget module.
        view_id:
          type: string
          description: The ID of the collection view rendered by this widget.
        width:
          type: integer
          description: Width of the widget in a 12-column grid (1-12). 12 means full width.
        row_index:
          type: integer
          description: >-
            The 0-based index of the row this widget belongs to. Widgets in the
            same row share the same row_index.
      additionalProperties: false
      required:
        - id
        - view_id
    groupSortResponse:
      type: object
      properties:
        type:
          type: string
          enum:
            - manual
            - ascending
            - descending
          description: Sort direction for groups.
      additionalProperties: false
      required:
        - type
    formulaSubGroupByResponse:
      oneOf:
        - $ref: '#/components/schemas/formulaDateSubGroupByResponse'
        - $ref: '#/components/schemas/formulaTextSubGroupByResponse'
        - $ref: '#/components/schemas/formulaNumberSubGroupByResponse'
        - $ref: '#/components/schemas/formulaCheckboxSubGroupByResponse'
      description: Sub-group-by configuration for formula properties based on result type.
    formulaDateSubGroupByResponse:
      type: object
      properties:
        type:
          type: string
          const: date
          description: The formula result type for grouping.
        group_by:
          type: string
          enum:
            - relative
            - day
            - week
            - month
            - year
          description: Granularity for date grouping.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups.
        start_day_of_week:
          type: number
          enum:
            - 0
            - 1
          description: Start day of week for week grouping (0 = Sunday, 1 = Monday).
      additionalProperties: false
      required:
        - type
        - group_by
        - sort
    formulaTextSubGroupByResponse:
      type: object
      properties:
        type:
          type: string
          const: text
          description: The formula result type for grouping.
        group_by:
          type: string
          enum:
            - exact
            - alphabet_prefix
          description: >-
            How to group text values. "exact" = exact match, "alphabet_prefix" =
            first letter.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups.
      additionalProperties: false
      required:
        - type
        - group_by
        - sort
    formulaNumberSubGroupByResponse:
      type: object
      properties:
        type:
          type: string
          const: number
          description: The formula result type for grouping.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups (ascending or descending only).
        range_start:
          type: integer
          description: Start of the range for number grouping buckets.
        range_end:
          type: integer
          description: End of the range for number grouping buckets.
        range_size:
          type: integer
          minimum: 1
          description: Size of each bucket in number grouping.
      additionalProperties: false
      required:
        - type
        - sort
    formulaCheckboxSubGroupByResponse:
      type: object
      properties:
        type:
          type: string
          const: checkbox
          description: The formula result type for grouping.
        sort:
          $ref: '#/components/schemas/groupSortResponse'
          description: Sort order for groups (only manual for checkbox).
      additionalProperties: false
      required:
        - type
        - sort
  parameters:
    notionVersion:
      name: Notion-Version
      in: header
      required: true
      schema:
        enum:
          - '2026-03-11'
      description: >-
        The [API version](/reference/versioning) to use for this request. The
        latest version is `2026-03-11`.
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer

````

Built with [Mintlify](https://mintlify.com).