package com.budgetbuddy

import android.content.Context

/*
 * Start of class
 * Name of class and related classes (parent/child classes): TransactionRepo
 * Parent class: Any; child classes: none; related classes: LocalDataStore and transaction-reporting activities.
 * What the class does: Wraps local transaction reads behind success and error callbacks.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
class TransactionRepo(context: Context) {
    private val localData = LocalDataStore(context)

    // Fetch all transactions for the current user
    fun fetchAll(onComplete: (List<Transaction>) -> Unit,
                 onError: (Exception) -> Unit) {
        runCatching { localData.getTransactions() }
            .onSuccess(onComplete)
            .onFailure { onError(Exception("Could not read local transactions", it)) }
    }

    // Fetch transactions in a given date range
    fun fetchInRange(start: String, end: String,
                     onComplete: (List<Transaction>) -> Unit,
                     onError: (Exception) -> Unit) {
        runCatching { localData.getTransactions(start, end) }
            .onSuccess(onComplete)
            .onFailure { onError(Exception("Could not read local transactions", it)) }
    }
}
// End of class: TransactionRepo

