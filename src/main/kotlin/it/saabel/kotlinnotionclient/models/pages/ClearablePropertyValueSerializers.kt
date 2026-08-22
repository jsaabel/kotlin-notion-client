package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.serialization.ClearableValueSerializer
import kotlinx.serialization.builtins.serializer

// Serializers for the page property values that can be *cleared*.
//
// Each of these carries a single nullable payload where `null` means "empty this property", and
// Notion only hears that instruction if the key survives: `{"select": null}`, not `{}`. Under the
// client's `explicitNulls = false` the generated serializers drop the key and the update silently
// does nothing, so each writes its key unconditionally instead. See [ClearableValueSerializer] and
// `docs/adr/0002-explicit-null-payloads.md`.
//
// List-valued properties (`multi_select`, `people`, `relation`, `files`) are not here: they clear
// with `[]`, which is not a null and encodes fine.

/** See [ClearableValueSerializer]. */
internal object NumberValueSerializer : ClearableValueSerializer<PagePropertyValue.NumberValue, Double>(
    serialName = "number",
    payloadKey = "number",
    payloadSerializer = Double.serializer(),
) {
    override fun payloadOf(value: PagePropertyValue.NumberValue): Double? = value.number

    override fun withPayload(payload: Double?): PagePropertyValue.NumberValue = PagePropertyValue.NumberValue(number = payload)
}

/** See [ClearableValueSerializer]. */
internal object UrlValueSerializer : ClearableValueSerializer<PagePropertyValue.UrlValue, String>(
    serialName = "url",
    payloadKey = "url",
    payloadSerializer = String.serializer(),
) {
    override fun payloadOf(value: PagePropertyValue.UrlValue): String? = value.url

    override fun withPayload(payload: String?): PagePropertyValue.UrlValue = PagePropertyValue.UrlValue(url = payload)
}

/** See [ClearableValueSerializer]. */
internal object EmailValueSerializer : ClearableValueSerializer<PagePropertyValue.EmailValue, String>(
    serialName = "email",
    payloadKey = "email",
    payloadSerializer = String.serializer(),
) {
    override fun payloadOf(value: PagePropertyValue.EmailValue): String? = value.email

    override fun withPayload(payload: String?): PagePropertyValue.EmailValue = PagePropertyValue.EmailValue(email = payload)
}

/** See [ClearableValueSerializer]. */
internal object PhoneNumberValueSerializer : ClearableValueSerializer<PagePropertyValue.PhoneNumberValue, String>(
    serialName = "phone_number",
    payloadKey = "phone_number",
    payloadSerializer = String.serializer(),
) {
    override fun payloadOf(value: PagePropertyValue.PhoneNumberValue): String? = value.phoneNumber

    override fun withPayload(payload: String?): PagePropertyValue.PhoneNumberValue =
        PagePropertyValue.PhoneNumberValue(phoneNumber = payload)
}

/** See [ClearableValueSerializer]. */
internal object SelectValueSerializer : ClearableValueSerializer<PagePropertyValue.SelectValue, SelectOption>(
    serialName = "select",
    payloadKey = "select",
    payloadSerializer = SelectOption.serializer(),
) {
    override fun payloadOf(value: PagePropertyValue.SelectValue): SelectOption? = value.select

    override fun withPayload(payload: SelectOption?): PagePropertyValue.SelectValue = PagePropertyValue.SelectValue(select = payload)
}

/** See [ClearableValueSerializer]. */
internal object StatusValueSerializer : ClearableValueSerializer<PagePropertyValue.StatusValue, StatusOption>(
    serialName = "status",
    payloadKey = "status",
    payloadSerializer = StatusOption.serializer(),
) {
    override fun payloadOf(value: PagePropertyValue.StatusValue): StatusOption? = value.status

    override fun withPayload(payload: StatusOption?): PagePropertyValue.StatusValue = PagePropertyValue.StatusValue(status = payload)
}

/** See [ClearableValueSerializer]. */
internal object DateValueSerializer : ClearableValueSerializer<PagePropertyValue.DateValue, DateData>(
    serialName = "date",
    payloadKey = "date",
    payloadSerializer = DateData.serializer(),
) {
    override fun payloadOf(value: PagePropertyValue.DateValue): DateData? = value.date

    override fun withPayload(payload: DateData?): PagePropertyValue.DateValue = PagePropertyValue.DateValue(date = payload)
}
