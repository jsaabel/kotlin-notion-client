package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.getCheckboxProperty
import it.saabel.kotlinnotionclient.models.pages.getNumberProperty
import it.saabel.kotlinnotionclient.models.pages.getTitleAsPlainText
import kotlinx.coroutines.delay

/**
 * Integration test for unknown property type handling.
 *
 * Tests that pages with unsupported property types (button, unique_id, etc.)
 * can be queried successfully with unknown types deserialized as PageProperty.Unknown.
 *
 * ## How to Run
 * 1. First run creates database - note the data source ID
 * 2. Add a button property to the database via Notion UI
 * 3. Set: export NOTION_TEST_DATASOURCE_ID="<data-source-id-from-step-1>"
 * 4. Re-run to verify
 *
 * Run: ./gradlew test --tests "*UnknownPropertyTypesIntegrationTest"
 */
@Tags("Integration", "RequiresApi", "ManualSetup")
class UnknownPropertyTypesIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped)" {
                println("Skipping - set NOTION_API_TOKEN and NOTION_TEST_PAGE_ID")
            }
        } else {
            val testDataSourceId = System.getenv("NOTION_TEST_DATASOURCE_ID")

            if (testDataSourceId == null) {
                "Create test database (manual button property setup required)" {
                    val token = System.getenv("NOTION_API_TOKEN")
                    val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
                    val client = NotionClient(NotionConfig(apiToken = token))

                    val database =
                        client.databases.create {
                            parent.page(parentPageId)
                            title("Unknown Property Test")
                            icon.emoji("🧪")

                            properties {
                                title("Name")
                                richText("Description")
                                number("Score")
                                checkbox("Active")
                            }
                        }

                    val dataSourceId = database.dataSources.firstOrNull()?.id
                    dataSourceId.shouldNotBeNull()

                    delay(1000)
                    client.pages.create {
                        parent.dataSource(dataSourceId)
                        properties {
                            title("Name", "Test Item")
                            richText("Description", "Test description")
                            number("Score", 42.0)
                            checkbox("Active", true)
                        }
                    }

                    println("\n✅ Database created!")
                    println("   URL: ${database.url}")
                    println("   Data Source ID: $dataSourceId\n")
                    println("📝 Next steps:")
                    println("   1. Open: ${database.url}")
                    println("   2. Add a 'Button' property")
                    println("   3. export NOTION_TEST_DATASOURCE_ID=\"$dataSourceId\"")
                    println("   4. Re-run this test\n")
                }
            } else {
                "Verify unknown property types handled gracefully" {
                    val token = System.getenv("NOTION_API_TOKEN")
                    val client = NotionClient(NotionConfig(apiToken = token))

                    println("\n🧪 Testing with data source: $testDataSourceId\n")

                    // This should succeed even with button properties
                    val pages =
                        try {
                            client.dataSources.query(testDataSourceId) {}
                        } catch (e: Exception) {
                            println("❌ Query failed: ${e.message}")
                            println("   Expected: Success with unknown properties as Unknown type")
                            throw e
                        }

                    println("✅ Query succeeded! ${pages.size} page(s)\n")
                    pages.shouldNotBeEmpty()

                    pages.forEach { page ->
                        val title = page.getTitleAsPlainText("Name") ?: "Untitled"
                        println("Page: $title")

                        var unknownCount = 0
                        var supportedCount = 0

                        page.properties.forEach { (name, prop) ->
                            when (prop) {
                                is PageProperty.Unknown -> {
                                    unknownCount++
                                    println("   ✨ $name: ${prop.type} (unknown - handled)")
                                }
                                else -> {
                                    supportedCount++
                                    println("   ✓ $name: ${prop.type}")
                                }
                            }
                        }

                        println("\n   Summary: $supportedCount supported, $unknownCount unknown\n")

                        // Verify supported properties still work
                        val score = page.getNumberProperty("Score")
                        val active = page.getCheckboxProperty("Active")
                        score.shouldNotBeNull()
                        active.shouldNotBeNull()
                        println("   ✅ Property access works: Score=$score, Active=$active\n")
                    }

                    println("✅ Test passed - unknown types handled gracefully!")
                }
            }
        }
    })
