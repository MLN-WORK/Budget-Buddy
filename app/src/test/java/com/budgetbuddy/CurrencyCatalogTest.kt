package com.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): CurrencyCatalogTest
 * Parent class: Any; child classes: none; related classes: CurrencyCatalog, JUnit, and the application code under test.
 * What the class does: Verifies the CurrencyCatalog behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class CurrencyCatalogTest {
    @Test
    fun euroIsTheDefaultCurrency() {
        assertEquals("EUR", CurrencyCatalog.DEFAULT_CODE)
        assertEquals("€", CurrencyCatalog.DEFAULT_SYMBOL)
        assertEquals("EUR", CurrencyCatalog.findByCode(CurrencyCatalog.DEFAULT_CODE)?.code)
    }

    @Test
    fun currenciesCanBeSearchedByNameCodeOrSymbol() {
        assertEquals("ZAR", CurrencyCatalog.search("south africa").single().code)
        assertEquals("EUR", CurrencyCatalog.search("eur").single().code)
        assertTrue(CurrencyCatalog.search("$").any { it.code == "USD" })
        assertTrue(CurrencyCatalog.options.size >= 178)
    }

    @Test
    fun currenciesAreAlphabeticalByName() {
        assertEquals(
            CurrencyCatalog.options.map { it.name.lowercase() }.sorted(),
            CurrencyCatalog.options.map { it.name.lowercase() }
        )
    }

    @Test
    fun catalogueContainsEveryCurrentIso4217CodeWithoutDuplicates() {
        val catalogueCodes = CurrencyCatalog.options.map { it.code }

        assertEquals(178, CurrencyCatalog.currentIsoCodes.size)
        assertTrue(catalogueCodes.containsAll(CurrencyCatalog.currentIsoCodes))
        assertEquals(catalogueCodes.distinct().size, catalogueCodes.size)
        assertTrue(CurrencyCatalog.options.all { it.name.isNotBlank() && it.symbol.isNotBlank() })
        listOf("AFN", "BWP", "XAD", "XCG", "ZWG").forEach { code ->
            assertNotNull(code, CurrencyCatalog.findByCode(code))
        }
    }
}
// End of class: CurrencyCatalogTest
