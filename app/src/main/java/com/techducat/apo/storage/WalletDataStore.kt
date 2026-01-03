package com.techducat.apo.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.techducat.apo.models.*

class WalletDataStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("wallet_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveAddressBook(entries: List<AddressBookEntry>) {
        prefs.edit().putString("address_book", gson.toJson(entries)).apply()
    }
    
    fun loadAddressBook(): List<AddressBookEntry> {
        val json = prefs.getString("address_book", "[]") ?: "[]"
        val type = object : TypeToken<List<AddressBookEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun saveTransactionNotes(notes: Map<String, TransactionNote>) {
        prefs.edit().putString("tx_notes", gson.toJson(notes)).apply()
    }
    
    fun loadTransactionNotes(): Map<String, TransactionNote> {
        val json = prefs.getString("tx_notes", "{}") ?: "{}"
        val type = object : TypeToken<Map<String, TransactionNote>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
    
    fun saveSubaddresses(subaddresses: List<Subaddress>) {
        prefs.edit().putString("subaddresses", gson.toJson(subaddresses)).apply()
    }
    
    fun loadSubaddresses(): List<Subaddress> {
        val json = prefs.getString("subaddresses", "[]") ?: "[]"
        val type = object : TypeToken<List<Subaddress>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun saveDefaultPriority(priority: TransactionPriority) {
        prefs.edit().putString("default_priority", priority.name).apply()
    }
    
    fun loadDefaultPriority(): TransactionPriority {
        val name = prefs.getString("default_priority", null)
        return if (name != null) {
            try {
                TransactionPriority.valueOf(name)
            } catch (e: Exception) {
                TransactionPriority.MEDIUM
            }
        } else {
            TransactionPriority.MEDIUM
        }
    }
}
