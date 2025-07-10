package no.saabelit.kotlinnotionclient.models.users

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val name: String?,
    @SerialName("avatar_url")
    val avatarUrl: String?,
    val type: UserType,
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
