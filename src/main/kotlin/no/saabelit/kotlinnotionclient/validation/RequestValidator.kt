package no.saabelit.kotlinnotionclient.validation

import no.saabelit.kotlinnotionclient.config.NotionApiLimits
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.blocks.Block
import no.saabelit.kotlinnotionclient.models.blocks.BlockRequest
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseRequest
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.pages.UpdatePageRequest

/**
 * Comprehensive request validation framework for proactive API limit enforcement.
 *
 * This validator performs client-side validation before making HTTP requests to the Notion API,
 * preventing server errors and providing helpful feedback about limit violations.
 *
 * Features:
 * - Content length validation (rich text, URLs, equations)
 * - Array size validation (blocks, multi-select, relations)
 * - Payload size estimation and validation
 * - Configurable validation modes (strict vs. permissive)
 * - Auto-fixing capabilities (splitting long text into multiple segments)
 *
 * Usage:
 * ```kotlin
 * val validator = RequestValidator()
 * val result = validator.validatePageRequest(request)
 * if (!result.isValid) {
 *     // Handle validation errors or apply auto-fixes
 * }
 * ```
 */
class RequestValidator(
    private val config: ValidationConfig = ValidationConfig.default(),
) {
    /**
     * Validates a page creation request.
     */
    fun validatePageRequest(request: CreatePageRequest): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()

        // Validate properties
        request.properties.forEach { (propertyName, property) ->
            violations.addAll(validatePageProperty(propertyName, property))
        }

        // Validate children blocks if present
        request.children?.let { children ->
            violations.addAll(validateBlockArray("children", children))
        }

        return ValidationResult(violations)
    }

    /**
     * Validates a page update request.
     */
    fun validatePageUpdateRequest(request: UpdatePageRequest): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()

        // Validate properties
        request.properties?.forEach { (propertyName, property) ->
            violations.addAll(validatePageProperty(propertyName, property))
        }

        return ValidationResult(violations)
    }

    /**
     * Validates a database creation request.
     */
    fun validateDatabaseRequest(request: CreateDatabaseRequest): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()

        // Validate title
        violations.addAll(validateRichTextArray("title", request.title))

        // Validate description if present
        request.description?.let { description ->
            violations.addAll(validateRichTextArray("description", description))
        }

        return ValidationResult(violations)
    }

    // =============================================================================
    // SIMPLIFIED VALIDATION API - Single method to validate and fix if needed
    // =============================================================================

    /**
     * Validates a page creation request and automatically fixes text violations if configured.
     *
     * @param request The page creation request to validate
     * @return A valid request (potentially with auto-fixed text content)
     * @throws ValidationException if validation fails for non-fixable violations
     */
    fun validateOrFix(request: CreatePageRequest): CreatePageRequest {
        val violations = validatePageRequest(request)

        if (violations.isValid) return request

        return handleViolations(request, violations) { req, textViolations ->
            fixPageTextViolations(req, textViolations)
        }
    }

    /**
     * Validates a page update request and automatically fixes text violations if configured.
     *
     * @param request The page update request to validate
     * @return A valid request (potentially with auto-fixed text content)
     * @throws ValidationException if validation fails for non-fixable violations
     */
    fun validateOrFix(request: UpdatePageRequest): UpdatePageRequest {
        val violations = validatePageUpdateRequest(request)

        if (violations.isValid) return request

        return handleViolations(request, violations) { req, textViolations ->
            fixPageUpdateTextViolations(req, textViolations)
        }
    }

    /**
     * Validates a database creation request and automatically fixes text violations if configured.
     *
     * @param request The database creation request to validate
     * @return A valid request (potentially with auto-fixed text content)
     * @throws ValidationException if validation fails for non-fixable violations
     */
    fun validateOrFix(request: CreateDatabaseRequest): CreateDatabaseRequest {
        val violations = validateDatabaseRequest(request)

        if (violations.isValid) return request

        return handleViolations(request, violations) { req, textViolations ->
            fixDatabaseTextViolations(req, textViolations)
        }
    }

    /**
     * Validates a block array and throws if any violations exist.
     * Block arrays don't support auto-fixing (too complex), so we fail fast.
     *
     * @param fieldName The name of the field being validated
     * @param blocks The blocks to validate
     * @throws ValidationException if any violations are found
     */
    fun validateOrThrow(
        fieldName: String,
        blocks: List<BlockRequest>,
    ) {
        val violations = validateBlockArray(fieldName, blocks)

        if (violations.isNotEmpty()) {
            throw ValidationException(ValidationResult(violations))
        }
    }

    /**
     * Validates an array of blocks (for block operations).
     */
    fun validateBlockArray(
        fieldName: String,
        blocks: List<BlockRequest>,
    ): List<ValidationViolation> {
        val violations = mutableListOf<ValidationViolation>()

        // Check array size
        if (NotionApiLimits.Utils.isArrayTooLarge(blocks.size)) {
            violations.add(
                ValidationViolation(
                    field = fieldName,
                    violationType = ViolationType.ARRAY_TOO_LARGE,
                    message = "Block array too large: ${blocks.size} blocks (max: ${NotionApiLimits.Collections.MAX_ARRAY_ELEMENTS})",
                    currentValue = blocks.size,
                    limit = NotionApiLimits.Collections.MAX_ARRAY_ELEMENTS,
                    autoFixAvailable = true,
                ),
            )
        }

        // Validate content within blocks
        blocks.forEachIndexed { index, block ->
            violations.addAll(validateBlockContent("$fieldName[$index]", block))
        }

        return violations
    }

    /**
     * Validates an array of blocks for Block objects (used in page children).
     */
    fun validateBlockArrayForBlocks(
        fieldName: String,
        blocks: List<Block>,
    ): List<ValidationViolation> {
        val violations = mutableListOf<ValidationViolation>()

        // Check array size
        if (NotionApiLimits.Utils.isArrayTooLarge(blocks.size)) {
            violations.add(
                ValidationViolation(
                    field = fieldName,
                    violationType = ViolationType.ARRAY_TOO_LARGE,
                    message = "Block array too large: ${blocks.size} blocks (max: ${NotionApiLimits.Collections.MAX_ARRAY_ELEMENTS})",
                    currentValue = blocks.size,
                    limit = NotionApiLimits.Collections.MAX_ARRAY_ELEMENTS,
                    autoFixAvailable = true,
                ),
            )
        }

        // For now, we'll skip detailed content validation for Block objects
        // since they are different from BlockRequest objects
        // This could be extended later if needed

        return violations
    }

    /**
     * Validates a single page property.
     */
    private fun validatePageProperty(
        propertyName: String,
        property: PagePropertyValue,
    ): List<ValidationViolation> {
        val violations = mutableListOf<ValidationViolation>()

        when (property) {
            is PagePropertyValue.TitleValue -> {
                violations.addAll(validateRichTextArray("$propertyName.title", property.title))
            }
            is PagePropertyValue.RichTextValue -> {
                violations.addAll(validateRichTextArray("$propertyName.richText", property.richText))
            }
            is PagePropertyValue.MultiSelectValue -> {
                if (property.multiSelect.size > NotionApiLimits.Collections.MAX_MULTI_SELECT_OPTIONS) {
                    violations.add(
                        ValidationViolation(
                            field = "$propertyName.multiSelect",
                            violationType = ViolationType.ARRAY_TOO_LARGE,
                            message =
                                "Multi-select has too many options: ${property.multiSelect.size} " +
                                    "(max: ${NotionApiLimits.Collections.MAX_MULTI_SELECT_OPTIONS})",
                            currentValue = property.multiSelect.size,
                            limit = NotionApiLimits.Collections.MAX_MULTI_SELECT_OPTIONS,
                            autoFixAvailable = true,
                        ),
                    )
                }
            }
            is PagePropertyValue.RelationValue -> {
                if (property.relation.size > NotionApiLimits.Collections.MAX_RELATION_PAGES) {
                    violations.add(
                        ValidationViolation(
                            field = "$propertyName.relation",
                            violationType = ViolationType.ARRAY_TOO_LARGE,
                            message =
                                "Relation has too many pages: ${property.relation.size} " +
                                    "(max: ${NotionApiLimits.Collections.MAX_RELATION_PAGES})",
                            currentValue = property.relation.size,
                            limit = NotionApiLimits.Collections.MAX_RELATION_PAGES,
                            autoFixAvailable = true,
                        ),
                    )
                }
            }
            is PagePropertyValue.PeopleValue -> {
                if (property.people.size > NotionApiLimits.Collections.MAX_PEOPLE_USERS) {
                    violations.add(
                        ValidationViolation(
                            field = "$propertyName.people",
                            violationType = ViolationType.ARRAY_TOO_LARGE,
                            message =
                                "People property has too many users: ${property.people.size} " +
                                    "(max: ${NotionApiLimits.Collections.MAX_PEOPLE_USERS})",
                            currentValue = property.people.size,
                            limit = NotionApiLimits.Collections.MAX_PEOPLE_USERS,
                            autoFixAvailable = true,
                        ),
                    )
                }
            }
            is PagePropertyValue.UrlValue -> {
                property.url?.let { url ->
                    if (NotionApiLimits.Utils.isUrlTooLong(url)) {
                        violations.add(
                            ValidationViolation(
                                field = "$propertyName.url",
                                violationType = ViolationType.CONTENT_TOO_LONG,
                                message = "URL too long: ${url.length} chars (max: ${NotionApiLimits.Content.MAX_URL_LENGTH})",
                                currentValue = url.length,
                                limit = NotionApiLimits.Content.MAX_URL_LENGTH,
                                autoFixAvailable = false,
                            ),
                        )
                    }
                }
            }
            is PagePropertyValue.EmailValue -> {
                property.email?.let { email ->
                    if (email.length > NotionApiLimits.Content.MAX_EMAIL_LENGTH) {
                        violations.add(
                            ValidationViolation(
                                field = "$propertyName.email",
                                violationType = ViolationType.CONTENT_TOO_LONG,
                                message = "Email too long: ${email.length} chars (max: ${NotionApiLimits.Content.MAX_EMAIL_LENGTH})",
                                currentValue = email.length,
                                limit = NotionApiLimits.Content.MAX_EMAIL_LENGTH,
                                autoFixAvailable = false,
                            ),
                        )
                    }
                }
            }
            is PagePropertyValue.PhoneNumberValue -> {
                property.phoneNumber?.let { phone ->
                    if (phone.length > NotionApiLimits.Content.MAX_PHONE_LENGTH) {
                        violations.add(
                            ValidationViolation(
                                field = "$propertyName.phoneNumber",
                                violationType = ViolationType.CONTENT_TOO_LONG,
                                message = "Phone number too long: ${phone.length} chars (max: ${NotionApiLimits.Content.MAX_PHONE_LENGTH})",
                                currentValue = phone.length,
                                limit = NotionApiLimits.Content.MAX_PHONE_LENGTH,
                                autoFixAvailable = false,
                            ),
                        )
                    }
                }
            }
            // Other property types don't have size constraints
            else -> { /* No validation needed */ }
        }

        return violations
    }

    /**
     * Validates an array of rich text objects.
     */
    private fun validateRichTextArray(
        fieldName: String,
        richTextArray: List<RichText>,
    ): List<ValidationViolation> {
        val violations = mutableListOf<ValidationViolation>()

        richTextArray.forEachIndexed { index, richText ->
            val content = extractTextContent(richText)
            if (NotionApiLimits.Utils.isRichTextTooLong(content)) {
                violations.add(
                    ValidationViolation(
                        field = "$fieldName[$index]",
                        violationType = ViolationType.CONTENT_TOO_LONG,
                        message = "Rich text too long: ${content.length} chars (max: ${NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH})",
                        currentValue = content.length,
                        limit = NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH,
                        autoFixAvailable = true,
                    ),
                )
            }

            // Validate URLs in rich text
            richText.href?.let { url ->
                if (NotionApiLimits.Utils.isUrlTooLong(url)) {
                    violations.add(
                        ValidationViolation(
                            field = "$fieldName[$index].href",
                            violationType = ViolationType.CONTENT_TOO_LONG,
                            message = "Rich text URL too long: ${url.length} chars (max: ${NotionApiLimits.Content.MAX_URL_LENGTH})",
                            currentValue = url.length,
                            limit = NotionApiLimits.Content.MAX_URL_LENGTH,
                            autoFixAvailable = false,
                        ),
                    )
                }
            }

            // Validate equations
            richText.equation?.expression?.let { expression ->
                if (expression.length > NotionApiLimits.Content.MAX_EQUATION_LENGTH) {
                    violations.add(
                        ValidationViolation(
                            field = "$fieldName[$index].equation",
                            violationType = ViolationType.CONTENT_TOO_LONG,
                            message = "Equation too long: ${expression.length} chars (max: ${NotionApiLimits.Content.MAX_EQUATION_LENGTH})",
                            currentValue = expression.length,
                            limit = NotionApiLimits.Content.MAX_EQUATION_LENGTH,
                            autoFixAvailable = true,
                        ),
                    )
                }
            }
        }

        return violations
    }

    /**
     * Validates the content within a block.
     */
    private fun validateBlockContent(
        fieldName: String,
        block: BlockRequest,
    ): List<ValidationViolation> {
        val violations = mutableListOf<ValidationViolation>()

        // Extract rich text content from different block types
        val richTextArrays = extractRichTextFromBlock(block)
        richTextArrays.forEach { (subField, richTexts) ->
            violations.addAll(validateRichTextArray("$fieldName.$subField", richTexts))
        }

        return violations
    }

    /**
     * Extracts text content from a rich text object.
     */
    private fun extractTextContent(richText: RichText): String =
        richText.plainText
            ?: richText.text?.content
            ?: richText.equation?.expression
            ?: ""

    /**
     * Extracts rich text arrays from different block types.
     */
    private fun extractRichTextFromBlock(block: BlockRequest): Map<String, List<RichText>> {
        val richTextArrays = mutableMapOf<String, List<RichText>>()

        when (block) {
            is BlockRequest.Paragraph -> {
                richTextArrays["paragraph.richText"] = block.paragraph.richText
            }
            is BlockRequest.Heading1 -> {
                richTextArrays["heading1.richText"] = block.heading1.richText
            }
            is BlockRequest.Heading2 -> {
                richTextArrays["heading2.richText"] = block.heading2.richText
            }
            is BlockRequest.Heading3 -> {
                richTextArrays["heading3.richText"] = block.heading3.richText
            }
            is BlockRequest.BulletedListItem -> {
                richTextArrays["bulletedListItem.richText"] = block.bulletedListItem.richText
            }
            is BlockRequest.NumberedListItem -> {
                richTextArrays["numberedListItem.richText"] = block.numberedListItem.richText
            }
            is BlockRequest.ToDo -> {
                richTextArrays["toDo.richText"] = block.toDo.richText
            }
            is BlockRequest.Toggle -> {
                richTextArrays["toggle.richText"] = block.toggle.richText
            }
            is BlockRequest.Quote -> {
                richTextArrays["quote.richText"] = block.quote.richText
            }
            is BlockRequest.Callout -> {
                richTextArrays["callout.richText"] = block.callout.richText
            }
            is BlockRequest.Code -> {
                richTextArrays["code.richText"] = block.code.richText
                richTextArrays["code.caption"] = block.code.caption
            }
            is BlockRequest.Image -> {
                richTextArrays["image.caption"] = block.image.caption
            }
            is BlockRequest.Video -> {
                richTextArrays["video.caption"] = block.video.caption
            }
            is BlockRequest.Audio -> {
                richTextArrays["audio.caption"] = block.audio.caption
            }
            is BlockRequest.File -> {
                richTextArrays["file.caption"] = block.file.caption
            }
            is BlockRequest.PDF -> {
                richTextArrays["pdf.caption"] = block.pdf.caption
            }
            is BlockRequest.Divider -> {
                // Divider blocks don't have rich text content
            }
        }

        return richTextArrays
    }

    // =============================================================================
    // PRIVATE HELPER METHODS FOR VIOLATION HANDLING AND TEXT FIXING
    // =============================================================================

    /**
     * Generic violation handler that separates text from non-text violations.
     */
    private inline fun <T> handleViolations(
        request: T,
        violations: ValidationResult,
        fixTextViolations: (T, List<ValidationViolation>) -> T,
    ): T {
        val textViolations = violations.violations.filter { isTextViolation(it) }
        val otherViolations = violations.violations.filter { !isTextViolation(it) }

        // Always fail fast on non-text violations
        if (otherViolations.isNotEmpty()) {
            throw ValidationException(ValidationResult(otherViolations))
        }

        // Handle text violations based on configuration
        if (textViolations.isNotEmpty()) {
            if (config.autoSplitLongText) {
                val fixedRequest = fixTextViolations(request, textViolations)

                // Log info about auto-split text
                textViolations.forEach { violation ->
                    println(
                        "INFO: Auto-split long text content in field '${violation.field}' into multiple segments",
                    ) // TODO: Use proper logging
                }

                return fixedRequest
            } else {
                throw ValidationException(ValidationResult(textViolations))
            }
        }

        return request
    }

    /**
     * Checks if a violation is a text content violation that can be auto-fixed.
     */
    private fun isTextViolation(violation: ValidationViolation): Boolean =
        violation.violationType == ViolationType.CONTENT_TOO_LONG && violation.autoFixAvailable

    /**
     * Fixes text violations in a page creation request.
     */
    private fun fixPageTextViolations(
        request: CreatePageRequest,
        textViolations: List<ValidationViolation>,
    ): CreatePageRequest {
        var fixedRequest = request.copy()

        textViolations.forEach { violation ->
            when {
                violation.field.startsWith("title.") -> {
                    val fixedTitle = fixRichTextInProperty(violation.field, fixedRequest.properties["title"])
                    if (fixedTitle != null) {
                        val newProperties = fixedRequest.properties.toMutableMap()
                        newProperties["title"] = fixedTitle
                        fixedRequest = fixedRequest.copy(properties = newProperties)
                    }
                }
                violation.field.contains(".richText[") || violation.field.contains(".title[") -> {
                    val propertyName = violation.field.substringBefore('.')
                    val originalProperty = fixedRequest.properties[propertyName]
                    val fixedProperty = fixRichTextInProperty(violation.field, originalProperty)
                    if (fixedProperty != null) {
                        val newProperties = fixedRequest.properties.toMutableMap()
                        newProperties[propertyName] = fixedProperty
                        fixedRequest = fixedRequest.copy(properties = newProperties)
                    }
                }
            }
        }

        return fixedRequest
    }

    /**
     * Fixes text violations in a page update request.
     */
    private fun fixPageUpdateTextViolations(
        request: UpdatePageRequest,
        textViolations: List<ValidationViolation>,
    ): UpdatePageRequest {
        val properties = request.properties ?: return request
        var fixedRequest = request.copy()

        textViolations.forEach { violation ->
            val propertyName = violation.field.substringBefore('.')
            val originalProperty = properties[propertyName]
            val fixedProperty = fixRichTextInProperty(violation.field, originalProperty)
            if (fixedProperty != null) {
                val newProperties = properties.toMutableMap()
                newProperties[propertyName] = fixedProperty
                fixedRequest = fixedRequest.copy(properties = newProperties)
            }
        }

        return fixedRequest
    }

    /**
     * Fixes text violations in a database creation request.
     */
    private fun fixDatabaseTextViolations(
        request: CreateDatabaseRequest,
        textViolations: List<ValidationViolation>,
    ): CreateDatabaseRequest {
        var fixedRequest = request.copy()

        textViolations.forEach { violation ->
            when {
                violation.field.startsWith("title[") -> {
                    val fixedTitle = fixRichTextArray(fixedRequest.title)
                    fixedRequest = fixedRequest.copy(title = fixedTitle)
                }
                violation.field.startsWith("description[") -> {
                    fixedRequest.description?.let { desc ->
                        val fixedDescription = fixRichTextArray(desc)
                        fixedRequest = fixedRequest.copy(description = fixedDescription)
                    }
                }
            }
        }

        return fixedRequest
    }

    /**
     * Fixes rich text content in a specific page property.
     */
    private fun fixRichTextInProperty(
        violationField: String,
        property: PagePropertyValue?,
    ): PagePropertyValue? =
        when (property) {
            is PagePropertyValue.TitleValue -> {
                if (violationField.contains("title[")) {
                    val fixedTitle = fixRichTextArray(property.title)
                    property.copy(title = fixedTitle)
                } else {
                    null
                }
            }
            is PagePropertyValue.RichTextValue -> {
                if (violationField.contains("richText[")) {
                    val fixedRichText = fixRichTextArray(property.richText)
                    property.copy(richText = fixedRichText)
                } else {
                    null
                }
            }
            else -> null
        }

    /**
     * Fixes an array of rich text by splitting long segments into multiple segments.
     * This preserves all content while staying within API limits per segment.
     *
     * Note: We use splitting rather than truncation because Notion's API enforces
     * a 2000-character limit PER SEGMENT, not on the total array length. This means
     * we can preserve all content by distributing it across multiple RichText objects
     * instead of losing data through truncation.
     */
    private fun fixRichTextArray(richTextArray: List<RichText>): List<RichText> = splitRichTextArray(richTextArray)

    /**
     * Splits a long rich text segment into multiple segments while preserving formatting.
     * Each segment will be under the maximum length limit while maintaining all annotations.
     * Only handles text content - throws error for other types (equations, etc.) that exceed limits.
     */
    private fun splitRichText(
        richText: RichText,
        maxSegmentLength: Int = NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH,
    ): List<RichText> {
        val content = extractTextContent(richText)

        // If content is within limits, return as-is
        if (content.length <= NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH) {
            return listOf(richText)
        }

        // Only split text content - other types should fail fast
        if (richText.text == null) {
            throw IllegalArgumentException(
                "Non-text rich text content exceeds character limit and cannot be split: ${content.length} chars (max: ${NotionApiLimits.Content.MAX_RICH_TEXT_LENGTH})",
            )
        }

        // Split the content into chunks
        val chunks = content.chunked(maxSegmentLength)

        // Create new RichText objects for each chunk, preserving all formatting and annotations
        return chunks.map { chunk ->
            richText.copy(
                text = richText.text?.copy(content = chunk),
                plainText = chunk,
                annotations = richText.annotations, // Preserve formatting
                href = richText.href, // Preserve links
            )
        }
    }

    /**
     * Splits long rich text segments in an array into multiple segments.
     * Returns a new list with long segments split while preserving sequence and formatting.
     */
    private fun splitRichTextArray(richTextArray: List<RichText>): List<RichText> {
        val result = mutableListOf<RichText>()

        richTextArray.forEach { richText ->
            result.addAll(splitRichText(richText))
        }

        return result
    }
}
