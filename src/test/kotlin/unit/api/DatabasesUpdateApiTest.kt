package unit.api

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.DatabasesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.databases.UpdateDatabaseRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import unit.util.TestFixtures

/**
 * Verifies `databases.update` against the official `patch_update_a_database.json` sample and
 * asserts what actually goes on the wire.
 *
 * The response half matters because the 2025-09-03 database object no longer carries
 * `properties`; the request half matters because a container update is defined entirely by which
 * keys it does and does not carry.
 */
@Tags("Unit")
class DatabasesUpdateApiTest :
    DescribeSpec({

        val json = Json { ignoreUnknownKeys = true }
        val databaseId = "248104cd-477e-80fd-b757-e945d38000bd"

        /** A client that records each request and replies with the official update sample. */
        fun recordingClient(recorded: MutableList<Pair<io.ktor.client.request.HttpRequestData, JsonObject>>): HttpClient {
            val engine =
                MockEngine { request ->
                    val body = request.body.toByteArray().decodeToString()
                    recorded.add(request to json.parseToJsonElement(body).jsonObject)
                    respond(
                        content = TestFixtures.Databases.updateDatabaseAsString(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            return HttpClient(engine) { install(ContentNegotiation) { json(json) } }
        }

        describe("databases.update") {
            it("PATCHes /v1/databases/{id} and decodes the official sample") {
                val recorded = mutableListOf<Pair<io.ktor.client.request.HttpRequestData, JsonObject>>()
                val api = DatabasesApi(recordingClient(recorded), NotionConfig(apiToken = "test-token"))

                val database = api.update(databaseId) { title("My Task Tracker") }

                val (request, body) = recorded.single()
                request.method shouldBe HttpMethod.Patch
                request.url.encodedPath shouldBe "/v1/databases/$databaseId"
                body.keys shouldBe setOf("title")

                database.id shouldBe databaseId
                database.title.first().plainText shouldBe "My Task Tracker"
                database.isInline shouldBe false
                database.inTrash shouldBe false
                database.dataSources.single().id shouldBe "248104cd-477e-80af-bc30-000bd28de8f9"
                // The sample carries `"icon": null` — a removed icon reads back as absent.
                database.icon shouldBe null
            }

            it("sends the container attributes the DSL named, and nothing else") {
                val recorded = mutableListOf<Pair<io.ktor.client.request.HttpRequestData, JsonObject>>()
                val api = DatabasesApi(recordingClient(recorded), NotionConfig(apiToken = "test-token"))

                api.update(databaseId) {
                    parent.page("new-parent-page-id")
                    icon.emoji("📊")
                    inline(true)
                }

                val body = recorded.single().second
                body.keys shouldBe setOf("parent", "icon", "is_inline")
                body["parent"]!!.jsonObject["page_id"]!!.jsonPrimitive.content shouldBe "new-parent-page-id"
                body["icon"]!!.jsonObject["emoji"]!!.jsonPrimitive.content shouldBe "📊"
                body["is_inline"]!!.jsonPrimitive.content shouldBe "true"
            }

            it("carries an icon removal to the wire as an explicit null") {
                val recorded = mutableListOf<Pair<io.ktor.client.request.HttpRequestData, JsonObject>>()
                val api = DatabasesApi(recordingClient(recorded), NotionConfig(apiToken = "test-token"))

                api.update(databaseId) { icon.remove() }

                val body = recorded.single().second
                body.keys shouldBe setOf("icon")
                body["icon"].shouldNotBeNull().toString() shouldBe "null"
            }

            it("accepts a prebuilt request as well as the DSL") {
                val recorded = mutableListOf<Pair<io.ktor.client.request.HttpRequestData, JsonObject>>()
                val api = DatabasesApi(recordingClient(recorded), NotionConfig(apiToken = "test-token"))

                api.update(databaseId, UpdateDatabaseRequest(isInline = true))

                recorded.single().second.keys shouldBe setOf("is_inline")
            }
        }

        describe("databases.trash") {
            it("still sends in_trash = true, now through the update path") {
                val recorded = mutableListOf<Pair<io.ktor.client.request.HttpRequestData, JsonObject>>()
                val api = DatabasesApi(recordingClient(recorded), NotionConfig(apiToken = "test-token"))

                api.trash(databaseId)

                val (request, body) = recorded.single()
                request.method shouldBe HttpMethod.Patch
                request.url.encodedPath shouldBe "/v1/databases/$databaseId"
                body.keys shouldBe setOf("in_trash")
                body["in_trash"]!!.jsonPrimitive.content shouldBe "true"
            }
        }
    })
