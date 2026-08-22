package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon

/**
 * Pins which of `icon` and `cover` each update endpoint accepts, and in which direction.
 *
 * The four answers below are not symmetric, not documented anywhere as a set, and each one
 * decides whether a builder affordance exists. All four were established live on 2026-08-22
 * (issue #82) and are asserted here so that a change on Notion's side — or a well-meaning
 * addition on ours — fails a test rather than a user's call.
 *
 * | Endpoint | Attribute | Set | Clear (`null`) |
 * | --- | --- | --- | --- |
 * | `PATCH /v1/databases` | `icon` | ✅ | ❌ `body.icon should be an object or `undefined`` |
 * | `PATCH /v1/databases` | `cover` | ✅ | ❌ `body.cover should be an object or `undefined`` |
 * | `PATCH /v1/data_sources` | `icon` | ✅ | ✅ |
 * | `PATCH /v1/data_sources` | `cover` | ❌ `not supported for data sources` | — |
 *
 * What that shape means for callers, and why the builders look the way they do:
 *
 * - **`UpdateDatabaseRequestBuilder` has no `remove()`** on either icon or cover. The container's
 *   are write-once-then-replace.
 * - **`UpdateDataSourceRequestBuilder.icon.remove()` exists**, and it is the one that matters:
 *   Notion's UI renders the data source, so this is how the icon a reader sees is cleared.
 * - **`UpdateDataSourceRequestBuilder` has no cover builder**, because the endpoint says so in as
 *   many words — "Use the Update Database API instead".
 *
 * The three `null` cases are sent through a bare Ktor client because two of them are shapes our
 * typed builders deliberately refuse to produce; the data source cover likewise has no model
 * field. The removal that *is* supported is exercised twice — once raw, once through
 * `dataSources.update { icon.remove() }` — so the assertion covers both the endpoint's behaviour
 * and our sentinel encoding of it.
 *
 * The control at the end sends a shape known to be accepted, so a 400 above means "this endpoint
 * refuses this" rather than "the token or headers were wrong".
 *
 * Run with: ./gradlew integrationTest --tests "*IconCoverSupportIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class IconCoverSupportIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) icon and cover endpoint support" {
                println("Skipping IconCoverSupportIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val rootPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val config = NotionConfig(apiToken = token)
            val notion = NotionClient.create(config)

            val coverUrl = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d"
            val coverObject = """{"type":"external","external":{"url":"$coverUrl"}}"""

            "icon and cover are supported asymmetrically across the two update endpoints" {
                val database =
                    notion.databases.create {
                        parent.page(rootPageId)
                        title("Icon/cover support matrix")
                        icon.emoji("🧪")
                        properties { title("Name") }
                    }
                val dataSourceId = database.dataSources.first().id
                // create() propagates the icon to the initial data source, so both sides start
                // with one and "was it cleared?" is a meaningful question on each.
                val databaseUrl = "${config.baseUrl}/databases/${database.id}"
                val dataSourceUrl = "${config.baseUrl}/data_sources/$dataSourceId"

                val raw = HttpClient(CIO) { expectSuccess = false } // a 400 is an expected result here

                try {
                    suspend fun patch(
                        label: String,
                        url: String,
                        body: String,
                    ): Pair<Int, String> {
                        val response: HttpResponse =
                            raw.patch(url) {
                                header(HttpHeaders.Authorization, "Bearer $token")
                                header("Notion-Version", config.apiVersion)
                                contentType(ContentType.Application.Json)
                                setBody(body)
                            }
                        val status = response.status.value
                        val text = response.bodyAsText()
                        println("$label → HTTP $status")
                        if (status !in 200..299) println("    $text")
                        return status to text
                    }

                    // --- The container: both attributes can be set, neither can be cleared ---

                    val (setCover, _) = patch("container, set cover", databaseUrl, """{"cover":$coverObject}""")
                    setCover shouldBe 200
                    notion.databases.retrieve(database.id).cover shouldNotBe null

                    val (nullIcon, nullIconBody) = patch("container, clear icon", databaseUrl, """{"icon":null}""")
                    nullIcon shouldBe 400
                    nullIconBody shouldContain "body.icon should be an object or `undefined`"

                    val (nullCover, nullCoverBody) = patch("container, clear cover", databaseUrl, """{"cover":null}""")
                    nullCover shouldBe 400
                    nullCoverBody shouldContain "body.cover should be an object or `undefined`"

                    // Both survive the rejected clears — a 400 changes nothing.
                    val afterRejectedClears = notion.databases.retrieve(database.id)
                    afterRejectedClears.icon shouldNotBe null
                    afterRejectedClears.cover shouldNotBe null

                    // --- The data source: a cover is refused outright, an icon can be cleared ---

                    val (dsCover, dsCoverBody) = patch("data source, set cover", dataSourceUrl, """{"cover":$coverObject}""")
                    dsCover shouldBe 400
                    dsCoverBody shouldContain "`cover` property is not supported for data sources"

                    notion.dataSources.retrieve(dataSourceId).icon shouldNotBe null
                    val (dsNullIcon, _) = patch("data source, clear icon", dataSourceUrl, """{"icon":null}""")
                    dsNullIcon shouldBe 200
                    notion.dataSources.retrieve(dataSourceId).icon shouldBe null

                    // The same removal through the typed path, which is what callers actually use:
                    // set it again, then clear it with the sentinel our builder records.
                    notion.dataSources.update(dataSourceId) { icon.emoji("🧪") }
                    notion.dataSources.retrieve(dataSourceId).icon shouldBe Icon.Emoji(emoji = "🧪")

                    notion.dataSources.update(dataSourceId) { icon.remove() }
                    notion.dataSources.retrieve(dataSourceId).icon shouldBe null

                    // --- Control: a shape known to be accepted, so the 400s above are about the
                    // endpoints and not about the credentials or headers.
                    val (control, _) = patch("control, set container icon", databaseUrl, """{"icon":{"type":"emoji","emoji":"✅"}}""")
                    control shouldBe 200
                } finally {
                    raw.close()
                    if (shouldCleanupAfterTest()) {
                        notion.databases.trash(database.id)
                    } else {
                        println("  Database URL (kept for inspection): ${database.url}")
                    }
                }
            }
        }
    })
