package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import kotlinx.coroutines.delay

/**
 * Live confirmation for the payloads where JSON `null` is the instruction (issue #80).
 *
 * Two DSL affordances used to compile, run, return successfully and do nothing, because
 * `explicitNulls = false` dropped the very `null` that carried the intent. The unit suite pins the
 * encoded bytes; this pins what Notion does with them:
 *
 * 1. `icon.remove()` — set an icon, remove it, re-retrieve, assert it is gone.
 * 2. `select(name, null)` — set a select option, clear it, re-retrieve, assert it is empty.
 *
 * Both also record what Notion does with the *old* payload shape (the key dropped entirely), the
 * question left open in the issue: rejected outright, or accepted and ignored. That answer is
 * printed rather than asserted — it is a finding about the API, not a contract of this client.
 *
 * Prerequisites: NOTION_API_TOKEN, NOTION_TEST_PAGE_ID, NOTION_RUN_INTEGRATION_TESTS=true
 * (or a `.env` file — see .env.example).
 *
 * Run with: ./gradlew integrationTest --tests "*ExplicitNullPayloadIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class ExplicitNullPayloadIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "(Skipped) explicit null payload integration — env gate not satisfied" {
                println("Skipping ExplicitNullPayloadIntegrationTest — set required env vars (see .env.example)")
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
                        title("Explicit Null Payloads — Integration Tests")
                        icon.emoji("🕳️")
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

            "icon.remove() actually removes the icon" {
                val page =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Icon removal")
                        icon.emoji("🎯")
                        cover.external("https://www.notion.so/images/page-cover/met_william_morris_1877_willow.jpg")
                    }
                delay(500)

                val before = notion.pages.retrieve(page.id)
                before.icon.shouldNotBeNull()
                before.cover.shouldNotBeNull()

                notion.pages.update(page.id) {
                    icon.remove()
                    cover.remove()
                }
                delay(500)

                val after = notion.pages.retrieve(page.id)
                after.icon shouldBe null
                after.cover shouldBe null
                println("  ✅ icon.remove() and cover.remove() cleared both — the null reached the API")
            }

            "clearing a select property empties it" {
                val database =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Clearable Properties")
                        properties {
                            title("Name")
                            select("Stage") {
                                option("Doing", SelectOptionColor.YELLOW)
                            }
                            url("Link")
                            number("Score")
                        }
                    }
                delay(500)
                val dsId = database.dataSources.first().id

                val page =
                    notion.pages.create {
                        parent.dataSource(dsId)
                        properties {
                            title("Name", "Row A")
                            select("Stage", "Doing")
                            url("Link", "https://example.com")
                            number("Score", 42)
                        }
                    }
                delay(500)

                (notion.pages.retrieve(page.id).properties["Stage"] as PageProperty.Select).select?.name shouldBe "Doing"

                notion.pages.update(page.id) {
                    properties {
                        select("Stage", null as String?)
                        url("Link", null)
                        number("Score", null as Double?)
                    }
                }
                delay(500)

                val after = notion.pages.retrieve(page.id)
                (after.properties["Stage"] as PageProperty.Select).select shouldBe null
                (after.properties["Link"] as PageProperty.Url).url shouldBe null
                (after.properties["Score"] as PageProperty.Number).number shouldBe null
                println("  ✅ select, url and number cleared — the null payload reached the API")

                // What the *old* payload did: the discriminator with the key dropped. Sent raw,
                // because no client API can build that shape any more. Recorded, not asserted —
                // it is a finding about the API, and the answer #80 could not get without
                // credentials.
                notion.pages.update(page.id) { properties { select("Stage", "Doing") } }
                delay(500)
                val raw = rawPatch(token, "pages/${page.id}", """{"properties":{"Stage":{"type":"select"}}}""")
                delay(500)
                val afterRaw = (notion.pages.retrieve(page.id).properties["Stage"] as PageProperty.Select).select?.name
                println("🔎 FINDING: PATCH {\"Stage\":{\"type\":\"select\"}} -> HTTP ${raw.first}: ${raw.second}")
                println("🔎 FINDING: Stage after that PATCH -> $afterRaw (was \"Doing\")")
            }
        }
    })

/**
 * Sends a hand-written body to the Notion API, bypassing the client's models.
 *
 * Only for recording what the API does with a payload the client can no longer produce.
 *
 * @return the HTTP status code and the response body
 */
private suspend fun rawPatch(
    token: String,
    path: String,
    body: String,
): Pair<Int, String> {
    val config = NotionConfig(apiToken = token)
    val client = HttpClient(CIO)
    return try {
        val response =
            client.patch("${config.baseUrl}/$path") {
                header("Authorization", "Bearer $token")
                header("Notion-Version", config.apiVersion)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        response.status.value to response.bodyAsText()
    } finally {
        client.close()
    }
}
