package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import kotlinx.serialization.json.Json

/**
 * Unit tests for the place page property type.
 *
 * Tests proper deserialization of place properties with full location data,
 * partial data, and validates the formattedLocation convenience property.
 */
@Tags("Unit")
class PagePropertyPlaceTest :
    StringSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
            }

        "Should deserialize place property with full location data" {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Location": {
                      "id": "%3FJG%7D",
                      "type": "place",
                      "place": {
                        "lat": 60.19116,
                        "lon": 11.10242,
                        "name": "Oslo Airport",
                        "address": "Oslo Airport, E16, 2060 Gardermoen, Norway",
                        "aws_place_id": "AQAAAFUAJOZ89r-mb1SYL7-SoMdRt07f78RSAwxxWdEftbKanfZs-NqGy40xt67lWhjfJzRfiogmMr75O8PZ3b4T0PKbYS3OTBLMB8cgTubHqwS7sTFnIVYYShVzNMhVJtBKJPu03EeEWbfslnPMluRM9eImLnrMM_bz",
                        "google_place_id": null
                      }
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"
            page.properties.size shouldBe 1

            // Verify place property
            val placeProperty = page.properties["Location"]
            placeProperty shouldNotBe null
            val place = placeProperty.shouldBeInstanceOf<PageProperty.Place>()
            place.type shouldBe "place"
            place.id shouldBe "%3FJG%7D"

            // Verify place value
            place.place shouldNotBe null
            val placeValue = place.place!!
            placeValue.lat shouldBe 60.19116
            placeValue.lon shouldBe 11.10242
            placeValue.name shouldBe "Oslo Airport"
            placeValue.address shouldBe "Oslo Airport, E16, 2060 Gardermoen, Norway"
            placeValue.awsPlaceId shouldBe
                "AQAAAFUAJOZ89r-mb1SYL7-SoMdRt07f78RSAwxxWdEftbKanfZs-NqGy40xt67lWhjfJzRfiogmMr75O8PZ3b4T0PKbYS3OTBLMB8cgTubHqwS7sTFnIVYYShVzNMhVJtBKJPu03EeEWbfslnPMluRM9eImLnrMM_bz"
            placeValue.googlePlaceId shouldBe null

            // Verify formatted location
            place.formattedLocation shouldBe "Oslo Airport (60.19116, 11.10242)"
        }

        "Should deserialize place property with coordinates only (no name)" {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Location": {
                      "id": "place-id",
                      "type": "place",
                      "place": {
                        "lat": 40.7128,
                        "lon": -74.0060,
                        "name": null,
                        "address": null,
                        "aws_place_id": null,
                        "google_place_id": null
                      }
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"
            page.properties.size shouldBe 1

            // Verify place property
            val placeProperty = page.properties["Location"]
            placeProperty shouldNotBe null
            val place = placeProperty.shouldBeInstanceOf<PageProperty.Place>()

            // Verify place value
            place.place shouldNotBe null
            val placeValue = place.place!!
            placeValue.lat shouldBe 40.7128
            placeValue.lon shouldBe -74.0060
            placeValue.name shouldBe null
            placeValue.address shouldBe null

            // Verify formatted location (coordinates only, no name)
            place.formattedLocation shouldBe "(40.7128, -74.006)"
        }

        "Should deserialize place property with name only (no coordinates)" {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Location": {
                      "id": "place-id",
                      "type": "place",
                      "place": {
                        "lat": null,
                        "lon": null,
                        "name": "Eiffel Tower",
                        "address": "Champ de Mars, Paris, France",
                        "aws_place_id": null,
                        "google_place_id": "ChIJLU7jZClu5kcR4PcOOO6p3I0"
                      }
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"

            // Verify place property
            val placeProperty = page.properties["Location"]
            placeProperty shouldNotBe null
            val place = placeProperty.shouldBeInstanceOf<PageProperty.Place>()

            // Verify place value
            place.place shouldNotBe null
            val placeValue = place.place!!
            placeValue.lat shouldBe null
            placeValue.lon shouldBe null
            placeValue.name shouldBe "Eiffel Tower"
            placeValue.address shouldBe "Champ de Mars, Paris, France"
            placeValue.googlePlaceId shouldBe "ChIJLU7jZClu5kcR4PcOOO6p3I0"

            // Verify formatted location (name only, no coordinates)
            place.formattedLocation shouldBe "Eiffel Tower"
        }

        "Should deserialize place alongside other property types" {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Title": {
                      "id": "title-id",
                      "type": "title",
                      "title": [
                        {
                          "type": "text",
                          "text": {
                            "content": "Trip to Oslo"
                          },
                          "plain_text": "Trip to Oslo",
                          "href": null,
                          "annotations": {
                            "bold": false,
                            "italic": false,
                            "strikethrough": false,
                            "underline": false,
                            "code": false,
                            "color": "default"
                          }
                        }
                      ]
                    },
                    "Destination": {
                      "id": "place-id",
                      "type": "place",
                      "place": {
                        "lat": 59.9139,
                        "lon": 10.7522,
                        "name": "Oslo",
                        "address": "Oslo, Norway",
                        "aws_place_id": null,
                        "google_place_id": null
                      }
                    },
                    "Visited": {
                      "id": "checkbox-id",
                      "type": "checkbox",
                      "checkbox": true
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"
            page.properties.size shouldBe 3

            // Verify all property types
            page.properties["Title"]!!.shouldBeInstanceOf<PageProperty.Title>()
            page.properties["Destination"]!!.shouldBeInstanceOf<PageProperty.Place>()
            page.properties["Visited"]!!.shouldBeInstanceOf<PageProperty.Checkbox>()

            // Verify place specific values
            val destination = page.properties["Destination"].shouldBeInstanceOf<PageProperty.Place>()
            destination.place shouldNotBe null
            val destPlace = destination.place!!
            destPlace.name shouldBe "Oslo"
            destPlace.lat shouldBe 59.9139
            destPlace.lon shouldBe 10.7522
            destination.formattedLocation shouldBe "Oslo (59.9139, 10.7522)"
        }

        "Should handle null place value" {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Location": {
                      "id": "place-id",
                      "type": "place",
                      "place": null
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"

            // Verify place property with null value
            val placeProperty = page.properties["Location"]
            placeProperty shouldNotBe null
            val place = placeProperty.shouldBeInstanceOf<PageProperty.Place>()
            place.place shouldBe null
            place.formattedLocation shouldBe null
        }

        "Should handle empty place value (all fields null)" {
            val pageJson =
                """
                {
                  "object": "page",
                  "id": "test-page-id",
                  "created_time": "2025-01-01T00:00:00.000Z",
                  "last_edited_time": "2025-01-01T00:00:00.000Z",
                  "archived": false,
                  "in_trash": false,
                  "parent": {
                    "type": "workspace",
                    "workspace": true
                  },
                  "properties": {
                    "Location": {
                      "id": "place-id",
                      "type": "place",
                      "place": {
                        "lat": null,
                        "lon": null,
                        "name": null,
                        "address": null,
                        "aws_place_id": null,
                        "google_place_id": null
                      }
                    }
                  },
                  "url": "https://www.notion.so/test-page-id"
                }
                """.trimIndent()

            val page = json.decodeFromString<Page>(pageJson)

            // Verify page deserialized successfully
            page.id shouldBe "test-page-id"

            // Verify place property
            val placeProperty = page.properties["Location"]
            placeProperty shouldNotBe null
            val place = placeProperty.shouldBeInstanceOf<PageProperty.Place>()
            place.place shouldNotBe null
            val placeValue = place.place!!
            placeValue.lat shouldBe null
            placeValue.lon shouldBe null
            placeValue.name shouldBe null
            placeValue.address shouldBe null
            place.formattedLocation shouldBe null
        }
    })
