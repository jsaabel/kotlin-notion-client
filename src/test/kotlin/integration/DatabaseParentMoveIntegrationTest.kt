package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon

/**
 * Live check of the one genuinely new capability in `databases.update`: moving a database to a
 * different parent.
 *
 * Everything else the container update carries — title, icon, cover, `is_inline`, `in_trash` —
 * existed before 2025-09-03 in some form and is pinned by unit tests against the official
 * sample. `parent` is new in the API and is the piece least likely to behave as documented, so
 * it is worth confirming rather than assuming.
 *
 * The spec creates its own parent page and a second one to move to, so it depends only on
 * `NOTION_TEST_PAGE_ID` being a page the integration can write under. Set
 * `NOTION_CLEANUP_AFTER_TEST=false` to leave the objects behind for inspection.
 *
 * Run with: ./gradlew integrationTest --tests "*DatabaseParentMoveIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class DatabaseParentMoveIntegrationTest :
    StringSpec({

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) database parent move" {
                println("Skipping DatabaseParentMoveIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val rootPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            // Notion accepts ids with or without hyphens and always answers with them, so a
            // parent read back never string-equals an id copied from an env var or a URL.
            fun String?.asId(): String? = this?.replace("-", "")

            "moves a database to a different parent page" {
                val destination =
                    notion.pages.create {
                        parent.page(rootPageId)
                        title("Move destination")
                    }

                val database =
                    notion.databases.create {
                        parent.page(rootPageId)
                        title("Parent move — before")
                        properties { title("Name") }
                    }

                database.parent.id.asId() shouldBe rootPageId.asId()

                val moved =
                    notion.databases.update(database.id) {
                        parent.page(destination.id)
                        title("Parent move — after")
                    }

                moved.parent.id.asId() shouldBe destination.id.asId()
                moved.title.first().plainText shouldBe "Parent move — after"

                // Confirm the move persisted rather than only being echoed by the PATCH response.
                val reread = notion.databases.retrieve(database.id)
                reread.parent.id.asId() shouldBe destination.id.asId()

                if (shouldCleanupAfterTest()) {
                    notion.databases.trash(database.id)
                    notion.pages.trash(destination.id)
                } else {
                    println("  Database URL   : ${moved.url}")
                    println("  Destination    : ${destination.url}")
                }
            }

            "replaces a container icon" {
                // Replacing is the whole capability here: `PATCH /v1/databases` rejects
                // `"icon": null`, so a container icon cannot be cleared once set. See
                // DatabaseAttributeProbeIntegrationTest and UpdateDatabaseRequestBuilder's KDoc.
                val database =
                    notion.databases.create {
                        parent.page(rootPageId)
                        title("Container icon replace")
                        properties { title("Name") }
                    }

                val withEmoji = notion.databases.update(database.id) { icon.emoji("📊") }
                withEmoji.icon shouldBe Icon.Emoji(emoji = "📊")

                val replaced = notion.databases.update(database.id) { icon.emoji("✅") }
                replaced.icon shouldBe Icon.Emoji(emoji = "✅")
                notion.databases.retrieve(database.id).icon shouldBe Icon.Emoji(emoji = "✅")

                if (shouldCleanupAfterTest()) notion.databases.trash(database.id)
            }
        }
    })
