package examples

import integration.integrationTestEnvVarsAreSet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.exceptions.NotionException
import no.saabelit.kotlinnotionclient.models.users.UserType

/**
 * Users API Examples
 *
 * This file contains validated examples for the Users API, suitable for documentation.
 * Each example has been tested against the live Notion API.
 *
 * Prerequisites:
 * - Set environment variable: export NOTION_RUN_INTEGRATION_TESTS="true"
 * - Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * - Optional: export NOTION_TEST_USER_ID="user_id" (for user retrieval examples)
 *
 * Note: retrieve() and list() methods require user information capabilities.
 * Examples will gracefully handle 403 Forbidden errors if these capabilities are missing.
 */
@Tags("Integration", "RequiresApi", "Examples")
class UsersExamples :
    StringSpec({

        if (!integrationTestEnvVarsAreSet("NOTION_API_TOKEN", "NOTION_TEST_USER_ID")) {
            "!(Skipped) Users examples" {
                println("⏭️ Skipping - set NOTION_RUN_INTEGRATION_TESTS=true and NOTION_API_TOKEN")
            }
        } else {

            val token = System.getenv("NOTION_API_TOKEN")!!
            val testUserId = System.getenv("NOTION_TEST_USER_ID")

            val notion = NotionClient.create(NotionConfig(apiToken = token))

            afterSpec {
                notion.close()
            }

            "Example 1: Get current bot user" {
                // Example from docs: Basic: Get Current Bot User
                val botUser = notion.users.getCurrentUser()
                println("Bot name: ${botUser.name}")
                println("Bot ID: ${botUser.id}")
                println("Type: ${botUser.type}") // Will be UserType.BOT
            }

            "Example 2: Validate API token at startup" {
                // Example from docs: Validate API Token at Startup
                suspend fun initializeNotionClient(token: String): NotionClient {
                    val client = NotionClient.create(NotionConfig(apiToken = token))

                    try {
                        val botUser = client.users.getCurrentUser()
                        println("✓ Authenticated as: ${botUser.name}")
                        return client
                    } catch (e: NotionException.AuthenticationError) {
                        throw IllegalStateException("Invalid API token", e)
                    }
                }

                val client = initializeNotionClient(token)
                println("Client initialized successfully")
                client.close()
            }

            val example3 = "Example 3: Retrieve a specific user by ID"
            example3 {
                // Example from docs: Retrieve a Specific User by ID
                if (testUserId.isNullOrBlank()) {
                    println("⏭️ Skipping - NOTION_TEST_USER_ID not set")
                    return@example3
                }

                try {
                    val user = notion.users.retrieve(testUserId)

                    when (user.type) {
                        UserType.PERSON -> {
                            println("Person: ${user.name}")
                            user.person?.email?.let { email ->
                                println("Email: $email")
                            }
                        }
                        UserType.BOT -> {
                            println("Bot: ${user.name}")
                        }
                        else -> println("Unknown user type")
                    }
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("Integration lacks user information capabilities")
                    } else {
                        throw e
                    }
                }
            }

            "Example 4: List all users in workspace" {
                // Example from docs: List All Users in Workspace
                try {
                    val userList = notion.users.list()

                    userList.results.forEach { user ->
                        println("${user.name} (${user.type})")
                    }

                    if (userList.hasMore) {
                        println("More users available, cursor: ${userList.nextCursor}")
                    }
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("Integration lacks user information capabilities")
                    } else {
                        throw e
                    }
                }
            }

            "Example 5: List with pagination" {
                // Example from docs: List with Pagination
                try {
                    // Get first page
                    val firstPage = notion.users.list(pageSize = 50)
                    firstPage.results.forEach { user ->
                        println(user.name)
                    }

                    // Get next page if available
                    if (firstPage.hasMore && firstPage.nextCursor != null) {
                        val secondPage =
                            notion.users.list(
                                startCursor = firstPage.nextCursor,
                                pageSize = 50,
                            )
                        println("Got ${secondPage.results.size} more users")
                    }
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("Integration lacks user information capabilities")
                    } else {
                        throw e
                    }
                }
            }

            val example6 = "Example 6: Working with person vs bot users"
            example6 {
                // Example from docs: Working with Person vs Bot Users
                if (testUserId.isNullOrBlank()) {
                    println("⏭️ Skipping - NOTION_TEST_USER_ID not set")
                    return@example6
                }

                try {
                    val user = notion.users.retrieve(testUserId)

                    when (user.type) {
                        UserType.PERSON -> {
                            println("This is a person user")
                            val email = user.person?.email ?: "Email not available"
                            println("Email: $email")
                        }
                        UserType.BOT -> {
                            println("This is a bot integration")
                            user.bot?.owner?.let { owner ->
                                println("Owner type: ${owner.type}")
                            }
                        }
                        null -> println("User type not specified")
                    }
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("Integration lacks user information capabilities")
                    } else {
                        throw e
                    }
                }
            }

            val example7 = "Example 7: Graceful handling of missing capabilities"
            example7 {
                // Example from docs: Graceful Handling of Missing Capabilities
                suspend fun getUserInfo(
                    client: NotionClient,
                    userId: String,
                ): no.saabelit.kotlinnotionclient.models.users.User? =
                    try {
                        client.users.retrieve(userId)
                    } catch (e: NotionException.ApiError) {
                        when (e.status) {
                            403 -> {
                                println("Integration lacks user capabilities")
                                null
                            }
                            404 -> {
                                println("User not found")
                                null
                            }
                            else -> throw e
                        }
                    }

                if (testUserId.isNullOrBlank()) {
                    println("⏭️ Skipping - NOTION_TEST_USER_ID not set")
                    return@example7
                }

                val user = getUserInfo(notion, testUserId)
                if (user != null) {
                    println("Retrieved user: ${user.name}")
                } else {
                    println("Could not retrieve user (missing capabilities or not found)")
                }
            }

            "Example 8: Iterate through all users" {
                // Example from docs: Iterate Through All Users
                suspend fun getAllUsers(client: NotionClient): List<no.saabelit.kotlinnotionclient.models.users.User> {
                    val allUsers = mutableListOf<no.saabelit.kotlinnotionclient.models.users.User>()
                    var cursor: String? = null

                    do {
                        val page = client.users.list(startCursor = cursor, pageSize = 100)
                        allUsers.addAll(page.results)
                        cursor = page.nextCursor
                    } while (page.hasMore)

                    return allUsers
                }

                try {
                    val allUsers = getAllUsers(notion)
                    println("Retrieved ${allUsers.size} total users")
                    allUsers.forEach { user ->
                        println("- ${user.name} (${user.type})")
                    }
                } catch (e: NotionException.ApiError) {
                    if (e.status == 403) {
                        println("Integration lacks user information capabilities")
                    } else {
                        throw e
                    }
                }
            }

            val example9 = "Example 9: Error handling"
            example9 {
                // Example from docs: Error Handling
                if (testUserId.isNullOrBlank()) {
                    println("⏭️ Skipping - NOTION_TEST_USER_ID not set")
                    return@example9
                }

                try {
                    val user = notion.users.retrieve(testUserId)
                    println("Successfully retrieved user: ${user.name}")
                } catch (_: NotionException.AuthenticationError) {
                    println("Authentication failed: Check your API token")
                } catch (e: NotionException.ApiError) {
                    when (e.status) {
                        403 -> println("Missing user information capabilities")
                        404 -> println("User not found")
                        429 -> println("Rate limited - wait and retry")
                        else -> println("API error: ${e.details}")
                    }
                } catch (e: NotionException.NetworkError) {
                    println("Network error: ${e.cause?.message}")
                } catch (e: IllegalArgumentException) {
                    println("Invalid parameter: ${e.message}")
                }
            }
        }
    })
