@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Color values for rich text annotations and block content.
 * Supports both foreground and background color variants.
 */
@Serializable
enum class Color {
    @SerialName("default")
    DEFAULT,

    @SerialName("gray")
    GRAY,

    @SerialName("brown")
    BROWN,

    @SerialName("orange")
    ORANGE,

    @SerialName("yellow")
    YELLOW,

    @SerialName("green")
    GREEN,

    @SerialName("blue")
    BLUE,

    @SerialName("purple")
    PURPLE,

    @SerialName("pink")
    PINK,

    @SerialName("red")
    RED,

    @SerialName("gray_background")
    GRAY_BACKGROUND,

    @SerialName("brown_background")
    BROWN_BACKGROUND,

    @SerialName("orange_background")
    ORANGE_BACKGROUND,

    @SerialName("yellow_background")
    YELLOW_BACKGROUND,

    @SerialName("green_background")
    GREEN_BACKGROUND,

    @SerialName("blue_background")
    BLUE_BACKGROUND,

    @SerialName("purple_background")
    PURPLE_BACKGROUND,

    @SerialName("pink_background")
    PINK_BACKGROUND,

    @SerialName("red_background")
    RED_BACKGROUND,
}

/**
 * Color values for select and multi-select database property options.
 */
@Serializable
enum class SelectOptionColor {
    @SerialName("blue")
    BLUE,

    @SerialName("brown")
    BROWN,

    @SerialName("default")
    DEFAULT,

    @SerialName("gray")
    GRAY,

    @SerialName("green")
    GREEN,

    @SerialName("orange")
    ORANGE,

    @SerialName("pink")
    PINK,

    @SerialName("purple")
    PURPLE,

    @SerialName("red")
    RED,

    @SerialName("yellow")
    YELLOW,
}

/**
 * File source type for media blocks and file references.
 */
@Serializable
enum class FileType {
    @SerialName("external")
    EXTERNAL,

    @SerialName("file")
    FILE,

    @SerialName("file_upload")
    FILE_UPLOAD,
}

/**
 * Icon type for callouts and pages.
 */
@Serializable
enum class IconType {
    @SerialName("emoji")
    EMOJI,

    @SerialName("external")
    EXTERNAL,

    @SerialName("file")
    FILE,
}

/**
 * Programming language for code blocks.
 * This is not exhaustive - Notion supports many languages.
 * Using sealed class to allow for custom values.
 */
@Serializable
sealed class CodeLanguage {
    abstract val value: String

    @Serializable
    @SerialName("abap")
    object Abap : CodeLanguage() {
        override val value = "abap"
    }

    @Serializable
    @SerialName("arduino")
    object Arduino : CodeLanguage() {
        override val value = "arduino"
    }

    @Serializable
    @SerialName("bash")
    object Bash : CodeLanguage() {
        override val value = "bash"
    }

    @Serializable
    @SerialName("basic")
    object Basic : CodeLanguage() {
        override val value = "basic"
    }

    @Serializable
    @SerialName("c")
    object C : CodeLanguage() {
        override val value = "c"
    }

    @Serializable
    @SerialName("clojure")
    object Clojure : CodeLanguage() {
        override val value = "clojure"
    }

    @Serializable
    @SerialName("coffeescript")
    object CoffeeScript : CodeLanguage() {
        override val value = "coffeescript"
    }

    @Serializable
    @SerialName("c++")
    object CPlusPlus : CodeLanguage() {
        override val value = "c++"
    }

    @Serializable
    @SerialName("c#")
    object CSharp : CodeLanguage() {
        override val value = "c#"
    }

    @Serializable
    @SerialName("css")
    object CSS : CodeLanguage() {
        override val value = "css"
    }

    @Serializable
    @SerialName("dart")
    object Dart : CodeLanguage() {
        override val value = "dart"
    }

    @Serializable
    @SerialName("diff")
    object Diff : CodeLanguage() {
        override val value = "diff"
    }

    @Serializable
    @SerialName("docker")
    object Docker : CodeLanguage() {
        override val value = "docker"
    }

    @Serializable
    @SerialName("elixir")
    object Elixir : CodeLanguage() {
        override val value = "elixir"
    }

    @Serializable
    @SerialName("elm")
    object Elm : CodeLanguage() {
        override val value = "elm"
    }

    @Serializable
    @SerialName("erlang")
    object Erlang : CodeLanguage() {
        override val value = "erlang"
    }

    @Serializable
    @SerialName("flow")
    object Flow : CodeLanguage() {
        override val value = "flow"
    }

    @Serializable
    @SerialName("fortran")
    object Fortran : CodeLanguage() {
        override val value = "fortran"
    }

    @Serializable
    @SerialName("f#")
    object FSharp : CodeLanguage() {
        override val value = "f#"
    }

    @Serializable
    @SerialName("gherkin")
    object Gherkin : CodeLanguage() {
        override val value = "gherkin"
    }

    @Serializable
    @SerialName("glsl")
    object GLSL : CodeLanguage() {
        override val value = "glsl"
    }

    @Serializable
    @SerialName("go")
    object Go : CodeLanguage() {
        override val value = "go"
    }

    @Serializable
    @SerialName("graphql")
    object GraphQL : CodeLanguage() {
        override val value = "graphql"
    }

    @Serializable
    @SerialName("groovy")
    object Groovy : CodeLanguage() {
        override val value = "groovy"
    }

    @Serializable
    @SerialName("haskell")
    object Haskell : CodeLanguage() {
        override val value = "haskell"
    }

    @Serializable
    @SerialName("html")
    object HTML : CodeLanguage() {
        override val value = "html"
    }

    @Serializable
    @SerialName("java")
    object Java : CodeLanguage() {
        override val value = "java"
    }

    @Serializable
    @SerialName("javascript")
    object JavaScript : CodeLanguage() {
        override val value = "javascript"
    }

    @Serializable
    @SerialName("json")
    object JSON : CodeLanguage() {
        override val value = "json"
    }

    @Serializable
    @SerialName("julia")
    object Julia : CodeLanguage() {
        override val value = "julia"
    }

    @Serializable
    @SerialName("kotlin")
    object Kotlin : CodeLanguage() {
        override val value = "kotlin"
    }

    @Serializable
    @SerialName("latex")
    object LaTeX : CodeLanguage() {
        override val value = "latex"
    }

    @Serializable
    @SerialName("less")
    object Less : CodeLanguage() {
        override val value = "less"
    }

    @Serializable
    @SerialName("lisp")
    object Lisp : CodeLanguage() {
        override val value = "lisp"
    }

    @Serializable
    @SerialName("livescript")
    object LiveScript : CodeLanguage() {
        override val value = "livescript"
    }

    @Serializable
    @SerialName("lua")
    object Lua : CodeLanguage() {
        override val value = "lua"
    }

    @Serializable
    @SerialName("makefile")
    object Makefile : CodeLanguage() {
        override val value = "makefile"
    }

    @Serializable
    @SerialName("markdown")
    object Markdown : CodeLanguage() {
        override val value = "markdown"
    }

    @Serializable
    @SerialName("markup")
    object Markup : CodeLanguage() {
        override val value = "markup"
    }

    @Serializable
    @SerialName("matlab")
    object MATLAB : CodeLanguage() {
        override val value = "matlab"
    }

    @Serializable
    @SerialName("mermaid")
    object Mermaid : CodeLanguage() {
        override val value = "mermaid"
    }

    @Serializable
    @SerialName("nix")
    object Nix : CodeLanguage() {
        override val value = "nix"
    }

    @Serializable
    @SerialName("objective-c")
    object ObjectiveC : CodeLanguage() {
        override val value = "objective-c"
    }

    @Serializable
    @SerialName("ocaml")
    object OCaml : CodeLanguage() {
        override val value = "ocaml"
    }

    @Serializable
    @SerialName("pascal")
    object Pascal : CodeLanguage() {
        override val value = "pascal"
    }

    @Serializable
    @SerialName("perl")
    object Perl : CodeLanguage() {
        override val value = "perl"
    }

    @Serializable
    @SerialName("php")
    object PHP : CodeLanguage() {
        override val value = "php"
    }

    @Serializable
    @SerialName("plain text")
    object PlainText : CodeLanguage() {
        override val value = "plain text"
    }

    @Serializable
    @SerialName("powershell")
    object PowerShell : CodeLanguage() {
        override val value = "powershell"
    }

    @Serializable
    @SerialName("prolog")
    object Prolog : CodeLanguage() {
        override val value = "prolog"
    }

    @Serializable
    @SerialName("protobuf")
    object Protobuf : CodeLanguage() {
        override val value = "protobuf"
    }

    @Serializable
    @SerialName("python")
    object Python : CodeLanguage() {
        override val value = "python"
    }

    @Serializable
    @SerialName("r")
    object R : CodeLanguage() {
        override val value = "r"
    }

    @Serializable
    @SerialName("reason")
    object Reason : CodeLanguage() {
        override val value = "reason"
    }

    @Serializable
    @SerialName("ruby")
    object Ruby : CodeLanguage() {
        override val value = "ruby"
    }

    @Serializable
    @SerialName("rust")
    object Rust : CodeLanguage() {
        override val value = "rust"
    }

    @Serializable
    @SerialName("sass")
    object Sass : CodeLanguage() {
        override val value = "sass"
    }

    @Serializable
    @SerialName("scala")
    object Scala : CodeLanguage() {
        override val value = "scala"
    }

    @Serializable
    @SerialName("scheme")
    object Scheme : CodeLanguage() {
        override val value = "scheme"
    }

    @Serializable
    @SerialName("scss")
    object SCSS : CodeLanguage() {
        override val value = "scss"
    }

    @Serializable
    @SerialName("shell")
    object Shell : CodeLanguage() {
        override val value = "shell"
    }

    @Serializable
    @SerialName("sql")
    object SQL : CodeLanguage() {
        override val value = "sql"
    }

    @Serializable
    @SerialName("swift")
    object Swift : CodeLanguage() {
        override val value = "swift"
    }

    @Serializable
    @SerialName("typescript")
    object TypeScript : CodeLanguage() {
        override val value = "typescript"
    }

    @Serializable
    @SerialName("vb.net")
    object VBNet : CodeLanguage() {
        override val value = "vb.net"
    }

    @Serializable
    @SerialName("verilog")
    object Verilog : CodeLanguage() {
        override val value = "verilog"
    }

    @Serializable
    @SerialName("vhdl")
    object VHDL : CodeLanguage() {
        override val value = "vhdl"
    }

    @Serializable
    @SerialName("visual basic")
    object VisualBasic : CodeLanguage() {
        override val value = "visual basic"
    }

    @Serializable
    @SerialName("webassembly")
    object WebAssembly : CodeLanguage() {
        override val value = "webassembly"
    }

    @Serializable
    @SerialName("xml")
    object XML : CodeLanguage() {
        override val value = "xml"
    }

    @Serializable
    @SerialName("yaml")
    object YAML : CodeLanguage() {
        override val value = "yaml"
    }

    @Serializable
    @SerialName("java/c/c++/c#")
    object JavaCFamily : CodeLanguage() {
        override val value = "java/c/c++/c#"
    }

    /**
     * Custom language not in the predefined list.
     */
    @Serializable
    data class Custom(
        override val value: String,
    ) : CodeLanguage()
}
