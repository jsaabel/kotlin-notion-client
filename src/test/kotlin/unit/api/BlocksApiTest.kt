package unit.api

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import no.saabelit.kotlinnotionclient.api.BlocksApi
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.base.Color
import no.saabelit.kotlinnotionclient.models.blocks.Block
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.models.blocks.Heading2RequestContent
import no.saabelit.kotlinnotionclient.models.blocks.ParagraphRequestContent
import no.saabelit.kotlinnotionclient.models.requests.RequestBuilders
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
                            method = HttpMethod.Companion.Get,
                            path = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.Companion.OK,
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

                    else -> throw AssertionError("Expected Heading2 block")
                }
            }

            test("should handle 404 not found error") {
                val blockId = "non-existent-block"

                httpClient =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Companion.Get,
                            urlPattern = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.Companion.NotFound,
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
                            method = HttpMethod.Companion.Get,
                            path = "/v1/blocks/$parentBlockId/children",
                            statusCode = HttpStatusCode.Companion.OK,
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
                            method = HttpMethod.Companion.Patch,
                            path = "/v1/blocks/$parentBlockId/children",
                            statusCode = HttpStatusCode.Companion.OK,
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
                            method = HttpMethod.Companion.Patch,
                            path = "/v1/blocks/$parentBlockId/children",
                            statusCode = HttpStatusCode.Companion.OK,
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
        }

        context("BlocksApi.update") {
            test("should update a block successfully") {
                val blockId = "c02fc1d3-db8b-45c5-a222-27595b15aea7"
                val updateResponse = TestFixtures.readApiResponse("blocks/patch_update_a_block.json")

                httpClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Companion.Patch,
                            path = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.Companion.OK,
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

                    else -> throw AssertionError("Expected Heading2 block")
                }
            }

            test("should update a block using DSL builder") {
                val blockId = "c02fc1d3-db8b-45c5-a222-27595b15aea7"
                val updateResponse = TestFixtures.readApiResponse("blocks/patch_update_a_block.json")

                httpClient =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Companion.Patch,
                            path = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.Companion.OK,
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
                            method = HttpMethod.Companion.Patch,
                            urlPattern = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.Companion.BadRequest,
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
                            method = HttpMethod.Companion.Patch,
                            path = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.Companion.OK,
                            responseBody = deleteResponse.toString(),
                        )
                    }
                blocksApi = BlocksApi(httpClient, config)

                val deletedBlock = blocksApi.delete(blockId)

                deletedBlock.id shouldBe blockId
                deletedBlock.archived shouldBe true
                deletedBlock.type shouldBe "paragraph"
            }

            test("should handle 404 not found error when deleting") {
                val blockId = "non-existent-block"

                httpClient =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Companion.Patch,
                            urlPattern = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.Companion.NotFound,
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
                            method = HttpMethod.Companion.Patch,
                            urlPattern = "/v1/blocks/$blockId",
                            statusCode = HttpStatusCode.Companion.Forbidden,
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
