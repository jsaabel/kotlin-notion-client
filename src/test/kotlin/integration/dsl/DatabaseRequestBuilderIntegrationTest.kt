package integration.dsl

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.databases.DatabaseProperty
import it.saabel.kotlinnotionclient.models.pages.PageCover
import kotlinx.coroutines.delay

/**
 * Self-contained integration test for the DatabaseRequestBuilder DSL.
 *
 * This test validates the full workflow of creating databases using the DSL,
 * uploading them to Notion, and verifying the structure.
 *
 * Prerequisites:
 * 1. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 2. Set environment variable: export NOTION_TEST_PAGE_ID="your_parent_page_id"
 * 3. Your integration should have permissions to create/read/update databases
 * 4. Optional: Set NOTION_CLEANUP_AFTER_TEST="false" to keep test objects for manual inspection
 */
@Tags("Integration", "RequiresApi")
class DatabaseRequestBuilderIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping DatabaseRequestBuilderIntegrationTest due to missing environment variables") }
        } else {
            "Should create database with DSL and verify structure" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🗄️ Creating test database with DatabaseRequestBuilder DSL...")

                    // Create database using DSL
                    val createdDatabase =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("DSL Integration Test Database")
                            description("This database was created using the DatabaseRequestBuilder DSL!")
                            icon.emoji("🚀")
                            cover.external("https://placehold.co/1200x400.png")
                            properties {
                                title("Task Name")
                                richText("Description")
                                number("Priority", format = "number")
                                checkbox("Completed")
                                select("Status") {
                                    option("To Do", SelectOptionColor.RED)
                                    option("In Progress", SelectOptionColor.YELLOW)
                                    option("Done", SelectOptionColor.GREEN)
                                }
                                multiSelect("Tags") {
                                    option("Important", SelectOptionColor.RED)
                                    option("Urgent", SelectOptionColor.ORANGE)
                                }
                                date("Due Date")
                                url("Reference URL")
                                email("Assignee Email")
                                phoneNumber("Phone")
                                people("Assignee")
                            }
                        }
                    createdDatabase.objectType shouldBe "database"
                    createdDatabase.inTrash shouldBe false

                    println("✅ Database created: ${createdDatabase.id}")

                    // Small delay to ensure Notion has processed the database creation
                    delay(500)

                    // Verify database properties (normalize UUID format)
                    createdDatabase.parent.id?.replace("-", "") shouldBe parentPageId.replace("-", "")

                    // Icon and cover verification
                    // KNOWN ISSUE: In the 2025-09-03 API, icon and cover are set correctly during creation
                    // (visible in Notion UI immediately after creation), but may be cleared/reset to default
                    // shortly after. This appears to be API-level behavior, not a client implementation issue.
                    // Our code correctly sends icon/cover in the request, and they are returned in the
                    // initial creation response. Investigation needed to determine if this is a Notion API
                    // bug or intended behavior in the new database/data-source model.
                    // TODO: Investigate icon/cover persistence on databases in 2025-09-03 API
                    println("   Icon: ${createdDatabase.icon?.type} = ${(createdDatabase.icon as? Icon.Emoji)?.emoji}")
                    println("   Cover: ${createdDatabase.cover?.type} = ${(createdDatabase.cover as? PageCover.External)?.external?.url}")

                    // Icon and cover are returned in the creation response
                    (createdDatabase.icon as? Icon.Emoji)?.emoji shouldBe "🚀"
                    (createdDatabase.cover as? PageCover.External)?.external?.url shouldContain "placehold"

                    println("   ⚠️  Note: Icon/cover may not persist in Notion UI due to 2025-09-03 API behavior")

                    // Verify the title was set correctly
                    createdDatabase.title.shouldNotBeNull()
                    createdDatabase.title.shouldHaveSize(1)
                    createdDatabase.title[0].plainText shouldBe "DSL Integration Test Database"

                    // Verify description
                    createdDatabase.description.shouldNotBeNull()
                    createdDatabase.description shouldHaveSize (1)
                    createdDatabase.description[0].plainText shouldBe "This database was created using the DatabaseRequestBuilder DSL!"

                    println("✅ Database metadata verified")

                    // Retrieve data source to verify properties (2025-09-03 API)
                    val retrievedDb = client.databases.retrieve(createdDatabase.id)
                    retrievedDb.dataSources.shouldNotBeNull()
                    val dataSource = retrievedDb.dataSources.first()

                    val dataSourceDetails = client.dataSources.retrieve(dataSource.id)
                    val properties = dataSourceDetails.properties

                    // Verify all properties were created in the data source
                    properties.size shouldBe 11
                    properties.keys shouldBe
                        setOf(
                            "Task Name",
                            "Description",
                            "Priority",
                            "Completed",
                            "Status",
                            "Tags",
                            "Due Date",
                            "Reference URL",
                            "Assignee Email",
                            "Phone",
                            "Assignee",
                        )

                    // Verify specific property types
                    properties["Task Name"].shouldBeInstanceOf<DatabaseProperty.Title>()
                    properties["Description"].shouldBeInstanceOf<DatabaseProperty.RichText>()
                    properties["Priority"].shouldBeInstanceOf<DatabaseProperty.Number>()
                    properties["Completed"].shouldBeInstanceOf<DatabaseProperty.Checkbox>()
                    properties["Status"].shouldBeInstanceOf<DatabaseProperty.Select>()
                    properties["Tags"].shouldBeInstanceOf<DatabaseProperty.MultiSelect>()
                    properties["Due Date"].shouldBeInstanceOf<DatabaseProperty.Date>()
                    properties["Reference URL"].shouldBeInstanceOf<DatabaseProperty.Url>()
                    properties["Assignee Email"].shouldBeInstanceOf<DatabaseProperty.Email>()
                    properties["Phone"].shouldBeInstanceOf<DatabaseProperty.PhoneNumber>()
                    properties["Assignee"].shouldBeInstanceOf<DatabaseProperty.People>()

                    println("✅ Data source properties verified")

                    // Verify select options
                    val statusProperty = properties["Status"] as DatabaseProperty.Select
                    statusProperty.select.options.shouldHaveSize(3)
                    statusProperty.select.options.map { it.name } shouldBe listOf("To Do", "In Progress", "Done")

                    val tagsProperty = properties["Tags"] as DatabaseProperty.MultiSelect
                    tagsProperty.multiSelect.options.shouldHaveSize(2)
                    tagsProperty.multiSelect.options.map { it.name } shouldBe listOf("Important", "Urgent")

                    println("✅ DatabaseRequestBuilder DSL integration test completed successfully!")
                    println("   - DSL created database with all components: ✅")
                    println("   - Properties configuration working: ✅")
                    println("   - Icon and cover working: ✅")
                    println("   - Type safety enforced: ✅")

                    // Conditionally clean up
                    delay(500)
                    if (shouldCleanupAfterTest()) {
                        println("🧹 Cleaning up - trashing test database...")
                        val archivedDatabase = client.databases.trash(createdDatabase.id)
                        archivedDatabase.inTrash shouldBe true
                        println("✅ Test database trashed successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created database: ${createdDatabase.id} (\"DSL Integration Test Database\")")
                        println("   Contains 11 properties configured via DSL")
                    }
                } finally {
                    client.close()
                }
            }

            "Should create database properties with descriptions and read them back" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    println("🗄️ Creating database with property-level descriptions...")

                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Property Description Test")
                            icon.emoji("📝")
                            properties {
                                title("Name")
                                richText("Notes", description = "Free-form notes about the item")
                                number("Score", description = "A numeric score from 1 to 10")
                                select("Priority", description = "Task urgency level") {
                                    option("High", SelectOptionColor.RED)
                                    option("Low", SelectOptionColor.GRAY)
                                }
                                checkbox("Done", description = "Whether the task is complete")
                            }
                        }
                    println("✅ Database created: https://notion.so/${database.id.replace("-", "")}")

                    delay(500)

                    val dataSource = client.dataSources.retrieve(database.dataSources.first().id)
                    val props = dataSource.properties

                    val notesProp = props["Notes"] as DatabaseProperty.RichText
                    notesProp.description shouldBe "Free-form notes about the item"
                    println("✅ RichText description: ${notesProp.description}")

                    val scoreProp = props["Score"] as DatabaseProperty.Number
                    scoreProp.description shouldBe "A numeric score from 1 to 10"
                    println("✅ Number description: ${scoreProp.description}")

                    val priorityProp = props["Priority"] as DatabaseProperty.Select
                    priorityProp.description shouldBe "Task urgency level"
                    println("✅ Select description: ${priorityProp.description}")

                    val doneProp = props["Done"] as DatabaseProperty.Checkbox
                    doneProp.description shouldBe "Whether the task is complete"
                    println("✅ Checkbox description: ${doneProp.description}")

                    // Property without a description should come back as null
                    val nameProp = props["Name"] as DatabaseProperty.Title
                    nameProp.description shouldBe null
                    println("✅ Title description is null as expected")

                    if (shouldCleanupAfterTest()) {
                        client.databases.trash(database.id)
                        println("🧹 Cleaned up")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
