# Webhooks Reference

> Source: https://developers.notion.com/reference/webhooks
> Fetched: 2026-08-22 (API version `2026-03-11`)

Prior to this refresh, the only webhooks material in `reference/notion-api/`
was `03_Webhooks.md` — an informal how-to guide (setup click-path, testing
tips), not a formal reference of the payload envelope and signature scheme.
This file is the more rigorous companion; `19_Webhooks_Events_And_Delivery.md`
covers the full event-type catalog and delivery guarantees. `03_Webhooks.md`
is left in place as the "getting started" walkthrough — its
`notion.so/my-integrations` link was updated to `notion.com` as part of this
refresh's link sweep (issue #60 item 17), but its content is otherwise
unchanged.

## Creating a subscription (not an API call)

Webhook subscriptions are configured through the UI, not a REST endpoint:

1. Go to your connection settings at
   `https://app.notion.com/developers/connections` (previously
   `notion.so/my-integrations` — see `17_Changelog_...md`'s July 15, 2026
   entry on the `notion.so` → `notion.com` link migration).
2. Open the **Webhooks** tab → **+ Create a subscription**.
3. Enter a public, SSL-secured webhook URL (`localhost` will not work).
4. Choose event types.
5. **Create subscription**.

The webhook URL can only be changed **before** verification; after
verification, changing it requires deleting and recreating the subscription.

## Verification handshake

On creation, Notion POSTs a one-time verification payload to the configured
URL:

```json
{ "verification_token": "secret_tMrlL1qK5vuQAh1b6cZGhFChZTSYJlce98V0pYn7yBl" }
```

Store the token, then paste it into the **⚠️ Verify** control on the
Webhooks tab to activate the subscription. Until verified, no events are
delivered.

## Signature verification

Every delivered event carries an `X-Notion-Signature` header: an HMAC-SHA256
hash of the **raw** request body, keyed by the subscription's
`verification_token`.

```
sha256=HMAC-SHA256(verification_token, raw_request_body)
```

Compare with constant-time equality. **Re-serialized JSON produces different
bytes and fails verification** — hash the body exactly as received, before
any JSON parse/re-stringify round-trip.

```javascript
import { createHmac, timingSafeEqual } from "crypto"
const calculatedSignature = `sha256=${createHmac("sha256", verificationToken).update(rawBody).digest("hex")}`
const isTrustedPayload = timingSafeEqual(
  Buffer.from(calculatedSignature),
  Buffer.from(headers["X-Notion-Signature"]),
)
```

```python
hmac_obj = hmac.new(verification_token.encode("utf-8"), raw_body_bytes, hashlib.sha256)
calculated_signature = "sha256=" + hmac_obj.hexdigest()
is_trusted_payload = hmac.compare_digest(calculated_signature, headers["X-Notion-Signature"])
```

```ruby
digest = OpenSSL::HMAC.hexdigest("SHA256", verification_token, raw_body)
calculated_signature = "sha256=#{digest}"
is_trusted_payload = ActiveSupport::SecurityUtils.secure_compare(calculated_signature, headers["X-Notion-Signature"])
```

`@notionhq/client` v5.23.0+ ships `verifyWebhookSignature()` (verification)
and a companion `signWebhookPayload()` (test-signing) that wrap this by
hand-rolled logic above.

## This client

Webhook signature verification, the event envelope, and typed accessors are
already implemented (see `FOLLOWUPS.md` items 29–31 for design notes:
no replay protection beyond dedupe-on-`id` guidance since Notion sends no
timestamp header; lowercase-hex-only signature comparison, matching Notion's
own casing; `updated_properties` kept as raw JSON since its shape is
genuinely polymorphic per event type). Cross-check that implementation's
comparison strictness and header name against this page on the next live
pass.
