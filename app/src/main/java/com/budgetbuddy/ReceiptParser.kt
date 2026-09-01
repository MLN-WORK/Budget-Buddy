package com.budgetbuddy

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale

/** Turns unstructured on-device OCR text into conservative, user-reviewable suggestions. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): ReceiptParser
 * Parent class: Any; child classes: Candidate and AmountCandidate; related classes: ReceiptOcrScanner, ReceiptOcrResult, Candidate, and AmountCandidate.
 * What the class does: Extracts likely merchant, date, and total fields from recognized receipt text.
 * What's important to other classes, if applicable: OCR callers treat its output as a suggestion and must keep user review and input validation in place.
 * Code with comments begins below.
 */
object ReceiptParser {
    private val amountToken = Regex(
        """(?i)(?:USD|EUR|GBP|ZAR|CAD|AUD|NZD|JPY|INR|CHF|R|\p{Sc})?\s*[-+]?\d[\d\s,.'’]*"""
    )
    private val currencySignal = Regex("""(?i)(USD|EUR|GBP|ZAR|CAD|AUD|NZD|JPY|INR|CHF|\p{Sc}|\bR\s*\d)""")
    private val decimalSignal = Regex("""\d[.,]\d{2}(?!\d)""")
    private val numericDate = Regex(
        """(?<!\d)(\d{1,4})\s*[./-]{1,2}\s*(\d{1,2})\s*[./-]{1,2}\s*(\d{1,4})(?!\d)"""
    )
    private val textDate = Regex(
        """(?i)(?<!\w)(\d{1,2}\s+[a-z]{3,9}\s+\d{2,4}|[a-z]{3,9}\s+\d{1,2},?\s+\d{2,4})(?!\w)"""
    )
    private val ignoredMerchantWords = setOf(
        "receipt", "tax invoice", "invoice", "customer copy", "merchant copy", "welcome", "thank you"
    )
    private val ignoredMerchantFragments = listOf(
        "www.", "http", "@", "tel:", "phone", "vat no", "tax no", "cashier", "served by",
        "street", " road", " avenue", " lane", " drive", " mall", "branch:"
    )
    private val rejectedAmountLabels = listOf(
        "subtotal", "sub total", "tax", "vat", "gst", "change", "cash", "tendered",
        "discount", "saving", "tip", "gratuity", "rounding", "service charge"
    )
    private val preferredAmountLabels = listOf(
        "total amount payable" to 155,
        "total amount due" to 150,
        "grand total" to 145,
        "amount payable" to 140,
        "amount due" to 138,
        "total payable" to 136,
        "balance due" to 134,
        "total due" to 132,
        "purchase total" to 130,
        "purchased total" to 129,
        "total purchase" to 128,
        "total purchased" to 127,
        "amount charged" to 126,
        "total charged" to 125,
        "sale total" to 124,
        "order total" to 123,
        "net total" to 122,
        "amount paid" to 120,
        "total cost" to 118,
        "card total" to 116,
        "payment total" to 114,
        "payable" to 105,
        "purchased" to 102,
        "total" to 90,
        "sum" to 85,
        "cost" to 80
    )
    private val rejectedPrimaryAmountLine = Regex(
        """(?i)^\s*(?:sub\s*total|tax|vat|gst|change|cash(?:\s+tendered)?|tendered|discount|savings?|tip|gratuity|rounding|service\s+charge)\b"""
    )
    private val rejectedTotalPhrase = Regex(
        """(?i)\b(?:total\s+(?:items?|products?|qty|quantity|savings?|discount|tax|vat|gst|points?)|(?:items?|products?|qty|quantity|savings?|discount|tax|vat|gst|points?)\s+total|purchased\s+\d+\s+(?:items?|products?))\b"""
    )
    private val metadataAmountLine = Regex(
        """(?i)\b(?:tel(?:ephone)?|phone|fax|invoice|receipt\s*(?:no|number|#)|order\s*(?:no|number|#)|transaction\s*(?:id|no|number|#)|auth(?:orisation|orization)?|approval|reference|ref\.?|terminal|merchant\s+id|vat\s*(?:no|number)|tax\s*(?:no|number)|account\s*(?:id|no|number|#)|customer\s*(?:id|no|number|#)|member|loyalty|points?|barcode|visa|mastercard|master\s+card|amex|american\s+express|debit\s+card|credit\s+card)\b"""
    )
    private val ignoredItemLabels = listOf(
        "subtotal", "sub total", "total", "amount due", "sum", "cost", "tax", "vat",
        "change", "cash", "card", "tendered", "discount", "saving", "tip", "date", "time",
        "invoice", "receipt", "cashier", "served by", "approval", "auth"
    )

    fun parse(rawText: String, prominentMerchant: String? = null): ReceiptOcrResult {
        val lines = rawText.lineSequence()
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter(String::isNotBlank)
            .toList()
        return ReceiptOcrResult(
            merchant = prominentMerchant?.takeIf(::isMerchantCandidate) ?: findMerchant(lines),
            date = findDate(lines),
            total = findTotal(lines),
            items = findItems(lines),
            rawText = rawText
        )
    }

    internal fun isMerchantCandidate(line: String): Boolean {
        val lower = line.lowercase(Locale.ROOT)
        return line.length in 2..60 &&
            line.any(Char::isLetter) &&
            ignoredMerchantWords.none { lower == it || lower.startsWith("$it #") } &&
            ignoredMerchantFragments.none(lower::contains) &&
            preferredAmountLabels.none { (label, _) -> containsLabel(lower, label) } &&
            rejectedAmountLabels.none(lower::contains) &&
            numericDate.find(line) == null && textDate.find(line) == null
    }

    private fun findMerchant(lines: List<String>): String? = lines
        .take(10)
        .mapIndexedNotNull { index, line ->
            if (!isMerchantCandidate(line)) return@mapIndexedNotNull null
            val letters = line.filter(Char::isLetter)
            val upperRatio = letters.count(Char::isUpperCase).toDouble() / letters.length.coerceAtLeast(1)
            val score = 100 - (index * 9) +
                (if (upperRatio >= 0.75) 18 else 0) +
                (if (line.split(' ').size <= 5) 8 else 0)
            line to score
        }
        .maxByOrNull { it.second }
        ?.first

    private fun findDate(lines: List<String>): String? {
        /*
         * Start of class
         * Name of class and related classes (parent/child classes): Candidate
         * Parent class: Any; child classes: none; related classes: ReceiptParser only.
         * What the class does: Stores a temporary scored parsing candidate inside ReceiptParser.
         * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
         * Code with comments begins below.
         */
        data class Candidate(val date: LocalDate, val score: Int)
        // End of class: Candidate
        val candidates = mutableListOf<Candidate>()
        lines.forEachIndexed { index, line ->
            val lower = line.lowercase(Locale.ROOT)
            val labelScore = when {
                "receipt date" in lower || "transaction date" in lower || "purchase date" in lower -> 90
                Regex("""(?i)(^|\s)date\s*[:#]""").containsMatchIn(line) -> 75
                "time:" in lower || lower.startsWith("time ") -> 45
                "due date" in lower || "expiry" in lower || "valid until" in lower -> -80
                else -> 0
            }
            fun add(date: LocalDate) {
                // Real receipt dates may be old. Position and nearby labels decide which
                // printed date is the transaction date; age alone never rewrites it.
                candidates += Candidate(date, 60 - index.coerceAtMost(50) + labelScore)
            }
            numericDate.findAll(line).forEach { match -> parseNumericDate(match.groupValues.drop(1))?.let(::add) }
            textDate.findAll(line).forEach { match -> parseTextDate(match.value)?.let(::add) }
        }
        return candidates.maxByOrNull(Candidate::score)?.date?.toString()
    }

    private fun parseNumericDate(parts: List<String>): LocalDate? {
        val first = parts[0].toIntOrNull() ?: return null
        val middle = parts[1].toIntOrNull() ?: return null
        var last = parts[2].toIntOrNull() ?: return null
        return runCatching {
            when {
                parts[0].length == 4 -> LocalDate.of(first, middle, last)
                parts[2].length >= 2 -> {
                    if (last < 100) last += if (last >= 70) 1900 else 2000
                    // Receipt OCR is ambiguous for values <= 12; prefer the widely used day-month-year form.
                    LocalDate.of(last, middle, first)
                }
                else -> return null
            }
        }.getOrNull()?.takeIf(::plausibleDate)
    }

    private fun parseTextDate(value: String): LocalDate? {
        val patterns = listOf("d MMM uuuu", "d MMMM uuuu", "MMM d uuuu", "MMMM d uuuu")
        val normalized = value.replace(",", "").trim()
        for (pattern in patterns) {
            val formatter = DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.SMART)
            runCatching { LocalDate.parse(normalized, formatter) }
                .getOrNull()
                ?.takeIf(::plausibleDate)
                ?.let { return it }
        }
        return null
    }

    private fun plausibleDate(date: LocalDate): Boolean = date.year in 2000..(LocalDate.now().year + 1)

    private fun findTotal(lines: List<String>): Double? {
        /*
         * Start of class
         * Name of class and related classes (parent/child classes): Candidate
         * Parent class: Any; child classes: none; related classes: ReceiptParser only.
         * What the class does: Stores a temporary scored parsing candidate inside ReceiptParser.
         * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
         * Code with comments begins below.
         */
        data class Candidate(val amount: Double, val score: Int, val lineIndex: Int)
        // End of class: Candidate
        val candidates = mutableListOf<Candidate>()

        lines.forEachIndexed { index, line ->
            val lower = normaliseAmountLabels(line)
            preferredAmountLabels.firstOrNull { (label, _) -> containsLabel(lower, label) }?.let { (label, labelScore) ->
                if (isRejectedTotalLine(lower)) return@forEachIndexed
                val labelEnd = lower.indexOf(label).takeIf { it >= 0 }?.plus(label.length) ?: 0
                val sameLine = extractAmountCandidates(line, allowInteger = true)
                val selected = sameLine.maxByOrNull { amount ->
                    amount.confidence +
                        (if (amount.start >= labelEnd) 32 else 0) +
                        (if (sameLine.size == 1) 8 else 0)
                }
                if (selected != null) {
                    candidates += Candidate(
                        selected.amount,
                        labelScore + selected.confidence +
                            (if (selected.start >= labelEnd) 32 else 0) +
                            receiptPositionBonus(index, lines.size),
                        index
                    )
                } else {
                    val nextLine = lines.getOrNull(index + 1)
                    val nextAmount = nextLine
                        ?.takeUnless { isFallbackNoiseLine(it) }
                        ?.let { extractAmountCandidates(it, allowInteger = true).maxByOrNull(AmountCandidate::confidence) }
                    if (nextAmount != null) {
                        candidates += Candidate(
                            nextAmount.amount,
                            labelScore + nextAmount.confidence + receiptPositionBonus(index + 1, lines.size) - 8,
                            index + 1
                        )
                    }
                }
            }
        }
        candidates.maxWithOrNull(compareBy<Candidate> { it.score }.thenBy { it.lineIndex })?.let { return it.amount }

        // If OCR lost the label, prefer a clean money value near the receipt's bottom.
        // This is deliberately position-led rather than amount-led so a phone number,
        // loyalty id, or card reference cannot win merely by being numerically larger.
        return lines.flatMapIndexed { index, line ->
            if (isFallbackNoiseLine(line)) return@flatMapIndexed emptyList()
            extractAmountCandidates(line, allowInteger = false).map { amount ->
                Candidate(
                    amount.amount,
                    amount.confidence + receiptPositionBonus(index, lines.size) * 3,
                    index
                )
            }
        }.maxWithOrNull(compareBy<Candidate> { it.score }.thenBy { it.lineIndex })?.amount
    }

    private fun extractAmounts(line: String, allowInteger: Boolean): List<Double> {
        return extractAmountCandidates(line, allowInteger).map(AmountCandidate::amount)
    }

    /*
     * Start of class
     * Name of class and related classes (parent/child classes): AmountCandidate
     * Parent class: Any; child classes: none; related classes: ReceiptParser only.
     * What the class does: Stores a ranked receipt amount candidate and its source position.
     * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
     * Code with comments begins below.
     */
    private data class AmountCandidate(
        val amount: Double,
        val start: Int,
        val confidence: Int
    )
    // End of class: AmountCandidate

    private fun extractAmountCandidates(line: String, allowInteger: Boolean): List<AmountCandidate> {
        val hasMoneySignal = currencySignal.containsMatchIn(line) || decimalSignal.containsMatchIn(line)
        if (!allowInteger && !hasMoneySignal) return emptyList()
        return amountToken.findAll(line)
            .mapNotNull { match ->
                val amount = parseAmount(match.value) ?: return@mapNotNull null
                if (amount <= 0.0 || amount > 1_000_000_000.0) return@mapNotNull null
                val confidence =
                    (if (currencySignal.containsMatchIn(match.value) || currencySignal.containsMatchIn(line)) 28 else 0) +
                    (if (decimalSignal.containsMatchIn(match.value)) 24 else 0) +
                    (if (match.value.count(Char::isDigit) <= 7) 6 else -24)
                AmountCandidate(amount, match.range.first, confidence)
            }
            .toList()
    }

    private fun receiptPositionBonus(index: Int, lineCount: Int): Int =
        if (lineCount <= 1) 0 else (index.coerceAtLeast(0) * 20) / (lineCount - 1)

    private fun normaliseAmountLabels(line: String): String = line.lowercase(Locale.ROOT)
        .replace(Regex("""\bt[0o]ta[li1]\b"""), "total")
        .replace(Regex("""\bam[0o]unt\b"""), "amount")

    private fun isRejectedTotalLine(normalisedLower: String): Boolean =
        rejectedPrimaryAmountLine.containsMatchIn(normalisedLower) ||
            rejectedTotalPhrase.containsMatchIn(normalisedLower)

    private fun isFallbackNoiseLine(line: String): Boolean {
        val lower = normaliseAmountLabels(line)
        if (isRejectedTotalLine(lower)) return true
        if (metadataAmountLine.containsMatchIn(lower)) return true
        if ('%' in line) return true
        val hasTotalLabel = preferredAmountLabels.any { (label, _) -> containsLabel(lower, label) }
        if (!hasTotalLabel && (numericDate.containsMatchIn(line) || textDate.containsMatchIn(line))) return true
        if (!hasTotalLabel && Regex("""(?<!\d)\d{1,2}:\d{2}(?::\d{2})?(?!\d)""").containsMatchIn(line)) return true
        return false
    }

    private fun findItems(lines: List<String>): List<String> = lines.mapNotNull { line ->
        val lower = line.lowercase(Locale.ROOT)
        if (ignoredItemLabels.any { containsLabel(lower, it) }) return@mapNotNull null
        if (numericDate.containsMatchIn(line) || textDate.containsMatchIn(line)) return@mapNotNull null
        if (extractAmounts(line, allowInteger = false).isEmpty()) return@mapNotNull null
        val name = amountToken.replace(line, " ")
            .replace(Regex("""(?i)\b(?:ea|each|qty|x)\b"""), " ")
            .replace(Regex("""[-–—:=*]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '.', ',', ':', ';')
        name.takeIf {
            it.length in 2..60 &&
                it.any(Char::isLetter) &&
                ignoredMerchantFragments.none(lower::contains)
        }
    }.distinctBy { it.lowercase(Locale.ROOT) }.take(MAX_ITEM_SUGGESTIONS)

    private fun containsLabel(text: String, label: String): Boolean {
        val escapedLabel = label.trim()
            .split(Regex("""\s+"""))
            .joinToString("""\s+""", transform = Regex::escape)
        val pattern = Regex("""(?i)(?<![a-z0-9])$escapedLabel(?![a-z0-9])""")
        return pattern.containsMatchIn(text)
    }

    internal fun parseAmount(value: String): Double? {
        var normalized = value.uppercase(Locale.ROOT)
            .replace(Regex("""(USD|EUR|GBP|ZAR|CAD|AUD|NZD|JPY|INR|CHF)"""), "")
            .replace(Regex("""[\p{Sc}R\s'’]"""), "")
            .trim()
        if (normalized.isBlank() || normalized.startsWith("-")) return null
        normalized = normalized.removePrefix("+")
        if (!Regex("""\d[\d,.]*""").matches(normalized)) return null

        val comma = normalized.lastIndexOf(',')
        val dot = normalized.lastIndexOf('.')
        normalized = when {
            comma >= 0 && dot >= 0 -> {
                val decimal = if (comma > dot) ',' else '.'
                val thousands = if (decimal == ',') '.' else ','
                val digitsAfter = normalized.length - normalized.lastIndexOf(decimal) - 1
                when {
                    digitsAfter in 1..2 -> normalized.replace(thousands.toString(), "").replace(decimal, '.')
                    digitsAfter == 3 -> normalized.replace(",", "").replace(".", "")
                    else -> return null
                }
            }
            comma >= 0 -> normalizeSingleSeparator(normalized, ',') ?: return null
            dot >= 0 -> normalizeSingleSeparator(normalized, '.') ?: return null
            else -> normalized
        }
        return normalized.toDoubleOrNull()?.takeIf(Double::isFinite)
    }

    private fun normalizeSingleSeparator(value: String, separator: Char): String? {
        val groups = value.split(separator)
        if (groups.any { it.isBlank() || it.any { character -> !character.isDigit() } }) return null
        if (groups.size == 2) {
            return when (groups[1].length) {
                in 1..2 -> "${groups[0]}.${groups[1]}"
                3 -> if (groups[0].length in 1..3) groups.joinToString("") else null
                else -> null
            }
        }
        if (groups.drop(1).all { it.length == 3 }) return groups.joinToString("")
        if (groups.last().length in 1..2 && groups.drop(1).dropLast(1).all { it.length == 3 }) {
            return groups.dropLast(1).joinToString("") + "." + groups.last()
        }
        return null
    }

    private const val MAX_ITEM_SUGGESTIONS = 8
}
// End of class: ReceiptParser
