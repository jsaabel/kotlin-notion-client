package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.datasources.RelativeDateValue
import kotlinx.coroutines.delay

/**
 * Comprehensive integration test for the Data Sources API (2025-09-03).
 *
 * This test validates the complete data sources workflow:
 * 1. Creating a database with initial data source
 * 2. Retrieving database and accessing data sources
 * 3. Querying pages from a data source
 * 4. Creating pages with data_source_id parent
 * 5. Updating data source properties
 * 6. Creating a second data source in the same database
 * 7. Verifying multi-source database behavior
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
class DataSourcesIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping DataSourcesIntegrationTest due to missing environment variables") }
        } else {
            "Full data sources workflow - create database, query, update, and multi-source" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📊 Step 1: Creating database with initial data source...")
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Data Sources Test Database")
                            description("Testing 2025-09-03 data sources API")
                            icon.emoji("📊")

                            properties {
                                title("Task Name")
                                select("Status") {
                                    option("To Do")
                                    option("In Progress")
                                    option("Done")
                                }
                                number("Priority")
                                date("Due Date")
                                checkbox("Completed")
                            }
                        }

                    database.shouldNotBeNull()
                    println("✅ Database created: ${database.id}")

                    // Small delay to ensure database is ready
                    delay(1000)

                    println("\n📋 Step 2: Retrieving database and verifying data sources...")
                    val retrievedDatabase = client.databases.retrieve(database.id)
                    retrievedDatabase.dataSources.shouldNotBeNull()
                    retrievedDatabase.dataSources.isNotEmpty() shouldBe true
                    val firstDataSource = retrievedDatabase.dataSources.first()
                    firstDataSource.id.shouldNotBeNull()
                    println("✅ Found data source: ${firstDataSource.id} - ${firstDataSource.name}")

                    println("\n🔍 Step 3: Retrieving data source details...")
                    val dataSource = client.dataSources.retrieve(firstDataSource.id)
                    dataSource.shouldNotBeNull()
                    dataSource.id shouldBe firstDataSource.id
                    dataSource.properties.shouldNotBeNull()
                    (dataSource.properties.size >= 5) shouldBe true
                    dataSource.properties.keys shouldContain "Task Name"
                    dataSource.properties.keys shouldContain "Status"
                    println("✅ Data source has ${dataSource.properties.size} properties")

                    println("\n📝 Step 4: Creating pages in the data source...")
                    val page1 =
                        client.pages.create {
                            parent.dataSource(firstDataSource.id)
                            properties {
                                title("Task Name", "First Task")
                                select("Status", "To Do")
                                number("Priority", 1.0)
                                checkbox("Completed", false)
                            }
                        }
                    page1.shouldNotBeNull()
                    println("✅ Created page 1: ${page1.id}")

                    val page2 =
                        client.pages.create {
                            parent.dataSource(firstDataSource.id)
                            properties {
                                title("Task Name", "Second Task")
                                select("Status", "In Progress")
                                number("Priority", 2.0)
                                checkbox("Completed", false)
                            }
                        }
                    page2.shouldNotBeNull()
                    println("✅ Created page 2: ${page2.id}")

                    delay(1000)

                    println("\n🔎 Step 5: Querying pages from data source...")
                    val pages = client.dataSources.query(firstDataSource.id)
                    (pages.size >= 2) shouldBe true
                    println("✅ Found ${pages.size} pages in data source")

                    println("\n✏️ Step 6: Updating data source properties (adding Email property)...")
                    val updatedDataSource =
                        client.dataSources.update(firstDataSource.id) {
                            title("Updated Tasks Data Source")
                            properties {
                                // Re-define existing properties
                                title("Task Name")
                                select("Status") {
                                    option("To Do")
                                    option("In Progress")
                                    option("Done")
                                    option("Blocked")
                                }
                                number("Priority")
                                date("Due Date")
                                checkbox("Completed")
                                // Add new property
                                email("Contact Email")
                            }
                        }
                    updatedDataSource.shouldNotBeNull()
                    (updatedDataSource.properties.size >= 6) shouldBe true
                    updatedDataSource.properties.keys shouldContain "Contact Email"
                    println("✅ Data source updated with new property")

                    println("\n🆕 Step 7: Creating a second data source in the same database...")
                    val secondDataSource =
                        client.dataSources.create {
                            databaseId(database.id)
                            title("Projects Data Source")

                            properties {
                                title("Project Name")
                                select("Phase") {
                                    option("Planning")
                                    option("Development")
                                    option("Testing")
                                    option("Launch")
                                }
                                number("Budget")
                                date("Deadline")
                            }
                        }
                    secondDataSource.shouldNotBeNull()
                    println("✅ Created second data source: ${secondDataSource.id}")

                    delay(1000)

                    println("\n🔄 Step 8: Verifying multi-source database...")
                    val multiSourceDb = client.databases.retrieve(database.id)
                    (multiSourceDb.dataSources.size >= 2) shouldBe true
                    val dataSourceIds = multiSourceDb.dataSources.map { it.id }
                    dataSourceIds shouldContain firstDataSource.id
                    dataSourceIds shouldContain secondDataSource.id
                    println("✅ Database now has ${multiSourceDb.dataSources.size} data sources")

                    println("\n📊 Step 9: Creating page in second data source...")
                    val projectPage =
                        client.pages.create {
                            parent.dataSource(secondDataSource.id)
                            properties {
                                title("Project Name", "New Website")
                                select("Phase", "Planning")
                                number("Budget", 50000.0)
                            }
                        }
                    projectPage.shouldNotBeNull()
                    println("✅ Created project page: ${projectPage.id}")

                    delay(1000)

                    println("\n🔍 Step 10: Querying both data sources separately...")
                    val tasksPages = client.dataSources.query(firstDataSource.id)
                    val projectsPages = client.dataSources.query(secondDataSource.id)
                    println("✅ Tasks data source: ${tasksPages.size} pages")
                    println("✅ Projects data source: ${projectsPages.size} pages")

                    // Verify isolation between data sources
                    (tasksPages.size >= 2) shouldBe true
                    (projectsPages.isNotEmpty()) shouldBe true

                    println("\n✅ All data sources workflow tests passed!")

                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test database...")
                        client.databases.trash(database.id)
                        println("✅ Cleanup complete")
                    } else {
                        println("\n⚠️ Skipping cleanup (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("📌 Database ID: ${database.id}")
                        println("📌 Data Source 1: ${firstDataSource.id}")
                        println("📌 Data Source 2: ${secondDataSource.id}")
                    }
                } finally {
                    client.close()
                }
            }

            "List templates for a data source" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📊 Creating database for template test...")
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Template Test Database")
                            properties {
                                title("Name")
                                richText("Description")
                            }
                        }

                    println("✅ Database created: ${database.url}")

                    delay(1000)

                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    println("🔍 Listing templates for data source...")
                    val templates = client.dataSources.listTemplates(dataSourceId)

                    // Templates may or may not exist - just verify the call succeeds
                    templates.shouldNotBeNull()
                    println("✅ Template listing succeeded: found ${templates.size} template(s)")

                    if (templates.isNotEmpty()) {
                        val defaultTemplate = templates.find { it.isDefault }
                        if (defaultTemplate != null) {
                            println("   📋 Default template: ${defaultTemplate.name} (${defaultTemplate.id})")
                        }
                        templates.forEach { template ->
                            println("   📄 ${template.name} (${template.id}) - default: ${template.isDefault}")
                        }
                    } else {
                        println("   ℹ️  No templates found for this data source")
                    }

                    // Test with name filter
                    println("\n🔍 Testing name filter...")
                    val filteredTemplates = client.dataSources.listTemplates(dataSourceId, nameFilter = "test")
                    filteredTemplates.shouldNotBeNull()
                    println("✅ Template listing with filter succeeded: found ${filteredTemplates.size} template(s)")

                    if (shouldCleanupAfterTest()) {
                        println("\n🧹 Cleaning up test database...")
                        client.databases.trash(database.id)
                        println("✅ Cleanup complete")
                    } else {
                        println("\n⚠️ Skipping cleanup (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("📌 Database URL: ${database.url}")
                        println("📌 Database ID: ${database.id}")
                        println("📌 Data Source ID: $dataSourceId")
                    }
                } finally {
                    client.close()
                }
            }

            "Data source query with filters" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")

                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("📊 Creating database for filtered query test...")
                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Filter Test Database")
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

                    val retrievedDb = client.databases.retrieve(database.id)
                    val dataSourceId = retrievedDb.dataSources.first().id

                    // Compute fixed dates for all 7 relative values
                    val now = java.time.LocalDate.now()
                    val dateToday = now.toString()
                    val dateYesterday = now.minusDays(1).toString()
                    val dateTomorrow = now.plusDays(1).toString()
                    val dateOneWeekAgo = now.minusDays(7).toString()
                    val dateOneWeekFromNow = now.plusDays(7).toString()
                    val dateOneMonthAgo = now.minusMonths(1).toString()
                    val dateOneMonthFromNow = now.plusMonths(1).toString()

                    println("📝 Creating test pages (property filter + 7 dated pages)...")
                    // Pages for property filter test
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "High Priority Active")
                            select("Priority", "High")
                            checkbox("Active", true)
                        }
                    }
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Low Priority Inactive")
                            select("Priority", "Low")
                            checkbox("Active", false)
                        }
                    }

                    // One page per relative date value
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Due Today")
                            date("Due Date", dateToday)
                        }
                    }
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Due Yesterday")
                            date("Due Date", dateYesterday)
                        }
                    }
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Due Tomorrow")
                            date("Due Date", dateTomorrow)
                        }
                    }
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Due One Week Ago")
                            date("Due Date", dateOneWeekAgo)
                        }
                    }
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Due One Week From Now")
                            date("Due Date", dateOneWeekFromNow)
                        }
                    }
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Due One Month Ago")
                            date("Due Date", dateOneMonthAgo)
                        }
                    }
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Due One Month From Now")
                            date("Due Date", dateOneMonthFromNow)
                        }
                    }

                    delay(1000)

                    // --- Property filter sanity check ---
                    println("🔍 Querying with property filters (select + checkbox)...")
                    val activeHighPriority =
                        client.dataSources.query(dataSourceId) {
                            filter {
                                and(
                                    select("Priority").equals("High"),
                                    checkbox("Active").equals(true),
                                )
                            }
                        }
                    activeHighPriority shouldHaveSize 1
                    println("✅ Property filter: ${activeHighPriority.size} page(s)")

                    // --- equals: each relative value matches exactly the one page with that date ---
                    println("🔍 Testing equals() for all 7 relative date values...")
                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").equals(RelativeDateValue.TODAY) }
                    } shouldHaveSize 1

                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").equals(RelativeDateValue.YESTERDAY) }
                    } shouldHaveSize 1

                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").equals(RelativeDateValue.TOMORROW) }
                    } shouldHaveSize 1

                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").equals(RelativeDateValue.ONE_WEEK_AGO) }
                    } shouldHaveSize 1

                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").equals(RelativeDateValue.ONE_WEEK_FROM_NOW) }
                    } shouldHaveSize 1

                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").equals(RelativeDateValue.ONE_MONTH_AGO) }
                    } shouldHaveSize 1

                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").equals(RelativeDateValue.ONE_MONTH_FROM_NOW) }
                    } shouldHaveSize 1
                    println("✅ equals() checks passed for all 7 relative values")

                    // --- after: strictly after yesterday = today, tomorrow, +1w, +1m (4 pages) ---
                    println("🔍 Testing after(YESTERDAY)...")
                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").after(RelativeDateValue.YESTERDAY) }
                    } shouldHaveSize 4
                    println("✅ after(YESTERDAY): 4 page(s)")

                    // --- before: strictly before today = yesterday, -1w, -1m (3 pages) ---
                    println("🔍 Testing before(TODAY)...")
                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").before(RelativeDateValue.TODAY) }
                    } shouldHaveSize 3
                    println("✅ before(TODAY): 3 page(s)")

                    // --- onOrAfter: today or later = today, tomorrow, +1w, +1m (4 pages) ---
                    println("🔍 Testing onOrAfter(TODAY)...")
                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").onOrAfter(RelativeDateValue.TODAY) }
                    } shouldHaveSize 4
                    println("✅ onOrAfter(TODAY): 4 page(s)")

                    // --- onOrBefore: today or earlier = today, yesterday, -1w, -1m (4 pages) ---
                    println("🔍 Testing onOrBefore(TODAY)...")
                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").onOrBefore(RelativeDateValue.TODAY) }
                    } shouldHaveSize 4
                    println("✅ onOrBefore(TODAY): 4 page(s)")

                    // --- onOrAfter with future relative value: +1w and +1m (2 pages) ---
                    println("🔍 Testing onOrAfter(ONE_WEEK_FROM_NOW)...")
                    client.dataSources.query(dataSourceId) {
                        filter { date("Due Date").onOrAfter(RelativeDateValue.ONE_WEEK_FROM_NOW) }
                    } shouldHaveSize 2
                    println("✅ onOrAfter(ONE_WEEK_FROM_NOW): 2 page(s)")

                    if (shouldCleanupAfterTest()) {
                        client.databases.trash(database.id)
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
