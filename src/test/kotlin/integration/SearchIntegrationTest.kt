package integration

import integration.integrationTestEnvVarsAreSet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.search.searchRequest

/**
 * Integration tests for the Search API.
 *
 * These tests require:
 * - NOTION_API_TOKEN environment variable with a valid API token
 * - Integration must have access to at least one page or database
 *
 * Note: Search indexing is not immediate. If you just shared content with the
 * integration, results may be delayed.
 */
@Tags("Integration", "RequiresApi")
class SearchIntegrationTest :
    StringSpec({
        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) Search integration tests" {
                println("⏭️ Skipping SearchIntegrationTest - missing environment variables")
            }
        } else {
            val client = NotionClient.create(NotionConfig(apiToken = System.getenv("NOTION_API_TOKEN")))

            "search should return accessible content" {
                val response = client.search.search()

                response.objectType shouldBe "list"
                response.results shouldNotBe null
                println("✓ Search returned ${response.results.size} results")
            }

            "search with query should filter results" {
                val response = client.search.search("test")

                response shouldNotBe null
                println("✓ Search with query returned ${response.results.size} results")
            }

            "search DSL with filters should work" {
                val response =
                    client.search.search(
                        searchRequest {
                            filterPages()
                            sortDescending()
                            pageSize(10)
                        },
                    )

                response shouldNotBe null
                println("✓ Search with DSL returned ${response.results.size} results")
            }

            "search for data sources should work with 2025-09-03 API" {
                val response =
                    client.search.search(
                        searchRequest {
                            filterDataSources()
                        },
                    )

                response shouldNotBe null
                println("✓ Data source search returned ${response.results.size} results")
            }
        }
    })
