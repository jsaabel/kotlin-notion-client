@file:Suppress("unused")

package it.saabel.kotlinnotionclient.models.pages

import it.saabel.kotlinnotionclient.models.base.RichText
import it.saabel.kotlinnotionclient.models.users.User

// /**
// * Extension functions for easier property access on Page objects.
// *
// * These provide type-safe alternatives to manual JSON parsing while preserving
// * the ability to access full rich text objects when needed.
// */

/**
 * Get a typed property from the page properties.
 *
 * Usage:
 * ```kotlin
 * val numberProp = page.getProperty<PageProperty.Number>("Score")
 * val score = numberProp?.number ?: 0.0
 * ```
 */
inline fun <reified T : PageProperty> Page.getProperty(name: String): T? = properties[name] as? T

// ========================================
// Type-specific property accessors
// ========================================

/**
 * Get a number property value directly.
 */
fun Page.getNumberProperty(name: String): Double? = getProperty<PageProperty.Number>(name)?.number

/**
 * Get a checkbox property value directly.
 */
fun Page.getCheckboxProperty(name: String): Boolean? = getProperty<PageProperty.Checkbox>(name)?.checkbox

/**
 * Get a URL property value directly.
 */
fun Page.getUrlProperty(name: String): String? = getProperty<PageProperty.Url>(name)?.url

/**
 * Get an email property value directly.
 */
fun Page.getEmailProperty(name: String): String? = getProperty<PageProperty.Email>(name)?.email

/**
 * Get a phone number property value directly.
 */
fun Page.getPhoneNumberProperty(name: String): String? = getProperty<PageProperty.PhoneNumber>(name)?.phoneNumber

/**
 * Get a unique_id property value (returns the full UniqueIdValue object).
 */
fun Page.getUniqueIdProperty(name: String): UniqueIdValue? = getProperty<PageProperty.UniqueId>(name)?.uniqueId

/**
 * Get a unique_id property as formatted string (e.g., "TASK-123" or "42").
 */
fun Page.getUniqueIdAsString(name: String): String? = getProperty<PageProperty.UniqueId>(name)?.formattedId

/**
 * Get a place property value (returns the full PlaceValue object).
 */
fun Page.getPlaceProperty(name: String): PlaceValue? = getProperty<PageProperty.Place>(name)?.place

/**
 * Get a place property as formatted location string (e.g., "Oslo Airport (60.19, 11.10)").
 */
fun Page.getPlaceAsString(name: String): String? = getProperty<PageProperty.Place>(name)?.formattedLocation

/**
 * Get a select property option (returns the full SelectOption object).
 */
fun Page.getSelectProperty(name: String): SelectOption? = getProperty<PageProperty.Select>(name)?.select

/**
 * Get a select property option name (convenience for just the name).
 */
fun Page.getSelectPropertyName(name: String): String? = getSelectProperty(name)?.name

/**
 * Get multi-select property options (returns full SelectOption objects).
 */
fun Page.getMultiSelectProperty(name: String): List<SelectOption> = getProperty<PageProperty.MultiSelect>(name)?.multiSelect ?: emptyList()

/**
 * Get multi-select property option names (convenience for just the names).
 */
fun Page.getMultiSelectPropertyNames(name: String): List<String> = getMultiSelectProperty(name).map { it.name }

/**
 * Get a date property value directly.
 */
fun Page.getDateProperty(name: String): DateData? = getProperty<PageProperty.Date>(name)?.date

/**
 * Get a people property (returns full User objects).
 */
fun Page.getPeopleProperty(name: String): List<User> = getProperty<PageProperty.People>(name)?.people ?: emptyList()

/**
 * Get a relation property page references.
 */
fun Page.getRelationProperty(name: String): List<PageReference> = getProperty<PageProperty.Relation>(name)?.relation ?: emptyList()

// ========================================
// Rich Text handling (preserves vs simplifies)
// ========================================

/**
 * Get a title property as full RichText objects (preserves formatting, links, etc.).
 */
fun Page.getTitleProperty(name: String): List<RichText>? = getProperty<PageProperty.Title>(name)?.title

/**
 * Get a title property as plain text (loses formatting).
 */
fun Page.getTitleAsPlainText(name: String): String? = getProperty<PageProperty.Title>(name)?.plainText

/**
 * Get a rich text property as full RichText objects (preserves formatting, links, etc.).
 */
fun Page.getRichTextProperty(name: String): List<RichText>? = getProperty<PageProperty.RichTextProperty>(name)?.richText

/**
 * Get a rich text property as plain text (loses formatting).
 */
fun Page.getRichTextAsPlainText(name: String): String? = getProperty<PageProperty.RichTextProperty>(name)?.plainText

// ========================================
// Catch-all plain text extractor
// ========================================

/**
 * Get plain text representation of any property, regardless of its type.
 *
 * This is useful for cases like tests where you just need a string representation
 * without caring about the specific property type.
 *
 * Usage:
 * ```kotlin
 * val title = page.getPlainTextForProperty("Name") // Works for title properties
 * val description = page.getPlainTextForProperty("Description") // Works for rich text
 * val score = page.getPlainTextForProperty("Score") // Works for numbers -> "95.0"
 * val status = page.getPlainTextForProperty("Status") // Works for select -> "In Progress"
 * ```
 */
fun Page.getPlainTextForProperty(name: String): String? {
    val property = properties[name] ?: return null

    return when (property) {
        is PageProperty.Title -> property.plainText
        is PageProperty.RichTextProperty -> property.plainText
        is PageProperty.Number -> property.number?.toString()
        is PageProperty.Checkbox -> property.checkbox.toString()
        is PageProperty.Url -> property.url
        is PageProperty.Email -> property.email
        is PageProperty.PhoneNumber -> property.phoneNumber
        is PageProperty.UniqueId -> property.formattedId
        is PageProperty.Place -> property.formattedLocation
        is PageProperty.Select -> property.select?.name
        is PageProperty.MultiSelect -> property.multiSelect.joinToString(", ") { it.name }
        is PageProperty.Status -> property.status?.name
        is PageProperty.Date -> property.date?.start
        is PageProperty.People -> property.people.joinToString(", ") { it.name ?: it.id }
        is PageProperty.Relation -> "${property.relation.size} relation(s)"
        is PageProperty.Formula -> {
            when (val formula = property.formula) {
                is FormulaResult.StringResult -> formula.string
                is FormulaResult.NumberResult -> formula.number?.toString()
                is FormulaResult.BooleanResult -> formula.boolean?.toString()
                is FormulaResult.DateResult -> formula.date?.start
            }
        }
        is PageProperty.Rollup -> {
            when (val rollup = property.rollup) {
                is RollupResult.NumberResult -> rollup.number?.toString()
                is RollupResult.DateResult -> rollup.date?.start
                is RollupResult.ArrayResult -> "${rollup.array.size} item(s)"
            }
        }
        is PageProperty.CreatedTime -> property.createdTime
        is PageProperty.LastEditedTime -> property.lastEditedTime
        is PageProperty.CreatedBy -> property.createdBy.name ?: property.createdBy.id
        is PageProperty.LastEditedBy -> property.lastEditedBy.name ?: property.lastEditedBy.id
        else -> null
    }
}
