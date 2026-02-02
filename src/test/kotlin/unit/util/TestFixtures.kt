@file:Suppress("unused")

package unit.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import java.io.File

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
        val file = File("reference/notion-api/sample_responses/$category/$filename.json")
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
        val file = File("reference/notion-api/sample_responses/$category/$filename.json")
        return file.readText()
    }

    // Database-specific helpers (2025-09-03 API)
    object Databases {
        fun retrieveDatabase() = loadSampleResponse("databases", "get_retrieve_a_database")

        fun createDatabase() = loadSampleResponse("databases", "post_create_a_database")

        fun updateDatabase() = loadSampleResponse("databases", "patch_update_a_database")

        fun retrieveDatabaseAsString() = loadSampleResponseAsString("databases", "get_retrieve_a_database")

        fun createDatabaseAsString() = loadSampleResponseAsString("databases", "post_create_a_database")

        fun updateDatabaseAsString() = loadSampleResponseAsString("databases", "patch_update_a_database")
    }

    // Data Sources-specific helpers (2025-09-03 API)
    object DataSources {
        fun retrieveDataSource() = loadSampleResponse("data_sources", "get_retrieve_a_data_source")

        fun createDataSource() = loadSampleResponse("data_sources", "post_create_a_data_source")

        fun updateDataSource() = loadSampleResponse("data_sources", "patch_update_a_data_source")

        fun queryDataSource() = loadSampleResponse("data_sources", "post_query_a_data_source")

        fun listTemplates() = loadSampleResponse("data_sources", "get_list_data_source_templates")

        fun retrieveDataSourceAsString() = loadSampleResponseAsString("data_sources", "get_retrieve_a_data_source")

        fun createDataSourceAsString() = loadSampleResponseAsString("data_sources", "post_create_a_data_source")

        fun updateDataSourceAsString() = loadSampleResponseAsString("data_sources", "patch_update_a_data_source")

        fun queryDataSourceAsString() = loadSampleResponseAsString("data_sources", "post_query_a_data_source")

        fun listTemplatesAsString() = loadSampleResponseAsString("data_sources", "get_list_data_source_templates")
    }

    // Page-specific helpers (2025-09-03 API)
    object Pages {
        fun retrievePage() = loadSampleResponse("pages", "get_retrieve_a_page")

        fun createPage() = loadSampleResponse("pages", "post_create_a_page")

        fun retrievePageProperty() = loadSampleResponse("pages", "get_retrieve_a_page_property_item")

        fun retrievePageAsString() = loadSampleResponseAsString("pages", "get_retrieve_a_page")

        fun createPageAsString() = loadSampleResponseAsString("pages", "post_create_a_page")

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

        // Helper methods for testing
        fun retrieveChildrenPaginated() = retrieveBlockChildren()

        fun appendChildrenResponse() = appendBlockChildren()
    }

    // Helper method to read API response files directly
    fun readApiResponse(path: String): JsonElement {
        val resourcePath = "/api/$path"
        val content =
            this::class.java.getResource(resourcePath)?.readText()
                ?: throw IllegalArgumentException("API response not found: $resourcePath")
        return json.parseToJsonElement(content)
    }

    // Comment-specific helpers
    object Comments {
        fun retrieveComments() = loadSampleResponse("comments", "get_retrieve_comments")

        fun createComment() = loadSampleResponse("comments", "post_create_comment")

        fun retrieveCommentsAsString() = loadSampleResponseAsString("comments", "get_retrieve_comments")

        fun createCommentAsString() = loadSampleResponseAsString("comments", "post_create_comment")
    }

    // User-specific helpers
    object Users {
        fun retrieveBotUser() = loadSampleResponse("users", "get_retrieve_bot_user")

        fun retrievePersonUser() = loadSampleResponse("users", "get_retrieve_a_person_user")

        fun retrieveABotUser() = loadSampleResponse("users", "get_retrieve_a_bot_user")

        fun listUsers() = loadSampleResponse("users", "get_list_all_users")

        fun retrieveBotUserAsString() = loadSampleResponseAsString("users", "get_retrieve_bot_user")

        fun retrievePersonUserAsString() = loadSampleResponseAsString("users", "get_retrieve_a_person_user")

        fun retrieveABotUserAsString() = loadSampleResponseAsString("users", "get_retrieve_a_bot_user")

        fun listUsersAsString() = loadSampleResponseAsString("users", "get_list_all_users")
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

    object Search {
        fun searchByTitle() = loadSampleResponse("search", "post_search_by_title")

        fun searchByTitleAsString() = loadSampleResponseAsString("search", "post_search_by_title")
    }
}

/**
 * Extension function to easily unit.util.decode JsonElement to a specific type
 */
inline fun <reified T> JsonElement.decode(): T = TestFixtures.json.decodeFromJsonElement(serializer<T>(), this)
