package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.comments.retrieveCommentsRequest

/**
 * Unit tests for RetrieveCommentsRequestBuilder DSL.
 *
 * These tests validate the DSL functionality for retrieving comments,
 * focusing on correct parameter construction and validation logic.
 */
@Tags("Unit")
class RetrieveCommentsRequestBuilderTest :
    DescribeSpec({

        describe("RetrieveCommentsRequestBuilder DSL") {

            describe("basic construction") {
                it("should create a minimal retrieve request with block ID only") {
                    val (blockId, pageSize, startCursor) =
                        retrieveCommentsRequest {
                            blockId("test-block-id")
                        }

                    blockId shouldBe "test-block-id"
                    pageSize.shouldBeNull()
                    startCursor.shouldBeNull()
                }

                it("should create retrieve request with all parameters") {
                    val (blockId, pageSize, startCursor) =
                        retrieveCommentsRequest {
                            blockId("test-block-id")
                            pageSize(50)
                            startCursor("cursor-123")
                        }

                    blockId shouldBe "test-block-id"
                    pageSize shouldBe 50
                    startCursor shouldBe "cursor-123"
                }
            }

            describe("parameter configuration") {
                it("should accept valid page sizes") {
                    val (_, pageSize, _) =
                        retrieveCommentsRequest {
                            blockId("test-id")
                            pageSize(1)
                        }

                    pageSize shouldBe 1

                    val (_, pageSize100, _) =
                        retrieveCommentsRequest {
                            blockId("test-id")
                            pageSize(100)
                        }

                    pageSize100 shouldBe 100
                }

                it("should accept any cursor string") {
                    val (_, _, startCursor) =
                        retrieveCommentsRequest {
                            blockId("test-id")
                            startCursor("some-complex-cursor-string-123")
                        }

                    startCursor shouldBe "some-complex-cursor-string-123"
                }

                it("should allow overwriting parameters") {
                    val (blockId, pageSize, startCursor) =
                        retrieveCommentsRequest {
                            blockId("first-id")
                            pageSize(25)
                            startCursor("first-cursor")

                            // Override values
                            blockId("second-id")
                            pageSize(75)
                            startCursor("second-cursor")
                        }

                    blockId shouldBe "second-id"
                    pageSize shouldBe 75
                    startCursor shouldBe "second-cursor"
                }
            }

            describe("validation") {
                it("should require block ID to be specified") {
                    val exception =
                        shouldThrow<IllegalStateException> {
                            retrieveCommentsRequest {
                                pageSize(50)
                                startCursor("cursor")
                                // Missing blockId
                            }
                        }

                    exception.message shouldContain "Block ID must be specified"
                }

                it("should validate page size minimum") {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            retrieveCommentsRequest {
                                blockId("test-id")
                                pageSize(0)
                            }
                        }

                    exception.message shouldContain "Page size must be between 1 and 100"
                    exception.message shouldContain "got 0"
                }

                it("should validate page size maximum") {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            retrieveCommentsRequest {
                                blockId("test-id")
                                pageSize(101)
                            }
                        }

                    exception.message shouldContain "Page size must be between 1 and 100"
                    exception.message shouldContain "got 101"
                }

                it("should reject negative page sizes") {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            retrieveCommentsRequest {
                                blockId("test-id")
                                pageSize(-5)
                            }
                        }

                    exception.message shouldContain "Page size must be between 1 and 100"
                    exception.message shouldContain "got -5"
                }
            }

            describe("comprehensive scenarios") {
                it("should handle various block ID formats") {
                    // Test with UUID format
                    val (blockId1, _, _) =
                        retrieveCommentsRequest {
                            blockId("12345678-1234-1234-1234-123456789abc")
                        }
                    blockId1 shouldBe "12345678-1234-1234-1234-123456789abc"

                    // Test with UUID without dashes
                    val (blockId2, _, _) =
                        retrieveCommentsRequest {
                            blockId("123456781234123412341234567890ab")
                        }
                    blockId2 shouldBe "123456781234123412341234567890ab"

                    // Test with arbitrary string (API might accept other formats)
                    val (blockId3, _, _) =
                        retrieveCommentsRequest {
                            blockId("custom-block-id")
                        }
                    blockId3 shouldBe "custom-block-id"
                }

                it("should allow building multiple requests independently") {
                    val request1 =
                        retrieveCommentsRequest {
                            blockId("block-1")
                            pageSize(25)
                        }

                    val request2 =
                        retrieveCommentsRequest {
                            blockId("block-2")
                            startCursor("cursor-2")
                        }

                    // Verify they're independent
                    request1.first shouldBe "block-1"
                    request1.second shouldBe 25
                    request1.third.shouldBeNull()

                    request2.first shouldBe "block-2"
                    request2.second.shouldBeNull()
                    request2.third shouldBe "cursor-2"
                }

                it("should support common API pagination patterns") {
                    // First page request
                    val firstPage =
                        retrieveCommentsRequest {
                            blockId("target-block")
                            pageSize(20)
                        }

                    firstPage.first shouldBe "target-block"
                    firstPage.second shouldBe 20
                    firstPage.third.shouldBeNull()

                    // Subsequent page request
                    val nextPage =
                        retrieveCommentsRequest {
                            blockId("target-block")
                            pageSize(20)
                            startCursor("next-page-cursor")
                        }

                    nextPage.first shouldBe "target-block"
                    nextPage.second shouldBe 20
                    nextPage.third shouldBe "next-page-cursor"
                }
            }
        }
    })
