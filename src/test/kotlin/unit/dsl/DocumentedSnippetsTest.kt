package unit.dsl

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import it.saabel.kotlinnotionclient.models.base.Icon
import it.saabel.kotlinnotionclient.models.base.Parent
import it.saabel.kotlinnotionclient.models.comments.createCommentRequest
import it.saabel.kotlinnotionclient.models.databases.databaseRequest
import it.saabel.kotlinnotionclient.models.databases.updateDatabaseRequest
import it.saabel.kotlinnotionclient.models.datasources.createDataSourceRequest
import it.saabel.kotlinnotionclient.models.pages.PageCover
import it.saabel.kotlinnotionclient.models.pages.PagePosition
import it.saabel.kotlinnotionclient.models.pages.PageTemplate
import it.saabel.kotlinnotionclient.models.pages.createPageRequest
import it.saabel.kotlinnotionclient.models.pages.updatePageRequest
import it.saabel.kotlinnotionclient.models.views.ViewType
import it.saabel.kotlinnotionclient.models.views.createViewRequest

/**
 * Compiles the DSL forms the documentation teaches.
 *
 * Every snippet in `README.md`, `QUICKSTART.md`, `docs/` and KDoc is prose — it is never
 * compiled, so it drifts silently. Twenty snippets across the repo had drifted into
 * shapes that did not compile at all (#81) before anyone noticed. This spec holds the
 * documented conventions as real code so that a builder change which would falsify a
 * README snippet fails the build instead of the reader.
 *
 * It is not an exhaustive mirror of the docs; it covers the conventions in
 * `docs/dsl-conventions.md`. Add a case when you add a builder.
 */
@Tags("Unit")
class DocumentedSnippetsTest :
    DescribeSpec({

        describe("single-value nesting — the documented receiver form") {
            it("compiles for every nested builder on createPageRequest") {
                val request =
                    createPageRequest {
                        parent.dataSource("data-source-id")
                        title("My Project")
                        icon.emoji("📄")
                        cover.external("https://example.com/cover.png")
                        template.none()
                        position.pageEnd()
                    }

                request.parent shouldBe Parent.DataSourceParent(dataSourceId = "data-source-id")
                request.icon shouldBe Icon.Emoji(emoji = "📄")
                request.template shouldBe PageTemplate.None
                request.position shouldBe PagePosition.PageEnd
            }

            it("compiles for updatePageRequest, databaseRequest and the data source builders") {
                updatePageRequest {
                    icon.emoji("✅")
                    cover.remove()
                    template.default()
                }.icon shouldBe Icon.Emoji(emoji = "✅")

                databaseRequest {
                    parent.page("parent-page-id")
                    title("My Database")
                    icon.emoji("🗂️")
                    cover.external("https://example.com/cover.png")
                    properties { title("Name") }
                }.icon shouldBe Icon.Emoji(emoji = "🗂️")

                createDataSourceRequest {
                    parent.database("existing-database-id")
                    title("Projects Data Source")
                    properties { title("Project Name") }
                }.parent shouldBe Parent.DatabaseParent(databaseId = "existing-database-id")

                updateDatabaseRequest {
                    title("Q3 Planning")
                    icon.emoji("📊")
                    parent.page("new-parent-page-id")
                    inline(true)
                }.parent shouldBe Parent.PageParent(pageId = "new-parent-page-id")
            }
        }

        describe("single-value nesting — the lambda form") {
            it("is accepted everywhere the receiver form is, and agrees with it") {
                val receiverForm =
                    createPageRequest {
                        parent.dataSource("data-source-id")
                        icon.emoji("📄")
                        cover.external("https://example.com/cover.png")
                        template.none()
                        position.pageEnd()
                    }

                val lambdaForm =
                    createPageRequest {
                        parent { dataSource("data-source-id") }
                        icon { emoji("📄") }
                        cover { external("https://example.com/cover.png") }
                        template { none() }
                        position { pageEnd() }
                    }

                lambdaForm shouldBe receiverForm
            }

            it("is last-call-wins across both forms") {
                val request =
                    createPageRequest {
                        parent.page("page-id")
                        parent { dataSource("data-source-id") }
                        icon { emoji("📄") }
                        icon.emoji("✅")
                    }

                request.parent shouldBe Parent.DataSourceParent(dataSourceId = "data-source-id")
                request.icon shouldBe Icon.Emoji(emoji = "✅")
            }

            it("covers the remaining single-value builders") {
                updatePageRequest {
                    icon { emoji("✅") }
                    cover { remove() }
                    template { default() }
                }.cover shouldBe PageCover.Removed

                databaseRequest {
                    parent { page("parent-page-id") }
                    title("My Database")
                    icon { emoji("🗂️") }
                    cover { external("https://example.com/cover.png") }
                    properties { title("Name") }
                }.parent shouldBe Parent.PageParent(pageId = "parent-page-id")

                createCommentRequest {
                    parent { page("page-id") }
                    content { text("A comment") }
                }.parent shouldBe Parent.PageParent(pageId = "page-id")

                createDataSourceRequest {
                    parent { database("existing-database-id") }
                    title("Projects")
                    properties { title("Project Name") }
                }.parent shouldBe Parent.DatabaseParent(databaseId = "existing-database-id")

                updateDatabaseRequest {
                    parent { page("new-parent-page-id") }
                    icon { emoji("📊") }
                    cover { remove() }
                }.cover shouldBe PageCover.Removed
            }
        }

        describe("parent addressing — parent.<object>(id) everywhere") {
            it("addresses a comment parent by object, not by id") {
                createCommentRequest {
                    parent.page("page-id")
                    content { text("On a page") }
                }.parent shouldBe Parent.PageParent(pageId = "page-id")

                createCommentRequest {
                    parent.block("block-id")
                    content { text("On a block") }
                }.parent shouldBe Parent.BlockParent(blockId = "block-id")
            }

            it("addresses a view's container through parent, and its source flat") {
                val onDatabase =
                    createViewRequest {
                        dataSourceId("ds-id")
                        parent.database("db-id")
                        name("Kanban")
                        type(ViewType.BOARD)
                    }
                onDatabase.dataSourceId shouldBe "ds-id"
                onDatabase.databaseId shouldBe "db-id"

                createViewRequest {
                    dataSourceId("ds-id")
                    parent.dashboard("dashboard-view-id")
                    name("Widget")
                    type(ViewType.BOARD)
                }.viewId shouldBe "dashboard-view-id"

                createViewRequest {
                    dataSourceId("ds-id")
                    parent { newDatabase(pageId = "page-id", afterBlockId = "block-id") }
                    name("Linked")
                    type(ViewType.TABLE)
                }.createDatabase?.parent?.pageId shouldBe "page-id"
            }
        }
    })
