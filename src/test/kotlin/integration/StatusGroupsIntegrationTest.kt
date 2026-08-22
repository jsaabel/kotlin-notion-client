package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.databases.DatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.StatusOptionGroup
import kotlinx.coroutines.delay

/**
 * Integration tests for status option *group assignment* (FOLLOWUPS item 23; issue #61).
 *
 * The `group` field on status options was implemented from live docs but never exercised
 * against the real API. These tests verify that group assignment round-trips on database
 * create and on data source update, and *log* the documented-but-unratified update
 * semantics (omitted group preserves; new option defaults to "To-do" — FOLLOWUPS item 24).
 *
 * Kept as a separate spec (rather than inside DatabaseFeaturesIntegrationTest) so it can
 * be run in isolation with `--tests`.
 *
 * Prerequisites: NOTION_API_TOKEN, NOTION_TEST_PAGE_ID, NOTION_RUN_INTEGRATION_TESTS=true
 * (or a `.env` file — see .env.example).
 *
 * Run with: ./gradlew integrationTest --tests "*StatusGroupsIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class StatusGroupsIntegrationTest :
    StringSpec({

        fun DatabaseProperty.Status.groupOf(optionName: String): String? {
            val optionId = status.options.firstOrNull { it.name == optionName }?.id ?: return null
            return status.groups.firstOrNull { optionId in it.optionIds }?.name
        }

        fun describeStatus(prop: DatabaseProperty.Status) {
            println("🔎 status options: ${prop.status.options.map { "${it.name}(${it.color})" }}")
            prop.status.groups.forEach { group ->
                val members =
                    group.optionIds.map { id ->
                        prop.status.options
                            .first { it.id == id }
                            .name
                    }
                println("   group \"${group.name}\" (${group.color}): $members")
            }
        }

        if (!integrationTestEnvVarsAreSet()) {
            "(Skipped) status groups integration — env gate not satisfied" {
                println("Skipping StatusGroupsIntegrationTest — set required env vars (see .env.example)")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Status Groups — Integration Tests")
                        icon.emoji("🚦")
                    }
                containerPageId = container.id
                println("📄 Container: ${container.url}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("✅ Cleaned up container page (all children trashed)")
                } else {
                    println("🔧 Cleanup skipped — container page preserved for inspection")
                }
                notion.close()
            }

            "status option group assignment round-trips on create and update" {
                // --- Create: one custom option per predefined group ---
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Status Groups Test")
                        icon.emoji("🚦")
                        properties {
                            title("Name")
                            status("Stage") {
                                option("Backlog", SelectOptionColor.GRAY, group = StatusOptionGroup.TO_DO)
                                option("Doing", SelectOptionColor.YELLOW, group = StatusOptionGroup.IN_PROGRESS)
                                option("Shipped", SelectOptionColor.GREEN, group = StatusOptionGroup.COMPLETE)
                                option("Groupless", SelectOptionColor.BLUE)
                            }
                        }
                    }
                println("  Status DB: ${database.url}")
                delay(500)

                val dsId = database.dataSources.first().id
                val created = notion.dataSources.retrieve(dsId).properties["Stage"] as DatabaseProperty.Status
                describeStatus(created)

                // The core item-23 assertion: explicit groups landed where we sent them.
                created.groupOf("Backlog") shouldBe "To-do"
                created.groupOf("Doing") shouldBe "In progress"
                created.groupOf("Shipped") shouldBe "Complete"
                println("🔎 FINDING: on create, option with NO group landed in: ${created.groupOf("Groupless")}")

                // --- Update: move an existing option, add new options with/without group ---
                delay(500)
                notion.dataSources.update(dsId) {
                    properties {
                        status("Stage") {
                            // Existing options re-sent: one moved to a new group, one with group omitted.
                            option("Backlog", SelectOptionColor.GRAY, group = StatusOptionGroup.IN_PROGRESS)
                            option("Doing", SelectOptionColor.YELLOW)
                            option("Shipped", SelectOptionColor.GREEN, group = StatusOptionGroup.COMPLETE)
                            option("Groupless", SelectOptionColor.BLUE)
                            // New options: one with explicit group, one without.
                            option("Verified", SelectOptionColor.PURPLE, group = StatusOptionGroup.COMPLETE)
                            option("Incoming", SelectOptionColor.RED)
                        }
                    }
                }
                delay(500)

                val updated = notion.dataSources.retrieve(dsId).properties["Stage"] as DatabaseProperty.Status
                describeStatus(updated)

                // Explicit group on update round-trips (moved + new).
                updated.groupOf("Backlog") shouldBe "In progress"
                updated.groupOf("Shipped") shouldBe "Complete"
                updated.groupOf("Verified") shouldBe "Complete"

                // Documented-but-unratified semantics (item 24) — assert what the docs claim,
                // and the describeStatus dump above is the evidence either way.
                println("🔎 FINDING: existing option with group omitted on update kept: ${updated.groupOf("Doing")}")
                println("🔎 FINDING: new option with no group defaulted to: ${updated.groupOf("Incoming")}")
                updated.groupOf("Doing") shouldBe "In progress"
                updated.groupOf("Incoming") shouldBe "To-do"

                // Sanity: options we re-sent all survived, and default seed options
                // ("Not started"/"Done") were not resurrected by the update.
                val names = updated.status.options.map { it.name }
                names shouldContain "Groupless"
                names shouldNotContain "Not started"
            }
        }
    })
