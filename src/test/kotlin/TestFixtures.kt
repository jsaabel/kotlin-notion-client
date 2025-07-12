@file:Suppress("unused")

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * Utility class for loading official Notion API sample responses for testing.
 * All sample responses are from the official Notion API documentation.
 */
object TestFixtures {
    val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = false
            explicitNulls = false
            coerceInputValues = true
        }

    /**
     * Load a sample response from the test resources.
     * @param category The API category (databases, pages, blocks, comments, users)
     * @param filename The filename without extension
     * @return JsonElement containing the parsed response
     */
    fun loadSampleResponse(
        category: String,
        filename: String,
    ): JsonElement {
        val resourcePath = "/api/$category/$filename.json"
        val content =
            this::class.java.getResource(resourcePath)?.readText()
                ?: throw IllegalArgumentException("Sample response not found: $resourcePath")
        return json.parseToJsonElement(content)
    }

    /**
     * Load sample response as raw JSON string.
     */
    fun loadSampleResponseAsString(
        category: String,
        filename: String,
    ): String {
        val resourcePath = "/api/$category/$filename.json"
        return this::class.java.getResource(resourcePath)?.readText()
            ?: throw IllegalArgumentException("Sample response not found: $resourcePath")
    }

    /**
     * Load a sample response from the reference directory (for newer samples).
     * This reads directly from the reference/notion-api/sample_responses directory.
     * @param category The API category (file_uploads, etc.)
     * @param filename The filename without extension
     * @return JsonElement containing the parsed response
     */
    fun loadReferenceSampleResponse(
        category: String,
        filename: String,
    ): JsonElement {
        val file = java.io.File("reference/notion-api/sample_responses/$category/$filename.json")
        val content = file.readText()
        return json.parseToJsonElement(content)
    }

    /**
     * Load reference sample response as raw JSON string.
     */
    fun loadReferenceSampleResponseAsString(
        category: String,
        filename: String,
    ): String {
        val file = java.io.File("reference/notion-api/sample_responses/$category/$filename.json")
        return file.readText()
    }

    // Database-specific helpers
    object Databases {
        fun retrieveDatabase() = loadSampleResponse("databases", "get_retrieve_a_database")

        fun createDatabase() = loadSampleResponse("databases", "post_create_a_database")

        fun updateDatabase() = loadSampleResponse("databases", "patch_update_a_database")

        fun queryDatabase() = loadSampleResponse("databases", "post_query_a_database")

        fun retrieveDatabaseAsString() = loadSampleResponseAsString("databases", "get_retrieve_a_database")

        fun createDatabaseAsString() = loadSampleResponseAsString("databases", "post_create_a_database")

        fun updateDatabaseAsString() = loadSampleResponseAsString("databases", "patch_update_a_database")

        fun queryDatabaseAsString() = loadSampleResponseAsString("databases", "post_query_a_database")
    }

    // Page-specific helpers
    object Pages {
        fun retrievePage() = loadSampleResponse("pages", "get_retrieve_a_page")

        fun createPage() = loadSampleResponse("pages", "post_create_a_page")

        fun updatePageProperties() = loadSampleResponse("pages", "patch_update_page_properties")

        fun retrievePageProperty() = loadSampleResponse("pages", "get_retrieve_a_page_property_item")

        fun retrievePageAsString() = loadSampleResponseAsString("pages", "get_retrieve_a_page")

        fun createPageAsString() = loadSampleResponseAsString("pages", "post_create_a_page")

        fun updatePagePropertiesAsString() = loadSampleResponseAsString("pages", "patch_update_page_properties")

        fun retrievePagePropertyAsString() = loadSampleResponseAsString("pages", "get_retrieve_a_page_property_item")
    }

    // Block-specific helpers
    object Blocks {
        fun retrieveBlock() = loadSampleResponse("blocks", "get_retrieve_a_block")

        fun retrieveBlockChildren() = loadSampleResponse("blocks", "get_retrieve_block_children")

        fun appendBlockChildren() = loadSampleResponse("blocks", "patch_append_block_children")

        fun updateBlock() = loadSampleResponse("blocks", "patch_update_a_block")

        fun deleteBlock() = loadSampleResponse("blocks", "delete_delete_a_block")

        fun retrieveBlockAsString() = loadSampleResponseAsString("blocks", "get_retrieve_a_block")

        fun retrieveBlockChildrenAsString() = loadSampleResponseAsString("blocks", "get_retrieve_block_children")

        fun appendBlockChildrenAsString() = loadSampleResponseAsString("blocks", "patch_append_block_children")

        fun updateBlockAsString() = loadSampleResponseAsString("blocks", "patch_update_a_block")

        fun deleteBlockAsString() = loadSampleResponseAsString("blocks", "delete_delete_a_block")
    }

    // Comment-specific helpers
    object Comments {
        fun retrieveComments() = loadSampleResponse("comments", "get_retrieve_comments")

        fun createComment() = loadSampleResponse("comments", "post_create_comment")

        fun retrieveCommentsAsString() = loadSampleResponseAsString("comments", "get_retrieve_comments")

        fun createCommentAsString() = loadSampleResponseAsString("comments", "post_create_comment")
    }

    // FileUpload-specific helpers (using reference directory)
    object FileUploads {
        fun createFileUpload() = loadReferenceSampleResponse("file_uploads", "post_create_a_file_upload")

        fun retrieveFileUpload() = loadReferenceSampleResponse("file_uploads", "get_retrieve_a_file_upload")

        fun completeFileUpload() = loadReferenceSampleResponse("file_uploads", "post_complete_a_file_upload")

        fun listFileUploads() = loadReferenceSampleResponse("file_uploads", "get_list_file_uploads")

        fun sendFileUpload() = loadReferenceSampleResponse("file_uploads", "post_send_a_file_upload")

        fun createFileUploadAsString() = loadReferenceSampleResponseAsString("file_uploads", "post_create_a_file_upload")

        fun retrieveFileUploadAsString() = loadReferenceSampleResponseAsString("file_uploads", "get_retrieve_a_file_upload")

        fun completeFileUploadAsString() = loadReferenceSampleResponseAsString("file_uploads", "post_complete_a_file_upload")

        fun listFileUploadsAsString() = loadReferenceSampleResponseAsString("file_uploads", "get_list_file_uploads")

        fun sendFileUploadAsString() = loadReferenceSampleResponseAsString("file_uploads", "post_send_a_file_upload")
    }
}

/**
 * Extension function to easily decode JsonElement to a specific type
 */
inline fun <reified T> JsonElement.decode(): T = TestFixtures.json.decodeFromJsonElement(serializer<T>(), this)
