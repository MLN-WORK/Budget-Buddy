package com.budgetbuddy

/** Pure finance calculations shared by the UI and local unit tests. */
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
}
