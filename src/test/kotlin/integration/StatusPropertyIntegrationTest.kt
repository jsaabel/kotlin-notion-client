package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.databases.DatabaseProperty
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import kotlinx.coroutines.delay

/**
 * Integration tests for the Status database property.
 *
 * Note: Status properties cannot be updated via the API (unlike select/multi-select).
 * Options and group assignments can only be changed from the Notion UI after creation.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 *
 * Run with: ./gradlew integrationTest --tests "*StatusPropertyIntegrationTest"
*/
@Tags("Integration", "RequiresApi")
class StatusPropertyIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) status property integration" {
                println("Skipping StatusPropertyIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            "should create status property with default options and verify standard groups" {
                println("📊 Creating database with default status options...")
                val database =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Status Default Options Test")
                        icon.emoji("🔖")
                        properties {
                            title("Name")
                            status("Stage")
                        }
                    }
                println("✅ Database: https://notion.so/${database.id.replace("-", "")}")

                delay(500)

                val dataSource = notion.dataSources.retrieve(database.dataSources.first().id)
                val stageProp = dataSource.properties["Stage"] as DatabaseProperty.Status

                val optionNames = stageProp.status.options.map { it.name }
                optionNames shouldContain "Not started"
                optionNames shouldContain "In progress"
                optionNames shouldContain "Done"
                println("✅ Default options: $optionNames")

                val groupNames = stageProp.status.groups.map { it.name }
                groupNames shouldContain "To-do"
                groupNames shouldContain "In progress"
                groupNames shouldContain "Complete"
                println("✅ Default groups: $groupNames")

                // Verify each option is in the expected group
                val notStartedId =
                    stageProp.status.options
                        .first { it.name == "Not started" }
                        .id
                val inProgressId =
                    stageProp.status.options
                        .first { it.name == "In progress" }
                        .id
                val doneId =
                    stageProp.status.options
                        .first { it.name == "Done" }
                        .id

                stageProp.status.groups
                    .first { it.name == "To-do" }
                    .optionIds shouldContain notStartedId
                stageProp.status.groups
                    .first { it.name == "In progress" }
                    .optionIds shouldContain inProgressId
                stageProp.status.groups
                    .first { it.name == "Complete" }
                    .optionIds shouldContain doneId
                println("✅ Options are in the correct groups")

                delay(500)

                println("📄 Creating pages with different status values...")
                val page1 =
                    notion.pages.create {
                        parent.dataSource(dataSource.id)
                        properties {
                            title("Name", "Task A")
                            status("Stage", "Not started")
                        }
                    }
                val page2 =
                    notion.pages.create {
                        parent.dataSource(dataSource.id)
                        properties {
                            title("Name", "Task B")
                            status("Stage", "Done")
                        }
                    }

                delay(500)

                val retrieved1 = notion.pages.retrieve(page1.id)
                (retrieved1.properties["Stage"] as PageProperty.Status).status?.name shouldBe "Not started"
                val retrieved2 = notion.pages.retrieve(page2.id)
                (retrieved2.properties["Stage"] as PageProperty.Status).status?.name shouldBe "Done"
                println("✅ Page status values read back correctly")

                if (shouldCleanupAfterTest()) {
                    notion.databases.trash(database.id)
                    println("🧹 Cleaned up")
                }
            }

            // Note: all custom options appear in the "To-do" group by default.
            // Groups cannot be rearranged via the API — only via the Notion UI.
            "should create status property with custom options and verify pages use those options" {
                println("📊 Creating database with custom status options...")
                val database =
                    notion.databases.create {
                        parent.page(parentPageId)
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
                println("✅ Database: https://notion.so/${database.id.replace("-", "")}")

                delay(500)

                val dataSource = notion.dataSources.retrieve(database.dataSources.first().id)
                val stageProp = dataSource.properties["Stage"] as DatabaseProperty.Status

                val optionNames = stageProp.status.options.map { it.name }
                optionNames shouldContain "Backlog"
                optionNames shouldContain "In Review"
                optionNames shouldContain "Shipped"
                println("✅ Custom options: $optionNames")

                val backlogOption = stageProp.status.options.first { it.name == "Backlog" }
                backlogOption.color shouldBe SelectOptionColor.GRAY
                backlogOption.description shouldBe "Work not yet started"
                stageProp.status.options
                    .first { it.name == "In Review" }
                    .color shouldBe SelectOptionColor.YELLOW
                stageProp.status.options
                    .first { it.name == "Shipped" }
                    .color shouldBe SelectOptionColor.GREEN
                println("✅ Custom option colors and description verified")

                delay(500)

                println("📄 Creating pages with custom status values...")
                val page1 =
                    notion.pages.create {
                        parent.dataSource(dataSource.id)
                        properties {
                            title("Name", "Item A")
                            status("Stage", "Backlog")
                        }
                    }
                val page2 =
                    notion.pages.create {
                        parent.dataSource(dataSource.id)
                        properties {
                            title("Name", "Item B")
                            status("Stage", "Shipped")
                        }
                    }

                delay(500)

                val retrieved1 = notion.pages.retrieve(page1.id)
                (retrieved1.properties["Stage"] as PageProperty.Status).status?.name shouldBe "Backlog"
                val retrieved2 = notion.pages.retrieve(page2.id)
                (retrieved2.properties["Stage"] as PageProperty.Status).status?.name shouldBe "Shipped"
                println("✅ Page status values read back correctly")

                if (shouldCleanupAfterTest()) {
                    notion.databases.trash(database.id)
                    println("🧹 Cleaned up")
                }
            }
        }
    })
