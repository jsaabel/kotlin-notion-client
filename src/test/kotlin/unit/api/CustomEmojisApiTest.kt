package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.saabel.kotlinnotionclient.api.CustomEmojisApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.base.CustomEmojiList
import it.saabel.kotlinnotionclient.models.base.CustomEmojiObject
import unit.util.TestFixtures
import unit.util.decode
import unit.util.mockClient

/**
 * Unit tests for CustomEmojisApi and the CustomEmojiList model.
 */
@Tags("Unit")
class CustomEmojisApiTest :
    FunSpec({
        val config = NotionConfig(apiToken = "test-token")

        context("CustomEmojiList model deserialization") {
            test("should deserialize the list response correctly") {
                val list = TestFixtures.CustomEmojis.listCustomEmojis().decode<CustomEmojiList>()

                list.results.size shouldBe 2
                list.hasMore shouldBe false
                list.nextCursor shouldBe null
            }

            test("should deserialize first emoji correctly") {
                val list = TestFixtures.CustomEmojis.listCustomEmojis().decode<CustomEmojiList>()

                val first = list.results[0]
                first.shouldBeInstanceOf<CustomEmojiObject>()
                first.id shouldBe "45ce454c-d427-4f53-9489-e5d0f3d1db6b"
                first.name shouldBe "bufo"
                first.url shouldBe "https://files.notion.com/custom_emoji/bufo.png"
            }

            test("should deserialize second emoji correctly") {
                val list = TestFixtures.CustomEmojis.listCustomEmojis().decode<CustomEmojiList>()

                val second = list.results[1]
                second.id shouldBe "7a1b2c3d-e4f5-6789-abcd-ef0123456789"
                second.name shouldBe "bufo-cool"
            }

            test("should deserialize empty results list correctly") {
                val jsonStr = """{"object":"list","type":"custom_emoji","results":[],"has_more":false}"""
                val list = TestFixtures.json.decodeFromString<CustomEmojiList>(jsonStr)

                list.results.size shouldBe 0
                list.hasMore shouldBe false
            }

            test("should deserialize pagination fields when has_more is true") {
                val jsonStr = """{
                    "object": "list",
                    "type": "custom_emoji",
                    "results": [{"id": "abc", "name": "test", "url": "https://example.com"}],
                    "has_more": true,
                    "next_cursor": "cursor-abc"
                }"""
                val list = TestFixtures.json.decodeFromString<CustomEmojiList>(jsonStr)

                list.hasMore shouldBe true
                list.nextCursor shouldBe "cursor-abc"
            }
        }

        context("CustomEmojisApi.list()") {
            test("should list custom emojis successfully") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/custom_emojis",
                            responseBody = TestFixtures.CustomEmojis.listCustomEmojisAsString(),
                        )
                    }
                val api = CustomEmojisApi(client, config)

                val result = api.list()

                result.results.size shouldBe 2
                result.hasMore shouldBe false
            }

            test("should include name query param when name is provided") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/custom_emojis?name=bufo",
                            responseBody = TestFixtures.CustomEmojis.listCustomEmojisAsString(),
                        )
                    }
                val api = CustomEmojisApi(client, config)

                // Should succeed — mock matches because URL contains "name=bufo"
                val result = api.list(name = "bufo")
                result.results.size shouldBe 2
            }

            test("should include start_cursor query param when provided") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/custom_emojis?start_cursor=my-cursor",
                            responseBody = TestFixtures.CustomEmojis.listCustomEmojisAsString(),
                        )
                    }
                val api = CustomEmojisApi(client, config)

                val result = api.list(startCursor = "my-cursor")
                result.results.size shouldBe 2
            }

            test("should include page_size query param when provided") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/custom_emojis?page_size=10",
                            responseBody = TestFixtures.CustomEmojis.listCustomEmojisAsString(),
                        )
                    }
                val api = CustomEmojisApi(client, config)

                val result = api.list(pageSize = 10)
                result.results.size shouldBe 2
            }

            test("should throw IllegalArgumentException for page size below 1") {
                val api = CustomEmojisApi(mockClient { }, config)

                shouldThrow<IllegalArgumentException> {
                    api.list(pageSize = 0)
                }
            }

            test("should throw IllegalArgumentException for page size above 100") {
                val api = CustomEmojisApi(mockClient { }, config)

                shouldThrow<IllegalArgumentException> {
                    api.list(pageSize = 101)
                }
            }

            test("should throw ApiError on 403 response") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "*/v1/custom_emojis*",
                            statusCode = HttpStatusCode.Forbidden,
                        )
                    }
                val api = CustomEmojisApi(client, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.list()
                    }

                exception.status shouldBe 403
            }

            test("should throw ApiError on 401 response") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "*/v1/custom_emojis*",
                            statusCode = HttpStatusCode.Unauthorized,
                        )
                    }
                val api = CustomEmojisApi(client, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.list()
                    }

                exception.status shouldBe 401
            }
        }
    })
