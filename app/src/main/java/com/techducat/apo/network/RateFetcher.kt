package com.techducat.apo.network

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.ui.res.stringResource

suspend fun fetchXMRUSDRate(): Result<Double> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=monero&vs_currencies=usd")
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        // Use hardcoded string instead of stringResource()
        connection.setRequestProperty("Accept", "application/json")
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val rate = json.getJSONObject("monero").getDouble("usd")
            Result.success(rate)
        } else {
            Result.failure(Exception("Failed to fetch USD rate"))
        }
    } catch (e: Exception) {
        Timber.e("USDRate", "Error fetching USD rate", e)
        Result.failure(e)
    } finally {
        connection?.disconnect()
    }
}
