package com.techducat.apo.utils

import com.google.gson.Gson
import com.techducat.apo.models.Transaction
import com.techducat.apo.models.TransactionNote
import java.text.SimpleDateFormat
import java.util.*

object ExportManager {
    fun exportToCsv(transactions: List<Transaction>, notes: Map<String, TransactionNote>): String {
        val csv = StringBuilder()
        // CSV header
        csv.append("Type,Amount,Date,Confirmed,TxID,Note\n")
        
        transactions.forEach { tx ->
            val note = notes[tx.txId]?.note ?: ""
            // Escape quotes in the note field for CSV
            val escapedNote = note.replace("\"", "\"\"")
            
            csv.append("\"${tx.type}\",")
            csv.append("\"${tx.amount}\",")
            csv.append("\"${tx.date}\",")
            csv.append("\"${tx.confirmed}\",")
            csv.append("\"${tx.txId}\",")
            csv.append("\"$escapedNote\"\n")
        }
        
        return csv.toString()
    }
    
    fun exportToJson(transactions: List<Transaction>, notes: Map<String, TransactionNote>): String {
        // Create a list of transaction maps for JSON
        val transactionList = transactions.map { tx ->
            val note = notes[tx.txId]?.note ?: ""
            
            mapOf(
                "type" to tx.type,
                "amount" to tx.amount,
                "date" to tx.date,
                "confirmed" to tx.confirmed,
                "tx_id" to tx.txId,
                "note" to note
            )
        }
        
        // Create the complete data structure
        val exportData = mapOf(
            "export_date" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
            "export_timestamp" to System.currentTimeMillis(),
            "transaction_count" to transactions.size,
            "transactions" to transactionList
        )
        
        // Convert to JSON with pretty printing
        return Gson().toJson(exportData)
    }
    
    fun exportToJsonPretty(transactions: List<Transaction>, notes: Map<String, TransactionNote>): String {
        val gson = Gson()
        val transactionList = transactions.map { tx ->
            val note = notes[tx.txId]?.note ?: ""
            
            mapOf(
                "type" to tx.type,
                "amount" to tx.amount,
                "date" to tx.date,
                "confirmed" to tx.confirmed,
                "tx_id" to tx.txId,
                "note" to note
            )
        }
        
        val exportData = mapOf(
            "export_date" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
            "export_timestamp" to System.currentTimeMillis(),
            "transaction_count" to transactions.size,
            "transactions" to transactionList
        )
        
        return gson.toJson(exportData)
    }
    
    // Optional: Export to a simple text format
    fun exportToText(transactions: List<Transaction>, notes: Map<String, TransactionNote>): String {
        val text = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val exportDate = dateFormat.format(Date())
        
        text.append("Transaction Export\n")
        text.append("==================\n")
        text.append("Export Date: $exportDate\n")
        text.append("Total Transactions: ${transactions.size}\n")
        text.append("==================\n\n")
        
        transactions.forEachIndexed { index, tx ->
            val note = notes[tx.txId]?.note ?: "No notes"
            
            text.append("Transaction #${index + 1}\n")
            text.append("  Type: ${tx.type}\n")
            text.append("  Amount: ${tx.amount}\n")
            text.append("  Date: ${tx.date}\n")
            text.append("  Confirmed: ${if (tx.confirmed) "Yes" else "No"}\n")
            text.append("  Transaction ID: ${tx.txId}\n")
            text.append("  Note: $note\n")
            text.append("\n")
        }
        
        return text.toString()
    }
}
