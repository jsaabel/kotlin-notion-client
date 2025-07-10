package no.saabelit.kotlinnotionclient

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.saabelit.kotlinnotionclient.api.DatabasesApi
import no.saabelit.kotlinnotionclient.api.PagesApi
import no.saabelit.kotlinnotionclient.api.UsersApi
import no.saabelit.kotlinnotionclient.config.NotionConfig

/**
 * Main entry point for the Notion API client.
 *
 * This client provides access to all Notion API endpoints through a clean,
 * type-safe Kotlin interface. It uses suspend functions for non-blocking I/O
 * and follows a facade pattern where different API areas are accessed through
 * dedicated properties.
 *
 * Example usage:
 * ```kotlin
 * val client = NotionClient.create(
 *     NotionConfig(token = "your-token-here")
 * )
 *
 * // Use the client
 * val user = client.users.getCurrentUser()
 *
 * // Don't forget to close when done
 * client.close()
 * ```
 */
class NotionClient private constructor(
    private val httpClient: HttpClient,
    val config: NotionConfig,
) {
    // API delegates - each area has its own specialized client
    val users = UsersApi(httpClient, config)
    val pages = PagesApi(httpClient, config)
    val databases = DatabasesApi(httpClient, config)

    // TODO: Add remaining API delegates as we implement them
    // val blocks = BlocksApi(httpClient)

    companion object {
        /**
         * Creates a new NotionClient instance with the provided configuration.
         *
         * @param config Configuration object containing API token and other settings
         * @return Configured NotionClient instance
         */
        fun create(config: NotionConfig): NotionClient {
            val httpClient =
                HttpClient(CIO) {
                    // Install JSON serialization
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                prettyPrint = config.prettyPrint
                            },
                        )
                    }

                    // Install authentication
                    install(Auth) {
                        bearer {
                            loadTokens {
                                BearerTokens(config.token, "")
                            }
                        }
                    }

                    // Install logging if requested
                    if (config.logLevel != LogLevel.NONE) {
                        install(Logging) {
                            logger =
                                object : Logger {
                                    override fun log(message: String) {
                                        println(message)
                                    }
                                }
                            level = config.logLevel
                        }
                    }

                    // Set timeouts
                    install(HttpTimeout) {
                        requestTimeoutMillis = config.requestTimeout.inWholeMilliseconds
                        connectTimeoutMillis = config.connectTimeout.inWholeMilliseconds
                        socketTimeoutMillis = config.socketTimeout.inWholeMilliseconds
                    }

                    // Set default request configuration
                    defaultRequest {
                        url(config.baseUrl)
                        headers.append("Notion-Version", config.apiVersion)
                        headers.append("User-Agent", config.userAgent)
                    }
                }

            return NotionClient(httpClient, config)
        }
    }

    /**
     * Closes the HTTP client and releases associated resources.
     * Call this when you're done using the client.
     */
    suspend fun close() {
        httpClient.close()
    }
}
