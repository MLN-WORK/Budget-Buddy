package com.example.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyCatalogTest {
    @Test
    fun euroIsTheDefaultCurrency() {
        assertEquals("EUR", CurrencyCatalog.DEFAULT_CODE)
        assertEquals("€", CurrencyCatalog.DEFAULT_SYMBOL)
        assertEquals("EUR", CurrencyCatalog.options.first().code)
    }

    @Test
    fun currenciesCanBeSearchedByNameCodeOrSymbol() {
        assertEquals("ZAR", CurrencyCatalog.search("south africa").single().code)
        assertEquals("EUR", CurrencyCatalog.search("eur").single().code)
        assertTrue(CurrencyCatalog.search("$").any { it.code == "USD" })
        assertTrue(CurrencyCatalog.options.size >= 40)
    }
}
