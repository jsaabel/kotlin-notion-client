package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.saabel.kotlinnotionclient.api.DataSourcesApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.datasources.Template
import it.saabel.kotlinnotionclient.models.datasources.TemplatesResponse
import unit.util.TestFixtures
import unit.util.decode
import unit.util.mockClient

/**
 * Unit tests for the DataSourcesApi class.
 *
 * These tests verify that the DataSourcesApi correctly handles API requests
 * and responses for data source operations, including template listing.
 */
@Tags("Unit")
class DataSourcesApiTest :
    FunSpec({
        lateinit var api: DataSourcesApi
        lateinit var config: NotionConfig

        beforeTest {
            config = NotionConfig(apiToken = "test-token")
        }

        context("Template model serialization") {
            test("should deserialize templates response correctly") {
                val response = TestFixtures.DataSources.listTemplates().decode<TemplatesResponse>()

                response.objectType shouldBe "list"
                response.templates.size shouldBe 3
                response.hasMore shouldBe false
                response.nextCursor shouldBe null

                // Verify first template (default)
                val defaultTemplate = response.templates[0]
                defaultTemplate.id shouldBe "12345678-1234-1234-1234-123456789abc"
                defaultTemplate.name shouldBe "Project Template"
                defaultTemplate.isDefault shouldBe true

                // Verify second template
                val meetingTemplate = response.templates[1]
                meetingTemplate.id shouldBe "87654321-4321-4321-4321-cba987654321"
                meetingTemplate.name shouldBe "Meeting Notes Template"
                meetingTemplate.isDefault shouldBe false

                // Verify third template
                val taskTemplate = response.templates[2]
                taskTemplate.id shouldBe "abcdef12-3456-7890-abcd-ef1234567890"
                taskTemplate.name shouldBe "Task Template"
                taskTemplate.isDefault shouldBe false
            }
        }

        context("list templates") {
            test("should list templates successfully") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/data_sources/test-data-source-id/templates",
                            responseBody = TestFixtures.DataSources.listTemplatesAsString(),
                        )
                    }

                api = DataSourcesApi(client, config)
                val templates = api.listTemplates("test-data-source-id")

                templates.shouldBeInstanceOf<List<Template>>()
                templates.size shouldBe 3
                templates[0].name shouldBe "Project Template"
                templates[0].isDefault shouldBe true
                templates[1].name shouldBe "Meeting Notes Template"
                templates[1].isDefault shouldBe false
            }

            test("should list templates with name filter") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/data_sources/test-data-source-id/templates?name=Project",
                            responseBody = TestFixtures.DataSources.listTemplatesAsString(),
                        )
                    }

                api = DataSourcesApi(client, config)
                val templates = api.listTemplates("test-data-source-id", nameFilter = "Project")

                templates.shouldBeInstanceOf<List<Template>>()
                templates.size shouldBe 3
            }

            test("should handle pagination when listing templates") {
                // Create responses for pagination test
                val firstPageJson =
                    """
                    {
                      "object": "list",
                      "templates": [
                        {
                          "id": "template-1",
                          "name": "Template 1",
                          "is_default": true
                        }
                      ],
                      "has_more": true,
                      "next_cursor": "cursor-123"
                    }
                    """.trimIndent()

                val secondPageJson =
                    """
                    {
                      "object": "list",
                      "templates": [
                        {
                          "id": "template-2",
                          "name": "Template 2",
                          "is_default": false
                        }
                      ],
                      "has_more": false,
                      "next_cursor": null
                    }
                    """.trimIndent()

                val client =
                    mockClient {
                        // Second page first (more specific URL with cursor)
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "start_cursor=cursor-123",
                            responseBody = secondPageJson,
                        )
                        // First page (less specific, matches without cursor)
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/data_sources/test-data-source-id/templates",
                            responseBody = firstPageJson,
                        )
                    }

                api = DataSourcesApi(client, config)
                val templates = api.listTemplates("test-data-source-id")

                templates.size shouldBe 2
                templates[0].id shouldBe "template-1"
                templates[1].id shouldBe "template-2"
            }

            test("should handle API error when listing templates") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "*/v1/data_sources/*/templates*",
                            statusCode = HttpStatusCode.NotFound,
                        )
                    }

                api = DataSourcesApi(client, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.listTemplates("invalid-id")
                    }

                exception.code shouldBe "404"
                exception.status shouldBe 404
            }

            test("should handle 403 Forbidden for insufficient permissions") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "/v1/data_sources/some-data-source-id/templates",
                            statusCode = HttpStatusCode.Forbidden,
                        )
                    }

                api = DataSourcesApi(client, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.listTemplates("some-data-source-id")
                    }

                exception.code shouldBe "403"
                exception.status shouldBe 403
            }
        }
    })
