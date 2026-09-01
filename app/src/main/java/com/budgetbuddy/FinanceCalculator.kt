package com.budgetbuddy

/** Pure finance calculations shared by the UI and local unit tests. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): FinanceCalculator
 * Parent class: Any; child classes: none; related classes: Transaction, Balance, LocalDataStore, and MainActivity.
 * What the class does: Computes balances and totals without Android dependencies.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
object FinanceCalculator {
    fun balanceForMonth(transactions: List<Transaction>, monthKey: String): Balance {
        val inMonth = transactions.filter { it.date.startsWith(monthKey) }
        val income = inMonth.filter(Transaction::isIncome).sumOf(Transaction::amount)
        val expenses = inMonth.filterNot(Transaction::isIncome).sumOf(Transaction::amount)
        return Balance(totalIncome = income, totalExpenses = expenses, closingBalance = income - expenses)
    }

    fun inDateRange(transactions: List<Transaction>, start: String, end: String): List<Transaction> =
        transactions.filter { it.date in start..end }.sortedBy(Transaction::date)

    fun expenseTotalsByCategory(transactions: List<Transaction>): Map<String, Double> =
        transactions.filterNot(Transaction::isIncome)
            .groupBy(Transaction::categoryId)
            .mapValues { (_, values) -> values.sumOf(Transaction::amount) }

    fun incomeAddedToSpendingLimit(transactions: List<Transaction>, monthKey: String): Double =
        transactions.filter {
            it.isIncome && it.addsToSpendingLimit && it.date.startsWith(monthKey)
        }.sumOf(Transaction::amount)

    fun effectiveSpendingLimit(baseLimit: Double, transactions: List<Transaction>, monthKey: String): Double =
        baseLimit.coerceAtLeast(0.0) + incomeAddedToSpendingLimit(transactions, monthKey)
}
// End of class: FinanceCalculator
