package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.VerificationRequest
import it.saabel.kotlinnotionclient.models.pages.updatePageRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Tags("Unit")
class VerificationPropertyTest :
    FunSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            }

        // ──────────────────────────────────────────────
        // Deserialization
        // ──────────────────────────────────────────────

        context("Deserialization — unverified") {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": { "type": "workspace", "workspace": true },
                  "properties": {
                    "Verification": {
                      "id": "fpVq",
                      "type": "verification",
                      "verification": {
                        "state": "unverified",
                        "verified_by": null,
                        "date": null
                      }
                    }
                  },
                  "url": "https://www.notion.so/page-id"
                }
                """.trimIndent()

            test("deserializes as PageProperty.Verification") {
                val page = json.decodeFromString<Page>(pageJson)
                val prop = page.properties["Verification"]
                prop as PageProperty.Verification
                prop.id shouldBe "fpVq"
                prop.type shouldBe "verification"
                val v = prop.verification.shouldNotBeNull()
                v.state shouldBe "unverified"
                v.verifiedBy.shouldBeNull()
                v.date.shouldBeNull()
            }
        }

        context("Deserialization — verified with no expiration") {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": { "type": "workspace", "workspace": true },
                  "properties": {
                    "Verification": {
                      "id": "fpVq",
                      "type": "verification",
                      "verification": {
                        "state": "verified",
                        "verified_by": {
                          "object": "user",
                          "id": "01e46064-d5fb-4444-8ecc-ad47d076f804",
                          "name": "User Name",
                          "avatar_url": null,
                          "type": "person",
                          "person": {}
                        },
                        "date": {
                          "start": "2023-08-01T04:00:00.000Z",
                          "end": null,
                          "time_zone": null
                        }
                      }
                    }
                  },
                  "url": "https://www.notion.so/page-id"
                }
                """.trimIndent()

            test("deserializes state as 'verified'") {
                val page = json.decodeFromString<Page>(pageJson)
                val prop = page.properties["Verification"] as PageProperty.Verification
                prop.verification.shouldNotBeNull().state shouldBe "verified"
            }

            test("deserializes verified_by user") {
                val page = json.decodeFromString<Page>(pageJson)
                val prop = page.properties["Verification"] as PageProperty.Verification
                val verifiedBy =
                    prop.verification
                        .shouldNotBeNull()
                        .verifiedBy
                        .shouldNotBeNull()
                verifiedBy.id shouldBe "01e46064-d5fb-4444-8ecc-ad47d076f804"
            }

            test("deserializes date with start and null end") {
                val page = json.decodeFromString<Page>(pageJson)
                val prop = page.properties["Verification"] as PageProperty.Verification
                val date = prop.verification.shouldNotBeNull().date
                date.shouldNotBeNull()
                date.start shouldBe "2023-08-01T04:00:00.000Z"
                date.end.shouldBeNull()
            }
        }

        context("Deserialization — verified with 90-day expiration") {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": { "type": "workspace", "workspace": true },
                  "properties": {
                    "Verification": {
                      "id": "fpVq",
                      "type": "verification",
                      "verification": {
                        "state": "verified",
                        "verified_by": { "object": "user", "id": "01e46064-d5fb-4444-8ecc-ad47d076f804" },
                        "date": {
                          "start": "2023-08-01T04:00:00.000Z",
                          "end": "2023-10-30T04:00:00.000Z",
                          "time_zone": null
                        }
                      }
                    }
                  },
                  "url": "https://www.notion.so/page-id"
                }
                """.trimIndent()

            test("deserializes date with both start and end") {
                val page = json.decodeFromString<Page>(pageJson)
                val prop = page.properties["Verification"] as PageProperty.Verification
                val date = prop.verification!!.date
                date.shouldNotBeNull()
                date.start shouldBe "2023-08-01T04:00:00.000Z"
                date.end shouldBe "2023-10-30T04:00:00.000Z"
            }
        }

        context("Deserialization — null verification object") {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": { "type": "workspace", "workspace": true },
                  "properties": {
                    "Verification": {
                      "id": "fpVq",
                      "type": "verification",
                      "verification": null
                    }
                  },
                  "url": "https://www.notion.so/page-id"
                }
                """.trimIndent()

            test("deserializes with null verification data") {
                val page = json.decodeFromString<Page>(pageJson)
                val prop = page.properties["Verification"] as PageProperty.Verification
                prop.verification.shouldBeNull()
            }
        }

        // ──────────────────────────────────────────────
        // Serialization (write model)
        // ──────────────────────────────────────────────

        context("Serialization — verify() DSL") {
            test("unverify() serializes state as 'unverified' with no date") {
                val request =
                    updatePageRequest {
                        properties { unverify("Verification") }
                    }
                val serialized = json.encodeToString(request)
                serialized shouldContain """"state":"unverified""""
            }

            test("verify() without dates serializes state as 'verified' with no date field") {
                val request =
                    updatePageRequest {
                        properties { verify("Verification") }
                    }
                val serialized = json.encodeToString(request)
                serialized shouldContain """"state":"verified""""
            }

            test("verify() with start date serializes date.start") {
                val request =
                    updatePageRequest {
                        properties { verify("Verification", start = "2026-03-25T00:00:00.000Z") }
                    }
                val serialized = json.encodeToString(request)
                serialized shouldContain """"start":"2026-03-25T00:00:00.000Z""""
            }

            test("verify() with start and end serializes both date fields") {
                val request =
                    updatePageRequest {
                        properties {
                            verify(
                                "Verification",
                                start = "2026-03-25T00:00:00.000Z",
                                end = "2026-06-25T00:00:00.000Z",
                            )
                        }
                    }
                val serialized = json.encodeToString(request)
                serialized shouldContain """"start":"2026-03-25T00:00:00.000Z""""
                serialized shouldContain """"end":"2026-06-25T00:00:00.000Z""""
            }

            test("VerificationValue is serialized under 'verification' key") {
                val value = PagePropertyValue.VerificationValue(VerificationRequest(state = "unverified"))
                val serialized = json.encodeToString<PagePropertyValue>(value)
                serialized shouldContain """"verification":"""
            }
        }
    })
