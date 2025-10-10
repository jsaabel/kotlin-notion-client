package it.saabel.kotlinnotionclient.validation

/**
 * Configuration for request validation behavior.
 *
 * @param autoSplitLongText Whether to automatically split text content that exceeds per-segment limits.
 *                          When true, long text is split into multiple RichText segments.
 *                          When false, validation fails with an exception for text violations.
 *
 * Note: We use splitting rather than truncation because Notion's API enforces a 2000-character
 * limit PER SEGMENT in rich text arrays, not on the total array length. This allows us to
 * preserve all content by distributing it across multiple segments instead of losing data
 * through truncation.
 */
data class ValidationConfig(
    val autoSplitLongText: Boolean = true,
) {
    companion object {
        fun default() = ValidationConfig()

        fun withAutoSplit() =
            ValidationConfig(
                autoSplitLongText = true,
            )

        fun withoutAutoSplit() =
            ValidationConfig(
                autoSplitLongText = false,
            )
    }
}

/**
 * Result of a validation operation.
 */
data class ValidationResult(
    val violations: List<ValidationViolation>,
) {
    val isValid: Boolean get() = violations.isEmpty()
    val hasErrors: Boolean get() = violations.any { it.violationType.isError }
    val hasWarnings: Boolean get() = violations.any { it.violationType.isWarning }

    /**
     * Gets violations of a specific type.
     */
    fun getViolations(type: ViolationType): List<ValidationViolation> = violations.filter { it.violationType == type }

    /**
     * Gets violations for a specific field.
     */
    fun getViolationsForField(field: String): List<ValidationViolation> = violations.filter { it.field == field }

    /**
     * Returns a human-readable summary of all violations.
     */
    fun getSummary(): String {
        if (isValid) return "No validation violations found"

        val errorCount = violations.count { it.violationType.isError }
        val warningCount = violations.count { it.violationType.isWarning }

        return buildString {
            appendLine("Validation Summary:")
            if (errorCount > 0) appendLine("  Errors: $errorCount")
            if (warningCount > 0) appendLine("  Warnings: $warningCount")
            appendLine()
            violations.forEach { violation ->
                appendLine("  ${violation.violationType.name}: ${violation.message}")
            }
        }
    }
}

/**
 * Represents a single validation violation.
 */
data class ValidationViolation(
    /** The field or property that violated the constraint */
    val field: String,
    /** The type of violation */
    val violationType: ViolationType,
    /** Human-readable description of the violation */
    val message: String,
    /** Current value that caused the violation */
    val currentValue: Any? = null,
    /** The limit that was exceeded */
    val limit: Any? = null,
    /** Whether this violation can be automatically fixed */
    val autoFixAvailable: Boolean = false,
    /** Suggested action to resolve the violation */
    val suggestedAction: String? = null,
) {
    /**
     * Returns a detailed description including suggested fixes.
     */
    fun getDetailedMessage(): String =
        buildString {
            append(message)
            if (currentValue != null && limit != null) {
                append(" (current: $currentValue, limit: $limit)")
            }
            if (autoFixAvailable) {
                append(" - Auto-fix available")
            }
            suggestedAction?.let { action ->
                append(" - Suggested: $action")
            }
        }
}

/**
 * Types of validation violations.
 */
enum class ViolationType(
    val isError: Boolean = true,
    val isWarning: Boolean = false,
) {
    /** Content exceeds length limits */
    CONTENT_TOO_LONG(isError = true),

    /** Array exceeds size limits */
    ARRAY_TOO_LARGE(isError = true),

    /** Overall payload exceeds size limits */
    PAYLOAD_TOO_LARGE(isError = true),

    /** URL format or length issues */
    INVALID_URL(isError = true),

    /** Email format or length issues */
    INVALID_EMAIL(isError = true),

    /** Phone number format or length issues */
    INVALID_PHONE(isError = true),

    /** Content approaching limits (warning) */
    CONTENT_NEAR_LIMIT(isError = false, isWarning = true),

    /** Array approaching size limits (warning) */
    ARRAY_NEAR_LIMIT(isError = false, isWarning = true),

    /** Payload approaching size limits (warning) */
    PAYLOAD_NEAR_LIMIT(isError = false, isWarning = true),
}

/**
 * Exception thrown when validation fails in strict mode.
 */
class ValidationException(
    val validationResult: ValidationResult,
    message: String = "Request validation failed",
) : IllegalArgumentException("$message\n${validationResult.getSummary()}")

/**
 * Result of attempting to automatically fix validation violations.
 */
data class AutoFixResult<T>(
    /** The fixed/modified request object */
    val fixedRequest: T,
    /** Violations that were successfully fixed */
    val fixedViolations: List<ValidationViolation>,
    /** Violations that could not be automatically fixed */
    val remainingViolations: List<ValidationViolation>,
    /** Summary of what was changed */
    val changesSummary: List<String>,
) {
    val wasFullyFixed: Boolean get() = remainingViolations.isEmpty()
    val hasRemainingErrors: Boolean get() = remainingViolations.any { it.violationType.isError }
}
