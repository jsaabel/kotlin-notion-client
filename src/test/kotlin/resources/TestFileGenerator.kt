package no.saabelit.kotlinnotionclient.resources

import java.io.File
import java.util.Base64

/**
 * Utility to generate test files for integration testing.
 * Run this to create the test files in the test resources directory.
 */
object TestFileGenerator {
    // 1x1 pixel transparent PNG (smallest valid PNG)
    private const val TINY_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="

    // Minimal PDF with just header
    private const val MINIMAL_PDF = """%PDF-1.4
1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj
xref
0 4
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
trailer<</Size 4/Root 1 0 R>>
startxref
203
%%EOF"""

    fun generateTestFiles() {
        val testFilesDir = File("src/test/resources/test-files")
        testFilesDir.mkdirs()

        // Create tiny PNG
        val pngFile = File(testFilesDir, "test-image.png")
        pngFile.writeBytes(Base64.getDecoder().decode(TINY_PNG_BASE64))
        println("Created ${pngFile.absolutePath} (${pngFile.length()} bytes)")

        // Create minimal PDF
        val pdfFile = File(testFilesDir, "test-document.pdf")
        pdfFile.writeText(MINIMAL_PDF)
        println("Created ${pdfFile.absolutePath} (${pdfFile.length()} bytes)")

        // Create simple text file
        val textFile = File(testFilesDir, "test-file.txt")
        textFile.writeText("Hello, Notion!")
        println("Created ${textFile.absolutePath} (${textFile.length()} bytes)")

        // Create small CSV file
        val csvFile = File(testFilesDir, "test-data.csv")
        csvFile.writeText("name,value\ntest,123\n")
        println("Created ${csvFile.absolutePath} (${csvFile.length()} bytes)")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        generateTestFiles()
    }
}
