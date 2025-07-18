package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.base.SelectOptionColor
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseProperty
import no.saabelit.kotlinnotionclient.models.databases.CreateSelectOption
import no.saabelit.kotlinnotionclient.models.databases.RelationConfiguration
import no.saabelit.kotlinnotionclient.models.databases.databaseRequest
import no.saabelit.kotlinnotionclient.models.pages.ExternalFile
import no.saabelit.kotlinnotionclient.models.pages.NotionFile
import no.saabelit.kotlinnotionclient.models.pages.PageCover
import no.saabelit.kotlinnotionclient.models.pages.PageIcon
import io.kotest.matchers.collections.shouldHaveSize as shouldHaveSizeList

@Tags("Unit")
class DatabaseRequestBuilderTest :
    FreeSpec({

        "DatabaseRequestBuilder" - {
            "should create basic database request" {
                val request =
                    databaseRequest {
                        parent.page("test-page-id")
                        title("Test Database")
                        properties {
                            title("Name")
                        }
                    }

                request.parent shouldBe Parent(type = "page_id", pageId = "test-page-id")
                request.title shouldBe listOf(RichText.fromPlainText("Test Database"))
                request.properties shouldHaveSize 1
                request.properties shouldContainKey "Name"
                request.properties["Name"].shouldBeInstanceOf<CreateDatabaseProperty.Title>()
                request.icon shouldBe null
                request.cover shouldBe null
                request.description shouldBe null
            }

            "should create database with all parent types" - {
                "page parent" {
                    val request =
                        databaseRequest {
                            parent.page("page-id")
                            title("Database")
                            properties {
                                title("Name")
                            }
                        }

                    request.parent shouldBe Parent(type = "page_id", pageId = "page-id")
                }

                "block parent" {
                    val request =
                        databaseRequest {
                            parent.block("block-id")
                            title("Database")
                            properties {
                                title("Name")
                            }
                        }

                    request.parent shouldBe Parent(type = "block_id", blockId = "block-id")
                }

                "workspace parent" {
                    val request =
                        databaseRequest {
                            parent.workspace()
                            title("Database")
                            properties {
                                title("Name")
                            }
                        }

                    request.parent shouldBe Parent(type = "workspace", workspace = true)
                }
            }

            "should create database with description" {
                val request =
                    databaseRequest {
                        parent.page("test-page-id")
                        title("Test Database")
                        description("This is a test database")
                        properties {
                            title("Name")
                        }
                    }

                request.description shouldBe listOf(RichText.fromPlainText("This is a test database"))
            }

            "should create database with icon" - {
                "emoji icon" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            icon.emoji("📊")
                            properties {
                                title("Name")
                            }
                        }

                    request.icon shouldBe PageIcon(type = "emoji", emoji = "📊")
                }

                "external icon" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            icon.external("https://example.com/icon.png")
                            properties {
                                title("Name")
                            }
                        }

                    request.icon shouldBe PageIcon(type = "external", url = "https://example.com/icon.png")
                }

                "file icon" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            icon.file("https://files.notion.so/icon.png", "2023-01-01T00:00:00.000Z")
                            properties {
                                title("Name")
                            }
                        }

                    request.icon shouldBe
                        PageIcon(
                            type = "file",
                            url = "https://files.notion.so/icon.png",
                            expiryTime = "2023-01-01T00:00:00.000Z",
                        )
                }

                "file icon without expiry" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            icon.file("https://files.notion.so/icon.png")
                            properties {
                                title("Name")
                            }
                        }

                    request.icon shouldBe
                        PageIcon(
                            type = "file",
                            url = "https://files.notion.so/icon.png",
                            expiryTime = null,
                        )
                }
            }

            "should create database with cover" - {
                "external cover" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            cover.external("https://example.com/cover.jpg")
                            properties {
                                title("Name")
                            }
                        }

                    request.cover shouldBe
                        PageCover(
                            type = "external",
                            external = ExternalFile(url = "https://example.com/cover.jpg"),
                        )
                }

                "file cover" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            cover.file("https://files.notion.so/cover.jpg", "2023-01-01T00:00:00.000Z")
                            properties {
                                title("Name")
                            }
                        }

                    request.cover shouldBe
                        PageCover(
                            type = "file",
                            file =
                                NotionFile(
                                    url = "https://files.notion.so/cover.jpg",
                                    expiryTime = "2023-01-01T00:00:00.000Z",
                                ),
                        )
                }

                "file cover without expiry" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            cover.file("https://files.notion.so/cover.jpg")
                            properties {
                                title("Name")
                            }
                        }

                    request.cover shouldBe
                        PageCover(
                            type = "file",
                            file =
                                NotionFile(
                                    url = "https://files.notion.so/cover.jpg",
                                    expiryTime = null,
                                ),
                        )
                }
            }

            "should create database with all property types" - {
                "basic properties" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                title("Name")
                                richText("Description")
                                number("Score")
                                date("Due Date")
                                checkbox("Completed")
                                url("Website")
                                email("Email")
                                phoneNumber("Phone")
                                people("Assignee")
                            }
                        }

                    request.properties shouldHaveSize 9
                    request.properties["Name"].shouldBeInstanceOf<CreateDatabaseProperty.Title>()
                    request.properties["Description"].shouldBeInstanceOf<CreateDatabaseProperty.RichText>()
                    request.properties["Score"].shouldBeInstanceOf<CreateDatabaseProperty.Number>()
                    request.properties["Due Date"].shouldBeInstanceOf<CreateDatabaseProperty.Date>()
                    request.properties["Completed"].shouldBeInstanceOf<CreateDatabaseProperty.Checkbox>()
                    request.properties["Website"].shouldBeInstanceOf<CreateDatabaseProperty.Url>()
                    request.properties["Email"].shouldBeInstanceOf<CreateDatabaseProperty.Email>()
                    request.properties["Phone"].shouldBeInstanceOf<CreateDatabaseProperty.PhoneNumber>()
                    request.properties["Assignee"].shouldBeInstanceOf<CreateDatabaseProperty.People>()
                }

                "number with custom format" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                number("Price", format = "dollar")
                            }
                        }

                    val numberProperty = request.properties["Price"] as CreateDatabaseProperty.Number
                    numberProperty.number.format shouldBe "dollar"
                }

                "select with options" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                select("Status") {
                                    option("To Do", SelectOptionColor.RED)
                                    option("In Progress", SelectOptionColor.YELLOW)
                                    option("Done", SelectOptionColor.GREEN)
                                }
                            }
                        }

                    val selectProperty = request.properties["Status"] as CreateDatabaseProperty.Select
                    selectProperty.select.options shouldHaveSizeList 3
                    selectProperty.select.options[0] shouldBe CreateSelectOption("To Do", SelectOptionColor.RED)
                    selectProperty.select.options[1] shouldBe CreateSelectOption("In Progress", SelectOptionColor.YELLOW)
                    selectProperty.select.options[2] shouldBe CreateSelectOption("Done", SelectOptionColor.GREEN)
                }

                "select without options" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                select("Status")
                            }
                        }

                    val selectProperty = request.properties["Status"] as CreateDatabaseProperty.Select
                    selectProperty.select.options shouldHaveSizeList 0
                }

                "multi-select with options" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                multiSelect("Tags") {
                                    option("Important", SelectOptionColor.RED)
                                    option("Urgent", SelectOptionColor.ORANGE)
                                }
                            }
                        }

                    val multiSelectProperty = request.properties["Tags"] as CreateDatabaseProperty.MultiSelect
                    multiSelectProperty.multiSelect.options shouldHaveSizeList 2
                    multiSelectProperty.multiSelect.options[0] shouldBe CreateSelectOption("Important", SelectOptionColor.RED)
                    multiSelectProperty.multiSelect.options[1] shouldBe CreateSelectOption("Urgent", SelectOptionColor.ORANGE)
                }

                "relation with single property" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                relation("Related", "target-db-id") {
                                    single()
                                }
                            }
                        }

                    val relationProperty = request.properties["Related"] as CreateDatabaseProperty.Relation
                    relationProperty.relation.databaseId shouldBe "target-db-id"
                    relationProperty.relation.singleProperty shouldNotBe null
                    relationProperty.relation.dualProperty shouldBe null
                    relationProperty.relation.syncedPropertyName shouldBe null
                }

                "relation with dual property" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                relation("Related", "target-db-id") {
                                    dual("Backlink", "prop-id")
                                }
                            }
                        }

                    val relationProperty = request.properties["Related"] as CreateDatabaseProperty.Relation
                    relationProperty.relation shouldBe RelationConfiguration.dualProperty("target-db-id", "Backlink", "prop-id")
                }

                "relation with synced property" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                relation("Related", "target-db-id") {
                                    synced("Backlink")
                                }
                            }
                        }

                    val relationProperty = request.properties["Related"] as CreateDatabaseProperty.Relation
                    relationProperty.relation shouldBe RelationConfiguration.synced("target-db-id", "Backlink")
                }

                "relation with default configuration" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                relation("Related", "target-db-id")
                            }
                        }

                    val relationProperty = request.properties["Related"] as CreateDatabaseProperty.Relation
                    relationProperty.relation.databaseId shouldBe "target-db-id"
                    relationProperty.relation.singleProperty shouldNotBe null
                    relationProperty.relation.dualProperty shouldBe null
                    relationProperty.relation.syncedPropertyName shouldBe null
                }
            }

            "should create comprehensive database with all features" {
                val request =
                    databaseRequest {
                        parent.page("parent-page-id")
                        title("Comprehensive Database")
                        description("A database with all possible features")
                        icon.emoji("🚀")
                        cover.external("https://example.com/cover.jpg")
                        properties {
                            title("Name")
                            richText("Description")
                            number("Score", format = "number")
                            select("Status") {
                                option("To Do", SelectOptionColor.RED)
                                option("In Progress", SelectOptionColor.YELLOW)
                                option("Done", SelectOptionColor.GREEN)
                            }
                            multiSelect("Tags") {
                                option("Important", SelectOptionColor.RED)
                                option("Urgent", SelectOptionColor.ORANGE)
                            }
                            date("Due Date")
                            checkbox("Completed")
                            url("Website")
                            email("Contact")
                            phoneNumber("Phone")
                            people("Assignee")
                            relation("Related Tasks", "other-db-id") {
                                dual("Backlink", "prop-id")
                            }
                        }
                    }

                request.parent shouldBe Parent(type = "page_id", pageId = "parent-page-id")
                request.title shouldBe listOf(RichText.fromPlainText("Comprehensive Database"))
                request.description shouldBe listOf(RichText.fromPlainText("A database with all possible features"))
                request.icon shouldBe PageIcon(type = "emoji", emoji = "🚀")
                request.cover shouldBe PageCover(type = "external", external = ExternalFile(url = "https://example.com/cover.jpg"))
                request.properties shouldHaveSize 12
            }

            "should validate required fields" - {
                "require parent" {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            databaseRequest {
                                title("Test Database")
                                properties {
                                    title("Name")
                                }
                            }
                        }
                    exception.message shouldBe "Parent must be specified"
                }

                "require title" {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            databaseRequest {
                                parent.page("test-page-id")
                                properties {
                                    title("Name")
                                }
                            }
                        }
                    exception.message shouldBe "Title must be specified"
                }

                "require at least one property" {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            databaseRequest {
                                parent.page("test-page-id")
                                title("Test Database")
                            }
                        }
                    exception.message shouldBe "Database must have at least one property"
                }
            }

            "should handle edge cases" - {
                "empty select options" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                select("Status") {
                                    // No options
                                }
                            }
                        }

                    val selectProperty = request.properties["Status"] as CreateDatabaseProperty.Select
                    selectProperty.select.options shouldHaveSizeList 0
                }

                "select option with default color" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                select("Status") {
                                    option("Default")
                                }
                            }
                        }

                    val selectProperty = request.properties["Status"] as CreateDatabaseProperty.Select
                    selectProperty.select.options[0].color shouldBe SelectOptionColor.DEFAULT
                }

                "number with default format" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                number("Score")
                            }
                        }

                    val numberProperty = request.properties["Score"] as CreateDatabaseProperty.Number
                    numberProperty.number.format shouldBe "number"
                }
            }

            "should build complex property configurations" - {
                "multiple select properties with different configurations" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                select("Priority") {
                                    option("High", SelectOptionColor.RED)
                                    option("Medium", SelectOptionColor.YELLOW)
                                    option("Low", SelectOptionColor.GREEN)
                                }
                                multiSelect("Categories") {
                                    option("Work", SelectOptionColor.BLUE)
                                    option("Personal", SelectOptionColor.PURPLE)
                                }
                            }
                        }

                    request.properties shouldHaveSize 2

                    val priorityProperty = request.properties["Priority"] as CreateDatabaseProperty.Select
                    priorityProperty.select.options shouldHaveSizeList 3

                    val categoriesProperty = request.properties["Categories"] as CreateDatabaseProperty.MultiSelect
                    categoriesProperty.multiSelect.options shouldHaveSizeList 2
                }

                "multiple relation properties" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                relation("Parent Tasks", "tasks-db-id") {
                                    single()
                                }
                                relation("Child Tasks", "tasks-db-id") {
                                    dual("Parent", "parent-prop-id")
                                }
                            }
                        }

                    request.properties shouldHaveSize 2

                    val parentProperty = request.properties["Parent Tasks"] as CreateDatabaseProperty.Relation
                    parentProperty.relation.databaseId shouldBe "tasks-db-id"
                    parentProperty.relation.singleProperty shouldNotBe null

                    val childProperty = request.properties["Child Tasks"] as CreateDatabaseProperty.Relation
                    childProperty.relation shouldBe RelationConfiguration.dualProperty("tasks-db-id", "Parent", "parent-prop-id")
                }
            }

            "should support method chaining and nested configurations" {
                val request =
                    databaseRequest {
                        parent.page("test-page-id")
                        title("Chained Database")
                        description("Testing method chaining")
                        icon.emoji("⛓️")
                        cover.external("https://example.com/chain.jpg")
                        properties {
                            title("Name")
                            richText("Notes")
                            select("Status") {
                                option("New", SelectOptionColor.GRAY)
                                option("Active", SelectOptionColor.GREEN)
                                option("Closed", SelectOptionColor.RED)
                            }
                            relation("Dependencies", "deps-db-id") {
                                dual("Dependents", "dep-prop-id")
                            }
                        }
                    }

                request shouldNotBe null
                request.properties shouldHaveSize 4
                request.icon shouldNotBe null
                request.cover shouldNotBe null
                request.description shouldNotBe null
            }
        }
    })
