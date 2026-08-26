package com.example.budgetbuddy

import android.content.Context

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

