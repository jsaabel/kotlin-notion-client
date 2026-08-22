package it.saabel.kotlinnotionclient.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import it.saabel.kotlinnotionclient.config.NotionApiLimits
import it.saabel.kotlinnotionclient.utils.PropertyIds

/**
 * Fails fast if [propertyIds] exceeds Notion's documented cap for `filter_properties`.
 *
 * Deliberately called by each public API method **before** any HTTP call is attempted (outside
 * any `try`/`catch` that would otherwise fold an [IllegalArgumentException] into
 * [it.saabel.kotlinnotionclient.exceptions.NotionException.NetworkError]) — validation should
 * surface as-is, not get wrapped as if the request had actually failed on the wire.
 *
 * @throws IllegalArgumentException if more than [NotionApiLimits.Query.MAX_FILTER_PROPERTIES] IDs
 *   are supplied.
 */
internal fun validateFilterPropertiesLimit(propertyIds: List<String>?) {
    val count = propertyIds?.size ?: return
    require(count <= NotionApiLimits.Query.MAX_FILTER_PROPERTIES) {
        "filter_properties accepts at most ${NotionApiLimits.Query.MAX_FILTER_PROPERTIES} property IDs, got $count"
    }
}

/**
 * Appends the `filter_properties` query parameter once per property ID, shared by every endpoint
 * that supports it ([PagesApi.retrieve], [DataSourcesApi.query] and friends).
 *
 * Property IDs coming from [it.saabel.kotlinnotionclient.models.datasources.DataSource] schema
 * deserialization are already decoded — `DatabasePropertySerializer` normalises them — but
 * [PropertyIds.decode] is still applied defensively so an ID a caller copied from a raw API
 * response (still in its percent-encoded, pre-normalization shape) resolves to the same wire
 * value instead of being double-encoded by Ktor's own query encoding.
 *
 * Callers are expected to have already run [validateFilterPropertiesLimit] — this function does
 * not re-validate, since by the time a request is being built it's usually inside a `try` that
 * would misclassify a validation failure as a network error.
 */
internal fun HttpRequestBuilder.filterProperties(propertyIds: List<String>?) {
    propertyIds?.forEach { propertyId ->
        parameter("filter_properties", PropertyIds.decode(propertyId))
    }
}
