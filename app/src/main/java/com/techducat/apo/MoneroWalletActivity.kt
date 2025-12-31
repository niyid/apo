package com.techducat.apo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import org.json.JSONObject
import java.util.*
import timber.log.Timber
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.m2049r.xmrwallet.model.TransactionInfo
import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.DialogProperties

// ============================================================================
// QR CODE GENERATOR
// ============================================================================

fun generateQRCode(content: String, size: Int = 512): Bitmap? {
    return try {
        val hints = hashMapOf<EncodeHintType, Any>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        hints[EncodeHintType.MARGIN] = 1
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        
        bitmap
    } catch (e: Exception) {
        Timber.e(e, "QR Code generation failed")
        null
    }
}

// ============================================================================
// CHANGENOW SWAP SERVICE
// ============================================================================

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

// ============================================================================
// DATA MODELS
// ============================================================================

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

enum class TransactionPriority(val value: Int, val displayName: String, val feeMultiplier: Double) {
    LOW(1, "Slow & Cheaper", 0.5),
    MEDIUM(2, "Normal", 1.0),
    HIGH(3, "Fast & Standard", 1.5),
    URGENT(4, "Urgent & Fastest", 2.0);

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

// ============================================================================
// WALLET DATA STORE
// ============================================================================

class WalletDataStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("wallet_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_ADDRESS_BOOK = "address_book"
        private const val KEY_TRANSACTION_NOTES = "tx_notes"
        private const val KEY_SUBADDRESSES = "subaddresses"
        private const val KEY_PREFERRED_CURRENCY = "preferred_currency"
        private const val KEY_DEFAULT_PRIORITY = "default_priority"
        private const val KEY_AUTO_BACKUP = "auto_backup"
        private const val KEY_WATCH_ONLY = "watch_only"
    }
    
    fun saveAddressBook(entries: List<AddressBookEntry>) {
        prefs.edit().putString(KEY_ADDRESS_BOOK, gson.toJson(entries)).apply()
    }
    
    fun loadAddressBook(): List<AddressBookEntry> {
        val json = prefs.getString(KEY_ADDRESS_BOOK, "[]") ?: "[]"
        val type = object : TypeToken<List<AddressBookEntry>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun saveTransactionNotes(notes: Map<String, TransactionNote>) {
        prefs.edit().putString(KEY_TRANSACTION_NOTES, gson.toJson(notes)).apply()
    }
    
    fun loadTransactionNotes(): Map<String, TransactionNote> {
        val json = prefs.getString(KEY_TRANSACTION_NOTES, "{}") ?: "{}"
        val type = object : TypeToken<Map<String, TransactionNote>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
    
    fun addTransactionNote(txId: String, note: String) {
        val notes = loadTransactionNotes().toMutableMap()
        notes[txId] = TransactionNote(txId, note)
        saveTransactionNotes(notes)
    }
    
    fun saveSubaddresses(subaddresses: List<Subaddress>) {
        prefs.edit().putString(KEY_SUBADDRESSES, gson.toJson(subaddresses)).apply()
    }
    
    fun loadSubaddresses(): List<Subaddress> {
        val json = prefs.getString(KEY_SUBADDRESSES, "[]") ?: "[]"
        val type = object : TypeToken<List<Subaddress>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun addSubaddress(subaddress: Subaddress) {
        val subaddresses = loadSubaddresses().toMutableList()
        subaddresses.add(subaddress)
        saveSubaddresses(subaddresses)
    }
    
    fun savePreferredCurrency(currency: FiatCurrency) {
        prefs.edit().putString(KEY_PREFERRED_CURRENCY, gson.toJson(currency)).apply()
    }
    
    fun loadPreferredCurrency(): FiatCurrency {
        val json = prefs.getString(KEY_PREFERRED_CURRENCY, null)
        return if (json != null) {
            gson.fromJson(json, FiatCurrency::class.java)
        } else {
            FiatCurrency("USD", "$", "US Dollar")
        }
    }
    
    fun getAvailableCurrencies(): List<FiatCurrency> {
        return listOf(
            FiatCurrency("USD", "$", "US Dollar"),
            FiatCurrency("EUR", "€", "Euro"),
            FiatCurrency("GBP", "£", "British Pound"),
            FiatCurrency("JPY", "¥", "Japanese Yen"),
            FiatCurrency("CAD", "$", "Canadian Dollar"),
            FiatCurrency("AUD", "$", "Australian Dollar"),
            FiatCurrency("CNY", "¥", "Chinese Yuan"),
            FiatCurrency("INR", "₹", "Indian Rupee")
        )
    }
    
    fun saveDefaultPriority(priority: TransactionPriority) {
        prefs.edit().putString(KEY_DEFAULT_PRIORITY, priority.name).apply()
    }
    
    fun loadDefaultPriority(): TransactionPriority {
        val name = prefs.getString(KEY_DEFAULT_PRIORITY, null)
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
    
    fun saveAutoBackup(enabled: Boolean, intervalHours: Int) {
        prefs.edit().apply {
            putBoolean("auto_backup_enabled", enabled)
            putInt("auto_backup_interval", intervalHours)
            apply()
        }
    }
    
    fun loadAutoBackup(): Pair<Boolean, Int> {
        val enabled = prefs.getBoolean("auto_backup_enabled", false)
        val interval = prefs.getInt("auto_backup_interval", 24)
        return Pair(enabled, interval)
    }

    fun saveWatchOnlyMode(enabled: Boolean) {
        prefs.edit().putBoolean("watch_only_mode", enabled).apply()
    }
    
    fun loadWatchOnlyMode(): Boolean {
        return prefs.getBoolean("watch_only_mode", false)
    }
    
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}

// ============================================================================
// URI HANDLER
// ============================================================================

object MoneroUriHandler {
    fun parseUri(uri: String): PaymentRequest? {
        return try {
            if (!uri.startsWith("monero:")) return null
            
            val cleanUri = if (uri.startsWith("monero://")) {
                uri.replaceFirst("monero://", "monero:")
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
        return address.startsWith("4") || address.startsWith("8") && address.length >= 95 && address.length <= 106
    }
}

// ============================================================================
// TRANSACTION FILTERS & UTILITIES
// ============================================================================

class TransactionFilterManager(private val dataStore: WalletDataStore) {
    fun filterTransactions(
        transactions: List<Transaction>,
        filterState: TransactionFilterState
    ): List<Transaction> {
        return transactions.filter { tx ->
            val typeMatch = when (filterState.filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.SENT -> tx.type == "Sent"
                TransactionFilter.RECEIVED -> tx.type == "Received"
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
                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).parse(it.date)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
            else -> transactions
        }.let { if (!ascending) it.reversed() else it }
    }
}

// ============================================================================
// EXPORT MANAGER
// ============================================================================

object ExportManager {
    fun exportToCsv(transactions: List<Transaction>, notes: Map<String, TransactionNote>): String {
        val csv = StringBuilder()
        csv.append("Type,Amount,Date,Confirmed,TxID,Note\n")
        
        transactions.forEach { tx ->
            val note = notes[tx.txId]?.note ?: ""
            csv.append("\"${tx.type}\",")
            csv.append("\"${tx.amount}\",")
            csv.append("\"${tx.date}\",")
            csv.append("\"${tx.confirmed}\",")
            csv.append("\"${tx.txId}\",")
            csv.append("\"${note.replace("\"", "\"\"")}\"\n")
        }
        
        return csv.toString()
    }
    
    fun exportToJson(transactions: List<Transaction>, notes: Map<String, TransactionNote>): String {
        val data = mapOf(
            "export_date" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
            "transactions" to transactions.map { tx ->
                mapOf(
                    "type" to tx.type,
                    "amount" to tx.amount,
                    "date" to tx.date,
                    "confirmed" to tx.confirmed,
                    "tx_id" to tx.txId,
                    "note" to (notes[tx.txId]?.note ?: "")
                )
            }
        )
        
        return Gson().toJson(data)
    }
}

// ============================================================================
// MAIN ACTIVITY
// ============================================================================

class MoneroWalletActivity : ComponentActivity() {
    private lateinit var walletSuite: WalletSuite
    private lateinit var dataStore: WalletDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        dataStore = WalletDataStore(this)
        
        if (!PermissionHandler.hasStoragePermissions(this)) {
            Timber.w("MoneroWallet", "Storage permission not granted - requesting")
            PermissionHandler.requestStoragePermissions(this)
        }
        
        PermissionHandler.logPermissionStatus(this)
        
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        
        walletSuite = WalletSuite.getInstance(this)
            
        setContent {
            MoneroWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneroWalletScreen(walletSuite, dataStore)
                }
            }
        }
        
        lifecycleScope.launch {
            delay(1500)
            keepSplashOnScreen = false
        }
    }
    
    override fun onDestroy() {
        if (isFinishing) {
            Timber.d("MoneroWallet", "Activity finishing - closing wallet")
            try {
                walletSuite.close()
            } catch (e: Exception) {
                Timber.e("MoneroWallet", "Error closing wallet", e)
            }
        } else {
            Timber.d("MoneroWallet", "Activity destroyed but not finishing - keeping wallet open")
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        PermissionHandler.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                Timber.i("MoneroWallet", "Storage permission granted")
            },
            onDenied = {
                Timber.w("MoneroWallet", "Storage permission denied")
                showPermissionDeniedDialogTraditional()
            }
        )
    }

    private fun showPermissionDeniedDialogTraditional() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage("Storage permission is required to save wallet data, create backups, and export transaction history.")
            .setPositiveButton("Grant Permission") { _, _ ->
                PermissionHandler.requestStoragePermissions(this)
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    } 
}

@Composable
fun MoneroWalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF6600),
            primaryContainer = Color(0xFFFF8833),
            secondary = Color(0xFF4D4D4D),
            tertiary = Color(0xFF4CAF50),
            background = Color(0xFF0F0F0F),
            surface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFF252525),
            onPrimary = Color.White,
            onBackground = Color(0xFFE0E0E0),
            onSurface = Color(0xFFE0E0E0),
            error = Color(0xFFCF6679)
        ),
        content = content
    )
}

// ============================================================================
// USD RATE FETCHER
// ============================================================================

suspend fun fetchXMRUSDRate(): Result<Double> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=monero&vs_currencies=usd")
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
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

// ============================================================================
// MAIN WALLET SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneroWalletScreen(walletSuite: WalletSuite, dataStore: WalletDataStore) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var balance by remember { mutableStateOf(0L) }
    var unlockedBalance by remember { mutableStateOf(0L) }
    var lockedBalance by remember { mutableStateOf(0L) }
    var usdRate by remember { mutableStateOf<Double?>(null) }
    var isLoadingRate by remember { mutableStateOf(false) }
    var rateError by remember { mutableStateOf<String?>(null) }
    var syncProgress by remember { mutableStateOf(0.0) }
    var isSyncing by remember { mutableStateOf(false) }
    var walletAddress by remember { mutableStateOf("") }
    var walletHeight by remember { mutableStateOf(0L) }
    var daemonHeight by remember { mutableStateOf(0L) }
    var showReceiveDialog by remember { mutableStateOf(false) }
    var sendSuccess by remember { mutableStateOf<String?>(null) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var initMessage by remember { mutableStateOf("") }
    var initError by remember { mutableStateOf<String?>(null) }
    
    // Add a state to trigger retry
    var retryTrigger by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        isLoadingRate = true
        fetchXMRUSDRate()
            .onSuccess { rate ->
                usdRate = rate
                rateError = null
                isLoadingRate = false
            }
            .onFailure { error ->
                rateError = error.message
                isLoadingRate = false
            }
        
        while (true) {
            delay(60000)
            isLoadingRate = true
            fetchXMRUSDRate()
                .onSuccess { rate ->
                    usdRate = rate
                    rateError = null
                    isLoadingRate = false
                }
                .onFailure { error ->
                    rateError = error.message
                    isLoadingRate = false
                }
        }
    }
    
    LaunchedEffect(Unit) {
        walletSuite.setWalletStatusListener(object : WalletSuite.WalletStatusListener {
            override fun onWalletInitialized(success: Boolean, message: String) {
                isInitialized = success
                initMessage = message
                if (success) {
                    walletAddress = walletSuite.cachedAddress ?: ""
                    balance = walletSuite.balanceValue
                    unlockedBalance = walletSuite.unlockedBalanceValue
                    lockedBalance = balance - unlockedBalance
                } else {
                    initError = message
                }
            }
            
            override fun onBalanceUpdated(bal: Long, unl: Long) {
                balance = bal
                unlockedBalance = unl
                lockedBalance = bal - unl
            }
            
            override fun onSyncProgress(h: Long, sh: Long, eh: Long, pd: Double) {
                walletHeight = h
                daemonHeight = eh
                syncProgress = pd
                isSyncing = pd < 100.0
            }
        })
        
        try {
            if (!walletSuite.isReady) {
                walletSuite.initializeWallet()
            } else {
                isInitialized = true
                walletAddress = walletSuite.cachedAddress ?: ""
                balance = walletSuite.balanceValue
                unlockedBalance = walletSuite.unlockedBalanceValue
                lockedBalance = balance - unlockedBalance
            }
        } catch (e: Exception) {
            initError = e.message ?: context.getString(R.string.failed_to_initialize_wallet)
            isInitialized = false
        }
    }
    
    // Separate LaunchedEffect for retrying initialization
    LaunchedEffect(retryTrigger) {
        if (retryTrigger > 0) {
            try {
                initError = null
                isInitialized = false
                initMessage = context.getString(R.string.retrying_initialization)
                walletSuite.initializeWallet()
            } catch (e: Exception) {
                initError = e.message ?: context.getString(R.string.failed_to_initialize_wallet)
            }
        }
    }
    
    if (initError != null) {
        ErrorScreen(
            message = initError ?: context.getString(R.string.unknown_error),
            onRetry = {
                // Increment retryTrigger to trigger the LaunchedEffect above
                retryTrigger++
            }
        )
        return
    }
    
    if (!isInitialized) {
        LoadingScreen(initMessage)
        return
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            TwoRowNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    balance, unlockedBalance, lockedBalance, usdRate,
                    walletAddress, syncProgress, isSyncing,
                    walletHeight, daemonHeight,
                    { walletSuite.triggerImmediateSync() },
                    { showReceiveDialog = true },
                    { selectedTab = 1 },
                    { selectedTab = 4 }
                )
                1 -> SendScreen(
                    walletSuite, unlockedBalance,
                    { addr, amt ->
                        walletSuite.sendTransaction(addr, amt, object : WalletSuite.TransactionCallback {
                            override fun onSuccess(txId: String, amount: Long) {
                                sendSuccess = txId
                                sendError = null
                            }
                            override fun onError(error: String) {
                                sendError = error
                                sendSuccess = null
                            }
                        })
                    },
                    sendSuccess, sendError,
                    { sendSuccess = null; sendError = null }
                )
                2 -> HistoryScreen(walletSuite)
                3 -> SettingsScreen(walletSuite, walletAddress)
                4 -> ExchangeScreen(walletSuite, walletAddress, unlockedBalance)
                5 -> AddressBookScreen(
                    dataStore,
                    onSelectAddress = { selectedAddress ->
                        // Store selected address for enhanced send
                        selectedTab = 8
                    },
                    onClose = { selectedTab = 0 }
                )
                6 -> SubaddressScreen(
                    walletSuite = walletSuite,
                    dataStore = dataStore,
                    onBack = { selectedTab = 0 }
                )
                7 -> PaymentRequestScreen(
                    walletAddress = walletAddress,
                    onBack = { selectedTab = 0 }
                )
                8 -> EnhancedSendScreen(
                    walletSuite = walletSuite,
                    dataStore = dataStore,
                    unlockedBalance = unlockedBalance,
                    onBack = { selectedTab = 1 },
                    onSendComplete = { message ->
                        sendSuccess = message
                        selectedTab = 0
                    }
                )
            }
        }
    }
    
    if (showReceiveDialog) {
        ReceiveDialog(walletAddress) { showReceiveDialog = false }
    }
}

// ============================================================================
// CUSTOM TWO-ROW NAVIGATION BAR
// ============================================================================

@Composable
fun TwoRowNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // First Row: Main Navigation (4 items)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = stringResource(R.string.nav_wallet),
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                NavItem(
                    icon = Icons.Default.Send,
                    label = stringResource(R.string.nav_send),
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
                NavItem(
                    icon = Icons.Default.History,
                    label = stringResource(R.string.nav_history),
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
                NavItem(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.nav_settings),
                    selected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
                )
            }
            
            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
            
            // Second Row: Additional Features (5 items)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(
                    icon = Icons.Default.SwapHoriz,
                    label = stringResource(R.string.nav_exchange),
                    selected = selectedTab == 4,
                    onClick = { onTabSelected(4) },
                    enabled = ChangeNowSwapService.isConfigured()
                )
                NavItem(
                    icon = Icons.Default.ContactPage,
                    label = stringResource(R.string.nav_contacts),
                    selected = selectedTab == 5,
                    onClick = { onTabSelected(5) }
                )
                NavItem(
                    icon = Icons.Default.LocationOn,
                    label = stringResource(R.string.nav_subaddresses),
                    selected = selectedTab == 6,
                    onClick = { onTabSelected(6) }
                )
                NavItem(
                    icon = Icons.Default.QrCode,
                    label = stringResource(R.string.nav_request),
                    selected = selectedTab == 7,
                    onClick = { onTabSelected(7) }
                )
                NavItem(
                    icon = Icons.Default.Tune,
                    label = stringResource(R.string.nav_enhanced_send_short),
                    selected = selectedTab == 8,
                    onClick = { onTabSelected(8) }
                )
            }
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val color by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        },
        label = "navItemColor"
    )
    
    Column(
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .widthIn(min = 48.dp, max = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ============================================================================
// EXPORT KEYS DIALOG
// ============================================================================

@Composable
fun ExportKeysDialog(
    walletSuite: WalletSuite,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var viewKeyCopied by remember { mutableStateOf(false) }
    var spendKeyCopied by remember { mutableStateOf(false) }
    
    val viewKey = remember { walletSuite.viewKey }
    val spendKey = remember { walletSuite.spendKey }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Export Wallet Keys",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Security Warning!",
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Text(
                    text = "Anyone with these keys can access your funds. Store them securely and never share them.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                KeyDisplayCard(
                    label = "View Key",
                    keyValue = viewKey,
                    isCopied = viewKeyCopied,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(viewKey))
                        viewKeyCopied = true
                    }
                )
                
                KeyDisplayCard(
                    label = "Spend Key",
                    keyValue = spendKey,
                    isCopied = spendKeyCopied,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(spendKey))
                        spendKeyCopied = true
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
    
    LaunchedEffect(viewKeyCopied, spendKeyCopied) {
        if (viewKeyCopied) {
            delay(2000)
            viewKeyCopied = false
        }
        if (spendKeyCopied) {
            delay(2000)
            spendKeyCopied = false
        }
    }
}

@Composable
fun KeyDisplayCard(
    label: String,
    keyValue: String,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (keyValue.length > 40)
                        "${keyValue.take(20)}...${keyValue.takeLast(20)}"
                    else keyValue,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = if (isCopied)
                            Icons.Default.Check
                        else
                            Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (isCopied)
                            Color(0xFF4CAF50)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ============================================================================
// SECURITY SETTINGS DIALOG
// ============================================================================

@Composable
fun SecuritySettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var biometricEnabled by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        try {
            val prefs = context.getSharedPreferences("wallet_security", android.content.Context.MODE_PRIVATE)
            biometricEnabled = prefs.getBoolean("biometric_enabled", false)
            pinEnabled = prefs.getBoolean("pin_enabled", false)
        } catch (e: Exception) {
            Timber.d("Security", "Default settings not found, using defaults")
        }
        isLoading = false
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Security Settings",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else {
                    SecurityOption(
                        title = "Biometric Authentication",
                        subtitle = "Use fingerprint or face recognition to access wallet",
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it },
                        enabled = !isSaving
                    )
                    
                    SecurityOption(
                        title = "PIN Protection",
                        subtitle = "Require PIN code for sensitive operations",
                        checked = pinEnabled,
                        onCheckedChange = { pinEnabled = it },
                        enabled = !isSaving
                    )
                    
                    Text(
                        text = "Note: Full implementation requires additional security modules",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val prefs = context.getSharedPreferences("wallet_security", android.content.Context.MODE_PRIVATE)
                            prefs.edit().apply {
                                putBoolean("biometric_enabled", biometricEnabled)
                                putBoolean("pin_enabled", pinEnabled)
                                apply()
                            }
                            
                            delay(300)
                            onDismiss()
                        } catch (e: Exception) {
                            Timber.e("Security", "Error saving security settings", e)
                            isSaving = false
                        }
                    }
                },
                enabled = !isLoading && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = "Save")
            }
        }
    )
}

@Composable
fun SecurityOption(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface 
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

// ============================================================================
// LOADING SCREEN
// ============================================================================

@Composable
fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp).rotate(rotation),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            
            Text(
                text = message.ifEmpty { "Initializing Wallet..." },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// ERROR SCREEN
// ============================================================================

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = "Error",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = onRetry,
                modifier = Modifier.width(200.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

// ============================================================================
// HOME SCREEN
// ============================================================================

@Composable
fun HomeScreen(
    balance: Long,
    unlockedBalance: Long,
    lockedBalance: Long,
    usdRate: Double?,
    walletAddress: String,
    syncProgress: Double,
    isSyncing: Boolean,
    walletHeight: Long,
    daemonHeight: Long,
    onRefresh: () -> Unit,
    onReceiveClick: () -> Unit,
    onSendClick: () -> Unit,
    onExchangeClick: () -> Unit = {}
) {
    val balanceXMR = WalletSuite.convertAtomicToXmr(balance).toDoubleOrNull() ?: 0.0
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance).toDoubleOrNull() ?: 0.0
    val lockedXMR = WalletSuite.convertAtomicToXmr(lockedBalance).toDoubleOrNull() ?: 0.0
    val clipboardManager = LocalClipboardManager.current
    var addressCopied by remember { mutableStateOf(false) }
    
    LaunchedEffect(addressCopied) {
        if (addressCopied) {
            delay(2000)
            addressCopied = false
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFF6600), Color(0xFFFF8833), Color(0xFFFFAA66))
                        )
                    ).padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Balance",
                                color = Color.White.copy(0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            val infiniteTransition = rememberInfiniteTransition(label = "refresh")
                            val rotation by infiniteTransition.animateFloat(
                                0f, if (isSyncing) 360f else 0f,
                                infiniteRepeatable(tween(1000, easing = LinearEasing)),
                                label = "rotation"
                            )
                            
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    Icons.Default.Refresh, "Refresh",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp).rotate(if (isSyncing) rotation else 0f)
                                )
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                String.format("%.6f %s", balanceXMR, stringResource(R.string.monero_symbol)),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            usdRate?.let { rate ->
                                val usdValue = balanceXMR * rate
                                Text(
                                    String.format("≈ $%.2f USD", usdValue),
                                    color = Color.White.copy(0.9f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Unlocked",
                                    color = Color.White.copy(0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    String.format("%.6f %s", unlockedXMR, stringResource(R.string.monero_symbol)),
                                    color = Color(0xFF1B5E20),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Locked",
                                    color = Color.White.copy(0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    String.format("%.6f %s", lockedXMR, stringResource(R.string.monero_symbol)),
                                    color = Color.White.copy(0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        if (isSyncing) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LinearProgressIndicator(
                                    progress = (syncProgress / 100.0).toFloat(),
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(0.3f)
                                )
                                Text(
                                    String.format("Syncing: %.1f%% (%d/%d)", syncProgress, walletHeight, daemonHeight),
                                    color = Color.White.copy(0.9f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    Icons.Default.CallReceived, "Receive",
                    Modifier.weight(1f), onReceiveClick, Color(0xFF4CAF50)
                )
                QuickActionButton(
                    Icons.Default.Send, "Send",
                    Modifier.weight(1f), onSendClick, Color(0xFFFF6600)
                )
                QuickActionButton(
                    Icons.Default.SwapHoriz, "Exchange",
                    Modifier.weight(1f), onExchangeClick, Color(0xFF9C27B0)
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Your Address", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (walletAddress.length > 40) 
                                    "${walletAddress.take(20)}...${walletAddress.takeLast(20)}"
                                else walletAddress,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )
                            
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(walletAddress))
                                addressCopied = true
                            }) {
                                Icon(
                                    if (addressCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    "Copy",
                                    tint = if (addressCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.height(110.dp).semantics(mergeDescendants = true) { },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ============================================================================
// SEND SCREEN
// ============================================================================

@Composable
fun SendScreen(
    walletSuite: WalletSuite,
    unlockedBalance: Long,
    onSend: (String, Double) -> Unit,
    successMessage: String? = null,
    errorMessage: String? = null,
    onDismissMessage: () -> Unit = {}
) {
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showConfirmation by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var showTxSearch by remember { mutableStateOf(false) }
    
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance).toDoubleOrNull() ?: 0.0
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(successMessage, errorMessage) {
        successMessage?.let {
            scope.launch {
                snackbarHost.showSnackbar(
                    "Transaction sent! TX: ${it.take(16)}...",
                    duration = SnackbarDuration.Long
                )
            }
        }
        errorMessage?.let {
            scope.launch {
                snackbarHost.showSnackbar(it, duration = SnackbarDuration.Long)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Send XMR", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Available Balance", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            Text(
                                String.format("%.6f %s", unlockedXMR, stringResource(R.string.monero_symbol)), 
                                fontSize = 28.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF4CAF50).copy(0.3f), modifier = Modifier.size(48.dp))
                    }
                }
            }
            
            item {
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Recipient Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = { 
                            scope.launch {
                                snackbarHost.showSnackbar(
                                    "QR scanner not implemented in this version",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }) {
                            Icon(Icons.Default.QrCode, "Scan QR")
                        }
                    },
                    maxLines = 3
                )
            }
            
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        TextButton(onClick = { amount = unlockedXMR.toString() }) {
                            Text("MAX", fontWeight = FontWeight.Bold)
                        }
                    },
                    singleLine = true,
                    prefix = { Text(stringResource(R.string.monero_symbol) + " ") }
                )
            }
            
            item {
                Button(
                    onClick = { 
                        if (!isSending) showConfirmation = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = recipient.isNotEmpty() && amount.isNotEmpty() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Creating Transaction...")
                    } else {
                        Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Send Transaction", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            item {
                TextButton(
                    onClick = { showTxSearch = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Search Transaction")
                }
            }
        }
        
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
    
    if (showConfirmation) {
        TransactionConfirmDialog(
            recipient, amount, isSending,
            {
                isSending = true
                onSend(recipient, amount.toDoubleOrNull() ?: 0.0)
                showConfirmation = false
                recipient = ""
                amount = ""
                isSending = false
            },
            { if (!isSending) showConfirmation = false }
        )
    }
    
    if (showTxSearch) {
        TransactionSearchDialog(walletSuite) { showTxSearch = false }
    }
}

@Composable
fun TransactionConfirmDialog(
    recipient: String,
    amount: String,
    isSending: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = { Text("Confirm Transaction", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Amount", fontWeight = FontWeight.SemiBold)
                        Text(
                            "$amount XMR", 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 18.sp
                        )
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("To", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (recipient.length > 30) "${recipient.take(15)}...${recipient.takeLast(15)}" else recipient,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                    Text(
                        "Monero transactions are irreversible. Please verify the address before confirming.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isSending) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Confirm Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSending) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================================
// RECEIVE DIALOG
// ============================================================================

@Composable
fun ReceiveDialog(address: String, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    
    val qrBitmap = remember(address) {
        generateQRCode(address, 512)
    }
    
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Monero", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your Address", fontWeight = FontWeight.SemiBold)
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (qrBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier.size(200.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        "Generating QR Code...",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        Text(
                            address,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            maxLines = 3
                        )
                    }
                }
                
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(address))
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (copied) "Address Copied!" else "Copy Address")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ============================================================================
// TRANSACTION SEARCH DIALOG
// ============================================================================

@Composable
fun TransactionSearchDialog(walletSuite: WalletSuite, onDismiss: () -> Unit) {
    var txId by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Get the string resource outside the callback
    val moneroSymbol = stringResource(R.string.monero_symbol)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Transaction", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Enter a transaction ID to search and import it", fontSize = 12.sp)
                
                OutlinedTextField(
                    value = txId,
                    onValueChange = { 
                        txId = it
                        searchResult = null
                        errorMessage = null
                    },
                    label = { Text("Transaction ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSearching,
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                
                searchResult?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                            Text(it, color = Color(0xFF4CAF50), fontSize = 13.sp)
                        }
                    }
                }
                
                errorMessage?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error.copy(0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSearching = true
                    errorMessage = null
                    searchResult = null
                    
                    walletSuite.searchAndImportTransaction(txId, object : WalletSuite.TransactionSearchCallback {
                        override fun onTransactionFound(txId: String, amount: Long, confirmations: Long, blockHeight: Long) {
                            isSearching = false
                            searchResult = "Transaction found! Amount: ${WalletSuite.convertAtomicToXmr(amount)} $moneroSymbol, Confirmations: $confirmations"
                        }
                        
                        override fun onTransactionNotFound(txId: String) {
                            isSearching = false
                            errorMessage = "Transaction not found"
                        }
                        
                        override fun onError(error: String) {
                            isSearching = false
                            errorMessage = "Error: $error"
                        }
                    })
                },
                enabled = txId.isNotEmpty() && !isSearching
            ) {
                Text("Search")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ============================================================================
// HISTORY SCREEN
// ============================================================================

@Composable
fun HistoryScreen(walletSuite: WalletSuite) {
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            walletSuite.getTransactionHistory(object : WalletSuite.TransactionHistoryCallback {
                override fun onSuccess(txList: List<TransactionInfo>) {
                    transactions = txList.map { txInfo ->
                        val isReceived = txInfo.direction == TransactionInfo.Direction.Direction_In
                        val absoluteAmount = if (txInfo.amount < 0) -txInfo.amount else txInfo.amount
                        Transaction(
                            type = if (isReceived) "Received" else "Sent",
                            amount = String.format(
                                "%.6f XMR",
                                WalletSuite.convertAtomicToXmr(absoluteAmount).toDoubleOrNull() ?: 0.0
                            ),
                            date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(txInfo.timestamp * 1000)),
                            confirmed = !txInfo.isPending,
                            txId = txInfo.hash
                        )
                    }
                    isLoading = false
                }
                
                override fun onError(error: String) {
                    errorMessage = error
                    isLoading = false
                }
            })
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction History",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    IconButton(onClick = { 
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            delay(500)
                            isLoading = false
                        }
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                errorMessage ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            items(transactions) { tx ->
                TransactionCard(tx)
            }
            
            if (transactions.isEmpty() && !isLoading && errorMessage == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "No transactions yet",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var txIdCopied by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val isReceived = transaction.type == "Received"
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isReceived) 
                                    Color(0xFF4CAF50).copy(alpha = 0.2f)
                                else 
                                    Color(0xFFFF6600).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isReceived) Icons.Default.CallReceived else Icons.Default.Send,
                            contentDescription = null,
                            tint = if (isReceived) Color(0xFF4CAF50) else Color(0xFFFF6600),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.type,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = transaction.date,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (transaction.type == "Received") "+" else "-"}${transaction.amount}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type == "Received") 
                            Color(0xFF4CAF50) 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    if (transaction.confirmed) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Confirmed",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Pending",
                                fontSize = 11.sp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Transaction ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (transaction.txId.length > 40)
                                "${transaction.txId.take(20)}...${transaction.txId.takeLast(20)}"
                            else transaction.txId,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(transaction.txId))
                                txIdCopied = true
                            }
                        ) {
                            Icon(
                                if (txIdCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (txIdCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(txIdCopied) {
        if (txIdCopied) {
            delay(2000)
            txIdCopied = false
        }
    }
}

// ============================================================================
// EXCHANGE SCREEN
// ============================================================================

@Composable
fun ExchangeScreen(walletSuite: WalletSuite, walletAddress: String, unlockedBalance: Long) {
    
    if (!ChangeNowSwapService.isConfigured()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Exchange Service Not Available",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "API key not configured. Please check your build configuration.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
        return
    }
    
    val changeNowService = remember { ChangeNowSwapService() }
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    
    var fromCurrency by remember { mutableStateOf("xmr") }
    var toCurrency by remember { mutableStateOf("btc") }
    var fromAmount by remember { mutableStateOf("") }
    var estimatedAmount by remember { mutableStateOf<Double?>(null) }
    var toAddress by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var exchangeStatus by remember { mutableStateOf<ChangeNowSwapService.ExchangeStatus?>(null) }
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance).toDoubleOrNull() ?: 0.0
    
    LaunchedEffect(fromAmount, fromCurrency, toCurrency) {
        if (fromAmount.isNotEmpty()) {
            val amount = fromAmount.toDoubleOrNull()
            if (amount != null && amount > 0) {
                delay(500)
                isLoading = true
                
                changeNowService.getEstimate(fromCurrency, toCurrency, amount)
                    .onSuccess { estimate ->
                        estimatedAmount = estimate.estimatedAmount
                        isLoading = false
                    }
                    .onFailure { 
                        estimatedAmount = null
                        isLoading = false
                    }
            }
        } else { 
            estimatedAmount = null 
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { 
                Text(
                    text = "Exchange", 
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.Bold
                ) 
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(16.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Available XMR", 
                                fontSize = 14.sp, 
                                color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                            )
                            Text(
                                String.format("%.6f %s", unlockedXMR, stringResource(R.string.monero_symbol)), 
                                fontSize = 24.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Icon(
                            Icons.Default.SwapHoriz, 
                            null, 
                            tint = Color(0xFF9C27B0).copy(0.3f), 
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(16.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp), 
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("From", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                fromCurrency.uppercase(), 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold, 
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = fromAmount, 
                                onValueChange = { fromAmount = it }, 
                                modifier = Modifier.weight(2f), 
                                singleLine = true, 
                                placeholder = { Text("Enter amount") },
                                prefix = { Text(stringResource(R.string.monero_symbol) + " ") },
                                trailingIcon = { 
                                    if (fromCurrency == "xmr") { 
                                        TextButton(onClick = { fromAmount = unlockedXMR.toString() }) { 
                                            Text("MAX", fontWeight = FontWeight.Bold, fontSize = 12.sp) 
                                        } 
                                    } 
                                }
                            )
                        }
                    }
                }
            }
            
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { 
                            val t = fromCurrency
                            fromCurrency = toCurrency
                            toCurrency = t
                            fromAmount = ""
                            estimatedAmount = null 
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, tint = Color.White)
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(16.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp), 
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("To", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                toCurrency.uppercase(), 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold, 
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant), 
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) { 
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp), 
                                        strokeWidth = 2.dp
                                    ) 
                                } else { 
                                    Text(
                                        text = estimatedAmount?.let { String.format("%.6f", it) } ?: "0.0", 
                                        fontSize = 16.sp, 
                                        fontWeight = FontWeight.Medium
                                    ) 
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                OutlinedTextField(
                    value = toAddress, 
                    onValueChange = { toAddress = it }, 
                    label = { Text("Recipient ${toCurrency.uppercase()} Address") },
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(16.dp), 
                    maxLines = 3
                )
            }
            
            item {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            
                            val result = changeNowService.createExchange(
                                fromCurrency, 
                                toCurrency, 
                                fromAmount.toDoubleOrNull() ?: 0.0, 
                                toAddress, 
                                if (fromCurrency == "xmr") walletAddress else null
                            )
                            
                            result.onSuccess { status ->
                                exchangeStatus = status
                                snackbarHost.showSnackbar(
                                    "Exchange created! Send ${fromCurrency.uppercase()} to: ${status.payinAddress}"
                                )
                            }.onFailure { 
                                snackbarHost.showSnackbar("Exchange failed: ${it.message ?: "Unknown error"}")
                            }
                            
                            isLoading = false
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth().height(56.dp), 
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading && estimatedAmount != null && toAddress.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Creating Exchange...")
                    } else {
                        Icon(Icons.Default.SwapHoriz, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Exchange", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            exchangeStatus?.let { status ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(16.dp), 
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                        Column(
                            modifier = Modifier.padding(20.dp), 
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Exchange Status", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Status: ${status.status.uppercase()}", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Send to: ${status.payinAddress}", 
                                fontSize = 10.sp, 
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ============================================================================
// SETTINGS SCREEN
// ============================================================================

@Composable
fun SettingsScreen(
    walletSuite: WalletSuite,
    walletAddress: String
) {
    var showRescanDialog by remember { mutableStateOf(false) }
    var showSeedDialog by remember { mutableStateOf(false) }
    var showNodeDialog by remember { mutableStateOf(false) }
    var showTxSearchDialog by remember { mutableStateOf(false) }
    var showExportKeysDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var rescanProgress by remember { mutableStateOf(0.0) }
    var isRescanning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        walletSuite.setRescanBalanceCallback(object : WalletSuite.RescanBalanceCallback {
            override fun onBalanceUpdated(balance: Long, unlockedBalance: Long) {
                val status = walletSuite.syncStatus
                rescanProgress = status.percentDone
                isRescanning = status.syncing
            }
        })
    }

    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Wallet Info", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (walletSuite.isSyncing) "Syncing" else "Ready",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Maintenance",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            SettingsCard(
                title = "Rescan Blockchain",
                subtitle = if (isRescanning) "Rescanning: ${String.format("%.1f%%", rescanProgress)}" else "Rescan wallet to fix balance issues",
                icon = Icons.Default.Refresh,
                onClick = { showRescanDialog = true },
                enabled = !isRescanning
            )
        }

        item {
            SettingsCard(
                title = "Force Refresh",
                subtitle = "Force immediate wallet sync",
                icon = Icons.Default.Refresh,
                onClick = { walletSuite.triggerImmediateSync() }
            )
        }

        item {
            Text(
                text = "Backup & Security",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            SettingsCard(
                title = "View Seed Phrase",
                subtitle = "View your 25-word mnemonic seed",
                icon = Icons.Default.Key,
                onClick = { showSeedDialog = true }
            )
        }

        item {
            SettingsCard(
                title = "Export Keys",
                subtitle = "Export view and spend keys",
                icon = Icons.Default.Key,
                onClick = { showExportKeysDialog = true }
            )
        }

        item {
            Text(
                text = "Network",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            SettingsCard(
                title = "Node Settings",
                subtitle = "Current node: ${walletSuite.daemonAddress}:${walletSuite.daemonPort}",
                icon = Icons.Default.Cloud,
                onClick = { showNodeDialog = true }
            )
        }

        item {
            SettingsCard(
                title = "Transaction Search",
                subtitle = "Search and import transactions",
                icon = Icons.Default.Search,
                onClick = { showTxSearchDialog = true }
            )
        }

        item {
            SettingsCard(
                title = "Security Settings",
                subtitle = "Biometric & PIN protection",
                icon = Icons.Default.Lock,
                onClick = { showSecurityDialog = true }
            )
        }
    }

    if (showRescanDialog) {
        RescanDialog(
            onConfirm = { 
                showRescanDialog = false
                isRescanning = true
                walletSuite.rescanBlockchain() 
            },
            onDismiss = { showRescanDialog = false }
        )
    }

    if (showSeedDialog) {
        SeedPhraseDialog(
            walletSuite = walletSuite,
            onDismiss = { showSeedDialog = false }
        )
    }

    if (showNodeDialog) {
        NodeConfigDialog(
            walletSuite = walletSuite,
            onDismiss = { showNodeDialog = false }
        )
    }

    if (showTxSearchDialog) {
        TransactionSearchDialog(
            walletSuite = walletSuite,
            onDismiss = { showTxSearchDialog = false }
        )
    }

    if (showExportKeysDialog) {
        ExportKeysDialog(
            walletSuite = walletSuite,
            onDismiss = { showExportKeysDialog = false }
        )
    }

    if (showSecurityDialog) {
        SecuritySettingsDialog(
            onDismiss = { showSecurityDialog = false }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================================
// SUPPORTING DIALOGS
// ============================================================================

@Composable
fun RescanDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Rescan Blockchain",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Text(text = "This will rescan the entire blockchain to fix balance issues. It may take several minutes.") 
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = "Start Rescan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
fun SeedPhraseDialog(
    walletSuite: WalletSuite,
    onDismiss: () -> Unit
) {
    var seedPhrase by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        walletSuite.getSeedAsync(object : WalletSuite.SeedCallback {
            override fun onSuccess(seed: String) {
                seedPhrase = seed
                isLoading = false
            }
            
            override fun onError(errorMsg: String) {
                error = errorMsg
                isLoading = false
            }
        })
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Seed Phrase",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Security Warning!",
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "Anyone with this seed phrase can access your funds. Store it securely and never share it.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val clipboardManager = LocalClipboardManager.current
                        var seedCopied by remember { mutableStateOf(false) }
                        
                        when {
                            isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            error != null -> {
                                Text(
                                    text = error ?: "Unknown error",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                )
                            }
                            seedPhrase != null -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = seedPhrase ?: "",
                                        fontSize = 12.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            seedPhrase?.let {
                                                clipboardManager.setText(AnnotatedString(it))
                                                seedCopied = true
                                            }
                                        },
                                        enabled = seedPhrase != null
                                    ) {
                                        Icon(
                                            if (seedCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = if (seedCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        
                        LaunchedEffect(seedCopied) {
                            if (seedCopied) {
                                delay(2000)
                                seedCopied = false
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}

@Composable
fun NodeConfigDialog(
    walletSuite: WalletSuite,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Node Settings",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current Daemon",
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${walletSuite.daemonAddress}:${walletSuite.daemonPort}",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Text(
                    text = "To change the node, modify the wallet configuration.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}

// ============================================================================
// ADDRESS BOOK SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBookScreen(
    dataStore: WalletDataStore,
    onSelectAddress: (String) -> Unit = {},
    onClose: () -> Unit = {}
) {
    var entries by remember { mutableStateOf(dataStore.loadAddressBook()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<AddressBookEntry?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<AddressBookEntry?>(null) }
    val scope = rememberCoroutineScope()
    
    val filteredEntries = remember(entries, searchQuery) {
        entries.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true) ||
            it.notes.contains(searchQuery, ignoreCase = true)
        }.sortedWith(
            compareByDescending<AddressBookEntry> { it.isFavorite }
                .thenByDescending { it.dateAdded }
        )
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Address Book", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Contact")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Contacts") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            if (filteredEntries.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ContactPage,
                    title = if (searchQuery.isNotEmpty()) "No matching contacts" else "No Contacts",
                    message = if (searchQuery.isNotEmpty()) 
                        "Try a different search term"
                    else 
                        "Add your first contact to get started"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEntries) { entry ->
                        AddressBookCard(
                            entry = entry,
                            onEdit = { selectedEntry = entry },
                            onDelete = {
                                entryToDelete = entry
                                showDeleteConfirm = true
                            },
                            onToggleFavorite = {
                                scope.launch {
                                    entries = entries.map {
                                        if (it.id == entry.id) it.copy(isFavorite = !it.isFavorite) else it
                                    }
                                    dataStore.saveAddressBook(entries)
                                }
                            },
                            onSelect = { onSelectAddress(entry.address) }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog || selectedEntry != null) {
        AddEditAddressDialog(
            entry = selectedEntry,
            onSave = { updatedEntry ->
                scope.launch {
                    if (selectedEntry == null) {
                        entries = entries + updatedEntry
                    } else {
                        entries = entries.map { if (it.id == updatedEntry.id) updatedEntry else it }
                    }
                    dataStore.saveAddressBook(entries)
                    selectedEntry = null
                    showAddDialog = false
                }
            },
            onDismiss = {
                selectedEntry = null
                showAddDialog = false
            }
        )
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete ${entryToDelete?.name ?: "this contact"}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            entryToDelete?.let {
                                entries = entries.filter { e -> e.id != it.id }
                                dataStore.saveAddressBook(entries)
                            }
                            showDeleteConfirm = false
                            entryToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirm = false
                    entryToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddressBookCard(
    entry: AddressBookEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                entry.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (entry.isFavorite) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Remove Favorite",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(Date(entry.dateAdded)),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Row {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (entry.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (entry.isFavorite) "Remove Favorite" else "Add to Favorites",
                            tint = if (entry.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Text(
                entry.address,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (entry.notes.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        entry.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSelect,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun AddEditAddressDialog(
    entry: AddressBookEntry?,
    onSave: (AddressBookEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(entry?.name ?: "") }
    var address by remember { mutableStateOf(entry?.address ?: "") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }
    var isFavorite by remember { mutableStateOf(entry?.isFavorite ?: false) }
    var addressError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (entry == null) "Add Contact" else "Edit Contact",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isEmpty(),
                    supportingText = {
                        if (name.isEmpty()) {
                            Text("Name is required")
                        }
                    }
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { 
                        address = it
                        addressError = null
                    },
                    label = { Text("Monero Address") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    isError = addressError != null,
                    supportingText = {
                        if (addressError != null) {
                            Text(addressError ?: "")
                        } else if (!MoneroUriHandler.isMoneroAddress(address) && address.isNotEmpty()) {
                            Text("Doesn't look like a valid Monero address")
                        }
                    }
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add to Favorites", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isEmpty()) {
                        return@Button
                    }
                    
                    if (!MoneroUriHandler.isMoneroAddress(address)) {
                        addressError = "Invalid Monero address"
                        return@Button
                    }
                    
                    onSave(
                        entry?.copy(
                            name = name,
                            address = address,
                            notes = notes,
                            isFavorite = isFavorite
                        ) ?: AddressBookEntry(
                            name = name,
                            address = address,
                            notes = notes,
                            isFavorite = isFavorite
                        )
                    )
                },
                enabled = name.isNotEmpty() && MoneroUriHandler.isMoneroAddress(address),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (entry == null) "Add Contact" else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================================
// ENHANCED SEND SCREEN WITH FEATURES
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSendScreen(
    walletSuite: WalletSuite,
    dataStore: WalletDataStore,
    unlockedBalance: Long,
    onBack: () -> Unit = {},
    onSendComplete: (String) -> Unit = {}
) {
    var recipients by remember { mutableStateOf(listOf(RecipientInput(address = "", amount = ""))) }
    var selectedPriority by remember { mutableStateOf(dataStore.loadDefaultPriority()) }
    var showAddressBook by remember { mutableStateOf(false) }
    var showPriorityInfo by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    var activeRecipientIndex by remember { mutableStateOf(0) }
    var totalAmount by remember { mutableStateOf(0.0) }
    var estimatedFee by remember { mutableStateOf(0.0) }
    var isSending by remember { mutableStateOf(false) }
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance).toDoubleOrNull() ?: 0.0
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    
    // Calculate total amount and fee whenever recipients or priority changes
    LaunchedEffect(recipients, selectedPriority) {
        totalAmount = recipients.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        estimatedFee = totalAmount * 0.002 * selectedPriority.feeMultiplier
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh UI state when returning to this screen
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Send XMR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Available", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text(
                            String.format("%.6f %s", unlockedXMR, stringResource(R.string.monero_symbol)), 
                            fontSize = 24.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Total", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text(
                            String.format("%.6f %s", totalAmount, stringResource(R.string.monero_symbol)), 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Fee: ${String.format("%.6f %s", estimatedFee, stringResource(R.string.monero_symbol))}", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                    }
                }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Transaction Priority", fontWeight = FontWeight.SemiBold)
                        IconButton(
                            onClick = { showPriorityInfo = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Info, "Priority Info")
                        }
                    }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(TransactionPriority.values()) { priority ->
                            FilterChip(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority },
                                label = { 
                                    Text(
                                        when (priority) {
                                            TransactionPriority.LOW -> "Slow & Cheaper"
                                            TransactionPriority.MEDIUM -> "Normal"
                                            TransactionPriority.HIGH -> "Fast & Standard"
                                            TransactionPriority.URGENT -> "Urgent & Fastest"
                                        }
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    enabled = true,
                                    selected = selectedPriority == priority
                                )
                            )
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipients.size) { index ->
                    RecipientCard(
                        recipient = recipients[index],
                        index = index,
                        onAddressChange = { newAddress ->
                            recipients = recipients.mapIndexed { i, r ->
                                if (i == index) r.copy(address = newAddress) else r
                            }
                        },
                        onAmountChange = { newAmount ->
                            recipients = recipients.mapIndexed { i, r ->
                                if (i == index) r.copy(amount = newAmount) else r
                            }
                        },
                        onRemove = {
                            if (recipients.size > 1) {
                                recipients = recipients.filterIndexed { i, _ -> i != index }
                            }
                        },
                        onPaste = {
                            clipboardManager.getText()?.text?.let { text ->
                                recipients = recipients.mapIndexed { i, r ->
                                    if (i == index) r.copy(address = text) else r
                                }
                            }
                        },
                        onSelectFromAddressBook = {
                            activeRecipientIndex = index
                            showAddressBook = true
                        },
                        onUseMax = {
                            recipients = recipients.mapIndexed { i, r ->
                                if (i == index) r.copy(amount = unlockedXMR.toString()) else r
                            }
                        }
                    )
                }
                
                // Add button to add another recipient
                item {
                    Button(
                        onClick = {
                            recipients = recipients + RecipientInput(address = "", amount = "")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Another Recipient")
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val isValid = recipients.all { r ->
                            r.address.isNotEmpty() && 
                            r.amount.isNotEmpty() && 
                            r.amount.toDoubleOrNull() != null &&
                            r.amount.toDouble() > 0
                        }
                        
                        if (!isValid) {
                            scope.launch {
                                snackbarHost.showSnackbar("Please fill in all recipient fields")
                            }
                            return@Button
                        }
                        
                        if (totalAmount + estimatedFee > unlockedXMR) {
                            scope.launch {
                                snackbarHost.showSnackbar("Insufficient balance for this transaction")
                            }
                            return@Button
                        }
                        
                        showConfirmation = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isSending && recipients.any { it.address.isNotEmpty() && it.amount.isNotEmpty() }
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Review Transaction")
                    }
                }
            }
        }
    }
    
    if (showAddressBook) {
        Dialog(
            onDismissRequest = { showAddressBook = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                AddressBookScreen(
                    dataStore = dataStore,
                    onSelectAddress = { address ->
                        recipients = recipients.mapIndexed { i, r ->
                            if (i == activeRecipientIndex) r.copy(address = address) else r
                        }
                        showAddressBook = false
                    },
                    onClose = { showAddressBook = false }
                )
            }
        }
    }
    
    if (showPriorityInfo) {
        AlertDialog(
            onDismissRequest = { showPriorityInfo = false },
            title = { Text("Transaction Priority", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TransactionPriority.values().forEach { priority ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        when (priority) {
                                            TransactionPriority.LOW -> "Slow & Cheaper"
                                            TransactionPriority.MEDIUM -> "Normal"
                                            TransactionPriority.HIGH -> "Fast & Standard"
                                            TransactionPriority.URGENT -> "Urgent & Fastest"
                                        }, 
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        String.format("%.0f%%", priority.feeMultiplier * 100), 
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    when (priority) {
                                        TransactionPriority.LOW -> "Lowest fees, may take hours"
                                        TransactionPriority.MEDIUM -> "Standard fees, confirmed in minutes"
                                        TransactionPriority.HIGH -> "Faster confirmation, higher fee"
                                        TransactionPriority.URGENT -> "Highest priority for fastest confirmation"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPriorityInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }
    
    if (showConfirmation) {
        TransactionConfirmationDialog(
            recipients = recipients,
            totalAmount = totalAmount,
            estimatedFee = estimatedFee,
            priority = selectedPriority,
            isProcessing = isSending,
            onConfirm = {
                scope.launch {
                    isSending = true
                    // Simulate sending process
                    delay(1500)
                    isSending = false
                    showConfirmation = false
                    onSendComplete("Transaction sent successfully!")
                }
            },
            onDismiss = { if (!isSending) showConfirmation = false }
        )
    }
}

@Composable
fun RecipientCard(
    recipient: RecipientInput,
    index: Int,
    onAddressChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit,
    onPaste: () -> Unit,
    onSelectFromAddressBook: () -> Unit,
    onUseMax: () -> Unit
) {
    var addressError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recipient ${index + 1}", fontWeight = FontWeight.SemiBold)
                if (index > 0) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Address", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onPaste,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, "Paste")
                        }
                        IconButton(
                            onClick = onSelectFromAddressBook,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContactPage, "Select from Address Book")
                        }
                    }
                }
                OutlinedTextField(
                    value = recipient.address,
                    onValueChange = {
                        onAddressChange(it)
                        addressError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = addressError != null,
                    supportingText = { addressError?.let { Text(it) } },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amount (XMR)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    TextButton(onClick = onUseMax) {
                        Text("MAX")
                    }
                }
                OutlinedTextField(
                    value = recipient.amount,
                    onValueChange = {
                        onAmountChange(it)
                        amountError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text(stringResource(R.string.monero_symbol) + " ") }
                )
            }
        }
    }
}

@Composable
fun TransactionConfirmationDialog(
    recipients: List<RecipientInput>,
    totalAmount: Double,
    estimatedFee: Double,
    priority: TransactionPriority,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("Confirm Transaction", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Amount", fontWeight = FontWeight.Medium)
                            Text(
                                String.format("%.6f %s", totalAmount, stringResource(R.string.monero_symbol)), 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Network Fee", fontWeight = FontWeight.Medium)
                            Text(
                                String.format("%.6f %s", estimatedFee, stringResource(R.string.monero_symbol)), 
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total to Send", fontWeight = FontWeight.SemiBold)
                            Text(
                                String.format("%.6f %s", totalAmount + estimatedFee, stringResource(R.string.monero_symbol)), 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Priority", fontWeight = FontWeight.Medium)
                    AssistChip(
                        onClick = {},
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                        label = { 
                            Text(
                                when (priority) {
                                    TransactionPriority.LOW -> "Slow & Cheaper"
                                    TransactionPriority.MEDIUM -> "Normal"
                                    TransactionPriority.HIGH -> "Fast & Standard"
                                    TransactionPriority.URGENT -> "Urgent & Fastest"
                                }
                            ) 
                        }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recipients: ${recipients.size}", 
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800))
                    Text(
                        "Monero transactions are irreversible. Double-check all details before confirming.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm Send")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================================
// SUBADDRESS MANAGER
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubaddressScreen(
    walletSuite: WalletSuite,
    dataStore: WalletDataStore,
    onBack: () -> Unit = {}
) {
    var subaddresses by remember { mutableStateOf(dataStore.loadSubaddresses()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedSubaddress by remember { mutableStateOf<Subaddress?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Subaddresses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Create Subaddress")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF2196F3)
                    )
                    Text(
                        "Subaddresses improve privacy by generating unique addresses that forward to your main wallet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (subaddresses.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AddLocation,
                    title = "No Subaddresses",
                    message = "Create your first subaddress to improve privacy"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subaddresses.sortedByDescending { it.creationTime }) { sub ->
                        SubaddressCard(
                            subaddress = sub,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(sub.address))
                                scope.launch {
                                    snackbarHost.showSnackbar("Address copied")
                                }
                            },
                            onClick = {
                                selectedSubaddress = sub
                                showDetailsDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateSubaddressDialog(
            existingLabels = subaddresses.map { it.label },
            onConfirm = { label ->
                scope.launch {
                    val newSub = Subaddress(
                        index = subaddresses.size + 1,
                        address = generateMockSubaddress(),
                        label = label
                    )
                    
                    subaddresses = subaddresses + newSub
                    dataStore.saveSubaddresses(subaddresses)
                    
                    snackbarHost.showSnackbar("Subaddress '$label' created")
                    showCreateDialog = false
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }
    
    selectedSubaddress?.let { sub ->
        if (showDetailsDialog) {
            SubaddressDetailsDialog(
                subaddress = sub,
                onClose = {
                    selectedSubaddress = null
                    showDetailsDialog = false
                }
            )
        }
    }
}

@Composable
fun SubaddressCard(
    subaddress: Subaddress,
    onCopy: () -> Unit,
    onClick: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(subaddress.label, fontWeight = FontWeight.SemiBold)
                    if (subaddress.used) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Used",
                                fontSize = 10.sp,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(
                    "Index: ${subaddress.index}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(subaddress.creationTime)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            IconButton(
                onClick = {
                    onCopy()
                    copied = true
                }
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (copied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CreateSubaddressDialog(
    existingLabels: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var labelError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Subaddress", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Subaddresses improve privacy by generating unique addresses that forward to your main wallet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        labelError = null
                    },
                    label = { Text("Subaddress Label") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = labelError != null,
                    supportingText = { labelError?.let { Text(it) } },
                    placeholder = { Text("e.g., Online Shopping, Donations") }
                )
                
                if (existingLabels.contains(label)) {
                    Text(
                        "Label already exists. Please choose a unique label.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isEmpty()) {
                        labelError = "Label is required"
                        return@Button
                    }
                    if (existingLabels.contains(label)) {
                        labelError = "Label already exists"
                        return@Button
                    }
                    onConfirm(label)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = label.isNotEmpty() && !existingLabels.contains(label)
            ) {
                Text("Create Subaddress")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SubaddressDetailsDialog(
    subaddress: Subaddress,
    onClose: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val qrBitmap = remember(subaddress.address) {
        generateQRCode(subaddress.address, 256)
    }
    
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onClose,
        title = { 
            Text(
                subaddress.label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier.size(200.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Subaddress QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Address", fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            subaddress.address,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Index", fontWeight = FontWeight.Medium)
                        Text(subaddress.index.toString())
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Created", fontWeight = FontWeight.Medium)
                        Text(
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(Date(subaddress.creationTime))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status", fontWeight = FontWeight.Medium)
                        Text(
                            if (subaddress.used) "Used" else "Unused",
                            color = if (subaddress.used) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(subaddress.address))
                    copied = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (copied) "Copied!" else "Copy")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    )
}

// ============================================================================
// PAYMENT REQUEST GENERATOR
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRequestScreen(
    walletAddress: String,
    onBack: () -> Unit = {}
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var generatedUri by remember { mutableStateOf<String?>(null) }
    var showUriDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Payment Request", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        tint = Color(0xFF2196F3)
                    )
                    Text(
                        "Generate a payment request QR code that includes amount and description",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Your Address", fontWeight = FontWeight.SemiBold)
                        Text(
                            walletAddress.take(20) + "..." + walletAddress.takeLast(20),
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (XMR)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter amount (optional)") },
                    prefix = { Text(stringResource(R.string.monero_symbol) + " ") },
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Recipient Name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., John's Cafe") },
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    placeholder = { Text("e.g., Coffee payment") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val request = PaymentRequest(
                        address = walletAddress,
                        amount = if (amount.isNotEmpty()) amount else null,
                        recipientName = if (recipientName.isNotEmpty()) recipientName else null,
                        txDescription = if (description.isNotEmpty()) description else null
                    )
                    
                    generatedUri = MoneroUriHandler.createUri(request)
                    showUriDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                enabled = amount.isEmpty() || amount.toDoubleOrNull() != null
            ) {
                Icon(Icons.Default.QrCode, null)
                Spacer(Modifier.width(8.dp))
                Text("Generate Payment Request")
            }
        }
    }
    
    generatedUri?.let { uri ->
        if (showUriDialog) {
            PaymentRequestDialog(
                uri = uri,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(uri))
                    scope.launch {
                        snackbarHost.showSnackbar("Payment request copied")
                    }
                },
                onShare = {
                    scope.launch {
                        snackbarHost.showSnackbar("Share not implemented in this version")
                    }
                },
                onClose = { showUriDialog = false }
            )
        }
    }
}

@Composable
fun PaymentRequestDialog(
    uri: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val qrBitmap = remember(uri) {
        generateQRCode(uri, 300)
    }
    
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Payment Request", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier.size(250.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Payment Request QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        uri,
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onCopy()
                        copied = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (copied) "Copied!" else "Copy")
                }
                
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    )
}

// ============================================================================
// DATA CLASSES
// ============================================================================

data class Transaction(
    val type: String,
    val amount: String,
    val date: String,
    val confirmed: Boolean,
    val txId: String = ""
)

// ============================================================================
// UTILITY FUNCTIONS & UI COMPONENTS
// ============================================================================

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun generateMockSubaddress(): String {
    return "8" + UUID.randomUUID().toString().replace("-", "").take(94)
}
