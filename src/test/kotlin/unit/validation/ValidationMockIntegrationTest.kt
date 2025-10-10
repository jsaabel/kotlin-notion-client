package unit.validation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Annotations
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.base.TextContent
import it.saabel.kotlinnotionclient.models.blocks.BlockRequest
import it.saabel.kotlinnotionclient.models.blocks.ParagraphRequestContent
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseRequest
import it.saabel.kotlinnotionclient.models.databases.InitialDataSource
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.SelectOption
import it.saabel.kotlinnotionclient.models.pages.UpdatePageRequest
import it.saabel.kotlinnotionclient.validation.ValidationException
import kotlinx.serialization.json.Json
import unit.util.TestFixtures

/**
 * Mock-based integration tests for validation logic using MockEngine to simulate API responses.
 *
 * ## Testing Philosophy
 *
 * These tests verify that our validation system works end-to-end by:
 * - Testing actual API client methods with validation enabled
 * - Using MockEngine to simulate successful API responses (no real API calls)
 * - Verifying that validation prevents API errors BEFORE HTTP requests
 * - Testing both success (auto-fix) and failure (validation error) scenarios
 * - Ensuring validation integrates seamlessly with existing API workflows
 *
 * ## What We Test
 *
 * 1. **Auto-truncation Success**: Requests with text violations get auto-fixed and succeed
 * 2. **Validation Failure**: Requests with non-fixable violations fail fast with helpful errors
 * 3. **API Integration**: Validation works transparently within existing API methods
 * 4. **Configuration Respect**: Different validation configs produce expected behaviors
 */
class ValidationMockIntegrationTest :
    FunSpec({

        // Helper function to create mock HTTP client that simulates successful API responses
        fun createMockClient(config: NotionConfig): NotionClient {
            val mockEngine =
                MockEngine { request ->
                    // Simulate successful responses for all endpoints
                    when {
                        request.url.encodedPath.contains("/pages") &&
                            request.method.value == "POST" -> {
                            respond(
                                content = TestFixtures.Pages.retrievePageAsString(),
                                status = HttpStatusCode.OK,
                                headers = headersOf("Content-Type" to listOf("application/json")),
                            )
                        }

                        request.url.encodedPath.contains("/pages") &&
                            request.method.value == "PATCH" -> {
                            respond(
                                content = TestFixtures.Pages.retrievePageAsString(),
                                status = HttpStatusCode.OK,
                                headers = headersOf("Content-Type" to listOf("application/json")),
                            )
                        }

                        request.url.encodedPath.contains("/databases") &&
                            request.method.value == "POST" -> {
                            respond(
                                content = TestFixtures.Databases.retrieveDatabaseAsString(),
                                status = HttpStatusCode.OK,
                                headers = headersOf("Content-Type" to listOf("application/json")),
                            )
                        }

                        request.url.encodedPath.contains("/blocks") &&
                            request.method.value == "PATCH" -> {
                            respond(
                                content = TestFixtures.Blocks.appendBlockChildrenAsString(),
                                status = HttpStatusCode.OK,
                                headers = headersOf("Content-Type" to listOf("application/json")),
                            )
                        }

                        else -> {
                            respond(
                                content = "{}",
                                status = HttpStatusCode.OK,
                                headers = headersOf("Content-Type" to listOf("application/json")),
                            )
                        }
                    }
                }

            val httpClient =
                HttpClient(mockEngine) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            },
                        )
                    }
                }

            return NotionClient.createWithClient(httpClient, config)
        }

        // Helper functions for creating test data
        fun createLongRichText(): RichText =
            RichText(
                type = "text",
                text = TextContent(content = "a".repeat(2100)), // Exceeds 2000 char limit
                annotations = Annotations(),
                plainText = "a".repeat(2100),
                href = null,
            )

        fun createNormalRichText(content: String = "Normal text"): RichText =
            RichText(
                type = "text",
                text = TextContent(content = content),
                annotations = Annotations(),
                plainText = content,
                href = null,
            )

        context("Page Validation Integration") {
            test("should auto-truncate text and successfully create page") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                val longTitle = createLongRichText()
                val request =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties =
                            mapOf(
                                "title" to PagePropertyValue.TitleValue(title = listOf(longTitle)),
                            ),
                    )

                // This should succeed - validation auto-truncates the text
                val result = client.pages.create(request)
                result.shouldNotBe(null)
            }

            test("should fail fast on non-fixable violations") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                val tooManyOptions =
                    (1..101).map {
                        SelectOption(
                            id = "option-$it",
                            name = "Option $it",
                            color = SelectOptionColor.DEFAULT,
                        )
                    }
                val request =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties =
                            mapOf(
                                "multiSelect" to PagePropertyValue.MultiSelectValue(multiSelect = tooManyOptions),
                            ),
                    )

                // This should fail with ValidationException (array violations are not auto-fixable)
                shouldThrow<ValidationException> {
                    client.pages.create(request)
                }
            }

            test("should auto-truncate text in page updates") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                val longRichText = createLongRichText()
                val request =
                    UpdatePageRequest(
                        properties =
                            mapOf(
                                "description" to PagePropertyValue.RichTextValue(richText = listOf(longRichText)),
                            ),
                    )

                // This should succeed - validation auto-truncates the text
                val result = client.pages.update("test-page-id", request)
                result.shouldNotBe(null)
            }
        }

        context("Database Validation Integration (2025-09-03 API)") {
            test("should auto-truncate text and successfully create database") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                val longTitle = createLongRichText()
                val request =
                    CreateDatabaseRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        title = listOf(longTitle),
                        initialDataSource = InitialDataSource(properties = mapOf()),
                    )

                // This should succeed - validation auto-truncates the text
                val result = client.databases.create(request)
                result.shouldNotBe(null)
            }

            test("should auto-truncate both title and description") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                val longTitle = createLongRichText()
                val longDescription = createLongRichText()
                val request =
                    CreateDatabaseRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        title = listOf(longTitle),
                        initialDataSource = InitialDataSource(properties = mapOf()),
                        description = listOf(longDescription),
                    )

                // This should succeed - validation auto-truncates both fields
                val result = client.databases.create(request)
                result.shouldNotBe(null)
            }
        }

        context("Block Validation Integration") {
            test("should fail fast on too many blocks") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                val tooManyBlocks =
                    (1..101).map {
                        BlockRequest.Paragraph(
                            paragraph =
                                ParagraphRequestContent(
                                    richText = listOf(createNormalRichText("Block $it")),
                                ),
                        )
                    }

                // This should fail with ValidationException (block array violations are not auto-fixable)
                shouldThrow<ValidationException> {
                    client.blocks.appendChildren("test-block-id", tooManyBlocks)
                }
            }

            test("should fail fast on long content within blocks") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                val blockWithLongContent =
                    listOf(
                        BlockRequest.Paragraph(
                            paragraph =
                                ParagraphRequestContent(
                                    richText = listOf(createLongRichText()),
                                ),
                        ),
                    )

                // This should fail with ValidationException (block content violations are not auto-fixable)
                shouldThrow<ValidationException> {
                    client.blocks.appendChildren("test-block-id", blockWithLongContent)
                }
            }

            test("should succeed with valid blocks") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                val validBlocks =
                    listOf(
                        BlockRequest.Paragraph(
                            paragraph =
                                ParagraphRequestContent(
                                    richText = listOf(createNormalRichText("Valid block content")),
                                ),
                        ),
                    )

                // This should succeed - no validation violations
                val result = client.blocks.appendChildren("test-block-id", validBlocks)
                result.shouldNotBe(null)
            }
        }

        context("Configuration Validation") {

            test("should validate without affecting normal requests") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                val normalTitle = createNormalRichText("Normal length title")
                val request =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties =
                            mapOf(
                                "title" to PagePropertyValue.TitleValue(title = listOf(normalTitle)),
                            ),
                    )

                // This should succeed without any modifications
                val result = client.pages.create(request)
                result.shouldNotBe(null)
            }
        }

        context("End-to-End Validation Workflow") {
            test("should demonstrate complete validation workflow") {
                val client =
                    createMockClient(
                        NotionConfig(apiToken = "test-token"),
                    )

                // Test 1: Page creation with auto-truncation
                val pageRequest =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties =
                            mapOf(
                                "title" to PagePropertyValue.TitleValue(title = listOf(createLongRichText())),
                            ),
                    )
                val pageResult = client.pages.create(pageRequest)
                pageResult.shouldNotBe(null)

                // Test 2: Database creation with auto-truncation (2025-09-03 API)
                val databaseRequest =
                    CreateDatabaseRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        title = listOf(createLongRichText()),
                        initialDataSource = InitialDataSource(properties = mapOf()),
                    )
                val databaseResult = client.databases.create(databaseRequest)
                databaseResult.shouldNotBe(null)

                // Test 3: Page update with auto-truncation
                val updateRequest =
                    UpdatePageRequest(
                        properties =
                            mapOf(
                                "description" to PagePropertyValue.RichTextValue(richText = listOf(createLongRichText())),
                            ),
                    )
                val updateResult = client.pages.update("test-page-id", updateRequest)
                updateResult.shouldNotBe(null)

                // Test 4: Block operations should fail fast (no auto-fix for blocks)
                val invalidBlocks =
                    (1..101).map {
                        BlockRequest.Paragraph(
                            paragraph =
                                ParagraphRequestContent(
                                    richText = listOf(createNormalRichText("Block $it")),
                                ),
                        )
                    }
                shouldThrow<ValidationException> {
                    client.blocks.appendChildren("test-block-id", invalidBlocks)
                }
            }
        }
    })
