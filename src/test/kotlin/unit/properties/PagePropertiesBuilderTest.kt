package unit.properties

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.pageProperties

/**
 * Tests for the PagePropertiesBuilder DSL functionality.
 *
 * Validates the builder pattern for creating page properties with clean, type-safe syntax.
 */
@Tags("Unit")
class PagePropertiesBuilderTest :
    StringSpec({

        "Should build empty properties map" {
            val properties = pageProperties { }

            properties shouldHaveSize 0
        }

        "Should build title property from plain text" {
            val properties =
                pageProperties {
                    title("Name", "Test Task")
                }

            properties shouldContainKey "Name"
            properties["Name"].shouldBeInstanceOf<PagePropertyValue.TitleValue>()
            val titleValue = properties["Name"] as PagePropertyValue.TitleValue
            titleValue.title shouldHaveSize 1
            titleValue.title.first().plainText shouldBe "Test Task"
        }

        "Should build rich text property from plain text" {
            val properties =
                pageProperties {
                    richText("Description", "Task description")
                }

            properties shouldContainKey "Description"
            properties["Description"].shouldBeInstanceOf<PagePropertyValue.RichTextValue>()
            val richTextValue = properties["Description"] as PagePropertyValue.RichTextValue
            richTextValue.richText shouldHaveSize 1
            richTextValue.richText.first().plainText shouldBe "Task description"
        }

        "Should build number properties with different types" {
            val properties =
                pageProperties {
                    number("Score", 85.5)
                    number("Count", 42)
                    number("Rating", null)
                }

            properties shouldHaveSize 3

            val scoreValue = properties["Score"] as PagePropertyValue.NumberValue
            scoreValue.number shouldBe 85.5

            val countValue = properties["Count"] as PagePropertyValue.NumberValue
            countValue.number shouldBe 42.0

            val ratingValue = properties["Rating"] as PagePropertyValue.NumberValue
            ratingValue.number shouldBe null
        }

        "Should build checkbox property" {
            val properties =
                pageProperties {
                    checkbox("Completed", true)
                    checkbox("Active", false)
                }

            properties shouldHaveSize 2

            val completedValue = properties["Completed"] as PagePropertyValue.CheckboxValue
            completedValue.checkbox shouldBe true

            val activeValue = properties["Active"] as PagePropertyValue.CheckboxValue
            activeValue.checkbox shouldBe false
        }

        "Should build URL and email properties" {
            val properties =
                pageProperties {
                    url("Website", "https://example.com")
                    url("EmptyURL", null)
                    email("Contact", "user@example.com")
                    email("EmptyEmail", null)
                    phoneNumber("Phone", "+1-555-1234")
                }

            properties shouldHaveSize 5

            val websiteValue = properties["Website"] as PagePropertyValue.UrlValue
            websiteValue.url shouldBe "https://example.com"

            val emptyUrlValue = properties["EmptyURL"] as PagePropertyValue.UrlValue
            emptyUrlValue.url shouldBe null

            val contactValue = properties["Contact"] as PagePropertyValue.EmailValue
            contactValue.email shouldBe "user@example.com"

            val phoneValue = properties["Phone"] as PagePropertyValue.PhoneNumberValue
            phoneValue.phoneNumber shouldBe "+1-555-1234"
        }

        "Should build select properties by name" {
            val properties =
                pageProperties {
                    select("Priority", "High")
                    select("Status", null)
                }

            properties shouldHaveSize 2

            val priorityValue = properties["Priority"] as PagePropertyValue.SelectValue
            priorityValue.select shouldNotBe null
            priorityValue.select!!.name shouldBe "High"

            val statusValue = properties["Status"] as PagePropertyValue.SelectValue
            statusValue.select shouldBe null
        }

        "Should build multi-select properties by names" {
            val properties =
                pageProperties {
                    multiSelect("Tags", "urgent", "work", "important")
                    multiSelectFromList("Categories", listOf("project", "personal"))
                }

            properties shouldHaveSize 2

            val tagsValue = properties["Tags"] as PagePropertyValue.MultiSelectValue
            tagsValue.multiSelect shouldHaveSize 3
            tagsValue.multiSelect.map { it.name } shouldBe listOf("urgent", "work", "important")

            val categoriesValue = properties["Categories"] as PagePropertyValue.MultiSelectValue
            categoriesValue.multiSelect shouldHaveSize 2
            categoriesValue.multiSelect.map { it.name } shouldBe listOf("project", "personal")
        }

        "Should build date properties with various formats" {
            val properties =
                pageProperties {
                    date("StartDate", "2024-01-15")
                    date("EmptyDate", null)
                    dateRange("Duration", "2024-01-15", "2024-01-20")
                    dateTime("CreatedAt", "2024-01-15T14:30:00")
                    dateTimeRange("Meeting", "2024-01-15T14:30:00", "2024-01-15T15:30:00")
                    dateWithTimeZone("Launch", "2024-01-15", "America/Los_Angeles")
                    dateTimeWithTimeZone("Webinar", "2024-01-15T14:30:00", "UTC")
                }

            properties shouldHaveSize 7

            val startDateValue = properties["StartDate"] as PagePropertyValue.DateValue
            startDateValue.date!!.start shouldBe "2024-01-15"
            startDateValue.date.end shouldBe null

            val emptyDateValue = properties["EmptyDate"] as PagePropertyValue.DateValue
            emptyDateValue.date shouldBe null

            val durationValue = properties["Duration"] as PagePropertyValue.DateValue
            durationValue.date!!.start shouldBe "2024-01-15"
            durationValue.date.end shouldBe "2024-01-20"

            val createdAtValue = properties["CreatedAt"] as PagePropertyValue.DateValue
            createdAtValue.date!!.start shouldBe "2024-01-15T14:30:00"

            val launchValue = properties["Launch"] as PagePropertyValue.DateValue
            launchValue.date!!.timeZone shouldBe "America/Los_Angeles"

            val webinarValue = properties["Webinar"] as PagePropertyValue.DateValue
            webinarValue.date!!.timeZone shouldBe "UTC"
        }

        "Should build people and relation properties" {
            val properties =
                pageProperties {
                    people("Assignees", "user-123", "user-456")
                    relation("RelatedPages", "page-abc", "page-def")
                }

            properties shouldHaveSize 2

            val assigneesValue = properties["Assignees"] as PagePropertyValue.PeopleValue
            assigneesValue.people shouldHaveSize 2
            assigneesValue.people.map { it.id } shouldBe listOf("user-123", "user-456")

            val relatedValue = properties["RelatedPages"] as PagePropertyValue.RelationValue
            relatedValue.relation shouldHaveSize 2
            relatedValue.relation.map { it.id } shouldBe listOf("page-abc", "page-def")
        }

        "Should build comprehensive property set demonstrating all types" {
            val properties =
                pageProperties {
                    title("Title", "Comprehensive Test Page")
                    richText("Description", "This page tests all property types")
                    number("Priority", 1)
                    number("Score", 95.5)
                    checkbox("Active", true)
                    checkbox("Completed", false)
                    url("Repository", "https://github.com/example/repo")
                    email("Contact", "maintainer@example.com")
                    phoneNumber("Phone", "+1-555-0123")
                    select("Status", "In Progress")
                    multiSelect("Tags", "test", "comprehensive", "unit/validation")
                    date("StartDate", "2024-01-01")
                    dateTime("CreatedAt", "2024-01-01T09:00:00")
                    dateRange("Sprint", "2024-01-01", "2024-01-14")
                    people("Assignees", "user-1", "user-2")
                    relation("Dependencies", "page-1", "page-2", "page-3")
                }

            // Verify we created all expected properties
            properties shouldHaveSize 16

            // Verify we have the right types
            properties["Title"].shouldBeInstanceOf<PagePropertyValue.TitleValue>()
            properties["Description"].shouldBeInstanceOf<PagePropertyValue.RichTextValue>()
            properties["Priority"].shouldBeInstanceOf<PagePropertyValue.NumberValue>()
            properties["Active"].shouldBeInstanceOf<PagePropertyValue.CheckboxValue>()
            properties["Repository"].shouldBeInstanceOf<PagePropertyValue.UrlValue>()
            properties["Contact"].shouldBeInstanceOf<PagePropertyValue.EmailValue>()
            properties["Phone"].shouldBeInstanceOf<PagePropertyValue.PhoneNumberValue>()
            properties["Status"].shouldBeInstanceOf<PagePropertyValue.SelectValue>()
            properties["Tags"].shouldBeInstanceOf<PagePropertyValue.MultiSelectValue>()
            properties["StartDate"].shouldBeInstanceOf<PagePropertyValue.DateValue>()
            properties["Assignees"].shouldBeInstanceOf<PagePropertyValue.PeopleValue>()
            properties["Dependencies"].shouldBeInstanceOf<PagePropertyValue.RelationValue>()
        }

        "Should test companion object factory methods directly" {
            val titleValue = PagePropertyValue.TitleValue.fromPlainText("Direct Title")
            titleValue.title shouldHaveSize 1
            titleValue.title.first().plainText shouldBe "Direct Title"

            val richTextValue = PagePropertyValue.RichTextValue.fromPlainText("Direct Rich Text")
            richTextValue.richText shouldHaveSize 1
            richTextValue.richText.first().plainText shouldBe "Direct Rich Text"

            val selectValue = PagePropertyValue.SelectValue.byName("High Priority")
            selectValue.select shouldNotBe null
            selectValue.select!!.name shouldBe "High Priority"

            val multiSelectValue = PagePropertyValue.MultiSelectValue.byNames("tag1", "tag2", "tag3")
            multiSelectValue.multiSelect shouldHaveSize 3
            multiSelectValue.multiSelect.map { it.name } shouldBe listOf("tag1", "tag2", "tag3")

            val dateValue = PagePropertyValue.DateValue.fromDateString("2024-12-31")
            dateValue.date!!.start shouldBe "2024-12-31"
            dateValue.date.end shouldBe null

            val dateTimeValue = PagePropertyValue.DateValue.fromDateTimeString("2024-12-31T23:59:59")
            dateTimeValue.date!!.start shouldBe "2024-12-31T23:59:59"

            val dateRangeValue = PagePropertyValue.DateValue.fromDateRange("2024-01-01", "2024-12-31")
            dateRangeValue.date!!.start shouldBe "2024-01-01"
            dateRangeValue.date.end shouldBe "2024-12-31"
        }

        "Should build properties with method chaining style" {
            val properties =
                pageProperties {
                    title("Task", "Important Task")
                    number("Priority", 1)
                    checkbox("Urgent", true)
                    select("Status", "In Progress")
                    date("Due", "2024-12-31")
                }

            properties shouldHaveSize 5

            // Verify the properties were all created correctly
            (properties["Task"] as PagePropertyValue.TitleValue).title.first().plainText shouldBe "Important Task"
            (properties["Priority"] as PagePropertyValue.NumberValue).number shouldBe 1.0
            (properties["Urgent"] as PagePropertyValue.CheckboxValue).checkbox shouldBe true
            (properties["Status"] as PagePropertyValue.SelectValue).select!!.name shouldBe "In Progress"
            (properties["Due"] as PagePropertyValue.DateValue).date!!.start shouldBe "2024-12-31"
        }
    })
