package dsl

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import mockClient
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.databases.DatabaseQueryRequest
import no.saabelit.kotlinnotionclient.models.databases.SortDirection
import no.saabelit.kotlinnotionclient.models.databases.databaseQuery

/**
 * Tests for the Database Query DSL functionality.
 *
 * This test suite focuses on the fluent DSL API for database querying,
 * testing both the standalone databaseQuery function and the API integration.
 */
@Tags("Unit")
class DatabaseQueryDslTest :
    StringSpec({

        fun createMockClient() =
            mockClient {
                addDatabaseQueryResponse()
            }

        "databaseQuery function should create proper request object" {
            val request =
                databaseQuery {
                    filter {
                        title("Name").contains("Test")
                    }
                    sortBy("Priority", SortDirection.DESCENDING)
                    pageSize(25)
                }

            request.shouldBeTypeOf<DatabaseQueryRequest>()
            request.filter?.property shouldBe "Name"
            request.filter?.title?.contains shouldBe "Test"
            request.sorts?.size shouldBe 1
            request.sorts?.first()?.property shouldBe "Priority"
            request.sorts?.first()?.direction shouldBe SortDirection.DESCENDING
            request.pageSize shouldBe 25
        }

        "databaseQuery should support complex nested filters" {
            val request =
                databaseQuery {
                    filter {
                        and(
                            title("Project").isNotEmpty(),
                            or(
                                select("Status").equals("Active"),
                                checkbox("Urgent").equals(true),
                            ),
                        )
                    }
                }

            request.filter?.and?.size shouldBe 2
            request.filter
                ?.and
                ?.get(0)
                ?.property shouldBe "Project"
            request.filter
                ?.and
                ?.get(1)
                ?.or
                ?.size shouldBe 2
        }

        "databaseQuery should support all property filter types" {
            val request =
                databaseQuery {
                    filter {
                        and(
                            title("Title").startsWith("Project"),
                            richText("Description").contains("important"),
                            number("Score").greaterThan(80),
                            select("Category").equals("Work"),
                            multiSelect("Tags").isNotEmpty(),
                            date("Created").pastWeek(),
                            checkbox("Active").equals(true),
                            email("Contact").endsWith("@company.com"),
                            url("Website").doesNotContain("staging"),
                            phoneNumber("Phone").isNotEmpty(),
                        )
                    }
                }

            request.filter?.and?.size shouldBe 10
        }

        "databaseQuery should support multiple sorts and pagination" {
            val request =
                databaseQuery {
                    sortBy("Priority", SortDirection.DESCENDING)
                    sortBy("Name", SortDirection.ASCENDING)
                    sortByTimestamp("created_time", SortDirection.DESCENDING)
                    pageSize(50)
                    startCursor("test-cursor-123")
                }

            request.sorts?.size shouldBe 3
            request.sorts?.get(0)?.property shouldBe "Priority"
            request.sorts?.get(1)?.property shouldBe "Name"
            request.sorts?.get(2)?.timestamp shouldBe "created_time"
            request.pageSize shouldBe 50
            request.startCursor shouldBe "test-cursor-123"
        }

        "API integration - query with DSL should work" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages =
                client.databases.query("test-database-id") {
                    filter {
                        title("Name").contains("Important")
                    }
                    sortBy("Priority", SortDirection.DESCENDING)
                }

            pages.shouldNotBeEmpty()
        }

        "API integration - complex query with DSL should work" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages =
                client.databases.query("test-database-id") {
                    filter {
                        and(
                            title("Task Name").contains("Urgent"),
                            checkbox("Completed").equals(false),
                            or(
                                select("Priority").equals("High"),
                                date("Due Date").pastWeek(),
                            ),
                        )
                    }
                    sortBy("Created", SortDirection.DESCENDING)
                    sortBy("Priority", SortDirection.ASCENDING)
                    pageSize(10)
                }

            pages.shouldNotBeEmpty()
        }

        "API integration - query without parameters should work" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages =
                client.databases.query("test-database-id") {
                    // Empty query - should return all records
                }

            pages.shouldNotBeEmpty()
        }

        "API integration - filter-only query should work" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages =
                client.databases.query("test-database-id") {
                    filter {
                        checkbox("Active").equals(true)
                    }
                }

            pages.shouldNotBeEmpty()
        }

        "API integration - sort-only query should work" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages =
                client.databases.query("test-database-id") {
                    sortByTimestamp("last_edited_time", SortDirection.DESCENDING)
                    pageSize(5)
                }

            pages.shouldNotBeEmpty()
        }

        "API integration - all number filter operations should work" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages =
                client.databases.query("test-database-id") {
                    filter {
                        and(
                            number("Score").greaterThan(50),
                            number("Rating").lessThanOrEqualTo(10),
                            number("Count").doesNotEqual(0),
                            number("Budget").greaterThanOrEqualTo(1000),
                        )
                    }
                }

            pages.shouldNotBeEmpty()
        }

        "API integration - all date filter operations should work" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages =
                client.databases.query("test-database-id") {
                    filter {
                        or(
                            date("Created").after("2024-01-01"),
                            date("Due").before("2024-12-31"),
                            date("Modified").pastMonth(),
                            date("Started").nextWeek(),
                        )
                    }
                }

            pages.shouldNotBeEmpty()
        }

        "API integration - text filter operations should work" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages =
                client.databases.query("test-database-id") {
                    filter {
                        and(
                            title("Title").startsWith("Project"),
                            richText("Description").endsWith("done"),
                            email("Contact").doesNotContain("test"),
                            url("Link").isEmpty(),
                        )
                    }
                }

            pages.shouldNotBeEmpty()
        }

        "API integration - empty and not empty filters should work" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val pages =
                client.databases.query("test-database-id") {
                    filter {
                        and(
                            title("Required").isNotEmpty(),
                            richText("Optional").isEmpty(),
                            select("Category").isNotEmpty(),
                            number("Score").isNotEmpty(),
                        )
                    }
                }

            pages.shouldNotBeEmpty()
        }
    })
