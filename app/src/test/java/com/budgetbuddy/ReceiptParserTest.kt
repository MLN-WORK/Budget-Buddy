package com.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): ReceiptParserTest
 * Parent class: Any; child classes: none; related classes: ReceiptParser, JUnit, and the application code under test.
 * What the class does: Verifies the ReceiptParser behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class ReceiptParserTest {
    @Test
    fun `extracts merchant date and grand total`() {
        val result = ReceiptParser.parse(
            """
            Corner Coffee
            28/08/2026 14:32
            Latte       $4.50
            Subtotal    $4.50
            Tax         $0.68
            GRAND TOTAL $5.18
            """.trimIndent()
        )

        assertEquals("Corner Coffee", result.merchant)
        assertEquals("2026-08-28", result.date)
        assertEquals(5.18, result.total!!, 0.001)
        assertEquals(listOf("Latte"), result.items)
    }

    @Test
    fun `supports comma decimal receipts`() {
        val result = ReceiptParser.parse(
            """
            MARKET PLACE
            2026-08-27
            SUBTOTAL 10,00 EUR
            VAT 1,50 EUR
            AMOUNT DUE 11,50 EUR
            """.trimIndent()
        )

        assertEquals("MARKET PLACE", result.merchant)
        assertEquals("2026-08-27", result.date)
        assertEquals(11.50, result.total!!, 0.001)
    }

    @Test
    fun `does not treat tax or change as total`() {
        val result = ReceiptParser.parse("Receipt\nVAT 2.00\nCASH 20.00\nCHANGE 5.00")

        assertNull(result.total)
    }

    @Test
    fun `parses grouped monetary values`() {
        assertEquals(1234.56, ReceiptParser.parseAmount("R 1 234,56")!!, 0.001)
        assertEquals(1234.56, ReceiptParser.parseAmount("$1,234.56")!!, 0.001)
    }

    @Test
    fun `prominent top text can override a weaker first-line merchant guess`() {
        val result = ReceiptParser.parse(
            "STORE COPY\nBUDDY MARKET\nTOTAL 20.00",
            prominentMerchant = "BUDDY MARKET"
        )

        assertEquals("BUDDY MARKET", result.merchant)
    }

    @Test
    fun `supports common dashed and slashed numeric date formats`() {
        val examples = listOf(
            "28-08-26",
            "2026-08--28",
            "28-08-2026",
            "28/08/26",
            "2026/08/28",
            "28/08/2026"
        )

        examples.forEach { value ->
            assertEquals(value, "2026-08-28", ReceiptParser.parse("BUDDY SHOP\nDate $value\nTOTAL 1.00").date)
        }
    }

    @Test
    fun `recognises additional total keywords and purchased items`() {
        val result = ReceiptParser.parse(
            """
            BUDDY MARKET
            Bread 2.50
            Fresh Milk 18.99
            Apples 12.00
            TOTAL AMOUNT DUE 33.49
            """.trimIndent()
        )

        assertEquals(33.49, result.total!!, 0.001)
        assertEquals(listOf("Bread", "Fresh Milk", "Apples"), result.items)
        assertEquals(42.50, ReceiptParser.parse("SHOP\nSUM 42.50").total!!, 0.001)
        assertEquals(19.95, ReceiptParser.parse("SHOP\nCOST 19.95").total!!, 0.001)
    }

    @Test
    fun `prefers a labelled receipt date over a later due date`() {
        val result = ReceiptParser.parse(
            """
            REPAIR SHOP
            RECEIPT DATE: 11/02/2019
            DUE DATE: 26/02/2019
            TOTAL: ${'$'}154.06
            """.trimIndent()
        )

        assertEquals("2019-02-11", result.date)
    }

    @Test
    fun `supports attached currency codes and multi column receipt items`() {
        val result = ReceiptParser.parse(
            """
            PINE RIDGE CAMP
            10/10/2025 08:10
            PROGRAM FEE USD180
            MEALS PACKAGE USD40
            T-SHIRT OPTIONAL USD15
            SUBTOTAL USD235
            TOTAL USD235
            """.trimIndent()
        )

        assertEquals(235.0, result.total!!, 0.001)
        assertEquals(listOf("PROGRAM FEE", "MEALS PACKAGE", "T SHIRT OPTIONAL"), result.items)
    }

    @Test
    fun `prefers payable total over card phone and reference numbers`() {
        val result = ReceiptParser.parse(
            """
            BUDDY SUPERMARKET
            Tel 011 555 8492
            Receipt No 784512963
            29/08/2026 18:42
            Bread R 24.99
            Milk R 18.50
            TOTAL AMOUNT PAYABLE R 43.49
            VISA **** 6247
            Auth 492819
            """.trimIndent()
        )

        assertEquals(43.49, result.total!!, 0.001)
    }

    @Test
    fun `recognises purchased payable and split line total labels`() {
        assertEquals(
            349.90,
            ReceiptParser.parse("STORE\nPURCHASED TOTAL R 349,90").total!!,
            0.001
        )
        assertEquals(
            204.0,
            ReceiptParser.parse("STORE\nTOTAL COST\nR 204.00").total!!,
            0.001
        )
    }

    @Test
    fun `accepts total including vat but rejects item and tax totals`() {
        val result = ReceiptParser.parse(
            """
            CORNER SHOP
            TOTAL ITEMS 12
            VAT TOTAL 12.50
            GRAND TOTAL INCL VAT R 87.50
            """.trimIndent()
        )

        assertEquals(87.50, result.total!!, 0.001)
    }

    @Test
    fun `unlabelled fallback favours clean bottom money rather than metadata`() {
        val result = ReceiptParser.parse(
            """
            LOCAL STORE
            Phone 012.3456789
            Reference 987654321
            Apples 18.00
            Bread 12.00
            R 30.00
            """.trimIndent()
        )

        assertEquals(30.0, result.total!!, 0.001)
        assertNull(ReceiptParser.parseAmount("012.3456789"))
    }

    @Test
    fun `does not confuse purchased item counts or payment card numbers with the total`() {
        assertNull(ReceiptParser.parse("STORE\nPURCHASED 12 ITEMS").total)
        assertNull(ReceiptParser.parse("STORE\nAMOUNT PAID\nVISA 6247").total)
    }
}
// End of class: ReceiptParserTest
