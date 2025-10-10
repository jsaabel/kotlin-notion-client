package it.saabel.kotlinnotionclient

fun main() {
    println("Notion Kotlin Client Library")
    println("This is a library JAR - use NotionClient class to interact with Notion API")
    println("Version: 0.0.1-SNAPSHOT")

    // Example usage info
    println("\nExample usage:")
    println("val client = NotionClient.create(\"your-api-token\")")
    println("val page = client.pages.retrieve(\"page-id\")")
}
