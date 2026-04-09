package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.pages.PageProperty

/**
 * Throwaway integration test for the verification property.
 *
 * Wiki databases cannot be created programmatically, so this test requires a manually
 * pre-created wiki page. Set the following environment variables before running:
 *
 *   export NOTION_API_TOKEN="secret_..."
 *   export NOTION_TEST_WIKI_PAGE_ID="<id of a page inside a wiki database>"
 *
 * Run with:
 *   ./gradlew integrationTest --tests "*WikiVerificationIntegrationTest*"
 *
 * The test verifies the page, then unverifies it, leaving the page in its original state.
 */
@Tags("Integration", "RequiresApi", "ManualSetup")
class WikiVerificationIntegrationTest :
    StringSpec({
        val wikiPageId = System.getenv("NOTION_TEST_WIKI_PAGE_ID")

        if (!integrationTestEnvVarsAreSet() || wikiPageId == null) {
            "!(Skipped) Wiki verification tests — set NOTION_API_TOKEN and NOTION_TEST_WIKI_PAGE_ID" {
                println("⏭️ Skipping WikiVerificationIntegrationTest")
                println("   Set NOTION_TEST_WIKI_PAGE_ID to the ID of a page inside a wiki database")
            }
        } else {
            "Verify and then unverify a wiki page" {
                val token = System.getenv("NOTION_API_TOKEN")
                val client = NotionClient(NotionConfig(apiToken = token))

                try {
                    // Step 1: read current state
                    println("\n🔍 Retrieving wiki page $wikiPageId...")
                    val page = client.pages.retrieve(wikiPageId)
                    val verificationProp = page.properties["Verification"]
                    verificationProp.shouldNotBeNull()
                    verificationProp as PageProperty.Verification
                    println("   Current state: ${verificationProp.verification?.state ?: "null"}")

                    // Step 2: verify with a 90-day window
                    println("\n✅ Setting verification state to 'verified'...")
                    val verifiedPage =
                        client.pages.update(wikiPageId) {
                            properties {
                                verify(
                                    "Verification",
                                    start = "2026-03-25T00:00:00.000Z",
                                    end = "2026-06-25T00:00:00.000Z",
                                )
                            }
                        }
                    val verifiedProp = verifiedPage.properties["Verification"] as PageProperty.Verification
                    verifiedProp.verification.shouldNotBeNull()
                    verifiedProp.verification.state shouldBe "verified"
                    verifiedProp.verification.date.shouldNotBeNull()
                    verifiedProp.verification.date.start shouldBe "2026-03-25T00:00:00.000Z"
                    verifiedProp.verification.date.end shouldBe "2026-06-25T00:00:00.000Z"
                    println("   ✅ Page verified, verified_by: ${verifiedProp.verification.verifiedBy?.id}")

                    // Step 3: unverify
                    println("\n🔄 Unverifying page...")
                    val unverifiedPage =
                        client.pages.update(wikiPageId) {
                            properties { unverify("Verification") }
                        }
                    val unverifiedProp = unverifiedPage.properties["Verification"] as PageProperty.Verification
                    unverifiedProp.verification.shouldNotBeNull()
                    unverifiedProp.verification.state shouldBe "unverified"
                    println("   ✅ Page unverified")

                    // Step 4: re-verify — leave page in verified state for easy inspection
                    println("\n✅ Re-verifying page...")
                    val reverifiedPage =
                        client.pages.update(wikiPageId) {
                            properties {
                                verify(
                                    "Verification",
                                    start = "2026-03-25T00:00:00.000Z",
                                    end = "2026-06-25T00:00:00.000Z",
                                )
                            }
                        }
                    val reverifiedProp = reverifiedPage.properties["Verification"] as PageProperty.Verification
                    reverifiedProp.verification.shouldNotBeNull()
                    reverifiedProp.verification.state shouldBe "verified"
                    println("   ✅ Page re-verified — left in verified state for inspection")
                } finally {
                    client.close()
                }
            }
        }
    })
