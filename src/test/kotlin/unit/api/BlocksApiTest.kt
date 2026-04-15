package unit.api

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.BlocksApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.base.Color
import it.saabel.kotlinnotionclient.models.blocks.Block
import it.saabel.kotlinnotionclient.models.blocks.BlockAppendPosition
import it.saabel.kotlinnotionclient.models.blocks.BlockReference
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.Heading2RequestContent
import it.saabel.kotlinnotionclient.models.blocks.ParagraphRequestContent
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import unit.util.TestFixtures
import unit.util.mockClient

@Tags("Unit")
class BlocksApiTest :
    FunSpec({
        lateinit var httpClient: HttpClient
        lateinit var blocksApi: BlocksApi
        val config = NotionConfig(apiToken = "xxx")

        beforeTest {
            // Reset for each test
            httpClient = HttpClient()
            blocksApi = BlocksApi(httpClient, config)
        }

        afterTest {
            httpClient.close()
        }

        context("BlocksApi.retrieve") {
            test("should retrieve a block successfully") {
                val blockId = "c02fc1d3-db8b-45c5-a222-27595b15aea7"
                val blockData = TestFixtures.Blocks.retrieveBlock()

                httpClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.OK,
                            responseBody = blockData.toString(),
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                val block = blocksApi.retrieve(blockId)

                block shouldNotBe null
                block.id shouldBe blockId
                block.type shouldBe "heading_2"
                when (block) {
                    is Block.Heading2 -> {
                        block.heading2.richText
                            .first()
                            .plainText shouldBe "Lacinato kale"
                        block.heading2.color shouldBe Color.DEFAULT
                    }

                    else -> {
                        throw AssertionError("Expected Heading2 block")
                    }
                }
            }

            test("should handle 404 not found error") {
                val blockId = "non-existent-block"

                httpClient =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.NotFound,
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                try {
                    blocksApi.retrieve(blockId)
                    throw AssertionError("Expected NotionException.ApiError")
                } catch (e: NotionException.ApiError) {
                    e.status shouldBe 404
                }
            }
        }

        context("BlocksApi.retrieveChildren") {
            test("should retrieve all child blocks with pagination") {
                val parentBlockId = "59833787-2cf9-4fdf-8782-e53db20768a5"
                val childrenData = TestFixtures.Blocks.retrieveChildrenPaginated()

                httpClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/blocks/$parentBlockId/children",
                            statusCode = HttpStatusCode.OK,
                            responseBody = childrenData.toString(),
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                val children = blocksApi.retrieveChildren(parentBlockId)

                children.size shouldBe 2
                children[0].type shouldBe "heading_2"
                children[1].type shouldBe "paragraph"
            }
        }

        context("BlocksApi.appendChildren") {
            test("should append children blocks successfully") {
                val parentBlockId = "59833787-2cf9-4fdf-8782-e53db20768a5"
                val appendChildrenResponse = TestFixtures.Blocks.appendChildrenResponse()

                httpClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Patch,
                            path = "/v1/blocks/$parentBlockId/children",
                            statusCode = HttpStatusCode.OK,
                            responseBody = appendChildrenResponse.toString(),
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                val children =
                    listOf(
                        BlockRequest.Paragraph(
                            paragraph =
                                ParagraphRequestContent(
                                    richText = listOf(RequestBuilders.createSimpleRichText("New paragraph")),
                                ),
                        ),
                    )

                val result = blocksApi.appendChildren(parentBlockId, children)

                result.results.size shouldBe 2
                result.results.first().type shouldBe "heading_2"
            }

            test("should append children using DSL builder") {
                val parentBlockId = "59833787-2cf9-4fdf-8782-e53db20768a5"
                val appendChildrenResponse = TestFixtures.Blocks.appendChildrenResponse()

                httpClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Patch,
                            path = "/v1/blocks/$parentBlockId/children",
                            statusCode = HttpStatusCode.OK,
                            responseBody = appendChildrenResponse.toString(),
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                val result =
                    blocksApi.appendChildren(parentBlockId) {
                        paragraph("New paragraph from DSL")
                        heading2("A heading")
                    }

                result.results.size shouldBe 2
                result.results.first().type shouldBe "heading_2"
            }

            context("with position parameter") {
                fun makeCaptureClient(
                    responseBody: String,
                    capturedBody: StringBuilder,
                ): Pair<HttpClient, BlocksApi> {
                    val engine =
                        MockEngine { request ->
                            if (request.method == HttpMethod.Patch && request.url.toString().contains("/children")) {
                                try {
                                    val bytes = (request.body as? OutgoingContent.ByteArrayContent)?.bytes() ?: ByteArray(0)
                                    capturedBody.append(bytes.decodeToString())
                                } catch (_: Exception) {
                                }
                                respond(
                                    content = responseBody,
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                                )
                            } else {
                                respondError(HttpStatusCode.NotFound)
                            }
                        }
                    val client =
                        HttpClient(engine) {
                            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                        }
                    return client to BlocksApi(client, config)
                }

                val singleBlock =
                    listOf(
                        BlockRequest.Paragraph(
                            paragraph = ParagraphRequestContent(richText = listOf(RequestBuilders.createSimpleRichText("test"))),
                        ),
                    )

                test("should include position=start in request body") {
                    val responseBody = TestFixtures.Blocks.appendChildrenResponse().toString()
                    val capturedBody = StringBuilder()
                    val (client, api) = makeCaptureClient(responseBody, capturedBody)

                    try {
                        api.appendChildren("parent-id", singleBlock, position = BlockAppendPosition.Start)
                    } finally {
                        client.close()
                    }

                    val body = Json.parseToJsonElement(capturedBody.toString()).jsonObject
                    body["position"]
                        ?.jsonObject
                        ?.get("type")
                        ?.jsonPrimitive
                        ?.content shouldBe "start"
                }

                test("should include position=end in request body") {
                    val responseBody = TestFixtures.Blocks.appendChildrenResponse().toString()
                    val capturedBody = StringBuilder()
                    val (client, api) = makeCaptureClient(responseBody, capturedBody)

                    try {
                        api.appendChildren("parent-id", singleBlock, position = BlockAppendPosition.End)
                    } finally {
                        client.close()
                    }

                    val body = Json.parseToJsonElement(capturedBody.toString()).jsonObject
                    body["position"]
                        ?.jsonObject
                        ?.get("type")
                        ?.jsonPrimitive
                        ?.content shouldBe "end"
                }

                test("should include position=after_block with block id in request body") {
                    val targetBlockId = "b5d8fd79-1234-1234-1234-123456789abc"
                    val responseBody = TestFixtures.Blocks.appendChildrenResponse().toString()
                    val capturedBody = StringBuilder()
                    val (client, api) = makeCaptureClient(responseBody, capturedBody)

                    try {
                        api.appendChildren(
                            "parent-id",
                            singleBlock,
                            position = BlockAppendPosition.AfterBlock(BlockReference(id = targetBlockId)),
                        )
                    } finally {
                        client.close()
                    }

                    val body = Json.parseToJsonElement(capturedBody.toString()).jsonObject
                    val positionObj = body["position"]?.jsonObject
                    positionObj?.get("type")?.jsonPrimitive?.content shouldBe "after_block"
                    positionObj
                        ?.get("after_block")
                        ?.jsonObject
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.content shouldBe targetBlockId
                }

                test("should omit position field when position=null") {
                    val responseBody = TestFixtures.Blocks.appendChildrenResponse().toString()
                    val capturedBody = StringBuilder()
                    val (client, api) = makeCaptureClient(responseBody, capturedBody)

                    try {
                        api.appendChildren("parent-id", singleBlock, position = null)
                    } finally {
                        client.close()
                    }

                    val body = Json.parseToJsonElement(capturedBody.toString()).jsonObject
                    body.containsKey("position") shouldBe false
                }
            }
        }

        context("BlocksApi.update") {
            test("should update a block successfully") {
                val blockId = "c02fc1d3-db8b-45c5-a222-27595b15aea7"
                val updateResponse = TestFixtures.readApiResponse("blocks/patch_update_a_block.json")

                httpClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Patch,
                            path = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.OK,
                            responseBody = updateResponse.toString(),
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                val updateRequest =
                    BlockRequest.Heading2(
                        heading2 =
                            Heading2RequestContent(
                                richText = listOf(RequestBuilders.createSimpleRichText("Lacinato kale")),
                                color = Color.GREEN,
                            ),
                    )

                val updatedBlock = blocksApi.update(blockId, updateRequest)

                updatedBlock.id shouldBe blockId
                updatedBlock.type shouldBe "heading_2"
                when (updatedBlock) {
                    is Block.Heading2 -> {
                        updatedBlock.heading2.richText
                            .first()
                            .plainText shouldBe "Lacinato kale"
                        updatedBlock.heading2.richText
                            .first()
                            .annotations
                            .color shouldBe Color.GREEN
                    }

                    else -> {
                        throw AssertionError("Expected Heading2 block")
                    }
                }
            }

            test("should update a block using DSL builder") {
                val blockId = "c02fc1d3-db8b-45c5-a222-27595b15aea7"
                val updateResponse = TestFixtures.readApiResponse("blocks/patch_update_a_block.json")

                httpClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Patch,
                            path = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.OK,
                            responseBody = updateResponse.toString(),
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                val updatedBlock =
                    blocksApi.update(blockId) {
                        heading2("Lacinato kale", color = Color.GREEN)
                    }

                updatedBlock.id shouldBe blockId
                updatedBlock.type shouldBe "heading_2"
            }

            test("should fail when DSL builder produces multiple blocks") {
                val blockId = "test-block-id"

                httpClient =
                    mockClient {
                        // No response needed as it should fail before making request
                    }
                blocksApi = BlocksApi(httpClient, config)

                try {
                    blocksApi.update(blockId) {
                        paragraph("First block")
                        paragraph("Second block")
                    }
                    throw AssertionError("Expected IllegalArgumentException")
                } catch (e: IllegalArgumentException) {
                    e.message shouldBe "Block update builder must produce exactly one block, but produced 2 blocks"
                }
            }

            test("should fail when DSL builder produces no blocks") {
                val blockId = "test-block-id"

                httpClient =
                    mockClient {
                        // No response needed as it should fail before making request
                    }
                blocksApi = BlocksApi(httpClient, config)

                try {
                    blocksApi.update(blockId) {
                        // Empty builder
                    }
                    throw AssertionError("Expected IllegalArgumentException")
                } catch (e: IllegalArgumentException) {
                    e.message shouldBe "Block update builder must produce exactly one block, but produced 0 blocks"
                }
            }

            test("should handle API error for type mismatch") {
                val blockId = "test-block-id"

                httpClient =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Patch,
                            urlPattern = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.BadRequest,
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                val updateRequest =
                    BlockRequest.Paragraph(
                        paragraph =
                            ParagraphRequestContent(
                                richText = listOf(RequestBuilders.createSimpleRichText("Changed type")),
                            ),
                    )

                try {
                    blocksApi.update(blockId, updateRequest)
                    throw AssertionError("Expected NotionException.ApiError")
                } catch (e: NotionException.ApiError) {
                    e.status shouldBe 400
                }
            }
        }

        context("BlocksApi.delete") {
            test("should delete (archive) a block successfully") {
                val blockId = "7985540b-2e77-4ac6-8615-c3047e36f872"
                val deleteResponse = TestFixtures.readApiResponse("blocks/delete_delete_a_block.json")

                httpClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Patch,
                            path = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.OK,
                            responseBody = deleteResponse.toString(),
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                val deletedBlock = blocksApi.delete(blockId)

                deletedBlock.id shouldBe blockId
                deletedBlock.inTrash shouldBe true
                deletedBlock.type shouldBe "paragraph"
            }

            test("should handle 404 not found error when deleting") {
                val blockId = "non-existent-block"

                httpClient =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Patch,
                            urlPattern = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.NotFound,
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                try {
                    blocksApi.delete(blockId)
                    throw AssertionError("Expected NotionException.ApiError")
                } catch (e: NotionException.ApiError) {
                    e.status shouldBe 404
                }
            }

            test("should handle permission error when deleting") {
                val blockId = "protected-block"

                httpClient =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Patch,
                            urlPattern = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.Forbidden,
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                try {
                    blocksApi.delete(blockId)
                    throw AssertionError("Expected NotionException.ApiError")
                } catch (e: NotionException.ApiError) {
                    e.status shouldBe 403
                }
            }
        }
    })
