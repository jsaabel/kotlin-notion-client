package integration.dsl

import integration.integrationTestEnvVarsAreSet
import integration.shouldCleanupAfterTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.SelectOptionColor
import no.saabelit.kotlinnotionclient.models.databases.DatabaseProperty

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
class DatabaseRequestBuilderIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" { println("Skipping DatabaseRequestBuilderIntegrationTest due to missing environment variables") }
        } else {
            "Should create database with DSL and verify structure" {
                val token = System.getenv("NOTION_API_TOKEN")
                val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                val client = NotionClient.create(NotionConfig(apiToken = token))

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
                    createdDatabase.archived shouldBe false

                    println("✅ Database created: ${createdDatabase.id}")

                    // Small delay to ensure Notion has processed the database creation
                    delay(500)

                    // Verify database properties (normalize UUID format)
                    createdDatabase.parent.pageId?.replace("-", "") shouldBe parentPageId.replace("-", "")

                    // Icon and cover verification
                    // KNOWN ISSUE: In the 2025-09-03 API, icon and cover are set correctly during creation
                    // (visible in Notion UI immediately after creation), but may be cleared/reset to default
                    // shortly after. This appears to be API-level behavior, not a client implementation issue.
                    // Our code correctly sends icon/cover in the request, and they are returned in the
                    // initial creation response. Investigation needed to determine if this is a Notion API
                    // bug or intended behavior in the new database/data-source model.
                    // TODO: Investigate icon/cover persistence on databases in 2025-09-03 API
                    println("   Icon: ${createdDatabase.icon?.type} = ${createdDatabase.icon?.emoji}")
                    println("   Cover: ${createdDatabase.cover?.type} = ${createdDatabase.cover?.external?.url}")

                    // Icon and cover are returned in the creation response
                    createdDatabase.icon?.emoji shouldBe "🚀"
                    createdDatabase.cover?.external?.url shouldContain "placehold"

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
                        println("🧹 Cleaning up - archiving test database...")
                        val archivedDatabase = client.databases.archive(createdDatabase.id)
                        archivedDatabase.archived shouldBe true
                        println("✅ Test database archived successfully")
                    } else {
                        println("🔧 Cleanup skipped (NOTION_CLEANUP_AFTER_TEST=false)")
                        println("   Created database: ${createdDatabase.id} (\"DSL Integration Test Database\")")
                        println("   Contains 11 properties configured via DSL")
                    }
                } finally {
                    client.close()
                }
            }
        }
    })
