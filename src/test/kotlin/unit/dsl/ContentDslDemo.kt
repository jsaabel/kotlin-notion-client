package unit.dsl

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.saabelit.kotlinnotionclient.models.blocks.pageContent

/**
 * Demonstration of the new Content Creation DSL.
 *
 * This test showcases the clean, intuitive API for building
 * structured content that matches klibnotion's capabilities
 * while leveraging our superior type safety and testing.
 */
@Tags("Unit")
class ContentDslDemo :
    StringSpec(
        {
            "showcases clean content creation DSL" {
                // This demonstrates the power and elegance of our new DSL
                val content =
                    pageContent {
                        // Main heading
                        heading1("🚀 Kotlin Notion Client - Content Creation DSL")

                        // Introduction
                        paragraph(
                            "Welcome to our advanced content creation system! This DSL provides a clean, type-safe way to build rich Notion content.",
                        )

                        divider()

                        // Getting started section
                        heading2("📚 Getting Started")
                        paragraph("Building content is now as simple as:") {
                            code(
                                language = "kotlin",
                                code =
                                    """
                                    val content = pageContent {
                                        heading1("My Page Title")
                                        paragraph("Some introductory text")
                                        
                                        bullet("First point")
                                        bullet("Second point") {
                                            paragraph("With nested content!")
                                        }
                                    }
                                    """.trimIndent(),
                            )
                        }

                        // Feature showcase
                        heading2("✨ Key Features")

                        bullet("🔒 **Type Safety** - Leverages Kotlin's type system") {
                            paragraph("All blocks are strongly typed and validated at compile time")
                            code(language = "kotlin", code = "pageContent { paragraph(\"Type-safe!\") }")
                        }

                        bullet("🎯 **Intuitive API** - Clean, readable syntax") {
                            paragraph("No more verbose JSON construction")
                            quote("The best APIs feel like natural language")
                        }

                        bullet("🧪 **Comprehensive Testing** - Built for reliability") {
                            paragraph("Every feature is thoroughly tested with real API samples")
                            toDo("Run all tests", checked = true)
                            toDo("Verify against live API", checked = true)
                            toDo("Document edge cases", checked = false)
                        }

                        bullet("🔄 **Nested Content** - Supports complex hierarchies") {
                            toggle("Click to see nested content example") {
                                paragraph("This content is nested inside a toggle!")

                                heading3("Even deeper nesting")
                                number("Step 1: Create parent block") {
                                    bullet("Define the structure")
                                    bullet("Add child content") {
                                        paragraph("As deep as you need to go!")
                                    }
                                }

                                number("Step 2: Build with DSL")
                                number("Step 3: Enjoy the results! 🎉")
                            }
                        }

                        divider()

                        // Comparison section
                        heading2("🆚 Comparison with Alternatives")

                        callout("💡", "Our implementation improves upon existing solutions") {
                            paragraph("**klibnotion**: Great DSL, but complex architecture and limited testing")
                            paragraph("**notion-sdk-kotlin**: Simple but read-only, minimal validation")
                            paragraph("**Python clients**: Functional but lack type safety")
                            paragraph("**Our solution**: Best of all worlds - clean DSL + type safety + comprehensive testing")
                        }

                        // Advanced examples
                        heading2("🔧 Advanced Examples")

                        toggle("Complex Documentation Structure") {
                            heading3("API Reference")
                            paragraph("Complete API documentation with examples")

                            heading3("Getting Started Guide")
                            number("Install the library") {
                                code(language = "kotlin", code = "implementation(\"com.notion:kotlin-client:1.0.0\")")
                            }
                            number("Configure your client") {
                                code(
                                    language = "kotlin",
                                    code =
                                        """
                                        val client = NotionClient(
                                            config = NotionConfig(
                                                apiToken = "your-token",
                                                logRequests = true
                                            )
                                        )
                                        """.trimIndent(),
                                )
                            }
                            number("Start building content!") {
                                paragraph("Use our DSL to create rich, nested content structures")
                            }

                            heading3("Best Practices")
                            bullet("Always validate your content before sending")
                            bullet("Use meaningful names for toggle sections")
                            bullet("Keep nesting levels reasonable for readability")
                            bullet("Leverage type safety to catch errors early")
                        }

                        // Footer
                        divider()

                        callout("🎉", "Ready to build amazing Notion content?") {
                            paragraph("This DSL makes it easier than ever to create rich, structured content for your Notion workspace.")
                            paragraph("Happy building! 🚀")
                        }
                    }

                // Verify we created a substantial piece of content
                println("✅ Created ${content.size} top-level blocks with our DSL!")
                // We expect 17 top-level blocks based on the structure
                content.size shouldBe 17

                // This content demonstrates:
                // 1. All major block types (headings, paragraphs, lists, code, callouts, etc.)
                // 2. Nested content structures
                // 3. Complex hierarchies
                // 4. Real-world documentation patterns
                // 5. Clean, readable syntax that's easy to maintain
            }
        },
    )
