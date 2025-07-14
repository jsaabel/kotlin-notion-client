package no.saabelit.kotlinnotionclient.config

import io.ktor.client.plugins.logging.LogLevel
import no.saabelit.kotlinnotionclient.ratelimit.RateLimitConfig
import no.saabelit.kotlinnotionclient.validation.ValidationConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration class for the Notion API client.
 *
 * This class encapsulates all configuration options for the client,
 * including authentication, timeouts, logging, and API settings.
 *
 * @param apiToken The Notion API token (required)
 * @param baseUrl The base URL for the Notion API
 * @param apiVersion The API version to use (sent in Notion-Version header)
 * @param userAgent The user agent string to send with requests
 * @param requestTimeout Maximum time to wait for a complete request
 * @param connectTimeout Maximum time to wait for connection establishment
 * @param socketTimeout Maximum time to wait for socket operations
 * @param logLevel The logging level for HTTP requests/responses
 * @param prettyPrint Whether to format JSON responses for debugging
 * @param rateLimitConfig Configuration for rate limiting behavior
 * @param enableRateLimit Whether to enable automatic rate limiting
 * @param validationConfig Configuration for request validation behavior
 */
data class NotionConfig(
    val apiToken: String = System.getenv("NOTION_API_TOKEN"),
    val baseUrl: String = "https://api.notion.com/v1",
    val apiVersion: String = "2022-06-28",
    val userAgent: String = "kotlin-notion-client/0.0.1",
    val requestTimeout: Duration = 30.seconds,
    val connectTimeout: Duration = 10.seconds,
    val socketTimeout: Duration = 30.seconds,
    val logLevel: LogLevel = LogLevel.NONE,
    val prettyPrint: Boolean = false,
    val rateLimitConfig: RateLimitConfig = RateLimitConfig.BALANCED,
    val enableRateLimit: Boolean = true,
    val validationConfig: ValidationConfig = ValidationConfig.default(),
) {
    init {
        require(apiToken.isNotBlank()) { "API token cannot be blank" }
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank" }
        require(apiVersion.isNotBlank()) { "API version cannot be blank" }
    }
}
