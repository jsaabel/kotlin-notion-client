package no.saabelit.kotlinnotionclient.models.users

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: It might not make a whole lot of sense to represent BOTH the "rich" object returned by calls to /Users endpoint
//  as well as simple "user references" (as in createdBy:...) through this data class. We should find an alternative approach.

/**
 * Represents a User in the Notion API.
 *
 * Users include full workspace members, guests, and integrations.
 * For bot users (like API integrations), the type will be "bot" and
 * include additional bot-specific information.
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
 * Information about a bot user (API integration).
 */
@Serializable
data class BotInfo(
    val owner: Owner,
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
