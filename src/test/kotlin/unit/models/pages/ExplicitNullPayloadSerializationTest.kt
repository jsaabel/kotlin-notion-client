package unit.models.pages

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.SelectOption
import it.saabel.kotlinnotionclient.models.pages.UpdatePageRequest
import it.saabel.kotlinnotionclient.models.pages.updatePageRequest
import it.saabel.kotlinnotionclient.serialization.NotionJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Guards the payloads where JSON `null` is the instruction, not the absence of one.
 *
 * These assert on the encoded string, using [NotionJson] — the same configuration the client
 * installs. Model-level assertions cannot see this class of bug: before the fix, `icon.remove()`
 * left `request.icon == null`, indistinguishable from an untouched icon, and the request went out
 * as `{}`. See `docs/adr/0002-explicit-null-payloads.md`.
 */
@Tags("Unit")
class ExplicitNullPayloadSerializationTest :
    FunSpec({
        val json: Json = NotionJson.default

        fun encode(request: UpdatePageRequest): String = json.encodeToString(UpdatePageRequest.serializer(), request)

        fun encodedProperty(value: PagePropertyValue): JsonObject =
            json
                .parseToJsonElement(encode(UpdatePageRequest(properties = mapOf("P" to value))))
                .jsonObject["properties"]!!
                .jsonObject["P"]!!
                .jsonObject

        context("icon and cover removal reach the wire as null") {
            test("icon.remove() encodes as an explicit null") {
                encode(updatePageRequest { icon.remove() }) shouldBe """{"icon":null}"""
            }

            test("cover.remove() encodes as an explicit null") {
                encode(updatePageRequest { cover.remove() }) shouldBe """{"cover":null}"""
            }

            test("removing both encodes both nulls") {
                encode(
                    updatePageRequest {
                        icon.remove()
                        cover.remove()
                    },
                ) shouldBe """{"icon":null,"cover":null}"""
            }

            test("an untouched icon or cover is still omitted entirely") {
                encode(updatePageRequest { lock() }) shouldBe """{"is_locked":true}"""
            }

            test("a set icon is unaffected by the sentinel") {
                encode(updatePageRequest { icon.emoji("✅") }) shouldBe """{"icon":{"type":"emoji","emoji":"✅"}}"""
            }

            test("a set cover is unaffected by the sentinel") {
                encode(updatePageRequest { cover.external("https://example.com/c.jpg") }) shouldBe
                    """{"cover":{"external":{"url":"https://example.com/c.jpg"},"type":"external"}}"""
            }

            test("Icon.Removed encodes to null on its own") {
                json.encodeToString(Icon.serializer(), Icon.Removed) shouldBe "null"
            }

            test("PageCover.Removed encodes to null on its own") {
                json.encodeToString(PageCover.serializer(), PageCover.Removed) shouldBe "null"
            }
        }

        context("clearing a property value keeps the payload key") {
            test("select(name, null) encodes select: null") {
                encodedProperty(PagePropertyValue.SelectValue(select = null)) shouldBe
                    JsonObject(mapOf("type" to json.parseToJsonElement("\"select\""), "select" to JsonNull))
            }

            // The remaining clearable setters, asserted through the DSL that documents "null for
            // empty" — one case per setter listed in issue #80.
            val cleared =
                mapOf(
                    "select" to updatePageRequest { properties { select("P", null as String?) } },
                    "status" to updatePageRequest { properties { status("P", null) } },
                    "date" to updatePageRequest { properties { date("P", null) } },
                    "number" to updatePageRequest { properties { number("P", null as Double?) } },
                    "url" to updatePageRequest { properties { url("P", null) } },
                    "email" to updatePageRequest { properties { email("P", null) } },
                    "phone_number" to updatePageRequest { properties { phoneNumber("P", null) } },
                )

            cleared.forEach { (key, request) ->
                test("clearing a $key property encodes $key: null") {
                    encode(request) shouldBe """{"properties":{"P":{"type":"$key","$key":null}}}"""
                }
            }

            test("dateTime(name, null) clears the same way date(name, null) does") {
                encode(updatePageRequest { properties { dateTime("P", null as String?) } }) shouldBe
                    """{"properties":{"P":{"type":"date","date":null}}}"""
            }

            test("a set value still encodes its payload") {
                encodedProperty(PagePropertyValue.SelectValue(select = SelectOption(name = "Done")))["select"]
                    ?.jsonObject
                    ?.get("name")
                    .toString() shouldBe "\"Done\""
            }

            test("a set number still encodes its payload") {
                encode(updatePageRequest { properties { number("P", 42) } }) shouldBe
                    """{"properties":{"P":{"type":"number","number":42.0}}}"""
            }

            test("list-valued properties clear with an empty array, not a null") {
                encode(updatePageRequest { properties { multiSelect("P") } }) shouldBe
                    """{"properties":{"P":{"type":"multi_select","multi_select":[]}}}"""
            }
        }

        context("clearable values still round-trip") {
            test("a cleared select decodes back to a null payload") {
                val encoded = """{"type":"select","select":null}"""
                json.decodeFromString(PagePropertyValue.serializer(), encoded) shouldBe
                    PagePropertyValue.SelectValue(select = null)
            }

            test("a set select decodes back to its option") {
                val encoded = """{"type":"select","select":{"name":"Done"}}"""
                val decoded = json.decodeFromString(PagePropertyValue.serializer(), encoded)
                decoded shouldBe PagePropertyValue.SelectValue(select = SelectOption(name = "Done"))
            }

            test("a set date decodes back to its payload") {
                val encoded = """{"type":"date","date":{"start":"2026-01-01"}}"""
                val decoded = json.decodeFromString(PagePropertyValue.serializer(), encoded)
                json.encodeToString(PagePropertyValue.serializer(), decoded) shouldBe encoded
            }

            test("a removed icon reads back as a plain null, never as the sentinel") {
                val decoded = json.decodeFromString(UpdatePageRequest.serializer(), """{"icon":null}""")
                decoded.icon shouldBe null
            }
        }
    })
