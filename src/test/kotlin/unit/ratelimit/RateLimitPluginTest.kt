package unit.ratelimit

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.saabel.kotlinnotionclient.api.FileUploadApi
import it.saabel.kotlinnotionclient.api.SearchApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.files.CreateFileUploadRequest
import it.saabel.kotlinnotionclient.models.search.SearchRequest
import it.saabel.kotlinnotionclient.ratelimit.NotionRateLimit
import it.saabel.kotlinnotionclient.ratelimit.RateLimitConfig
import kotlinx.serialization.json.Json
import unit.util.TestFixtures
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

/**
 * Verifies that [NotionRateLimit], now a `createClientPlugin` registered on the `Send` phase,
 * transparently intercepts **every** outbound request — including endpoints that were never
 * wrapped in the old `executeWithRateLimit` helper (Search, FileUpload).
 *
 * The mock engine returns `429` a fixed number of times before succeeding; if the request flows
 * through the plugin, the call succeeds after the expected number of attempts.
 */
@Tags("Unit")
class RateLimitPluginTest :
    FunSpec({

        // Fast, deterministic backoff so the retry loop adds negligible latency to the suite.
        fun clientThatFailsThenSucceeds(
            counter: AtomicInteger,
            failures: Int,
            successBody: String,
            retries: Int = 3,
        ): HttpClient =
            HttpClient(
                MockEngine { _ ->
                    val attempt = counter.incrementAndGet()
                    if (attempt <= failures) {
                        respond(
                            content = """{"object":"error","status":429,"code":"rate_limited","message":"slow down"}""",
                            status = HttpStatusCode.TooManyRequests,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    } else {
                        respond(
                            content = successBody,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    }
                },
            ) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                install(NotionRateLimit) {
                    rateLimitConfig =
                        RateLimitConfig(
                            maxRetries = retries,
                            retryBaseDelay = 1.milliseconds,
                            retryMaxDelay = 5.milliseconds,
                            jitterFactor = 0.0,
                        )
                }
            }

        val config = NotionConfig(apiToken = "test-token")

        test("SearchApi.search retries through the plugin and succeeds after 429s") {
            val counter = AtomicInteger(0)
            val client =
                clientThatFailsThenSucceeds(
                    counter = counter,
                    failures = 2,
                    successBody = TestFixtures.Search.searchByTitleAsString(),
                )
            val searchApi = SearchApi(client, config)

            val response = searchApi.search(SearchRequest(query = "kale"))

            // 2 rejected attempts + 1 successful = 3 trips through the Send pipeline.
            counter.get() shouldBe 3
            response.results.isNotEmpty() shouldBe true

            client.close()
        }

        test("FileUploadApi.createFileUpload retries through the plugin and succeeds after a 429") {
            val counter = AtomicInteger(0)
            val client =
                clientThatFailsThenSucceeds(
                    counter = counter,
                    failures = 1,
                    successBody = TestFixtures.FileUploads.createFileUploadAsString(),
                )
            val fileUploadApi = FileUploadApi(client, config)

            val upload =
                fileUploadApi.createFileUpload(
                    CreateFileUploadRequest(filename = "test.txt", contentType = "text/plain"),
                )

            counter.get() shouldBe 2
            upload.id.isNotEmpty() shouldBe true

            client.close()
        }

        test("plugin stops retrying once maxRetries is exhausted and surfaces the 429") {
            val counter = AtomicInteger(0)
            // Always 429; with maxRetries = 2 we expect 1 initial + 2 retries = 3 attempts.
            val client =
                clientThatFailsThenSucceeds(
                    counter = counter,
                    failures = Int.MAX_VALUE,
                    successBody = "{}",
                    retries = 2,
                )
            val fileUploadApi = FileUploadApi(client, config)

            val thrown =
                runCatching {
                    fileUploadApi.createFileUpload(
                        CreateFileUploadRequest(filename = "test.txt", contentType = "text/plain"),
                    )
                }.exceptionOrNull()

            counter.get() shouldBe 3
            thrown.shouldBeInstanceOf<NotionException.ApiError>()
            thrown.status shouldBe 429

            client.close()
        }
    })
