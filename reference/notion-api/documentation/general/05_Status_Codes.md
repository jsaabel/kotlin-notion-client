# Status Codes

Responses from the Notion API use standard **HTTP status codes** to indicate the general class of success or error.

---

## Success Codes

| HTTP Status Code | Description                            |
|------------------|----------------------------------------|
| `200`            | Notion successfully processed the request. |

---

## Error Codes

Error responses include details in the response body using `code` and `message` properties.

| HTTP Status Code | `code`                         | Description                                                                                                                                      | Example `message`                                                                                                 |
|------------------|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `400`            | `invalid_json`                | The request body could not be decoded as JSON.                                                                                                   | `"Error parsing JSON body."`                                                                                      |
| `400`            | `invalid_request_url`         | The request URL is not valid.                                                                                                                    | `"Invalid request URL"`                                                                                           |
| `400`            | `invalid_request`             | This request is not supported.                                                                                                                   | `"Unsupported request: <request name>."`                                                                          |
| `400`            | `invalid_grant`               | The provided authorization grant or refresh token is invalid, expired, revoked, mismatched, or issued to another client.                        | `"Invalid code: this code has been revoked."`                                                                     |
| `400`            | `validation_error`            | The request body does not match the expected parameter schema.                                                                                   | `"body failed validation: body.properties should be defined, instead was undefined."`                             |
| `400`            | `missing_version`             | The required `Notion-Version` header is missing.                                                                                                 | `"Notion-Version header failed validation: Notion-Version header should be defined, instead was undefined."`     |
| `401`            | `unauthorized`                | The bearer token is not valid.                                                                                                                   | `"API token is invalid."`                                                                                         |
| `403`            | `restricted_resource`         | The token does not have permission to perform the operation.                                                                                     | `"API token does not have access to this resource."`                                                              |
| `404`            | `object_not_found`            | The resource does not exist or has not been shared with the integration.                                                                         | `"Could not find database with ID: be907abe-510e-4116-a3d1-7ea71018c06f. Make sure the relevant pages and databases are shared with your integration."` |
| `409`            | `conflict_error`              | The transaction could not be completed due to a data conflict.                                                                                   | `"Conflict occurred while saving. Please try again."`                                                             |
| `429`            | `rate_limited`                | Too many requests. See [rate limits](#rate-limits).                                                                                              | `"You have been rate limited. Please try again in a few minutes."`                                                |
| `500`            | `internal_server_error`       | An unexpected error occurred. Contact Notion support.                                                                                            | `"Unexpected error occurred."`                                                                                    |
| `502`            | `bad_gateway`                 | Notion failed to complete the request due to a connection issue.                                                                                 | `"Bad Gateway"`                                                                                                   |
| `503`            | `service_unavailable`         | Notion is unavailable (e.g., request took longer than 60s).                                                                                      | `"Notion is unavailable, please try again later."`                                                                |
| `503`            | `database_connection_unavailable` | Notion's database is unavailable or cannot be queried.                                                                                       | `"Notion is unavailable, please try again later."`                                                                |
| `504`            | `gateway_timeout`             | Notion timed out while attempting to complete the request.                                                                                       | `"Gateway Timeout"`                                                                                               |