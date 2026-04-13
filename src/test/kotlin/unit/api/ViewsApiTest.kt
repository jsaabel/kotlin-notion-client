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
import it.saabel.kotlinnotionclient.models.views.CardLayout
import it.saabel.kotlinnotionclient.models.views.ChartType
import it.saabel.kotlinnotionclient.models.views.CoverAspect
import it.saabel.kotlinnotionclient.models.views.CoverConfig
import it.saabel.kotlinnotionclient.models.views.CoverSize
import it.saabel.kotlinnotionclient.models.views.CoverType
import it.saabel.kotlinnotionclient.models.views.CreateViewRequest
import it.saabel.kotlinnotionclient.models.views.DeletedViewQuery
import it.saabel.kotlinnotionclient.models.views.PartialView
import it.saabel.kotlinnotionclient.models.views.SubmissionPermissions
import it.saabel.kotlinnotionclient.models.views.UpdateViewRequest
import it.saabel.kotlinnotionclient.models.views.View
import it.saabel.kotlinnotionclient.models.views.ViewConfiguration
import it.saabel.kotlinnotionclient.models.views.ViewList
import it.saabel.kotlinnotionclient.models.views.ViewPropertyConfig
import it.saabel.kotlinnotionclient.models.views.ViewQuery
import it.saabel.kotlinnotionclient.models.views.ViewQueryResults
import it.saabel.kotlinnotionclient.models.views.ViewRange
import it.saabel.kotlinnotionclient.models.views.ViewType
import it.saabel.kotlinnotionclient.models.views.createViewRequest
import it.saabel.kotlinnotionclient.models.views.updateViewRequest
import kotlinx.serialization.json.Json
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

        // ========== ViewConfiguration typed model tests ==========

        context("ViewConfiguration serialization") {
            val json = Json { ignoreUnknownKeys = true }

            test("Table config round-trips with all simple fields") {
                val config =
                    ViewConfiguration.Table(
                        properties = listOf(ViewPropertyConfig(propertyId = "prop-1", visible = true)),
                        wrapCells = true,
                        frozenColumnIndex = 2,
                        showVerticalLines = false,
                    )
                val encoded = json.encodeToString(ViewConfiguration.serializer(), config)
                val decoded = json.decodeFromString(ViewConfiguration.serializer(), encoded)
                val table = decoded.shouldBeInstanceOf<ViewConfiguration.Table>()
                table.wrapCells shouldBe true
                table.frozenColumnIndex shouldBe 2
                table.showVerticalLines shouldBe false
                table.properties?.first()?.propertyId shouldBe "prop-1"
            }

            test("Gallery config round-trips with cover and card layout") {
                val config =
                    ViewConfiguration.Gallery(
                        cover = CoverConfig(type = CoverType.PAGE_COVER),
                        coverSize = CoverSize.LARGE,
                        coverAspect = CoverAspect.COVER,
                        cardLayout = CardLayout.COMPACT,
                    )
                val encoded = json.encodeToString(ViewConfiguration.serializer(), config)
                val decoded = json.decodeFromString(ViewConfiguration.serializer(), encoded) as ViewConfiguration.Gallery
                decoded.cover?.type shouldBe CoverType.PAGE_COVER
                decoded.coverSize shouldBe CoverSize.LARGE
                decoded.coverAspect shouldBe CoverAspect.COVER
                decoded.cardLayout shouldBe CardLayout.COMPACT
            }

            test("Calendar config round-trips with datePropertyId and viewRange") {
                val config =
                    ViewConfiguration.Calendar(
                        datePropertyId = "date-prop-id",
                        viewRange = ViewRange.WEEK,
                        showWeekends = false,
                    )
                val encoded = json.encodeToString(ViewConfiguration.serializer(), config)
                val decoded = json.decodeFromString(ViewConfiguration.serializer(), encoded) as ViewConfiguration.Calendar
                decoded.datePropertyId shouldBe "date-prop-id"
                decoded.viewRange shouldBe ViewRange.WEEK
                decoded.showWeekends shouldBe false
            }

            test("Form config round-trips with all fields") {
                val config =
                    ViewConfiguration.Form(
                        isFormClosed = true,
                        anonymousSubmissions = false,
                        submissionPermissions = SubmissionPermissions.READER,
                    )
                val encoded = json.encodeToString(ViewConfiguration.serializer(), config)
                val decoded = json.decodeFromString(ViewConfiguration.serializer(), encoded) as ViewConfiguration.Form
                decoded.isFormClosed shouldBe true
                decoded.anonymousSubmissions shouldBe false
                decoded.submissionPermissions shouldBe SubmissionPermissions.READER
            }

            test("Chart config round-trips with chartType and display options") {
                val config =
                    ViewConfiguration.Chart(
                        chartType = ChartType.COLUMN,
                        hideEmptyGroups = true,
                        showDataLabels = false,
                    )
                val encoded = json.encodeToString(ViewConfiguration.serializer(), config)
                val decoded = json.decodeFromString(ViewConfiguration.serializer(), encoded) as ViewConfiguration.Chart
                decoded.chartType shouldBe ChartType.COLUMN
                decoded.hideEmptyGroups shouldBe true
                decoded.showDataLabels shouldBe false
            }

            test("Unknown type preserved as raw JSON") {
                val rawJson = """{"type":"custom_future_type","some_field":true}"""
                val decoded = json.decodeFromString(ViewConfiguration.serializer(), rawJson)
                decoded.shouldBeInstanceOf<ViewConfiguration.Unknown>()
            }
        }

        context("ViewConfiguration DSL builder") {
            test("configuration() on create builder sets typed config") {
                val request =
                    createViewRequest {
                        dataSourceId("ds-1")
                        name("My Table")
                        type(ViewType.TABLE)
                        database("db-1")
                        configuration(ViewConfiguration.Table(wrapCells = true, frozenColumnIndex = 1))
                    }

                val config = request.configuration.shouldNotBeNull() as ViewConfiguration.Table
                config.wrapCells shouldBe true
                config.frozenColumnIndex shouldBe 1
            }

            test("showProperties() on TABLE view produces typed Table config") {
                val request =
                    createViewRequest {
                        dataSourceId("ds-1")
                        name("My Table")
                        type(ViewType.TABLE)
                        database("db-1")
                        showProperties("p1", "p2")
                        hideProperties("p3")
                    }

                val config = request.configuration.shouldNotBeNull() as ViewConfiguration.Table
                config.properties?.size shouldBe 3
                config.properties?.get(0)?.visible shouldBe true
                config.properties?.get(2)?.visible shouldBe false
            }

            test("showProperties() on GALLERY view produces typed Gallery config") {
                val request =
                    createViewRequest {
                        dataSourceId("ds-1")
                        name("My Gallery")
                        type(ViewType.GALLERY)
                        database("db-1")
                        showProperties("p1")
                    }

                request.configuration.shouldBeInstanceOf<ViewConfiguration.Gallery>()
            }

            test("showProperties() on FORM view throws IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    createViewRequest {
                        dataSourceId("ds-1")
                        name("My Form")
                        type(ViewType.FORM)
                        database("db-1")
                        showProperties("p1")
                    }
                }
            }

            test("updateViewRequest configuration() sets typed config") {
                val request =
                    updateViewRequest {
                        configuration(ViewConfiguration.Gallery(coverSize = CoverSize.LARGE))
                    }

                val config = request.configuration.shouldNotBeNull() as ViewConfiguration.Gallery
                config.coverSize shouldBe CoverSize.LARGE
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
