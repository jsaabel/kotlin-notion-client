@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.requests

import no.saabelit.kotlinnotionclient.models.base.Annotations
import no.saabelit.kotlinnotionclient.models.base.Mention
import no.saabelit.kotlinnotionclient.models.base.Parent
import no.saabelit.kotlinnotionclient.models.base.RichText
import no.saabelit.kotlinnotionclient.models.base.TextContent
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseProperty
import no.saabelit.kotlinnotionclient.models.databases.CreateDatabaseRequest
import no.saabelit.kotlinnotionclient.models.databases.InitialDataSource
import no.saabelit.kotlinnotionclient.models.pages.CreatePageRequest
import no.saabelit.kotlinnotionclient.models.pages.ExternalFile
import no.saabelit.kotlinnotionclient.models.pages.PageIcon
import no.saabelit.kotlinnotionclient.models.pages.PagePropertyValue
import no.saabelit.kotlinnotionclient.models.users.User

/**
 * Builder utilities for creating common request objects.
 *
 * These functions provide convenient ways to construct request objects
 * with sensible defaults and validation.
 *
 * Note on naming conventions:
 * - PagePropertyValue (property values for API requests)
 * - DatabaseProperty (property definitions/schemas)
 * - base.RichText (actual rich text content structure)
 * This naming avoids conflicts and clarifies the model boundaries.
 */
object RequestBuilders {
    /**
     * Creates a simple database creation request with a title property.
     *
     * @param parentPageId The ID of the parent page
     * @param title The database title
     * @return Configured CreateDatabaseRequest
     */
    fun createSimpleDatabase(
        parentPageId: String,
        title: String,
    ): CreateDatabaseRequest =
        CreateDatabaseRequest(
            parent =
                Parent(
                    type = "page_id",
                    pageId = parentPageId,
                ),
            title = listOf(createSimpleRichText(title)),
            initialDataSource =
                InitialDataSource(
                    properties =
                        mapOf(
                            "Name" to CreateDatabaseProperty.Title(),
                        ),
                ),
        )

    /**
     * Creates a page creation request for a database child.
     *
     * @param databaseId The ID of the parent database
     * @param title The page title
     * @return Configured CreatePageRequest
     */
    fun createDatabasePage(
        databaseId: String,
        title: String,
    ): CreatePageRequest =
        CreatePageRequest(
            parent =
                Parent(
                    type = "database_id",
                    databaseId = databaseId,
                ),
            properties =
                mapOf(
                    "Name" to
                        PagePropertyValue.TitleValue(
                            title = listOf(createSimpleRichText(title)),
                        ),
                ),
        )

    /**
     * Creates a page creation request for a page child.
     *
     * @param parentPageId The ID of the parent page
     * @param title The page title
     * @return Configured CreatePageRequest
     */
    fun createChildPage(
        parentPageId: String,
        title: String,
    ): CreatePageRequest =
        CreatePageRequest(
            parent =
                Parent(
                    type = "page_id",
                    pageId = parentPageId,
                ),
            properties =
                mapOf(
                    "title" to
                        PagePropertyValue.TitleValue(
                            title = listOf(createSimpleRichText(title)),
                        ),
                ),
        )

    /**
     * Creates a simple rich text object with plain text content.
     *
     * @param content The text content
     * @return Configured RichText object
     */
    fun createSimpleRichText(content: String): RichText =
        RichText(
            type = "text",
            text =
                TextContent(
                    content = content,
                    link = null,
                ),
            annotations = Annotations(),
            plainText = content,
            href = null,
        )

    /**
     * Creates a rich text object with a user mention.
     *
     * @param userId The ID of the user to mention
     * @param userName The display name of the user (used as plain text)
     * @return Configured RichText object with user mention
     */
    fun createUserMention(
        userId: String,
        userName: String = "User",
    ): RichText =
        RichText(
            type = "mention",
            mention =
                Mention.User(
                    user = User(id = userId, objectType = "user"),
                ),
            annotations = Annotations(),
            plainText = "@$userName",
            href = null,
        )

    /**
     * Creates an emoji icon for pages and databases.
     *
     * @param emoji The emoji character (e.g., "🗄️", "📄", "✅")
     * @return Configured PageIcon for emoji
     */
    fun createEmojiIcon(emoji: String): PageIcon =
        PageIcon(
            type = "emoji",
            emoji = emoji,
        )

    /**
     * Creates an external file icon for pages and databases.
     *
     * @param url The URL of the external icon file
     * @return Configured PageIcon for external file
     */
    fun createExternalIcon(url: String): PageIcon =
        PageIcon(
            type = "external",
            external = ExternalFile(url = url),
        )
}
