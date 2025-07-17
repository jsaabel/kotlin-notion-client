package dsl

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.saabelit.kotlinnotionclient.models.base.Color
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.models.blocks.pageContent

/**
 * Unit tests for PageContentBuilder DSL focusing on the new block types.
 *
 * These tests verify that the DSL correctly creates BlockRequest objects for all
 * the newly implemented block types without requiring API calls.
 */
@Tags("Unit")
class PageContentBuilderTest :
    DescribeSpec({

        describe("PageContentBuilder DSL - New Block Types") {

            describe("bookmark blocks") {
                it("should create bookmark block with url") {
                    val content =
                        pageContent {
                            bookmark("https://example.com")
                        }

                    content shouldHaveSize 1
                    val bookmarkBlock = content[0].shouldBeInstanceOf<BlockRequest.Bookmark>()
                    bookmarkBlock.bookmark.url shouldBe "https://example.com"
                    bookmarkBlock.bookmark.caption shouldBe emptyList()
                }

                it("should create bookmark block with url and caption") {
                    val content =
                        pageContent {
                            bookmark("https://example.com", caption = "Example website")
                        }

                    content shouldHaveSize 1
                    val bookmarkBlock = content[0].shouldBeInstanceOf<BlockRequest.Bookmark>()
                    bookmarkBlock.bookmark.url shouldBe "https://example.com"
                    bookmarkBlock.bookmark.caption shouldHaveSize 1
                    bookmarkBlock.bookmark.caption[0].plainText shouldBe "Example website"
                }
            }

            describe("embed blocks") {
                it("should create embed block with url") {
                    val content =
                        pageContent {
                            embed("https://youtube.com/watch?v=example")
                        }

                    content shouldHaveSize 1
                    val embedBlock = content[0].shouldBeInstanceOf<BlockRequest.Embed>()
                    embedBlock.embed.url shouldBe "https://youtube.com/watch?v=example"
                }
            }

            describe("child page blocks") {
                // Note: Child page blocks cannot be created via blocks API
                // They must be created through pages API with proper parent relationships
                // These tests verify DSL structure only
                it("should create child page block with title") {
                    val content =
                        pageContent {
                            childPage("My Child Page")
                        }

                    content shouldHaveSize 1
                    val childPageBlock = content[0].shouldBeInstanceOf<BlockRequest.ChildPage>()
                    childPageBlock.childPage.title shouldBe "My Child Page"
                }
            }

            describe("child database blocks") {
                // Note: Child database blocks cannot be created via blocks API
                // They must be created through pages API with proper parent relationships
                // These tests verify DSL structure only
                it("should create child database block with title") {
                    val content =
                        pageContent {
                            childDatabase("My Database")
                        }

                    content shouldHaveSize 1
                    val childDatabaseBlock = content[0].shouldBeInstanceOf<BlockRequest.ChildDatabase>()
                    childDatabaseBlock.childDatabase.title shouldBe "My Database"
                }
            }

            describe("column list blocks") {
                it("should create column list with single column") {
                    val content =
                        pageContent {
                            columnList {
                                column {
                                    paragraph("Column content")
                                }
                            }
                        }

                    content shouldHaveSize 1
                    val columnListBlock = content[0].shouldBeInstanceOf<BlockRequest.ColumnList>()
                    columnListBlock.columnList.children!! shouldHaveSize 1

                    val columnBlock = columnListBlock.columnList.children[0].shouldBeInstanceOf<BlockRequest.Column>()
                    columnBlock.column.children!! shouldHaveSize 1

                    val paragraphBlock = columnBlock.column.children[0].shouldBeInstanceOf<BlockRequest.Paragraph>()
                    paragraphBlock.paragraph.richText[0].plainText shouldBe "Column content"
                }

                it("should create column list with multiple columns") {
                    val content =
                        pageContent {
                            columnList {
                                column {
                                    paragraph("Left column")
                                }
                                column {
                                    paragraph("Right column")
                                }
                            }
                        }

                    content shouldHaveSize 1
                    val columnListBlock = content[0].shouldBeInstanceOf<BlockRequest.ColumnList>()
                    columnListBlock.columnList.children!! shouldHaveSize 2

                    val leftColumn = columnListBlock.columnList.children[0].shouldBeInstanceOf<BlockRequest.Column>()
                    leftColumn.column.children!! shouldHaveSize 1

                    val rightColumn = columnListBlock.columnList.children[1].shouldBeInstanceOf<BlockRequest.Column>()
                    rightColumn.column.children!! shouldHaveSize 1
                }
            }

            describe("breadcrumb blocks") {
                it("should create breadcrumb block") {
                    val content =
                        pageContent {
                            breadcrumb()
                        }

                    content shouldHaveSize 1
                    content[0].shouldBeInstanceOf<BlockRequest.Breadcrumb>()
                }
            }

            describe("table of contents blocks") {
                it("should create table of contents block with default color") {
                    val content =
                        pageContent {
                            tableOfContents()
                        }

                    content shouldHaveSize 1
                    val tocBlock = content[0].shouldBeInstanceOf<BlockRequest.TableOfContents>()
                    tocBlock.tableOfContents.color shouldBe Color.DEFAULT
                }

                it("should create table of contents block with custom color") {
                    val content =
                        pageContent {
                            tableOfContents(color = Color.BLUE)
                        }

                    content shouldHaveSize 1
                    val tocBlock = content[0].shouldBeInstanceOf<BlockRequest.TableOfContents>()
                    tocBlock.tableOfContents.color shouldBe Color.BLUE
                }
            }

            describe("equation blocks") {
                it("should create equation block with LaTeX expression") {
                    val content =
                        pageContent {
                            equation("E = mc^2")
                        }

                    content shouldHaveSize 1
                    val equationBlock = content[0].shouldBeInstanceOf<BlockRequest.Equation>()
                    equationBlock.equation.expression shouldBe "E = mc^2"
                }
            }

            describe("synced blocks") {
                it("should create original synced block") {
                    val content =
                        pageContent {
                            syncedBlock {
                                paragraph("Original content")
                            }
                        }

                    content shouldHaveSize 1
                    val syncedBlock = content[0].shouldBeInstanceOf<BlockRequest.SyncedBlock>()
                    syncedBlock.syncedBlock.syncedFrom.shouldBeNull()
                    syncedBlock.syncedBlock.children!! shouldHaveSize 1

                    val paragraphBlock = syncedBlock.syncedBlock.children[0].shouldBeInstanceOf<BlockRequest.Paragraph>()
                    paragraphBlock.paragraph.richText[0].plainText shouldBe "Original content"
                }

                it("should create synced block reference") {
                    val content =
                        pageContent {
                            syncedBlockReference("12345678-1234-1234-1234-123456789abc")
                        }

                    content shouldHaveSize 1
                    val syncedBlock = content[0].shouldBeInstanceOf<BlockRequest.SyncedBlock>()
                    syncedBlock.syncedBlock.syncedFrom.shouldNotBeNull()
                    syncedBlock.syncedBlock.syncedFrom.blockId shouldBe "12345678-1234-1234-1234-123456789abc"
                    syncedBlock.syncedBlock.children.shouldBeNull()
                }
            }

            describe("mixed content with new block types") {
                it("should create content with various new block types") {
                    val content =
                        pageContent {
                            heading1("New Block Types Demo")
                            divider()

                            bookmark("https://example.com", caption = "Example bookmark")
                            embed("https://youtube.com/watch?v=example")

                            columnList {
                                column {
                                    paragraph("Left side")
                                    equation("x^2 + y^2 = z^2")
                                }
                                column {
                                    paragraph("Right side")
                                    breadcrumb()
                                }
                            }

                            tableOfContents(color = Color.GRAY)

                            syncedBlock {
                                paragraph("Original synced content")
                            }

                            childPage("Child Page")
                            childDatabase("Child Database")
                        }

                    content shouldHaveSize 9

                    // Verify the order and types
                    content[0].shouldBeInstanceOf<BlockRequest.Heading1>()
                    content[1].shouldBeInstanceOf<BlockRequest.Divider>()
                    content[2].shouldBeInstanceOf<BlockRequest.Bookmark>()
                    content[3].shouldBeInstanceOf<BlockRequest.Embed>()
                    content[4].shouldBeInstanceOf<BlockRequest.ColumnList>()
                    content[5].shouldBeInstanceOf<BlockRequest.TableOfContents>()
                    content[6].shouldBeInstanceOf<BlockRequest.SyncedBlock>()
                    content[7].shouldBeInstanceOf<BlockRequest.ChildPage>()
                    content[8].shouldBeInstanceOf<BlockRequest.ChildDatabase>()
                }
            }
        }
    })
