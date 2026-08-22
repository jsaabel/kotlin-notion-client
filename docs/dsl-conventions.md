# DSL Conventions

The rules every request builder in this library follows. New builders are expected to match
them; the reasoning behind them is in
[ADR 0003](adr/0003-dsl-nesting-and-parent-addressing.md).

## 1. Nesting shape follows arity

**Collection-shaped nesting takes a lambda.** The braces group many calls, so they earn their
place:

```kotlin
notion.pages.create {
    properties {
        title("Name", "My Project")
        select("Status", "In Progress")
    }
    content {
        heading1("Welcome")
        paragraph("Some body text")
    }
}
```

**Single-value nesting takes a receiver property — and also accepts a lambda.** A parent, an
icon, a cover, a template and a position are each set exactly once, so the receiver form is what
the docs use:

```kotlin
notion.pages.create {
    parent.dataSource("data-source-id")
    icon.emoji("📄")
    cover.external("https://example.com/cover.png")
}
```

The lambda form is equivalent, and supported everywhere the receiver form is:

```kotlin
notion.pages.create {
    parent { dataSource("data-source-id") }
    icon { emoji("📄") }
}
```

Both drive the same builder and are last-call-wins — the last call to either wins, whichever
form it used. Prefer the receiver form in new docs and examples; reach for the lambda when a
single-value builder genuinely takes several calls.

**When adding a single-value nested builder, expose both.** A `val` and a same-named `fun`
coexist in Kotlin without a JVM signature clash:

```kotlin
val parent = ParentBuilder()

fun parent(block: ParentBuilder.() -> Unit) {
    parent.block()
}
```

## 2. Parents are always `parent.<object>(id)`

The accessor is named after the **object**, never after its id — `page(id)`, not `pageId(id)`:

| Surface | Parent |
| --- | --- |
| `pages.create` | `parent.page(id)` · `parent.dataSource(id)` · `parent.block(id)` · `parent.workspace()` |
| `databases.create` | `parent.page(id)` · `parent.block(id)` · `parent.workspace()` |
| `databases.update` | `parent.page(id)` · `parent.block(id)` · `parent.workspace()` — **moves** the database |
| `comments.create` | `parent.page(id)` · `parent.block(id)` |
| `dataSources.create` | `parent.database(id)` |
| `views.create` | `parent.database(id, position)` · `parent.dashboard(id, placement)` · `parent.newDatabase(pageId, afterBlockId)` |

### What is *not* a parent

`views.create` also takes `dataSourceId(id)`, and it stays flat on purpose. It names the data
source the view **reads from**, not what the view hangs off — a `CreateViewRequest` carries both
independently. Only the thing a request is attached to belongs under `parent`.

```kotlin
notion.views.create {
    dataSourceId("ds-id")          // what this view reads
    parent.database("db-id")       // where this view lives
    name("Kanban")
    type(ViewType.BOARD)
}
```

## 3. Deprecated spellings

These still compile and behave identically. They carry `@Deprecated` with `ReplaceWith`, so the
IDE will migrate them for you, and they are scheduled for removal at 1.0.

| Deprecated | Use |
| --- | --- |
| `parent.pageId(id)` (comments) | `parent.page(id)` |
| `parent.blockId(id)` (comments) | `parent.block(id)` |
| `databaseId(id)` (data sources) | `parent.database(id)` |
| `database(id, position)` (views) | `parent.database(id, position)` |
| `dashboard(id, placement)` (views) | `parent.dashboard(id, placement)` |
| `createDatabase(pageId, afterBlockId)` (views) | `parent.newDatabase(pageId, afterBlockId)` |

## 4. Documented snippets are compiled

`src/test/kotlin/unit/dsl/DocumentedSnippetsTest.kt` holds the documented forms as real,
compiled code. It is not an exhaustive mirror of the docs — it covers the conventions above, so
that a change to a builder that would falsify a README snippet fails the build instead of the
reader. Add a case to it when you add a builder.
