package it.saabel.kotlinnotionclient.utils

import io.ktor.http.URLDecodeException
import io.ktor.http.decodeURLQueryComponent

/**
 * Helpers for Notion property IDs.
 *
 * Notion exposes property IDs in two shapes depending on where they come from: percent-encoded
 * in a data source's schema (e.g. `%7DVpb`, `ue%5Cl`) and decoded everywhere else — page property
 * responses, view filter/sort payloads (e.g. `}Vpb`, `ue\l`). [DataSource][it.saabel.kotlinnotionclient.models.datasources.DataSource]
 * deserialization normalises schema IDs to the decoded form (see `DatabasePropertySerializer`),
 * so callers see one consistent shape everywhere in the client. This decoder remains the shared
 * fallback for the rare case a caller passes an ID in its still-encoded, pre-normalization shape.
 */
object PropertyIds {
    /**
     * Percent-decodes a property ID, returning it unchanged if it isn't validly encoded.
     */
    fun decode(propertyId: String): String =
        try {
            propertyId.decodeURLQueryComponent()
        } catch (_: URLDecodeException) {
            propertyId
        }
}
