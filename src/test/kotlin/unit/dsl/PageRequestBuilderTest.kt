package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.pages.createPageRequest

// TODO: Adjust/replace following API version update

/**
 * Comprehensive unit tests for PageRequestBuilder DSL.
 *
 * These tests validate the DSL functionality without requiring API calls,
 * focusing on correct object construction and validation logic.
 */
class PageRequestBuilderTest :
    DescribeSpec({

        xdescribe("PageRequestBuilder DSL") {

            describe("basic construction") {

                // TODO: Adjust/replace following API version update
//                it("should create a minimal page request with database parent") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                        }
//
//                    request.parent shouldBe
//                        Parent(
//                            type = "database_id",
//                            databaseId = "test-db-id",
//                        )
//                    request.properties shouldBe emptyMap()
//                    request.icon.shouldBeNull()
//                    request.cover.shouldBeNull()
//                    request.children.shouldBeNull()
//                }

                it("should create a minimal page request with page parent") {
                    val request =
                        createPageRequest {
                            parent.page("test-page-id")
                        }

                    request.parent shouldBe
                        Parent(
                            type = "page_id",
                            pageId = "test-page-id",
                        )
                }

                it("should create a minimal page request with block parent") {
                    val request =
                        createPageRequest {
                            parent.block("test-block-id")
                        }

                    request.parent shouldBe
                        Parent(
                            type = "block_id",
                            blockId = "test-block-id",
                        )
                }

                it("should create a minimal page request with workspace parent") {
                    val request =
                        createPageRequest {
                            parent.workspace()
                        }

                    request.parent shouldBe
                        Parent(
                            type = "workspace",
                            workspace = true,
                        )
                }

                it("should fail when parent is not specified") {
                    shouldThrow<IllegalArgumentException> {
                        createPageRequest {
                            // No parent specified
                        }
                    }.message shouldContain "Parent must be specified"
                }
            }

            // TODO: Adjust/replace following API version update
//            describe("title configuration") {
//                it("should set title property") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            title("My Page Title")
//                        }
//
//                    request.properties shouldContainKey "title"
//                    val titleValue = request.properties["title"]
//                    titleValue.shouldBeInstanceOf<PagePropertyValue.TitleValue>()
//                    titleValue as PagePropertyValue.TitleValue
//                    titleValue.title shouldHaveSize 1
//                    titleValue.title[0].plainText shouldBe "My Page Title"
//                }
//            }

            // TODO: Adjust/replace following API version update
//            describe("properties configuration") {
//                it("should set properties for database pages") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            properties {
//                                title("Name", "Test Page")
//                                richText("Description", "Test description")
//                                number("Score", 85.5)
//                                checkbox("Completed", false)
//                            }
//                        }
//
//                    request.properties shouldContainKey "Name"
//                    request.properties shouldContainKey "Description"
//                    request.properties shouldContainKey "Score"
//                    request.properties shouldContainKey "Completed"
//
//                    val nameValue = request.properties["Name"]
//                    nameValue.shouldBeInstanceOf<PagePropertyValue.TitleValue>()
//
//                    val descValue = request.properties["Description"]
//                    descValue.shouldBeInstanceOf<PagePropertyValue.RichTextValue>()
//
//                    val scoreValue = request.properties["Score"]
//                    scoreValue.shouldBeInstanceOf<PagePropertyValue.NumberValue>()
//                    (scoreValue as PagePropertyValue.NumberValue).number shouldBe 85.5
//
//                    val completedValue = request.properties["Completed"]
//                    completedValue.shouldBeInstanceOf<PagePropertyValue.CheckboxValue>()
//                    (completedValue as PagePropertyValue.CheckboxValue).checkbox shouldBe false
//                }

            it("should fail when properties are used with page parent") {
                shouldThrow<IllegalStateException> {
                    createPageRequest {
                        parent.page("test-page-id")
                        properties {
                            richText("Description", "This should fail")
                        }
                    }
                }.message shouldContain "Custom properties can only be set when creating pages in a database"
            }

            it("should allow title property with page parent") {
                val request =
                    createPageRequest {
                        parent.page("test-page-id")
                        title("My Page Title")
                    }

                request.properties shouldContainKey "title"
                // Should not throw exception
            }
        }

        // TODO: Adjust/replace following API version update
//            describe("icon configuration") {
//                it("should set emoji icon") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            icon.emoji("📄")
//                        }
//
//                    request.icon.shouldNotBeNull()
//                    request.icon!!.type shouldBe "emoji"
//                    request.icon!!.emoji shouldBe "📄"
//                }
//
//                it("should set external icon") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            icon.external("https://example.com/icon.png")
//                        }
//
//                    request.icon.shouldNotBeNull()
//                    request.icon!!.type shouldBe "external"
//                    request.icon!!.external!!.url shouldBe "https://example.com/icon.png"
//                }
//
//                it("should set file icon") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            icon.file("https://files.notion.com/icon.png", "2024-12-31")
//                        }
//
//                    request.icon.shouldNotBeNull()
//                    request.icon!!.type shouldBe "file"
//                    request.icon!!.file!!.url shouldBe "https://files.notion.com/icon.png"
//                    request.icon!!.file!!.expiryTime shouldBe "2024-12-31"
//                }
//
//                it("should set file icon without expiry time") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            icon.file("https://files.notion.com/icon.png")
//                        }
//
//                    request.icon.shouldNotBeNull()
//                    request.icon!!.type shouldBe "file"
//                    request.icon!!.file!!.url shouldBe "https://files.notion.com/icon.png"
//                    request.icon!!
//                        .file!!
//                        .expiryTime
//                        .shouldBeNull()
//                }
//            }

        // TODO: Adjust/replace following API version update
//            describe("cover configuration") {
//                it("should set external cover") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            cover.external("https://example.com/cover.jpg")
//                        }
//
//                    request.cover.shouldNotBeNull()
//                    request.cover!!.type shouldBe "external"
//                    request.cover!!.external.shouldNotBeNull()
//                    request.cover!!.external!!.url shouldBe "https://example.com/cover.jpg"
//                }
//
//                it("should set file cover") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            cover.file("https://files.notion.com/cover.jpg", "2024-12-31")
//                        }
//
//                    request.cover.shouldNotBeNull()
//                    request.cover!!.type shouldBe "file"
//                    request.cover!!.file.shouldNotBeNull()
//                    request.cover!!.file!!.url shouldBe "https://files.notion.com/cover.jpg"
//                    request.cover!!.file!!.expiryTime shouldBe "2024-12-31"
//                }
//
//                it("should set file cover without expiry time") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            cover.file("https://files.notion.com/cover.jpg")
//                        }
//
//                    request.cover.shouldNotBeNull()
//                    request.cover!!.type shouldBe "file"
//                    request.cover!!.file.shouldNotBeNull()
//                    request.cover!!.file!!.url shouldBe "https://files.notion.com/cover.jpg"
//                    request.cover!!
//                        .file!!
//                        .expiryTime
//                        .shouldBeNull()
//                }
//            }

        // TODO: Adjust/replace following API version update
//        describe("content configuration") {
//                it("should set content using pagecontentbuilder") {
//                    val request =
//                        createpagerequest {
//                            parent.database("test-db-id")
//                            content {
//                                heading1("welcome")
//                                paragraph("this is a paragraph")
//                                divider()
//                            }
//                        }
//
//                    request.children.shouldnotbenull()
//                    request.children!! shouldhavesize 3
//
//                    // children should be blockrequest instances
//                    request.children!!.foreach { child ->
//                        child.shouldbeinstanceof<blockrequest>()
//                    }
//                }
//            }
//
//            describe("comprehensive configuration") {
//                it("should create a fully configured database page") {
//                    val request =
//                        createpagerequest {
//                            parent.database("test-db-id")
//                            title("comprehensive test page")
//                            properties {
//                                richtext("description", "a comprehensive test")
//                                number("score", 95.0)
//                                checkbox("featured", true)
//                            }
//                            icon.emoji("🚀")
//                            cover.external("https://example.com/banner.jpg")
//                            content {
//                                heading1("main title")
//                                paragraph("introduction paragraph")
//                                bullet("first bullet point")
//                                bullet("second bullet point")
//                                divider()
//                                quote("an inspiring quote")
//                            }
//                        }
//
//                    // verify all components
//                    request.parent.type shouldbe "database_id"
//                    request.parent.databaseid shouldbe "test-db-id"
//
//                    request.properties shouldcontainkey "title"
//                    request.properties shouldcontainkey "description"
//                    request.properties shouldcontainkey "score"
//                    request.properties shouldcontainkey "featured"
//
//                    request.icon.shouldnotbenull()
//                    request.icon!!.emoji shouldbe "🚀"
//
//                    request.cover.shouldnotbenull()
//                    request.cover!!.external!!.url shouldbe "https://example.com/banner.jpg"
//
//                    request.children.shouldnotbenull()
//                    request.children!! shouldhavesize 6
//                }

//                it("should create a fully configured child page") {
//                    val request =
//                        createPageRequest {
//                            parent.page("parent-page-id")
//                            title("Child Page Title")
//                            icon.emoji("📄")
//                            cover.file("https://files.notion.com/image.png")
//                            content {
//                                heading1("Child Page Content")
//                                paragraph("This is a child page")
//                            }
//                        }
//
//                    // Verify all components
//                    request.parent.type shouldBe "page_id"
//                    request.parent.pageId shouldBe "parent-page-id"
//
//                    // Should only have title property
//                    request.properties shouldContainKey "title"
//                    request.properties.size shouldBe 1
//
//                    request.icon.shouldNotBeNull()
//                    request.icon!!.emoji shouldBe "📄"
//
//                    request.cover.shouldNotBeNull()
//                    request.cover!!.file!!.url shouldBe "https://files.notion.com/image.png"
//
//                    request.children.shouldNotBeNull()
//                    request.children!! shouldHaveSize 2
//                }
//            }

//            describe("validation edge cases") {
//                it("should allow empty properties block for database parent") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            properties {
//                                // Empty properties block
//                            }
//                        }
//
//                    request.properties shouldBe emptyMap()
//                }
//
//                it("should allow title and properties together for database parent") {
//                    val request =
//                        createPageRequest {
//                            parent.database("test-db-id")
//                            title("Page Title")
//                            properties {
//                                richText("Description", "Some description")
//                            }
//                        }
//
//                    request.properties shouldContainKey "title"
//                    request.properties shouldContainKey "Description"
//                    request.properties.size shouldBe 2
//                }
//
//                it("should validate against mixing custom properties with non-database parent") {
//                    shouldThrow<IllegalStateException> {
//                        createPageRequest {
//                            parent.workspace()
//                            properties {
//                                richText("Invalid", "This should fail")
//                            }
//                        }
//                    }.message shouldContain "Custom properties can only be set when creating pages in a database"
//                }
//            }
//        }
    })
