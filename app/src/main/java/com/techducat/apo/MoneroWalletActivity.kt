package com.techducat.apo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import org.json.JSONObject
import java.util.*
import android.util.Log
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
        Log.e("QRCode", "Failed to generate QR code", e)
        null
    }
}

// ============================================================================
// CHANGENOW SWAP SERVICE
// ============================================================================

class ChangeNowSwapService {
    companion object {
        private const val BASE_URL = "https://api.changenow.io/v2"
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val READ_TIMEOUT = 15000 // 15 seconds
        
        private val API_KEY: String by lazy {
            try {
                BuildConfig.CHANGENOW_API_KEY.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException(
                        "ChangeNOW API key not configured."
                    )
            } catch (e: Exception) {
                throw IllegalStateException(
                    "ChangeNOW API key not configured.",
                    e
                )
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
            // FIX: Properly close connection to prevent resource leaks
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
            
            // FIXED: Use 'use' to guarantee proper resource cleanup
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
                Result.failure(Exception("HTTP error: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Connection cleanup - all streams already closed by 'use'
            connection?.disconnect()
        }
    }
}

// ============================================================================
// MAIN ACTIVITY
// ============================================================================

class MoneroWalletActivity : ComponentActivity() {
    private lateinit var walletSuite: WalletSuite

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+)
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep splash screen visible while wallet initializes
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        
        walletSuite = WalletSuite.getInstance(this)
        
        setContent {
            MoneroWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneroWalletScreen(walletSuite)
                }
            }
        }
        
        // Hide splash screen after wallet is initialized
        lifecycleScope.launch {
            delay(1500) // Minimum splash duration
            keepSplashOnScreen = false
        }
    }
    
    override fun onDestroy() {
        // CRITICAL FIX: Only close wallet if app is actually finishing
        // Don't close on configuration changes or temporary activity destruction
        if (isFinishing) {
            Log.d("MoneroWallet", "Activity finishing - closing wallet")
            try {
                walletSuite.close()
            } catch (e: Exception) {
                Log.e("MoneroWallet", "Error closing wallet", e)
            }
        } else {
            Log.d("MoneroWallet", "Activity destroyed but not finishing - keeping wallet open")
        }
        super.onDestroy()
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
// MAIN WALLET SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneroWalletScreen(walletSuite: WalletSuite) {
    var selectedTab by remember { mutableStateOf(0) }
    var balance by remember { mutableStateOf(0L) }
    var unlockedBalance by remember { mutableStateOf(0L) }
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
    
    // Setup listeners
    LaunchedEffect(Unit) {
        walletSuite.setWalletStatusListener(object : WalletSuite.WalletStatusListener {
            override fun onWalletInitialized(success: Boolean, message: String) {
                isInitialized = success
                initMessage = message
                if (success) {
                    walletAddress = walletSuite.cachedAddress ?: ""
                    balance = walletSuite.balanceValue
                    unlockedBalance = walletSuite.unlockedBalanceValue
                }
            }
            
            override fun onBalanceUpdated(bal: Long, unl: Long) {
                balance = bal
                unlockedBalance = unl
            }
            
            override fun onSyncProgress(h: Long, sh: Long, eh: Long, pd: Double) {
                walletHeight = h
                daemonHeight = eh
                syncProgress = pd
                isSyncing = pd < 100.0
            }
        })
        
        if (!walletSuite.isReady) {
            walletSuite.initializeWallet()
        } else {
            isInitialized = true
            walletAddress = walletSuite.cachedAddress ?: ""
            balance = walletSuite.balanceValue
            unlockedBalance = walletSuite.unlockedBalanceValue
        }
    }
    
    if (!isInitialized) {
        LoadingScreen(initMessage)
        return
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, stringResource(R.string.nav_wallet)) },
                    label = { Text(stringResource(R.string.nav_wallet)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Send, stringResource(R.string.nav_send)) },
                    label = { Text(stringResource(R.string.nav_send)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, stringResource(R.string.nav_history)) },
                    label = { Text(stringResource(R.string.nav_history)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, stringResource(R.string.nav_settings)) },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.SwapHoriz, stringResource(R.string.nav_exchange)) },
                    label = { Text(stringResource(R.string.nav_exchange)) },
                    enabled = ChangeNowSwapService.isConfigured()
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    balance, unlockedBalance, walletAddress, syncProgress, isSyncing,
                    walletHeight, daemonHeight,
                    { walletSuite.triggerImmediateSync() },
                    { showReceiveDialog = true },
                    { selectedTab = 1 },
                    { selectedTab = 4 } // FIXED: Add exchange callback
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
            }
        }
    }
    
    if (showReceiveDialog) {
        ReceiveDialog(walletAddress) { showReceiveDialog = false }
    }
}

@Composable
fun ExportKeysDialog(
    walletSuite: WalletSuite,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var viewKeyCopied by remember { mutableStateOf(false) }
    var spendKeyCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Get actual keys from wallet
    val viewKey = remember { walletSuite.viewKey }
    val spendKey = remember { walletSuite.spendKey }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.export_keys_dialog_title),
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
                        text = stringResource(R.string.export_keys_warning),
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Text(
                    text = stringResource(R.string.export_keys_info),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                KeyDisplayCard(
                    label = stringResource(R.string.export_keys_view_key_label),
                    keyValue = viewKey,
                    isCopied = viewKeyCopied,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(viewKey))
                        viewKeyCopied = true
                    }
                )
                
                KeyDisplayCard(
                    label = stringResource(R.string.export_keys_spend_key_label),
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
                Text(text = stringResource(R.string.dialog_close))
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
            modifier = Modifier.padding(12.dp),
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
                        if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.action_copy),
                        tint = if (isCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SecuritySettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var biometricEnabled by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Load current settings from SharedPreferences
    LaunchedEffect(Unit) {
        try {
            val prefs = context.getSharedPreferences("wallet_security", android.content.Context.MODE_PRIVATE)
            biometricEnabled = prefs.getBoolean("biometric_enabled", false)
            pinEnabled = prefs.getBoolean("pin_enabled", false)
        } catch (e: Exception) {
            // Settings don't exist yet, use defaults (false)
            Log.d("SecuritySettings", "No saved settings found, using defaults")
        }
        isLoading = false
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.security_title),
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
                        title = stringResource(R.string.security_biometric_title),
                        subtitle = stringResource(R.string.security_biometric_subtitle),
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it },
                        enabled = !isSaving
                    )
                    
                    SecurityOption(
                        title = stringResource(R.string.security_pin_title),
                        subtitle = stringResource(R.string.security_pin_subtitle),
                        checked = pinEnabled,
                        onCheckedChange = { pinEnabled = it },
                        enabled = !isSaving
                    )
                    
                    Text(
                        text = "Note: Full implementation requires androidx.biometric library and proper setup",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
                            // Save settings to SharedPreferences
                            val prefs = context.getSharedPreferences("wallet_security", android.content.Context.MODE_PRIVATE)
                            prefs.edit().apply {
                                putBoolean("biometric_enabled", biometricEnabled)
                                putBoolean("pin_enabled", pinEnabled)
                                apply()
                            }
                            
                            delay(300) // Brief delay to show save action
                            onDismiss()
                        } catch (e: Exception) {
                            Log.e("SecuritySettings", "Failed to save settings", e)
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
                Text(text = stringResource(R.string.dialog_close))
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
                text = message.ifEmpty { stringResource(R.string.init_wallet) },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
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
    walletAddress: String,
    syncProgress: Double,
    isSyncing: Boolean,
    walletHeight: Long,
    daemonHeight: Long,
    onRefresh: () -> Unit,
    onReceiveClick: () -> Unit,
    onSendClick: () -> Unit,
    onExchangeClick: () -> Unit = {} // NEW PARAMETER
) {
    val balanceXMR = WalletSuite.convertAtomicToXmr(balance)
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance)
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
        // Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(220.dp).semantics(mergeDescendants = true) { },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFF6600), Color(0xFFFF8833), Color(0xFFFFAA66))
                        )
                    ).padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.wallet_total_balance),
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
                                    Icons.Default.Refresh, stringResource(R.string.action_refresh),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp).rotate(if (isSyncing) rotation else 0f)
                                )
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "$balanceXMR XMR",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.wallet_unlocked_balance, unlockedXMR),
                                color = Color.White.copy(0.8f),
                                fontSize = 14.sp
                            )
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
                                    stringResource(R.string.wallet_syncing, syncProgress, walletHeight, daemonHeight),
                                    color = Color.White.copy(0.9f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    Icons.Default.CallReceived, stringResource(R.string.action_receive),
                    Modifier.weight(1f), onReceiveClick, Color(0xFF4CAF50)
                )
                QuickActionButton(
                    Icons.Default.Send, stringResource(R.string.nav_send),
                    Modifier.weight(1f), onSendClick, Color(0xFFFF6600)
                )
                QuickActionButton(
                    Icons.Default.SwapHoriz, stringResource(R.string.action_exchange),
                    Modifier.weight(1f), onExchangeClick, Color(0xFF9C27B0) // FIXED
                )
            }
        }
        
        // Address Card
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
                    Text(stringResource(R.string.wallet_your_address), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    
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
                                    stringResource(R.string.action_copy),
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
    
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance)
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(successMessage, errorMessage) {
        successMessage?.let {
            scope.launch {
                snackbarHost.showSnackbar(
                    "Transaction sent! TxID: ${it.take(16)}...",
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
                Text(stringResource(R.string.send_title), fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
                            Text(stringResource(R.string.send_available_balance), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            Text("$unlockedXMR XMR", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                        Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF4CAF50).copy(0.3f), modifier = Modifier.size(48.dp))
                    }
                }
            }
            
            item {
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text(stringResource(R.string.send_recipient_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = { 
                            // Launch QR scanner (requires implementation)
                            scope.launch {
                                snackbarHost.showSnackbar(
                                    "QR Scanner not yet implemented. Please enter address manually.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }) {
                            Icon(Icons.Default.QrCode, stringResource(R.string.send_scan_qr))
                        }
                    },
                    maxLines = 3
                )
            }
            
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.send_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        TextButton(onClick = { amount = unlockedXMR }) {
                            Text(stringResource(R.string.send_max), fontWeight = FontWeight.Bold)
                        }
                    },
                    singleLine = true
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
                    } else {
                        Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.send_transaction), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                    Text(stringResource(R.string.action_search_transaction))
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
        title = { Text(stringResource(R.string.send_confirm_title), fontWeight = FontWeight.Bold) },
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
                        Text(stringResource(R.string.send_confirm_amount), fontWeight = FontWeight.SemiBold)
                        Text("$amount XMR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        Text(stringResource(R.string.send_confirm_to), fontWeight = FontWeight.SemiBold)
                        Text(
                            if (recipient.length > 30) "${recipient.take(15)}...${recipient.takeLast(15)}" else recipient,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(R.string.send_confirm_warning),
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
                Text(stringResource(R.string.send_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSending) {
                Text(stringResource(R.string.dialog_cancel))
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
    
    // Generate QR code
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
        title = { Text(stringResource(R.string.receive_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.receive_your_address), fontWeight = FontWeight.SemiBold)
                
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
                        // QR Code display
                        if (qrBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier.size(200.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.qr_code_placeholder),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            }
                        } else {
                            // Fallback if QR generation fails
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
                    Text(if (copied) stringResource(R.string.status_copied) else stringResource(R.string.receive_copy_address))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close))
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_search_transaction), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.tx_search_info), fontSize = 12.sp)
                
                OutlinedTextField(
                    value = txId,
                    onValueChange = { 
                        txId = it
                        searchResult = null
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.tx_search_id)) },
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
                            searchResult = "Transaction found!\nAmount: ${WalletSuite.convertAtomicToXmr(amount)} XMR\nConfirmations: $confirmations"
                        }
                        
                        override fun onTransactionNotFound(txId: String) {
                            isSearching = false
                            errorMessage = "Transaction not found in wallet history"
                        }
                        
                        override fun onError(error: String) {
                            isSearching = false
                            errorMessage = "Error: $error"
                        }
                    })
                },
                enabled = txId.isNotEmpty() && !isSearching
            ) {
                Text(stringResource(R.string.action_search))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close))
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
    
    // Load real transaction data
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            walletSuite.getTransactionHistory(object : WalletSuite.TransactionHistoryCallback {
                override fun onSuccess(txList: List<TransactionInfo>) {
                    transactions = txList.map { txInfo ->
                        val isReceived = txInfo.direction == TransactionInfo.Direction.Direction_In
                        Transaction(
                            type = if (isReceived) "Received" else "Sent",
                            amount = WalletSuite.convertAtomicToXmr(Math.abs(txInfo.amount)),
                            date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
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
                        text = stringResource(R.string.history_title),
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
                            contentDescription = stringResource(R.string.action_refresh),
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
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
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
                                text = stringResource(R.string.history_no_transactions),
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
                        text = "${if (transaction.type == "Received") "+" else "-"}${transaction.amount} XMR",
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
                                text = stringResource(R.string.history_confirmed),
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
                                text = stringResource(R.string.history_pending),
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
                        text = stringResource(R.string.tx_search_id),
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
                                contentDescription = stringResource(R.string.action_copy),
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
    
    // Check if Exchange is available
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
                    text = "API key not configured. Add changenow.api.key to gradle.properties",
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
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance)
    
    // FIX: Get estimates on background thread
    LaunchedEffect(fromAmount, fromCurrency, toCurrency) {
        if (fromAmount.isNotEmpty()) {
            val amount = fromAmount.toDoubleOrNull()
            if (amount != null && amount > 0) {
                delay(500) // Debounce
                isLoading = true
                
                // Move to IO dispatcher
                withContext(Dispatchers.IO) {
                    changeNowService.getEstimate(fromCurrency, toCurrency, amount)
                        .onSuccess { estimate ->
                            withContext(Dispatchers.Main) {
                                estimatedAmount = estimate.estimatedAmount
                                isLoading = false
                            }
                        }
                        .onFailure { 
                            withContext(Dispatchers.Main) {
                                estimatedAmount = null
                                isLoading = false
                            }
                        }
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
                                "$unlockedXMR XMR", 
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
                                placeholder = { Text("0.0") },
                                trailingIcon = { 
                                    if (fromCurrency == "xmr") { 
                                        TextButton(onClick = { fromAmount = unlockedXMR }) { 
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
                                        text = estimatedAmount?.let { "≈ %.6f".format(it) } ?: "0.0", 
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
                            
                            // FIX: Execute on IO thread
                            withContext(Dispatchers.IO) {
                                changeNowService.createExchange(
                                    fromCurrency, 
                                    toCurrency, 
                                    fromAmount.toDoubleOrNull() ?: 0.0, 
                                    toAddress, 
                                    if (fromCurrency == "xmr") walletAddress else null
                                ).onSuccess { status ->
                                    withContext(Dispatchers.Main) {
                                        exchangeStatus = status
                                        snackbarHost.showSnackbar(
                                            "Exchange created! Send to: ${status.payinAddress}"
                                        )
                                        isLoading = false
                                    }
                                }.onFailure { 
                                    withContext(Dispatchers.Main) {
                                        snackbarHost.showSnackbar("Error: ${it.message}")
                                        isLoading = false
                                    }
                                }
                            }
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
                        Text("Processing...")
                    } else {
                        Icon(Icons.Default.SwapHoriz, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Exchange", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
    // ALL STATE VARIABLES - FIXED
    var showRescanDialog by remember { mutableStateOf(false) }
    var showSeedDialog by remember { mutableStateOf(false) }
    var showNodeDialog by remember { mutableStateOf(false) }
    var showTxSearchDialog by remember { mutableStateOf(false) }
    var showExportKeysDialog by remember { mutableStateOf(false) }  // ADDED
    var showSecurityDialog by remember { mutableStateOf(false) }     // ADDED
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
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.nav_settings),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // Wallet Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_wallet_info),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.settings_status),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (walletSuite.isReady) Color(0xFF4CAF50) 
                                            else Color(0xFFFF6600)
                                        )
                                )
                                Text(
                                    text = if (walletSuite.isReady) stringResource(R.string.settings_ready) else stringResource(R.string.settings_not_ready),
                                    color = if (walletSuite.isReady) Color(0xFF4CAF50) else Color(0xFFFF6600),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Syncing",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = if (walletSuite.isSyncing) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = stringResource(R.string.settings_maintenance),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_rescan_blockchain),
                    subtitle = if (isRescanning) 
                        stringResource(R.string.settings_rescan_progress, rescanProgress)
                    else 
                        stringResource(R.string.settings_rescan_subtitle),
                    icon = Icons.Default.Refresh,
                    onClick = { showRescanDialog = true },
                    enabled = !isRescanning
                )
            }
            
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_force_refresh),
                    subtitle = stringResource(R.string.settings_force_refresh_subtitle),
                    icon = Icons.Default.Refresh,
                    onClick = { walletSuite.triggerImmediateSync() }
                )
            }
            
            item {
                Text(
                    text = stringResource(R.string.settings_backup_security),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_view_seed),
                    subtitle = stringResource(R.string.settings_view_seed_subtitle),
                    icon = Icons.Default.Key,
                    onClick = { showSeedDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_export_keys),
                    subtitle = stringResource(R.string.settings_export_keys_subtitle),
                    icon = Icons.Default.Key,
                    onClick = { showExportKeysDialog = true }
                )
            }
            
            item {
                Text(
                    text = stringResource(R.string.settings_network),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_node_settings),
                    subtitle = stringResource(R.string.settings_current_node, walletSuite.daemonAddress, walletSuite.daemonPort),
                    icon = Icons.Default.Cloud,
                    onClick = { showNodeDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_reload_config),
                    subtitle = stringResource(R.string.settings_reload_config_subtitle),
                    icon = Icons.Default.Settings,
                    onClick = { 
                        scope.launch {
                            try {
                                // Call reload - it runs on background thread internally
                                walletSuite.reloadConfiguration()
                                
                                // Show immediate feedback since the method returns quickly
                                // The actual daemon reconnection happens in background
                                snackbarHost.showSnackbar(
                                    context.getString(
                                        R.string.reload_config_initiated,
                                        walletSuite.daemonAddress,
                                        walletSuite.daemonPort
                                    ),
                                    duration = SnackbarDuration.Long
                                )
                            } catch (e: Exception) {
                                snackbarHost.showSnackbar(
                                    context.getString(
                                        R.string.reload_config_failed,
                                        e.message ?: "Unknown error"
                                    ),
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    }
                )
            }
            
            item {
                Text(
                    text = stringResource(R.string.settings_advanced),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_tx_search),
                    subtitle = stringResource(R.string.settings_tx_search_subtitle),
                    icon = Icons.Default.Search,
                    onClick = { showTxSearchDialog = true }
                )
            }
            
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_security),
                    subtitle = stringResource(R.string.settings_security_subtitle),
                    icon = Icons.Default.Security,
                    onClick = { showSecurityDialog = true }
                )
            }
        }
        
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
    
    // ALL DIALOGS
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
            containerColor = if (enabled) MaterialTheme.colorScheme.surface 
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
        enabled = enabled
    ) {
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
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
                text = stringResource(R.string.settings_rescan_blockchain),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Text(text = stringResource(R.string.rescan_message)) 
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = stringResource(R.string.rescan_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
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
    
    // Load seed phrase asynchronously using the safe async method
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
                text = stringResource(R.string.seed_title),
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
                        text = stringResource(R.string.seed_warning),
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = stringResource(R.string.seed_info),
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
                                            contentDescription = stringResource(R.string.action_copy),
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
                Text(text = stringResource(R.string.dialog_close))
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
                text = stringResource(R.string.node_title),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.node_current_daemon),
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
                    text = stringResource(R.string.node_change_info),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_close))
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
