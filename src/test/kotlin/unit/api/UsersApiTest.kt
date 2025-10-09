package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import no.saabelit.kotlinnotionclient.api.UsersApi
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.users.PersonInfo
import no.saabelit.kotlinnotionclient.models.users.User
import no.saabelit.kotlinnotionclient.models.users.UserList
import no.saabelit.kotlinnotionclient.models.users.UserType
import unit.util.TestFixtures
import unit.util.decode
import unit.util.mockClient

/**
 * Unit tests for the UsersApi class.
 *
 * These tests verify that the UsersApi correctly handles API requests
 * and responses for user operations.
 */
@Tags("Unit")
class UsersApiTest :
    FunSpec({
        lateinit var api: UsersApi
        lateinit var config: NotionConfig

        beforeTest {
            config = NotionConfig(apiToken = "test-token")
        }

        context("User model serialization") {
            test("should deserialize bot user response correctly") {
                val user = TestFixtures.Users.retrieveBotUser().decode<User>()

                user.id shouldBe "4666301e-ddb5-45de-b2f9-88eec463052b"
                user.objectType shouldBe "user"
                user.type shouldBe UserType.BOT
                user.name shouldBe "My Integration Bot"
                user.avatarUrl shouldBe "https://secure.notion-static.com/xxxx/bot-avatar.png"
                user.bot shouldNotBe null
                user.bot?.owner?.type shouldNotBe null
                user.person shouldBe null
            }

            test("should deserialize person user response correctly") {
                val user = TestFixtures.Users.retrievePersonUser().decode<User>()

                user.id shouldBe "d40e767c-d7af-4b18-a86d-55c61f1e39a4"
                user.objectType shouldBe "user"
                user.type shouldBe UserType.PERSON
                user.name shouldBe "Avocado Lovelace"
                user.avatarUrl shouldBe "https://secure.notion-static.com/e6a352a8-8381-44d0-a1dc-9ed80e62b53d.jpg"
                user.person shouldNotBe null
                user.person shouldBe PersonInfo(email = "avo@example.org")
                user.bot shouldBe null
            }

            test("should deserialize user list response correctly") {
                val userList = TestFixtures.Users.listUsers().decode<UserList>()

                userList.objectType shouldBe "list"
                userList.results.size shouldBe 2
                userList.hasMore shouldBe true
                userList.nextCursor shouldBe "fe2cc560-036c-44cd-90e8-294d5a74cebc"

                // First user is a person
                val personUser = userList.results[0]
                personUser.type shouldBe UserType.PERSON
                personUser.person?.email shouldBe "avo@example.org"

                // Second user is a bot
                val botUser = userList.results[1]
                botUser.type shouldBe UserType.BOT
                botUser.bot shouldNotBe null
            }
        }

        context("retrieve user") {
            test("should retrieve user successfully") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/users/d40e767c-d7af-4b18-a86d-55c61f1e39a4",
                            responseBody = TestFixtures.Users.retrievePersonUserAsString(),
                        )
                    }

                api = UsersApi(client, config)
                val user = api.retrieve("d40e767c-d7af-4b18-a86d-55c61f1e39a4")

                user.shouldBeInstanceOf<User>()
                user.id shouldBe "d40e767c-d7af-4b18-a86d-55c61f1e39a4"
                user.type shouldBe UserType.PERSON
                user.name shouldBe "Avocado Lovelace"
            }

            test("should handle API error when retrieving user") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "*/v1/users/*",
                            statusCode = HttpStatusCode.NotFound,
                        )
                    }

                api = UsersApi(client, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.retrieve("invalid-id")
                    }

                exception.code shouldBe "404"
                exception.status shouldBe 404
            }

            test("should handle 403 Forbidden for missing user capabilities") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "*/v1/users/*",
                            statusCode = HttpStatusCode.Forbidden,
                        )
                    }

                api = UsersApi(client, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.retrieve("some-user-id")
                    }

                exception.code shouldBe "403"
                exception.status shouldBe 403
            }
        }

        context("list users") {
            test("should list users successfully") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/users",
                            responseBody = TestFixtures.Users.listUsersAsString(),
                        )
                    }

                api = UsersApi(client, config)
                val userList = api.list()

                userList.shouldBeInstanceOf<UserList>()
                userList.results.size shouldBe 2
                userList.hasMore shouldBe true
                userList.nextCursor shouldBe "fe2cc560-036c-44cd-90e8-294d5a74cebc"
            }

            test("should list users with pagination parameters") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/users?page_size=50",
                            responseBody = TestFixtures.Users.listUsersAsString(),
                        )
                    }

                api = UsersApi(client, config)
                val userList = api.list(pageSize = 50)

                userList.shouldBeInstanceOf<UserList>()
                userList.results.size shouldBe 2
            }

            test("should list users with start cursor") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/users?start_cursor=some-cursor",
                            responseBody = TestFixtures.Users.listUsersAsString(),
                        )
                    }

                api = UsersApi(client, config)
                val userList = api.list(startCursor = "some-cursor")

                userList.shouldBeInstanceOf<UserList>()
            }

            test("should validate page size is within bounds") {
                api = UsersApi(mockClient {}, config)

                shouldThrow<IllegalArgumentException> {
                    api.list(pageSize = 0)
                }

                shouldThrow<IllegalArgumentException> {
                    api.list(pageSize = 101)
                }
            }

            test("should handle API error when listing users") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "*/v1/users*",
                            statusCode = HttpStatusCode.Forbidden,
                        )
                    }

                api = UsersApi(client, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.list()
                    }

                exception.code shouldBe "403"
                exception.status shouldBe 403
            }
        }

        context("get current user") {
            test("should get current bot user successfully") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/users/me",
                            responseBody = TestFixtures.Users.retrieveBotUserAsString(),
                        )
                    }

                api = UsersApi(client, config)
                val user = api.getCurrentUser()

                user.shouldBeInstanceOf<User>()
                user.id shouldBe "4666301e-ddb5-45de-b2f9-88eec463052b"
                user.type shouldBe UserType.BOT
                user.name shouldBe "My Integration Bot"
                user.bot shouldNotBe null
            }

            test("should handle API error when getting current user") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "*/v1/users/me",
                            statusCode = HttpStatusCode.Unauthorized,
                        )
                    }

                api = UsersApi(client, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.getCurrentUser()
                    }

                exception.code shouldBe "401"
                exception.status shouldBe 401
            }
        }
    })
