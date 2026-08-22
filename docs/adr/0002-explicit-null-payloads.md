# ADR 0002: An intended JSON `null` is carried by a sentinel, never by a Kotlin `null`

- **Status:** accepted
- **Date:** 2026-08-22
- **Issue:** [#80](https://github.com/jsaabel/kotlin-notion-client/issues/80) (first of the #80/#81/#82 DSL-gap series)

## Context

The client encodes every request with `explicitNulls = false` (`NotionClient.kt`, now
`serialization/NotionJson.kt`). That setting is right for the dominant case: request models are
one nullable field per optional input, and a PATCH must carry only the fields it changes. A
`null` field means "this request does not touch that", and dropping it is exactly correct.

But two DSL affordances mean the opposite by `null`, and the serializer cannot tell the two
apart:

- **`icon.remove()` / `cover.remove()`.** Notion removes an icon or cover when the request
  carries `"icon": null` — `reference/notion-api/documentation/endpoints/Update_Page_2025.md`
  is explicit ("Set to `null` to remove"). Both builders set the backing field to `null`, so
  `updatePageRequest { icon.remove() }` encoded to `{}` — an empty PATCH. The call compiled,
  ran, returned successfully and did nothing.
- **Clearing a property value.** Seven `PagePropertiesBuilder` setters document "null for
  empty" and construct a value object whose payload field is `null`: `select`, `status`,
  `date`, `dateTime`, `number`, `url`, `email`, `phoneNumber`. Notion clears a property when it
  receives `{"Status":{"select":null}}`; the encoder emitted `{"Status":{"type":"select"}}` —
  the discriminator survived, the instruction did not.

Neither was visible to the test suite, and that is the more interesting half of the finding.
`UpdatePageRequestBuilderTest` asserted `request.icon shouldBe null` after `icon.remove()` —
byte-identical to the assertion for a request that never mentioned the icon. Every affected
path had a **passing model-level test**. The bug lives entirely between the model and the wire,
so only an assertion on encoded bytes can see it.

List-valued properties (`multi_select`, `people`, `relation`, `files`) are unaffected: they
clear with `[]`, which is not a null.

## Decision

Model an intended `null` as a **value**, not as the absence of one. A Kotlin `null` on a
nullable request field keeps its single, honest meaning — "this request says nothing about that
field" — and anything that must reach the wire as `null` is a non-null Kotlin value whose
serializer writes `null` on purpose. `explicitNulls = false` then never has a decision to make.

Two mechanisms in `serialization/ExplicitNullSerializers.kt`:

- **`RemovalSentinelSerializer`** — for `Icon.Removed` and `PageCover.Removed`, new variants of
  the existing sealed hierarchies. `remove()` sets the sentinel; the serializer emits JSON
  `null`. Because the field holds a non-null value, `encodeNullableSerializableElement` never
  drops it.
- **`ClearableValueSerializer`** — for the seven single-payload property values. It writes the
  payload key unconditionally, encoding through `encodeSerializableElement` with a nullable
  payload serializer rather than `encodeNullableSerializableElement`, which is where
  `explicitNulls = false` does its dropping. A cleared value and a set one then differ only in
  the payload, and the key is always present — which is also what Notion sends on the way in.

`IconSerializer` and `PageCoverSerializer` dispatch on the subclass explicitly when encoding
instead of resolving the subclass serializer reflectively, as `JsonContentPolymorphicSerializer`
does. A reflective lookup for an `object` variant can fall back to the generic object serializer
and emit `{}` — the same silent-wrong-payload failure this ADR exists to remove. Decoding still
goes through a `JsonContentPolymorphicSerializer` and is unchanged.

The client's JSON configuration moved to `serialization/NotionJson`, so tests assert against the
configuration the client actually installs rather than a hand-copied approximation of it.

This follows the shape of [ADR 0001](0001-deferred-file-upload-resolution.md): where the request
tree carries an intent the default serializer cannot express, add a sentinel variant plus a
serializer, rather than reshaping every model around it.

## Alternatives considered

- **Flip `explicitNulls` to `true` and model absence explicitly.** Semantically the more honest
  model, and it removes the *cause* rather than the instances. Rejected on blast radius: every
  `?= null` field in every request model would start serializing as `null` unless individually
  wrapped in an absence type, which is a rewrite of the whole request layer to fix two known
  sites. The sentinel mechanism is itself reusable, so a third instance is cheap — and if the
  request layer is ever reshaped for other reasons, this decision is worth revisiting.
- **`@EncodeDefault` on the nullable payload fields.** Does not work: `explicitNulls = false`
  drops a null nullable field in `encodeNullableSerializableElement`, independently of whether
  the value is a default.
- **One serializer over the whole property map that re-adds any missing key named by the
  `type` discriminator.** Less code than seven serializers, and it would cover future property
  values automatically. Rejected as fix-up-at-a-distance: the invariant lives nowhere near the
  classes it constrains, and a reader of `SelectValue` sees a nullable field with no hint that
  its null is load-bearing.
- **Leaving it and documenting that removal needs a raw request.** Rejected — the affordances
  already exist in the DSL and appear to work.

## Consequences

- **Behaviour change, not a new feature.** `icon.remove()`, `cover.remove()` and the seven
  clearing setters start doing what they always said they did. Code that called them and
  worked around them doing nothing will now see the removal happen.
- `Icon` and `PageCover` gain a variant. Both are sealed and public, so an exhaustive `when`
  over them in user code stops compiling. In this repo nothing matched exhaustively —
  `RequestValidator` and `PendingUploadResolver` both test `is …PendingUpload` and are
  unaffected.
- The sentinels are write-only: a removed icon reads back as `null`, so they never appear in a
  decoded response, and `RemovalSentinelSerializer.deserialize` throws if one is ever asked for.
- **Serialization-level tests are now the standard for payload-shape claims.** Model-level
  assertions demonstrably cannot see this class of bug;
  `ExplicitNullPayloadSerializationTest` asserts on the encoded string for every affected path.
