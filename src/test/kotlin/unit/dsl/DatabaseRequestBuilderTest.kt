package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.NotionFile
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.SelectOptionColor
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.CreateSelectOption
import it.saabel.kotlinnotionclient.models.databases.RelationConfiguration
import it.saabel.kotlinnotionclient.models.databases.databaseRequest
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.PageIcon
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

                request.parent shouldBe Parent.PageParent(pageId = "test-page-id")
                request.title shouldBe listOf(RichText.fromPlainText("Test Database"))
                request.initialDataSource.properties shouldHaveSize 1
                request.initialDataSource.properties shouldContainKey "Name"
                request.initialDataSource.properties["Name"].shouldBeInstanceOf<CreateDatabaseProperty.Title>()
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

                    request.parent shouldBe Parent.PageParent(pageId = "page-id")
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

                    request.parent shouldBe Parent.BlockParent(blockId = "block-id")
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

                    request.parent shouldBe Parent.WorkspaceParent
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

                    request.icon shouldBe PageIcon.Emoji(emoji = "📊")
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

                    request.icon shouldBe PageIcon.External(external = ExternalFile(url = "https://example.com/icon.png"))
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
                        PageIcon.File(
                            file = NotionFile(url = "https://files.notion.so/icon.png", expiryTime = "2023-01-01T00:00:00.000Z"),
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
                        PageIcon.File(
                            file = NotionFile(url = "https://files.notion.so/icon.png", expiryTime = null),
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
                        PageCover.External(
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
                        PageCover.File(
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
                        PageCover.File(
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

                    request.initialDataSource.properties shouldHaveSize 9
                    request.initialDataSource.properties["Name"].shouldBeInstanceOf<CreateDatabaseProperty.Title>()
                    request.initialDataSource.properties["Description"].shouldBeInstanceOf<CreateDatabaseProperty.RichText>()
                    request.initialDataSource.properties["Score"].shouldBeInstanceOf<CreateDatabaseProperty.Number>()
                    request.initialDataSource.properties["Due Date"].shouldBeInstanceOf<CreateDatabaseProperty.Date>()
                    request.initialDataSource.properties["Completed"].shouldBeInstanceOf<CreateDatabaseProperty.Checkbox>()
                    request.initialDataSource.properties["Website"].shouldBeInstanceOf<CreateDatabaseProperty.Url>()
                    request.initialDataSource.properties["Email"].shouldBeInstanceOf<CreateDatabaseProperty.Email>()
                    request.initialDataSource.properties["Phone"].shouldBeInstanceOf<CreateDatabaseProperty.PhoneNumber>()
                    request.initialDataSource.properties["Assignee"].shouldBeInstanceOf<CreateDatabaseProperty.People>()
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

                    val numberProperty = request.initialDataSource.properties["Price"] as CreateDatabaseProperty.Number
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

                    val selectProperty = request.initialDataSource.properties["Status"] as CreateDatabaseProperty.Select
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

                    val selectProperty = request.initialDataSource.properties["Status"] as CreateDatabaseProperty.Select
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

                    val multiSelectProperty = request.initialDataSource.properties["Tags"] as CreateDatabaseProperty.MultiSelect
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
                                relation("Related", "target-db-id", "target-ds-id") {
                                    single()
                                }
                            }
                        }

                    val relationProperty = request.initialDataSource.properties["Related"] as CreateDatabaseProperty.Relation
                    relationProperty.relation.databaseId shouldBe "target-db-id"
                    relationProperty.relation.dataSourceId shouldBe "target-ds-id"
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
                                relation("Related", "target-db-id", "target-ds-id") {
                                    dual("Backlink", "prop-id")
                                }
                            }
                        }

                    val relationProperty = request.initialDataSource.properties["Related"] as CreateDatabaseProperty.Relation
                    relationProperty.relation shouldBe
                        RelationConfiguration.dualProperty("target-db-id", "target-ds-id", "Backlink", "prop-id")
                }

                "relation with synced property" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                relation("Related", "target-db-id", "target-ds-id") {
                                    synced("Backlink")
                                }
                            }
                        }

                    val relationProperty = request.initialDataSource.properties["Related"] as CreateDatabaseProperty.Relation
                    relationProperty.relation shouldBe RelationConfiguration.synced("target-db-id", "target-ds-id", "Backlink")
                }

                "relation with default configuration" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                relation("Related", "target-db-id", "target-ds-id")
                            }
                        }

                    val relationProperty = request.initialDataSource.properties["Related"] as CreateDatabaseProperty.Relation
                    relationProperty.relation.databaseId shouldBe "target-db-id"
                    relationProperty.relation.dataSourceId shouldBe "target-ds-id"
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
                            relation("Related Tasks", "other-db-id", "other-ds-id") {
                                dual("Backlink", "prop-id")
                            }
                        }
                    }

                request.parent shouldBe Parent.PageParent(pageId = "parent-page-id")
                request.title shouldBe listOf(RichText.fromPlainText("Comprehensive Database"))
                request.description shouldBe listOf(RichText.fromPlainText("A database with all possible features"))
                request.icon shouldBe PageIcon.Emoji(emoji = "🚀")
                request.cover shouldBe PageCover.External(external = ExternalFile(url = "https://example.com/cover.jpg"))
                request.initialDataSource.properties shouldHaveSize 12
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

                    val selectProperty = request.initialDataSource.properties["Status"] as CreateDatabaseProperty.Select
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

                    val selectProperty = request.initialDataSource.properties["Status"] as CreateDatabaseProperty.Select
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

                    val numberProperty = request.initialDataSource.properties["Score"] as CreateDatabaseProperty.Number
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

                    request.initialDataSource.properties shouldHaveSize 2

                    val priorityProperty = request.initialDataSource.properties["Priority"] as CreateDatabaseProperty.Select
                    priorityProperty.select.options shouldHaveSizeList 3

                    val categoriesProperty = request.initialDataSource.properties["Categories"] as CreateDatabaseProperty.MultiSelect
                    categoriesProperty.multiSelect.options shouldHaveSizeList 2
                }

                "multiple relation properties" {
                    val request =
                        databaseRequest {
                            parent.page("test-page-id")
                            title("Test Database")
                            properties {
                                relation("Parent Tasks", "tasks-db-id", "tasks-ds-id") {
                                    single()
                                }
                                relation("Child Tasks", "tasks-db-id", "tasks-ds-id") {
                                    dual("Parent", "parent-prop-id")
                                }
                            }
                        }

                    request.initialDataSource.properties shouldHaveSize 2

                    val parentProperty = request.initialDataSource.properties["Parent Tasks"] as CreateDatabaseProperty.Relation
                    parentProperty.relation.databaseId shouldBe "tasks-db-id"
                    parentProperty.relation.singleProperty shouldNotBe null

                    val childProperty = request.initialDataSource.properties["Child Tasks"] as CreateDatabaseProperty.Relation
                    childProperty.relation shouldBe
                        RelationConfiguration.dualProperty("tasks-db-id", "tasks-ds-id", "Parent", "parent-prop-id")
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
                            relation("Dependencies", "deps-db-id", "deps-ds-id") {
                                dual("Dependents", "dep-prop-id")
                            }
                        }
                    }

                request shouldNotBe null
                request.initialDataSource.properties shouldHaveSize 4
                request.icon shouldNotBe null
                request.cover shouldNotBe null
                request.description shouldNotBe null
            }
        }
    })
