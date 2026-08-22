# Webhooks: Event Types & Delivery

> Source: https://developers.notion.com/reference/webhooks-events-delivery
> Fetched: 2026-08-22 (API version `2026-03-11`)

Webhook events are notification signals, not full payloads — they carry
enough to identify what changed (`entity.id`/`entity.type`) but not the
changed content itself; follow up with a normal API call to fetch current
state.

## Event types

**Page events** (all aggregated except the two marked otherwise):
`page.created`, `page.content_updated`, `page.properties_updated`,
`page.moved`, `page.deleted`, `page.undeleted`, `page.locked` (not
aggregated), `page.unlocked` (not aggregated)

**Database events** (aggregated):
`database.created`, `database.content_updated` (**deprecated as of
2025-09-03**), `database.schema_updated` (**deprecated as of 2025-09-03**),
`database.moved`, `database.deleted`, `database.undeleted`

**Data source events** (aggregated, new in the 2025-09-03 API version):
`data_source.created`, `data_source.content_updated`,
`data_source.schema_updated`, `data_source.moved`, `data_source.deleted`,
`data_source.undeleted`

**Comment events** (not aggregated):
`comment.created`, `comment.updated`, `comment.deleted`

**View events** (per the 2026-03-19 Views API launch changelog entry — not
independently re-confirmed on this fetch of the events-and-delivery page,
which predates that entry; treat as likely-present but unverified here):
`view.created`, `view.updated`, `view.deleted`

## Envelope fields

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Unique event identifier |
| `timestamp` | string (ISO 8601) | Occurrence time — use this to reorder events, since delivery order isn't guaranteed |
| `workspace_id` | UUID | Originating workspace |
| `subscription_id` | UUID | The webhook subscription that matched |
| `integration_id` | UUID | The connection/bot the event is scoped to |
| `type` | string | Event type, e.g. `"page.created"` |
| `authors` | array | `{id, type}` — `type` is `person`, `bot`, or `agent` |
| `accessible_by` | array | Users/bots with access to the entity (public connections only) |
| `attempt_number` | integer | Delivery attempt, 1–8 |
| `entity` | object | `{id, type}` — `type` is `page`, `block`, `database`, `data_source`, or `comment` |
| `data` | object | Event-specific extra detail (varies by `type`) |

## Delivery guarantees

- **Timing**: events should arrive within 5 minutes; most within one minute.
- **Aggregation**: high-frequency events (e.g. `page.content_updated`) are
  batched within a short window into one notification, adding up to ~1
  minute of delay but cutting redundant deliveries.
- **Ordering**: events may arrive out of order — use `timestamp`, not
  arrival order, when sequencing matters.
- **Delivery model**: the source text states Notion "aims for at-most-once
  event delivery" (this wording was returned twice independently on this
  fetch, so it is not treated as a fetch artifact — but it reads unusually
  for a webhook system, since most describe themselves as at-least-once
  with retries; the retry behavior described immediately below is itself
  consistent with at-least-once semantics, so treat "at-most-once" here with
  some caution and re-confirm on a live pass rather than trusting it at face
  value).
- **Retries**: a failing endpoint (non-2xx or timeout) is retried up to 8
  times with exponential backoff; the final retry lands roughly 24 hours
  after the initial trigger.
- **Staleness**: a delivered event may not reflect the very latest state by
  the time you process it — always re-fetch via the API rather than trusting
  event `data` as current truth.
- **No replay protection**: no timestamp-signing scheme guards against replay
  of a captured, validly-signed request (see `18_Webhooks_Reference.md`).
  Dedupe on event `id` if this matters for your consumer (already flagged as
  a design gap for this client in `FOLLOWUPS.md` item 29 — mitigation is a
  docs/consumer concern, not something the client itself can enforce, since
  it never sees repeat/replayed deliveries as anything but a normal request).

## This client

Webhook event modeling — envelope + typed accessors, `updated_properties`
kept as raw JSON for its per-event-type polymorphism — is implemented; see
`FOLLOWUPS.md` items 29–31 for the design decisions made without a live
webhook subscription to test against.
