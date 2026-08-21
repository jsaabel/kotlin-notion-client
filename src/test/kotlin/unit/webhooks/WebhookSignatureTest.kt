package unit.webhooks

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.webhooks.WebhookSignature
import it.saabel.kotlinnotionclient.webhooks.verifyWebhookSignature

/**
 * Signature verification tests.
 *
 * The positive cases are *known-answer* vectors computed with independent
 * implementations, not round-trips of our own code:
 *
 * - The Notion docs vector: the worked example from
 *   https://developers.notion.com/reference/webhooks (header value
 *   `sha256=461e8cbc...`), independently confirmed with both
 *   `openssl dgst -sha256 -hmac` and Python's `hmac`/`hashlib` over the minified body.
 * - RFC 4231 test case 2 (key `"Jefe"`), the official HMAC-SHA256 test vector with a
 *   printable-string key.
 */
@Tags("Unit")
class WebhookSignatureTest :
    FunSpec({

        // Vector straight from the Notion webhooks reference, cross-checked with
        // OpenSSL and Python (see class KDoc).
        val notionToken = "secret_tMrlL1qK5vuQAh1b6cZGhFChZTSYJlce98V0pYn7yBl"
        val notionBody = """{"verification_token":"secret_tMrlL1qK5vuQAh1b6cZGhFChZTSYJlce98V0pYn7yBl"}"""
        val notionSignature = "sha256=461e8cbcba8a75c3edd866f0e71280f5a85cbf21eff040ebd10fe266df38a735"

        // RFC 4231, HMAC-SHA256, test case 2.
        val rfcKey = "Jefe"
        val rfcData = "what do ya want for nothing?"
        val rfcSignature = "sha256=5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843"

        context("known-answer vectors") {
            test("verifies the vector from the Notion webhooks reference") {
                WebhookSignature.verify(notionBody, notionSignature, notionToken) shouldBe true
            }

            test("verifies the Notion vector from raw bytes") {
                WebhookSignature.verify(notionBody.toByteArray(Charsets.UTF_8), notionSignature, notionToken) shouldBe true
            }

            test("verifies RFC 4231 HMAC-SHA256 test case 2") {
                WebhookSignature.verify(rfcData, rfcSignature, rfcKey) shouldBe true
            }

            test("computeSignature reproduces the documented header value") {
                WebhookSignature.computeSignature(notionBody, notionToken) shouldBe notionSignature
            }

            test("top-level verifyWebhookSignature delegates correctly") {
                verifyWebhookSignature(notionBody, notionSignature, notionToken) shouldBe true
                verifyWebhookSignature(notionBody.toByteArray(Charsets.UTF_8), notionSignature, notionToken) shouldBe true
                verifyWebhookSignature(notionBody, rfcSignature, notionToken) shouldBe false
            }
        }

        context("rejects tampering") {
            test("tampered body fails") {
                val tampered = notionBody.replaceFirst("verification_token", "verification_tokem")
                WebhookSignature.verify(tampered, notionSignature, notionToken) shouldBe false
            }

            test("single flipped byte in body fails") {
                val bytes = notionBody.toByteArray(Charsets.UTF_8)
                bytes[bytes.size - 2] = (bytes[bytes.size - 2].toInt() xor 0x01).toByte()
                WebhookSignature.verify(bytes, notionSignature, notionToken) shouldBe false
            }

            test("wrong verification token fails") {
                WebhookSignature.verify(notionBody, notionSignature, "secret_wrongTokenEntirely") shouldBe false
            }

            test("signature for a different body fails") {
                WebhookSignature.verify(notionBody, rfcSignature, notionToken) shouldBe false
            }

            test("re-serialized (pretty-printed) body fails even though it is semantically equal") {
                // The classic trap: parse-then-reserialize changes bytes, breaking the HMAC.
                val reserialized =
                    """
                    {
                      "verification_token": "secret_tMrlL1qK5vuQAh1b6cZGhFChZTSYJlce98V0pYn7yBl"
                    }
                    """.trimIndent()
                WebhookSignature.verify(reserialized, notionSignature, notionToken) shouldBe false
            }
        }

        context("fails closed on malformed input") {
            test("null header fails") {
                WebhookSignature.verify(notionBody, null, notionToken) shouldBe false
            }

            test("empty header fails") {
                WebhookSignature.verify(notionBody, "", notionToken) shouldBe false
            }

            test("blank header fails") {
                WebhookSignature.verify(notionBody, "   ", notionToken) shouldBe false
            }

            test("missing sha256= prefix fails even with a correct digest") {
                val bareDigest = notionSignature.removePrefix("sha256=")
                WebhookSignature.verify(notionBody, bareDigest, notionToken) shouldBe false
            }

            test("wrong prefix fails") {
                WebhookSignature.verify(notionBody, "sha512=" + notionSignature.removePrefix("sha256="), notionToken) shouldBe false
            }

            test("truncated digest fails") {
                WebhookSignature.verify(notionBody, notionSignature.dropLast(2), notionToken) shouldBe false
            }

            test("over-long digest fails") {
                WebhookSignature.verify(notionBody, notionSignature + "ab", notionToken) shouldBe false
            }

            test("non-hex characters fail") {
                val corrupted = notionSignature.dropLast(2) + "zz"
                WebhookSignature.verify(notionBody, corrupted, notionToken) shouldBe false
            }

            test("uppercase hex fails (Notion sends lowercase; comparison is strict)") {
                WebhookSignature.verify(notionBody, "sha256=" + notionSignature.removePrefix("sha256=").uppercase(), notionToken) shouldBe
                    false
            }

            test("prefix only fails") {
                WebhookSignature.verify(notionBody, "sha256=", notionToken) shouldBe false
            }

            test("empty token fails") {
                WebhookSignature.verify(notionBody, notionSignature, "") shouldBe false
            }

            test("empty body with valid-looking signature fails") {
                WebhookSignature.verify(ByteArray(0), notionSignature, notionToken) shouldBe false
            }

            test("never returns true for garbage header") {
                WebhookSignature.verify(notionBody, "definitely-not-a-signature", notionToken) shouldBe false
            }
        }

        context("computeSignature") {
            test("matches RFC 4231 test case 2") {
                WebhookSignature.computeSignature(rfcData, rfcKey) shouldBe rfcSignature
            }

            test("rejects an empty token") {
                runCatching { WebhookSignature.computeSignature(notionBody, "") }.isFailure shouldBe true
            }
        }
    })
