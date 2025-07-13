# Request Limits

To ensure a consistent developer experience for all API users, the Notion API is rate limited and basic size limits
apply to request parameters.

---

## Rate Limits

Rate-limited requests will return a `rate_limited` error code (HTTP status **429**). The rate limit for incoming
requests per integration is an average of **three requests per second**. Some bursts beyond the average rate are
allowed.

Integrations should accommodate variable rate limits by:

- Handling **HTTP 429** responses.
- Respecting the `Retry-After` response header (given as a number of seconds).

Requests made after waiting the specified time should no longer be rate-limited.

Alternatively, you can use **backoff strategies** by slowing down future requests. One common method is to use queues
for pending requests and only send them when Notion does not respond with a 429.

> 🚧 **Rate limits may change**
>
> Notion may adjust rate limits over time to balance demand and reliability. Future plans include different rate limits
> for workspaces on various pricing plans.

---

## Size Limits

Notion enforces limits on:

- Parameter sizes
- Request body depth

Requests exceeding these limits return a `validation_error` (HTTP status **400**) with details in the `message` field.

Integrations should:

- Avoid exceeding these proactively
- Use large test data to verify error handling
- Handle errors gracefully (e.g., logging, user alerts)

> Example: If your integration inserts URLs into Notion, it should handle cases where the URL exceeds **2000 characters
**.

> 📘 **Payload limits**
>
> - Max **1000 block elements**
> - Max **500 KB** overall request size

---

## Limits for Property Values

| Property Value Type | Inner Property        | Size Limit        |
|---------------------|-----------------------|-------------------|
| Rich text object    | `text.content`        | 2000 characters   |
| Rich text object    | `text.link.url`       | 2000 characters   |
| Rich text object    | `equation.expression` | 1000 characters   |
| Array of any blocks | *(all types)*         | 100 elements      |
| Any URL             |                       | 2000 characters   |
| Any email           |                       | 200 characters    |
| Any phone number    |                       | 200 characters    |
| Multi-select        |                       | 100 options       |
| Relation            |                       | 100 related pages |
| People              |                       | 100 users         |

> 📘 **Note:**  
> These limits apply to **requests sent to Notion's API** only.  
> The number of relations and people mentions **in responses** may have **different limits**.