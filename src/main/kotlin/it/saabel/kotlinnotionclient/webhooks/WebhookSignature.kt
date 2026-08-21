@file:Suppress("unused")

package it.saabel.kotlinnotionclient.webhooks

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Verifies Notion webhook request signatures.
 *
 * Every webhook request Notion sends carries an `X-Notion-Signature` header of the form
 * `sha256=<hex digest>`, where the digest is an HMAC-SHA256 of the **raw request body**
 * keyed by the subscription's `verification_token` (delivered once, in the initial
 * subscription-verification request).
 *
 * ## ⚠️ You MUST pass the raw request body
 *
 * The HMAC is computed by Notion over the exact bytes it sent. If you deserialize the
 * body into a model and re-serialize it — different key order, whitespace, escaping, or
 * field defaults — the digest will not match and verification will fail (or worse, you
 * might be tempted to weaken the check). Capture the body **before** any JSON parsing:
 *
 * ```kotlin
 * // Ktor server example
 * val rawBody: ByteArray = call.receive<ByteArray>()
 * val signature = call.request.headers["X-Notion-Signature"]
 * if (!WebhookSignature.verify(rawBody, signature, verificationToken)) {
 *     call.respond(HttpStatusCode.Unauthorized)
 *     return@post
 * }
 * val event = WebhookEvent.fromJson(rawBody.decodeToString())
 * ```
 *
 * There is deliberately no overload accepting a deserialized model.
 *
 * ## Security properties
 *
 * - Comparison uses [MessageDigest.isEqual], which is constant-time for equal-length
 *   inputs — never `==`/[String.equals], which would be a timing oracle.
 * - Verification **fails closed**: a missing/blank header, missing `sha256=` prefix,
 *   wrong digest length, non-lowercase-hex characters, or a blank token all return
 *   `false`. The function never throws on malformed attacker-controlled input.
 *
 * Spec: [Notion webhooks reference](https://developers.notion.com/reference/webhooks).
 */
object WebhookSignature {
    /** Name of the HTTP header carrying the signature. */
    const val SIGNATURE_HEADER: String = "X-Notion-Signature"

    /** Prefix of the signature header value. */
    const val SIGNATURE_PREFIX: String = "sha256="

    private const val HMAC_ALGORITHM = "HmacSHA256"

    /** Length of a hex-encoded SHA-256 digest. */
    private const val HEX_DIGEST_LENGTH = 64

    /**
     * Verifies that [signatureHeader] is a valid Notion signature for [rawBody].
     *
     * @param rawBody the request body **exactly as received on the wire** — see the
     *   class-level warning about never re-serializing a parsed model
     * @param signatureHeader the value of the `X-Notion-Signature` header, e.g.
     *   `sha256=461e8cbc…`; `null` (header absent) fails verification
     * @param verificationToken the `verification_token` for this webhook subscription
     * @return `true` only if the signature is present, well-formed, and matches;
     *   `false` in every other case (fail closed) — this method never throws on
     *   malformed input
     */
    fun verify(
        rawBody: ByteArray,
        signatureHeader: String?,
        verificationToken: String,
    ): Boolean {
        if (signatureHeader.isNullOrBlank() || verificationToken.isEmpty()) return false
        if (!signatureHeader.startsWith(SIGNATURE_PREFIX)) return false

        val providedHex = signatureHeader.substring(SIGNATURE_PREFIX.length)
        if (providedHex.length != HEX_DIGEST_LENGTH || !isLowercaseHex(providedHex)) return false

        val expected = hmacSha256(verificationToken, rawBody)
        val provided = decodeHex(providedHex) ?: return false

        // Constant-time comparison of the two digests. MessageDigest.isEqual is
        // time-constant for equal-length inputs; lengths are guaranteed equal here.
        return MessageDigest.isEqual(expected, provided)
    }

    /**
     * Convenience overload for a body captured as text. The string is encoded as UTF-8,
     * which matches the bytes Notion signs **only if** the string is the unmodified
     * request body (e.g. Ktor's `call.receiveText()`); see the class-level warning.
     */
    fun verify(
        rawBody: String,
        signatureHeader: String?,
        verificationToken: String,
    ): Boolean = verify(rawBody.toByteArray(Charsets.UTF_8), signatureHeader, verificationToken)

    /**
     * Computes the signature header value Notion would send for [rawBody], i.e.
     * `sha256=<lowercase hex HMAC-SHA256>`. Useful for building test fixtures and
     * local webhook simulators. Do **not** use its result with `==` to verify incoming
     * requests — use [verify], which compares in constant time.
     *
     * @throws IllegalArgumentException if [verificationToken] is empty
     */
    fun computeSignature(
        rawBody: ByteArray,
        verificationToken: String,
    ): String {
        require(verificationToken.isNotEmpty()) { "verificationToken must not be empty" }
        return SIGNATURE_PREFIX + toHex(hmacSha256(verificationToken, rawBody))
    }

    /** See [computeSignature]; encodes [rawBody] as UTF-8. */
    fun computeSignature(
        rawBody: String,
        verificationToken: String,
    ): String = computeSignature(rawBody.toByteArray(Charsets.UTF_8), verificationToken)

    private fun hmacSha256(
        key: String,
        data: ByteArray,
    ): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
        return mac.doFinal(data)
    }

    private fun isLowercaseHex(value: String): Boolean = value.all { it in '0'..'9' || it in 'a'..'f' }

    private fun decodeHex(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        val bytes = ByteArray(hex.length / 2)
        for (i in bytes.indices) {
            val high = Character.digit(hex[2 * i], 16)
            val low = Character.digit(hex[2 * i + 1], 16)
            if (high < 0 || low < 0) return null
            bytes[i] = ((high shl 4) or low).toByte()
        }
        return bytes
    }

    private fun toHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(Character.forDigit((b.toInt() shr 4) and 0xF, 16))
            sb.append(Character.forDigit(b.toInt() and 0xF, 16))
        }
        return sb.toString()
    }
}

/**
 * Top-level convenience matching the naming used in Notion's own SDKs
 * (`verifyWebhookSignature`). Delegates to [WebhookSignature.verify] — see its
 * documentation, especially the warning that [rawBody] must be the raw request
 * body exactly as received, never a re-serialized model.
 */
fun verifyWebhookSignature(
    rawBody: ByteArray,
    signatureHeader: String?,
    verificationToken: String,
): Boolean = WebhookSignature.verify(rawBody, signatureHeader, verificationToken)

/** See [WebhookSignature.verify]; encodes [rawBody] as UTF-8. */
fun verifyWebhookSignature(
    rawBody: String,
    signatureHeader: String?,
    verificationToken: String,
): Boolean = WebhookSignature.verify(rawBody, signatureHeader, verificationToken)
