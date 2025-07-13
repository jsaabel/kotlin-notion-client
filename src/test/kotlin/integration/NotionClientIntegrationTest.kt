package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.client.plugins.logging.LogLevel
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.users.UserType

/**
 * Integration test for the Notion Kotlin Client.
 *
 * To run this test:
 * 1. Get a Notion API token from https://developers.notion.com/
 * 2. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 3. Run: ./gradlew integrationTest
 */
@Tags("Integration", "RequiresApi")
class NotionClientIntegrationTest :
    BehaviorSpec({

        given("a real Notion API token") {
            val token = System.getenv("NOTION_API_TOKEN")

            `when`("the token is not set") {
                then("skip the test") {
                    if (token.isNullOrBlank()) {
                        println("⏭️ Skipping integration test - NOTION_API_TOKEN not set")
                        return@then
                    }
                }
            }

            `when`("calling the /users/me endpoint") {
                val client =
                    NotionClient.create(
                        NotionConfig(
                            apiToken = token!!,
                            logLevel = LogLevel.INFO,
                        ),
                    )

                then("should successfully retrieve user information") {
                    try {
                        println("📡 Calling /users/me endpoint...")
                        println("🔍 Debug info:")
                        println("   Base URL: ${client.config.baseUrl}")
                        println("   API Version: ${client.config.apiVersion}")
                        println("   Token starts with: ${token.take(10)}...")

                        val user = client.users.getCurrentUser()

                        // Validate the response structure
                        user.id.shouldNotBeBlank()
                        user.objectType shouldBe "user"
                        user.type shouldNotBe null

                        println("✅ Success! Retrieved user information:")
                        println("   ID: ${user.id}")
                        println("   Name: ${user.name}")
                        println("   Type: ${user.type}")
                        println("   Avatar: ${user.avatarUrl ?: "No avatar"}")

                        if (user.type == UserType.BOT && user.bot != null) {
                            println("   🤖 Bot Information:")
                            println("      Owner Type: ${user.bot.owner.type}")
                            user.bot.owner.user?.let { ownerUser ->
                                println("      Owner Name: ${ownerUser.name}")
                                println("      Owner ID: ${ownerUser.id}")
                            }
                        }
                    } catch (e: NotionException.AuthenticationError) {
                        println("❌ Authentication failed: ${e.message}")
                        println("   Check your NOTION_API_TOKEN is valid")
                        throw e
                    } catch (e: NotionException.ApiError) {
                        println("❌ API Error: ${e.message}")
                        println("   Status: ${e.status}")
                        println("   Code: ${e.code}")
                        throw e
                    } catch (e: NotionException.NetworkError) {
                        println("❌ Network Error: ${e.message}")
                        println("   Check your internet connection")
                        throw e
                    } finally {
                        client.close()
                        println("🔒 Client closed")
                    }
                }
            }
        }
    })
