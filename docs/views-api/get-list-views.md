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

> List all views in a database.

# List views

Returns a paginated list of [View](/reference/view) references for the specified database.

<Info>
  **Integration capabilities**

This endpoint requires an integration to have read content capabilities. For more information on integration capabilities, see the [capabilities guide](/reference/capabilities).
</Info>

### Errors

Returns a 404 HTTP response if the database doesn't exist, or if the integration doesn't have access.

Returns a 400 or 429 HTTP response if the request exceeds the [request limits](/reference/request-limits).


## OpenAPI

````yaml get /v1/views
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
  /v1/views:
    get:
      tags:
        - Views
      summary: List views
      operationId: list-views
      parameters:
        - name: database_id
          in: query
          schema:
            $ref: '#/components/schemas/idRequest'
            description: >-
              ID of a Notion database (collection view block) to list views for.
              At least one of database_id or data_source_id is required.
        - name: data_source_id
          in: query
          schema:
            $ref: '#/components/schemas/idRequest'
            description: >-
              ID of a data source (collection) to list all views for, including
              linked views across the workspace. At least one of database_id or
              data_source_id is required.
        - name: start_cursor
          in: query
          schema:
            type: string
            description: >-
              If supplied, this endpoint will return a page of results starting
              after the cursor provided. If not supplied, this endpoint will
              return the first page of results.
        - name: page_size
          in: query
          schema:
            type: integer
            minimum: 1
            maximum: 100
            description: >-
              The number of items from the full list desired in the response.
              Maximum: 100
        - $ref: '#/components/parameters/notionVersion'
      responses:
        '200':
          description: ''
          content:
            application/json:
              schema:
                type: object
                properties:
                  object:
                    type: string
                    const: list
                    description: Always `list`
                  next_cursor:
                    oneOf:
                      - $ref: '#/components/schemas/idResponse'
                      - type: 'null'
                  has_more:
                    type: boolean
                  results:
                    type: array
                    items:
                      $ref: '#/components/schemas/dataSourceViewReferenceResponse'
                  type:
                    type: string
                    const: view
                    description: Always `view`
                  view:
                    $ref: '#/components/schemas/emptyObject'
                required:
                  - object
                  - next_cursor
                  - has_more
                  - results
                  - type
                  - view
                additionalProperties: false
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

            const response = await notion.views.list({
              database_id: "248104cd-477e-80fd-b757-e945d38000bd"
            })
components:
  schemas:
    idRequest:
      type: string
    idResponse:
      type: string
      format: uuid
    dataSourceViewReferenceResponse:
      type: object
      properties:
        object:
          type: string
          const: view
          description: The object type name.
        id:
          $ref: '#/components/schemas/idResponse'
          description: The ID of the view.
      additionalProperties: false
      required:
        - object
        - id
    emptyObject:
      type: object
      properties: {}
      additionalProperties: false
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