package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.base.Mention
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.comments.CommentAttachmentRequest
import it.saabel.kotlinnotionclient.models.comments.CommentDisplayNameType
import it.saabel.kotlinnotionclient.models.comments.createCommentRequest

/**
 * Comprehensive unit tests for CreateCommentRequestBuilder DSL.
 *
 * These tests validate the DSL functionality without requiring API calls,
 * focusing on correct object construction and validation logic.
 */
@Tags("Unit")
class CreateCommentRequestBuilderTest :
    DescribeSpec({

        describe("CreateCommentRequestBuilder DSL") {

            describe("basic construction") {
                it("should create a minimal comment request with page parent") {
                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            content {
                                text("Hello world")
                            }
                        }

                    request.parent shouldBe
                        Parent(
                            type = "page_id",
                            pageId = "test-page-id",
                        )
                    request.richText shouldHaveSize 1
                    request.richText[0].plainText shouldBe "Hello world"
                    request.discussionId.shouldBeNull()
                    request.attachments.shouldBeNull()
                    request.displayName.shouldBeNull()
                }

                it("should create a minimal comment request with block parent") {
                    val request =
                        createCommentRequest {
                            parent.blockId("test-block-id")
                            content {
                                text("Comment on block")
                            }
                        }

                    request.parent shouldBe
                        Parent(
                            type = "block_id",
                            blockId = "test-block-id",
                        )
                    request.richText shouldHaveSize 1
                    request.richText[0].plainText shouldBe "Comment on block"
                }
            }

            describe("parent configuration") {
                it("should support page parent configuration") {
                    val request =
                        createCommentRequest {
                            parent.pageId("12345678-1234-1234-1234-123456789abc")
                            content {
                                text("Test")
                            }
                        }

                    request.parent.type shouldBe "page_id"
                    request.parent.pageId shouldBe "12345678-1234-1234-1234-123456789abc"
                    request.parent.blockId.shouldBeNull()
                }

                it("should support block parent configuration") {
                    val request =
                        createCommentRequest {
                            parent.blockId("87654321-4321-4321-4321-210987654321")
                            content {
                                text("Test")
                            }
                        }

                    request.parent.type shouldBe "block_id"
                    request.parent.blockId shouldBe "87654321-4321-4321-4321-210987654321"
                    request.parent.pageId.shouldBeNull()
                }

                it("should overwrite parent if set multiple times") {
                    val request =
                        createCommentRequest {
                            parent.pageId("page-id")
                            parent.blockId("block-id") // This should overwrite the page
                            content {
                                text("Test")
                            }
                        }

                    request.parent.type shouldBe "block_id"
                    request.parent.blockId shouldBe "block-id"
                    request.parent.pageId.shouldBeNull()
                }

                it("should support page() alias for pageId()") {
                    val request =
                        createCommentRequest {
                            parent.page("page-id-123")
                            content {
                                text("Test using page alias")
                            }
                        }

                    request.parent.type shouldBe "page_id"
                    request.parent.pageId shouldBe "page-id-123"
                }

                it("should support block() alias for blockId()") {
                    val request =
                        createCommentRequest {
                            parent.block("block-id-456")
                            content {
                                text("Test using block alias")
                            }
                        }

                    request.parent.type shouldBe "block_id"
                    request.parent.blockId shouldBe "block-id-456"
                }
            }

            describe("rich text content") {
                it("should support simple text content") {
                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            content {
                                text("Simple comment text")
                            }
                        }

                    request.richText shouldHaveSize 1
                    request.richText[0].plainText shouldBe "Simple comment text"
                    request.richText[0].annotations.bold shouldBe false
                    request.richText[0].annotations.italic shouldBe false
                }

                it("should support richText() alias for content()") {
                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            richText {
                                text("Using richText alias")
                                bold("formatted text")
                            }
                        }

                    request.richText shouldHaveSize 2
                    request.richText[0].plainText shouldBe "Using richText alias"
                    request.richText[1].plainText shouldBe "formatted text"
                    request.richText[1].annotations.bold shouldBe true
                }

                it("should support formatted text content") {
                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            content {
                                text("This comment has ")
                                bold("bold text")
                                text(" and ")
                                italic("italic text")
                                text("!")
                            }
                        }

                    request.richText shouldHaveSize 5
                    request.richText[0].plainText shouldBe "This comment has "
                    request.richText[1].plainText shouldBe "bold text"
                    request.richText[1].annotations.bold shouldBe true
                    request.richText[2].plainText shouldBe " and "
                    request.richText[3].plainText shouldBe "italic text"
                    request.richText[3].annotations.italic shouldBe true
                    request.richText[4].plainText shouldBe "!"
                }

                it("should support complex rich text with links and mentions") {
                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            content {
                                text("Check out ")
                                link("https://notion.so", "Notion")
                                text(" and contact ")
                                userMention("user-id-123")
                            }
                        }

                    request.richText shouldHaveSize 4
                    request.richText[0].plainText shouldBe "Check out "
                    request.richText[1].plainText shouldBe "Notion"
                    request.richText[1].href shouldBe "https://notion.so"
                    request.richText[2].plainText shouldBe " and contact "
                    request.richText[3].type shouldBe "mention"
                }

                it("should support all types of mentions") {
                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            content {
                                text("Mentions: ")
                                userMention("user-123")
                                text(", ")
                                pageMention("page-456")
                                text(", ")
                                databaseMention("db-789")
                                text(", and ")
                                dateMention("2023-12-25")
                            }
                        }

                    request.richText shouldHaveSize 8

                    // User mention
                    request.richText[1].type shouldBe "mention"
                    (request.richText[1].mention is Mention.User) shouldBe true

                    // Page mention
                    request.richText[3].type shouldBe "mention"
                    (request.richText[3].mention is Mention.Page) shouldBe true

                    // Database mention
                    request.richText[5].type shouldBe "mention"
                    (request.richText[5].mention is Mention.Database) shouldBe true

                    // Date mention
                    request.richText[7].type shouldBe "mention"
                    (request.richText[7].mention is Mention.Date) shouldBe true
                }
            }

            describe("optional properties") {
                it("should support discussion ID") {
                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            content {
                                text("Reply to discussion")
                            }
                            discussionId("discussion-123")
                        }

                    request.discussionId shouldBe "discussion-123"
                }

                it("should support custom display name") {
                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            content {
                                text("Comment from bot")
                            }
                            displayName("Custom Bot Name")
                        }

                    request.displayName.shouldNotBeNull()
                    request.displayName!!.type shouldBe CommentDisplayNameType.CUSTOM
                    request.displayName!!.custom.shouldNotBeNull()
                    request.displayName!!.custom!!.name shouldBe "Custom Bot Name"
                }

                it("should support single attachment") {
                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            content {
                                text("Comment with attachment")
                            }
                            attachment("file-upload-123")
                        }

                    request.attachments.shouldNotBeNull()
                    request.attachments!! shouldHaveSize 1
                    request.attachments!![0].fileUploadId shouldBe "file-upload-123"
                    request.attachments!![0].type shouldBe "file_upload"
                }

                it("should support multiple attachments") {
                    val attachments =
                        listOf(
                            CommentAttachmentRequest("file1"),
                            CommentAttachmentRequest("file2"),
                            CommentAttachmentRequest("file3"),
                        )

                    val request =
                        createCommentRequest {
                            parent.pageId("test-page-id")
                            content {
                                text("Comment with multiple attachments")
                            }
                            attachments(attachments)
                        }

                    request.attachments.shouldNotBeNull()
                    request.attachments!! shouldHaveSize 3
                    request.attachments!![0].fileUploadId shouldBe "file1"
                    request.attachments!![1].fileUploadId shouldBe "file2"
                    request.attachments!![2].fileUploadId shouldBe "file3"
                }
            }

            describe("validation") {
                it("should require parent to be specified") {
                    val exception =
                        shouldThrow<IllegalStateException> {
                            createCommentRequest {
                                content {
                                    text("Comment without parent")
                                }
                            }
                        }

                    exception.message shouldContain "Parent must be specified"
                }

                it("should require content to be specified") {
                    val exception =
                        shouldThrow<IllegalStateException> {
                            createCommentRequest {
                                parent.pageId("test-page-id")
                            }
                        }

                    exception.message shouldContain "Comment content cannot be empty"
                }

                it("should require content to be non-empty") {
                    val exception =
                        shouldThrow<IllegalStateException> {
                            createCommentRequest {
                                parent.pageId("test-page-id")
                                content {
                                    // Empty content block
                                }
                            }
                        }

                    exception.message shouldContain "Comment content cannot be empty"
                }

                it("should validate attachment limit for attachments() method") {
                    val tooManyAttachments =
                        listOf(
                            CommentAttachmentRequest("file1"),
                            CommentAttachmentRequest("file2"),
                            CommentAttachmentRequest("file3"),
                            CommentAttachmentRequest("file4"), // Too many!
                        )

                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            createCommentRequest {
                                parent.pageId("test-page-id")
                                content {
                                    text("Test")
                                }
                                attachments(tooManyAttachments)
                            }
                        }

                    exception.message shouldContain "Comments can have a maximum of 3 attachments"
                    exception.message shouldContain "but 4 were provided"
                }

                it("should validate attachment limit for attachment() method") {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            createCommentRequest {
                                parent.pageId("test-page-id")
                                content {
                                    text("Test")
                                }
                                attachment("file1")
                                attachment("file2")
                                attachment("file3")
                                attachment("file4") // This should fail
                            }
                        }

                    exception.message shouldContain "Comments can have a maximum of 3 attachments"
                }
            }

            describe("comprehensive scenarios") {
                it("should create complex comment with all features") {
                    val request =
                        createCommentRequest {
                            parent.blockId("block-123")
                            content {
                                text("This is a comprehensive comment with ")
                                bold("bold")
                                text(", ")
                                italic("italic")
                                text(", and ")
                                code("code")
                                text(" formatting. Visit ")
                                link("https://notion.so", "Notion")
                                text(" for more info!")
                            }
                            discussionId("discussion-456")
                            displayName("Advanced Bot")
                            attachment("file-upload-789")
                        }

                    // Verify parent
                    request.parent.type shouldBe "block_id"
                    request.parent.blockId shouldBe "block-123"

                    // Verify rich text content
                    request.richText shouldHaveSize 9
                    request.richText[1].annotations.bold shouldBe true
                    request.richText[3].annotations.italic shouldBe true
                    request.richText[5].annotations.code shouldBe true
                    request.richText[7].href shouldBe "https://notion.so"

                    // Verify optional properties
                    request.discussionId shouldBe "discussion-456"
                    request.displayName!!.custom!!.name shouldBe "Advanced Bot"
                    request.attachments!! shouldHaveSize 1
                    request.attachments!![0].fileUploadId shouldBe "file-upload-789"
                }

                it("should allow building multiple requests independently") {
                    val request1 =
                        createCommentRequest {
                            parent.pageId("page-1")
                            content {
                                text("First comment")
                            }
                        }

                    val request2 =
                        createCommentRequest {
                            parent.blockId("block-2")
                            content {
                                bold("Second comment")
                            }
                            discussionId("discussion-2")
                        }

                    // Verify they're independent
                    request1.parent.pageId shouldBe "page-1"
                    request1.parent.blockId.shouldBeNull()
                    request1.discussionId.shouldBeNull()

                    request2.parent.blockId shouldBe "block-2"
                    request2.parent.pageId.shouldBeNull()
                    request2.discussionId shouldBe "discussion-2"
                    request2.richText[0].annotations.bold shouldBe true
                }
            }
        }
    })
