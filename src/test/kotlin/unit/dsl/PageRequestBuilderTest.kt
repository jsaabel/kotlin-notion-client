package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.PageIcon
import it.saabel.kotlinnotionclient.models.pages.PagePosition
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.pages.PageTemplate
import it.saabel.kotlinnotionclient.models.pages.createPageRequest

/**
 * Comprehensive unit tests for PageRequestBuilder DSL.
 *
 * These tests validate the DSL functionality without requiring API calls,
 * focusing on correct object construction and validation logic.
 */
class PageRequestBuilderTest :
    DescribeSpec({

        describe("PageRequestBuilder DSL") {

            describe("basic construction") {

                it("should create a minimal page request with data source parent") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                        }

                    request.parent shouldBe
                        Parent.DataSourceParent(dataSourceId = "test-ds-id")
                    request.properties shouldBe emptyMap()
                    request.icon.shouldBeNull()
                    request.cover.shouldBeNull()
                    request.children.shouldBeNull()
                }

                it("should create a minimal page request with page parent") {
                    val request =
                        createPageRequest {
                            parent.page("test-page-id")
                        }

                    request.parent shouldBe
                        Parent.PageParent(pageId = "test-page-id")
                }

                it("should create a minimal page request with block parent") {
                    val request =
                        createPageRequest {
                            parent.block("test-block-id")
                        }

                    request.parent shouldBe
                        Parent.BlockParent(blockId = "test-block-id")
                }

                it("should create a minimal page request with workspace parent") {
                    val request =
                        createPageRequest {
                            parent.workspace()
                        }

                    request.parent shouldBe Parent.WorkspaceParent
                }

                it("should fail when parent is not specified") {
                    shouldThrow<IllegalArgumentException> {
                        createPageRequest {
                            // No parent specified
                        }
                    }.message shouldContain "Parent must be specified"
                }
            }

            describe("title configuration") {
                it("should set title property") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            title("My Page Title")
                        }

                    request.properties shouldContainKey "title"
                    val titleValue = request.properties["title"]
                    titleValue.shouldBeInstanceOf<PagePropertyValue.TitleValue>()
                    titleValue.title shouldHaveSize 1
                    titleValue.title[0].plainText shouldBe "My Page Title"
                }

                it("should allow title property with page parent") {
                    val request =
                        createPageRequest {
                            parent.page("test-page-id")
                            title("My Page Title")
                        }

                    request.properties shouldContainKey "title"
                }
            }

            describe("properties configuration") {
                it("should set properties for data source pages") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            properties {
                                title("Name", "Test Page")
                                richText("Description", "Test description")
                                number("Score", 85.5)
                                checkbox("Completed", false)
                            }
                        }

                    request.properties shouldContainKey "Name"
                    request.properties shouldContainKey "Description"
                    request.properties shouldContainKey "Score"
                    request.properties shouldContainKey "Completed"

                    val nameValue = request.properties["Name"]
                    nameValue.shouldBeInstanceOf<PagePropertyValue.TitleValue>()

                    val descValue = request.properties["Description"]
                    descValue.shouldBeInstanceOf<PagePropertyValue.RichTextValue>()

                    val scoreValue = request.properties["Score"]
                    scoreValue.shouldBeInstanceOf<PagePropertyValue.NumberValue>()
                    scoreValue.number shouldBe 85.5

                    val completedValue = request.properties["Completed"]
                    completedValue.shouldBeInstanceOf<PagePropertyValue.CheckboxValue>()
                    completedValue.checkbox shouldBe false
                }

                it("should fail when properties are used with page parent") {
                    shouldThrow<IllegalStateException> {
                        createPageRequest {
                            parent.page("test-page-id")
                            properties {
                                richText("Description", "This should fail")
                            }
                        }
                    }.message shouldContain "Custom properties can only be set when creating pages in a data source"
                }

                it("should allow empty properties block for data source parent") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            properties {
                                // Empty properties block
                            }
                        }

                    request.properties shouldBe emptyMap()
                }

                it("should allow title and properties together for data source parent") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            title("Page Title")
                            properties {
                                richText("Description", "Some description")
                            }
                        }

                    request.properties shouldContainKey "title"
                    request.properties shouldContainKey "Description"
                    request.properties.size shouldBe 2
                }

                it("should validate against mixing custom properties with non-data-source parent") {
                    shouldThrow<IllegalStateException> {
                        createPageRequest {
                            parent.workspace()
                            properties {
                                richText("Invalid", "This should fail")
                            }
                        }
                    }.message shouldContain "Custom properties can only be set when creating pages in a data source"
                }
            }

            describe("template configuration") {
                it("should set template to none") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            template.none()
                        }

                    request.template shouldBe PageTemplate.None
                }

                it("should set template to default") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            template.default()
                        }

                    request.template shouldBe PageTemplate.Default
                }

                it("should set template by ID") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            template.byId("template-123")
                        }

                    request.template.shouldBeInstanceOf<PageTemplate.TemplateId>().templateId shouldBe "template-123"
                }

                it("should have null template when not specified") {
                    val request =
                        createPageRequest {
                            parent.page("test-page-id")
                        }

                    request.template.shouldBeNull()
                }

                it("should fail when both template and children are specified") {
                    shouldThrow<IllegalStateException> {
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            template.default()
                            content {
                                paragraph("This should cause an error")
                            }
                        }
                    }.message shouldContain "Template and children are mutually exclusive"
                }
            }

            describe("position configuration") {
                it("should set position after block") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            position.afterBlock("block-123")
                        }

                    request.position.shouldBeInstanceOf<PagePosition.AfterBlock>().afterBlock shouldBe "block-123"
                }

                it("should set position at page start") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            position.pageStart()
                        }

                    request.position shouldBe PagePosition.PageStart
                }

                it("should set position at page end") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            position.pageEnd()
                        }

                    request.position shouldBe PagePosition.PageEnd
                }

                it("should have null position when not specified") {
                    val request =
                        createPageRequest {
                            parent.page("test-page-id")
                        }

                    request.position.shouldBeNull()
                }
            }

            describe("icon configuration") {
                it("should set emoji icon") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            icon.emoji("\uD83D\uDCC4")
                        }

                    val emoji = request.icon.shouldBeInstanceOf<PageIcon.Emoji>()
                    emoji.emoji shouldBe "\uD83D\uDCC4"
                }

                it("should set external icon") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            icon.external("https://example.com/icon.png")
                        }

                    val external = request.icon.shouldBeInstanceOf<PageIcon.External>()
                    external.external.url shouldBe "https://example.com/icon.png"
                }

                it("should set file icon with expiry time") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            icon.file("https://files.notion.com/icon.png", "2024-12-31")
                        }

                    val fileIcon = request.icon.shouldBeInstanceOf<PageIcon.File>()
                    fileIcon.file.url shouldBe "https://files.notion.com/icon.png"
                    fileIcon.file.expiryTime shouldBe "2024-12-31"
                }

                it("should set file icon without expiry time") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            icon.file("https://files.notion.com/icon.png")
                        }

                    val fileIcon = request.icon.shouldBeInstanceOf<PageIcon.File>()
                    fileIcon.file.url shouldBe "https://files.notion.com/icon.png"
                    fileIcon.file.expiryTime.shouldBeNull()
                }
            }

            describe("cover configuration") {
                it("should set external cover") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            cover.external("https://example.com/cover.jpg")
                        }

                    val external = request.cover.shouldBeInstanceOf<PageCover.External>()
                    external.external.url shouldBe "https://example.com/cover.jpg"
                }

                it("should set file cover with expiry time") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            cover.file("https://files.notion.com/cover.jpg", "2024-12-31")
                        }

                    val fileCover = request.cover.shouldBeInstanceOf<PageCover.File>()
                    fileCover.file.url shouldBe "https://files.notion.com/cover.jpg"
                    fileCover.file.expiryTime shouldBe "2024-12-31"
                }

                it("should set file cover without expiry time") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            cover.file("https://files.notion.com/cover.jpg")
                        }

                    val fileCover = request.cover.shouldBeInstanceOf<PageCover.File>()
                    fileCover.file.url shouldBe "https://files.notion.com/cover.jpg"
                    fileCover.file.expiryTime.shouldBeNull()
                }
            }

            describe("content configuration") {
                it("should set content using PageContentBuilder") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            content {
                                heading1("Welcome")
                                paragraph("This is a paragraph")
                                divider()
                            }
                        }

                    val children = request.children.shouldNotBeNull()
                    children shouldHaveSize 3
                }
            }

            describe("comprehensive configuration") {
                it("should create a fully configured data source page") {
                    val request =
                        createPageRequest {
                            parent.dataSource("test-ds-id")
                            title("Comprehensive Test Page")
                            properties {
                                richText("Description", "A comprehensive test")
                                number("Score", 95.0)
                                checkbox("Featured", true)
                            }
                            icon.emoji("\uD83D\uDE80")
                            cover.external("https://example.com/banner.jpg")
                            content {
                                heading1("Main Title")
                                paragraph("Introduction paragraph")
                                bullet("First bullet point")
                                bullet("Second bullet point")
                                divider()
                                quote("An inspiring quote")
                            }
                        }

                    request.parent shouldBe Parent.DataSourceParent(dataSourceId = "test-ds-id")

                    request.properties shouldContainKey "title"
                    request.properties shouldContainKey "Description"
                    request.properties shouldContainKey "Score"
                    request.properties shouldContainKey "Featured"

                    val emoji = request.icon.shouldBeInstanceOf<PageIcon.Emoji>()
                    emoji.emoji shouldBe "\uD83D\uDE80"

                    val cover = request.cover.shouldBeInstanceOf<PageCover.External>()
                    cover.external.url shouldBe "https://example.com/banner.jpg"

                    val children = request.children.shouldNotBeNull()
                    children shouldHaveSize 6
                }

                it("should create a fully configured child page") {
                    val request =
                        createPageRequest {
                            parent.page("parent-page-id")
                            title("Child Page Title")
                            icon.emoji("\uD83D\uDCC4")
                            cover.file("https://files.notion.com/image.png")
                            content {
                                heading1("Child Page Content")
                                paragraph("This is a child page")
                            }
                        }

                    request.parent shouldBe Parent.PageParent(pageId = "parent-page-id")

                    // Should only have title property
                    request.properties shouldContainKey "title"
                    request.properties.size shouldBe 1

                    val emoji = request.icon.shouldBeInstanceOf<PageIcon.Emoji>()
                    emoji.emoji shouldBe "\uD83D\uDCC4"

                    val cover = request.cover.shouldBeInstanceOf<PageCover.File>()
                    cover.file.url shouldBe "https://files.notion.com/image.png"

                    val children = request.children.shouldNotBeNull()
                    children shouldHaveSize 2
                }
            }
        }
    })
