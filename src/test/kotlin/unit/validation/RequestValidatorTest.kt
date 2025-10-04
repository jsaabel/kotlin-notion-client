package unit.validation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import no.saabelit.kotlinnotionclient.config.NotionApiLimits
import no.saabelit.kotlinnotionclient.models.base.Annotations
import no.saabelit.kotlinnotionclient.models.base.Color
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.base.SelectOptionColor
import no.saabelit.kotlinnotionclient.models.base.TextContent
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.models.blocks.ParagraphRequestContent
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.pages.PageReference
import no.saabelit.kotlinnotionclient.models.pages.SelectOption
import no.saabelit.kotlinnotionclient.models.pages.UpdatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.UserReference
import no.saabelit.kotlinnotionclient.validation.RequestValidator
import no.saabelit.kotlinnotionclient.validation.ValidationConfig
import no.saabelit.kotlinnotionclient.validation.ValidationException
import no.saabelit.kotlinnotionclient.validation.ValidationResult
import no.saabelit.kotlinnotionclient.validation.ValidationViolation
import no.saabelit.kotlinnotionclient.validation.ViolationType

@Tags("Unit")
class RequestValidatorTest :
    FunSpec({

        val validator = RequestValidator()

        // Helper functions for creating test data
        fun createRichText(content: String = "Normal text"): RichText =
            RichText(
                type = "text",
                text = TextContent(content = content),
                annotations = Annotations(),
                plainText = content,
                href = null,
            )

        fun createLongRichText(): RichText = createRichText("a".repeat(2100)) // Exceeds 2000 char limit

        fun createLongUrlRichText(): RichText =
            RichText(
                type = "text",
                text = TextContent(content = "Link text"),
                annotations = Annotations(),
                plainText = "Link text",
                href = "https://example.com/" + "a".repeat(2000), // Exceeds URL limit
            )

        fun createLargeArray(size: Int): List<Any> = (1..size).map { "item$it" }

        fun createTestPageRequest(
            title: String = "Test Page",
            richTextContent: List<RichText> = listOf(createRichText()),
            children: List<BlockRequest>? = null,
        ): CreatePageRequest =
            CreatePageRequest(
                parent = Parent(type = "page_id", pageId = "test-parent-id"),
                properties =
                    mapOf(
                        "title" to PagePropertyValue.TitleValue(title = listOf(createRichText(title))),
                        "Description" to PagePropertyValue.RichTextValue(richText = richTextContent),
                    ),
                children = children,
            )

        fun createParagraphBlock(content: String = "Normal paragraph"): BlockRequest.Paragraph =
            BlockRequest.Paragraph(
                paragraph =
                    ParagraphRequestContent(
                        richText = listOf(createRichText(content)),
                    ),
            )

        context("Page Request Validation") {
            test("should validate normal page request without violations") {
                val request = createTestPageRequest()
                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeTrue()
                result.violations.shouldBeEmpty()
            }

            test("should detect rich text content too long in title") {
                val request =
                    createTestPageRequest(
                        title = "a".repeat(2100), // Exceeds 2000 char limit
                    )
                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
                result.violations.first().field shouldBe "title.title[0]"
                result.violations
                    .first()
                    .autoFixAvailable
                    .shouldBeTrue()
            }

            test("should detect rich text content too long in property") {
                val request =
                    createTestPageRequest(
                        richTextContent = listOf(createLongRichText()),
                    )
                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
                result.violations.first().field shouldBe "Description.richText[0]"
                result.violations
                    .first()
                    .autoFixAvailable
                    .shouldBeTrue()
            }

            test("should detect URL too long in rich text") {
                val request =
                    createTestPageRequest(
                        richTextContent = listOf(createLongUrlRichText()),
                    )
                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
                result.violations.first().field shouldBe "Description.richText[0].href"
                result.violations
                    .first()
                    .autoFixAvailable
                    .shouldBeFalse()
            }

            test("should validate multi-select property with too many options") {
                val multiSelectProperty =
                    PagePropertyValue.MultiSelectValue(
                        multiSelect =
                            createLargeArray(150).map {
                                SelectOption(
                                    id = it.toString(),
                                    name = it.toString(),
                                    color = SelectOptionColor.DEFAULT,
                                )
                            },
                    )
                val request =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties = mapOf("MultiSelect" to multiSelectProperty),
                    )

                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.ARRAY_TOO_LARGE
                result.violations.first().field shouldBe "MultiSelect.multiSelect"
                result.violations
                    .first()
                    .autoFixAvailable
                    .shouldBeTrue()
                result.violations.first().currentValue shouldBe 150
                result.violations.first().limit shouldBe NotionApiLimits.Collections.MAX_MULTI_SELECT_OPTIONS
            }

            test("should validate relation property with too many pages") {
                val relationProperty =
                    PagePropertyValue.RelationValue(
                        relation =
                            createLargeArray(150).map {
                                PageReference(id = it.toString())
                            },
                    )
                val request =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties = mapOf("Relations" to relationProperty),
                    )

                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.ARRAY_TOO_LARGE
                result.violations.first().field shouldBe "Relations.relation"
                result.violations
                    .first()
                    .autoFixAvailable
                    .shouldBeTrue()
            }

            test("should validate people property with too many users") {
                val peopleProperty =
                    PagePropertyValue.PeopleValue(
                        people =
                            createLargeArray(150).map {
                                UserReference(
                                    id = it.toString(),
                                )
                            },
                    )
                val request =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties = mapOf("People" to peopleProperty),
                    )

                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.ARRAY_TOO_LARGE
                result.violations.first().field shouldBe "People.people"
                result.violations
                    .first()
                    .autoFixAvailable
                    .shouldBeTrue()
            }

            test("should validate URL property with too long URL") {
                val urlProperty =
                    PagePropertyValue.UrlValue(
                        url = "https://example.com/" + "a".repeat(2000), // Exceeds URL limit
                    )
                val request =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties = mapOf("URL" to urlProperty),
                    )

                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
                result.violations.first().field shouldBe "URL.url"
                result.violations
                    .first()
                    .autoFixAvailable
                    .shouldBeFalse()
            }

            test("should validate email property with too long email") {
                val emailProperty =
                    PagePropertyValue.EmailValue(
                        email = "test@" + "a".repeat(200) + ".com", // Exceeds email limit
                    )
                val request =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties = mapOf("Email" to emailProperty),
                    )

                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
                result.violations.first().field shouldBe "Email.email"
                result.violations
                    .first()
                    .autoFixAvailable
                    .shouldBeFalse()
            }

            test("should validate phone property with too long phone number") {
                val phoneProperty =
                    PagePropertyValue.PhoneNumberValue(
                        phoneNumber = "+1" + "1".repeat(200), // Exceeds phone limit
                    )
                val request =
                    CreatePageRequest(
                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
                        properties = mapOf("Phone" to phoneProperty),
                    )

                val result = validator.validatePageRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
                result.violations.first().field shouldBe "Phone.phoneNumber"
                result.violations
                    .first()
                    .autoFixAvailable
                    .shouldBeFalse()
            }
        }

        context("Page Update Request Validation") {
            test("should validate normal page update request without violations") {
                val request =
                    UpdatePageRequest(
                        properties =
                            mapOf(
                                "Description" to
                                    PagePropertyValue.RichTextValue(
                                        richText = listOf(createRichText("Updated description")),
                                    ),
                            ),
                    )
                val result = validator.validatePageUpdateRequest(request)

                result.isValid.shouldBeTrue()
                result.violations.shouldBeEmpty()
            }

            test("should detect violations in page update request") {
                val request =
                    UpdatePageRequest(
                        properties =
                            mapOf(
                                "Description" to
                                    PagePropertyValue.RichTextValue(
                                        richText = listOf(createLongRichText()),
                                    ),
                            ),
                    )
                val result = validator.validatePageUpdateRequest(request)

                result.isValid.shouldBeFalse()
                result.violations shouldHaveSize 1
                result.violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
            }

            test("should handle null properties in update request") {
                val request = UpdatePageRequest(properties = null)
                val result = validator.validatePageUpdateRequest(request)

                result.isValid.shouldBeTrue()
                result.violations.shouldBeEmpty()
            }
        }

        context("Block Array Validation") {
            test("should validate normal block array without violations") {
                val blocks =
                    listOf(
                        createParagraphBlock("First paragraph"),
                        createParagraphBlock("Second paragraph"),
                    )
                val violations = validator.validateBlockArray("children", blocks)

                violations.shouldBeEmpty()
            }

            test("should detect array too large violation") {
                val blocks = (1..150).map { createParagraphBlock("Paragraph $it") }
                val violations = validator.validateBlockArray("children", blocks)

                violations shouldHaveSize 1
                violations.first().violationType shouldBe ViolationType.ARRAY_TOO_LARGE
                violations.first().field shouldBe "children"
                violations.first().autoFixAvailable.shouldBeTrue()
                violations.first().currentValue shouldBe 150
                violations.first().limit shouldBe NotionApiLimits.Collections.MAX_ARRAY_ELEMENTS
            }

            test("should detect content violations within blocks") {
                val blocks =
                    listOf(
                        createParagraphBlock("a".repeat(2100)), // Exceeds rich text limit
                    )
                val violations = validator.validateBlockArray("children", blocks)

                violations shouldHaveSize 1
                violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
                violations.first().field shouldBe "children[0].paragraph.richText[0]"
                violations.first().autoFixAvailable.shouldBeTrue()
            }

            test("should detect multiple violations in block array") {
                val blocks =
                    (1..150).map {
                        createParagraphBlock(
                            if (it == 1) "a".repeat(2100) else "Normal content $it",
                        )
                    }
                val violations = validator.validateBlockArray("children", blocks)

                violations shouldHaveSize 2 // Array too large + content too long
                violations.any { it.violationType == ViolationType.ARRAY_TOO_LARGE }.shouldBeTrue()
                violations.any { it.violationType == ViolationType.CONTENT_TOO_LONG }.shouldBeTrue()
            }
        }

//          TODO: Adjust/replace following API version update
//        context("Database Request Validation") {
//            test("should validate normal database request without violations") {
//                val request =
//                    CreateDatabaseRequest(
//                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
//                        title = listOf(createRichText("Test Database")),
//                        properties = emptyMap(),
//                    )
//                val result = validator.validateDatabaseRequest(request)
//
//                result.isValid.shouldBeTrue()
//                result.violations.shouldBeEmpty()
//            }
//
//            test("should detect violations in database title") {
//                val request =
//                    CreateDatabaseRequest(
//                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
//                        title = listOf(createLongRichText()),
//                        properties = emptyMap(),
//                    )
//                val result = validator.validateDatabaseRequest(request)
//
//                result.isValid.shouldBeFalse()
//                result.violations shouldHaveSize 1
//                result.violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
//                result.violations.first().field shouldBe "title[0]"
//            }
//
//            test("should detect violations in database description") {
//                val request =
//                    CreateDatabaseRequest(
//                        parent = Parent(type = "page_id", pageId = "test-parent-id"),
//                        title = listOf(createRichText("Test Database")),
//                        properties = emptyMap(),
//                        description = listOf(createLongRichText()),
//                    )
//                val result = validator.validateDatabaseRequest(request)
//
//                result.isValid.shouldBeFalse()
//                result.violations shouldHaveSize 1
//                result.violations.first().violationType shouldBe ViolationType.CONTENT_TOO_LONG
//                result.violations.first().field shouldBe "description[0]"
//            }
//        }

        context("ValidationResult") {
            test("should provide correct validation status") {
                val validResult = ValidationResult(emptyList())
                validResult.isValid.shouldBeTrue()
                validResult.hasErrors.shouldBeFalse()
                validResult.hasWarnings.shouldBeFalse()

                val invalidResult =
                    ValidationResult(
                        listOf(
                            ValidationViolation(
                                field = "test",
                                violationType = ViolationType.CONTENT_TOO_LONG,
                                message = "Test violation",
                                autoFixAvailable = true,
                            ),
                        ),
                    )
                invalidResult.isValid.shouldBeFalse()
                invalidResult.hasErrors.shouldBeTrue()
            }

            test("should filter violations by type") {
                val violations =
                    listOf(
                        ValidationViolation(
                            field = "field1",
                            violationType = ViolationType.CONTENT_TOO_LONG,
                            message = "Content too long",
                            autoFixAvailable = true,
                        ),
                        ValidationViolation(
                            field = "field2",
                            violationType = ViolationType.ARRAY_TOO_LARGE,
                            message = "Array too large",
                            autoFixAvailable = true,
                        ),
                    )
                val result = ValidationResult(violations)

                val contentViolations = result.getViolations(ViolationType.CONTENT_TOO_LONG)
                contentViolations shouldHaveSize 1
                contentViolations.first().field shouldBe "field1"

                val arrayViolations = result.getViolations(ViolationType.ARRAY_TOO_LARGE)
                arrayViolations shouldHaveSize 1
                arrayViolations.first().field shouldBe "field2"
            }

            test("should filter violations by field") {
                val violations =
                    listOf(
                        ValidationViolation(
                            field = "field1",
                            violationType = ViolationType.CONTENT_TOO_LONG,
                            message = "Violation 1",
                            autoFixAvailable = true,
                        ),
                        ValidationViolation(
                            field = "field1",
                            violationType = ViolationType.ARRAY_TOO_LARGE,
                            message = "Violation 2",
                            autoFixAvailable = true,
                        ),
                        ValidationViolation(
                            field = "field2",
                            violationType = ViolationType.CONTENT_TOO_LONG,
                            message = "Violation 3",
                            autoFixAvailable = true,
                        ),
                    )
                val result = ValidationResult(violations)

                val field1Violations = result.getViolationsForField("field1")
                field1Violations shouldHaveSize 2

                val field2Violations = result.getViolationsForField("field2")
                field2Violations shouldHaveSize 1
            }

            test("should generate proper summary") {
                val violations =
                    listOf(
                        ValidationViolation(
                            field = "field1",
                            violationType = ViolationType.CONTENT_TOO_LONG,
                            message = "Content too long",
                            autoFixAvailable = true,
                        ),
                    )
                val result = ValidationResult(violations)

                val summary = result.getSummary()
                summary.contains("Validation Summary:").shouldBeTrue()
                summary.contains("Errors: 1").shouldBeTrue()
                summary.contains("CONTENT_TOO_LONG: Content too long").shouldBeTrue()
            }
        }

        context("ValidateOrFix Methods") {
            context("Page Request Auto-Fixing") {
                test("should auto-split long text content when enabled") {
                    val config = ValidationConfig(autoSplitLongText = true)
                    val autoSplitValidator = RequestValidator(config)

                    val longContent = createLongRichText() // 2100 chars
                    val request =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = "test-page-id"),
                            properties =
                                mapOf(
                                    "title" to PagePropertyValue.TitleValue(title = listOf(createRichText("My Page Title"))),
                                    "description" to PagePropertyValue.RichTextValue(richText = listOf(longContent)),
                                ),
                        )

                    val result = autoSplitValidator.validateOrFix(request)

                    // Title should remain unchanged
                    val title = (result.properties["title"] as PagePropertyValue.TitleValue).title
                    title.size.shouldBe(1)
                    title[0].text?.content.shouldBe("My Page Title")

                    // Rich text content should be split into multiple segments
                    val fixedContent = (result.properties["description"] as PagePropertyValue.RichTextValue).richText

                    // Should be split into 2 segments
                    fixedContent.size.shouldBe(2)

                    // First segment should be under limit
                    val firstSegment = fixedContent[0]
                    val firstContent = firstSegment.text?.content ?: ""
                    firstContent.length.shouldBeLessThanOrEqualTo(NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH)

                    // Second segment should contain remaining content
                    val secondSegment = fixedContent[1]
                    val secondContent = secondSegment.text?.content ?: ""
                    secondContent.length.shouldBeGreaterThan(0)

                    // Total content should be preserved
                    val totalContent = firstContent + secondContent
                    totalContent.shouldBe("a".repeat(2100))

                    // Formatting should be preserved in both segments
                    firstSegment.annotations.shouldBe(longContent.annotations)
                    secondSegment.annotations.shouldBe(longContent.annotations)
                }

                test("should fail fast when auto-split is disabled") {
                    val noAutoSplitValidator = RequestValidator(ValidationConfig(autoSplitLongText = false))

                    val longContent = createLongRichText()
                    val request =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = "test-page-id"),
                            properties =
                                mapOf(
                                    "title" to PagePropertyValue.TitleValue(title = listOf(createRichText("My Page Title"))),
                                    "content" to PagePropertyValue.RichTextValue(richText = listOf(longContent)),
                                ),
                        )

                    shouldThrow<ValidationException> {
                        noAutoSplitValidator.validateOrFix(request)
                    }
                }

                test("should fail fast on non-text violations regardless of config") {
                    val autoSplitValidator = RequestValidator(ValidationConfig(autoSplitLongText = true))

                    val tooManyOptions =
                        (1..101).map {
                            SelectOption(
                                id = "option-$it",
                                name = "Option$it",
                                color = SelectOptionColor.DEFAULT,
                            )
                        }
                    val request =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = "test-page-id"),
                            properties =
                                mapOf(
                                    "multiSelect" to PagePropertyValue.MultiSelectValue(multiSelect = tooManyOptions),
                                ),
                        )

                    shouldThrow<ValidationException> {
                        autoSplitValidator.validateOrFix(request)
                    }
                }

                test("should preserve formatting when splitting text") {
                    val config = ValidationConfig(autoSplitLongText = true)
                    val autoSplitValidator = RequestValidator(config)

                    // Create long rich text with formatting
                    val longFormattedText =
                        RichText(
                            type = "text",
                            text = TextContent(content = "a".repeat(2100)),
                            annotations =
                                Annotations(
                                    bold = true,
                                    italic = true,
                                    color = Color.RED,
                                ),
                            plainText = "a".repeat(2100),
                            href = "https://example.com",
                        )

                    val request =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = "test-page-id"),
                            properties =
                                mapOf(
                                    "title" to PagePropertyValue.TitleValue(title = listOf(createRichText("My Page Title"))),
                                    "notes" to PagePropertyValue.RichTextValue(richText = listOf(longFormattedText)),
                                ),
                        )

                    val result = autoSplitValidator.validateOrFix(request)

                    val fixedContent = (result.properties["notes"] as PagePropertyValue.RichTextValue).richText

                    // All segments should preserve formatting
                    fixedContent.forEach { segment ->
                        segment.annotations.bold.shouldBe(true)
                        segment.annotations.italic.shouldBe(true)
                        segment.annotations.color.shouldBe(Color.RED)
                        segment.href.shouldBe("https://example.com")
                    }
                }
            }

            context("Page Update Request Auto-Fixing") {
                test("should auto-split text in update requests") {
                    val config = ValidationConfig(autoSplitLongText = true)
                    val autoSplitValidator = RequestValidator(config)

                    val longRichText = createLongRichText()
                    val request =
                        UpdatePageRequest(
                            properties =
                                mapOf(
                                    "description" to PagePropertyValue.RichTextValue(richText = listOf(longRichText)),
                                ),
                        )

                    val result = autoSplitValidator.validateOrFix(request)

                    val fixedDescription = (result.properties!!["description"] as PagePropertyValue.RichTextValue).richText

                    // Should be split into 2 segments
                    fixedDescription.size.shouldBe(2)

                    // Each segment should be under limit
                    fixedDescription.forEach { segment ->
                        val content = segment.text?.content ?: ""
                        content.length.shouldBeLessThanOrEqualTo(NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH)
                    }

                    // Total content should be preserved
                    val totalContent = fixedDescription.joinToString("") { it.text?.content ?: "" }
                    totalContent.shouldBe("a".repeat(2100))
                }

                test("should handle null properties in update request") {
                    val validator = RequestValidator()
                    val request = UpdatePageRequest(properties = null)

                    val result = validator.validateOrFix(request)
                    result.properties.shouldBe(null)
                }
            }

            // TODO: Adjust/replace following API version update
//            context("Database Request Auto-Fixing") {
//                test("should auto-split text in database title") {
//                    val config = ValidationConfig(autoSplitLongText = true)
//                    val autoSplitValidator = RequestValidator(config)
//
//                    val longTitle = createLongRichText()
//                    val request =
//                        CreateDatabaseRequest(
//                            parent = Parent(type = "page_id", pageId = "test-page-id"),
//                            title = listOf(longTitle),
//                            properties = mapOf(),
//                        )
//
//                    val result = autoSplitValidator.validateOrFix(request)
//
//                    // Should be split into 2 segments
//                    result.title.size.shouldBe(2)
//
//                    // Each segment should be under limit
//                    result.title.forEach { segment ->
//                        val content = segment.text?.content ?: ""
//                        content.length.shouldBeLessThanOrEqualTo(NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH)
//                    }
//
//                    // Total content should be preserved
//                    val totalContent = result.title.joinToString("") { it.text?.content ?: "" }
//                    totalContent.shouldBe("a".repeat(2100))
//                }
//
//                test("should auto-split text in database description") {
//                    val config = ValidationConfig(autoSplitLongText = true)
//                    val autoSplitValidator = RequestValidator(config)
//
//                    val longDescription = createLongRichText()
//                    val request =
//                        CreateDatabaseRequest(
//                            parent = Parent(type = "page_id", pageId = "test-page-id"),
//                            title = listOf(createRichText("Normal title")),
//                            properties = mapOf(),
//                            description = listOf(longDescription),
//                        )
//
//                    val result = autoSplitValidator.validateOrFix(request)
//
//                    // Should be split into 2 segments
//                    result.description!!.size.shouldBe(2)
//
//                    // Each segment should be under limit
//                    result.description.forEach { segment ->
//                        val content = segment.text?.content ?: ""
//                        content.length.shouldBeLessThanOrEqualTo(NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH)
//                    }
//
//                    // Total content should be preserved
//                    val totalContent = result.description.joinToString("") { it.text?.content ?: "" }
//                    totalContent.shouldBe("a".repeat(2100))
//                }
//            }

            context("Block Array Validation") {
                test("should fail fast on block array violations") {
                    val validator = RequestValidator()

                    val tooManyBlocks =
                        (1..101).map {
                            BlockRequest.Paragraph(
                                paragraph =
                                    ParagraphRequestContent(
                                        richText = listOf(createRichText("Block $it")),
                                    ),
                            )
                        }

                    shouldThrow<ValidationException> {
                        validator.validateOrThrow("children", tooManyBlocks)
                    }
                }

                test("should allow valid block arrays") {
                    val validator = RequestValidator()

                    val validBlocks =
                        listOf(
                            BlockRequest.Paragraph(
                                paragraph =
                                    ParagraphRequestContent(
                                        richText = listOf(createRichText("Valid block")),
                                    ),
                            ),
                        )

                    // Should not throw
                    validator.validateOrThrow("children", validBlocks)
                }

                test("should fail fast on content violations within blocks") {
                    val validator = RequestValidator()

                    val blockWithLongContent =
                        listOf(
                            BlockRequest.Paragraph(
                                paragraph =
                                    ParagraphRequestContent(
                                        richText = listOf(createLongRichText()),
                                    ),
                            ),
                        )

                    shouldThrow<ValidationException> {
                        validator.validateOrThrow("children", blockWithLongContent)
                    }
                }
            }

            context("Return Type Validation") {
                test("should return same request when no violations exist") {
                    val validator = RequestValidator()

                    val request =
                        CreatePageRequest(
                            parent = Parent(type = "page_id", pageId = "test-page-id"),
                            properties =
                                mapOf(
                                    "title" to PagePropertyValue.TitleValue(title = listOf(createRichText("Valid title"))),
                                ),
                        )

                    val result = validator.validateOrFix(request)
                    result.shouldBe(request) // Should be the exact same object
                }
            }
        }
    })
