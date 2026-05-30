package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.comments.updateCommentRequest

/**
 * Comprehensive unit tests for UpdateCommentRequestBuilder DSL.
 *
 * Mirrors CreateCommentRequestBuilderTest, focusing on the content-only update surface
 * and the runtime XOR validation between rich_text and markdown.
 */
@Tags("Unit")
class UpdateCommentRequestBuilderTest :
    DescribeSpec({

        describe("UpdateCommentRequestBuilder DSL") {

            describe("build success") {
                it("should build a request with rich text content") {
                    val request =
                        updateCommentRequest {
                            content {
                                text("Updated comment text")
                            }
                        }

                    request.richText.shouldNotBeNull() shouldHaveSize 1
                    checkNotNull(request.richText)[0].plainText shouldBe "Updated comment text"
                    request.markdown.shouldBeNull()
                }

                it("should support richText() alias for content()") {
                    val request =
                        updateCommentRequest {
                            richText {
                                text("Updated via alias")
                                bold("formatted")
                            }
                        }

                    request.richText.shouldNotBeNull() shouldHaveSize 2
                    checkNotNull(request.richText)[1].annotations.bold shouldBe true
                    request.markdown.shouldBeNull()
                }

                it("should build a request with markdown content") {
                    val request =
                        updateCommentRequest {
                            markdown("**Updated** via markdown")
                        }

                    request.markdown shouldBe "**Updated** via markdown"
                    request.richText.shouldBeNull()
                }
            }

            describe("build failure") {
                it("should reject both rich_text and markdown being set") {
                    val exception =
                        shouldThrow<IllegalStateException> {
                            updateCommentRequest {
                                content { text("hello") }
                                markdown("**hello**")
                            }
                        }

                    exception.message shouldContain "either rich_text or markdown, not both"
                }

                it("should reject neither rich_text nor markdown being set") {
                    val exception =
                        shouldThrow<IllegalStateException> {
                            updateCommentRequest {
                                // no content configured
                            }
                        }

                    exception.message shouldContain "Comment content cannot be empty"
                }

                it("should reject an empty content block") {
                    val exception =
                        shouldThrow<IllegalStateException> {
                            updateCommentRequest {
                                content {
                                    // intentionally empty
                                }
                            }
                        }

                    exception.message shouldContain "Comment content cannot be empty"
                }
            }
        }
    })
