package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.databases.DatabaseProperty
import it.saabel.kotlinnotionclient.models.pages.FormulaResult
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import kotlinx.coroutines.delay

/**
 * Integration tests for formula property writes (FOLLOWUPS item 34; issue #61).
 *
 * The readable `prop("Name")` expression syntax was implemented from live docs but never
 * exercised against the real API. Verified here:
 * - a `prop()` expression is accepted on database create and data source update
 * - the formula actually *computes* against page values (the strongest proof the
 *   expression was understood, independent of which syntax the read side returns)
 * - the read side is tolerated in **both** syntaxes — the readable rollout is gradual,
 *   so the returned expression may be readable or the legacy
 *   `{{notion:block_property:...}}` form ([FormulaConfiguration.usesInternalReferences]);
 *   which one came back is logged as a finding, and verbatim storage is only asserted
 *   when the readable form is returned.
 *
 * Run with: ./gradlew integrationTest --tests "*FormulaWritesIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class FormulaWritesIntegrationTest :
    StringSpec({

        fun describeExpression(
            label: String,
            prop: DatabaseProperty.Formula,
        ) {
            println("🔎 $label:")
            println("   expression              = ${prop.expression}")
            println("   usesInternalReferences  = ${prop.formula.usesInternalReferences()}")
            println("   propertyReferences      = ${prop.formula.propertyReferences()}")
        }

        if (!integrationTestEnvVarsAreSet()) {
            "(Skipped) formula writes integration — env gate not satisfied" {
                println("Skipping FormulaWritesIntegrationTest — set required env vars (see .env.example)")
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
                        title("Formula Writes — Integration Tests")
                        icon.emoji("🧮")
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

            "prop() formula expression round-trips on create and update and computes" {
                val createExpression = """prop("Price") * 1.25"""
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Formula Writes Test")
                        icon.emoji("🧮")
                        properties {
                            title("Name")
                            number("Price")
                            formula("Total", createExpression)
                        }
                    }
                println("  Formula DB: ${database.url}")
                delay(500)

                val dsId = database.dataSources.first().id
                val created = notion.dataSources.retrieve(dsId).properties["Total"] as DatabaseProperty.Formula
                describeExpression("expression after create (sent: $createExpression)", created)
                if (created.formula.usesInternalReferences()) {
                    println("🔎 FINDING: read side returned the LEGACY {{notion:...}} syntax")
                } else {
                    println("🔎 FINDING: read side returned the READABLE syntax")
                    created.expression shouldBe createExpression
                }

                // The formula computes against a real page value — proof the expression
                // was semantically understood, regardless of the returned syntax.
                val page =
                    notion.pages.create {
                        parent.dataSource(dsId)
                        properties {
                            title("Name", "Widget")
                            number("Price", 100.0)
                        }
                    }
                delay(500)
                val total =
                    (notion.pages.retrieve(page.id).properties["Total"] as PageProperty.Formula)
                        .formula
                        .shouldBeInstanceOf<FormulaResult.NumberResult>()
                println("🔎 computed Total for Price=100.0: ${total.number}")
                total.number shouldBe 125.0

                // --- Update the expression via data source update ---
                val updateExpression = """prop("Price") * 2"""
                delay(500)
                notion.dataSources.update(dsId) {
                    properties {
                        formula("Total", updateExpression)
                    }
                }
                delay(500)

                val updated = notion.dataSources.retrieve(dsId).properties["Total"] as DatabaseProperty.Formula
                describeExpression("expression after update (sent: $updateExpression)", updated)
                if (!updated.formula.usesInternalReferences()) {
                    updated.expression shouldBe updateExpression
                }

                val page2 =
                    notion.pages.create {
                        parent.dataSource(dsId)
                        properties {
                            title("Name", "Gadget")
                            number("Price", 10.0)
                        }
                    }
                delay(500)
                val total2 =
                    (notion.pages.retrieve(page2.id).properties["Total"] as PageProperty.Formula)
                        .formula
                        .shouldBeInstanceOf<FormulaResult.NumberResult>()
                println("🔎 computed Total for Price=10.0 after update: ${total2.number}")
                total2.number shouldBe 20.0

                println("✅ prop() expression accepted on create and update; formula computes correctly")
            }
        }
    })
