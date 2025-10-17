package unit.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import it.saabel.kotlinnotionclient.api.CommentsApi
import it.saabel.kotlinnotionclient.config.NotionConfig
import it.saabel.kotlinnotionclient.exceptions.NotionException
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.comments.Comment
import it.saabel.kotlinnotionclient.models.comments.CommentAttachmentCategory
import it.saabel.kotlinnotionclient.models.comments.CommentAttachmentRequest
import it.saabel.kotlinnotionclient.models.comments.CommentDisplayNameRequest
import it.saabel.kotlinnotionclient.models.comments.CommentDisplayNameType
import it.saabel.kotlinnotionclient.models.comments.CommentList
import it.saabel.kotlinnotionclient.models.comments.CreateCommentRequest
import it.saabel.kotlinnotionclient.models.requests.RequestBuilders
import unit.util.TestFixtures
import unit.util.mockClient

/**
 * Unit tests for the CommentsApi class.
 *
 * These tests verify that the CommentsApi correctly handles API requests
 * and responses for comment operations.
 */
@Tags("Unit")
class CommentsApiTest :
    FunSpec({
        lateinit var api: CommentsApi
        lateinit var config: NotionConfig

        beforeTest {
            config = NotionConfig(apiToken = "test-token")
        }

        context("Comment model serialization") {
            test("should deserialize comment response correctly") {
                val jsonString = TestFixtures.Comments.createCommentAsString()
                val comment = TestFixtures.json.decodeFromString<Comment>(jsonString)

                comment.id shouldBe "b52b8ed6-e029-4707-a671-832549c09de3"
                comment.discussionId shouldBe "f1407351-36f5-4c49-a13c-49f8ba11776d"
                comment.richText.size shouldBe 1
                comment.richText[0].plainText shouldBe "Hello world"
                comment.attachments shouldNotBe null
                comment.attachments?.size shouldBe 1
                comment.attachments?.get(0)?.category shouldBe CommentAttachmentCategory.IMAGE
                comment.displayName shouldNotBe null
                comment.displayName?.type shouldBe CommentDisplayNameType.INTEGRATION
                comment.displayName?.resolvedName shouldBe "Public Integration"
            }

            test("should deserialize comment list response correctly") {
                val jsonString = TestFixtures.Comments.retrieveCommentsAsString()
                val commentList = TestFixtures.json.decodeFromString<CommentList>(jsonString)

                commentList.objectType shouldBe "list"
                commentList.results.size shouldBe 1
                commentList.hasMore shouldBe false
                commentList.nextCursor shouldBe null

                val comment = commentList.results[0]
                comment.id shouldBe "94cc56ab-9f02-409d-9f99-1037e9fe502f"
                comment.richText[0].plainText shouldBe "Single comment"
            }

            test("should serialize create comment request correctly") {
                val request =
                    CreateCommentRequest(
                        parent = Parent.PageParent(pageId = "test-page-id"),
                        richText = listOf(RequestBuilders.createSimpleRichText("Test comment")),
                        attachments =
                            listOf(
                                CommentAttachmentRequest(
                                    fileUploadId = "file-upload-id",
                                    type = "file_upload",
                                ),
                            ),
                        displayName =
                            CommentDisplayNameRequest(
                                type = CommentDisplayNameType.INTEGRATION,
                            ),
                    )

                val json = TestFixtures.json.encodeToString(CreateCommentRequest.serializer(), request)

                // Verify it can be serialized without errors
                json shouldNotBe null
                json.contains("page_id") shouldBe true
                json.contains("Test comment") shouldBe true
                json.contains("file-upload-id") shouldBe true
                json.contains("integration") shouldBe true
            }

            test("should serialize create comment request with user mentions correctly") {
                val request =
                    CreateCommentRequest(
                        parent = Parent.PageParent(pageId = "test-page-id"),
                        richText =
                            listOf(
                                RequestBuilders.createSimpleRichText("Hello "),
                                RequestBuilders.createUserMention("test-user-id", "Test User"),
                                RequestBuilders.createSimpleRichText("!"),
                            ),
                    )

                val json = TestFixtures.json.encodeToString(CreateCommentRequest.serializer(), request)

                // Verify serialization includes mention
                json shouldNotBe null
                json.contains("page_id") shouldBe true
                json.contains("Hello") shouldBe true
                json.contains("mention") shouldBe true
                json.contains("test-user-id") shouldBe true
                json.contains("@Test User") shouldBe true
            }
        }

        context("retrieve comments") {
            test("should retrieve comments successfully") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/comments?block_id=test-block-id",
                            responseBody = TestFixtures.Comments.retrieveCommentsAsString(),
                        )
                    }

                api = CommentsApi(client, config)
                val comments = api.retrieve("test-block-id")

                comments.shouldBeInstanceOf<List<Comment>>()
                comments.size shouldBe 1
                comments[0].id shouldBe "94cc56ab-9f02-409d-9f99-1037e9fe502f"
            }

            test("should handle automatic pagination when retrieving comments") {
                val client =
                    mockClient {
                        // First page
                        addJsonResponse(
                            method = HttpMethod.Get,
                            path = "/v1/comments?block_id=test-block-id&page_size=100",
                            responseBody = TestFixtures.Comments.retrieveCommentsAsString(),
                        )
                    }

                api = CommentsApi(client, config)
                val comments = api.retrieve("test-block-id")

                comments.shouldBeInstanceOf<List<Comment>>()
                comments.size shouldBe 1
            }

            test("should handle API error when retrieving comments") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Get,
                            urlPattern = "*/v1/comments*",
                            statusCode = HttpStatusCode.NotFound,
                        )
                    }

                api = CommentsApi(client, config)

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.retrieve("invalid-id")
                    }

                exception.code shouldBe "404"
                exception.status shouldBe 404
            }
        }

        context("create comment") {
            test("should create comment successfully") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Post,
                            path = "/v1/comments",
                            responseBody = TestFixtures.Comments.createCommentAsString(),
                        )
                    }

                api = CommentsApi(client, config)

                val request =
                    CreateCommentRequest(
                        parent = Parent.PageParent(pageId = "test-page-id"),
                        richText = listOf(RequestBuilders.createSimpleRichText("Hello world")),
                    )

                val result = api.create(request)

                result.shouldBeInstanceOf<Comment>()
                result.id shouldBe "b52b8ed6-e029-4707-a671-832549c09de3"
                result.richText[0].plainText shouldBe "Hello world"
            }

            test("should create comment with attachments") {
                val client =
                    mockClient {
                        addJsonResponse(
                            method = HttpMethod.Post,
                            path = "/v1/comments",
                            responseBody = TestFixtures.Comments.createCommentAsString(),
                        )
                    }

                api = CommentsApi(client, config)

                val request =
                    CreateCommentRequest(
                        parent = Parent.BlockParent(blockId = "test-block-id"),
                        richText = listOf(RequestBuilders.createSimpleRichText("Comment with attachment")),
                        attachments =
                            listOf(
                                CommentAttachmentRequest(fileUploadId = "file-upload-id-1"),
                                CommentAttachmentRequest(fileUploadId = "file-upload-id-2"),
                            ),
                    )

                val result = api.create(request)

                result.shouldBeInstanceOf<Comment>()
                result.id shouldBe "b52b8ed6-e029-4707-a671-832549c09de3"
            }

            test("should validate attachment limit") {
                val client = mockClient { /* No responses needed for validation error */ }
                api = CommentsApi(client, config)

                val request =
                    CreateCommentRequest(
                        parent = Parent.PageParent(pageId = "test-page-id"),
                        richText = listOf(RequestBuilders.createSimpleRichText("Too many attachments")),
                        attachments =
                            listOf(
                                CommentAttachmentRequest(fileUploadId = "file1"),
                                CommentAttachmentRequest(fileUploadId = "file2"),
                                CommentAttachmentRequest(fileUploadId = "file3"),
                                CommentAttachmentRequest(fileUploadId = "file4"), // This exceeds the limit
                            ),
                    )

                val exception =
                    shouldThrow<IllegalArgumentException> {
                        api.create(request)
                    }

                exception.message shouldBe "Comments can have a maximum of 3 attachments, but 4 were provided"
            }

            test("should validate empty rich text") {
                val client = mockClient { /* No responses needed for validation error */ }
                api = CommentsApi(client, config)

                val request =
                    CreateCommentRequest(
                        parent = Parent.PageParent(pageId = "test-page-id"),
                        richText = emptyList(), // Empty rich text
                    )

                val exception =
                    shouldThrow<IllegalArgumentException> {
                        api.create(request)
                    }

                exception.message shouldBe "Comment rich text cannot be empty"
            }

            test("should handle API error when creating comment") {
                val client =
                    mockClient {
                        addErrorResponse(
                            method = HttpMethod.Post,
                            urlPattern = "*/v1/comments*",
                            statusCode = HttpStatusCode.BadRequest,
                        )
                    }

                api = CommentsApi(client, config)

                val request =
                    CreateCommentRequest(
                        parent = Parent.PageParent(pageId = "invalid-page-id"),
                        richText = listOf(RequestBuilders.createSimpleRichText("This will fail")),
                    )

                val exception =
                    shouldThrow<NotionException.ApiError> {
                        api.create(request)
                    }

                exception.code shouldBe "400"
                exception.status shouldBe 400
            }
        }
    })
