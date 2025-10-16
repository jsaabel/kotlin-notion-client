@file:Suppress("unused")

package it.saabel.kotlinnotionclient

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
import it.saabel.kotlinnotionclient.api.BlocksApi
import it.saabel.kotlinnotionclient.api.CommentsApi
import it.saabel.kotlinnotionclient.api.DataSourcesApi
import it.saabel.kotlinnotionclient.api.DatabasesApi
import it.saabel.kotlinnotionclient.api.EnhancedFileUploadApi
import it.saabel.kotlinnotionclient.api.FileUploadApi
import it.saabel.kotlinnotionclient.api.PagesApi
import it.saabel.kotlinnotionclient.api.SearchApi
import it.saabel.kotlinnotionclient.api.UsersApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.ratelimit.NotionRateLimit
import kotlinx.serialization.json.Json

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
 * // Simple initialization
 * val client = NotionClient("your-api-token")
 *
 * // Advanced initialization with custom config
 * val client = NotionClient(
 *     NotionConfig(
 *         apiToken = "your-token-here",
 *         logLevel = LogLevel.INFO
 *     )
 * )
 *
 * // Access different API areas
 * val user = client.users.me()
 * val page = client.pages.retrieve("page-id")
 * val database = client.databases.retrieve("db-id")
 * val dataSource = client.dataSources.retrieve("data-source-id")
 * val block = client.blocks.retrieve("block-id")
 * val comments = client.comments.retrieve("block-id")
 *
 * // Don't forget to close when done
 * client.close()
 * ```
 */
class NotionClient
    @JvmOverloads
    constructor(
        config: NotionConfig,
        internal val client: HttpClient? = null,
    ) {
        /**
         * Convenience constructor for simple initialization with just an API token.
         *
         * @param apiToken The Notion API token
         */
        constructor(apiToken: String) : this(NotionConfig(apiToken = apiToken), null)

        private val httpClient: HttpClient = client ?: createHttpClient(config)

        // API delegates - each area has its own specialized client
        val users = UsersApi(httpClient, config)
        val pages = PagesApi(httpClient, config)
        val databases = DatabasesApi(httpClient, config)
        val dataSources = DataSourcesApi(httpClient, config)
        val blocks = BlocksApi(httpClient, config)
        val comments = CommentsApi(httpClient, config)
        val search = SearchApi(httpClient, config)
        val fileUploads = FileUploadApi(httpClient, config)

        /**
         * Enhanced file upload API with advanced features like progress tracking,
         * automatic chunking, retry logic, and comprehensive error handling.
         */
        val enhancedFileUploads = EnhancedFileUploadApi(httpClient, config, fileUploads)

        companion object {
            /**
             * Creates a new NotionClient instance with just an API token.
             * Uses default configuration with no logging.
             *
             * Alternative to primary constructor. Both patterns are supported:
             * - `NotionClient(apiToken)` (constructor)
             * - `NotionClient.create(apiToken)` (factory method)
             *
             * @param apiToken The Notion API token
             * @return Configured NotionClient instance
             */
            fun create(apiToken: String): NotionClient = NotionClient(apiToken)

            /**
             * Creates a new NotionClient instance with the provided configuration.
             *
             * Alternative to primary constructor. Both patterns are supported:
             * - `NotionClient(config)` (constructor)
             * - `NotionClient.create(config)` (factory method)
             *
             * @param config Configuration object containing API token and other settings
             * @return Configured NotionClient instance
             */
            fun create(config: NotionConfig): NotionClient = NotionClient(config)

            /**
             * Creates a NotionClient with a custom HttpClient (for testing).
             *
             * @param httpClient Custom HTTP client instance
             * @param config Configuration object
             * @return Configured NotionClient instance
             */
            internal fun createWithClient(
                httpClient: HttpClient,
                config: NotionConfig,
            ): NotionClient = NotionClient(config, httpClient)

            /**
             * Creates the configured HTTP client for the Notion API.
             *
             * @param config Configuration object containing API token and settings
             * @return Configured HttpClient instance
             */
            private fun createHttpClient(config: NotionConfig): HttpClient =
                HttpClient(CIO) {
                    // Install JSON serialization
                    install(ContentNegotiation) {
                        // TODO: Consider adding info on why these options are chosen
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                prettyPrint = config.prettyPrint
                                encodeDefaults = true
                                explicitNulls = false
                            },
                        )
                    }

                    // Install authentication
                    install(Auth) {
                        bearer {
                            loadTokens {
                                BearerTokens(config.apiToken, "")
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

                    // Install rate limiting if enabled
                    if (config.enableRateLimit) {
                        install(NotionRateLimit) {
                            strategy = config.rateLimitConfig.strategy
                            maxRetries = config.rateLimitConfig.maxRetries
                            baseDelayMs = config.rateLimitConfig.baseDelayMs
                            maxDelayMs = config.rateLimitConfig.maxDelayMs
                            jitterFactor = config.rateLimitConfig.jitterFactor
                            respectRetryAfter = config.rateLimitConfig.respectRetryAfter
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
        }

        /**
         * Closes the HTTP client and releases associated resources.
         * Call this when you're done using the client.
         */
        fun close() {
            httpClient.close()
        }
    }
