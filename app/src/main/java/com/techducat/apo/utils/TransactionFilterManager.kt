package com.techducat.apo.utils

import com.techducat.apo.models.*
import com.techducat.apo.storage.WalletDataStore
import java.text.SimpleDateFormat
import java.util.*

class TransactionFilterManager(private val dataStore: WalletDataStore) {
    fun filterTransactions(
        transactions: List<Transaction>,
        filterState: TransactionFilterState,
        sentTypeText: String,
        receivedTypeText: String
    ): List<Transaction> {
        return transactions.filter { tx ->
            val typeMatch = when (filterState.filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.SENT -> tx.type == sentTypeText
                TransactionFilter.RECEIVED -> tx.type == receivedTypeText
                TransactionFilter.PENDING -> !tx.confirmed
                TransactionFilter.CONFIRMED -> tx.confirmed
                TransactionFilter.LARGE -> {
                    val amount = tx.amount.replace("μ", "").trim().toDoubleOrNull() ?: 0.0
                    amount > 1.0
                }
                TransactionFilter.SMALL -> {
                    val amount = tx.amount.replace("μ", "").trim().toDoubleOrNull() ?: 0.0
                    amount <= 0.1
                }
            }
            
            val dateMatch = try {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                val txDate = dateFormat.parse(tx.date)?.time ?: 0L
                (filterState.dateFrom == null || txDate >= filterState.dateFrom) &&
                (filterState.dateTo == null || txDate <= filterState.dateTo)
            } catch (e: Exception) {
                true
            }
            
            val amountMatch = try {
                val txAmount = tx.amount.replace("μ", "").trim().toDoubleOrNull() ?: 0.0
                val minAmount = filterState.minAmount.toDoubleOrNull() ?: Double.MIN_VALUE
                val maxAmount = filterState.maxAmount.toDoubleOrNull() ?: Double.MAX_VALUE
                txAmount in minAmount..maxAmount
            } catch (e: Exception) {
                true
            }
            
            val searchMatch = filterState.searchQuery.isEmpty() ||
                    tx.type.contains(filterState.searchQuery, ignoreCase = true) ||
                    tx.amount.contains(filterState.searchQuery, ignoreCase = true) ||
                    tx.date.contains(filterState.searchQuery, ignoreCase = true) ||
                    tx.txId.contains(filterState.searchQuery, ignoreCase = true)
            
            typeMatch && dateMatch && amountMatch && searchMatch
        }
    }
    
    fun sortTransactions(
        transactions: List<Transaction>,
        sortBy: String = "date",
        ascending: Boolean = false
    ): List<Transaction> {
        return when (sortBy) {
            "amount" -> transactions.sortedBy {
                it.amount.replace("μ", "").trim().toDoubleOrNull() ?: 0.0
            }
            "date" -> transactions.sortedBy {
                try {
                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .parse(it.date)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
            else -> transactions
        }.let { if (!ascending) it.reversed() else it }
    }
}
