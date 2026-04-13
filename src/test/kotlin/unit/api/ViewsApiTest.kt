package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.saabel.kotlinnotionclient.api.ViewsApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.views.CreateViewRequest
import it.saabel.kotlinnotionclient.models.views.DeletedViewQuery
import it.saabel.kotlinnotionclient.models.views.PartialView
import it.saabel.kotlinnotionclient.models.views.UpdateViewRequest
import it.saabel.kotlinnotionclient.models.views.View
import it.saabel.kotlinnotionclient.models.views.ViewList
import it.saabel.kotlinnotionclient.models.views.ViewQuery
import it.saabel.kotlinnotionclient.models.views.ViewQueryResults
import it.saabel.kotlinnotionclient.models.views.ViewType
import it.saabel.kotlinnotionclient.models.views.createViewRequest
import it.saabel.kotlinnotionclient.models.views.updateViewRequest
import unit.util.TestFixtures
import unit.util.decode
import unit.util.mockClient

@Tags("Unit")
class ViewsApiTest :
    FunSpec({
        lateinit var api: ViewsApi
        lateinit var config: NotionConfig

        beforeTest {
            config = NotionConfig(apiToken = "test-token")
        }

        // ========== Model serialization tests ==========

        context("View model serialization") {
            test("should deserialize full View from fixture") {
                val view = TestFixtures.Views.retrieveView().decode<View>()

                view.objectType shouldBe "view"
                view.id shouldBe "a3f1b2c4-5678-4def-abcd-1234567890ab"
                view.name shouldBe "All tasks"
                view.type shouldBe ViewType.TABLE
                view.parent.type shouldBe "database_id"
                view.parent.databaseId shouldBe "248104cd-477e-80fd-b757-e945d38000bd"
                view.createdTime shouldBe "2026-03-01T12:00:00.000Z"
                view.lastEditedTime shouldBe "2026-04-01T09:00:00.000Z"
                view.dataSourceId shouldBe "248104cd-477e-80af-bc30-000bd28de8f9"
                view.filter.shouldBeNull()
                view.sorts.shouldBeNull()
                view.quickFilters.shouldBeNull()
                view.configuration.shouldBeNull()
            }

            test("should deserialize ViewList from fixture") {
                val list = TestFixtures.Views.listViews().decode<ViewList>()

                list.objectType shouldBe "list"
                list.results.size shouldBe 2
                list.hasMore shouldBe false
                list.nextCursor.shouldBeNull()

                list.results[0].id shouldBe "a3f1b2c4-5678-4def-abcd-1234567890ab"
                list.results[0].objectType shouldBe "view"
                list.results[1].id shouldBe "b4e2c3d5-6789-4efa-bcde-234567890abc"
            }

            test("should deserialize PartialView from fixture") {
                val partial = TestFixtures.Views.partialView().decode<PartialView>()

                partial.objectType shouldBe "view"
                partial.id shouldBe "a3f1b2c4-5678-4def-abcd-1234567890ab"
                partial.type shouldBe ViewType.TABLE
                val parent = partial.parent.shouldNotBeNull()
                parent.databaseId shouldBe "248104cd-477e-80fd-b757-e945d38000bd"
            }

            test("should deserialize ViewQuery from fixture") {
                val query = TestFixtures.Views.createViewQuery().decode<ViewQuery>()

                query.objectType shouldBe "view_query"
                query.id shouldBe "q1a2b3c4-5678-4def-abcd-1234567890ab"
                query.viewId shouldBe "a3f1b2c4-5678-4def-abcd-1234567890ab"
                query.expiresAt shouldBe "2026-04-13T10:15:00.000Z"
                query.totalCount shouldBe 2
                query.results.size shouldBe 1
                query.results[0].objectType shouldBe "page"
                query.results[0].id shouldBe "p1a2b3c4-5678-4def-abcd-1234567890ab"
                query.hasMore shouldBe false
                query.nextCursor.shouldBeNull()
            }

            test("should deserialize ViewQueryResults from fixture") {
                val results = TestFixtures.Views.viewQueryResults().decode<ViewQueryResults>()

                results.objectType shouldBe "list"
                results.results.size shouldBe 2
                results.results[0].id shouldBe "p1a2b3c4-5678-4def-abcd-1234567890ab"
                results.results[1].id shouldBe "p2b3c4d5-6789-4efa-bcde-234567890abc"
                results.hasMore shouldBe false
                results.nextCursor.shouldBeNull()
            }

            test("should deserialize DeletedViewQuery from fixture") {
                val deleted = TestFixtures.Views.deletedViewQuery().decode<DeletedViewQuery>()

                deleted.objectType shouldBe "view_query"
                deleted.id shouldBe "q1a2b3c4-5678-4def-abcd-1234567890ab"
                deleted.deleted.shouldBeTrue()
            }
        }

        // ========== retrieve() ==========

        context("retrieve()") {
            test("should retrieve a view successfully") {
                val client =
                    mockClient {
                        addViewRetrieveResponse()
                    }
                api = ViewsApi(client, config)
                val view = api.retrieve("a3f1b2c4-5678-4def-abcd-1234567890ab")

                view.shouldBeInstanceOf<View>()
                view.id shouldBe "a3f1b2c4-5678-4def-abcd-1234567890ab"
                view.type shouldBe ViewType.TABLE
            }

            test("should throw ApiError on 404") {
                val client =
                    mockClient {
                        addErrorResponse(HttpMethod.Get, "/v1/views/", HttpStatusCode.NotFound)
                    }
                api = ViewsApi(client, config)

                val ex =
                    shouldThrow<NotionException.ApiError> {
                        api.retrieve("nonexistent-id")
                    }
                ex.status shouldBe 404
            }
        }

        // ========== list() ==========

        context("list()") {
            test("should list views and deserialize ViewList") {
                val client =
                    mockClient {
                        addViewListResponse()
                    }
                api = ViewsApi(client, config)
                val list = api.list(databaseId = "248104cd-477e-80fd-b757-e945d38000bd")

                list.shouldBeInstanceOf<ViewList>()
                list.results.size shouldBe 2
                list.hasMore shouldBe false
            }

            test("should send database_id query param") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "database_id=test-db-id",
                            responseBody = TestFixtures.Views.listViewsAsString(),
                        )
                    }
                api = ViewsApi(client, config)
                val list = api.list(databaseId = "test-db-id")

                list.results.size shouldBe 2
            }

            test("should send data_source_id query param") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "data_source_id=test-ds-id",
                            responseBody = TestFixtures.Views.listViewsAsString(),
                        )
                    }
                api = ViewsApi(client, config)
                val list = api.list(dataSourceId = "test-ds-id")

                list.results.size shouldBe 2
            }

            test("should throw when neither databaseId nor dataSourceId provided") {
                val client = mockClient { addViewListResponse() }
                api = ViewsApi(client, config)

                shouldThrow<IllegalArgumentException> {
                    api.list()
                }
            }

            test("should throw ApiError on 400") {
                val client =
                    mockClient {
                        addErrorResponse(HttpMethod.Get, "/v1/views", HttpStatusCode.BadRequest)
                    }
                api = ViewsApi(client, config)

                shouldThrow<NotionException.ApiError> {
                    api.list(databaseId = "bad-id")
                }.status shouldBe 400
            }
        }

        // ========== create() ==========

        context("create()") {
            test("should create a view and return View") {
                val client =
                    mockClient {
                        addViewCreateResponse()
                    }
                api = ViewsApi(client, config)

                val request =
                    CreateViewRequest(
                        dataSourceId = "248104cd-477e-80af-bc30-000bd28de8f9",
                        name = "All tasks",
                        type = ViewType.TABLE,
                        databaseId = "248104cd-477e-80fd-b757-e945d38000bd",
                    )
                val view = api.create(request)

                view.shouldBeInstanceOf<View>()
                view.type shouldBe ViewType.TABLE
            }

            test("should throw when multiple parent fields provided") {
                val client = mockClient { addViewCreateResponse() }
                api = ViewsApi(client, config)

                shouldThrow<IllegalArgumentException> {
                    api.create(
                        CreateViewRequest(
                            dataSourceId = "ds-id",
                            name = "Test",
                            type = ViewType.TABLE,
                            databaseId = "db-id",
                            viewId = "view-id",
                        ),
                    )
                }
            }

            test("should throw when no parent field provided") {
                val client = mockClient { addViewCreateResponse() }
                api = ViewsApi(client, config)

                shouldThrow<IllegalArgumentException> {
                    api.create(
                        CreateViewRequest(
                            dataSourceId = "ds-id",
                            name = "Test",
                            type = ViewType.TABLE,
                        ),
                    )
                }
            }
        }

        // ========== update() ==========

        context("update()") {
            test("should update a view and return updated View") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Patch,
                            path = "/v1/views/",
                            responseBody = TestFixtures.Views.retrieveViewAsString(),
                        )
                    }
                api = ViewsApi(client, config)

                val view = api.update("a3f1b2c4-5678-4def-abcd-1234567890ab", UpdateViewRequest(name = "Renamed view"))

                view.shouldBeInstanceOf<View>()
            }
        }

        // ========== delete() ==========

        context("delete()") {
            test("should delete a view and return PartialView") {
                val client =
                    mockClient {
                        addViewDeleteResponse()
                    }
                api = ViewsApi(client, config)

                val partial = api.delete("a3f1b2c4-5678-4def-abcd-1234567890ab")

                partial.shouldBeInstanceOf<PartialView>()
                partial.id shouldBe "a3f1b2c4-5678-4def-abcd-1234567890ab"
            }
        }

        // ========== createQuery() ==========

        context("createQuery()") {
            test("should create a view query and return ViewQuery") {
                val client =
                    mockClient {
                        addViewCreateQueryResponse()
                    }
                api = ViewsApi(client, config)

                val query = api.createQuery("a3f1b2c4-5678-4def-abcd-1234567890ab")

                query.shouldBeInstanceOf<ViewQuery>()
                query.viewId shouldBe "a3f1b2c4-5678-4def-abcd-1234567890ab"
                query.expiresAt shouldBe "2026-04-13T10:15:00.000Z"
                query.totalCount shouldBe 2
            }
        }

        // ========== getQueryResults() ==========

        context("getQueryResults()") {
            test("should retrieve cached query results") {
                val client =
                    mockClient {
                        addViewQueryResultsResponse()
                    }
                api = ViewsApi(client, config)

                val results = api.getQueryResults("view-id", "query-id")

                results.shouldBeInstanceOf<ViewQueryResults>()
                results.results.size shouldBe 2
                results.hasMore shouldBe false
            }
        }

        // ========== deleteQuery() ==========

        context("deleteQuery()") {
            test("should delete a cached query and return DeletedViewQuery") {
                val client =
                    mockClient {
                        addViewDeleteQueryResponse()
                    }
                api = ViewsApi(client, config)

                val deleted = api.deleteQuery("view-id", "query-id")

                deleted.shouldBeInstanceOf<DeletedViewQuery>()
                deleted.deleted.shouldBeTrue()
            }
        }

        // ========== DSL builder tests ==========

        context("DSL builders") {
            test("createViewRequest DSL produces correct CreateViewRequest") {
                val request =
                    createViewRequest {
                        dataSourceId("ds-123")
                        name("My Table")
                        type(ViewType.TABLE)
                        database("db-456")
                    }

                request.dataSourceId shouldBe "ds-123"
                request.name shouldBe "My Table"
                request.type shouldBe ViewType.TABLE
                request.databaseId shouldBe "db-456"
                request.viewId.shouldBeNull()
                request.createDatabase.shouldBeNull()
            }

            test("createViewRequest DSL with dashboard() sets viewId") {
                val request =
                    createViewRequest {
                        dataSourceId("ds-123")
                        name("Revenue Chart")
                        type(ViewType.CHART)
                        dashboard("dash-view-id")
                    }

                request.viewId shouldBe "dash-view-id"
                request.databaseId.shouldBeNull()
                request.createDatabase.shouldBeNull()
            }

            test("createViewRequest DSL with createDatabase() sets createDatabase") {
                val request =
                    createViewRequest {
                        dataSourceId("ds-123")
                        name("Tasks")
                        type(ViewType.TABLE)
                        createDatabase(pageId = "page-abc")
                    }

                val createDb = request.createDatabase.shouldNotBeNull()
                createDb.parent.pageId shouldBe "page-abc"
                createDb.position.shouldBeNull()
                request.databaseId.shouldBeNull()
                request.viewId.shouldBeNull()
            }

            test("createViewRequest DSL with createDatabase() and afterBlockId") {
                val request =
                    createViewRequest {
                        dataSourceId("ds-123")
                        name("Tasks")
                        type(ViewType.TABLE)
                        createDatabase(pageId = "page-abc", afterBlockId = "block-xyz")
                    }

                val position =
                    request.createDatabase
                        .shouldNotBeNull()
                        .position
                        .shouldNotBeNull()
                position.blockId shouldBe "block-xyz"
            }

            test("createViewRequest DSL throws when no parent set") {
                shouldThrow<IllegalArgumentException> {
                    createViewRequest {
                        dataSourceId("ds-123")
                        name("Test")
                        type(ViewType.TABLE)
                    }
                }
            }

            test("createViewRequest DSL throws when dataSourceId missing") {
                shouldThrow<IllegalArgumentException> {
                    createViewRequest {
                        name("Test")
                        type(ViewType.TABLE)
                        database("db-id")
                    }
                }
            }

            test("updateViewRequest DSL produces correct UpdateViewRequest") {
                val request =
                    updateViewRequest {
                        name("Renamed")
                    }

                request.name shouldBe "Renamed"
            }

            test("updateViewRequest DSL with no fields produces all-null request") {
                val request = updateViewRequest {}

                request.name.shouldBeNull()
            }

            test("create() DSL overload calls through to HTTP layer") {
                val client =
                    mockClient {
                        addViewCreateResponse()
                    }
                api = ViewsApi(client, config)

                val view =
                    api.create {
                        dataSourceId("248104cd-477e-80af-bc30-000bd28de8f9")
                        name("DSL View")
                        type(ViewType.TABLE)
                        database("248104cd-477e-80fd-b757-e945d38000bd")
                    }

                view.shouldBeInstanceOf<View>()
            }

            test("update() DSL overload calls through to HTTP layer") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Patch,
                            path = "/v1/views/",
                            responseBody = TestFixtures.Views.retrieveViewAsString(),
                        )
                    }
                api = ViewsApi(client, config)

                val view =
                    api.update("a3f1b2c4-5678-4def-abcd-1234567890ab") {
                        name("Renamed via DSL")
                    }

                view.shouldBeInstanceOf<View>()
            }
        }

        // ========== Error responses ==========

        context("error handling") {
            test("should throw ApiError on 403 Forbidden") {
                val client =
                    mockClient {
                        addErrorResponse(HttpMethod.Get, "/v1/views/", HttpStatusCode.Forbidden)
                    }
                api = ViewsApi(client, config)

                shouldThrow<NotionException.ApiError> {
                    api.retrieve("some-id")
                }.status shouldBe 403
            }
        }
    })
