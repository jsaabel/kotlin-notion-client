package integration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.saabelit.kotlinnotionclient.NotionClient
import no.saabelit.kotlinnotionclient.config.NotionApiLimits
import no.saabelit.kotlinnotionclient.config.NotionConfig
import no.saabelit.kotlinnotionclient.models.base.Annotations
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.base.SelectOptionColor
import no.saabelit.kotlinnotionclient.models.base.TextContent
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.models.blocks.BulletedListItemRequestContent
import no.saabelit.kotlinnotionclient.models.blocks.Heading1RequestContent
import no.saabelit.kotlinnotionclient.models.blocks.ParagraphRequestContent
import no.saabelit.kotlinnotionclient.models.blocks.QuoteRequestContent
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PageProperty
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.validation.ValidationException

/**
 * Real integration tests for validation logic that make actual API calls to Notion.
 *
 * These tests verify that our validation system works in real-world scenarios by:
 * - Making actual HTTP requests to the Notion API
 * - Testing validation with real API responses and error conditions
 * - Verifying that validation prevents actual API errors
 * - Testing end-to-end workflows with real data limits
 * - Ensuring our validation logic matches Notion's actual API behavior
 *
 * Prerequisites:
 * - NOTION_API_TOKEN environment variable with a valid Notion API token
 * - NOTION_TEST_PAGE_ID environment variable with a test page ID for creating children
 */
class ValidationIntegrationTest :
    FunSpec({

        if (!integrationTestEnvVarsAreSet()) {
            xtest("(Skipped)") {
                println("Skipping ValidationIntegrationTest due to missing environment variables")
            }
        } else {
            val apiToken = System.getenv("NOTION_API_TOKEN")
            val testPageId = System.getenv("NOTION_TEST_PAGE_ID")

            val client = NotionClient.create(NotionConfig(apiToken = apiToken))

            // Helper functions for creating test data
            fun createLongRichText(length: Int = 2100): RichText {
                val content = "a".repeat(length)
                return RichText(
                    type = "text",
                    text = TextContent(content = content),
                    annotations = Annotations(),
                    plainText = content,
                    href = null,
                )
            }

            fun createNormalRichText(content: String = "Normal text"): RichText =
                RichText(
                    type = "text",
                    text = TextContent(content = content),
                    annotations = Annotations(),
                    plainText = content,
                    href = null,
                )

            context("Real API Validation - Array Limits") {
                test("should fail fast on too many multi-select options") {
                    val tooManyOptions =
                        (1..101).map {
                            no.saabelit.kotlinnotionclient.models.pages.SelectOption(
                                id = "option-$it",
                                name = "Option $it",
                                color = SelectOptionColor.DEFAULT,
                            )
                        }
                    val request =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = testPageId),
                            properties =
                                mapOf(
                                    "title" to PagePropertyValue.TitleValue(title = listOf(createNormalRichText("Test Page"))),
                                    "multiSelect" to PagePropertyValue.MultiSelectValue(multiSelect = tooManyOptions),
                                ),
                        )

                    // This should fail with ValidationException BEFORE making the API call
                    val exception =
                        shouldThrow<ValidationException> {
                            client.pages.create(request)
                        }
                    exception.message.shouldContain("too many options")
                    exception.message.shouldContain("100") // Should mention the limit
                }

                test("should fail fast on too many blocks") {
                    val tooManyBlocks =
                        (1..101).map {
                            BlockRequest.Paragraph(
                                paragraph =
                                    ParagraphRequestContent(
                                        richText = listOf(createNormalRichText("Block $it")),
                                    ),
                            )
                        }

                    // This should fail with ValidationException BEFORE making the API call
                    val exception =
                        shouldThrow<ValidationException> {
                            client.blocks.appendChildren(testPageId, tooManyBlocks)
                        }
                    exception.message.shouldContain("too large")
                    exception.message.shouldContain("100") // Should mention the limit
                }

                test("should fail fast on long content within blocks") {
                    val blockWithLongContent =
                        listOf(
                            BlockRequest.Paragraph(
                                paragraph =
                                    ParagraphRequestContent(
                                        richText = listOf(createLongRichText(2500)),
                                    ),
                            ),
                        )

                    // This should fail with ValidationException BEFORE making the API call
                    val exception =
                        shouldThrow<ValidationException> {
                            client.blocks.appendChildren(testPageId, blockWithLongContent)
                        }
                    exception.message.shouldContain("too long")
                    exception.message.shouldContain("2000") // Should mention the limit
                }
            }

            context("Real API Validation - Performance") {
                test("should validate without significant performance impact") {
                    val startTime = System.currentTimeMillis()

                    // Create a normal page (should be fast with validation)
                    val request =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = testPageId),
                            properties =
                                mapOf(
                                    "title" to PagePropertyValue.TitleValue(title = listOf(createNormalRichText("Performance Test Page"))),
                                ),
                        )

                    val result = client.pages.create(request)
                    val endTime = System.currentTimeMillis()

                    result.shouldNotBe(null)

                    // Validation should add minimal overhead (< 100ms for simple requests)
                    val totalTime = endTime - startTime
                    println("Page creation with validation took: ${totalTime}ms")
                    // Note: We don't assert on time as network latency varies, but log for monitoring
                }
            }

            context("Real API Validation - Edge Cases") {
                test("should handle edge case of exactly 2000 character content") {
                    val exactLimitText = createLongRichText(NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH)
                    val request =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = testPageId),
                            properties =
                                mapOf(
                                    "title" to PagePropertyValue.TitleValue(title = listOf(exactLimitText)),
                                ),
                        )

                    // This should succeed without truncation (exactly at limit)
                    val result = client.pages.create(request)
                    result.shouldNotBe(null)

                    // Content should be unchanged
                    val titleProperty = result.properties["title"] as? PageProperty.Title
                    titleProperty?.title?.firstOrNull()?.plainText?.let { actualTitle ->
                        actualTitle.length.shouldBe(NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH)
                        actualTitle.shouldNotContain("...") // Should NOT contain truncation suffix
                    }
                }

                test("should handle normal requests without modification") {
                    val normalRequest =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = testPageId),
                            properties =
                                mapOf(
                                    "title" to PagePropertyValue.TitleValue(title = listOf(createNormalRichText("Normal Page Title"))),
                                ),
                        )

                    // This should succeed without any validation modifications
                    val result = client.pages.create(normalRequest)
                    result.shouldNotBe(null)

                    val titleProperty = result.properties["title"] as? PageProperty.Title
                    titleProperty
                        ?.title
                        ?.firstOrNull()
                        ?.plainText
                        .shouldBe("Normal Page Title")
                }
            }

            context("Real API Validation - Rich Text in Database Pages") {

                context("Real API Validation - Page Content Blocks") {
                    test("should handle rich text content in paragraph blocks") {
                        // Create a simple page first
                        val pageRequest =
                            CreatePageRequest(
                                parent = Parent(type = "page_id", pageId = testPageId),
                                properties =
                                    mapOf(
                                        "title" to PagePropertyValue.TitleValue(title = listOf(createNormalRichText("Block Content Test"))),
                                    ),
                            )
                        val page = client.pages.create(pageRequest)

                        // Try to add blocks with normal content (should succeed)
                        val normalBlocks =
                            listOf(
                                BlockRequest.Paragraph(
                                    paragraph =
                                        ParagraphRequestContent(
                                            richText = listOf(createNormalRichText("This is normal paragraph content.")),
                                        ),
                                ),
                                BlockRequest.Paragraph(
                                    paragraph =
                                        ParagraphRequestContent(
                                            richText = listOf(createNormalRichText("This is another paragraph with normal length.")),
                                        ),
                                ),
                            )

                        // This should succeed - no validation violations
                        val result = client.blocks.appendChildren(page.id, normalBlocks)
                        result.shouldNotBe(null)
                        result.results.size.shouldBe(2)
                    }

                    test("should fail fast on long content within paragraph blocks") {
                        // Create a simple page first
                        val pageRequest =
                            CreatePageRequest(
                                parent = Parent(type = "page_id", pageId = testPageId),
                                properties =
                                    mapOf(
                                        "title" to
                                            PagePropertyValue.TitleValue(title = listOf(createNormalRichText("Block Validation Test"))),
                                    ),
                            )
                        val page = client.pages.create(pageRequest)

                        // Try to add blocks with long content (should fail)
                        val longContentBlocks =
                            listOf(
                                BlockRequest.Paragraph(
                                    paragraph =
                                        ParagraphRequestContent(
                                            richText = listOf(createLongRichText(2500)), // Exceeds limit
                                        ),
                                ),
                            )

                        // This should fail with ValidationException BEFORE making the API call
                        val exception =
                            shouldThrow<ValidationException> {
                                client.blocks.appendChildren(page.id, longContentBlocks)
                            }
                        exception.message.shouldContain("too long")
                        exception.message.shouldContain("2000") // Should mention the limit
                    }

                    test("should validate mixed block types with rich text content") {
                        // Create a simple page first
                        val pageRequest =
                            CreatePageRequest(
                                parent = Parent(type = "page_id", pageId = testPageId),
                                properties =
                                    mapOf(
                                        "title" to
                                            PagePropertyValue.TitleValue(title = listOf(createNormalRichText("Mixed Block Types Test"))),
                                    ),
                            )
                        val page = client.pages.create(pageRequest)

                        // Create a mix of different block types with normal content
                        val mixedBlocks =
                            listOf(
                                BlockRequest.Paragraph(
                                    paragraph =
                                        ParagraphRequestContent(
                                            richText = listOf(createNormalRichText("This is a paragraph.")),
                                        ),
                                ),
                                BlockRequest.Heading1(
                                    heading1 =
                                        Heading1RequestContent(
                                            richText = listOf(createNormalRichText("This is a heading")),
                                        ),
                                ),
                                BlockRequest.BulletedListItem(
                                    bulletedListItem =
                                        BulletedListItemRequestContent(
                                            richText = listOf(createNormalRichText("This is a bullet point")),
                                        ),
                                ),
                                BlockRequest.Quote(
                                    quote =
                                        QuoteRequestContent(
                                            richText = listOf(createNormalRichText("This is a quote")),
                                        ),
                                ),
                            )

                        // This should succeed - all content is within limits
                        val result = client.blocks.appendChildren(page.id, mixedBlocks)
                        result.shouldNotBe(null)
                        result.results.size.shouldBe(4)
                    }
                }

                context("Real API Validation - Edge Cases and Complex Scenarios") {
                    test("CONFIRMED: per-segment limit allows splitting long text") {
                        // Create a database with rich text property first
                        val database =
                            client.databases.create {
                                parent.page(testPageId)
                                title("Per-Segment Limit Test Database")
                                properties {
                                    title("Name")
                                    richText("Content")
                                }
                            }

                        // Get the data source from the created database (2025-09-03 API)
                        val retrievedDb = client.databases.retrieve(database.id)
                        val dataSourceId = retrievedDb.dataSources.first().id

                        // Test: Multiple segments where each < 2000 chars but total > 2000 chars
                        val segment1 = createLongRichText(1800) // Under limit
                        val segment2 = createLongRichText(1800) // Under limit
                        val segment3 = createNormalRichText("Final segment") // Small
                        // Total: 1800 + 1800 + 13 = 3613 characters - well over the 2000 limit

                        val multipleSegments = listOf(segment1, segment2, segment3)

                        val pageRequest =
                            CreatePageRequest(
                                parent = Parent(type = "data_source_id", dataSourceId = dataSourceId),
                                properties =
                                    mapOf(
                                        "Name" to PagePropertyValue.TitleValue(title = listOf(createNormalRichText("Test Entry"))),
                                        "Content" to PagePropertyValue.RichTextValue(richText = multipleSegments),
                                    ),
                            )

                        // If our theory is correct, this should SUCCEED because each segment < 2000 chars
                        val result = client.pages.create(pageRequest)
                        result.shouldNotBe(null)

                        // Verify all segments are preserved
                        val retrievedPage = client.pages.retrieve(result.id)
                        val contentProperty = retrievedPage.properties["Content"] as? PageProperty.RichTextProperty
                        contentProperty.shouldNotBe(null)

                        val richTextSegments = contentProperty?.richText ?: emptyList()
                        richTextSegments.size.shouldBe(3) // All 3 segments should be preserved

                        // Calculate total length - should be > 2000 if per-segment limit applies
                        val totalContent = richTextSegments.joinToString("") { it.plainText }
                        val totalLength = totalContent.length

                        println("Total length of all segments: $totalLength characters")
                        println("Segment 1 length: ${richTextSegments[0].plainText.length}")
                        println("Segment 2 length: ${richTextSegments[1].plainText.length}")
                        println("Segment 3 length: ${richTextSegments[2].plainText.length}")

                        // If per-segment limit theory is correct:
                        // - Total length should be > 2000 (proving it's not a total limit)
                        // - Each individual segment should be ≤ 2000
                        totalLength shouldBeGreaterThan NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH
                        richTextSegments[0].plainText.length shouldBeLessThanOrEqualTo NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH
                        richTextSegments[1].plainText.length shouldBeLessThanOrEqualTo NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH
                        richTextSegments[2].plainText.length shouldBeLessThanOrEqualTo NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH
                    }
                }
            }
        }
    })
