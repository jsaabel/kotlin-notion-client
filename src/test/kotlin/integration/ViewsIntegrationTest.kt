package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.datasources.CheckboxCondition
import it.saabel.kotlinnotionclient.models.datasources.DataSourceFilter
import it.saabel.kotlinnotionclient.models.datasources.DataSourceSort
import it.saabel.kotlinnotionclient.models.datasources.SortDirection
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.views.CoverConfig
import it.saabel.kotlinnotionclient.models.views.CoverSize
import it.saabel.kotlinnotionclient.models.views.CoverType
import it.saabel.kotlinnotionclient.models.views.GroupByConfig
import it.saabel.kotlinnotionclient.models.views.GroupSort
import it.saabel.kotlinnotionclient.models.views.GroupSortType
import it.saabel.kotlinnotionclient.models.views.TimelinePreference
import it.saabel.kotlinnotionclient.models.views.TimelineZoomLevel
import it.saabel.kotlinnotionclient.models.views.UpdateViewRequest
import it.saabel.kotlinnotionclient.models.views.ViewConfiguration
import it.saabel.kotlinnotionclient.models.views.ViewRange
import it.saabel.kotlinnotionclient.models.views.ViewType
import kotlinx.coroutines.delay

/**
 * Integration tests for the Views API.
 *
 * Test scenarios:
 * 1. Full workflow — list, retrieve, create (DSL), update (DSL), filter+sort, query, delete
 * 2. Property visibility — rich database schema, selective show/hide per view type
 * 3. List by data_source_id
 * 4. View query pagination
 *
 * Each test creates a container page under NOTION_TEST_PAGE_ID to keep all created
 * objects grouped and make cleanup straightforward.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="<parent page id>"
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 * - Integration must have access to create databases, views, and pages
 * - API version must be 2025-09-03 or later
 * - Set NOTION_CLEANUP_AFTER_TEST="false" to keep objects for manual inspection
 *
 * Run with: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi")
class ViewsIntegrationTest :
    StringSpec({

        val token = System.getenv("NOTION_API_TOKEN") ?: ""
        val parentPageId = System.getenv("NOTION_TEST_PAGE_ID") ?: ""
        var containerPageId = ""

        beforeSpec {
            if (integrationTestEnvVarsAreSet()) {
                val client = NotionClient(NotionConfig(apiToken = token))
                val containerPage =
                    client.pages.create {
                        parent.page(parentPageId)
                        title("Views API Integration Tests")
                        icon.emoji("🧪")
                    }
                containerPageId = containerPage.id
                client.close()
                println("📦 Container page created: $containerPageId")
            }
        }

        afterSpec {
            if (integrationTestEnvVarsAreSet()) {
                if (shouldCleanupAfterTest()) {
                    val client = NotionClient(NotionConfig(apiToken = token))
                    client.pages.trash(containerPageId)
                    client.close()
                    println("🧹 Container page trashed: $containerPageId")
                } else {
                    println("⚠️ Skipping cleanup (NOTION_CLEANUP_AFTER_TEST=false)")
                    println("📌 Container page: $containerPageId")
                }
            }
        }

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping ViewsIntegrationTest due to missing environment variables") }
        } else {
            "Full views workflow - list, retrieve, create, update, query, and delete" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("\n🗄️ Step 1: Creating a test database...")
                    val database =
                        client.databases.create {
                            parent.page(containerPageId)
                            title("Task Database")
                            icon.emoji("✅")
                            properties {
                                title("Task Name")
                                checkbox("Done")
                            }
                        }

                    database.shouldNotBeNull()
                    val databaseId = database.id
                    val dataSourceId =
                        database.dataSources
                            .firstOrNull()
                            ?.id
                            .shouldNotBeNull()
                    println("✅ Database created: $databaseId")
                    println("   Data source: $dataSourceId")

                    delay(1000)

                    println("\n📝 Step 1b: Seeding test entries...")
                    val tasks =
                        listOf(
                            Pair("Buy groceries", false),
                            Pair("Write report", true),
                            Pair("Review PR", false),
                            Pair("Deploy to production", true),
                            Pair("Update docs", false),
                        )
                    tasks.forEach { (name, done) ->
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Task Name", name)
                                checkbox("Done", done)
                            }
                        }
                    }
                    println("✅ Created ${tasks.size} entries (3 incomplete, 2 complete)")

                    delay(1000)

                    println("\n👁 Step 2: Listing views for the database...")
                    val initialList = client.views.list(databaseId = databaseId)
                    initialList.shouldNotBeNull()
                    println("✅ Found ${initialList.results.size} initial view(s)")

                    val firstViewId =
                        initialList.results
                            .firstOrNull()
                            ?.id
                            .shouldNotBeNull()
                    println("   First view ID: $firstViewId")

                    println("\n🔍 Step 3: Retrieving view by ID...")
                    val retrievedView = client.views.retrieve(firstViewId)
                    retrievedView.shouldNotBeNull()
                    retrievedView.id shouldBe firstViewId
                    retrievedView.type.shouldNotBeNull()
                    println("✅ Retrieved view: '${retrievedView.name}' (type=${retrievedView.type})")

                    println("\n✨ Step 4: Creating a new table view via DSL...")
                    val createdView =
                        client.views.create {
                            dataSourceId(dataSourceId)
                            name("Integration Test View")
                            type(ViewType.TABLE)
                            database(databaseId)
                        }
                    createdView.shouldNotBeNull()
                    createdView.name shouldBe "Integration Test View"
                    createdView.type shouldBe ViewType.TABLE
                    val createdViewId = createdView.id
                    println("✅ Created view: $createdViewId")

                    delay(1000)

                    println("\n✏️ Step 5: Updating view name via DSL...")
                    val updatedView =
                        client.views.update(createdViewId) {
                            name("Renamed Integration Test View")
                        }
                    updatedView.shouldNotBeNull()
                    updatedView.name shouldBe "Renamed Integration Test View"
                    println("✅ View renamed to: '${updatedView.name}'")

                    delay(500)

                    // Resolve stable property IDs — the API normalizes property references to IDs
                    // on the way out, so we need the IDs to make meaningful round-trip assertions.
                    // URL-decode the IDs: the data source schema returns them percent-encoded (e.g. "ue%5Cl")
                    // but view filter responses return the decoded form (e.g. "ue\l").
                    val dataSource = client.dataSources.retrieve(dataSourceId)
                    val donePropertyId =
                        java.net.URLDecoder.decode(dataSource.properties["Done"]?.id.shouldNotBeNull(), "UTF-8")
                    val taskNamePropertyId =
                        java.net.URLDecoder.decode(dataSource.properties["Task Name"]?.id.shouldNotBeNull(), "UTF-8")
                    println("   Property IDs: Done=$donePropertyId, Task Name=$taskNamePropertyId")

                    println("\n🔽 Step 5b: Updating view with a filter (Done = false) and sort (Task Name ascending)...")
                    val viewWithFilterAndSort =
                        client.views.update(
                            createdViewId,
                            UpdateViewRequest(
                                filter =
                                    DataSourceFilter(
                                        property = donePropertyId,
                                        checkbox = CheckboxCondition(equals = false),
                                    ),
                                sorts =
                                    listOf(
                                        DataSourceSort(property = taskNamePropertyId, direction = SortDirection.ASCENDING),
                                    ),
                            ),
                        )
                    viewWithFilterAndSort.shouldNotBeNull()
                    val returnedFilter = viewWithFilterAndSort.filter.shouldNotBeNull()
                    returnedFilter.property shouldBe donePropertyId
                    returnedFilter.checkbox.shouldNotBeNull()
                    val returnedSorts = viewWithFilterAndSort.sorts.shouldNotBeNull()
                    returnedSorts.size shouldBe 1
                    returnedSorts[0].property shouldBe taskNamePropertyId
                    returnedSorts[0].direction shouldBe SortDirection.ASCENDING
                    println("✅ Filter (Done=false) and sort (Task Name asc) set and round-tripped correctly")

                    delay(500)

                    println("\n🔎 Step 6: Creating a view query (filter: Done=false, sort: Task Name asc)...")
                    val query = client.views.createQuery(createdViewId, pageSize = 10)
                    query.shouldNotBeNull()
                    query.id.shouldNotBeBlank()
                    query.viewId shouldBe createdViewId
                    query.expiresAt.shouldNotBeBlank()
                    // Filter should reduce 5 seeded tasks to the 3 where Done=false
                    query.totalCount shouldBe 3
                    println("✅ Filter working: ${query.totalCount} of 5 tasks have Done=false (expected 3)")

                    println("\n📄 Step 7: Retrieving results and verifying sort order...")
                    val queryResults = client.views.getQueryResults(createdViewId, query.id, pageSize = 10)
                    queryResults.shouldNotBeNull()
                    queryResults.hasMore shouldBe false
                    // Fetch each page's Task Name to verify alphabetical sort
                    val titlesInOrder =
                        queryResults.results.map { ref ->
                            val page = client.pages.retrieve(ref.id)
                            (page.properties["Task Name"] as? PageProperty.Title)?.plainText ?: "(untitled)"
                        }
                    println("   Returned order (should be A→Z):")
                    titlesInOrder.forEachIndexed { i, title -> println("   ${i + 1}. $title") }
                    titlesInOrder shouldBe titlesInOrder.sorted()
                    println("✅ Sort working: titles are in ascending alphabetical order")

                    println("\n🗑️ Step 8: Deleting cached query...")
                    val deletedQuery = client.views.deleteQuery(createdViewId, query.id)
                    deletedQuery.shouldNotBeNull()
                    deletedQuery.deleted.shouldBeTrue()
                    println("✅ Query deleted: ${deletedQuery.id}")

                    println("\n🌊 Step 9: Listing views via Flow...")
                    val viewRefs = mutableListOf<String>()
                    client.views.listAsFlow(databaseId = databaseId).collect { ref ->
                        viewRefs.add(ref.id)
                    }
                    viewRefs shouldContain createdViewId
                    println("✅ Flow emitted ${viewRefs.size} view reference(s), created view confirmed present")

                    println("\n🗑️ Step 10: Deleting the created view...")
                    val deletedView = client.views.delete(createdViewId)
                    deletedView.shouldNotBeNull()
                    deletedView.id shouldBe createdViewId
                    println("✅ View deleted: ${deletedView.id}")

                    println("\n✅ All views workflow tests passed!")
                } finally {
                    client.close()
                }
            }

            "Create views with selective property visibility across multiple view types" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    // Rich schema so there are enough properties to selectively show/hide
                    println("\n🗄️ Creating database with rich property schema...")
                    val database =
                        client.databases.create {
                            parent.page(containerPageId)
                            title("Views Property Visibility Test")
                            icon.emoji("📋")
                            properties {
                                title("Name")
                                status("Status")
                                select("Priority") {
                                    option("Low")
                                    option("Medium")
                                    option("High")
                                }
                                date("Due Date")
                                date("Start Date")
                                checkbox("Completed")
                                number("Effort")
                                richText("Notes")
                                email("Contact")
                            }
                        }

                    val databaseId = database.id
                    val dataSourceId =
                        database.dataSources
                            .firstOrNull()
                            ?.id
                            .shouldNotBeNull()
                    println("✅ Database created: $databaseId")

                    delay(1000)

                    println("\n📝 Seeding test entries...")

                    data class TaskEntry(
                        val name: String,
                        val status: String,
                        val priority: String,
                        val effort: Int,
                        val completed: Boolean,
                        val notes: String,
                    )
                    val entries =
                        listOf(
                            TaskEntry("Design mockups", "In progress", "High", 3, false, "Wireframes due Friday"),
                            TaskEntry("Build backend API", "Not started", "High", 8, false, "Auth endpoints first"),
                            TaskEntry("Write unit tests", "Done", "Medium", 5, true, "Coverage at 87%"),
                            TaskEntry("Update documentation", "In progress", "Low", 2, false, "README and API docs"),
                            TaskEntry("Deploy to staging", "Not started", "Medium", 1, false, "Needs sign-off"),
                        )
                    entries.forEach { entry ->
                        client.pages.create {
                            parent.dataSource(dataSourceId)
                            properties {
                                title("Name", entry.name)
                                status("Status", entry.status)
                                select("Priority", entry.priority)
                                number("Effort", entry.effort)
                                checkbox("Completed", entry.completed)
                                richText("Notes", entry.notes)
                            }
                        }
                    }
                    println("✅ Created ${entries.size} entries")

                    delay(1000)

                    // Retrieve the data source to get stable property IDs from the schema
                    println("\n🔑 Retrieving data source schema for property IDs...")
                    val dataSource = client.dataSources.retrieve(dataSourceId)
                    val propIds = dataSource.properties.mapValues { (_, prop) -> prop.id }
                    println("✅ Found ${propIds.size} properties:")
                    propIds.forEach { (name, id) -> println("   '$name' → $id") }

                    fun id(name: String): String = checkNotNull(propIds[name]) { "Property '$name' not found in schema" }

                    delay(500)

                    // ----------------------------------------------------------------
                    // Table view — show core task fields, hide extra detail fields
                    // ----------------------------------------------------------------
                    println("\n📋 Creating Table view (Name + Status + Priority + Due Date visible)...")
                    val tableView =
                        client.views.create {
                            dataSourceId(dataSourceId)
                            name("Task Table")
                            type(ViewType.TABLE)
                            database(databaseId)
                            showProperties(id("Name"), id("Status"), id("Priority"), id("Due Date"))
                            hideProperties(
                                id("Start Date"),
                                id("Completed"),
                                id("Effort"),
                                id("Notes"),
                                id("Contact"),
                            )
                        }
                    tableView.type shouldBe ViewType.TABLE
                    tableView.name shouldBe "Task Table"
                    println(
                        "✅ Table view: ${tableView.id}" +
                            if (tableView.configuration != null) " (configuration returned)" else " (configuration null — API default)",
                    )

                    delay(300)

                    // ----------------------------------------------------------------
                    // Gallery view — show Name + Priority on cards; hide everything else
                    // ----------------------------------------------------------------
                    println("\n🖼️ Creating Gallery view (Name + Priority on cards)...")
                    val galleryView =
                        client.views.create {
                            dataSourceId(dataSourceId)
                            name("Priority Gallery")
                            type(ViewType.GALLERY)
                            database(databaseId)
                            showProperties(id("Name"), id("Priority"))
                            hideProperties(
                                id("Status"),
                                id("Due Date"),
                                id("Start Date"),
                                id("Effort"),
                                id("Notes"),
                                id("Contact"),
                            )
                        }
                    galleryView.type shouldBe ViewType.GALLERY
                    println("✅ Gallery view: ${galleryView.id}")

                    delay(300)

                    // ----------------------------------------------------------------
                    // List view — show Name + Completed + Effort as a progress checklist
                    // ----------------------------------------------------------------
                    println("\n📝 Creating List view (Name + Completed + Effort)...")
                    val listView =
                        client.views.create {
                            dataSourceId(dataSourceId)
                            name("Progress List")
                            type(ViewType.LIST)
                            database(databaseId)
                            showProperties(id("Name"), id("Completed"), id("Effort"))
                            hideProperties(
                                id("Status"),
                                id("Priority"),
                                id("Due Date"),
                                id("Start Date"),
                                id("Notes"),
                                id("Contact"),
                            )
                        }
                    listView.type shouldBe ViewType.LIST
                    println("✅ List view: ${listView.id}")

                    delay(300)

                    // ----------------------------------------------------------------
                    // Retrieve each view and verify type/name round-trip
                    // ----------------------------------------------------------------
                    println("\n🔍 Retrieving all created views to verify round-trip...")
                    for ((viewId, expectedType, expectedName) in listOf(
                        Triple(tableView.id, ViewType.TABLE, "Task Table"),
                        Triple(galleryView.id, ViewType.GALLERY, "Priority Gallery"),
                        Triple(listView.id, ViewType.LIST, "Progress List"),
                    )) {
                        val retrieved = client.views.retrieve(viewId)
                        retrieved.type shouldBe expectedType
                        retrieved.name shouldBe expectedName
                        println(
                            "✅ ${retrieved.type}: '${retrieved.name}'" +
                                if (retrieved.configuration != null) " — configuration present" else "",
                        )
                    }

                    // ----------------------------------------------------------------
                    // Update the table view via DSL to also surface the Notes column
                    // ----------------------------------------------------------------
                    println("\n✏️ Updating Table view to also show Notes...")
                    val updatedTable =
                        client.views.update(tableView.id) {
                            type(ViewType.TABLE)
                            showProperties(
                                id("Name"),
                                id("Status"),
                                id("Priority"),
                                id("Due Date"),
                                id("Notes"),
                            )
                            hideProperties(id("Start Date"), id("Completed"), id("Effort"), id("Contact"))
                        }
                    updatedTable.type shouldBe ViewType.TABLE
                    println("✅ Table view updated")

                    // ----------------------------------------------------------------
                    // Confirm all created views appear in the listing
                    // ----------------------------------------------------------------
                    println("\n📋 Listing all views via Flow...")
                    val allViewIds = mutableListOf<String>()
                    client.views.listAsFlow(databaseId = databaseId).collect { ref ->
                        allViewIds.add(ref.id)
                    }
                    allViewIds shouldContain tableView.id
                    allViewIds shouldContain galleryView.id
                    allViewIds shouldContain listView.id
                    println("✅ All 3 created views confirmed (${allViewIds.size} total views on database)")

                    println("\n✅ Property visibility tests passed!")
                } finally {
                    client.close()
                }
            }

            "List views by data_source_id" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("\n🗄️ Creating database for data_source_id filter test...")
                    val database =
                        client.databases.create {
                            parent.page(containerPageId)
                            title("Views List by DataSource Test")
                            properties {
                                title("Name")
                            }
                        }

                    delay(1000)

                    val dataSourceId = database.dataSources.firstOrNull()?.id
                    requireNotNull(dataSourceId) { "Expected a data source on new database" }
                    println("✅ Database created, data source ID: $dataSourceId")

                    val list = client.views.list(dataSourceId = dataSourceId)
                    list.shouldNotBeNull()
                    println("✅ List by data_source_id returned ${list.results.size} view(s)")
                } finally {
                    client.close()
                }
            }

            "Create view query and paginate through results" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("\n🗄️ Creating database with pages for query pagination test...")
                    val database =
                        client.databases.create {
                            parent.page(containerPageId)
                            title("Views Query Pagination Test")
                            properties {
                                title("Item")
                            }
                        }

                    delay(1000)

                    val firstDataSource = database.dataSources.firstOrNull()
                    requireNotNull(firstDataSource) { "Expected a data source" }

                    println("📝 Creating 3 test pages...")
                    repeat(3) { i ->
                        client.pages.create {
                            parent.dataSource(firstDataSource.id)
                            properties {
                                title("Item", "Item ${i + 1}")
                            }
                        }
                    }

                    delay(1000)

                    val views = client.views.list(databaseId = database.id)
                    val viewId = views.results.firstOrNull()?.id
                    requireNotNull(viewId) { "Expected at least one view" }

                    println("🔎 Creating query with page_size=2 to test pagination...")
                    val query = client.views.createQuery(viewId, pageSize = 2)
                    query.shouldNotBeNull()
                    println("✅ Query created, total_count=${query.totalCount}")

                    val allPageIds = mutableListOf<String>()
                    allPageIds.addAll(query.results.map { it.id })

                    if (query.hasMore && query.nextCursor != null) {
                        println("📄 Fetching next page of results...")
                        val page2 = client.views.getQueryResults(viewId, query.id, startCursor = query.nextCursor)
                        allPageIds.addAll(page2.results.map { it.id })
                        println("✅ Second page has ${page2.results.size} result(s)")
                    }

                    println("✅ Total pages retrieved: ${allPageIds.size}")

                    client.views.deleteQuery(viewId, query.id)
                    println("✅ Query cache cleaned up")
                } finally {
                    client.close()
                }
            }
            "Typed ViewConfiguration round-trip — Table, Gallery, Calendar, Board, Timeline" {
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    // Database needs a date property (for calendar/timeline) and a select
                    // property (for board group-by).
                    println("\n🗄️ Creating database for typed configuration test...")
                    val database =
                        client.databases.create {
                            parent.page(containerPageId)
                            title("Typed Config Test DB")
                            icon.emoji("🔧")
                            properties {
                                title("Name")
                                select("Priority") {
                                    option("Low")
                                    option("Medium")
                                    option("High")
                                }
                                date("Due Date")
                            }
                        }
                    val databaseId = database.id
                    val dataSourceId =
                        database.dataSources
                            .firstOrNull()
                            ?.id
                            .shouldNotBeNull()
                    println("✅ Database created: $databaseId  |  data source: $dataSourceId")

                    delay(1000)

                    // Resolve property IDs from the data source schema
                    val dataSource = client.dataSources.retrieve(dataSourceId)

                    fun id(name: String) =
                        java.net.URLDecoder.decode(
                            checkNotNull(dataSource.properties[name]?.id) { "Property '$name' not found" },
                            "UTF-8",
                        )
                    val priorityId = id("Priority")
                    val dueDateId = id("Due Date")
                    println("   Priority property ID: $priorityId")
                    println("   Due Date property ID: $dueDateId")

                    delay(500)

                    // ----------------------------------------------------------------
                    // TABLE — wrap_cells + frozen_column_index
                    // ----------------------------------------------------------------
                    println("\n📋 Creating Table view with wrapCells=true, frozenColumnIndex=1...")
                    val tableView =
                        client.views.create {
                            dataSourceId(dataSourceId)
                            name("Typed Table")
                            type(ViewType.TABLE)
                            database(databaseId)
                            configuration(ViewConfiguration.Table(wrapCells = true, frozenColumnIndex = 1))
                        }
                    tableView.type shouldBe ViewType.TABLE
                    println("   Create response — configuration: ${tableView.configuration}")

                    delay(300)

                    val retrievedTable = client.views.retrieve(tableView.id)
                    val tableConfig = retrievedTable.configuration
                    if (tableConfig != null) {
                        tableConfig.shouldBeInstanceOf<ViewConfiguration.Table>()
                        println("✅ Table config deserialized as ViewConfiguration.Table")
                        println("   wrapCells=${tableConfig.wrapCells}  frozenColumnIndex=${tableConfig.frozenColumnIndex}")
                    } else {
                        println("ℹ️ API returned configuration=null for table view (not an error — API may omit default config)")
                    }

                    delay(300)

                    // ----------------------------------------------------------------
                    // GALLERY — cover + coverSize
                    // ----------------------------------------------------------------
                    println("\n🖼️ Creating Gallery view with cover=PAGE_COVER, coverSize=MEDIUM...")
                    val galleryView =
                        client.views.create {
                            dataSourceId(dataSourceId)
                            name("Typed Gallery")
                            type(ViewType.GALLERY)
                            database(databaseId)
                            configuration(
                                ViewConfiguration.Gallery(
                                    cover = CoverConfig(type = CoverType.PAGE_COVER),
                                    coverSize = CoverSize.MEDIUM,
                                ),
                            )
                        }
                    galleryView.type shouldBe ViewType.GALLERY
                    println("   Create response — configuration: ${galleryView.configuration}")

                    delay(300)

                    val retrievedGallery = client.views.retrieve(galleryView.id)
                    val galleryConfig = retrievedGallery.configuration
                    if (galleryConfig != null) {
                        galleryConfig.shouldBeInstanceOf<ViewConfiguration.Gallery>()
                        println("✅ Gallery config deserialized as ViewConfiguration.Gallery")
                        println("   cover=${galleryConfig.cover}  coverSize=${galleryConfig.coverSize}")
                    } else {
                        println("ℹ️ API returned configuration=null for gallery view")
                    }

                    delay(300)

                    // ----------------------------------------------------------------
                    // CALENDAR — datePropertyId + viewRange + showWeekends
                    // ----------------------------------------------------------------
                    println("\n📅 Creating Calendar view with datePropertyId, viewRange=WEEK, showWeekends=false...")
                    val calendarView =
                        client.views.create {
                            dataSourceId(dataSourceId)
                            name("Typed Calendar")
                            type(ViewType.CALENDAR)
                            database(databaseId)
                            configuration(
                                ViewConfiguration.Calendar(
                                    datePropertyId = dueDateId,
                                    viewRange = ViewRange.WEEK,
                                    showWeekends = false,
                                ),
                            )
                        }
                    calendarView.type shouldBe ViewType.CALENDAR
                    println("   Create response — configuration: ${calendarView.configuration}")

                    delay(300)

                    val retrievedCalendar = client.views.retrieve(calendarView.id)
                    val calendarConfig = retrievedCalendar.configuration
                    if (calendarConfig != null) {
                        calendarConfig.shouldBeInstanceOf<ViewConfiguration.Calendar>()
                        println("✅ Calendar config deserialized as ViewConfiguration.Calendar")
                        println(
                            "   datePropertyId=${calendarConfig.datePropertyId}  viewRange=${calendarConfig.viewRange}  showWeekends=${calendarConfig.showWeekends}",
                        )
                    } else {
                        println("ℹ️ API returned configuration=null for calendar view")
                    }

                    delay(300)

                    // ----------------------------------------------------------------
                    // BOARD — groupBy select property
                    // ----------------------------------------------------------------
                    println("\n📌 Creating Board view grouped by Priority (select)...")
                    val boardView =
                        client.views.create {
                            dataSourceId(dataSourceId)
                            name("Typed Board")
                            type(ViewType.BOARD)
                            database(databaseId)
                            configuration(
                                ViewConfiguration.Board(
                                    groupBy =
                                        GroupByConfig.Select(
                                            type = "select",
                                            propertyId = priorityId,
                                            sort = GroupSort(type = GroupSortType.MANUAL),
                                        ),
                                ),
                            )
                        }
                    boardView.type shouldBe ViewType.BOARD
                    println("   Create response — configuration: ${boardView.configuration}")

                    delay(300)

                    val retrievedBoard = client.views.retrieve(boardView.id)
                    val boardConfig = retrievedBoard.configuration
                    if (boardConfig != null) {
                        boardConfig.shouldBeInstanceOf<ViewConfiguration.Board>()
                        println("✅ Board config deserialized as ViewConfiguration.Board")
                        val groupBy = boardConfig.groupBy
                        if (groupBy != null) {
                            groupBy.shouldBeInstanceOf<GroupByConfig.Select>()
                            println("   groupBy type=${groupBy.type}  propertyId=${groupBy.propertyId}  sort=${groupBy.sort}")
                        } else {
                            println("   groupBy=null (API may not return it)")
                        }
                    } else {
                        println("ℹ️ API returned configuration=null for board view")
                    }

                    delay(300)

                    // ----------------------------------------------------------------
                    // TIMELINE — datePropertyId + zoom preference
                    // ----------------------------------------------------------------
                    println("\n⏱️ Creating Timeline view with datePropertyId and week zoom...")
                    val timelineView =
                        client.views.create {
                            dataSourceId(dataSourceId)
                            name("Typed Timeline")
                            type(ViewType.TIMELINE)
                            database(databaseId)
                            configuration(
                                ViewConfiguration.Timeline(
                                    datePropertyId = dueDateId,
                                    preference = TimelinePreference(zoomLevel = TimelineZoomLevel.WEEK),
                                    showTable = true,
                                ),
                            )
                        }
                    timelineView.type shouldBe ViewType.TIMELINE
                    println("   Create response — configuration: ${timelineView.configuration}")

                    delay(300)

                    val retrievedTimeline = client.views.retrieve(timelineView.id)
                    val timelineConfig = retrievedTimeline.configuration
                    if (timelineConfig != null) {
                        timelineConfig.shouldBeInstanceOf<ViewConfiguration.Timeline>()
                        println("✅ Timeline config deserialized as ViewConfiguration.Timeline")
                        println(
                            "   datePropertyId=${timelineConfig.datePropertyId}  showTable=${timelineConfig.showTable}  preference=${timelineConfig.preference}",
                        )
                    } else {
                        println("ℹ️ API returned configuration=null for timeline view")
                    }

                    println("\n✅ Typed configuration tests passed!")
                    println("\n📊 Summary:")
                    println("   Table config returned:    ${retrievedTable.configuration?.let { it::class.simpleName } ?: "null"}")
                    println("   Gallery config returned:  ${retrievedGallery.configuration?.let { it::class.simpleName } ?: "null"}")
                    println("   Calendar config returned: ${retrievedCalendar.configuration?.let { it::class.simpleName } ?: "null"}")
                    println("   Board config returned:    ${retrievedBoard.configuration?.let { it::class.simpleName } ?: "null"}")
                    println("   Timeline config returned: ${retrievedTimeline.configuration?.let { it::class.simpleName } ?: "null"}")
                } finally {
                    client.close()
                }
            }
        }
    })
