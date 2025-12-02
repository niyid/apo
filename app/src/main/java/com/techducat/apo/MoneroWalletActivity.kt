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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.monero.wallet.WalletSuite
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MoneroWalletActivity : ComponentActivity() {
    private lateinit var walletSuite: WalletSuite

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        walletSuite = WalletSuite.getInstance(this)
        
        setContent {
            MoneroWalletTheme {
                MoneroWalletScreen(walletSuite)
            }
        }
    }
    
    // Receive Dialog
    if (showReceiveDialog) {
        ReceiveDialogWithQR(
            address = walletAddress,
            onDismiss = { showReceiveDialog = false }
        )
    }
}

@Composable
fun ReceiveDialogWithQR(
    address: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var addressCopied by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Receive Monero",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // QR Code Placeholder (implement QRCodeGenerator for production)
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            modifier = Modifier.size(100.dp),
                            tint = Color.Black
                        )
                        Text(
                            "QR Code",
                            color = Color.Black,
                            fontSize = 12.sp
                        )
                        Text(
                            "Add QRCodeGenerator library",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                    }
                }
                
                // Optional amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Text(
                    "Your Monero Address",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            address,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(address))
                        addressCopied = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (addressCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (addressCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (addressCopied) "Copied!" else "Copy Address")
                }
                
                Text(
                    "Share this address to receive Monero payments",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun MoneroWalletTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF6600),
            secondary = Color(0xFF4D4D4D),
            background = Color(0xFF0F0F0F),
            surface = Color(0xFF1A1A1A),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFE0E0E0),
            onSurface = Color(0xFFE0E0E0)
        ),
        content = content
    )
}

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
    var initMessage by remember { mutableStateOf("Initializing wallet...") }
    
    val scope = rememberCoroutineScope()
    
    // Setup wallet listeners
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
            
            override fun onBalanceUpdated(balance: Long, unlocked: Long) {
                this@MoneroWalletScreen.balance = balance
                this@MoneroWalletScreen.unlockedBalance = unlocked
            }
            
            override fun onSyncProgress(height: Long, startHeight: Long, endHeight: Long, percentDone: Double) {
                walletHeight = height
                daemonHeight = endHeight
                syncProgress = percentDone
                isSyncing = percentDone < 100.0
            }
        })
        
        // Initialize if needed
        if (!walletSuite.isReady) {
            walletSuite.initializeWallet()
        } else {
            isInitialized = true
            walletAddress = walletSuite.cachedAddress ?: ""
            balance = walletSuite.balanceValue
            unlockedBalance = walletSuite.unlockedBalanceValue
        }
    }
    
    // Show loading screen if not initialized
    if (!isInitialized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp
                )
                Text(
                    initMessage,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "This may take a moment...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        return
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, "Home") },
                    label = { Text("Wallet") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Send, "Send") },
                    label = { Text("Send") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, "History") },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    balance = balance,
                    unlockedBalance = unlockedBalance,
                    walletAddress = walletAddress,
                    syncProgress = syncProgress,
                    isSyncing = isSyncing,
                    walletHeight = walletHeight,
                    daemonHeight = daemonHeight,
                    onRefresh = { walletSuite.triggerImmediateSync() },
                    onReceiveClick = { showReceiveDialog = true }
                )
                1 -> SendScreen(
                    unlockedBalance = unlockedBalance,
                    onSend = { address, amount ->
                        walletSuite.sendTransaction(address, amount, object : WalletSuite.TransactionCallback {
                            override fun onSuccess(txId: String, amount: Long) {
                                sendSuccess = "Transaction sent!\nTxID: ${txId.take(16)}..."
                                sendError = null
                            }
                            override fun onError(error: String) {
                                sendError = error
                                sendSuccess = null
                            }
                        })
                    },
                    successMessage = sendSuccess,
                    errorMessage = sendError,
                    onDismissMessage = { 
                        sendSuccess = null
                        sendError = null
                    }
                )
                2 -> HistoryScreen(walletSuite = walletSuite)
                3 -> SettingsScreen(
                    walletSuite = walletSuite,
                    walletAddress = walletAddress,
                    onRescan = { 
                        scope.launch {
                            // Trigger rescan
                        }
                    }
                )
            }
        }
    }
}

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
    onReceiveClick: () -> Unit
) {
    val balanceXMR = WalletSuite.convertAtomicToXmr(balance)
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance)
    val clipboardManager = LocalClipboardManager.current
    var addressCopied by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF6600),
                                    Color(0xFFFF8833)
                                )
                            )
                        )
                        .padding(24.dp)
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
                                "Total Balance",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Column {
                            Text(
                                "$balanceXMR XMR",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Unlocked: $unlockedXMR XMR",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                        
                        if (isSyncing) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = (syncProgress / 100.0).toFloat(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                                Text(
                                    "Syncing: ${String.format("%.1f", syncProgress)}% ($walletHeight / $daemonHeight)",
                                    color = Color.White.copy(alpha = 0.8f),
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
                    icon = Icons.Default.CallReceived,
                    label = "Receive",
                    modifier = Modifier.weight(1f),
                    onClick = onReceiveClick
                )
                QuickActionButton(
                    icon = Icons.Default.Send,
                    label = "Send",
                    modifier = Modifier.weight(1f),
                    onClick = { /* Navigate to send */ }
                )
                QuickActionButton(
                    icon = Icons.Default.SwapHoriz,
                    label = "Exchange",
                    modifier = Modifier.weight(1f),
                    onClick = { /* Navigate to exchange */ }
                )
            }
        }
        
        // Address Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Your Address",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            walletAddress.take(20) + "..." + walletAddress.takeLast(20),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(walletAddress))
                                addressCopied = true
                            }
                        ) {
                            Icon(
                                if (addressCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (addressCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        // Stats Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Height",
                    value = walletHeight.toString(),
                    icon = Icons.Default.Layers,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Network",
                    value = daemonHeight.toString(),
                    icon = Icons.Default.Cloud,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SendScreen(
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
    
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance)
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Text(
                "Send Monero",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Available Balance",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        "$unlockedXMR XMR",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
        
        item {
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("Recipient Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                ),
                trailingIcon = {
                    IconButton(onClick = { /* QR Scanner */ }) {
                        Icon(Icons.Default.QrCodeScanner, "Scan QR")
                    }
                }
            )
        }
        
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (XMR)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                ),
                trailingIcon = {
                    TextButton(onClick = { amount = unlockedXMR }) {
                        Text("MAX", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
        
        item {
            Button(
                onClick = { showConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = recipient.isNotEmpty() && amount.isNotEmpty()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Transaction", fontSize = 16.sp)
            }
        }
    }
    
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isSending) showConfirmation = false },
            title = { Text("Confirm Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Amount:", fontWeight = FontWeight.SemiBold)
                        Text("$amount XMR", color = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("To:", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${recipient.take(10)}...${recipient.takeLast(10)}",
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    Text(
                        "This transaction cannot be reversed. Please verify the details carefully.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSending = true
                        onSend(recipient, amount.toDoubleOrNull() ?: 0.0)
                        showConfirmation = false
                        recipient = ""
                        amount = ""
                    },
                    enabled = !isSending
                ) {
                    Text("Confirm & Send")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmation = false },
                    enabled = !isSending
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Transaction Search Dialog
    if (showTxSearchDialog) {
        TransactionSearchDialog(
            walletSuite = walletSuite,
            onDismiss = { showTxSearchDialog = false }
        )
    }
}

@Composable
fun TransactionSearchDialog(
    walletSuite: WalletSuite,
    onDismiss: () -> Unit
) {
    var txId by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Enter transaction ID to search for missing transactions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                OutlinedTextField(
                    value = txId,
                    onValueChange = { txId = it },
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            it,
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                
                errorMessage?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF6600).copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            it,
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFFFF6600)
                        )
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
                    
                    walletSuite.searchAndImportTransaction(
                        txId,
                        object : WalletSuite.TransactionSearchCallback {
                            override fun onTransactionFound(
                                txId: String,
                                amount: Long,
                                confirmations: Long,
                                blockHeight: Long
                            ) {
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
                        }
                    )
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

@Composable
fun HistoryScreen(walletSuite: WalletSuite) {
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    // Load real transaction data
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val txHistory = walletSuite.getTransactionHistory()
                transactions = txHistory.map { txInfo ->
                    Transaction(
                        type = if (txInfo.direction == com.m2049r.xmrwallet.model.TransactionInfo.Direction.Direction_In) "Received" else "Sent",
                        amount = WalletSuite.convertAtomicToXmr(Math.abs(txInfo.amount)),
                        date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date(txInfo.timestamp * 1000)),
                        confirmed = !txInfo.isPending
                    )
                }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message
                isLoading = false
            }
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
                Text(
                    "Transaction History",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF6600).copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            "Error: $errorMessage",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFFF6600)
                        )
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
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions yet",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    walletSuite: WalletSuite,
    walletAddress: String,
    onRescan: () -> Unit
) {
    var showRescanDialog by remember { mutableStateOf(false) }
    var showSeedDialog by remember { mutableStateOf(false) }
    var showNodeDialog by remember { mutableStateOf(false) }
    var showTxSearchDialog by remember { mutableStateOf(false) }
    var rescanProgress by remember { mutableStateOf(0.0) }
    var isRescanning by remember { mutableStateOf(false) }
    
    // Set up rescan balance callback
    LaunchedEffect(Unit) {
        walletSuite.setRescanBalanceCallback(object : WalletSuite.RescanBalanceCallback {
            override fun onBalanceUpdated(balance: Long, unlockedBalance: Long) {
                // Update progress based on sync
                val status = walletSuite.syncStatus
                rescanProgress = status.percentDone
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
                "Settings",
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
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Wallet Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(
                            if (walletSuite.isReady) "Ready" else "Not Ready",
                            color = if (walletSuite.isReady) Color(0xFF4CAF50) else Color(0xFFFF6600),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Syncing", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(
                            if (walletSuite.isSyncing) "Yes" else "No",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        item {
            Text(
                "Maintenance",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        item {
            SettingsCard(
                title = "Rescan Blockchain",
                subtitle = if (isRescanning) "Scanning... ${String.format("%.1f", rescanProgress)}%" 
                          else "Fix missing transactions",
                icon = Icons.Default.Refresh,
                onClick = { showRescanDialog = true }
            )
        }
        
        item {
            SettingsCard(
                title = "Force Balance Refresh",
                subtitle = "Recalculate wallet balance",
                icon = Icons.Default.Refresh,
                onClick = { 
                    // Trigger immediate sync to refresh balance
                    walletSuite.triggerImmediateSync()
                }
            )
        }
        
        item {
            Text(
                "Backup & Security",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        item {
            SettingsCard(
                title = "View Seed Phrase",
                subtitle = "Backup your wallet recovery phrase",
                icon = Icons.Default.Key,
                onClick = { showSeedDialog = true }
            )
        }
        
        item {
            SettingsCard(
                title = "Export Keys",
                subtitle = "Export private keys",
                icon = Icons.Default.Key,
                onClick = { /* Export keys */ }
            )
        }
        
        item {
            Text(
                "Network",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        item {
            SettingsCard(
                title = "Node Settings",
                subtitle = "Current: ${walletSuite.daemonAddress}:${walletSuite.daemonPort}",
                icon = Icons.Default.Cloud,
                onClick = { showNodeDialog = true }
            )
        }
        
        item {
            SettingsCard(
                title = "Reload Configuration",
                subtitle = "Refresh wallet configuration",
                icon = Icons.Default.Settings,
                onClick = { walletSuite.reloadConfiguration() }
            )
        }
        
        item {
            Text(
                "Advanced",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        item {
            SettingsCard(
                title = "Transaction Search",
                subtitle = "Search for missing transaction by ID",
                icon = Icons.Default.Search,
                onClick = { showTxSearchDialog = true }
            )
        }
        
        item {
            SettingsCard(
                title = "Security",
                subtitle = "Password & privacy settings",
                icon = Icons.Default.Security,
                onClick = { /* Security settings */ }
            )
        }
    }
    
    // Rescan Confirmation Dialog
    if (showRescanDialog) {
        AlertDialog(
            onDismissRequest = { showRescanDialog = false },
            title = { Text("Rescan Blockchain") },
            text = { 
                Text("This will rescan the blockchain to find missing transactions. This may take several minutes. Continue?") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRescanDialog = false
                        isRescanning = true
                        // Trigger rescan via WalletSuite
                        onRescan()
                    }
                ) {
                    Text("Start Rescan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Seed Phrase Dialog
    if (showSeedDialog) {
        AlertDialog(
            onDismissRequest = { showSeedDialog = false },
            title = { Text("Seed Phrase") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "⚠️ Never share your seed phrase with anyone!",
                        color = Color(0xFFFF6600),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your seed phrase is the master key to your wallet. Store it securely offline.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    // TODO: Get actual seed from WalletSuite
                    Text(
                        "[Seed phrase would be displayed here - implement wallet.getSeed() method]",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSeedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Node Configuration Dialog
    if (showNodeDialog) {
        AlertDialog(
            onDismissRequest = { showNodeDialog = false },
            title = { Text("Node Configuration") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Current daemon:")
                    Text(
                        "${walletSuite.daemonAddress}:${walletSuite.daemonPort}",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Text(
                        "To change the node, update wallet.properties file.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showNodeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (transaction.type == "Received") Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else Color(0xFFFF6600).copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (transaction.type == "Received") Icons.Default.CallReceived else Icons.Default.Send,
                        contentDescription = null,
                        tint = if (transaction.type == "Received") Color(0xFF4CAF50) else Color(0xFFFF6600)
                    )
                }
                
                Column {
                    Text(
                        transaction.type,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        transaction.date,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (transaction.type == "Received") "+" else "-"}${transaction.amount} XMR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == "Received") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
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
                            "Confirmed",
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
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
                verticalAlignment = Alignment.CenterVertically
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column {
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

data class Transaction(
    val type: String,
    val amount: String,
    val date: String,
    val confirmed: Boolean
)

// Extension function to get transaction history from WalletSuite
fun WalletSuite.getTransactionHistory(): List<com.m2049r.xmrwallet.model.TransactionInfo> {
    return try {
        val walletField = this.javaClass.getDeclaredField("wallet")
        walletField.isAccessible = true
        val wallet = walletField.get(this) as? com.m2049r.xmrwallet.model.Wallet
        
        wallet?.let {
            val history = it.history
            history?.refresh()
            history?.all ?: emptyList()
        } ?: emptyList()
    } catch (e: Exception) {
        android.util.Log.e("WalletSuite", "Failed to get transaction history", e)
        emptyList()
    }
}

@Composable
fun ReceiveDialog(
    address: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var addressCopied by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Receive Monero",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // QR Code Placeholder
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO: Implement actual QR code generation
                    // For now, show a placeholder
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            modifier = Modifier.size(100.dp),
                            tint = Color.Black
                        )
                        Text(
                            "QR Code",
                            color = Color.Black,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Text(
                    "Your Monero Address",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            address,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(address))
                        addressCopied = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (addressCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (addressCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (addressCopied) "Copied!" else "Copy Address")
                }
                
                Text(
                    "Share this address to receive Monero payments",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
