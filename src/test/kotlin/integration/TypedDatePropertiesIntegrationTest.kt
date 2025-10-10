package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.pages.endLocalDateValue
import it.saabel.kotlinnotionclient.models.pages.instantValue
import it.saabel.kotlinnotionclient.models.pages.localDateValue
import it.saabel.kotlinnotionclient.models.pages.toLocalDateTime
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

/**
 * Comprehensive integration test for typed date/datetime property API.
 *
 * This test validates the kotlinx-datetime integration with the Notion API:
 * 1. Creating pages with LocalDate properties
 * 2. Creating pages with LocalDateTime properties
 * 3. Creating pages with Instant properties
 * 4. Creating pages with date ranges using DSL builders
 * 5. Querying pages using typed date filters
 * 6. Reading date properties using typed accessor extensions
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update databases and pages
 * 4. API version must be 2025-09-03 or later
 * 5. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 *
 * Run with: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi")
class TypedDatePropertiesIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping TypedDatePropertiesIntegrationTest due to missing environment variables") }
        } else {
            "Typed date properties - LocalDate, LocalDateTime, Instant, and ranges" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    println("📊 Step 1: Creating database with date properties...")
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Typed Date Properties Test")
                            description("Testing kotlinx-datetime integration")
                            icon.emoji("📅")

                            properties {
                                title("Event Name")
                                date("Due Date")
                                date("Meeting Time")
                                date("Event Period")
                                date("Deployment Window")
                            }
                        }

                    database.shouldNotBeNull()
                    println("✅ Database created: ${database.id}")

                    delay(1000)

                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id
                    println("✅ Data source: $dataSourceId")

                    println("\n📝 Step 2: Creating page with LocalDate property...")
                    val localDatePage =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Event Name", "Product Launch")
                                date("Due Date", LocalDate(2025, 3, 15))
                            }
                        }

                    localDatePage.shouldNotBeNull()
                    println("✅ Created page with LocalDate: ${localDatePage.id}")

                    println("\n📝 Step 3: Creating page with LocalDateTime property...")
                    val localDateTimePage =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Event Name", "Team Meeting")
                                dateTime("Meeting Time", LocalDateTime(2025, 3, 20, 14, 30), TimeZone.UTC)
                            }
                        }

                    localDateTimePage.shouldNotBeNull()
                    println("✅ Created page with LocalDateTime: ${localDateTimePage.id}")

                    println("\n📝 Step 4: Creating page with Instant property...")
                    val instantPage =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Event Name", "System Deployment")
                                dateTime("Meeting Time", Instant.parse("2025-03-25T18:00:00Z"))
                            }
                        }

                    instantPage.shouldNotBeNull()
                    println("✅ Created page with Instant: ${instantPage.id}")

                    println("\n📝 Step 5: Creating page with date range using DSL...")
                    val dateRangePage =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Event Name", "Conference")
                                dateRange("Event Period") {
                                    start = LocalDate(2025, 4, 1)
                                    end = LocalDate(2025, 4, 3)
                                }
                            }
                        }

                    dateRangePage.shouldNotBeNull()
                    println("✅ Created page with date range: ${dateRangePage.id}")

                    println("\n📝 Step 6: Creating page with datetime range using DSL...")
                    val dateTimeRangePage =
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Event Name", "Maintenance Window")
                                dateTimeRange("Deployment Window", timeZone = TimeZone.UTC) {
                                    start = LocalDateTime(2025, 4, 10, 22, 0)
                                    end = LocalDateTime(2025, 4, 11, 2, 0)
                                }
                            }
                        }

                    dateTimeRangePage.shouldNotBeNull()
                    println("✅ Created page with datetime range: ${dateTimeRangePage.id}")

                    delay(1000)

                    println("\n🔍 Step 7: Querying with typed date filters...")
                    val afterMarch15 =
                        client.dataSources.query(dataSourceId) {
                            filter {
                                date("Due Date").after(LocalDate(2025, 3, 14))
                            }
                        }

                    (afterMarch15.isNotEmpty()) shouldBe true
                    println("✅ Found ${afterMarch15.size} page(s) after March 14")

                    val beforeMarch30 =
                        client.dataSources.query(dataSourceId) {
                            filter {
                                date("Meeting Time").before(
                                    LocalDateTime(2025, 3, 30, 0, 0),
                                    TimeZone.UTC,
                                )
                            }
                        }

                    (beforeMarch30.size >= 2) shouldBe true
                    println("✅ Found ${beforeMarch30.size} page(s) before March 30")

                    val onMarch15 =
                        client.dataSources.query(dataSourceId) {
                            filter {
                                date("Due Date").equals(LocalDate(2025, 3, 15))
                            }
                        }

                    onMarch15 shouldHaveSize 1
                    println("✅ Found exactly 1 page on March 15")

                    println("\n📖 Step 8: Testing typed accessor properties...")
                    val retrievedPage = client.pages.retrieve(localDatePage.id)
                    val dueDateProp = retrievedPage.properties["Due Date"]
                    dueDateProp.shouldNotBeNull()

                    if (dueDateProp is it.saabel.kotlinnotionclient.models.pages.PageProperty.Date) {
                        val localDate = dueDateProp.localDateValue
                        localDate.shouldNotBeNull()
                        localDate shouldBe LocalDate(2025, 3, 15)
                        println("✅ LocalDate accessor works: $localDate")
                    }

                    val retrievedMeetingPage = client.pages.retrieve(localDateTimePage.id)
                    val meetingTimeProp = retrievedMeetingPage.properties["Meeting Time"]
                    meetingTimeProp.shouldNotBeNull()

                    if (meetingTimeProp is it.saabel.kotlinnotionclient.models.pages.PageProperty.Date) {
                        val instant = meetingTimeProp.instantValue
                        instant.shouldNotBeNull()
                        instant shouldBe Instant.parse("2025-03-20T14:30:00Z")
                        println("✅ Instant accessor works: $instant")

                        val localDateTime = meetingTimeProp.toLocalDateTime(TimeZone.UTC)
                        localDateTime.shouldNotBeNull()
                        localDateTime shouldBe LocalDateTime(2025, 3, 20, 14, 30, 0)
                        println("✅ LocalDateTime accessor works: $localDateTime")
                    }

                    println("\n📖 Step 9: Testing range accessor properties...")
                    val retrievedRangePage = client.pages.retrieve(dateRangePage.id)
                    val rangeProp = retrievedRangePage.properties["Event Period"]
                    rangeProp.shouldNotBeNull()

                    if (rangeProp is it.saabel.kotlinnotionclient.models.pages.PageProperty.Date) {
                        rangeProp.localDateValue shouldBe LocalDate(2025, 4, 1)
                        rangeProp.endLocalDateValue shouldBe LocalDate(2025, 4, 3)
                        println("✅ Date range accessors work: ${rangeProp.localDateValue} to ${rangeProp.endLocalDateValue}")
                    }

                    println("\n✅ All typed date property tests passed!")

                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test database...")
                        client.databases.archive(database.id)
                        println("✅ Cleanup complete")
                    } else {
                        println("\n⚠️ Skipping cleanup (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("📌 Database ID: ${database.id}")
                        println("📌 Data Source ID: $dataSourceId")
                    }
                } finally {
                    client.close()
                }
            }

            "Typed date filters with complex queries" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

                val client = NotionClient.create(NotionConfig(apiToken = token))

                try {
                    println("📊 Creating database for complex date filter test...")
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Date Filter Test")
                            properties {
                                title("Task")
                                date("Deadline")
                                checkbox("Completed")
                            }
                        }

                    delay(1000)

                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    println("📝 Creating test pages with various dates...")
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task", "Early Task")
                            date("Deadline", LocalDate(2025, 3, 10))
                            checkbox("Completed", false)
                        }
                    }

                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task", "Mid Task")
                            date("Deadline", LocalDate(2025, 3, 20))
                            checkbox("Completed", true)
                        }
                    }

                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task", "Late Task")
                            date("Deadline", LocalDate(2025, 3, 30))
                            checkbox("Completed", false)
                        }
                    }

                    delay(1000)

                    println("🔍 Querying with combined filters (date + checkbox)...")
                    val incompleteAfterMarch15 =
                        client.dataSources.query(dataSourceId) {
                            filter {
                                and(
                                    date("Deadline").after(LocalDate(2025, 3, 15)),
                                    checkbox("Completed").equals(false),
                                )
                            }
                        }

                    incompleteAfterMarch15 shouldHaveSize 1
                    println("✅ Combined filter returned ${incompleteAfterMarch15.size} page(s)")

                    val beforeMarch25 =
                        client.dataSources.query(dataSourceId) {
                            filter {
                                date("Deadline").onOrBefore(LocalDate(2025, 3, 25))
                            }
                        }

                    (beforeMarch25.size >= 2) shouldBe true
                    println("✅ onOrBefore filter returned ${beforeMarch25.size} page(s)")

                    if (shouldCleanupAfterTest()) {
                        client.databases.archive(database.id)
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
