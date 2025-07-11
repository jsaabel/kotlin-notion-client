import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.saabelit.kotlinnotionclient.TestFixtures
import no.saabelit.kotlinnotionclient.decode
import no.saabelit.kotlinnotionclient.models.pages.*

/**
 * Unit tests for type-safe page property access system.
 *
 * Tests the new PageProperty models and extension functions using official
 * Notion API sample data to ensure compatibility with real-world scenarios.
 */
@Tags("Unit")
class PagePropertyAccessTest :
    StringSpec({

        "Should deserialize official page sample with typed properties" {
            val page: Page = TestFixtures.Pages.retrievePage().decode()

            page.id shouldBe "59833787-2cf9-4fdf-8782-e53db20768a5"
            page.properties.isNotEmpty() shouldBe true

            // Verify expected properties exist
            page.properties.keys shouldContain "Store availability"
            page.properties.keys shouldContain "Food group"
            page.properties.keys shouldContain "Price"
        }

        "Should access number properties with convenience methods" {
            val page: Page = TestFixtures.Pages.retrievePage().decode()

            // Test direct access
            val priceProperty = page.getProperty<PageProperty.Number>("Price")
            priceProperty shouldNotBe null

            // Test convenience method
            val price = page.getNumberProperty("Price")
            price shouldBe 2.5
        }

        "Should access select properties with convenience methods" {
            val page: Page = TestFixtures.Pages.retrievePage().decode()

            // Test direct access
            val foodGroupProperty = page.getProperty<PageProperty.Select>("Food group")
            foodGroupProperty shouldNotBe null

            // Test convenience methods
            val foodGroupName = page.getSelectPropertyName("Food group")
            foodGroupName shouldBe "🥬 Vegetable"
        }

        "Should access multi-select properties with convenience methods" {
            val page: Page = TestFixtures.Pages.retrievePage().decode()

            // Test direct access
            val storeProperty = page.getProperty<PageProperty.MultiSelect>("Store availability")
            storeProperty shouldNotBe null

            // Test convenience methods
            val storeOptions = page.getMultiSelectProperty("Store availability")
            storeOptions.size shouldBe 2

            val storeNames = page.getMultiSelectPropertyNames("Store availability")
            storeNames shouldContain "Gus's Community Market"
            storeNames shouldContain "Rainbow Grocery"
        }

        "Should handle missing properties gracefully" {
            val page: Page = TestFixtures.Pages.retrievePage().decode()

            val missingNumber = page.getNumberProperty("NonexistentProperty")
            missingNumber.shouldBeNull()

            val missingSelect = page.getSelectPropertyName("NonexistentProperty")
            missingSelect.shouldBeNull()

            val missingMultiSelect = page.getMultiSelectProperty("NonexistentProperty")
            missingMultiSelect.shouldBeEmpty()
        }

        "Should provide catch-all plain text extraction" {
            val page: Page = TestFixtures.Pages.retrievePage().decode()

            // Test plain text extraction for different property types
            val priceText = page.getPlainTextForProperty("Price")
            priceText shouldBe "2.5"

            val foodGroupText = page.getPlainTextForProperty("Food group")
            foodGroupText shouldBe "🥬 Vegetable"

            val storeText = page.getPlainTextForProperty("Store availability")
            storeText shouldBe "Gus's Community Market, Rainbow Grocery"

            // Test missing property
            val missingText = page.getPlainTextForProperty("NonexistentProperty")
            missingText.shouldBeNull()
        }

        "Should handle type mismatches gracefully" {
            val page: Page = TestFixtures.Pages.retrievePage().decode()

            // Try to access a select property as a number (should return null)
            val wrongType = page.getProperty<PageProperty.Number>("Food group")
            wrongType.shouldBeNull()

            // Convenience methods should also handle this gracefully
            val wrongNumber = page.getNumberProperty("Food group")
            wrongNumber.shouldBeNull()
        }

        "Should provide correct type information" {
            val page: Page = TestFixtures.Pages.retrievePage().decode()

            val priceProperty = page.getProperty<PageProperty.Number>("Price")
            priceProperty shouldNotBe null
            priceProperty!!.type shouldBe "number"
            priceProperty.id shouldNotBe null

            val selectProperty = page.getProperty<PageProperty.Select>("Food group")
            selectProperty shouldNotBe null
            selectProperty!!.type shouldBe "select"
            selectProperty.id shouldNotBe null
        }

        "Should work with all properties in official API sample" {
            val page: Page = TestFixtures.Pages.retrievePage().decode()

            // Verify all expected properties from the official sample exist
            val expectedProperties =
                setOf(
                    "Store availability",
                    "Food group",
                    "Price",
                    "Responsible Person",
                    "Last ordered",
                    "Cost of next trip",
                    "Recipes",
                    "Description",
                    "In stock",
                    "Number of meals",
                    "Photo",
                    "Name",
                )

            expectedProperties.forEach { propName ->
                page.properties.keys shouldContain propName
            }

            // Verify we can access all property types without crashing
            expectedProperties.forEach { propName ->
                // This should not throw an exception for any property
                val plainText = page.getPlainTextForProperty(propName)
                // Just verify the call succeeds (some might return null, that's fine)
            }
        }
    })
