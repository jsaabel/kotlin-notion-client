package unit.api

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.NativeIconColor
import it.saabel.kotlinnotionclient.models.base.NativeIconObject
import it.saabel.kotlinnotionclient.models.blocks.nativeIcon
import it.saabel.kotlinnotionclient.models.databases.databaseRequest
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import it.saabel.kotlinnotionclient.models.pages.updatePageRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import unit.util.TestFixtures

/**
 * Unit tests for Icon.NativeIcon: model serialization, deserialization, and DSL helpers.
 */
@Tags("Unit")
class NativeIconTest :
    FunSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
                explicitNulls = false
            }

        context("Icon.NativeIcon serialization") {
            test("should serialize NativeIcon with color correctly") {
                val icon = Icon.NativeIcon(NativeIconObject(name = "pizza", color = NativeIconColor.BLUE))
                val encoded = json.encodeToString(icon)

                encoded shouldBe """{"type":"icon","icon":{"name":"pizza","color":"blue"}}"""
            }

            test("should serialize NativeIcon without color and omit color key") {
                val icon = Icon.NativeIcon(NativeIconObject(name = "pizza"))
                val encoded = json.encodeToString(icon)

                encoded shouldBe """{"type":"icon","icon":{"name":"pizza"}}"""
            }

            test("should serialize LIGHT_GRAY as 'lightgray'") {
                val icon = Icon.NativeIcon(NativeIconObject(name = "circle", color = NativeIconColor.LIGHT_GRAY))
                val encoded = json.encodeToString(icon)

                encoded shouldBe """{"type":"icon","icon":{"name":"circle","color":"lightgray"}}"""
            }

            test("should deserialize NativeIcon from API doc example JSON") {
                val jsonStr = """{"type":"icon","icon":{"name":"pizza","color":"blue"}}"""
                val icon = json.decodeFromString<Icon>(jsonStr)

                val native = icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                native.icon.color shouldBe NativeIconColor.BLUE
                native.type shouldBe "icon"
            }

            test("should deserialize 'lightgray' to LIGHT_GRAY") {
                val jsonStr = """{"type":"icon","icon":{"name":"circle","color":"lightgray"}}"""
                val icon = json.decodeFromString<Icon>(jsonStr)

                val native = icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.color shouldBe NativeIconColor.LIGHT_GRAY
            }

            test("should deserialize NativeIcon without color") {
                val jsonStr = """{"type":"icon","icon":{"name":"pizza"}}"""
                val icon = json.decodeFromString<Icon>(jsonStr)

                val native = icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                native.icon.color.shouldBeNull()
            }

            test("should round-trip NativeIcon with color") {
                val original = Icon.NativeIcon(NativeIconObject(name = "star", color = NativeIconColor.RED))
                val encoded = json.encodeToString(original)
                val decoded = json.decodeFromString<Icon>(encoded)

                val native = decoded.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "star"
                native.icon.color shouldBe NativeIconColor.RED
            }

            test("secondary constructor sets type to 'icon'") {
                val icon = Icon.NativeIcon(NativeIconObject(name = "bolt"))
                icon.type shouldBe "icon"
            }
        }

        context("nativeIcon() top-level DSL helper") {
            test("should return Icon.NativeIcon with name and color") {
                val icon = nativeIcon("pizza", NativeIconColor.BLUE)

                val native = icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                native.icon.color shouldBe NativeIconColor.BLUE
            }

            test("should return Icon.NativeIcon with name only (no color)") {
                val icon = nativeIcon("pizza")

                val native = icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                native.icon.color.shouldBeNull()
            }
        }

        context("icon.native() in CreatePageRequestBuilder") {
            test("should set a native icon with name and color") {
                val request =
                    createPageRequest {
                        parent.page("test-page-id")
                        icon.native("pizza", NativeIconColor.BLUE)
                    }

                val native = request.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                native.icon.color shouldBe NativeIconColor.BLUE
            }

            test("should set a native icon with name only") {
                val request =
                    createPageRequest {
                        parent.page("test-page-id")
                        icon.native("bolt")
                    }

                val native = request.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "bolt"
                native.icon.color.shouldBeNull()
            }
        }

        context("icon.native() in UpdatePageRequestBuilder") {
            test("should set a native icon with name and color") {
                val request =
                    updatePageRequest {
                        icon.native("pizza", NativeIconColor.GREEN)
                    }

                val native = request.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "pizza"
                native.icon.color shouldBe NativeIconColor.GREEN
            }
        }

        context("icon.native() in DatabaseRequestBuilder") {
            test("should set a native icon with name and color") {
                val request =
                    databaseRequest {
                        parent.page("test-page-id")
                        title("My DB")
                        properties {
                            title("Name")
                        }
                        icon.native("database", NativeIconColor.PURPLE)
                    }

                val native = request.icon.shouldBeInstanceOf<Icon.NativeIcon>()
                native.icon.name shouldBe "database"
                native.icon.color shouldBe NativeIconColor.PURPLE
            }
        }

        context("fixture-based deserialization") {
            test("other Icon types still deserialize correctly alongside NativeIcon") {
                val emojiJson = """{"type":"emoji","emoji":"🥑"}"""
                val externalJson = """{"type":"external","external":{"url":"https://example.com/icon.png"}}"""

                json.decodeFromString<Icon>(emojiJson).shouldBeInstanceOf<Icon.Emoji>()
                json.decodeFromString<Icon>(externalJson).shouldBeInstanceOf<Icon.External>()
            }
        }

        context("TestFixtures custom emoji list contains CustomEmojiObject correctly") {
            test("should load the custom emoji list fixture") {
                val fixture = TestFixtures.CustomEmojis.listCustomEmojis()
                fixture.toString().contains("bufo") shouldBe true
            }
        }
    })
