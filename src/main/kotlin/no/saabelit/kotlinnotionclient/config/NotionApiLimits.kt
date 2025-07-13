package no.saabelit.kotlinnotionclient.config

/**
 * Centralized definition of Notion API limits and constraints.
 *
 * These limits are based on the official Notion API documentation and should be updated
 * when Notion changes their API constraints. Having them centralized makes it easy to
 * maintain consistency across the codebase and adapt to API changes.
 *
 * Reference: https://developers.notion.com/reference/request-limits
 */
object NotionApiLimits {
    /**
     * General payload and request size constraints.
     */
    object Payload {
        /** Maximum number of block elements per request payload */
        const val MAX_BLOCK_ELEMENTS = 1000

        /** Maximum payload size in bytes (500KB) */
        const val MAX_PAYLOAD_SIZE_BYTES = 500 * 1024

        /** Maximum payload size in KB for human readability */
        const val MAX_PAYLOAD_SIZE_KB = 500
    }

    /**
     * Content and property value size limits.
     */
    object Content {
        /** Maximum characters in rich text content */
        const val MAX_RICH_TEXT_LENGTH = 2000

        /** Maximum characters in rich text URLs */
        const val MAX_URL_LENGTH = 2000

        /** Maximum characters in equation expressions */
        const val MAX_EQUATION_LENGTH = 1000

        /** Maximum characters in email addresses */
        const val MAX_EMAIL_LENGTH = 200

        /** Maximum characters in phone numbers */
        const val MAX_PHONE_LENGTH = 200
    }

    /**
     * Array and collection size limits.
     */
    object Collections {
        /** Maximum elements in any array (blocks, multi-select, etc.) */
        const val MAX_ARRAY_ELEMENTS = 100

        /** Maximum number of multi-select options */
        const val MAX_MULTI_SELECT_OPTIONS = 100

        /** Maximum number of related pages in relation properties */
        const val MAX_RELATION_PAGES = 100

        /** Maximum number of users in people properties */
        const val MAX_PEOPLE_USERS = 100
    }

    /**
     * Response and pagination limits.
     * Note: These apply to API responses, not requests.
     */
    object Response {
        /** Default number of relations returned in relation properties */
        const val DEFAULT_RELATION_LIMIT = 20

        /** Default page size for paginated responses */
        const val DEFAULT_PAGE_SIZE = 100

        /** Maximum page size that can be requested */
        const val MAX_PAGE_SIZE = 100
    }

    /**
     * Error response information for size limit violations.
     */
    object SizeLimitError {
        /** HTTP status code for size limit violations */
        const val HTTP_STATUS_CODE = 400

        /** Error code returned in size limit responses */
        const val ERROR_CODE = "validation_error"
    }

    /**
     * Utility functions for working with limits.
     */
    object Utils {
        /**
         * Checks if a text content exceeds the rich text limit.
         */
        fun isRichTextTooLong(text: String): Boolean = text.length > Content.MAX_RICH_TEXT_LENGTH

        /**
         * Checks if a URL exceeds the URL limit.
         */
        fun isUrlTooLong(url: String): Boolean = url.length > Content.MAX_URL_LENGTH

        /**
         * Checks if an array exceeds the collection limit.
         */
        fun isArrayTooLarge(size: Int): Boolean = size > Collections.MAX_ARRAY_ELEMENTS

        /**
         * Checks if a payload size (in bytes) exceeds the limit.
         */
        fun isPayloadTooLarge(sizeBytes: Int): Boolean = sizeBytes > Payload.MAX_PAYLOAD_SIZE_BYTES

        /**
         * Truncates text to fit within rich text limits.
         * @param text The text to truncate
         * @param suffix Optional suffix to append (e.g., "...")
         * @return Truncated text that fits within limits
         */
        fun truncateRichText(
            text: String,
            suffix: String = "...",
        ): String {
            if (text.length <= Content.MAX_RICH_TEXT_LENGTH) return text

            val maxContentLength = Content.MAX_RICH_TEXT_LENGTH - suffix.length
            return if (maxContentLength > 0) {
                text.take(maxContentLength) + suffix
            } else {
                text.take(Content.MAX_RICH_TEXT_LENGTH)
            }
        }

        /**
         * Splits an array into chunks that respect collection limits.
         */
        fun <T> chunkArray(
            items: List<T>,
            maxSize: Int = Collections.MAX_ARRAY_ELEMENTS,
        ): List<List<T>> = items.chunked(maxSize)
    }
}
