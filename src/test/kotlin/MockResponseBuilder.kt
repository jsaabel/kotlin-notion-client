@file:Suppress("unused")

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * DSL function for easily building mock clients with official Notion API sample data.
 */
fun mockClient(builder: MockClientBuilder.() -> Unit): HttpClient = MockClientBuilder().apply(builder).build()

class MockClientBuilder {
    private val handlers = mutableListOf<MockRequestHandler>()

    /**
     * Add a page retrieve response using official sample data.
     */
    fun addPageRetrieveResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Get && request.url.toString().contains("/v1/pages/")) {
                respond(
                    content = TestFixtures.Pages.retrievePageAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Page not found")
            }
        }
    }

    /**
     * Add a database retrieve response using official sample data.
     */
    fun addDatabaseRetrieveResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Get && request.url.toString().contains("/v1/databases/")) {
                respond(
                    content = TestFixtures.Databases.retrieveDatabaseAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Database not found")
            }
        }
    }

    /**
     * Add a database query response using official sample data.
     */
    fun addDatabaseQueryResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Post &&
                request.url.toString().contains("/v1/databases/") &&
                request.url.toString().contains("/query")
            ) {
                respond(
                    content = TestFixtures.Databases.queryDatabaseAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Query endpoint not found")
            }
        }
    }

    /**
     * Add a database create response using official sample data.
     */
    fun addDatabaseCreateResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Post &&
                request.url.toString().contains("/v1/databases") &&
                !request.url.toString().contains("/query")
            ) {
                respond(
                    content = TestFixtures.Databases.createDatabaseAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Endpoint not found")
            }
        }
    }

    /**
     * Add a page create response using official sample data.
     */
    fun addPageCreateResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Post && request.url.toString().contains("/v1/pages")) {
                respond(
                    content = TestFixtures.Pages.createPageAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Endpoint not found")
            }
        }
    }

    /**
     * Add a block retrieve response using official sample data.
     */
    fun addBlockRetrieveResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Get && request.url.toString().contains("/v1/blocks/")) {
                respond(
                    content = TestFixtures.Blocks.retrieveBlockAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Block not found")
            }
        }
    }

    /**
     * Add a block children retrieve response using official sample data.
     */
    fun addBlockChildrenRetrieveResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Get &&
                request.url.toString().contains("/v1/blocks/") &&
                request.url.toString().contains("/children")
            ) {
                respond(
                    content = TestFixtures.Blocks.retrieveBlockChildrenAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Block children not found")
            }
        }
    }

    /**
     * Add a block children append response using official sample data.
     */
    fun addBlockChildrenAppendResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Patch &&
                request.url.toString().contains("/v1/blocks/") &&
                request.url.toString().contains("/children")
            ) {
                respond(
                    content = TestFixtures.Blocks.appendBlockChildrenAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Endpoint not found")
            }
        }
    }

    /**
     * Add a comment retrieve response using official sample data.
     */
    fun addCommentsRetrieveResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Get && request.url.toString().contains("/v1/comments")) {
                respond(
                    content = TestFixtures.Comments.retrieveCommentsAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Comments not found")
            }
        }
    }

    /**
     * Add a comment create response using official sample data.
     */
    fun addCommentCreateResponse() {
        handlers.add { request ->
            if (request.method == HttpMethod.Post && request.url.toString().contains("/v1/comments")) {
                respond(
                    content = TestFixtures.Comments.createCommentAsString(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Endpoint not found")
            }
        }
    }

    /**
     * Add error response for testing error handling.
     */
    fun addErrorResponse(
        method: HttpMethod,
        urlPattern: String,
        statusCode: HttpStatusCode,
        errorMessage: String = "API Error",
    ) {
        handlers.add { request ->
            if (request.method == method && request.url.toString().contains(urlPattern.replace("*", ""))) {
                respond(
                    content = """{"object": "error", "status": ${statusCode.value}, "code": "test_error", "message": "$errorMessage"}""",
                    status = statusCode,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "No mock response configured")
            }
        }
    }

    /**
     * Add a custom JSON response for any endpoint.
     */
    fun addJsonResponse(
        method: HttpMethod,
        path: String,
        responseBody: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
    ) {
        handlers.add { request ->
            if (request.method == method && request.url.toString().contains(path)) {
                respond(
                    content = responseBody,
                    status = statusCode,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            } else {
                respondError(HttpStatusCode.NotFound, "No mock response configured")
            }
        }
    }

    fun build(): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    // Try each handler until one responds
                    for (handler in handlers) {
                        val response = handler(request)
                        if (response.statusCode != HttpStatusCode.NotFound) {
                            return@addHandler response
                        }
                    }
                    // Default response if no handler matched
                    respondError(HttpStatusCode.NotFound, "No mock response configured for: ${request.method.value} ${request.url}")
                }
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                    },
                )
            }
        }
    }
}

/**
 * Convenient presets for common test scenarios.
 */
object MockPresets {
    /**
     * Create a mock client with all standard CRUD operations for databases and pages.
     */
    fun standardCrudOperations(): HttpClient =
        mockClient {
            addDatabaseRetrieveResponse()
            addDatabaseCreateResponse()
            addPageRetrieveResponse()
            addPageCreateResponse()
        }

    /**
     * Create a mock client with comprehensive coverage of all API endpoints.
     */
    fun comprehensiveOperations(): HttpClient =
        mockClient {
            addDatabaseRetrieveResponse()
            addDatabaseCreateResponse()
            addPageRetrieveResponse()
            addPageCreateResponse()
            addBlockRetrieveResponse()
            addBlockChildrenRetrieveResponse()
            addBlockChildrenAppendResponse()
            addCommentsRetrieveResponse()
            addCommentCreateResponse()
        }

    /**
     * Create a mock client that returns errors for testing error handling.
     */
    fun errorScenarios(): HttpClient =
        mockClient {
            addErrorResponse(HttpMethod.Get, "/v1/databases/", HttpStatusCode.NotFound, "Database not found")
            addErrorResponse(HttpMethod.Get, "/v1/pages/", HttpStatusCode.Forbidden, "Access denied")
            addErrorResponse(HttpMethod.Get, "/v1/blocks/", HttpStatusCode.NotFound, "Block not found")
            addErrorResponse(HttpMethod.Get, "/v1/comments/", HttpStatusCode.Forbidden, "Comments access denied")
            addErrorResponse(HttpMethod.Post, "/v1/", HttpStatusCode.BadRequest, "Invalid request")
        }
}
