@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.requests

import it.saabel.kotlinnotionclient.models.base.Annotations
import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.Mention
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.base.TextContent
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseProperty
import it.saabel.kotlinnotionclient.models.databases.CreateDatabaseRequest
import it.saabel.kotlinnotionclient.models.databases.InitialDataSource
import it.saabel.kotlinnotionclient.models.pages.CreatePageRequest
import it.saabel.kotlinnotionclient.models.pages.PageIcon
import it.saabel.kotlinnotionclient.models.pages.PagePropertyValue
import it.saabel.kotlinnotionclient.models.users.User

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
            parent = Parent.PageParent(pageId = parentPageId),
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
            parent = Parent.DatabaseParent(databaseId = databaseId),
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
            parent = Parent.PageParent(pageId = parentPageId),
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
    fun createEmojiIcon(emoji: String): PageIcon = PageIcon.Emoji(emoji = emoji)

    /**
     * Creates an external file icon for pages and databases.
     *
     * @param url The URL of the external icon file
     * @return Configured PageIcon for external file
     */
    fun createExternalIcon(url: String): PageIcon = PageIcon.External(external = ExternalFile(url = url))
}
