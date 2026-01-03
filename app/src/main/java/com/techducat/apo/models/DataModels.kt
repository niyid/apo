package com.techducat.apo.models

import java.util.*
import androidx.annotation.StringRes
import com.techducat.apo.R

data class AddressBookEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val notes: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

data class TransactionNote(
    val txId: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Subaddress(
    val index: Int,
    val address: String,
    val label: String,
    val used: Boolean = false,
    val creationTime: Long = System.currentTimeMillis()
)

data class FiatCurrency(
    val code: String,
    val symbol: String,
    val name: String
)

enum class TransactionPriority(
    val value: Int, 
    @StringRes val displayNameRes: Int? = null, 
    val displayName: String? = null,
    val feeMultiplier: Double
) {
    LOW(1, displayName = "Slow & Cheaper", feeMultiplier = 0.5),
    MEDIUM(2, displayNameRes = R.string.normal, feeMultiplier = 1.0),
    HIGH(3, displayName = "Fast & Standard", feeMultiplier = 1.5),
    URGENT(4, displayName = "Urgent & Fastest", feeMultiplier = 2.0);

    companion object {
        fun fromName(name: String?): TransactionPriority {
            return when (name) {
                "LOW" -> LOW
                "HIGH" -> HIGH
                "URGENT" -> URGENT
                else -> MEDIUM
            }
        }
    }
}

data class PaymentRequest(
    val address: String,
    val amount: String? = null,
    val recipientName: String? = null,
    val txDescription: String? = null,
    val uri: String = ""
)

data class RecipientInput(
    val id: String = UUID.randomUUID().toString(),
    val address: String,
    val amount: String
)

enum class TransactionFilter {
    ALL, SENT, RECEIVED, PENDING, CONFIRMED, LARGE, SMALL
}

data class TransactionFilterState(
    val filter: TransactionFilter = TransactionFilter.ALL,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val minAmount: String = "",
    val maxAmount: String = "",
    val searchQuery: String = ""
)

data class Transaction(
    val type: String,
    val amount: String,
    val date: String,
    val confirmed: Boolean,
    val txId: String = ""
)
