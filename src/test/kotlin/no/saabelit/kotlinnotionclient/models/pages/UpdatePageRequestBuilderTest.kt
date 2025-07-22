package no.saabelit.kotlinnotionclient.models.pages

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

@Tags("Unit")
class UpdatePageRequestBuilderTest :
    StringSpec({

        "should build basic update request with properties" {
            val request =
                updatePageRequest {
                    properties {
                        checkbox("Completed", true)
                        number("Score", 85.5)
                        title("Name", "Updated Title")
                    }
                }

            request.properties shouldNotBe null
            request.properties!!.size shouldBe 3
            request.icon shouldBe null
            request.cover shouldBe null
            request.archived shouldBe null
        }

        "should build update request with icon and cover" {
            val request =
                updatePageRequest {
                    icon.emoji("✅")
                    cover.external("https://example.com/cover.jpg")
                }

            request.icon shouldNotBe null
            request.icon!!.type shouldBe "emoji"
            request.icon!!.emoji shouldBe "✅"

            request.cover shouldNotBe null
            request.cover!!.type shouldBe "external"
            request.cover!!.external!!.url shouldBe "https://example.com/cover.jpg"

            request.properties shouldBe null
            request.archived shouldBe null
        }

        "should build update request with archive status" {
            val request =
                updatePageRequest {
                    archive()
                }

            request.archived shouldBe true
            request.properties shouldBe null
            request.icon shouldBe null
            request.cover shouldBe null
        }

        "should build update request with archive false" {
            val request =
                updatePageRequest {
                    archive(false)
                }

            request.archived shouldBe false
        }

        "should build comprehensive update request" {
            val request =
                updatePageRequest {
                    properties {
                        title("Task", "Updated Task Title")
                        richText("Description", "Updated description")
                        select("Priority", "High")
                        multiSelect("Tags", "urgent", "completed")
                        date("Due", "2024-12-31")
                        checkbox("Done", true)
                    }
                    icon.external("https://example.com/icon.png")
                    cover.file("https://example.com/cover.jpg", "2024-12-31T23:59:59.000Z")
                    archive()
                }

            request.properties shouldNotBe null
            request.properties!!.size shouldBe 6

            request.icon shouldNotBe null
            request.icon!!.type shouldBe "external"
            request.icon!!.external?.url shouldBe "https://example.com/icon.png"

            request.cover shouldNotBe null
            request.cover!!.type shouldBe "file"

            request.archived shouldBe true
        }

        "should build empty update request when no configuration provided" {
            val request = updatePageRequest { }

            request.properties shouldBe null
            request.icon shouldBe null
            request.cover shouldBe null
            request.archived shouldBe null
        }

        "should build update request with icon and cover removal" {
            val request =
                updatePageRequest {
                    icon.remove()
                    cover.remove()
                }

            request.icon shouldBe null
            request.cover shouldBe null
            request.properties shouldBe null
            request.archived shouldBe null
        }

        "should build update request with select options without colors" {
            val request =
                updatePageRequest {
                    properties {
                        select("Status", "Done")
                        multiSelect("Categories", "work", "urgent")
                    }
                }

            request.properties shouldNotBe null
            val selectValue = request.properties!!["Status"] as PagePropertyValue.SelectValue
            selectValue.select!!.name shouldBe "Done"
            selectValue.select!!.color shouldBe null // No color by default to avoid conflicts

            val multiSelectValue = request.properties!!["Categories"] as PagePropertyValue.MultiSelectValue
            multiSelectValue.multiSelect.size shouldBe 2
            multiSelectValue.multiSelect.forEach { option ->
                option.color shouldBe null // No colors by default to avoid conflicts
            }
        }
    })
