package unit.base

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import it.saabel.kotlinnotionclient.models.base.Parent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Tags("Unit")
class ParentAgentIdTest :
    FunSpec({
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }

        context("Parent.AgentParent serialization") {
            test("deserializes an agent_id parent into Parent.AgentParent") {
                val raw =
                    """
                    {
                      "type": "agent_id",
                      "agent_id": "11111111-2222-3333-4444-555555555555"
                    }
                    """.trimIndent()

                val parent = json.decodeFromString<Parent>(raw)

                val agent = parent.shouldBeInstanceOf<Parent.AgentParent>()
                agent.type shouldBe "agent_id"
                agent.agentId shouldBe "11111111-2222-3333-4444-555555555555"
                agent.id shouldBe "11111111-2222-3333-4444-555555555555"
            }

            test("re-serializes Parent.AgentParent back to the API shape") {
                val agent = Parent.AgentParent(agentId = "11111111-2222-3333-4444-555555555555")

                val encoded = json.encodeToString<Parent>(agent)
                val obj = json.parseToJsonElement(encoded) as JsonObject

                obj["type"]?.jsonPrimitive?.content shouldBe "agent_id"
                obj["agent_id"]?.jsonPrimitive?.content shouldBe "11111111-2222-3333-4444-555555555555"
            }

            test("round-trips through Parent without losing information") {
                val raw =
                    """
                    {
                      "type": "agent_id",
                      "agent_id": "abcdef00-0000-0000-0000-000000000001"
                    }
                    """.trimIndent()

                val decoded = json.decodeFromString<Parent>(raw)
                val reencoded = json.encodeToString<Parent>(decoded)
                val redecoded = json.decodeFromString<Parent>(reencoded)

                redecoded shouldBe decoded
            }
        }
    })
