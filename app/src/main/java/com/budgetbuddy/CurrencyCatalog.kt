package com.budgetbuddy

import java.util.Locale

data class CurrencyOption(
    val name: String,
    val code: String,
    val symbol: String
) {
    val displayLabel: String
        get() = "$name ($code) — $symbol"

    override fun toString(): String = displayLabel
}

/** Offline currency catalogue used by onboarding and Settings. */
object CurrencyCatalog {
    const val DEFAULT_CODE = "EUR"
    const val DEFAULT_SYMBOL = "€"

    val options: List<CurrencyOption> = listOf(
        CurrencyOption("Euro", "EUR", "€"),
        CurrencyOption("South African rand", "ZAR", "R"),
        CurrencyOption("US dollar", "USD", "$"),
        CurrencyOption("British pound", "GBP", "£"),
        CurrencyOption("Japanese yen", "JPY", "¥"),
        CurrencyOption("Chinese yuan", "CNY", "¥"),
        CurrencyOption("Indian rupee", "INR", "₹"),
        CurrencyOption("Brazilian real", "BRL", "R$"),
        CurrencyOption("Nigerian naira", "NGN", "₦"),
        CurrencyOption("Canadian dollar", "CAD", "C$"),
        CurrencyOption("Australian dollar", "AUD", "A$"),
        CurrencyOption("New Zealand dollar", "NZD", "NZ$"),
        CurrencyOption("Swiss franc", "CHF", "CHF"),
        CurrencyOption("Swedish krona", "SEK", "kr"),
        CurrencyOption("Norwegian krone", "NOK", "kr"),
        CurrencyOption("Danish krone", "DKK", "kr"),
        CurrencyOption("Polish złoty", "PLN", "zł"),
        CurrencyOption("Czech koruna", "CZK", "Kč"),
        CurrencyOption("Hungarian forint", "HUF", "Ft"),
        CurrencyOption("Romanian leu", "RON", "lei"),
        CurrencyOption("Bulgarian lev", "BGN", "лв"),
        CurrencyOption("Turkish lira", "TRY", "₺"),
        CurrencyOption("Ukrainian hryvnia", "UAH", "₴"),
        CurrencyOption("UAE dirham", "AED", "د.إ"),
        CurrencyOption("Saudi riyal", "SAR", "ر.س"),
        CurrencyOption("Israeli new shekel", "ILS", "₪"),
        CurrencyOption("South Korean won", "KRW", "₩"),
        CurrencyOption("Singapore dollar", "SGD", "S$"),
        CurrencyOption("Hong Kong dollar", "HKD", "HK$"),
        CurrencyOption("Mexican peso", "MXN", "MX$"),
        CurrencyOption("Argentine peso", "ARS", "AR$"),
        CurrencyOption("Chilean peso", "CLP", "CLP$"),
        CurrencyOption("Colombian peso", "COP", "COL$"),
        CurrencyOption("Indonesian rupiah", "IDR", "Rp"),
        CurrencyOption("Malaysian ringgit", "MYR", "RM"),
        CurrencyOption("Philippine peso", "PHP", "₱"),
        CurrencyOption("Thai baht", "THB", "฿"),
        CurrencyOption("Vietnamese đồng", "VND", "₫"),
        CurrencyOption("Pakistani rupee", "PKR", "₨"),
        CurrencyOption("Bangladeshi taka", "BDT", "৳"),
        CurrencyOption("Egyptian pound", "EGP", "E£"),
        CurrencyOption("Kenyan shilling", "KES", "KSh"),
        CurrencyOption("Ghanaian cedi", "GHS", "GH₵"),
        CurrencyOption("Moroccan dirham", "MAD", "DH"),
        CurrencyOption("Icelandic króna", "ISK", "kr")
    )

    fun findByCode(code: String?): CurrencyOption? =
        options.firstOrNull { it.code.equals(code, ignoreCase = true) }

    fun findBySymbol(symbol: String?): CurrencyOption? =
        options.firstOrNull { it.symbol == symbol }

    fun findExact(value: String?): CurrencyOption? {
        val query = value?.trim().orEmpty()
        return options.firstOrNull {
            it.displayLabel.equals(query, ignoreCase = true) ||
                it.code.equals(query, ignoreCase = true) ||
                it.symbol == query
        }
    }

    fun search(query: String?): List<CurrencyOption> {
        val term = query?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (term.isBlank()) return options
        return options.filter {
            it.name.lowercase(Locale.ROOT).contains(term) ||
                it.code.lowercase(Locale.ROOT).contains(term) ||
                it.symbol.lowercase(Locale.ROOT).contains(term)
        }
    }
}
