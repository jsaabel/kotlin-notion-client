package unit.config

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import it.saabel.kotlinnotionclient.config.NotionApiLimits
import it.saabel.kotlinnotionclient.models.base.Annotations
import it.saabel.kotlinnotionclient.models.base.Link
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.TextContent
import it.saabel.kotlinnotionclient.models.pages.Page
import it.saabel.kotlinnotionclient.models.pages.PageProperty
import it.saabel.kotlinnotionclient.models.pages.getProperty
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.readText

@Tags("Unit")
class NotionApiLimitsTest :
    FunSpec({

        val json = Json { ignoreUnknownKeys = true }

        // Helper to load sample data
        fun loadSamplePage(): Page {
            val samplePath = Path("reference/notion-api/sample_responses/pages/get_retrieve_a_page.json")
            val content = samplePath.readText()
            return json.decodeFromString<Page>(content)
        }

        // Helper to extract actual RichText objects from sample data
        fun extractSampleRichText(): List<RichText> {
            val samplePage = loadSamplePage()
            val descriptionProperty = samplePage.getProperty<PageProperty.RichTextProperty>("Description")
            return descriptionProperty?.richText ?: emptyList()
        }

        // Helper to create a long RichText content using actual RichText objects
        fun createLongRichTextArray(repeats: Int = 50): List<RichText> {
            val sampleRichText = extractSampleRichText()
            return (1..repeats).flatMap { sampleRichText }
        }

        // Helper to create a single RichText with long content
        fun createLongRichTextContent(): RichText {
            val sampleRichText = extractSampleRichText().first()
            val longContent = "a".repeat(2100) // Exceeds 2000 char limit

            return sampleRichText.copy(
                text = sampleRichText.text?.copy(content = longContent),
                plainText = longContent,
            )
        }

        // Helper to create RichText with long URL
        fun createLongUrlRichText(): RichText {
            val longUrl = "https://example.com/" + "a".repeat(2000) // Exceeds URL limit

            return RichText(
                type = "text",
                text =
                    TextContent(
                        content = "Click here",
                        link = Link(url = longUrl),
                    ),
                annotations = Annotations(),
                plainText = "Click here",
                href = longUrl,
            )
        }

        context("Content limits") {
            test("should match documented content character limits") {
                NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH shouldBe 2000
                NotionApiLimits.Content.MAX_URL_LENGTH shouldBe 2000
                NotionApiLimits.Content.MAX_EQUATION_LENGTH shouldBe 1000
                NotionApiLimits.Content.MAX_EMAIL_LENGTH shouldBe 200
                NotionApiLimits.Content.MAX_PHONE_LENGTH shouldBe 200
            }

            test("should validate rich text length using actual RichText objects") {
                val sampleRichText = extractSampleRichText()
                val normalText = sampleRichText.joinToString("") { it.plainText }

                // Normal sample text should be within limits
                NotionApiLimits.Utils.isRichTextTooLong(normalText).shouldBeFalse()

                // RichText with long content should exceed limits
                val longRichText = createLongRichTextContent()
                NotionApiLimits.Utils.isRichTextTooLong(longRichText.plainText).shouldBeTrue()
            }

            test("should validate URL length using actual RichText with links") {
                val samplePage = loadSamplePage()
                val photoProperty = samplePage.getProperty<PageProperty.Url>("Photo")
                val photoUrl = photoProperty?.url ?: ""

                // Normal sample URL should be within limits
                NotionApiLimits.Utils.isUrlTooLong(photoUrl).shouldBeFalse()

                // RichText with long URL should exceed limits
                val longUrlRichText = createLongUrlRichText()
                val linkUrl = longUrlRichText.text?.link?.url ?: ""
                NotionApiLimits.Utils.isUrlTooLong(linkUrl).shouldBeTrue()
            }
        }

        context("Collection limits") {
            test("should match documented collection size limits") {
                NotionApiLimits.Collections.MAX_ARRAY_ELEMENTS shouldBe 100
                NotionApiLimits.Collections.MAX_MULTI_SELECT_OPTIONS shouldBe 100
                NotionApiLimits.Collections.MAX_RELATION_PAGES shouldBe 100
                NotionApiLimits.Collections.MAX_PEOPLE_USERS shouldBe 100
            }

            test("should validate array size using actual RichText arrays") {
                val sampleRichText = extractSampleRichText()
                val recipesProperty = loadSamplePage().getProperty<PageProperty.Relation>("Recipes")
                val relationItems = recipesProperty?.relation ?: emptyList()

                // Normal sample arrays should be within limits
                NotionApiLimits.Utils.isArrayTooLarge(sampleRichText.size).shouldBeFalse()
                NotionApiLimits.Utils.isArrayTooLarge(relationItems.size).shouldBeFalse()

                // Large RichText arrays should exceed limits
                val largeRichTextArray = createLongRichTextArray()
                NotionApiLimits.Utils.isArrayTooLarge(largeRichTextArray.size).shouldBeTrue()
            }
        }

        context("Payload limits") {
            test("should match documented payload size limits") {
                NotionApiLimits.Payload.MAX_BLOCK_ELEMENTS shouldBe 1000
                NotionApiLimits.Payload.MAX_PAYLOAD_SIZE_BYTES shouldBe 500 * 1024
                NotionApiLimits.Payload.MAX_PAYLOAD_SIZE_KB shouldBe 500
            }

            test("should validate payload size") {
                val normalSize = 100 * 1024 // 100KB
                val largeSize = 600 * 1024 // 600KB

                NotionApiLimits.Utils.isPayloadTooLarge(normalSize).shouldBeFalse()
                NotionApiLimits.Utils.isPayloadTooLarge(largeSize).shouldBeTrue()
            }
        }

        context("Response limits") {
            test("should match documented response limits") {
                NotionApiLimits.Response.DEFAULT_RELATION_LIMIT shouldBe 20
                NotionApiLimits.Response.DEFAULT_PAGE_SIZE shouldBe 100
                NotionApiLimits.Response.MAX_PAGE_SIZE shouldBe 100
            }

            test("should reflect relation limit behavior from sample data") {
                val samplePage = loadSamplePage()
                val recipesProperty = samplePage.getProperty<PageProperty.Relation>("Recipes")
                val recipes = recipesProperty?.relation ?: emptyList()
                val hasMore = recipesProperty?.hasMore ?: false

                // Sample data shows 2 relations with has_more=false
                recipes.size shouldBe 2
                hasMore.shouldBeFalse()

                // But the limit tells us only 20 are returned by default
                recipes.size shouldBe 2 // This specific sample has 2
                NotionApiLimits.Response.DEFAULT_RELATION_LIMIT shouldBe 20 // But API returns max 20
            }
        }

        context("Utility functions with actual RichText") {
            test("should truncate rich text content properly using sample data") {
                val longRichText = createLongRichTextContent()
                val longText = longRichText.plainText

                val truncated = NotionApiLimits.Utils.truncateRichText(longText)

                truncated.length shouldBe NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH
                truncated.endsWith("...").shouldBeTrue()
            }

            test("should preserve short rich text content using actual RichText objects") {
                val sampleRichText = extractSampleRichText()
                val normalText = sampleRichText.joinToString("") { it.plainText }

                val result = NotionApiLimits.Utils.truncateRichText(normalText)

                result shouldBe normalText // Should be unchanged
            }

            test("should handle custom suffix in truncation") {
                val longRichText = createLongRichTextContent()
                val customSuffix = " [TRUNCATED]"

                val truncated = NotionApiLimits.Utils.truncateRichText(longRichText.plainText, customSuffix)

                truncated.length shouldBe NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH
                truncated.endsWith(customSuffix).shouldBeTrue()
            }

            test("should chunk RichText arrays properly using sample-based data") {
                val largeRichTextArray = createLongRichTextArray(34) // Creates 102 items (3 sample items * 34 repeats)

                val chunks = NotionApiLimits.Utils.chunkArray(largeRichTextArray)

                chunks shouldHaveSize 2 // Should split into chunks of 100
                chunks[0] shouldHaveSize 100
                chunks[1].size shouldBe (largeRichTextArray.size - 100)
            }

            test("should handle small RichText arrays in chunking") {
                val sampleRichText = extractSampleRichText()

                val chunks = NotionApiLimits.Utils.chunkArray(sampleRichText)

                chunks shouldHaveSize 1 // Small list stays in one chunk
                chunks[0] shouldBe sampleRichText
            }

            test("should chunk with custom size using RichText objects") {
                val largeRichTextArray = createLongRichTextArray(20) // Creates ~100 items

                val chunks = NotionApiLimits.Utils.chunkArray(largeRichTextArray, maxSize = 25)

                chunks.size shouldBe (largeRichTextArray.size + 24) / 25 // Ceiling division
                chunks.forEachIndexed { index, chunk ->
                    if (index == chunks.lastIndex) {
                        // Last chunk may be smaller
                        chunk.size shouldBe (largeRichTextArray.size % 25).takeIf { it != 0 } ?: 25
                    } else {
                        chunk.size shouldBe 25
                    }
                }
            }
        }

        context("Realistic content scenarios using sample data") {
            test("should handle actual Notion page description content") {
                val sampleRichText = extractSampleRichText()

                // Sample description from Notion API should have specific structure
                sampleRichText.size shouldBe 3 // Based on sample data
                sampleRichText.forEach { richText ->
                    richText.type shouldBe "text"
                    richText.annotations shouldNotBe null
                    richText.plainText shouldNotBe null
                }
            }

            test("should validate URL from actual sample page") {
                val samplePage = loadSamplePage()
                val photoProperty = samplePage.getProperty<PageProperty.Url>("Photo")
                val photoUrl = photoProperty?.url ?: ""
                val coverUrl = samplePage.cover?.external?.url ?: ""

                // Sample URLs should be valid and within limits
                photoUrl shouldNotBe ""
                coverUrl shouldNotBe ""
                NotionApiLimits.Utils.isUrlTooLong(photoUrl).shouldBeFalse()
                NotionApiLimits.Utils.isUrlTooLong(coverUrl).shouldBeFalse()
            }

            test("should handle multi-select and relation arrays from sample") {
                val samplePage = loadSamplePage()
                val storeProperty = samplePage.getProperty<PageProperty.MultiSelect>("Store availability")
                val recipesProperty = samplePage.getProperty<PageProperty.Relation>("Recipes")
                val multiSelect = storeProperty?.multiSelect ?: emptyList()
                val relations = recipesProperty?.relation ?: emptyList()

                // Sample arrays should be within collection limits
                NotionApiLimits.Utils.isArrayTooLarge(multiSelect.size).shouldBeFalse()
                NotionApiLimits.Utils.isArrayTooLarge(relations.size).shouldBeFalse()

                // Verify actual sample data structure
                multiSelect.size shouldBe 2 // Based on sample data
                relations.size shouldBe 2 // Based on sample data
            }
        }

        context("Error response information") {
            test("should match documented error response details") {
                NotionApiLimits.SizeLimitError.HTTP_STATUS_CODE shouldBe 400
                NotionApiLimits.SizeLimitError.ERROR_CODE shouldBe "validation_error"
            }
        }
    })
