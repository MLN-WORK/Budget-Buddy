package com.budgetbuddy

import java.util.Locale
import java.util.Currency

/*
 * Start of class
 * Name of class and related classes (parent/child classes): CurrencyOption
 * Parent class: Any; child classes: none; related classes: CurrencyCatalog, CurrencySearchAdapter, and ProfileActivity.
 * What the class does: Stores one searchable currency code, name, symbol, and keywords.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class CurrencyOption(
    val name: String,
    val code: String,
    val symbol: String
) {
    val displayLabel: String
        get() = "$name ($code) — $symbol"

    override fun toString(): String = displayLabel
}
// End of class: CurrencyOption

/** Offline currency catalogue used by onboarding and Settings. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): CurrencyCatalog
 * Parent class: Any; child classes: none; related classes: CurrencyOption, CurrencySearchAdapter, LocalDataStore, and ProfileActivity.
 * What the class does: Provides the supported currencies and safe lookup operations.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
object CurrencyCatalog {
    const val DEFAULT_CODE = "EUR"
    const val DEFAULT_SYMBOL = "€"

    private val preferredOptions = listOf(
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

    /**
     * ISO 4217 List One, verified against the official SIX maintenance-agency
     * publication on 30 August 2026. Keeping the codes in the APK makes the
     * catalogue complete and deterministic even on phones with an older ICU
     * currency database.
     */
    internal val currentIsoCodes: Set<String> = """
        AED,AFN,ALL,AMD,AOA,ARS,AUD,AWG,AZN,BAM,BBD,BDT,BHD,BIF,BMD,BND,BOB,BOV,BRL,BSD,BTN,BWP,BYN,BZD,CAD,CDF,CHE,CHF,CHW,CLF,CLP,CNY,COP,COU,CRC,CUP,CVE,CZK,DJF,DKK,DOP,DZD,EGP,ERN,ETB,EUR,FJD,FKP,GBP,GEL,GHS,GIP,GMD,GNF,GTQ,GYD,HKD,HNL,HTG,HUF,IDR,ILS,INR,IQD,IRR,ISK,JMD,JOD,JPY,KES,KGS,KHR,KMF,KPW,KRW,KWD,KYD,KZT,LAK,LBP,LKR,LRD,LSL,LYD,MAD,MDL,MGA,MKD,MMK,MNT,MOP,MRU,MUR,MVR,MWK,MXN,MXV,MYR,MZN,NAD,NGN,NIO,NOK,NPR,NZD,OMR,PAB,PEN,PGK,PHP,PKR,PLN,PYG,QAR,RON,RSD,RUB,RWF,SAR,SBD,SCR,SDG,SEK,SGD,SHP,SLE,SOS,SRD,SSP,STN,SVC,SYP,SZL,THB,TJS,TMT,TND,TOP,TRY,TTD,TWD,TZS,UAH,UGX,USD,USN,UYI,UYU,UYW,UZS,VED,VES,VND,VUV,WST,XAD,XAF,XAG,XAU,XBA,XBB,XBC,XBD,XCD,XCG,XDR,XOF,XPD,XPF,XPT,XSU,XTS,XUA,XXX,YER,ZAR,ZMW,ZWG
    """.trimIndent().replace("\n", "").split(',').filter(String::isNotBlank).toSet()

    private val recentIsoNames = mapOf(
        "SLE" to "Sierra Leonean leone",
        "VED" to "Venezuelan digital bolívar",
        "XAD" to "Arab Accounting Dinar",
        "XCG" to "Caribbean guilder",
        "ZWG" to "Zimbabwe Gold"
    )

    val options: List<CurrencyOption> = buildList {
        val preferredByCode = preferredOptions.associateBy(CurrencyOption::code)
        currentIsoCodes.forEach { code ->
            val preferred = preferredByCode[code]
            val platform = runCatching { Currency.getInstance(code) }.getOrNull()
            add(
                preferred ?: CurrencyOption(
                    name = platform?.getDisplayName(Locale.UK)
                        ?.takeIf(String::isNotBlank)
                        ?: recentIsoNames[code]
                        ?: code,
                    code = code,
                    symbol = platform?.getSymbol(Locale.UK)
                        ?.takeIf(String::isNotBlank)
                        ?: code
                )
            )
        }
        // Keep previously offered historic currencies selectable so an existing
        // offline profile can never silently fall back to a different currency.
        preferredOptions.filter { it.code !in currentIsoCodes }.forEach { add(it) }
    }.distinctBy(CurrencyOption::code).sortedWith(
        compareBy<CurrencyOption> { it.name.lowercase(Locale.ROOT) }.thenBy(CurrencyOption::code)
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
        options.firstOrNull { it.code.equals(term, ignoreCase = true) }?.let { return listOf(it) }
        return options.filter {
            it.name.lowercase(Locale.ROOT).contains(term) ||
                it.code.lowercase(Locale.ROOT).contains(term) ||
                it.symbol.lowercase(Locale.ROOT).contains(term)
        }
    }
}
// End of class: CurrencyCatalog
