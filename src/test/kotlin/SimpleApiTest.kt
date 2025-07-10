import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionConfig

/**
 * Simple integration tests for Pages and Databases APIs.
 *
 * Instructions:
 * 1. Create a test page and database in your Notion workspace
 * 2. Replace the placeholder IDs below:
 *    - Line 19: Replace "REPLACE_WITH_ACTUAL_PAGE_ID" with your page ID
 *    - Line 35: Replace "REPLACE_WITH_ACTUAL_DATABASE_ID" with your database ID
 * 3. Set environment variable: export NOTION_API_TOKEN="your_token_here"
 * 4. Run: ./gradlew test --tests "*SimpleApiTest*"
 */
class SimpleApiTest :
    StringSpec({

        "Pages API should retrieve page successfully" {
            val token = System.getenv("NOTION_API_TOKEN")
            val pageId = "22bc63fd82ed80dabbe4d340cd1b97c7"

            if (token != null && pageId != "REPLACE_WITH_ACTUAL_PAGE_ID") {
                val client = NotionClient.create(NotionConfig(token = token))

                val page = client.pages.retrieve(pageId)

                page.id.replace("-", "") shouldBe pageId
                page.objectType shouldBe "page"
                page.url.isNotBlank() shouldBe true
                page.createdBy shouldNotBe null

                client.close()
            } else {
                println("⏭️ Skipping test - set NOTION_API_TOKEN and replace pageId")
            }
        }

        "Databases API should retrieve database successfully" {
            val token = System.getenv("NOTION_API_TOKEN")

            val databaseId = "22bc63fd82ed80d6a648d1433b382457"

            if (token != null && databaseId != "REPLACE_WITH_ACTUAL_DATABASE_ID") {
                val client = NotionClient.create(NotionConfig(token = token))

                val database = client.databases.retrieve(databaseId)

                database.id.replace("-", "") shouldBe databaseId // TODO: Should find a good (centralized) way to deal with hyphens in ids
                database.objectType shouldBe "database"
                database.url.isNotBlank() shouldBe true
                database.createdBy shouldNotBe null
                database.properties.isNotEmpty() shouldBe true

                client.close()
            } else {
                println("⏭️ Skipping test - set NOTION_API_TOKEN and replace databaseId")
            }
        }
    })
