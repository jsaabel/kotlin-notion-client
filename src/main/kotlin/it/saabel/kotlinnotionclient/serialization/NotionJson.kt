package it.saabel.kotlinnotionclient.serialization

import kotlinx.serialization.json.Json

/**
 * The JSON configuration every request the client sends is encoded with.
 *
 * Kept in one place so tests can assert on the *exact* bytes a request produces rather than on a
 * hand-copied approximation of this config. The settings are load-bearing:
 *
 * - `ignoreUnknownKeys` — Notion adds response fields without notice; an unmodelled one must not
 *   fail a read.
 * - `encodeDefaults` — request models carry meaningful defaults (`in_trash = true`) that have to
 *   reach the wire.
 * - `explicitNulls = false` — a PATCH should carry only the fields it changes, so an unset
 *   nullable field is omitted rather than sent as `null`. Where `null` *is* the payload — icon and
 *   cover removal, clearing a property value — the intent is carried by a sentinel and a serializer
 *   that emits JSON `null` explicitly, never by a Kotlin `null` field. See
 *   `docs/adr/0002-explicit-null-payloads.md`.
 */
internal object NotionJson {
    /** Builds the client's JSON configuration. @param prettyPrint indent the encoded output */
    fun forClient(prettyPrint: Boolean = false): Json =
        Json {
            ignoreUnknownKeys = true
            this.prettyPrint = prettyPrint
            encodeDefaults = true
            explicitNulls = false
        }

    /** The client's JSON configuration with pretty-printing off — what requests are encoded with. */
    val default: Json = forClient(prettyPrint = false)
}
