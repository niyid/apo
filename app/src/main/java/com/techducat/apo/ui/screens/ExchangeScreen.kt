package com.techducat.apo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.WalletSuite
import com.techducat.apo.network.ChangeNowSwapService
import com.techducat.apo.R
import androidx.compose.ui.text.font.FontFamily

// ============================================================================
// EXCHANGESCREEN
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
                    stringResource(R.string.exchange_service_not_available),
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
    
    // Extract string resources outside of coroutine
    val unknownErrorMsg = stringResource(R.string.unknown_error)
    
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
                    stringResource(R.string.nav_exchange), 
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
                                stringResource(R.string.exchange_available_xmr), 
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
                        Text(stringResource(R.string.from_label), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                                placeholder = { Text(stringResource(R.string.hint_enter_amount)) },
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
                    label = { 
                        Text(
                            stringResource(
                                id = R.string.exchange_recipient_address,
                                toCurrency.uppercase()
                            )
                        ) 
                    },
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
                                snackbarHost.showSnackbar("Exchange failed: ${it.message ?: unknownErrorMsg}")
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
                        Text(stringResource(R.string.loading_creating_exchange))
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
                            Text(stringResource(R.string.exchange_status_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Status: ${status.status.uppercase()}", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Send to: ${status.payinAddress}", 
                                fontSize = 10.sp, 
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
