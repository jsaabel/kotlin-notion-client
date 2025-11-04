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

        "Should build query with relation property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            relation("Related Project").contains("12345678-1234-1234-1234-123456789abc"),
                            relation("Blocked By").isEmpty(),
                            relation("Dependencies").isNotEmpty(),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with people property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            people("Assignee").contains("87654321-4321-4321-4321-cba987654321"),
                            people("Collaborators").isNotEmpty(),
                            people("Reviewer").doesNotContain("11111111-1111-1111-1111-111111111111"),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with status property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            status("Status").equals("In Progress"),
                            status("Priority Status").doesNotEqual("Archived"),
                            status("Review Status").isNotEmpty(),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with unique_id property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            uniqueId("ID").greaterThan(100),
                            uniqueId("ID").lessThanOrEqualTo(999),
                            uniqueId("Ticket Number").doesNotEqual(42),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with files property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            files("Attachments").isNotEmpty(),
                            files("Documents").isEmpty(),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }

        "Should build query with mixed new and existing property filters" {
            val client = NotionClient.createWithClient(createMockClient(), NotionConfig("test-token"))

            val query =
                DataSourceQueryBuilder()
                    .filter {
                        and(
                            title("Task Name").contains("Feature"),
                            status("Status").equals("Active"),
                            people("Assignee").isNotEmpty(),
                            relation("Epic").contains("abc12345-6789-0abc-def0-123456789abc"),
                            uniqueId("ID").greaterThan(1),
                            files("Screenshots").isNotEmpty(),
                            checkbox("Urgent").equals(true),
                        )
                    }.build()

            val pages = client.dataSources.query("test-data-source-id", query)

            pages.shouldNotBeEmpty()
        }
    })
