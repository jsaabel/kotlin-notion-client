# Request Limits

To ensure a consistent developer experience, the Notion API enforces **rate limits** and **size limits** on incoming requests.

---

## Rate Limits

If your integration exceeds the allowed rate, you'll receive:

- **HTTP status**: `429 Too Many Requests`
- **Error code**: `"rate_limited"`

### Current Limits

- **Average**: 3 requests per second per integration
- **Bursts**: Short bursts above the average may be allowed

### Handling Rate Limits

Integrations should handle `429` responses gracefully by:

- Reading the `Retry-After` response header (in **seconds**) and waiting that long before retrying
- Using backoff strategies (e.g., exponential backoff or request queuing)

> 🚧 **Note:**  
> Rate limits may change in the future. Notion may introduce **distinct limits per workspace** based on pricing plans.

---

## Size Limits

Requests that exceed size limits will return:

- **HTTP status**: `400 Bad Request`
- **Error code**: `"validation_error"`
- **Response body**: includes a `"message"` with more details

### General Payload Limits

- **Max block elements per payload**: `1000`
- **Max payload size**: `500KB`

### Limits for Property Values

| Property Value Type                  | Inner Property            | Size Limit           |
|-------------------------------------|---------------------------|----------------------|
| Rich text object                    | `text.content`            | 2000 characters      |
| Rich text object                    | `text.link.url`           | 2000 characters      |
| Rich text object                    | `equation.expression`     | 1000 characters      |
| Any array of block types (incl. rich text) | -                 | 100 elements         |
| Any URL                             | -                         | 2000 characters      |
| Any email                           | -                         | 200 characters       |
| Any phone number                    | -                         | 200 characters       |
| Any multi-select                    | -                         | 100 options          |
| Any relation                        | -                         | 100 related pages    |
| Any people                          | -                         | 100 users            |

> 📘 **Note:**  
> These limits apply to **requests sent to Notion's API**.  
> Response limits (e.g., number of relations or mentions returned) may differ.

### Best Practices

- Avoid exceeding limits proactively.
- Include test cases with large parameters to ensure proper error handling.
- For example, if reading a long URL from an external system, be ready to handle URLs longer than 2000 characters.  
  Actions may include:
    - Logging the error
    - Sending a user alert via email
    - Skipping the problematic entry