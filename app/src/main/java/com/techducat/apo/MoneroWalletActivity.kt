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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.techducat.apo.WalletSuite
import com.m2049r.xmrwallet.model.TransactionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// MAIN ACTIVITY
// ============================================================================

class MoneroWalletActivity : ComponentActivity() {
    private lateinit var walletSuite: WalletSuite

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    { selectedTab = 1 }
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
            }
        }
    }
    
    if (showReceiveDialog) {
        ReceiveDialog(walletAddress) { showReceiveDialog = false }
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
    onSendClick: () -> Unit
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
                modifier = Modifier.fillMaxWidth().height(220.dp),
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
                    Modifier.weight(1f), {}, Color(0xFF9C27B0)
                )
            }
        }
        
        // Address Card
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
        modifier = modifier.height(110.dp),
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
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.QrCodeScanner, stringResource(R.string.send_scan_qr))
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
                        // QR Code placeholder
                        Box(
                            modifier = Modifier.size(200.dp).background(Color.White, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.qr_code_placeholder), color = Color.Gray)
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
                    Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, null)
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
                                Icons.Default.Receipt,
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
                                Icons.Default.Schedule,
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
                icon = Icons.Default.VpnKey,
                onClick = { /* Export keys */ }
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
                onClick = { walletSuite.reloadConfiguration() }
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
                onClick = { /* Security settings */ }
            )
        }
    }
    
    // Dialogs
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
        SeedPhraseDialog(onDismiss = { showSeedDialog = false })
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
                Icons.Default.ChevronRight,
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
fun SeedPhraseDialog(onDismiss: () -> Unit) {
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
                    Text(
                        text = "[Seed phrase would be displayed here - implement wallet.getSeed() method]",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
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
