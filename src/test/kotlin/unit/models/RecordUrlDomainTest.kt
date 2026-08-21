package unit.models

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import it.saabel.kotlinnotionclient.models.base.Annotations
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.databases.Database
import it.saabel.kotlinnotionclient.models.datasources.DataSource
import it.saabel.kotlinnotionclient.models.pages.Page
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import unit.util.TestFixtures

/**
 * Notion migrated API-generated record links from `notion.so` to `app.notion.com`
 * (changelog: "Notion App Domain Migration", Jul 15, 2026). `url`/`public_url` on
 * pages, databases and data sources — and `href` on page/database mentions — now
 * point at `https://app.notion.com/p/{id}`, while legacy `notion.so` links keep
 * working.
 *
 * The library treats these values as opaque, human-facing strings: it never builds
 * one and never parses one to recover an id (use `id` for that). These tests pin
 * that tolerance so a future "helpful" normalisation cannot silently reject either
 * domain.
 */
@Tags("Unit")
class RecordUrlDomainTest :
    FunSpec({

        val appDomain = "https://app.notion.com"
        val legacyDomain = "https://www.notion.so"

        context("record urls deserialize regardless of domain") {
            test("page url and public_url accept app.notion.com") {
                val raw =
                    TestFixtures.Pages
                        .retrievePageAsString()
                        .replace(legacyDomain, appDomain)
                        .replace("https://notion.so", appDomain)

                val page = TestFixtures.json.decodeFromString(Page.serializer(), raw)

                page.url shouldStartWith appDomain
            }

            test("page url still accepts legacy notion.so links") {
                val page =
                    TestFixtures.json.decodeFromString(
                        Page.serializer(),
                        TestFixtures.Pages.retrievePageAsString(),
                    )

                page.url shouldStartWith legacyDomain
            }

            test("database url accepts app.notion.com") {
                val raw =
                    TestFixtures.Databases
                        .retrieveDatabaseAsString()
                        .replace(legacyDomain, appDomain)

                val database = TestFixtures.json.decodeFromString(Database.serializer(), raw)

                database.url?.shouldStartWith(appDomain)
            }

            test("data source url accepts app.notion.com") {
                val raw =
                    TestFixtures.DataSources
                        .retrieveDataSourceAsString()
                        .replace(legacyDomain, appDomain)

                // The official sample omits `description`, which DataSource requires —
                // an unrelated fixture/model gap. Supply it so this test stays about the domain.
                val patched =
                    JsonObject(
                        TestFixtures.json
                            .parseToJsonElement(raw)
                            .jsonObject + ("description" to JsonArray(emptyList())),
                    )

                val dataSource = TestFixtures.json.decodeFromJsonElement(DataSource.serializer(), patched)

                dataSource.url shouldStartWith appDomain
            }
        }

        context("mention hrefs are carried through unchanged") {
            test("app.notion.com mention href round-trips") {
                val href = "$appDomain/p/598337872cf94fdf8782e53db20768a5"
                val json =
                    """
                    {
                      "type": "mention",
                      "mention": { "type": "page", "page": { "id": "59833787-2cf9-4fdf-8782-e53db20768a5" } },
                      "annotations": {},
                      "plain_text": "Tuscan kale",
                      "href": "$href"
                    }
                    """.trimIndent()

                val richText = TestFixtures.json.decodeFromString(RichText.serializer(), json)

                richText.href shouldBe href
            }

            test("legacy notion.so mention href round-trips") {
                val href = "$legacyDomain/598337872cf94fdf8782e53db20768a5"
                val richText =
                    RichText(
                        type = "mention",
                        annotations = Annotations(),
                        plainText = "Tuscan kale",
                        href = href,
                    )

                richText.href shouldBe href
            }
        }
    })
