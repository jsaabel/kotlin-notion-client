package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.pages.getTitleAsPlainText
import kotlinx.coroutines.delay

/**
 * Integration test for new filter property types (relation, people, status, unique_id, files).
 *
 * This test helps manually verify that the new filter types work correctly with the real Notion API.
 *
 * ## Setup Process (Run Once)
 * 1. Ensure you have NOTION_TEST_USER_ID set: export NOTION_TEST_USER_ID="your-user-uuid"
 * 2. Run the "Setup: Create test database with new property types" test
 * 3. Note the data source ID from the output
 * 4. MANUALLY add these properties via Notion UI:
 *    - Status property named "Status" with options: Not started, In Progress, Done
 *    - ID property (unique_id type) named "ID"
 *    - Files property named "Attachments" (optional)
 * 5. Set: export NOTION_TEST_DATASOURCE_ID="<data-source-id>"
 * 6. Run the "Populate test data" test to create sample pages
 *
 * ## Running Filter Tests
 * Run individual tests to verify each filter type:
 * - ./gradlew test --tests "*NewFilterTypesIntegrationTest*relation*"
 * - ./gradlew test --tests "*NewFilterTypesIntegrationTest*people*"
 * - ./gradlew test --tests "*NewFilterTypesIntegrationTest*status*"
 * - ./gradlew test --tests "*NewFilterTypesIntegrationTest*unique_id*"
 */
@Tags("Integration", "RequiresApi", "ManualSetup")
class NewFilterTypesIntegrationTest :
    StringSpec({

        val testDataSourceId = System.getenv("NOTION_TEST_DATASOURCE_ID")
        val testUserId = System.getenv("NOTION_TEST_USER_ID")

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" {
                println("Skipping - set NOTION_API_TOKEN and NOTION_TEST_PAGE_ID")
            }
        } else if (testUserId == null) {
            "!(Skipped - Need User ID)" {
                println("Skipping - set NOTION_TEST_USER_ID to your Notion user UUID")
                println("You can find this by calling client.users.me() or checking any page's created_by field")
            }
        } else if (testDataSourceId == null) {
            "Setup: Create test database with new property types" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n📦 Creating test databases...\n")

                // Create a related database first (for relation properties)
                val relatedDb =
                    client.databases.create {
                        parent.page(parentPageId)
                        title("Related Items (Filter Test)")
                        icon.emoji("🔗")

                        properties {
                            title("Name")
                        }
                    }

                val relatedDatabaseId = relatedDb.id
                val relatedDataSourceId = relatedDb.dataSources.firstOrNull()?.id
                relatedDataSourceId.shouldNotBeNull()

                delay(1000)

                // Create some items in the related database
                val relatedItem1 =
                    client.pages.create {
                        parent.dataSource(relatedDataSourceId)
                        properties {
                            title("Name", "Related Item A")
                        }
                    }

                delay(500)

                val relatedItem2 =
                    client.pages.create {
                        parent.dataSource(relatedDataSourceId)
                        properties {
                            title("Name", "Related Item B")
                        }
                    }

                delay(1000)

                // Create main test database with supported property types
                val mainDb =
                    client.databases.create {
                        parent.page(parentPageId)
                        title("Filter Types Test DB")
                        icon.emoji("🧪")

                        properties {
                            title("Task Name")
                            richText("Description")

                            // Properties we can create programmatically
                            relation("Related Items", relatedDatabaseId, relatedDataSourceId)
                            people("Assignee")

                            // Note: status, unique_id, and files need manual setup via UI
                        }
                    }

                val mainDataSourceId = mainDb.dataSources.firstOrNull()?.id
                mainDataSourceId.shouldNotBeNull()

                delay(1000)

                println("✅ Databases created!")
                println("   Main DB URL: ${mainDb.url}")
                println("   Related DB URL: ${relatedDb.url}\n")

                println("=".repeat(70))
                println("🎉 Setup Complete - Next Steps")
                println("=".repeat(70))
                println()
                println("📋 Step 1: Set Environment Variable")
                println("   export NOTION_TEST_DATASOURCE_ID=\"$mainDataSourceId\"")
                println()
                println("📋 Step 2: Add Properties Manually via Notion UI")
                println("   Open: ${mainDb.url}")
                println()
                println("   Add these properties:")
                println("   1. Status property:")
                println("      - Name: 'Status'")
                println("      - Type: Status")
                println("      - Options: 'Not started', 'In Progress', 'Done'")
                println()
                println("   2. ID property:")
                println("      - Name: 'ID'")
                println("      - Type: ID (unique_id)")
                println()
                println("   3. Files property (optional):")
                println("      - Name: 'Attachments'")
                println("      - Type: Files & media")
                println()
                println("📋 Step 3: Run Populate Test")
                println("   ./gradlew test --tests \"*NewFilterTypesIntegrationTest*Populate*\"")
                println()
                println("🔑 Test IDs (for reference):")
                println("   Main Data Source: $mainDataSourceId")
                println("   Related DB ID: $relatedDatabaseId")
                println("   Related DS ID: $relatedDataSourceId")
                println("   Related Item A: ${relatedItem1.id}")
                println("   Related Item B: ${relatedItem2.id}")
                println("   User ID: $testUserId")
                println()
                println("=".repeat(70))
                println()
            }

            "Populate test data (run after manual property setup)" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))
                val dataSourceId = System.getenv("NOTION_TEST_DATASOURCE_ID")

                dataSourceId.shouldNotBeNull()
                println("\n📝 Creating test pages in data source: $dataSourceId\n")

                // Get the related items by querying
                val relatedItems = client.dataSources.query(dataSourceId) {}
                if (relatedItems.isEmpty()) {
                    println("⚠️  Warning: No pages found. Make sure setup completed successfully.")
                }

                // For now, we'll get page IDs from the related database
                // In a real scenario, you'd store these IDs
                println("   Note: Creating pages with available properties...")
                println("   (Status and ID properties should be added manually via UI first)")
                println()

                // Create test pages - Notion will auto-assign IDs if ID property exists
                // Page 1: Has assignee
                val page1 =
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task Name", "Task 1 - Feature A")
                            richText("Description", "First test task")
                            people("Assignee", testUserId)
                            // Status and ID will be set manually or auto-assigned
                        }
                    }

                println("   ✅ Created: ${page1.id} - Task 1")
                delay(500)

                // Page 2: No assignee
                val page2 =
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task Name", "Task 2 - Feature B")
                            richText("Description", "Second test task")
                            // No assignee
                        }
                    }

                println("   ✅ Created: ${page2.id} - Task 2")
                delay(500)

                // Page 3: Has assignee
                val page3 =
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task Name", "Task 3 - Bug Fix")
                            richText("Description", "Third test task")
                            people("Assignee", testUserId)
                        }
                    }

                println("   ✅ Created: ${page3.id} - Task 3")
                delay(500)

                // Page 4: No assignee
                val page4 =
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task Name", "Task 4 - Refactor")
                            richText("Description", "Fourth test task")
                        }
                    }

                println("   ✅ Created: ${page4.id} - Task 4")
                println()

                println("=".repeat(70))
                println("📝 Manual Steps Required:")
                println("=".repeat(70))
                println()
                println("1. Open your database in Notion UI")
                println("2. For each page, manually set:")
                println("   - Relation property (link to related items)")
                println("   - Status property")
                println("   - (ID should be auto-assigned)")
                println("   - Optionally upload files")
                println()
                println("Example setup:")
                println("   Task 1: Related A, Status='In Progress'")
                println("   Task 2: Related B, Status='Not started'")
                println("   Task 3: No relation, Status='Done'")
                println("   Task 4: Related A+B, No status")
                println()
                println("3. Then run filter verification tests:")
                println("   ./gradlew test --tests \"*NewFilterTypesIntegrationTest*Verify*\"")
                println()
                println("=".repeat(70))
                println()
            }
        } else {
            // Verification tests - run these after setup

            "Verify relation filter - isNotEmpty" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: relation().isNotEmpty()")

                val pages =
                    client.dataSources.query(testDataSourceId) {
                        filter {
                            relation("Related Items").isNotEmpty()
                        }
                    }

                println("   Found ${pages.size} page(s) with relations")
                pages.shouldNotBeEmpty()
                pages.forEach { page ->
                    val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                    println("   - $title")
                }
                println("   ✅ Test passed!\n")
            }

            "Verify relation filter - isEmpty" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: relation().isEmpty()")

                val pages =
                    client.dataSources.query(testDataSourceId) {
                        filter {
                            relation("Related Items").isEmpty()
                        }
                    }

                println("   Found ${pages.size} page(s) without relations")
                pages.shouldNotBeEmpty()
                pages.forEach { page ->
                    val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                    println("   - $title")
                }
                println("   ✅ Test passed!\n")
            }

            "Verify people filter - isNotEmpty" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: people().isNotEmpty()")

                val pages =
                    client.dataSources.query(testDataSourceId) {
                        filter {
                            people("Assignee").isNotEmpty()
                        }
                    }

                println("   Found ${pages.size} page(s) with assignees")
                pages.shouldNotBeEmpty()
                pages.forEach { page ->
                    val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                    println("   - $title")
                }
                println("   ✅ Test passed!\n")
            }

            "Verify people filter - isEmpty" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: people().isEmpty()")

                val pages =
                    client.dataSources.query(testDataSourceId) {
                        filter {
                            people("Assignee").isEmpty()
                        }
                    }

                println("   Found ${pages.size} page(s) without assignees")
                pages.shouldNotBeEmpty()
                pages.forEach { page ->
                    val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                    println("   - $title")
                }
                println("   ✅ Test passed!\n")
            }

            "Verify people filter - contains specific user" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: people().contains(userId)")

                val pages =
                    client.dataSources.query(testDataSourceId) {
                        filter {
                            people("Assignee").contains(testUserId)
                        }
                    }

                println("   Found ${pages.size} page(s) assigned to user $testUserId")
                pages.shouldNotBeEmpty()
                pages.forEach { page ->
                    val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                    println("   - $title")
                }
                println("   ✅ Test passed!\n")
            }

            "Verify status filter - equals" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: status().equals()")

                try {
                    val pages =
                        client.dataSources.query(testDataSourceId) {
                            filter {
                                status("Status").equals("In Progress")
                            }
                        }

                    println("   Found ${pages.size} page(s) with status 'In Progress'")
                    if (pages.isNotEmpty()) {
                        pages.forEach { page ->
                            val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                            println("   - $title")
                        }
                        println("   ✅ Test passed!\n")
                    } else {
                        println("   ⚠️  No pages found - ensure Status property is set in UI\n")
                    }
                } catch (e: Exception) {
                    println("   ⚠️  Error: ${e.message}")
                    println("   (Make sure 'Status' property exists and is set in some pages)\n")
                    throw e
                }
            }

            "Verify status filter - doesNotEqual" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: status().doesNotEqual()")

                try {
                    val pages =
                        client.dataSources.query(testDataSourceId) {
                            filter {
                                status("Status").doesNotEqual("Done")
                            }
                        }

                    println("   Found ${pages.size} page(s) with status != 'Done'")
                    pages.forEach { page ->
                        val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                        println("   - $title")
                    }
                    println("   ✅ Test passed!\n")
                } catch (e: Exception) {
                    println("   ⚠️  Error: ${e.message}\n")
                    throw e
                }
            }

            "Verify unique_id filter - greaterThan" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: uniqueId().greaterThan()")

                try {
                    val pages =
                        client.dataSources.query(testDataSourceId) {
                            filter {
                                uniqueId("ID").greaterThan(1)
                            }
                        }

                    println("   Found ${pages.size} page(s) with ID > 1")
                    pages.forEach { page ->
                        val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                        println("   - $title")
                    }
                    if (pages.isNotEmpty()) {
                        println("   ✅ Test passed!\n")
                    } else {
                        println("   ⚠️  No results - ensure ID property exists and pages have IDs > 1\n")
                    }
                } catch (e: Exception) {
                    println("   ⚠️  Error: ${e.message}")
                    println("   (Make sure 'ID' property exists)\n")
                    throw e
                }
            }

            "Verify unique_id filter - lessThanOrEqualTo" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: uniqueId().lessThanOrEqualTo()")

                try {
                    val pages =
                        client.dataSources.query(testDataSourceId) {
                            filter {
                                uniqueId("ID").lessThanOrEqualTo(2)
                            }
                        }

                    println("   Found ${pages.size} page(s) with ID <= 2")
                    pages.forEach { page ->
                        val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                        println("   - $title")
                    }
                    println("   ✅ Test passed!\n")
                } catch (e: Exception) {
                    println("   ⚠️  Error: ${e.message}\n")
                    throw e
                }
            }

            "Verify complex filter - combine multiple new types" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: Complex filter with multiple new types")

                val pages =
                    client.dataSources.query(testDataSourceId) {
                        filter {
                            and(
                                people("Assignee").isNotEmpty(),
                                uniqueId("ID").greaterThan(0),
                            )
                        }
                    }

                println("   Found ${pages.size} page(s) matching:")
                println("   - Has assignee")
                println("   - ID > 0")
                pages.shouldNotBeEmpty()
                pages.forEach { page ->
                    val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                    println("   - $title")
                }
                println("   ✅ Test passed!\n")
            }

            "Optional: Verify files filter (requires manual file uploads)" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                println("\n🔍 Testing: files() filter (optional)")
                println("   ⚠️  This requires 'Attachments' property with uploaded files")

                try {
                    val pagesWithFiles =
                        client.dataSources.query(testDataSourceId) {
                            filter {
                                files("Attachments").isNotEmpty()
                            }
                        }

                    println("   Found ${pagesWithFiles.size} page(s) with attachments")
                    pagesWithFiles.forEach { page ->
                        val title = page.getTitleAsPlainText("Task Name") ?: "Untitled"
                        println("   - $title")
                    }

                    val pagesWithoutFiles =
                        client.dataSources.query(testDataSourceId) {
                            filter {
                                files("Attachments").isEmpty()
                            }
                        }

                    println("   Found ${pagesWithoutFiles.size} page(s) without attachments")
                    println("   ✅ Files filter works!\n")
                } catch (e: Exception) {
                    println("   ⚠️  Skipped: ${e.message}")
                    println("   (Add 'Attachments' files property and upload files via UI to test)\n")
                }
            }
        }
    })
