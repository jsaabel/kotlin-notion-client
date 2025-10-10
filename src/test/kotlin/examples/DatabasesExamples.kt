package examples

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import kotlinx.coroutines.delay

/**
 * Validation tests for all code examples in docs/databases.md
 *
 * These tests ensure that every code example in the documentation:
 * 1. Compiles successfully
 * 2. Runs against the real Notion API
 * 3. Produces expected results
 *
 * Prerequisites:
 * - export NOTION_RUN_INTEGRATION_TESTS=true
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="page-id"
 *
 * Run with: ./gradlew test --tests "*DatabasesExamples"
 */
@Tags("Integration", "RequiresApi", "Examples")
class DatabasesExamples :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) Databases examples" {
                println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true and required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            // Test data setup
            val createdDatabases = mutableListOf<String>()

            beforeSpec {
                println("🔧 Setting up tests for databases examples...")
            }

            afterSpec {
                if (shouldCleanupAfterTest() && createdDatabases.isNotEmpty()) {
                    println("🧹 Cleaning up test databases...")
                    try {
                        createdDatabases.forEach { dbId ->
                            notion.databases.archive(dbId)
                        }
                        println("✅ Cleanup complete")
                    } catch (e: Exception) {
                        println("⚠️ Cleanup failed: ${e.message}")
                    }
                }
            }

            // ========================================
            // Example 1: Retrieve a Database
            // ========================================
            "Example 1: Retrieve a database" {
                println("\n📖 Running Example 1: Retrieve a database")

                // First create a database to retrieve
                val db =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Test Database for Retrieval")
                        properties {
                            title("Name")
                        }
                    }
                createdDatabases.add(db.id)
                delay(1000)

                // Now retrieve it
                val database = notion.databases.retrieve(db.id)

                // Access container properties
                val title = database.title.firstOrNull()?.plainText ?: "Untitled"
                println("Database: $title")
                println("Created: ${database.createdTime}")
                println("Archived: ${database.archived}")

                // Get data sources within this database
                database.dataSources.forEach { dataSourceRef ->
                    println("Data source ID: ${dataSourceRef.id}")
                }

                // Typically you'll want the first data source ID
                val dataSourceId = database.dataSources.firstOrNull()?.id

                // Validation
                database.shouldNotBeNull()
                title shouldBe "Test Database for Retrieval"
                database.archived shouldBe false
                dataSourceId.shouldNotBeNull()

                println("✅ Example 1 passed")
            }

            // ========================================
            // Example 2: Create a Simple Database
            // ========================================
            "Example 2: Create a simple database" {
                println("\n📖 Running Example 2: Create a simple database")

                val database =
                    notion.databases.create {
                        // Specify parent page
                        parent.page(parentPageId)

                        // Set database title
                        title("Project Tracker")

                        // Define initial schema (the first table/data source)
                        properties {
                            title("Task Name")
                            select("Status") {
                                option("To Do")
                                option("In Progress")
                                option("Done")
                            }
                            people("Assignee")
                            date("Due Date")
                            checkbox("Completed")
                        }
                    }
                createdDatabases.add(database.id)

                println("Created database: ${database.id}")

                // Get the data source ID to start adding pages
                val dataSourceId = database.dataSources.firstOrNull()?.id

                // Validation
                database.shouldNotBeNull()
                val title = database.title.firstOrNull()?.plainText
                title shouldBe "Project Tracker"
                dataSourceId.shouldNotBeNull()

                println("✅ Example 2 passed")
            }

            // ========================================
            // Example 3: Create a Database with Rich Schema
            // ========================================
            "Example 3: Create a database with rich schema" {
                println("\n📖 Running Example 3: Create database with rich schema")

                val database =
                    notion.databases.create {
                        parent.page(parentPageId)

                        title("Product Roadmap")
                        description("Track features and releases")

                        properties {
                            // Title property (required - usually first)
                            title("Feature Name")

                            // Text properties
                            richText("Description")

                            // Number properties
                            number("Story Points")
                            number("Budget", format = "dollar")

                            // Selection properties
                            select("Priority") {
                                option("Critical", SelectOptionColor.RED)
                                option("High", SelectOptionColor.ORANGE)
                                option("Medium", SelectOptionColor.YELLOW)
                                option("Low", SelectOptionColor.GREEN)
                            }

                            multiSelect("Tags") {
                                option("Frontend", SelectOptionColor.BLUE)
                                option("Backend", SelectOptionColor.PURPLE)
                                option("Infrastructure", SelectOptionColor.GRAY)
                            }

                            // Date properties
                            date("Target Date")
                            date("Completed Date")

                            // People properties
                            people("Owner")
                            people("Team Members")

                            // Other properties
                            checkbox("Is Launched")
                            url("Documentation Link")
                            email("Contact")
                        }

                        // Optional: Add icon and cover
                        icon.emoji("🗺️")
                        cover.external("https://images.unsplash.com/photo-1557683316-973673baf926")
                    }
                createdDatabases.add(database.id)

                // Validation
                database.shouldNotBeNull()
                val title = database.title.firstOrNull()?.plainText
                title shouldBe "Product Roadmap"
                database.icon?.emoji shouldBe "🗺️"
                database.cover?.external?.url shouldBe "https://images.unsplash.com/photo-1557683316-973673baf926"

                // Verify data source was created
                val dataSource = notion.dataSources.retrieve(database.dataSources.first().id)
                dataSource.properties.keys.shouldNotBeEmpty()

                println("✅ Example 3 passed")
            }

            // ========================================
            // Example 4: Create a Database with Relation
            // ========================================
            "Example 4: Create a database with relation" {
                println("\n📖 Running Example 4: Create database with relation")

                // First, create a Projects database
                val projectsDb =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Projects")
                        properties {
                            title("Project Name")
                            select("Status") {
                                option("Active")
                                option("Completed")
                            }
                        }
                    }
                createdDatabases.add(projectsDb.id)

                delay(1000) // Allow API to process

                // Get the data source ID from the projects database (needed for relation)
                val projectsDataSourceId = projectsDb.dataSources.first().id

                // Then create a Tasks database with a relation to Projects
                val tasksDb =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Tasks")
                        properties {
                            title("Task Name")

                            // Relation to the Projects database (requires both database and data source IDs)
                            relation("Project", projectsDb.id, projectsDataSourceId) {
                                dual("Related Tasks") // Creates bidirectional relation
                            }

                            select("Status") {
                                option("To Do")
                                option("Done")
                            }
                        }
                    }
                createdDatabases.add(tasksDb.id)

                // Validation
                projectsDb.shouldNotBeNull()
                tasksDb.shouldNotBeNull()

                val projectsTitle = projectsDb.title.firstOrNull()?.plainText
                val tasksTitle = tasksDb.title.firstOrNull()?.plainText

                projectsTitle shouldBe "Projects"
                tasksTitle shouldBe "Tasks"

                println("✅ Example 4 passed")
            }

            // ========================================
            // Example 5: Archive a Database
            // ========================================
            "Example 5: Archive a database" {
                println("\n📖 Running Example 5: Archive a database")

                // Create a database to archive
                val db =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Database to Archive")
                        properties {
                            title("Name")
                        }
                    }
                // Don't add to cleanup list since we're archiving it

                delay(1000)

                val archived = notion.databases.archive(db.id)

                println("Database in trash: ${archived.inTrash}")

                // Validation
                archived.shouldNotBeNull()
                // Note: The API may not immediately reflect the status.
                // Verify by retrieving the database again
                delay(1000)
                val retrieved = notion.databases.retrieve(db.id)
                retrieved.inTrash shouldBe true

                println("✅ Example 5 passed")
            }

            // ========================================
            // Example 6: Get Data Source ID from Created Database
            // ========================================
            "Example 6: Get data source ID from created database" {
                println("\n📖 Running Example 6: Get data source ID")

                val database =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("My Database")
                        properties {
                            title("Name")
                        }
                    }
                createdDatabases.add(database.id)

                // Get the first (and usually only) data source
                val dataSourceId =
                    database.dataSources.firstOrNull()?.id
                        ?: error("Database has no data sources")

                // Now you can query it or create pages in it
                val pages = notion.dataSources.query(dataSourceId) {}

                // Validation
                dataSourceId.shouldNotBeNull()
                pages.shouldNotBeNull()

                println("✅ Example 6 passed")
            }

            // ========================================
            // Example 7: Create Database and Add Initial Pages
            // ========================================
            "Example 7: Create database and add initial pages" {
                println("\n📖 Running Example 7: Create database and add pages")

                // Step 1: Create database
                val database =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Team Tasks")
                        properties {
                            title("Task")
                            select("Status") {
                                option("To Do")
                                option("Done")
                            }
                        }
                    }
                createdDatabases.add(database.id)

                val dataSourceId = database.dataSources.first().id

                delay(1000) // Allow database to be fully created

                // Step 2: Add some initial pages
                val tasks = listOf("Setup project", "Write docs", "Deploy")
                tasks.forEach { taskName ->
                    notion.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Task", taskName)
                            select("Status", "To Do")
                        }
                    }
                }

                delay(1000) // Allow pages to be created

                // Validation - query to verify pages were created
                val createdPages = notion.dataSources.query(dataSourceId) {}
                createdPages shouldHaveSize 3

                println("✅ Example 7 passed")
            }

            // ========================================
            // Example 8: Check If Database is Archived
            // ========================================
            "Example 8: Check if database is archived" {
                println("\n📖 Running Example 8: Check archived status")

                // Create a database
                val db =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Test Active Database")
                        properties {
                            title("Name")
                        }
                    }
                createdDatabases.add(db.id)

                delay(1000)

                val database = notion.databases.retrieve(db.id)

                if (database.inTrash) {
                    println("This database is in trash")
                } else {
                    println("Database is active")
                }

                // Validation
                database.inTrash shouldBe false

                println("✅ Example 8 passed")
            }
        }
    })
