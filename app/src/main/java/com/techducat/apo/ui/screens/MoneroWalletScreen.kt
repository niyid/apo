package com.techducat.apo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.res.stringResource
import com.techducat.apo.WalletSuite
import com.techducat.apo.storage.WalletDataStore
import com.techducat.apo.ui.components.ErrorScreen
import com.techducat.apo.ui.components.LoadingScreen
import com.techducat.apo.ui.components.TwoRowNavigationBar
import com.techducat.apo.ui.dialogs.ReceiveDialog
import com.techducat.apo.R

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
        Result.failure(e)
    } finally {
        connection?.disconnect()
    }
}
