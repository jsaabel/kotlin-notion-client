# Webhooks

Notion can push change events to an HTTPS endpoint you host. This library provides:

- **Signature verification** — [`WebhookSignature`](../src/main/kotlin/it/saabel/kotlinnotionclient/webhooks/WebhookSignature.kt) (and the top-level `verifyWebhookSignature(...)` convenience), implementing the HMAC-SHA256 scheme from the [official webhooks reference](https://developers.notion.com/reference/webhooks)
- **Typed event models** — [`WebhookEvent`](../src/main/kotlin/it/saabel/kotlinnotionclient/models/webhooks/WebhookEvent.kt) and friends, matching the payloads in [Event types & delivery](https://developers.notion.com/reference/webhooks-events-delivery)

Subscription management (creating/pausing subscriptions) happens in the Notion integration settings UI, not through the REST API, so there is no client endpoint for it.

## How verification works

Every delivery carries an `X-Notion-Signature` header:

```
X-Notion-Signature: sha256=461e8cbcba8a75c3edd866f0e71280f5a85cbf21eff040ebd10fe266df38a735
```

The value after `sha256=` is an HMAC-SHA256 of the **raw request body**, keyed with your subscription's `verification_token` (delivered once, in the initial subscription handshake — persist it securely).

```kotlin
import it.saabel.kotlinnotionclient.webhooks.WebhookSignature

val ok = WebhookSignature.verify(rawBodyBytes, signatureHeader, verificationToken)
```

`verify` compares digests with `MessageDigest.isEqual` (constant time) and **fails closed**: a missing header, wrong prefix, wrong length, non-hex characters, or empty token all return `false` — it never throws on malformed input.

### ⚠️ The raw-body trap

The HMAC covers the exact bytes Notion sent. If you let your framework deserialize the body and then re-serialize a model to verify it, key ordering / whitespace / escaping will differ and verification will fail. **Capture the body as bytes or text before any JSON parsing**, verify, and only then parse. `WebhookSignature` deliberately accepts only `ByteArray`/`String` for this reason.

## The verification handshake

When you first register your endpoint, Notion sends a one-time POST:

```json
{ "verification_token": "secret_tMrlL1qK5vuQAh1b6cZGhFChZTSYJlce98V0pYn7yBl" }
```

Store this token — it signs every subsequent delivery (including, self-referentially, the handshake request itself). Use `WebhookVerificationRequest.fromJsonOrNull(rawBody)` to detect and parse it.

## Worked example: Ktor server receiver

```kotlin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import it.saabel.kotlinnotionclient.models.webhooks.WebhookEvent
import it.saabel.kotlinnotionclient.models.webhooks.WebhookEventType
import it.saabel.kotlinnotionclient.models.webhooks.WebhookVerificationRequest
import it.saabel.kotlinnotionclient.webhooks.WebhookSignature

fun main() {
    // In production, load this from secure storage after the one-time handshake.
    var verificationToken: String? = System.getenv("NOTION_VERIFICATION_TOKEN")

    embeddedServer(Netty, port = 8080) {
        routing {
            post("/notion-webhook") {
                // 1. Read the RAW body FIRST — do not install ContentNegotiation on
                //    this route or otherwise parse before verifying.
                val rawBody: ByteArray = call.receive<ByteArray>()
                val signature = call.request.headers[WebhookSignature.SIGNATURE_HEADER]

                // 2. Handle the one-time subscription handshake.
                val handshake = WebhookVerificationRequest.fromJsonOrNull(rawBody.decodeToString())
                if (handshake != null) {
                    verificationToken = handshake.verificationToken
                    // Persist it! Then confirm receipt.
                    call.respond(HttpStatusCode.OK)
                    return@post
                }

                // 3. Verify the signature against the raw bytes. Fail closed.
                val token = verificationToken
                if (token == null || !WebhookSignature.verify(rawBody, signature, token)) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                // 4. Only now parse the event.
                val event = WebhookEvent.fromJson(rawBody.decodeToString())
                when (event.eventType) {
                    WebhookEventType.PAGE_CREATED ->
                        println("Page ${event.entity?.id} created in ${event.data?.parent?.id}")
                    WebhookEventType.PAGE_PROPERTIES_UPDATED ->
                        println("Properties changed: ${event.data?.updatedPropertyIds}")
                    WebhookEventType.DATA_SOURCE_SCHEMA_UPDATED ->
                        event.data?.updatedSchemaProperties?.forEach {
                            println("Schema ${it.action}: ${it.name} (${it.id})")
                        }
                    WebhookEventType.UNKNOWN ->
                        println("Unknown event type ${event.type} — library update available?")
                    else ->
                        println("Event ${event.type} on ${event.entity?.type} ${event.entity?.id}")
                }

                // 5. Acknowledge quickly (2xx) — do slow work asynchronously.
                call.respond(HttpStatusCode.OK)
            }
        }
    }.start(wait = true)
}
```

Notes:

- **Respond fast.** Notion retries non-2xx deliveries up to 8 times with exponential backoff. Queue heavy work instead of doing it inline.
- **Events are signals, not content.** Fetch the affected entity via the regular API (`client.pages.retrieve(...)`, etc.) to get current state.
- **Ordering is not guaranteed.** Re-order by `timestamp` if your handler cares.
- `page.content_updated` aggregates rapid edits into one event with multiple `updated_blocks`.

## Event model

`WebhookEvent` mirrors the common envelope every event shares:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `String` | Unique event id |
| `timestamp` | `String` | ISO 8601 |
| `type` | `String` | Raw type, e.g. `"page.created"` |
| `eventType` | `WebhookEventType` | Typed view; `UNKNOWN` for unrecognized types |
| `workspaceId` / `workspaceName` | `String?` | Originating workspace |
| `subscriptionId` / `integrationId` | `String?` | Delivery metadata |
| `authors` | `List<WebhookPrincipal>` | `person`, `bot`, or `agent` |
| `accessibleBy` | `List<WebhookPrincipal>` | Public integrations only |
| `attemptNumber` | `Int?` | 1–8 |
| `apiVersion` | `String?` | Present on newer (2025-09-03+) events |
| `entity` | `WebhookEntityRef?` | The affected object (`id` + `type`) |
| `data` | `WebhookEventData?` | Event-specific details |

`WebhookEventData` covers the per-type payloads: `parent`, `pageId` (comment events), `updatedBlocks` (`*.content_updated`), and `updated_properties` — which the API sends in **two shapes** (an array of property-id strings for `page.properties_updated`; an array of `{id, name, action}` objects for `*.schema_updated`). Use the `updatedPropertyIds` / `updatedSchemaProperties` accessors respectively.

Unknown event types, entity types, and extra JSON keys never break deserialization — the models are tolerant by design, so a Notion-side addition degrades gracefully instead of throwing.
