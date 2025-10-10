package integration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.users.UserType

/**
 * Integration tests for the Users API.
 *
 * These tests require:
 * - NOTION_API_TOKEN environment variable with a valid API token
 * - Optional: NOTION_TEST_USER_ID for testing user retrieval
 *
 * Note: The `retrieve()` and `list()` methods require the integration to have
 * user information capabilities. Tests will gracefully handle 403 Forbidden errors
 * if these capabilities are missing.
 */
@Tags("Integration", "RequiresApi")
class UsersIntegrationTest :
    StringSpec({
        if (!integrationTestEnvVarsAreSet("NOTION_API_TOKEN", "NOTION_TEST_USER_ID")) {
            "!(Skipped) Users integration tests" {
                println("⏭️ Skipping UsersIntegrationTest - missing environment variables")
            }
        } else {
            val client = NotionClient.create(NotionConfig(apiToken = System.getenv("NOTION_API_TOKEN")))

            "getCurrentUser should return bot user" {
                val user = client.users.getCurrentUser()

                // Validate response structure
                user.id.shouldNotBeBlank()
                user.objectType shouldBe "user"
                user.type.shouldNotBeNull()
                user.type shouldBe UserType.BOT

                // Bot users should have bot info
                user.bot.shouldNotBeNull()

                // Person info should be null for bots
                user.person shouldBe null

                // Owner info may or may not be present
                if (user.bot.owner != null) {
                    println("✓ Got bot user: ${user.name} (${user.id}) with owner: ${user.bot.owner.type}")
                } else {
                    println("✓ Got bot user: ${user.name} (${user.id}) - no owner info")
                }
            }

            val testRetrieveWithEnvUserId = "retrieve should get user by ID when NOTION_TEST_USER_ID is set"
            testRetrieveWithEnvUserId {
                val testUserId = System.getenv("NOTION_TEST_USER_ID")

                if (testUserId.isNullOrBlank()) {
                    println("⏭️ Skipping retrieve test - NOTION_TEST_USER_ID not set")
                    return@testRetrieveWithEnvUserId
                }

                try {
                    val user = client.users.retrieve(testUserId)

                    // Validate the response (normalize UUIDs for comparison - env var may not have hyphens)
                    val normalizedUserId = user.id.replace("-", "")
                    val normalizedTestUserId = testUserId.replace("-", "")
                    normalizedUserId shouldBe normalizedTestUserId
                    user.objectType shouldBe "user"
                    user.type.shouldNotBeNull()

                    println("✓ Retrieved user: ${user.name} (${user.id}), type: ${user.type}")
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("⚠️  Integration lacks user information capabilities (403 Forbidden)")
                    } else {
                        throw e
                    }
                }
            }

            "retrieve should get current bot by its own ID" {
                val currentUser = client.users.getCurrentUser()

                try {
                    val user = client.users.retrieve(currentUser.id)

                    // Should get the same user
                    user.id shouldBe currentUser.id
                    user.type shouldBe UserType.BOT

                    println("✓ Retrieved bot by ID: ${user.name}")
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("⚠️  Integration lacks user information capabilities (403 Forbidden)")
                    } else {
                        throw e
                    }
                }
            }

            "list should return users when user capabilities are available" {
                try {
                    val userList = client.users.list(pageSize = 10)

                    // Validate the response
                    userList.objectType shouldBe "list"
                    userList.results shouldNotBe null

                    println("✓ Listed ${userList.results.size} users (hasMore: ${userList.hasMore})")

                    if (userList.results.isNotEmpty()) {
                        val firstUser = userList.results[0]
                        firstUser.id.shouldNotBeBlank()
                        firstUser.type.shouldNotBeNull()
                    }
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("⚠️  Integration lacks user information capabilities (403 Forbidden)")
                    } else {
                        throw e
                    }
                }
            }

            "list should handle pagination with cursor" {
                try {
                    val firstPage = client.users.list(pageSize = 1)

                    if (firstPage.hasMore && firstPage.nextCursor != null) {
                        val secondPage = client.users.list(startCursor = firstPage.nextCursor, pageSize = 1)

                        secondPage.objectType shouldBe "list"
                        secondPage.results shouldNotBe null

                        // If both pages have results, they should be different users
                        if (firstPage.results.isNotEmpty() && secondPage.results.isNotEmpty()) {
                            firstPage.results[0].id shouldNotBe secondPage.results[0].id
                            println("✓ Pagination works: page 1 user ${firstPage.results[0].id}, page 2 user ${secondPage.results[0].id}")
                        }
                    } else {
                        println("⚠️  Only one page of users available, can't test pagination")
                    }
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("⚠️  Integration lacks user information capabilities (403 Forbidden)")
                    } else {
                        throw e
                    }
                }
            }

            "list should validate page size bounds" {
                shouldThrow<IllegalArgumentException> {
                    client.users.list(pageSize = 0)
                }

                shouldThrow<IllegalArgumentException> {
                    client.users.list(pageSize = 101)
                }

                println("✓ Page size validation works correctly")
            }

            "retrieve should handle invalid user ID" {
                val exception =
                    shouldThrow<NotionException.ApiError> {
                        client.users.retrieve("invalid-user-id-12345678")
                    }

                // Should be either 404 (not found) or 400 (invalid ID format) or 403 (no capabilities)
                (exception.status in listOf(400, 403, 404)) shouldBe true

                println("✓ Correctly handles invalid user ID with status ${exception.status}")
            }
        }
    })
