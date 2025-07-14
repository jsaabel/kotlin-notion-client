package no.saabelit.kotlinnotionclient.validation

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

@Tags("Unit")
class ValidationModelsTest :
    FunSpec({

        context("ValidationConfig") {
            test("should provide default configuration") {
                val config = ValidationConfig.default()

                config.autoSplitLongText.shouldBeTrue()
            }

            test("should provide configuration with auto-split") {
                val config = ValidationConfig.withAutoSplit()

                config.autoSplitLongText.shouldBeTrue()
            }

            test("should provide configuration without auto-split") {
                val config = ValidationConfig.withoutAutoSplit()

                config.autoSplitLongText.shouldBeFalse()
            }

            test("should allow custom configuration") {
                val config =
                    ValidationConfig(
                        autoSplitLongText = false,
                    )

                config.autoSplitLongText.shouldBeFalse()
            }
        }

        context("ValidationResult") {
            test("should report valid result for no violations") {
                val result = ValidationResult(emptyList())

                result.isValid.shouldBeTrue()
                result.hasErrors.shouldBeFalse()
                result.hasWarnings.shouldBeFalse()
                result.violations shouldHaveSize 0
            }

            test("should report invalid result for violations") {
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

                result.isValid.shouldBeFalse()
                result.hasErrors.shouldBeTrue()
                result.hasWarnings.shouldBeFalse()
                result.violations shouldHaveSize 1
            }

            test("should distinguish between errors and warnings") {
                val violations =
                    listOf(
                        ValidationViolation(
                            field = "field1",
                            violationType = ViolationType.CONTENT_TOO_LONG, // Error
                            message = "Content too long",
                            autoFixAvailable = true,
                        ),
                        ValidationViolation(
                            field = "field2",
                            violationType = ViolationType.CONTENT_NEAR_LIMIT, // Warning
                            message = "Content near limit",
                            autoFixAvailable = false,
                        ),
                    )
                val result = ValidationResult(violations)

                result.isValid.shouldBeFalse()
                result.hasErrors.shouldBeTrue()
                result.hasWarnings.shouldBeTrue()
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
                        ValidationViolation(
                            field = "field3",
                            violationType = ViolationType.CONTENT_TOO_LONG,
                            message = "Another content too long",
                            autoFixAvailable = true,
                        ),
                    )
                val result = ValidationResult(violations)

                val contentViolations = result.getViolations(ViolationType.CONTENT_TOO_LONG)
                contentViolations shouldHaveSize 2

                val arrayViolations = result.getViolations(ViolationType.ARRAY_TOO_LARGE)
                arrayViolations shouldHaveSize 1

                val nonExistentViolations = result.getViolations(ViolationType.INVALID_URL)
                nonExistentViolations shouldHaveSize 0
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

                val nonExistentFieldViolations = result.getViolationsForField("field3")
                nonExistentFieldViolations shouldHaveSize 0
            }

            test("should generate summary for valid result") {
                val result = ValidationResult(emptyList())
                val summary = result.getSummary()

                summary shouldBe "No validation violations found"
            }

            test("should generate detailed summary for violations") {
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
                            violationType = ViolationType.CONTENT_NEAR_LIMIT,
                            message = "Content near limit",
                            autoFixAvailable = false,
                        ),
                    )
                val result = ValidationResult(violations)
                val summary = result.getSummary()

                summary shouldContain "Validation Summary:"
                summary shouldContain "Errors: 1"
                summary shouldContain "Warnings: 1"
                summary shouldContain "CONTENT_TOO_LONG: Content too long"
                summary shouldContain "CONTENT_NEAR_LIMIT: Content near limit"
            }
        }

        context("ValidationViolation") {
            test("should create basic violation") {
                val violation =
                    ValidationViolation(
                        field = "test.field",
                        violationType = ViolationType.CONTENT_TOO_LONG,
                        message = "Test message",
                        autoFixAvailable = true,
                    )

                violation.field shouldBe "test.field"
                violation.violationType shouldBe ViolationType.CONTENT_TOO_LONG
                violation.message shouldBe "Test message"
                violation.autoFixAvailable.shouldBeTrue()
                violation.currentValue shouldBe null
                violation.limit shouldBe null
                violation.suggestedAction shouldBe null
            }

            test("should create detailed violation with values and limits") {
                val violation =
                    ValidationViolation(
                        field = "content",
                        violationType = ViolationType.CONTENT_TOO_LONG,
                        message = "Content exceeds limit",
                        currentValue = 2100,
                        limit = 2000,
                        autoFixAvailable = true,
                        suggestedAction = "Split content into multiple segments",
                    )

                violation.currentValue shouldBe 2100
                violation.limit shouldBe 2000
                violation.suggestedAction shouldBe "Split content into multiple segments"
            }

            test("should generate detailed message without values") {
                val violation =
                    ValidationViolation(
                        field = "test.field",
                        violationType = ViolationType.CONTENT_TOO_LONG,
                        message = "Test message",
                        autoFixAvailable = true,
                    )

                val detailedMessage = violation.getDetailedMessage()
                detailedMessage shouldBe "Test message - Auto-fix available"
            }

            test("should generate detailed message with values and limits") {
                val violation =
                    ValidationViolation(
                        field = "content",
                        violationType = ViolationType.CONTENT_TOO_LONG,
                        message = "Content exceeds limit",
                        currentValue = 2100,
                        limit = 2000,
                        autoFixAvailable = true,
                        suggestedAction = "Split content",
                    )

                // TODO: Critically re-examine this approach / pattern... do we need all those strings?
                val detailedMessage = violation.getDetailedMessage()
                detailedMessage shouldContain "Content exceeds limit"
                detailedMessage shouldContain "(current: 2100, limit: 2000)"
                detailedMessage shouldContain "Auto-fix available"
                detailedMessage shouldContain "Suggested: Split content"
            }

            test("should generate detailed message without auto-fix") {
                val violation =
                    ValidationViolation(
                        field = "url",
                        violationType = ViolationType.INVALID_URL,
                        message = "URL format invalid",
                        autoFixAvailable = false,
                        suggestedAction = "Manually correct the URL format",
                    )

                val detailedMessage = violation.getDetailedMessage()
                detailedMessage shouldContain "URL format invalid"
                detailedMessage shouldNotContain "Auto-fix available"
                detailedMessage shouldContain "Suggested: Manually correct the URL format"
            }
        }

        context("ViolationType") {
            test("should correctly categorize error types") {
                ViolationType.CONTENT_TOO_LONG.isError.shouldBeTrue()
                ViolationType.CONTENT_TOO_LONG.isWarning.shouldBeFalse()

                ViolationType.ARRAY_TOO_LARGE.isError.shouldBeTrue()
                ViolationType.ARRAY_TOO_LARGE.isWarning.shouldBeFalse()

                ViolationType.PAYLOAD_TOO_LARGE.isError.shouldBeTrue()
                ViolationType.PAYLOAD_TOO_LARGE.isWarning.shouldBeFalse()

                ViolationType.INVALID_URL.isError.shouldBeTrue()
                ViolationType.INVALID_URL.isWarning.shouldBeFalse()

                ViolationType.INVALID_EMAIL.isError.shouldBeTrue()
                ViolationType.INVALID_EMAIL.isWarning.shouldBeFalse()

                ViolationType.INVALID_PHONE.isError.shouldBeTrue()
                ViolationType.INVALID_PHONE.isWarning.shouldBeFalse()
            }

            test("should correctly categorize warning types") {
                ViolationType.CONTENT_NEAR_LIMIT.isError.shouldBeFalse()
                ViolationType.CONTENT_NEAR_LIMIT.isWarning.shouldBeTrue()

                ViolationType.ARRAY_NEAR_LIMIT.isError.shouldBeFalse()
                ViolationType.ARRAY_NEAR_LIMIT.isWarning.shouldBeTrue()

                ViolationType.PAYLOAD_NEAR_LIMIT.isError.shouldBeFalse()
                ViolationType.PAYLOAD_NEAR_LIMIT.isWarning.shouldBeTrue()
            }
        }

        context("ValidationException") {
            test("should create exception with validation result") {
                val violations =
                    listOf(
                        ValidationViolation(
                            field = "field1",
                            violationType = ViolationType.CONTENT_TOO_LONG,
                            message = "Content too long",
                            autoFixAvailable = true,
                        ),
                    )
                val validationResult = ValidationResult(violations)
                val exception = ValidationException(validationResult)

                exception.validationResult shouldBe validationResult
                exception.message shouldContain "Request validation failed"
                exception.message shouldContain "Validation Summary:"
                exception.message shouldContain "CONTENT_TOO_LONG: Content too long"
            }

            test("should create exception with custom message") {
                val violations =
                    listOf(
                        ValidationViolation(
                            field = "field1",
                            violationType = ViolationType.CONTENT_TOO_LONG,
                            message = "Content too long",
                            autoFixAvailable = true,
                        ),
                    )
                val validationResult = ValidationResult(violations)
                val customMessage = "Custom validation error"
                val exception = ValidationException(validationResult, customMessage)

                exception.message shouldContain customMessage
                exception.message shouldContain "Validation Summary:"
            }

            test("should be instance of IllegalArgumentException") {
                val validationResult = ValidationResult(emptyList())
                val exception = ValidationException(validationResult)

                exception.shouldBeInstanceOf<IllegalArgumentException>()
            }
        }

        context("AutoFixResult") {
            test("should report fully fixed result") {
                val result =
                    AutoFixResult(
                        fixedRequest = "fixed content",
                        fixedViolations =
                            listOf(
                                ValidationViolation(
                                    field = "field1",
                                    violationType = ViolationType.CONTENT_TOO_LONG,
                                    message = "Fixed",
                                    autoFixAvailable = true,
                                ),
                            ),
                        remainingViolations = emptyList(),
                        changesSummary = listOf("Split content"),
                    )

                result.wasFullyFixed.shouldBeTrue()
                result.hasRemainingErrors.shouldBeFalse()
            }

            test("should report partially fixed result") {
                val result =
                    AutoFixResult(
                        fixedRequest = "partially fixed content",
                        fixedViolations =
                            listOf(
                                ValidationViolation(
                                    field = "field1",
                                    violationType = ViolationType.CONTENT_TOO_LONG,
                                    message = "Fixed",
                                    autoFixAvailable = true,
                                ),
                            ),
                        remainingViolations =
                            listOf(
                                ValidationViolation(
                                    field = "field2",
                                    violationType = ViolationType.INVALID_URL,
                                    message = "Cannot fix",
                                    autoFixAvailable = false,
                                ),
                            ),
                        changesSummary = listOf("Split content"),
                    )

                result.wasFullyFixed.shouldBeFalse()
                result.hasRemainingErrors.shouldBeTrue()
            }

            test("should handle warnings in remaining violations") {
                val result =
                    AutoFixResult(
                        fixedRequest = "content",
                        fixedViolations = emptyList(),
                        remainingViolations =
                            listOf(
                                ValidationViolation(
                                    field = "field1",
                                    violationType = ViolationType.CONTENT_NEAR_LIMIT, // Warning, not error
                                    message = "Near limit",
                                    autoFixAvailable = false,
                                ),
                            ),
                        changesSummary = emptyList(),
                    )

                result.wasFullyFixed.shouldBeFalse()
                result.hasRemainingErrors.shouldBeFalse() // Warnings don't count as errors
            }
        }
    })
