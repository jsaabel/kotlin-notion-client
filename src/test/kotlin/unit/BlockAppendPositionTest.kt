package unit

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.blocks.BlockAppendPosition
import it.saabel.kotlinnotionclient.models.blocks.BlockReference
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Tags("Unit")
class BlockAppendPositionTest :
    FunSpec({
        val json = Json { prettyPrint = false }

        context("BlockAppendPosition serialization") {
            test("Start serializes with type=start") {
                val encoded = json.encodeToString<BlockAppendPosition>(BlockAppendPosition.Start)
                val parsed = Json.parseToJsonElement(encoded).jsonObject
                parsed["type"]?.jsonPrimitive?.content shouldBe "start"
            }

            test("End serializes with type=end") {
                val encoded = json.encodeToString<BlockAppendPosition>(BlockAppendPosition.End)
                val parsed = Json.parseToJsonElement(encoded).jsonObject
                parsed["type"]?.jsonPrimitive?.content shouldBe "end"
            }

            test("AfterBlock serializes with type=after_block and nested block reference") {
                val blockId = "b5d8fd79-1234-1234-1234-123456789abc"
                val position = BlockAppendPosition.AfterBlock(afterBlock = BlockReference(id = blockId))
                val encoded = json.encodeToString<BlockAppendPosition>(position)
                val parsed = Json.parseToJsonElement(encoded).jsonObject
                parsed["type"]?.jsonPrimitive?.content shouldBe "after_block"
                parsed["after_block"]
                    ?.jsonObject
                    ?.get("id")
                    ?.jsonPrimitive
                    ?.content shouldBe blockId
            }
        }

        context("BlockAppendPosition deserialization") {
            test("Deserializes start correctly") {
                val position = Json.decodeFromString<BlockAppendPosition>("""{"type":"start"}""")
                position shouldBe BlockAppendPosition.Start
            }

            test("Deserializes end correctly") {
                val position = Json.decodeFromString<BlockAppendPosition>("""{"type":"end"}""")
                position shouldBe BlockAppendPosition.End
            }

            test("Deserializes after_block with nested block reference correctly") {
                val blockId = "b5d8fd79-1234-1234-1234-123456789abc"
                val position =
                    Json.decodeFromString<BlockAppendPosition>(
                        """{"type":"after_block","after_block":{"id":"$blockId"}}""",
                    )
                position shouldBe BlockAppendPosition.AfterBlock(afterBlock = BlockReference(id = blockId))
            }
        }

        context("BlockReference") {
            test("serializes id field correctly") {
                val ref = BlockReference(id = "abc-123")
                val encoded = json.encodeToString(ref)
                val parsed = Json.parseToJsonElement(encoded).jsonObject
                parsed["id"]?.jsonPrimitive?.content shouldBe "abc-123"
            }
        }
    })
