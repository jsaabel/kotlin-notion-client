package unit.databases

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import it.saabel.kotlinnotionclient.models.base.ExternalFile
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.databases.UpdateDatabaseRequest
import it.saabel.kotlinnotionclient.models.databases.updateDatabaseRequest
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.serialization.NotionJson

/**
 * Covers the container-attribute half of the 2025-09-03 split: `parent`, `title`, `icon`,
 * `cover`, `is_inline` and `in_trash` are reachable only through the Update Database endpoint.
 *
 * The JSON-shape assertions use [NotionJson] — the configuration the client actually installs —
 * rather than the model, because the two places this builder can go silently wrong are both
 * invisible at model level: a removal that never reaches the wire, and an untouched field that
 * does. See `docs/adr/0002-explicit-null-payloads.md`.
 */
@Tags("Unit")
class UpdateDatabaseRequestBuilderTest :
    DescribeSpec({

        fun encode(request: UpdateDatabaseRequest): String = NotionJson.default.encodeToString(UpdateDatabaseRequest.serializer(), request)

        describe("container attributes") {
            it("sets the title from plain text") {
                val request = updateDatabaseRequest { title("Q3 Planning") }

                request.title?.single()?.plainText shouldBe "Q3 Planning"
                encode(request) shouldContain """"content":"Q3 Planning""""
            }

            it("moves the database to another page") {
                updateDatabaseRequest {
                    parent.page("new-parent-page-id")
                }.parent shouldBe Parent.PageParent(pageId = "new-parent-page-id")
            }

            it("moves the database to a block or to the workspace") {
                updateDatabaseRequest { parent.block("block-id") }.parent shouldBe
                    Parent.BlockParent(blockId = "block-id")

                updateDatabaseRequest { parent.workspace() }.parent shouldBe Parent.WorkspaceParent
            }

            it("toggles inline rendering") {
                updateDatabaseRequest { inline() }.isInline shouldBe true
                updateDatabaseRequest { inline(false) }.isInline shouldBe false
            }

            it("trashes and restores") {
                updateDatabaseRequest { trash() }.inTrash shouldBe true
                updateDatabaseRequest { trash(false) }.inTrash shouldBe false
                updateDatabaseRequest { restore() }.inTrash shouldBe false
            }

            it("sets an emoji, external and native icon") {
                updateDatabaseRequest { icon.emoji("📊") }.icon shouldBe Icon.Emoji(emoji = "📊")

                updateDatabaseRequest { icon.external("https://example.com/i.png") }.icon shouldBe
                    Icon.External(external = ExternalFile(url = "https://example.com/i.png"))

                updateDatabaseRequest { icon.native("pizza") }.icon shouldBe
                    Icon.NativeIcon(
                        it.saabel.kotlinnotionclient.models.base
                            .NativeIconObject(name = "pizza", color = null),
                    )
            }

            it("sets an external cover") {
                updateDatabaseRequest { cover.external("https://example.com/c.png") }.cover shouldBe
                    PageCover.External(external = ExternalFile(url = "https://example.com/c.png"))
            }

            it("accepts the lambda form for every single-value builder") {
                val receiverForm =
                    updateDatabaseRequest {
                        parent.page("page-id")
                        icon.emoji("📊")
                        cover.external("https://example.com/c.png")
                    }

                val lambdaForm =
                    updateDatabaseRequest {
                        parent { page("page-id") }
                        icon { emoji("📊") }
                        cover { external("https://example.com/c.png") }
                    }

                lambdaForm shouldBe receiverForm
            }

            it("is last-call-wins across both forms") {
                updateDatabaseRequest {
                    icon { emoji("📊") }
                    icon.emoji("✅")
                }.icon shouldBe Icon.Emoji(emoji = "✅")
            }
        }

        describe("what reaches the wire") {
            it("sends only the attributes the block names") {
                encode(updateDatabaseRequest { inline(true) }) shouldBe """{"is_inline":true}"""
            }

            it("omits an untouched icon and cover entirely") {
                encode(updateDatabaseRequest { trash() }) shouldBe """{"in_trash":true}"""
            }

            it("sends icon.remove() as an explicit null") {
                encode(updateDatabaseRequest { icon.remove() }) shouldBe """{"icon":null}"""
            }

            it("sends cover.remove() as an explicit null") {
                encode(updateDatabaseRequest { cover.remove() }) shouldBe """{"cover":null}"""
            }

            it("carries a parent move as the typed parent shape") {
                encode(updateDatabaseRequest { parent.page("page-id") }) shouldBe
                    """{"parent":{"page_id":"page-id","type":"page_id"}}"""
            }
        }

        describe("cover is not supported on an inline database") {
            it("rejects a cover set alongside is_inline = true") {
                val error =
                    shouldThrow<IllegalArgumentException> {
                        updateDatabaseRequest {
                            inline(true)
                            cover.external("https://example.com/c.png")
                        }
                    }

                error.message shouldContain "does not support a cover on an inline database"
            }

            it("rejects it whichever order the two calls arrive in") {
                shouldThrow<IllegalArgumentException> {
                    updateDatabaseRequest {
                        cover.external("https://example.com/c.png")
                        inline(true)
                    }
                }
            }

            it("allows removing a cover while going inline") {
                val request =
                    updateDatabaseRequest {
                        inline(true)
                        cover.remove()
                    }

                request.cover shouldBe PageCover.Removed
                request.isInline shouldBe true
            }

            it("allows a cover when the request says nothing about inline") {
                updateDatabaseRequest {
                    cover.external("https://example.com/c.png")
                }.isInline shouldBe null
            }

            it("allows a cover alongside inline(false)") {
                updateDatabaseRequest {
                    inline(false)
                    cover.external("https://example.com/c.png")
                }.isInline shouldBe false
            }
        }
    })
