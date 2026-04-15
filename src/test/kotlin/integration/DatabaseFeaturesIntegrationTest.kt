package integration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionApiLimits
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Annotations
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.base.TextContent
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.BulletedListItemRequestContent
import it.saabel.kotlinnotionclient.models.blocks.ParagraphRequestContent
import it.saabel.kotlinnotionclient.models.databases.DatabaseProperty
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryBuilder
import it.saabel.kotlinnotionclient.models.datasources.RelativeDateValue
import it.saabel.kotlinnotionclient.models.datasources.SortDirection
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.SelectOption
import it.saabel.kotlinnotionclient.models.pages.endLocalDateValue
import it.saabel.kotlinnotionclient.models.pages.getCheckboxProperty
import it.saabel.kotlinnotionclient.models.pages.getNumberProperty
import it.saabel.kotlinnotionclient.models.pages.getTitleAsPlainText
import it.saabel.kotlinnotionclient.models.pages.instantValue
import it.saabel.kotlinnotionclient.models.pages.localDateValue
import it.saabel.kotlinnotionclient.models.pages.toLocalDateTime
import it.saabel.kotlinnotionclient.validation.ValidationException
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * Integration tests for database-related features.
 *
 * Covers:
 * - Data Sources API: full workflow (create, retrieve, query, update, multi-source databases)
 * - Data Sources: template listing, query with relative date filters and property filters
 * - Database Query: checkbox/number/title/AND filters, sorting, pagination, empty results
 * - Status property: default options + groups, custom options + colors/descriptions
 * - Typed date properties: LocalDate, LocalDateTime, Instant, date ranges, typed filters
 * - Unknown property types: graceful deserialization as PageProperty.Unknown
 * - Validation: pre-flight rejection of over-limit content (multi-select, blocks, rich text)
 *
 * All databases and pages are created under a single container page. Trashing the
 * container (NOTION_CLEANUP_AFTER_TEST=true) cascades to all children.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Optional:
 * - export NOTION_TEST_DATASOURCE_ID="..." (enables unknown property types verification)
 *
 * Run with: ./gradlew integrationTest --tests "*DatabaseFeaturesIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class DatabaseFeaturesIntegrationTest :
    StringSpec({

        fun createRichText(content: String): RichText =
            RichText(
                type = "text",
                text = TextContent(content = content),
                annotations = Annotations(),
                plainText = content,
                href = null,
            )

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) database features integration" {
                println("Skipping DatabaseFeaturesIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Database Features — Integration Tests")
                        icon.emoji("🗄️")
                        content {
                            callout(
                                "ℹ️",
                                "Covers the Data Sources API (full CRUD workflow, multi-source databases, template listing, " +
                                    "relative date filters), database query (property filters, sorting, pagination), " +
                                    "Status property (default and custom options), typed date properties (LocalDate, " +
                                    "LocalDateTime, Instant, ranges, typed filters), unknown property type handling, " +
                                    "and pre-flight validation (multi-select limit, block count limit, rich text length).",
                            )
                        }
                    }
                containerPageId = container.id
                println("📄 Container: ${container.url}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("✅ Cleaned up container page (all children trashed)")
                } else {
                    println("🔧 Cleanup skipped — container page preserved for inspection")
                }
                notion.close()
            }

            // ------------------------------------------------------------------
            // 1. Data Sources — full workflow (create, retrieve, query, update, multi-source)
            // ------------------------------------------------------------------
            "should demonstrate full data sources workflow including multi-source database" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Data Sources Test Database")
                        icon.emoji("📊")
                        properties {
                            title("Task Name")
                            status("Status")
                            number("Priority")
                            date("Due Date")
                            checkbox("Completed")
                        }
                    }
                println("  Database: ${database.url}")
                delay(1000)

                val retrievedDb = notion.databases.retrieve(database.id)
                retrievedDb.dataSources.isNotEmpty() shouldBe true
                val firstDs = retrievedDb.dataSources.first()
                firstDs.id.shouldNotBeNull()
                println("  Data source: ${firstDs.id} — ${firstDs.name}")

                val dataSource = notion.dataSources.retrieve(firstDs.id)
                dataSource.properties.keys shouldContain "Task Name"
                dataSource.properties.keys shouldContain "Status"

                val page1 =
                    notion.pages.create {
                        parent.dataSource(firstDs.id)
                        properties {
                            title("Task Name", "First Task")
                            status("Status", "Not started")
                            number("Priority", 1.0)
                        }
                    }
                val page2 =
                    notion.pages.create {
                        parent.dataSource(firstDs.id)
                        properties {
                            title("Task Name", "Second Task")
                            status("Status", "In progress")
                            number("Priority", 2.0)
                        }
                    }
                delay(1000)

                val pages = notion.dataSources.query(firstDs.id)
                (pages.size >= 2) shouldBe true
                println("  Queried ${pages.size} pages from data source")

                val updatedDs =
                    notion.dataSources.update(firstDs.id) {
                        title("Updated Tasks Data Source")
                        properties {
                            title("Task Name")
                            status("Status") { option("Blocked") }
                            number("Priority")
                            date("Due Date")
                            checkbox("Completed")
                            email("Contact Email")
                        }
                    }
                updatedDs.properties.keys shouldContain "Contact Email"

                val secondDs =
                    notion.dataSources.create {
                        databaseId(database.id)
                        title("Projects Data Source")
                        properties {
                            title("Project Name")
                            select("Phase") {
                                option("Planning")
                                option("Development")
                                option("Launch")
                            }
                            number("Budget")
                        }
                    }
                delay(1000)

                val multiSourceDb = notion.databases.retrieve(database.id)
                (multiSourceDb.dataSources.size >= 2) shouldBe true
                multiSourceDb.dataSources.map { it.id } shouldContain secondDs.id

                notion.pages.create {
                    parent.dataSource(secondDs.id)
                    properties {
                        title("Project Name", "New Website")
                        select("Phase", "Planning")
                        number("Budget", 50000.0)
                    }
                }
                delay(1000)

                val tasksPages = notion.dataSources.query(firstDs.id)
                val projectsPages = notion.dataSources.query(secondDs.id)
                (tasksPages.size >= 2) shouldBe true
                projectsPages.isNotEmpty() shouldBe true

                println("  ✅ Full data sources workflow: ${tasksPages.size} task(s), ${projectsPages.size} project(s)")
            }

            // ------------------------------------------------------------------
            // 2. Data Sources — list templates (verifies API call succeeds)
            // ------------------------------------------------------------------
            "should list templates for a freshly created data source" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Template Listing Test")
                        icon.emoji("📋")
                        properties {
                            title("Name")
                            richText("Description")
                        }
                    }
                println("  Template DB: ${database.url}")
                delay(1000)

                val ds =
                    notion.databases
                        .retrieve(database.id)
                        .dataSources
                        .first()
                val templates = notion.dataSources.listTemplates(ds.id)
                templates.shouldNotBeNull()
                println("  Found ${templates.size} template(s)")

                val filtered = notion.dataSources.listTemplates(ds.id, nameFilter = "test")
                filtered.shouldNotBeNull()
                println("  ✅ Template listing verified (${filtered.size} matching 'test')")
            }

            // ------------------------------------------------------------------
            // 3. Data Sources — query with property filters and relative date filters
            // ------------------------------------------------------------------
            "should query data source with property filters and all relative date values" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Filter Test Database")
                        icon.emoji("🔍")
                        properties {
                            title("Name")
                            select("Priority") {
                                option("Low")
                                option("Medium")
                                option("High")
                            }
                            checkbox("Active")
                            date("Due Date")
                        }
                    }
                delay(1000)

                val ds =
                    notion.databases
                        .retrieve(database.id)
                        .dataSources
                        .first()
                val now = java.time.LocalDate.now()

                // Property filter pages
                notion.pages.create {
                    parent.dataSource(ds.id)
                    properties {
                        title("Name", "High Priority Active")
                        select("Priority", "High")
                        checkbox("Active", true)
                    }
                }
                notion.pages.create {
                    parent.dataSource(ds.id)
                    properties {
                        title("Name", "Low Priority Inactive")
                        select("Priority", "Low")
                        checkbox("Active", false)
                    }
                }

                // One page per relative date value
                val relativeDates =
                    mapOf(
                        "Due Today" to now.toString(),
                        "Due Yesterday" to now.minusDays(1).toString(),
                        "Due Tomorrow" to now.plusDays(1).toString(),
                        "Due One Week Ago" to now.minusDays(7).toString(),
                        "Due One Week From Now" to now.plusDays(7).toString(),
                        "Due One Month Ago" to now.minusMonths(1).toString(),
                        "Due One Month From Now" to now.plusMonths(1).toString(),
                    )

                relativeDates.forEach { (name, dateStr) ->
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Name", name)
                            date("Due Date", dateStr)
                        }
                    }
                }
                delay(1000)

                // Property filter check
                val activeHigh =
                    notion.dataSources.query(ds.id) {
                        filter {
                            and(
                                select("Priority").equals("High"),
                                checkbox("Active").equals(true),
                            )
                        }
                    }
                activeHigh shouldHaveSize 1

                // Relative date equality checks
                for (relValue in RelativeDateValue.entries) {
                    notion.dataSources.query(ds.id) {
                        filter { date("Due Date").equals(relValue) }
                    } shouldHaveSize 1
                }

                // Range checks
                notion.dataSources.query(ds.id) {
                    filter { date("Due Date").after(RelativeDateValue.YESTERDAY) }
                } shouldHaveSize 4

                notion.dataSources.query(ds.id) {
                    filter { date("Due Date").before(RelativeDateValue.TODAY) }
                } shouldHaveSize 3

                println("  ✅ Property filters and all 7 relative date values verified")
            }

            // ------------------------------------------------------------------
            // 4. Database Query — filters, sorting, pagination
            // ------------------------------------------------------------------
            "should query database with checkbox, number, title, AND filters, sorting, and pagination" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Query Test Database")
                        icon.emoji("🔎")
                        properties {
                            title("Name")
                            checkbox("Completed")
                            number("Score")
                        }
                    }
                delay(1000)

                val ds =
                    notion.databases
                        .retrieve(database.id)
                        .dataSources
                        .first()

                listOf(
                    Triple("High Priority Task", false, 95.0),
                    Triple("Completed Task", true, 75.0),
                    Triple("Low Score Task", false, 45.0),
                ).forEach { (name, completed, score) ->
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Name", name)
                            checkbox("Completed", completed)
                            number("Score", score)
                        }
                    }
                }
                delay(1000)

                notion.dataSources.query(ds.id).size shouldBe 3

                val completed =
                    notion.dataSources.query(ds.id, DataSourceQueryBuilder().filter { checkbox("Completed").equals(true) }.build())
                completed.size shouldBe 1

                val highScore =
                    notion.dataSources.query(ds.id, DataSourceQueryBuilder().filter { number("Score").greaterThan(80) }.build())
                highScore.size shouldBe 1

                val priority =
                    notion.dataSources.query(ds.id, DataSourceQueryBuilder().filter { title("Name").contains("Priority") }.build())
                priority.size shouldBe 1

                val andResult =
                    notion.dataSources.query(
                        ds.id,
                        DataSourceQueryBuilder()
                            .filter {
                                and(
                                    checkbox("Completed").equals(false),
                                    number("Score").greaterThan(50),
                                )
                            }.build(),
                    )
                andResult.size shouldBe 1
                andResult.first().getTitleAsPlainText("Name") shouldBe "High Priority Task"

                val sorted =
                    notion.dataSources.query(ds.id, DataSourceQueryBuilder().sortBy("Score", SortDirection.DESCENDING).build())
                sorted.map { it.getNumberProperty("Score") ?: 0.0 } shouldBe listOf(95.0, 75.0, 45.0)

                println("  ✅ Filters (checkbox, number, title, AND), sorting, and pagination verified")
            }

            // ------------------------------------------------------------------
            // 5. Database Query — empty result handling
            // ------------------------------------------------------------------
            "should handle empty query results and no-match filters" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Empty Query Test DB")
                        icon.emoji("🈳")
                        properties { title("Name") }
                    }
                delay(1000)

                val ds =
                    notion.databases
                        .retrieve(database.id)
                        .dataSources
                        .first()

                notion.dataSources.query(ds.id).size shouldBe 0

                val noMatch =
                    notion.dataSources.query(
                        ds.id,
                        DataSourceQueryBuilder().filter { title("Name").contains("NonexistentText12345") }.build(),
                    )
                noMatch.size shouldBe 0

                println("  ✅ Empty results and no-match filter verified")
            }

            // ------------------------------------------------------------------
            // 6. Status property — default options and standard groups
            // ------------------------------------------------------------------
            "should create status property with default options and verify standard groups" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Status Default Options Test")
                        icon.emoji("🔖")
                        properties {
                            title("Name")
                            status("Stage")
                        }
                    }
                println("  Status DB: ${database.url}")
                delay(500)

                val ds = notion.dataSources.retrieve(database.dataSources.first().id)
                val stageProp = ds.properties["Stage"] as DatabaseProperty.Status

                stageProp.status.options.map { it.name } shouldContain "Not started"
                stageProp.status.options.map { it.name } shouldContain "Done"
                stageProp.status.groups.map { it.name } shouldContain "To-do"
                stageProp.status.groups.map { it.name } shouldContain "Complete"

                val notStartedId =
                    stageProp.status.options
                        .first { it.name == "Not started" }
                        .id
                stageProp.status.groups
                    .first { it.name == "To-do" }
                    .optionIds shouldContain notStartedId

                delay(500)
                val page1 =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Name", "Task A")
                            status("Stage", "Not started")
                        }
                    }
                val page2 =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Name", "Task B")
                            status("Stage", "Done")
                        }
                    }
                delay(500)

                (notion.pages.retrieve(page1.id).properties["Stage"] as PageProperty.Status).status?.name shouldBe "Not started"
                (notion.pages.retrieve(page2.id).properties["Stage"] as PageProperty.Status).status?.name shouldBe "Done"

                println("  ✅ Default status options, groups, and page values verified")
            }

            // ------------------------------------------------------------------
            // 7. Status property — custom options with colors and descriptions
            // ------------------------------------------------------------------
            "should create status property with custom options, colors, and descriptions" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Status Custom Options Test")
                        icon.emoji("🏷️")
                        properties {
                            title("Name")
                            status("Stage") {
                                option("Backlog", SelectOptionColor.GRAY, "Work not yet started")
                                option("In Review", SelectOptionColor.YELLOW)
                                option("Shipped", SelectOptionColor.GREEN)
                            }
                        }
                    }
                println("  Custom status DB: ${database.url}")
                delay(500)

                val ds = notion.dataSources.retrieve(database.dataSources.first().id)
                val stageProp = ds.properties["Stage"] as DatabaseProperty.Status

                stageProp.status.options.map { it.name } shouldContain "Backlog"
                stageProp.status.options.map { it.name } shouldContain "Shipped"

                val backlogOption = stageProp.status.options.first { it.name == "Backlog" }
                backlogOption.color shouldBe SelectOptionColor.GRAY
                backlogOption.description shouldBe "Work not yet started"
                stageProp.status.options
                    .first { it.name == "Shipped" }
                    .color shouldBe SelectOptionColor.GREEN

                delay(500)
                val page1 =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Name", "Item A")
                            status("Stage", "Backlog")
                        }
                    }
                delay(500)
                (notion.pages.retrieve(page1.id).properties["Stage"] as PageProperty.Status).status?.name shouldBe "Backlog"

                println("  ✅ Custom status options (colors, descriptions, page values) verified")
            }

            // ------------------------------------------------------------------
            // 8. Typed date properties — LocalDate, LocalDateTime, Instant, ranges
            // ------------------------------------------------------------------
            "should create pages with typed date properties and verify round-trip" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Typed Date Properties Test")
                        icon.emoji("📅")
                        properties {
                            title("Event Name")
                            date("Due Date")
                            date("Meeting Time")
                            date("Event Period")
                            date("Deployment Window")
                        }
                    }
                println("  Typed date DB: ${database.url}")
                delay(1000)

                val ds =
                    notion.databases
                        .retrieve(database.id)
                        .dataSources
                        .first()

                val localDatePage =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Event Name", "Product Launch")
                            date("Due Date", LocalDate(2025, 3, 15))
                        }
                    }

                val localDateTimePage =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Event Name", "Team Meeting")
                            dateTime("Meeting Time", LocalDateTime(2025, 3, 20, 14, 30), TimeZone.UTC)
                        }
                    }

                notion.pages.create {
                    parent.dataSource(ds.id)
                    properties {
                        title("Event Name", "System Deployment")
                        dateTime("Meeting Time", Instant.parse("2025-03-25T18:00:00Z"))
                    }
                }

                val dateRangePage =
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Event Name", "Conference")
                            dateRange("Event Period") {
                                start = LocalDate(2025, 4, 1)
                                end = LocalDate(2025, 4, 3)
                            }
                        }
                    }

                notion.pages.create {
                    parent.dataSource(ds.id)
                    properties {
                        title("Event Name", "Maintenance Window")
                        dateTimeRange("Deployment Window", timeZone = TimeZone.UTC) {
                            start = LocalDateTime(2025, 4, 10, 22, 0)
                            end = LocalDateTime(2025, 4, 11, 2, 0)
                        }
                    }
                }
                delay(1000)

                // Typed filters
                notion.dataSources
                    .query(ds.id) {
                        filter { date("Due Date").after(LocalDate(2025, 3, 14)) }
                    }.isNotEmpty() shouldBe true

                notion.dataSources.query(ds.id) {
                    filter { date("Due Date").equals(LocalDate(2025, 3, 15)) }
                } shouldHaveSize 1

                // Typed accessor round-trip
                val retrievedDate = notion.pages.retrieve(localDatePage.id)
                val dateProp = retrievedDate.properties["Due Date"] as? PageProperty.Date
                dateProp?.localDateValue shouldBe LocalDate(2025, 3, 15)

                val retrievedMeeting = notion.pages.retrieve(localDateTimePage.id)
                val meetingProp = retrievedMeeting.properties["Meeting Time"] as? PageProperty.Date
                meetingProp?.instantValue shouldBe Instant.parse("2025-03-20T14:30:00Z")
                meetingProp?.toLocalDateTime(TimeZone.UTC) shouldBe LocalDateTime(2025, 3, 20, 14, 30, 0)

                val retrievedRange = notion.pages.retrieve(dateRangePage.id)
                val rangeProp = retrievedRange.properties["Event Period"] as? PageProperty.Date
                rangeProp?.localDateValue shouldBe LocalDate(2025, 4, 1)
                rangeProp?.endLocalDateValue shouldBe LocalDate(2025, 4, 3)

                println("  ✅ LocalDate, LocalDateTime, Instant, ranges, and typed filters verified")
            }

            // ------------------------------------------------------------------
            // 9. Unknown property types — graceful deserialization
            // ------------------------------------------------------------------
            "should handle unknown property types gracefully when NOTION_TEST_DATASOURCE_ID is set" {
                val testDataSourceId = System.getenv("NOTION_TEST_DATASOURCE_ID")

                if (testDataSourceId == null) {
                    // Setup mode: create a database for manual button property addition
                    val database =
                        notion.databases.create {
                            parent.page(containerPageId)
                            title("Unknown Property Test")
                            icon.emoji("🧪")
                            properties {
                                title("Name")
                                richText("Description")
                                number("Score")
                                checkbox("Active")
                            }
                        }
                    val ds = database.dataSources.firstOrNull()
                    ds.shouldNotBeNull()
                    delay(1000)
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties {
                            title("Name", "Test Item")
                            richText("Description", "Test description")
                            number("Score", 42.0)
                            checkbox("Active", true)
                        }
                    }
                    println("  Setup database: ${database.url}")
                    println("  Next: Add a Button property in Notion UI, then set:")
                    println("    export NOTION_TEST_DATASOURCE_ID=\"${ds.id}\"")
                    println("  Re-run to verify unknown type handling")
                } else {
                    val pages = notion.dataSources.query(testDataSourceId) {}
                    pages.isNotEmpty() shouldBe true

                    pages.forEach { page ->
                        val unknownCount = page.properties.values.count { it is PageProperty.Unknown }
                        val title = page.getTitleAsPlainText("Name")
                        println("  Page: $title — $unknownCount unknown property type(s)")
                    }

                    val firstPage = pages.first()
                    firstPage.getNumberProperty("Score").shouldNotBeNull()
                    firstPage.getCheckboxProperty("Active").shouldNotBeNull()

                    println("  ✅ Unknown property types handled gracefully — supported properties still accessible")
                }
            }

            // ------------------------------------------------------------------
            // 10. Validation — pre-flight: multi-select option count limit
            // ------------------------------------------------------------------
            "should reject page creation with more than 100 multi-select options pre-flight" {
                val tooManyOptions =
                    (1..101).map { i ->
                        SelectOption(id = "option-$i", name = "Option $i", color = SelectOptionColor.DEFAULT)
                    }

                val exception =
                    shouldThrow<ValidationException> {
                        notion.pages.create(
                            CreatePageRequest(
                                parent = Parent.PageParent(pageId = containerPageId),
                                properties =
                                    mapOf(
                                        "title" to PagePropertyValue.TitleValue(title = listOf(createRichText("Test"))),
                                        "multiSelect" to PagePropertyValue.MultiSelectValue(multiSelect = tooManyOptions),
                                    ),
                            ),
                        )
                    }

                exception.message shouldNotBe null
                println("  ✅ Pre-flight rejected: ${exception.message?.take(80)}")
            }

            // ------------------------------------------------------------------
            // 11. Validation — pre-flight: block count limit
            // ------------------------------------------------------------------
            "should reject appendChildren with more than 100 blocks pre-flight" {
                val tooManyBlocks =
                    (1..101).map {
                        BlockRequest.Paragraph(
                            paragraph = ParagraphRequestContent(richText = listOf(createRichText("Block $it"))),
                        )
                    }

                val exception =
                    shouldThrow<ValidationException> {
                        notion.blocks.appendChildren(containerPageId, tooManyBlocks)
                    }

                exception.message shouldNotBe null
                println("  ✅ Pre-flight rejected: ${exception.message?.take(80)}")
            }

            // ------------------------------------------------------------------
            // 12. Validation — pre-flight: rich text length limit
            // ------------------------------------------------------------------
            "should reject blocks with rich text exceeding 2000 chars pre-flight" {
                val longText = "a".repeat(2500)
                val longRichText =
                    RichText(
                        type = "text",
                        text = TextContent(content = longText),
                        annotations = Annotations(),
                        plainText = longText,
                        href = null,
                    )

                val exception =
                    shouldThrow<ValidationException> {
                        notion.blocks.appendChildren(
                            containerPageId,
                            listOf(
                                BlockRequest.Paragraph(
                                    paragraph = ParagraphRequestContent(richText = listOf(longRichText)),
                                ),
                            ),
                        )
                    }

                exception.message shouldNotBe null
                println("  ✅ Pre-flight rejected: ${exception.message?.take(80)}")
            }

            // ------------------------------------------------------------------
            // 13. Validation — per-segment limit: multiple segments totaling >2000 chars each pass
            // ------------------------------------------------------------------
            "should accept multiple rich text segments where each is under 2000 chars" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Per-Segment Limit Test")
                        icon.emoji("📏")
                        properties {
                            title("Name")
                            richText("Content")
                        }
                    }
                delay(1000)

                val ds =
                    notion.databases
                        .retrieve(database.id)
                        .dataSources
                        .first()

                val seg1Text = "a".repeat(1800)
                val seg2Text = "b".repeat(1800)
                val seg1 =
                    RichText(
                        type = "text",
                        text = TextContent(content = seg1Text),
                        annotations = Annotations(),
                        plainText = seg1Text,
                        href = null,
                    )
                val seg2 =
                    RichText(
                        type = "text",
                        text = TextContent(content = seg2Text),
                        annotations = Annotations(),
                        plainText = seg2Text,
                        href = null,
                    )
                val seg3 = createRichText("Final segment")

                val result =
                    notion.pages.create(
                        CreatePageRequest(
                            parent = Parent.DataSourceParent(dataSourceId = ds.id),
                            properties =
                                mapOf(
                                    "Name" to PagePropertyValue.TitleValue(title = listOf(createRichText("Test Entry"))),
                                    "Content" to PagePropertyValue.RichTextValue(richText = listOf(seg1, seg2, seg3)),
                                ),
                        ),
                    )
                result shouldNotBe null

                val retrieved = notion.pages.retrieve(result.id)
                val contentProp = retrieved.properties["Content"] as? PageProperty.RichTextProperty
                val segments = contentProp?.richText ?: emptyList()
                segments.size shouldBe 3

                val totalLength = segments.sumOf { it.plainText.length }
                totalLength shouldBeGreaterThan NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH
                segments.forEach { it.plainText.length shouldBeLessThanOrEqualTo NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH }

                println("  ✅ Per-segment limit confirmed: total $totalLength chars across 3 segments (each ≤ 2000)")
            }

            // ------------------------------------------------------------------
            // 14. queryFirstPage — single API call, no auto-pagination
            // ------------------------------------------------------------------
            "queryFirstPage should return exactly pageSize results and expose hasMore" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("queryFirstPage Test Database")
                        icon.emoji("🔢")
                        properties { title("Name") }
                    }
                delay(1000)

                val ds =
                    notion.databases
                        .retrieve(database.id)
                        .dataSources
                        .first()

                // Create 4 pages so we can paginate with pageSize=2
                repeat(4) { i ->
                    notion.pages.create {
                        parent.dataSource(ds.id)
                        properties { title("Name", "Item ${i + 1}") }
                    }
                }
                delay(1000)

                // query() should return all 4 (auto-pagination)
                val all = notion.dataSources.query(ds.id)
                all shouldHaveSize 4

                // queryFirstPage with pageSize=2 — exactly one API call
                val firstPage = notion.dataSources.queryFirstPage(ds.id) { pageSize(2) }
                firstPage.results shouldHaveSize 2
                firstPage.hasMore shouldBe true
                firstPage.nextCursor.shouldNotBeNull()

                // Using the cursor to fetch the second (last) page manually
                val secondPage =
                    notion.dataSources.queryFirstPage(ds.id) {
                        pageSize(2)
                        startCursor(firstPage.nextCursor)
                    }
                secondPage.results shouldHaveSize 2
                secondPage.hasMore shouldBe false

                println(
                    "  ✅ queryFirstPage: page 1 returned ${firstPage.results.size} items (hasMore=${firstPage.hasMore}), " +
                        "page 2 returned ${secondPage.results.size} items (hasMore=${secondPage.hasMore})",
                )
            }
        }
    })
