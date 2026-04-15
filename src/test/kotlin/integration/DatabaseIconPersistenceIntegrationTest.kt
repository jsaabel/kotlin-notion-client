package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon
import kotlinx.coroutines.delay

/**
 * Diagnostic integration test for the suspected bug where a database icon
 * disappears after creation (IDEAS.md #6).
 *
 * Hypothesis: In the 2025-09-03 API model the database object is a thin
 * *container*; what Notion renders in the UI at the database URL is the
 * *data source* page. The icon set via `databases.create { icon.emoji(...) }`
 * lands on the container, but the data source has its own `icon` field that
 * starts as null. Notion's UI shows the data source icon, so it looks like
 * the icon disappears even though the container still has it.
 *
 * This test:
 *   1. Creates a database with an emoji icon on the container
 *   2. Immediately retrieves the data source and reports its icon
 *   3. Sets the same icon directly on the data source via update
 *   4. Checks whether the icon is now visible in the UI
 *
 * Set NOTION_CLEANUP_AFTER_TEST=false to keep both objects alive for inspection.
 *
 * Run with: ./gradlew integrationTest --tests "*DatabaseIconPersistenceIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class DatabaseIconPersistenceIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) database icon persistence" {
                println("Skipping DatabaseIconPersistenceIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))
            val observe = !shouldCleanupAfterTest()
            val stepPause = if (observe) 5000L else 1000L

            "diagnose icon placement: container vs data source" {
                println("Creating database with emoji icon 🧪 on the container...")
                val database =
                    notion.databases.create {
                        parent.page(parentPageId)
                        title("Icon Persistence — Diagnosis")
                        icon.emoji("🧪")
                        properties {
                            title("Name")
                        }
                    }

                val dataSourceId = database.dataSources.first().id
                println("  Database container URL : ${database.url}")
                println("  Database container icon: ${database.icon}")
                println("  Data source ID         : $dataSourceId")
                if (observe) println("  👀 Open the database URL and watch the icon")
                delay(stepPause)

                // Step 1 — what does the data source report for its icon right now?
                val dsAfterCreate = notion.dataSources.retrieve(dataSourceId)
                println("\nData source after create:")
                println("  Data source URL : ${dsAfterCreate.url}")
                println("  Data source icon: ${dsAfterCreate.icon}   ← null means the UI shows no icon")

                // Step 2 — re-retrieve the database container to confirm its icon is still there
                val dbAfterCreate = notion.databases.retrieve(database.id)
                println("\nDatabase container after retrieve:")
                println("  Container icon  : ${dbAfterCreate.icon}   ← should still be 🧪")
                delay(stepPause)

                // Step 3 — set the icon directly on the data source and observe
                println("\nSetting icon 🧪 directly on the data source...")
                if (observe) println("  👀 Watch the UI — icon should now appear and stay")
                val updatedDs =
                    notion.dataSources.update(dataSourceId) {
                        icon.emoji("🧪")
                    }
                println("  Data source icon after update: ${updatedDs.icon}")
                delay(stepPause)

                // Step 4 — final retrieve of both to confirm steady state
                val dsFinal = notion.dataSources.retrieve(dataSourceId)
                val dbFinal = notion.databases.retrieve(database.id)
                println("\nFinal state:")
                println("  Container icon  : ${dbFinal.icon}")
                println("  Data source icon: ${dsFinal.icon}")

                if (observe) {
                    println("\n  Database URL (keep open for inspection): ${database.url}")
                    println("  Data source URL                        : ${dsAfterCreate.url}")
                } else {
                    notion.databases.trash(database.id)
                    println("\n  Cleaned up.")
                }
            }
        }
    })
