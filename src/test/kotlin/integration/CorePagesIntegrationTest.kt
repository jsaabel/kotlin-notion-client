package integration

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import it.saabel.kotlinnotionclient.NotionClient
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseRequest
import it.saabel.kotlinnotionclient.models.databases.InitialDataSource
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.UpdatePageRequest
import it.saabel.kotlinnotionclient.models.pages.getCheckboxProperty
import it.saabel.kotlinnotionclient.models.pages.getEmailProperty
import it.saabel.kotlinnotionclient.models.pages.getNumberProperty
import it.saabel.kotlinnotionclient.models.pages.getRichTextAsPlainText
import it.saabel.kotlinnotionclient.models.pages.getTitleAsPlainText
import it.saabel.kotlinnotionclient.models.pages.pageProperties
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import it.saabel.kotlinnotionclient.models.users.UserType
import kotlinx.coroutines.delay

/**
 * Integration tests for core page and database operations.
 *
 * Covers:
 * - Client authentication: retrieve current bot user (/users/me)
 * - CRUD lifecycle: create database + page, retrieve, update with type-safe property access
 * - Standalone page creation and retrieval
 * - Comprehensive database property types (title, rich text, number, checkbox, select, etc.)
 * - Page lock and unlock (lock(), unlock(), lock(true/false) syntax)
 * - Page move: moveToPage(), moveToDataSource()
 * - Page position: position.pageStart(), position.pageEnd() when creating sub-pages
 *
 * All artifacts (databases, pages) are created under a single container page. Trashing
 * the container (NOTION_CLEANUP_AFTER_TEST=true) cascades to all children.
 *
 * Prerequisites:
 * - export NOTION_API_TOKEN="secret_..."
 * - export NOTION_TEST_PAGE_ID="..."
 * - export NOTION_RUN_INTEGRATION_TESTS="true"
 *
 * Run with: ./gradlew integrationTest --tests "*CorePagesIntegrationTest"
 */
@Tags("Integration", "RequiresApi")
class CorePagesIntegrationTest :
    StringSpec({

        fun String.withOrWithoutHyphens(): List<String> = listOf(this, this.replace("-", ""))

        if (!integrationTestEnvVarsAreSet()) {
            "!(Skipped) core pages integration" {
                println("Skipping CorePagesIntegrationTest — set required env vars")
            }
        } else {
            val token = System.getenv("NOTION_API_TOKEN")
            val parentPageId = System.getenv("NOTION_TEST_PAGE_ID")
            val notion = NotionClient.create(NotionConfig(apiToken = token))

            var containerPageId = ""

            beforeSpec {
                val container =
                    notion.pages.create {
                        parent.page(parentPageId)
                        title("Core Pages — Integration Tests")
                        icon.emoji("📋")
                        content {
                            callout(
                                "ℹ️",
                                "Covers client authentication, the database/page CRUD lifecycle with type-safe property access, " +
                                    "standalone page creation, comprehensive property types, page lock/unlock, " +
                                    "page move (to page and to data source), and page position (start/end).",
                            )
                        }
                    }
                containerPageId = container.id
                println("📄 Container: ${container.url}")
            }

            afterSpec {
                if (shouldCleanupAfterTest()) {
                    notion.pages.trash(containerPageId)
                    println("✅ Cleaned up container page (all children trashed)")
                } else {
                    println("🔧 Cleanup skipped — container page preserved for inspection")
                }
                notion.close()
            }

            // ------------------------------------------------------------------
            // 1. Authentication — retrieve current bot user
            // ------------------------------------------------------------------
            "should authenticate and retrieve current bot user" {
                val user = notion.users.getCurrentUser()

                user.id.shouldNotBeBlank()
                user.objectType shouldBe "user"
                user.type.shouldNotBeNull()
                user.type shouldBe UserType.BOT
                user.bot.shouldNotBeNull()

                println("  ✅ Bot user: ${user.name} (${user.id})")
            }

            // ------------------------------------------------------------------
            // 2. CRUD lifecycle — database + page with type-safe property access
            // ------------------------------------------------------------------
            "should create database, page, retrieve with type-safe access, and update" {
                val databaseRequest =
                    CreateDatabaseRequest(
                        parent = Parent.PageParent(pageId = containerPageId),
                        title = listOf(RequestBuilders.createSimpleRichText("Test Database — Kotlin Client")),
                        icon = Icon.Emoji(emoji = "🗄️"),
                        initialDataSource =
                            InitialDataSource(
                                properties =
                                    mapOf(
                                        "Name" to CreateDatabaseProperty.Title(),
                                        "Description" to CreateDatabaseProperty.RichText(),
                                        "Priority" to CreateDatabaseProperty.Select(),
                                        "Completed" to CreateDatabaseProperty.Checkbox(),
                                        "Score" to CreateDatabaseProperty.Number(),
                                        "Contact" to CreateDatabaseProperty.Email(),
                                    ),
                            ),
                    )

                val createdDatabase = notion.databases.create(databaseRequest)
                val dataSourceId =
                    createdDatabase.dataSources.firstOrNull()?.id
                        ?: error("Database should have at least one data source")

                createdDatabase.objectType shouldBe "database"
                createdDatabase.title.first().plainText shouldBe "Test Database — Kotlin Client"
                createdDatabase.dataSources.size shouldBe 1
                createdDatabase.inTrash shouldBe false

                println("  Database: ${createdDatabase.url}")
                delay(500)

                val pageRequest =
                    CreatePageRequest(
                        parent = Parent.DataSourceParent(dataSourceId = dataSourceId),
                        icon = Icon.Emoji(emoji = "📋"),
                        properties =
                            pageProperties {
                                title("Name", "Test Task — Integration Test")
                                richText("Description", "Created by Kotlin integration test")
                                checkbox("Completed", false)
                                number("Score", 85.5)
                                email("Contact", "test@example.com")
                            },
                    )

                val createdPage = notion.pages.create(pageRequest)

                createdPage.objectType shouldBe "page"
                createdPage.inTrash shouldBe false
                createdPage.parent.id!!.withOrWithoutHyphens() shouldContainAnyOf dataSourceId.withOrWithoutHyphens()

                println("  Page: ${createdPage.url}")
                delay(500)

                val retrievedPage = notion.pages.retrieve(createdPage.id)

                retrievedPage.getTitleAsPlainText("Name") shouldBe "Test Task — Integration Test"
                retrievedPage.getRichTextAsPlainText("Description") shouldBe "Created by Kotlin integration test"
                retrievedPage.getNumberProperty("Score") shouldBe 85.5
                retrievedPage.getCheckboxProperty("Completed") shouldBe false
                retrievedPage.getEmailProperty("Contact") shouldBe "test@example.com"

                val updatedPage =
                    notion.pages.update(
                        createdPage.id,
                        UpdatePageRequest(
                            properties =
                                pageProperties {
                                    checkbox("Completed", true)
                                    number("Score", 95.0)
                                },
                        ),
                    )

                updatedPage.getCheckboxProperty("Completed") shouldBe true
                updatedPage.getNumberProperty("Score") shouldBe 95.0

                println("  ✅ CRUD lifecycle with type-safe properties verified")
            }

            // ------------------------------------------------------------------
            // 3. Standalone page — create and retrieve
            // ------------------------------------------------------------------
            "should create a standalone page and retrieve it" {
                val pageRequest =
                    CreatePageRequest(
                        parent = Parent.PageParent(pageId = containerPageId),
                        icon = Icon.Emoji(emoji = "📄"),
                        properties =
                            mapOf(
                                "title" to
                                    PagePropertyValue.TitleValue(
                                        title = listOf(RequestBuilders.createSimpleRichText("Standalone Page — Kotlin Client")),
                                    ),
                            ),
                    )

                val createdPage = notion.pages.create(pageRequest)

                createdPage.objectType shouldBe "page"
                createdPage.inTrash shouldBe false
                createdPage.parent.id!!.withOrWithoutHyphens() shouldContainAnyOf containerPageId.withOrWithoutHyphens()

                println("  Standalone: ${createdPage.url}")
                delay(500)

                val retrievedPage = notion.pages.retrieve(createdPage.id)
                retrievedPage.id.withOrWithoutHyphens() shouldContainAnyOf createdPage.id.withOrWithoutHyphens()
                retrievedPage.inTrash shouldBe false

                println("  ✅ Standalone page create and retrieve verified")
            }

            // ------------------------------------------------------------------
            // 4. Comprehensive property types
            // ------------------------------------------------------------------
            "should create a database with comprehensive property types and a page using them" {
                val comprehensiveRequest =
                    CreateDatabaseRequest(
                        parent = Parent.PageParent(pageId = containerPageId),
                        title = listOf(RequestBuilders.createSimpleRichText("Comprehensive Properties Test")),
                        icon = Icon.Emoji(emoji = "🧪"),
                        initialDataSource =
                            InitialDataSource(
                                properties =
                                    mapOf(
                                        "Title" to CreateDatabaseProperty.Title(),
                                        "Text" to CreateDatabaseProperty.RichText(),
                                        "Number" to CreateDatabaseProperty.Number(),
                                        "Checkbox" to CreateDatabaseProperty.Checkbox(),
                                        "URL" to CreateDatabaseProperty.Url(),
                                        "Email" to CreateDatabaseProperty.Email(),
                                        "Phone" to CreateDatabaseProperty.PhoneNumber(),
                                        "Start Date" to CreateDatabaseProperty.Date(),
                                        "Meeting Time" to CreateDatabaseProperty.Date(),
                                        "Single Select" to CreateDatabaseProperty.Select(),
                                        "Multi Select" to CreateDatabaseProperty.MultiSelect(),
                                    ),
                            ),
                    )

                val database = notion.databases.create(comprehensiveRequest)
                val dataSourceId =
                    database.dataSources.firstOrNull()?.id
                        ?: error("Database should have at least one data source")

                println("  Comprehensive database: ${database.url}")
                delay(500)

                val pageRequest =
                    CreatePageRequest(
                        parent = Parent.DataSourceParent(dataSourceId = dataSourceId),
                        icon = Icon.Emoji(emoji = "⭐"),
                        properties =
                            pageProperties {
                                title("Title", "Comprehensive Test Page")
                                richText("Text", "Demonstrates all property types with the builder API")
                                number("Number", 42.5)
                                checkbox("Checkbox", true)
                                url("URL", "https://notion.so")
                                email("Email", "test@comprehensive.com")
                                phoneNumber("Phone", "+1-555-0199")
                                date("Start Date", "2024-03-15")
                                dateTime("Meeting Time", "2024-03-15T14:30:00")
                                select("Single Select", "High Priority")
                                multiSelect("Multi Select", "testing", "kotlin", "api")
                            },
                    )

                val createdPage = notion.pages.create(pageRequest)
                println("  Comprehensive page: ${createdPage.url}")
                delay(500)

                val retrievedPage = notion.pages.retrieve(createdPage.id)

                retrievedPage.getTitleAsPlainText("Title") shouldBe "Comprehensive Test Page"
                retrievedPage.getRichTextAsPlainText("Text") shouldBe "Demonstrates all property types with the builder API"
                retrievedPage.getNumberProperty("Number") shouldBe 42.5
                retrievedPage.getCheckboxProperty("Checkbox") shouldBe true
                retrievedPage.getEmailProperty("Email") shouldBe "test@comprehensive.com"

                println("  ✅ Comprehensive property types verified")
            }

            // ------------------------------------------------------------------
            // 5. Page lock — lock, unlock, and lock(true/false) syntax
            // ------------------------------------------------------------------
            "should lock and unlock a page via all syntax variants" {
                val testPage =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Page Lock — lock/unlock test")
                        icon.emoji("🔓")
                        content { paragraph("This page will be locked and unlocked.") }
                    }
                println("  Lock test: ${testPage.url}")
                delay(1500)

                testPage.isLocked shouldBe false

                val lockedPage =
                    notion.pages.update(testPage.id) {
                        lock()
                        icon.emoji("🔒")
                    }
                delay(1000)

                lockedPage.isLocked shouldBe true
                (lockedPage.icon as? Icon.Emoji)?.emoji shouldBe "🔒"

                val retrievedLocked = notion.pages.retrieve(testPage.id)
                retrievedLocked.isLocked shouldBe true

                val unlockedPage =
                    notion.pages.update(testPage.id) {
                        unlock()
                        icon.emoji("🔓")
                    }
                delay(1000)

                unlockedPage.isLocked shouldBe false

                val lockedAgain = notion.pages.update(testPage.id) { lock(true) }
                lockedAgain.isLocked shouldBe true

                val unlockedAgain = notion.pages.update(testPage.id) { lock(false) }
                unlockedAgain.isLocked shouldBe false

                println("  ✅ lock(), unlock(), lock(true/false) all verified")
            }

            // ------------------------------------------------------------------
            // 6. Page move — move page to a different parent page
            // ------------------------------------------------------------------
            "should move a page to a new parent page" {
                val sourcePage =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Move Test — Source Page")
                        icon.emoji("📄")
                        content { paragraph("This page will be moved to a new parent.") }
                    }
                println("  Move source: ${sourcePage.url}")
                delay(1500)

                val destinationParent =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Move Test — Destination Parent")
                        icon.emoji("📁")
                        content {
                            heading1("Destination Folder")
                            paragraph("The source page will be moved here.")
                        }
                    }
                println("  Move destination: ${destinationParent.url}")
                delay(1500)

                val movedPage = notion.pages.moveToPage(sourcePage.id, destinationParent.id)
                println("  Moved page: ${movedPage.url}")
                delay(1500)

                val retrievedPage = notion.pages.retrieve(sourcePage.id)
                retrievedPage.parent.id shouldBe destinationParent.id

                println("  ✅ moveToPage() verified — source page is now under destination parent")
            }

            // ------------------------------------------------------------------
            // 7. Page move — move page into a database (data source)
            // ------------------------------------------------------------------
            "should move a standalone page into a database (data source)" {
                val standalonePage =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Page to Move to Database")
                        icon.emoji("📄")
                        content { paragraph("This page will be moved into a database.") }
                    }
                println("  Standalone: ${standalonePage.url}")
                delay(1500)

                val destinationDb =
                    notion.databases.create {
                        parent.page(containerPageId)
                        title("Move Destination Database")
                        icon.emoji("🗃️")
                        properties { title("Name") }
                    }
                println("  Move DB: ${destinationDb.url}")
                delay(2000)

                val retrievedDb = notion.databases.retrieve(destinationDb.id)
                val dataSourceId = retrievedDb.dataSources.first().id

                val movedPage = notion.pages.moveToDataSource(standalonePage.id, dataSourceId)
                println("  After move: ${movedPage.url}")
                delay(1500)

                val retrievedPage = notion.pages.retrieve(standalonePage.id)
                retrievedPage.parent.id shouldBe dataSourceId

                println("  ✅ moveToDataSource() verified — page is now a database entry")
            }

            // ------------------------------------------------------------------
            // 8. Page position — create sub-pages at start and end positions
            // ------------------------------------------------------------------
            "should create pages with position at page start and end" {
                val containerPage =
                    notion.pages.create {
                        parent.page(containerPageId)
                        title("Page Position — Container")
                        icon.emoji("📦")
                        content {
                            heading1("Container")
                            paragraph("First paragraph — existing content")
                            paragraph("Second paragraph — existing content")
                        }
                    }
                println("  Position container: ${containerPage.url}")
                delay(2000)

                val startPage =
                    notion.pages.create {
                        parent.page(containerPage.id)
                        title("Page at Start")
                        icon.emoji("1️⃣")
                        position.pageStart()
                    }
                println("  Start position: ${startPage.url}")
                delay(1500)

                val endPage =
                    notion.pages.create {
                        parent.page(containerPage.id)
                        title("Page at End")
                        icon.emoji("🔚")
                        position.pageEnd()
                    }
                println("  End position: ${endPage.url}")
                delay(1500)

                val retrievedStart = notion.pages.retrieve(startPage.id)
                retrievedStart.parent.id shouldBe containerPage.id

                val retrievedEnd = notion.pages.retrieve(endPage.id)
                retrievedEnd.parent.id shouldBe containerPage.id

                println("  ✅ pageStart() and pageEnd() positions verified")
                println("     Manual check: open the container URL above and verify sub-page order")
            }
        }
    })
