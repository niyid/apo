package com.techducat.apo.network

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import org.json.JSONObject
import com.techducat.apo.BuildConfig

class ChangeNowSwapService {
    companion object {
        private const val BASE_URL = "https://api.changenow.io/v2"
        private const val CONNECTION_TIMEOUT = 10000
        private const val READ_TIMEOUT = 15000
        
        private val API_KEY: String by lazy {
            try {
                BuildConfig.CHANGENOW_API_KEY.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("ChangeNow API key not configured")
            } catch (e: Exception) {
                throw IllegalStateException("ChangeNow API key not configured", e)
            }
        }
        
        fun isConfigured(): Boolean = try {
            API_KEY.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
    
    data class Currency(val ticker: String, val name: String, val isAvailable: Boolean)
    data class ExchangeEstimate(
        val fromCurrency: String, 
        val toCurrency: String, 
        val fromAmount: Double, 
        val estimatedAmount: Double
    )
    data class ExchangeStatus(
        val id: String, 
        val status: String, 
        val payinAddress: String, 
        val payoutAddress: String, 
        val fromCurrency: String, 
        val toCurrency: String
    )
    
    suspend fun getEstimate(
        fromCurrency: String, 
        toCurrency: String, 
        amount: Double
    ): Result<ExchangeEstimate> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(
                "$BASE_URL/exchange/estimated-amount?" +
                "fromCurrency=$fromCurrency&toCurrency=$toCurrency&" +
                "fromAmount=$amount&flow=standard&type=direct"
            )
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("x-changenow-api-key", API_KEY)
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                Result.success(
                    ExchangeEstimate(
                        fromCurrency, 
                        toCurrency, 
                        amount, 
                        json.getDouble("toAmount")
                    )
                )
            } else {
                Result.failure(Exception("HTTP error: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
    
    suspend fun createExchange(
        fromCurrency: String, 
        toCurrency: String, 
        fromAmount: Double, 
        toAddress: String, 
        refundAddress: String? = null
    ): Result<ExchangeStatus> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/exchange")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-changenow-api-key", API_KEY)
            connection.doOutput = true
            
            val requestBody = JSONObject().apply {
                put("fromCurrency", fromCurrency)
                put("toCurrency", toCurrency)
                put("fromAmount", fromAmount)
                put("address", toAddress)
                put("flow", "standard")
                refundAddress?.let { put("refundAddress", it) }
            }
            
            connection.outputStream.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }
            }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                Result.success(
                    ExchangeStatus(
                        json.getString("id"), 
                        json.getString("status"), 
                        json.getString("payinAddress"), 
                        json.getString("payoutAddress"), 
                        json.getString("fromCurrency"), 
                        json.getString("toCurrency")
                    )
                )
            } else {
                Result.failure(Exception("Failed to create exchange: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
