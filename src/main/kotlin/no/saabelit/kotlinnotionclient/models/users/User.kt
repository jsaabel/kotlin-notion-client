@file:Suppress("unused")

package no.saabelit.kotlinnotionclient.models.users

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.saabelit.kotlinnotionclient.utils.PaginatedResponse

/**
 * Represents a User in the Notion API.
 *
 * Users include full workspace members, guests, and integrations.
 * For bot users (like API integrations), the type will be "bot" and
 * include additional bot-specific information.
 * For person users, the type will be "person" and may include person-specific information.
 */
@Serializable
data class User(
    @SerialName("object")
    val objectType: String,
    val id: String,
    val name: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val type: UserType? = null,
    val person: PersonInfo? = null,
    val bot: BotInfo? = null,
)

/**
 * Type of user in the Notion workspace.
 */
@Serializable
enum class UserType {
    @SerialName("person")
    PERSON,

    @SerialName("bot")
    BOT,
}

/**
 * Information about a person user.
 *
 * Note: Email is only present if the integration has user capabilities
 * that allow access to email addresses.
 */
@Serializable
data class PersonInfo(
    val email: String? = null,
)

/**
 * Information about a bot user (API integration).
 *
 * Note: The owner field may not always be present in API responses.
 */
@Serializable
data class BotInfo(
    val owner: Owner? = null,
)

/**
 * Information about the owner who authorized the bot integration.
 */
@Serializable
data class Owner(
    val type: OwnerType,
    @SerialName("user")
    val user: OwnerUser? = null,
    @SerialName("workspace")
    val workspace: Boolean? = null,
)

/**
 * Type of owner that authorized the bot.
 */
@Serializable
enum class OwnerType {
    @SerialName("user")
    USER,

    @SerialName("workspace")
    WORKSPACE,
}

/**
 * User information for the owner who authorized the bot.
 */
@Serializable
data class OwnerUser(
    @SerialName("object")
    val objectType: String,
    val id: String,
    val name: String?,
    @SerialName("avatar_url")
    val avatarUrl: String?,
    val type: UserType,
)

/**
 * Represents a paginated list of users (used for list users responses).
 *
 * @property objectType Always "list"
 * @property results Array of User objects
 * @property nextCursor Cursor for the next page of results (null if no more results)
 * @property hasMore Whether there are more results available
 */
@Serializable
data class UserList(
    @SerialName("object")
    val objectType: String = "list",
    @SerialName("results")
    override val results: List<User>,
    @SerialName("next_cursor")
    override val nextCursor: String? = null,
    @SerialName("has_more")
    override val hasMore: Boolean = false,
) : PaginatedResponse<User>
