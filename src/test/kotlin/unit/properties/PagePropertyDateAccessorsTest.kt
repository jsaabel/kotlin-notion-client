package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.pages.DateData
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.endInstantValue
import it.saabel.kotlinnotionclient.models.pages.endLocalDateTimeNaive
import it.saabel.kotlinnotionclient.models.pages.endLocalDateValue
import it.saabel.kotlinnotionclient.models.pages.endStringValue
import it.saabel.kotlinnotionclient.models.pages.instantValue
import it.saabel.kotlinnotionclient.models.pages.localDateTimeNaive
import it.saabel.kotlinnotionclient.models.pages.localDateValue
import it.saabel.kotlinnotionclient.models.pages.stringValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Instant

/**
 * Tests for PageProperty.Date extension properties.
 *
 * Validates the convenience accessors for reading dates using kotlinx-datetime types.
 */
@Tags("Unit")
class PagePropertyDateAccessorsTest :
    StringSpec({

        "localDateValue should parse valid date string" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15",
                            end = null,
                            timeZone = null,
                        ),
                )

            property.localDateValue shouldBe LocalDate(2025, 3, 15)
        }

        "localDateValue should return null for invalid date string" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "invalid-date",
                            end = null,
                            timeZone = null,
                        ),
                )

            property.localDateValue shouldBe null
        }

        "localDateValue should return null when date is null" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date = null,
                )

            property.localDateValue shouldBe null
        }

        "localDateTimeNaive should parse valid datetime string" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15T14:30:00",
                            end = null,
                            timeZone = null,
                        ),
                )

            property.localDateTimeNaive shouldBe LocalDateTime(2025, 3, 15, 14, 30, 0)
        }

        "localDateTimeNaive should return null for invalid datetime string" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15",
                            end = null,
                            timeZone = null,
                        ),
                )

            // Date without time can't be parsed as LocalDateTime
            property.localDateTimeNaive shouldBe null
        }

        "instantValue should parse valid instant string" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15T14:30:00Z",
                            end = null,
                            timeZone = null,
                        ),
                )

            property.instantValue shouldBe Instant.parse("2025-03-15T14:30:00Z")
        }

        "instantValue should return null for invalid instant string" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15",
                            end = null,
                            timeZone = null,
                        ),
                )

            // Date without time can't be parsed as Instant
            property.instantValue shouldBe null
        }

        "endLocalDateValue should parse valid end date" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15",
                            end = "2025-03-22",
                            timeZone = null,
                        ),
                )

            property.endLocalDateValue shouldBe LocalDate(2025, 3, 22)
        }

        "endLocalDateValue should return null when end is null" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15",
                            end = null,
                            timeZone = null,
                        ),
                )

            property.endLocalDateValue shouldBe null
        }

        "endLocalDateTimeNaive should parse valid end datetime" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15T14:30:00",
                            end = "2025-03-15T16:00:00",
                            timeZone = null,
                        ),
                )

            property.endLocalDateTimeNaive shouldBe LocalDateTime(2025, 3, 15, 16, 0, 0)
        }

        "endInstantValue should parse valid end instant" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15T00:00:00Z",
                            end = "2025-03-15T04:00:00Z",
                            timeZone = null,
                        ),
                )

            property.endInstantValue shouldBe Instant.parse("2025-03-15T04:00:00Z")
        }

        "stringValue should return start date string" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15",
                            end = null,
                            timeZone = null,
                        ),
                )

            property.stringValue shouldBe "2025-03-15"
        }

        "endStringValue should return end date string" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date =
                        DateData(
                            start = "2025-03-15",
                            end = "2025-03-22",
                            timeZone = null,
                        ),
                )

            property.endStringValue shouldBe "2025-03-22"
        }

        "All accessors should handle null date gracefully" {
            val property =
                PageProperty.Date(
                    id = "id",
                    type = "date",
                    date = null,
                )

            property.localDateValue shouldBe null
            property.localDateTimeNaive shouldBe null
            property.instantValue shouldBe null
            property.endLocalDateValue shouldBe null
            property.endLocalDateTimeNaive shouldBe null
            property.endInstantValue shouldBe null
            property.stringValue shouldBe null
            property.endStringValue shouldBe null
        }
    })
