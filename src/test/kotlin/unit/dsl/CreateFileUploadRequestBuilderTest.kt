package unit.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import it.saabel.kotlinnotionclient.models.files.CreateFileUploadRequestBuilder
import it.saabel.kotlinnotionclient.models.files.FileUploadMode
import it.saabel.kotlinnotionclient.models.files.createFileUploadRequest

/**
 * Unit tests for the CreateFileUploadRequestBuilder DSL.
 *
 * Tests all DSL functionality including validation, builder methods,
 * and error handling for different upload modes.
 */
@Tags("Unit")
class CreateFileUploadRequestBuilderTest :
    DescribeSpec({

        describe("CreateFileUploadRequestBuilder DSL") {

            describe("Single-Part Upload DSL") {

                it("should create single-part request with filename and content type") {
                    val request =
                        createFileUploadRequest {
                            filename("document.pdf")
                            contentType("application/pdf")
                        }

                    request.mode shouldBe FileUploadMode.SINGLE_PART
                    request.filename shouldBe "document.pdf"
                    request.contentType shouldBe "application/pdf"
                    request.numberOfParts shouldBe null
                    request.externalUrl shouldBe null
                }

                it("should create single-part request with just filename (default mode)") {
                    val request =
                        createFileUploadRequest {
                            filename("simple.txt")
                        }

                    request.mode shouldBe FileUploadMode.SINGLE_PART
                    request.filename shouldBe "simple.txt"
                    request.contentType shouldBe null
                }

                it("should create single-part request using singlePart() method") {
                    val request =
                        createFileUploadRequest {
                            singlePart()
                            filename("image.jpg")
                            contentType("image/jpeg")
                        }

                    request.mode shouldBe FileUploadMode.SINGLE_PART
                    request.filename shouldBe "image.jpg"
                    request.contentType shouldBe "image/jpeg"
                }

                it("should create minimal single-part request with no filename") {
                    val request =
                        createFileUploadRequest {
                            contentType("text/plain")
                        }

                    request.mode shouldBe FileUploadMode.SINGLE_PART
                    request.filename shouldBe null
                    request.contentType shouldBe "text/plain"
                }

                it("should reject numberOfParts for single-part upload") {
                    shouldThrow<IllegalStateException> {
                        createFileUploadRequest {
                            singlePart()
                            filename("file.txt")
                            numberOfParts(2)
                        }
                    } shouldHaveMessage "Number of parts should not be specified for single-part uploads"
                }

                it("should reject externalUrl for single-part upload") {
                    shouldThrow<IllegalStateException> {
                        createFileUploadRequest {
                            singlePart()
                            filename("file.txt")
                            externalUrl("https://example.com/file.txt")
                        }
                    } shouldHaveMessage "External URL should not be specified for single-part uploads"
                }
            }

            describe("Multi-Part Upload DSL") {

                it("should create multi-part request with all required fields") {
                    val request =
                        createFileUploadRequest {
                            multiPart()
                            filename("large-video.mp4")
                            contentType("video/mp4")
                            numberOfParts(5)
                        }

                    request.mode shouldBe FileUploadMode.MULTI_PART
                    request.filename shouldBe "large-video.mp4"
                    request.contentType shouldBe "video/mp4"
                    request.numberOfParts shouldBe 5
                    request.externalUrl shouldBe null
                }

                it("should create multi-part request using mode() method") {
                    val request =
                        createFileUploadRequest {
                            mode(FileUploadMode.MULTI_PART)
                            filename("archive.zip")
                            contentType("application/zip")
                            numberOfParts(10)
                        }

                    request.mode shouldBe FileUploadMode.MULTI_PART
                    request.filename shouldBe "archive.zip"
                    request.numberOfParts shouldBe 10
                }

                it("should require filename for multi-part upload") {
                    shouldThrow<IllegalStateException> {
                        createFileUploadRequest {
                            multiPart()
                            contentType("video/mp4")
                            numberOfParts(3)
                        }
                    } shouldHaveMessage "Filename is required for multi-part uploads"
                }

                it("should require numberOfParts for multi-part upload") {
                    shouldThrow<IllegalStateException> {
                        createFileUploadRequest {
                            multiPart()
                            filename("video.mp4")
                            contentType("video/mp4")
                        }
                    } shouldHaveMessage "Number of parts is required for multi-part uploads"
                }

                it("should validate numberOfParts range - too low") {
                    shouldThrow<IllegalArgumentException> {
                        createFileUploadRequest {
                            multiPart()
                            filename("file.txt")
                            numberOfParts(0)
                        }
                    } shouldHaveMessage "Number of parts must be between 1 and 1,000, but was 0"
                }

                it("should validate numberOfParts range - too high") {
                    shouldThrow<IllegalArgumentException> {
                        createFileUploadRequest {
                            multiPart()
                            filename("file.txt")
                            numberOfParts(1001)
                        }
                    } shouldHaveMessage "Number of parts must be between 1 and 1,000, but was 1001"
                }

                it("should accept valid numberOfParts at boundaries") {
                    val request1 =
                        createFileUploadRequest {
                            multiPart()
                            filename("file1.txt")
                            numberOfParts(1)
                        }
                    request1.numberOfParts shouldBe 1

                    val request2 =
                        createFileUploadRequest {
                            multiPart()
                            filename("file2.txt")
                            numberOfParts(1000)
                        }
                    request2.numberOfParts shouldBe 1000
                }

                it("should reject externalUrl for multi-part upload") {
                    shouldThrow<IllegalStateException> {
                        createFileUploadRequest {
                            multiPart()
                            filename("file.txt")
                            numberOfParts(2)
                            externalUrl("https://example.com/file.txt")
                        }
                    } shouldHaveMessage "External URL should not be specified for multi-part uploads"
                }
            }

            describe("External URL Upload DSL") {

                it("should create external URL request with all required fields") {
                    val request =
                        createFileUploadRequest {
                            filename("remote-image.jpg")
                            contentType("image/jpeg")
                            externalUrl("https://example.com/image.jpg") // Sets both mode and URL
                        }

                    request.mode shouldBe FileUploadMode.EXTERNAL_URL
                    request.filename shouldBe "remote-image.jpg"
                    request.contentType shouldBe "image/jpeg"
                    request.externalUrl shouldBe "https://example.com/image.jpg"
                    request.numberOfParts shouldBe null
                }

                it("should create external URL request using mode() method") {
                    val request =
                        createFileUploadRequest {
                            filename("document.pdf")
                            externalUrl("https://files.example.com/document.pdf") // Sets mode automatically
                        }

                    request.mode shouldBe FileUploadMode.EXTERNAL_URL
                    request.filename shouldBe "document.pdf"
                    request.externalUrl shouldBe "https://files.example.com/document.pdf"
                }

                it("should require filename for external URL upload") {
                    shouldThrow<IllegalStateException> {
                        createFileUploadRequest {
                            mode(FileUploadMode.EXTERNAL_URL)
                            contentType("image/png")
                            // Missing filename - should fail for external mode
                        }
                    } shouldHaveMessage "Filename is required for external URL uploads"
                }

                it("should require externalUrl for external URL upload") {
                    shouldThrow<IllegalStateException> {
                        createFileUploadRequest {
                            filename("image.png")
                            contentType("image/png")
                            // Missing external URL - should fail for external mode
                            mode(FileUploadMode.EXTERNAL_URL)
                        }
                    } shouldHaveMessage "External URL is required for external URL uploads"
                }

                it("should validate HTTPS requirement for external URL") {
                    shouldThrow<IllegalArgumentException> {
                        createFileUploadRequest {
                            filename("file.txt")
                            externalUrl("http://example.com/file.txt") // HTTP should fail
                        }
                    } shouldHaveMessage "External URL must be HTTPS, but was: http://example.com/file.txt"
                }

                it("should validate HTTPS requirement during build") {
                    shouldThrow<IllegalArgumentException> {
                        createFileUploadRequest {
                            filename("file.txt")
                            externalUrl("ftp://example.com/file.txt") // FTP should fail
                        }
                    } shouldHaveMessage "External URL must be HTTPS, but was: ftp://example.com/file.txt"
                }
            }

            describe("DSL Method Variations") {

                it("should support different ways to set modes") {
                    // Direct mode setting
                    val request1 =
                        createFileUploadRequest {
                            mode(FileUploadMode.SINGLE_PART)
                            filename("file1.txt")
                        }
                    request1.mode shouldBe FileUploadMode.SINGLE_PART

                    // Helper methods
                    val request2 =
                        createFileUploadRequest {
                            singlePart()
                            filename("file2.txt")
                        }
                    request2.mode shouldBe FileUploadMode.SINGLE_PART

                    val request3 =
                        createFileUploadRequest {
                            multiPart()
                            filename("file3.txt")
                            numberOfParts(2)
                        }
                    request3.mode shouldBe FileUploadMode.MULTI_PART
                }

                it("should handle method chaining") {
                    val request =
                        createFileUploadRequest {
                            multiPart()
                                .also { filename("chained.zip") }
                                .also { contentType("application/zip") }
                                .also { numberOfParts(3) }
                        }

                    request.mode shouldBe FileUploadMode.MULTI_PART
                    request.filename shouldBe "chained.zip"
                    request.contentType shouldBe "application/zip"
                    request.numberOfParts shouldBe 3
                }
            }

            describe("Builder State Management") {

                it("should start with default single-part mode") {
                    val builder = CreateFileUploadRequestBuilder()
                    val request =
                        builder
                            .apply {
                                filename("default-mode.txt")
                            }.build()

                    request.mode shouldBe FileUploadMode.SINGLE_PART
                }

                it("should allow mode changes") {
                    val request =
                        createFileUploadRequest {
                            multiPart() // First set to multi-part
                            singlePart() // Then change to single-part
                            filename("changed-mode.txt")
                        }

                    request.mode shouldBe FileUploadMode.SINGLE_PART
                }

                it("should preserve all set values") {
                    val request =
                        createFileUploadRequest {
                            filename("test.pdf")
                            contentType("application/pdf")
                            multiPart()
                            numberOfParts(2)
                        }

                    request.filename shouldBe "test.pdf"
                    request.contentType shouldBe "application/pdf"
                    request.mode shouldBe FileUploadMode.MULTI_PART
                    request.numberOfParts shouldBe 2
                }
            }

            describe("Edge Cases") {

                it("should handle empty DSL block") {
                    val request = createFileUploadRequest { }

                    request.mode shouldBe FileUploadMode.SINGLE_PART
                    request.filename shouldBe null
                    request.contentType shouldBe null
                    request.numberOfParts shouldBe null
                    request.externalUrl shouldBe null
                }

                it("should handle special characters in filename") {
                    val request =
                        createFileUploadRequest {
                            filename("file with spaces & symbols!.txt")
                            contentType("text/plain")
                        }

                    request.filename shouldBe "file with spaces & symbols!.txt"
                }

                it("should handle Unicode characters in filename") {
                    val request =
                        createFileUploadRequest {
                            filename("文档.pdf")
                            contentType("application/pdf")
                        }

                    request.filename shouldBe "文档.pdf"
                }

                it("should handle complex external URLs") {
                    val complexUrl = "https://cdn.example.com/files/user123/documents/report.pdf?version=2&token=abc123"

                    val request =
                        createFileUploadRequest {
                            filename("complex-url-file.pdf")
                            externalUrl(complexUrl) // Sets both mode and URL
                        }

                    request.externalUrl shouldBe complexUrl
                }
            }
        }
    })
