package unit.datasources

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.datasources.UpdateDataSourceRequest
import it.saabel.kotlinnotionclient.models.datasources.updateDataSourceRequest
import it.saabel.kotlinnotionclient.serialization.NotionJson

/**
 * Guards the data source icon's removal payload.
 *
 * `PATCH /v1/data_sources` clears an icon when the request carries `"icon": null`, and this is
 * the removal that matters: Notion's UI renders the data source, not the database container,
 * whose icon cannot be cleared at all. Both established live — see
 * `IconCoverSupportIntegrationTest` for the matrix.
 *
 * The assertion is on the encoded string, using the configuration the client installs. A
 * model-level check cannot see this class of bug: before the sentinels of
 * `docs/adr/0002-explicit-null-payloads.md`, a `null` icon field encoded to `{}` — indistinguishable
 * from a request that never mentioned the icon.
 */
@Tags("Unit")
class UpdateDataSourceRequestBuilderTest :
    DescribeSpec({

        fun encode(request: UpdateDataSourceRequest): String =
            NotionJson.default.encodeToString(UpdateDataSourceRequest.serializer(), request)

        describe("icon removal") {
            it("records the removal sentinel") {
                updateDataSourceRequest { icon.remove() }.icon shouldBe Icon.Removed
            }

            it("encodes as an explicit null") {
                encode(updateDataSourceRequest { icon.remove() }) shouldBe """{"icon":null}"""
            }

            it("leaves an untouched icon out of the payload entirely") {
                encode(updateDataSourceRequest { title("Renamed") }) shouldBe
                    """{"title":[{"type":"text","text":{"content":"Renamed"},""" +
                    """"annotations":{"bold":false,"italic":false,"strikethrough":false,""" +
                    """"underline":false,"code":false,"color":"default"},"plain_text":"Renamed"}]}"""
            }

            it("is last-call-wins against a set icon, in both DSL forms") {
                updateDataSourceRequest {
                    icon.emoji("🧪")
                    icon { remove() }
                }.icon shouldBe Icon.Removed

                updateDataSourceRequest {
                    icon.remove()
                    icon.emoji("🧪")
                }.icon shouldBe Icon.Emoji(emoji = "🧪")
            }
        }
    })
