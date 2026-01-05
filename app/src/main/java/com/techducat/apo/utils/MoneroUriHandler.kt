package com.techducat.apo.utils

import android.net.Uri
import com.techducat.apo.models.PaymentRequest
import timber.log.Timber

object MoneroUriHandler {
    fun parseUri(uri: String): PaymentRequest? {
        return try {
            if (!uri.startsWith("monero:")) return null
            
            val cleanUri = if (uri.startsWith("monero:")) {
                uri.replaceFirst("monero:", "monero:")
            } else {
                uri
            }
            
            val parsed = Uri.parse(cleanUri)
            val address = parsed.schemeSpecificPart?.split("?")?.get(0) ?: return null
            
            val amount = parsed.getQueryParameter("tx_amount")
            val recipientName = parsed.getQueryParameter("recipient_name")
            val description = parsed.getQueryParameter("tx_description")
            
            PaymentRequest(
                address = address,
                amount = amount,
                recipientName = recipientName,
                txDescription = description,
                uri = cleanUri
            )
        } catch (e: Exception) {
            Timber.e("URIParser", e)
            null
        }
    }
    
    fun createUri(request: PaymentRequest): String {
        var uri = "monero:${request.address}"
        val params = mutableListOf<String>()
        
        request.amount?.let { params.add("tx_amount=$it") }
        request.recipientName?.let { params.add("recipient_name=${Uri.encode(it)}") }
        request.txDescription?.let { params.add("tx_description=${Uri.encode(it)}") }
        
        if (params.isNotEmpty()) {
            uri += "?" + params.joinToString("&")
        }
        
        return uri
    }
    
    fun isMoneroAddress(address: String): Boolean {
        return address.startsWith("4") || address.startsWith("8") && 
               address.length >= 95 && address.length <= 106
    }
}
