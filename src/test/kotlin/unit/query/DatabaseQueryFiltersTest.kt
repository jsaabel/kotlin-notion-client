package unit.query

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.datasources.DataSourceQueryBuilder
import unit.util.mockClient

/**
 * Tests for database query filters across all property types (2025-09-03 API).
 *
 * Focuses on filter functionality for different property types and combinations.
 * In 2025-09-03, queries target data sources instead of databases.
 */
class DatabaseQueryFiltersTest :
    StringSpec({

        fun createMockClient() =
            mockClient {
                addDataSourceQueryResponse()
            }

        "Should build query with AND conditions" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            title("Task Name").contains("Urgent"),
                            checkbox("Completed").equals(false),
                            number("Priority").greaterThan(5),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with OR conditions" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        or(
                            select("Status").equals("In Progress"),
                            select("Status").equals("Review"),
                            date("Due Date").pastWeek(),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with complex nested conditions" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            title("Project").isNotEmpty(),
                            or(
                                select("Priority").equals("High"),
                                and(
                                    select("Priority").equals("Medium"),
                                    date("Due Date").before("2024-12-31"),
                                ),
                            ),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with text property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            title("Title").startsWith("Project"),
                            richText("Description").contains("important"),
                            email("Contact").endsWith("@company.com"),
                            url("Website").doesNotContain("staging"),
                            phoneNumber("Phone").isNotEmpty(),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with number property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            number("Score").greaterThan(80),
                            number("Count").lessThanOrEqualTo(100),
                            number("Rating").doesNotEqual(0),
                            number("Budget").greaterThanOrEqualTo(1000),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with select property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            select("Category").equals("Project"),
                            multiSelect("Tags").isNotEmpty(),
                            select("Status").doesNotEqual("Archived"),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with date property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            date("Created").after("2024-01-01"),
                            date("Created").before("2024-12-31"),
                            date("Due Date").onOrAfter("2024-06-01"),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with relative date filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        or(
                            date("Created").pastWeek(),
                            date("Due Date").nextMonth(),
                            date("Modified").pastYear(),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with empty and not empty filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            title("Required Field").isNotEmpty(),
                            richText("Optional Notes").isEmpty(),
                            number("Score").isNotEmpty(),
                            select("Category").isNotEmpty(),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with checkbox filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            checkbox("Active").equals(true),
                            checkbox("Completed").doesNotEqual(true),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }
    })
